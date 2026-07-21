package com.example.antennalab_v1.model

import com.example.antennalab_v1.model.testing.CalibrationSession

/*
########################################################################
FILE: ProjectData.kt
PACKAGE: com.example.antennalab_v1.model
LAYER: Core Data Model / Project State

FILE ROLE
ProjectData is the master data structure for a single antenna project.

It acts as the single source of truth for the entire application and
connects all major systems:

Wizard → Calculation Engine → Project Workspace → Testing → Storage.

Every system reads from and writes to this model.

ARCHITECTURE POSITION

UI Layer
 ├ Wizard Screens
 ├ ProjectPageScreen
 └ Workspace Screens
        │
        ▼
ProjectData (MASTER MODEL)
        │
        ▼
Calculation Engine
 ├ CalculationRequest
 ├ CalculationEngine
 ├ Calculators
 └ CalculationMappers
        │
        ▼
CalculatedDesign
        │
        ▼
UI Rendering + Testing Tools

EDITING GUIDE
Each section below is clearly marked.

SAFE
✔ Add new calculated design fields
✔ Extend material configuration
✔ Extend test data structure
✔ Add new enums
✔ Add new helper accessors for safe reading

CAUTION
⚠ Changes here affect ALL systems

DO NOT
✖ Place UI logic in this file
✖ Place antenna calculation logic in this file
✖ Access Android framework classes here

This file must remain pure data model only.
########################################################################
*/

/*
########################################################################
MAIN PROJECT DATA STRUCTURE
------------------------------------------------------------------------
PURPOSE
Holds the entire project state in one model.

SAFE EDIT AREA
- add new top-level project sections
- add new helper read-only properties/functions
########################################################################
*/
data class ProjectData(
    val meta: ProjectMeta = ProjectMeta(),
    val antennaClassification: AntennaClassification =
        AntennaClassification.NOT_YET_CLASSIFIED,
    val designInput: DesignInput = DesignInput(),
    val materialConfig: MaterialConfig = MaterialConfig(),
    val calculatedDesign: CalculatedDesign = CalculatedDesign(),
    val testData: TestData = TestData(),
    val discoverySnapshot: DiscoverySnapshot? = null,
    val sweepHistory: List<ProjectSweepHistoryEntry> = emptyList(),
    val calibrationData: ProjectCalibrationData = ProjectCalibrationData(),
    val uiState: ProjectUiState = ProjectUiState(),
    val versionInfo: VersionInfo = VersionInfo(),
    val buildCostProfile: BuildCostProfile = BuildCostProfile.STANDARD,
    val availablePartsProfile: AvailablePartsProfile =
        AvailablePartsProfile.SOME_EXISTING_PARTS,
    val testHardwareProfile: TestHardwareProfile =
        TestHardwareProfile.NANOVNA_H4
) {

    /*
    ####################################################################
    HARDWARE CAPABILITY ACCESS HELPERS
    --------------------------------------------------------------------
    PURPOSE
    Provides safe access points for resolving the currently selected
    test hardware into structured capability models.

    SAFE EDIT AREA
    - add more read-only helper properties
    - keep mapping logic consistent with TestHardwareProfile values
    ####################################################################
    */

    val hardwareCapabilityProfile: HardwareCapabilityProfile
        get() = testHardwareProfile.toHardwareCapabilityProfile()

    val hardwareMeasurementCapabilities: HardwareMeasurementCapabilities
        get() = testHardwareProfile.toHardwareMeasurementCapabilities()

    val hardwareFrequencyMinHzOrDefault: Long
        get() = hardwareCapabilityProfile.minFrequencyHz

    val hardwareFrequencyMaxHzOrDefault: Long
        get() = hardwareCapabilityProfile.maxFrequencyHz

    val supportsSmithChartOrDefault: Boolean
        get() = hardwareCapabilityProfile.supportsSmithChart

    val supportsPhaseOrDefault: Boolean
        get() = hardwareCapabilityProfile.supportsPhase

    val supportsS21OrDefault: Boolean
        get() = hardwareCapabilityProfile.supportsS21

    val supportsTdrPreviewOrDefault: Boolean
        get() = hardwareCapabilityProfile.supportsTdrPreview

    /*
    ####################################################################
    CALIBRATION ACCESS HELPERS
    --------------------------------------------------------------------
    PURPOSE
    Provides safe access points for project-scoped stored calibration
    without forcing callers to manually null-check calibration storage.
    ####################################################################
    */

    val hasStoredCalibration: Boolean
        get() = calibrationData.storedCalibrationSession != null

    val storedCalibrationOrNull: CalibrationSession?
        get() = calibrationData.storedCalibrationSession

    val storedCalibrationMatchesSelectedHardware: Boolean
        get() = calibrationData.storedCalibrationSession
            ?.matchesHardwareDisplayName(hardwareCapabilityProfile.displayName)
            ?: false


    /*
    ####################################################################
    DISCOVERY / HISTORY ACCESS HELPERS
    --------------------------------------------------------------------
    PURPOSE
    Provides safe access points for detached discovery results and
    persisted project sweep history summaries.
    ####################################################################
    */

    val hasDiscoverySnapshot: Boolean
        get() = discoverySnapshot != null

    val latestDiscoverySnapshotOrNull: DiscoverySnapshot?
        get() = discoverySnapshot

    val hasSweepHistory: Boolean
        get() = sweepHistory.isNotEmpty()

    val latestSweepHistoryEntryOrNull: ProjectSweepHistoryEntry?
        get() = sweepHistory.maxByOrNull { it.recordedAtEpochMs }

    /*
    ####################################################################
    LAB ENTRY HELPERS
    --------------------------------------------------------------------
    PURPOSE
    Provides a safe way for UI and workflow logic to understand whether
    the current project is a normal saved project, a temporary unknown
    discovery workspace, or a project-template test launch.
    ####################################################################
    */

    val isUnknownDiscoverySession: Boolean
        get() = meta.labEntryMode == LabEntryMode.UNKNOWN_DISCOVERY

    val isProjectTemplateTestSession: Boolean
        get() = meta.labEntryMode == LabEntryMode.PROJECT_TEMPLATE_TEST
}

