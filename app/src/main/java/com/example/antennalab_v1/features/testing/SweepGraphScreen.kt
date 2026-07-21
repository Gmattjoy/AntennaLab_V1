package com.example.antennalab_v1.features.testing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.antennalab_v1.domain.analysis.AdjustmentEstimate
import com.example.antennalab_v1.domain.analysis.AntennaBehaviorClassification
import com.example.antennalab_v1.domain.analysis.TuningSuggestionReport
import com.example.antennalab_v1.domain.analysis.TuningWorkflowReport
import com.example.antennalab_v1.domain.testing.UsbSessionManager
import com.example.antennalab_v1.features.app.AppTopRightMenu
import com.example.antennalab_v1.features.app.InstrumentStatusCard
import com.example.antennalab_v1.model.AntennaClassification
import com.example.antennalab_v1.model.DriverProfile
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.TestHardwareProfile
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult

enum class SweepDisplayMode {
    SWR,
    RETURN_LOSS,
    RESISTANCE,
    REACTANCE,
    ANALOG_SWR,
    ANALOG_RETURN_LOSS,
    ANALOG_RESISTANCE,
    ANALOG_REACTANCE,
    WATERFALL,
    SMITH,
    IMPEDANCE_LOCUS,
    S21_ESTIMATE
}

private enum class ActiveMarkerTarget { A, B }

