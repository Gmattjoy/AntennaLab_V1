package com.example.antennalab_v1.features.testing.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.domain.analysis.ChartLayoutMath
import com.example.antennalab_v1.model.IaruRegion
import com.example.antennalab_v1.ui.theme.AntennaLabTheme
import com.example.antennalab_v1.ui.theme.AntennaLab_V1Theme

/*
########################################################################
FILE: BandAxisOverlay.kt
PACKAGE: com.example.antennalab_v1.features.testing.charts
LAYER: Features / Testing / Shared chart components

SYSTEM ROLE
The amateur-band overlay on the frequency axis (UI redesign spec 2.3).
Draws a band as a shaded span across the plot width so an operator can
see at a glance where the trace sits relative to their allocation.

THIS FILE COMPUTES NOTHING.
Positions come from ChartLayoutMath.bandSpanFractions; band data comes
from AmateurBandPlan (Region 3 by default). This file turns fractions
into rectangles.

HONESTY
The underlying table is region-level, NOT a legal band plan — country and
licence-class limits are narrower. The overlay orients a trace; it never
implies permission to transmit.
########################################################################
*/

private val DEFAULT_OVERLAY_HEIGHT = 18.dp

/*
--------------------------------------------------------------------
Band strip
EDIT SECTION 1001
--------------------------------------------------------------------
A thin strip meant to sit directly beneath (or behind) a trace plot,
sharing the plot's horizontal extent so the fractions line up. Renders
nothing when no band overlaps the span, which is the correct outcome for
a sweep sitting in a gap between allocations.
--------------------------------------------------------------------
*/
@Composable
fun BandAxisOverlay(
    axisStartMHz: Double,
    axisEndMHz: Double,
    modifier: Modifier = Modifier,
    region: IaruRegion = com.example.antennalab_v1.domain.analysis.AmateurBandPlan.DEFAULT_REGION,
    height: Dp = DEFAULT_OVERLAY_HEIGHT
) {
    val spans = ChartLayoutMath.bandSpanFractions(
        axisStartMHz = axisStartMHz,
        axisEndMHz = axisEndMHz,
        region = region
    )
    if (spans.isEmpty()) return

    val fill = AntennaLabTheme.semantic.info
    val edge = AntennaLabTheme.semantic.neutral

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        spans.forEach { span ->
            drawBandSpan(
                startFraction = span.startFraction.toFloat(),
                endFraction = span.endFraction.toFloat(),
                fillAlpha = 0.22f,
                fillColor = fill,
                edgeColor = edge
            )
        }
    }
}

/*
--------------------------------------------------------------------
One shaded span
EDIT SECTION 1002
--------------------------------------------------------------------
Separated so the drawing is testable by eye in isolation and so the
Phase-4 viewer can reuse it behind a full-height plot rather than only in
a strip.
--------------------------------------------------------------------
*/
private fun DrawScope.drawBandSpan(
    startFraction: Float,
    endFraction: Float,
    fillAlpha: Float,
    fillColor: androidx.compose.ui.graphics.Color,
    edgeColor: androidx.compose.ui.graphics.Color
) {
    val left = size.width * startFraction
    val right = size.width * endFraction
    val width = (right - left).coerceAtLeast(1f)

    drawRect(
        color = fillColor.copy(alpha = fillAlpha),
        topLeft = Offset(left, 0f),
        size = Size(width, size.height)
    )

    // Edges marked explicitly: a band boundary is the thing an operator is
    // actually looking for, and a flat wash hides it.
    drawRect(
        color = edgeColor.copy(alpha = 0.55f),
        topLeft = Offset(left, 0f),
        size = Size(1f, size.height)
    )
    drawRect(
        color = edgeColor.copy(alpha = 0.55f),
        topLeft = Offset(right - 1f, 0f),
        size = Size(1f, size.height)
    )
}

/*
====================================================================
PREVIEWS
EDIT SECTION 1003
====================================================================
Three spans worth eyeballing: a narrow sweep inside one band, a wide HF
sweep crossing many, and a span in a gap (which must render nothing).
--------------------------------------------------------------------
*/
@Preview(name = "Band overlay — 20m sweep", showBackground = true, widthDp = 360)
@Composable
private fun BandAxisOverlayNarrowPreview() {
    AntennaLab_V1Theme(darkTheme = true) {
        Box(modifier = Modifier.padding(AntennaLabTheme.spacing.lg)) {
            BandAxisOverlay(axisStartMHz = 13.9, axisEndMHz = 14.5)
        }
    }
}

@Preview(name = "Band overlay — full HF", showBackground = true, widthDp = 360)
@Composable
private fun BandAxisOverlayWidePreview() {
    AntennaLab_V1Theme(darkTheme = true) {
        Box(modifier = Modifier.padding(AntennaLabTheme.spacing.lg)) {
            BandAxisOverlay(axisStartMHz = 1.0, axisEndMHz = 30.0)
        }
    }
}

@Preview(name = "Band overlay — light, 2m", showBackground = true, widthDp = 360)
@Composable
private fun BandAxisOverlayLightPreview() {
    AntennaLab_V1Theme(darkTheme = false) {
        Box(modifier = Modifier.padding(AntennaLabTheme.spacing.lg)) {
            BandAxisOverlay(axisStartMHz = 143.0, axisEndMHz = 149.0)
        }
    }
}
