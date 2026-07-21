package com.example.antennalab_v1.domain.calculator

import com.example.antennalab_v1.model.CalculatedDesign
import com.example.antennalab_v1.model.ElementRole
import com.example.antennalab_v1.model.TestData
import kotlin.math.abs

/*
########################################################################
FILE: TuningAssistant.kt
PACKAGE: com.example.antennalab_v1.domain.calculator
LAYER: Domain / Tuning Assistance

SYSTEM ROLE
Compares measured test results against the calculated design target and
produces practical tuning suggestions.

This layer does NOT calculate initial antenna geometry.
It interprets measurement results after a build or test sweep.

INPUTS
- CalculatedDesign
- TestData
- targetFrequencyMHz

OUTPUT
- TuningAssistantResult
  • summary
  • trimSuggestions
  • cautionNotes

RULES
- Advice is approximate and beginner-safe
- Suggestions should avoid pretending to be precision RF optimisation
- If measured data is missing, return guidance for next measurement step
########################################################################
*/

data class TuningAssistantResult(
    val summary: String,
    val trimSuggestions: List<String>,
    val cautionNotes: List<String>
)

fun analyzeTuningAdjustment(
    calculatedDesign: CalculatedDesign,
    testData: TestData,
    targetFrequencyMHz: Double
): TuningAssistantResult {

    if (!testData.hasMeasuredData || testData.resonantFrequencyMHz <= 0.0) {
        return TuningAssistantResult(
            summary = "No measured tuning data available yet.",
            trimSuggestions = listOf(
                "Run a sweep and record the resonant frequency before changing element lengths."
            ),
            cautionNotes = listOf(
                "Do not trim the antenna until a measured resonant frequency is confirmed."
            )
        )
    }

    val measured = testData.resonantFrequencyMHz
    val errorMHz = targetFrequencyMHz - measured
    val errorPercent = if (targetFrequencyMHz > 0.0) {
        (errorMHz / targetFrequencyMHz) * 100.0
    } else {
        0.0
    }

    val trimSuggestions = mutableListOf<String>()
    val cautionNotes = mutableListOf<String>()

    val summary = when {
        abs(errorPercent) < 0.5 ->
            "Measured resonance is already close to target."

        measured < targetFrequencyMHz ->
            "Measured resonance is below target. The antenna is electrically too long."

        else ->
            "Measured resonance is above target. The antenna is electrically too short."
    }

    if (abs(errorPercent) < 0.5) {
        trimSuggestions.add(
            "Only make very small adjustments, or test installation changes before trimming."
        )
    } else if (measured < targetFrequencyMHz) {

        val suggestedTrimPercent = abs(errorPercent) * 0.5

        trimSuggestions.add(
            "Shorten the active tuning dimension slightly. Start with a small trim of about ${formatPercentForAdvice(suggestedTrimPercent)}."
        )

        val trimEstimate = estimateTrimFromDesign(
            calculatedDesign = calculatedDesign,
            suggestedTrimPercent = suggestedTrimPercent
        )

        trimEstimate?.let {
            trimSuggestions.add(it)
        }

        trimSuggestions.add(
            "Trim symmetrically on matched elements where applicable."
        )
    } else {
        trimSuggestions.add(
            "The antenna is already shorter than ideal for the target frequency."
        )
        trimSuggestions.add(
            "Lengthening may be required. If physical extension is not practical, review matching, mounting, or rebuild dimensions."
        )
    }

    if (calculatedDesign.elements.size >= 3) {
        cautionNotes.add(
            "For directional antennas, confirm element spacing and driven element dimensions before trimming other elements."
        )
    }

    if (testData.minimumSwr > 0.0 && testData.minimumSwr > 2.0) {
        cautionNotes.add(
            "High SWR suggests feed, grounding, matching, or installation issues may exist in addition to length error."
        )
    }

    if (calculatedDesign.feedRecommendation.feedMethod.isBlank()) {
        cautionNotes.add(
            "Feed method is not documented, so tuning recommendations are lower confidence."
        )
    }

    cautionNotes.add(
        "Make one small change at a time and re-measure after each adjustment."
    )

    return TuningAssistantResult(
        summary = summary,
        trimSuggestions = trimSuggestions,
        cautionNotes = cautionNotes
    )
}

private fun estimateTrimFromDesign(
    calculatedDesign: CalculatedDesign,
    suggestedTrimPercent: Double
): String? {

    val drivenElement =
        calculatedDesign.elements.firstOrNull { it.role == ElementRole.DRIVEN }

    if (drivenElement != null && drivenElement.lengthMm > 0.0) {
        val totalTrimMm = drivenElement.lengthMm * (suggestedTrimPercent / 100.0)
        val eachSideMm = totalTrimMm / 2.0

        return "Estimated driven element trim: about ${formatMillimeters(totalTrimMm)} total, or ${formatMillimeters(eachSideMm)} per side."
    }

    if (calculatedDesign.elements.size == 2) {
        val matchedLegLength = calculatedDesign.elements.firstOrNull()?.lengthMm ?: 0.0
        if (matchedLegLength > 0.0) {
            val eachLegTrimMm = matchedLegLength * (suggestedTrimPercent / 100.0)
            val totalTrimMm = eachLegTrimMm * 2.0

            return "Estimated dipole trim: about ${formatMillimeters(totalTrimMm)} total, or ${formatMillimeters(eachLegTrimMm)} from each side."
        }
    }

    if (calculatedDesign.elementLengthsMm.isNotEmpty()) {
        val firstLength = calculatedDesign.elementLengthsMm.first()
        if (firstLength > 0.0) {
            val estimatedTrimMm = firstLength * (suggestedTrimPercent / 100.0)
            return "Estimated starting trim on the main tuning element: about ${formatMillimeters(estimatedTrimMm)}."
        }
    }

    return null
}

private fun formatPercentForAdvice(value: Double): String {
    return String.format("%.1f%%", value.coerceAtLeast(0.1))
}

private fun formatMillimeters(value: Double): String {
    return String.format("%.1f mm", value)
}