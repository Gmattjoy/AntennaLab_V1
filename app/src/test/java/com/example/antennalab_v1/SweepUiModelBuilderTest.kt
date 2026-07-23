package com.example.antennalab_v1

import com.example.antennalab_v1.features.testing.SweepUiModelBuilder
import com.example.antennalab_v1.model.AntennaClassification
import com.example.antennalab_v1.model.DiscoverySnapshot
import com.example.antennalab_v1.model.HardwareCapabilityProfiles
import com.example.antennalab_v1.model.HardwareConnectionState
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.ProjectSweepHistoryEntry
import com.example.antennalab_v1.model.UsbConnectionInfo
import com.example.antennalab_v1.model.testing.CalibrationReadiness
import com.example.antennalab_v1.model.testing.InstrumentCalibrationState
import com.example.antennalab_v1.model.testing.InstrumentDataSourceKind
import com.example.antennalab_v1.model.testing.InstrumentSessionState
import com.example.antennalab_v1.model.testing.MeasurementTrustLevel
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Coverage for [SweepUiModelBuilder] — the pure run-contract decision engine,
 * failure-message classifier, and label/discovery formatting extracted out of
 * SweepWorkspaceViewModel.buildUiModel. Everything here is a pure function of
 * its arguments, so no Context / Compose state is involved.
 */
class SweepUiModelBuilderTest {

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private fun session(
        dataSourceKind: InstrumentDataSourceKind = InstrumentDataSourceKind.NONE,
        connectionState: HardwareConnectionState = HardwareConnectionState.NOT_CONNECTED,
        sessionOpen: Boolean = false,
        transportReady: Boolean = false,
        supportTier: String = "Unknown",
        calibrationState: InstrumentCalibrationState = InstrumentCalibrationState(),
        sessionSummary: String = "session summary"
    ) = InstrumentSessionState(
        selectedHardwareName = "LiteVNA64 v0.3.3",
        connectionInfo = UsbConnectionInfo(
            state = connectionState,
            sessionOpen = sessionOpen
        ),
        transportReady = transportReady,
        transportStatusSummary = "transport summary",
        protocolFamily = "TestFamily",
        supportTier = supportTier,
        capabilityProfile = HardwareCapabilityProfiles.VNA_STANDARD,
        measurementTrust = MeasurementTrustLevel.UNKNOWN,
        calibrationState = calibrationState,
        dataSourceKind = dataSourceKind,
        sessionSummary = sessionSummary
    )

    private fun liveReadySession(
        calibrationState: InstrumentCalibrationState = InstrumentCalibrationState()
    ) = session(
        dataSourceKind = InstrumentDataSourceKind.REAL_INSTRUMENT,
        connectionState = HardwareConnectionState.READY,
        sessionOpen = true,
        transportReady = true,
        supportTier = "Full Support",
        calibrationState = calibrationState
    )

    private fun sweep(hardwareProfile: String) = SweepResult(
        startFrequencyMHz = 140.0,
        endFrequencyMHz = 150.0,
        stepMHz = 1.0,
        points = emptyList<SweepPoint>(),
        hardwareProfile = hardwareProfile
    )

    // ------------------------------------------------------------------
    // getCurrentSweepSourceLabel
    // ------------------------------------------------------------------

    @Test
    fun currentSweepSourceLabel_coversAllBranches() {
        assertEquals("No Sweep Loaded", SweepUiModelBuilder.getCurrentSweepSourceLabel(null))
        assertEquals("Loaded Sweep", SweepUiModelBuilder.getCurrentSweepSourceLabel(sweep("")))
        assertEquals("Simulated Sweep", SweepUiModelBuilder.getCurrentSweepSourceLabel(sweep("simulated")))
        assertEquals("NanoVNA-H4", SweepUiModelBuilder.getCurrentSweepSourceLabel(sweep("NanoVNA-H4")))
    }

    // ------------------------------------------------------------------
    // buildSelectedSweepPathLabel
    // ------------------------------------------------------------------

    @Test
    fun selectedSweepPathLabel_mapsDataSourceKind() {
        assertEquals(
            "Real Instrument",
            SweepUiModelBuilder.buildSelectedSweepPathLabel(session(dataSourceKind = InstrumentDataSourceKind.REAL_INSTRUMENT))
        )
        assertEquals(
            "Simulated Instrument",
            SweepUiModelBuilder.buildSelectedSweepPathLabel(session(dataSourceKind = InstrumentDataSourceKind.SIMULATED))
        )
        assertEquals(
            "No Instrument",
            SweepUiModelBuilder.buildSelectedSweepPathLabel(session(dataSourceKind = InstrumentDataSourceKind.NONE))
        )
    }

