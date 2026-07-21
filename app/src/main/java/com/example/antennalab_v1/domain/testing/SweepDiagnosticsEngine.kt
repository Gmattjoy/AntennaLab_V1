package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.testing.InstrumentError
import com.example.antennalab_v1.model.testing.InstrumentErrorCategory
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import kotlin.math.abs

/*
------------------------------------------------------------
EDIT SECTION 1000
FILE HEADER
------------------------------------------------------------
FILE: SweepDiagnosticsEngine.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing Analysis

LAST UPDATED 18/3/2026 02:28

SYSTEM ROLE
Converts raw sweep data into higher-level antenna diagnostics.

CURRENT DEVELOPMENT ROLE
Provides a structured interpretation layer for sweep measurements so
testing tools, tuning systems, and future engineering dashboards can
reuse the same analysis outputs.

CURRENT DIAGNOSTIC GOALS
• resonance detection
• minimum SWR detection
• bandwidth estimates
• matching quality summary
• impedance stability summary
• sweep shape interpretation
• likely condition hints

ARCHITECTURE ROLE (UPDATED)
Now carries a structured diagnostics error field for empty or unusable
sweep input so analysis failures are no longer represented only by
strings.

SAFE EDIT AREA
- add richer RF diagnostics later
- add multi-resonance refinement later
- add hardware-specific interpretation later
------------------------------------------------------------
*/

/*
------------------------------------------------------------
EDIT SECTION 1100
DIAGNOSTICS MODEL
------------------------------------------------------------
PURPOSE
Defines the stable diagnostics output model and enum classifications
used by sweep analysis.

SAFE EDIT AREA
- add more metrics later
- keep defaults safe for old callers
------------------------------------------------------------
*/
data class SweepDiagnostics(
    val hasUsableData: Boolean = false,
    val minimumSwr: Double = 0.0,
    val minimumSwrFrequencyMHz: Double = 0.0,
    val resonanceFrequencyMHz: Double = 0.0,
    val secondaryResonanceFrequencyMHz: Double? = null,
    val estimatedBandwidthMHz: Double = 0.0,
    val estimatedBandwidthAt15MHz: Double = 0.0,
    val matchingQuality: MatchingQuality = MatchingQuality.UNKNOWN,
    val impedanceStability: ImpedanceStability = ImpedanceStability.UNKNOWN,
    val sweepShape: SweepShape = SweepShape.UNKNOWN,
    val reactanceTrend: ReactanceTrend = ReactanceTrend.UNKNOWN,
    val resonanceCountEstimate: Int = 0,
    val mismatchSeverity: MismatchSeverity = MismatchSeverity.UNKNOWN,
    val likelyCondition: LikelyCondition = LikelyCondition.UNKNOWN,
    val feedlineLossSuspicion: FeedlineLossSuspicion = FeedlineLossSuspicion.UNKNOWN,
    val summary: String = "No sweep diagnostics available.",
    val error: InstrumentError? = null
)

enum class MatchingQuality {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    UNKNOWN
}

enum class ImpedanceStability {
    STABLE,
    MODERATE,
    UNSTABLE,
    UNKNOWN
}

enum class SweepShape {
    SHARP_SINGLE_DIP,
    BROAD_SINGLE_DIP,
    MULTIPLE_DIPS,
    FLAT_RESPONSE,
    WEAK_COUPLING,
    IRREGULAR,
    UNKNOWN
}

enum class ReactanceTrend {
    MOSTLY_INDUCTIVE,
    MOSTLY_CAPACITIVE,
    CROSSES_RESONANCE,
    MIXED,
    UNKNOWN
}

enum class MismatchSeverity {
    LOW,
    MODERATE,
    HIGH,
    EXTREME,
    UNKNOWN
}

enum class LikelyCondition {
    CLOSE_TO_TARGET,
    LIKELY_TOO_SHORT,
    LIKELY_TOO_LONG,
    HIGH_LOSS_OR_WEAK_RADIATION,
    COMPLEX_MULTI_RESONANCE,
    UNKNOWN
}

