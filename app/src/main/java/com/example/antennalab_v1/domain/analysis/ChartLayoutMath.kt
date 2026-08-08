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

    /*
    --------------------------------------------------------------------
    Plot inset — where the plotting area actually starts
    EDIT SECTION 1005
    --------------------------------------------------------------------
    A trace does NOT begin at its composable's left edge. Two things push
    it inward: the y-axis label gutter, and (for the scalar renderer) the
    padding inside ScalarTraceGraphCanvas. Anything drawn alongside a
    trace and expected to line up with it — the band overlay first —
    must be inset by the SAME total, or its 0..1 maps to a wider extent
    than the trace and every position is wrong.

    These were three independent hardcoded values before this function:
    SweepScalarTraceView's local axisGutter, ScalarTraceGraphCanvas's
    literal padding, and PhaseTraceCell's literal gutter. They are one
    source now so they cannot drift.

    Keyed on the RENDERER, not ChartKind: ChartKind.SMITH is a square
    polar plot with no frequency axis and so has no honest answer, and
    the legacy full-width chart is not a ChartKind at all (it renders 12
    SweepDisplayMode values). Both callers can name themselves truthfully
    this way.

    Plain Ints (dp) — domain/ stays Compose-free; the composables convert.
    --------------------------------------------------------------------
    */
    enum class PlotRenderer { SCALAR, PHASE }

    data class PlotInsets(
        val startDp: Int,
        val endDp: Int
    )

    /*
    Padding inside ScalarTraceGraphCanvas's Canvas. Public so that view
    and this contract read the same number, and so the scalar view can
    recover its label-column width as startDp - this.
    */
    const val SCALAR_CANVAS_PADDING_DP = 10

    /* Y-axis label column widths. */
    const val SCALAR_GUTTER_COMPACT_DP = 40
    const val SCALAR_GUTTER_FULL_DP = 56
    const val PHASE_GUTTER_DP = 40

    /*
    `compact` is accepted for PHASE but deliberately ignored: PhaseTraceCell
    has no full-width variant, so its gutter is fixed.

    PHASE and SCALAR-compact resolve to the SAME insets (50/10), and that is
    the point — both renderers paint their background with a Surface and pad
    the Canvas inside it by the same amount, so the two traces in a grid row
    pair share one plotting extent and one overlay inset serves the whole
    grid. They were 40/0 vs 50/10 until slice 3b-i; if they ever diverge
    again, a band overlay silently mis-aligns on half the cells.
    */
    fun plotInsetsFor(
        renderer: PlotRenderer,
        compact: Boolean
    ): PlotInsets =
        when (renderer) {
            PlotRenderer.SCALAR -> {
                val gutter =
                    if (compact) SCALAR_GUTTER_COMPACT_DP else SCALAR_GUTTER_FULL_DP
                PlotInsets(
                    startDp = gutter + SCALAR_CANVAS_PADDING_DP,
                    endDp = SCALAR_CANVAS_PADDING_DP
                )
            }

            PlotRenderer.PHASE -> PlotInsets(
                startDp = PHASE_GUTTER_DP + SCALAR_CANVAS_PADDING_DP,
                endDp = SCALAR_CANVAS_PADDING_DP
            )
        }
}
