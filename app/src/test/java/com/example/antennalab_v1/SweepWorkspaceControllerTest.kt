package com.example.antennalab_v1

import com.example.antennalab_v1.domain.analysis.ChartKind
import com.example.antennalab_v1.features.testing.SweepDisplayMode
import com.example.antennalab_v1.features.testing.SweepWorkspaceController
import com.example.antennalab_v1.features.testing.SweepWorkspaceState
import com.example.antennalab_v1.features.testing.TraceCompareMode
import com.example.antennalab_v1.features.testing.WorkspaceMarkerTarget
import com.example.antennalab_v1.model.AntennaClassification
import com.example.antennalab_v1.model.ProjectSweepHistoryMode
import com.example.antennalab_v1.model.testing.InstrumentDataSourceKind
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Behavior coverage for the pure workspace mutation + marker-tool logic in
 * [SweepWorkspaceController]. These exercise the real controller against real
 * SweepResult/SweepPoint models (no mocking); runSweep drives the real simulated
 * SweepController path.
 */
@RunWith(RobolectricTestRunner::class)
class SweepWorkspaceControllerTest {

    private fun point(
        frequencyMHz: Double,
        swr: Double,
        returnLossDb: Double = -10.0,
        resistance: Double = 50.0,
        reactance: Double = 0.0
    ) = SweepPoint(
        frequencyMHz = frequencyMHz,
        swr = swr,
        returnLossDb = returnLossDb,
        resistance = resistance,
        reactance = reactance
    )

    /** A five-point sweep with a clear SWR valley at index 2. */
    private fun valleySweep() = SweepResult(
        startFrequencyMHz = 10.0,
        endFrequencyMHz = 14.0,
        stepMHz = 1.0,
        points = listOf(
            point(10.0, swr = 3.0),
            point(11.0, swr = 1.5),
            point(12.0, swr = 1.2),
            point(13.0, swr = 1.4),
            point(14.0, swr = 3.0)
        )
    )

    /** Every chart kind available — the "no capability has been lost" case. */
    private val ALL_CHART_KINDS = listOf(
        ChartKind.SWR,
        ChartKind.SMITH,
        ChartKind.RETURN_LOSS,
        ChartKind.PHASE
    )

    // ------------------------------------------------------------------
    // Transient chart focus (tap-to-expand) — spec 2.2(b)
    // ------------------------------------------------------------------
    // Focus is a nullable OVERLAY, never a value of the layout mode. These
    // pin the three toggle cases, the explicit collapse, the capability
    // strand-guard, and — the structural one — that focus touches nothing
    // else in the state.

    @Test
    fun toggleExpandedChart_focusesWhenNothingIsExpanded() {
        val state = SweepWorkspaceState(currentSweep = valleySweep())
        assertNull(state.expandedChartKind)

        val result = SweepWorkspaceController.toggleExpandedChart(state, ChartKind.SWR)

        assertEquals(ChartKind.SWR, result.expandedChartKind)
    }

    @Test
    fun toggleExpandedChart_collapsesWhenTheSameKindIsTappedAgain() {
        val state = SweepWorkspaceState(
            currentSweep = valleySweep(),
            expandedChartKind = ChartKind.SMITH
        )

        val result = SweepWorkspaceController.toggleExpandedChart(state, ChartKind.SMITH)

        assertNull(result.expandedChartKind)
    }

    @Test
    fun toggleExpandedChart_switchesWhenADifferentKindIsTapped() {
        // The case a naive `if (expanded != null) null else kind` gets wrong:
        // tapping another chart moves focus, it does not collapse. Comparing
        // two traces is the common operator gesture.
        val state = SweepWorkspaceState(
            currentSweep = valleySweep(),
            expandedChartKind = ChartKind.SWR
        )

        val result = SweepWorkspaceController.toggleExpandedChart(state, ChartKind.PHASE)

        assertEquals(ChartKind.PHASE, result.expandedChartKind)
    }

    @Test
    fun collapseExpandedChart_clearsFocus() {
        val state = SweepWorkspaceState(
            currentSweep = valleySweep(),
            expandedChartKind = ChartKind.RETURN_LOSS
        )

        val result = SweepWorkspaceController.collapseExpandedChart(state)

        assertNull(result.expandedChartKind)
    }

