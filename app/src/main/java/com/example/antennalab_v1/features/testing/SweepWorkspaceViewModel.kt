package com.example.antennalab_v1.features.testing

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.antennalab_v1.domain.analysis.AdjustmentEstimator
import com.example.antennalab_v1.domain.analysis.AntennaBehaviorClassifier
import com.example.antennalab_v1.domain.analysis.TuningSuggestionEngine
import com.example.antennalab_v1.domain.analysis.TuningWorkflowBuilder
import com.example.antennalab_v1.domain.testing.SweepAnalyzer
import com.example.antennalab_v1.domain.testing.SweepController
import com.example.antennalab_v1.domain.testing.UsbSessionManager
import com.example.antennalab_v1.features.app.InstrumentStatusUiMapper
import com.example.antennalab_v1.model.AntennaClassification
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.ProjectSweepHistoryEntry
import com.example.antennalab_v1.storage.ProjectStorage
import com.example.antennalab_v1.model.testing.InstrumentDataSourceKind
import com.example.antennalab_v1.model.testing.InstrumentSessionState
import com.example.antennalab_v1.model.testing.SweepResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
########################################################################
FILE: SweepWorkspaceViewModel.kt
PACKAGE: com.example.antennalab_v1.features.testing
LAYER: UI / Testing Tools / ViewModel

LAST UPDATED 04/04/2026 23:20