enum class TraceCompareMode {
    CURRENT_ONLY,
    CURRENT_PLUS_REFERENCE,
    DIFFERENCE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SweepGraphScreen(
    project: ProjectData,
    onBack: () -> Unit,
    onOpenSystemDevices: () -> Unit = {},
    onOpenInstrumentDetails: () -> Unit = onOpenSystemDevices
) {
    val context = LocalContext.current
    val hardware = project.testHardwareProfile
    val capabilityProfile = project.hardwareCapabilityProfile
    val measurementCapabilities = project.hardwareMeasurementCapabilities
    val targetFreq = project.designInput.targetFrequencyMHz

    val selectedHardwareName = buildSweepScreenHardwareDisplayName(
        capabilityProfileDisplayName = capabilityProfile.displayName,
        selectedDriverProfile = UsbSessionManager.getSelectedDriverProfile()
    )

    val sweepWidth =
        if (hardware == TestHardwareProfile.LITEVNA64_V0_3_3) 0.50 else 0.25

    val sweepStep =
        if (hardware == TestHardwareProfile.LITEVNA64_V0_3_3) 0.01 else 0.02

    val unclampedSweepStart = targetFreq - sweepWidth
    val unclampedSweepEnd = targetFreq + sweepWidth
    val hardwareMinMHz = capabilityProfile.minFrequencyHz / 1_000_000.0
    val hardwareMaxMHz = capabilityProfile.maxFrequencyHz / 1_000_000.0

    val sweepStart = unclampedSweepStart.coerceIn(hardwareMinMHz, hardwareMaxMHz)
    val sweepEnd = unclampedSweepEnd.coerceIn(hardwareMinMHz, hardwareMaxMHz)

    val availableDisplayModes = buildList {
        if (measurementCapabilities.supportsSWR) {
            add(SweepDisplayMode.SWR)
            add(SweepDisplayMode.ANALOG_SWR)
            add(SweepDisplayMode.WATERFALL)
        }
        if (measurementCapabilities.supportsReturnLoss) {
            add(SweepDisplayMode.RETURN_LOSS)
            add(SweepDisplayMode.ANALOG_RETURN_LOSS)
        }
        if (measurementCapabilities.supportsResistance) {
            add(SweepDisplayMode.RESISTANCE)
            add(SweepDisplayMode.ANALOG_RESISTANCE)
        }
        if (measurementCapabilities.supportsReactance) {
            add(SweepDisplayMode.REACTANCE)
            add(SweepDisplayMode.ANALOG_REACTANCE)
        }
        if (measurementCapabilities.supportsSmithChart) add(SweepDisplayMode.SMITH)
        if (measurementCapabilities.supportsImpedanceLocus) add(SweepDisplayMode.IMPEDANCE_LOCUS)
        if (measurementCapabilities.supportsS21) add(SweepDisplayMode.S21_ESTIMATE)
    }

    val initialWorkspaceState = remember(
        project.meta.projectId,
        project.isUnknownDiscoverySession,
        project.antennaClassification,
        availableDisplayModes
    ) {
        SweepWorkspaceState(
            isDiscoveryMode = project.isUnknownDiscoverySession,
            discoveryAntennaClassification = project.antennaClassification,
            displayMode = availableDisplayModes.firstOrNull() ?: SweepDisplayMode.SWR
        )
    }

    val viewModelStoreOwner = context as ViewModelStoreOwner

    val viewModel = remember(project.meta.projectId) {
        ViewModelProvider(
            viewModelStoreOwner,
            SweepWorkspaceViewModel.provideFactory(initialWorkspaceState)
        )[SweepWorkspaceViewModel::class.java]
    }

    val workspaceState = viewModel.workspaceState
    val uiModel = viewModel.buildUiModel(
        context = context,
        project = project,
        selectedHardwareName = selectedHardwareName,
        targetFrequencyMHz = targetFreq
    )

    LaunchedEffect(availableDisplayModes) {
        viewModel.ensureCompatibleState(availableDisplayModes)
    }

    val sweepResult = workspaceState.currentSweep
    val referenceSweep = workspaceState.referenceSweep
    val traceCompareMode = workspaceState.traceCompareMode
    val displayMode = workspaceState.displayMode
    val markerAIndex = workspaceState.markerAIndex
    val markerBIndex = workspaceState.markerBIndex
    val showCsvPreview = workspaceState.showCsvPreview
    val activeMarkerTarget = when (workspaceState.activeMarkerTarget) {
        WorkspaceMarkerTarget.A -> ActiveMarkerTarget.A
        WorkspaceMarkerTarget.B -> ActiveMarkerTarget.B
    }

    val resonanceMHz = uiModel.resonanceMHz
    val diagnosticsUiModel = uiModel.diagnostics
    val behaviorClassification = uiModel.behaviorClassification
    val tuningSuggestionReport = uiModel.tuningSuggestionReport
    val adjustmentEstimate = uiModel.adjustmentEstimate
    val tuningWorkflowReport = uiModel.tuningWorkflowReport
    val markerAPoint = uiModel.markerAPoint
    val markerBPoint = uiModel.markerBPoint
    val sweepRunContract = uiModel.sweepRunContract
    val discoveryUi = uiModel.discoveryUi

    val activeSweepFailureText =
        if (uiModel.shouldSuppressStaleOperatorWarning) {
            null
        } else {
            uiModel.fallbackReasonText?.takeIf {
                it.startsWith("Latest sweep failure:", ignoreCase = true)
            }
        }

    Scaffold(
        containerColor = InstrumentBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = InstrumentBackground,
                    titleContentColor = InstrumentTextPrimary
                ),
                title = {
                    Column {
                        InstrumentTitle(
                            if (project.isUnknownDiscoverySession) {
                                "Unknown Antenna Discovery"
                            } else {
                                "Sweep Viewer"
                            }
                        )
                        InstrumentMutedText(
                            if (project.isUnknownDiscoverySession) {
                                "Detached discovery sweep mode"
                            } else {
                                "RF instrument mode"
                            }
                        )
                    }
                },
                actions = { AppTopRightMenu() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(InstrumentBackground)
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InstrumentStatusCard(
                model = uiModel.instrumentStatusCard,
                onOpenDetails = onOpenInstrumentDetails
            )

            SweepRunReadinessCard(
                sweepRunContract = sweepRunContract,
                currentSweepSourceLabel = uiModel.currentSweepSourceLabel,
                selectedSweepPathLabel = uiModel.selectedSweepPathLabel,
                onOpenInstrumentDetails = onOpenInstrumentDetails
            )

            activeSweepFailureText?.let { failureText ->
                SweepOperatorWarningCard(
                    failureText = failureText,
                    onOpenInstrumentDetails = onOpenInstrumentDetails
                )
            }

            if (discoveryUi.isDiscoveryMode) {
                DiscoveryClassificationCard(
                    discoveryUi = discoveryUi,
                    onClassificationSelected = { selectedClassification ->
                        viewModel.setDiscoveryAntennaClassification(selectedClassification)
                    }
                )
            }

            SweepDisplayModesCard(
                measurementCapabilities = measurementCapabilities,
                displayMode = displayMode,
                onDisplayModeSelected = { selectedMode -> viewModel.setDisplayMode(selectedMode) },
                instrumentSurface = InstrumentSurface,
                instrumentDivider = InstrumentDivider,
                instrumentAccent = InstrumentAccent,
                instrumentTextPrimary = InstrumentTextPrimary
            )

            SweepControlsCard(
                sweepResult = sweepResult,
                referenceSweep = referenceSweep,
                showCsvPreview = showCsvPreview,
                supportsCsvPreview = capabilityProfile.supportsCsvPreview,
                runSweepButtonText = sweepRunContract.runButtonText,
                runSweepEnabled = sweepRunContract.runEnabled,
                runSweepStatusText = sweepRunContract.statusText,
                onRunSweep = {
                    viewModel.runSweep(
                        context = context,
                        project = project,
                        startMHz = sweepStart,
                        endMHz = sweepEnd,
                        stepMHz = sweepStep
                    )
                },
                onSetReference = { viewModel.setCurrentAsReference() },
                onClearReference = { viewModel.clearReference() },
                onToggleCsvPreview = { viewModel.toggleCsvPreview() },
                onBack = onBack,
                instrumentSurface = InstrumentSurface,
                instrumentDivider = InstrumentDivider,
                instrumentAccent = InstrumentAccent,
                instrumentTextPrimary = InstrumentTextPrimary,
                instrumentTextSecondary = InstrumentTextSecondary
            )

            TraceMemoryPanel(
                currentSweep = sweepResult,
                referenceSweep = referenceSweep,
                traceCompareMode = traceCompareMode,
                sweepHistoryCount = workspaceState.sweepHistory.size,
                onUseCurrentAsReference = { viewModel.setCurrentAsReference() },
                onUsePreviousHistoryAsReference = { viewModel.setHistorySweepAsReference(1) },
                onClearReference = { viewModel.clearReference() },
                onTraceModeChange = { newMode -> viewModel.setTraceCompareMode(newMode) }
            )

            sweepResult?.let { result ->
                InstrumentCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InstrumentSectionHeader("Active Display")

                        when (displayMode) {
                            SweepDisplayMode.SMITH -> {
                                SweepSmithChartView(
                                    result = result,
                                    markerAIndex = markerAIndex,
                                    markerBIndex = markerBIndex,
                                    instrumentSurfaceVariant = InstrumentSurfaceVariant,
                                    instrumentDivider = InstrumentDivider,
                                    instrumentAccent = InstrumentAccent,
                                    instrumentTextSecondary = InstrumentTextSecondary,
                                    markerAColor = InstrumentBlue,
                                    markerBColor = InstrumentMagenta
                                )
                            }

                            SweepDisplayMode.IMPEDANCE_LOCUS -> {
                                SweepImpedanceLocusView(
                                    result = result,
                                    markerAIndex = markerAIndex,
                                    markerBIndex = markerBIndex,
                                    instrumentSurfaceVariant = InstrumentSurfaceVariant,
                                    instrumentDivider = InstrumentDivider,
                                    instrumentAccent = InstrumentAccent,
                                    markerAColor = InstrumentBlue,
                                    markerBColor = InstrumentMagenta
                                )
                            }

                            SweepDisplayMode.ANALOG_SWR,
                            SweepDisplayMode.ANALOG_RETURN_LOSS,
                            SweepDisplayMode.ANALOG_RESISTANCE,
                            SweepDisplayMode.ANALOG_REACTANCE -> {
                                SweepAnalogGauge(
                                    result = result,
                                    mode = displayMode,
                                    markerAIndex = markerAIndex,
                                    instrumentSurface = InstrumentSurfaceVariant,
                                    instrumentDivider = InstrumentDivider,
                                    instrumentAccent = InstrumentAccent,
                                    instrumentTextPrimary = InstrumentTextPrimary,
                                    instrumentTextSecondary = InstrumentTextSecondary
                                )
                            }

                            SweepDisplayMode.WATERFALL -> {
                                SweepWaterfallSweepView(
                                    sweepHistory = workspaceState.sweepHistory,
                                    markerAIndex = markerAIndex,
                                    instrumentSurfaceVariant = InstrumentSurfaceVariant,
                                    instrumentDivider = InstrumentDivider,
                                    instrumentTextPrimary = InstrumentTextPrimary
                                )
                            }

                            else -> {
                                SweepScalarTraceView(
                                    result = result,
                                    referenceResult = referenceSweep,
                                    traceCompareMode = traceCompareMode,
                                    mode = displayMode,
                                    markerAIndex = markerAIndex,
                                    markerBIndex = markerBIndex,
                                    instrumentSurfaceVariant = InstrumentSurfaceVariant,
                                    instrumentDivider = InstrumentDivider,
                                    instrumentAccent = InstrumentAccent,
                                    instrumentTextPrimary = InstrumentTextPrimary,
                                    instrumentTextSecondary = InstrumentTextSecondary,
                                    instrumentBlue = InstrumentBlue,
                                    instrumentMagenta = InstrumentMagenta,
                                    instrumentGreen = InstrumentGreen
                                )
                            }
                        }
                    }
                }

                if (capabilityProfile.supportsMarkerSystem) {
                    MarkerControlPanel(
                        mode = displayMode,
                        targetFrequencyMHz = targetFreq,
                        activeMarkerTarget = activeMarkerTarget,
                        markerAIndex = markerAIndex,
                        markerBIndex = markerBIndex,
                        resonanceIndex = viewModel.getResonanceIndex(),
                        peakCount = viewModel.getPeakIndices(displayMode).size,
                        highestPeakAvailable = viewModel.getHighestPeakIndex(displayMode) != null,
                        bandwidthMarkerPairAvailable = viewModel.hasBandwidthMarkerPair(2.0),
                        onActiveMarkerTargetChange = {
                            viewModel.setActiveMarkerTarget(
                                when (it) {
                                    ActiveMarkerTarget.A -> WorkspaceMarkerTarget.A
                                    ActiveMarkerTarget.B -> WorkspaceMarkerTarget.B
                                }
                            )
                        },
                        onActiveMarkerNudge = { delta -> viewModel.nudgeActiveMarker(delta) },
                        onMarkerANudge = { delta -> viewModel.nudgeMarkerA(delta) },
                        onMarkerBNudge = { delta -> viewModel.nudgeMarkerB(delta) },
                        onPeakSearch = { viewModel.moveActiveMarkerToHighestPeak(displayMode) },
                        onMoveActiveToResonance = { viewModel.moveActiveMarkerToResonance() },
                        onNextPeak = { viewModel.moveActiveMarkerToNextPeak(displayMode) },
                        onPreviousPeak = { viewModel.moveActiveMarkerToPreviousPeak(displayMode) },
                        onMoveAToResonance = { viewModel.moveMarkerAToResonance() },
                        onMoveBToResonance = { viewModel.moveMarkerBToResonance() },
                        onMoveAToCenter = { viewModel.moveMarkerAToCenter() },
                        onMoveBToCenter = { viewModel.moveMarkerBToCenter() },
                        onMoveActiveToTarget = { viewModel.moveActiveMarkerToTargetFrequency(targetFreq) },
                        onMoveActiveToUserFrequency = { frequency -> viewModel.moveActiveMarkerToUserFrequency(frequency) },
                        onMoveAToTarget = { viewModel.moveMarkerAToTargetFrequency(targetFreq) },
                        onMoveBToTarget = { viewModel.moveMarkerBToTargetFrequency(targetFreq) },
                        onPlaceBandwidthMarkers = { viewModel.placeBandwidthMarkers(2.0) },
                        onPlaceFullSpanMarkers = { viewModel.placeFullSpanMarkers() }
                    )
                }

                if (capabilityProfile.supportsMarkerSystem) {
                    MarkerDataPanel(
                        result = result,
                        markerAPoint = markerAPoint,
                        markerBPoint = markerBPoint,
                        mode = displayMode,
                        showDelta = capabilityProfile.supportsDeltaMarkers,
                        showS21Estimate = measurementCapabilities.supportsS21
                    )
                }

                SweepSummaryCard(
                    result = result,
                    resonanceMHz = resonanceMHz,
                    measurementCapabilities = measurementCapabilities,
                    hardware = hardware,
                    instrumentSurface = InstrumentSurface,
                    instrumentDivider = InstrumentDivider,
                    instrumentAccent = InstrumentAccent,
                    instrumentTextPrimary = InstrumentTextPrimary,
                    instrumentTextSecondary = InstrumentTextSecondary
                )

                DiagnosticsSummaryCard(diagnostics = diagnosticsUiModel)
                TuningInterpretationCard(
                    classification = behaviorClassification,
                    suggestionReport = tuningSuggestionReport,
                    adjustmentEstimate = adjustmentEstimate,
                    workflowReport = tuningWorkflowReport
                )

                if (discoveryUi.isDiscoveryMode) {
                    DiscoveryHandoffCard(
                        discoveryUi = discoveryUi,
                        onApplyToCurrentProject = {
                            if (viewModel.applyDiscoveryToCurrentProject(context, project) != null) {
                                onBack()
                            }
                        },
                        onSaveAsNewProject = {
                            if (viewModel.saveDiscoveryAsNewProject(context, project) != null) {
                                onBack()
                            }
                        },
                        onReturnWithoutSaving = {
                            viewModel.returnWithoutSavingDiscovery()
                            onBack()
                        },
                        onDiscardSession = {
                            viewModel.discardDiscoverySession()
                            onBack()
                        }
                    )
                }

                if (showCsvPreview && capabilityProfile.supportsCsvPreview) {
                    CsvPreviewCard(
                        result = result,
                        showS21Estimate = measurementCapabilities.supportsS21
                    )
                }
            }

