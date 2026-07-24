package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.UsbSessionManager
import com.example.antennalab_v1.features.app.AppRootController
import com.example.antennalab_v1.features.app.CalibrationRestoreAction
import com.example.antennalab_v1.features.lab.LabTestTemplate
import com.example.antennalab_v1.model.AntennaType
import com.example.antennalab_v1.model.CalibrationRestorePolicy
import com.example.antennalab_v1.model.LabEntryMode
import com.example.antennalab_v1.model.ProjectCalibrationData
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.ProjectMeta
import com.example.antennalab_v1.model.ProjectSource
import com.example.antennalab_v1.model.TestHardwareProfile
import com.example.antennalab_v1.model.testing.CalibrationSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/**
 * Behavior coverage for the AppRootScreen project/calibration logic extracted into
 * the pure [AppRootController]: ProjectData factories, antenna-type resolution + lab
 * template application, wizard finish normalization, calibration-wizard session
 * building, and the stored-calibration restore-policy decision. Real ProjectData /
 * LabTestTemplate models, no mocking.
 *
 * buildWizardCalibrationSession reads the UsbSessionManager singleton (no Context),
 * so that state is reset before each test. Plain JVM.
 */
class AppRootControllerTest {

    @Before
    fun resetSessionManager() {
        UsbSessionManager.clearCalibrationState()
        UsbSessionManager.clearSelectedHardwareConfig()
    }

    private fun template(
        id: String = "yagi_hf_20m",
        displayName: String = "Yagi HF",
        bandLabel: String = "20 m / 14.2 MHz",
        antennaTypeKey: String = "YAGI",
        targetFrequencyMHz: Double = 14.2
    ) = LabTestTemplate(
        id = id,
        displayName = displayName,
        bandLabel = bandLabel,
        antennaTypeKey = antennaTypeKey,
        targetFrequencyMHz = targetFrequencyMHz,
        summary = "summary"
    )

    // Uses the LiteVNA profile's capability display name so a stored calibration
    // can match / not match project hardware in the restore tests.
    private fun projectWithStoredCalibration(
        restorePolicy: CalibrationRestorePolicy,
        calHardwareName: String,
        calStartMHz: Double = 1.0,
        calEndMHz: Double = 30.0,
        targetFrequencyMHz: Double = 14.2,
        hardware: TestHardwareProfile = TestHardwareProfile.LITEVNA64_V0_3_3
    ): ProjectData {
        val base = ProjectData(
            designInput = com.example.antennalab_v1.model.DesignInput(targetFrequencyMHz = targetFrequencyMHz),
            testHardwareProfile = hardware
        )
        return base.copy(
            calibrationData = ProjectCalibrationData(
                storedCalibrationSession = CalibrationSession(
                    hardwareDisplayName = calHardwareName,
                    startFrequencyMHz = calStartMHz,
                    endFrequencyMHz = calEndMHz,
                    openCaptured = true,
                    shortCaptured = true,
                    loadCaptured = true
                ),
                restorePolicy = restorePolicy
            )
        )
    }

    private fun hardwareName(hardware: TestHardwareProfile) =
        ProjectData(testHardwareProfile = hardware).hardwareCapabilityProfile.displayName

    // ------------------------------------------------------------------
    // Project factories
    // ------------------------------------------------------------------

    @Test
    fun factories_buildExpectedStarterProjects() {
        val empty = AppRootController.emptyProjectPlaceholder()
        assertEquals("", empty.meta.projectName)
        assertEquals(AntennaType.OTHER, empty.designInput.antennaType)
        assertEquals(14.2, empty.designInput.targetFrequencyMHz, 0.0)
        assertEquals(TestHardwareProfile.NANOVNA_H4, empty.testHardwareProfile)

        val rf = AppRootController.buildRfTestModeProject()
        assertEquals("RF Test Mode", rf.meta.projectName)
        assertEquals(AntennaType.OTHER, rf.designInput.antennaType)

        val discovery = AppRootController.buildUnknownDiscoveryProject()
        assertEquals("Unknown Antenna Discovery", discovery.meta.projectName)
        assertEquals(LabEntryMode.UNKNOWN_DISCOVERY, discovery.meta.labEntryMode)
        assertEquals(ProjectSource.LAB_UNKNOWN_DISCOVERY, discovery.versionInfo.appDataSource)
    }

