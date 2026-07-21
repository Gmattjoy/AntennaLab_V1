package com.example.antennalab_v1.features.testing

/*
########################################################################
FILE: SweepWorkspaceController.kt
PACKAGE: com.example.antennalab_v1.features.testing
LAYER: UI / Testing Tools / Workspace Control

LAST UPDATED 16/3/2026 23:45

SYSTEM ROLE
Provides structured mutation helpers for the sweep workspace state.

CURRENT DEVELOPMENT ROLE
This controller owns the pure workspace mutation rules and marker tool
logic used by SweepWorkspaceViewModel.

It currently owns:

• sweep run state updates
• reference trace updates
• compare mode safety
• display mode updates
• marker index updates
• active marker selection
• CSV preview toggle
• history trimming
• compatibility / safety normalization
• marker apply logic
• marker nudge logic
• resonance / center / target jump logic
• user-entered frequency jump logic
• peak search helpers
• SWR bandwidth marker placement
• full-span marker placement

DESIGN INTENT
Keep this layer pure and easy to migrate into a future reducer or
repository-backed state model.

IMPORTANT
This file intentionally avoids Compose APIs and Android dependencies.

SAFE EDIT AREA
- add harmonic search later
- add notch search later
- add richer marker search tools later
########################################################################
*/

import com.example.antennalab_v1.domain.testing.SweepController
import com.example.antennalab_v1.model.AntennaClassification
import com.example.antennalab_v1.model.DiscoverySnapshot
import com.example.antennalab_v1.model.ProjectSweepHistoryEntry
import com.example.antennalab_v1.model.ProjectSweepHistoryMode
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import kotlin.math.abs
import kotlin.math.max

/*
########################################################################
SECTION 1000
CONTROLLER TYPES
########################################################################
PURPOSE
Small pure data carriers used by controller marker operations.
########################################################################
*/
data class WorkspaceMarkerUpdate(
    val markerAIndex: Int,
    val markerBIndex: Int
)

data class WorkspaceMarkerPair(
    val lowerIndex: Int,
    val upperIndex: Int
)

/*
########################################################################
SECTION 1100
CONTROLLER OBJECT
########################################################################
PURPOSE
Stateless mutation helpers for SweepWorkspaceState and marker tools.
########################################################################
*/
object SweepWorkspaceController {

