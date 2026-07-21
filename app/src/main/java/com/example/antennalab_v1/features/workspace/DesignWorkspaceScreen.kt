package com.example.antennalab_v1.features.workspace

/*
########################################################################
FILE: DesignWorkspaceScreen.kt
PACKAGE: com.example.antennalab_v1.features.workspace
LAYER: UI / Workspace / Design

LAST UPDATED 04/04/2026 22:55

SYSTEM ROLE
Design workspace view for an existing project.

CURRENT DEVELOPMENT ROLE
Shows the generated design summary and calculated dimensions for the
current project.

ARCHITECTURE RULE
Core design inputs are currently locked after project creation.

IMPORTANT PRODUCT RULE
The wizard remains the generator/controller of the final design.
Major core-design changes should be handled by creating a new project.
A future controlled regenerate flow may be added later, but free-form
live editing is intentionally disabled for now.
########################################################################
*/

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.model.ProjectData

@Composable
fun DesignWorkspaceScreen(
    project: ProjectData,
    onBack: () -> Unit,
    onApplyChanges: (ProjectData) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DesignLockedSummaryCard(project)
        DesignCalculatedResultsCard(project)
        DesignWorkspaceActions(
            onBack = onBack
        )
    }
}

@Composable
private fun DesignLockedSummaryCard(
    project: ProjectData
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Design Summary")
            HorizontalDivider()

            Text("Project: ${project.meta.projectName}")
            Text("Antenna Type: ${project.designInput.antennaType.name}")
            Text("Target Frequency: ${project.designInput.targetFrequencyMHz} MHz")
            Text("Priority Mode: ${project.designInput.priorityMode.name}")

            HorizontalDivider()

            Text("Conductor Material: ${project.materialConfig.conductorMaterial.name}")
            Text("Conductor Diameter: ${project.materialConfig.conductorDiameterMm} mm")
            Text("Build Notes: ${project.materialConfig.buildNotes.ifBlank { "None" }}")

            HorizontalDivider()

            Text(
                "Core design inputs are locked for this project. " +
                        "Use the wizard to generate a new design when major changes are required."
            )
        }
    }
}

@Composable
private fun DesignCalculatedResultsCard(
    project: ProjectData
) {
    val calculatedDesign = project.calculatedDesign

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Calculated Results")
            HorizontalDivider()

            if (calculatedDesign.elementLengthsMm.isNotEmpty()) {
                Text("Element Lengths")
                calculatedDesign.elementLengthsMm.forEachIndexed { index, value ->
                    Text("Element ${index + 1}: ${formatMillimetres(value)}")
                }
            } else {
                Text("No stored element lengths.")
            }

            if (calculatedDesign.elementSpacingMm.isNotEmpty()) {
                HorizontalDivider()
                Text("Element Spacing")
                calculatedDesign.elementSpacingMm.forEachIndexed { index, value ->
                    Text("Spacing ${index + 1}: ${formatMillimetres(value)}")
                }
            }

            if (calculatedDesign.boomLengthMm > 0.0) {
                HorizontalDivider()
                Text("Boom Length: ${formatMillimetres(calculatedDesign.boomLengthMm)}")
            }

            if (calculatedDesign.feedPointGapMm > 0.0) {
                Text("Feed Point Gap: ${formatMillimetres(calculatedDesign.feedPointGapMm)}")
            }

            if (calculatedDesign.matchingMethod.isNotBlank()) {
                Text("Matching Method: ${calculatedDesign.matchingMethod}")
            }

            if (calculatedDesign.designWarnings.isNotEmpty()) {
                HorizontalDivider()
                Text("Design Warnings")
                calculatedDesign.designWarnings.forEach { warning ->
                    Text("• $warning")
                }
            }
        }
    }
}

@Composable
private fun DesignWorkspaceActions(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

private fun formatMillimetres(valueMm: Double): String {
    return if (valueMm >= 1000.0) {
        String.format("%.1f mm (%.3f m)", valueMm, valueMm / 1000.0)
    } else {
        String.format("%.1f mm", valueMm)
    }
}