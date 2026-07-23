package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.UsbSessionManager
import com.example.antennalab_v1.model.AntennaClassification
import com.example.antennalab_v1.model.AvailablePartsProfile
import com.example.antennalab_v1.model.BuildCostProfile
import com.example.antennalab_v1.model.DesignInput
import com.example.antennalab_v1.model.DiscoverySnapshot
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.ProjectMeta
import com.example.antennalab_v1.model.ProjectStatus
import com.example.antennalab_v1.model.ProjectSweepHistoryEntry
import com.example.antennalab_v1.model.ProjectSweepHistoryMode
import com.example.antennalab_v1.model.TestHardwareProfile
import com.example.antennalab_v1.project.ProjectWorkspaceController
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

/**
 * Behavior coverage for the ProjectPageScreen workspace logic extracted into the
 * pure [ProjectWorkspaceController]: sweep-return merge, calibration-session
 * building (against the real shared UsbSessionManager truth), workflow guidance
 * derivations, and value/enum formatting. Real ProjectData model, no mocking.
 *
 * UsbSessionManager is a process-wide singleton, so calibration state is reset
 * before each test.
 */
@RunWith(RobolectricTestRunner::class)
class ProjectWorkspaceControllerTest {

    @Before
    fun resetSessionManager() {
        UsbSessionManager.clearCalibrationState()
        UsbSessionManager.clearSelectedHardwareConfig()
    }

    private fun completeSession() = CalibrationSession(
        hardwareDisplayName = "LiteVNA64 v0.3.3",
        startFrequencyMHz = 1.0,
        endFrequencyMHz = 30.0,
        openCaptured = true,
        shortCaptured = true,
        loadCaptured = true
    )

    // ------------------------------------------------------------------
    // Sweep-return merge
    // ------------------------------------------------------------------

    @Test
    fun mergeProjectReturnFromSweep_adoptsSweepFieldsButKeepsLocalEdits() {
        val local = ProjectData(
            meta = ProjectMeta(projectName = "Local"),
            designInput = DesignInput(targetFrequencyMHz = 14.0),
            antennaClassification = AntennaClassification.NOT_YET_CLASSIFIED,
            sweepHistory = emptyList(),
            discoverySnapshot = null
        )
        val storedSnapshot = DiscoverySnapshot(
            capturedAtEpochMs = 7L,
            antennaClassificationGuess = AntennaClassification.YAGI,
            hardwareName = "LiteVNA64 v0.3.3",
            sweepStartMHz = 1.0,
            sweepEndMHz = 30.0,
            bestFrequencyMHz = 14.1,
            bestSwr = 1.2,
            returnLossDb = -20.0,
            summaryLabel = "s",
            operatorNotes = "n"
        )
        val stored = ProjectData(
            meta = ProjectMeta(projectName = "Stored"),
            designInput = DesignInput(targetFrequencyMHz = 99.0),
            antennaClassification = AntennaClassification.YAGI,
            sweepHistory = listOf(ProjectSweepHistoryEntry(bestSwr = 1.3)),
            discoverySnapshot = storedSnapshot
        )

        val merged = ProjectWorkspaceController.mergeProjectReturnFromSweep(local, stored)

        // Sweep-owned fields come from the stored project.
        assertEquals(AntennaClassification.YAGI, merged.antennaClassification)
        assertEquals(stored.sweepHistory, merged.sweepHistory)
        assertEquals(storedSnapshot, merged.discoverySnapshot)
        assertEquals(stored.testData, merged.testData)

        // Local edits are preserved.
        assertEquals("Local", merged.meta.projectName)
        assertEquals(14.0, merged.designInput.targetFrequencyMHz, 0.0)
    }

    // ------------------------------------------------------------------
    // Calibration-session building
    // ------------------------------------------------------------------

    @Test
    fun buildFreshCalibrationSession_isUncapturedAndSizedToHardwareRange() {
        val project = ProjectData(testHardwareProfile = TestHardwareProfile.LITEVNA64_V0_3_3)

        val session = ProjectWorkspaceController.buildFreshCalibrationSession(project)

        val expectedStart = project.hardwareCapabilityProfile.minFrequencyHz / 1_000_000.0
        val expectedEnd = project.hardwareCapabilityProfile.maxFrequencyHz / 1_000_000.0
        assertEquals(expectedStart, session.startFrequencyMHz, 0.0)
        assertEquals(expectedEnd, session.endFrequencyMHz, 0.0)

        assertFalse(session.hasAnyCapturedStep())
        assertEquals("Not captured yet", session.timestampLabel)
        assertNull(session.capturedSessionKey)
        assertTrue(session.hardwareDisplayName.isNotBlank())
    }

    @Test
    fun buildWizardCalibrationSession_withNoUsableCalibration_startsFresh() {
        // Cleared state -> NOT_STARTED, no shared session.
        val session = ProjectWorkspaceController.buildWizardCalibrationSession(ProjectData())

        assertNull(session.capturedSessionKey)
        assertEquals("Not captured yet", session.timestampLabel)
        assertFalse(session.hasAnyCapturedStep())
    }

    @Test
    fun buildWizardCalibrationSession_reusesValidSharedCalibration() {
        UsbSessionManager.registerSimulatedCalibrationSession(completeSession())

        val session = ProjectWorkspaceController.buildWizardCalibrationSession(ProjectData())

        // Reuses the shared (simulated) calibration rather than a fresh one.
        assertEquals("SIMULATED_CAL_SESSION", session.capturedSessionKey)
        assertTrue(session.isFullyCaptured())
    }

