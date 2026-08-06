package com.example.antennalab_v1.features.app

import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.ProjectListItem
import com.example.antennalab_v1.ui.components.AppStatusLevel
import java.util.Locale

/*
########################################################################
FILE: DashboardController.kt
PACKAGE: com.example.antennalab_v1.features.app
LAYER: UI / App / Dashboard / Logic Control

Pure (non-Compose, non-Context, no I/O) display logic for the dashboard —
the same UI-free controller pattern as SweepWorkspaceController /
LoadProjectController. The Compose shell (DashboardScreen) performs the
actual reads (UsbSessionManager, ProjectStorage) and calls these to derive
what to show.

Two-stage recent-project rendering so first paint never blocks on disk:
  buildProjectCard(item)        — cheap, from the lightweight index item
  buildProjectBadgeOrNull(full) — richer badge, once the full ProjectData
                                  has loaded off the main thread (null when
                                  the load failed / project is absent, so a
                                  bad project just shows a row without badges)
########################################################################
*/
object DashboardController {

    // ------------------------------------------------------------------
    // Quick actions — three DISTINCT destinations (Test Antenna / design
    // extras live in the ⋮ overflow + Lab, not a redundant button here).
    // ------------------------------------------------------------------

    enum class DashboardAction { MEASURE_NOW, NEW_PROJECT, IDENTIFY_ANTENNA }

    data class DashboardActionSpec(
        val action: DashboardAction,
        val label: String,
        val isPrimary: Boolean
    )

    fun quickActions(): List<DashboardActionSpec> = listOf(
        DashboardActionSpec(DashboardAction.MEASURE_NOW, "Measure now", isPrimary = true),
        DashboardActionSpec(DashboardAction.NEW_PROJECT, "New project", isPrimary = false),
        DashboardActionSpec(DashboardAction.IDENTIFY_ANTENNA, "Identify antenna", isPrimary = false)
    )

    // ------------------------------------------------------------------
    // Device / calibration status chips: moved to the shared, single-source
    // InstrumentStatusPresenter.buildStatusChips (consumed by BOTH the
    // dashboard and Device Connections, so they cannot diverge).
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // Recent projects — cheap cards now, richer badges async.
    // ------------------------------------------------------------------

    data class DashboardProjectCard(
        val projectId: String,
        val name: String,
        val antennaTypeLabel: String,
        val targetFrequencyText: String,
        val lastEditedText: String
    )

    /*
    No calibration chip here. Calibration is live-only, so a saved project
    has none to report — and showing the INSTRUMENT's current calibration
    on a project row would imply the project carries it, the exact
    ambiguity the calibration-honesty rule forbids.
    */
    data class DashboardProjectBadge(
        val lastMinSwrText: String?
    )

    /** Most-recently-edited first, capped. */
    fun recentProjects(
        items: List<ProjectListItem>,
        limit: Int = 4
    ): List<ProjectListItem> =
        items.sortedByDescending { it.lastEditedEpochMillis }
            .take(limit.coerceAtLeast(0))

    /** Cheap card straight from the lightweight index item (no disk). */
    fun buildProjectCard(item: ProjectListItem): DashboardProjectCard =
        DashboardProjectCard(
            projectId = item.projectId,
            name = item.name.ifBlank { "Untitled project" },
            antennaTypeLabel = item.antennaType.ifBlank { "Antenna" },
            targetFrequencyText =
                LoadProjectController.formatTargetFrequencyMHz(item.targetFrequencyHz) + " MHz",
            lastEditedText = LoadProjectController.formatLastEdited(item.lastEditedEpochMillis)
        )

    /**
     * Badge from a loaded project, or null when the load failed / absent — a
     * corrupt or missing project must never crash or blank the dashboard, it
     * just renders its row without badges. Never throws.
     */
    fun buildProjectBadgeOrNull(project: ProjectData?): DashboardProjectBadge? {
        if (project == null) return null

        val bestSwr = project.latestSweepHistoryEntryOrNull?.bestSwr
        val lastMinSwrText =
            if (bestSwr != null && bestSwr > 0.0) {
                "Last SWR " + String.format(Locale.US, "%.3f", bestSwr)
            } else {
                null
            }

        return DashboardProjectBadge(
            lastMinSwrText = lastMinSwrText
        )
    }
}
