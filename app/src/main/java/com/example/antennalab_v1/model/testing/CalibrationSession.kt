package com.example.antennalab_v1.model.testing

/*
########################################################################
FILE: CalibrationSession.kt
PACKAGE: com.example.antennalab_v1.model.testing
LAYER: Model / Testing / Calibration

LAST UPDATED 4/4/2026 13:35

SYSTEM ROLE
Represents the captured calibration content for a sweep-capable
instrument session.

ARCHITECTURE ROLE
This model stores the calibration capture result itself.

Shared truth about whether that calibration is currently valid,
stale, or usable belongs in InstrumentCalibrationState.
########################################################################
*/

/*
########################################################################
SECTION 1000
CALIBRATION STEP ENUM
########################################################################
*/
enum class CalibrationStep {
    OPEN,
    SHORT,
    LOAD
}

/*
########################################################################
SECTION 1050
CALIBRATION CAPTURE SOURCE
########################################################################
*/
enum class CalibrationCaptureSource {
    WIZARD,
    RESTORED_FROM_PROJECT,
    IMPORTED,
    UNKNOWN
}

/*
########################################################################
SECTION 1100
CALIBRATION COMPLETION STATE
########################################################################
*/
enum class CalibrationCompletionState {
    NOT_STARTED,
    PARTIAL,
    COMPLETE
}

/*
########################################################################
SECTION 1200
CALIBRATION SESSION MODEL
########################################################################
*/
data class CalibrationSession(
    val hardwareDisplayName: String,
    val startFrequencyMHz: Double,
    val endFrequencyMHz: Double,
    val openCaptured: Boolean = false,
    val shortCaptured: Boolean = false,
    val loadCaptured: Boolean = false,
    val timestampLabel: String = "Not Captured",
    val capturedAtEpochMs: Long = 0L,
    val captureSource: CalibrationCaptureSource = CalibrationCaptureSource.WIZARD,
    val capturedProtocolFamily: String? = null,
    val capturedInstrumentIdentityText: String? = null,
    val capturedSessionKey: String? = null
) {
    val completionState: CalibrationCompletionState
        get() = when {
            openCaptured && shortCaptured && loadCaptured ->
                CalibrationCompletionState.COMPLETE

            openCaptured || shortCaptured || loadCaptured ->
                CalibrationCompletionState.PARTIAL

            else ->
                CalibrationCompletionState.NOT_STARTED
        }

    val completedStepCount: Int
        get() = listOf(openCaptured, shortCaptured, loadCaptured).count { it }

    fun isStepCaptured(step: CalibrationStep): Boolean =
        when (step) {
            CalibrationStep.OPEN -> openCaptured
            CalibrationStep.SHORT -> shortCaptured
            CalibrationStep.LOAD -> loadCaptured
        }

    fun hasAnyCapturedStep(): Boolean {
        return openCaptured || shortCaptured || loadCaptured
    }

    fun isFullyCaptured(): Boolean {
        return openCaptured && shortCaptured && loadCaptured
    }

    fun matchesHardwareDisplayName(
        selectedHardwareName: String
    ): Boolean {
        return normalizeIdentity(hardwareDisplayName) ==
                normalizeIdentity(selectedHardwareName)
    }

    fun matchesProtocolFamily(
        protocolFamily: String?
    ): Boolean {
        if (capturedProtocolFamily.isNullOrBlank() || protocolFamily.isNullOrBlank()) {
            return true
        }

        return normalizeIdentity(capturedProtocolFamily) ==
                normalizeIdentity(protocolFamily)
    }

    fun matchesInstrumentIdentity(
        instrumentIdentityText: String?
    ): Boolean {
        if (capturedInstrumentIdentityText.isNullOrBlank() || instrumentIdentityText.isNullOrBlank()) {
            return true
        }

        return normalizeIdentity(capturedInstrumentIdentityText) ==
                normalizeIdentity(instrumentIdentityText)
    }

    fun coversFrequencyRange(
        requestedStartMHz: Double,
        requestedEndMHz: Double
    ): Boolean {
        return startFrequencyMHz <= requestedStartMHz &&
                endFrequencyMHz >= requestedEndMHz
    }

    fun isCompatibleWithRequestedRange(
        selectedHardwareName: String,
        requestedStartMHz: Double,
        requestedEndMHz: Double
    ): Boolean {
        return isFullyCaptured() &&
                matchesHardwareDisplayName(selectedHardwareName) &&
                coversFrequencyRange(
                    requestedStartMHz = requestedStartMHz,
                    requestedEndMHz = requestedEndMHz
                )
    }

    private fun normalizeIdentity(
        rawText: String
    ): String {
        return rawText
            .trim()
            .lowercase()
            .replace(" ", "")
            .replace("-", "")
            .replace("_", "")
    }
}