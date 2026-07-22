package com.example.antennalab_v1

import android.content.Context
import com.example.antennalab_v1.model.ProjectListItem
import com.example.antennalab_v1.storage.ProjectIndexManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

/**
 * Robolectric coverage for the file-backed saved-project index
 * (files/projects/project_index.json).
 */
@RunWith(RobolectricTestRunner::class)
class ProjectIndexManagerTest {

    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    @Before
    fun clearIndex() {
        // Guarantee a clean index dir regardless of sandbox reuse.
        File(context.filesDir, "projects").deleteRecursively()
    }

    private fun item(
        id: String,
        name: String = "Project $id",
        antennaType: String = "DIPOLE",
        targetHz: Long = 14_200_000L,
        lastEdited: Long = 1_000L
    ) = ProjectListItem(
        projectId = id,
        name = name,
        antennaType = antennaType,
        targetFrequencyHz = targetHz,
        lastEditedEpochMillis = lastEdited
    )

    @Test
    fun getAllProjects_isEmptyInitially() {
        assertTrue(ProjectIndexManager.getAllProjects(context).isEmpty())
    }

    @Test
    fun saveOrUpdate_addsAndRoundTripsFields() {
        val original = item("a", name = "My Yagi", antennaType = "YAGI", targetHz = 144_300_000L, lastEdited = 42L)
        ProjectIndexManager.saveOrUpdateProject(context, original)

        val loaded = ProjectIndexManager.getProjectById(context, "a")
        assertEquals(original, loaded)
        assertTrue(ProjectIndexManager.containsProjectId(context, "a"))
    }

    @Test
    fun saveOrUpdate_updatesExistingInPlace_noDuplicate() {
        ProjectIndexManager.saveOrUpdateProject(context, item("a", name = "First"))
        ProjectIndexManager.saveOrUpdateProject(context, item("a", name = "Renamed"))

        val all = ProjectIndexManager.getAllProjects(context)
        assertEquals(1, all.size)
        assertEquals("Renamed", all.first().name)
    }

    @Test
    fun getAllProjects_sortedByLastEditedDescending() {
        ProjectIndexManager.saveOrUpdateProject(context, item("old", lastEdited = 100L))
        ProjectIndexManager.saveOrUpdateProject(context, item("new", lastEdited = 300L))
        ProjectIndexManager.saveOrUpdateProject(context, item("mid", lastEdited = 200L))

        val ids = ProjectIndexManager.getAllProjects(context).map { it.projectId }
        assertEquals(listOf("new", "mid", "old"), ids)
    }

    @Test
    fun removeProject_removesOnlyThatEntry() {
        ProjectIndexManager.saveOrUpdateProject(context, item("a"))
        ProjectIndexManager.saveOrUpdateProject(context, item("b"))

        ProjectIndexManager.removeProject(context, "a")

        assertFalse(ProjectIndexManager.containsProjectId(context, "a"))
        assertTrue(ProjectIndexManager.containsProjectId(context, "b"))
        assertNull(ProjectIndexManager.getProjectById(context, "a"))
    }

    @Test
    fun buildDuplicateName_incrementsPastExistingCopies() {
        // No existing copy → "<name> Copy".
        assertEquals("Beam Copy", ProjectIndexManager.buildDuplicateName(context, "Beam"))

        ProjectIndexManager.saveOrUpdateProject(context, item("1", name = "Beam Copy"))
        assertEquals("Beam Copy 2", ProjectIndexManager.buildDuplicateName(context, "Beam"))

        ProjectIndexManager.saveOrUpdateProject(context, item("2", name = "Beam Copy 2"))
        assertEquals("Beam Copy 3", ProjectIndexManager.buildDuplicateName(context, "Beam"))
    }
}
