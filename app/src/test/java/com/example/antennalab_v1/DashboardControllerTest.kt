package com.example.antennalab_v1

import com.example.antennalab_v1.features.app.DashboardController
import com.example.antennalab_v1.features.app.DashboardController.DashboardAction
import com.example.antennalab_v1.model.ProjectCalibrationData
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.ProjectListItem
import com.example.antennalab_v1.model.ProjectSweepHistoryEntry
import com.example.antennalab_v1.model.testing.CalibrationReadiness
import com.example.antennalab_v1.model.testing.CalibrationSession
import com.example.antennalab_v1.model.testing.InstrumentDataSourceKind
import com.example.antennalab_v1.model.testing.MeasurementTrustLevel
import com.example.antennalab_v1.ui.components.AppStatusLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure coverage for the dashboard's decision/derivation logic. No Compose, no
 * Context, no disk — the shell does the reads; this proves what it renders.
 */
class DashboardControllerTest {

    private fun item(id: String, lastEdited: Long) = ProjectListItem(
        projectId = id,
        name = "Project $id",
        antennaType = "DIPOLE",
        targetFrequencyHz = 14_200_000L,
        lastEditedEpochMillis = lastEdited
    )

    private fun calSession(open: Boolean, short: Boolean, load: Boolean) = CalibrationSession(
        hardwareDisplayName = "LiteVNA64 v0.3.3",
        startFrequencyMHz = 14.0,
        endFrequencyMHz = 14.4,
        openCaptured = open,
        shortCaptured = short,
        loadCaptured = load
    )

    // ---- recent projects: newest first, capped -------------------------

    @Test
    fun recentProjects_sortsNewestFirstAndCaps() {
        val items = listOf(
            item("a", 100), item("b", 500), item("c", 300),
            item("d", 900), item("e", 200), item("f", 700)
        )
        val recent = DashboardController.recentProjects(items, limit = 4)

        assertEquals(listOf("d", "f", "b", "c"), recent.map { it.projectId })
    }

    @Test
    fun recentProjects_limitZeroOrOverSizeIsSafe() {
        val items = listOf(item("a", 1), item("b", 2))
        assertTrue(DashboardController.recentProjects(items, limit = 0).isEmpty())
        assertEquals(2, DashboardController.recentProjects(items, limit = 10).size)
    }

    // ---- cheap card ----------------------------------------------------

    @Test
    fun buildProjectCard_fromIndexItem() {
        val card = DashboardController.buildProjectCard(item("x", 1_000))
        assertEquals("x", card.projectId)
        assertEquals("Project x", card.name)
        assertEquals("DIPOLE", card.antennaTypeLabel)
        assertTrue(card.targetFrequencyText.endsWith("MHz"))
        assertTrue(card.targetFrequencyText.contains("14.2"))
    }

    // ---- badge derivation (the async half) -----------------------------

    @Test
    fun buildProjectBadge_nullProject_returnsNull_theLoadFailureCase() {
        // A failed/corrupt load surfaces as null → no badges, no crash, no blank.
        assertNull(DashboardController.buildProjectBadgeOrNull(null))
    }

    @Test
    fun buildProjectBadge_noCalibrationNoHistory() {
        val badge = DashboardController.buildProjectBadgeOrNull(ProjectData())
        assertEquals(AppStatusLevel.NEUTRAL, badge!!.calLevel)
        assertEquals("No calibration", badge.calLabel)
        assertNull(badge.lastMinSwrText)
    }

    @Test
    fun buildProjectBadge_completeCalibrationAndLastSwr() {
        val project = ProjectData(
            calibrationData = ProjectCalibrationData(
                storedCalibrationSession = calSession(open = true, short = true, load = true)
            ),
            sweepHistory = listOf(
                ProjectSweepHistoryEntry(recordedAtEpochMs = 10L, bestSwr = 2.0),
                ProjectSweepHistoryEntry(recordedAtEpochMs = 20L, bestSwr = 1.5)
            )
        )
        val badge = DashboardController.buildProjectBadgeOrNull(project)!!
        assertEquals(AppStatusLevel.POSITIVE, badge.calLevel)
        assertEquals("Calibrated", badge.calLabel)
        // Latest entry (recordedAtEpochMs 20) wins.
        assertEquals("Last SWR 1.500", badge.lastMinSwrText)
    }

