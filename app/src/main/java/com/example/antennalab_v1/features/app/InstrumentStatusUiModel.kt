package com.example.antennalab_v1.features.app

/*
------------------------------------------------------------
FILE: InstrumentStatusUiModel.kt
PACKAGE: com.example.antennalab_v1.features.app
LAYER: UI / App Instrument Status

LAST UPDATED 4/4/2026 00:20

SYSTEM ROLE
Provides shared operator-facing instrument status models and mapping
helpers that can be reused across the whole app.

CURRENT DEVELOPMENT ROLE
This file is the shared truth-presentation layer for:

• compact instrument status cards
• full instrument details screens
• operator status summaries
• technical identity visibility
• engineering/debug details

IMPORTANT RULE
Screens should stop rebuilding raw USB/session truth themselves.
They should consume these shared mapped models instead.

IMPORTANT FIX
When a LiteVNA profile is selected, operator-facing titles now prefer
the selected profile truth over older project/request fallback labels
such as NanoVNA-H4.
------------------------------------------------------------
*/

import android.content.Context
import com.example.antennalab_v1.domain.testing.UsbSessionManager
import com.example.antennalab_v1.model.DriverProfile
import com.example.antennalab_v1.model.DriverProtocolType
import com.example.antennalab_v1.model.testing.InstrumentDataSourceKind
import com.example.antennalab_v1.model.testing.InstrumentSessionState
import com.example.antennalab_v1.model.testing.MeasurementTrustLevel

data class InstrumentStatusCardUiModel(
    val title: String = "No Instrument",
    val subtitle: String = "No active instrument session.",
    val connectionLabel: String = "Disconnected",
    val transportLabel: String = "Not Ready",
    val pathLabel: String = "No Instrument",
    val trustLabel: String = "Unknown",
    val calibrationLabel: String = "Not Started",
    val statusLabel: String = "Unavailable",
    val statusSummary: String = "No instrument session state is available.",
    val detailsButtonLabel: String = "Open Instrument Details"
)

data class InstrumentDetailsUiModel(
    val headerTitle: String = "Instrument Details",
    val headerSubtitle: String = "Global instrument and session status",
    val instrumentTitle: String = "No Instrument",
    val technicalIdentityLabel: String = "No technical identity available.",
    val selectedProfileLabel: String = "No profile selected",
    val protocolLabel: String = "Unknown",
    val supportTierLabel: String = "Unknown",
    val connectionLabel: String = "Disconnected",
    val transportLabel: String = "Not Ready",
    val dataPathLabel: String = "No Instrument",
    val trustLabel: String = "Unknown",
    val calibrationLabel: String = "Not Started",
    val calibrationSummary: String = "No calibration summary available.",
    val operatorSummary: String = "No instrument session state is available.",
    val sessionSummary: String = "No session summary available.",
    val transportSummary: String = "No transport summary available.",
    val lastCommandStatusLabel: String = "Unknown",
    val lastReadSizeLabel: String = "—",
    val lastErrorLabel: String = "None",
    val liteVnaDebugSummary: String = "No LiteVNA debug summary available."
)

object InstrumentStatusUiMapper {

    fun buildCardUiModel(
        context: Context,
        selectedHardwareName: String
    ): InstrumentStatusCardUiModel {
        val state = resolveInstrumentSessionState(context, selectedHardwareName)

        return InstrumentStatusCardUiModel(
            title = buildOperatorInstrumentTitle(
                state = state,
                selectedHardwareName = selectedHardwareName
            ),
            subtitle = buildInstrumentSubtitle(state),
            connectionLabel = buildConnectionLabel(state),
            transportLabel = buildTransportLabel(state),
            pathLabel = buildPathLabel(state),
            trustLabel = buildTrustLabel(state),
            calibrationLabel = buildCalibrationLabel(state),
            statusLabel = buildStatusLabel(state),
            statusSummary = buildOperatorSummary(state)
        )
    }

