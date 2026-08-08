package com.example.antennalab_v1.features.testing

/*
########################################################################
FILE: SweepGraphWidgets.kt
PACKAGE: com.example.antennalab_v1.features.testing
LAYER: UI / Testing Tools / Workspace Widgets

LAST UPDATED 4/4/2026 21:05

SYSTEM ROLE
Provides reusable widget sections and graph helpers for the sweep
workspace.

CURRENT DEVELOPMENT ROLE
This file now owns extracted reusable workspace UI sections and graph
rendering helpers from SweepGraphScreen.

THIS FILE CURRENTLY OWNS
• Session Overview card
• Display Modes card
• Controls card
• Sweep Summary card
• generic widget button styles
• scalar trace workspace view
• scalar graph canvas
• waterfall sweep view
• Smith chart preview
• impedance locus view
• trace drawing helpers
• marker visuals
• shared scalar display helpers

IMPORTANT ARCHITECTURE RULE
This file must only contain reusable sweep workspace UI and rendering
helpers.

This file must NOT contain:
• sweep acquisition logic
• USB communication
• navigation routing
• project storage logic

SAFE EDIT AREA
- extract more screen cards later
- centralize repeated button styles later
- split graph helpers into a dedicated file later if desired
########################################################################
*/

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.domain.analysis.ChartLayoutMath
import com.example.antennalab_v1.model.HardwareMeasurementCapabilities
import com.example.antennalab_v1.model.TestHardwareProfile
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import com.example.antennalab_v1.ui.components.SegmentedChoiceButton
import kotlin.math.max
import kotlin.math.min