IMPORTANT FIX
Sweep execution now performs heavy sweep work on a background dispatcher
but commits Compose/UI state updates back on the main thread. This
prevents intermittent cases where a sweep completed but the graph did
not render reliably on the first attempt.
########################################################################
*/
class SweepWorkspaceViewModel(
    initialState: SweepWorkspaceState
) : ViewModel() {

    var workspaceState by mutableStateOf(initialState)
        private set

    var latestSweepFailureMessage by mutableStateOf<String?>(null)
        private set

    var sweepRunInProgress by mutableStateOf(false)
        private set

    fun buildUiModel(
        context: Context,
        project: ProjectData,
        selectedHardwareName: String,
        targetFrequencyMHz: Double
    ): SweepWorkspaceUiModel {
        val currentSweep = workspaceState.currentSweep
        val resonanceMHz = currentSweep?.let(SweepAnalyzer::getResonantFrequencyMHz)
        val diagnostics = currentSweep?.let(::buildDiagnosticsUiModel)

        val behaviorClassification =
            currentSweep?.let { sweep ->
                AntennaBehaviorClassifier.classify(
                    sweepResult = sweep,
                    targetFrequencyMHz = targetFrequencyMHz
                )
            }

        val tuningSuggestionReport =
            if (behaviorClassification != null) {
                TuningSuggestionEngine.generate(
                    classification = behaviorClassification,
                    targetFrequencyMHz = targetFrequencyMHz,
                    detectedResonanceMHz = resonanceMHz
                )
            } else {
                null
            }

        val adjustmentEstimate =
            if (resonanceMHz != null) {
                AdjustmentEstimator.estimate(
                    targetFrequencyMHz = targetFrequencyMHz,
                    detectedResonanceMHz = resonanceMHz
                )
            } else {
                null
            }

        val tuningWorkflowReport =
            if (
                behaviorClassification != null &&
                tuningSuggestionReport != null &&
                adjustmentEstimate != null
            ) {
                TuningWorkflowBuilder.build(
                    classification = behaviorClassification,
                    suggestionReport = tuningSuggestionReport,
                    adjustmentEstimate = adjustmentEstimate
                )
            } else {
                null
            }

        val markerAPoint = currentSweep?.points?.getOrNull(workspaceState.markerAIndex)
        val markerBPoint = currentSweep?.points?.getOrNull(workspaceState.markerBIndex)

        val instrumentSessionState =
            UsbSessionManager.buildInstrumentSessionState(
                context = context,
                selectedHardwareName = selectedHardwareName
            )

        val instrumentStatusCard =
            InstrumentStatusUiMapper.buildCardUiModel(
                context = context,
                selectedHardwareName = selectedHardwareName
            )

        val detailsModel =
            InstrumentStatusUiMapper.buildDetailsUiModel(
                context = context,
                selectedHardwareName = selectedHardwareName
            )

        val effectiveFailureMessage = resolveActiveFailureMessage(
            currentSweep = currentSweep,
            latestFailureMessage = latestSweepFailureMessage
        )

        val sweepRunContract = buildSweepRunContract(
            instrumentSessionState = instrumentSessionState,
            latestFailureMessage = effectiveFailureMessage
        )

        return SweepWorkspaceUiModel(
            resonanceMHz = resonanceMHz,
            diagnostics = diagnostics,
            behaviorClassification = behaviorClassification,
            tuningSuggestionReport = tuningSuggestionReport,
            adjustmentEstimate = adjustmentEstimate,
            tuningWorkflowReport = tuningWorkflowReport,
            markerAPoint = markerAPoint,
            markerBPoint = markerBPoint,
            currentSweepSourceLabel = getCurrentSweepSourceLabel(),
            selectedSweepPathLabel = buildSelectedSweepPathLabel(instrumentSessionState),
            fallbackReasonText = buildFallbackReasonText(
                currentSweep = currentSweep,
                instrumentSessionState = instrumentSessionState,
                latestFailureMessage = effectiveFailureMessage
            ),
            instrumentSessionState = instrumentSessionState,
            instrumentStatusCard = instrumentStatusCard,
            engineeringDetails = SweepEngineeringDetailsUiModel(
                commandStatusLabel = detailsModel.lastCommandStatusLabel,
                lastReadSizeLabel = detailsModel.lastReadSizeLabel,
                lastErrorLabel = detailsModel.lastErrorLabel,
                transportSummary = detailsModel.transportSummary,
                liteVnaDebugSummary = detailsModel.liteVnaDebugSummary
            ),
            sweepRunContract = sweepRunContract,
            discoveryUi = buildDiscoveryUiModel(project = project)
        )
    }

    fun getCurrentSweepSourceLabel(): String {
        val currentSweep = workspaceState.currentSweep
        val hardwareProfile = currentSweep?.hardwareProfile

        return when {
            currentSweep == null -> "No Sweep Loaded"
            hardwareProfile.isNullOrBlank() -> "Loaded Sweep"
            hardwareProfile.equals("SIMULATED", ignoreCase = true) -> "Simulated Sweep"
            else -> hardwareProfile
        }
    }

    fun ensureCompatibleState(
        availableDisplayModes: List<SweepDisplayMode>
    ) {
        workspaceState = SweepWorkspaceController.ensureCompatibleState(
            currentState = workspaceState,
            availableDisplayModes = availableDisplayModes
        )
    }


    fun setDiscoveryAntennaClassification(
        antennaClassification: AntennaClassification
    ) {
        workspaceState = SweepWorkspaceController.setDiscoveryAntennaClassification(
            currentState = workspaceState,
            antennaClassification = antennaClassification
        )
    }

    fun applyDiscoveryToCurrentProject(
        context: Context,
        project: ProjectData
    ): ProjectData? {
        val snapshot = workspaceState.pendingDiscoverySnapshot ?: return null
        val historyEntry = workspaceState.pendingProjectSweepHistoryEntry ?: return null

        val updatedProject = runCatching {
            ProjectStorage.applyDiscoveryToCurrentProject(
                context = context,
                project = project,
                discoverySnapshot = snapshot,
                historyEntry = historyEntry
            )
        }.getOrNull()

        if (updatedProject != null) {
            workspaceState = SweepWorkspaceController.completeDiscoveryHandoff(
                currentState = workspaceState,
                statusMessage = "Discovery applied to current project."
            )
        }

        return updatedProject
    }

    fun saveDiscoveryAsNewProject(
        context: Context,
        sourceProject: ProjectData
    ): ProjectData? {
        val snapshot = workspaceState.pendingDiscoverySnapshot ?: return null
        val historyEntry = workspaceState.pendingProjectSweepHistoryEntry ?: return null

        val savedProject = runCatching {
            ProjectStorage.saveDiscoveryAsNewProject(
                context = context,
                sourceProject = sourceProject,
                discoverySnapshot = snapshot,
                historyEntry = historyEntry
            )
        }.getOrNull()

        if (savedProject != null) {
            workspaceState = SweepWorkspaceController.completeDiscoveryHandoff(
                currentState = workspaceState,
                statusMessage = "Discovery saved as a new project."
            )
        }

        return savedProject
    }

    fun returnWithoutSavingDiscovery() {
        workspaceState = SweepWorkspaceController.returnWithoutSavingDiscovery(workspaceState)
    }

    fun discardDiscoverySession() {
        workspaceState = SweepWorkspaceController.discardDiscoverySession(workspaceState)
    }

    fun runSweep(
        context: Context,
        project: ProjectData,
        startMHz: Double,
        endMHz: Double,
        stepMHz: Double
    ) {
        if (sweepRunInProgress) return

        sweepRunInProgress = true
        latestSweepFailureMessage = null

        viewModelScope.launch {
            val currentStateSnapshot = workspaceState

            val outcome = withContext(Dispatchers.Default) {
                runCatching {
                    SweepWorkspaceController.runSweep(
                        currentState = currentStateSnapshot,
                        startMHz = startMHz,
                        endMHz = endMHz,
                        stepMHz = stepMHz
                    )
                }
            }

            if (outcome.isSuccess) {
                val updatedState = outcome.getOrThrow()
                var committedState = updatedState

                if (!project.isUnknownDiscoverySession) {
                    val historyEntry = updatedState.pendingProjectSweepHistoryEntry
                    if (historyEntry != null) {
                        committedState =
                            if (historyEntry.isComplete) {
                                val savedProject =
                                    appendProjectHistoryEntry(context, project, historyEntry)
                                if (savedProject != null) {
                                    updatedState.copy(
                                        pendingProjectSweepHistoryEntry = null,
                                        workflowStatusMessage = "Project sweep added to Sweep History."
                                    )
                                } else {
                                    updatedState.copy(
                                        workflowStatusMessage = "Sweep completed, but project history could not be saved."
                                    )
                                }
                            } else {
                                // Incomplete sweep: hold the entry for operator
                                // confirmation instead of auto-saving it. The sweep
                                // data itself stays on screen either way.
                                updatedState.copy(
                                    pendingProjectSweepHistoryEntry = null,
                                    pendingIncompleteSaveEntry = historyEntry,
                                    workflowStatusMessage = "Partial sweep — confirm before saving to history."
                                )
                            }
                    }
                }

                workspaceState = committedState
                latestSweepFailureMessage = null
            } else {
                val error = outcome.exceptionOrNull()
                val structuredDetail =
                    SweepController.getLastExecutionError()?.detail
                        ?.takeIf { it.isNotBlank() }

                latestSweepFailureMessage =
                    buildOperatorSweepFailureMessage(
                        rawFailureMessage = structuredDetail ?: error?.message
                    )
            }

            sweepRunInProgress = false
        }
    }

    /*
    ------------------------------------------------------------
    INCOMPLETE-SWEEP SAVE CONFIRMATION
    ------------------------------------------------------------
    PURPOSE
    A partial (incomplete) project sweep is not auto-saved. Its
    history entry is held in state.pendingIncompleteSaveEntry and
    the operator confirms or dismisses the save from the workspace.
    ------------------------------------------------------------
    */
    fun confirmSaveIncompleteEntry(context: Context, project: ProjectData) {
        val entry = workspaceState.pendingIncompleteSaveEntry ?: return

        viewModelScope.launch {
            val savedProject = appendProjectHistoryEntry(context, project, entry)
            workspaceState =
                if (savedProject != null) {
                    workspaceState.copy(
                        pendingIncompleteSaveEntry = null,
                        workflowStatusMessage = "Partial sweep added to Sweep History."
                    )
                } else {
                    // Keep the pending entry so the operator can retry.
                    workspaceState.copy(
                        workflowStatusMessage = "Partial sweep could not be saved to history."
                    )
                }
        }
    }

    fun dismissSaveIncompleteEntry() {
        if (workspaceState.pendingIncompleteSaveEntry == null) return
        workspaceState = workspaceState.copy(
            pendingIncompleteSaveEntry = null,
            workflowStatusMessage = "Partial sweep not saved to history."
        )
    }

    private suspend fun appendProjectHistoryEntry(
        context: Context,
        project: ProjectData,
        historyEntry: ProjectSweepHistoryEntry
    ): ProjectData? {
        return withContext(Dispatchers.IO) {
            runCatching {
                ProjectStorage.appendProjectSweepHistoryEntry(
                    context = context,
                    project = project,
                    historyEntry = historyEntry
                )
            }.getOrNull()
        }
    }

    fun setCurrentAsReference() {
        workspaceState = SweepWorkspaceController.setCurrentAsReference(workspaceState)
    }

    fun setHistorySweepAsReference(historyIndex: Int) {
        workspaceState = SweepWorkspaceController.setHistorySweepAsReference(
            currentState = workspaceState,
            historyIndex = historyIndex
        )
    }

    fun clearReference() {
        workspaceState = SweepWorkspaceController.clearReference(workspaceState)
    }

    fun setDisplayMode(displayMode: SweepDisplayMode) {
        workspaceState = SweepWorkspaceController.setDisplayMode(
            currentState = workspaceState,
            displayMode = displayMode
        )
    }

    fun setTraceCompareMode(traceCompareMode: TraceCompareMode) {
        workspaceState = SweepWorkspaceController.setTraceCompareMode(
            currentState = workspaceState,
            traceCompareMode = traceCompareMode
        )
    }

    fun setActiveMarkerTarget(target: WorkspaceMarkerTarget) {
        workspaceState = SweepWorkspaceController.setActiveMarkerTarget(
            currentState = workspaceState,
            target = target
        )
    }

    fun setMarkerAIndex(index: Int) {
        workspaceState = SweepWorkspaceController.setMarkerAIndex(
            currentState = workspaceState,
            index = index
        )
    }

    fun setMarkerBIndex(index: Int) {
        workspaceState = SweepWorkspaceController.setMarkerBIndex(
            currentState = workspaceState,
            index = index
        )
    }

    fun getResonanceIndex(): Int {
        return SweepWorkspaceController.findResonanceIndex(workspaceState.currentSweep)
    }

    fun getPeakIndices(mode: SweepDisplayMode): List<Int> {
        return SweepWorkspaceController.findLocalPeakIndices(
            sweep = workspaceState.currentSweep,
            mode = mode
        )
    }

    fun getHighestPeakIndex(mode: SweepDisplayMode): Int? {
        return SweepWorkspaceController.findHighestPeakIndex(
            sweep = workspaceState.currentSweep,
            mode = mode
        )
    }

    fun hasBandwidthMarkerPair(threshold: Double): Boolean {
        return SweepWorkspaceController.findBandwidthMarkerPair(
            sweep = workspaceState.currentSweep,
            threshold = threshold
        ) != null
    }

    fun nudgeActiveMarker(delta: Int) {
        applyMarkerUpdate(
            SweepWorkspaceController.nudgeActiveMarker(
                sweep = workspaceState.currentSweep,
                target = workspaceState.activeMarkerTarget,
                markerAIndex = workspaceState.markerAIndex,
                markerBIndex = workspaceState.markerBIndex,
                delta = delta
            )
        )
    }

    fun nudgeMarkerA(delta: Int) {
        applyMarkerUpdate(
            SweepWorkspaceController.nudgeMarkerA(
                sweep = workspaceState.currentSweep,
                markerAIndex = workspaceState.markerAIndex,
                markerBIndex = workspaceState.markerBIndex,
                delta = delta
            )
        )
    }

    fun nudgeMarkerB(delta: Int) {
        applyMarkerUpdate(
            SweepWorkspaceController.nudgeMarkerB(
                sweep = workspaceState.currentSweep,
                markerAIndex = workspaceState.markerAIndex,
                markerBIndex = workspaceState.markerBIndex,
                delta = delta
            )
        )
    }

    fun moveActiveMarkerToResonance() {
        applyMarkerUpdate(
            SweepWorkspaceController.applyIndexToTarget(
                sweep = workspaceState.currentSweep,
                target = workspaceState.activeMarkerTarget,
                markerAIndex = workspaceState.markerAIndex,
                markerBIndex = workspaceState.markerBIndex,
                index = getResonanceIndex()
            )
        )
    }

    fun moveActiveMarkerToHighestPeak(mode: SweepDisplayMode) {
        SweepWorkspaceController.moveActiveMarkerToHighestPeak(
            sweep = workspaceState.currentSweep,
            mode = mode,
            target = workspaceState.activeMarkerTarget,
            markerAIndex = workspaceState.markerAIndex,
            markerBIndex = workspaceState.markerBIndex
        )?.let(::applyMarkerUpdate)
    }

    fun moveActiveMarkerToNextPeak(mode: SweepDisplayMode) {
        SweepWorkspaceController.moveActiveMarkerToNextPeak(
            sweep = workspaceState.currentSweep,
            mode = mode,
            target = workspaceState.activeMarkerTarget,
            markerAIndex = workspaceState.markerAIndex,
            markerBIndex = workspaceState.markerBIndex
        )?.let(::applyMarkerUpdate)
    }

    fun moveActiveMarkerToPreviousPeak(mode: SweepDisplayMode) {
        SweepWorkspaceController.moveActiveMarkerToPreviousPeak(
            sweep = workspaceState.currentSweep,
            mode = mode,
            target = workspaceState.activeMarkerTarget,
            markerAIndex = workspaceState.markerAIndex,
            markerBIndex = workspaceState.markerBIndex
        )?.let(::applyMarkerUpdate)
    }

    fun moveMarkerAToResonance() {
        applyMarkerUpdate(
            SweepWorkspaceController.moveMarkerAToResonance(
                sweep = workspaceState.currentSweep,
                markerBIndex = workspaceState.markerBIndex
            )
        )
    }

    fun moveMarkerBToResonance() {
        applyMarkerUpdate(
            SweepWorkspaceController.moveMarkerBToResonance(
                sweep = workspaceState.currentSweep,
                markerAIndex = workspaceState.markerAIndex
            )
        )
    }

    fun moveMarkerAToCenter() {
        applyMarkerUpdate(
            SweepWorkspaceController.moveMarkerAToCenter(
                sweep = workspaceState.currentSweep,
                markerBIndex = workspaceState.markerBIndex
            )
        )
    }

    fun moveMarkerBToCenter() {
        applyMarkerUpdate(
            SweepWorkspaceController.moveMarkerBToCenter(
                sweep = workspaceState.currentSweep,
                markerAIndex = workspaceState.markerAIndex
            )
        )
    }

    fun moveActiveMarkerToTargetFrequency(frequencyMHz: Double) {
        applyMarkerUpdate(
            SweepWorkspaceController.moveActiveMarkerToTargetFrequency(
                sweep = workspaceState.currentSweep,
                target = workspaceState.activeMarkerTarget,
                markerAIndex = workspaceState.markerAIndex,
                markerBIndex = workspaceState.markerBIndex,
                frequencyMHz = frequencyMHz
            )
        )
    }

    fun moveActiveMarkerToUserFrequency(frequencyMHz: Double?) {
        SweepWorkspaceController.moveActiveMarkerToUserFrequency(
            sweep = workspaceState.currentSweep,
            target = workspaceState.activeMarkerTarget,
            markerAIndex = workspaceState.markerAIndex,
            markerBIndex = workspaceState.markerBIndex,
            frequencyMHz = frequencyMHz
        )?.let(::applyMarkerUpdate)
    }

    fun moveMarkerAToTargetFrequency(frequencyMHz: Double) {
        applyMarkerUpdate(
            SweepWorkspaceController.moveMarkerAToTargetFrequency(
                sweep = workspaceState.currentSweep,
                markerBIndex = workspaceState.markerBIndex,
                frequencyMHz = frequencyMHz
            )
        )
    }

    fun moveMarkerBToTargetFrequency(frequencyMHz: Double) {
        applyMarkerUpdate(
            SweepWorkspaceController.moveMarkerBToTargetFrequency(
                sweep = workspaceState.currentSweep,
                markerAIndex = workspaceState.markerAIndex,
                frequencyMHz = frequencyMHz
            )
        )
    }

    fun placeBandwidthMarkers(threshold: Double) {
        SweepWorkspaceController.placeBandwidthMarkers(
            sweep = workspaceState.currentSweep,
            threshold = threshold
        )?.let(::applyMarkerUpdate)
    }

    fun placeFullSpanMarkers() {
        applyMarkerUpdate(
            SweepWorkspaceController.placeFullSpanMarkers(
                sweep = workspaceState.currentSweep
            )
        )
    }

    fun toggleCsvPreview() {
        workspaceState = SweepWorkspaceController.toggleCsvPreview(workspaceState)
    }

    private fun applyMarkerUpdate(update: WorkspaceMarkerUpdate) {
        workspaceState = workspaceState.copy(
            markerAIndex = update.markerAIndex,
            markerBIndex = update.markerBIndex
        )
    }

    private fun buildDiscoveryUiModel(
        project: ProjectData
    ): SweepDiscoveryUiModel {
        if (!workspaceState.isDiscoveryMode) {
            return SweepDiscoveryUiModel()
        }

        val pendingSnapshot = workspaceState.pendingDiscoverySnapshot

        val summaryTitle =
            if (pendingSnapshot == null) {
                "No discovery result yet."
            } else {
                buildDiscoverySummaryTitle(pendingSnapshot)
            }

        val summarySupportingText =
            if (pendingSnapshot == null) {
                "Select the closest visible antenna shape, then run a sweep. Discovery stays detached until you apply or save it."
            } else {
                buildDiscoverySummarySupportingText(pendingSnapshot)
            }

        return SweepDiscoveryUiModel(
            isDiscoveryMode = true,
            selectedAntennaClassification = workspaceState.discoveryAntennaClassification,
            summaryTitle = summaryTitle,
            summarySupportingText = summarySupportingText,
            showHandoffActions = workspaceState.showDiscoveryHandoffActions,
            canApplyToCurrentProject =
                workspaceState.pendingProjectSweepHistoryEntry != null &&
                        (project.meta.projectId.isNotBlank() || project.meta.projectName.isNotBlank()),
            canSaveAsNewProject = workspaceState.pendingProjectSweepHistoryEntry != null,
            canReturnWithoutSaving = workspaceState.showDiscoveryHandoffActions,
            canDiscardSession = true,
            actionStatusText = workspaceState.workflowStatusMessage
        )
    }

    private fun buildDiscoverySummaryTitle(
        discoverySnapshot: com.example.antennalab_v1.model.DiscoverySnapshot
    ): String {
        return formatAntennaClassificationLabel(discoverySnapshot.antennaClassificationGuess)
    }

    private fun buildDiscoverySummarySupportingText(
        discoverySnapshot: com.example.antennalab_v1.model.DiscoverySnapshot
    ): String {
        val rlText = discoverySnapshot.returnLossDb?.let { value ->
            String.format(" | RL %.2f dB", value)
        }.orEmpty()

        return String.format(
            "Best SWR %.2f at %.3f MHz | Sweep %.3f → %.3f MHz%s",
            discoverySnapshot.bestSwr,
            discoverySnapshot.bestFrequencyMHz,
            discoverySnapshot.sweepStartMHz,
            discoverySnapshot.sweepEndMHz,
            rlText
        )
    }

    private fun formatAntennaClassificationLabel(
        antennaClassification: AntennaClassification
    ): String {
        return antennaClassification.name
            .lowercase()
            .split("_")
            .joinToString(" ") { token -> token.replaceFirstChar { it.uppercase() } }
    }

    private fun resolveActiveFailureMessage(
        currentSweep: SweepResult?,
        latestFailureMessage: String?
    ): String? {
        if (latestFailureMessage.isNullOrBlank()) {
            return null
        }

        val hasCurrentRealSweep =
            currentSweep != null &&
                    !currentSweep.hardwareProfile.equals("SIMULATED", ignoreCase = true)

        if (hasCurrentRealSweep && SweepController.hasSuccessfulRealSweepAfterLastFailure()) {
            latestSweepFailureMessage = null
            return null
        }

        return latestFailureMessage
    }

    private fun buildSweepRunContract(
        instrumentSessionState: InstrumentSessionState,
        latestFailureMessage: String?
    ): SweepRunContract {
        val calibrationState = instrumentSessionState.calibrationState

        val liveSweepAllowed =
            instrumentSessionState.dataSourceKind == InstrumentDataSourceKind.REAL_INSTRUMENT &&
                    instrumentSessionState.transportReady &&
                    (instrumentSessionState.supportTier == "Full Support" ||
                            instrumentSessionState.supportTier == "Partial Support")

        val demoSweepAllowed =
            instrumentSessionState.dataSourceKind == InstrumentDataSourceKind.SIMULATED

        val blockReason = when {
            sweepRunInProgress -> "Sweep is currently running."
            liveSweepAllowed -> null
            !latestFailureMessage.isNullOrBlank() -> latestFailureMessage
            instrumentSessionState.connectionInfo.state.name == "NOT_CONNECTED" -> "No instrument is connected."
            instrumentSessionState.connectionInfo.state.name == "PERMISSION_REQUIRED" -> "USB permission is required before live sweep can run."
            !instrumentSessionState.connectionInfo.sessionOpen -> "USB session is not open."
            !instrumentSessionState.transportReady -> "Transport is not ready."
            instrumentSessionState.supportTier != "Full Support" &&
                    instrumentSessionState.supportTier != "Partial Support" &&
                    instrumentSessionState.dataSourceKind == InstrumentDataSourceKind.REAL_INSTRUMENT ->
                "Detected instrument is not yet at a supported tier for live sweep."
            demoSweepAllowed -> "Real transport is not ready. Demo sweep is available instead."
            else -> "Sweep is locked until instrument readiness improves."
        }

        val runButtonText = when {
            sweepRunInProgress -> "Sweep Running..."
            liveSweepAllowed -> "Run Live Sweep"
            demoSweepAllowed -> "Run Demo Sweep"
            else -> "Run Sweep Locked"
        }

        val calibrationStateLabel = when (calibrationState.readiness) {
            com.example.antennalab_v1.model.testing.CalibrationReadiness.VALID -> "Valid"
            com.example.antennalab_v1.model.testing.CalibrationReadiness.IN_PROGRESS -> "In Progress"
            com.example.antennalab_v1.model.testing.CalibrationReadiness.STALE -> "Stale"
            com.example.antennalab_v1.model.testing.CalibrationReadiness.INVALID -> "Invalid"
            com.example.antennalab_v1.model.testing.CalibrationReadiness.NOT_STARTED -> "Not Started"
        }

        val calibrationWarningText = when (calibrationState.readiness) {
            com.example.antennalab_v1.model.testing.CalibrationReadiness.VALID -> null
            else -> calibrationState.operatorWarning
        }

        val statusText = when {
            sweepRunInProgress ->
                "Sweep is running on a background worker so the UI remains responsive."
            liveSweepAllowed && !calibrationState.trustDowngraded ->
                "Live instrument path is ready. Sweep will use the current real transport path with calibration valid for this session."
            liveSweepAllowed && calibrationState.trustDowngraded ->
                "Live instrument path is ready, but calibration trust is downgraded. Sweep will still run and should be interpreted with caution."
            demoSweepAllowed ->
                "Real transport is not ready. Running now will use simulated sweep data."
            else ->
                "Sweep is locked. Return to System → Device Connections until transport is ready or a safe simulated path is selected."
        }

        return SweepRunContract(
            runButtonText = runButtonText,
            runEnabled = !sweepRunInProgress && (liveSweepAllowed || demoSweepAllowed),
            runUsesRealInstrument = liveSweepAllowed,
            runUsesSimulation = demoSweepAllowed,
            statusText = statusText,
            blockReason = blockReason,
            calibrationStateLabel = calibrationStateLabel,
            calibrationStatusText = calibrationState.statusSummary,
            calibrationWarningText = calibrationWarningText,
            trustDowngraded = calibrationState.trustDowngraded
        )
    }

    private fun buildDiagnosticsUiModel(result: SweepResult): SweepDiagnosticsUiModel {
        val diagnostics = com.example.antennalab_v1.domain.testing.SweepDiagnosticsEngine.analyzeSweep(result)

        val summaryLines = diagnostics.summary
            .split(". ")
            .map { it.trim() }
            .map { line -> line.removeSuffix(".") }
            .filter { it.isNotBlank() }

        return SweepDiagnosticsUiModel(
            minimumSwrText = String.format("%.3f", diagnostics.minimumSwr),
            resonanceText = String.format("%.3f MHz", diagnostics.resonanceFrequencyMHz),
            secondaryResonanceText = diagnostics.secondaryResonanceFrequencyMHz?.let {
                String.format("%.3f MHz", it)
            },
            bandwidthText = String.format("%.3f MHz", diagnostics.estimatedBandwidthMHz),
            bandwidthAt15Text = String.format("%.3f MHz", diagnostics.estimatedBandwidthAt15MHz),
            matchingQualityText = diagnostics.matchingQuality.name,
            impedanceStabilityText = diagnostics.impedanceStability.name,
            sweepShapeText = diagnostics.sweepShape.name,
            reactanceTrendText = diagnostics.reactanceTrend.name,
            mismatchSeverityText = diagnostics.mismatchSeverity.name,
            likelyConditionText = diagnostics.likelyCondition.name,
            feedlineLossSuspicionText = diagnostics.feedlineLossSuspicion.name,
            resonanceCountText = diagnostics.resonanceCountEstimate.toString(),
            summaryLines = summaryLines
        )
    }

    private fun buildSelectedSweepPathLabel(
        instrumentSessionState: InstrumentSessionState
    ): String {
        return when (instrumentSessionState.dataSourceKind) {
            InstrumentDataSourceKind.REAL_INSTRUMENT -> "Real Instrument"
            InstrumentDataSourceKind.SIMULATED -> "Simulated Instrument"
            InstrumentDataSourceKind.NONE -> "No Instrument"
        }
    }

    private fun buildFallbackReasonText(
        currentSweep: SweepResult?,
        instrumentSessionState: InstrumentSessionState,
        latestFailureMessage: String?
    ): String? {
        val hardwareProfile = currentSweep?.hardwareProfile

        return when {
            !latestFailureMessage.isNullOrBlank() ->
                "Latest sweep failure: $latestFailureMessage"
            hardwareProfile.isNullOrBlank() -> null
            hardwareProfile.equals("SIMULATED", ignoreCase = true) ->
                "Current loaded sweep is simulated. ${instrumentSessionState.sessionSummary}"
            instrumentSessionState.dataSourceKind == InstrumentDataSourceKind.SIMULATED ->
                "Session is currently operating in simulated mode. ${instrumentSessionState.sessionSummary}"
            else -> null
        }
    }

    private fun buildOperatorSweepFailureMessage(rawFailureMessage: String?): String {
        val failureText = rawFailureMessage?.trim().orEmpty()
        if (failureText.isBlank()) {
            return "Sweep failed for an unknown reason. Check transport health and try again."
        }

        return when {
            failureText.contains("transport is not ready", ignoreCase = true) ->
                "Transport is not ready. Return to System → Device Connections and confirm USB session, permission, and transport readiness."
            failureText.contains("no usb session is open", ignoreCase = true) ->
                "No USB session is open. Open the analyzer session before running a live sweep."
            failureText.contains("no active usb transport channel", ignoreCase = true) ->
                "No active USB transport channel is available. Re-scan the device and reopen the session."
            failureText.contains("identity query failed", ignoreCase = true) ->
                "Analyzer identity failed before sweep start. Check command transport health and retry."
            failureText.contains("not at full support tier", ignoreCase = true) ->
                "This detected device is not yet approved for real sweep execution. Discovery succeeded, but live sweep remains blocked."
            failureText.contains("too few usable sweep points", ignoreCase = true) ->
                "Live sweep returned too few usable points for safe graphing. Increase sweep stability or retry."
            failureText.contains("failed validation", ignoreCase = true) ->
                "Live sweep returned data, but validation rejected it. Check the transport summary and retry."
            failureText.contains("truncated", ignoreCase = true) ->
                "Sweep data looked truncated. Communication may be unstable. Check cable quality, USB stability, and retry."
            failureText.contains("repeated frequency", ignoreCase = true) ->
                "Sweep data contained repeated frequency samples. The device response looked malformed. Retry the sweep and inspect transport health."
            failureText.contains("not strictly increasing", ignoreCase = true) ->
                "Sweep ordering was invalid. The returned sweep frame looks malformed or incomplete."
            failureText.contains("non-finite", ignoreCase = true) ->
                "Sweep data contained invalid numeric values. Retry and inspect transport health."
            failureText.contains("invalid sweepresult", ignoreCase = true) ->
                "Sweep result failed validation. Retry and inspect the command/transport diagnostics."
            else -> failureText
        }
    }

    companion object {
        fun provideFactory(initialState: SweepWorkspaceState): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SweepWorkspaceViewModel(initialState = initialState) as T
                }
            }
        }
    }
}