            SweepEngineeringDetailsCard(
                engineeringDetails = uiModel.engineeringDetails,
                onOpenInstrumentDetails = onOpenInstrumentDetails
            )
        }
    }
}

@Composable
private fun TraceMemoryPanel(
    currentSweep: SweepResult?,
    referenceSweep: SweepResult?,
    traceCompareMode: TraceCompareMode,
    sweepHistoryCount: Int,
    onUseCurrentAsReference: () -> Unit,
    onUsePreviousHistoryAsReference: () -> Unit,
    onClearReference: () -> Unit,
    onTraceModeChange: (TraceCompareMode) -> Unit
) {
    SweepTraceMemoryPanel(
        currentSweep = currentSweep,
        referenceSweep = referenceSweep,
        traceCompareMode = traceCompareMode,
        sweepHistoryCount = sweepHistoryCount,
        onUseCurrentAsReference = onUseCurrentAsReference,
        onUsePreviousHistoryAsReference = onUsePreviousHistoryAsReference,
        onClearReference = onClearReference,
        onTraceModeChange = onTraceModeChange,
        instrumentSurface = InstrumentSurface,
        instrumentDivider = InstrumentDivider,
        instrumentAccent = InstrumentAccent,
        instrumentTextPrimary = InstrumentTextPrimary,
        instrumentTextSecondary = InstrumentTextSecondary
    )
}

