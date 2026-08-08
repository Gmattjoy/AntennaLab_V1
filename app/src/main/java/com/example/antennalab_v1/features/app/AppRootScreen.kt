package com.example.antennalab_v1.features.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.antennalab_v1.domain.testing.EffectiveHardwareResolver
import com.example.antennalab_v1.domain.testing.UsbSessionManager
import com.example.antennalab_v1.features.lab.LabHomeScreen
import com.example.antennalab_v1.features.lab.LabTestTemplates
import com.example.antennalab_v1.features.testing.CalibrationWizardScreen
import com.example.antennalab_v1.features.wizard.CreateAntennaWizardScreen
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.ProjectListItem
import com.example.antennalab_v1.project.ProjectPageScreen
import com.example.antennalab_v1.storage.ProjectIndexManager
import com.example.antennalab_v1.storage.ProjectStorage
import com.example.antennalab_v1.storage.SettingsRepository

@Composable
fun AppRootScreen() {
    val context = LocalContext.current

    /*
    Read once per composition and passed into the project factories, which stay
    Context-free by design. SettingsRepository caches, so this is a field read
    after the first call in the process.

    KNOWN LIMITATION, correct for now: current() returns a plain value, not
    observable state, so changing a default mid-session will not recompose this
    screen. That is fine while the factories only seed NEW project-less sessions
    and there is no settings UI to change them from — but whoever builds that UI
    must revisit this, or a changed default will appear not to take effect until
    the app restarts.
    */
    val appSettings = SettingsRepository.current(context)

    fun newEmptyProject(): ProjectData =
        AppRootController.emptyProjectPlaceholder(
            defaultTargetMHz = appSettings.defaultTargetFrequencyMHz,
            defaultInstrument = appSettings.defaultInstrument
        )

    fun newRfTestModeProject(): ProjectData =
        AppRootController.buildRfTestModeProject(
            defaultTargetMHz = appSettings.defaultTargetFrequencyMHz,
            defaultInstrument = appSettings.defaultInstrument
        )

    fun newUnknownDiscoveryProject(): ProjectData =
        AppRootController.buildUnknownDiscoveryProject(
            defaultTargetMHz = appSettings.defaultTargetFrequencyMHz,
            defaultInstrument = appSettings.defaultInstrument
        )

    val screen = remember { mutableStateOf("home") }
    val currentProject = remember { mutableStateOf<ProjectData?>(newEmptyProject()) }
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

    // Navigation does not touch live calibration: the instrument is still the
    // instrument whichever screen the operator is on.
    fun enterWizardMode() {
        currentProject.value = newEmptyProject()
        activeProjectOverride.value = null
        testMode.value = false
        projectResumeIntoSweep.value = false
        screen.value = "wizard"
    }

    fun enterRfTestWizardMode() {
        currentProject.value = newRfTestModeProject()
        activeProjectOverride.value = null
        testMode.value = true
        projectResumeIntoSweep.value = false
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
        activeProjectOverride.value = newUnknownDiscoveryProject()
        testMode.value = true
        projectResumeIntoSweep.value = true
        screen.value = "project"
    }

    fun enterProjectSweepMode() {
        val resolvedProject = effectiveProject() ?: newRfTestModeProject()
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
                currentProject.value = restoredProject
                testMode.value = false
                projectResumeIntoSweep.value = false
            }
        }
    }

    LaunchedEffect(screen.value) {
        if (screen.value == "projects" || screen.value == "home") {
            refreshProjectsList()
        }
    }

    when (screen.value) {
        "home" -> DashboardScreen(
            selectedHardwareName =
                EffectiveHardwareResolver.resolveCapabilityProfileForProject(effectiveProject()).displayName,
            recentProjects = savedProjects.value,
            onAction = { action ->
                when (action) {
                    DashboardController.DashboardAction.MEASURE_NOW -> enterProjectSweepMode()
                    DashboardController.DashboardAction.NEW_PROJECT -> enterWizardMode()
                    DashboardController.DashboardAction.IDENTIFY_ANTENNA -> enterUnknownDiscoveryMode()
                }
            },
            onOpenProject = { projectId ->
                val loadedProject = ProjectStorage.loadProjectById(context, projectId)
                if (loadedProject != null) {
                    currentProject.value = loadedProject
                    activeProjectOverride.value = null
                    testMode.value = false
                    projectResumeIntoSweep.value = false
                    screen.value = "project"
                }
            },
            onSeeAllProjects = { enterProjects() },
            onOpenDeviceStatus = {
                projectResumeIntoSweep.value = false
                deviceConnectionsReturnScreen.value = "home"
                screen.value = "device_connections"
            }
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
                val projectForCalibration = effectiveProject() ?: newRfTestModeProject()
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
            onBackHome = { enterHome() },
            /*
            appSettings is the same observable read this screen already does,
            so selecting a theme reassigns the repository's state and
            recomposes from MainActivity down — including this control, which
            is how the selection highlight moves.
            */
            themePreference = appSettings.themePreference,
            onThemePreferenceSelected = { preference ->
                SettingsRepository.update(context) { it.copy(themePreference = preference) }
            }
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
            val project = effectiveProject() ?: newRfTestModeProject()
            val calibrationSession = AppRootController.buildWizardCalibrationSession(project)

            CalibrationWizardScreen(
                calibrationSession = calibrationSession,
                onSessionChange = { session ->
                    UsbSessionManager.registerCalibrationSession(session)
                },
                onFinish = {
                    // The calibration lives in UsbSessionManager (registered by
                    // onSessionChange above) and stays there. Nothing is folded
                    // into the project — calibration is not persisted.
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

// Calibration is live-only: loading a project deliberately does not touch it.
// There is no applyStoredCalibrationToSharedSession — see AppRootController
// SECTION 1600 and the note in ProjectData.kt.