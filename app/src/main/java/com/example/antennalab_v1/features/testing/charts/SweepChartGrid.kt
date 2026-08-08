package com.example.antennalab_v1.features.testing.charts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.domain.analysis.ChartKind
import com.example.antennalab_v1.domain.analysis.ChartLayoutMath
import com.example.antennalab_v1.features.testing.SweepDisplayMode
import com.example.antennalab_v1.features.testing.SweepScalarTraceView
import com.example.antennalab_v1.features.testing.SweepSmithChartView
import com.example.antennalab_v1.features.testing.TraceCompareMode
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import com.example.antennalab_v1.ui.theme.AntennaLabTheme
import com.example.antennalab_v1.ui.theme.AntennaLab_V1Theme

/*
########################################################################
FILE: SweepChartGrid.kt
PACKAGE: com.example.antennalab_v1.features.testing.charts
LAYER: Features / Testing / Shared chart components

SYSTEM ROLE
The multi-chart grid from UI redesign spec 2.3 — SWR, Smith, return loss
and phase, capability-gated. Foundation the Phase-4 Sweep Viewer consumes.

IT WRAPS, IT DOES NOT REDRAW.
The renderers already exist in SweepGraphWidgets (SweepScalarTraceView,
SweepSmithChartView). A grid cell resolves colours from the Phase-0
tokens once, hosts the existing view at a cell-sized height, and adds a
title plus a tap target. No new trace drawing.

WHY ChartKind IS NOT SweepDisplayMode
SweepDisplayMode has no PHASE value and SweepGraphMath.getDisplayValue is
an exhaustive `when` over it, so adding one would ripple through ~7 files
and drag the axis math in (phase wants a fixed -180..180 axis, unlike
every existing auto-scaled SWR/RL/R/X axis). Phase 3 is chartered to
build shared components, not to extend the legacy enum, so the grid
carries its own small type and maps onto the existing views. Unifying the
two is a deliberate Phase 4 decision.

STATE OWNERSHIP
This component owns NO mode state. `onCellTap` reports which cell was
tapped; the Simple/Full toggle and the transient tap-to-expand behaviour
(spec 2.2 — two distinct controls, not to be conflated) belong to the
Phase-4 viewer.
########################################################################
*/

/*
ChartKind and the capability gating (ChartLayoutMath.availableChartKinds)
live in domain/analysis — capability logic is not UI, per CLAUDE.md's
"no calc logic in features/". This file only renders.
*/
private val DEFAULT_CELL_HEIGHT = 170.dp

/*
--------------------------------------------------------------------
The grid
EDIT SECTION 1003
--------------------------------------------------------------------
Column count comes from ChartLayoutMath.gridColumnCount: one chart takes
the full width, two or more pair into two columns. Renders an explicit
empty state rather than a blank area when the instrument supports nothing
in this set.
--------------------------------------------------------------------
*/
@Composable
fun SweepChartGrid(
    result: SweepResult,
    kinds: List<ChartKind>,
    modifier: Modifier = Modifier,
    markerAIndex: Int = -1,
    markerBIndex: Int = -1,
    cellHeight: Dp = DEFAULT_CELL_HEIGHT,
    onCellTap: ((ChartKind) -> Unit)? = null
) {
    if (kinds.isEmpty()) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AntennaLabTheme.spacing.md),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                modifier = Modifier.padding(AntennaLabTheme.spacing.lg),
                text = "This instrument reports no chartable measurements.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val columns = ChartLayoutMath.gridColumnCount(kinds.size)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AntennaLabTheme.spacing.sm)
    ) {
        kinds.chunked(columns).forEach { rowKinds ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AntennaLabTheme.spacing.sm)
            ) {
                rowKinds.forEach { kind ->
                    ChartGridCell(
                        modifier = Modifier.weight(1f),
                        kind = kind,
                        result = result,
                        markerAIndex = markerAIndex,
                        markerBIndex = markerBIndex,
                        cellHeight = cellHeight,
                        onTap = onCellTap
                    )
                }

                // Keep a lone final cell at column width instead of letting it
                // stretch across the row, so the grid stays a grid.
                repeat(columns - rowKinds.size) {
                    Column(modifier = Modifier.weight(1f)) {}
                }
            }
        }
    }
}

