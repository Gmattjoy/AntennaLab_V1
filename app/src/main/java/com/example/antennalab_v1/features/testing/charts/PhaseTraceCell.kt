package com.example.antennalab_v1.features.testing.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.domain.analysis.ChartLayoutMath
import com.example.antennalab_v1.domain.testing.SweepMarkerMath
import com.example.antennalab_v1.model.testing.SweepResult
import com.example.antennalab_v1.ui.theme.AntennaLabTheme

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
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val semantic = AntennaLabTheme.semantic

    // Computed here, once, before drawing — the DrawScope below does no
    // reflection math of its own.
    val fractions = result.points.map { point ->
        ChartLayoutMath.phaseFraction(SweepMarkerMath.reflectionPhaseDegrees(point))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .width(40.dp)
                    .height(cellHeight)
                    .padding(end = AntennaLabTheme.spacing.xs),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                // Fixed axis, so fixed labels.
                listOf("+180°", "0°", "-180°").forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cellHeight)
            ) {
                drawRect(color = scheme.surfaceVariant)

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