/*
########################################################################
PROJECT META DATA
------------------------------------------------------------------------
PURPOSE
Stores project identity, lifecycle state, notes, and temporary LAB
entry context.

SAFE EDIT AREA
- add new metadata fields
- extend project status handling
########################################################################
*/
data class ProjectMeta(
    val projectId: String = "",
    val projectName: String = "Default",
    val projectStatus: ProjectStatus = ProjectStatus.DRAFT,
    val createdAtEpochMs: Long = 0,
    val updatedAtEpochMs: Long = 0,
    val notes: String = "",
    val labEntryMode: LabEntryMode = LabEntryMode.NONE,
    val labSourceProjectName: String = "",
    val labTemplateDisplayName: String = "",
    val labTemplateBandLabel: String = ""
)

/*
########################################################################
DESIGN INPUT MODEL
------------------------------------------------------------------------
PURPOSE
Stores the design choices that feed the calculation engine.

SAFE EDIT AREA
- add new design input fields
- extend environment or priority selection
########################################################################
*/
data class DesignInput(
    val antennaType: AntennaType = AntennaType.DIPOLE,
    val frequencyMode: FrequencyMode = FrequencyMode.SINGLE_FREQUENCY,
    val targetFrequencyMHz: Double = 0.0,
    val frequencyStartMHz: Double = 0.0,
    val frequencyEndMHz: Double = 0.0,
    val environmentType: EnvironmentType = EnvironmentType.UNKNOWN,
    val priorityMode: PriorityMode = PriorityMode.BALANCED
)

