package com.example.antennalab_v1.features.workspace

/*
########################################################################
FILE: DesignWorkspaceController.kt
PACKAGE: com.example.antennalab_v1.features.workspace
LAYER: UI / Workspace / Design / Logic Control

SYSTEM ROLE
Owns the pure (non-Compose) display logic for DesignWorkspaceScreen so
the screen can stay a Compose shell.

It currently owns:

• the locked design-summary text lines
• the calculated-results line building (element lengths, spacing, boom,
  feed-point gap, matching method) with their conditional inclusion
• millimetre value formatting

DESIGN INTENT
Keep this layer pure and easy to test/migrate. Mirrors the same UI-free
controller pattern used by SweepWorkspaceController and the project /
wizard controllers.

IMPORTANT
This file intentionally avoids Compose APIs and Android dependencies.
########################################################################
*/

import com.example.antennalab_v1.model.CalculatedDesign
import com.example.antennalab_v1.model.ProjectData

/*
########################################################################
SECTION 1000
CONTROLLER OBJECT
########################################################################
PURPOSE
Stateless display helpers for DesignWorkspaceScreen.
########################################################################
*/
object DesignWorkspaceController {

    /*
    ------------------------------------------------------------
    SECTION 1100
    LOCKED DESIGN SUMMARY
    ------------------------------------------------------------
    PURPOSE
    Builds the design-identity summary lines (project, antenna, material)
    shown in the locked summary card.
    ------------------------------------------------------------
    */
    fun buildDesignSummarySections(project: ProjectData): List<List<String>> {
        val material = project.materialConfig
        return listOf(
            listOf(
                "Project: ${project.meta.projectName}",
                "Antenna Type: ${project.designInput.antennaType.name}",
                "Target Frequency: ${project.designInput.targetFrequencyMHz} MHz",
                "Priority Mode: ${project.designInput.priorityMode.name}"
            ),
            listOf(
                "Conductor Material: ${material.conductorMaterial.name}",
                "Conductor Diameter: ${material.conductorDiameterMm} mm",
                "Build Notes: ${material.buildNotes.ifBlank { "None" }}"
            )
        )
    }

    /*
    ------------------------------------------------------------
    SECTION 1200
    CALCULATED RESULTS
    ------------------------------------------------------------
    PURPOSE
    Builds the formatted result lines, each section included only when it
    has data (mirroring the card's conditional rendering).
    ------------------------------------------------------------
    */
    fun buildElementLengthLines(design: CalculatedDesign): List<String> {
        return design.elementLengthsMm.mapIndexed { index, value ->
            "Element ${index + 1}: ${formatMillimetres(value)}"
        }
    }

    fun buildElementSpacingLines(design: CalculatedDesign): List<String> {
        return design.elementSpacingMm.mapIndexed { index, value ->
            "Spacing ${index + 1}: ${formatMillimetres(value)}"
        }
    }

    fun boomLengthLine(design: CalculatedDesign): String? {
        return if (design.boomLengthMm > 0.0) {
            "Boom Length: ${formatMillimetres(design.boomLengthMm)}"
        } else {
            null
        }
    }

    fun feedPointGapLine(design: CalculatedDesign): String? {
        return if (design.feedPointGapMm > 0.0) {
            "Feed Point Gap: ${formatMillimetres(design.feedPointGapMm)}"
        } else {
            null
        }
    }

    fun matchingMethodLine(design: CalculatedDesign): String? {
        return if (design.matchingMethod.isNotBlank()) {
            "Matching Method: ${design.matchingMethod}"
        } else {
            null
        }
    }

    /*
    ------------------------------------------------------------
    SECTION 1300
    FORMATTING
    ------------------------------------------------------------
    */
    fun formatMillimetres(valueMm: Double): String {
        return if (valueMm >= 1000.0) {
            String.format("%.1f mm (%.3f m)", valueMm, valueMm / 1000.0)
        } else {
            String.format("%.1f mm", valueMm)
        }
    }
}
