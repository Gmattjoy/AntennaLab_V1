package com.example.antennalab_v1.domain.analysis

import com.example.antennalab_v1.model.AmateurBand
import com.example.antennalab_v1.model.HardwareMeasurementCapabilities
import com.example.antennalab_v1.model.IaruRegion

/*
########################################################################
FILE: ChartLayoutMath.kt
PACKAGE: com.example.antennalab_v1.domain.analysis
LAYER: Domain / Analysis

SYSTEM ROLE
Pure layout math for the Phase-3 shared chart components: where band
spans sit on the frequency axis, the fixed phase axis, and how many
columns a capability-filtered chart list wants. No Compose, no Android —
the composables consume these numbers and only draw.

LAYER NOTE
New pure math lives in domain/ per CLAUDE.md ("no calc logic in
features/"). SweepGraphMath sitting in features/testing is a
pre-existing exception, not the pattern to copy.
########################################################################
*/
/*
--------------------------------------------------------------------
What a grid cell can show
EDIT SECTION 1000
--------------------------------------------------------------------
Deliberately NOT an extension of SweepDisplayMode. That enum has no
PHASE value and SweepGraphMath.getDisplayValue is an exhaustive `when`
over it, so adding one would ripple through ~7 files and drag the axis
math in (phase wants a fixed -180..180 axis, unlike every auto-scaled
SWR/RL/R/X axis). Unifying the two is a deliberate Phase 4 decision.
--------------------------------------------------------------------
*/
enum class ChartKind {
    SWR,
    SMITH,
    RETURN_LOSS,
    PHASE
}

object ChartLayoutMath {

    /*
    --------------------------------------------------------------------
    Capability gating
    EDIT SECTION 1000b
    --------------------------------------------------------------------
    Which charts an instrument can honestly show. Callers pass
    capabilities resolved through EffectiveHardwareResolver — the
    hardware actually measuring — never project.testHardwareProfile.

    Order is the operator's scan order, not enum order. PHASE needs BOTH
    the phase-analysis feature and an S11 phase source; the trace is
    derived from gamma, but a device that reports no phase at all has
    nothing meaningful to plot.
    --------------------------------------------------------------------
    */
    fun availableChartKinds(
        capabilities: HardwareMeasurementCapabilities
    ): List<ChartKind> = buildList {
        if (capabilities.supportsSWR) add(ChartKind.SWR)
        if (capabilities.supportsSmithChart) add(ChartKind.SMITH)
        if (capabilities.supportsReturnLoss) add(ChartKind.RETURN_LOSS)
        if (capabilities.supportsPhaseAnalysis && capabilities.supportsS11Phase) {
            add(ChartKind.PHASE)
        }
    }

    /*
    Reflection phase is bounded by definition, so the phase chart uses a
    fixed symmetric axis rather than auto-scaling to the data. An
    auto-scaled phase axis makes two sweeps incomparable and exaggerates
    noise on a flat trace.
    */
    const val PHASE_AXIS_MIN_DEGREES = -180.0
    const val PHASE_AXIS_MAX_DEGREES = 180.0

    /*
    --------------------------------------------------------------------
    Band span as a fraction of the axis
    EDIT SECTION 1001
    --------------------------------------------------------------------
    startFraction/endFraction are 0..1 across the visible span, clamped,
    so a band running off either edge draws to the edge and no further.
    `isClippedLow`/`isClippedHigh` let the overlay omit a label that would
    sit half outside the plot.
    --------------------------------------------------------------------
    */
    data class BandSpan(
        val band: AmateurBand,
        val startFraction: Double,
        val endFraction: Double,
        val isClippedLow: Boolean,
        val isClippedHigh: Boolean
    ) {
        val widthFraction: Double
            get() = endFraction - startFraction
    }

    /*
    --------------------------------------------------------------------
    Position every band touching a span
    EDIT SECTION 1002
    --------------------------------------------------------------------
    Returns empty for a zero-width or inverted-to-equal axis rather than
    dividing by zero. Reversed bounds are normalised, matching
    AmateurBandPlan.bandsOverlapping.
    --------------------------------------------------------------------
    */
    fun bandSpanFractions(
        axisStartMHz: Double,
        axisEndMHz: Double,
        region: IaruRegion = AmateurBandPlan.DEFAULT_REGION
    ): List<BandSpan> {
        val low = minOf(axisStartMHz, axisEndMHz)
        val high = maxOf(axisStartMHz, axisEndMHz)
        val width = high - low
        if (width <= 0.0) return emptyList()

        return AmateurBandPlan.bandsOverlapping(low, high, region).map { band ->
            val rawStart = (band.startMHz - low) / width
            val rawEnd = (band.endMHz - low) / width
            BandSpan(
                band = band,
                startFraction = rawStart.coerceIn(0.0, 1.0),
                endFraction = rawEnd.coerceIn(0.0, 1.0),
                isClippedLow = rawStart < 0.0,
                isClippedHigh = rawEnd > 1.0
            )
        }
    }

    /*
    --------------------------------------------------------------------
    Phase axis
    EDIT SECTION 1003
    --------------------------------------------------------------------
    Fixed regardless of the data, for the reason above.
    --------------------------------------------------------------------
    */
    fun phaseAxisBounds(): ClosedFloatingPointRange<Double> =
        PHASE_AXIS_MIN_DEGREES..PHASE_AXIS_MAX_DEGREES

    /*
    Normalises a phase reading to 0..1 against the fixed axis, clamped so
    an out-of-range value cannot draw outside the plot.
    */
    fun phaseFraction(degrees: Double): Double =
        ((degrees - PHASE_AXIS_MIN_DEGREES) /
            (PHASE_AXIS_MAX_DEGREES - PHASE_AXIS_MIN_DEGREES))
            .coerceIn(0.0, 1.0)

    /*
    --------------------------------------------------------------------
    Grid shape
    EDIT SECTION 1004
    --------------------------------------------------------------------
    Column count for a capability-filtered chart list: one chart gets the
    full width, two or more pair into two columns. Deliberately simple —
    the orientation/size-aware AUTO behaviour is Phase 4's Simple/Full
    decision, not this component's.
    --------------------------------------------------------------------
    */
    fun gridColumnCount(chartCount: Int): Int =
        when {
            chartCount <= 0 -> 0
            chartCount == 1 -> 1
            else -> 2
        }

    /*
    Row count implied by the column count, so a caller can reserve height
    without laying out first.
    */
    fun gridRowCount(chartCount: Int): Int {
        val columns = gridColumnCount(chartCount)
        if (columns == 0) return 0
        return (chartCount + columns - 1) / columns
    }
}