/*
########################################################################
MATERIAL CONFIGURATION MODEL
------------------------------------------------------------------------
PURPOSE
Stores conductor, boom, support, connector, and feedline settings.

SAFE EDIT AREA
- add new material options
- add new construction settings
########################################################################
*/
data class MaterialConfig(
    val conductorMaterial: ConductorMaterial = ConductorMaterial.COPPER,
    val conductorDiameterMm: Double = 1.0,
    val boomMaterial: BoomMaterial = BoomMaterial.ALUMINIUM,
    val supportMaterial: SupportMaterial = SupportMaterial.NONE,
    val connectorType: ConnectorType = ConnectorType.SMA,
    val feedlineType: FeedlineType = FeedlineType.COAX_50_OHM,
    val buildNotes: String = ""
)

/*
########################################################################
CALCULATED DESIGN MODEL
------------------------------------------------------------------------
PURPOSE
Stores the calculated antenna design produced by the calculation engine.

This includes:
- legacy numeric fields
- structured design fields
- predicted environmental performance

SAFE EDIT AREA
- add new structured design fields
- add new safe helper accessors
- add future diagnostics or tuning-related outputs
########################################################################
*/
data class CalculatedDesign(
    val elementLengthsMm: List<Double> = emptyList(),
    val elementSpacingMm: List<Double> = emptyList(),
    val boomLengthMm: Double = 0.0,
    val feedPointGapMm: Double = 0.0,
    val matchingMethod: String = "",
    val estimatedGainDbI: Double = 0.0,
    val estimatedFrontToBackDb: Double = 0.0,
    val estimatedBandwidthMHz: Double = 0.0,
    val designWarnings: List<String> = emptyList(),
    val elements: List<CalculatedElement> = emptyList(),
    val spacings: List<CalculatedSpacing> = emptyList(),
    val feedRecommendation: FeedRecommendation = FeedRecommendation(),
    val buildGuidance: BuildGuidance = BuildGuidance(),
    val designExplanation: DesignExplanation = DesignExplanation(),
    val predictedPerformance: PredictedPerformance? = null
) {

    /*
    ####################################################################
    SAFE PREDICTION ACCESS HELPERS
    --------------------------------------------------------------------
    PURPOSE
    Provide null-safe access points for UI, testing tools, and future
    dashboard systems.

    CURRENT BEHAVIOUR
    - returns fallback values when prediction is not available yet
    - avoids repeated null checks across the project

    SAFE EDIT AREA
    - add more read-only helper properties
    - adjust fallback values if the project needs different defaults
    ####################################################################
    */

    val hasPredictedPerformance: Boolean
        get() = predictedPerformance != null

    val predictedSWROrNull: Double?
        get() = predictedPerformance?.predictedSWR

    val predictedSWROrDefault: Double
        get() = predictedPerformance?.predictedSWR ?: 0.0

    val predictedResonanceShiftHzOrDefault: Double
        get() = predictedPerformance?.resonanceShiftHz ?: 0.0

    val predictedEfficiencyOrDefault: Double
        get() = predictedPerformance?.efficiencyEstimate ?: 0.0

    val predictedEnvironmentSummaryOrDefault: String
        get() = predictedPerformance?.environmentSummary ?: "Prediction not available"

    val predictedWarningsOrEmpty: List<String>
        get() = predictedPerformance?.warnings ?: emptyList()
}

/*
########################################################################
CALCULATED ELEMENT MODEL
------------------------------------------------------------------------
PURPOSE
Stores one calculated antenna element.

SAFE EDIT AREA
- add new descriptive element fields
########################################################################
*/
data class CalculatedElement(
    val role: ElementRole = ElementRole.OTHER,
    val label: String = "",
    val lengthMm: Double = 0.0,
    val notes: String = ""
)

/*
########################################################################
CALCULATED SPACING MODEL
------------------------------------------------------------------------
PURPOSE
Stores one calculated spacing relationship.

SAFE EDIT AREA
- add new spacing metadata fields
########################################################################
*/
data class CalculatedSpacing(
    val label: String = "",
    val distanceMm: Double = 0.0,
    val notes: String = ""
)