    // ------------------------------------------------------------------
    // buildFallbackReasonText
    // ------------------------------------------------------------------

    @Test
    fun fallbackReasonText_prioritisesFailureMessage() {
        assertEquals(
            "Latest sweep failure: boom",
            SweepUiModelBuilder.buildFallbackReasonText(
                currentSweep = sweep("NanoVNA-H4"),
                instrumentSessionState = session(),
                latestFailureMessage = "boom"
            )
        )
    }

    @Test
    fun fallbackReasonText_nullWhenRealSweepAndNoFailure() {
        assertNull(
            SweepUiModelBuilder.buildFallbackReasonText(
                currentSweep = sweep("NanoVNA-H4"),
                instrumentSessionState = session(dataSourceKind = InstrumentDataSourceKind.REAL_INSTRUMENT),
                latestFailureMessage = null
            )
        )
    }

    @Test
    fun fallbackReasonText_flagsSimulatedLoadedSweep() {
        assertEquals(
            "Current loaded sweep is simulated. session summary",
            SweepUiModelBuilder.buildFallbackReasonText(
                currentSweep = sweep("SIMULATED"),
                instrumentSessionState = session(),
                latestFailureMessage = null
            )
        )
    }

    @Test
    fun fallbackReasonText_flagsSimulatedSession() {
        // Reached only when a real-named sweep is loaded but the live session
        // has fallen back to simulated (the null-profile branch returns earlier).
        assertEquals(
            "Session is currently operating in simulated mode. session summary",
            SweepUiModelBuilder.buildFallbackReasonText(
                currentSweep = sweep("NanoVNA-H4"),
                instrumentSessionState = session(dataSourceKind = InstrumentDataSourceKind.SIMULATED),
                latestFailureMessage = null
            )
        )
    }

    // ------------------------------------------------------------------
    // buildSweepRunContract — run-contract decision engine
    // ------------------------------------------------------------------

    @Test
    fun runContract_sweepRunningWinsOverEverything() {
        val contract = SweepUiModelBuilder.buildSweepRunContract(
            instrumentSessionState = liveReadySession(),
            latestFailureMessage = null,
            sweepRunInProgress = true
        )
        assertEquals("Sweep Running...", contract.runButtonText)
        assertFalse(contract.runEnabled)
        assertEquals("Sweep is currently running.", contract.blockReason)
        assertTrue(contract.statusText.contains("background worker"))
    }

    @Test
    fun runContract_liveAllowedWhenRealTransportReadyAndSupported() {
        val contract = SweepUiModelBuilder.buildSweepRunContract(
            instrumentSessionState = liveReadySession(),
            latestFailureMessage = null,
            sweepRunInProgress = false
        )
        assertEquals("Run Live Sweep", contract.runButtonText)
        assertTrue(contract.runEnabled)
        assertTrue(contract.runUsesRealInstrument)
        assertFalse(contract.runUsesSimulation)
        assertNull(contract.blockReason)
    }

    @Test
    fun runContract_partialSupportStillCountsAsLive() {
        val contract = SweepUiModelBuilder.buildSweepRunContract(
            instrumentSessionState = session(
                dataSourceKind = InstrumentDataSourceKind.REAL_INSTRUMENT,
                connectionState = HardwareConnectionState.READY,
                sessionOpen = true,
                transportReady = true,
                supportTier = "Partial Support"
            ),
            latestFailureMessage = null,
            sweepRunInProgress = false
        )
        assertTrue(contract.runUsesRealInstrument)
        assertNull(contract.blockReason)
    }

