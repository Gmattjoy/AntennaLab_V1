package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.CalibrationSessionFactory
import com.example.antennalab_v1.domain.testing.UsbSessionManager
import com.example.antennalab_v1.model.DesignInput
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.TestHardwareProfile
import com.example.antennalab_v1.model.testing.CalibrationSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Coverage for [CalibrationSessionFactory] — the single source of truth for the
 * fresh/wizard calibration session used by both the Lab (AppRootController) and
 * Project-page (ProjectWorkspaceController) entry points. Verifies the unified
 * target-focused span (target +/- 0.5 MHz, clamped to hardware limits) and the
 * reuse-vs-fresh decision, against the real UsbSessionManager singleton.
 *
 * UsbSessionManager is process-wide, so its state is reset before each test.
 */
class CalibrationSessionFactoryTest {

    @Before
    fun resetSessionManager() {
        UsbSessionManager.clearCalibrationState()
        UsbSessionManager.clearSelectedHardwareConfig()
    }

    private fun project(
        targetMHz: Double,
        hardware: TestHardwareProfile = TestHardwareProfile.LITEVNA64_V0_3_3
    ) = ProjectData(
        designInput = DesignInput(targetFrequencyMHz = targetMHz),
        testHardwareProfile = hardware
    )

    private fun completeSession() = CalibrationSession(
        hardwareDisplayName = "LiteVNA64 v0.3.3",
        startFrequencyMHz = 1.0,
        endFrequencyMHz = 30.0,
        openCaptured = true,
        shortCaptured = true,
        loadCaptured = true
    )

    // ------------------------------------------------------------------
    // Fresh session — target-focused span
    // ------------------------------------------------------------------

    @Test
    fun buildFreshSession_spansTargetPlusMinusHalfMHz() {
        val session = CalibrationSessionFactory.buildFreshSession(project(14.2))

        assertEquals(13.7, session.startFrequencyMHz, 1e-9)
        assertEquals(14.7, session.endFrequencyMHz, 1e-9)
        assertFalse(session.hasAnyCapturedStep())
        assertEquals("Not captured yet", session.timestampLabel)
        assertNull(session.capturedSessionKey)
    }

    @Test
    fun buildFreshSession_clampsStartAtHardwareFloor() {
        // LiteVNA floor is 0.1 MHz; target 0.2 would give start -0.3 before clamping.
        val session = CalibrationSessionFactory.buildFreshSession(project(0.2))

        assertEquals(0.1, session.startFrequencyMHz, 1e-9) // clamped to hardware min
        assertEquals(0.7, session.endFrequencyMHz, 1e-9)
    }

    @Test
    fun buildFreshSession_clampsEndAtHardwareCeiling() {
        // LiteVNA ceiling is 6300 MHz.
        val session = CalibrationSessionFactory.buildFreshSession(project(6300.0))

        assertEquals(6299.5, session.startFrequencyMHz, 1e-9)
        assertEquals(6300.0, session.endFrequencyMHz, 1e-9) // clamped to hardware max
    }

    @Test
    fun buildFreshSession_hardwareNameFallsBackToCapabilityDisplayNameWhenNoSession() {
        val session = CalibrationSessionFactory.buildFreshSession(
            project(14.2, hardware = TestHardwareProfile.NANOVNA_H4)
        )
        assertEquals("NanoVNA-H4", session.hardwareDisplayName)
    }

    // ------------------------------------------------------------------
    // Wizard session — reuse vs fresh
    // ------------------------------------------------------------------

    @Test
    fun buildWizardSession_startsFreshWhenNoSharedCalibration() {
        val session = CalibrationSessionFactory.buildWizardSession(project(14.2))

        assertNull(session.capturedSessionKey)
        assertEquals(13.7, session.startFrequencyMHz, 1e-9)
        assertFalse(session.hasAnyCapturedStep())
    }

    @Test
    fun buildWizardSession_reusesValidSharedCalibration() {
        UsbSessionManager.registerSimulatedCalibrationSession(completeSession())

        val session = CalibrationSessionFactory.buildWizardSession(project(14.2))

        assertEquals("SIMULATED_CAL_SESSION", session.capturedSessionKey)
        assertTrue(session.isFullyCaptured())
    }

    @Test
    fun buildWizardSession_reusesInProgressSharedCalibration() {
        UsbSessionManager.registerSimulatedCalibrationSession(
            completeSession().copy(shortCaptured = false, loadCaptured = false)
        )

        val session = CalibrationSessionFactory.buildWizardSession(project(14.2))

        assertEquals("SIMULATED_CAL_SESSION", session.capturedSessionKey)
        assertTrue(session.hasAnyCapturedStep())
        assertFalse(session.isFullyCaptured())
    }

    @Test
    fun buildWizardSession_startsFreshWhenSharedCalibrationInvalid() {
        // A real capture with no live session registers as INVALID (session set,
        // readiness neither VALID nor IN_PROGRESS) -> must NOT be reused.
        UsbSessionManager.registerCalibrationSession(completeSession())

        val session = CalibrationSessionFactory.buildWizardSession(project(14.2))

        assertNull(session.capturedSessionKey)
        assertEquals("Not captured yet", session.timestampLabel)
    }
}
