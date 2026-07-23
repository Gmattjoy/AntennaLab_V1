package com.example.antennalab_v1.project

/*
########################################################################
FILE: ProjectWorkspaceController.kt
PACKAGE: com.example.antennalab_v1.project
LAYER: UI / Project Workspace Hub / Logic Control

SYSTEM ROLE
Owns the pure (non-Compose) workspace logic for ProjectPageScreen so the
screen can stay a Compose shell.

It currently owns:

• sweep-return merge (fold stored sweep results back into the local
  in-memory project without clobbering local edits)
• calibration-session building for the wizard hand-off (reuse the shared
  live calibration when usable, otherwise build a fresh session)
• workflow guidance derivations (next action, readiness, user view)
• enum / value formatting used across the workspace cards

DESIGN INTENT
Keep this layer pure and easy to test/migrate. Mirrors the same UI-free
controller pattern used by SweepWorkspaceController and the wizard
controllers.

IMPORTANT
This file intentionally avoids Compose APIs. It reads shared
UsbSessionManager truth (as the screen did) but owns no UI.
########################################################################
*/

import com.example.antennalab_v1.domain.testing.CalibrationSessionFactory
import com.example.antennalab_v1.model.AntennaClassification
import com.example.antennalab_v1.model.AvailablePartsProfile
import com.example.antennalab_v1.model.BuildCostProfile
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.ProjectSweepHistoryMode
import com.example.antennalab_v1.model.TestHardwareProfile
import com.example.antennalab_v1.model.testing.CalibrationSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
########################################################################
SECTION 1000
CONTROLLER OBJECT
########################################################################
PURPOSE
Stateless workspace helpers for ProjectPageScreen.
########################################################################
*/
object ProjectWorkspaceController {

    /*
    ------------------------------------------------------------
    SECTION 1100
    SWEEP-RETURN MERGE
    ------------------------------------------------------------
    PURPOSE
    When returning from a sweep, keep the local in-memory project edits
    but adopt the sweep-owned fields that the storage layer just updated.
    ------------------------------------------------------------
    */
    fun mergeProjectReturnFromSweep(
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

    /*
    ------------------------------------------------------------
    SECTION 1200
    CALIBRATION SESSION BUILDING
    ------------------------------------------------------------
    PURPOSE
    Reuse the shared live calibration when it is still usable (valid or
    in-progress); otherwise start a fresh target-focused session.
    Delegated to the shared CalibrationSessionFactory so the Project-page
    and Lab entry points capture over the same frequency span.
    ------------------------------------------------------------
    */
    fun buildWizardCalibrationSession(
        project: ProjectData
    ): CalibrationSession {
        return CalibrationSessionFactory.buildWizardSession(project)
    }

    fun buildFreshCalibrationSession(
        project: ProjectData
    ): CalibrationSession {
        return CalibrationSessionFactory.buildFreshSession(project)
    }

    /*
    ------------------------------------------------------------
    SECTION 1300
    WORKFLOW GUIDANCE
    ------------------------------------------------------------
    PURPOSE
    Derives the next-action / readiness / user-view guidance text shown
    in the workspace.
    ------------------------------------------------------------
    */
    fun buildNextAction(project: ProjectData): String {
        return when {
            project.designInput.targetFrequencyMHz <= 0.0 ->
                "Set or confirm the target frequency before continuing."

            project.meta.projectStatus.name.contains("DRAFT", ignoreCase = true) ->
                "Review the design workspace, then move to testing when the build is ready."

            else ->
                "Confirm hardware selection and run a sweep to compare the build against the target design."
        }
    }

    fun buildReadinessSummary(project: ProjectData): String {
        val partsText = formatParts(project.availablePartsProfile)
        val budgetText = formatBudget(project.buildCostProfile)
        return "Budget: $budgetText. Parts: $partsText. Hardware prepared: ${formatHardware(project.testHardwareProfile)}."
    }

    fun buildUserViewSummary(project: ProjectData): String {
        return when (project.availablePartsProfile) {
            AvailablePartsProfile.MINIMAL ->
                "Good for first-time builders. Keep the setup simple and verify the design with a basic sweep."
            AvailablePartsProfile.SOME_EXISTING_PARTS ->
                "Good balance for makers and hobby operators. You likely have enough parts to prototype and tune."
            AvailablePartsProfile.WELL_STOCKED_WORKSHOP ->
                "Strong engineering workflow potential. Suitable for rapid iteration, comparison testing, and refinement."
        }
    }

    /*
    ------------------------------------------------------------
    SECTION 1400
    FORMATTING
    ------------------------------------------------------------
    PURPOSE
    Pure value/enum formatting used across the workspace cards.
    ------------------------------------------------------------
    */
    fun formatBudget(profile: BuildCostProfile) =
        when (profile) {
            BuildCostProfile.BUDGET -> "Budget"
            BuildCostProfile.STANDARD -> "Standard"
            BuildCostProfile.PREMIUM -> "Premium"
        }

    fun formatParts(profile: AvailablePartsProfile) =
        when (profile) {
            AvailablePartsProfile.MINIMAL -> "Minimal"
            AvailablePartsProfile.SOME_EXISTING_PARTS -> "Some Existing Parts"
            AvailablePartsProfile.WELL_STOCKED_WORKSHOP -> "Well-Stocked Workshop"
        }

    fun formatHardware(profile: TestHardwareProfile) =
        when (profile) {
            TestHardwareProfile.NANOVNA_H4 -> "NanoVNA-H4"
            TestHardwareProfile.LITEVNA64_V0_3_3 -> "LiteVNA64 v0.3.3"
        }

    fun formatClassification(classification: AntennaClassification): String {
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

    fun antennaClassificationChoices(): List<AntennaClassification> {
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

    fun formatSweepHistoryMode(mode: ProjectSweepHistoryMode): String {
        return when (mode) {
            ProjectSweepHistoryMode.PROJECT_TEST -> "Project Test"
            ProjectSweepHistoryMode.DISCOVERY_APPLIED -> "Discovery Applied"
        }
    }

    fun formatTimestamp(epochMs: Long): String {
        if (epochMs <= 0L) return "Unknown"
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(epochMs))
    }

    fun formatMillimetres(valueMm: Double): String {
        return if (valueMm >= 1000.0) {
            String.format("%.1f mm (%.3f m)", valueMm, valueMm / 1000.0)
        } else {
            String.format("%.1f mm", valueMm)
        }
    }
}
