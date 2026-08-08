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
import com.example.antennalab_v1.domain.testing.SweepCsvExport
import com.example.antennalab_v1.ui.components.SelectionButtonStyle
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
            SharedInstrumentSectionHeader(text = "Trace Memory / Compare")

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
                    onClick = onUseCurrentAsReference
                )

                SweepWorkspaceActionButton(
                    label = "Recall Previous",
                    enabled = sweepHistoryCount >= 2,
                    onClick = onUsePreviousHistoryAsReference
                )

                SweepWorkspaceActionButton(
                    label = "Clear Ref",
                    enabled = referenceSweep != null,
                    onClick = onClearReference
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
                    onClick = { onTraceModeChange(TraceCompareMode.CURRENT_ONLY) }
                )

                SweepWorkspaceDisplayButton(
                    label = "Current + Ref",
                    active = traceCompareMode == TraceCompareMode.CURRENT_PLUS_REFERENCE,
                    enabled = referenceSweep != null,
                    onClick = { onTraceModeChange(TraceCompareMode.CURRENT_PLUS_REFERENCE) }
                )

                SweepWorkspaceDisplayButton(
                    label = "Difference",
                    active = traceCompareMode == TraceCompareMode.DIFFERENCE,
                    enabled = referenceSweep != null,
                    onClick = { onTraceModeChange(TraceCompareMode.DIFFERENCE) }
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
            SharedInstrumentSectionHeader(text = "Professional Marker System")

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
                    onClick = onSelectMarkerA
                )
                SweepWorkspaceDisplayButton(
                    label = "Marker B",
                    active = !activeMarkerIsA,
                    enabled = true,
                    onClick = onSelectMarkerB
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
                SweepWorkspaceActionButton("Active -", true, { onActiveMarkerNudge(-1) })
                SweepWorkspaceActionButton("Active +", true, { onActiveMarkerNudge(1) })
                SweepWorkspaceActionButton("A-", true, { onMarkerANudge(-1) })
                SweepWorkspaceActionButton("A+", true, { onMarkerANudge(1) })
                SweepWorkspaceActionButton("B-", true, { onMarkerBNudge(-1) })
                SweepWorkspaceActionButton("B+", true, { onMarkerBNudge(1) })
            }

            SharedInstrumentSubHeader(
                text = "Search Tools",
                instrumentTextPrimary = instrumentTextPrimary
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SweepWorkspaceActionButton("Peak Search", highestPeakAvailable, onPeakSearch)
                SweepWorkspaceActionButton("Min SWR", true, onMoveActiveToResonance)
                SweepWorkspaceActionButton("Next Peak", peakCount > 0, onNextPeak)
                SweepWorkspaceActionButton("Previous Peak", peakCount > 0, onPreviousPeak)
            }

            SharedInstrumentSubHeader(
                text = "Jump Tools",
                instrumentTextPrimary = instrumentTextPrimary
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SweepWorkspaceActionButton("A → Resonance", true, onMoveAToResonance)
                SweepWorkspaceActionButton("B → Resonance", true, onMoveBToResonance)
                SweepWorkspaceActionButton("A → Center", true, onMoveAToCenter)
                SweepWorkspaceActionButton("B → Center", true, onMoveBToCenter)
                SweepWorkspaceActionButton("Active → Target", true, onMoveActiveToTarget)
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
                SweepWorkspaceActionButton("Active → User Freq", userFrequencyEntryValid, { onMoveActiveToUserFrequency(parsedUserFrequencyMHz) })
                SweepWorkspaceActionButton("A → Target", true, onMoveAToTarget)
                SweepWorkspaceActionButton("B → Target", true, onMoveBToTarget)
            }

            SharedInstrumentSubHeader(
                text = "Bandwidth / Span Tools",
                instrumentTextPrimary = instrumentTextPrimary
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SweepWorkspaceActionButton("Mark SWR≤2 BW", bandwidthMarkerPairAvailable, onPlaceBandwidthMarkers)
                SweepWorkspaceActionButton("Full Span", true, onPlaceFullSpanMarkers)
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
    val bandwidthAt2 = estimateBandwidthAtOrBelowSwr(result, 2.0)
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
            SharedInstrumentSectionHeader(text = "Live Measurement Dashboard")
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
        /*
        Do NOT add Modifier.verticalScroll here. This panel renders inside
        SweepGraphScreen's outer Column(Modifier.verticalScroll), which measures
        its children with maxHeight = Infinity; a nested vertical scroller then
        fails checkScrollableContainerConstraints and throws
        "Vertically scrollable component was measured with an infinity maximum
        height constraints". It is not needed either — this is a PREVIEW, capped
        at 40 rows below, and the outer scroll already reaches all of it.
        */
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SharedInstrumentSectionHeader(text = "CSV Preview")

            SharedInstrumentDividerLine(
                instrumentDivider = instrumentDivider
            )

            // Provenance first: without it a simulated sweep's CSV is
            // indistinguishable from a real measurement. Built purely in
            // domain/testing/SweepCsvExport so it stays testable and agrees
            // with the .s1p instrument line.
            SharedInstrumentMutedText(
                text = SweepCsvExport.buildProvenanceHeader(result),
                instrumentTextSecondary = instrumentTextSecondary
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
    onClick: () -> Unit
) {
    // An action has no selected state, so it takes the unselected treatment:
    // orange outline, transparent fill. Solid means "selected" and nothing else.
    Button(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled,
        colors = SelectionButtonStyle.colors(selected = false),
        border = SelectionButtonStyle.border(selected = false, enabled = enabled),
        elevation = SelectionButtonStyle.elevation(selected = false),
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
// Colour comes from SelectionButtonStyle, so the instrument-palette params
// these two used to take are gone rather than left dangling and unread.
private fun SweepWorkspaceDisplayButton(
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled,
        colors = SelectionButtonStyle.colors(selected = active),
        border = SelectionButtonStyle.border(selected = active, enabled = enabled),
        elevation = SelectionButtonStyle.elevation(selected = active),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
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
