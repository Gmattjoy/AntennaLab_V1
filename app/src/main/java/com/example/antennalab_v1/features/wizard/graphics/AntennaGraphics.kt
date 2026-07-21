package com.example.antennalab_v1.features.wizard.graphics

/*
########################################################################
FILE: AntennaGraphics.kt
PURPOSE: High quality technical antenna graphics for the wizard.
ROLE: Compose Canvas graphics only.

RULES:
- Graphics only.
- No calculations here.
- No navigation here.
########################################################################
*/

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.max

private val elementStroke = 6f
private val thinStroke = 2.5f
private val metallicBrush = Brush.linearGradient(
    listOf(
        Color(0xFF8E959C),
        Color(0xFFDCE2E6),
        Color(0xFF8E959C)
    )
)

private val boomColor = Color(0xFF656B73)
private val dimensionColor = Color(0xFF7B91A4)
private val feedColor = Color(0xFF4E5963)

/*
########################################
Helpers
########################################
*/

private fun DrawScope.drawDimensionLineHorizontal(
    startX: Float,
    endX: Float,
    y: Float
) {
    drawLine(
        color = dimensionColor,
        start = Offset(startX, y),
        end = Offset(endX, y),
        strokeWidth = thinStroke
    )
    drawLine(
        color = dimensionColor,
        start = Offset(startX, y - 8f),
        end = Offset(startX, y + 8f),
        strokeWidth = thinStroke
    )
    drawLine(
        color = dimensionColor,
        start = Offset(endX, y - 8f),
        end = Offset(endX, y + 8f),
        strokeWidth = thinStroke
    )
}

private fun DrawScope.drawDimensionLineVertical(
    x: Float,
    startY: Float,
    endY: Float
) {
    drawLine(
        color = dimensionColor,
        start = Offset(x, startY),
        end = Offset(x, endY),
        strokeWidth = thinStroke
    )
    drawLine(
        color = dimensionColor,
        start = Offset(x - 8f, startY),
        end = Offset(x + 8f, startY),
        strokeWidth = thinStroke
    )
    drawLine(
        color = dimensionColor,
        start = Offset(x - 8f, endY),
        end = Offset(x + 8f, endY),
        strokeWidth = thinStroke
    )
}

/*
########################################
Static icons
########################################
*/

@Composable
fun DipoleGraphic() {
    DipoleGraphicScaled(scaleFraction = 0.78f)
}

@Composable
fun VerticalGraphic() {
    VerticalGraphicScaled(scaleFraction = 0.78f)
}

@Composable
fun YagiGraphic() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val boomY = size.height * 0.5f

        drawLine(
            color = boomColor,
            start = Offset(size.width * 0.08f, boomY),
            end = Offset(size.width * 0.92f, boomY),
            strokeWidth = 5f
        )

        val reflectorX = size.width * 0.25f
        val drivenX = size.width * 0.45f
        val directorX = size.width * 0.65f

        val reflectorH = size.height * 0.28f
        val drivenH = size.height * 0.22f
        val directorH = size.height * 0.18f

        drawElement(reflectorX, boomY, reflectorH)
        drawElement(drivenX, boomY, drivenH)
        drawElement(directorX, boomY, directorH)
    }
}

private fun DrawScope.drawElement(
    x: Float,
    y: Float,
    halfHeight: Float
) {
    drawLine(
        brush = metallicBrush,
        start = Offset(x, y - halfHeight),
        end = Offset(x, y + halfHeight),
        strokeWidth = elementStroke
    )
}

@Composable
fun LoopGraphic() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val margin = size.minDimension * 0.15f

        drawOval(
            brush = metallicBrush,
            topLeft = Offset(margin, margin),
            size = Size(size.width - margin * 2, size.height - margin * 2),
            style = Stroke(width = elementStroke)
        )

        drawLine(
            color = feedColor,
            start = Offset(size.width * 0.50f, size.height * 0.50f),
            end = Offset(size.width * 0.50f, size.height * 0.84f),
            strokeWidth = thinStroke
        )
    }
}

@Composable
fun UnknownGraphic() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = Color(0xFF8B949E),
            radius = max(size.minDimension / 2.8f, 12f),
            center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = elementStroke)
        )
    }
}

/*
########################################
Scaled workspace graphics
########################################
*/

@Composable
fun DipoleGraphicScaled(
    scaleFraction: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val clamped = scaleFraction.coerceIn(0.35f, 1.0f)

        val centerX = size.width * 0.50f
        val midY = size.height * 0.55f

        val fullAvailable = size.width * 0.82f
        val drawnLength = fullAvailable * clamped
        val half = drawnLength / 2f

        val leftStart = centerX - half
        val leftEnd = centerX - 10f
        val rightStart = centerX + 10f
        val rightEnd = centerX + half

        drawLine(
            brush = metallicBrush,
            start = Offset(leftStart, midY),
            end = Offset(leftEnd, midY),
            strokeWidth = elementStroke
        )

        drawLine(
            brush = metallicBrush,
            start = Offset(rightStart, midY),
            end = Offset(rightEnd, midY),
            strokeWidth = elementStroke
        )

        drawCircle(
            color = feedColor,
            radius = 6f,
            center = Offset(centerX, midY)
        )

        drawLine(
            color = feedColor,
            start = Offset(centerX, midY),
            end = Offset(centerX, size.height * 0.86f),
            strokeWidth = thinStroke
        )

        drawDimensionLineHorizontal(
            startX = leftStart,
            endX = rightEnd,
            y = size.height * 0.26f
        )
    }
}

@Composable
fun VerticalGraphicScaled(
    scaleFraction: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val clamped = scaleFraction.coerceIn(0.35f, 1.0f)

        val centerX = size.width * 0.50f
        val baseY = size.height * 0.84f

        val maxHeight = size.height * 0.68f
        val drawnHeight = maxHeight * clamped
        val topY = baseY - drawnHeight

        drawLine(
            brush = metallicBrush,
            start = Offset(centerX, topY),
            end = Offset(centerX, baseY),
            strokeWidth = elementStroke
        )

        drawLine(
            color = boomColor,
            start = Offset(size.width * 0.18f, baseY),
            end = Offset(size.width * 0.82f, baseY),
            strokeWidth = 4f
        )

        drawLine(
            color = boomColor,
            start = Offset(centerX, baseY),
            end = Offset(size.width * 0.30f, size.height * 0.94f),
            strokeWidth = 3f
        )

        drawLine(
            color = boomColor,
            start = Offset(centerX, baseY),
            end = Offset(size.width * 0.70f, size.height * 0.94f),
            strokeWidth = 3f
        )

        drawDimensionLineVertical(
            x = size.width * 0.24f,
            startY = topY,
            endY = baseY
        )
    }
}