    fun buildDetailsUiModel(
        context: Context,
        selectedHardwareName: String
    ): InstrumentDetailsUiModel {
        val state = resolveInstrumentSessionState(context, selectedHardwareName)
        val selectedProfile = UsbSessionManager.getSelectedDriverProfile()
        val transportHealth = UsbSessionManager.getLatestTransportHealthSnapshot()

        return InstrumentDetailsUiModel(
            instrumentTitle = buildOperatorInstrumentTitle(
                state = state,
                selectedHardwareName = selectedHardwareName
            ),
            technicalIdentityLabel = buildTechnicalIdentityLabel(state),
            selectedProfileLabel = buildSelectedProfileLabel(selectedProfile),
            protocolLabel = state?.protocolGuess ?: selectedProfile?.protocolType?.name ?: "Unknown",
            supportTierLabel = state?.supportTier ?: "Unknown",
            connectionLabel = buildConnectionLabel(state),
            transportLabel = buildTransportLabel(state),
            dataPathLabel = buildPathLabel(state),
            trustLabel = buildTrustLabel(state),
            calibrationLabel = buildCalibrationLabel(state),
            calibrationSummary = state?.calibrationStatusSummary ?: "No calibration summary available.",
            operatorSummary = buildOperatorSummary(state),
            sessionSummary = state?.sessionSummary ?: "No session summary available.",
            transportSummary = state?.transportStatusSummary ?: "No transport summary available.",
            lastCommandStatusLabel = if (transportHealth.lastCommandSucceeded) "OK" else "FAIL",
            lastReadSizeLabel = transportHealth.lastReadSizeBytes?.toString() ?: "—",
            lastErrorLabel = transportHealth.lastErrorReason ?: "None",
            liteVnaDebugSummary = UsbSessionManager.getLiteVnaBringUpDebugSummary()
        )
    }

    private fun resolveInstrumentSessionState(
        context: Context,
        selectedHardwareName: String
    ): InstrumentSessionState? {
        return UsbSessionManager.getLatestInstrumentSessionState()
            ?: UsbSessionManager.buildInstrumentSessionState(
                context = context,
                selectedHardwareName = selectedHardwareName
            )
    }

    internal fun buildOperatorInstrumentTitle(
        state: InstrumentSessionState?,
        selectedHardwareName: String
    ): String {
        val selectedProfile = UsbSessionManager.getSelectedDriverProfile()
        val liteProfileSelected =
            selectedProfile?.protocolType == DriverProtocolType.LITE_VNA_V2_STYLE

        return when {
            liteProfileSelected ->
                buildLiteVnaOperatorDisplayName(selectedProfile)

            selectedHardwareName.isNotBlank() &&
                    !selectedHardwareName.contains("nano", ignoreCase = true) ->
                selectedHardwareName

            !state?.selectedHardwareName.isNullOrBlank() &&
                    !state?.selectedHardwareName.orEmpty().contains("nano", ignoreCase = true) ->
                state?.selectedHardwareName ?: "No Instrument"

            !selectedProfile?.displayName.isNullOrBlank() ->
                selectedProfile?.displayName ?: "No Instrument"

            !state?.instrumentIdentityText.isNullOrBlank() ->
                state?.instrumentIdentityText ?: "No Instrument"

            selectedHardwareName.isNotBlank() ->
                selectedHardwareName

            else ->
                "No Instrument"
        }
    }

    internal fun buildLiteVnaOperatorDisplayName(
        selectedProfile: DriverProfile?
    ): String {
        return when {
            !selectedProfile?.displayName.isNullOrBlank() &&
                    selectedProfile!!.displayName.contains("lite", ignoreCase = true) ->
                selectedProfile.displayName

            else ->
                "LiteVNA64 HW 64-0.3.3 FW v1.4.06"
        }
    }

