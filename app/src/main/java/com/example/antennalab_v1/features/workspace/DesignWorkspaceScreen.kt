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

            DesignWorkspaceController.buildDesignSummarySections(project)
                .forEachIndexed { sectionIndex, lines ->
                    if (sectionIndex > 0) {
                        HorizontalDivider()
                    }
                    lines.forEach { line -> Text(line) }
                }

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

            val elementLengthLines = DesignWorkspaceController.buildElementLengthLines(calculatedDesign)
            if (elementLengthLines.isNotEmpty()) {
                Text("Element Lengths")
                elementLengthLines.forEach { line -> Text(line) }
            } else {
                Text("No stored element lengths.")
            }

            val elementSpacingLines = DesignWorkspaceController.buildElementSpacingLines(calculatedDesign)
            if (elementSpacingLines.isNotEmpty()) {
                HorizontalDivider()
                Text("Element Spacing")
                elementSpacingLines.forEach { line -> Text(line) }
            }

            DesignWorkspaceController.boomLengthLine(calculatedDesign)?.let { line ->
                HorizontalDivider()
                Text(line)
            }

            DesignWorkspaceController.feedPointGapLine(calculatedDesign)?.let { line ->
                Text(line)
            }

            DesignWorkspaceController.matchingMethodLine(calculatedDesign)?.let { line ->
                Text(line)
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