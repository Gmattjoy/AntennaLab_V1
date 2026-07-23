package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.DriverProfileRegistry
import com.example.antennalab_v1.domain.testing.LiteVnaBringUpResult
import com.example.antennalab_v1.features.app.DeviceConnectionsController
import com.example.antennalab_v1.model.HardwareConnectionState
import com.example.antennalab_v1.model.testing.CalibrationReadiness
import com.example.antennalab_v1.model.testing.MeasurementTrustLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavior coverage for the DeviceConnectionsScreen (hardware/driver selection)
 * decision logic extracted into the pure [DeviceConnectionsController]: default
 * profile selection, LiteVNA detection + validation state machine, trust /
 * calibration labels, action-button gating, and operator guidance text.
 *
 * Uses the real DriverProfileRegistry for profile inputs and constructs real
 * LiteVnaBringUpResult values — no mocking. Pure logic, so plain JVM.
 */
class DeviceConnectionsControllerTest {

    private val liteProfile = DriverProfileRegistry.getProfileById("litevna64_standard")!!
    private val nanoProfile = DriverProfileRegistry.getProfileById("nano_h_default")!!

    private fun result(stage: String, success: Boolean = false) =
        LiteVnaBringUpResult(success = success, stage = stage, summary = "s")

    // ------------------------------------------------------------------
    // Driver / profile selection
    // ------------------------------------------------------------------

    @Test
    fun preferredDefaultProfile_prefersLiteThenFallsBackToFirst() {
        // Registry contains LiteVNA profiles -> a LiteVNA one is preferred.
        val chosen = DeviceConnectionsController.preferredDefaultProfile(DriverProfileRegistry.profiles)
        assertTrue(DeviceConnectionsController.isLiteProfile(chosen))

        // No LiteVNA in the list -> first element.
        val nonLiteOnly = listOf(nanoProfile)
        assertEquals(nanoProfile, DeviceConnectionsController.preferredDefaultProfile(nonLiteOnly))
    }

    @Test
    fun isLiteProfile_detectsLiteProtocolOnly() {
        assertTrue(DeviceConnectionsController.isLiteProfile(liteProfile))
        assertFalse(DeviceConnectionsController.isLiteProfile(nanoProfile))
    }

    @Test
    fun buildProfileDisplayLabel_fixedForLite_displayNameOtherwise() {
        assertEquals(
            "LiteVNA64 HW 64-0.3.3 FW v1.4.06",
            DeviceConnectionsController.buildProfileDisplayLabel(liteProfile)
        )
        assertEquals(nanoProfile.displayName, DeviceConnectionsController.buildProfileDisplayLabel(nanoProfile))
    }

    // ------------------------------------------------------------------
    // LiteVNA validation state machine
    // ------------------------------------------------------------------

    @Test
    fun liteIdentityConfirmed_trueOnlyWhenSuccess() {
        assertTrue(DeviceConnectionsController.liteIdentityConfirmed(result("IDENTIFY_OK", success = true)))
        assertFalse(DeviceConnectionsController.liteIdentityConfirmed(result("IDENTIFY_OK", success = false)))
        assertFalse(DeviceConnectionsController.liteIdentityConfirmed(null))
    }

    @Test
    fun liteRegisterConfirmed_trueForEitherOkStage() {
        assertTrue(DeviceConnectionsController.liteRegisterConfirmed(result("REGISTER_READ_OK")))
        assertTrue(DeviceConnectionsController.liteRegisterConfirmed(result("SWEEP_PROBE_OK")))
        assertFalse(DeviceConnectionsController.liteRegisterConfirmed(result("SOMETHING_ELSE")))
        assertFalse(DeviceConnectionsController.liteRegisterConfirmed(null))
    }

    @Test
    fun liteTimedOut_trueIfAnySourceTimedOut() {
        val ok = result("OK")
        assertTrue(DeviceConnectionsController.liteTimedOut(result("TIMED_OUT"), ok, ok))
        assertTrue(DeviceConnectionsController.liteTimedOut(ok, result("TIMED_OUT"), ok))
        assertTrue(DeviceConnectionsController.liteTimedOut(ok, ok, result("TIMED_OUT")))
        assertFalse(DeviceConnectionsController.liteTimedOut(ok, ok, ok))
        assertFalse(DeviceConnectionsController.liteTimedOut(null, null, null))
    }

    @Test
    fun liteValidationRunning_trueOnlyWhenLiteReadyAndNothingResolvedYet() {
        assertTrue(
            DeviceConnectionsController.liteValidationRunning(
                isLiteProfile = true, sessionOpen = true, transportReady = true,
                liteIdentityConfirmed = false, liteRegisterConfirmed = false, liteTimedOut = false
            )
        )
        // Any precondition off, or any resolution, -> not running.
        assertFalse(
            DeviceConnectionsController.liteValidationRunning(
                isLiteProfile = false, sessionOpen = true, transportReady = true,
                liteIdentityConfirmed = false, liteRegisterConfirmed = false, liteTimedOut = false
            )
        )
        assertFalse(
            DeviceConnectionsController.liteValidationRunning(
                isLiteProfile = true, sessionOpen = false, transportReady = true,
                liteIdentityConfirmed = false, liteRegisterConfirmed = false, liteTimedOut = false
            )
        )
        assertFalse(
            DeviceConnectionsController.liteValidationRunning(
                isLiteProfile = true, sessionOpen = true, transportReady = true,
                liteIdentityConfirmed = true, liteRegisterConfirmed = false, liteTimedOut = false
            )
        )
        assertFalse(
            DeviceConnectionsController.liteValidationRunning(
                isLiteProfile = true, sessionOpen = true, transportReady = true,
                liteIdentityConfirmed = false, liteRegisterConfirmed = false, liteTimedOut = true
            )
        )
    }