@Composable
private fun SweepRunReadinessCard(
    sweepRunContract: SweepRunContract,
    currentSweepSourceLabel: String,
    selectedSweepPathLabel: String,
    onOpenInstrumentDetails: () -> Unit
) {
    InstrumentCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InstrumentSectionHeader("Run Readiness")

            StatusTwoValueRow("Selected Path", selectedSweepPathLabel)
            TwoValueRow("Loaded Sweep", currentSweepSourceLabel)
            StatusTwoValueRow("Calibration", sweepRunContract.calibrationStateLabel)
            TwoValueRow("Trust Downgraded", if (sweepRunContract.trustDowngraded) "Yes" else "No")

            InstrumentMutedText(sweepRunContract.statusText)
            InstrumentMutedText(sweepRunContract.calibrationStatusText)
            sweepRunContract.calibrationWarningText?.let { InstrumentMutedText(it) }

            TextButton(onClick = onOpenInstrumentDetails) {
                InstrumentValueText("Open Instrument Details")
            }
        }
    }
}

@Composable
private fun SweepOperatorWarningCard(
    failureText: String,
    onOpenInstrumentDetails: () -> Unit
) {
    InstrumentCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InstrumentSectionHeader("Operator Warning")
            TwoValueRow("Sweep Status", "Attention Required")
            InstrumentMutedText(failureText)
            InstrumentMutedText(
                "Check session continuity and instrument details before trusting the next live sweep."
            )
            TextButton(onClick = onOpenInstrumentDetails) {
                InstrumentValueText("Open Instrument Details")
            }
        }
    }
}

