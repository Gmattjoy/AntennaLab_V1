package com.example.antennalab_v1.features.testing

import com.example.antennalab_v1.model.HardwareMeasurementCapabilities
import com.example.antennalab_v1.model.TestHardwareProfile
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/*
########################################################################
FILE: SweepGraphMath.kt
PACKAGE: com.example.antennalab_v1.features.testing
LAYER: UI / Testing Tools / Graph Math

LAST UPDATED 14/3/2026 21:50

SYSTEM ROLE
Provides pure sweep graph helper math for screen rendering and marker
position calculations.

CURRENT DEVELOPMENT ROLE
This file is the first safe extraction point from SweepGraphScreen.kt.

It intentionally contains only stateless math helpers so it can be added
without changing runtime behaviour or moving acquisition logic.

IMPORTANT ARCHITECTURE RULE
This file must stay focused on:

• graph coordinate mapping
• numeric clamping
• safe range handling
• interpolation helpers
• marker position math

This file must NOT contain:

• sweep acquisition logic
• USB/device logic
• Compose UI
• project state management

SAFE EDIT AREA
- add more graph helpers later
- add marker interpolation helpers later
- add axis label helpers later
- add viewport helpers later
########################################################################
*/

/*
########################################################################
SECTION 1
BASIC RANGE HELPERS
########################################################################
PURPOSE
Provides safe numeric helpers used by graph rendering code.
########################################################################
*/

internal fun clampFloat(
    value: Float,
    minimum: Float,
    maximum: Float
): Float {
    return value.coerceIn(minimum, maximum)
}

internal fun clampDouble(
    value: Double,
    minimum: Double,
    maximum: Double
): Double {
    return value.coerceIn(minimum, maximum)
}

internal fun safeFloatRange(
    minimum: Float,
    maximum: Float,
    fallback: Float = 1f
): Float {
    val range = maximum - minimum
    return if (range == 0f) fallback else range
}

internal fun safeDoubleRange(
    minimum: Double,
    maximum: Double,
    fallback: Double = 1.0
): Double {
    val range = maximum - minimum
    return if (range == 0.0) fallback else range
}

/*
########################################################################
SECTION 2
NORMALISATION HELPERS
########################################################################
PURPOSE
Converts values into 0..1 graph space safely.
########################################################################
*/

internal fun normaliseFloat(
    value: Float,
    minimum: Float,
    maximum: Float
): Float {
    val range = safeFloatRange(minimum, maximum)
    return (value - minimum) / range
}

internal fun normaliseDouble(
    value: Double,
    minimum: Double,
    maximum: Double
): Double {
    val range = safeDoubleRange(minimum, maximum)
    return (value - minimum) / range
}

/*
########################################################################
SECTION 3
CANVAS POSITION HELPERS
########################################################################
PURPOSE
Maps normalised values into canvas X/Y coordinates.

Y mapping is inverted because screen coordinates grow downward.
########################################################################
*/

internal fun toCanvasX(
    value: Float,
    minimum: Float,
    maximum: Float,
    width: Float
): Float {
    val normalised = clampFloat(
        value = normaliseFloat(value, minimum, maximum),
        minimum = 0f,
        maximum = 1f
    )
    return normalised * width
}

internal fun toCanvasY(
    value: Float,
    minimum: Float,
    maximum: Float,
    height: Float
): Float {
    val normalised = clampFloat(
        value = normaliseFloat(value, minimum, maximum),
        minimum = 0f,
        maximum = 1f
    )
    return height - (normalised * height)
}

internal fun toCanvasXDouble(
    value: Double,
    minimum: Double,
    maximum: Double,
    width: Float
): Float {
    val normalised = clampDouble(
        value = normaliseDouble(value, minimum, maximum),
        minimum = 0.0,
        maximum = 1.0
    )
    return (normalised * width.toDouble()).toFloat()
}

internal fun toCanvasYDouble(
    value: Double,
    minimum: Double,
    maximum: Double,
    height: Float
): Float {
    val normalised = clampDouble(
        value = normaliseDouble(value, minimum, maximum),
        minimum = 0.0,
        maximum = 1.0
    )
    return (height.toDouble() - (normalised * height.toDouble())).toFloat()
}

