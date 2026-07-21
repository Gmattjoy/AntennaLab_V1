package com.example.antennalab_v1.domain.analysis

import com.example.antennalab_v1.domain.testing.FeedlineLossSuspicion
import com.example.antennalab_v1.domain.testing.ImpedanceStability
import com.example.antennalab_v1.domain.testing.LikelyCondition
import com.example.antennalab_v1.domain.testing.MatchingQuality
import com.example.antennalab_v1.domain.testing.SweepDiagnosticsEngine
import com.example.antennalab_v1.domain.testing.SweepShape
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import kotlin.math.abs

/*
------------------------------------------------------------
EDIT SECTION 1000
FILE HEADER
------------------------------------------------------------
FILE: AntennaBehaviorClassifier.kt
PACKAGE: com.example.antennalab_v1.domain.analysis
LAYER: Domain / Analysis

SYSTEM ROLE
Classifies overall antenna behaviour from sweep shape and impedance
movement.

This file provides a stable first-pass engineering interpretation layer
that converts sweep data into reusable behaviour categories for:

• Sweep diagnostics
• Tuning assistant
• Engineering dashboard
• Future educational explanations
• Future automatic tuning guidance

DESIGN GOAL
Keep this classifier simple, deterministic, and safe to expand.

It should provide useful engineering hints now without overcommitting to
high-precision RF claims before the hardware and calibration pipeline are
more advanced.

SAFE EDIT AREA
- add more behaviour categories
- refine thresholds
- add confidence scoring later
- add multi-band and trap-specific detection later
------------------------------------------------------------
*/

/*
------------------------------------------------------------
EDIT SECTION 1100
BEHAVIOUR ENUM
------------------------------------------------------------
PURPOSE
Defines the current high-level behaviour categories that can be inferred
from sweep shape and diagnostics depth.
------------------------------------------------------------
*/
enum class AntennaBehaviorType {
    HIGH_Q_NARROW_RESONANCE,
    BROAD_LOSSY_RESPONSE,
    MULTIPLE_RESONANCE_CANDIDATES,
    RESONANCE_BELOW_TARGET,
    RESONANCE_ABOVE_TARGET,
    FLAT_POOR_MATCH,
    FEEDLINE_INTERACTION_SUSPECTED,
    IMPEDANCE_UNSTABLE,
    GENERALLY_WELL_MATCHED,
    LIKELY_TOO_SHORT,
    LIKELY_TOO_LONG,
    HIGH_LOSS_OR_WEAK_RADIATION,
    COMPLEX_MULTI_RESONANCE,
    INCONCLUSIVE
}

/*
------------------------------------------------------------
EDIT SECTION 1200
RESULT MODEL
------------------------------------------------------------
PURPOSE
Provides a stable output structure for UI and higher-level diagnostic
systems.

SAFE EDIT AREA
- add confidence
- add supporting metrics
- add tuning suggestion references later
------------------------------------------------------------
*/
data class AntennaBehaviorClassification(
    val primaryBehavior: AntennaBehaviorType,
    val supportingBehaviors: List<AntennaBehaviorType>,
    val summary: String,
    val observations: List<String>
)

/*
------------------------------------------------------------
EDIT SECTION 1300
CLASSIFIER OBJECT
------------------------------------------------------------
PURPOSE
Public entry point for converting SweepResult into behaviour
classification.
------------------------------------------------------------
*/
object AntennaBehaviorClassifier {

