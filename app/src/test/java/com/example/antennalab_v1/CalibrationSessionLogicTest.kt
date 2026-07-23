package com.example.antennalab_v1

import android.content.Context
import com.example.antennalab_v1.domain.testing.UsbSessionManager
import com.example.antennalab_v1.model.HardwareConnectionState
import com.example.antennalab_v1.model.testing.CalibrationCompletionState
import com.example.antennalab_v1.model.testing.CalibrationReadiness
import com.example.antennalab_v1.model.testing.CalibrationSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Robolectric coverage for the calibration wizard's session logic: how captured
 * OSL calibration sessions are registered into the shared instrument calibration
 * truth ([UsbSessionManager]) and how that truth reacts to the live-session
 * lifecycle. Also covers the [CalibrationSession] readiness/matching helpers the
 * wizard depends on.
 *
 * UsbSessionManager is a process-wide singleton, so each test resets its
 * calibration + hardware-config state first.
 */
@RunWith(RobolectricTestRunner::class)
class CalibrationSessionLogicTest {

    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    @Before
    fun resetSessionManager() {
        UsbSessionManager.clearCalibrationState()
        UsbSessionManager.clearSelectedHardwareConfig()
    }

    private fun session(
        open: Boolean = false,
        short: Boolean = false,
        load: Boolean = false,
        protocolFamily: String? = null,
        instrumentIdentity: String? = null
    ) = CalibrationSession(
        hardwareDisplayName = "LiteVNA64 v0.3.3",
        startFrequencyMHz = 1.0,
        endFrequencyMHz = 30.0,
        openCaptured = open,
        shortCaptured = short,
        loadCaptured = load,
        capturedProtocolFamily = protocolFamily,
        capturedInstrumentIdentityText = instrumentIdentity
    )

    // ------------------------------------------------------------------
    // Real (hardware) capture registration
    // ------------------------------------------------------------------

    @Test
    fun registerCalibrationSession_withNoLiveSession_isInvalid() {
        // No USB session was ever opened, so there is no active session key.
        UsbSessionManager.registerCalibrationSession(session(open = true, short = true, load = true))

        val state = UsbSessionManager.getLatestInstrumentCalibrationState()
        assertEquals(CalibrationReadiness.INVALID, state.readiness)
        assertTrue(state.trustDowngraded)
        assertTrue(state.sweepAllowed) // flag, don't reject
        assertNotNull(state.calibrationSession)
        assertNull(state.sessionKeyAtCapture)
    }

    // ------------------------------------------------------------------
    // Simulated (debug, no-hardware) capture registration
    // ------------------------------------------------------------------

    @Test
    fun registerSimulatedCalibrationSession_complete_isValidAndTrusted() {
        UsbSessionManager.registerSimulatedCalibrationSession(
            session(open = true, short = true, load = true)
        )

        val state = UsbSessionManager.getLatestInstrumentCalibrationState()
        assertEquals(CalibrationReadiness.VALID, state.readiness)
        assertFalse(state.trustDowngraded)
        assertEquals("SIMULATED_CAL_SESSION", state.sessionKeyAtCapture)
        assertEquals("SIMULATED_CAL_SESSION", state.activeSessionKey)
    }

    @Test
    fun registerSimulatedCalibrationSession_partial_isInProgress() {
        UsbSessionManager.registerSimulatedCalibrationSession(session(open = true))

        val state = UsbSessionManager.getLatestInstrumentCalibrationState()
        assertEquals(CalibrationReadiness.IN_PROGRESS, state.readiness)
        assertTrue(state.trustDowngraded)
    }

    @Test
    fun registerSimulatedCalibrationSession_noCaptures_isNotStarted() {
        UsbSessionManager.registerSimulatedCalibrationSession(session())

        val state = UsbSessionManager.getLatestInstrumentCalibrationState()
        assertEquals(CalibrationReadiness.NOT_STARTED, state.readiness)
        assertTrue(state.trustDowngraded)
    }