@Composable
private fun SweepEngineeringDetailsCard(
    engineeringDetails: SweepEngineeringDetailsUiModel,
    onOpenInstrumentDetails: () -> Unit
) {
    InstrumentCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InstrumentSectionHeader("Engineering Details")
            StatusTwoValueRow("Command Status", engineeringDetails.commandStatusLabel)
            TwoValueRow("Last Read Size", "${engineeringDetails.lastReadSizeLabel} bytes")
            TwoValueRow("Last Error", engineeringDetails.lastErrorLabel)
            InstrumentMutedText(engineeringDetails.transportSummary)
            InstrumentDividerLine()
            InstrumentMutedText(
                "Advanced protocol and bring-up detail has been moved out of the main operator path."
            )
            TextButton(onClick = onOpenInstrumentDetails) {
                InstrumentValueText("Open Full Instrument Details")
            }
        }
    }
}

@Composable
private fun MarkerControlPanel(
    mode: SweepDisplayMode,
    targetFrequencyMHz: Double,
    activeMarkerTarget: ActiveMarkerTarget,
    markerAIndex: Int,
    markerBIndex: Int,
    resonanceIndex: Int,
    peakCount: Int,
    highestPeakAvailable: Boolean,
    bandwidthMarkerPairAvailable: Boolean,
    onActiveMarkerTargetChange: (ActiveMarkerTarget) -> Unit,
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
    onPlaceFullSpanMarkers: () -> Unit
) {
    SweepMarkerControlPanel(
        targetFrequencyMHz = targetFrequencyMHz,
        activeMarkerIsA = activeMarkerTarget == ActiveMarkerTarget.A,
        markerAIndex = markerAIndex,
        markerBIndex = markerBIndex,
        resonanceIndex = resonanceIndex,
        peakCount = peakCount,
        highestPeakAvailable = highestPeakAvailable,
        bandwidthMarkerPairAvailable = bandwidthMarkerPairAvailable,
        searchSourceLabel = getTraceAxisTitle(mode, TraceCompareMode.CURRENT_ONLY),
        onSelectMarkerA = { onActiveMarkerTargetChange(ActiveMarkerTarget.A) },
        onSelectMarkerB = { onActiveMarkerTargetChange(ActiveMarkerTarget.B) },
        onActiveMarkerNudge = onActiveMarkerNudge,
        onMarkerANudge = onMarkerANudge,
        onMarkerBNudge = onMarkerBNudge,
        onPeakSearch = onPeakSearch,
        onMoveActiveToResonance = onMoveActiveToResonance,
        onNextPeak = onNextPeak,
        onPreviousPeak = onPreviousPeak,
        onMoveAToResonance = onMoveAToResonance,
        onMoveBToResonance = onMoveBToResonance,
        onMoveAToCenter = onMoveAToCenter,
        onMoveBToCenter = onMoveBToCenter,
        onMoveActiveToTarget = onMoveActiveToTarget,
        onMoveActiveToUserFrequency = onMoveActiveToUserFrequency,
        onMoveAToTarget = onMoveAToTarget,
        onMoveBToTarget = onMoveBToTarget,
        onPlaceBandwidthMarkers = onPlaceBandwidthMarkers,
        onPlaceFullSpanMarkers = onPlaceFullSpanMarkers,
        instrumentSurface = InstrumentSurface,
        instrumentSurfaceVariant = InstrumentSurfaceVariant,
        instrumentDivider = InstrumentDivider,
        instrumentAccent = InstrumentAccent,
        instrumentTextPrimary = InstrumentTextPrimary,
        instrumentTextSecondary = InstrumentTextSecondary
    )
}