    @Test
    fun buildProjectBadge_partialCalibration() {
        val project = ProjectData(
            calibrationData = ProjectCalibrationData(
                storedCalibrationSession = calSession(open = true, short = false, load = false)
            )
        )
        val badge = DashboardController.buildProjectBadgeOrNull(project)!!
        assertEquals(AppStatusLevel.CAUTION, badge.calLevel)
        assertEquals("Calibration partial", badge.calLabel)
    }

    // ---- status chips: every enum value --------------------------------

    @Test
    fun statusChips_liveValidTrusted() {
        val chips = DashboardController.buildStatusChips(
            InstrumentDataSourceKind.REAL_INSTRUMENT,
            CalibrationReadiness.VALID,
            MeasurementTrustLevel.TRUSTED
        )
        assertEquals(DashboardController.DashboardStatusChip("Live", AppStatusLevel.POSITIVE), chips.dataSource)
        assertEquals("App calibration · Valid", chips.calibration.label)
        assertEquals(AppStatusLevel.POSITIVE, chips.calibration.level)
        assertEquals(AppStatusLevel.POSITIVE, chips.trust.level)
    }

    @Test
    fun statusChips_simulatedStaleDegraded() {
        val chips = DashboardController.buildStatusChips(
            InstrumentDataSourceKind.SIMULATED,
            CalibrationReadiness.STALE,
            MeasurementTrustLevel.DEGRADED
        )
        assertEquals("Simulated", chips.dataSource.label)
        assertEquals(AppStatusLevel.NEUTRAL, chips.dataSource.level)
        assertEquals("App calibration · Stale", chips.calibration.label)
        assertEquals(AppStatusLevel.CAUTION, chips.calibration.level)
        assertEquals(AppStatusLevel.CAUTION, chips.trust.level)
    }

    @Test
    fun statusChips_noInstrumentInvalidUnknown_nullSafe() {
        val chips = DashboardController.buildStatusChips(null, CalibrationReadiness.INVALID, null)
        assertEquals("No instrument", chips.dataSource.label)
        assertEquals(AppStatusLevel.NEGATIVE, chips.calibration.level)
        assertEquals("App calibration · Invalid", chips.calibration.label)
        assertEquals("Unknown", chips.trust.label)
        assertEquals(AppStatusLevel.NEUTRAL, chips.trust.level)
    }

    @Test
    fun statusChips_notStartedAndInProgressAndPartial() {
        val notStarted = DashboardController.buildStatusChips(
            InstrumentDataSourceKind.NONE, CalibrationReadiness.NOT_STARTED, MeasurementTrustLevel.SIMULATED
        )
        assertEquals("App calibration · Not started", notStarted.calibration.label)
        assertEquals(AppStatusLevel.NEUTRAL, notStarted.calibration.level)
        assertEquals(AppStatusLevel.NEUTRAL, notStarted.trust.level)

        val inProgress = DashboardController.buildStatusChips(
            InstrumentDataSourceKind.REAL_INSTRUMENT, CalibrationReadiness.IN_PROGRESS, MeasurementTrustLevel.PARTIAL
        )
        assertEquals(AppStatusLevel.CAUTION, inProgress.calibration.level)
        assertEquals("App calibration · In progress", inProgress.calibration.label)
        assertEquals(AppStatusLevel.CAUTION, inProgress.trust.level)
    }

    // ---- quick actions -------------------------------------------------

    @Test
    fun quickActions_threeDistinctInOrder_measureIsPrimary() {
        val actions = DashboardController.quickActions()
        assertEquals(
            listOf(DashboardAction.MEASURE_NOW, DashboardAction.NEW_PROJECT, DashboardAction.IDENTIFY_ANTENNA),
            actions.map { it.action }
        )
        assertEquals(listOf("Measure now", "New project", "Identify antenna"), actions.map { it.label })
        assertTrue("Measure now is the accent/primary action", actions.first().isPrimary)
        assertTrue("the other two are standard", actions.drop(1).none { it.isPrimary })
    }
}
