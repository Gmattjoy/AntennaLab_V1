package com.example.antennalab_v1.features.testing

/*
########################################################################
FILE: SweepTuningWidgets.kt
PACKAGE: com.example.antennalab_v1.features.testing
LAYER: UI / Testing / Tuning Display

LAST UPDATED 16/3/2026 23:24

SYSTEM ROLE
Provides reusable diagnostics and tuning display widgets used by the
sweep workspace.

CURRENT DEVELOPMENT ROLE
This file now owns:

• extracted diagnostics summary panel
• extracted tuning interpretation panel

IMPORTANT ARCHITECTURE RULE
This file must remain UI-only.

Do NOT place:

• sweep acquisition logic
• USB hardware code
• sweep math engines
• tuning algorithms

Those belong to domain layers.

SAFE EDIT AREA
- add workflow summary widgets later
- add novice/expert display modes later
- add confidence/severity presentation later
########################################################################
*/

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.domain.analysis.AdjustmentEstimate
import com.example.antennalab_v1.domain.analysis.AntennaBehaviorClassification
import com.example.antennalab_v1.domain.analysis.TuningSuggestionReport
import com.example.antennalab_v1.domain.analysis.TuningWorkflowReport

/*
########################################################################
SECTION 1
PUBLIC DIAGNOSTICS UI MODEL
########################################################################
PURPOSE
Provides a public extracted diagnostics display model so the screen can
map its local/private diagnostics state into a reusable panel.
########################################################################
*/
data class SweepDiagnosticsCardModel(
    val minimumSwrText: String? = null,
    val resonanceText: String? = null,
    val secondaryResonanceText: String? = null,
    val bandwidthText: String? = null,
    val bandwidthAt15Text: String? = null,
    val matchingQualityText: String? = null,
    val impedanceStabilityText: String? = null,
    val sweepShapeText: String? = null,
    val reactanceTrendText: String? = null,
    val mismatchSeverityText: String? = null,
    val likelyConditionText: String? = null,
    val feedlineLossSuspicionText: String? = null,
    val resonanceCountText: String? = null,
    val summaryLines: List<String> = emptyList()
)