    /*
    ------------------------------------------------------------
    EDIT SECTION 1200
    COMPATIBILITY SAFETY
    ------------------------------------------------------------
    PURPOSE
    Ensures workspace state remains valid when display modes or
    sweep-dependent indexes need clamping.
    ------------------------------------------------------------
    */
    fun ensureCompatibleState(
        currentState: SweepWorkspaceState,
        availableDisplayModes: List<SweepDisplayMode>
    ): SweepWorkspaceState {
        val safeDisplayMode =
            if (currentState.displayMode in availableDisplayModes) {
                currentState.displayMode
            } else {
                availableDisplayModes.firstOrNull() ?: SweepDisplayMode.SWR
            }

        val safeTraceCompareMode =
            if (currentState.referenceSweep == null) {
                TraceCompareMode.CURRENT_ONLY
            } else {
                currentState.traceCompareMode
            }

        val currentSweepLastIndex = currentState.currentSweep?.points?.lastIndex ?: 0

        return currentState.copy(
            displayMode = safeDisplayMode,
            traceCompareMode = safeTraceCompareMode,
            markerAIndex = currentState.markerAIndex.coerceIn(0, currentSweepLastIndex),
            markerBIndex = currentState.markerBIndex.coerceIn(0, currentSweepLastIndex)
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 1300
    SWEEP EXECUTION
    ------------------------------------------------------------
    PURPOSE
    Runs a sweep and applies the standard workspace state update rules.
    ------------------------------------------------------------
    */
    fun runSweep(
        currentState: SweepWorkspaceState,
        startMHz: Double,
        endMHz: Double,
        stepMHz: Double,
        maxHistorySize: Int = 24
    ): SweepWorkspaceState {
        val result = SweepController.runSweep(
            startMHz = startMHz,
            endMHz = endMHz,
            stepMHz = stepMHz
        )

        val updatedHistory = buildUpdatedHistory(
            existingHistory = currentState.sweepHistory,
            newResult = result,
            maxHistorySize = maxHistorySize
        )

        val pendingHistoryEntry = buildSweepHistoryEntry(
            currentState = currentState,
            result = result
        )

        val pendingDiscoverySnapshot =
            if (currentState.isDiscoveryMode) {
                buildDiscoverySnapshot(
                    currentState = currentState,
                    result = result
                )
            } else {
                null
            }

        val workflowStatusMessage =
            if (currentState.isDiscoveryMode && pendingDiscoverySnapshot != null) {
                "Discovery result ready. Choose Apply or Save to keep it."
            } else {
                null
            }

        return currentState.copy(
            currentSweep = result,
            sweepHistory = updatedHistory,
            pendingProjectSweepHistoryEntry = pendingHistoryEntry,
            pendingDiscoverySnapshot = pendingDiscoverySnapshot,
            showDiscoveryHandoffActions =
                currentState.isDiscoveryMode && pendingDiscoverySnapshot != null,
            workflowStatusMessage = workflowStatusMessage,
            markerAIndex = 0,
            markerBIndex = max(0, result.points.lastIndex),
            activeMarkerTarget = WorkspaceMarkerTarget.A,
            showCsvPreview = false
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 1400
    REFERENCE TRACE ACTIONS
    ------------------------------------------------------------
    PURPOSE
    Handles reference trace loading and compare mode safety.
    ------------------------------------------------------------
    */
    fun setCurrentAsReference(
        currentState: SweepWorkspaceState
    ): SweepWorkspaceState {
        val currentSweep = currentState.currentSweep ?: return currentState

        return currentState.copy(
            referenceSweep = currentSweep,
            traceCompareMode =
                if (currentState.traceCompareMode == TraceCompareMode.CURRENT_ONLY) {
                    TraceCompareMode.CURRENT_PLUS_REFERENCE
                } else {
                    currentState.traceCompareMode
                }
        )
    }

    fun setHistorySweepAsReference(
        currentState: SweepWorkspaceState,
        historyIndex: Int
    ): SweepWorkspaceState {
        val referenceSweep = currentState.sweepHistory.getOrNull(historyIndex) ?: return currentState

        return currentState.copy(
            referenceSweep = referenceSweep,
            traceCompareMode =
                if (currentState.traceCompareMode == TraceCompareMode.CURRENT_ONLY) {
                    TraceCompareMode.CURRENT_PLUS_REFERENCE
                } else {
                    currentState.traceCompareMode
                }
        )
    }

    fun clearReference(
        currentState: SweepWorkspaceState
    ): SweepWorkspaceState {
        return currentState.copy(
            referenceSweep = null,
            traceCompareMode = TraceCompareMode.CURRENT_ONLY
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 1500
    DISPLAY / TRACE MODE ACTIONS
    ------------------------------------------------------------
    PURPOSE
    Structured updates for view selection and trace mode changes.
    ------------------------------------------------------------
    */
    fun setDisplayMode(
        currentState: SweepWorkspaceState,
        displayMode: SweepDisplayMode
    ): SweepWorkspaceState {
        return currentState.copy(displayMode = displayMode)
    }

    fun setTraceCompareMode(
        currentState: SweepWorkspaceState,
        traceCompareMode: TraceCompareMode
    ): SweepWorkspaceState {
        return if (
            currentState.referenceSweep == null &&
            traceCompareMode != TraceCompareMode.CURRENT_ONLY
        ) {
            currentState.copy(traceCompareMode = TraceCompareMode.CURRENT_ONLY)
        } else {
            currentState.copy(traceCompareMode = traceCompareMode)
        }
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 1600
    MARKER ACTIONS
    ------------------------------------------------------------
    PURPOSE
    Structured marker target and index updates with sweep-safe clamping.
    ------------------------------------------------------------
    */
    fun setActiveMarkerTarget(
        currentState: SweepWorkspaceState,
        target: WorkspaceMarkerTarget
    ): SweepWorkspaceState {
        return currentState.copy(activeMarkerTarget = target)
    }

    fun setMarkerAIndex(
        currentState: SweepWorkspaceState,
        index: Int
    ): SweepWorkspaceState {
        return currentState.copy(
            markerAIndex = clampMarkerIndex(
                sweep = currentState.currentSweep,
                index = index
            )
        )
    }

    fun setMarkerBIndex(
        currentState: SweepWorkspaceState,
        index: Int
    ): SweepWorkspaceState {
        return currentState.copy(
            markerBIndex = clampMarkerIndex(
                sweep = currentState.currentSweep,
                index = index
            )
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 1700
    MARKER TOOL HELPERS
    ------------------------------------------------------------
    PURPOSE
    Centralizes marker apply, nudge, jump, search, and placement logic
    so MarkerControlPanel can stay mostly UI-only.
    ------------------------------------------------------------
    */
    fun getActiveMarkerIndex(
        activeTarget: WorkspaceMarkerTarget,
        markerAIndex: Int,
        markerBIndex: Int
    ): Int {
        return when (activeTarget) {
            WorkspaceMarkerTarget.A -> markerAIndex
            WorkspaceMarkerTarget.B -> markerBIndex
        }
    }

    fun applyIndexToTarget(
        sweep: SweepResult?,
        target: WorkspaceMarkerTarget,
        markerAIndex: Int,
        markerBIndex: Int,
        index: Int
    ): WorkspaceMarkerUpdate {
        val safeIndex = clampMarkerIndex(sweep, index)

        return when (target) {
            WorkspaceMarkerTarget.A -> WorkspaceMarkerUpdate(
                markerAIndex = safeIndex,
                markerBIndex = markerBIndex
            )

            WorkspaceMarkerTarget.B -> WorkspaceMarkerUpdate(
                markerAIndex = markerAIndex,
                markerBIndex = safeIndex
            )
        }
    }

    fun nudgeActiveMarker(
        sweep: SweepResult?,
        target: WorkspaceMarkerTarget,
        markerAIndex: Int,
        markerBIndex: Int,
        delta: Int
    ): WorkspaceMarkerUpdate {
        val currentIndex = getActiveMarkerIndex(
            activeTarget = target,
            markerAIndex = markerAIndex,
            markerBIndex = markerBIndex
        )

        return applyIndexToTarget(
            sweep = sweep,
            target = target,
            markerAIndex = markerAIndex,
            markerBIndex = markerBIndex,
            index = currentIndex + delta
        )
    }

    fun nudgeMarkerA(
        sweep: SweepResult?,
        markerAIndex: Int,
        markerBIndex: Int,
        delta: Int
    ): WorkspaceMarkerUpdate {
        return WorkspaceMarkerUpdate(
            markerAIndex = clampMarkerIndex(sweep, markerAIndex + delta),
            markerBIndex = markerBIndex
        )
    }

    fun nudgeMarkerB(
        sweep: SweepResult?,
        markerAIndex: Int,
        markerBIndex: Int,
        delta: Int
    ): WorkspaceMarkerUpdate {
        return WorkspaceMarkerUpdate(
            markerAIndex = markerAIndex,
            markerBIndex = clampMarkerIndex(sweep, markerBIndex + delta)
        )
    }

    fun findResonanceIndex(
        sweep: SweepResult?
    ): Int {
        val points = sweep?.points.orEmpty()
        if (points.isEmpty()) return 0

        return points.indices.minByOrNull { index -> points[index].swr } ?: 0
    }

    fun findCenterIndex(
        sweep: SweepResult?
    ): Int {
        val lastIndex = sweep?.points?.lastIndex ?: 0
        return (lastIndex / 2).coerceAtLeast(0)
    }

    fun moveMarkerAToResonance(
        sweep: SweepResult?,
        markerBIndex: Int
    ): WorkspaceMarkerUpdate {
        return WorkspaceMarkerUpdate(
            markerAIndex = findResonanceIndex(sweep),
            markerBIndex = markerBIndex
        )
    }

    fun moveMarkerBToResonance(
        sweep: SweepResult?,
        markerAIndex: Int
    ): WorkspaceMarkerUpdate {
        return WorkspaceMarkerUpdate(
            markerAIndex = markerAIndex,
            markerBIndex = findResonanceIndex(sweep)
        )
    }

    fun moveMarkerAToCenter(
        sweep: SweepResult?,
        markerBIndex: Int
    ): WorkspaceMarkerUpdate {
        return WorkspaceMarkerUpdate(
            markerAIndex = findCenterIndex(sweep),
            markerBIndex = markerBIndex
        )
    }

    fun moveMarkerBToCenter(
        sweep: SweepResult?,
        markerAIndex: Int
    ): WorkspaceMarkerUpdate {
        return WorkspaceMarkerUpdate(
            markerAIndex = markerAIndex,
            markerBIndex = findCenterIndex(sweep)
        )
    }

    fun findNearestFrequencyIndex(
        sweep: SweepResult?,
        frequencyMHz: Double
    ): Int {
        val points = sweep?.points.orEmpty()
        if (points.isEmpty()) return 0

        return points.indices.minByOrNull { index ->
            abs(points[index].frequencyMHz - frequencyMHz)
        } ?: 0
    }

    fun moveActiveMarkerToTargetFrequency(
        sweep: SweepResult?,
        target: WorkspaceMarkerTarget,
        markerAIndex: Int,
        markerBIndex: Int,
        frequencyMHz: Double
    ): WorkspaceMarkerUpdate {
        val nearestIndex = findNearestFrequencyIndex(
            sweep = sweep,
            frequencyMHz = frequencyMHz
        )

        return applyIndexToTarget(
            sweep = sweep,
            target = target,
            markerAIndex = markerAIndex,
            markerBIndex = markerBIndex,
            index = nearestIndex
        )
    }

    fun moveMarkerAToTargetFrequency(
        sweep: SweepResult?,
        markerBIndex: Int,
        frequencyMHz: Double
    ): WorkspaceMarkerUpdate {
        return WorkspaceMarkerUpdate(
            markerAIndex = findNearestFrequencyIndex(sweep, frequencyMHz),
            markerBIndex = markerBIndex
        )
    }

    fun moveMarkerBToTargetFrequency(
        sweep: SweepResult?,
        markerAIndex: Int,
        frequencyMHz: Double
    ): WorkspaceMarkerUpdate {
        return WorkspaceMarkerUpdate(
            markerAIndex = markerAIndex,
            markerBIndex = findNearestFrequencyIndex(sweep, frequencyMHz)
        )
    }

    fun moveActiveMarkerToUserFrequency(
        sweep: SweepResult?,
        target: WorkspaceMarkerTarget,
        markerAIndex: Int,
        markerBIndex: Int,
        frequencyMHz: Double?
    ): WorkspaceMarkerUpdate? {
        val safeFrequency = frequencyMHz ?: return null

        return moveActiveMarkerToTargetFrequency(
            sweep = sweep,
            target = target,
            markerAIndex = markerAIndex,
            markerBIndex = markerBIndex,
            frequencyMHz = safeFrequency
        )
    }

    fun findHighestPeakIndex(
        sweep: SweepResult?,
        mode: SweepDisplayMode
    ): Int? {
        val values = sweep?.points.orEmpty().map { point -> getDisplayValue(point, mode) }
        if (values.isEmpty()) return null

        return values.indices.maxByOrNull { index -> values[index] }
    }

    fun findLocalPeakIndices(
        sweep: SweepResult?,
        mode: SweepDisplayMode
    ): List<Int> {
        val values = sweep?.points.orEmpty().map { point -> getDisplayValue(point, mode) }

        if (values.isEmpty()) {
            return emptyList()
        }

        if (values.size == 1) {
            return listOf(0)
        }

        val peaks = mutableListOf<Int>()

        values.indices.forEach { index ->
            val current = values[index]
            val left = values.getOrNull(index - 1)
            val right = values.getOrNull(index + 1)

            val isPeak =
                when {
                    left == null && right == null -> true
                    left == null && right != null -> current >= right
                    right == null && left != null -> current >= left
                    left != null && right != null ->
                        current >= left && current >= right && (current > left || current > right)
                    else -> false
                }

            if (isPeak) {
                peaks.add(index)
            }
        }

        return peaks.sorted()
    }

    fun moveActiveMarkerToHighestPeak(
        sweep: SweepResult?,
        mode: SweepDisplayMode,
        target: WorkspaceMarkerTarget,
        markerAIndex: Int,
        markerBIndex: Int
    ): WorkspaceMarkerUpdate? {
        val peakIndex = findHighestPeakIndex(
            sweep = sweep,
            mode = mode
        ) ?: return null

        return applyIndexToTarget(
            sweep = sweep,
            target = target,
            markerAIndex = markerAIndex,
            markerBIndex = markerBIndex,
            index = peakIndex
        )
    }

    fun moveActiveMarkerToNextPeak(
        sweep: SweepResult?,
        mode: SweepDisplayMode,
        target: WorkspaceMarkerTarget,
        markerAIndex: Int,
        markerBIndex: Int
    ): WorkspaceMarkerUpdate? {
        val peakIndices = findLocalPeakIndices(
            sweep = sweep,
            mode = mode
        )
        if (peakIndices.isEmpty()) return null

        val currentIndex = getActiveMarkerIndex(
            activeTarget = target,
            markerAIndex = markerAIndex,
            markerBIndex = markerBIndex
        )

        val nextPeak = peakIndices.firstOrNull { it > currentIndex } ?: peakIndices.firstOrNull()
        return nextPeak?.let {
            applyIndexToTarget(
                sweep = sweep,
                target = target,
                markerAIndex = markerAIndex,
                markerBIndex = markerBIndex,
                index = it
            )
        }
    }

    fun moveActiveMarkerToPreviousPeak(
        sweep: SweepResult?,
        mode: SweepDisplayMode,
        target: WorkspaceMarkerTarget,
        markerAIndex: Int,
        markerBIndex: Int
    ): WorkspaceMarkerUpdate? {
        val peakIndices = findLocalPeakIndices(
            sweep = sweep,
            mode = mode
        )
        if (peakIndices.isEmpty()) return null

        val currentIndex = getActiveMarkerIndex(
            activeTarget = target,
            markerAIndex = markerAIndex,
            markerBIndex = markerBIndex
        )

        val previousPeak = peakIndices.lastOrNull { it < currentIndex } ?: peakIndices.lastOrNull()
        return previousPeak?.let {
            applyIndexToTarget(
                sweep = sweep,
                target = target,
                markerAIndex = markerAIndex,
                markerBIndex = markerBIndex,
                index = it
            )
        }
    }

    fun findBandwidthMarkerPair(
        sweep: SweepResult?,
        threshold: Double
    ): WorkspaceMarkerPair? {
        val points = sweep?.points.orEmpty()
        if (points.isEmpty()) {
            return null
        }

        val resonanceIndex = points.indices.minByOrNull { index -> points[index].swr }
            ?: return null

        if (points[resonanceIndex].swr > threshold) {
            return null
        }

        var lowerIndex = resonanceIndex
        var upperIndex = resonanceIndex

        while (lowerIndex > 0 && points[lowerIndex - 1].swr <= threshold) {
            lowerIndex -= 1
        }

        while (upperIndex < points.lastIndex && points[upperIndex + 1].swr <= threshold) {
            upperIndex += 1
        }

        return WorkspaceMarkerPair(
            lowerIndex = lowerIndex,
            upperIndex = upperIndex
        )
    }

    fun placeBandwidthMarkers(
        sweep: SweepResult?,
        threshold: Double
    ): WorkspaceMarkerUpdate? {
        val markers = findBandwidthMarkerPair(
            sweep = sweep,
            threshold = threshold
        ) ?: return null

        return WorkspaceMarkerUpdate(
            markerAIndex = markers.lowerIndex,
            markerBIndex = markers.upperIndex
        )
    }

    fun placeFullSpanMarkers(
        sweep: SweepResult?
    ): WorkspaceMarkerUpdate {
        val lastIndex = sweep?.points?.lastIndex ?: 0

        return WorkspaceMarkerUpdate(
            markerAIndex = 0,
            markerBIndex = lastIndex
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 1800
    TOOL ACTIONS
    ------------------------------------------------------------
    PURPOSE
    Structured updates for workspace tools and visibility toggles.
    ------------------------------------------------------------
    */
    fun setDiscoveryAntennaClassification(
        currentState: SweepWorkspaceState,
        antennaClassification: AntennaClassification
    ): SweepWorkspaceState {
        return currentState.copy(
            discoveryAntennaClassification = antennaClassification,
            workflowStatusMessage = null
        )
    }

    fun completeDiscoveryHandoff(
        currentState: SweepWorkspaceState,
        statusMessage: String
    ): SweepWorkspaceState {
        return currentState.copy(
            pendingDiscoverySnapshot = null,
            pendingProjectSweepHistoryEntry = null,
            showDiscoveryHandoffActions = false,
            workflowStatusMessage = statusMessage
        )
    }

    fun returnWithoutSavingDiscovery(
        currentState: SweepWorkspaceState
    ): SweepWorkspaceState {
        return currentState.copy(
            pendingDiscoverySnapshot = null,
            pendingProjectSweepHistoryEntry = null,
            showDiscoveryHandoffActions = false,
            workflowStatusMessage = "Discovery result was not saved."
        )
    }

    fun discardDiscoverySession(
        currentState: SweepWorkspaceState
    ): SweepWorkspaceState {
        return currentState.copy(
            currentSweep = null,
            referenceSweep = null,
            sweepHistory = emptyList(),
            pendingDiscoverySnapshot = null,
            pendingProjectSweepHistoryEntry = null,
            showDiscoveryHandoffActions = false,
            workflowStatusMessage = "Discovery session discarded.",
            markerAIndex = 0,
            markerBIndex = 0,
            activeMarkerTarget = WorkspaceMarkerTarget.A,
            showCsvPreview = false
        )
    }

    fun toggleCsvPreview(
        currentState: SweepWorkspaceState
    ): SweepWorkspaceState {
        return currentState.copy(showCsvPreview = !currentState.showCsvPreview)
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 1900
    INTERNAL HELPERS
    ------------------------------------------------------------
    PURPOSE
    Pure internal helpers used by controller actions.
    ------------------------------------------------------------
    */
    private fun buildUpdatedHistory(
        existingHistory: List<SweepResult>,
        newResult: SweepResult,
        maxHistorySize: Int
    ): List<SweepResult> {
        return buildList {
            add(newResult)
            addAll(existingHistory)
        }.take(maxHistorySize.coerceAtLeast(1))
    }

    private fun buildSweepHistoryEntry(
        currentState: SweepWorkspaceState,
        result: SweepResult
    ): ProjectSweepHistoryEntry {
        val bestPoint = result.points.minByOrNull { it.swr }

        return ProjectSweepHistoryEntry(
            recordedAtEpochMs = System.currentTimeMillis(),
            mode = if (currentState.isDiscoveryMode) {
                ProjectSweepHistoryMode.DISCOVERY_APPLIED
            } else {
                ProjectSweepHistoryMode.PROJECT_TEST
            },
            hardwareName = result.hardwareProfile.ifBlank { "Unknown Instrument" },
            sweepStartMHz = result.points.firstOrNull()?.frequencyMHz ?: 0.0,
            sweepEndMHz = result.points.lastOrNull()?.frequencyMHz ?: 0.0,
            bestFrequencyMHz = bestPoint?.frequencyMHz ?: 0.0,
            bestSwr = bestPoint?.swr ?: 0.0,
            returnLossDb = bestPoint?.returnLossDb,
            label = if (currentState.isDiscoveryMode) {
                "Discovery ${formatAntennaClassificationLabel(currentState.discoveryAntennaClassification)}"
            } else {
                "Project Sweep"
            },
            note = if (currentState.isDiscoveryMode) {
                "Discovery classification: ${formatAntennaClassificationLabel(currentState.discoveryAntennaClassification)}"
            } else {
                "Project-linked sweep"
            }
        )
    }

    private fun buildDiscoverySnapshot(
        currentState: SweepWorkspaceState,
        result: SweepResult
    ): DiscoverySnapshot {
        val bestPoint = result.points.minByOrNull { it.swr }

        return DiscoverySnapshot(
            capturedAtEpochMs = System.currentTimeMillis(),
            antennaClassificationGuess = currentState.discoveryAntennaClassification,
            hardwareName = result.hardwareProfile.ifBlank { "Unknown Instrument" },
            sweepStartMHz = result.points.firstOrNull()?.frequencyMHz ?: 0.0,
            sweepEndMHz = result.points.lastOrNull()?.frequencyMHz ?: 0.0,
            bestFrequencyMHz = bestPoint?.frequencyMHz ?: 0.0,
            bestSwr = bestPoint?.swr ?: 0.0,
            returnLossDb = bestPoint?.returnLossDb,
            summaryLabel = buildDiscoverySummaryLabel(currentState.discoveryAntennaClassification),
            operatorNotes = "Detached discovery result"
        )
    }

    private fun buildDiscoverySummaryLabel(
        antennaClassification: AntennaClassification
    ): String {
        return "${formatAntennaClassificationLabel(antennaClassification)} discovery"
    }

    private fun formatAntennaClassificationLabel(
        antennaClassification: AntennaClassification
    ): String {
        return antennaClassification.name
            .lowercase()
            .split("_")
            .joinToString(" ") { token -> token.replaceFirstChar { it.uppercase() } }
    }

private fun clampMarkerIndex(
        sweep: SweepResult?,
        index: Int
    ): Int {
        val lastIndex = sweep?.points?.lastIndex ?: 0
        return index.coerceIn(0, lastIndex)
    }

    private fun getDisplayValue(
        point: SweepPoint,
        mode: SweepDisplayMode
    ): Double =
        when (mode) {
            SweepDisplayMode.SWR,
            SweepDisplayMode.ANALOG_SWR,
            SweepDisplayMode.WATERFALL -> point.swr

            SweepDisplayMode.RETURN_LOSS,
            SweepDisplayMode.ANALOG_RETURN_LOSS -> point.returnLossDb

            SweepDisplayMode.RESISTANCE,
            SweepDisplayMode.ANALOG_RESISTANCE -> point.resistance

            SweepDisplayMode.REACTANCE,
            SweepDisplayMode.ANALOG_REACTANCE -> point.reactance

            SweepDisplayMode.S21_ESTIMATE -> -abs(point.returnLossDb * 0.35)
            SweepDisplayMode.SMITH -> point.swr
            SweepDisplayMode.IMPEDANCE_LOCUS -> point.swr
        }
}