@Composable
private fun MarkerDataPanel(
    result: SweepResult,
    markerAPoint: SweepPoint?,
    markerBPoint: SweepPoint?,
    mode: SweepDisplayMode,
    showDelta: Boolean,
    showS21Estimate: Boolean
) {
    SweepMarkerDataPanel(
        result = result,
        markerAPoint = markerAPoint,
        markerBPoint = markerBPoint,
        mode = mode,
        showDelta = showDelta,
        showS21Estimate = showS21Estimate,
        instrumentSurface = InstrumentSurface,
        instrumentDivider = InstrumentDivider,
        instrumentAccent = InstrumentAccent,
        instrumentTextPrimary = InstrumentTextPrimary,
        instrumentTextSecondary = InstrumentTextSecondary
    )
}

@Composable
private fun DiagnosticsSummaryCard(diagnostics: SweepDiagnosticsUiModel?) {
    val mappedDiagnostics = diagnostics?.let {
        SweepDiagnosticsCardModel(
            minimumSwrText = it.minimumSwrText,
            resonanceText = it.resonanceText,
            secondaryResonanceText = it.secondaryResonanceText,
            bandwidthText = it.bandwidthText,
            bandwidthAt15Text = it.bandwidthAt15Text,
            matchingQualityText = it.matchingQualityText,
            impedanceStabilityText = it.impedanceStabilityText,
            sweepShapeText = it.sweepShapeText,
            reactanceTrendText = it.reactanceTrendText,
            mismatchSeverityText = it.mismatchSeverityText,
            likelyConditionText = it.likelyConditionText,
            feedlineLossSuspicionText = it.feedlineLossSuspicionText,
            resonanceCountText = it.resonanceCountText,
            summaryLines = it.summaryLines
        )
    }

    SweepDiagnosticsSummaryPanel(
        diagnostics = mappedDiagnostics,
        instrumentSurface = InstrumentSurface,
        instrumentDivider = InstrumentDivider,
        instrumentAccent = InstrumentAccent,
        instrumentTextPrimary = InstrumentTextPrimary,
        instrumentTextSecondary = InstrumentTextSecondary
    )
}

