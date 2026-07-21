package com.example.antennalab_v1.project

/*
########################################################################
FILE: DesignWorkspaceCard.kt
PACKAGE: com.example.antennalab_v1.project
LAYER: UI / Project Workspace / Design

SYSTEM ROLE
Displays the calculated antenna design stored in ProjectData.

CURRENT DEVELOPMENT ROLE
Provides a practical design summary panel using the real fields that
exist in ProjectData.CalculatedDesign.

SAFE EDIT AREA
- expand design editing later
- add recalculation controls later
- add diagrams later
########################################################################
*/

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.model.ProjectData

/*
########################################################################
EDIT SECTION 1001
DESIGN WORKSPACE CARD
------------------------------------------------------------------------
PURPOSE
Shows the currently calculated design using confirmed model fields.
########################################################################
*/
@Composable
fun DesignWorkspaceCard(
    project: ProjectData
) {
    val design = project.calculatedDesign

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Design Workspace")

            HorizontalDivider()

            Text("Element Count")
            Text(design.elements.size.toString())

            Text("Spacing Count")
            Text(design.spacings.size.toString())

            Text("Boom Length")
            Text("${design.boomLengthMm} mm")

            Text("Feed Point Gap")
            Text("${design.feedPointGapMm} mm")

            HorizontalDivider()

            Text("Matching Method")
            Text(design.matchingMethod.ifBlank { "Not specified" })

            Text("Estimated Gain")
            Text("${design.estimatedGainDbI} dBi")

            Text("Front To Back")
            Text("${design.estimatedFrontToBackDb} dB")

            Text("Estimated Bandwidth")
            Text("${design.estimatedBandwidthMHz} MHz")

            HorizontalDivider()

            Text("Feed Recommendation")
            Text(design.feedRecommendation.feedMethod.ifBlank { "Not specified" })

            Text("Balun Recommendation")
            Text(design.feedRecommendation.balunRecommendation.ifBlank { "Not specified" })

            Text("Matching Notes")
            Text(design.feedRecommendation.matchingNotes.ifBlank { "None" })

            HorizontalDivider()

            Text("Build Guidance")
            Text("Size Class: ${design.buildGuidance.sizeClass.name}")
            Text("Build Difficulty: ${design.buildGuidance.buildDifficulty.name}")
            Text("Tuning Sensitivity: ${design.buildGuidance.tuningSensitivity.name}")
            Text("Recommended Radials: ${design.buildGuidance.recommendedRadialCount}")
            Text("Support Notes: ${design.buildGuidance.supportNotes.ifBlank { "None" }}")
            Text("Mounting Notes: ${design.buildGuidance.mountingNotes.ifBlank { "None" }}")

            HorizontalDivider()

            Text("Design Explanation")
            Text(design.designExplanation.designSummary.ifBlank { "No summary available." })
            Text("Intended Use: ${design.designExplanation.intendedUse.ifBlank { "Not specified" }}")
            Text("Novice Guidance: ${design.designExplanation.noviceGuidance.ifBlank { "Not specified" }}")
            Text("Tuning Advice: ${design.designExplanation.tuningAdvice.ifBlank { "Not specified" }}")

            if (design.designWarnings.isNotEmpty()) {
                HorizontalDivider()

                Text("Warnings")
                design.designWarnings.forEach { warning ->
                    Text("• $warning")
                }
            }

            if (design.elementLengthsMm.isNotEmpty()) {
                HorizontalDivider()

                Text("Element Lengths (mm)")
                design.elementLengthsMm.forEachIndexed { index, value ->
                    Text("${index + 1}. $value")
                }
            }

            if (design.elementSpacingMm.isNotEmpty()) {
                HorizontalDivider()

                Text("Element Spacing (mm)")
                design.elementSpacingMm.forEachIndexed { index, value ->
                    Text("${index + 1}. $value")
                }
            }
        }
    }
}