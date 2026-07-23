package com.example.antennalab_v1.features.app

/*
########################################################################
FILE: AppRootController.kt
PACKAGE: com.example.antennalab_v1.features.app
LAYER: UI / App Root / Logic Control

SYSTEM ROLE
Owns the pure (non-Compose) project- and calibration-orchestration logic
for AppRootScreen so the navigation host can stay a Compose shell.

It currently owns:

• starter/placeholder ProjectData factories (wizard, RF test, discovery)
• antenna-type key resolution + lab-template application
• project-template test-mode project assembly
• wizard finish-project normalization ("Quick Test" default)
• calibration-wizard session building (reuse shared / build fresh)
• stored-calibration restore-policy decision

DESIGN INTENT
Keep this layer pure and easy to test/migrate. Mirrors the same UI-free
controller pattern used across the app (ProjectWorkspaceController etc.).

IMPORTANT
This file avoids Compose APIs and Android Context. The screen keeps
navigation state and performs the Context-bound calibration side effects
(register/refresh) using decideCalibrationRestore's result.
########################################################################
*/

import com.example.antennalab_v1.domain.testing.UsbSessionManager
import com.example.antennalab_v1.features.lab.LabTestTemplate
import com.example.antennalab_v1.model.AntennaType
import com.example.antennalab_v1.model.CalibrationRestorePolicy
import com.example.antennalab_v1.model.DesignInput
import com.example.antennalab_v1.model.LabEntryMode
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.ProjectMeta
import com.example.antennalab_v1.model.ProjectSource
import com.example.antennalab_v1.model.TestHardwareProfile
import com.example.antennalab_v1.model.VersionInfo
import com.example.antennalab_v1.model.testing.CalibrationReadiness
import com.example.antennalab_v1.model.testing.CalibrationSession

/*
########################################################################
SECTION 1000
CALIBRATION RESTORE DECISION
########################################################################
PURPOSE
Pure result of the stored-calibration restore policy. The screen turns
this into the actual (Context-bound) UsbSessionManager side effects.
########################################################################
*/
enum class CalibrationRestoreAction {
    CLEAR,
    RESTORE
}

/*
########################################################################
SECTION 1100
CONTROLLER OBJECT
########################################################################
PURPOSE
Stateless project/calibration helpers for AppRootScreen.
########################################################################
*/
object AppRootController {

    /*
    ------------------------------------------------------------
    SECTION 1200
    PROJECT FACTORIES
    ------------------------------------------------------------
    */
    fun emptyProjectPlaceholder(): ProjectData {
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

    fun buildRfTestModeProject(): ProjectData {
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

    fun buildUnknownDiscoveryProject(): ProjectData {
        return ProjectData(
            meta = ProjectMeta(
                projectName = "Unknown Antenna Discovery",
                labEntryMode = LabEntryMode.UNKNOWN_DISCOVERY
            ),
            designInput = DesignInput(
                antennaType = AntennaType.OTHER,
                targetFrequencyMHz = 14.2
            ),
            versionInfo = VersionInfo(
                appDataSource = ProjectSource.LAB_UNKNOWN_DISCOVERY
            ),
            testHardwareProfile = TestHardwareProfile.NANOVNA_H4
        )
    }

    /*
    ------------------------------------------------------------
    SECTION 1300
    TEMPLATE APPLICATION
    ------------------------------------------------------------
    */
    fun resolveAntennaType(antennaTypeKey: String): AntennaType {
        return enumValues<AntennaType>()
            .firstOrNull { it.name.equals(antennaTypeKey, ignoreCase = true) }
            ?: AntennaType.OTHER
    }

    fun applyTemplateToProject(
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

    fun buildProjectTemplateTestProject(
        attached: ProjectData,
        template: LabTestTemplate
    ): ProjectData {
        val sourceProjectName = attached.meta.projectName.ifBlank { "Unnamed Project" }
        val templatedProject = applyTemplateToProject(attached, template)

        return templatedProject.copy(
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
    }

    /*
    ------------------------------------------------------------
    SECTION 1400
    WIZARD FINISH NORMALIZATION
    ------------------------------------------------------------
    PURPOSE
    In test mode a blank project name defaults to "Quick Test".
    ------------------------------------------------------------
    */
    fun finalizeWizardProject(
        createdProject: ProjectData,
        testMode: Boolean
    ): ProjectData {
        return if (testMode) {
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
    }

    /*
    ------------------------------------------------------------
    SECTION 1500
    CALIBRATION WIZARD SESSION
    ------------------------------------------------------------
    PURPOSE
    Reuse the shared live calibration when usable (valid/in-progress),
    otherwise build a fresh session focused on the design frequency
    (target ± 0.5 MHz).

    NOTE: this deliberately differs from
    ProjectWorkspaceController.buildWizardCalibrationSession, whose fresh
    branch spans the full hardware-capability range. Kept as-is to
    preserve AppRoot behaviour; unifying the two is a separate decision.
    ------------------------------------------------------------
    */
    fun buildWizardCalibrationSession(
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

    /*
    ------------------------------------------------------------
    SECTION 1600
    STORED-CALIBRATION RESTORE POLICY (pure decision)
    ------------------------------------------------------------
    PURPOSE
    Decide whether a project's stored calibration should be cleared or
    restored into the shared session. The screen performs the resulting
    UsbSessionManager side effects (which need a Context).
    ------------------------------------------------------------
    */
    fun decideCalibrationRestore(project: ProjectData): CalibrationRestoreAction {
        val storedCalibration = project.storedCalibrationOrNull
        val selectedHardwareName = project.hardwareCapabilityProfile.displayName

        return when {
            storedCalibration == null ->
                CalibrationRestoreAction.CLEAR

            project.calibrationData.restorePolicy == CalibrationRestorePolicy.DO_NOT_RESTORE ->
                CalibrationRestoreAction.CLEAR

            !storedCalibration.matchesHardwareDisplayName(selectedHardwareName) ->
                CalibrationRestoreAction.CLEAR

            project.calibrationData.restorePolicy == CalibrationRestorePolicy.RESTORE_IF_COMPATIBLE &&
                !storedCalibration.isCompatibleWithRequestedRange(
                    selectedHardwareName = selectedHardwareName,
                    requestedStartMHz = project.designInput.targetFrequencyMHz,
                    requestedEndMHz = project.designInput.targetFrequencyMHz
                ) ->
                CalibrationRestoreAction.CLEAR

            else ->
                CalibrationRestoreAction.RESTORE
        }
    }
}