    // ------------------------------------------------------------------
    // Status labels
    // ------------------------------------------------------------------

    @Test
    fun trustText_mapsEveryLevelAndNull() {
        assertEquals("Trusted", DeviceConnectionsController.trustText(MeasurementTrustLevel.TRUSTED))
        assertEquals("Degraded", DeviceConnectionsController.trustText(MeasurementTrustLevel.DEGRADED))
        assertEquals("Partial", DeviceConnectionsController.trustText(MeasurementTrustLevel.PARTIAL))
        assertEquals("Simulated", DeviceConnectionsController.trustText(MeasurementTrustLevel.SIMULATED))
        assertEquals("Unknown", DeviceConnectionsController.trustText(MeasurementTrustLevel.UNKNOWN))
        assertEquals("Unknown", DeviceConnectionsController.trustText(null))
    }

    @Test
    fun calibrationStateLabel_usesReadinessNameOrDefault() {
        assertEquals("VALID", DeviceConnectionsController.calibrationStateLabel(CalibrationReadiness.VALID))
        assertEquals("STALE", DeviceConnectionsController.calibrationStateLabel(CalibrationReadiness.STALE))
        assertEquals("NOT_STARTED", DeviceConnectionsController.calibrationStateLabel(null))
    }

    // ------------------------------------------------------------------
    // Action-button gating
    // ------------------------------------------------------------------

    @Test
    fun buttonGating_reflectsConnectionState() {
        assertTrue(DeviceConnectionsController.showRequestPermission(HardwareConnectionState.PERMISSION_REQUIRED))
        assertFalse(DeviceConnectionsController.showRequestPermission(HardwareConnectionState.READY))
        assertFalse(DeviceConnectionsController.showRequestPermission(null))

        assertTrue(DeviceConnectionsController.showConnect(permissionGranted = true, sessionOpen = false))
        assertFalse(DeviceConnectionsController.showConnect(permissionGranted = true, sessionOpen = true))
        assertFalse(DeviceConnectionsController.showConnect(permissionGranted = false, sessionOpen = false))

        assertTrue(DeviceConnectionsController.showDisconnect(sessionOpen = true))
        assertFalse(DeviceConnectionsController.showDisconnect(sessionOpen = false))

        assertTrue(DeviceConnectionsController.showValidateLiteVna(isLiteProfile = true, sessionOpen = true, transportReady = true))
        assertFalse(DeviceConnectionsController.showValidateLiteVna(isLiteProfile = false, sessionOpen = true, transportReady = true))
        assertFalse(DeviceConnectionsController.showValidateLiteVna(isLiteProfile = true, sessionOpen = true, transportReady = false))
    }

    // ------------------------------------------------------------------
    // Operator guidance
    // ------------------------------------------------------------------

    @Test
    fun buildNextHardwareStepText_coversTheDecisionLadder() {
        fun step(
            connectionState: HardwareConnectionState? = HardwareConnectionState.READY,
            permissionGranted: Boolean = true,
            sessionOpen: Boolean = true,
            transportReady: Boolean = true,
            isLiteProfile: Boolean = false,
            liveInstrumentReady: Boolean = false,
            liteValidationRunning: Boolean = false,
            liteIdentityConfirmed: Boolean = false,
            liteRegisterConfirmed: Boolean = false
        ) = DeviceConnectionsController.buildNextHardwareStepText(
            connectionState, permissionGranted, sessionOpen, transportReady, isLiteProfile,
            liveInstrumentReady, liteValidationRunning, liteIdentityConfirmed, liteRegisterConfirmed
        )

        assertEquals(
            "Attach the USB instrument, then press Refresh.",
            step(connectionState = HardwareConnectionState.NOT_CONNECTED)
        )
        assertEquals("Grant USB permission for the selected device.", step(permissionGranted = false))
        assertEquals("Open a device session.", step(sessionOpen = false))
        assertEquals("Refresh and confirm the transport becomes ready.", step(transportReady = false))
        assertEquals(
            "LiteVNA validation is running. Wait for it to complete.",
            step(isLiteProfile = true, liteValidationRunning = true)
        )
        assertEquals(
            "Run Validate Device before moving on to calibration or sweep.",
            step(isLiteProfile = true, liteIdentityConfirmed = false, liteRegisterConfirmed = false)
        )
        assertEquals(
            "Hardware is ready. Move on to calibration or sweep.",
            step(liveInstrumentReady = true)
        )
        assertEquals(
            "Open Instrument Details / Troubleshooting if readiness still looks wrong.",
            step(liveInstrumentReady = false)
        )
    }

    @Test
    fun buildValidationLabel_coversAllSevenBranches() {
        fun label(
            isLiteProfile: Boolean = true,
            liveInstrumentReady: Boolean = false,
            liteValidationRunning: Boolean = false,
            liteIdentityConfirmed: Boolean = false,
            liteRegisterConfirmed: Boolean = false,
            liteTimedOut: Boolean = false
        ) = DeviceConnectionsController.buildValidationLabel(
            isLiteProfile, liveInstrumentReady, liteValidationRunning,
            liteIdentityConfirmed, liteRegisterConfirmed, liteTimedOut
        )

        assertEquals("Ready", label(isLiteProfile = false, liveInstrumentReady = true))
        assertEquals("Not Required", label(isLiteProfile = false, liveInstrumentReady = false))
        assertEquals("Running", label(liteValidationRunning = true))
        assertEquals("Passed", label(liteIdentityConfirmed = true, liteRegisterConfirmed = true))
        assertEquals("Timed Out", label(liteTimedOut = true))
        assertEquals("Partial", label(liteIdentityConfirmed = true))
        assertEquals("Pending", label())
    }
}