/*
########################################################################
SECTION 4
LINEAR INTERPOLATION HELPERS
########################################################################
PURPOSE
Supports marker and graph calculations when future smoothing or cursor
sampling is added.
########################################################################
*/

internal fun lerpFloat(
    start: Float,
    end: Float,
    fraction: Float
): Float {
    return start + ((end - start) * fraction)
}

internal fun lerpDouble(
    start: Double,
    end: Double,
    fraction: Double
): Double {
    return start + ((end - start) * fraction)
}

/*
########################################################################
SECTION 5
INDEX HELPERS
########################################################################
PURPOSE
Provides safe helpers for converting a position or ratio into a list
index for marker placement logic.
########################################################################
*/

internal fun ratioToIndex(
    ratio: Float,
    itemCount: Int
): Int {
    if (itemCount <= 0) return 0
    val clampedRatio = clampFloat(ratio, 0f, 1f)
    val rawIndex = (clampedRatio * (itemCount - 1)).toInt()
    return rawIndex.coerceIn(0, itemCount - 1)
}

internal fun xToIndex(
    x: Float,
    width: Float,
    itemCount: Int
): Int {
    if (width <= 0f) return 0
    return ratioToIndex(
        ratio = x / width,
        itemCount = itemCount
    )
}

/*
########################################################################
SECTION 6
MIDPOINT HELPERS
########################################################################
PURPOSE
Useful for future marker delta displays and graph annotations.
########################################################################
*/

internal fun midpointFloat(
    first: Float,
    second: Float
): Float {
    return (first + second) / 2f
}

internal fun midpointDouble(
    first: Double,
    second: Double
): Double {
    return (first + second) / 2.0
}

/*
########################################################################
SECTION 7
TRACE VALUES
########################################################################
PURPOSE
Per-point display value selection (moved from SweepGraphWidgets) plus the
current-vs-reference difference series. Single source of truth for the
value shown per SweepDisplayMode.
########################################################################
*/

internal fun getDisplayValue(
    point: SweepPoint,
    mode: SweepDisplayMode
): Double =
    when (mode) {
        SweepDisplayMode.SWR,
        SweepDisplayMode.ANALOG_SWR,
        SweepDisplayMode.WATERFALL -> point.swr

        SweepDisplayMode.RETURN_LOSS,
        SweepDisplayMode.ANALOG_RETURN_LOSS -> point.returnLossDb

        SweepDisplayMode.RESISTANCE,
        SweepDisplayMode.ANALOG_RESISTANCE -> point.resistance

        SweepDisplayMode.REACTANCE,
        SweepDisplayMode.ANALOG_REACTANCE -> point.reactance

        SweepDisplayMode.S21_ESTIMATE -> estimateS21Db(point)
        SweepDisplayMode.SMITH -> point.swr
        SweepDisplayMode.IMPEDANCE_LOCUS -> point.swr
    }

internal fun estimateS21Db(
    point: SweepPoint
): Double {
    return -abs(point.returnLossDb * 0.35)
}

internal fun buildDifferenceValues(
    currentValues: List<Double>,
    referenceValues: List<Double>
): List<Double> {
    if (currentValues.isEmpty()) {
        return emptyList()
    }

    if (referenceValues.isEmpty()) {
        return currentValues
    }

    val sharedSize = min(currentValues.size, referenceValues.size)
    if (sharedSize <= 0) {
        return currentValues
    }

    return List(sharedSize) { index ->
        currentValues[index] - referenceValues[index]
    }
}

/*
########################################################################
SECTION 8
AXIS BOUNDS AND SCALE
########################################################################
PURPOSE
Per-mode trace axis bounds and the instrument-style scale rounding used
to size the graph's Y axis.
########################################################################
*/

internal data class TraceAxisBounds(
    val minimum: Double,
    val maximum: Double,
    val range: Double
)