/*
########################################################################
FEED RECOMMENDATION MODEL
------------------------------------------------------------------------
PURPOSE
Stores feed and matching advice.

SAFE EDIT AREA
- add new matching-related fields
########################################################################
*/
data class FeedRecommendation(
    val feedMethod: String = "",
    val balunRecommendation: String = "",
    val matchingNotes: String = ""
)

/*
########################################################################
BUILD GUIDANCE MODEL
------------------------------------------------------------------------
PURPOSE
Stores practical construction and tuning guidance.

SAFE EDIT AREA
- add new build difficulty and mounting fields
########################################################################
*/
data class BuildGuidance(
    val sizeClass: SizeClass = SizeClass.UNKNOWN,
    val buildDifficulty: BuildDifficulty = BuildDifficulty.UNKNOWN,
    val tuningSensitivity: TuningSensitivity = TuningSensitivity.UNKNOWN,
    val recommendedRadialCount: Int = 0,
    val supportNotes: String = "",
    val mountingNotes: String = ""
)

/*
########################################################################
DESIGN EXPLANATION MODEL
------------------------------------------------------------------------
PURPOSE
Stores explanation text for the calculated design.

SAFE EDIT AREA
- add new explanatory text fields
########################################################################
*/
data class DesignExplanation(
    val designSummary: String = "",
    val intendedUse: String = "",
    val noviceGuidance: String = "",
    val tuningAdvice: String = ""
)

/*
########################################################################
TEST DATA MODEL
------------------------------------------------------------------------
PURPOSE
Stores measured and testing-related project data.

SAFE EDIT AREA
- add new measurement fields
- add sweep history or test session fields later
########################################################################
*/
data class TestData(
    val hasMeasuredData: Boolean = false,
    val lastSweepDateEpochMs: Long = 0,
    val sweepStartMHz: Double = 0.0,
    val sweepEndMHz: Double = 0.0,
    val resonantFrequencyMHz: Double = 0.0,
    val minimumSwr: Double = 0.0,
    val returnLossDb: Double = 0.0,
    val measurementNotes: String = "",
    val trimHistory: List<String> = emptyList()
)

/*
########################################################################
PROJECT CALIBRATION DATA MODEL
------------------------------------------------------------------------
PURPOSE
Stores project-scoped calibration persistence data.

ARCHITECTURE NOTE
This does NOT mean stored calibration is automatically trusted as live
session calibration.

Stored calibration is a project-level persistence record that may later
be restored as:
- compatible
- stale
- incompatible

SAFE EDIT AREA
- add richer restore metadata later
- add calibration file references later
- add per-instrument compatibility tracking later
########################################################################
*/
data class ProjectCalibrationData(
    val storedCalibrationSession: CalibrationSession? = null,
    val lastCalibrationSavedEpochMs: Long = 0L,
    val lastCalibrationStatusSummary: String = "",
    val restorePolicy: CalibrationRestorePolicy =
        CalibrationRestorePolicy.RESTORE_AS_STALE,
    val restoredFromStorage: Boolean = false
) {
    val hasStoredCalibration: Boolean
        get() = storedCalibrationSession != null

    val storedCalibrationCompletionStateName: String
        get() = storedCalibrationSession?.completionState?.name ?: "NOT_STARTED"
}

/*
########################################################################
UI STATE MODEL
------------------------------------------------------------------------
PURPOSE
Stores lightweight project UI state.

SAFE EDIT AREA
- add new remember-last-view style fields
########################################################################
*/
data class ProjectUiState(
    val lastOpenedSection: ProjectSection = ProjectSection.OVERVIEW,
    val lastExpandedCard: ProjectCard = ProjectCard.SUMMARY,
    val hasSeenProjectIntro: Boolean = false
)

/*
########################################################################
VERSION INFO MODEL
------------------------------------------------------------------------
PURPOSE
Stores schema and source versioning details.

SAFE EDIT AREA
- add future migration or compatibility fields
########################################################################
*/
data class VersionInfo(
    val dataSchemaVersion: Int = 1,
    val appDataSource: ProjectSource = ProjectSource.WIZARD_CREATED
)