/*
########################################################################
SECTION 1
SESSION OVERVIEW CARD
########################################################################
PURPOSE
Renders the current sweep session summary card that appears near the
top of the workspace.
########################################################################
*/
@Composable
fun SweepSessionOverviewCard(
    capabilityProfileDisplayName: String,
    sweepStartMHz: Double,
    sweepEndMHz: Double,
    sweepStepMHz: Double,
    hardwareMinMHz: Double,
    hardwareMaxMHz: Double,
    sweepHistoryCount: Int,
    hasReferenceSweep: Boolean,
    traceCompareMode: TraceCompareMode,
    activeMarkerTargetLabel: String,
    instrumentSurface: Color,
    instrumentDivider: Color,
    instrumentAccent: Color,
    instrumentTextPrimary: Color,
    instrumentTextSecondary: Color,
    currentSweepSourceLabel: String? = null,
    selectedSweepPathLabel: String? = null,
    transportStatusLabel: String? = null,
    analyzerIdentityDisplayName: String? = null,
    protocolDisplayName: String? = null
) {
    SharedInstrumentCard(
        instrumentSurface = instrumentSurface,
        instrumentDivider = instrumentDivider
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, instrumentDivider)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                SharedInstrumentValueText(
                    text = capabilityProfileDisplayName,
                    instrumentTextPrimary = instrumentTextPrimary
                )

                transportStatusLabel?.let { statusText ->
                    SharedInstrumentValueText(
                        text = statusText,
                        instrumentTextPrimary =
                            when {
                                statusText.contains("Ready", ignoreCase = true) ||
                                        statusText.contains("Live", ignoreCase = true) ||
                                        statusText.contains("OK", ignoreCase = true) -> InstrumentGreen

                                statusText.contains("Simulated", ignoreCase = true) ||
                                        statusText.contains("Demo", ignoreCase = true) ||
                                        statusText.contains("Partial", ignoreCase = true) -> MaterialTheme.colorScheme.primary

                                else -> InstrumentMagenta
                            }
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val identityText = analyzerIdentityDisplayName ?: "No Device"

                SharedInstrumentMutedText(
                    text = identityText,
                    instrumentTextSecondary = instrumentTextSecondary
                )

                selectedSweepPathLabel?.let { pathText ->
                    SharedInstrumentValueText(
                        text = pathText,
                        instrumentTextPrimary =
                            when {
                                pathText.contains("Real", ignoreCase = true) ||
                                        pathText.contains("Live", ignoreCase = true) -> InstrumentGreen

                                pathText.contains("Simulated", ignoreCase = true) ||
                                        pathText.contains("Demo", ignoreCase = true) -> MaterialTheme.colorScheme.primary

                                else -> InstrumentMagenta
                            }
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                protocolDisplayName?.let { protocolText ->
                    SharedInstrumentMutedText(
                        text = "Protocol: $protocolText",
                        instrumentTextSecondary = instrumentTextSecondary
                    )
                }

                currentSweepSourceLabel?.let { sourceText ->
                    SharedInstrumentMutedText(
                        text = sourceText,
                        instrumentTextSecondary = instrumentTextSecondary
                    )
                }
            }

            SharedInstrumentDividerLine(
                instrumentDivider = instrumentDivider
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                SharedInstrumentMutedText(
                    text = String.format("%.2f → %.2f MHz", sweepStartMHz, sweepEndMHz),
                    instrumentTextSecondary = instrumentTextSecondary
                )

                SharedInstrumentMutedText(
                    text = "Step %.2f MHz".format(sweepStepMHz),
                    instrumentTextSecondary = instrumentTextSecondary
                )

                SharedInstrumentMutedText(
                    text = "Frames $sweepHistoryCount",
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                SharedInstrumentMutedText(
                    text = if (hasReferenceSweep) "Ref: ON" else "Ref: OFF",
                    instrumentTextSecondary = instrumentTextSecondary
                )

                SharedInstrumentMutedText(
                    text = when (traceCompareMode) {
                        TraceCompareMode.CURRENT_ONLY -> "Current"
                        TraceCompareMode.CURRENT_PLUS_REFERENCE -> "Current+Ref"
                        TraceCompareMode.DIFFERENCE -> "Δ Mode"
                    },
                    instrumentTextSecondary = instrumentTextSecondary
                )

                SharedInstrumentMutedText(
                    text = activeMarkerTargetLabel,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }
        }
    }
}

/*
########################################################################
SECTION 2
DISPLAY MODES CARD
########################################################################
PURPOSE
Renders the horizontal display mode selection card.
########################################################################
*/
@Composable
fun SweepDisplayModesCard(
    measurementCapabilities: HardwareMeasurementCapabilities,
    displayMode: SweepDisplayMode,
    onDisplayModeSelected: (SweepDisplayMode) -> Unit,
    instrumentSurface: Color,
    instrumentDivider: Color,
    instrumentAccent: Color,
    instrumentTextPrimary: Color
) {
    val widgetAccent = MaterialTheme.colorScheme.primary

    SharedInstrumentCard(
        instrumentSurface = instrumentSurface,
        instrumentDivider = instrumentDivider
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SharedInstrumentSectionHeader(
                text = "Display Modes",
                instrumentAccent = widgetAccent
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (measurementCapabilities.supportsSWR) {
                    SegmentedChoiceButton(
                        text = "SWR",
                        selected = displayMode == SweepDisplayMode.SWR,
                        onClick = { onDisplayModeSelected(SweepDisplayMode.SWR) }
                    )
                    SegmentedChoiceButton(
                        text = "Analog SWR",
                        selected = displayMode == SweepDisplayMode.ANALOG_SWR,
                        onClick = { onDisplayModeSelected(SweepDisplayMode.ANALOG_SWR) }
                    )
                    SegmentedChoiceButton(
                        text = "Waterfall",
                        selected = displayMode == SweepDisplayMode.WATERFALL,
                        onClick = { onDisplayModeSelected(SweepDisplayMode.WATERFALL) }
                    )
                }

                if (measurementCapabilities.supportsReturnLoss) {
                    SegmentedChoiceButton(
                        text = "RL",
                        selected = displayMode == SweepDisplayMode.RETURN_LOSS,
                        onClick = { onDisplayModeSelected(SweepDisplayMode.RETURN_LOSS) }
                    )
                    SegmentedChoiceButton(
                        text = "Analog RL",
                        selected = displayMode == SweepDisplayMode.ANALOG_RETURN_LOSS,
                        onClick = { onDisplayModeSelected(SweepDisplayMode.ANALOG_RETURN_LOSS) }
                    )
                }

                if (measurementCapabilities.supportsResistance) {
                    SegmentedChoiceButton(
                        text = "R",
                        selected = displayMode == SweepDisplayMode.RESISTANCE,
                        onClick = { onDisplayModeSelected(SweepDisplayMode.RESISTANCE) }
                    )
                    SegmentedChoiceButton(
                        text = "Analog R",
                        selected = displayMode == SweepDisplayMode.ANALOG_RESISTANCE,
                        onClick = { onDisplayModeSelected(SweepDisplayMode.ANALOG_RESISTANCE) }
                    )
                }

                if (measurementCapabilities.supportsReactance) {
                    SegmentedChoiceButton(
                        text = "X",
                        selected = displayMode == SweepDisplayMode.REACTANCE,
                        onClick = { onDisplayModeSelected(SweepDisplayMode.REACTANCE) }
                    )
                    SegmentedChoiceButton(
                        text = "Analog X",
                        selected = displayMode == SweepDisplayMode.ANALOG_REACTANCE,
                        onClick = { onDisplayModeSelected(SweepDisplayMode.ANALOG_REACTANCE) }
                    )
                }

                if (measurementCapabilities.supportsSmithChart) {
                    SegmentedChoiceButton(
                        text = "Smith",
                        selected = displayMode == SweepDisplayMode.SMITH,
                        onClick = { onDisplayModeSelected(SweepDisplayMode.SMITH) }
                    )
                }

                if (measurementCapabilities.supportsImpedanceLocus) {
                    SegmentedChoiceButton(
                        text = "Z Locus",
                        selected = displayMode == SweepDisplayMode.IMPEDANCE_LOCUS,
                        onClick = { onDisplayModeSelected(SweepDisplayMode.IMPEDANCE_LOCUS) }
                    )
                }

                if (measurementCapabilities.supportsS21) {
                    SegmentedChoiceButton(
                        text = "S21 Est",
                        selected = displayMode == SweepDisplayMode.S21_ESTIMATE,
                        onClick = { onDisplayModeSelected(SweepDisplayMode.S21_ESTIMATE) }
                    )
                }
            }
        }
    }
}

/*
########################################################################
SECTION 3
CONTROLS CARD
########################################################################
PURPOSE
Renders the main action row for sweep execution and reference controls.
########################################################################
*/
@Composable
fun SweepControlsCard(
    sweepResult: SweepResult?,
    referenceSweep: SweepResult?,
    showCsvPreview: Boolean,
    supportsCsvPreview: Boolean,
    runSweepButtonText: String,
    runSweepEnabled: Boolean,
    runSweepStatusText: String?,
    onRunSweep: () -> Unit,
    onSetReference: () -> Unit,
    onClearReference: () -> Unit,
    onToggleCsvPreview: () -> Unit,
    onBack: () -> Unit,
    instrumentSurface: Color,
    instrumentDivider: Color,
    instrumentAccent: Color,
    instrumentTextPrimary: Color,
    instrumentTextSecondary: Color
) {
    val widgetAccent = MaterialTheme.colorScheme.primary

    SharedInstrumentCard(
        instrumentSurface = instrumentSurface,
        instrumentDivider = instrumentDivider
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SharedInstrumentSectionHeader(
                text = "Controls",
                instrumentAccent = widgetAccent
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SweepWorkspaceControlButton(
                    text = runSweepButtonText,
                    enabled = runSweepEnabled,
                    isPrimaryAction = true,
                    onClick = onRunSweep
                )

                SweepWorkspaceControlButton(
                    text = "Set Reference",
                    enabled = sweepResult != null,
                    onClick = onSetReference
                )

                SweepWorkspaceControlButton(
                    text = "Clear Ref",
                    enabled = referenceSweep != null,
                    onClick = onClearReference
                )

                SweepWorkspaceControlButton(
                    text = if (showCsvPreview) "Hide CSV" else "Export CSV",
                    enabled = sweepResult != null && supportsCsvPreview,
                    onClick = onToggleCsvPreview
                )

                SweepWorkspaceControlButton(
                    text = "Back",
                    onClick = onBack
                )
            }

            runSweepStatusText?.let { statusText ->
                SharedInstrumentMutedText(
                    text = statusText,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }
        }
    }
}

/*
########################################################################
SECTION 4
SWEEP SUMMARY CARD
########################################################################
PURPOSE
Renders the sweep summary and first-pass cable preview block.
########################################################################
*/
@Composable
fun SweepSummaryCard(
    result: SweepResult,
    resonanceMHz: Double?,
    measurementCapabilities: HardwareMeasurementCapabilities,
    hardware: TestHardwareProfile,
    instrumentSurface: Color,
    instrumentDivider: Color,
    instrumentAccent: Color,
    instrumentTextPrimary: Color,
    instrumentTextSecondary: Color
) {
    val minimumSwrPoint = result.points.minByOrNull { it.swr }
    val swr2BandwidthMHz = estimateBandwidthAtOrBelowSwr(result, 2.0)
    val widgetAccent = MaterialTheme.colorScheme.primary

    SharedInstrumentCard(
        instrumentSurface = instrumentSurface,
        instrumentDivider = instrumentDivider
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SharedInstrumentSectionHeader(
                text = "Sweep Summary",
                instrumentAccent = widgetAccent
            )

            resonanceMHz?.let {
                // §10b item 1: this is the minimum-SWR frequency (best match), NOT the
                // reactance-null "Electrical Resonance" shown in Diagnostics. The caption
                // makes the distinction legible so the two numbers don't read as a
                // contradiction. Display only — the value is unchanged.
                SharedTwoValueRow(
                    label = "Best-Match Frequency",
                    value = String.format("%.3f MHz", it),
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )
                SharedInstrumentMutedText(
                    text = "Lowest SWR — the best match to 50 Ω.",
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            minimumSwrPoint?.let { point ->
                SharedTwoValueRow(
                    label = "Minimum SWR",
                    value = String.format("%.3f at %.3f MHz", point.swr, point.frequencyMHz),
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            swr2BandwidthMHz?.let {
                SharedTwoValueRow(
                    label = "SWR ≤ 2 Bandwidth",
                    value = String.format("%.3f MHz", it),
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            SharedTwoValueRow(
                label = "Points Collected",
                value = if (result.isComplete) {
                    result.actualPointCount.toString()
                } else {
                    "${result.actualPointCount} / ${result.requestedPointCount}"
                },
                instrumentTextPrimary = instrumentTextPrimary,
                instrumentTextSecondary = instrumentTextSecondary
            )

            if (!result.isComplete) {
                SharedInstrumentValueText(
                    text = "⚠ Incomplete sweep — measured ${result.actualPointCount} of " +
                            "${result.requestedPointCount} points",
                    instrumentTextPrimary = instrumentAccent
                )
            }

            SharedTwoValueRow(
                // ALWAYS the app's calibration, never ambiguous with the device's own.
                label = "App calibration",
                value = if (result.isCalibrated) {
                    result.calibrationLabel.ifBlank { "Applied" }
                } else {
                    "Not applied"
                },
                instrumentTextPrimary = instrumentTextPrimary,
                instrumentTextSecondary = instrumentTextSecondary
            )

            if (!result.isCalibrated) {
                SharedInstrumentValueText(
                    text = "⚠ Uncalibrated sweep — no OSL correction applied; " +
                            "treat impedance and SWR with reduced trust",
                    instrumentTextPrimary = instrumentAccent
                )
            }

            if (measurementCapabilities.supportsTDR) {
                SharedInstrumentSubHeader(
                    text = "Cable Fault / TDR Preview",
                    instrumentTextPrimary = instrumentTextPrimary
                )
                SharedInstrumentValueText(
                    text = buildCableFaultPreview(result, measurementCapabilities, hardware),
                    instrumentTextPrimary = instrumentTextPrimary
                )
            }
        }
    }
}

/*
########################################################################
SECTION 5
WORKSPACE BUTTONS
########################################################################
PURPOSE
Shared button styling for display and action rows in the sweep
workspace.
########################################################################
*/
@Composable
fun SweepWorkspaceControlButton(
    text: String,
    enabled: Boolean = true,
    isPrimaryAction: Boolean = false,
    onClick: () -> Unit
) {
    val fillColor =
        if (isPrimaryAction) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
        }

    val contentColor =
        if (isPrimaryAction) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    val borderColor =
        if (isPrimaryAction) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
        }

    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = fillColor,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = BorderStroke(
            width = 1.dp,
            color = borderColor
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isPrimaryAction) 4.dp else 3.dp,
            pressedElevation = 1.dp,
            disabledElevation = 0.dp
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = text,
            fontWeight = if (isPrimaryAction) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

/*
########################################################################
SECTION 6
GRID DRAWING
########################################################################
PURPOSE
Draws the background graph grid used by sweep displays.
########################################################################
*/
fun DrawScope.drawSweepGrid(
    horizontalLines: Int,
    verticalLines: Int,
    color: Color
) {
    val width = size.width
    val height = size.height

    if (horizontalLines > 0) {
        val spacing = height / horizontalLines

        for (i in 0..horizontalLines) {
            val y = i * spacing

            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }
    }

    if (verticalLines > 0) {
        val spacing = width / verticalLines

        for (i in 0..verticalLines) {
            val x = i * spacing

            drawLine(
                color = color,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f
            )
        }
    }
}

/*
########################################################################
SECTION 7
TRACE DRAWING
########################################################################
PURPOSE
Draws a sweep trace line from a list of graph points.
########################################################################
*/
fun DrawScope.drawSweepTrace(
    points: List<Offset>,
    color: Color,
    strokeWidth: Float
) {
    if (points.size < 2) return

    for (i in 0 until points.size - 1) {
        val p1 = points[i]
        val p2 = points[i + 1]

        drawLine(
            color = color,
            start = p1,
            end = p2,
            strokeWidth = strokeWidth
        )
    }
}

/*
########################################################################
SECTION 8
MARKER DRAWING
########################################################################
PURPOSE
Draws a circular marker on a trace.
########################################################################
*/
fun DrawScope.drawSweepMarker(
    position: Offset,
    color: Color
) {
    drawCircle(
        color = color,
        radius = 6f,
        center = position
    )
}

/*
########################################################################
SECTION 9
SAFE RANGE HELPERS
########################################################################
PURPOSE
Provides safe clamping for graph scaling operations.
########################################################################
*/
fun safeMin(
    a: Float,
    b: Float
): Float {
    return min(a, b)
}

fun safeMax(
    a: Float,
    b: Float
): Float {
    return max(a, b)
}

/*
########################################################################
SECTION 10
NORMALISATION HELPERS
########################################################################
PURPOSE
Converts values into graph coordinate space.
########################################################################
*/
fun normaliseValue(
    value: Float,
    min: Float,
    max: Float
): Float {
    val range = max - min

    if (range == 0f) return 0f

    return (value - min) / range
}

/*
########################################################################
SECTION 11
SCALAR TRACE CANVAS
########################################################################
PURPOSE
Provides a reusable scalar trace graph surface for sweep displays.
########################################################################
*/
@Composable
fun ScalarTraceGraphCanvas(
    modifier: Modifier = Modifier,
    surfaceColor: Color,
    borderColor: Color,
    gridColor: Color,
    resonanceLineColor: Color,
    markerAColor: Color,
    markerBColor: Color,
    currentTraceColor: Color,
    referenceTraceColor: Color,
    differenceTraceColor: Color,
    zeroLineColor: Color,
    activeValues: List<Double>,
    currentValues: List<Double>,
    referenceValues: List<Double>,
    differenceValues: List<Double>,
    showReferenceTrace: Boolean,
    showDifferenceTrace: Boolean,
    resonanceIndex: Int?,
    markerAIndex: Int,
    markerBIndex: Int,
    minValue: Double,
    range: Double
) {
    Surface(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
        color = surfaceColor,
        shape = RoundedCornerShape(14.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // Same number ChartLayoutMath.plotInsetsFor folds into its
                // start/end inset — read from there so the two cannot drift.
                .padding(ChartLayoutMath.SCALAR_CANVAS_PADDING_DP.dp)
        ) {
            if (activeValues.isEmpty()) return@Canvas

            drawSweepGrid(
                horizontalLines = 4,
                verticalLines = 4,
                color = gridColor
            )

            if (showDifferenceTrace && minValue < 0.0 && (minValue + range) > 0.0) {
                val zeroY = size.height - ((((0.0 - minValue) / range).toFloat()) * size.height)
                drawLine(
                    color = zeroLineColor,
                    start = Offset(0f, zeroY),
                    end = Offset(size.width, zeroY),
                    strokeWidth = 2f
                )
            }

            resonanceIndex?.let { index ->
                val resonanceX =
                    if (currentValues.size > 1) {
                        index * (size.width / (currentValues.size - 1))
                    } else {
                        size.width / 2f
                    }

                drawLine(
                    color = resonanceLineColor,
                    start = Offset(resonanceX, 0f),
                    end = Offset(resonanceX, size.height),
                    strokeWidth = 2f
                )
            }

            if (showReferenceTrace && referenceValues.isNotEmpty()) {
                drawScalarTraceValues(
                    values = referenceValues,
                    color = referenceTraceColor,
                    canvasWidth = size.width,
                    canvasHeight = size.height,
                    minValue = minValue,
                    range = range,
                    strokeWidth = 2f
                )
            }

            if (showDifferenceTrace) {
                drawScalarTraceValues(
                    values = differenceValues,
                    color = differenceTraceColor,
                    canvasWidth = size.width,
                    canvasHeight = size.height,
                    minValue = minValue,
                    range = range,
                    strokeWidth = 3f
                )
            } else {
                drawScalarTraceValues(
                    values = currentValues,
                    color = currentTraceColor,
                    canvasWidth = size.width,
                    canvasHeight = size.height,
                    minValue = minValue,
                    range = range,
                    strokeWidth = 3f
                )
            }

            val widthStep =
                if (activeValues.size > 1) {
                    size.width / (activeValues.size - 1)
                } else {
                    size.width
                }

            val markerAClamped = markerAIndex.coerceIn(0, activeValues.lastIndex)
            val markerBClamped = markerBIndex.coerceIn(0, activeValues.lastIndex)

            val markerAX = markerAClamped * widthStep
            val markerBX = markerBClamped * widthStep

            val markerAY =
                size.height - ((((activeValues[markerAClamped] - minValue) / range).toFloat()) * size.height)
            val markerBY =
                size.height - ((((activeValues[markerBClamped] - minValue) / range).toFloat()) * size.height)

            drawSweepMarker(
                position = Offset(markerAX, markerAY),
                color = markerAColor
            )

            drawSweepMarker(
                position = Offset(markerBX, markerBY),
                color = markerBColor
            )
        }
    }
}

/*
########################################################################
SECTION 12
SCALAR TRACE VALUE DRAWING
########################################################################
PURPOSE
Converts scalar values into graph points and renders a line trace.
########################################################################
*/
fun DrawScope.drawScalarTraceValues(
    values: List<Double>,
    color: Color,
    canvasWidth: Float,
    canvasHeight: Float,
    minValue: Double,
    range: Double,
    strokeWidth: Float
) {
    if (values.isEmpty()) return

    val widthStep =
        if (values.size > 1) {
            canvasWidth / (values.size - 1)
        } else {
            canvasWidth
        }

    val tracePoints = values.mapIndexed { index, value ->
        val x = index * widthStep
        val normalized = ((value - minValue) / range).toFloat()
        val y = canvasHeight - (normalized * canvasHeight)
        Offset(x, y)
    }

    drawSweepTrace(
        points = tracePoints,
        color = color,
        strokeWidth = strokeWidth
    )
}

/*
########################################################################
SECTION 13
SCALAR TRACE VIEW
########################################################################
PURPOSE
Renders standard scalar traces such as SWR, return loss, resistance,
reactance, and S21 estimate.
########################################################################
*/
@Composable
fun SweepScalarTraceView(
    result: SweepResult,
    referenceResult: SweepResult?,
    traceCompareMode: TraceCompareMode,
    mode: SweepDisplayMode,
    markerAIndex: Int,
    markerBIndex: Int,
    instrumentSurfaceVariant: Color,
    instrumentDivider: Color,
    instrumentAccent: Color,
    instrumentTextPrimary: Color,
    instrumentTextSecondary: Color,
    instrumentBlue: Color,
    instrumentMagenta: Color,
    instrumentGreen: Color,
    /*
    Plot height. Defaults to the original 240.dp so every existing call
    site is unchanged; the Phase-3 chart grid passes a smaller value to
    fit a cell.
    */
    heightDp: Dp = 240.dp,
    /*
    Half-width hosting (the Phase-3 chart grid cell). Full width is the
    default, so every existing call site keeps today's layout exactly.
    Compact narrows the gutter and thins the axis labels so they fit
    instead of collapsing into stacked digit fragments.

    GEOMETRY ONLY — this is a statement about WIDTH. It used to also gate
    the header and footer, which is a statement about HOSTING; see below.
    */
    compact: Boolean = false,
    /*
    Whether to draw the embedded header and footer. Split out of `compact`
    in slice 4c-i, because the two reasons were never the same reason: the
    chrome is suppressed in a grid cell because THE CELL ALREADY TITLES THE
    CHART and the grid always renders CURRENT_ONLY (making the footer a
    constant string) — both true at any width.

    Conflated, a full-width sole chart in the grid (gridColumnCount(1) == 1)
    could not take the wide gutter without also gaining a second title and a
    constant footer. Defaults to !compact, so every existing call site is
    byte-for-byte unchanged; the grid passes false explicitly at both widths.
    */
    showHeaderAndFooter: Boolean = !compact
) {
    val points = result.points
    val currentValues = points.map { getDisplayValue(it, mode) }
    val referenceValues = referenceResult?.points?.map { getDisplayValue(it, mode) }.orEmpty()
    val differenceValues = buildDifferenceValues(
        currentValues = currentValues,
        referenceValues = referenceValues
    )
    val resonanceIndex = points.indices.minByOrNull { index -> points[index].swr }
    val axisBounds = buildTraceAxisBounds(
        mode = mode,
        traceCompareMode = traceCompareMode,
        currentValues = currentValues,
        referenceValues = referenceValues,
        differenceValues = differenceValues
    )
    val minValue = axisBounds.minimum
    val range = axisBounds.range
    val yAxisLabels =
        buildAxisLabels(axisBounds.maximum, minValue, count = if (compact) 3 else 5)
    val frequencyTicks = buildFrequencyTicks(
        startMHz = result.startFrequencyMHz,
        endMHz = result.endFrequencyMHz
    )
    // Shared with PhaseTraceCell since slice 4b; rationale lives with the
    // rule, at ChartLayoutMath EDIT SECTION 1006.
    val visibleFrequencyTicks =
        ChartLayoutMath.visibleFrequencyTicks(frequencyTicks, compact = compact)

    /*
    Gutter and the tick row's start padding are the same measurement; they
    were previously written as 56+8 and a separately-hardcoded 64, and the
    canvas padding below was a third independent literal.

    plotInsetsFor is now the single source: startDp is the FULL inset to the
    plotting area (gutter + canvas padding), so the label column recovers its
    own width by subtracting the padding back off. Anything drawn ALONGSIDE
    this trace insets by startDp/endDp and lines up by construction.
    */
    val plotInsets = ChartLayoutMath.plotInsetsFor(
        renderer = ChartLayoutMath.PlotRenderer.SCALAR,
        compact = compact
    )
    val axisGutter =
        (plotInsets.startDp - ChartLayoutMath.SCALAR_CANVAS_PADDING_DP).dp
    val axisGutterEnd = if (compact) 4.dp else 8.dp

    val widgetAccent = MaterialTheme.colorScheme.primary

    val activeValues =
        when (traceCompareMode) {
            TraceCompareMode.CURRENT_ONLY -> currentValues
            TraceCompareMode.CURRENT_PLUS_REFERENCE -> currentValues
            TraceCompareMode.DIFFERENCE -> differenceValues
        }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Suppressed in a grid cell: the cell already renders the ChartKind
        // title, so this would be a second one. Keyed on hosting, not width —
        // that stays true for a full-width sole chart in the grid.
        if (showHeaderAndFooter) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SharedInstrumentSubHeader(
                    text = getTraceAxisTitle(mode, traceCompareMode),
                    instrumentTextPrimary = instrumentTextPrimary
                )
                SharedInstrumentMutedText(
                    text = getTraceModeSummary(traceCompareMode),
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }
        }

        Row {
            Column(
                modifier = Modifier
                    .width(axisGutter)
                    .height(heightDp)
                    .padding(end = axisGutterEnd),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                yAxisLabels.forEach { label ->
                    SharedInstrumentMutedText(
                        text = label,
                        instrumentTextSecondary = instrumentTextSecondary
                    )
                }
            }

            ScalarTraceGraphCanvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightDp),
                surfaceColor = instrumentSurfaceVariant,
                borderColor = instrumentDivider,
                gridColor = instrumentDivider.copy(alpha = 0.85f),
                resonanceLineColor = widgetAccent,
                markerAColor = instrumentBlue,
                markerBColor = instrumentMagenta,
                currentTraceColor = widgetAccent,
                referenceTraceColor = instrumentTextSecondary,
                differenceTraceColor = instrumentMagenta,
                zeroLineColor = instrumentGreen,
                activeValues = activeValues,
                currentValues = currentValues,
                referenceValues = referenceValues,
                differenceValues = differenceValues,
                showReferenceTrace = traceCompareMode == TraceCompareMode.CURRENT_PLUS_REFERENCE,
                showDifferenceTrace = traceCompareMode == TraceCompareMode.DIFFERENCE,
                resonanceIndex = resonanceIndex,
                markerAIndex = markerAIndex,
                markerBIndex = markerBIndex,
                minValue = minValue,
                range = range
            )
        }

        /*
        Padded on BOTH sides by the true plot insets, so SpaceBetween
        distributes across exactly the extent the trace occupies and the first
        and last labels land ON the plot edges.

        This used to pad start = axisGutter + axisGutterEnd (44 compact / 64
        full) against a plot starting at 50/66, putting every label 6 dp (cell)
        or 2 dp (full) left of the value it annotates; and with no end padding
        the last label sat at the row's edge while the plot stopped 10 dp
        inside, so the right-hand tick overhung. Both fixed here.
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
            visibleFrequencyTicks.forEach { tick ->
                SharedInstrumentMutedText(
                    text = tick,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }
        }

        // Suppressed in a grid cell: the grid always renders CURRENT_ONLY with
        // no reference, so this footer is a constant string there, and the
        // resonance frequency is already carried by the workspace's own cards.
        // Keyed on hosting, not width — a wider cell does not make a constant
        // string informative.
        if (showHeaderAndFooter) {
            when (traceCompareMode) {
                TraceCompareMode.CURRENT_ONLY -> {
                    SharedInstrumentValueText(
                        text = "Trace Mode: Current only",
                        instrumentTextPrimary = instrumentTextPrimary
                    )
                }

                TraceCompareMode.CURRENT_PLUS_REFERENCE -> {
                    SharedInstrumentValueText(
                        text = "Trace Overlay: Primary = current, Grey = reference",
                        instrumentTextPrimary = instrumentTextPrimary
                    )
                }

                TraceCompareMode.DIFFERENCE -> {
                    SharedInstrumentValueText(
                        text = "Trace Math: Magenta = current minus reference, Green = zero line",
                        instrumentTextPrimary = instrumentTextPrimary
                    )
                }
            }

            resonanceIndex?.let { index ->
                SharedInstrumentMutedText(
                    text = String.format(
                        "Resonance marker: %.3f MHz",
                        result.points[index].frequencyMHz
                    ),
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }
        }
    }
}

/*
########################################################################
SECTION 14
WATERFALL SWEEP VIEW
########################################################################
PURPOSE
Shows stored sweep frames as a simple waterfall display using SWR
intensity across frequency and time.
########################################################################
*/
@Composable
fun SweepWaterfallSweepView(
    sweepHistory: List<SweepResult>,
    markerAIndex: Int,
    instrumentSurfaceVariant: Color,
    instrumentDivider: Color,
    instrumentTextPrimary: Color
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SharedInstrumentSectionHeader(
            text = "Waterfall Sweep Display",
            instrumentAccent = MaterialTheme.colorScheme.primary
        )

        if (sweepHistory.isEmpty()) {
            SharedInstrumentMutedText(
                text = "Run at least one sweep to build waterfall history.",
                instrumentTextSecondary = instrumentTextPrimary
            )
            return
        }

        val latest = sweepHistory.first()
        val markerPoint = latest.points.getOrNull(markerAIndex)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .border(1.dp, instrumentDivider, RoundedCornerShape(14.dp)),
            color = instrumentSurfaceVariant,
            shape = RoundedCornerShape(14.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                val rowCount = sweepHistory.size.coerceAtLeast(1)
                val columnCount = sweepHistory.maxOfOrNull { it.points.size }?.coerceAtLeast(1) ?: 1
                val cellWidth = size.width / columnCount
                val cellHeight = size.height / rowCount

                sweepHistory.forEachIndexed { rowIndex, sweep ->
                    val maxSwr = sweep.points.maxOfOrNull { it.swr }?.coerceAtLeast(1.0) ?: 1.0

                    sweep.points.forEachIndexed { columnIndex, point ->
                        val normalized = (point.swr / maxSwr).coerceIn(0.0, 1.0).toFloat()
                        val cellColor = Color(
                            red = normalized,
                            green = 0.20f + (0.45f * normalized),
                            blue = 0.12f,
                            alpha = 1f
                        )

                        drawRect(
                            color = cellColor,
                            topLeft = Offset(columnIndex * cellWidth, rowIndex * cellHeight),
                            size = Size(cellWidth + 1f, cellHeight + 1f)
                        )
                    }
                }

                val latestPointCount = latest.points.size.coerceAtLeast(1)
                val markerX =
                    if (latestPointCount > 1) {
                        markerAIndex.coerceIn(0, latestPointCount - 1) * (size.width / latestPointCount)
                    } else {
                        size.width / 2f
                    }

                drawLine(
                    color = instrumentTextPrimary,
                    start = Offset(markerX, 0f),
                    end = Offset(markerX, size.height),
                    strokeWidth = 2f
                )
            }
        }

        SharedInstrumentMutedText(
            text = "Top row is newest sweep. Lower rows are older stored sweeps.",
            instrumentTextSecondary = instrumentTextPrimary
        )

        markerPoint?.let {
            SharedInstrumentValueText(
                text = String.format(
                    "Marker A in latest frame: %.3f MHz  |  SWR %.3f",
                    it.frequencyMHz,
                    it.swr
                ),
                instrumentTextPrimary = instrumentTextPrimary
            )
        }
    }
}

/*
########################################################################
SECTION 15
SMITH CHART VIEW
########################################################################
PURPOSE
Draws a simple Smith-style preview using normalized resistance and
reactance mapping.
########################################################################
*/
@Composable
fun SweepSmithChartView(
    result: SweepResult,
    markerAIndex: Int,
    markerBIndex: Int,
    instrumentSurfaceVariant: Color,
    instrumentDivider: Color,
    instrumentAccent: Color,
    instrumentTextSecondary: Color,
    markerAColor: Color,
    markerBColor: Color,
    /*
    Plot height. Defaults to the original 280.dp so every existing call
    site is unchanged; the Phase-3 chart grid passes a smaller value to
    fit a cell.
    */
    heightDp: Dp = 280.dp
) {
    val points = result.points
    val widgetAccent = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp)
            .border(1.dp, instrumentDivider, RoundedCornerShape(14.dp)),
        color = instrumentSurfaceVariant,
        shape = RoundedCornerShape(14.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) * 0.42f

            drawCircle(
                color = instrumentTextSecondary,
                radius = radius,
                center = center,
                style = Stroke(width = 2f)
            )

            drawLine(
                color = instrumentDivider,
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 1f
            )

            drawCircle(
                color = instrumentDivider.copy(alpha = 0.6f),
                radius = radius * 0.5f,
                center = center,
                style = Stroke(width = 1f)
            )

            val tracePoints = points.map { point ->
                val normR = ((point.resistance - 50.0) / 100.0).toFloat()
                val normX = (point.reactance / 100.0).toFloat()

                val px = center.x + normR.coerceIn(-1f, 1f) * radius
                val py = center.y - normX.coerceIn(-1f, 1f) * radius
                Offset(px, py)
            }

            drawSweepTrace(
                points = tracePoints,
                color = widgetAccent,
                strokeWidth = 3f
            )

            tracePoints.getOrNull(markerAIndex)?.let { markerOffset ->
                drawSweepMarker(
                    position = markerOffset,
                    color = markerAColor
                )
            }

            tracePoints.getOrNull(markerBIndex)?.let { markerOffset ->
                drawSweepMarker(
                    position = markerOffset,
                    color = markerBColor
                )
            }
        }
    }
}