internal fun buildTraceAxisBounds(
    mode: SweepDisplayMode,
    traceCompareMode: TraceCompareMode,
    currentValues: List<Double>,
    referenceValues: List<Double>,
    differenceValues: List<Double>
): TraceAxisBounds {
    val sourceValues =
        when (traceCompareMode) {
            TraceCompareMode.CURRENT_ONLY -> currentValues
            TraceCompareMode.CURRENT_PLUS_REFERENCE ->
                if (referenceValues.isNotEmpty()) currentValues + referenceValues else currentValues
            TraceCompareMode.DIFFERENCE ->
                if (differenceValues.isNotEmpty()) differenceValues else currentValues
        }

    if (sourceValues.isEmpty()) {
        return TraceAxisBounds(0.0, 1.0, 1.0)
    }

    return when (traceCompareMode) {
        TraceCompareMode.DIFFERENCE -> {
            val peakAbs = sourceValues.maxOf { abs(it) }.coerceAtLeast(0.1)
            val roundedPeak = roundUpForInstrumentScale(peakAbs)
            TraceAxisBounds(
                minimum = -roundedPeak,
                maximum = roundedPeak,
                range = (roundedPeak * 2.0).coerceAtLeast(0.0001)
            )
        }

        else -> {
            when (mode) {
                SweepDisplayMode.SWR -> {
                    val maxValue = roundUpForInstrumentScale(
                        max(2.0, sourceValues.maxOrNull() ?: 2.0)
                    )
                    TraceAxisBounds(
                        minimum = 1.0,
                        maximum = maxValue,
                        range = (maxValue - 1.0).coerceAtLeast(0.0001)
                    )
                }

                SweepDisplayMode.RETURN_LOSS,
                SweepDisplayMode.S21_ESTIMATE -> {
                    val maxValue = roundUpForInstrumentScale(
                        max(5.0, sourceValues.maxOrNull() ?: 5.0)
                    )
                    val minValue = min(0.0, sourceValues.minOrNull() ?: 0.0)
                    TraceAxisBounds(
                        minimum = minValue,
                        maximum = maxValue,
                        range = (maxValue - minValue).coerceAtLeast(0.0001)
                    )
                }

                SweepDisplayMode.RESISTANCE -> {
                    val maxValue = roundUpForInstrumentScale(
                        max(50.0, sourceValues.maxOrNull() ?: 50.0)
                    )
                    TraceAxisBounds(
                        minimum = 0.0,
                        maximum = maxValue,
                        range = maxValue.coerceAtLeast(0.0001)
                    )
                }

                SweepDisplayMode.REACTANCE -> {
                    val peakAbs = sourceValues.maxOf { abs(it) }.coerceAtLeast(10.0)
                    val roundedPeak = roundUpForInstrumentScale(peakAbs)
                    TraceAxisBounds(
                        minimum = -roundedPeak,
                        maximum = roundedPeak,
                        range = (roundedPeak * 2.0).coerceAtLeast(0.0001)
                    )
                }

                else -> {
                    val maxValue = roundUpForInstrumentScale(sourceValues.maxOrNull() ?: 1.0)
                    val minValue = sourceValues.minOrNull() ?: 0.0
                    TraceAxisBounds(
                        minimum = minValue,
                        maximum = maxValue,
                        range = (maxValue - minValue).coerceAtLeast(0.0001)
                    )
                }
            }
        }
    }
}

internal fun roundUpForInstrumentScale(
    value: Double
): Double {
    return when {
        value <= 1.0 -> 1.0
        value <= 2.0 -> 2.0
        value <= 5.0 -> 5.0
        value <= 10.0 -> 10.0
        value <= 20.0 -> 20.0
        value <= 50.0 -> 50.0
        value <= 100.0 -> 100.0
        value <= 200.0 -> 200.0
        else -> {
            ((value / 100.0).toInt() + 1) * 100.0
        }
    }
}

/*
########################################################################
SECTION 9
AXIS LABELS, TICKS, AND TITLES
########################################################################
PURPOSE
Formatting of Y-axis labels, X-axis frequency ticks, and the trace
axis title / compare-mode summary strings.
########################################################################
*/

internal fun buildAxisLabels(
    maxValue: Double,
    minValue: Double,
    count: Int
): List<String> {
    if (count <= 1) {
        return listOf(formatAxisLabel(maxValue))
    }

    return List(count) { index ->
        val fraction = index.toDouble() / (count - 1).toDouble()
        val value = maxValue - ((maxValue - minValue) * fraction)
        formatAxisLabel(value)
    }
}