enum class FeedlineLossSuspicion {
    LOW,
    MODERATE,
    HIGH,
    UNKNOWN
}

/*
------------------------------------------------------------
EDIT SECTION 1200
ENGINE OBJECT
------------------------------------------------------------
PURPOSE
Primary diagnostics entry point for converting a sweep result into a
stable diagnostics report.
------------------------------------------------------------
*/
object SweepDiagnosticsEngine {

    /*
    ------------------------------------------------------------
    EDIT SECTION 1201
    ANALYZE ENTRY
    ------------------------------------------------------------
    PURPOSE
    Runs the current diagnostics pipeline and returns a stable typed
    report for UI and analysis systems.
    ------------------------------------------------------------
    */
    fun analyzeSweep(result: SweepResult): SweepDiagnostics {
        if (result.points.isEmpty()) {
            return SweepDiagnostics(
                error = InstrumentError(
                    category = InstrumentErrorCategory.SWEEP,
                    code = "SWEEP_DIAGNOSTICS_NO_POINTS",
                    summary = "Sweep diagnostics could not run because the sweep contained no points.",
                    detail = "Diagnostics requires at least one sweep point.",
                    recoverable = true
                )
            )
        }

        val minimumSwrPoint = findMinimumSwrPoint(result.points)
        val resonancePoint = findResonancePoint(result.points)
        val secondaryResonancePoint = findSecondaryResonancePoint(
            points = result.points,
            primaryResonancePoint = resonancePoint
        )

        val bandwidthAt20MHz = estimateBandwidthMHz(result.points, maxSwr = 2.0)
        val bandwidthAt15MHz = estimateBandwidthMHz(result.points, maxSwr = 1.5)
        val matchingQuality = classifyMatchingQuality(minimumSwrPoint?.swr ?: 0.0)
        val impedanceStability = classifyImpedanceStability(result.points)
        val sweepShape = classifySweepShape(result.points)
        val reactanceTrend = classifyReactanceTrend(result.points)
        val resonanceCountEstimate = estimateResonanceCount(result.points)
        val mismatchSeverity = classifyMismatchSeverity(minimumSwrPoint?.swr ?: 0.0)
        val likelyCondition = classifyLikelyCondition(
            points = result.points,
            minimumSwrPoint = minimumSwrPoint,
            resonancePoint = resonancePoint,
            sweepShape = sweepShape,
            reactanceTrend = reactanceTrend
        )
        val feedlineLossSuspicion = classifyFeedlineLossSuspicion(
            points = result.points,
            minimumSwrPoint = minimumSwrPoint,
            bandwidthMHz = bandwidthAt20MHz
        )

        return SweepDiagnostics(
            hasUsableData = true,
            minimumSwr = minimumSwrPoint?.swr ?: 0.0,
            minimumSwrFrequencyMHz = minimumSwrPoint?.frequencyMHz ?: 0.0,
            resonanceFrequencyMHz = resonancePoint?.frequencyMHz ?: 0.0,
            secondaryResonanceFrequencyMHz = secondaryResonancePoint?.frequencyMHz,
            estimatedBandwidthMHz = bandwidthAt20MHz,
            estimatedBandwidthAt15MHz = bandwidthAt15MHz,
            matchingQuality = matchingQuality,
            impedanceStability = impedanceStability,
            sweepShape = sweepShape,
            reactanceTrend = reactanceTrend,
            resonanceCountEstimate = resonanceCountEstimate,
            mismatchSeverity = mismatchSeverity,
            likelyCondition = likelyCondition,
            feedlineLossSuspicion = feedlineLossSuspicion,
            summary = buildSummary(
                minimumSwrPoint = minimumSwrPoint,
                resonancePoint = resonancePoint,
                secondaryResonancePoint = secondaryResonancePoint,
                bandwidthMHz = bandwidthAt20MHz,
                bandwidthAt15MHz = bandwidthAt15MHz,
                matchingQuality = matchingQuality,
                impedanceStability = impedanceStability,
                sweepShape = sweepShape,
                reactanceTrend = reactanceTrend,
                resonanceCountEstimate = resonanceCountEstimate,
                mismatchSeverity = mismatchSeverity,
                likelyCondition = likelyCondition,
                feedlineLossSuspicion = feedlineLossSuspicion
            ),
            error = null
        )
    }
}