/*
########################################################################
SECTION 16
IMPEDANCE LOCUS VIEW
########################################################################
PURPOSE
Draws a resistance/reactance path using the current sweep samples.
########################################################################
*/
@Composable
fun SweepImpedanceLocusView(
    result: SweepResult,
    markerAIndex: Int,
    markerBIndex: Int,
    instrumentSurfaceVariant: Color,
    instrumentDivider: Color,
    instrumentAccent: Color,
    markerAColor: Color,
    markerBColor: Color
) {
    val points = result.points
    val widgetAccent = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .border(1.dp, instrumentDivider, RoundedCornerShape(14.dp)),
        color = instrumentSurfaceVariant,
        shape = RoundedCornerShape(14.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            if (points.isEmpty()) return@Canvas

            val minR = points.minOf { it.resistance }
            val maxR = points.maxOf { it.resistance }
            val minX = points.minOf { it.reactance }
            val maxX = points.maxOf { it.reactance }

            val rRange = (maxR - minR).coerceAtLeast(0.0001)
            val xRange = (maxX - minX).coerceAtLeast(0.0001)

            drawLine(
                color = instrumentDivider,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 1f
            )

            drawLine(
                color = instrumentDivider,
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = 1f
            )

            val tracePoints = points.map { point ->
                val x = (((point.resistance - minR) / rRange) * size.width).toFloat()
                val y = size.height - (((point.reactance - minX) / xRange) * size.height).toFloat()
                Offset(x, y)
            }

            drawSweepTrace(
                points = tracePoints,
                color = widgetAccent,
                strokeWidth = 3f
            )

            tracePoints.getOrNull(markerAIndex)?.let { markerOffset ->
                drawSweepMarker(
                    position = markerOffset,
                    color = markerAColor
                )
            }

            tracePoints.getOrNull(markerBIndex)?.let { markerOffset ->
                drawSweepMarker(
                    position = markerOffset,
                    color = markerBColor
                )
            }
        }
    }
}
