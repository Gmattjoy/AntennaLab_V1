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

import com.example.antennalab_v1.domain.testing.CalibrationSessionFactory
import com.example.antennalab_v1.domain.testing.EffectiveHardwareResolver
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
    otherwise build a fresh target-focused session. Delegated to the
    shared CalibrationSessionFactory so the Lab and Project-page entry
    points capture over the same frequency span.
    ------------------------------------------------------------
    */
    fun buildWizardCalibrationSession(
        project: ProjectData
    ): CalibrationSession {
        return CalibrationSessionFactory.buildWizardSession(project)
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
    /*
    `logger` is an injected sink, defaulting to a no-op. It exists so the bench can
    see WHICH predicate decided a load without this controller taking an Android
    dependency: android.util.Log is not mocked in plain-JVM unit tests, and this
    file (like every controller under features/) stays framework-free. The Compose
    layer supplies the real sink. Reporting from inside the single evaluation means
    the log can never disagree with the decision it describes.
    */
    fun decideCalibrationRestore(
        project: ProjectData,
        effectiveHardware: TestHardwareProfile,
        logger: (String) -> Unit = {}
    ): CalibrationRestoreAction {
        val storedCalibration = project.storedCalibrationOrNull
        val reason: String

        val action = when {
            storedCalibration == null -> {
                reason = "no-stored-calibration"
                CalibrationRestoreAction.CLEAR
            }

            project.calibrationData.restorePolicy == CalibrationRestorePolicy.DO_NOT_RESTORE -> {
                reason = "policy-do-not-restore"
                CalibrationRestoreAction.CLEAR
            }

            // Match on the hardware FAMILY, not on one spelling of its name. The
            // stored name may be a driver label ("LiteVNA64 HW 64-0.3.3 FW v1.4.06")
            // while the capability displayName is "LiteVNA64 v0.3.3" — comparing
            // those two strings directly silently CLEARED real calibrations.
            !EffectiveHardwareResolver.storedNameMatchesHardware(
                storedHardwareName = storedCalibration.hardwareDisplayName,
                profile = effectiveHardware
            ) -> {
                reason = "hardware-name-mismatch"
                CalibrationRestoreAction.CLEAR
            }

            project.calibrationData.restorePolicy == CalibrationRestorePolicy.RESTORE_IF_COMPATIBLE &&
                !storedCalibration.coversFrequencyRange(
                    requestedStartMHz = project.designInput.targetFrequencyMHz,
                    requestedEndMHz = project.designInput.targetFrequencyMHz
                ) -> {
                reason = "range-not-covered"
                CalibrationRestoreAction.CLEAR
            }

            project.calibrationData.restorePolicy == CalibrationRestorePolicy.RESTORE_IF_COMPATIBLE &&
                !storedCalibration.isFullyCaptured() -> {
                reason = "not-fully-captured"
                CalibrationRestoreAction.CLEAR
            }

            else -> {
                reason = "ok"
                CalibrationRestoreAction.RESTORE
            }
        }

        logger(
            buildCalibrationRestoreLogLine(
                action = action,
                reason = reason,
                storedCalibration = storedCalibration,
                effectiveHardware = effectiveHardware,
                project = project
            )
        )

        return action
    }

    /*
    ONE greppable line per project load: `adb logcat -s CalRestore`.
    policy / target / storedSpan are included because policy-do-not-restore and
    range-not-covered are otherwise indistinguishable from the operator's side.
    */
    internal fun buildCalibrationRestoreLogLine(
        action: CalibrationRestoreAction,
        reason: String,
        storedCalibration: CalibrationSession?,
        effectiveHardware: TestHardwareProfile,
        project: ProjectData
    ): String {
        val storedSpan =
            if (storedCalibration == null) {
                "none"
            } else {
                String.format(
                    "%.3f-%.3f",
                    storedCalibration.startFrequencyMHz,
                    storedCalibration.endFrequencyMHz
                )
            }

        return "calRestore decision=$action reason=$reason " +
            "storedName='${storedCalibration?.hardwareDisplayName ?: "none"}' " +
            "effective=$effectiveHardware " +
            "policy=${project.calibrationData.restorePolicy} " +
            String.format("target=%.3f ", project.designInput.targetFrequencyMHz) +
            "storedSpan=$storedSpan"
    }

    /** Convenience overload resolving the effective hardware from the live session. */
    fun decideCalibrationRestore(
        project: ProjectData,
        logger: (String) -> Unit = {}
    ): CalibrationRestoreAction {
        return decideCalibrationRestore(
            project = project,
            effectiveHardware = EffectiveHardwareResolver.resolveForProject(project),
            logger = logger
        )
    }
}