    // ------------------------------------------------------------------
    // Template application
    // ------------------------------------------------------------------

    @Test
    fun resolveAntennaType_isCaseInsensitiveWithOtherFallback() {
        assertEquals(AntennaType.YAGI, AppRootController.resolveAntennaType("yagi"))
        assertEquals(AntennaType.DIPOLE, AppRootController.resolveAntennaType("DIPOLE"))
        assertEquals(AntennaType.OTHER, AppRootController.resolveAntennaType("nonexistent"))
    }

    @Test
    fun applyTemplateToProject_fillsNameOnlyWhenBlankAndSetsDesign() {
        val blankName = AppRootController.applyTemplateToProject(
            ProjectData(meta = ProjectMeta(projectName = "")),
            template(displayName = "Dipole HF", antennaTypeKey = "DIPOLE", targetFrequencyMHz = 7.1)
        )
        assertEquals("Dipole HF", blankName.meta.projectName)
        assertEquals(AntennaType.DIPOLE, blankName.designInput.antennaType)
        assertEquals(7.1, blankName.designInput.targetFrequencyMHz, 0.0)

        val namedKept = AppRootController.applyTemplateToProject(
            ProjectData(meta = ProjectMeta(projectName = "My Build")),
            template(displayName = "Dipole HF")
        )
        assertEquals("My Build", namedKept.meta.projectName)
    }

    @Test
    fun buildProjectTemplateTestProject_tagsLabEntryAndSource() {
        val tagged = AppRootController.buildProjectTemplateTestProject(
            attached = ProjectData(meta = ProjectMeta(projectName = "Field Yagi")),
            template = template(displayName = "Yagi HF", bandLabel = "20 m / 14.2 MHz")
        )

        assertEquals(LabEntryMode.PROJECT_TEMPLATE_TEST, tagged.meta.labEntryMode)
        assertEquals("Field Yagi", tagged.meta.labSourceProjectName)
        assertEquals("Yagi HF", tagged.meta.labTemplateDisplayName)
        assertEquals("20 m / 14.2 MHz", tagged.meta.labTemplateBandLabel)
        assertEquals(ProjectSource.LAB_PROJECT_TEMPLATE_TEST, tagged.versionInfo.appDataSource)
    }

    @Test
    fun buildProjectTemplateTestProject_defaultsBlankSourceName() {
        val tagged = AppRootController.buildProjectTemplateTestProject(
            attached = ProjectData(meta = ProjectMeta(projectName = "")),
            template = template()
        )
        assertEquals("Unnamed Project", tagged.meta.labSourceProjectName)
    }

    // ------------------------------------------------------------------
    // Wizard finish normalization
    // ------------------------------------------------------------------

    @Test
    fun finalizeWizardProject_defaultsQuickTestOnlyInTestModeWhenBlank() {
        val blank = ProjectData(meta = ProjectMeta(projectName = ""))
        assertEquals("Quick Test", AppRootController.finalizeWizardProject(blank, testMode = true).meta.projectName)

        val named = ProjectData(meta = ProjectMeta(projectName = "Real Name"))
        assertEquals("Real Name", AppRootController.finalizeWizardProject(named, testMode = true).meta.projectName)

        assertEquals("", AppRootController.finalizeWizardProject(blank, testMode = false).meta.projectName)
    }

    // ------------------------------------------------------------------
    // Calibration wizard session
    // ------------------------------------------------------------------

    @Test
    fun buildWizardCalibrationSession_buildsFreshSessionAroundTargetWhenNoSharedCalibration() {
        val project = ProjectData(
            designInput = com.example.antennalab_v1.model.DesignInput(targetFrequencyMHz = 14.2)
        )

        val session = AppRootController.buildWizardCalibrationSession(project)

        assertEquals(13.7, session.startFrequencyMHz, 1e-9) // target - 0.5
        assertEquals(14.7, session.endFrequencyMHz, 1e-9)   // target + 0.5
        assertFalse(session.hasAnyCapturedStep())
        assertEquals("Not captured yet", session.timestampLabel)
    }