/*
########################################################################
HARDWARE CAPABILITY PROFILE MODEL
------------------------------------------------------------------------
PURPOSE
Stores the resolved feature set for a selected test hardware profile.

This is intentionally a pure data model so UI, testing tools, storage,
and future hardware discovery systems can all read the same structure.

SAFE EDIT AREA
- add new capability flags
- add limit fields
- extend notes or classification fields
########################################################################
*/
data class HardwareCapabilityProfile(
    val hardwareProfile: TestHardwareProfile = TestHardwareProfile.NANOVNA_H4,
    val displayName: String = "",
    val hardwareClass: HardwareClass = HardwareClass.ADVANCED_ANALYZER,
    val minFrequencyHz: Long = 0L,
    val maxFrequencyHz: Long = 0L,
    val supportsSwr: Boolean = false,
    val supportsSweep: Boolean = false,
    val supportsReturnLoss: Boolean = false,
    val supportsImpedanceRx: Boolean = false,
    val supportsSmithChart: Boolean = false,
    val supportsPhase: Boolean = false,
    val supportsS21: Boolean = false,
    val supportsTdrPreview: Boolean = false,
    val supportsMarkerSystem: Boolean = false,
    val supportsDeltaMarkers: Boolean = false,
    val supportsCsvPreview: Boolean = false,
    val notes: String = ""
)

/*
########################################################################
HARDWARE PROFILE RESOLUTION HELPERS
------------------------------------------------------------------------
PURPOSE
Maps each stored TestHardwareProfile enum value to a structured,
centralised HardwareCapabilityProfile.

SAFE EDIT AREA
- add mapping branches for new hardware
- keep old stored enum names stable
########################################################################
*/
fun TestHardwareProfile.toHardwareCapabilityProfile(): HardwareCapabilityProfile {
    return when (this) {
        TestHardwareProfile.NANOVNA_H4 -> {
            HardwareCapabilityProfile(
                hardwareProfile = TestHardwareProfile.NANOVNA_H4,
                displayName = "NanoVNA-H4",
                hardwareClass = HardwareClass.ADVANCED_ANALYZER,
                minFrequencyHz = 50_000L,
                maxFrequencyHz = 1_500_000_000L,
                supportsSwr = true,
                supportsSweep = true,
                supportsReturnLoss = true,
                supportsImpedanceRx = true,
                supportsSmithChart = true,
                supportsPhase = true,
                supportsS21 = true,
                supportsTdrPreview = true,
                supportsMarkerSystem = true,
                supportsDeltaMarkers = true,
                supportsCsvPreview = true,
                notes = "Compact handheld VNA profile with S11/S21 support."
            )
        }

        TestHardwareProfile.LITEVNA64_V0_3_3 -> {
            HardwareCapabilityProfile(
                hardwareProfile = TestHardwareProfile.LITEVNA64_V0_3_3,
                displayName = "LiteVNA64 v0.3.3",
                hardwareClass = HardwareClass.ADVANCED_ANALYZER,
                minFrequencyHz = 100_000L,
                maxFrequencyHz = 6_300_000_000L,
                supportsSwr = true,
                supportsSweep = true,
                supportsReturnLoss = true,
                supportsImpedanceRx = true,
                supportsSmithChart = true,
                supportsPhase = true,
                supportsS21 = true,
                supportsTdrPreview = true,
                supportsMarkerSystem = true,
                supportsDeltaMarkers = true,
                supportsCsvPreview = true,
                notes = "Extended-frequency handheld VNA profile with S11/S21 support."
            )
        }
    }
}

/*
########################################################################
HARDWARE MEASUREMENT CAPABILITY HELPERS
------------------------------------------------------------------------
PURPOSE
Maps each stored TestHardwareProfile enum value to a structured
measurement capability model for testing and future driver systems.

SAFE EDIT AREA
- add mapping branches for new hardware
- keep old stored enum names stable
########################################################################
*/
fun TestHardwareProfile.toHardwareMeasurementCapabilities(): HardwareMeasurementCapabilities {
    return when (this) {
        TestHardwareProfile.NANOVNA_H4 -> HardwareCapabilityProfiles.VNA_STANDARD
        TestHardwareProfile.LITEVNA64_V0_3_3 -> HardwareCapabilityProfiles.VNA_STANDARD
    }
}

