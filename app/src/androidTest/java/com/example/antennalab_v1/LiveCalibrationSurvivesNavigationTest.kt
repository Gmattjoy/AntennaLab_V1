package com.example.antennalab_v1

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.antennalab_v1.domain.testing.UsbSessionManager
import com.example.antennalab_v1.features.app.AppMenuDestination
import com.example.antennalab_v1.features.app.AppNavigationMenuBridge
import com.example.antennalab_v1.features.app.AppRootScreen
import com.example.antennalab_v1.model.DesignInput
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.ProjectMeta
import com.example.antennalab_v1.model.testing.CalibrationReadiness
import com.example.antennalab_v1.model.testing.CalibrationSession
import com.example.antennalab_v1.storage.ProjectStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TIER 2 GUARD — live calibration must SURVIVE navigation.
 *
 * Before the hard teardown, entering the wizard, RF-test mode, or unknown
 * discovery each called UsbSessionManager.clearCalibrationState() with no
 * guard whatsoever, and opening a project ran a restore policy that CLEARED
 * live calibration in four of its five branches. A valid bench calibration
 * was destroyed by mere navigation.
 *
 * This drives the real AppRootScreen and asserts the live calibration still
 * EXISTS after each transition — see assertCalibrationSurvived for why that
 * is the right bar rather than "still VALID". If someone reintroduces a
 * navigation wipe, this fails.
 */
@RunWith(AndroidJUnit4::class)
class LiveCalibrationSurvivesNavigationTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun readiness() =
        UsbSessionManager.getLatestInstrumentCalibrationState().readiness

    /**
     * "Survived" means the calibration is STILL THERE — session object retained,
     * all three standards still captured. It does NOT mean still fully trusted.
     *
     * Off-bench there is no open USB session, so the staleness detector may
     * legitimately downgrade VALID → STALE (UsbSessionManager
     * refreshCalibrationStateForCurrentSession). That is a `copy(readiness = STALE)`
     * which KEEPS the coefficients — SweepController still applies correction for
     * STALE. The Tier 2 bug was different in kind: clearCalibrationState() replaced
     * the whole state with InstrumentCalibrationState(), leaving readiness
     * NOT_STARTED and calibrationSession null. That is what must never happen on
     * navigation, and it is what this asserts.
     */
    private fun assertCalibrationSurvived(step: String) {
        val state = UsbSessionManager.getLatestInstrumentCalibrationState()
        val session = state.calibrationSession

        assertNotNull("$step: calibration session was WIPED (cleared, not downgraded)", session)
        assertTrue(
            "$step: captured standards were lost",
            session!!.isFullyCaptured()
        )
        assertNotEquals(
            "$step: calibration was reset to NOT_STARTED — this is the navigation wipe",
            CalibrationReadiness.NOT_STARTED,
            state.readiness
        )
    }

    /** A COMPLETE simulated capture — the debug path the calibration wizard's chip uses. */
    private fun simulateValidCalibration() {
        UsbSessionManager.registerSimulatedCalibrationSession(
            CalibrationSession(
                hardwareDisplayName = "LiteVNA64 v0.3.3",
                startFrequencyMHz = 13.7,
                endFrequencyMHz = 14.7,
                openCaptured = true,
                shortCaptured = true,
                loadCaptured = true
            )
        )
    }

    @Before
    fun seedSavedProjectAndCalibration() {
        UsbSessionManager.clearCalibrationState()
        ProjectStorage.saveProject(
            context,
            ProjectData(
                meta = ProjectMeta(projectName = "Tier2 Guard Project"),
                designInput = DesignInput(targetFrequencyMHz = 14.2)
            )
        )
    }

    /** Step 1: the simulated O/S/L capture reaches VALID at all. */
    @Test
    fun step1_simulatedCapture_isValid() {
        simulateValidCalibration()
        assertEquals(CalibrationReadiness.VALID, readiness())
    }

    @Test
    fun step2a_enterWizardMode_calibrationSurvives() {
        compose.setContent { AppRootScreen() }
        compose.waitForIdle()
        simulateValidCalibration()
        assertEquals(CalibrationReadiness.VALID, readiness())

        compose.runOnUiThread { AppNavigationMenuBridge.navigateTo(AppMenuDestination.WIZARD) }
        compose.waitForIdle()

        assertCalibrationSurvived("enter wizard mode")
    }

    @Test
    fun step2b_enterRfTestMode_calibrationSurvives() {
        compose.setContent { AppRootScreen() }
        compose.waitForIdle()
        simulateValidCalibration()
        assertEquals(CalibrationReadiness.VALID, readiness())

        compose.runOnUiThread { AppNavigationMenuBridge.navigateTo(AppMenuDestination.TEST_ANTENNA) }
        compose.waitForIdle()

        assertCalibrationSurvived("enter RF-test mode")
    }

    @Test
    fun step2c_enterUnknownDiscoveryMode_calibrationSurvives() {
        compose.setContent { AppRootScreen() }
        compose.waitForIdle()
        simulateValidCalibration()
        assertEquals(CalibrationReadiness.VALID, readiness())

        // Dashboard quick action → enterUnknownDiscoveryMode()
        compose.onNodeWithText("Identify antenna").performClick()
        compose.waitForIdle()

        assertCalibrationSurvived("enter unknown-discovery mode")
    }

    @Test
    fun step2d_openSavedProject_calibrationSurvives() {
        compose.setContent { AppRootScreen() }
        compose.waitForIdle()
        simulateValidCalibration()
        assertEquals(CalibrationReadiness.VALID, readiness())

        compose.runOnUiThread { AppNavigationMenuBridge.navigateTo(AppMenuDestination.PROJECTS) }
        compose.waitForIdle()

        // Any saved project will do; click the first card's load button.
        compose.onAllNodesWithText("Load Project")[0].performClick()
        compose.waitForIdle()

        assertCalibrationSurvived("open saved project")
    }
}
