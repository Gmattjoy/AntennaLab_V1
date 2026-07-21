package com.example.antennalab_v1.features.testing

/*
########################################################################
FILE: SweepWorkspaceUiModel.kt
PACKAGE: com.example.antennalab_v1.features.testing
LAYER: UI / Testing Tools / Workspace UI Model

LAST UPDATED 08/04/2026 00:00

SYSTEM ROLE
Provides UI-facing derived models for the sweep workspace.

CURRENT DEVELOPMENT ROLE
This file now separates the sweep workspace into clearer UI bundles:

• instrument status summary
• operator run contract
• engineering details
• sweep analysis and tuning data
• detached discovery classification / handoff data

IMPORTANT UI FIX
Operator warnings must not be suppressed when there is an actual
fallback/failure reason text available. That failure text is now the
primary way to expose why a sweep was dropped even though transport/read
looked successful.
########################################################################
*/

import com.example.antennalab_v1.domain.analysis.AdjustmentEstimate
import com.example.antennalab_v1.domain.analysis.AntennaBehaviorClassification
import com.example.antennalab_v1.domain.analysis.TuningSuggestionReport
import com.example.antennalab_v1.domain.analysis.TuningWorkflowReport
import com.example.antennalab_v1.features.app.InstrumentStatusCardUiModel
import com.example.antennalab_v1.model.AntennaClassification
import com.example.antennalab_v1.model.testing.InstrumentSessionState
import com.example.antennalab_v1.model.testing.SweepPoint

data class SweepDiagnosticsUiModel(
    val minimumSwrText: String? = null,
    val resonanceText: String? = null,
    val secondaryResonanceText: String? = null,
    val bandwidthText: String? = null,
    val bandwidthAt15Text: String? = null,
    val matchingQualityText: String? = null,
    val impedanceStabilityText: String? = null,
    val sweepShapeText: String? = null,
    val reactanceTrendText: String? = null,
    val mismatchSeverityText: String? = null,
    val likelyConditionText: String? = null,
    val feedlineLossSuspicionText: String? = null,
    val resonanceCountText: String? = null,
    val summaryLines: List<String> = emptyList()
)

data class SweepEngineeringDetailsUiModel(
    val commandStatusLabel: String = "Unknown",
    val lastReadSizeLabel: String = "—",
    val lastErrorLabel: String = "None",
    val transportSummary: String = "No transport summary available.",
    val liteVnaDebugSummary: String = "No LiteVNA debug summary available."
)

data class SweepRunContract(
    val runButtonText: String = "Run Sweep Locked",
    val runEnabled: Boolean = false,
    val runUsesRealInstrument: Boolean = false,
    val runUsesSimulation: Boolean = false,
    val statusText: String = "Sweep is locked.",
    val blockReason: String? = null,
    val calibrationStateLabel: String = "Not Started",
    val calibrationStatusText: String = "",
    val calibrationWarningText: String? = null,
    val trustDowngraded: Boolean = false
)

data class SweepDiscoveryUiModel(
    val isDiscoveryMode: Boolean = false,
    val selectedAntennaClassification: AntennaClassification =
        AntennaClassification.NOT_YET_CLASSIFIED,
    val availableClassifications: List<AntennaClassification> =
        discoveryClassificationOptions(),
    val summaryTitle: String = "No discovery result yet.",
    val summarySupportingText: String =
        "Select the closest visible antenna shape, then run a sweep.",
    val showHandoffActions: Boolean = false,
    val canApplyToCurrentProject: Boolean = false,
    val canSaveAsNewProject: Boolean = false,
    val canReturnWithoutSaving: Boolean = false,
    val canDiscardSession: Boolean = false,
    val actionStatusText: String? = null
)

data class SweepWorkspaceUiModel(
    val resonanceMHz: Double? = null,
    val diagnostics: SweepDiagnosticsUiModel? = null,
    val behaviorClassification: AntennaBehaviorClassification? = null,
    val tuningSuggestionReport: TuningSuggestionReport? = null,
    val adjustmentEstimate: AdjustmentEstimate? = null,
    val tuningWorkflowReport: TuningWorkflowReport? = null,
    val markerAPoint: SweepPoint? = null,
    val markerBPoint: SweepPoint? = null,
    val currentSweepSourceLabel: String = "No Sweep Loaded",
    val selectedSweepPathLabel: String = "Simulated Sweep",
    val fallbackReasonText: String? = null,
    val instrumentSessionState: InstrumentSessionState? = null,
    val instrumentStatusCard: InstrumentStatusCardUiModel = InstrumentStatusCardUiModel(),
    val engineeringDetails: SweepEngineeringDetailsUiModel = SweepEngineeringDetailsUiModel(),
    val sweepRunContract: SweepRunContract = SweepRunContract(),
    val discoveryUi: SweepDiscoveryUiModel = SweepDiscoveryUiModel()
) {
    val hasCurrentLoadedSweep: Boolean
        get() = currentSweepSourceLabel != "No Sweep Loaded"

    val shouldSuppressStaleOperatorWarning: Boolean
        get() = !hasCurrentLoadedSweep &&
                fallbackReasonText.isNullOrBlank() &&
                engineeringDetails.commandStatusLabel.equals("OK", ignoreCase = true) &&
                engineeringDetails.lastErrorLabel.equals("None", ignoreCase = true)
}

fun discoveryClassificationOptions(): List<AntennaClassification> {
    return listOf(
        AntennaClassification.NOT_YET_CLASSIFIED,
        AntennaClassification.DIPOLE,
        AntennaClassification.MONOPOLE,
        AntennaClassification.VERTICAL,
        AntennaClassification.LOOP,
        AntennaClassification.YAGI,
        AntennaClassification.GROUND_PLANE,
        AntennaClassification.LONG_WIRE,
        AntennaClassification.OTHER
    )
}
