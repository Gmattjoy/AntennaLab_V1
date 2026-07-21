package com.example.antennalab_v1.storage

/*
------------------------------------------------------------
EDIT SECTION 1000
FILE: ProjectIndexManager.kt
PACKAGE: com.example.antennalab_v1.storage
LAYER: Storage / Project Index

SYSTEM ROLE
Maintains the master saved-project index for AntennaLab V1.

CURRENT DEVELOPMENT ROLE
Handles the list of saved projects so the UI can:

• display saved projects
• update the index when projects are saved
• remove entries when projects are deleted
• generate safe duplicate project names

STORAGE LOCATION
files/projects/project_index.json

SAFE EDIT AREA
- migrate storage location later
- add external storage support later
- add project search or filtering later
------------------------------------------------------------
*/

import android.content.Context
import com.example.antennalab_v1.model.ProjectListItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/*
------------------------------------------------------------
EDIT SECTION 1001
PROJECT INDEX MANAGER
------------------------------------------------------------
PURPOSE
Provides the high-level project index operations used by the app.

SAFE EDIT AREA
- extend index operations later
- add batch operations later
------------------------------------------------------------
*/
object ProjectIndexManager {

    private const val PROJECTS_FOLDER_NAME = "projects"
    private const val INDEX_FILE_NAME = "project_index.json"

    /*
    ------------------------------------------------------------
    EDIT SECTION 1002
    PROJECT DIRECTORY ACCESS
    ------------------------------------------------------------
    PURPOSE
    Ensures the project directory exists.
    ------------------------------------------------------------
    */
    private fun getProjectsDirectory(context: Context): File {
        val dir = File(context.filesDir, PROJECTS_FOLDER_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 1003
    INDEX FILE ACCESS
    ------------------------------------------------------------
    PURPOSE
    Ensures the index file exists and returns it.
    ------------------------------------------------------------
    */
    private fun getIndexFile(context: Context): File {
        val indexFile = File(getProjectsDirectory(context), INDEX_FILE_NAME)

        if (!indexFile.exists()) {
            indexFile.writeText("[]")
        }

        return indexFile
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 1004
    LOAD ALL PROJECTS
    ------------------------------------------------------------
    PURPOSE
    Returns the complete project index list sorted by last edited time.
    ------------------------------------------------------------
    */
    fun getAllProjects(context: Context): List<ProjectListItem> {
        val indexFile = getIndexFile(context)
        val rawText = indexFile.readText()

        if (rawText.isBlank()) {
            return emptyList()
        }

        val jsonArray = JSONArray(rawText)
        val results = mutableListOf<ProjectListItem>()

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            results.add(item.toProjectListItem())
        }

        return results.sortedByDescending { it.lastEditedEpochMillis }
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 1005
    SAVE OR UPDATE PROJECT ENTRY
    ------------------------------------------------------------
    PURPOSE
    Adds or updates a project summary entry in the index.
    ------------------------------------------------------------
    */
    fun saveOrUpdateProject(context: Context, project: ProjectListItem) {
        val currentList = getAllProjects(context).toMutableList()

        val existingIndex = currentList.indexOfFirst { it.projectId == project.projectId }

        if (existingIndex >= 0) {
            currentList[existingIndex] = project
        } else {
            currentList.add(project)
        }

        writeProjectList(context, currentList)
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 1006
    REMOVE PROJECT ENTRY
    ------------------------------------------------------------
    PURPOSE
    Removes a project summary entry from the index.
    ------------------------------------------------------------
    */
    fun removeProject(context: Context, projectId: String) {
        val currentList = getAllProjects(context)
            .filterNot { it.projectId == projectId }

        writeProjectList(context, currentList)
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 1007
    PROJECT LOOKUP
    ------------------------------------------------------------
    PURPOSE
    Finds a project entry by its ID.
    ------------------------------------------------------------
    */
    fun getProjectById(context: Context, projectId: String): ProjectListItem? {
        return getAllProjects(context).firstOrNull { it.projectId == projectId }
    }

    fun containsProjectId(context: Context, projectId: String): Boolean {
        return getProjectById(context, projectId) != null
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 1008
    DUPLICATE NAME BUILDER
    ------------------------------------------------------------
    PURPOSE
    Generates safe duplicate project names.
    ------------------------------------------------------------
    */
    fun buildDuplicateName(
        context: Context,
        originalName: String
    ): String {
        val existingNames = getAllProjects(context)
            .map { it.name.trim() }
            .toSet()

        val baseCopyName = "$originalName Copy"

        if (!existingNames.contains(baseCopyName)) {
            return baseCopyName
        }

        var counter = 2
        while (true) {
            val candidate = "$originalName Copy $counter"
            if (!existingNames.contains(candidate)) {
                return candidate
            }
            counter++
        }
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2001
    WRITE PROJECT LIST
    ------------------------------------------------------------
    PURPOSE
    Writes the full index list back to disk.
    ------------------------------------------------------------
    */
    private fun writeProjectList(
        context: Context,
        projects: List<ProjectListItem>
    ) {
        val sortedProjects = projects.sortedByDescending { it.lastEditedEpochMillis }
        val jsonArray = JSONArray()

        sortedProjects.forEach { project ->
            jsonArray.put(project.toJson())
        }

        getIndexFile(context).writeText(jsonArray.toString(2))
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2002
    JSON SERIALIZATION HELPERS
    ------------------------------------------------------------
    PURPOSE
    Converts ProjectListItem objects to and from JSON.
    ------------------------------------------------------------
    */
    private fun ProjectListItem.toJson(): JSONObject {
        return JSONObject().apply {
            put("projectId", projectId)
            put("name", name)
            put("antennaType", antennaType)
            put("targetFrequencyHz", targetFrequencyHz)
            put("lastEditedEpochMillis", lastEditedEpochMillis)
        }
    }

    private fun JSONObject.toProjectListItem(): ProjectListItem {
        return ProjectListItem(
            projectId = optString("projectId", ""),
            name = optString("name", "Unnamed Project"),
            antennaType = optString("antennaType", "Unknown"),
            targetFrequencyHz = optLong("targetFrequencyHz", 0L),
            lastEditedEpochMillis = optLong("lastEditedEpochMillis", 0L)
        )
    }
}

/*
------------------------------------------------------------
END EDIT SECTIONS 2003
------------------------------------------------------------

 */