@Composable
private fun TuningInterpretationCard(
    classification: AntennaBehaviorClassification?,
    suggestionReport: TuningSuggestionReport?,
    adjustmentEstimate: AdjustmentEstimate?,
    workflowReport: TuningWorkflowReport?
) {
    SweepTuningInterpretationPanel(
        classification = classification,
        suggestionReport = suggestionReport,
        adjustmentEstimate = adjustmentEstimate,
        workflowReport = workflowReport,
        instrumentSurface = InstrumentSurface,
        instrumentDivider = InstrumentDivider,
        instrumentAccent = InstrumentAccent,
        instrumentTextPrimary = InstrumentTextPrimary,
        instrumentTextSecondary = InstrumentTextSecondary
    )
}

@Composable
private fun CsvPreviewCard(result: SweepResult, showS21Estimate: Boolean) {
    SweepCsvPreviewPanel(
        result = result,
        showS21Estimate = showS21Estimate,
        instrumentSurface = InstrumentSurface,
        instrumentDivider = InstrumentDivider,
        instrumentAccent = InstrumentAccent,
        instrumentTextPrimary = InstrumentTextPrimary,
        instrumentTextSecondary = InstrumentTextSecondary
    )
}

@Composable
private fun InstrumentCard(content: @Composable () -> Unit) {
    SharedInstrumentCard(
        instrumentSurface = InstrumentSurface,
        instrumentDivider = InstrumentDivider,
        content = content
    )
}

@Composable
private fun InstrumentTitle(text: String) {
    SharedInstrumentTitle(text = text, instrumentTextPrimary = InstrumentTextPrimary)
}

@Composable
private fun InstrumentSectionHeader(text: String) {
    SharedInstrumentSectionHeader(text = text, instrumentAccent = InstrumentAccent)
}

@Composable
private fun InstrumentValueText(text: String) {
    SharedInstrumentValueText(text = text, instrumentTextPrimary = InstrumentTextPrimary)
}

@Composable
private fun InstrumentMutedText(text: String) {
    SharedInstrumentMutedText(text = text, instrumentTextSecondary = InstrumentTextSecondary)
}

@Composable
private fun InstrumentDividerLine() {
    SharedInstrumentDividerLine(instrumentDivider = InstrumentDivider)
}

@Composable
private fun TwoValueRow(label: String, value: String) {
    SharedTwoValueRow(
        label = label,
        value = value,
        instrumentTextPrimary = InstrumentTextPrimary,
        instrumentTextSecondary = InstrumentTextSecondary
    )
}

