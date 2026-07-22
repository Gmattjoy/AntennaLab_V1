package com.example.antennalab_v1.storage

/*
------------------------------------------------------------
EDIT SECTION 1000
FILE: ProjectStorage.kt
PACKAGE: com.example.antennalab_v1.storage
LAYER: Storage / Project Persistence

SYSTEM ROLE
Handles multi-project file storage for ProjectData.

CURRENT DEVELOPMENT ROLE
Provides the main storage layer for:

• saving projects
• loading projects
• loading by project ID
• listing indexed projects
• deleting projects
• duplicating projects
• migrating legacy shared-preferences project data

STORAGE
- Project files: files/projects/project_<projectId>.json
- Index file:    files/projects/project_index.json
- Active pointer: files/projects/active_project_id.txt

SAFE EDIT AREA
- extend project JSON structure later
- add external storage support later
- add schema migration helpers later
------------------------------------------------------------
*/

import android.content.Context
import com.example.antennalab_v1.model.AntennaClassification
import com.example.antennalab_v1.model.AntennaType
import com.example.antennalab_v1.model.AvailablePartsProfile
import com.example.antennalab_v1.model.BoomMaterial
import com.example.antennalab_v1.model.BuildCostProfile
import com.example.antennalab_v1.model.CalculatedDesign
import com.example.antennalab_v1.model.CalibrationRestorePolicy
import com.example.antennalab_v1.model.ConductorMaterial
import com.example.antennalab_v1.model.ConnectorType
import com.example.antennalab_v1.model.DesignInput
import com.example.antennalab_v1.model.DiscoverySnapshot
import com.example.antennalab_v1.model.EnvironmentType
import com.example.antennalab_v1.model.FeedlineType
import com.example.antennalab_v1.model.FrequencyMode
import com.example.antennalab_v1.model.LabEntryMode
import com.example.antennalab_v1.model.MaterialConfig
import com.example.antennalab_v1.model.PriorityMode
import com.example.antennalab_v1.model.ProjectCalibrationData
import com.example.antennalab_v1.model.ProjectCard
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.ProjectListItem
import com.example.antennalab_v1.model.ProjectMeta
import com.example.antennalab_v1.model.ProjectSection
import com.example.antennalab_v1.model.ProjectSource
import com.example.antennalab_v1.model.ProjectStatus
import com.example.antennalab_v1.model.ProjectSweepHistoryEntry
import com.example.antennalab_v1.model.ProjectSweepHistoryMode
import com.example.antennalab_v1.model.ProjectUiState
import com.example.antennalab_v1.model.SupportMaterial
import com.example.antennalab_v1.model.TestData
import com.example.antennalab_v1.model.TestHardwareProfile
import com.example.antennalab_v1.model.VersionInfo
import com.example.antennalab_v1.model.testing.CalibrationCaptureSource
import com.example.antennalab_v1.model.testing.CalibrationSession
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/*
------------------------------------------------------------
EDIT SECTION 1001
STORAGE CONSTANTS
------------------------------------------------------------
PURPOSE
Defines storage paths and legacy migration keys.

SAFE EDIT AREA
- add new migration keys later
- adjust folder names carefully
------------------------------------------------------------
*/
object ProjectStorage {

    private const val PROJECTS_FOLDER_NAME = "projects"
    private const val ACTIVE_PROJECT_FILE_NAME = "active_project_id.txt"

    private const val LEGACY_PREFS_NAME = "antenna_lab_project_store"
    private const val KEY_PROJECT_ID = "project_id"
    private const val KEY_PROJECT_NAME = "project_name"
    private const val KEY_PROJECT_STATUS = "project_status"
    private const val KEY_CREATED_AT = "created_at"
    private const val KEY_UPDATED_AT = "updated_at"
    private const val KEY_NOTES = "notes"

    private const val KEY_ANTENNA_TYPE = "antenna_type"
    private const val KEY_FREQUENCY_MODE = "frequency_mode"
    private const val KEY_TARGET_FREQUENCY = "target_frequency"
    private const val KEY_FREQUENCY_START = "frequency_start"
    private const val KEY_FREQUENCY_END = "frequency_end"
    private const val KEY_ENVIRONMENT_TYPE = "environment_type"
    private const val KEY_PRIORITY_MODE = "priority_mode"

    private const val KEY_CONDUCTOR_MATERIAL = "conductor_material"
    private const val KEY_CONDUCTOR_DIAMETER = "conductor_diameter"
    private const val KEY_BOOM_MATERIAL = "boom_material"
    private const val KEY_SUPPORT_MATERIAL = "support_material"
    private const val KEY_CONNECTOR_TYPE = "connector_type"
    private const val KEY_FEEDLINE_TYPE = "feedline_type"
    private const val KEY_BUILD_NOTES = "build_notes"

    private const val KEY_ELEMENT_LENGTHS = "element_lengths"
    private const val KEY_ELEMENT_SPACING = "element_spacing"
    private const val KEY_BOOM_LENGTH = "boom_length"
    private const val KEY_FEED_POINT_GAP = "feed_point_gap"
    private const val KEY_MATCHING_METHOD = "matching_method"
    private const val KEY_EST_GAIN = "estimated_gain"
    private const val KEY_EST_FRONT_TO_BACK = "estimated_front_to_back"
    private const val KEY_EST_BANDWIDTH = "estimated_bandwidth"
    private const val KEY_DESIGN_WARNINGS = "design_warnings"

    private const val KEY_HAS_MEASURED_DATA = "has_measured_data"
    private const val KEY_LAST_SWEEP_DATE = "last_sweep_date"
    private const val KEY_SWEEP_START = "sweep_start"
    private const val KEY_SWEEP_END = "sweep_end"
    private const val KEY_RESONANT_FREQUENCY = "resonant_frequency"
    private const val KEY_MINIMUM_SWR = "minimum_swr"
    private const val KEY_RETURN_LOSS = "return_loss"
    private const val KEY_MEASUREMENT_NOTES = "measurement_notes"
    private const val KEY_TRIM_HISTORY = "trim_history"