/*
------------------------------------------------------------
EDIT SECTION 2000
CORE SEARCH HELPERS
------------------------------------------------------------
PURPOSE
Finds the key points, resonance candidates, and bandwidth estimates from
sweep data.
------------------------------------------------------------
*/
private fun findMinimumSwrPoint(points: List<SweepPoint>): SweepPoint? {
    return points.minByOrNull { it.swr }
}

private fun findResonancePoint(points: List<SweepPoint>): SweepPoint? {
    return points.minByOrNull { abs(it.reactance) }
}

private fun findSecondaryResonancePoint(
    points: List<SweepPoint>,
    primaryResonancePoint: SweepPoint?
): SweepPoint? {
    if (points.size < 5 || primaryResonancePoint == null) {
        return null
    }

    val frequencySeparationThresholdMHz = estimateFrequencyStepMHz(points) * 3.0

    return points
        .filter { point ->
            abs(point.frequencyMHz - primaryResonancePoint.frequencyMHz) >=
                    frequencySeparationThresholdMHz
        }
        .minByOrNull { abs(it.reactance) }
        ?.takeIf { abs(it.reactance) <= 25.0 }
}

private fun estimateBandwidthMHz(
    points: List<SweepPoint>,
    maxSwr: Double
): Double {
    val insideBandwidth = points.filter { it.swr <= maxSwr }

    if (insideBandwidth.isEmpty()) {
        return 0.0
    }

    val start = insideBandwidth.first().frequencyMHz
    val end = insideBandwidth.last().frequencyMHz

    return (end - start).coerceAtLeast(0.0)
}

private fun estimateFrequencyStepMHz(points: List<SweepPoint>): Double {
    if (points.size < 2) {
        return 0.0
    }

    val sortedPoints = points.sortedBy { it.frequencyMHz }
    return (sortedPoints[1].frequencyMHz - sortedPoints[0].frequencyMHz)
        .coerceAtLeast(0.0)
}

private fun estimateResonanceCount(points: List<SweepPoint>): Int {
    if (points.size < 3) {
        return 0
    }

    var count = 0

    for (index in 1 until points.lastIndex) {
        val previousPoint = points[index - 1]
        val currentPoint = points[index]
        val nextPoint = points[index + 1]

        val isLocalSwrDip = currentPoint.swr <= previousPoint.swr &&
                currentPoint.swr <= nextPoint.swr &&
                currentPoint.swr <= 3.0

        val isNearReactanceNull = abs(currentPoint.reactance) <= 20.0

        if (isLocalSwrDip && isNearReactanceNull) {
            count++
        }
    }

    return count
}

/*
------------------------------------------------------------
EDIT SECTION 3000
CLASSIFICATION HELPERS
------------------------------------------------------------
PURPOSE
Classifies matching quality, sweep behaviour, resonance character, and
first-stage engineering hints from sweep data.
------------------------------------------------------------
*/
private fun classifyMatchingQuality(minimumSwr: Double): MatchingQuality {
    return when {
        minimumSwr <= 1.2 -> MatchingQuality.EXCELLENT
        minimumSwr <= 1.5 -> MatchingQuality.GOOD
        minimumSwr <= 2.0 -> MatchingQuality.FAIR
        minimumSwr > 2.0 -> MatchingQuality.POOR
        else -> MatchingQuality.UNKNOWN
    }
}

private fun classifyImpedanceStability(
    points: List<SweepPoint>
): ImpedanceStability {
    if (points.size < 2) {
        return ImpedanceStability.UNKNOWN
    }

    val resistanceSpread = points.maxOf { it.resistance } - points.minOf { it.resistance }
    val reactanceSpread = points.maxOf { it.reactance } - points.minOf { it.reactance }
    val combinedSpread = resistanceSpread + reactanceSpread

    return when {
        combinedSpread <= 40.0 -> ImpedanceStability.STABLE
        combinedSpread <= 120.0 -> ImpedanceStability.MODERATE
        combinedSpread > 120.0 -> ImpedanceStability.UNSTABLE
        else -> ImpedanceStability.UNKNOWN
    }
}

