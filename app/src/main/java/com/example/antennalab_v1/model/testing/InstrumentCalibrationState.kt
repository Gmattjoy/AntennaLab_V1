package com.example.antennalab_v1.model.testing

/*
########################################################################
FILE: InstrumentCalibrationState.kt
PACKAGE: com.example.antennalab_v1.model.testing
LAYER: Model / Testing / Shared Calibration Truth

LAST UPDATED 4/4/2026 13:35

SYSTEM ROLE
Provides the shared calibration truth model for the currently active
instrument session.
########################################################################
*/

enum class CalibrationReadiness {
    NOT_STARTED,
    IN_PROGRESS,
    VALID,
    STALE,
    INVALID
}

data class InstrumentCalibrationState(
    val readiness: CalibrationReadiness = CalibrationReadiness.NOT_STARTED,
    val calibrationSession: CalibrationSession? = null,
    val sessionKeyAtCapture: String? = null,
    val activeSessionKey: String? = null,
    val statusSummary: String = "No calibration has been captured for this instrument session.",
    val operatorWarning: String = "Measurements are currently uncalibrated and should be treated with reduced trust.",
    val sweepAllowed: Boolean = true,
    val trustDowngraded: Boolean = true
)