@Composable
private fun StatusValueText(text: String) {
    val normalized = text.trim().uppercase()
    val statusColor = when {
        normalized.contains("READY") ||
                normalized.contains("LIVE") ||
                normalized.contains("REAL INSTRUMENT") ||
                normalized.contains("TRUSTED") ||
                normalized == "OK" -> InstrumentGreen
        normalized.contains("SIMULATED") ||
                normalized.contains("PARTIAL") ||
                normalized.contains("DEMO") ||
                normalized.contains("DETECTED") ||
                normalized.contains("PENDING") -> InstrumentAccent
        normalized.contains("FAIL") ||
                normalized.contains("LOCKED") ||
                normalized.contains("ERROR") ||
                normalized.contains("NOT READY") ||
                normalized.contains("NO INSTRUMENT") ||
                normalized.contains("UNKNOWN") -> InstrumentMagenta
        else -> InstrumentTextPrimary
    }

    SharedInstrumentValueText(text = text, instrumentTextPrimary = statusColor)
}

@Composable
private fun StatusTwoValueRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SharedInstrumentMutedText(text = label, instrumentTextSecondary = InstrumentTextSecondary)
        StatusValueText(text = value)
    }
}

@Composable
private fun DiscoveryClassificationCard(
    discoveryUi: SweepDiscoveryUiModel,
    onClassificationSelected: (AntennaClassification) -> Unit
) {
    InstrumentCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InstrumentSectionHeader("Discovery Classification")
            InstrumentMutedText(
                "Pick the closest visible antenna shape. Discovery remains detached until you explicitly apply or save it."
            )

            discoveryUi.availableClassifications.forEach { classification ->
                FilterChip(
                    selected = classification == discoveryUi.selectedAntennaClassification,
                    onClick = { onClassificationSelected(classification) },
                    label = {
                        Text(formatAntennaClassificationLabel(classification))
                    }
                )
            }

            discoveryUi.actionStatusText?.let { statusText ->
                InstrumentMutedText(statusText)
            }
        }
    }
}

@Composable
private fun DiscoveryHandoffCard(
    discoveryUi: SweepDiscoveryUiModel,
    onApplyToCurrentProject: () -> Unit,
    onSaveAsNewProject: () -> Unit,
    onReturnWithoutSaving: () -> Unit,
    onDiscardSession: () -> Unit
) {
    InstrumentCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InstrumentSectionHeader("Discovery Handoff")
            InstrumentValueText(discoveryUi.summaryTitle)
            InstrumentMutedText(discoveryUi.summarySupportingText)
            discoveryUi.actionStatusText?.let { statusText ->
                InstrumentMutedText(statusText)
            }

            if (discoveryUi.showHandoffActions) {
                Button(
                    onClick = onApplyToCurrentProject,
                    enabled = discoveryUi.canApplyToCurrentProject
                ) {
                    Text("Apply to Current")
                }

                Button(
                    onClick = onSaveAsNewProject,
                    enabled = discoveryUi.canSaveAsNewProject
                ) {
                    Text("Save as New")
                }

                OutlinedButton(
                    onClick = onReturnWithoutSaving,
                    enabled = discoveryUi.canReturnWithoutSaving
                ) {
                    Text("Return Unsaved")
                }

                OutlinedButton(
                    onClick = onDiscardSession,
                    enabled = discoveryUi.canDiscardSession
                ) {
                    Text("Discard Session")
                }
            }
        }
    }
}

private fun formatAntennaClassificationLabel(

    antennaClassification: AntennaClassification
): String {
    return antennaClassification.name
        .lowercase()
        .split("_")
        .joinToString(" ") { token -> token.replaceFirstChar { it.uppercase() } }
}

private fun buildSweepScreenHardwareDisplayName(
    capabilityProfileDisplayName: String,
    selectedDriverProfile: DriverProfile?
): String {
    val selectedProfileDisplayName = selectedDriverProfile?.let(::buildProfileDisplayLabel)

    return selectedProfileDisplayName
        ?: capabilityProfileDisplayName
}

private fun buildProfileDisplayLabel(profile: DriverProfile): String {
    return if (profile.protocolType.name.contains("LITE", ignoreCase = true)) {
        "LiteVNA64 HW 64-0.3.3 FW v1.4.06"
    } else {
        profile.displayName
    }
}