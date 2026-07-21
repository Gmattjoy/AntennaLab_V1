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
    CSV PREVIEW
    ------------------------------------------------------------
    */
    val showCsvPreview: Boolean = false
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