    internal fun buildTechnicalIdentityLabel(
        state: InstrumentSessionState?
    ): String {
        return when {
            state == null -> "No technical identity available."
            !state.instrumentIdentityText.isNullOrBlank() -> state.instrumentIdentityText
            !state.connectionInfo.deviceName.isNullOrBlank() -> state.connectionInfo.deviceName
            else -> "No technical identity available."
        }
    }

    internal fun buildInstrumentSubtitle(state: InstrumentSessionState?): String {
        return when {
            state == null ->
                "No active instrument session."
            state.transportReady &&
                    state.dataSourceKind == InstrumentDataSourceKind.REAL_INSTRUMENT ->
                "Live transport path is ready."
            state.transportReady ->
                "Transport is prepared, but live path is not fully ready."
            state.connectionInfo.sessionOpen ->
                "USB session is open, but transport is not ready."
            else ->
                "Open a device session to begin."
        }
    }

    internal fun buildSelectedProfileLabel(selectedProfile: DriverProfile?): String {
        return when {
            selectedProfile == null -> "No profile selected"
            selectedProfile.protocolType == DriverProtocolType.LITE_VNA_V2_STYLE ->
                "LiteVNA Profile"
            else -> selectedProfile.displayName
        }
    }

    internal fun buildConnectionLabel(state: InstrumentSessionState?): String {
        return when {
            state == null -> "Disconnected"
            state.connectionInfo.sessionOpen -> "Session Open"
            state.connectionInfo.permissionGranted -> "Detected"
            else -> state.connectionInfo.state.name.replace("_", " ")
        }
    }

    internal fun buildTransportLabel(state: InstrumentSessionState?): String {
        return if (state?.transportReady == true) "Ready" else "Not Ready"
    }

    internal fun buildPathLabel(state: InstrumentSessionState?): String {
        return when (state?.dataSourceKind) {
            InstrumentDataSourceKind.REAL_INSTRUMENT -> "Real Instrument"
            InstrumentDataSourceKind.SIMULATED -> "Simulated"
            InstrumentDataSourceKind.NONE, null -> "No Instrument"
        }
    }

    internal fun buildTrustLabel(state: InstrumentSessionState?): String {
        return when (state?.measurementTrust) {
            MeasurementTrustLevel.TRUSTED -> "Trusted"
            MeasurementTrustLevel.DEGRADED -> "Degraded"
            MeasurementTrustLevel.PARTIAL -> "Partial"
            MeasurementTrustLevel.SIMULATED -> "Simulated"
            MeasurementTrustLevel.UNKNOWN, null -> "Unknown"
        }
    }

    internal fun buildCalibrationLabel(state: InstrumentSessionState?): String {
        return state?.calibrationState?.readiness?.name
            ?.replace("_", " ")
            ?.lowercase()
            ?.replaceFirstChar { it.uppercase() }
            ?: "Not Started"
    }

    internal fun buildStatusLabel(state: InstrumentSessionState?): String {
        return when {
            state == null -> "Unavailable"
            state.transportReady &&
                    state.dataSourceKind == InstrumentDataSourceKind.REAL_INSTRUMENT ->
                "Live Ready"
            state.transportReady ->
                "Transport Ready"
            state.connectionInfo.sessionOpen ->
                "Session Open"
            state.connectionInfo.permissionGranted ->
                "Detected"
            else ->
                "Disconnected"
        }
    }

    internal fun buildOperatorSummary(state: InstrumentSessionState?): String {
        return when {
            state == null ->
                "No instrument session state is available."
            state.transportReady &&
                    state.dataSourceKind == InstrumentDataSourceKind.REAL_INSTRUMENT ->
                "Instrument is ready for live measurement."
            state.transportReady && state.connectionInfo.sessionOpen ->
                "USB session is open and transport is ready, but live measurement readiness is still limited."
            state.connectionInfo.sessionOpen ->
                "USB session is open, but transport is not yet ready."
            state.connectionInfo.permissionGranted ->
                "Device is detected and permission is granted. Open the session to continue."
            else ->
                state.transportStatusSummary
        }
    }
}