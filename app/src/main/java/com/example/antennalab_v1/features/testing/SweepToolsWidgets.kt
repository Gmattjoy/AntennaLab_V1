package com.example.antennalab_v1.features.testing

/*
########################################################################
FILE: SweepToolsWidgets.kt
PACKAGE: com.example.antennalab_v1.features.testing
LAYER: UI / Testing Tools / Workspace Panels

LAST UPDATED 4/4/2026 21:20

SYSTEM ROLE
Provides reusable workspace tool panels for the sweep workspace host.

CURRENT DEVELOPMENT ROLE
This file now owns:

• extracted trace memory / compare panel UI
• extracted professional marker control panel UI
• extracted live measurement dashboard UI
• extracted CSV preview panel UI

IMPORTANT ARCHITECTURE RULE
This file must remain focused on UI panels and interaction widgets.

Do NOT place in this file:

• sweep acquisition logic
• USB/device communication
• storage logic
• domain analysis logic

SAFE EDIT AREA
- add compare tool panels later
- add export tool panels later
- add marker jump tools later
- add future workspace utility panels later
########################################################################
*/

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import kotlin.math.abs

@Composable
fun SweepTraceMemoryPanel(
    currentSweep: SweepResult?,
    referenceSweep: SweepResult?,
    traceCompareMode: TraceCompareMode,
    sweepHistoryCount: Int,
    onUseCurrentAsReference: () -> Unit,
    onUsePreviousHistoryAsReference: () -> Unit,
    onClearReference: () -> Unit,
    onTraceModeChange: (TraceCompareMode) -> Unit,
    instrumentSurface: Color,
    instrumentDivider: Color,
    instrumentAccent: Color,
    instrumentTextPrimary: Color,
    instrumentTextSecondary: Color
) {
    val widgetAccent = MaterialTheme.colorScheme.primary
    val widgetSurface = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)

    SharedInstrumentCard(
        instrumentSurface = instrumentSurface,
        instrumentDivider = instrumentDivider
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SharedInstrumentSectionHeader(
                text = "Trace Memory / Compare",
                instrumentAccent = widgetAccent
            )

            SharedInstrumentDividerLine(
                instrumentDivider = instrumentDivider
            )

            SharedTwoValueRow(
                label = "Current Trace",
                value = if (currentSweep != null) {
                    String.format(
                        "%.3f → %.3f MHz  |  %d points",
                        currentSweep.startFrequencyMHz,
                        currentSweep.endFrequencyMHz,
                        currentSweep.points.size
                    )
                } else {
                    "No active sweep loaded."
                },
                instrumentTextPrimary = instrumentTextPrimary,
                instrumentTextSecondary = instrumentTextSecondary
            )

            SharedTwoValueRow(
                label = "Reference Trace",
                value = if (referenceSweep != null) {
                    String.format(
                        "%.3f → %.3f MHz  |  %d points",
                        referenceSweep.startFrequencyMHz,
                        referenceSweep.endFrequencyMHz,
                        referenceSweep.points.size
                    )
                } else {
                    "No reference trace set."
                },
                instrumentTextPrimary = instrumentTextPrimary,
                instrumentTextSecondary = instrumentTextSecondary
            )

            SharedTwoValueRow(
                label = "Stored Sweep History",
                value = sweepHistoryCount.toString(),
                instrumentTextPrimary = instrumentTextPrimary,
                instrumentTextSecondary = instrumentTextSecondary
            )

            SharedInstrumentDividerLine(
                instrumentDivider = instrumentDivider
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SweepWorkspaceActionButton(
                    label = "Use Current as Ref",
                    enabled = currentSweep != null,
                    onClick = onUseCurrentAsReference,
                    instrumentSurfaceVariant = widgetSurface,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )

                SweepWorkspaceActionButton(
                    label = "Recall Previous",
                    enabled = sweepHistoryCount >= 2,
                    onClick = onUsePreviousHistoryAsReference,
                    instrumentSurfaceVariant = widgetSurface,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )

                SweepWorkspaceActionButton(
                    label = "Clear Ref",
                    enabled = referenceSweep != null,
                    onClick = onClearReference,
                    instrumentSurfaceVariant = widgetSurface,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            SharedInstrumentDividerLine(
                instrumentDivider = instrumentDivider
            )

            SharedInstrumentSubHeader(
                text = "Trace Math Mode",
                instrumentTextPrimary = instrumentTextPrimary
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SweepWorkspaceDisplayButton(
                    label = "Current",
                    active = traceCompareMode == TraceCompareMode.CURRENT_ONLY,
                    enabled = true,
                    onClick = { onTraceModeChange(TraceCompareMode.CURRENT_ONLY) },
                    instrumentAccent = widgetAccent,
                    instrumentBackground = instrumentSurface,
                    instrumentSurfaceVariant = widgetSurface,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )

                SweepWorkspaceDisplayButton(
                    label = "Current + Ref",
                    active = traceCompareMode == TraceCompareMode.CURRENT_PLUS_REFERENCE,
                    enabled = referenceSweep != null,
                    onClick = { onTraceModeChange(TraceCompareMode.CURRENT_PLUS_REFERENCE) },
                    instrumentAccent = widgetAccent,
                    instrumentBackground = instrumentSurface,
                    instrumentSurfaceVariant = widgetSurface,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )

                SweepWorkspaceDisplayButton(
                    label = "Difference",
                    active = traceCompareMode == TraceCompareMode.DIFFERENCE,
                    enabled = referenceSweep != null,
                    onClick = { onTraceModeChange(TraceCompareMode.DIFFERENCE) },
                    instrumentAccent = widgetAccent,
                    instrumentBackground = instrumentSurface,
                    instrumentSurfaceVariant = widgetSurface,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            SharedInstrumentMutedText(
                text = "Use reference overlay or difference mode to compare before/after tuning changes such as trimming, matching, or rebuild adjustments.",
                instrumentTextSecondary = instrumentTextSecondary
            )
        }
    }
}

@Composable
fun SweepMarkerControlPanel(
    targetFrequencyMHz: Double,
    activeMarkerIsA: Boolean,
    markerAIndex: Int,
    markerBIndex: Int,
    resonanceIndex: Int,
    peakCount: Int,
    highestPeakAvailable: Boolean,
    bandwidthMarkerPairAvailable: Boolean,
    searchSourceLabel: String,
    onSelectMarkerA: () -> Unit,
    onSelectMarkerB: () -> Unit,
    onActiveMarkerNudge: (Int) -> Unit,
    onMarkerANudge: (Int) -> Unit,
    onMarkerBNudge: (Int) -> Unit,
    onPeakSearch: () -> Unit,
    onMoveActiveToResonance: () -> Unit,
    onNextPeak: () -> Unit,
    onPreviousPeak: () -> Unit,
    onMoveAToResonance: () -> Unit,
    onMoveBToResonance: () -> Unit,
    onMoveAToCenter: () -> Unit,
    onMoveBToCenter: () -> Unit,
    onMoveActiveToTarget: () -> Unit,
    onMoveActiveToUserFrequency: (Double?) -> Unit,
    onMoveAToTarget: () -> Unit,
    onMoveBToTarget: () -> Unit,
    onPlaceBandwidthMarkers: () -> Unit,
    onPlaceFullSpanMarkers: () -> Unit,
    instrumentSurface: Color,
    instrumentSurfaceVariant: Color,
    instrumentDivider: Color,
    instrumentAccent: Color,
    instrumentTextPrimary: Color,
    instrumentTextSecondary: Color
) {
    var userFrequencyText by remember(targetFrequencyMHz) {
        mutableStateOf(String.format("%.3f", targetFrequencyMHz))
    }

    val parsedUserFrequencyMHz = userFrequencyText.toDoubleOrNull()
    val userFrequencyEntryValid = parsedUserFrequencyMHz != null
    val widgetAccent = MaterialTheme.colorScheme.primary
    val widgetSurface = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)

    SharedInstrumentCard(
        instrumentSurface = instrumentSurface,
        instrumentDivider = instrumentDivider
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SharedInstrumentSectionHeader(
                text = "Professional Marker System",
                instrumentAccent = widgetAccent
            )

            SharedInstrumentDividerLine(
                instrumentDivider = instrumentDivider
            )

            SharedInstrumentSubHeader(
                text = "Active Marker",
                instrumentTextPrimary = instrumentTextPrimary
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SweepWorkspaceDisplayButton(
                    label = "Marker A",
                    active = activeMarkerIsA,
                    enabled = true,
                    onClick = onSelectMarkerA,
                    instrumentAccent = widgetAccent,
                    instrumentBackground = instrumentSurface,
                    instrumentSurfaceVariant = widgetSurface,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )
                SweepWorkspaceDisplayButton(
                    label = "Marker B",
                    active = !activeMarkerIsA,
                    enabled = true,
                    onClick = onSelectMarkerB,
                    instrumentAccent = widgetAccent,
                    instrumentBackground = instrumentSurface,
                    instrumentSurfaceVariant = widgetSurface,
                    instrumentTextPrimary = instrumentTextPrimary,
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            SharedInstrumentDividerLine(
                instrumentDivider = instrumentDivider
            )

            SharedInstrumentSubHeader(
                text = "Quick Nudge",
                instrumentTextPrimary = instrumentTextPrimary
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SweepWorkspaceActionButton("Active -", true, { onActiveMarkerNudge(-1) }, widgetSurface, instrumentTextPrimary, instrumentTextSecondary)
                SweepWorkspaceActionButton("Active +", true, { onActiveMarkerNudge(1) }, widgetSurface, instrumentTextPrimary, instrumentTextSecondary)
                SweepWorkspaceActionButton("A-", true, { onMarkerANudge(-1) }, widgetSurface, instrumentTextPrimary, instrumentTextSecondary)
                SweepWorkspaceActionButton("A+", true, { onMarkerANudge(1) }, widgetSurface, instrumentTextPrimary, instrumentTextSecondary)
                SweepWorkspaceActionButton("B-", true, { onMarkerBNudge(-1) }, widgetSurface, instrumentTextPrimary, instrumentTextSecondary)
                SweepWorkspaceActionButton("B+", true, { onMarkerBNudge(1) }, widgetSurface, instrumentTextPrimary, instrumentTextSecondary)
            }

            SharedInstrumentSubHeader(
                text = "Search Tools",
                instrumentTextPrimary = instrumentTextPrimary
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SweepWorkspaceActionButton("Peak Search", highestPeakAvailable, onPeakSearch, widgetSurface, instrumentTextPrimary, instrumentTextSecondary)
                SweepWorkspaceActionButton("Min SWR", true, onMoveActiveToResonance, widgetSurface, instrumentTextPrimary, instrumentTextSecondary)
                SweepWorkspaceActionButton("Next Peak", peakCount > 0, onNextPeak, widgetSurface, instrumentTextPrimary, instrumentTextSecondary)
                SweepWorkspaceActionButton("Previous Peak", peakCount > 0, onPreviousPeak, widgetSurface, instrumentTextPrimary, instrumentTextSecondary)
            }

            SharedInstrumentSubHeader(
                text = "Jump Tools",
                instrumentTextPrimary = instrumentTextPrimary
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SweepWorkspaceActionButton("A → Resonance", true, onMoveAToResonance, widgetSurface, instrumentTextPrimary, instrumentTextSecondary)
                SweepWorkspaceActionButton("B → Resonance", true, onMoveBToResonance, widgetSurface, instrumentTextPrimary, instrumentTextSecondary)
                SweepWorkspaceActionButton("A → Center", true, onMoveAToCenter, widgetSurface, instrumentTextPrimary, instrumentTextSecondary)
                SweepWorkspaceActionButton("B → Center", true, onMoveBToCenter, widgetSurface, instrumentTextPrimary, instrumentTextSecondary)
                SweepWorkspaceActionButton("Active → Target", true, onMoveActiveToTarget, widgetSurface, instrumentTextPrimary, instrumentTextSecondary)
            }

            SharedInstrumentSubHeader(
                text = "Manual Frequency Entry",
                instrumentTextPrimary = instrumentTextPrimary
            )
            OutlinedTextField(
                value = userFrequencyText,
                onValueChange = { userFrequencyText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("User Frequency MHz", color = instrumentTextSecondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = widgetAccent,
                    unfocusedBorderColor = instrumentDivider,
                    focusedTextColor = instrumentTextPrimary,
                    unfocusedTextColor = instrumentTextPrimary,
                    focusedLabelColor = widgetAccent,
                    unfocusedLabelColor = instrumentTextSecondary,
                    cursorColor = widgetAccent,
                    focusedContainerColor = widgetSurface,
                    unfocusedContainerColor = widgetSurface
                )
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SweepWorkspaceActionButton("Active → User Freq", userFrequencyEntryValid, { onMoveActiveToUserFrequency(parsedUserFrequencyMHz) }, widgetSurface, instrumentTextPrimary, instrumentTextSecondary)
                SweepWorkspaceActionButton("A → Target", true, onMoveAToTarget, widgetSurface, instrumentTextPrimary, instrumentTextSecondary)
                SweepWorkspaceActionButton("B → Target", true, onMoveBToTarget, widgetSurface, instrumentTextPrimary, instrumentTextSecondary)
            }

            SharedInstrumentSubHeader(
                text = "Bandwidth / Span Tools",
                instrumentTextPrimary = instrumentTextPrimary
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SweepWorkspaceActionButton("Mark SWR≤2 BW", bandwidthMarkerPairAvailable, onPlaceBandwidthMarkers, widgetSurface, instrumentTextPrimary, instrumentTextSecondary)
                SweepWorkspaceActionButton("Full Span", true, onPlaceFullSpanMarkers, widgetSurface, instrumentTextPrimary, instrumentTextSecondary)
            }

            SharedInstrumentDividerLine(
                instrumentDivider = instrumentDivider
            )
            SharedTwoValueRow("Search Source", searchSourceLabel, instrumentTextPrimary, instrumentTextSecondary)
            SharedTwoValueRow("Target Frequency", String.format("%.3f MHz", targetFrequencyMHz), instrumentTextPrimary, instrumentTextSecondary)
            SharedTwoValueRow("Marker A Index", markerAIndex.toString(), instrumentTextPrimary, instrumentTextSecondary)
            SharedTwoValueRow("Marker B Index", markerBIndex.toString(), instrumentTextPrimary, instrumentTextSecondary)
            SharedTwoValueRow("Resonance Index", resonanceIndex.toString(), instrumentTextPrimary, instrumentTextSecondary)
            SharedTwoValueRow("Peak Count", peakCount.toString(), instrumentTextPrimary, instrumentTextSecondary)
            SharedTwoValueRow(
                "User Frequency Entry",
                parsedUserFrequencyMHz?.let { String.format("%.3f MHz", it) } ?: "Invalid entry",
                instrumentTextPrimary,
                instrumentTextSecondary
            )
        }
    }
}

@Composable
fun SweepMarkerDataPanel(
    result: SweepResult,
    markerAPoint: SweepPoint?,
    markerBPoint: SweepPoint?,
    mode: SweepDisplayMode,
    showDelta: Boolean,
    showS21Estimate: Boolean,
    instrumentSurface: Color,
    instrumentDivider: Color,
    instrumentAccent: Color,
    instrumentTextPrimary: Color,
    instrumentTextSecondary: Color
) {
    val resonancePoint = result.points.minByOrNull { it.swr }
    val bandwidthAt2 = estimateBandwidthAtOrBelowSwrLocal(result, 2.0)
    val widgetAccent = MaterialTheme.colorScheme.primary
    val activeTraceName =
        when (mode) {
            SweepDisplayMode.SWR,
            SweepDisplayMode.ANALOG_SWR -> "SWR"
            SweepDisplayMode.RETURN_LOSS,
            SweepDisplayMode.ANALOG_RETURN_LOSS -> "Return Loss"
            SweepDisplayMode.RESISTANCE,
            SweepDisplayMode.ANALOG_RESISTANCE -> "Resistance"
            SweepDisplayMode.REACTANCE,
            SweepDisplayMode.ANALOG_REACTANCE -> "Reactance"
            SweepDisplayMode.WATERFALL -> "Waterfall / SWR History"
            SweepDisplayMode.SMITH -> "Smith Preview"
            SweepDisplayMode.IMPEDANCE_LOCUS -> "Impedance Locus"
            SweepDisplayMode.S21_ESTIMATE -> "S21 Estimate"
        }

    SharedInstrumentCard(
        instrumentSurface = instrumentSurface,
        instrumentDivider = instrumentDivider
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SharedInstrumentSectionHeader(
                text = "Live Measurement Dashboard",
                instrumentAccent = widgetAccent
            )
            SharedInstrumentDividerLine(
                instrumentDivider = instrumentDivider
            )

            SharedTwoValueRow("Active Trace", activeTraceName, instrumentTextPrimary, instrumentTextSecondary)

            resonancePoint?.let { point ->
                SharedInstrumentSubHeader("Resonance", instrumentTextPrimary)
                SharedInstrumentValueText(
                    String.format(
                        "%.4f MHz  |  SWR %.4f  |  R %.2f Ω  |  X %.2f Ω",
                        point.frequencyMHz,
                        point.swr,
                        point.resistance,
                        point.reactance
                    ),
                    instrumentTextPrimary
                )
            }

            bandwidthAt2?.let {
                SharedTwoValueRow(
                    "Estimated SWR ≤ 2 Bandwidth",
                    String.format("%.4f MHz", it),
                    instrumentTextPrimary,
                    instrumentTextSecondary
                )
            }

            SharedInstrumentDividerLine(instrumentDivider)
            SharedInstrumentSubHeader("Professional Marker Table", instrumentTextPrimary)
            SharedMarkerTableHeaderRow(widgetAccent, instrumentTextPrimary, instrumentDivider)
            SharedMarkerTableDataRow("A", markerAPoint, widgetAccent, instrumentTextPrimary, instrumentDivider)
            SharedMarkerTableDataRow("B", markerBPoint, widgetAccent, instrumentTextPrimary, instrumentDivider)
            SharedMarkerTableDeltaRow(markerAPoint, markerBPoint, widgetAccent, instrumentTextPrimary, instrumentDivider)

            SharedInstrumentDividerLine(instrumentDivider)

            markerAPoint?.let { point ->
                SharedInstrumentSubHeader("Marker A", instrumentTextPrimary)
                SharedInstrumentValueText(buildPointSummaryLocal(point, mode, showS21Estimate), instrumentTextPrimary)
            }

            markerBPoint?.let { point ->
                SharedInstrumentSubHeader("Marker B", instrumentTextPrimary)
                SharedInstrumentValueText(buildPointSummaryLocal(point, mode, showS21Estimate), instrumentTextPrimary)
            }

            if (showDelta && markerAPoint != null && markerBPoint != null) {
                val deltaFreq = markerBPoint.frequencyMHz - markerAPoint.frequencyMHz
                val deltaValue = getDisplayValue(markerBPoint, mode) - getDisplayValue(markerAPoint, mode)
                val deltaResistance = markerBPoint.resistance - markerAPoint.resistance
                val deltaReactance = markerBPoint.reactance - markerAPoint.reactance
                val deltaSwr = markerBPoint.swr - markerAPoint.swr

                SharedInstrumentDividerLine(instrumentDivider)
                SharedInstrumentSubHeader("Delta Measurement", instrumentTextPrimary)
                SharedTwoValueRow("Δ Frequency", String.format("%.4f MHz", deltaFreq), instrumentTextPrimary, instrumentTextSecondary)
                SharedTwoValueRow("Δ SWR", String.format("%.4f", deltaSwr), instrumentTextPrimary, instrumentTextSecondary)
                SharedTwoValueRow("Δ Display Value", String.format("%.4f", deltaValue), instrumentTextPrimary, instrumentTextSecondary)
                SharedTwoValueRow("Δ R", String.format("%.4f Ω", deltaResistance), instrumentTextPrimary, instrumentTextSecondary)
                SharedTwoValueRow("Δ X", String.format("%.4f Ω", deltaReactance), instrumentTextPrimary, instrumentTextSecondary)

                if (mode == SweepDisplayMode.SWR || mode == SweepDisplayMode.ANALOG_SWR) {
                    SharedTwoValueRow(
                        "Marker Span / BW View",
                        String.format("%.4f MHz", abs(deltaFreq)),
                        instrumentTextPrimary,
                        instrumentTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun SweepCsvPreviewPanel(
    result: SweepResult,
    showS21Estimate: Boolean,
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
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SharedInstrumentSectionHeader(
                text = "CSV Preview",
                instrumentAccent = widgetAccent
            )

            SharedInstrumentDividerLine(
                instrumentDivider = instrumentDivider
            )

            SharedInstrumentMutedText(
                text = String.format(
                    "# range=%.3f-%.3f MHz  step=%.3f  points=%d/%d  complete=%b",
                    result.startFrequencyMHz,
                    result.endFrequencyMHz,
                    result.stepMHz,
                    result.actualPointCount,
                    result.requestedPointCount,
                    result.isComplete
                ),
                instrumentTextSecondary = instrumentTextSecondary
            )

            if (result.points.size > 40) {
                SharedInstrumentMutedText(
                    text = "# preview truncated to first 40 of ${result.points.size} rows",
                    instrumentTextSecondary = instrumentTextSecondary
                )
            }

            SharedInstrumentValueText(
                text = if (showS21Estimate) {
                    "frequencyMHz,swr,returnLossDb,resistance,reactance,estimatedS21Db"
                } else {
                    "frequencyMHz,swr,returnLossDb,resistance,reactance"
                },
                instrumentTextPrimary = instrumentTextPrimary
            )

            result.points.take(40).forEach { point ->
                if (showS21Estimate) {
                    SharedInstrumentMutedText(
                        text = String.format(
                            "%.4f,%.4f,%.4f,%.4f,%.4f,%.4f",
                            point.frequencyMHz,
                            point.swr,
                            point.returnLossDb,
                            point.resistance,
                            point.reactance,
                            estimateS21Db(point)
                        ),
                        instrumentTextSecondary = instrumentTextSecondary
                    )
                } else {
                    SharedInstrumentMutedText(
                        text = String.format(
                            "%.4f,%.4f,%.4f,%.4f,%.4f",
                            point.frequencyMHz,
                            point.swr,
                            point.returnLossDb,
                            point.resistance,
                            point.reactance
                        ),
                        instrumentTextSecondary = instrumentTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SweepWorkspaceActionButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    instrumentSurfaceVariant: Color,
    instrumentTextPrimary: Color,
    instrumentTextSecondary: Color
) {
    Button(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = instrumentSurfaceVariant,
            contentColor = instrumentTextPrimary,
            disabledContainerColor = instrumentSurfaceVariant.copy(alpha = 0.48f),
            disabledContentColor = instrumentTextSecondary
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.95f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 1.dp,
            disabledElevation = 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SweepWorkspaceDisplayButton(
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    instrumentAccent: Color,
    instrumentBackground: Color,
    instrumentSurfaceVariant: Color,
    instrumentTextPrimary: Color,
    instrumentTextSecondary: Color
) {
    Button(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) instrumentAccent else instrumentSurfaceVariant,
            contentColor = if (active) instrumentBackground else instrumentTextPrimary,
            disabledContainerColor = instrumentSurfaceVariant.copy(alpha = 0.48f),
            disabledContentColor = instrumentTextSecondary
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (active) {
                instrumentAccent.copy(alpha = 0.98f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.95f)
            }
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (active) 6.dp else 3.dp,
            pressedElevation = 1.dp,
            disabledElevation = 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun SharedMarkerTableHeaderRow(
    instrumentAccent: Color,
    instrumentTextPrimary: Color,
    instrumentDivider: Color
) {
    SharedMarkerTableRow(
        marker = "Marker",
        frequency = "Frequency",
        swr = "SWR",
        resistance = "R",
        reactance = "X",
        header = true,
        instrumentAccent = instrumentAccent,
        instrumentTextPrimary = instrumentTextPrimary,
        instrumentDivider = instrumentDivider
    )
}

@Composable
private fun SharedMarkerTableDataRow(
    label: String,
    point: SweepPoint?,
    instrumentAccent: Color,
    instrumentTextPrimary: Color,
    instrumentDivider: Color
) {
    SharedMarkerTableRow(
        marker = label,
        frequency = point?.let { String.format("%.4f", it.frequencyMHz) } ?: "--",
        swr = point?.let { String.format("%.4f", it.swr) } ?: "--",
        resistance = point?.let { String.format("%.2f", it.resistance) } ?: "--",
        reactance = point?.let { String.format("%.2f", it.reactance) } ?: "--",
        header = false,
        instrumentAccent = instrumentAccent,
        instrumentTextPrimary = instrumentTextPrimary,
        instrumentDivider = instrumentDivider
    )
}

@Composable
private fun SharedMarkerTableDeltaRow(
    markerAPoint: SweepPoint?,
    markerBPoint: SweepPoint?,
    instrumentAccent: Color,
    instrumentTextPrimary: Color,
    instrumentDivider: Color
) {
    SharedMarkerTableRow(
        marker = "Δ",
        frequency = if (markerAPoint != null && markerBPoint != null) {
            String.format("%.4f", markerBPoint.frequencyMHz - markerAPoint.frequencyMHz)
        } else {
            "--"
        },
        swr = if (markerAPoint != null && markerBPoint != null) {
            String.format("%.4f", markerBPoint.swr - markerAPoint.swr)
        } else {
            "--"
        },
        resistance = if (markerAPoint != null && markerBPoint != null) {
            String.format("%.2f", markerBPoint.resistance - markerAPoint.resistance)
        } else {
            "--"
        },
        reactance = if (markerAPoint != null && markerBPoint != null) {
            String.format("%.2f", markerBPoint.reactance - markerAPoint.reactance)
        } else {
            "--"
        },
        header = false,
        instrumentAccent = instrumentAccent,
        instrumentTextPrimary = instrumentTextPrimary,
        instrumentDivider = instrumentDivider
    )
}

@Composable
private fun SharedMarkerTableRow(
    marker: String,
    frequency: String,
    swr: String,
    resistance: String,
    reactance: String,
    header: Boolean,
    instrumentAccent: Color,
    instrumentTextPrimary: Color,
    instrumentDivider: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, instrumentDivider, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        SharedMarkerTableCell(marker, 64.dp, header, instrumentAccent, instrumentTextPrimary)
        SharedMarkerTableCell(frequency, 112.dp, header, instrumentAccent, instrumentTextPrimary)
        SharedMarkerTableCell(swr, 80.dp, header, instrumentAccent, instrumentTextPrimary)
        SharedMarkerTableCell(resistance, 80.dp, header, instrumentAccent, instrumentTextPrimary)
        SharedMarkerTableCell(reactance, 80.dp, header, instrumentAccent, instrumentTextPrimary)
    }
}

@Composable
private fun SharedMarkerTableCell(
    text: String,
    width: Dp,
    header: Boolean,
    instrumentAccent: Color,
    instrumentTextPrimary: Color
) {
    Text(
        text = text,
        color = if (header) instrumentAccent else instrumentTextPrimary,
        fontWeight = if (header) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier.width(width)
    )
}

private fun buildPointSummaryLocal(
    point: SweepPoint,
    mode: SweepDisplayMode,
    showS21Estimate: Boolean
): String {
    return buildString {
        append(String.format("Freq: %.4f MHz\n", point.frequencyMHz))
        append(String.format("SWR: %.4f\n", point.swr))
        append(String.format("RL: %.4f dB\n", point.returnLossDb))
        append(String.format("R: %.4f Ω\n", point.resistance))
        append(String.format("X: %.4f Ω\n", point.reactance))

        if (showS21Estimate) {
            append(String.format("S21 Est: %.4f dB\n", estimateS21Db(point)))
        }

        append(String.format("Display: %.4f", getDisplayValue(point, mode)))
    }
}

private fun estimateBandwidthAtOrBelowSwrLocal(
    result: SweepResult,
    threshold: Double
): Double? {
    val pointsInBand = result.points.filter { it.swr <= threshold }
    if (pointsInBand.isEmpty()) {
        return null
    }

    val start = pointsInBand.minOf { it.frequencyMHz }
    val end = pointsInBand.maxOf { it.frequencyMHz }
    return end - start
}