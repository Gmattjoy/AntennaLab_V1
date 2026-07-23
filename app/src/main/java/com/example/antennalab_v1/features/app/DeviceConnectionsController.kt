package com.example.antennalab_v1.features.app

/*
########################################################################
FILE: DeviceConnectionsController.kt
PACKAGE: com.example.antennalab_v1.features.app
LAYER: UI / App / Connections-Devices / Logic Control

SYSTEM ROLE
Owns the pure (non-Compose) decision logic for DeviceConnectionsScreen
(the hardware/driver selection + connection screen) so the screen can
stay a Compose shell around side-effecting session/permission calls.

It currently owns:

• driver-profile default selection + LiteVNA detection + display label
• LiteVNA validation state machine (identity / register / timeout /
  running) derived from bring-up result stage codes
• connection trust + calibration-readiness labels
• action-button visibility gating
• next-step operator guidance + validation status label

DESIGN INTENT
Keep this layer pure and easy to test/migrate. Mirrors the same UI-free
controller pattern used by the sweep / wizard / workspace controllers.

IMPORTANT
This file intentionally avoids Compose APIs and Android dependencies.
The screen keeps all UsbSessionManager / UsbPermissionManager calls, the
BroadcastReceiver, and Compose state; it feeds this controller plain
values and renders the results.
########################################################################
*/

import com.example.antennalab_v1.domain.testing.LiteVnaBringUpResult
import com.example.antennalab_v1.model.DriverProfile
import com.example.antennalab_v1.model.HardwareConnectionState
import com.example.antennalab_v1.model.testing.CalibrationReadiness
import com.example.antennalab_v1.model.testing.MeasurementTrustLevel

/*
########################################################################
SECTION 1000
CONTROLLER OBJECT
########################################################################
PURPOSE
Stateless decision helpers for DeviceConnectionsScreen.
########################################################################
*/
object DeviceConnectionsController {

    /*
    ------------------------------------------------------------
    SECTION 1100
    DRIVER / PROFILE SELECTION
    ------------------------------------------------------------
    PURPOSE
    Prefer a LiteVNA-style profile as the default; produce the operator
    display label for a profile.
    ------------------------------------------------------------
    */
    fun preferredDefaultProfile(profiles: List<DriverProfile>): DriverProfile {
        return profiles.firstOrNull { isLiteProfile(it) }
            ?: profiles.first()
    }

    fun isLiteProfile(profile: DriverProfile): Boolean {
        return profile.protocolType.name.contains("LITE", ignoreCase = true)
    }

    fun buildProfileDisplayLabel(profile: DriverProfile): String {
        return if (isLiteProfile(profile)) {
            "LiteVNA64 HW 64-0.3.3 FW v1.4.06"
        } else {
            profile.displayName
        }
    }

    /*
    ------------------------------------------------------------
    SECTION 1200
    LITEVNA VALIDATION STATE MACHINE
    ------------------------------------------------------------
    PURPOSE
    Derive LiteVNA validation flags from the latest bring-up result
    stage codes.
    ------------------------------------------------------------
    */
    fun liteIdentityConfirmed(liteIdentity: LiteVnaBringUpResult?): Boolean {
        return liteIdentity?.success == true
    }

    fun liteRegisterConfirmed(liteCommandTest: LiteVnaBringUpResult?): Boolean {
        return liteCommandTest?.stage == "REGISTER_READ_OK" ||
            liteCommandTest?.stage == "SWEEP_PROBE_OK"
    }

    fun liteTimedOut(
        liteIdentity: LiteVnaBringUpResult?,
        liteCommandTest: LiteVnaBringUpResult?,
        liteBringUp: LiteVnaBringUpResult?
    ): Boolean {
        return liteIdentity?.stage == "TIMED_OUT" ||
            liteCommandTest?.stage == "TIMED_OUT" ||
            liteBringUp?.stage == "TIMED_OUT"
    }

