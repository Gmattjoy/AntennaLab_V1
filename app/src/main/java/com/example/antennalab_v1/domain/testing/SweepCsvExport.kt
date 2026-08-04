package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.testing.SweepResult

/*
########################################################################
FILE: SweepCsvExport.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Export

SYSTEM ROLE
The CSV provenance header. Pure string building, no Compose, no IO.

WHY THIS EXISTS — a §10c.7-class leak
The CSV output carried NO provenance at all: its header was only
"# range=... step=... points=.../... complete=..." plus the column names.
A simulated sweep's CSV was therefore byte-identical, in provenance
terms, to a real measurement — nothing in the file said which it was.
That was survivable while synthetic sweeps needed hardware to produce;
it stops being survivable the moment a debug toggle can mint them
off-bench, so the leak is plugged in the same change that opens the tap.

Deliberately matches TouchstoneExport's instrument line, so the two
export formats agree about the same sweep:
  - a self-named sweep reports its own name ("SIMULATED", a driver tag)
  - an unnamed sweep reports "unknown" rather than guessing

SCOPE — HEADER ONLY.
CSV row building is still inline in SweepCsvPreviewPanel
(SweepToolsWidgets.kt). Extracting the rows is a separate task (Slice C);
this file is its intended home when that happens. Do not grow it into a
second row formatter in the meantime.
########################################################################
*/
object SweepCsvExport {

    /*
    --------------------------------------------------------------------
    Provenance header
    EDIT SECTION 1001
    --------------------------------------------------------------------
    One comment line naming what produced the data and whether calibration
    was applied. Comment-prefixed so it does not disturb a parser reading
    the column row.
    --------------------------------------------------------------------
    */
    fun buildProvenanceHeader(result: SweepResult): String =
        "# hardware=${instrumentLabel(result)}  calibrated=${result.isCalibrated}"

    /*
    Same rule as TouchstoneExport.instrumentLabel: never invent a name for
    a sweep that did not identify itself.
    */
    private fun instrumentLabel(result: SweepResult): String =
        result.hardwareProfile.ifBlank { "unknown" }
}