    @Test
    fun ensureCompatibleState_clearsAnExpandedKindTheInstrumentNoLongerSupports() {
        // A capability change must not strand the operator expanded on a chart
        // that can no longer be produced.
        val state = SweepWorkspaceState(
            currentSweep = valleySweep(),
            expandedChartKind = ChartKind.PHASE
        )

        val result = SweepWorkspaceController.ensureCompatibleState(
            currentState = state,
            availableDisplayModes = listOf(SweepDisplayMode.SWR),
            availableChartKinds = listOf(ChartKind.SWR)
        )

        assertNull(result.expandedChartKind)
    }

    @Test
    fun ensureCompatibleState_keepsAnExpandedKindThatIsStillAvailable() {
        val state = SweepWorkspaceState(
            currentSweep = valleySweep(),
            expandedChartKind = ChartKind.SWR
        )

        val result = SweepWorkspaceController.ensureCompatibleState(
            currentState = state,
            availableDisplayModes = listOf(SweepDisplayMode.SWR),
            availableChartKinds = listOf(ChartKind.SWR, ChartKind.SMITH)
        )

        assertEquals(ChartKind.SWR, result.expandedChartKind)
    }

    @Test
    fun toggleExpandedChart_changesOnlyTheExpandedField() {
        /*
        THE NON-CONFLATION GUARANTEE (spec 2.2: the toggle and tap-to-expand
        "must not be conflated with it in the design OR the code").

        Asserted as "exactly one field moved" rather than by naming fields, so
        it keeps holding when slice 5 adds the layout-mode field — that field
        will be covered here the day it exists, with no edit to this test.
        */
        val before = SweepWorkspaceState(
            currentSweep = valleySweep(),
            displayMode = SweepDisplayMode.RETURN_LOSS,
            traceCompareMode = TraceCompareMode.CURRENT_ONLY,
            markerAIndex = 1,
            markerBIndex = 3,
            activeMarkerTarget = WorkspaceMarkerTarget.B,
            showCsvPreview = true
        )

        val after = SweepWorkspaceController.toggleExpandedChart(before, ChartKind.SMITH)

        assertEquals(ChartKind.SMITH, after.expandedChartKind)
        assertEquals(before.copy(expandedChartKind = after.expandedChartKind), after)
    }

    // ------------------------------------------------------------------
    // App analysis collapse (slice 5f)
    // ------------------------------------------------------------------

    @Test
    fun toggleAppAnalysisExpanded_flipsBothWays() {
        // Collapsed is the default, so the first tap must OPEN it — a toggle
        // that only ever closed would still pass a one-direction test.
        val collapsed = SweepWorkspaceState()
        assertFalse(collapsed.appAnalysisExpanded)

        val expanded = SweepWorkspaceController.toggleAppAnalysisExpanded(collapsed)
        assertTrue(expanded.appAnalysisExpanded)

        val reCollapsed = SweepWorkspaceController.toggleAppAnalysisExpanded(expanded)
        assertFalse(reCollapsed.appAnalysisExpanded)
    }

    @Test
    fun toggleAppAnalysisExpanded_changesOnlyThatField() {
        // Same "exactly one field moved" shape as the tap-to-expand guard
        // above, against a state that dirties the neighbouring session flags —
        // collapsing an analysis panel must not disturb markers or the CSV
        // preview it sits beside.
        val before = SweepWorkspaceState(
            currentSweep = valleySweep(),
            displayMode = SweepDisplayMode.RETURN_LOSS,
            traceCompareMode = TraceCompareMode.CURRENT_ONLY,
            markerAIndex = 1,
            markerBIndex = 3,
            activeMarkerTarget = WorkspaceMarkerTarget.B,
            expandedChartKind = ChartKind.PHASE,
            showCsvPreview = true
        )

        val after = SweepWorkspaceController.toggleAppAnalysisExpanded(before)

        assertTrue(after.appAnalysisExpanded)
        assertEquals(before.copy(appAnalysisExpanded = after.appAnalysisExpanded), after)
    }

    // ------------------------------------------------------------------
    // ensureCompatibleState
    // ------------------------------------------------------------------