    fun liteValidationRunning(
        isLiteProfile: Boolean,
        sessionOpen: Boolean,
        transportReady: Boolean,
        liteIdentityConfirmed: Boolean,
        liteRegisterConfirmed: Boolean,
        liteTimedOut: Boolean
    ): Boolean {
        return isLiteProfile &&
            sessionOpen &&
            transportReady &&
            !liteIdentityConfirmed &&
            !liteRegisterConfirmed &&
            !liteTimedOut
    }

    /*
    ------------------------------------------------------------
    SECTION 1300
    STATUS LABELS
    ------------------------------------------------------------
    */
    fun trustText(trust: MeasurementTrustLevel?): String {
        return when (trust) {
            MeasurementTrustLevel.TRUSTED -> "Trusted"
            MeasurementTrustLevel.DEGRADED -> "Degraded"
            MeasurementTrustLevel.PARTIAL -> "Partial"
            MeasurementTrustLevel.SIMULATED -> "Simulated"
            MeasurementTrustLevel.UNKNOWN, null -> "Unknown"
        }
    }

    fun calibrationStateLabel(readiness: CalibrationReadiness?): String {
        return readiness?.name ?: "NOT_STARTED"
    }

    /*
    ------------------------------------------------------------
    SECTION 1400
    ACTION-BUTTON GATING
    ------------------------------------------------------------
    */
    fun showRequestPermission(connectionState: HardwareConnectionState?): Boolean {
        return connectionState == HardwareConnectionState.PERMISSION_REQUIRED
    }

    fun showConnect(permissionGranted: Boolean, sessionOpen: Boolean): Boolean {
        return permissionGranted && !sessionOpen
    }

    fun showDisconnect(sessionOpen: Boolean): Boolean {
        return sessionOpen
    }

    fun showValidateLiteVna(
        isLiteProfile: Boolean,
        sessionOpen: Boolean,
        transportReady: Boolean
    ): Boolean {
        return isLiteProfile && sessionOpen && transportReady
    }

    /*
    ------------------------------------------------------------
    SECTION 1500
    OPERATOR GUIDANCE
    ------------------------------------------------------------
    PURPOSE
    Next-step instruction text and the LiteVNA validation status label.
    ------------------------------------------------------------
    */
    fun buildNextHardwareStepText(
        connectionState: HardwareConnectionState?,
        permissionGranted: Boolean,
        sessionOpen: Boolean,
        transportReady: Boolean,
        isLiteProfile: Boolean,
        liveInstrumentReady: Boolean,
        liteValidationRunning: Boolean,
        liteIdentityConfirmed: Boolean,
        liteRegisterConfirmed: Boolean
    ): String {
        return when {
            connectionState == HardwareConnectionState.NOT_CONNECTED ->
                "Attach the USB instrument, then press Refresh."
            !permissionGranted ->
                "Grant USB permission for the selected device."
            !sessionOpen ->
                "Open a device session."
            !transportReady ->
                "Refresh and confirm the transport becomes ready."
            isLiteProfile && liteValidationRunning ->
                "LiteVNA validation is running. Wait for it to complete."
            isLiteProfile && (!liteIdentityConfirmed || !liteRegisterConfirmed) ->
                "Run Validate Device before moving on to calibration or sweep."
            liveInstrumentReady ->
                "Hardware is ready. Move on to calibration or sweep."
            else ->
                "Open Instrument Details / Troubleshooting if readiness still looks wrong."
        }
    }

    fun buildValidationLabel(
        isLiteProfile: Boolean,
        liveInstrumentReady: Boolean,
        liteValidationRunning: Boolean,
        liteIdentityConfirmed: Boolean,
        liteRegisterConfirmed: Boolean,
        liteTimedOut: Boolean
    ): String {
        return when {
            !isLiteProfile && liveInstrumentReady -> "Ready"
            !isLiteProfile -> "Not Required"
            liteValidationRunning -> "Running"
            liteIdentityConfirmed && liteRegisterConfirmed -> "Passed"
            liteTimedOut -> "Timed Out"
            liteIdentityConfirmed || liteRegisterConfirmed -> "Partial"
            else -> "Pending"
        }
    }
}
