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
        val diagnostics = currentSweep?.let(SweepUiModelBuilder::buildDiagnosticsUiModel)

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

        val sweepRunContract = SweepUiModelBuilder.buildSweepRunContract(
            instrumentSessionState = instrumentSessionState,
            latestFailureMessage = effectiveFailureMessage,
            sweepRunInProgress = sweepRunInProgress
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
            selectedSweepPathLabel = SweepUiModelBuilder.buildSelectedSweepPathLabel(instrumentSessionState),
            fallbackReasonText = SweepUiModelBuilder.buildFallbackReasonText(
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
            discoveryUi = SweepUiModelBuilder.buildDiscoveryUiModel(
                state = workspaceState,
                project = project
            )
        )
    }

    fun getCurrentSweepSourceLabel(): String {
        return SweepUiModelBuilder.getCurrentSweepSourceLabel(workspaceState.currentSweep)
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
                    SweepUiModelBuilder.buildOperatorSweepFailureMessage(
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