    @Test
    fun runContract_demoAllowedWhenSimulated() {
        // A not-connected simulated session still enables the demo path, but the
        // block reason reflects the earliest failing connection check.
        val notConnected = SweepUiModelBuilder.buildSweepRunContract(
            instrumentSessionState = session(dataSourceKind = InstrumentDataSourceKind.SIMULATED),
            latestFailureMessage = null,
            sweepRunInProgress = false
        )
        assertEquals("Run Demo Sweep", notConnected.runButtonText)
        assertTrue(notConnected.runEnabled)
        assertTrue(notConnected.runUsesSimulation)
        assertFalse(notConnected.runUsesRealInstrument)
        assertEquals("No instrument is connected.", notConnected.blockReason)

        // When the connection checks all pass, the demo-specific block reason shows.
        val transportReady = SweepUiModelBuilder.buildSweepRunContract(
            instrumentSessionState = session(
                dataSourceKind = InstrumentDataSourceKind.SIMULATED,
                connectionState = HardwareConnectionState.READY,
                sessionOpen = true,
                transportReady = true
            ),
            latestFailureMessage = null,
            sweepRunInProgress = false
        )
        assertTrue(transportReady.runUsesSimulation)
        assertEquals(
            "Real transport is not ready. Demo sweep is available instead.",
            transportReady.blockReason
        )
    }

    @Test
    fun runContract_failureMessageSurfacesAsBlockReason() {
        val contract = SweepUiModelBuilder.buildSweepRunContract(
            instrumentSessionState = session(
                dataSourceKind = InstrumentDataSourceKind.REAL_INSTRUMENT,
                connectionState = HardwareConnectionState.READY,
                sessionOpen = true,
                transportReady = false,
                supportTier = "Full Support"
            ),
            latestFailureMessage = "transport hiccup",
            sweepRunInProgress = false
        )
        assertEquals("Run Sweep Locked", contract.runButtonText)
        assertFalse(contract.runEnabled)
        assertEquals("transport hiccup", contract.blockReason)
    }

    @Test
    fun runContract_notConnectedBlockReason() {
        val contract = SweepUiModelBuilder.buildSweepRunContract(
            instrumentSessionState = session(connectionState = HardwareConnectionState.NOT_CONNECTED),
            latestFailureMessage = null,
            sweepRunInProgress = false
        )
        assertEquals("No instrument is connected.", contract.blockReason)
    }

    @Test
    fun runContract_permissionRequiredBlockReason() {
        val contract = SweepUiModelBuilder.buildSweepRunContract(
            instrumentSessionState = session(connectionState = HardwareConnectionState.PERMISSION_REQUIRED),
            latestFailureMessage = null,
            sweepRunInProgress = false
        )
        assertEquals("USB permission is required before live sweep can run.", contract.blockReason)
    }

    @Test
    fun runContract_unsupportedRealInstrumentBlockReason() {
        val contract = SweepUiModelBuilder.buildSweepRunContract(
            instrumentSessionState = session(
                dataSourceKind = InstrumentDataSourceKind.REAL_INSTRUMENT,
                connectionState = HardwareConnectionState.READY,
                sessionOpen = true,
                transportReady = true,
                supportTier = "Unknown"
            ),
            latestFailureMessage = null,
            sweepRunInProgress = false
        )
        assertEquals(
            "Detected instrument is not yet at a supported tier for live sweep.",
            contract.blockReason
        )
    }

    @Test
    fun runContract_calibrationLabelsAndWarning() {
        val stale = SweepUiModelBuilder.buildSweepRunContract(
            instrumentSessionState = liveReadySession(
                calibrationState = InstrumentCalibrationState(
                    readiness = CalibrationReadiness.STALE,
                    operatorWarning = "cal is stale",
                    trustDowngraded = true
                )
            ),
            latestFailureMessage = null,
            sweepRunInProgress = false
        )
        assertEquals("Stale", stale.calibrationStateLabel)
        assertEquals("cal is stale", stale.calibrationWarningText)
        assertTrue(stale.trustDowngraded)
        assertTrue(stale.statusText.contains("calibration trust is downgraded"))

        val valid = SweepUiModelBuilder.buildSweepRunContract(
            instrumentSessionState = liveReadySession(
                calibrationState = InstrumentCalibrationState(
                    readiness = CalibrationReadiness.VALID,
                    operatorWarning = "should be hidden",
                    trustDowngraded = false
                )
            ),
            latestFailureMessage = null,
            sweepRunInProgress = false
        )
        assertEquals("Valid", valid.calibrationStateLabel)
        assertNull(valid.calibrationWarningText)
        assertFalse(valid.trustDowngraded)
        assertTrue(valid.statusText.contains("calibration valid for this session"))
    }

    // ------------------------------------------------------------------
    // buildOperatorSweepFailureMessage — classifier
    // ------------------------------------------------------------------