internal fun formatAxisLabel(
    value: Double
): String {
    return when {
        abs(value) >= 100.0 -> String.format("%.0f", value)
        abs(value) >= 10.0 -> String.format("%.1f", value)
        else -> String.format("%.2f", value)
    }
}

internal fun buildFrequencyTicks(
    startMHz: Double,
    endMHz: Double
): List<String> {
    val span = endMHz - startMHz
    return listOf(
        String.format("%.2f", startMHz),
        String.format("%.2f", startMHz + (span * 0.25)),
        String.format("%.2f", startMHz + (span * 0.50)),
        String.format("%.2f", startMHz + (span * 0.75)),
        String.format("%.2f", endMHz)
    )
}

internal fun getTraceAxisTitle(
    mode: SweepDisplayMode,
    traceCompareMode: TraceCompareMode
): String {
    return when (traceCompareMode) {
        TraceCompareMode.DIFFERENCE -> {
            when (mode) {
                SweepDisplayMode.SWR -> "Δ SWR"
                SweepDisplayMode.RETURN_LOSS -> "Δ Return Loss (dB)"
                SweepDisplayMode.RESISTANCE -> "Δ Resistance (Ω)"
                SweepDisplayMode.REACTANCE -> "Δ Reactance (Ω)"
                SweepDisplayMode.S21_ESTIMATE -> "Δ S21 Estimate (dB)"
                else -> "Δ Trace"
            }
        }

        else -> {
            when (mode) {
                SweepDisplayMode.SWR -> "SWR"
                SweepDisplayMode.RETURN_LOSS -> "Return Loss (dB)"
                SweepDisplayMode.RESISTANCE -> "Resistance (Ω)"
                SweepDisplayMode.REACTANCE -> "Reactance (Ω)"
                SweepDisplayMode.S21_ESTIMATE -> "S21 Estimate (dB)"
                else -> "Trace"
            }
        }
    }
}

internal fun getTraceModeSummary(
    traceCompareMode: TraceCompareMode
): String {
    return when (traceCompareMode) {
        TraceCompareMode.CURRENT_ONLY -> "Current"
        TraceCompareMode.CURRENT_PLUS_REFERENCE -> "Overlay"
        TraceCompareMode.DIFFERENCE -> "Difference"
    }
}

/*
########################################################################
SECTION 10
SUMMARY ANALYSIS
########################################################################
PURPOSE
Pure sweep-summary derivations (moved from SweepGraphWidgets): usable
bandwidth at/below an SWR threshold and the TDR cable-fault preview text.
########################################################################
*/

internal fun estimateBandwidthAtOrBelowSwr(
    result: SweepResult,
    threshold: Double
): Double? {
    val pointsInBand = result.points.filter { it.swr <= threshold }
    if (pointsInBand.isEmpty()) {
        return null
    }

    val start = pointsInBand.minOf { it.frequencyMHz }
    val end = pointsInBand.maxOf { it.frequencyMHz }
    return end - start
}

internal fun buildCableFaultPreview(
    result: SweepResult,
    measurementCapabilities: HardwareMeasurementCapabilities,
    hardware: TestHardwareProfile
): String {
    if (!measurementCapabilities.supportsTDR) {
        return "TDR preview not supported by this hardware."
    }

    if (result.points.size < 3) {
        return "Not enough sweep points for preview estimate."
    }

    val strongestReactance = result.points.maxByOrNull { abs(it.reactance) }
        ?: return "Preview estimate unavailable."

    val velocityFactor =
        if (hardware == TestHardwareProfile.LITEVNA64_V0_3_3) {
            0.82
        } else {
            0.66
        }

    val sweepSpanHz =
        (result.endFrequencyMHz - result.startFrequencyMHz) * 1_000_000.0

    if (sweepSpanHz <= 0.0) {
        return "Preview estimate unavailable."
    }

    val estimatedMeters =
        (300_000_000.0 * velocityFactor) / (2.0 * sweepSpanHz)

    return String.format(
        "Preview only. Strongest discontinuity clue near %.3f MHz. Estimated distance scale %.2f m.",
        strongestReactance.frequencyMHz,
        estimatedMeters
    )
}