    // ------------------------------------------------------------------
    // Lifecycle: clear + session-driven staleness
    // ------------------------------------------------------------------

    @Test
    fun clearCalibrationState_resetsToDefaultNotStarted() {
        UsbSessionManager.registerSimulatedCalibrationSession(
            session(open = true, short = true, load = true)
        )
        assertEquals(
            CalibrationReadiness.VALID,
            UsbSessionManager.getLatestInstrumentCalibrationState().readiness
        )

        UsbSessionManager.clearCalibrationState()

        val state = UsbSessionManager.getLatestInstrumentCalibrationState()
        assertEquals(CalibrationReadiness.NOT_STARTED, state.readiness)
        assertNull(state.calibrationSession)
    }

    @Test
    fun refreshCurrentSessionState_withoutUsbHost_reportsErrorAndMarksCalibrationStale() {
        // Start from a valid (simulated) calibration.
        UsbSessionManager.registerSimulatedCalibrationSession(
            session(open = true, short = true, load = true)
        )

        // Robolectric's default device reports no USB host feature, so the live
        // session cannot be confirmed: the connection goes to ERROR and, because
        // no session is open, the prior calibration is downgraded to STALE.
        val refreshed = UsbSessionManager.refreshCurrentSessionState(
            context = context,
            selectedHardwareName = "LiteVNA64 v0.3.3"
        )

        assertEquals(HardwareConnectionState.ERROR, refreshed.connectionInfo.state)
        assertEquals("USB host unsupported", refreshed.connectionInfo.deviceName)

        assertEquals(
            CalibrationReadiness.STALE,
            UsbSessionManager.getLatestInstrumentCalibrationState().readiness
        )
    }

    // ------------------------------------------------------------------
    // CalibrationSession readiness/matching helpers the wizard relies on
    // ------------------------------------------------------------------

    @Test
    fun completionState_reflectsCapturedStandards() {
        assertEquals(CalibrationCompletionState.NOT_STARTED, session().completionState)
        assertEquals(CalibrationCompletionState.PARTIAL, session(open = true).completionState)
        assertEquals(
            CalibrationCompletionState.COMPLETE,
            session(open = true, short = true, load = true).completionState
        )
        assertEquals(2, session(open = true, load = true).completedStepCount)
    }

    @Test
    fun matchesProtocolFamily_isBlankTolerantAndNormalizes() {
        // Blank on either side is treated as compatible.
        assertTrue(session(protocolFamily = null).matchesProtocolFamily("LiteVNA"))
        assertTrue(session(protocolFamily = "LiteVNA").matchesProtocolFamily(null))

        // Case / spacing / separators are normalized away.
        assertTrue(session(protocolFamily = "Lite-VNA").matchesProtocolFamily("lite vna"))
        assertFalse(session(protocolFamily = "LiteVNA").matchesProtocolFamily("NanoVNA"))
    }

    @Test
    fun isCompatibleWithRequestedRange_requiresFullCaptureHardwareAndCoverage() {
        val complete = session(open = true, short = true, load = true)

        assertTrue(
            complete.isCompatibleWithRequestedRange(
                selectedHardwareName = "LiteVNA64 v0.3.3",
                requestedStartMHz = 5.0,
                requestedEndMHz = 20.0
            )
        )
        // Requested range escapes the captured coverage.
        assertFalse(
            complete.isCompatibleWithRequestedRange(
                selectedHardwareName = "LiteVNA64 v0.3.3",
                requestedStartMHz = 0.5,
                requestedEndMHz = 20.0
            )
        )
        // Partial capture is never compatible.
        assertFalse(
            session(open = true).isCompatibleWithRequestedRange(
                selectedHardwareName = "LiteVNA64 v0.3.3",
                requestedStartMHz = 5.0,
                requestedEndMHz = 20.0
            )
        )
    }
}
