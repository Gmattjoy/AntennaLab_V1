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
    --------------------------------------------------------------------
    Which charts plot against frequency
    EDIT SECTION 1000c
    --------------------------------------------------------------------
    The band overlay annotates a frequency axis, so it belongs on every
    kind that has one and on no kind that does not. SMITH is the exception:
    it plots reflection on the complex plane, where the sweep's frequency
    is the path along the locus rather than a screen axis.

    Stated here, once, rather than as `kind != SMITH` at a call site. The
    exhaustive `when` means a fifth ChartKind cannot compile without
    answering this question, whereas the call-site version would silently
    default a new kind to "has an axis" and mis-draw it.
    --------------------------------------------------------------------
    */
    fun hasFrequencyAxis(kind: ChartKind): Boolean =
        when (kind) {
            ChartKind.SWR, ChartKind.RETURN_LOSS, ChartKind.PHASE -> true
            ChartKind.SMITH -> false
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
    Whether a cell in this grid is a half-width one. DERIVED from the column
    count rather than restating it: a lone chart takes the full row, so it is
    not compact, and that follows from gridColumnCount alone.

    This is what lets "expanded" and "sole chart" be decided once. A single
    supported chart already rendered full-row but at compact geometry — a
    40 dp gutter, two tick labels and a 50/10 band strip on a full-width plot,
    the same mismatch class an expanded cell would have.

    chartCount <= 0 yields false, which is unreachable: SweepChartGrid returns
    its empty state before any cell is composed.
    */
    fun cellsAreCompact(chartCount: Int): Boolean = gridColumnCount(chartCount) > 1

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

    /*
    Y-axis label column widths. TRACE_, not SCALAR_: since slice 4b these
    define the phase gutter too. (SCALAR_CANVAS_PADDING_DP keeps its name —
    it still literally names the padding inside ScalarTraceGraphCanvas,
    which is where the number originates and which PhaseTraceCell mirrors.)
    */
    const val TRACE_GUTTER_COMPACT_DP = 40
    const val TRACE_GUTTER_FULL_DP = 56

    /*
    Both renderers resolve to the SAME insets in BOTH states — 50/10 compact,
    66/10 full. That is not a coincidence of constants but a structural fact:
    each paints its background with a Surface and pads a Canvas inside it by
    the same amount, so two traces in a grid row pair share one plotting
    extent and ONE overlay inset serves the whole grid.

    They were 40/0 vs 50/10 until slice 3b-i, and PHASE ignored `compact`
    entirely until slice 4b (PhaseTraceCell had no full-width variant, so its
    gutter was fixed at 40; an expanded phase chart would have sat at 50 while
    an expanded scalar sat at 66). PhaseTraceCell now takes a compact flag, so
    the two are one geometry and there is no divergence left to model — hence
    no separate phase constant to keep in sync.

    Merged arm rather than a collapsed expression, deliberately: `renderer`
    stays load-bearing, and a third PlotRenderer (SMITH is the real candidate
    — a square polar plot with genuinely different insets) cannot compile
    without answering whether it shares this geometry. Same argument
    hasFrequencyAxis makes at EDIT SECTION 1000c.
    */
    fun plotInsetsFor(
        renderer: PlotRenderer,
        compact: Boolean
    ): PlotInsets =
        when (renderer) {
            PlotRenderer.SCALAR, PlotRenderer.PHASE -> traceInsets(compact)
        }

    private fun traceInsets(compact: Boolean): PlotInsets {
        val gutter =
            if (compact) TRACE_GUTTER_COMPACT_DP else TRACE_GUTTER_FULL_DP
        return PlotInsets(
            startDp = gutter + SCALAR_CANVAS_PADDING_DP,
            endDp = SCALAR_CANVAS_PADDING_DP
        )
    }

    /*
    --------------------------------------------------------------------
    How many frequency ticks a trace can actually show
    EDIT SECTION 1006
    --------------------------------------------------------------------
    Presentational thinning only — the tick VALUES still come from
    buildFrequencyTicks. A grid cell is ~160 dp wide, which fits the span
    endpoints and nothing more: measured on device, even three labels wrap
    ("14." / "45"). Start and end are also the honest minimum for orienting
    a trace, since every intermediate value is linear between them.

    This is geometry, not decoration, which is why it lives here beside
    plotInsetsFor: the tick row is laid out with SpaceBetween across the
    plot extent, so the COUNT is what determines where every label lands.
    Two renderers consume it (SweepScalarTraceView and PhaseTraceCell) and
    a rule duplicated into both would drift — the same argument EDIT
    SECTION 1005 makes about the insets themselves.

    The size guard is why this is not `listOfNotNull(first, last)`: on a
    one-element list that expression yields the same label twice. Today
    buildFrequencyTicks always returns five, so the case is unreachable and
    this moves no pixels, but the pure function should not encode the bug.
    */
    fun visibleFrequencyTicks(
        ticks: List<String>,
        compact: Boolean
    ): List<String> =
        if (!compact || ticks.size < 2) ticks
        else listOf(ticks.first(), ticks.last())
}