    @Test
    fun buildWizardCalibrationSession_reusesValidSharedCalibration() {
        UsbSessionManager.registerSimulatedCalibrationSession(
            CalibrationSession(
                hardwareDisplayName = "LiteVNA64 v0.3.3",
                startFrequencyMHz = 1.0,
                endFrequencyMHz = 30.0,
                openCaptured = true,
                shortCaptured = true,
                loadCaptured = true
            )
        )

        val session = AppRootController.buildWizardCalibrationSession(ProjectData())
        assertEquals("SIMULATED_CAL_SESSION", session.capturedSessionKey)
        assertEquals(true, session.isFullyCaptured())
    }

    // ------------------------------------------------------------------
    // Stored-calibration restore policy
    // ------------------------------------------------------------------

    @Test
    fun decideCalibrationRestore_clearsWhenNoStoredCalibration() {
        assertEquals(CalibrationRestoreAction.CLEAR, AppRootController.decideCalibrationRestore(ProjectData()))
    }

    @Test
    fun decideCalibrationRestore_clearsForDoNotRestorePolicy() {
        val project = projectWithStoredCalibration(
            restorePolicy = CalibrationRestorePolicy.DO_NOT_RESTORE,
            calHardwareName = hardwareName(TestHardwareProfile.LITEVNA64_V0_3_3)
        )
        assertEquals(CalibrationRestoreAction.CLEAR, AppRootController.decideCalibrationRestore(project))
    }

    @Test
    fun decideCalibrationRestore_clearsOnHardwareMismatch() {
        val project = projectWithStoredCalibration(
            restorePolicy = CalibrationRestorePolicy.RESTORE_AS_STALE,
            calHardwareName = "Some Other Analyzer"
        )
        assertEquals(CalibrationRestoreAction.CLEAR, AppRootController.decideCalibrationRestore(project))
    }

    @Test
    fun decideCalibrationRestore_clearsWhenRestoreIfCompatibleButOutOfRange() {
        val project = projectWithStoredCalibration(
            restorePolicy = CalibrationRestorePolicy.RESTORE_IF_COMPATIBLE,
            calHardwareName = hardwareName(TestHardwareProfile.LITEVNA64_V0_3_3),
            calStartMHz = 20.0, // target 14.2 falls below the captured range
            calEndMHz = 30.0,
            targetFrequencyMHz = 14.2
        )
        assertEquals(CalibrationRestoreAction.CLEAR, AppRootController.decideCalibrationRestore(project))
    }

    @Test
    fun decideCalibrationRestore_restoresWhenCompatibleInRange() {
        val project = projectWithStoredCalibration(
            restorePolicy = CalibrationRestorePolicy.RESTORE_IF_COMPATIBLE,
            calHardwareName = hardwareName(TestHardwareProfile.LITEVNA64_V0_3_3),
            calStartMHz = 1.0,
            calEndMHz = 30.0,
            targetFrequencyMHz = 14.2
        )
        assertEquals(CalibrationRestoreAction.RESTORE, AppRootController.decideCalibrationRestore(project))
    }

    @Test
    fun decideCalibrationRestore_restoresAsStaleWhenHardwareMatches() {
        val project = projectWithStoredCalibration(
            restorePolicy = CalibrationRestorePolicy.RESTORE_AS_STALE,
            calHardwareName = hardwareName(TestHardwareProfile.LITEVNA64_V0_3_3),
            calStartMHz = 999.0, // range ignored for RESTORE_AS_STALE
            calEndMHz = 1000.0
        )
        assertEquals(CalibrationRestoreAction.RESTORE, AppRootController.decideCalibrationRestore(project))
    }

    // ------------------------------------------------------------------
    // Finding #7b — stored calibration vs the EFFECTIVE instrument
    // ------------------------------------------------------------------

    /*
    NAMED REGRESSION TEST — the headline bug.

    A calibration captured on a real LiteVNA was stored under the DRIVER LABEL
    ("LiteVNA64 HW 64-0.3.3 FW v1.4.06", written by DeviceConnectionsScreen via
    buildProfileDisplayLabel), then compared against the project's design-time
    capability displayName — "NanoVNA-H4", because testHardwareProfile defaults to
    NANOVNA_H4. The names never matched, so a verified OSL calibration was silently
    CLEARED on project load and the operator's calibration work was lost.

    Restoring must now succeed: the effective hardware is the LiteVNA, and the
    driver-label spelling is recognised as the same family.
    */
    @Test
    fun decideCalibrationRestore_restoresLiveLiteVnaCalibrationStoredUnderDriverLabel() {
        val project = projectWithStoredCalibration(
            restorePolicy = CalibrationRestorePolicy.RESTORE_IF_COMPATIBLE,
            calHardwareName = "LiteVNA64 HW 64-0.3.3 FW v1.4.06",
            calStartMHz = 1.0,
            calEndMHz = 30.0,
            targetFrequencyMHz = 14.2,
            hardware = TestHardwareProfile.NANOVNA_H4 // stale project default
        )

        assertEquals(
            CalibrationRestoreAction.RESTORE,
            AppRootController.decideCalibrationRestore(
                project = project,
                effectiveHardware = TestHardwareProfile.LITEVNA64_V0_3_3
            )
        )
    }

