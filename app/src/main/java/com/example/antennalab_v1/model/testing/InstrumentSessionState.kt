package com.example.antennalab_v1.model.testing

import com.example.antennalab_v1.model.HardwareMeasurementCapabilities
import com.example.antennalab_v1.model.UsbConnectionInfo

/*
########################################################################
FILE: InstrumentSessionState.kt
PACKAGE: com.example.antennalab_v1.model.testing
LAYER: Model / Testing / Instrument Session

LAST UPDATED 21/3/2026 20:55

SYSTEM ROLE
Provides a single unified instrument session snapshot for UI and
workflow logic.

ARCHITECTURE ROLE
Combines:

• physical connection state
• transport readiness
• protocol identity
• support tier
• capability profile
• trust level
• active data source classification
• resolved driver identity
• protocol guess
• discovery summary
• measurement trust summary
• shared calibration truth

IMPORTANT
This model is UI-safe and future-proof. It is intended to remain the
single source of truth for instrument state across testing screens.

SAFE EDIT AREA
- add richer discovery classification details later
- add error taxonomy fields later
- add negotiated session metadata later
- add multi-device session metadata later
########################################################################
*/

/*
########################################################################
EDIT SECTION 1000
MEASUREMENT TRUST MODEL
########################################################################
*/
enum class MeasurementTrustLevel {
    TRUSTED,
    DEGRADED,
    PARTIAL,
    SIMULATED,
    UNKNOWN
}

/*
########################################################################
EDIT SECTION 1100
DATA SOURCE MODEL
########################################################################
*/
enum class InstrumentDataSourceKind {
    NONE,
    REAL_INSTRUMENT,
    SIMULATED
}

/*
########################################################################
EDIT SECTION 2000
INSTRUMENT SESSION STATE
########################################################################
*/
data class InstrumentSessionState(
    val selectedHardwareName: String,
    val connectionInfo: UsbConnectionInfo,
    val transportReady: Boolean,
    val transportStatusSummary: String,
    val protocolFamily: String,
    val protocolGuess: String = "Unknown",
    val supportTier: String,
    val resolvedDriverId: String? = null,
    val capabilityProfile: HardwareMeasurementCapabilities,
    val measurementTrust: MeasurementTrustLevel,
    val measurementTrustSummary: String = "",
    val calibrationState: InstrumentCalibrationState = InstrumentCalibrationState(),
    val calibrationStatusSummary: String = calibrationState.statusSummary,
    val dataSourceKind: InstrumentDataSourceKind,
    val discoverySummary: String = "",
    val instrumentIdentityText: String? = null,
    val sessionSummary: String
)