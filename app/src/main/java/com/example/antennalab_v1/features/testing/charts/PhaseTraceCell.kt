package com.example.antennalab_v1.features.testing.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.domain.analysis.ChartLayoutMath
import com.example.antennalab_v1.domain.testing.SweepMarkerMath
import com.example.antennalab_v1.features.testing.SharedInstrumentMutedText
import com.example.antennalab_v1.features.testing.buildFrequencyTicks
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import com.example.antennalab_v1.ui.theme.AntennaLabTheme
import com.example.antennalab_v1.ui.theme.AntennaLab_V1Theme

/*
########################################################################
FILE: PhaseTraceCell.kt
PACKAGE: com.example.antennalab_v1.features.testing.charts
LAYER: Features / Testing / Shared chart components

SYSTEM ROLE
The S11 phase trace — the one chart in the spec 2.3 grid with no existing
renderer, because SweepDisplayMode has no PHASE value (see
SweepChartGrid's header for why we did not add one).

Values come from SweepMarkerMath.reflectionPhaseDegrees, which derives
phase from gamma via OslCalibrationEngine.gammaFromPoint — the same exact
R/X path CalibrationCorrector uses. So this trace is correct on
calibrated data and does NOT depend on SweepPoint.s11PhaseDegrees being
populated by the device.

The axis is FIXED at -180..180 (ChartLayoutMath.phaseAxisBounds), not
auto-scaled: phase is bounded by definition, an auto-scaled axis makes
two sweeps incomparable, and it would exaggerate noise on a flat trace.
########################################################################
*/
@Composable
internal fun PhaseTraceCell(
    result: SweepResult,
    markerAIndex: Int,
    markerBIndex: Int,
    cellHeight: Dp,
    /*
    Defaults TRUE, deliberately the opposite of SweepScalarTraceView's
    `compact = false`. Not a slip: that renderer's untouched call sites are
    the legacy full-width chart, this one's sole call site is a grid cell.
    Each defaults to what its existing caller already renders, so neither
    moves a pixel until something passes the flag explicitly (slice 4c).
    */
    compact: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val semantic = AntennaLabTheme.semantic

    // Computed here, once, before drawing — the DrawScope below does no
    // reflection math of its own.
    val fractions = result.points.map { point ->
        ChartLayoutMath.phaseFraction(SweepMarkerMath.reflectionPhaseDegrees(point))
    }

    /*
    Geometry comes from the shared alignment contract. This cell used to inset
    40/0 while a scalar cell inset 50/10, so the two traces in a grid row pair
    started and ended 10 dp apart. Both are 50/10 now — one geometry across the
    whole grid, which is what lets a band overlay use a single inset.

    That equality is structural, not a coincidence of constants: the plot below
    is a Surface painting the background with the Canvas padded inside it,
    exactly as ScalarTraceGraphCanvas does. The background CANNOT be a drawRect
    inside the Canvas — padding shrinks the DrawScope, so the fill would shrink
    with it and leave an unpainted margin.

    Slice 4b: the match now holds in BOTH states, not just compact. PHASE used
    to ignore `compact` because this cell had no full-width variant, so an
    expanded phase chart would have inset 50 while an expanded scalar inset 66
    — a visible gutter mismatch, and a band overlay needing a per-renderer
    inset again. Nothing here changes for the grid: compact is still 50/10.
    */
    val plotInsets = ChartLayoutMath.plotInsetsFor(
        renderer = ChartLayoutMath.PlotRenderer.PHASE,
        compact = compact
    )
    val axisGutter =
        (plotInsets.startDp - ChartLayoutMath.SCALAR_CANVAS_PADDING_DP).dp
    val plotPadding = ChartLayoutMath.SCALAR_CANVAS_PADDING_DP.dp

    /*
    Thinned to the span endpoints in a cell — the shared rule, so this row and
    a scalar cell's show the same number of labels in the same places.
    */
    val ticks = ChartLayoutMath.visibleFrequencyTicks(
        ticks = buildFrequencyTicks(
            startMHz = result.startFrequencyMHz,
            endMHz = result.endFrequencyMHz
        ),
        compact = compact
    )

    /*
    fillMaxWidth is load-bearing: it makes the width come from the incoming
    constraint. If this Column ever wrapped its content, the tick Row's
    intrinsic width would start driving the plot's.

    The gap MUST equal SweepScalarTraceView's Column(spacedBy(8.dp)) — it is
    the one number in this file matched by hand rather than shared, and it is
    what keeps the two cells' band strips on one line in a grid row pair.
    */
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AntennaLabTheme.spacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .width(axisGutter)
                    .height(cellHeight)
                    // Vertical padding matches the plot's, so SpaceBetween
                    // distributes across the same extent the trace occupies:
                    // +180° lands on the plot top, -180° on the bottom.
                    .padding(
                        end = AntennaLabTheme.spacing.xs,
                        top = plotPadding,
                        bottom = plotPadding
                    ),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                /*
                Fixed axis, so fixed labels.

                labelSmall here is load-bearing, NOT drift from the tick row's
                SharedInstrumentMutedText below: "+180°" at that row's 16 sp is
                ~50 dp against a 40 dp gutter (36 dp usable after the end pad),
                and still marginal at the 56 dp full-width gutter. A parity
                pass that "finishes" the unification blows out the gutter.
                */
                listOf("+180°", "0°", "-180°").forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cellHeight)
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(14.dp)),
                color = scheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(plotPadding)
                ) {
                    // Zero-degree reference: the line an operator reads resonance
                    // against, so it is drawn distinctly rather than as grid.
                    val zeroY = size.height *
                        (1f - ChartLayoutMath.phaseFraction(0.0).toFloat())
                    drawLine(
                        color = scheme.outlineVariant,
                        start = Offset(0f, zeroY),
                        end = Offset(size.width, zeroY),
                        strokeWidth = 1f
                    )

                    if (fractions.size < 2) return@Canvas

                    val path = Path()
                    fractions.forEachIndexed { index, fraction ->
                        val x = size.width * index / (fractions.size - 1).toFloat()
                        val y = size.height * (1f - fraction.toFloat())
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = scheme.primary,
                        style = Stroke(width = 2f)
                    )

                    drawPhaseMarker(fractions, markerAIndex, semantic.info)
                    drawPhaseMarker(fractions, markerBIndex, semantic.warning)
                }
            }
        }

        /*
        Frequency ticks — a SIBLING of the plot Row, not a child, so it
        measures from the same content edge and the inset lands on the plot.
        Padded on BOTH sides by the true plot insets, so SpaceBetween
        distributes across exactly the extent the trace occupies: the 3b-i
        pattern from SweepScalarTraceView, unchanged.

        This cell had no x-axis at all before slice 4b — its band strip sat
        directly under the plot border while a scalar cell's sat below its
        ticks, leaving the two cards in a row pair ending 32 dp apart. The row
        already reserved that height for the taller sibling, so filling it
        costs no grid height and lines the two strips up.

        SharedInstrumentMutedText rather than a local Text: it makes this row
        measure IDENTICALLY to a scalar cell's whatever bodyLarge's line height
        and font padding resolve to, which is what keeps the strips flush.
        */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = plotInsets.startDp.dp,
                    end = plotInsets.endDp.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ticks.forEach { tick ->
                SharedInstrumentMutedText(
                    text = tick,
                    instrumentTextSecondary = scheme.onSurfaceVariant
                )
            }
        }
    }
}