    /*
    THE DANGEROUS DIRECTION. Loosening the name comparison must not let a LiteVNA
    calibration be adopted by an attached NanoVNA — that applies wrong corrections
    silently, which is worse than clearing a good calibration.
    */
    @Test
    fun decideCalibrationRestore_clearsLiteVnaCalibrationWhenNanoVnaIsAttached() {
        val project = projectWithStoredCalibration(
            restorePolicy = CalibrationRestorePolicy.RESTORE_IF_COMPATIBLE,
            calHardwareName = "LiteVNA64 HW 64-0.3.3 FW v1.4.06",
            calStartMHz = 1.0,
            calEndMHz = 30.0,
            targetFrequencyMHz = 14.2,
            hardware = TestHardwareProfile.LITEVNA64_V0_3_3
        )

        assertEquals(
            CalibrationRestoreAction.CLEAR,
            AppRootController.decideCalibrationRestore(
                project = project,
                effectiveHardware = TestHardwareProfile.NANOVNA_H4
            )
        )
    }

    @Test
    fun decideCalibrationRestore_clearsNanoVnaCalibrationWhenLiteVnaIsAttached() {
        val project = projectWithStoredCalibration(
            restorePolicy = CalibrationRestorePolicy.RESTORE_IF_COMPATIBLE,
            calHardwareName = "NanoVNA-H4",
            calStartMHz = 1.0,
            calEndMHz = 30.0,
            targetFrequencyMHz = 14.2
        )

        assertEquals(
            CalibrationRestoreAction.CLEAR,
            AppRootController.decideCalibrationRestore(
                project = project,
                effectiveHardware = TestHardwareProfile.LITEVNA64_V0_3_3
            )
        )
    }

    // The canonical capability displayName still restores — canonicalised captures
    // and legacy driver-label captures must both work.
    @Test
    fun decideCalibrationRestore_restoresCanonicalCapabilityName() {
        val project = projectWithStoredCalibration(
            restorePolicy = CalibrationRestorePolicy.RESTORE_IF_COMPATIBLE,
            calHardwareName = "LiteVNA64 v0.3.3",
            calStartMHz = 1.0,
            calEndMHz = 30.0,
            targetFrequencyMHz = 14.2,
            hardware = TestHardwareProfile.NANOVNA_H4
        )

        assertEquals(
            CalibrationRestoreAction.RESTORE,
            AppRootController.decideCalibrationRestore(
                project = project,
                effectiveHardware = TestHardwareProfile.LITEVNA64_V0_3_3
            )
        )
    }

    // A partially-captured calibration must not be restored as compatible.
    @Test
    fun decideCalibrationRestore_clearsPartiallyCapturedCalibration() {
        val base = ProjectData(
            designInput = com.example.antennalab_v1.model.DesignInput(targetFrequencyMHz = 14.2),
            testHardwareProfile = TestHardwareProfile.LITEVNA64_V0_3_3
        )
        val project = base.copy(
            calibrationData = ProjectCalibrationData(
                storedCalibrationSession = CalibrationSession(
                    hardwareDisplayName = "LiteVNA64 v0.3.3",
                    startFrequencyMHz = 1.0,
                    endFrequencyMHz = 30.0,
                    openCaptured = true,
                    shortCaptured = true,
                    loadCaptured = false // never finished
                ),
                restorePolicy = CalibrationRestorePolicy.RESTORE_IF_COMPATIBLE
            )
        )

        assertEquals(
            CalibrationRestoreAction.CLEAR,
            AppRootController.decideCalibrationRestore(
                project = project,
                effectiveHardware = TestHardwareProfile.LITEVNA64_V0_3_3
            )
        )
    }
}
