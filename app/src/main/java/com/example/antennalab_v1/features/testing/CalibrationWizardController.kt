package com.example.antennalab_v1.features.testing

/*
########################################################################
FILE: CalibrationWizardController.kt
PACKAGE: com.example.antennalab_v1.features.testing
LAYER: UI / Testing / Calibration Wizard / Logic Control

SYSTEM ROLE
Owns the pure (non-Compose) OSL capture state machine + session
orchestration for CalibrationWizardScreen so the screen stays a Compose
shell around the side effects (running the sweep, registering the session,
invoking callbacks).

It currently owns:

• the fixed capture point count + O/S/L step order
• first-incomplete-step derivation
• the per-standard sweep step size
• building the updated CalibrationSession for a captured step
• applying a captured standard: accumulate, compute coefficients once all
  three are present, and derive the next step + completion

DESIGN INTENT
Keep this layer pure and easy to test/migrate. Mirrors the same UI-free
controller pattern used across the app. The OSL math itself lives in the
already-tested OslCalibrationEngine.

IMPORTANT
No Compose/Android here. The clock is injected (captureTimeMs) so the
session build is deterministic; the screen passes System.currentTimeMillis().
########################################################################
*/

import com.example.antennalab_v1.domain.testing.OslCalibrationEngine
import com.example.antennalab_v1.model.testing.CalibrationCaptureSource
import com.example.antennalab_v1.model.testing.CalibrationSession
import com.example.antennalab_v1.model.testing.CalibrationStep
import com.example.antennalab_v1.model.testing.SweepResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
########################################################################
SECTION 1000
CAPTURE RESULT
########################################################################
PURPOSE
Pure outcome of applying one captured standard: the new session, the
accumulated standards, the next step index, and whether calibration is
complete (the screen then performs the side effects).
########################################################################
*/
data class CalibrationCaptureResult(
    val updatedSession: CalibrationSession,
    val updatedCapturedStandards: Map<CalibrationStep, SweepResult>,
    val nextStepIndex: Int,
    val isComplete: Boolean
)

/*
########################################################################
SECTION 1100
CONTROLLER OBJECT
########################################################################
PURPOSE
Stateless capture state machine for CalibrationWizardScreen.
########################################################################
*/
object CalibrationWizardController {

    /** Fixed number of frequency points captured per OSL standard. */
    const val CALIBRATION_POINT_COUNT = 101

    /** Canonical capture order; indices returned by this controller map to it. */
    val steps: List<CalibrationStep> = listOf(
        CalibrationStep.OPEN,
        CalibrationStep.SHORT,
        CalibrationStep.LOAD
    )

    /*
    ------------------------------------------------------------
    SECTION 1200
    STEP-STATE DERIVATION
    ------------------------------------------------------------
    */
    fun findFirstIncompleteStepIndex(
        calibrationSession: CalibrationSession
    ): Int {
        return when {
            !calibrationSession.openCaptured -> 0
            !calibrationSession.shortCaptured -> 1
            !calibrationSession.loadCaptured -> 2
            else -> 2
        }
    }

    /*
    ------------------------------------------------------------
    SECTION 1300
    CAPTURE STEP SIZE
    ------------------------------------------------------------
    PURPOSE
    Frequency step between the fixed number of capture points.
    ------------------------------------------------------------
    */
    fun sweepStepMHz(
        startMHz: Double,
        endMHz: Double,
        pointCount: Int = CALIBRATION_POINT_COUNT
    ): Double {
        return if (pointCount > 1) {
            (endMHz - startMHz) / (pointCount - 1)
        } else {
            0.0
        }
    }

    /*
    ------------------------------------------------------------
    SECTION 1400
    SESSION BUILD FOR A CAPTURED STEP
    ------------------------------------------------------------
    PURPOSE
    Stamps the session with the captured step + capture truth. The clock
    is injected so this stays deterministic/testable.
    ------------------------------------------------------------
    */
    fun buildCapturedStepSession(
        existingSession: CalibrationSession,
        currentStep: CalibrationStep,
        selectedHardwareName: String,
        protocolFamily: String?,
        instrumentIdentityText: String?,
        captureTimeMs: Long
    ): CalibrationSession {
        val timeLabel = SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss",
            Locale.getDefault()
        ).format(Date(captureTimeMs))

        val baseSession = existingSession.copy(
            hardwareDisplayName = selectedHardwareName,
            timestampLabel = "Captured $timeLabel",
            capturedAtEpochMs = captureTimeMs,
            captureSource = CalibrationCaptureSource.WIZARD,
            capturedProtocolFamily = protocolFamily,
            capturedInstrumentIdentityText = instrumentIdentityText
        )

        return when (currentStep) {
            CalibrationStep.OPEN -> baseSession.copy(openCaptured = true)
            CalibrationStep.SHORT -> baseSession.copy(shortCaptured = true)
            CalibrationStep.LOAD -> baseSession.copy(loadCaptured = true)
        }
    }

    /*
    ------------------------------------------------------------
    SECTION 1500
    APPLY A CAPTURED STANDARD (state machine)
    ------------------------------------------------------------
    PURPOSE
    Fold a freshly captured standard sweep into the session: accumulate
    the standards, build the updated session, compute the OSL correction
    once OPEN/SHORT/LOAD are all present, and derive the next step +
    completion. Pure — the screen performs the resulting side effects.
    ------------------------------------------------------------
    */
    fun applyCapturedStandard(
        currentSession: CalibrationSession,
        capturedStandards: Map<CalibrationStep, SweepResult>,
        currentStep: CalibrationStep,
        capturedSweep: SweepResult,
        selectedHardwareName: String,
        protocolFamily: String?,
        instrumentIdentityText: String?,
        captureTimeMs: Long
    ): CalibrationCaptureResult {
        val newCapturedStandards = capturedStandards + (currentStep to capturedSweep)

        var updatedSession = buildCapturedStepSession(
            existingSession = currentSession,
            currentStep = currentStep,
            selectedHardwareName = selectedHardwareName,
            protocolFamily = protocolFamily,
            instrumentIdentityText = instrumentIdentityText,
            captureTimeMs = captureTimeMs
        )

        // Once all three standards are captured, compute the real per-frequency
        // error terms and attach them to the session.
        val openSweep = newCapturedStandards[CalibrationStep.OPEN]
        val shortSweep = newCapturedStandards[CalibrationStep.SHORT]
        val loadSweep = newCapturedStandards[CalibrationStep.LOAD]
        if (openSweep != null && shortSweep != null && loadSweep != null) {
            updatedSession = updatedSession.copy(
                correction = OslCalibrationEngine.computeCoefficients(
                    open = openSweep,
                    short = shortSweep,
                    load = loadSweep
                )
            )
        }

        val nextStepIndex =
            if (updatedSession.loadCaptured) {
                steps.lastIndex
            } else {
                findFirstIncompleteStepIndex(updatedSession)
            }

        val isComplete =
            updatedSession.openCaptured &&
                updatedSession.shortCaptured &&
                updatedSession.loadCaptured

        return CalibrationCaptureResult(
            updatedSession = updatedSession,
            updatedCapturedStandards = newCapturedStandards,
            nextStepIndex = nextStepIndex,
            isComplete = isComplete
        )
    }
}
