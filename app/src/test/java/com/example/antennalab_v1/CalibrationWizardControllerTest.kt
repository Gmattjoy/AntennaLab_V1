package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.DebugOslCalibrationSimulator
import com.example.antennalab_v1.features.testing.CalibrationWizardController
import com.example.antennalab_v1.model.testing.CalibrationCaptureSource
import com.example.antennalab_v1.model.testing.CalibrationSession
import com.example.antennalab_v1.model.testing.CalibrationStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavior coverage for the OSL capture state machine extracted out of
 * CalibrationWizardScreen into the pure [CalibrationWizardController]:
 * step-state derivation, capture step size, per-step session build, and the
 * applyCapturedStandard fold (accumulate → compute coefficients once all three
 * standards are captured → derive next step + completion). Uses real
 * DebugOslCalibrationSimulator sweeps and OslCalibrationEngine — no mocking.
 * Pure logic, so plain JVM.
 */
class CalibrationWizardControllerTest {

    private val captureTimeMs = 1_700_000_000_000L

    private fun freshSession() = CalibrationSession(
        hardwareDisplayName = "LiteVNA64 v0.3.3",
        startFrequencyMHz = 1.0,
        endFrequencyMHz = 30.0
    )

    private fun standard(step: CalibrationStep) =
        DebugOslCalibrationSimulator.simulateStandardCaptureSweep(
            step = step,
            startMHz = 1.0,
            endMHz = 30.0,
            pointCount = CalibrationWizardController.CALIBRATION_POINT_COUNT
        )

    // ------------------------------------------------------------------
    // Step-state derivation
    // ------------------------------------------------------------------

    @Test
    fun findFirstIncompleteStepIndex_walksOpenShortLoad() {
        val base = freshSession()
        assertEquals(0, CalibrationWizardController.findFirstIncompleteStepIndex(base))
        assertEquals(1, CalibrationWizardController.findFirstIncompleteStepIndex(base.copy(openCaptured = true)))
        assertEquals(
            2,
            CalibrationWizardController.findFirstIncompleteStepIndex(base.copy(openCaptured = true, shortCaptured = true))
        )
        assertEquals(
            2,
            CalibrationWizardController.findFirstIncompleteStepIndex(
                base.copy(openCaptured = true, shortCaptured = true, loadCaptured = true)
            )
        )
    }

    // ------------------------------------------------------------------
    // Capture step size
    // ------------------------------------------------------------------

    @Test
    fun sweepStepMHz_dividesSpanAcrossPointsAndGuardsSinglePoint() {
        assertEquals(0.01, CalibrationWizardController.sweepStepMHz(1.0, 2.0, pointCount = 101), 1e-12)
        assertEquals(0.5, CalibrationWizardController.sweepStepMHz(13.7, 14.7, pointCount = 3), 1e-12)
        assertEquals(0.0, CalibrationWizardController.sweepStepMHz(1.0, 30.0, pointCount = 1), 0.0)
    }

    // ------------------------------------------------------------------
    // Per-step session build (deterministic via injected clock)
    // ------------------------------------------------------------------

    @Test
    fun buildCapturedStepSession_setsStepFlagAndCaptureTruth() {
        val open = CalibrationWizardController.buildCapturedStepSession(
            existingSession = freshSession(),
            currentStep = CalibrationStep.OPEN,
            selectedHardwareName = "LiteVNA64 v0.3.3",
            protocolFamily = "LiteVNA",
            instrumentIdentityText = "unit-1",
            captureTimeMs = captureTimeMs
        )

        assertTrue(open.openCaptured)
        assertFalse(open.shortCaptured)
        assertFalse(open.loadCaptured)
        assertEquals(CalibrationCaptureSource.WIZARD, open.captureSource)
        assertEquals(captureTimeMs, open.capturedAtEpochMs)
        assertTrue(open.timestampLabel.startsWith("Captured "))
        assertEquals("LiteVNA64 v0.3.3", open.hardwareDisplayName)
        assertEquals("LiteVNA", open.capturedProtocolFamily)
        assertEquals("unit-1", open.capturedInstrumentIdentityText)

        val short = CalibrationWizardController.buildCapturedStepSession(
            freshSession(), CalibrationStep.SHORT, "hw", null, null, captureTimeMs
        )
        assertTrue(short.shortCaptured)
        assertFalse(short.openCaptured)

        val load = CalibrationWizardController.buildCapturedStepSession(
            freshSession(), CalibrationStep.LOAD, "hw", null, null, captureTimeMs
        )
        assertTrue(load.loadCaptured)
    }

    // ------------------------------------------------------------------
    // Capture state machine: OPEN -> SHORT -> LOAD
    // ------------------------------------------------------------------

    @Test
    fun applyCapturedStandard_threadsAllThreeAndComputesCorrectionOnCompletion() {
        // OPEN
        val r1 = CalibrationWizardController.applyCapturedStandard(
            currentSession = freshSession(),
            capturedStandards = emptyMap(),
            currentStep = CalibrationStep.OPEN,
            capturedSweep = standard(CalibrationStep.OPEN),
            selectedHardwareName = "LiteVNA64 v0.3.3",
            protocolFamily = null,
            instrumentIdentityText = null,
            captureTimeMs = captureTimeMs
        )
        assertTrue(r1.updatedSession.openCaptured)
        assertTrue(r1.updatedCapturedStandards.containsKey(CalibrationStep.OPEN))
        assertEquals(1, r1.nextStepIndex)
        assertFalse(r1.isComplete)
        assertNull(r1.updatedSession.correction)

        // SHORT
        val r2 = CalibrationWizardController.applyCapturedStandard(
            currentSession = r1.updatedSession,
            capturedStandards = r1.updatedCapturedStandards,
            currentStep = CalibrationStep.SHORT,
            capturedSweep = standard(CalibrationStep.SHORT),
            selectedHardwareName = "LiteVNA64 v0.3.3",
            protocolFamily = null,
            instrumentIdentityText = null,
            captureTimeMs = captureTimeMs
        )
        assertTrue(r2.updatedSession.shortCaptured)
        assertEquals(2, r2.nextStepIndex)
        assertFalse(r2.isComplete)
        assertNull(r2.updatedSession.correction)

        // LOAD -> completes and computes the correction
        val r3 = CalibrationWizardController.applyCapturedStandard(
            currentSession = r2.updatedSession,
            capturedStandards = r2.updatedCapturedStandards,
            currentStep = CalibrationStep.LOAD,
            capturedSweep = standard(CalibrationStep.LOAD),
            selectedHardwareName = "LiteVNA64 v0.3.3",
            protocolFamily = null,
            instrumentIdentityText = null,
            captureTimeMs = captureTimeMs
        )
        assertTrue(r3.updatedSession.isFullyCaptured())
        assertTrue(r3.isComplete)
        assertEquals(CalibrationWizardController.steps.lastIndex, r3.nextStepIndex)
        assertNotNull(r3.updatedSession.correction)
        assertTrue(r3.updatedSession.correction!!.isUsable)
        assertEquals(3, r3.updatedCapturedStandards.size)
    }
}
