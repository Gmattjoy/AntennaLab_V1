package com.example.antennalab_v1.features.testing

/*
########################################################################
FILE: SweepInstrumentUi.kt
PACKAGE: com.example.antennalab_v1.features.testing
LAYER: UI / Testing Tools / Instrument Widgets

LAST UPDATED 4/4/2026 21:20

SYSTEM ROLE
Provides reusable instrument-style display widgets for sweep workspace UI.

CURRENT DEVELOPMENT ROLE
This file now owns:

• extracted analog gauge implementation
• shared public instrument UI primitives
• reusable instrument widgets for future panel extraction
• centralized sweep workspace button helpers

IMPORTANT ARCHITECTURE RULE
This file must stay focused on:

• reusable visual instrument cards
• analog-style meter drawing helpers
• small labelled value displays
• shared typography helpers
• shared divider / row helpers
• status chip style UI blocks
• shared workspace button visuals

This file must NOT contain:

• sweep acquisition logic
• USB/device logic
• project storage logic
• navigation ownership
• business analysis logic

SAFE EDIT AREA
- add more meter styles later
- add compact instrument rows later
- add warning state visuals later
- add theme refinements later
########################################################################
*/

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun SweepInstrumentCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            content()
        }
    }
}

@Composable
fun SharedInstrumentCard(
    instrumentSurface: Color,
    instrumentDivider: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = instrumentSurface
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, instrumentDivider, RoundedCornerShape(18.dp))
                .background(
                    color = instrumentSurface,
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SharedInstrumentTitle(
    text: String,
    instrumentTextPrimary: Color
) {
    Text(
        text = text,
        color = instrumentTextPrimary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun SharedInstrumentSectionHeader(
    text: String,
    instrumentAccent: Color
) {
    Text(
        text = text,
        color = instrumentAccent,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun SharedInstrumentSubHeader(
    text: String,
    instrumentTextPrimary: Color
) {
    Text(
        text = text,
        color = instrumentTextPrimary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun SharedInstrumentValueText(
    text: String,
    instrumentTextPrimary: Color
) {
    Text(
        text = text,
        color = instrumentTextPrimary
    )
}

@Composable
fun SharedInstrumentMutedText(
    text: String,
    instrumentTextSecondary: Color
) {
    Text(
        text = text,
        color = instrumentTextSecondary
    )
}

@Composable
fun SharedInstrumentDividerLine(
    instrumentDivider: Color
) {
    HorizontalDivider(
        color = instrumentDivider,
        thickness = 1.dp
    )
}

@Composable
fun SharedTwoValueRow(
    label: String,
    value: String,
    instrumentTextPrimary: Color,
    instrumentTextSecondary: Color,
    modifier: Modifier = Modifier,
    labelWidth: Dp = 150.dp
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = instrumentTextSecondary,
            modifier = Modifier.width(labelWidth)
        )
        Text(
            text = value,
            color = instrumentTextPrimary,
            modifier = Modifier.wrapContentHeight()
        )
    }
}

@Composable
fun SweepWorkspaceActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp,
            disabledElevation = 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SweepWorkspaceDisplayButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val fillColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.80f)
        }

    val textColor =
        if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    val borderColor =
        if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.95f)
        }

    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = borderColor
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (selected) 6.dp else 3.dp,
            pressedElevation = 2.dp,
            disabledElevation = 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = fillColor,
            contentColor = textColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
fun SweepValueReadout(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SweepStatusChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                shape = RoundedCornerShape(999.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SweepAnalogMeter(
    title: String,
    valueText: String,
    ratio: Float,
    modifier: Modifier = Modifier,
    footerText: String? = null
) {
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val safeRatio = clampFloat(ratio, 0f, 1f)

    SweepInstrumentCard(
        title = title,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawSweepMeterDial(
                        outlineColor = outlineColor,
                        activeColor = primaryColor,
                        needleColor = onSurfaceColor,
                        ratio = safeRatio
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor
                    )
                }
            }

            if (footerText != null) {
                Text(
                    text = footerText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SweepAnalogGauge(
    result: SweepResult,
    mode: SweepDisplayMode,
    markerAIndex: Int,
    instrumentSurface: Color,
    instrumentDivider: Color,
    instrumentAccent: Color,
    instrumentTextPrimary: Color,
    instrumentTextSecondary: Color
) {
    val selectedPoint = result.points.getOrNull(markerAIndex)
        ?: result.points.minByOrNull { it.swr }
        ?: return

    val displayValue = getDisplayValue(selectedPoint, mode)
    val gaugeTitle = gaugeTitle(mode)
    val gaugeUnit = gaugeUnit(mode)
    val gaugeMin = gaugeMin(mode)
    val gaugeMax = gaugeMax(mode)
    val gaugeMid = (gaugeMin + gaugeMax) / 2.0
    val clampedValue = displayValue.coerceIn(gaugeMin, gaugeMax)
    val fraction = ((clampedValue - gaugeMin) / (gaugeMax - gaugeMin)).coerceIn(0.0, 1.0)

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SharedInstrumentSectionHeader(
            text = gaugeTitle,
            instrumentAccent = instrumentAccent
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .border(1.dp, instrumentDivider, RoundedCornerShape(14.dp)),
            color = instrumentSurface,
            shape = RoundedCornerShape(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val center = Offset(size.width / 2f, size.height * 0.82f)
                    val radius = min(size.width, size.height) * 0.42f
                    val startAngleDeg = 210.0
                    val sweepAngleDeg = 120.0

                    drawArc(
                        color = instrumentDivider,
                        startAngle = startAngleDeg.toFloat(),
                        sweepAngle = sweepAngleDeg.toFloat(),
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 8f)
                    )

                    for (tickIndex in 0..10) {
                        val tickFraction = tickIndex / 10.0
                        val tickAngleDeg = startAngleDeg + (sweepAngleDeg * tickFraction)
                        val tickAngleRad = Math.toRadians(tickAngleDeg)

                        val outerX = center.x + (radius * cos(tickAngleRad)).toFloat()
                        val outerY = center.y + (radius * sin(tickAngleRad)).toFloat()
                        val innerRadius = if (tickIndex % 5 == 0) radius - 26f else radius - 16f
                        val innerX = center.x + (innerRadius * cos(tickAngleRad)).toFloat()
                        val innerY = center.y + (innerRadius * sin(tickAngleRad)).toFloat()

                        drawLine(
                            color = instrumentTextPrimary,
                            start = Offset(innerX, innerY),
                            end = Offset(outerX, outerY),
                            strokeWidth = if (tickIndex % 5 == 0) 4f else 2f
                        )
                    }

                    val needleAngleDeg = startAngleDeg + (sweepAngleDeg * fraction)
                    val needleAngleRad = Math.toRadians(needleAngleDeg)
                    val needleLength = radius - 28f

                    val needleX = center.x + (needleLength * cos(needleAngleRad)).toFloat()
                    val needleY = center.y + (needleLength * sin(needleAngleRad)).toFloat()

                    drawLine(
                        color = instrumentAccent,
                        start = center,
                        end = Offset(needleX, needleY),
                        strokeWidth = 5f
                    )

                    drawCircle(
                        color = instrumentSurface,
                        radius = 12f,
                        center = center
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SharedInstrumentValueText(
                        text = String.format("%.3f %s", displayValue, gaugeUnit),
                        instrumentTextPrimary = instrumentTextPrimary
                    )
                    SharedInstrumentMutedText(
                        text = String.format("Marker A  %.3f MHz", selectedPoint.frequencyMHz),
                        instrumentTextSecondary = instrumentTextSecondary
                    )
                    SharedInstrumentMutedText(
                        text = gaugeSubtitle(mode, selectedPoint),
                        instrumentTextSecondary = instrumentTextSecondary
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SharedInstrumentMutedText(
                text = String.format("%.1f", gaugeMin),
                instrumentTextSecondary = instrumentTextSecondary
            )
            SharedInstrumentMutedText(
                text = String.format("%.1f", gaugeMid),
                instrumentTextSecondary = instrumentTextSecondary
            )
            SharedInstrumentMutedText(
                text = String.format("%.1f", gaugeMax),
                instrumentTextSecondary = instrumentTextSecondary
            )
        }
    }
}

@Composable
fun SweepDualReadoutRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String,
    modifier: Modifier = Modifier,
    leftSupportingText: String? = null,
    rightSupportingText: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SweepValueReadout(
            label = leftLabel,
            value = leftValue,
            supportingText = leftSupportingText,
            modifier = Modifier.weight(1f)
        )

        SweepValueReadout(
            label = rightLabel,
            value = rightValue,
            supportingText = rightSupportingText,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SweepIndicatorDot(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(10.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            )
    )
}

@Composable
fun SweepPlaceholderInstrumentPanel(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    SweepInstrumentCard(
        title = title,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSweepMeterDial(
    outlineColor: Color,
    activeColor: Color,
    needleColor: Color,
    ratio: Float
) {
    val strokeWidth = 16.dp.toPx()
    val startAngle = 150f
    val sweepAngle = 240f

    drawArc(
        color = outlineColor,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(strokeWidth, strokeWidth),
        size = Size(
            width = size.width - (strokeWidth * 2f),
            height = size.height - (strokeWidth * 2f)
        ),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    drawArc(
        color = activeColor,
        startAngle = startAngle,
        sweepAngle = sweepAngle * ratio,
        useCenter = false,
        topLeft = Offset(strokeWidth, strokeWidth),
        size = Size(
            width = size.width - (strokeWidth * 2f),
            height = size.height - (strokeWidth * 2f)
        ),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    val angleDegrees = startAngle + (sweepAngle * ratio)
    val angleRadians = Math.toRadians(angleDegrees.toDouble())
    val radius = (size.minDimension / 2f) - (strokeWidth * 1.7f)
    val center = Offset(size.width / 2f, size.height / 2f)

    val needleEnd = Offset(
        x = center.x + (radius * cos(angleRadians).toFloat()),
        y = center.y + (radius * sin(angleRadians).toFloat())
    )

    drawLine(
        color = needleColor,
        start = center,
        end = needleEnd,
        strokeWidth = 6.dp.toPx(),
        cap = StrokeCap.Round
    )

    drawCircle(
        color = needleColor,
        radius = 8.dp.toPx(),
        center = center
    )
}

private fun gaugeTitle(
    mode: SweepDisplayMode
): String =
    when (mode) {
        SweepDisplayMode.ANALOG_SWR -> "Analog SWR Meter"
        SweepDisplayMode.ANALOG_RETURN_LOSS -> "Analog Return Loss Meter"
        SweepDisplayMode.ANALOG_RESISTANCE -> "Analog Resistance Meter"
        SweepDisplayMode.ANALOG_REACTANCE -> "Analog Reactance Meter"
        else -> "Analog Meter"
    }

private fun gaugeSubtitle(
    mode: SweepDisplayMode,
    point: SweepPoint
): String =
    when (mode) {
        SweepDisplayMode.ANALOG_SWR -> {
            if (point.swr <= 1.5) {
                "Good match region"
            } else if (point.swr <= 2.0) {
                "Usable match region"
            } else {
                "High mismatch region"
            }
        }

        SweepDisplayMode.ANALOG_RETURN_LOSS -> {
            if (point.returnLossDb >= 20.0) {
                "Strong return-loss result"
            } else if (point.returnLossDb >= 10.0) {
                "Moderate return-loss result"
            } else {
                "Weak return-loss result"
            }
        }

        SweepDisplayMode.ANALOG_RESISTANCE -> {
            if (point.resistance in 40.0..60.0) {
                "Near 50 Ω region"
            } else {
                "Away from 50 Ω region"
            }
        }

        SweepDisplayMode.ANALOG_REACTANCE -> {
            when {
                point.reactance > 5.0 -> "Inductive region"
                point.reactance < -5.0 -> "Capacitive region"
                else -> "Near resonance region"
            }
        }

        else -> ""
    }

private fun gaugeUnit(
    mode: SweepDisplayMode
): String =
    when (mode) {
        SweepDisplayMode.ANALOG_SWR -> "SWR"
        SweepDisplayMode.ANALOG_RETURN_LOSS -> "dB"
        SweepDisplayMode.ANALOG_RESISTANCE,
        SweepDisplayMode.ANALOG_REACTANCE -> "Ω"

        else -> ""
    }

private fun gaugeMin(
    mode: SweepDisplayMode
): Double =
    when (mode) {
        SweepDisplayMode.ANALOG_SWR -> 1.0
        SweepDisplayMode.ANALOG_RETURN_LOSS -> 0.0
        SweepDisplayMode.ANALOG_RESISTANCE -> 0.0
        SweepDisplayMode.ANALOG_REACTANCE -> -100.0
        else -> 0.0
    }

private fun gaugeMax(
    mode: SweepDisplayMode
): Double =
    when (mode) {
        SweepDisplayMode.ANALOG_SWR -> 10.0
        SweepDisplayMode.ANALOG_RETURN_LOSS -> 40.0
        SweepDisplayMode.ANALOG_RESISTANCE -> 100.0
        SweepDisplayMode.ANALOG_REACTANCE -> 100.0
        else -> 100.0
    }