    @Test
    fun ensureCompatibleState_clampsMarkers_fallsBackMode_andResetsCompareMode() {
        val state = SweepWorkspaceState(
            currentSweep = valleySweep(),
            referenceSweep = null,
            displayMode = SweepDisplayMode.SMITH,
            traceCompareMode = TraceCompareMode.CURRENT_PLUS_REFERENCE,
            markerAIndex = 99,
            markerBIndex = -5
        )

        val result = SweepWorkspaceController.ensureCompatibleState(
            currentState = state,
            availableDisplayModes = listOf(SweepDisplayMode.SWR, SweepDisplayMode.RETURN_LOSS),
            availableChartKinds = ALL_CHART_KINDS
        )

        assertEquals(SweepDisplayMode.SWR, result.displayMode)
        assertEquals(TraceCompareMode.CURRENT_ONLY, result.traceCompareMode)
        assertEquals(4, result.markerAIndex)
        assertEquals(0, result.markerBIndex)
    }

    @Test
    fun ensureCompatibleState_keepsValidModeAndCompareModeWhenReferencePresent() {
        val sweep = valleySweep()
        val state = SweepWorkspaceState(
            currentSweep = sweep,
            referenceSweep = sweep,
            displayMode = SweepDisplayMode.RETURN_LOSS,
            traceCompareMode = TraceCompareMode.CURRENT_PLUS_REFERENCE,
            markerAIndex = 1,
            markerBIndex = 3
        )

        val result = SweepWorkspaceController.ensureCompatibleState(
            currentState = state,
            availableDisplayModes = listOf(SweepDisplayMode.SWR, SweepDisplayMode.RETURN_LOSS),
            availableChartKinds = ALL_CHART_KINDS
        )

        assertEquals(SweepDisplayMode.RETURN_LOSS, result.displayMode)
        assertEquals(TraceCompareMode.CURRENT_PLUS_REFERENCE, result.traceCompareMode)
        assertEquals(1, result.markerAIndex)
        assertEquals(3, result.markerBIndex)
    }

    // ------------------------------------------------------------------
    // runSweep (drives the real simulated SweepController)
    // ------------------------------------------------------------------

    @Test
    fun runSweep_projectMode_producesSweepHistoryAndPendingEntry() {
        val result = SweepWorkspaceController.runSweep(
            currentState = SweepWorkspaceState(),
            startMHz = 14.0,
            endMHz = 14.4,
            stepMHz = 0.02
        )

        assertNotNull(result.currentSweep)
        assertTrue(result.currentSweep!!.points.isNotEmpty())
        assertEquals(1, result.sweepHistory.size)
        assertEquals(result.currentSweep, result.sweepHistory.first())

        // Markers reset to span the fresh sweep.
        assertEquals(0, result.markerAIndex)
        assertEquals(result.currentSweep!!.points.lastIndex, result.markerBIndex)
        assertEquals(WorkspaceMarkerTarget.A, result.activeMarkerTarget)
        assertFalse(result.showCsvPreview)

        // Project mode: a pending project history entry, no discovery handoff.
        val pending = result.pendingProjectSweepHistoryEntry
        assertNotNull(pending)
        assertEquals(ProjectSweepHistoryMode.PROJECT_TEST, pending!!.mode)
        assertNull(result.pendingDiscoverySnapshot)
        assertFalse(result.showDiscoveryHandoffActions)
        assertNull(result.workflowStatusMessage)
    }

    @Test
    fun runSweep_discoveryMode_producesDiscoverySnapshotHandoff() {
        val result = SweepWorkspaceController.runSweep(
            currentState = SweepWorkspaceState(
                isDiscoveryMode = true,
                discoveryAntennaClassification = AntennaClassification.DIPOLE
            ),
            startMHz = 14.0,
            endMHz = 14.4,
            stepMHz = 0.02
        )

        assertNotNull(result.pendingDiscoverySnapshot)
        assertTrue(result.showDiscoveryHandoffActions)
        assertNotNull(result.workflowStatusMessage)
        assertEquals(
            ProjectSweepHistoryMode.DISCOVERY_APPLIED,
            result.pendingProjectSweepHistoryEntry!!.mode
        )
    }

    @Test
    fun runSweep_trimsHistoryToMaxSize() {
        val existing = List(5) { valleySweep() }
        val result = SweepWorkspaceController.runSweep(
            currentState = SweepWorkspaceState(sweepHistory = existing),
            startMHz = 14.0,
            endMHz = 14.4,
            stepMHz = 0.02,
            maxHistorySize = 3
        )

        assertEquals(3, result.sweepHistory.size)
        // Newest sweep is at the front.
        assertEquals(result.currentSweep, result.sweepHistory.first())
    }

