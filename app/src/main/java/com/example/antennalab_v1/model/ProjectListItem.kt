package com.example.antennalab_v1.model

/*
########################################################################
FILE: ProjectListItem.kt
PURPOSE: Lightweight project summary used for project list display and
         master project index storage.

NOTES
- This is intentionally smaller than full ProjectData.
- It stores only the data needed to identify and list a project.
- The full editable antenna project remains in ProjectData storage.
########################################################################
*/

data class ProjectListItem(
    val projectId: String,
    val name: String,
    val antennaType: String,
    val targetFrequencyHz: Long,
    val lastEditedEpochMillis: Long
)