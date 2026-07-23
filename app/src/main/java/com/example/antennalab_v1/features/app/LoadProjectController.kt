package com.example.antennalab_v1.features.app

/*
########################################################################
FILE: LoadProjectController.kt
PACKAGE: com.example.antennalab_v1.features.app
LAYER: UI / App / Project Manager / Logic Control

SYSTEM ROLE
Owns the pure (non-Compose) display logic for LoadProjectScreen (the
save/load "Project Manager") so the screen can stay a Compose shell.

It currently owns:

• initial saved-project list resolution (passed-in list, else the
  file-backed index)
• saved-project card derivations (target-frequency + last-edited
  formatting, stored-calibration presence + completion label)

DESIGN INTENT
Keep this layer pure and easy to test/migrate. Mirrors the same UI-free
controller pattern used by the sweep / wizard / workspace controllers.

IMPORTANT
This file intentionally avoids Compose APIs. It reads plain model data
and does its own value formatting; it owns no UI and performs no storage
I/O (the screen still calls ProjectStorage / ProjectIndexManager).
########################################################################
*/

import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.ProjectListItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
########################################################################
SECTION 1000
CONTROLLER OBJECT
########################################################################
PURPOSE
Stateless display helpers for LoadProjectScreen.
########################################################################
*/
object LoadProjectController {

    /*
    ------------------------------------------------------------
    SECTION 1100
    INITIAL LIST RESOLUTION
    ------------------------------------------------------------
    PURPOSE
    Prefer the list passed into the screen; fall back to the file-backed
    index list when nothing was passed in.
    ------------------------------------------------------------
    */
    fun resolveInitialProjects(
        passedInProjects: List<ProjectListItem>,
        indexProjects: List<ProjectListItem>
    ): List<ProjectListItem> {
        return if (passedInProjects.isNotEmpty()) {
            passedInProjects
        } else {
            indexProjects
        }
    }

    /*
    ------------------------------------------------------------
    SECTION 1200
    SAVED-PROJECT CARD DERIVATIONS
    ------------------------------------------------------------
    PURPOSE
    Formatting and calibration-summary derivation for a saved-project
    card row.
    ------------------------------------------------------------
    */
    fun formatTargetFrequencyMHz(targetFrequencyHz: Long): String {
        val frequencyMHz = targetFrequencyHz.toDouble() / 1_000_000.0
        return String.format(Locale.getDefault(), "%.3f", frequencyMHz)
    }

    fun formatLastEdited(lastEditedEpochMillis: Long): String {
        return SimpleDateFormat(
            "d MMM yyyy  h:mm a",
            Locale.getDefault()
        ).format(Date(lastEditedEpochMillis))
    }

    fun hasStoredCalibration(project: ProjectData?): Boolean {
        return project?.hasStoredCalibration == true
    }

    fun storedCalibrationCompletion(project: ProjectData?): String {
        return project?.calibrationData?.storedCalibrationCompletionStateName ?: "NOT_STARTED"
    }
}