private fun classifySweepShape(points: List<SweepPoint>): SweepShape {
    if (points.size < 3) {
        return SweepShape.UNKNOWN
    }

    val minimumPoint = findMinimumSwrPoint(points) ?: return SweepShape.UNKNOWN
    val minimumSwr = minimumPoint.swr
    val maximumSwr = points.maxOf { it.swr }
    val bandwidthAt20MHz = estimateBandwidthMHz(points, maxSwr = 2.0)
    val resonanceCount = estimateResonanceCount(points)

    if ((maximumSwr - minimumSwr) < 0.35) {
        return SweepShape.FLAT_RESPONSE
    }

    if (minimumSwr > 3.0) {
        return SweepShape.WEAK_COUPLING
    }

    if (resonanceCount >= 2) {
        return SweepShape.MULTIPLE_DIPS
    }

    return when {
        bandwidthAt20MHz <= estimateFrequencyStepMHz(points) * 4.0 ->
            SweepShape.SHARP_SINGLE_DIP
        bandwidthAt20MHz > estimateFrequencyStepMHz(points) * 4.0 ->
            SweepShape.BROAD_SINGLE_DIP
        else -> SweepShape.IRREGULAR
    }
}

private fun classifyReactanceTrend(points: List<SweepPoint>): ReactanceTrend {
    if (points.isEmpty()) {
        return ReactanceTrend.UNKNOWN
    }

    val inductiveCount = points.count { it.reactance > 5.0 }
    val capacitiveCount = points.count { it.reactance < -5.0 }
    val nearZeroCount = points.count { abs(it.reactance) <= 5.0 }

    val hasPositive = points.any { it.reactance > 0.0 }
    val hasNegative = points.any { it.reactance < 0.0 }

    if (hasPositive && hasNegative && nearZeroCount > 0) {
        return ReactanceTrend.CROSSES_RESONANCE
    }

    return when {
        inductiveCount > capacitiveCount * 2 -> ReactanceTrend.MOSTLY_INDUCTIVE
        capacitiveCount > inductiveCount * 2 -> ReactanceTrend.MOSTLY_CAPACITIVE
        inductiveCount > 0 || capacitiveCount > 0 -> ReactanceTrend.MIXED
        else -> ReactanceTrend.UNKNOWN
    }
}

private fun classifyMismatchSeverity(minimumSwr: Double): MismatchSeverity {
    return when {
        minimumSwr <= 1.5 -> MismatchSeverity.LOW
        minimumSwr <= 2.0 -> MismatchSeverity.MODERATE
        minimumSwr <= 3.0 -> MismatchSeverity.HIGH
        minimumSwr > 3.0 -> MismatchSeverity.EXTREME
        else -> MismatchSeverity.UNKNOWN
    }
}

private fun classifyLikelyCondition(
    points: List<SweepPoint>,
    minimumSwrPoint: SweepPoint?,
    resonancePoint: SweepPoint?,
    sweepShape: SweepShape,
    reactanceTrend: ReactanceTrend
): LikelyCondition {
    if (points.isEmpty() || minimumSwrPoint == null || resonancePoint == null) {
        return LikelyCondition.UNKNOWN
    }

    if (sweepShape == SweepShape.MULTIPLE_DIPS) {
        return LikelyCondition.COMPLEX_MULTI_RESONANCE
    }

    if (sweepShape == SweepShape.WEAK_COUPLING || sweepShape == SweepShape.FLAT_RESPONSE) {
        return LikelyCondition.HIGH_LOSS_OR_WEAK_RADIATION
    }

    val lowerFrequency = points.minOf { it.frequencyMHz }
    val upperFrequency = points.maxOf { it.frequencyMHz }
    val midpointFrequency = (lowerFrequency + upperFrequency) / 2.0

    if (minimumSwrPoint.swr <= 1.5 && abs(resonancePoint.reactance) <= 10.0) {
        return LikelyCondition.CLOSE_TO_TARGET
    }

    return when {
        resonancePoint.frequencyMHz < midpointFrequency &&
                reactanceTrend == ReactanceTrend.MOSTLY_CAPACITIVE ->
            LikelyCondition.LIKELY_TOO_LONG

        resonancePoint.frequencyMHz > midpointFrequency &&
                reactanceTrend == ReactanceTrend.MOSTLY_INDUCTIVE ->
            LikelyCondition.LIKELY_TOO_SHORT

        else -> LikelyCondition.UNKNOWN
    }
}