    // ------------------------------------------------------------------
    // buildSweepHistoryEntry — §6/§10c.7 data-integrity pin
    //
    // A real LiteVNA sweep carries the driver's OMITTED (neutral, empty)
    // hardwareProfile. Before the fix, the writer copied that verbatim and
    // the old "SIMULATED" model default meant three genuine bench captures
    // were persisted as Hardware: SIMULATED and survived save/reload. These
    // pin that a REAL_INSTRUMENT sweep persists as REAL, never "SIMULATED".
    // ------------------------------------------------------------------

    /** A LiteVNA-shaped sweep: driver omitted hardwareProfile → neutral default. */
    private fun driverUnnamedSweep() = SweepResult(
        startFrequencyMHz = 144.5,
        endFrequencyMHz = 145.49,
        stepMHz = 0.01,
        points = listOf(point(144.5, swr = 2.0), point(145.0, swr = 1.6))
    )

    @Test
    fun buildSweepHistoryEntry_realInstrumentSweep_persistsAsReal_notSimulated() {
        val entry = SweepWorkspaceController.buildSweepHistoryEntry(
            currentState = SweepWorkspaceState(),
            result = driverUnnamedSweep(),
            liveDataSourceKind = InstrumentDataSourceKind.REAL_INSTRUMENT,
            liveHardwareName = "LiteVNA64 v0.3.3"
        )

        // The exact §6 regression: this MUST NOT read "SIMULATED".
        assertEquals("LiteVNA64 v0.3.3", entry.hardwareName)
        assertFalse(entry.hardwareName.equals("SIMULATED", ignoreCase = true))
    }

    @Test
    fun buildSweepHistoryEntry_realInstrumentSweep_unnamedSession_fallsBackToGenericReal() {
        val entry = SweepWorkspaceController.buildSweepHistoryEntry(
            currentState = SweepWorkspaceState(),
            result = driverUnnamedSweep(),
            liveDataSourceKind = InstrumentDataSourceKind.REAL_INSTRUMENT,
            liveHardwareName = "   "
        )

        assertEquals("Real Instrument", entry.hardwareName)
    }

    @Test
    fun buildSweepHistoryEntry_simulatedSweep_stillPersistsAsSimulated() {
        val entry = SweepWorkspaceController.buildSweepHistoryEntry(
            currentState = SweepWorkspaceState(),
            result = driverUnnamedSweep().copy(hardwareProfile = "SIMULATED"),
            liveDataSourceKind = InstrumentDataSourceKind.SIMULATED,
            liveHardwareName = null
        )

        assertEquals("SIMULATED", entry.hardwareName)
    }

    // ------------------------------------------------------------------
    // Reference trace + compare mode
    // ------------------------------------------------------------------

    @Test
    fun setCurrentAsReference_setsReferenceAndSwitchesCompareMode() {
        val sweep = valleySweep()
        val result = SweepWorkspaceController.setCurrentAsReference(
            SweepWorkspaceState(currentSweep = sweep)
        )

        assertEquals(sweep, result.referenceSweep)
        assertEquals(TraceCompareMode.CURRENT_PLUS_REFERENCE, result.traceCompareMode)
    }

    @Test
    fun setCurrentAsReference_noCurrentSweep_isUnchanged() {
        val state = SweepWorkspaceState(currentSweep = null)
        assertEquals(state, SweepWorkspaceController.setCurrentAsReference(state))
    }

    @Test
    fun setHistorySweepAsReference_validAndInvalidIndex() {
        val sweep = valleySweep()
        val state = SweepWorkspaceState(sweepHistory = listOf(sweep))

        val valid = SweepWorkspaceController.setHistorySweepAsReference(state, 0)
        assertEquals(sweep, valid.referenceSweep)

        val invalid = SweepWorkspaceController.setHistorySweepAsReference(state, 5)
        assertEquals(state, invalid)
    }

