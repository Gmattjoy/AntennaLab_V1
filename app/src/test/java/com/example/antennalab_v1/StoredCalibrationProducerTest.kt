package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.StoredCalibrationProducer
import com.example.antennalab_v1.domain.testing.UsbSessionManager
import com.example.antennalab_v1.model.CalibrationRestorePolicy
import com.example.antennalab_v1.model.DesignInput
import com.example.antennalab_v1.model.ProjectCalibrationData
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.TestHardwareProfile
import com.example.antennalab_v1.model.testing.CalibrationCaptureSource
import com.example.antennalab_v1.model.testing.CalibrationReadiness
import com.example.antennalab_v1.model.testing.CalibrationSession
import com.example.antennalab_v1.model.testing.InstrumentCalibrationState
import com.example.antennalab_v1.model.testing.OslCalibrationCoefficients
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Coverage for the §10c.6 calibration PRODUCER [StoredCalibrationProducer] — the
 * previously-missing writer of project calibrationData.storedCalibrationSession.
 *
 * The buildStoredCalibrationData / isPersistable core is pure. captureIntoProject
 * reads the UsbSessionManager singleton (to resolve the canonical hardware name),
 * so that state is reset before each test. Plain JVM.
 */
class StoredCalibrationProducerTest {

    @Before
    fun resetSessionManager() {
        UsbSessionManager.clearCalibrationState()
        UsbSessionManager.clearSelectedHardwareConfig()
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private fun usableCorrection() = OslCalibrationCoefficients(
        frequencyHz = listOf(14_200_000L),
        directivityRe = listOf(0.0),
        directivityIm = listOf(0.0),
        sourceMatchRe = listOf(0.0),
        sourceMatchIm = listOf(0.0),
        reflectionTrackingRe = listOf(1.0),
        reflectionTrackingIm = listOf(0.0)
    )

    /** Fully captured session stored under the DRIVER LABEL, so producer name-normalisation is visible. */
    private fun fullSession(
        name: String = "LiteVNA64 HW 64-0.3.3 FW v1.4.06",
        correction: OslCalibrationCoefficients? = usableCorrection()
    ) = CalibrationSession(
        hardwareDisplayName = name,
        startFrequencyMHz = 14.0,
        endFrequencyMHz = 14.4,
        openCaptured = true,
        shortCaptured = true,
        loadCaptured = true,
        capturedSessionKey = "LIVE_SESSION_KEY_123",
        correction = correction
    )

    private fun state(
        readiness: CalibrationReadiness,
        session: CalibrationSession? = fullSession()
    ) = InstrumentCalibrationState(
        readiness = readiness,
        calibrationSession = session,
        statusSummary = "status-summary-under-test"
    )

    private fun liteProject(
        stored: CalibrationSession? = null
    ) = ProjectData(
        designInput = DesignInput(targetFrequencyMHz = 14.2),
        testHardwareProfile = TestHardwareProfile.LITEVNA64_V0_3_3,
        calibrationData = ProjectCalibrationData(storedCalibrationSession = stored)
    )

    // ------------------------------------------------------------------
    // isPersistable — the VALID-only gate
    // ------------------------------------------------------------------

    @Test
    fun isPersistable_trueOnlyForValidFullyCapturedWithCorrection() {
        assertTrue(StoredCalibrationProducer.isPersistable(state(CalibrationReadiness.VALID)))
        assertFalse(StoredCalibrationProducer.isPersistable(state(CalibrationReadiness.STALE)))
        assertFalse(StoredCalibrationProducer.isPersistable(state(CalibrationReadiness.INVALID)))
        assertFalse(StoredCalibrationProducer.isPersistable(state(CalibrationReadiness.IN_PROGRESS)))
        assertFalse(StoredCalibrationProducer.isPersistable(state(CalibrationReadiness.NOT_STARTED)))
        // VALID readiness but no usable correction data → still not persistable.
        assertFalse(
            StoredCalibrationProducer.isPersistable(
                state(CalibrationReadiness.VALID, fullSession(correction = null))
            )
        )
        // No session at all.
        assertFalse(
            StoredCalibrationProducer.isPersistable(
                InstrumentCalibrationState(readiness = CalibrationReadiness.VALID, calibrationSession = null)
            )
        )
    }

    // ------------------------------------------------------------------
    // buildStoredCalibrationData — pure fields
    // ------------------------------------------------------------------

    @Test
    fun buildStoredCalibrationData_valid_writesCanonicalNameAndNeutralisesVolatileFields() {
        val existing = ProjectCalibrationData(restorePolicy = CalibrationRestorePolicy.DO_NOT_RESTORE)

        val data = StoredCalibrationProducer.buildStoredCalibrationData(
            liveCalibration = state(CalibrationReadiness.VALID),
            canonicalHardwareDisplayName = "LiteVNA64 v0.3.3",
            nowEpochMs = 123_456L,
            existing = existing
        )

        assertNotNull(data)
        val stored = data!!.storedCalibrationSession!!
        // Driver label OVERWRITTEN with the canonical capability displayName.
        assertEquals("LiteVNA64 v0.3.3", stored.hardwareDisplayName)
        // Volatile live session key stripped; restore re-binds it.
        assertNull(stored.capturedSessionKey)
        assertEquals(CalibrationCaptureSource.WIZARD, stored.captureSource)
        // Capture data preserved.
        assertTrue(stored.isFullyCaptured())
        assertTrue(stored.hasCorrectionData)
        // Bookkeeping.
        assertEquals(123_456L, data.lastCalibrationSavedEpochMs)
        // Existing policy preserved, not reset.
        assertEquals(CalibrationRestorePolicy.DO_NOT_RESTORE, data.restorePolicy)
    }

    @Test
    fun buildStoredCalibrationData_notValid_returnsNull() {
        for (readiness in listOf(
            CalibrationReadiness.STALE,
            CalibrationReadiness.INVALID,
            CalibrationReadiness.IN_PROGRESS,
            CalibrationReadiness.NOT_STARTED
        )) {
            assertNull(
                "readiness=$readiness must not persist",
                StoredCalibrationProducer.buildStoredCalibrationData(
                    liveCalibration = state(readiness),
                    canonicalHardwareDisplayName = "LiteVNA64 v0.3.3",
                    nowEpochMs = 1L,
                    existing = ProjectCalibrationData()
                )
            )
        }
    }

    // ------------------------------------------------------------------
    // captureIntoProject — shared wrapper
    // ------------------------------------------------------------------

    @Test
    fun captureIntoProject_valid_writesCanonicalLiteVnaNameIntoProject() {
        val project = liteProject()

        val captured = StoredCalibrationProducer.captureIntoProject(
            project = project,
            liveCalibration = state(CalibrationReadiness.VALID),
            nowEpochMs = 999L
        )

        val stored = captured.calibrationData.storedCalibrationSession
        assertNotNull(stored)
        assertEquals("LiteVNA64 v0.3.3", stored!!.hardwareDisplayName)
    }

    /*
    NAMED GATE-NEGATIVE TEST (must not be dropped).
    A STALE or INVALID calibration is one the operator should NOT trust, so the
    producer must refuse to persist it: captureIntoProject leaves the project's
    storedCalibrationSession null (and returns the project unchanged).
    */
    @Test
    fun staleOrInvalidCalibration_isNotPersisted_captureIntoProjectLeavesStoredSessionNull() {
        for (readiness in listOf(CalibrationReadiness.STALE, CalibrationReadiness.INVALID)) {
            val project = liteProject(stored = null)

            val captured = StoredCalibrationProducer.captureIntoProject(
                project = project,
                liveCalibration = state(readiness),
                nowEpochMs = 999L
            )

            assertNull(
                "readiness=$readiness must not be persisted",
                captured.calibrationData.storedCalibrationSession
            )
            // Untouched: same instance back, no surprise mutation.
            assertSame(project, captured)
        }
    }
}