private fun classifyFeedlineLossSuspicion(
    points: List<SweepPoint>,
    minimumSwrPoint: SweepPoint?,
    bandwidthMHz: Double
): FeedlineLossSuspicion {
    if (points.isEmpty() || minimumSwrPoint == null) {
        return FeedlineLossSuspicion.UNKNOWN
    }

    val swrSpread = points.maxOf { it.swr } - points.minOf { it.swr }
    val fullSpanMHz = points.maxOf { it.frequencyMHz } - points.minOf { it.frequencyMHz }

    return when {
        minimumSwrPoint.swr > 2.5 && swrSpread < 0.5 ->
            FeedlineLossSuspicion.HIGH

        minimumSwrPoint.swr > 2.0 &&
                fullSpanMHz > 0.0 &&
                bandwidthMHz >= fullSpanMHz * 0.6 ->
            FeedlineLossSuspicion.MODERATE

        minimumSwrPoint.swr <= 1.8 ->
            FeedlineLossSuspicion.LOW

        else -> FeedlineLossSuspicion.UNKNOWN
    }
}

/*
------------------------------------------------------------
EDIT SECTION 4000
SUMMARY BUILDER
------------------------------------------------------------
PURPOSE
Produces a richer human-readable summary for UI and future tuning
systems.
------------------------------------------------------------
*/
private fun buildSummary(
    minimumSwrPoint: SweepPoint?,
    resonancePoint: SweepPoint?,
    secondaryResonancePoint: SweepPoint?,
    bandwidthMHz: Double,
    bandwidthAt15MHz: Double,
    matchingQuality: MatchingQuality,
    impedanceStability: ImpedanceStability,
    sweepShape: SweepShape,
    reactanceTrend: ReactanceTrend,
    resonanceCountEstimate: Int,
    mismatchSeverity: MismatchSeverity,
    likelyCondition: LikelyCondition,
    feedlineLossSuspicion: FeedlineLossSuspicion
): String {
    if (minimumSwrPoint == null || resonancePoint == null) {
        return "No sweep diagnostics available."
    }

    return buildString {
        append(
            String.format(
                "Minimum SWR %.3f at %.3f MHz. ",
                minimumSwrPoint.swr,
                minimumSwrPoint.frequencyMHz
            )
        )

        append(
            String.format(
                "Primary resonance estimate near %.3f MHz. ",
                resonancePoint.frequencyMHz
            )
        )

        if (secondaryResonancePoint != null) {
            append(
                String.format(
                    "Secondary resonance estimate near %.3f MHz. ",
                    secondaryResonancePoint.frequencyMHz
                )
            )
        }

        append(
            String.format(
                "Estimated SWR≤2.0 bandwidth %.3f MHz. ",
                bandwidthMHz
            )
        )

        append(
            String.format(
                "Estimated SWR≤1.5 bandwidth %.3f MHz. ",
                bandwidthAt15MHz
            )
        )

        append("Matching quality: ${matchingQuality.name}. ")
        append("Impedance stability: ${impedanceStability.name}. ")
        append("Sweep shape: ${sweepShape.name}. ")
        append("Reactance trend: ${reactanceTrend.name}. ")
        append("Resonance count estimate: $resonanceCountEstimate. ")
        append("Mismatch severity: ${mismatchSeverity.name}. ")
        append("Likely condition: ${likelyCondition.name}. ")
        append("Feedline loss suspicion: ${feedlineLossSuspicion.name}.")
    }
}

/*
------------------------------------------------------------
END EDIT SECTIONS 4999
------------------------------------------------------------
*/