/*
Vertical rule at a marker index. Out-of-range indices (the -1 "no marker"
default) draw nothing.
*/
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPhaseMarker(
    fractions: List<Double>,
    index: Int,
    color: Color
) {
    if (index < 0 || index >= fractions.size || fractions.size < 2) return
    val x = size.width * index / (fractions.size - 1).toFloat()
    drawLine(
        color = color,
        start = Offset(x, 0f),
        end = Offset(x, size.height),
        strokeWidth = 1.5f
    )
}

/*
====================================================================
Previews
====================================================================
The compact/full pair is the point of slice 4b: with no Compose UI
tests anywhere, this is the only non-device look at the two gutters
(40 vs 56) and at the tick row thinning 5 labels down to 2.
*/
@Preview(name = "Phase trace — compact cell", showBackground = true, widthDp = 200)
@Composable
private fun PhaseTraceCellCompactPreview() {
    AntennaLab_V1Theme(darkTheme = true) {
        PhaseTraceCell(
            result = phasePreviewSweep(),
            markerAIndex = 10,
            markerBIndex = 30,
            cellHeight = 170.dp,
            modifier = Modifier.padding(AntennaLabTheme.spacing.sm)
        )
    }
}

@Preview(name = "Phase trace — full width", showBackground = true, widthDp = 400)
@Composable
private fun PhaseTraceCellFullWidthPreview() {
    AntennaLab_V1Theme(darkTheme = true) {
        PhaseTraceCell(
            result = phasePreviewSweep(),
            markerAIndex = 10,
            markerBIndex = 30,
            cellHeight = 240.dp,
            compact = false,
            modifier = Modifier.padding(AntennaLabTheme.spacing.sm)
        )
    }
}

private fun phasePreviewSweep(): SweepResult {
    val points = (0..40).map { index ->
        val frequencyMHz = 14.0 + index * 0.01
        val detune = (frequencyMHz - 14.2) * 40.0
        SweepPoint(
            frequencyMHz = frequencyMHz,
            swr = 1.05 + kotlin.math.abs(detune) * 1.6,
            returnLossDb = -26.0 + kotlin.math.abs(detune) * 18.0,
            resistance = 50.0 - kotlin.math.abs(detune) * 6.0,
            reactance = detune * 12.0
        )
    }
    return SweepResult(
        startFrequencyMHz = 14.0,
        endFrequencyMHz = 14.4,
        stepMHz = 0.01,
        points = points,
        hardwareProfile = "LiteVNA64 v0.3.3"
    )
}