    @Test
    fun clearReference_nullsReferenceAndResetsCompareMode() {
        val sweep = valleySweep()
        val state = SweepWorkspaceState(
            referenceSweep = sweep,
            traceCompareMode = TraceCompareMode.CURRENT_PLUS_REFERENCE
        )

        val result = SweepWorkspaceController.clearReference(state)
        assertNull(result.referenceSweep)
        assertEquals(TraceCompareMode.CURRENT_ONLY, result.traceCompareMode)
    }

    @Test
    fun setTraceCompareMode_forcesCurrentOnlyWithoutReference_butHonorsWithReference() {
        val forced = SweepWorkspaceController.setTraceCompareMode(
            SweepWorkspaceState(referenceSweep = null),
            TraceCompareMode.CURRENT_PLUS_REFERENCE
        )
        assertEquals(TraceCompareMode.CURRENT_ONLY, forced.traceCompareMode)

        val honored = SweepWorkspaceController.setTraceCompareMode(
            SweepWorkspaceState(referenceSweep = valleySweep()),
            TraceCompareMode.CURRENT_PLUS_REFERENCE
        )
        assertEquals(TraceCompareMode.CURRENT_PLUS_REFERENCE, honored.traceCompareMode)
    }

    // ------------------------------------------------------------------
    // Marker index setters + active target
    // ------------------------------------------------------------------

    @Test
    fun markerIndexSetters_clampToSweepBounds() {
        val state = SweepWorkspaceState(currentSweep = valleySweep())

        assertEquals(4, SweepWorkspaceController.setMarkerAIndex(state, 99).markerAIndex)
        assertEquals(0, SweepWorkspaceController.setMarkerBIndex(state, -3).markerBIndex)
    }

    @Test
    fun setActiveMarkerTarget_updatesTarget() {
        val result = SweepWorkspaceController.setActiveMarkerTarget(
            SweepWorkspaceState(),
            WorkspaceMarkerTarget.B
        )
        assertEquals(WorkspaceMarkerTarget.B, result.activeMarkerTarget)
    }

    // ------------------------------------------------------------------
    // Marker tool helpers
    // ------------------------------------------------------------------

    @Test
    fun getActiveMarkerIndex_returnsTargetedIndex() {
        assertEquals(2, SweepWorkspaceController.getActiveMarkerIndex(WorkspaceMarkerTarget.A, 2, 7))
        assertEquals(7, SweepWorkspaceController.getActiveMarkerIndex(WorkspaceMarkerTarget.B, 2, 7))
    }

    @Test
    fun applyIndexToTarget_writesClampedIndexToChosenTarget() {
        val sweep = valleySweep()

        val toA = SweepWorkspaceController.applyIndexToTarget(sweep, WorkspaceMarkerTarget.A, 0, 4, 99)
        assertEquals(4, toA.markerAIndex)
        assertEquals(4, toA.markerBIndex)

        val toB = SweepWorkspaceController.applyIndexToTarget(sweep, WorkspaceMarkerTarget.B, 0, 4, -1)
        assertEquals(0, toB.markerAIndex)
        assertEquals(0, toB.markerBIndex)
    }

    @Test
    fun nudgeActiveMarker_movesActiveTargetByDelta() {
        val sweep = valleySweep()
        val update = SweepWorkspaceController.nudgeActiveMarker(sweep, WorkspaceMarkerTarget.A, 1, 4, delta = 2)
        assertEquals(3, update.markerAIndex)
        assertEquals(4, update.markerBIndex)
    }

    @Test
    fun findResonanceIndex_returnsMinSwr_andZeroForEmpty() {
        assertEquals(2, SweepWorkspaceController.findResonanceIndex(valleySweep()))
        assertEquals(0, SweepWorkspaceController.findResonanceIndex(null))
    }

    @Test
    fun findCenterIndex_returnsMidpoint() {
        assertEquals(2, SweepWorkspaceController.findCenterIndex(valleySweep()))
    }

    @Test
    fun findNearestFrequencyIndex_picksClosestPoint() {
        assertEquals(3, SweepWorkspaceController.findNearestFrequencyIndex(valleySweep(), 12.9))
    }

    @Test
    fun moveActiveMarkerToUserFrequency_returnsNullForNullFrequency() {
        val result = SweepWorkspaceController.moveActiveMarkerToUserFrequency(
            sweep = valleySweep(),
            target = WorkspaceMarkerTarget.A,
            markerAIndex = 0,
            markerBIndex = 4,
            frequencyMHz = null
        )
        assertNull(result)
    }

