package com.example.antennalab_v1.project

/*
########################################################################
FILE: ProjectPageScreen.kt
PACKAGE: com.example.antennalab_v1.project
LAYER: UI / Project Workspace Hub

LAST UPDATED 08/04/2026 23:10

SYSTEM ROLE
Primary workspace hub for a saved or active antenna project.

CURRENT DEVELOPMENT ROLE
This file now moves the project workspace closer to the agreed project
side architecture:

• engineering dashboard remains the top summary block
• overview focuses on quick identity / classification inputs
• duplicated Project Overview block is removed
• sweep history is visible from the project page
• testing remains the entry point into sweep/calibration tools

SAFE EDIT AREA
- add richer overview identity fields later
- add sweep-history filtering later
- add per-entry sweep detail dialogs later
########################################################################
*/

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.domain.testing.HardwareSweepCapability
import com.example.antennalab_v1.domain.testing.UsbSessionManager
import com.example.antennalab_v1.domain.testing.UsbVnaTransportStatus
import com.example.antennalab_v1.features.app.AppTopRightMenu
import com.example.antennalab_v1.features.testing.CalibrationWizardScreen
import com.example.antennalab_v1.features.testing.SweepGraphScreen
import com.example.antennalab_v1.features.workspace.DesignWorkspaceScreen
import com.example.antennalab_v1.model.AntennaClassification
import com.example.antennalab_v1.model.AvailablePartsProfile
import com.example.antennalab_v1.model.BuildCostProfile
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.ProjectSection
import com.example.antennalab_v1.model.ProjectSweepHistoryEntry
import com.example.antennalab_v1.model.ProjectSweepHistoryMode
import com.example.antennalab_v1.model.TestHardwareProfile
import com.example.antennalab_v1.model.testing.CalibrationReadiness
import com.example.antennalab_v1.model.testing.CalibrationSession
import com.example.antennalab_v1.storage.ProjectStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectPageScreen(
    project: ProjectData,
    startInTesting: Boolean = false,
    startInSweep: Boolean = false,
    onSweepResumeConsumed: () -> Unit = {},
    onProjectLoaded: (ProjectData) -> Unit = {},
    onProjectChanged: (ProjectData) -> Unit = {},
    onGoHome: () -> Unit = {},
    onOpenProjects: () -> Unit = {},
    onOpenSystemDevices: () -> Unit = {},
    onOpenSystemDevicesFromSweep: () -> Unit = {}
) {
    val context = LocalContext.current

    var section by remember {
        mutableStateOf(
            if (startInTesting) ProjectSection.TESTING
            else ProjectSection.OVERVIEW
        )
    }

    var showSweep by remember { mutableStateOf(false) }
    var showCalibrationWizard by remember { mutableStateOf(false) }

    var workingProject by remember { mutableStateOf(project) }
    var calibrationSession by remember {
        mutableStateOf(buildWizardCalibrationSession(project))
    }

    var actionMessage by remember { mutableStateOf("") }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var saveAsName by remember { mutableStateOf("") }

    LaunchedEffect(project.meta.projectId) {
        workingProject = project
        calibrationSession = buildWizardCalibrationSession(project)
        showCalibrationWizard = false
        showSweep = false
        actionMessage = ""
        showSaveAsDialog = false
        saveAsName = project.meta.projectName

        if (section != ProjectSection.TESTING && section != ProjectSection.DESIGN) {
            section = ProjectSection.OVERVIEW
        }
    }

    LaunchedEffect(startInTesting) {
        if (startInTesting) {
            section = ProjectSection.TESTING
        }
    }

    LaunchedEffect(startInSweep, project.meta.projectId) {
        if (startInSweep) {
            section = ProjectSection.TESTING
            showSweep = true
            onSweepResumeConsumed()
        }
    }

    if (showSaveAsDialog) {
        AlertDialog(
            onDismissRequest = { showSaveAsDialog = false },
            title = { Text("Save As") },
            text = {
                OutlinedTextField(
                    value = saveAsName,
                    onValueChange = { saveAsName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("New Project Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmedName = saveAsName.trim().ifBlank { "Default" }

                        val duplicatedProject =
                            workingProject.copy(
                                meta = workingProject.meta.copy(
                                    projectId = "",
                                    projectName = trimmedName
                                )
                            )

                        ProjectStorage.saveProject(context, duplicatedProject)
                        actionMessage = "Project saved as \"$trimmedName\". Open it from Projects to continue with that copy."
                        showSaveAsDialog = false
                    }
                ) {
                    Text("Save As")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveAsDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCalibrationWizard) {
        CalibrationWizardScreen(
            calibrationSession = calibrationSession,
            onSessionChange = { updatedSession ->
                calibrationSession = updatedSession
                UsbSessionManager.registerCalibrationSession(updatedSession)
            },
            onFinish = {
                calibrationSession = buildWizardCalibrationSession(workingProject)
                showCalibrationWizard = false
                actionMessage = "Calibration updated."
            },
            onCancel = {
                calibrationSession = buildWizardCalibrationSession(workingProject)
                showCalibrationWizard = false
            }
        )
        return
    }

    if (showSweep) {
        SweepGraphScreen(
            project = workingProject,
            onBack = {
                val projectBeforeReturn = workingProject
                val storedProject =
                    projectBeforeReturn.meta.projectId
                        .takeIf { it.isNotBlank() }
                        ?.let { projectId ->
                            ProjectStorage.loadProjectById(context, projectId)
                        }

                if (storedProject != null) {
                    val mergedProject = mergeProjectReturnFromSweep(
                        localProject = projectBeforeReturn,
                        storedProject = storedProject
                    )

                    val historyCountIncreased =
                        storedProject.sweepHistory.size > projectBeforeReturn.sweepHistory.size

                    val discoveryApplied =
                        storedProject.discoverySnapshot != projectBeforeReturn.discoverySnapshot ||
                                storedProject.antennaClassification != projectBeforeReturn.antennaClassification

                    workingProject = mergedProject
                    onProjectChanged(mergedProject)

                    actionMessage = when {
                        historyCountIncreased -> "Project sweep history updated from the latest sweep."
                        discoveryApplied -> "Project updated from the latest discovery result."
                        else -> actionMessage
                    }
                }

                showSweep = false
            },
            onOpenSystemDevices = onOpenSystemDevicesFromSweep
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = workingProject.meta.projectName,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    AppTopRightMenu()
                }
            )
        }
    ) { padding ->

        if (section == ProjectSection.DESIGN) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProjectTopActionsCard(
                    project = workingProject,
                    actionMessage = actionMessage,
                    onSaveProject = { projectToSave ->
                        ProjectStorage.saveProject(context, projectToSave)
                    },
                    onSaveCompleted = { message ->
                        actionMessage = message
                    },
                    onOpenSaveAs = {
                        saveAsName = workingProject.meta.projectName
                        showSaveAsDialog = true
                    },
                    onOpenProjects = onOpenProjects
                )

                WorkspaceTabs(section) { section = it }

                DesignWorkspaceScreen(
                    project = workingProject,
                    onBack = { section = ProjectSection.OVERVIEW },
                    onApplyChanges = { updatedProject ->
                        workingProject = updatedProject
                        onProjectChanged(updatedProject)
                        actionMessage = "Design changes applied locally. Save project to keep them."
                    }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProjectTopActionsCard(
                    project = workingProject,
                    actionMessage = actionMessage,
                    onSaveProject = { projectToSave ->
                        ProjectStorage.saveProject(context, projectToSave)
                    },
                    onSaveCompleted = { message ->
                        actionMessage = message
                    },
                    onOpenSaveAs = {
                        saveAsName = workingProject.meta.projectName
                        showSaveAsDialog = true
                    },
                    onOpenProjects = onOpenProjects
                )

                WorkspaceTabs(section) { section = it }

                when (section) {
                    ProjectSection.OVERVIEW -> {
                        EngineeringDashboardCard(workingProject)

                        ProjectIdentityClassificationCard(
                            project = workingProject,
                            onClassificationSelected = { selectedClassification ->
                                val updatedProject = workingProject.copy(
                                    antennaClassification = selectedClassification
                                )
                                workingProject = updatedProject
                                onProjectChanged(updatedProject)
                                actionMessage = "Antenna classification updated locally. Save project to keep it."
                            }
                        )

                        SweepHistorySectionCard(
                            sweepHistory = workingProject.sweepHistory,
                            latestDiscoverySnapshotLabel = workingProject.discoverySnapshot?.summaryLabel
                        )

                        HardwareSelectionCard(
                            selected = workingProject.testHardwareProfile,
                            onSelect = {
                                val updatedProject =
                                    workingProject.copy(
                                        testHardwareProfile = it
                                    )

                                workingProject = updatedProject
                                calibrationSession = buildFreshCalibrationSession(
                                    project = updatedProject
                                )
                                onProjectChanged(updatedProject)
                                actionMessage = "Hardware selection updated. Save project to keep it."
                            }
                        )
                    }

                    ProjectSection.MATERIALS ->
                        MaterialsSectionCard(workingProject)

                    ProjectSection.TESTING ->
                        TestingSection(
                            project = workingProject,
                            calibrationSession = calibrationSession,
                            onStartCalibration = {
                                calibrationSession = buildWizardCalibrationSession(workingProject)
                                showCalibrationWizard = true
                            },
                            onRunDemoSweep = { showSweep = true },
                            onRunRealSweep = {
                                calibrationSession = buildWizardCalibrationSession(workingProject)
                                showSweep = true
                            },
                            onOpenSystemDevices = onOpenSystemDevices
                        )

                    ProjectSection.NOTES ->
                        NotesSectionCard(
                            notes = workingProject.meta.notes,
                            onNotesChange = { updatedNotes ->
                                val updatedProject = workingProject.copy(
                                    meta = workingProject.meta.copy(
                                        notes = updatedNotes
                                    )
                                )
                                workingProject = updatedProject
                                onProjectChanged(updatedProject)
                                actionMessage = "Notes updated locally. Save project to keep them."
                            }
                        )

                    ProjectSection.DESIGN -> {
                    }
                }
            }
        }
    }
}

private fun mergeProjectReturnFromSweep(
    localProject: ProjectData,
    storedProject: ProjectData
): ProjectData {
    return localProject.copy(
        antennaClassification = storedProject.antennaClassification,
        discoverySnapshot = storedProject.discoverySnapshot,
        sweepHistory = storedProject.sweepHistory,
        testData = storedProject.testData
    )
}

private fun buildWizardCalibrationSession(
    project: ProjectData
): CalibrationSession {
    val calibrationState = UsbSessionManager.getLatestInstrumentCalibrationState()
    val sharedCalibration = calibrationState.calibrationSession

    return if (
        sharedCalibration != null &&
        (
                calibrationState.readiness == CalibrationReadiness.VALID ||
                        calibrationState.readiness == CalibrationReadiness.IN_PROGRESS
                )
    ) {
        sharedCalibration
    } else {
        buildFreshCalibrationSession(project)
    }
}

private fun buildFreshCalibrationSession(
    project: ProjectData
): CalibrationSession {
    val selectedHardwareName =
        UsbSessionManager.getLatestInstrumentSessionState()?.selectedHardwareName
            ?: formatHardware(project.testHardwareProfile)

    val startFrequencyMHz =
        project.hardwareCapabilityProfile.minFrequencyHz / 1_000_000.0

    val endFrequencyMHz =
        project.hardwareCapabilityProfile.maxFrequencyHz / 1_000_000.0

    return CalibrationSession(
        hardwareDisplayName = selectedHardwareName,
        startFrequencyMHz = startFrequencyMHz,
        endFrequencyMHz = endFrequencyMHz,
        openCaptured = false,
        shortCaptured = false,
        loadCaptured = false,
        timestampLabel = "Not captured yet",
        capturedAtEpochMs = 0L,
        capturedProtocolFamily = null,
        capturedInstrumentIdentityText = null,
        capturedSessionKey = null
    )
}

@Composable
private fun ProjectTopActionsCard(
    project: ProjectData,
    actionMessage: String,
    onSaveProject: (ProjectData) -> Unit,
    onSaveCompleted: (String) -> Unit,
    onOpenSaveAs: () -> Unit,
    onOpenProjects: () -> Unit
) {
    SectionCard(
        title = "Project Actions"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallActionButton(
                text = "Save",
                onClick = {
                    onSaveProject(project)
                    onSaveCompleted("Project saved.")
                }
            )

            SmallActionButton(
                text = "Save As",
                onClick = onOpenSaveAs
            )

            SmallActionButton(
                text = "Projects",
                onClick = onOpenProjects
            )
        }

        if (actionMessage.isNotBlank()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
            )

            Text(
                text = actionMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WorkspaceTabs(
    section: ProjectSection,
    onSelect: (ProjectSection) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabButton("Overview", section == ProjectSection.OVERVIEW) {
                onSelect(ProjectSection.OVERVIEW)
            }

            TabButton("Design", section == ProjectSection.DESIGN) {
                onSelect(ProjectSection.DESIGN)
            }

            TabButton("Materials", section == ProjectSection.MATERIALS) {
                onSelect(ProjectSection.MATERIALS)
            }

            TabButton("Testing", section == ProjectSection.TESTING) {
                onSelect(ProjectSection.TESTING)
            }

            TabButton("Notes", section == ProjectSection.NOTES) {
                onSelect(ProjectSection.NOTES)
            }
        }
    }
}

@Composable
private fun TabButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor =
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            contentColor =
                if (active) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color =
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun SmallActionButton(
    text: String,
    onClick: () -> Unit
) {
    Button(onClick = onClick) {
        Text(text)
    }
}

@Composable
fun EngineeringDashboardCard(project: ProjectData) {
    val nextAction = buildNextAction(project)
    val readiness = buildReadinessSummary(project)
    val userView = buildUserViewSummary(project)

    SectionCard(title = "Engineering Dashboard", highlighted = true) {
        Text("Next Recommended Action", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(nextAction, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
        Text("Project Snapshot", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        DataRow("Type", project.designInput.antennaType.name)
        DataRow("Classification", formatClassification(project.antennaClassification))
        DataRow("Target", "${project.designInput.targetFrequencyMHz} MHz")
        DataRow("Status", project.meta.projectStatus.name)
        DataRow("Hardware", formatHardware(project.testHardwareProfile))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
        Text("Build Readiness", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(readiness, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
        Text("User View", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(userView, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProjectIdentityClassificationCard(
    project: ProjectData,
    onClassificationSelected: (AntennaClassification) -> Unit
) {
    SectionCard(title = "Project Identity") {
        DataRow("Project Name", project.meta.projectName)
        DataRow("Design Type", project.designInput.antennaType.name)
        DataRow("Current Classification", formatClassification(project.antennaClassification))

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
        Text(
            text = "Antenna Classification",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Use this quick classification near the top of Overview so the project identity stays clear without duplicating the engineering dashboard.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            antennaClassificationChoices().forEach { classification ->
                ClassificationButton(
                    label = formatClassification(classification),
                    active = classification == project.antennaClassification,
                    onClick = { onClassificationSelected(classification) }
                )
            }
        }
    }
}

@Composable
private fun ClassificationButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor =
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            contentColor =
                if (active) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color =
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SweepHistorySectionCard(
    sweepHistory: List<ProjectSweepHistoryEntry>,
    latestDiscoverySnapshotLabel: String?
) {
    SectionCard(title = "Sweep History") {
        latestDiscoverySnapshotLabel?.takeIf { it.isNotBlank() }?.let { snapshotLabel ->
            Text(
                text = "Latest discovery snapshot: $snapshotLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
        }

        if (sweepHistory.isEmpty()) {
            Text(
                text = "No sweep history recorded yet. Run a project-linked sweep or apply a discovery result to start building history.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            sweepHistory
                .sortedByDescending { it.recordedAtEpochMs }
                .forEachIndexed { index, entry ->
                    SweepHistoryEntryCard(entry = entry)

                    if (index != sweepHistory.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
                    }
                }
        }
    }
}

@Composable
private fun SweepHistoryEntryCard(entry: ProjectSweepHistoryEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DataRow("Recorded", formatTimestamp(entry.recordedAtEpochMs))
        DataRow("Mode", formatSweepHistoryMode(entry.mode))
        DataRow("Hardware", entry.hardwareName.ifBlank { "Unknown" })
        DataRow(
            "Sweep Range",
            if (entry.hasUsableSweepRange) {
                String.format(Locale.US, "%.3f MHz → %.3f MHz", entry.sweepStartMHz, entry.sweepEndMHz)
            } else {
                "Unknown"
            }
        )
        DataRow("Best Frequency", String.format(Locale.US, "%.3f MHz", entry.bestFrequencyMHz))
        DataRow("Best SWR", String.format(Locale.US, "%.3f", entry.bestSwr))
        DataRow(
            "Return Loss",
            entry.returnLossDb?.let { String.format(Locale.US, "%.3f dB", it) } ?: "Not available"
        )

        if (entry.label.isNotBlank()) {
            DataRow("Label", entry.label)
        }

        if (entry.note.isNotBlank()) {
            Text(
                text = entry.note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MaterialsSectionCard(project: ProjectData) {
    val elementLengthsMm = project.calculatedDesign.elementLengthsMm
    val elementSpacingMm = project.calculatedDesign.elementSpacingMm
    val totalConductorLengthMm = elementLengthsMm.sum()

    SectionCard(title = "Materials") {
        Text("Material Properties", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        DataRow("Conductor Material", project.materialConfig.conductorMaterial.name)
        DataRow("Conductor Diameter", "${project.materialConfig.conductorDiameterMm} mm")
        DataRow("Boom Material", project.materialConfig.boomMaterial.name)
        DataRow("Support Material", project.materialConfig.supportMaterial.name)
        DataRow("Connector", project.materialConfig.connectorType.name)
        DataRow("Feedline", project.materialConfig.feedlineType.name)

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
        Text("Required Build Dimensions", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

        if (elementLengthsMm.isNotEmpty()) {
            DataRow("Total Conductor Length", formatMillimetres(totalConductorLengthMm))
            elementLengthsMm.forEachIndexed { index, lengthMm ->
                DataRow("Element ${index + 1} Length", formatMillimetres(lengthMm))
            }
        } else {
            Text("No calculated element lengths are stored yet for this project.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (elementSpacingMm.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
            Text("Element Spacing", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            elementSpacingMm.forEachIndexed { index, spacingMm ->
                DataRow("Spacing ${index + 1}", formatMillimetres(spacingMm))
            }
        }

        if (project.calculatedDesign.boomLengthMm > 0.0) {
            DataRow("Boom Length", formatMillimetres(project.calculatedDesign.boomLengthMm))
        }

        if (project.calculatedDesign.feedPointGapMm > 0.0) {
            DataRow("Feed Point Gap", formatMillimetres(project.calculatedDesign.feedPointGapMm))
        }

        if (project.calculatedDesign.matchingMethod.isNotBlank()) {
            DataRow("Matching Method", project.calculatedDesign.matchingMethod)
        }

        if (project.materialConfig.buildNotes.isNotBlank()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
            Text("Build Notes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(project.materialConfig.buildNotes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NotesSectionCard(
    notes: String,
    onNotesChange: (String) -> Unit
) {
    SectionCard(title = "Notes") {
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 6,
            label = { Text("Project Notes") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )

        Text("Edit notes here, then use Save near the top to keep them.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HardwareSelectionCard(
    selected: TestHardwareProfile,
    onSelect: (TestHardwareProfile) -> Unit
) {
    SectionCard(title = "Test Hardware") {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HardwareButton("NanoVNA-H4", selected == TestHardwareProfile.NANOVNA_H4) { onSelect(TestHardwareProfile.NANOVNA_H4) }
            HardwareButton("LiteVNA64 v0.3.3", selected == TestHardwareProfile.LITEVNA64_V0_3_3) { onSelect(TestHardwareProfile.LITEVNA64_V0_3_3) }
        }
    }
}

@Composable
private fun HardwareButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TestingSection(
    project: ProjectData,
    calibrationSession: CalibrationSession,
    onStartCalibration: () -> Unit,
    onRunDemoSweep: () -> Unit,
    onRunRealSweep: () -> Unit,
    onOpenSystemDevices: () -> Unit
) {
    val realSweepStatus: UsbVnaTransportStatus = HardwareSweepCapability.getRealSweepStatus()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TestingFastPathCard(realSweepStatus = realSweepStatus, onOpenSystemDevices = onOpenSystemDevices)
        SystemConnectionStatusCard(project = project, realSweepStatus = realSweepStatus, onOpenSystemDevices = onOpenSystemDevices)
        CalibrationStatusCard(calibrationSession = calibrationSession, onStartCalibration = onStartCalibration)
        MeasurementActionsCard(realSweepStatus = realSweepStatus, onRunDemoSweep = onRunDemoSweep, onRunRealSweep = onRunRealSweep)
        RecommendedWorkflowCard(realSweepStatus = realSweepStatus)
    }
}

@Composable
private fun TestingFastPathCard(
    realSweepStatus: UsbVnaTransportStatus,
    onOpenSystemDevices: () -> Unit
) {
    SectionCard(title = "Testing Fast Path", highlighted = true) {
        Text("System Devices → confirm USB ready → calibration if needed → Run Real Sweep", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
        Text(
            if (realSweepStatus.ready) {
                "USB transport is currently ready for real sweep entry."
            } else {
                "USB transport is not ready yet. Open System Devices to continue setup."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        PrimaryActionButton(text = "Open System Devices", onClick = onOpenSystemDevices)
    }
}

@Composable
private fun SystemConnectionStatusCard(
    project: ProjectData,
    realSweepStatus: UsbVnaTransportStatus,
    onOpenSystemDevices: () -> Unit
) {
    SectionCard(title = "System Connection Status") {
        DataRow("Selected Hardware", formatHardware(project.testHardwareProfile))
        DataRow("Real Sweep Availability", if (realSweepStatus.ready) "Ready" else "Not Ready")
        Text(realSweepStatus.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SecondaryActionButton(text = "Open System Devices", onClick = onOpenSystemDevices)
    }
}

@Composable
private fun CalibrationStatusCard(
    calibrationSession: CalibrationSession,
    onStartCalibration: () -> Unit
) {
    SectionCard(title = "Calibration Status") {
        DataRow("Hardware", calibrationSession.hardwareDisplayName)
        DataRow("Calibration Range", String.format("%.3f MHz → %.3f MHz", calibrationSession.startFrequencyMHz, calibrationSession.endFrequencyMHz))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
        DataRow("Completion State", calibrationSession.completionState.name)
        DataRow("Completed Steps", "${calibrationSession.completedStepCount} / 3")
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
        DataRow("OPEN", if (calibrationSession.openCaptured) "Captured" else "Pending")
        DataRow("SHORT", if (calibrationSession.shortCaptured) "Captured" else "Pending")
        DataRow("LOAD", if (calibrationSession.loadCaptured) "Captured" else "Pending")
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
        Text("Professional measurement mode should complete OPEN / SHORT / LOAD calibration before real hardware sweeps.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PrimaryActionButton(text = "Start Calibration Wizard", onClick = onStartCalibration)
    }
}

@Composable
private fun MeasurementActionsCard(
    realSweepStatus: UsbVnaTransportStatus,
    onRunDemoSweep: () -> Unit,
    onRunRealSweep: () -> Unit
) {
    SectionCard(title = "Measurement") {
        Text("Choose how you want to enter the sweep workflow for this project.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PrimaryActionButton(text = "Run Demo Sweep", onClick = onRunDemoSweep)
        PrimaryActionButton(text = "Run Real Sweep", enabled = realSweepStatus.ready, onClick = onRunRealSweep)
    }
}

@Composable
private fun RecommendedWorkflowCard(
    realSweepStatus: UsbVnaTransportStatus
) {
    SectionCard(title = "Recommended Workflow") {
        Text("• Use System → Connections / Devices to prepare hardware", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text("• Confirm permission and USB session are ready", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text("• Run analyzer identity test at system level", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text("• Return to the project for calibration and sweep work", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text("• Use demo sweep if hardware is not ready yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
        Text(
            if (realSweepStatus.ready) {
                "Hardware transport is ready for project-based real sweep work."
            } else {
                "Hardware transport is not ready yet. Open System Devices to continue setup."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    highlighted: Boolean = false,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (highlighted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
            content()
        }
    }
}

@Composable
private fun DataRow(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SecondaryActionButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

private fun formatBudget(profile: BuildCostProfile) =
    when (profile) {
        BuildCostProfile.BUDGET -> "Budget"
        BuildCostProfile.STANDARD -> "Standard"
        BuildCostProfile.PREMIUM -> "Premium"
    }

private fun formatParts(profile: AvailablePartsProfile) =
    when (profile) {
        AvailablePartsProfile.MINIMAL -> "Minimal"
        AvailablePartsProfile.SOME_EXISTING_PARTS -> "Some Existing Parts"
        AvailablePartsProfile.WELL_STOCKED_WORKSHOP -> "Well-Stocked Workshop"
    }

private fun formatHardware(profile: TestHardwareProfile) =
    when (profile) {
        TestHardwareProfile.NANOVNA_H4 -> "NanoVNA-H4"
        TestHardwareProfile.LITEVNA64_V0_3_3 -> "LiteVNA64 v0.3.3"
    }

private fun formatClassification(classification: AntennaClassification): String {
    return when (classification) {
        AntennaClassification.NOT_YET_CLASSIFIED -> "Not Yet Classified"
        AntennaClassification.DIPOLE -> "Dipole"
        AntennaClassification.MONOPOLE -> "Monopole"
        AntennaClassification.YAGI -> "Yagi"
        AntennaClassification.LOOP -> "Loop"
        AntennaClassification.GROUND_PLANE -> "Ground Plane"
        AntennaClassification.LONG_WIRE -> "Long Wire"
        AntennaClassification.VERTICAL -> "Vertical"
        AntennaClassification.OTHER -> "Other"
    }
}

private fun antennaClassificationChoices(): List<AntennaClassification> {
    return listOf(
        AntennaClassification.NOT_YET_CLASSIFIED,
        AntennaClassification.DIPOLE,
        AntennaClassification.MONOPOLE,
        AntennaClassification.YAGI,
        AntennaClassification.LOOP,
        AntennaClassification.GROUND_PLANE,
        AntennaClassification.LONG_WIRE,
        AntennaClassification.VERTICAL,
        AntennaClassification.OTHER
    )
}

private fun formatSweepHistoryMode(mode: ProjectSweepHistoryMode): String {
    return when (mode) {
        ProjectSweepHistoryMode.PROJECT_TEST -> "Project Test"
        ProjectSweepHistoryMode.DISCOVERY_APPLIED -> "Discovery Applied"
    }
}

private fun formatTimestamp(epochMs: Long): String {
    if (epochMs <= 0L) return "Unknown"
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(epochMs))
}

private fun formatMillimetres(valueMm: Double): String {
    return if (valueMm >= 1000.0) {
        String.format("%.1f mm (%.3f m)", valueMm, valueMm / 1000.0)
    } else {
        String.format("%.1f mm", valueMm)
    }
}

private fun buildNextAction(project: ProjectData): String {
    return when {
        project.designInput.targetFrequencyMHz <= 0.0 ->
            "Set or confirm the target frequency before continuing."

        project.meta.projectStatus.name.contains("DRAFT", ignoreCase = true) ->
            "Review the design workspace, then move to testing when the build is ready."

        else ->
            "Confirm hardware selection and run a sweep to compare the build against the target design."
    }
}

private fun buildReadinessSummary(project: ProjectData): String {
    val partsText = formatParts(project.availablePartsProfile)
    val budgetText = formatBudget(project.buildCostProfile)
    return "Budget: $budgetText. Parts: $partsText. Hardware prepared: ${formatHardware(project.testHardwareProfile)}."
}

private fun buildUserViewSummary(project: ProjectData): String {
    return when (project.availablePartsProfile) {
        AvailablePartsProfile.MINIMAL ->
            "Good for first-time builders. Keep the setup simple and verify the design with a basic sweep."
        AvailablePartsProfile.SOME_EXISTING_PARTS ->
            "Good balance for makers and hobby operators. You likely have enough parts to prototype and tune."
        AvailablePartsProfile.WELL_STOCKED_WORKSHOP ->
            "Strong engineering workflow potential. Suitable for rapid iteration, comparison testing, and refinement."
    }
}
