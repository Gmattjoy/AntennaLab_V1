package com.example.antennalab_v1.features.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.antennalab_v1.BuildConfig
import com.example.antennalab_v1.domain.testing.EffectiveHardwareResolver
import com.example.antennalab_v1.domain.testing.UsbSessionManager
import com.example.antennalab_v1.features.lab.LabHomeScreen
import com.example.antennalab_v1.features.lab.LabTestTemplates
import com.example.antennalab_v1.features.testing.CalibrationWizardScreen
import com.example.antennalab_v1.features.wizard.CreateAntennaWizardScreen
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.ProjectListItem
import com.example.antennalab_v1.model.testing.CalibrationCaptureSource
import com.example.antennalab_v1.model.testing.CalibrationSession
import com.example.antennalab_v1.project.ProjectPageScreen
import com.example.antennalab_v1.storage.ProjectIndexManager
import com.example.antennalab_v1.storage.ProjectStorage

@Composable
fun AppRootScreen() {
    val context = LocalContext.current

    val screen = remember { mutableStateOf("home") }
    val currentProject = remember { mutableStateOf<ProjectData?>(AppRootController.emptyProjectPlaceholder()) }
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
        currentProject.value = AppRootController.emptyProjectPlaceholder()
        activeProjectOverride.value = null
        testMode.value = false
        projectResumeIntoSweep.value = false
        UsbSessionManager.clearCalibrationState()
        screen.value = "wizard"
    }

    fun enterRfTestWizardMode() {
        currentProject.value = AppRootController.buildRfTestModeProject()
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

        currentProject.value = AppRootController.buildProjectTemplateTestProject(attached, template)

        activeProjectOverride.value = null
        testMode.value = true
        projectResumeIntoSweep.value = false
        screen.value = "project"
    }

    fun enterUnknownDiscoveryMode() {
        activeProjectOverride.value = AppRootController.buildUnknownDiscoveryProject()
        testMode.value = true
        projectResumeIntoSweep.value = true
        UsbSessionManager.clearCalibrationState()
        screen.value = "project"
    }

    fun enterProjectSweepMode() {
        val resolvedProject = effectiveProject() ?: AppRootController.buildRfTestModeProject()
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
                val projectForCalibration = effectiveProject() ?: AppRootController.buildRfTestModeProject()
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
                    AppRootController.finalizeWizardProject(createdProject, testMode.value)

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
                effectiveProject()
                    ?.let { EffectiveHardwareResolver.resolveCapabilityProfileForProject(it).displayName }
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
            val project = effectiveProject() ?: AppRootController.buildRfTestModeProject()
            val calibrationSession = AppRootController.buildWizardCalibrationSession(project)

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

private fun applyStoredCalibrationToSharedSession(
    context: android.content.Context,
    project: ProjectData
) {
    // DEBUG: one greppable line per project load (`adb logcat -s CalRestore`) recording
    // which predicate decided the load. Without it, a silently-cleared calibration is
    // indistinguishable from one that was never stored.
    val decision = AppRootController.decideCalibrationRestore(project) { message ->
        if (BuildConfig.DEBUG) android.util.Log.i("CalRestore", message)
    }

    when (decision) {
        CalibrationRestoreAction.CLEAR -> {
            UsbSessionManager.clearCalibrationState()
        }

        CalibrationRestoreAction.RESTORE -> {
            val storedCalibration = project.storedCalibrationOrNull ?: return
            val selectedHardwareName =
                EffectiveHardwareResolver.resolveCapabilityProfileForProject(project).displayName

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