    @Test
    fun findHighestPeakIndex_returnsNullForEmptySweep() {
        assertNull(SweepWorkspaceController.findHighestPeakIndex(null, SweepDisplayMode.SWR))
    }

    @Test
    fun localPeaks_supportNextAndPreviousPeakNavigation() {
        val sweep = valleySweep()
        val peaks = SweepWorkspaceController.findLocalPeakIndices(sweep, SweepDisplayMode.SWR)
        assertEquals(listOf(0, 4), peaks)

        val next = SweepWorkspaceController.moveActiveMarkerToNextPeak(
            sweep, SweepDisplayMode.SWR, WorkspaceMarkerTarget.A, markerAIndex = 0, markerBIndex = 4
        )
        assertEquals(4, next!!.markerAIndex)

        val previous = SweepWorkspaceController.moveActiveMarkerToPreviousPeak(
            sweep, SweepDisplayMode.SWR, WorkspaceMarkerTarget.A, markerAIndex = 4, markerBIndex = 4
        )
        assertEquals(0, previous!!.markerAIndex)
    }

    @Test
    fun placeBandwidthMarkers_bracketsTheValleyBelowThreshold() {
        val update = SweepWorkspaceController.placeBandwidthMarkers(valleySweep(), threshold = 2.0)
        assertNotNull(update)
        assertEquals(1, update!!.markerAIndex)
        assertEquals(3, update.markerBIndex)
    }

    @Test
    fun placeBandwidthMarkers_returnsNullWhenResonanceAboveThreshold() {
        assertNull(SweepWorkspaceController.placeBandwidthMarkers(valleySweep(), threshold = 1.0))
    }

    @Test
    fun placeFullSpanMarkers_spansFirstToLast() {
        val update = SweepWorkspaceController.placeFullSpanMarkers(valleySweep())
        assertEquals(0, update.markerAIndex)
        assertEquals(4, update.markerBIndex)
    }

    // ------------------------------------------------------------------
    // Discovery handoff + tool toggles
    // ------------------------------------------------------------------

    @Test
    fun setDiscoveryAntennaClassification_updatesAndClearsStatus() {
        val result = SweepWorkspaceController.setDiscoveryAntennaClassification(
            SweepWorkspaceState(workflowStatusMessage = "old"),
            AntennaClassification.YAGI
        )
        assertEquals(AntennaClassification.YAGI, result.discoveryAntennaClassification)
        assertNull(result.workflowStatusMessage)
    }

    @Test
    fun completeDiscoveryHandoff_clearsPendingAndSetsStatus() {
        val state = SweepWorkspaceState(
            pendingDiscoverySnapshot = null,
            showDiscoveryHandoffActions = true
        )
        val result = SweepWorkspaceController.completeDiscoveryHandoff(state, "Saved.")
        assertNull(result.pendingDiscoverySnapshot)
        assertNull(result.pendingProjectSweepHistoryEntry)
        assertFalse(result.showDiscoveryHandoffActions)
        assertEquals("Saved.", result.workflowStatusMessage)
    }

    @Test
    fun discardDiscoverySession_resetsWorkspace() {
        val sweep = valleySweep()
        val state = SweepWorkspaceState(
            currentSweep = sweep,
            referenceSweep = sweep,
            sweepHistory = listOf(sweep),
            showDiscoveryHandoffActions = true,
            markerAIndex = 3,
            markerBIndex = 4
        )

        val result = SweepWorkspaceController.discardDiscoverySession(state)
        assertNull(result.currentSweep)
        assertNull(result.referenceSweep)
        assertTrue(result.sweepHistory.isEmpty())
        assertFalse(result.showDiscoveryHandoffActions)
        assertEquals(0, result.markerAIndex)
        assertEquals(0, result.markerBIndex)
        assertEquals(WorkspaceMarkerTarget.A, result.activeMarkerTarget)
    }

    @Test
    fun toggleCsvPreview_flipsFlag() {
        val on = SweepWorkspaceController.toggleCsvPreview(SweepWorkspaceState(showCsvPreview = false))
        assertTrue(on.showCsvPreview)
        val off = SweepWorkspaceController.toggleCsvPreview(on)
        assertFalse(off.showCsvPreview)
    }
}