/*
########################################################################
ENUMS
------------------------------------------------------------------------
PURPOSE
Shared enum definitions for project models.

SAFE EDIT AREA
- add enum values carefully when expanding supported workflows
########################################################################
*/
enum class ProjectStatus {
    DRAFT,
    DESIGNED,
    READY_TO_BUILD,
    TESTING,
    COMPLETED
}

enum class AntennaType {
    DIPOLE,
    MONOPOLE,
    YAGI,
    LOOP,
    GROUND_PLANE,
    OTHER
}

enum class FrequencyMode {
    SINGLE_FREQUENCY,
    RANGE
}

enum class EnvironmentType {
    FIXED,
    PORTABLE,
    VEHICLE,
    INDOOR,
    OUTDOOR,
    UNKNOWN
}

enum class PriorityMode {
    GAIN,
    SIZE,
    BANDWIDTH,
    SIMPLE_BUILD,
    BALANCED
}

enum class ConductorMaterial {
    COPPER,
    ALUMINIUM,
    STEEL,
    BRASS,
    OTHER
}

enum class BoomMaterial {
    ALUMINIUM,
    FIBREGLASS,
    PVC,
    WOOD,
    OTHER
}

enum class SupportMaterial {
    NONE,
    PVC,
    FIBREGLASS,
    WOOD,
    OTHER
}

enum class ConnectorType {
    BNC,
    SMA,
    N_TYPE,
    SO239,
    BARE_FEED,
    OTHER
}

enum class FeedlineType {
    COAX_50_OHM,
    COAX_75_OHM,
    TWIN_LEAD,
    LADDER_LINE,
    DIRECT_FEED,
    OTHER
}

enum class ProjectSection {
    OVERVIEW,
    DESIGN,
    MATERIALS,
    TESTING,
    NOTES
}

enum class ProjectCard {
    SUMMARY,
    DESIGN_SNAPSHOT,
    MATERIALS_SNAPSHOT,
    TEST_STATUS
}

enum class ProjectSource {
    WIZARD_CREATED,
    MANUALLY_CREATED,
    LOADED_FROM_STORAGE,
    DUPLICATED_FROM_PROJECT,
    TEMPLATE_APPLIED,
    LAB_PROJECT_TEMPLATE_TEST,
    LAB_UNKNOWN_DISCOVERY
}

enum class LabEntryMode {
    NONE,
    PROJECT_TEMPLATE_TEST,
    UNKNOWN_DISCOVERY
}

enum class ElementRole {
    DRIVEN,
    REFLECTOR,
    DIRECTOR,
    RADIATOR,
    RADIAL,
    LOOP_SIDE,
    OTHER
}

enum class SizeClass {
    SMALL,
    MEDIUM,
    LARGE,
    UNKNOWN
}

enum class BuildDifficulty {
    EASY,
    MODERATE,
    ADVANCED,
    UNKNOWN
}

enum class TuningSensitivity {
    LOW,
    MEDIUM,
    HIGH,
    UNKNOWN
}

enum class HardwareClass {
    BASIC_ANALYZER,
    ADVANCED_ANALYZER,
    PROFESSIONAL_VNA
}

enum class CalibrationRestorePolicy {
    DO_NOT_RESTORE,
    RESTORE_AS_STALE,
    RESTORE_IF_COMPATIBLE
}

/*
########################################################################
TEST HARDWARE PROFILE
------------------------------------------------------------------------
PURPOSE
Identifies the connected first-test hardware profile for measurement
and capability handling.

SAFE EDIT AREA
- add future supported hardware profiles
- keep stored enum names stable
########################################################################
*/
enum class TestHardwareProfile {
    NANOVNA_H4,
    LITEVNA64_V0_3_3
}