    @Test
    fun buildWizardCalibrationSession_reusesInProgressSharedCalibration() {
        UsbSessionManager.registerSimulatedCalibrationSession(
            completeSession().copy(shortCaptured = false, loadCaptured = false)
        )

        val session = ProjectWorkspaceController.buildWizardCalibrationSession(ProjectData())

        assertEquals("SIMULATED_CAL_SESSION", session.capturedSessionKey)
        assertTrue(session.hasAnyCapturedStep())
        assertFalse(session.isFullyCaptured())
    }

    @Test
    fun buildWizardCalibrationSession_ignoresInvalidCalibrationAndStartsFresh() {
        // A real capture with no live session registers as INVALID (session set,
        // but readiness is neither VALID nor IN_PROGRESS).
        UsbSessionManager.registerCalibrationSession(completeSession())

        val session = ProjectWorkspaceController.buildWizardCalibrationSession(ProjectData())

        assertNull(session.capturedSessionKey)
        assertEquals("Not captured yet", session.timestampLabel)
    }

    // ------------------------------------------------------------------
    // Workflow guidance
    // ------------------------------------------------------------------

    @Test
    fun buildNextAction_coversEachBranch() {
        val noFrequency = ProjectData(designInput = DesignInput(targetFrequencyMHz = 0.0))
        assertEquals(
            "Set or confirm the target frequency before continuing.",
            ProjectWorkspaceController.buildNextAction(noFrequency)
        )

        val draft = ProjectData(
            meta = ProjectMeta(projectStatus = ProjectStatus.DRAFT),
            designInput = DesignInput(targetFrequencyMHz = 14.2)
        )
        assertTrue(
            ProjectWorkspaceController.buildNextAction(draft).startsWith("Review the design workspace")
        )

        val ready = ProjectData(
            meta = ProjectMeta(projectStatus = ProjectStatus.READY_TO_BUILD),
            designInput = DesignInput(targetFrequencyMHz = 14.2)
        )
        assertTrue(
            ProjectWorkspaceController.buildNextAction(ready).startsWith("Confirm hardware selection")
        )
    }

    @Test
    fun buildReadinessSummary_includesBudgetPartsAndHardware() {
        val project = ProjectData(
            buildCostProfile = BuildCostProfile.PREMIUM,
            availablePartsProfile = AvailablePartsProfile.MINIMAL,
            testHardwareProfile = TestHardwareProfile.LITEVNA64_V0_3_3
        )

        val summary = ProjectWorkspaceController.buildReadinessSummary(project)

        assertTrue(summary.contains("Budget: Premium"))
        assertTrue(summary.contains("Parts: Minimal"))
        assertTrue(summary.contains("LiteVNA64 v0.3.3"))
    }

    @Test
    fun buildUserViewSummary_variesByPartsProfile() {
        assertTrue(
            ProjectWorkspaceController.buildUserViewSummary(
                ProjectData(availablePartsProfile = AvailablePartsProfile.MINIMAL)
            ).startsWith("Good for first-time builders")
        )
        assertTrue(
            ProjectWorkspaceController.buildUserViewSummary(
                ProjectData(availablePartsProfile = AvailablePartsProfile.WELL_STOCKED_WORKSHOP)
            ).startsWith("Strong engineering workflow")
        )
    }

    // ------------------------------------------------------------------
    // Formatting
    // ------------------------------------------------------------------

    @Test
    fun formatters_mapEnumsToDisplayText() {
        assertEquals("Budget", ProjectWorkspaceController.formatBudget(BuildCostProfile.BUDGET))
        assertEquals(
            "Some Existing Parts",
            ProjectWorkspaceController.formatParts(AvailablePartsProfile.SOME_EXISTING_PARTS)
        )
        assertEquals("NanoVNA-H4", ProjectWorkspaceController.formatHardware(TestHardwareProfile.NANOVNA_H4))
        assertEquals(
            "Ground Plane",
            ProjectWorkspaceController.formatClassification(AntennaClassification.GROUND_PLANE)
        )
        assertEquals(
            "Discovery Applied",
            ProjectWorkspaceController.formatSweepHistoryMode(ProjectSweepHistoryMode.DISCOVERY_APPLIED)
        )
    }

    @Test
    fun antennaClassificationChoices_coversEveryClassificationValue() {
        assertEquals(
            AntennaClassification.values().toSet(),
            ProjectWorkspaceController.antennaClassificationChoices().toSet()
        )
    }

    @Test
    fun formatTimestamp_returnsUnknownForNonPositiveEpoch() {
        assertEquals("Unknown", ProjectWorkspaceController.formatTimestamp(0L))
        assertEquals("Unknown", ProjectWorkspaceController.formatTimestamp(-5L))
        // A real epoch formats to something other than the "Unknown" sentinel.
        assertNotNull(ProjectWorkspaceController.formatTimestamp(1_700_000_000_000L))
        assertFalse(ProjectWorkspaceController.formatTimestamp(1_700_000_000_000L) == "Unknown")
    }

    @Test
    fun formatMillimetres_switchesToMetresAboveOneThousand() {
        assertEquals("250.0 mm", ProjectWorkspaceController.formatMillimetres(250.0))
        assertTrue(ProjectWorkspaceController.formatMillimetres(1500.0).contains("(1.500 m)"))
    }
}
