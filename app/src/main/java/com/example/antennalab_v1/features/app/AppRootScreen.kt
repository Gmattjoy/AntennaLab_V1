package com.example.antennalab_v1.features.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.antennalab_v1.domain.testing.UsbSessionManager
import com.example.antennalab_v1.features.lab.LabHomeScreen
import com.example.antennalab_v1.features.lab.LabTestTemplate
import com.example.antennalab_v1.features.lab.LabTestTemplates
import com.example.antennalab_v1.features.testing.CalibrationWizardScreen
import com.example.antennalab_v1.features.wizard.CreateAntennaWizardScreen
import com.example.antennalab_v1.model.AntennaType
import com.example.antennalab_v1.model.CalibrationRestorePolicy
import com.example.antennalab_v1.model.DesignInput
import com.example.antennalab_v1.model.LabEntryMode
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.ProjectListItem
import com.example.antennalab_v1.model.ProjectMeta
import com.example.antennalab_v1.model.ProjectSource
import com.example.antennalab_v1.model.TestHardwareProfile
import com.example.antennalab_v1.model.testing.CalibrationCaptureSource
import com.example.antennalab_v1.model.testing.CalibrationReadiness
import com.example.antennalab_v1.model.testing.CalibrationSession
import com.example.antennalab_v1.project.ProjectPageScreen
import com.example.antennalab_v1.storage.ProjectIndexManager
import com.example.antennalab_v1.storage.ProjectStorage