    private const val KEY_LAST_OPENED_SECTION = "last_opened_section"
    private const val KEY_LAST_EXPANDED_CARD = "last_expanded_card"
    private const val KEY_HAS_SEEN_PROJECT_INTRO = "has_seen_project_intro"

    private const val KEY_DATA_SCHEMA_VERSION = "data_schema_version"
    private const val KEY_APP_DATA_SOURCE = "app_data_source"

    /*
    ------------------------------------------------------------
    EDIT SECTION 1002
    PUBLIC STORAGE OPERATIONS
    ------------------------------------------------------------
    PURPOSE
    Main public API for project save/load/list/delete/duplicate actions.

    SAFE EDIT AREA
    - add export/import helpers later
    - add bulk project operations later
    ------------------------------------------------------------
    */
    fun saveProject(context: Context, project: ProjectData) {
        val normalizedProject = normalizeProjectForSave(project)
        val file = getProjectFile(context, normalizedProject.meta.projectId)

        file.writeText(
            toJson(normalizedProject).toString(2)
        )

        ProjectIndexManager.saveOrUpdateProject(
            context = context,
            project = normalizedProject.toProjectListItem()
        )

        setActiveProjectId(
            context = context,
            projectId = normalizedProject.meta.projectId
        )
    }

    fun loadProject(context: Context): ProjectData {
        val activeProjectId = getActiveProjectId(context)

        if (!activeProjectId.isNullOrBlank()) {
            val activeProject = loadProjectById(context, activeProjectId)
            if (activeProject != null) {
                return activeProject
            }
        }

        val indexedProjects = ProjectIndexManager.getAllProjects(context)
        if (indexedProjects.isNotEmpty()) {
            val mostRecent = indexedProjects.first()
            val loaded = loadProjectById(context, mostRecent.projectId)
            if (loaded != null) {
                setActiveProjectId(context, mostRecent.projectId)
                return loaded
            }
        }

        val migratedLegacyProject = migrateLegacySharedPreferencesProjectIfPresent(context)
        if (migratedLegacyProject != null) {
            return migratedLegacyProject
        }

        return ProjectData()
    }

    fun loadProjectById(
        context: Context,
        projectId: String
    ): ProjectData? {
        val file = getProjectFile(context, projectId)

        if (!file.exists()) {
            return null
        }

        return runCatching {
            val json = JSONObject(file.readText())
            fromJson(json)
        }.getOrNull()
    }

    fun listProjects(context: Context): List<ProjectListItem> {
        return ProjectIndexManager.getAllProjects(context)
    }

    fun deleteProject(
        context: Context,
        projectId: String
    ): Boolean {
        val file = getProjectFile(context, projectId)
        val deleted = !file.exists() || file.delete()

        if (!deleted) {
            return false
        }

        ProjectIndexManager.removeProject(context, projectId)

        val activeProjectId = getActiveProjectId(context)
        if (activeProjectId == projectId) {
            val remaining = ProjectIndexManager.getAllProjects(context)
            if (remaining.isNotEmpty()) {
                setActiveProjectId(context, remaining.first().projectId)
            } else {
                clearActiveProjectId(context)
            }
        }

        return true
    }

    fun duplicateProject(
        context: Context,
        projectId: String
    ): ProjectData? {
        val original = loadProjectById(context, projectId) ?: return null
        val now = System.currentTimeMillis()
        val duplicateName = ProjectIndexManager.buildDuplicateName(
            context = context,
            originalName = original.meta.projectName
        )

        val duplicatedProject = original.copy(
            meta = original.meta.copy(
                projectId = generateProjectId(),
                projectName = duplicateName,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            ),
            calibrationData = original.calibrationData.copy(
                restoredFromStorage = false
            ),
            versionInfo = original.versionInfo.copy(
                appDataSource = ProjectSource.DUPLICATED_FROM_PROJECT
            )
        )

        saveProject(context, duplicatedProject)
        return duplicatedProject
    }

    fun clearProject(context: Context) {
        val projectsDirectory = getProjectsDirectory(context)

        projectsDirectory.listFiles()
            ?.filter { it.isFile }
            ?.forEach { file ->
                if (file.name == ACTIVE_PROJECT_FILE_NAME || file.extension.lowercase() == "json") {
                    file.delete()
                }
            }

        clearActiveProjectId(context)
    }


    fun appendProjectSweepHistoryEntry(
        context: Context,
        project: ProjectData,
        historyEntry: ProjectSweepHistoryEntry,
        maxHistoryEntries: Int = 64
    ): ProjectData {
        val baseProject = resolveBaseProjectForUpdate(context, project)

        val updatedProject = normalizeProjectForSave(
            baseProject.copy(
                sweepHistory = appendSweepHistoryEntry(
                    existingHistory = baseProject.sweepHistory,
                    historyEntry = historyEntry,
                    maxHistoryEntries = maxHistoryEntries
                ),
                testData = applySweepHistoryEntryToTestData(
                    current = baseProject.testData,
                    historyEntry = historyEntry
                )
            )
        )

        saveProject(context, updatedProject)
        return updatedProject
    }

    fun applyDiscoveryToCurrentProject(
        context: Context,
        project: ProjectData,
        discoverySnapshot: DiscoverySnapshot,
        historyEntry: ProjectSweepHistoryEntry,
        maxHistoryEntries: Int = 64
    ): ProjectData {
        val baseProject = resolveBaseProjectForUpdate(context, project)

        val discoveryHistoryEntry = historyEntry.copy(
            mode = ProjectSweepHistoryMode.DISCOVERY_APPLIED
        )

        val updatedProject = normalizeProjectForSave(
            baseProject.copy(
                antennaClassification = discoverySnapshot.antennaClassificationGuess,
                discoverySnapshot = discoverySnapshot,
                sweepHistory = appendSweepHistoryEntry(
                    existingHistory = baseProject.sweepHistory,
                    historyEntry = discoveryHistoryEntry,
                    maxHistoryEntries = maxHistoryEntries
                ),
                testData = applyDiscoverySnapshotToTestData(
                    current = baseProject.testData,
                    discoverySnapshot = discoverySnapshot
                ),
                meta = baseProject.meta.copy(
                    labEntryMode = LabEntryMode.NONE,
                    labSourceProjectName = "",
                    labTemplateDisplayName = "",
                    labTemplateBandLabel = ""
                )
            )
        )

        saveProject(context, updatedProject)
        return updatedProject
    }