/*
########################################################################
SECTION 2
DIAGNOSTICS SUMMARY PANEL
########################################################################
PURPOSE
Owns the extracted diagnostics summary UI.
########################################################################
*/
@Composable
fun SweepDiagnosticsSummaryPanel(
    diagnostics: SweepDiagnosticsCardModel?,
    instrumentSurface: Color,
    instrumentDivider: Color,
    instrumentAccent: Color,
    instrumentTextPrimary: Color,
    instrumentTextSecondary: Color
) {
    SharedInstrumentCard(
        instrumentSurface = instrumentSurface,
        instrumentDivider = instrumentDivider
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SharedInstrumentSectionHeader(
                text = "Diagnostics Summary",
                instrumentAccent = instrumentAccent
            )

            SharedInstrumentDividerLine(
                instrumentDivider = instrumentDivider
            )

            if (diagnostics == null) {
                SharedInstrumentMutedText(
                    text = "Diagnostics unavailable.",
                    instrumentTextSecondary = instrumentTextSecondary
                )
                return@SharedInstrumentCard
            }

            diagnostics.minimumSwrText?.let {
                SharedTwoValueRow(
                    label = "Minimum SWR",
                    value = it,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            diagnostics.resonanceText?.let {
                SharedTwoValueRow(
                    label = "Detected Resonance",
                    value = it,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            diagnostics.secondaryResonanceText?.let {
                SharedTwoValueRow(
                    label = "Secondary Resonance",
                    value = it,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            diagnostics.bandwidthText?.let {
                SharedTwoValueRow(
                    label = "SWR ≤ 2 Bandwidth",
                    value = it,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            diagnostics.bandwidthAt15Text?.let {
                SharedTwoValueRow(
                    label = "SWR ≤ 1.5 Bandwidth",
                    value = it,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            diagnostics.matchingQualityText?.let {
                SharedTwoValueRow(
                    label = "Matching Quality",
                    value = it,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            diagnostics.impedanceStabilityText?.let {
                SharedTwoValueRow(
                    label = "Impedance Stability",
                    value = it,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            diagnostics.sweepShapeText?.let {
                SharedTwoValueRow(
                    label = "Sweep Shape",
                    value = it,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            diagnostics.reactanceTrendText?.let {
                SharedTwoValueRow(
                    label = "Reactance Trend",
                    value = it,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            diagnostics.mismatchSeverityText?.let {
                SharedTwoValueRow(
                    label = "Mismatch Severity",
                    value = it,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            diagnostics.likelyConditionText?.let {
                SharedTwoValueRow(
                    label = "Likely Condition",
                    value = it,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            diagnostics.feedlineLossSuspicionText?.let {
                SharedTwoValueRow(
                    label = "Feedline Loss Suspicion",
                    value = it,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            diagnostics.resonanceCountText?.let {
                SharedTwoValueRow(
                    label = "Resonance Count Estimate",
                    value = it,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            if (diagnostics.summaryLines.isNotEmpty()) {
                SharedInstrumentDividerLine(
                    instrumentDivider = instrumentDivider
                )

                SharedInstrumentSubHeader(
                    text = "Summary",
                    instrumentTextPrimary = instrumentTextPrimary
                )

                diagnostics.summaryLines.forEach { line ->
                    SharedInstrumentValueText(
                        text = "• $line",
                        instrumentTextPrimary = instrumentTextPrimary
                    )
                }
            }
        }
    }
}

/*
########################################################################
SECTION 3
TUNING INTERPRETATION PANEL
########################################################################
PURPOSE
Owns the extracted tuning interpretation UI.
########################################################################
*/
@Composable
fun SweepTuningInterpretationPanel(
    classification: AntennaBehaviorClassification?,
    suggestionReport: TuningSuggestionReport?,
    adjustmentEstimate: AdjustmentEstimate?,
    workflowReport: TuningWorkflowReport?,
    instrumentSurface: Color,
    instrumentDivider: Color,
    instrumentAccent: Color,
    instrumentTextPrimary: Color,
    instrumentTextSecondary: Color
) {
    SharedInstrumentCard(
        instrumentSurface = instrumentSurface,
        instrumentDivider = instrumentDivider
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SharedInstrumentSectionHeader(
                text = "Tuning Interpretation",
                instrumentAccent = instrumentAccent
            )

            SharedInstrumentDividerLine(
                instrumentDivider = instrumentDivider
            )

            if (
                classification == null &&
                suggestionReport == null &&
                adjustmentEstimate == null &&
                workflowReport == null
            ) {
                SharedInstrumentMutedText(
                    text = "Tuning interpretation unavailable.",
                    instrumentTextSecondary = instrumentTextSecondary
                )
                return@SharedInstrumentCard
            }

            classification?.let { behavior ->
                SharedInstrumentSubHeader(
                    text = "Antenna Behaviour",
                    instrumentTextPrimary = instrumentTextPrimary
                )
                SharedInstrumentValueText(
                    text = behavior.primaryBehavior.name,
                    instrumentTextPrimary = instrumentTextPrimary
                )

                if (behavior.supportingBehaviors.isNotEmpty()) {
                    SharedInstrumentSubHeader(
                        text = "Supporting Behaviours",
                        instrumentTextPrimary = instrumentTextPrimary
                    )
                    behavior.supportingBehaviors.forEach { item ->
                        SharedInstrumentValueText(
                            text = "• ${item.name}",
                            instrumentTextPrimary = instrumentTextPrimary
                        )
                    }
                }

                SharedInstrumentSubHeader(
                    text = "Behaviour Summary",
                    instrumentTextPrimary = instrumentTextPrimary
                )
                SharedInstrumentValueText(
                    text = behavior.summary,
                    instrumentTextPrimary = instrumentTextPrimary
                )

                if (behavior.observations.isNotEmpty()) {
                    SharedInstrumentSubHeader(
                        text = "Observations",
                        instrumentTextPrimary = instrumentTextPrimary
                    )
                    behavior.observations.forEach { observation ->
                        SharedInstrumentValueText(
                            text = "• $observation",
                            instrumentTextPrimary = instrumentTextPrimary
                        )
                    }
                }

                SharedInstrumentDividerLine(
                    instrumentDivider = instrumentDivider
                )
            }

            suggestionReport?.let { report ->
                SharedInstrumentSubHeader(
                    text = "Recommended Action",
                    instrumentTextPrimary = instrumentTextPrimary
                )
                SharedInstrumentValueText(
                    text = report.primaryRecommendation,
                    instrumentTextPrimary = instrumentTextPrimary
                )

                SharedInstrumentSubHeader(
                    text = "Suggestion Summary",
                    instrumentTextPrimary = instrumentTextPrimary
                )
                SharedInstrumentValueText(
                    text = report.summary,
                    instrumentTextPrimary = instrumentTextPrimary
                )

                if (report.suggestions.isNotEmpty()) {
                    SharedInstrumentSubHeader(
                        text = "Suggested Actions",
                        instrumentTextPrimary = instrumentTextPrimary
                    )
                    report.suggestions.forEach { suggestion ->
                        SharedInstrumentValueText(
                            text = "• ${suggestion.title}",
                            instrumentTextPrimary = instrumentTextPrimary
                        )
                        SharedInstrumentMutedText(
                            text = "  Priority: ${suggestion.priority.name}",
                            instrumentTextSecondary = instrumentTextSecondary
                        )
                        SharedInstrumentMutedText(
                            text = "  ${suggestion.detail}",
                            instrumentTextSecondary = instrumentTextSecondary
                        )
                    }
                }

                SharedInstrumentDividerLine(
                    instrumentDivider = instrumentDivider
                )
            }

            adjustmentEstimate?.let { estimate ->
                SharedInstrumentSubHeader(
                    text = "Adjustment Estimate",
                    instrumentTextPrimary = instrumentTextPrimary
                )
                SharedInstrumentValueText(
                    text = estimate.summary,
                    instrumentTextPrimary = instrumentTextPrimary
                )

                SharedTwoValueRow(
                    label = "Direction",
                    value = when (estimate.direction.name) {
                        "SHORTEN" -> "SHORTEN"
                        "REBUILD_OR_REPLACE" -> "REBUILD OR REPLACE"
                        "RETARGET_HIGHER" -> "RETARGET HIGHER"
                        else -> "NONE"
                    },
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )

                SharedTwoValueRow(
                    label = "Severity",
                    value = estimate.severity.name,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )

                SharedTwoValueRow(
                    label = "Frequency Offset",
                    value = String.format("%.3f MHz", estimate.frequencyOffsetMHz),
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )

                SharedTwoValueRow(
                    label = "Frequency Offset Percent",
                    value = String.format("%.3f%%", estimate.frequencyOffsetPercent),
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )

                SharedInstrumentSubHeader(
                    text = "Physical Change",
                    instrumentTextPrimary = instrumentTextPrimary
                )

                when {
                    estimate.estimatedLengthChangeMm != null &&
                            estimate.estimatedLengthChangeInches != null -> {
                        SharedInstrumentValueText(
                            text = String.format(
                                "%.2f mm  |  %.3f in",
                                estimate.estimatedLengthChangeMm,
                                estimate.estimatedLengthChangeInches
                            ),
                            instrumentTextPrimary = instrumentTextPrimary
                        )
                    }

                    estimate.direction.name == "SHORTEN" -> {
                        SharedInstrumentValueText(
                            text = "Shortening direction available. Exact physical trim estimate requires active radiator length measurement.",
                            instrumentTextPrimary = instrumentTextPrimary
                        )
                    }

                    estimate.direction.name == "REBUILD_OR_REPLACE" -> {
                        SharedInstrumentValueText(
                            text = "Do not treat this as a simple add-length correction. Use rebuild, replacement length, replacement part, or retargeting.",
                            instrumentTextPrimary = instrumentTextPrimary
                        )
                    }

                    estimate.direction.name == "RETARGET_HIGHER" -> {
                        SharedInstrumentValueText(
                            text = "This result is better handled as a higher-frequency retarget decision rather than a physical add-length correction.",
                            instrumentTextPrimary = instrumentTextPrimary
                        )
                    }

                    else -> {
                        SharedInstrumentValueText(
                            text = "No direct physical change estimate is currently required.",
                            instrumentTextPrimary = instrumentTextPrimary
                        )
                    }
                }

                SharedInstrumentDividerLine(
                    instrumentDivider = instrumentDivider
                )
            }

            workflowReport?.let { report ->
                SharedInstrumentSubHeader(
                    text = "Tuning Workflow",
                    instrumentTextPrimary = instrumentTextPrimary
                )
                SharedInstrumentValueText(
                    text = report.headline,
                    instrumentTextPrimary = instrumentTextPrimary
                )

                SharedInstrumentSubHeader(
                    text = "Workflow Summary",
                    instrumentTextPrimary = instrumentTextPrimary
                )
                SharedInstrumentValueText(
                    text = report.summary,
                    instrumentTextPrimary = instrumentTextPrimary
                )

                if (report.steps.isNotEmpty()) {
                    SharedInstrumentSubHeader(
                        text = "Workflow Steps",
                        instrumentTextPrimary = instrumentTextPrimary
                    )
                    report.steps.forEach { step ->
                        SharedInstrumentValueText(
                            text = "${step.stepNumber}. ${step.title}",
                            instrumentTextPrimary = instrumentTextPrimary
                        )
                        SharedInstrumentMutedText(
                            text = "   ${step.detail}",
                            instrumentTextSecondary = instrumentTextSecondary
                        )
                    }
                }
            }
        }
    }
}