    /*
    ------------------------------------------------------------
    EDIT SECTION 1301
    CLASSIFY ENTRY
    ------------------------------------------------------------
    PURPOSE
    Produces a stable behaviour classification from the current sweep and
    target frequency.

    This upgraded version combines:
    • direct sweep heuristics
    • impedance movement checks
    • deeper diagnostics from SweepDiagnosticsEngine
    ------------------------------------------------------------
    */
    fun classify(
        sweepResult: SweepResult,
        targetFrequencyMHz: Double
    ): AntennaBehaviorClassification {

        /*
        ------------------------------------------------------------
        EDIT SECTION 1302
        EMPTY GUARD
        ------------------------------------------------------------
        */
        if (sweepResult.points.isEmpty()) {
            return AntennaBehaviorClassification(
                primaryBehavior = AntennaBehaviorType.INCONCLUSIVE,
                supportingBehaviors = emptyList(),
                summary = "No sweep data available.",
                observations = listOf("The sweep result contains no measurement points.")
            )
        }

        /*
        ------------------------------------------------------------
        EDIT SECTION 1303
        CORE INPUTS
        ------------------------------------------------------------
        */
        val diagnostics = SweepDiagnosticsEngine.analyzeSweep(sweepResult)

        val points = sweepResult.points
        val minimumSwrPoint = points.minByOrNull { it.swr }
            ?: return AntennaBehaviorClassification(
                primaryBehavior = AntennaBehaviorType.INCONCLUSIVE,
                supportingBehaviors = emptyList(),
                summary = "Unable to classify sweep behaviour.",
                observations = listOf("Minimum SWR point could not be determined.")
            )

        val minimumSwr = minimumSwrPoint.swr
        val resonanceFrequencyMHz = minimumSwrPoint.frequencyMHz
        val swrSpan = points.maxOf { it.swr } - points.minOf { it.swr }
        val resistanceSpan = points.maxOf { it.resistance } - points.minOf { it.resistance }
        val reactanceSpan = points.maxOf { it.reactance } - points.minOf { it.reactance }
        val thresholdBandwidthMHz = estimateBandwidthAtOrBelowSWR(points, 2.0)
        val lowSWRRegionCount = countLowSwrRegions(points, 2.0)
        val signChangeCount = countReactanceSignChanges(points)
        val nearFlatPoorMatch = isFlatPoorMatch(points, swrSpan, minimumSwr)
        val highQCandidate = isHighQNarrowResonance(minimumSwr, thresholdBandwidthMHz)
        val broadLossyCandidate = isBroadLossyResponse(minimumSwr, thresholdBandwidthMHz)
        val resonanceOffsetMHz = resonanceFrequencyMHz - targetFrequencyMHz

        val supportingBehaviors = mutableListOf<AntennaBehaviorType>()
        val observations = mutableListOf<String>()

        /*
        ------------------------------------------------------------
        EDIT SECTION 1304
        OBSERVATION BUILD
        ------------------------------------------------------------
        */
        observations += String.format(
            "Minimum SWR: %.3f at %.3f MHz.",
            minimumSwr,
            resonanceFrequencyMHz
        )

        observations += String.format(
            "Primary diagnostics resonance: %.3f MHz.",
            diagnostics.resonanceFrequencyMHz
        )

        diagnostics.secondaryResonanceFrequencyMHz?.let {
            observations += String.format(
                "Secondary diagnostics resonance: %.3f MHz.",
                it
            )
        }

        thresholdBandwidthMHz?.let {
            observations += String.format("Estimated SWR ≤ 2 bandwidth: %.3f MHz.", it)
        } ?: run {
            observations += "No SWR ≤ 2 bandwidth region detected."
        }

        observations += String.format(
            "Impedance span: R %.3f Ω, X %.3f Ω.",
            resistanceSpan,
            reactanceSpan
        )

        observations += "Diagnostics sweep shape: ${diagnostics.sweepShape.name}."
        observations += "Diagnostics reactance trend: ${diagnostics.reactanceTrend.name}."
        observations += "Diagnostics mismatch severity: ${diagnostics.mismatchSeverity.name}."
        observations += "Diagnostics likely condition: ${diagnostics.likelyCondition.name}."

        /*
        ------------------------------------------------------------
        EDIT SECTION 1305
        SUPPORTING FLAGS
        ------------------------------------------------------------
        */
        if (lowSWRRegionCount >= 2 || diagnostics.resonanceCountEstimate >= 2) {
            supportingBehaviors += AntennaBehaviorType.MULTIPLE_RESONANCE_CANDIDATES
            observations += "Multiple resonance indicators were detected."
        }

        if (
            signChangeCount >= 3 ||
            diagnostics.feedlineLossSuspicion == FeedlineLossSuspicion.HIGH
        ) {
            supportingBehaviors += AntennaBehaviorType.FEEDLINE_INTERACTION_SUSPECTED
            observations += "Interaction or feedline-related effects are suspected."
        }

        if (
            reactanceSpan + resistanceSpan > 120.0 ||
            diagnostics.impedanceStability == ImpedanceStability.UNSTABLE
        ) {
            supportingBehaviors += AntennaBehaviorType.IMPEDANCE_UNSTABLE
            observations += "Impedance varies strongly across the sweep."
        }

        if (diagnostics.likelyCondition == LikelyCondition.LIKELY_TOO_SHORT) {
            supportingBehaviors += AntennaBehaviorType.LIKELY_TOO_SHORT
            observations += "Diagnostics suggest the antenna may be electrically too short."
        }

        if (diagnostics.likelyCondition == LikelyCondition.LIKELY_TOO_LONG) {
            supportingBehaviors += AntennaBehaviorType.LIKELY_TOO_LONG
            observations += "Diagnostics suggest the antenna may be electrically too long."
        }

        if (diagnostics.likelyCondition == LikelyCondition.HIGH_LOSS_OR_WEAK_RADIATION) {
            supportingBehaviors += AntennaBehaviorType.HIGH_LOSS_OR_WEAK_RADIATION
            observations += "Diagnostics suggest high loss or weak radiation behaviour."
        }

        if (diagnostics.likelyCondition == LikelyCondition.COMPLEX_MULTI_RESONANCE) {
            supportingBehaviors += AntennaBehaviorType.COMPLEX_MULTI_RESONANCE
            observations += "Diagnostics suggest a complex multi-resonance response."
        }

        if (resonanceOffsetMHz < -0.03) {
            supportingBehaviors += AntennaBehaviorType.RESONANCE_BELOW_TARGET
            observations += String.format(
                "Resonance is below target by %.3f MHz.",
                abs(resonanceOffsetMHz)
            )
        } else if (resonanceOffsetMHz > 0.03) {
            supportingBehaviors += AntennaBehaviorType.RESONANCE_ABOVE_TARGET
            observations += String.format(
                "Resonance is above target by %.3f MHz.",
                abs(resonanceOffsetMHz)
            )
        } else {
            observations += "Resonance is close to target frequency."
        }

        /*
        ------------------------------------------------------------
        EDIT SECTION 1306
        PRIMARY PICK
        ------------------------------------------------------------
        */
        val primaryBehavior = when {
            diagnostics.likelyCondition == LikelyCondition.COMPLEX_MULTI_RESONANCE ->
                AntennaBehaviorType.COMPLEX_MULTI_RESONANCE

            diagnostics.likelyCondition == LikelyCondition.LIKELY_TOO_SHORT ->
                AntennaBehaviorType.LIKELY_TOO_SHORT

            diagnostics.likelyCondition == LikelyCondition.LIKELY_TOO_LONG ->
                AntennaBehaviorType.LIKELY_TOO_LONG

            diagnostics.likelyCondition == LikelyCondition.HIGH_LOSS_OR_WEAK_RADIATION ->
                AntennaBehaviorType.HIGH_LOSS_OR_WEAK_RADIATION

            nearFlatPoorMatch -> {
                observations += "The sweep is relatively flat while match quality remains poor."
                AntennaBehaviorType.FLAT_POOR_MATCH
            }

            lowSWRRegionCount >= 2 || diagnostics.resonanceCountEstimate >= 2 -> {
                observations += "The sweep contains more than one likely resonance region."
                AntennaBehaviorType.MULTIPLE_RESONANCE_CANDIDATES
            }

            highQCandidate || diagnostics.sweepShape == SweepShape.SHARP_SINGLE_DIP -> {
                observations += "The main dip is narrow and deep."
                AntennaBehaviorType.HIGH_Q_NARROW_RESONANCE
            }

            broadLossyCandidate || diagnostics.sweepShape == SweepShape.BROAD_SINGLE_DIP -> {
                observations += "The dip is broad and shallow, which may indicate loss or weak efficiency."
                AntennaBehaviorType.BROAD_LOSSY_RESPONSE
            }

            minimumSwr <= 1.5 &&
                    abs(resonanceOffsetMHz) <= 0.03 &&
                    diagnostics.matchingQuality != MatchingQuality.POOR -> {
                observations += "The antenna appears reasonably well matched near target."
                AntennaBehaviorType.GENERALLY_WELL_MATCHED
            }

            signChangeCount >= 3 ||
                    diagnostics.feedlineLossSuspicion == FeedlineLossSuspicion.HIGH -> {
                observations += "Sweep shape suggests interaction effects rather than a single clean resonance."
                AntennaBehaviorType.FEEDLINE_INTERACTION_SUSPECTED
            }

            resonanceOffsetMHz < -0.03 -> {
                observations += "Main resonance is below target frequency."
                AntennaBehaviorType.RESONANCE_BELOW_TARGET
            }

            resonanceOffsetMHz > 0.03 -> {
                observations += "Main resonance is above target frequency."
                AntennaBehaviorType.RESONANCE_ABOVE_TARGET
            }

            reactanceSpan + resistanceSpan > 120.0 ||
                    diagnostics.impedanceStability == ImpedanceStability.UNSTABLE -> {
                observations += "Impedance movement is unusually strong across the sweep."
                AntennaBehaviorType.IMPEDANCE_UNSTABLE
            }

            else -> {
                observations += "No dominant behaviour strongly exceeded the current thresholds."
                AntennaBehaviorType.INCONCLUSIVE
            }
        }

        /*
        ------------------------------------------------------------
        EDIT SECTION 1307
        RESULT BUILD
        ------------------------------------------------------------
        */
        val distinctSupportingBehaviors = supportingBehaviors.distinct()

        val summary = buildSummary(
            primaryBehavior = primaryBehavior,
            supportingBehaviors = distinctSupportingBehaviors,
            targetFrequencyMHz = targetFrequencyMHz,
            resonanceFrequencyMHz = resonanceFrequencyMHz
        )

        return AntennaBehaviorClassification(
            primaryBehavior = primaryBehavior,
            supportingBehaviors = distinctSupportingBehaviors,
            summary = summary,
            observations = observations
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2000
    SHAPE HEURISTICS
    ------------------------------------------------------------
    PURPOSE
    Encapsulates the first-pass shape rules used by the classifier.

    SAFE EDIT AREA
    - tune thresholds based on real hardware data
    - replace heuristic cutoffs with validated engineering ranges later
    ------------------------------------------------------------
    */
    private fun isHighQNarrowResonance(
        minimumSwr: Double,
        bandwidthMHz: Double?
    ): Boolean {
        if (bandwidthMHz == null) return false
        return minimumSwr <= 1.5 && bandwidthMHz <= 0.08
    }

    private fun isBroadLossyResponse(
        minimumSwr: Double,
        bandwidthMHz: Double?
    ): Boolean {
        if (bandwidthMHz == null) return false
        return minimumSwr > 1.5 && bandwidthMHz >= 0.20
    }

    private fun isFlatPoorMatch(
        points: List<SweepPoint>,
        swrSpan: Double,
        minimumSwr: Double
    ): Boolean {
        if (points.size < 4) return false
        return swrSpan <= 0.35 && minimumSwr >= 2.2
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2100
    BANDWIDTH HELPER
    ------------------------------------------------------------
    PURPOSE
    Estimates continuous bandwidth where SWR remains at or below a given
    threshold.

    SAFE EDIT AREA
    - later support multiple separate bandwidth islands
    - later support interpolation at threshold crossing
    ------------------------------------------------------------
    */
    private fun estimateBandwidthAtOrBelowSWR(
        points: List<SweepPoint>,
        threshold: Double
    ): Double? {
        val inBand = points.filter { it.swr <= threshold }
        if (inBand.isEmpty()) return null

        val start = inBand.minOf { it.frequencyMHz }
        val end = inBand.maxOf { it.frequencyMHz }
        return end - start
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2200
    REGION HELPER
    ------------------------------------------------------------
    PURPOSE
    Detects how many separate low-SWR regions exist across the sweep.

    SAFE EDIT AREA
    - later merge very close regions using frequency distance rules
    - later detect trap-like signatures with better valley analysis
    ------------------------------------------------------------
    */
    private fun countLowSwrRegions(
        points: List<SweepPoint>,
        threshold: Double
    ): Int {
        var regionCount = 0
        var insideRegion = false

        points.forEach { point ->
            val inRegion = point.swr <= threshold

            if (inRegion && !insideRegion) {
                regionCount += 1
            }

            insideRegion = inRegion
        }

        return regionCount
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2300
    REACTANCE HELPER
    ------------------------------------------------------------
    PURPOSE
    Counts reactance sign reversals across the sweep.

    SAFE EDIT AREA
    - later ignore tiny sign flips near zero with adaptive thresholds
    ------------------------------------------------------------
    */
    private fun countReactanceSignChanges(
        points: List<SweepPoint>
    ): Int {
        if (points.size < 2) return 0

        var signChanges = 0
        var previousSign = signOf(points.first().reactance)

        points.drop(1).forEach { point ->
            val currentSign = signOf(point.reactance)

            if (currentSign != 0 && previousSign != 0 && currentSign != previousSign) {
                signChanges += 1
            }

            if (currentSign != 0) {
                previousSign = currentSign
            }
        }

        return signChanges
    }

    private fun signOf(
        value: Double
    ): Int {
        return when {
            value > 0.5 -> 1
            value < -0.5 -> -1
            else -> 0
        }
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 3000
    SUMMARY HELPER
    ------------------------------------------------------------
    PURPOSE
    Produces a short reusable human-readable classification summary.
    ------------------------------------------------------------
    */
    private fun buildSummary(
        primaryBehavior: AntennaBehaviorType,
        supportingBehaviors: List<AntennaBehaviorType>,
        targetFrequencyMHz: Double,
        resonanceFrequencyMHz: Double
    ): String {
        val primaryText = when (primaryBehavior) {
            AntennaBehaviorType.HIGH_Q_NARROW_RESONANCE ->
                "Primary behaviour: narrow high-Q resonance."

            AntennaBehaviorType.BROAD_LOSSY_RESPONSE ->
                "Primary behaviour: broad shallow response that may indicate loss."

            AntennaBehaviorType.MULTIPLE_RESONANCE_CANDIDATES ->
                "Primary behaviour: multiple resonance candidates detected."

            AntennaBehaviorType.RESONANCE_BELOW_TARGET ->
                "Primary behaviour: resonance below target frequency."

            AntennaBehaviorType.RESONANCE_ABOVE_TARGET ->
                "Primary behaviour: resonance above target frequency."

            AntennaBehaviorType.FLAT_POOR_MATCH ->
                "Primary behaviour: flat poor match."

            AntennaBehaviorType.FEEDLINE_INTERACTION_SUSPECTED ->
                "Primary behaviour: feedline or interaction effects suspected."

            AntennaBehaviorType.IMPEDANCE_UNSTABLE ->
                "Primary behaviour: unstable impedance movement."

            AntennaBehaviorType.GENERALLY_WELL_MATCHED ->
                "Primary behaviour: generally well matched near target."

            AntennaBehaviorType.LIKELY_TOO_SHORT ->
                "Primary behaviour: antenna likely too short for target frequency."

            AntennaBehaviorType.LIKELY_TOO_LONG ->
                "Primary behaviour: antenna likely too long for target frequency."

            AntennaBehaviorType.HIGH_LOSS_OR_WEAK_RADIATION ->
                "Primary behaviour: high loss or weak radiation suspected."

            AntennaBehaviorType.COMPLEX_MULTI_RESONANCE ->
                "Primary behaviour: complex multi-resonance response detected."

            AntennaBehaviorType.INCONCLUSIVE ->
                "Primary behaviour: inconclusive."
        }

        val targetText = String.format(
            " Target %.3f MHz, detected resonance %.3f MHz.",
            targetFrequencyMHz,
            resonanceFrequencyMHz
        )

        val supportText =
            if (supportingBehaviors.isEmpty()) {
                ""
            } else {
                " Supporting indicators: " +
                        supportingBehaviors.joinToString(", ") { it.name }
            }

        return primaryText + targetText + supportText
    }
}

/*
------------------------------------------------------------
END EDIT SECTIONS 3999
------------------------------------------------------------
*/