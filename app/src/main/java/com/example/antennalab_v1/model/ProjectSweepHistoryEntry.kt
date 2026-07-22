package com.example.antennalab_v1.model

/*
########################################################################
FILE: ProjectSweepHistoryEntry.kt
PACKAGE: com.example.antennalab_v1.model
LAYER: Core Data Model / Sweep History

SYSTEM ROLE
Stores a compact persisted sweep-history summary for a project.

CURRENT DEVELOPMENT ROLE
This file provides the first-pass persisted history record used by:

• project-linked sweep history
• discovery-applied sweep history
• project overview reference lists
• future sweep-session timeline expansion

IMPORTANT
This model intentionally stores summary/reference data only.
Raw dense sweep arrays are NOT stored here in the first pass.

SAFE EDIT AREA
- add tags or user labels later
- add calibration trust summary later
- add linked raw sweep file references later
########################################################################
*/

enum class ProjectSweepHistoryMode {
    PROJECT_TEST,
    DISCOVERY_APPLIED
}

data class ProjectSweepHistoryEntry(
    val recordedAtEpochMs: Long = 0L,
    val mode: ProjectSweepHistoryMode = ProjectSweepHistoryMode.PROJECT_TEST,
    val hardwareName: String = "",
    val sweepStartMHz: Double = 0.0,
    val sweepEndMHz: Double = 0.0,
    val bestFrequencyMHz: Double = 0.0,
    val bestSwr: Double = 0.0,
    val returnLossDb: Double? = null,
    val label: String = "",
    val note: String = "",

    /*
    ------------------------------------------------------------
    SWEEP COMPLETENESS (flag, don't discard)
    ------------------------------------------------------------
    PURPOSE
    Carry the SweepResult completeness flag into persisted history
    so a partial sweep stays flagged after save/reload instead of
    silently reading as complete.

    Defaults assume a complete sweep so legacy saved entries and
    simulated callers are unaffected.
    ------------------------------------------------------------
    */
    val isComplete: Boolean = true,
    val actualPointCount: Int = 0,
    val requestedPointCount: Int = 0,

    /*
    ------------------------------------------------------------
    CALIBRATION STATE (flag, don't discard)
    ------------------------------------------------------------
    Whether OSL correction was applied to the sweep this entry
    summarizes. Defaults false so legacy entries read as
    uncalibrated (which they were).
    ------------------------------------------------------------
    */
    val isCalibrated: Boolean = false
) {
    val hasReturnLoss: Boolean
        get() = returnLossDb != null

    val hasUsableSweepRange: Boolean
        get() = sweepEndMHz > sweepStartMHz

    val hasIncompleteData: Boolean
        get() = !isComplete
}