/*
--------------------------------------------------------------------
One cell
EDIT SECTION 1004
--------------------------------------------------------------------
*/
@Composable
private fun ChartGridCell(
    kind: ChartKind,
    result: SweepResult,
    markerAIndex: Int,
    markerBIndex: Int,
    cellHeight: Dp,
    modifier: Modifier = Modifier,
    onTap: ((ChartKind) -> Unit)? = null
) {
    val semantic = AntennaLabTheme.semantic
    val scheme = MaterialTheme.colorScheme

    val tapModifier =
        if (onTap != null) Modifier.clickable { onTap(kind) } else Modifier

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AntennaLabTheme.spacing.md),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .then(tapModifier)
                .padding(AntennaLabTheme.spacing.sm)
        ) {
            Text(
                text = chartTitle(kind),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant
            )

            when (kind) {
                ChartKind.SMITH -> SweepSmithChartView(
                    result = result,
                    markerAIndex = markerAIndex,
                    markerBIndex = markerBIndex,
                    instrumentSurfaceVariant = scheme.surfaceVariant,
                    instrumentDivider = scheme.outlineVariant,
                    instrumentAccent = scheme.primary,
                    instrumentTextSecondary = scheme.onSurfaceVariant,
                    markerAColor = semantic.info,
                    markerBColor = semantic.warning,
                    heightDp = cellHeight
                )

                ChartKind.PHASE -> PhaseTraceCell(
                    result = result,
                    markerAIndex = markerAIndex,
                    markerBIndex = markerBIndex,
                    cellHeight = cellHeight
                )

                ChartKind.SWR, ChartKind.RETURN_LOSS -> SweepScalarTraceView(
                    result = result,
                    referenceResult = null,
                    traceCompareMode = TraceCompareMode.CURRENT_ONLY,
                    mode = scalarModeFor(kind),
                    markerAIndex = markerAIndex,
                    markerBIndex = markerBIndex,
                    instrumentSurfaceVariant = scheme.surfaceVariant,
                    instrumentDivider = scheme.outlineVariant,
                    instrumentAccent = scheme.primary,
                    instrumentTextPrimary = scheme.onSurface,
                    instrumentTextSecondary = scheme.onSurfaceVariant,
                    instrumentBlue = semantic.info,
                    instrumentMagenta = semantic.warning,
                    instrumentGreen = semantic.success,
                    heightDp = cellHeight,
                    // Half-width cell: drop the embedded header/footer and thin
                    // the axis labels, which otherwise collapse illegibly.
                    compact = true
                )
            }

            /*
            Band strip, on the kinds that plot against frequency. Smith is
            excluded by hasFrequencyAxis, not by a local `!= SMITH`.

            Passing SCALAR is not a slip: since slice 3b-i the SCALAR and PHASE
            compact insets are identical (50/10), so one value serves every cell
            here and naming one renderer reads better than branching to prove
            they match. plotInsets_phaseAndScalarCompactAreIdentical fails first
            if that ever stops being true.

            The overlay and the renderers are siblings in this Column, so both
            measure from the same content edge and the inset lands on the plot.
            Compact suppresses the scalar footer, so the strip sits directly
            under the tick row — no need to thread it into the renderer.
            */
            if (ChartLayoutMath.hasFrequencyAxis(kind)) {
                val bandInsets = ChartLayoutMath.plotInsetsFor(
                    renderer = ChartLayoutMath.PlotRenderer.SCALAR,
                    compact = true
                )
                BandAxisOverlay(
                    axisStartMHz = result.startFrequencyMHz,
                    axisEndMHz = result.endFrequencyMHz,
                    modifier = Modifier.padding(
                        start = bandInsets.startDp.dp,
                        end = bandInsets.endDp.dp
                    )
                )
            }
        }
    }
}

/*
--------------------------------------------------------------------
ChartKind -> existing SweepDisplayMode
EDIT SECTION 1005
--------------------------------------------------------------------
Only the kinds the existing scalar view already handles. PHASE has no
SweepDisplayMode and is deliberately absent — see the file header.
--------------------------------------------------------------------
*/
private fun scalarModeFor(kind: ChartKind): SweepDisplayMode =
    when (kind) {
        ChartKind.SWR -> SweepDisplayMode.SWR
        ChartKind.RETURN_LOSS -> SweepDisplayMode.RETURN_LOSS
        ChartKind.SMITH, ChartKind.PHASE ->
            error("$kind is not a scalar trace mode")
    }

private fun chartTitle(kind: ChartKind): String =
    when (kind) {
        ChartKind.SWR -> "SWR"
        ChartKind.SMITH -> "Smith"
        ChartKind.RETURN_LOSS -> "Return loss"
        ChartKind.PHASE -> "Phase (S11)"
    }

/*
====================================================================
PREVIEWS
EDIT SECTION 1006
====================================================================
*/
private fun previewSweep(): SweepResult {
    val points = (0..40).map { index ->
        val frequencyMHz = 14.0 + index * 0.01
        val detune = (frequencyMHz - 14.2) * 40.0
        val reactance = detune * 12.0
        val swr = 1.05 + kotlin.math.abs(detune) * 1.6
        SweepPoint(
            frequencyMHz = frequencyMHz,
            swr = swr,
            returnLossDb = -26.0 + kotlin.math.abs(detune) * 18.0,
            resistance = 50.0 - kotlin.math.abs(detune) * 6.0,
            reactance = reactance
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

@Preview(name = "Chart grid — 4 up, dark", showBackground = true, widthDp = 400, heightDp = 520)
@Composable
private fun SweepChartGridDarkPreview() {
    AntennaLab_V1Theme(darkTheme = true) {
        SweepChartGrid(
            result = previewSweep(),
            kinds = listOf(
                ChartKind.SWR,
                ChartKind.SMITH,
                ChartKind.RETURN_LOSS,
                ChartKind.PHASE
            ),
            markerAIndex = 20,
            modifier = Modifier.padding(AntennaLabTheme.spacing.lg)
        )
    }
}

@Preview(name = "Chart grid — single chart", showBackground = true, widthDp = 400, heightDp = 280)
@Composable
private fun SweepChartGridSinglePreview() {
    AntennaLab_V1Theme(darkTheme = false) {
        SweepChartGrid(
            result = previewSweep(),
            kinds = listOf(ChartKind.SWR),
            markerAIndex = 20,
            modifier = Modifier.padding(AntennaLabTheme.spacing.lg)
        )
    }
}

@Preview(name = "Chart grid — nothing supported", showBackground = true, widthDp = 400)
@Composable
private fun SweepChartGridEmptyPreview() {
    AntennaLab_V1Theme(darkTheme = true) {
        SweepChartGrid(
            result = previewSweep(),
            kinds = emptyList(),
            modifier = Modifier.padding(AntennaLabTheme.spacing.lg)
        )
    }
}