    fun saveDiscoveryAsNewProject(
        context: Context,
        sourceProject: ProjectData,
        discoverySnapshot: DiscoverySnapshot,
        historyEntry: ProjectSweepHistoryEntry,
        maxHistoryEntries: Int = 64
    ): ProjectData {
        val now = System.currentTimeMillis()
        val projectName = buildDiscoveryProjectName(discoverySnapshot)

        val newProject = normalizeProjectForSave(
            sourceProject.copy(
                meta = sourceProject.meta.copy(
                    projectId = generateProjectId(),
                    projectName = projectName,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now,
                    labEntryMode = LabEntryMode.NONE,
                    labSourceProjectName = "",
                    labTemplateDisplayName = "",
                    labTemplateBandLabel = ""
                ),
                antennaClassification = discoverySnapshot.antennaClassificationGuess,
                discoverySnapshot = discoverySnapshot,
                sweepHistory = appendSweepHistoryEntry(
                    existingHistory = emptyList(),
                    historyEntry = historyEntry.copy(
                        mode = ProjectSweepHistoryMode.DISCOVERY_APPLIED
                    ),
                    maxHistoryEntries = maxHistoryEntries
                ),
                testData = applyDiscoverySnapshotToTestData(
                    current = sourceProject.testData,
                    discoverySnapshot = discoverySnapshot
                ),
                versionInfo = sourceProject.versionInfo.copy(
                    appDataSource = ProjectSource.LAB_UNKNOWN_DISCOVERY
                )
            )
        )

        saveProject(context, newProject)
        return newProject
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 1003
    PROJECT NORMALIZATION
    ------------------------------------------------------------
    PURPOSE
    Ensures project IDs, names, and timestamps are valid before save.
    ------------------------------------------------------------
    */
    private fun normalizeProjectForSave(project: ProjectData): ProjectData {
        val now = System.currentTimeMillis()
        val normalizedProjectId = project.meta.projectId.ifBlank { generateProjectId() }
        val normalizedName = project.meta.projectName.ifBlank { "Default" }
        val normalizedCreatedAt = project.meta.createdAtEpochMs.takeIf { it > 0L } ?: now

        return project.copy(
            meta = project.meta.copy(
                projectId = normalizedProjectId,
                projectName = normalizedName,
                createdAtEpochMs = normalizedCreatedAt,
                updatedAtEpochMs = now
            )
        )
    }

    private fun ProjectData.toProjectListItem(): ProjectListItem {
        return ProjectListItem(
            projectId = meta.projectId,
            name = meta.projectName.ifBlank { "Default" },
            antennaType = designInput.antennaType.name,
            targetFrequencyHz = (designInput.targetFrequencyMHz * 1_000_000.0).toLong(),
            lastEditedEpochMillis = meta.updatedAtEpochMs
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 1004
    FILE ACCESS HELPERS
    ------------------------------------------------------------
    PURPOSE
    Resolves project storage directory and specific project files.
    ------------------------------------------------------------
    */
    private fun getProjectsDirectory(context: Context): File {
        val directory = File(context.filesDir, PROJECTS_FOLDER_NAME)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    private fun getProjectFile(
        context: Context,
        projectId: String
    ): File {
        return File(
            getProjectsDirectory(context),
            "project_${projectId}.json"
        )
    }

    private fun getActiveProjectFile(context: Context): File {
        return File(
            getProjectsDirectory(context),
            ACTIVE_PROJECT_FILE_NAME
        )
    }

    private fun setActiveProjectId(
        context: Context,
        projectId: String
    ) {
        getActiveProjectFile(context).writeText(projectId)
    }

    private fun getActiveProjectId(context: Context): String? {
        val file = getActiveProjectFile(context)
        if (!file.exists()) {
            return null
        }

        return file.readText().trim().ifBlank { null }
    }

    private fun clearActiveProjectId(context: Context) {
        val file = getActiveProjectFile(context)
        if (file.exists()) {
            file.delete()
        }
    }

    private fun generateProjectId(): String {
        return UUID.randomUUID().toString()
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2001
    ROOT JSON SERIALIZATION
    ------------------------------------------------------------
    PURPOSE
    Serializes and deserializes the full ProjectData object.
    ------------------------------------------------------------
    */
    private fun toJson(project: ProjectData): JSONObject {
        return JSONObject().apply {
            put("meta", project.meta.toJson())
            put("antennaClassification", project.antennaClassification.name)
            put("designInput", project.designInput.toJson())
            put("materialConfig", project.materialConfig.toJson())
            put("calculatedDesign", project.calculatedDesign.toJson())
            put("testData", project.testData.toJson())
            put("discoverySnapshot", project.discoverySnapshot?.toJson())
            put("sweepHistory", JSONArray().apply {
                project.sweepHistory.forEach { historyEntry ->
                    put(historyEntry.toJson())
                }
            })
            put("calibrationData", project.calibrationData.toJson())
            put("uiState", project.uiState.toJson())
            put("versionInfo", project.versionInfo.toJson())
            put("buildCostProfile", project.buildCostProfile.name)
            put("availablePartsProfile", project.availablePartsProfile.name)
            put("testHardwareProfile", project.testHardwareProfile.name)
        }
    }

    private fun fromJson(json: JSONObject): ProjectData {
        val designInput = json.optJSONObject("designInput")?.toDesignInput() ?: DesignInput()

        return ProjectData(
            meta = json.optJSONObject("meta")?.toProjectMeta() ?: ProjectMeta(),
            antennaClassification = enumValueOrDefault(
                json.optOptionalString("antennaClassification"),
                designInput.antennaType.toAntennaClassification()
            ),
            designInput = designInput,
            materialConfig = json.optJSONObject("materialConfig")?.toMaterialConfig() ?: MaterialConfig(),
            calculatedDesign = json.optJSONObject("calculatedDesign")?.toCalculatedDesign() ?: CalculatedDesign(),
            testData = json.optJSONObject("testData")?.toTestData() ?: TestData(),
            discoverySnapshot = json.optJSONObject("discoverySnapshot")?.toDiscoverySnapshot(),
            sweepHistory = json.optJSONArray("sweepHistory").toSweepHistoryList(),
            calibrationData = json.optJSONObject("calibrationData")?.toProjectCalibrationData() ?: ProjectCalibrationData(),
            uiState = json.optJSONObject("uiState")?.toProjectUiState() ?: ProjectUiState(),
            versionInfo = json.optJSONObject("versionInfo")?.toVersionInfo() ?: VersionInfo(),
            buildCostProfile = enumValueOrDefault(
                json.optOptionalString("buildCostProfile"),
                BuildCostProfile.STANDARD
            ),
            availablePartsProfile = enumValueOrDefault(
                json.optOptionalString("availablePartsProfile"),
                AvailablePartsProfile.SOME_EXISTING_PARTS
            ),
            testHardwareProfile = enumValueOrDefault(
                json.optOptionalString("testHardwareProfile"),
                TestHardwareProfile.NANOVNA_H4
            )
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2002
    META SERIALIZATION
    ------------------------------------------------------------
    PURPOSE
    Converts ProjectMeta to and from JSON.
    ------------------------------------------------------------
    */
    private fun ProjectMeta.toJson(): JSONObject {
        return JSONObject().apply {
            put("projectId", projectId)
            put("projectName", projectName)
            put("projectStatus", projectStatus.name)
            put("createdAtEpochMs", createdAtEpochMs)
            put("updatedAtEpochMs", updatedAtEpochMs)
            put("notes", notes)
        }
    }

    private fun JSONObject.toProjectMeta(): ProjectMeta {
        return ProjectMeta(
            projectId = optString("projectId"),
            projectName = optString("projectName", "Default"),
            projectStatus = enumValueOrDefault(
                optOptionalString("projectStatus"),
                ProjectStatus.DRAFT
            ),
            createdAtEpochMs = optLong("createdAtEpochMs", 0L),
            updatedAtEpochMs = optLong("updatedAtEpochMs", 0L),
            notes = optString("notes")
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2003
    DESIGN INPUT SERIALIZATION
    ------------------------------------------------------------
    PURPOSE
    Converts DesignInput to and from JSON.
    ------------------------------------------------------------
    */
    private fun DesignInput.toJson(): JSONObject {
        return JSONObject().apply {
            put("antennaType", antennaType.name)
            put("frequencyMode", frequencyMode.name)
            put("targetFrequencyMHz", targetFrequencyMHz)
            put("frequencyStartMHz", frequencyStartMHz)
            put("frequencyEndMHz", frequencyEndMHz)
            put("environmentType", environmentType.name)
            put("priorityMode", priorityMode.name)
        }
    }

    private fun JSONObject.toDesignInput(): DesignInput {
        return DesignInput(
            antennaType = enumValueOrDefault(
                optOptionalString("antennaType"),
                AntennaType.DIPOLE
            ),
            frequencyMode = enumValueOrDefault(
                optOptionalString("frequencyMode"),
                FrequencyMode.SINGLE_FREQUENCY
            ),
            targetFrequencyMHz = optDouble("targetFrequencyMHz", 0.0),
            frequencyStartMHz = optDouble("frequencyStartMHz", 0.0),
            frequencyEndMHz = optDouble("frequencyEndMHz", 0.0),
            environmentType = enumValueOrDefault(
                optOptionalString("environmentType"),
                EnvironmentType.UNKNOWN
            ),
            priorityMode = enumValueOrDefault(
                optOptionalString("priorityMode"),
                PriorityMode.BALANCED
            )
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2004
    MATERIAL CONFIG SERIALIZATION
    ------------------------------------------------------------
    PURPOSE
    Converts MaterialConfig to and from JSON.
    ------------------------------------------------------------
    */
    private fun MaterialConfig.toJson(): JSONObject {
        return JSONObject().apply {
            put("conductorMaterial", conductorMaterial.name)
            put("conductorDiameterMm", conductorDiameterMm)
            put("boomMaterial", boomMaterial.name)
            put("supportMaterial", supportMaterial.name)
            put("connectorType", connectorType.name)
            put("feedlineType", feedlineType.name)
            put("buildNotes", buildNotes)
        }
    }

    private fun JSONObject.toMaterialConfig(): MaterialConfig {
        return MaterialConfig(
            conductorMaterial = enumValueOrDefault(
                optOptionalString("conductorMaterial"),
                ConductorMaterial.COPPER
            ),
            conductorDiameterMm = optDouble("conductorDiameterMm", 1.0),
            boomMaterial = enumValueOrDefault(
                optOptionalString("boomMaterial"),
                BoomMaterial.ALUMINIUM
            ),
            supportMaterial = enumValueOrDefault(
                optOptionalString("supportMaterial"),
                SupportMaterial.NONE
            ),
            connectorType = enumValueOrDefault(
                optOptionalString("connectorType"),
                ConnectorType.SMA
            ),
            feedlineType = enumValueOrDefault(
                optOptionalString("feedlineType"),
                FeedlineType.COAX_50_OHM
            ),
            buildNotes = optString("buildNotes")
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2005
    CALCULATED DESIGN SERIALIZATION
    ------------------------------------------------------------
    PURPOSE
    Converts CalculatedDesign to and from JSON using currently stored
    fields.
    ------------------------------------------------------------
    */
    private fun CalculatedDesign.toJson(): JSONObject {
        return JSONObject().apply {
            put("elementLengthsMm", JSONArray(elementLengthsMm))
            put("elementSpacingMm", JSONArray(elementSpacingMm))
            put("boomLengthMm", boomLengthMm)
            put("feedPointGapMm", feedPointGapMm)
            put("matchingMethod", matchingMethod)
            put("estimatedGainDbI", estimatedGainDbI)
            put("estimatedFrontToBackDb", estimatedFrontToBackDb)
            put("estimatedBandwidthMHz", estimatedBandwidthMHz)
            put("designWarnings", JSONArray(designWarnings))
        }
    }

    private fun JSONObject.toCalculatedDesign(): CalculatedDesign {
        return CalculatedDesign(
            elementLengthsMm = optJSONArray("elementLengthsMm").toDoubleList(),
            elementSpacingMm = optJSONArray("elementSpacingMm").toDoubleList(),
            boomLengthMm = optDouble("boomLengthMm", 0.0),
            feedPointGapMm = optDouble("feedPointGapMm", 0.0),
            matchingMethod = optString("matchingMethod"),
            estimatedGainDbI = optDouble("estimatedGainDbI", 0.0),
            estimatedFrontToBackDb = optDouble("estimatedFrontToBackDb", 0.0),
            estimatedBandwidthMHz = optDouble("estimatedBandwidthMHz", 0.0),
            designWarnings = optJSONArray("designWarnings").toStringList()
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2006
    TEST DATA SERIALIZATION
    ------------------------------------------------------------
    PURPOSE
    Converts TestData to and from JSON.
    ------------------------------------------------------------
    */
    private fun TestData.toJson(): JSONObject {
        return JSONObject().apply {
            put("hasMeasuredData", hasMeasuredData)
            put("lastSweepDateEpochMs", lastSweepDateEpochMs)
            put("sweepStartMHz", sweepStartMHz)
            put("sweepEndMHz", sweepEndMHz)
            put("resonantFrequencyMHz", resonantFrequencyMHz)
            put("minimumSwr", minimumSwr)
            put("returnLossDb", returnLossDb)
            put("measurementNotes", measurementNotes)
            put("trimHistory", JSONArray(trimHistory))
        }
    }

    private fun JSONObject.toTestData(): TestData {
        return TestData(
            hasMeasuredData = optBoolean("hasMeasuredData", false),
            lastSweepDateEpochMs = optLong("lastSweepDateEpochMs", 0L),
            sweepStartMHz = optDouble("sweepStartMHz", 0.0),
            sweepEndMHz = optDouble("sweepEndMHz", 0.0),
            resonantFrequencyMHz = optDouble("resonantFrequencyMHz", 0.0),
            minimumSwr = optDouble("minimumSwr", 0.0),
            returnLossDb = optDouble("returnLossDb", 0.0),
            measurementNotes = optString("measurementNotes"),
            trimHistory = optJSONArray("trimHistory").toStringList()
        )
    }


    /*
    ------------------------------------------------------------
    EDIT SECTION 2006B
    DISCOVERY SNAPSHOT SERIALIZATION
    ------------------------------------------------------------
    PURPOSE
    Converts DiscoverySnapshot to and from JSON.
    ------------------------------------------------------------
    */
    private fun DiscoverySnapshot.toJson(): JSONObject {
        return JSONObject().apply {
            put("capturedAtEpochMs", capturedAtEpochMs)
            put("antennaClassificationGuess", antennaClassificationGuess.name)
            put("hardwareName", hardwareName)
            put("sweepStartMHz", sweepStartMHz)
            put("sweepEndMHz", sweepEndMHz)
            put("bestFrequencyMHz", bestFrequencyMHz)
            put("bestSwr", bestSwr)
            put("returnLossDb", returnLossDb)
            put("summaryLabel", summaryLabel)
            put("operatorNotes", operatorNotes)
        }
    }

    private fun JSONObject.toDiscoverySnapshot(): DiscoverySnapshot {
        return DiscoverySnapshot(
            capturedAtEpochMs = optLong("capturedAtEpochMs", 0L),
            antennaClassificationGuess = enumValueOrDefault(
                optOptionalString("antennaClassificationGuess"),
                AntennaClassification.NOT_YET_CLASSIFIED
            ),
            hardwareName = optString("hardwareName"),
            sweepStartMHz = optDouble("sweepStartMHz", 0.0),
            sweepEndMHz = optDouble("sweepEndMHz", 0.0),
            bestFrequencyMHz = optDouble("bestFrequencyMHz", 0.0),
            bestSwr = optDouble("bestSwr", 0.0),
            returnLossDb = optNullableDouble("returnLossDb"),
            summaryLabel = optString("summaryLabel"),
            operatorNotes = optString("operatorNotes")
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2006C
    SWEEP HISTORY SERIALIZATION
    ------------------------------------------------------------
    PURPOSE
    Converts ProjectSweepHistoryEntry to and from JSON.
    ------------------------------------------------------------
    */
    private fun ProjectSweepHistoryEntry.toJson(): JSONObject {
        return JSONObject().apply {
            put("recordedAtEpochMs", recordedAtEpochMs)
            put("mode", mode.name)
            put("hardwareName", hardwareName)
            put("sweepStartMHz", sweepStartMHz)
            put("sweepEndMHz", sweepEndMHz)
            put("bestFrequencyMHz", bestFrequencyMHz)
            put("bestSwr", bestSwr)
            put("returnLossDb", returnLossDb)
            put("label", label)
            put("note", note)
            put("isComplete", isComplete)
            put("actualPointCount", actualPointCount)
            put("requestedPointCount", requestedPointCount)
        }
    }

    private fun JSONObject.toProjectSweepHistoryEntry(): ProjectSweepHistoryEntry {
        return ProjectSweepHistoryEntry(
            recordedAtEpochMs = optLong("recordedAtEpochMs", 0L),
            mode = enumValueOrDefault(
                optOptionalString("mode"),
                ProjectSweepHistoryMode.PROJECT_TEST
            ),
            hardwareName = optString("hardwareName"),
            sweepStartMHz = optDouble("sweepStartMHz", 0.0),
            sweepEndMHz = optDouble("sweepEndMHz", 0.0),
            bestFrequencyMHz = optDouble("bestFrequencyMHz", 0.0),
            bestSwr = optDouble("bestSwr", 0.0),
            returnLossDb = optNullableDouble("returnLossDb"),
            label = optString("label"),
            note = optString("note"),
            // Legacy saves have no completeness keys → default to complete.
            isComplete = optBoolean("isComplete", true),
            actualPointCount = optInt("actualPointCount", 0),
            requestedPointCount = optInt("requestedPointCount", 0)
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2006A
    CALIBRATION DATA SERIALIZATION
    ------------------------------------------------------------
    PURPOSE
    Converts ProjectCalibrationData and CalibrationSession to and from
    JSON for project-level persistence.
    ------------------------------------------------------------
    */
    private fun ProjectCalibrationData.toJson(): JSONObject {
        return JSONObject().apply {
            put(
                "storedCalibrationSession",
                storedCalibrationSession?.toJson()
            )
            put("lastCalibrationSavedEpochMs", lastCalibrationSavedEpochMs)
            put("lastCalibrationStatusSummary", lastCalibrationStatusSummary)
            put("restorePolicy", restorePolicy.name)
            put("restoredFromStorage", restoredFromStorage)
        }
    }

    private fun JSONObject.toProjectCalibrationData(): ProjectCalibrationData {
        return ProjectCalibrationData(
            storedCalibrationSession = optJSONObject("storedCalibrationSession")
                ?.toCalibrationSession(),
            lastCalibrationSavedEpochMs = optLong("lastCalibrationSavedEpochMs", 0L),
            lastCalibrationStatusSummary = optString("lastCalibrationStatusSummary"),
            restorePolicy = enumValueOrDefault(
                optOptionalString("restorePolicy"),
                CalibrationRestorePolicy.RESTORE_AS_STALE
            ),
            restoredFromStorage = optBoolean("restoredFromStorage", true)
        )
    }

    private fun CalibrationSession.toJson(): JSONObject {
        return JSONObject().apply {
            put("hardwareDisplayName", hardwareDisplayName)
            put("startFrequencyMHz", startFrequencyMHz)
            put("endFrequencyMHz", endFrequencyMHz)
            put("openCaptured", openCaptured)
            put("shortCaptured", shortCaptured)
            put("loadCaptured", loadCaptured)
            put("timestampLabel", timestampLabel)
            put("capturedAtEpochMs", capturedAtEpochMs)
            put("captureSource", captureSource.name)
            put("capturedProtocolFamily", capturedProtocolFamily)
            put("capturedInstrumentIdentityText", capturedInstrumentIdentityText)
            put("capturedSessionKey", capturedSessionKey)
        }
    }

    private fun JSONObject.toCalibrationSession(): CalibrationSession {
        return CalibrationSession(
            hardwareDisplayName = optString("hardwareDisplayName"),
            startFrequencyMHz = optDouble("startFrequencyMHz", 0.0),
            endFrequencyMHz = optDouble("endFrequencyMHz", 0.0),
            openCaptured = optBoolean("openCaptured", false),
            shortCaptured = optBoolean("shortCaptured", false),
            loadCaptured = optBoolean("loadCaptured", false),
            timestampLabel = optString("timestampLabel", "Not Captured"),
            capturedAtEpochMs = optLong("capturedAtEpochMs", 0L),
            captureSource = enumValueOrDefault(
                optOptionalString("captureSource"),
                CalibrationCaptureSource.UNKNOWN
            ),
            capturedProtocolFamily = optOptionalString("capturedProtocolFamily"),
            capturedInstrumentIdentityText = optOptionalString("capturedInstrumentIdentityText"),
            capturedSessionKey = optOptionalString("capturedSessionKey")
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2007
    UI STATE SERIALIZATION
    ------------------------------------------------------------
    PURPOSE
    Converts ProjectUiState to and from JSON.
    ------------------------------------------------------------
    */
    private fun ProjectUiState.toJson(): JSONObject {
        return JSONObject().apply {
            put("lastOpenedSection", lastOpenedSection.name)
            put("lastExpandedCard", lastExpandedCard.name)
            put("hasSeenProjectIntro", hasSeenProjectIntro)
        }
    }

    private fun JSONObject.toProjectUiState(): ProjectUiState {
        return ProjectUiState(
            lastOpenedSection = enumValueOrDefault(
                optOptionalString("lastOpenedSection"),
                ProjectSection.OVERVIEW
            ),
            lastExpandedCard = enumValueOrDefault(
                optOptionalString("lastExpandedCard"),
                ProjectCard.SUMMARY
            ),
            hasSeenProjectIntro = optBoolean("hasSeenProjectIntro", false)
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2008
    VERSION INFO SERIALIZATION
    ------------------------------------------------------------
    PURPOSE
    Converts VersionInfo to and from JSON.
    ------------------------------------------------------------
    */
    private fun VersionInfo.toJson(): JSONObject {
        return JSONObject().apply {
            put("dataSchemaVersion", dataSchemaVersion)
            put("appDataSource", appDataSource.name)
        }
    }

    private fun JSONObject.toVersionInfo(): VersionInfo {
        return VersionInfo(
            dataSchemaVersion = optInt("dataSchemaVersion", 1),
            appDataSource = enumValueOrDefault(
                optOptionalString("appDataSource"),
                ProjectSource.WIZARD_CREATED
            )
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 3001
    JSON LIST HELPERS
    ------------------------------------------------------------
    PURPOSE
    Converts JSON arrays into Kotlin lists.
    ------------------------------------------------------------
    */
    private fun JSONArray?.toDoubleList(): List<Double> {
        if (this == null) return emptyList()

        val results = mutableListOf<Double>()
        for (i in 0 until length()) {
            results.add(optDouble(i, 0.0))
        }
        return results
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()

        val results = mutableListOf<String>()
        for (i in 0 until length()) {
            results.add(optString(i, ""))
        }
        return results.filter { it.isNotBlank() }
    }

    private fun JSONArray?.toSweepHistoryList(): List<ProjectSweepHistoryEntry> {
        if (this == null) return emptyList()

        val results = mutableListOf<ProjectSweepHistoryEntry>()
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            results.add(item.toProjectSweepHistoryEntry())
        }
        return results.sortedByDescending { it.recordedAtEpochMs }
    }

    private fun JSONObject.optOptionalString(key: String): String? {
        return if (has(key) && !isNull(key)) {
            optString(key)
        } else {
            null
        }
    }

    private fun JSONObject.optNullableDouble(key: String): Double? {
        return if (has(key) && !isNull(key)) {
            optDouble(key)
        } else {
            null
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        raw: String?,
        defaultValue: T
    ): T {
        return raw?.let { value ->
            enumValues<T>().firstOrNull { enumItem -> enumItem.name == value }
        } ?: defaultValue
    }

    private fun resolveBaseProjectForUpdate(
        context: Context,
        project: ProjectData
    ): ProjectData {
        val projectId = project.meta.projectId
        return if (projectId.isNotBlank()) {
            loadProjectById(context, projectId) ?: project
        } else {
            project
        }
    }

    private fun appendSweepHistoryEntry(
        existingHistory: List<ProjectSweepHistoryEntry>,
        historyEntry: ProjectSweepHistoryEntry,
        maxHistoryEntries: Int
    ): List<ProjectSweepHistoryEntry> {
        return buildList {
            add(historyEntry)
            addAll(existingHistory)
        }.sortedByDescending { it.recordedAtEpochMs }
            .take(maxHistoryEntries.coerceAtLeast(1))
    }

    private fun applySweepHistoryEntryToTestData(
        current: TestData,
        historyEntry: ProjectSweepHistoryEntry
    ): TestData {
        return current.copy(
            hasMeasuredData = true,
            lastSweepDateEpochMs = historyEntry.recordedAtEpochMs,
            sweepStartMHz = historyEntry.sweepStartMHz,
            sweepEndMHz = historyEntry.sweepEndMHz,
            resonantFrequencyMHz = historyEntry.bestFrequencyMHz,
            minimumSwr = historyEntry.bestSwr,
            returnLossDb = historyEntry.returnLossDb ?: current.returnLossDb,
            measurementNotes = historyEntry.note.ifBlank { current.measurementNotes }
        )
    }

    private fun applyDiscoverySnapshotToTestData(
        current: TestData,
        discoverySnapshot: DiscoverySnapshot
    ): TestData {
        return current.copy(
            hasMeasuredData = true,
            lastSweepDateEpochMs = discoverySnapshot.capturedAtEpochMs,
            sweepStartMHz = discoverySnapshot.sweepStartMHz,
            sweepEndMHz = discoverySnapshot.sweepEndMHz,
            resonantFrequencyMHz = discoverySnapshot.bestFrequencyMHz,
            minimumSwr = discoverySnapshot.bestSwr,
            returnLossDb = discoverySnapshot.returnLossDb ?: current.returnLossDb,
            measurementNotes = discoverySnapshot.summaryLabel.ifBlank { current.measurementNotes }
        )
    }

    private fun buildDiscoveryProjectName(
        discoverySnapshot: DiscoverySnapshot
    ): String {
        val classificationLabel = discoverySnapshot.antennaClassificationGuess.name
            .lowercase()
            .split("_")
            .joinToString(" ") { token -> token.replaceFirstChar { it.uppercase() } }

        return if (classificationLabel.isBlank()) {
            "Discovery Project"
        } else {
            "Discovery ${classificationLabel}"
        }
    }

    private fun AntennaType.toAntennaClassification(): AntennaClassification {
        return when (this) {
            AntennaType.DIPOLE -> AntennaClassification.DIPOLE
            AntennaType.MONOPOLE -> AntennaClassification.MONOPOLE
            AntennaType.YAGI -> AntennaClassification.YAGI
            AntennaType.LOOP -> AntennaClassification.LOOP
            AntennaType.GROUND_PLANE -> AntennaClassification.GROUND_PLANE
            AntennaType.OTHER -> AntennaClassification.OTHER
        }
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 3002
    LEGACY MIGRATION
    ------------------------------------------------------------
    PURPOSE
    Migrates old shared-preferences project data into the current file
    storage system.
    ------------------------------------------------------------
    */
    private fun migrateLegacySharedPreferencesProjectIfPresent(context: Context): ProjectData? {
        val prefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)

        val hasAnyLegacyData =
            prefs.contains(KEY_PROJECT_NAME) ||
                    prefs.contains(KEY_PROJECT_ID) ||
                    prefs.contains(KEY_TARGET_FREQUENCY)

        if (!hasAnyLegacyData) {
            return null
        }

        val migratedProject = ProjectData(
            meta = ProjectMeta(
                projectId = prefs.getString(KEY_PROJECT_ID, "") ?: "",
                projectName = prefs.getString(KEY_PROJECT_NAME, "Default") ?: "Default",
                projectStatus = enumValueOrDefault(
                    prefs.getString(KEY_PROJECT_STATUS, null),
                    ProjectStatus.DRAFT
                ),
                createdAtEpochMs = prefs.getLong(KEY_CREATED_AT, 0L),
                updatedAtEpochMs = prefs.getLong(KEY_UPDATED_AT, 0L),
                notes = prefs.getString(KEY_NOTES, "") ?: ""
            ),
            antennaClassification = enumValueOrDefault(
                prefs.getString(KEY_ANTENNA_TYPE, null),
                AntennaType.DIPOLE
            ).toAntennaClassification(),
            designInput = DesignInput(
                antennaType = enumValueOrDefault(
                    prefs.getString(KEY_ANTENNA_TYPE, null),
                    AntennaType.DIPOLE
                ),
                frequencyMode = enumValueOrDefault(
                    prefs.getString(KEY_FREQUENCY_MODE, null),
                    FrequencyMode.SINGLE_FREQUENCY
                ),
                targetFrequencyMHz = prefs.getString(KEY_TARGET_FREQUENCY, "0.0").toSafeDouble(),
                frequencyStartMHz = prefs.getString(KEY_FREQUENCY_START, "0.0").toSafeDouble(),
                frequencyEndMHz = prefs.getString(KEY_FREQUENCY_END, "0.0").toSafeDouble(),
                environmentType = enumValueOrDefault(
                    prefs.getString(KEY_ENVIRONMENT_TYPE, null),
                    EnvironmentType.UNKNOWN
                ),
                priorityMode = enumValueOrDefault(
                    prefs.getString(KEY_PRIORITY_MODE, null),
                    PriorityMode.BALANCED
                )
            ),
            materialConfig = MaterialConfig(
                conductorMaterial = enumValueOrDefault(
                    prefs.getString(KEY_CONDUCTOR_MATERIAL, null),
                    ConductorMaterial.COPPER
                ),
                conductorDiameterMm = prefs.getString(KEY_CONDUCTOR_DIAMETER, "1.0").toSafeDouble(),
                boomMaterial = enumValueOrDefault(
                    prefs.getString(KEY_BOOM_MATERIAL, null),
                    BoomMaterial.ALUMINIUM
                ),
                supportMaterial = enumValueOrDefault(
                    prefs.getString(KEY_SUPPORT_MATERIAL, null),
                    SupportMaterial.NONE
                ),
                connectorType = enumValueOrDefault(
                    prefs.getString(KEY_CONNECTOR_TYPE, null),
                    ConnectorType.SMA
                ),
                feedlineType = enumValueOrDefault(
                    prefs.getString(KEY_FEEDLINE_TYPE, null),
                    FeedlineType.COAX_50_OHM
                ),
                buildNotes = prefs.getString(KEY_BUILD_NOTES, "") ?: ""
            ),
            calculatedDesign = CalculatedDesign(
                elementLengthsMm = decodeDoubleList(prefs.getString(KEY_ELEMENT_LENGTHS, "")),
                elementSpacingMm = decodeDoubleList(prefs.getString(KEY_ELEMENT_SPACING, "")),
                boomLengthMm = prefs.getString(KEY_BOOM_LENGTH, "0.0").toSafeDouble(),
                feedPointGapMm = prefs.getString(KEY_FEED_POINT_GAP, "0.0").toSafeDouble(),
                matchingMethod = prefs.getString(KEY_MATCHING_METHOD, "") ?: "",
                estimatedGainDbI = prefs.getString(KEY_EST_GAIN, "0.0").toSafeDouble(),
                estimatedFrontToBackDb = prefs.getString(KEY_EST_FRONT_TO_BACK, "0.0").toSafeDouble(),
                estimatedBandwidthMHz = prefs.getString(KEY_EST_BANDWIDTH, "0.0").toSafeDouble(),
                designWarnings = decodeStringList(prefs.getString(KEY_DESIGN_WARNINGS, ""))
            ),
            testData = TestData(
                hasMeasuredData = prefs.getBoolean(KEY_HAS_MEASURED_DATA, false),
                lastSweepDateEpochMs = prefs.getLong(KEY_LAST_SWEEP_DATE, 0L),
                sweepStartMHz = prefs.getString(KEY_SWEEP_START, "0.0").toSafeDouble(),
                sweepEndMHz = prefs.getString(KEY_SWEEP_END, "0.0").toSafeDouble(),
                resonantFrequencyMHz = prefs.getString(KEY_RESONANT_FREQUENCY, "0.0").toSafeDouble(),
                minimumSwr = prefs.getString(KEY_MINIMUM_SWR, "0.0").toSafeDouble(),
                returnLossDb = prefs.getString(KEY_RETURN_LOSS, "0.0").toSafeDouble(),
                measurementNotes = prefs.getString(KEY_MEASUREMENT_NOTES, "") ?: "",
                trimHistory = decodeStringList(prefs.getString(KEY_TRIM_HISTORY, ""))
            ),
            calibrationData = ProjectCalibrationData(),
            uiState = ProjectUiState(
                lastOpenedSection = enumValueOrDefault(
                    prefs.getString(KEY_LAST_OPENED_SECTION, null),
                    ProjectSection.OVERVIEW
                ),
                lastExpandedCard = enumValueOrDefault(
                    prefs.getString(KEY_LAST_EXPANDED_CARD, null),
                    ProjectCard.SUMMARY
                ),
                hasSeenProjectIntro = prefs.getBoolean(KEY_HAS_SEEN_PROJECT_INTRO, false)
            ),
            versionInfo = VersionInfo(
                dataSchemaVersion = prefs.getInt(KEY_DATA_SCHEMA_VERSION, 1),
                appDataSource = enumValueOrDefault(
                    prefs.getString(KEY_APP_DATA_SOURCE, null),
                    ProjectSource.WIZARD_CREATED
                )
            )
        )

        saveProject(context, migratedProject)
        return loadProject(context)
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 3003
    LEGACY VALUE HELPERS
    ------------------------------------------------------------
    PURPOSE
    Decodes legacy flat stored strings into Kotlin values.
    ------------------------------------------------------------
    */
    private fun decodeDoubleList(raw: String?): List<Double> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("|").mapNotNull { it.toDoubleOrNull() }
    }

    private fun decodeStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("|||").filter { it.isNotBlank() }
    }

    private fun String?.toSafeDouble(): Double {
        return this?.toDoubleOrNull() ?: 0.0
    }
}

/*
------------------------------------------------------------
END EDIT SECTIONS 3004
------------------------------------------------------------
*/