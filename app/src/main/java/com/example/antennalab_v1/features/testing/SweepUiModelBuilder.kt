package com.example.antennalab_v1.features.testing

/*
########################################################################
FILE: SweepUiModelBuilder.kt
PACKAGE: com.example.antennalab_v1.features.testing
LAYER: UI / Testing Tools / Workspace UI Model Builder

SYSTEM ROLE
Pure, UI-framework-free decision/formatting logic extracted out of
SweepWorkspaceViewModel.buildUiModel so it can be unit-tested off the
ViewModel (no Compose state, no Context). Everything here is a pure
function of its arguments — no reads of ViewModel state, no side effects.

WHAT LIVES HERE
• run-contract decision engine (buildSweepRunContract)
• operator failure-message classifier (buildOperatorSweepFailureMessage)
• diagnostics UI-model formatting (buildDiagnosticsUiModel)
• sweep-source / path / fallback label formatting
• detached discovery UI-model + summary formatting

WHAT STAYS IN THE VIEWMODEL
Anything with side effects or live-state reads: sweep execution,
resolveActiveFailureMessage (mutates latestSweepFailureMessage and calls
SweepController), instrument-session/status-card construction (Context),
and the overall buildUiModel orchestration that stitches these together.
########################################################################
*/

import com.example.antennalab_v1.domain.testing.SweepDiagnosticsEngine
import com.example.antennalab_v1.model.AntennaClassification
import com.example.antennalab_v1.model.DiscoverySnapshot
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.testing.CalibrationReadiness
import com.example.antennalab_v1.model.testing.InstrumentDataSourceKind
import com.example.antennalab_v1.model.testing.InstrumentSessionState
import com.example.antennalab_v1.model.testing.SweepResult

object SweepUiModelBuilder {

    fun getCurrentSweepSourceLabel(currentSweep: SweepResult?): String {
        val hardwareProfile = currentSweep?.hardwareProfile

        return when {
            currentSweep == null -> "No Sweep Loaded"
            hardwareProfile.isNullOrBlank() -> "Loaded Sweep"
            hardwareProfile.equals("SIMULATED", ignoreCase = true) -> "Simulated Sweep"
            else -> hardwareProfile
        }
    }

    fun buildSelectedSweepPathLabel(
        instrumentSessionState: InstrumentSessionState
    ): String {
        return when (instrumentSessionState.dataSourceKind) {
            InstrumentDataSourceKind.REAL_INSTRUMENT -> "Real Instrument"
            InstrumentDataSourceKind.SIMULATED -> "Simulated Instrument"
            InstrumentDataSourceKind.NONE -> "No Instrument"
        }
    }

    fun buildFallbackReasonText(
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

    fun buildSweepRunContract(
        instrumentSessionState: InstrumentSessionState,
        latestFailureMessage: String?,
        sweepRunInProgress: Boolean
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
            CalibrationReadiness.VALID -> "Valid"
            CalibrationReadiness.IN_PROGRESS -> "In Progress"
            CalibrationReadiness.STALE -> "Stale"
            CalibrationReadiness.INVALID -> "Invalid"
            CalibrationReadiness.NOT_STARTED -> "Not Started"
        }

        val calibrationWarningText = when (calibrationState.readiness) {
            CalibrationReadiness.VALID -> null
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

    fun buildDiagnosticsUiModel(result: SweepResult): SweepDiagnosticsUiModel {
        val diagnostics = SweepDiagnosticsEngine.analyzeSweep(result)

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

    fun buildOperatorSweepFailureMessage(rawFailureMessage: String?): String {
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

    fun buildDiscoveryUiModel(
        state: SweepWorkspaceState,
        project: ProjectData
    ): SweepDiscoveryUiModel {
        if (!state.isDiscoveryMode) {
            return SweepDiscoveryUiModel()
        }

        val pendingSnapshot = state.pendingDiscoverySnapshot

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
            selectedAntennaClassification = state.discoveryAntennaClassification,
            summaryTitle = summaryTitle,
            summarySupportingText = summarySupportingText,
            showHandoffActions = state.showDiscoveryHandoffActions,
            canApplyToCurrentProject =
                state.pendingProjectSweepHistoryEntry != null &&
                        (project.meta.projectId.isNotBlank() || project.meta.projectName.isNotBlank()),
            canSaveAsNewProject = state.pendingProjectSweepHistoryEntry != null,
            canReturnWithoutSaving = state.showDiscoveryHandoffActions,
            canDiscardSession = true,
            actionStatusText = state.workflowStatusMessage
        )
    }

    private fun buildDiscoverySummaryTitle(
        discoverySnapshot: DiscoverySnapshot
    ): String {
        return formatAntennaClassificationLabel(discoverySnapshot.antennaClassificationGuess)
    }

    private fun buildDiscoverySummarySupportingText(
        discoverySnapshot: DiscoverySnapshot
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

    fun formatAntennaClassificationLabel(
        antennaClassification: AntennaClassification
    ): String {
        return antennaClassification.name
            .lowercase()
            .split("_")
            .joinToString(" ") { token -> token.replaceFirstChar { it.uppercase() } }
    }
}