@Composable
fun AppRootScreen() {
    val context = LocalContext.current

    val screen = remember { mutableStateOf("home") }
    val currentProject = remember { mutableStateOf<ProjectData?>(emptyProjectPlaceholder()) }
    val activeProjectOverride = remember { mutableStateOf<ProjectData?>(null) }
    val savedProjects = remember { mutableStateOf<List<ProjectListItem>>(emptyList()) }
    val selectedLabTemplateId = remember { mutableStateOf(LabTestTemplates.getDefaultTemplate().id) }

    val testMode = remember { mutableStateOf(false) }
    val projectResumeIntoSweep = remember { mutableStateOf(false) }

    val deviceConnectionsReturnScreen = remember { mutableStateOf("settings") }
    val instrumentDetailsReturnScreen = remember { mutableStateOf("settings") }

    val startupRestoreAttempted = remember { mutableStateOf(false) }

    fun effectiveProject(): ProjectData? {
        return activeProjectOverride.value ?: currentProject.value
    }

    fun refreshProjectsList() {
        savedProjects.value = ProjectIndexManager.getAllProjects(context)
    }

    fun enterHome() {
        testMode.value = false
        projectResumeIntoSweep.value = false
        activeProjectOverride.value = null
        screen.value = "home"
    }

    fun enterLab() {
        testMode.value = false
        projectResumeIntoSweep.value = false
        activeProjectOverride.value = null
        screen.value = "lab"
    }

    fun enterProjects() {
        projectResumeIntoSweep.value = false
        activeProjectOverride.value = null
        refreshProjectsList()
        screen.value = "projects"
    }

    fun enterSettings() {
        projectResumeIntoSweep.value = false
        activeProjectOverride.value = null
        screen.value = "settings"
    }

    fun enterWizardMode() {
        currentProject.value = emptyProjectPlaceholder()
        activeProjectOverride.value = null
        testMode.value = false
        projectResumeIntoSweep.value = false
        UsbSessionManager.clearCalibrationState()
        screen.value = "wizard"
    }

    fun enterRfTestWizardMode() {
        currentProject.value = buildRfTestModeProject()
        activeProjectOverride.value = null
        testMode.value = true
        projectResumeIntoSweep.value = false
        UsbSessionManager.clearCalibrationState()
        screen.value = "wizard"
    }

    fun enterProjectAntennaTestMode() {
        val attached = currentProject.value ?: return
        val template =
            LabTestTemplates.getTemplateById(selectedLabTemplateId.value)
                ?: LabTestTemplates.getDefaultTemplate()

        val sourceProjectName =
            attached.meta.projectName.ifBlank { "Unnamed Project" }

        val templatedProject = applyTemplateToProject(attached, template)

        currentProject.value =
            templatedProject.copy(
                meta = templatedProject.meta.copy(
                    labEntryMode = LabEntryMode.PROJECT_TEMPLATE_TEST,
                    labSourceProjectName = sourceProjectName,
                    labTemplateDisplayName = template.displayName,
                    labTemplateBandLabel = template.bandLabel
                ),
                versionInfo = templatedProject.versionInfo.copy(
                    appDataSource = ProjectSource.LAB_PROJECT_TEMPLATE_TEST
                )
            )

        activeProjectOverride.value = null
        testMode.value = true
        projectResumeIntoSweep.value = false
        screen.value = "project"
    }

    fun enterUnknownDiscoveryMode() {
        activeProjectOverride.value = buildUnknownDiscoveryProject()
        testMode.value = true
        projectResumeIntoSweep.value = true
        UsbSessionManager.clearCalibrationState()
        screen.value = "project"
    }

    fun enterProjectSweepMode() {
        val resolvedProject = effectiveProject() ?: buildRfTestModeProject()
        activeProjectOverride.value = resolvedProject
        testMode.value = true
        projectResumeIntoSweep.value = true
        screen.value = "project"
    }

    SideEffect {
        AppNavigationMenuBridge.navigateTo = { destination ->
            when (destination) {
                AppMenuDestination.HOME -> enterHome()
                AppMenuDestination.LAB -> enterLab()
                AppMenuDestination.SWEEP -> enterProjectSweepMode()
                AppMenuDestination.CONNECTIONS -> {
                    deviceConnectionsReturnScreen.value = screen.value
                    screen.value = "device_connections"
                }
                AppMenuDestination.INSTRUMENT_DETAILS -> {
                    instrumentDetailsReturnScreen.value = screen.value
                    screen.value = "instrument_details"
                }
                AppMenuDestination.WIZARD -> enterWizardMode()
                AppMenuDestination.TEST_ANTENNA -> enterRfTestWizardMode()
                AppMenuDestination.PROJECTS -> enterProjects()
                AppMenuDestination.SETTINGS -> enterSettings()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!startupRestoreAttempted.value) {
            startupRestoreAttempted.value = true
            val restoredProject = ProjectStorage.loadProject(context)

            if (restoredProject.meta.projectId.isNotBlank()) {
                applyStoredCalibrationToSharedSession(context, restoredProject)
                currentProject.value = restoredProject
                testMode.value = false
                projectResumeIntoSweep.value = false
            }
        }
    }

    LaunchedEffect(screen.value) {
        if (screen.value == "projects") {
            refreshProjectsList()
        }
    }

    when (screen.value) {
        "home" -> HomeScreen(
            onOpenLab = { enterLab() },
            onOpenSettings = { enterSettings() },
            onOpenWizard = { enterWizardMode() },
            onOpenTestAntenna = { enterRfTestWizardMode() },
            onOpenProjects = { enterProjects() }
        )

        "lab" -> LabHomeScreen(
            attachedProject = currentProject.value?.takeIf { it.meta.projectName.isNotBlank() },
            selectedTemplateId = selectedLabTemplateId.value,
            onTemplateSelected = { selectedLabTemplateId.value = it },
            onBack = { enterHome() },
            onOpenConnections = {
                projectResumeIntoSweep.value = false
                deviceConnectionsReturnScreen.value = "lab"
                screen.value = "device_connections"
            },
            onOpenCalibration = {
                val projectForCalibration = effectiveProject() ?: buildRfTestModeProject()
                activeProjectOverride.value = projectForCalibration
                testMode.value = true
                projectResumeIntoSweep.value = false
                screen.value = "calibration_wizard"
            },
            onOpenProjectAntennaTest = { enterProjectAntennaTestMode() },
            onOpenUnknownAntennaDiscovery = { enterUnknownDiscoveryMode() },
            onOpenProjects = { enterProjects() }
        )

        "wizard" -> CreateAntennaWizardScreen(
            onFinishProject = { createdProject ->
                val finalizedProject =
                    if (testMode.value) {
                        createdProject.copy(
                            meta = createdProject.meta.copy(
                                projectName =
                                    if (createdProject.meta.projectName.isBlank()) {
                                        "Quick Test"
                                    } else {
                                        createdProject.meta.projectName
                                    }
                            )
                        )
                    } else {
                        createdProject
                    }

                currentProject.value = finalizedProject
                activeProjectOverride.value = null
                projectResumeIntoSweep.value = false
                screen.value = "project"
            },
            onCancel = { enterHome() }
        )

        "projects" -> LoadProjectScreen(
            savedProjects = savedProjects.value,
            onLoadProject = { projectId ->
                val loadedProject = ProjectStorage.loadProjectById(context, projectId)
                if (loadedProject != null) {
                    applyStoredCalibrationToSharedSession(context, loadedProject)
                    currentProject.value = loadedProject
                    activeProjectOverride.value = null
                    testMode.value = false
                    projectResumeIntoSweep.value = false
                    screen.value = "project"
                }
            },
            onBackHome = { enterHome() }
        )

        "settings" -> SystemMenuScreen(
            onOpenConnectionsDevices = {
                projectResumeIntoSweep.value = false
                deviceConnectionsReturnScreen.value = "settings"
                screen.value = "device_connections"
            },
            onOpenInstrumentDetails = {
                instrumentDetailsReturnScreen.value = "settings"
                screen.value = "instrument_details"
            },
            onBackHome = { enterHome() }
        )

        "device_connections" -> DeviceConnectionsScreen(
            onBack = { screen.value = deviceConnectionsReturnScreen.value },
            onOpenInstrumentDetails = {
                instrumentDetailsReturnScreen.value = "device_connections"
                screen.value = "instrument_details"
            }
        )

        "instrument_details" -> {
            val selectedHardwareName =
                effectiveProject()?.hardwareCapabilityProfile?.displayName
                    ?: UsbSessionManager.getSelectedDriverProfile()?.displayName
                    ?: "Unknown USB Analyzer"

            InstrumentDetailsScreen(
                model = InstrumentStatusUiMapper.buildDetailsUiModel(context, selectedHardwareName),
                onBack = { screen.value = instrumentDetailsReturnScreen.value },
                onOpenConnectionsDevices = {
                    deviceConnectionsReturnScreen.value = "instrument_details"
                    screen.value = "device_connections"
                }
            )
        }

        "calibration_wizard" -> {
            val project = effectiveProject() ?: buildRfTestModeProject()
            val calibrationSession = buildWizardCalibrationSession(project)

            CalibrationWizardScreen(
                calibrationSession = calibrationSession,
                onSessionChange = { session ->
                    UsbSessionManager.registerCalibrationSession(session)
                },
                onFinish = {
                    screen.value = "project"
                    projectResumeIntoSweep.value = true
                },
                onCancel = {
                    screen.value = "lab"
                }
            )
        }

        "project" -> {
            effectiveProject()?.let { project ->
                ProjectPageScreen(
                    project = project,
                    startInTesting = testMode.value,
                    startInSweep = projectResumeIntoSweep.value,
                    onSweepResumeConsumed = {
                        projectResumeIntoSweep.value = false
                    },
                    onProjectLoaded = { loadedProject ->
                        applyStoredCalibrationToSharedSession(context, loadedProject)
                        currentProject.value = loadedProject
                        activeProjectOverride.value = null
                    },
                    onProjectChanged = { updatedProject ->
                        if (activeProjectOverride.value != null) {
                            activeProjectOverride.value = updatedProject
                        } else {
                            currentProject.value = updatedProject
                        }
                    },
                    onGoHome = { enterHome() },
                    onOpenProjects = { enterProjects() },
                    onOpenSystemDevices = {
                        projectResumeIntoSweep.value = false
                        deviceConnectionsReturnScreen.value = "project"
                        screen.value = "device_connections"
                    },
                    onOpenSystemDevicesFromSweep = {
                        projectResumeIntoSweep.value = true
                        deviceConnectionsReturnScreen.value = "project"
                        screen.value = "device_connections"
                    }
                )
            }
        }
    }
}

private fun buildWizardCalibrationSession(
    project: ProjectData
): CalibrationSession {
    val selectedHardwareName =
        UsbSessionManager.getLatestInstrumentSessionState()?.selectedHardwareName
            ?: project.hardwareCapabilityProfile.displayName

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
        CalibrationSession(
            hardwareDisplayName = selectedHardwareName,
            startFrequencyMHz = project.designInput.targetFrequencyMHz - 0.5,
            endFrequencyMHz = project.designInput.targetFrequencyMHz + 0.5,
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
}

private fun buildRfTestModeProject(): ProjectData {
    return ProjectData(
        meta = ProjectMeta(
            projectName = "RF Test Mode"
        ),
        designInput = DesignInput(
            antennaType = AntennaType.OTHER,
            targetFrequencyMHz = 14.2
        ),
        testHardwareProfile = TestHardwareProfile.NANOVNA_H4
    )
}

private fun buildUnknownDiscoveryProject(): ProjectData {
    return ProjectData(
        meta = ProjectMeta(
            projectName = "Unknown Antenna Discovery",
            labEntryMode = LabEntryMode.UNKNOWN_DISCOVERY
        ),
        designInput = DesignInput(
            antennaType = AntennaType.OTHER,
            targetFrequencyMHz = 14.2
        ),
        versionInfo = com.example.antennalab_v1.model.VersionInfo(
            appDataSource = ProjectSource.LAB_UNKNOWN_DISCOVERY
        ),
        testHardwareProfile = TestHardwareProfile.NANOVNA_H4
    )
}

private fun emptyProjectPlaceholder(): ProjectData {
    return ProjectData(
        meta = ProjectMeta(
            projectName = ""
        ),
        designInput = DesignInput(
            antennaType = AntennaType.OTHER,
            targetFrequencyMHz = 14.2
        ),
        testHardwareProfile = TestHardwareProfile.NANOVNA_H4
    )
}

private fun applyTemplateToProject(
    baseProject: ProjectData,
    template: LabTestTemplate
): ProjectData {
    return baseProject.copy(
        meta = baseProject.meta.copy(
            projectName = if (baseProject.meta.projectName.isBlank()) template.displayName else baseProject.meta.projectName
        ),
        designInput = baseProject.designInput.copy(
            antennaType = resolveAntennaType(template.antennaTypeKey),
            targetFrequencyMHz = template.targetFrequencyMHz
        )
    )
}

private fun resolveAntennaType(
    antennaTypeKey: String
): AntennaType {
    return enumValues<AntennaType>()
        .firstOrNull { it.name.equals(antennaTypeKey, ignoreCase = true) }
        ?: AntennaType.OTHER
}

private fun applyStoredCalibrationToSharedSession(
    context: android.content.Context,
    project: ProjectData
) {
    val storedCalibration = project.storedCalibrationOrNull
    val selectedHardwareName = project.hardwareCapabilityProfile.displayName

    when {
        storedCalibration == null -> {
            UsbSessionManager.clearCalibrationState()
        }

        project.calibrationData.restorePolicy == CalibrationRestorePolicy.DO_NOT_RESTORE -> {
            UsbSessionManager.clearCalibrationState()
        }

        !storedCalibration.matchesHardwareDisplayName(selectedHardwareName) -> {
            UsbSessionManager.clearCalibrationState()
        }

        project.calibrationData.restorePolicy == CalibrationRestorePolicy.RESTORE_IF_COMPATIBLE &&
                !storedCalibration.isCompatibleWithRequestedRange(
                    selectedHardwareName = selectedHardwareName,
                    requestedStartMHz = project.designInput.targetFrequencyMHz,
                    requestedEndMHz = project.designInput.targetFrequencyMHz
                ) -> {
            UsbSessionManager.clearCalibrationState()
        }

        else -> {
            UsbSessionManager.registerCalibrationSession(
                storedCalibration.copy(
                    captureSource = CalibrationCaptureSource.RESTORED_FROM_PROJECT,
                    capturedSessionKey = null
                )
            )

            UsbSessionManager.refreshCurrentSessionState(
                context = context,
                selectedHardwareName = selectedHardwareName
            )
        }
    }
}