package com.example.antennalab_v1.features.testing

/*
########################################################################
FILE: SweepWorkspaceState.kt
PACKAGE: com.example.antennalab_v1.features.testing
LAYER: UI / Testing Tools / Workspace State

LAST UPDATED 08/04/2026 00:00

SYSTEM ROLE
Provides a single state container for the sweep workspace UI.

CURRENT DEVELOPMENT ROLE
This model now separates two different sweep workflows:

• normal project-linked testing
• detached unknown-antenna discovery

This allows the workspace to carry the correct state for:

• current sweep result
• reference sweep
• trace history
• display mode and marker state
• discovery classification input
• pending discovery handoff data
• post-action status text

IMPORTANT
This file is intentionally UI-focused and does NOT own sweep execution
logic or domain analysis logic.

SAFE EDIT AREA
- add marker memory slots later
- add trace math state later
- add richer discovery description fields later
########################################################################
*/

import com.example.antennalab_v1.domain.analysis.ChartKind
import com.example.antennalab_v1.model.AntennaClassification
import com.example.antennalab_v1.model.DiscoverySnapshot
import com.example.antennalab_v1.model.ProjectSweepHistoryEntry
import com.example.antennalab_v1.model.testing.SweepResult

/*
########################################################################
SECTION 1
WORKSPACE STATE MODEL
########################################################################
PURPOSE
Container object used by SweepGraphScreen and workspace panels.
########################################################################
*/

data class SweepWorkspaceState(

    /*
    ------------------------------------------------------------
    CURRENT SWEEP
    ------------------------------------------------------------
    */
    val currentSweep: SweepResult? = null,

    /*
    ------------------------------------------------------------
    REFERENCE TRACE
    ------------------------------------------------------------
    */
    val referenceSweep: SweepResult? = null,

    /*
    ------------------------------------------------------------
    SWEEP HISTORY
    ------------------------------------------------------------
    */
    val sweepHistory: List<SweepResult> = emptyList(),

    /*
    ------------------------------------------------------------
    DISCOVERY FLOW
    ------------------------------------------------------------
    */
    val isDiscoveryMode: Boolean = false,
    val discoveryAntennaClassification: AntennaClassification =
        AntennaClassification.NOT_YET_CLASSIFIED,
    val pendingDiscoverySnapshot: DiscoverySnapshot? = null,
    val pendingProjectSweepHistoryEntry: ProjectSweepHistoryEntry? = null,
    val showDiscoveryHandoffActions: Boolean = false,
    val workflowStatusMessage: String? = null,

    /*
    ------------------------------------------------------------
    INCOMPLETE-SWEEP SAVE CONFIRMATION
    ------------------------------------------------------------
    PURPOSE
    When a project-linked sweep comes back incomplete, its history
    entry is held here (instead of auto-saved) until the operator
    confirms or dismisses the save. Null when there is nothing
    awaiting confirmation.
    ------------------------------------------------------------
    */
    val pendingIncompleteSaveEntry: ProjectSweepHistoryEntry? = null,

    /*
    ------------------------------------------------------------
    DISPLAY MODE
    ------------------------------------------------------------
    */
    val displayMode: SweepDisplayMode = SweepDisplayMode.SWR,

    /*
    ------------------------------------------------------------
    TRACE COMPARISON MODE
    ------------------------------------------------------------
    */
    val traceCompareMode: TraceCompareMode = TraceCompareMode.CURRENT_ONLY,

    /*
    ------------------------------------------------------------
    MARKER STATE
    ------------------------------------------------------------
    */
    val markerAIndex: Int = 0,
    val markerBIndex: Int = 0,
    val activeMarkerTarget: WorkspaceMarkerTarget = WorkspaceMarkerTarget.A,

    /*
    ------------------------------------------------------------
    TRANSIENT CHART FOCUS (tap-to-expand)
    ------------------------------------------------------------
    PURPOSE
    Which single chart is expanded, or null for the underlying
    layout. Spec 2.2 defines TWO distinct controls and says they
    "must not be conflated with it in the design OR the code":

      (a) the Simple/Full toggle is a PERSISTENT layout mode
      (b) tap-to-expand is TRANSIENT focus that RETURNS to the
          underlying layout

    So this is a nullable OVERLAY on top of the layout mode, never
    a value of it. Collapsing is expandedChartKind = null, which
    restores whatever mode was active without having to guess
    whether that was Simple or Full — the guess a single
    GRID | SINGLE(kind) enum would force.

    Slice 5's layout mode will be a SEPARATE non-null field. Do not
    merge them.
    ------------------------------------------------------------
    */
    val expandedChartKind: ChartKind? = null,

    /*
    ------------------------------------------------------------
    CSV PREVIEW
    ------------------------------------------------------------
    */
    val showCsvPreview: Boolean = false,

    /*
    ------------------------------------------------------------
    APP ANALYSIS COLLAPSE
    ------------------------------------------------------------
    Spec 2.3: the app's own diagnostics summary is reframed as "app
    analysis" — the value-add on top of the familiar VNA layout — and
    is COLLAPSED BY DEFAULT, so the app's interpretation never outranks
    the measurement it is interpreting.

    Per-SESSION, like showCsvPreview above: the operator collapses or
    expands for the work in front of them without changing a global
    preference. AppSettings.appAnalysisCollapsedDefault seeds the
    initial value when this state is first constructed and does nothing
    after that.

    Default false here is the honest one: it is what a caller that does
    not seed gets, and the spec's default is collapsed.
    ------------------------------------------------------------
    */
    val appAnalysisExpanded: Boolean = false
)

/*
########################################################################
SECTION 2
MARKER TARGET ENUM
########################################################################
PURPOSE
Public marker target enum for workspace state.
########################################################################
*/

enum class WorkspaceMarkerTarget {
    A,
    B
}