    @Test
    fun operatorFailureMessage_blankBecomesUnknownReason() {
        assertEquals(
            "Sweep failed for an unknown reason. Check transport health and try again.",
            SweepUiModelBuilder.buildOperatorSweepFailureMessage(null)
        )
        assertEquals(
            "Sweep failed for an unknown reason. Check transport health and try again.",
            SweepUiModelBuilder.buildOperatorSweepFailureMessage("   ")
        )
    }

    @Test
    fun operatorFailureMessage_classifiesKnownKeywords() {
        assertTrue(
            SweepUiModelBuilder.buildOperatorSweepFailureMessage("Transport is not ready right now")
                .startsWith("Transport is not ready. Return to System")
        )
        assertTrue(
            SweepUiModelBuilder.buildOperatorSweepFailureMessage("data looked TRUNCATED")
                .startsWith("Sweep data looked truncated.")
        )
        assertTrue(
            SweepUiModelBuilder.buildOperatorSweepFailureMessage("too few usable sweep points returned")
                .startsWith("Live sweep returned too few usable points")
        )
    }

    @Test
    fun operatorFailureMessage_unknownTextPassesThrough() {
        assertEquals(
            "some novel failure",
            SweepUiModelBuilder.buildOperatorSweepFailureMessage("some novel failure")
        )
    }

    // ------------------------------------------------------------------
    // formatAntennaClassificationLabel
    // ------------------------------------------------------------------

    @Test
    fun classificationLabel_titleCasesUnderscoredEnum() {
        assertEquals(
            "Ground Plane",
            SweepUiModelBuilder.formatAntennaClassificationLabel(AntennaClassification.GROUND_PLANE)
        )
        assertEquals(
            "Dipole",
            SweepUiModelBuilder.formatAntennaClassificationLabel(AntennaClassification.DIPOLE)
        )
    }

    // ------------------------------------------------------------------
    // buildDiscoveryUiModel
    // ------------------------------------------------------------------

    @Test
    fun discoveryUiModel_defaultWhenNotDiscoveryMode() {
        val model = SweepUiModelBuilder.buildDiscoveryUiModel(
            state = com.example.antennalab_v1.features.testing.SweepWorkspaceState(isDiscoveryMode = false),
            project = ProjectData()
        )
        assertFalse(model.isDiscoveryMode)
        assertEquals("No discovery result yet.", model.summaryTitle)
    }

    @Test
    fun discoveryUiModel_noSnapshotYet() {
        val model = SweepUiModelBuilder.buildDiscoveryUiModel(
            state = com.example.antennalab_v1.features.testing.SweepWorkspaceState(
                isDiscoveryMode = true,
                discoveryAntennaClassification = AntennaClassification.YAGI
            ),
            project = ProjectData()
        )
        assertTrue(model.isDiscoveryMode)
        assertEquals("No discovery result yet.", model.summaryTitle)
        assertEquals(AntennaClassification.YAGI, model.selectedAntennaClassification)
        assertFalse(model.canApplyToCurrentProject)
        assertFalse(model.canSaveAsNewProject)
        assertTrue(model.canDiscardSession)
    }

    @Test
    fun discoveryUiModel_withSnapshotAndPendingEntry() {
        val snapshot = DiscoverySnapshot(
            antennaClassificationGuess = AntennaClassification.LOOP,
            sweepStartMHz = 140.0,
            sweepEndMHz = 150.0,
            bestFrequencyMHz = 145.0,
            bestSwr = 1.25,
            returnLossDb = -18.5
        )
        val model = SweepUiModelBuilder.buildDiscoveryUiModel(
            state = com.example.antennalab_v1.features.testing.SweepWorkspaceState(
                isDiscoveryMode = true,
                pendingDiscoverySnapshot = snapshot,
                pendingProjectSweepHistoryEntry = ProjectSweepHistoryEntry(),
                showDiscoveryHandoffActions = true,
                workflowStatusMessage = "ready"
            ),
            project = ProjectData()
        )
        assertEquals("Loop", model.summaryTitle)
        assertTrue(model.summarySupportingText.contains("Best SWR 1.25 at 145.000 MHz"))
        assertTrue(model.summarySupportingText.contains("RL -18.50 dB"))
        assertTrue(model.canApplyToCurrentProject)
        assertTrue(model.canSaveAsNewProject)
        assertTrue(model.canReturnWithoutSaving)
        assertEquals("ready", model.actionStatusText)
    }
}
