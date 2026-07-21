package com.example.antennalab_v1.model.testing

import com.example.antennalab_v1.model.UsbConnectionInfo

/*
########################################################################
FILE: UsbHardwareSession.kt
PACKAGE: com.example.antennalab_v1.model.testing
LAYER: Model / Testing

LAST UPDATED 21/3/2026 21:42

SYSTEM ROLE
Carries the current hardware-session snapshot used by system-level USB
screens and setup workflows.

CURRENT DEVELOPMENT ROLE
Now also carries the unified instrument session truth model so screen
flows can migrate away from duplicated discovery and status resolution.

ARCHITECTURE ROLE
Provides a stable transport/session wrapper around the richer shared
InstrumentSessionState model.

This wrapper intentionally stays small, but now exposes calibration-safe
read helpers so callers do not need to repeatedly null-check the shared
instrument truth.

SAFE EDIT AREA
- add per-device identifiers later
- add multi-device selection metadata later
- add session history later
########################################################################
*/
data class UsbHardwareSession(
    val selectedHardwareName: String,
    val connectionInfo: UsbConnectionInfo = UsbConnectionInfo(),
    val instrumentSessionState: InstrumentSessionState? = null
) {
    /*
    ####################################################################
    SHARED SESSION PROJECTION HELPERS
    --------------------------------------------------------------------
    PURPOSE
    Provides safe read helpers for callers that only hold the outer
    UsbHardwareSession wrapper.
    ####################################################################
    */

    val calibrationStateOrDefault: InstrumentCalibrationState
        get() = instrumentSessionState?.calibrationState ?: InstrumentCalibrationState()

    val calibrationStatusSummaryOrDefault: String
        get() = calibrationStateOrDefault.statusSummary

    val calibrationWarningOrNull: String?
        get() = calibrationStateOrDefault.operatorWarning
            .takeIf { calibrationStateOrDefault.trustDowngraded }

    val measurementTrustOrDefault: MeasurementTrustLevel
        get() = instrumentSessionState?.measurementTrust ?: MeasurementTrustLevel.UNKNOWN

    val transportReadyOrDefault: Boolean
        get() = instrumentSessionState?.transportReady ?: false
}