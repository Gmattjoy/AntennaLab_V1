package com.example.antennalab_v1

import android.content.Context
import com.example.antennalab_v1.model.AntennaClassification
import com.example.antennalab_v1.model.DiscoverySnapshot
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.ProjectMeta
import com.example.antennalab_v1.model.ProjectSource
import com.example.antennalab_v1.model.ProjectSweepHistoryEntry
import com.example.antennalab_v1.model.ProjectSweepHistoryMode
import com.example.antennalab_v1.storage.ProjectStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

/**
 * Robolectric coverage for discovery-snapshot persistence: the DiscoverySnapshot
 * serialization round-trip plus the two public discovery entry points
 * (saveDiscoveryAsNewProject / applyDiscoveryToCurrentProject).
 */
@RunWith(RobolectricTestRunner::class)
class DiscoverySnapshotPersistenceTest {

    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    @Before
    fun clearProjects() {
        File(context.filesDir, "projects").deleteRecursively()
    }

    private fun snapshot(
        classification: AntennaClassification = AntennaClassification.GROUND_PLANE,
        returnLossDb: Double? = -17.5
    ) = DiscoverySnapshot(
        capturedAtEpochMs = 555L,
        antennaClassificationGuess = classification,
        hardwareName = "LiteVNA64 v0.3.3",
        sweepStartMHz = 1.0,
        sweepEndMHz = 30.0,
        bestFrequencyMHz = 14.2,
        bestSwr = 1.3,
        returnLossDb = returnLossDb,
        summaryLabel = "Discovery summary",
        operatorNotes = "Operator notes"
    )

    @Test
    fun discoverySnapshot_survivesSaveLoad() {
        val snap = snapshot()
        val project = ProjectData(
            meta = ProjectMeta(projectId = "disc-1"),
            discoverySnapshot = snap
        )

        ProjectStorage.saveProject(context, project)
        val loaded = ProjectStorage.loadProject(context)

        // Data-class equality checks every field, including nullable returnLossDb.
        assertEquals(snap, loaded.discoverySnapshot)
    }

    @Test
    fun discoverySnapshot_nullReturnLoss_survivesAsNull() {
        val snap = snapshot(classification = AntennaClassification.DIPOLE, returnLossDb = null)
        ProjectStorage.saveProject(
            context,
            ProjectData(meta = ProjectMeta(projectId = "disc-2"), discoverySnapshot = snap)
        )

        val loaded = ProjectStorage.loadProject(context)
        assertNull(loaded.discoverySnapshot?.returnLossDb)
        assertEquals(snap, loaded.discoverySnapshot)
    }

    @Test
    fun saveDiscoveryAsNewProject_createsNamedDiscoveryProject() {
        val snap = snapshot(classification = AntennaClassification.GROUND_PLANE)

        val created = ProjectStorage.saveDiscoveryAsNewProject(
            context = context,
            sourceProject = ProjectData(),
            discoverySnapshot = snap,
            historyEntry = ProjectSweepHistoryEntry(bestSwr = 1.2)
        )

        assertTrue(created.meta.projectId.isNotBlank())
        assertEquals("Discovery Ground Plane", created.meta.projectName)
        assertEquals(AntennaClassification.GROUND_PLANE, created.antennaClassification)
        assertEquals(snap, created.discoverySnapshot)
        assertEquals(1, created.sweepHistory.size)
        assertEquals(ProjectSweepHistoryMode.DISCOVERY_APPLIED, created.sweepHistory.first().mode)
        assertEquals(ProjectSource.LAB_UNKNOWN_DISCOVERY, created.versionInfo.appDataSource)

        // It was persisted and made active.
        val loaded = ProjectStorage.loadProject(context)
        assertEquals(created.meta.projectId, loaded.meta.projectId)
        assertEquals(snap, loaded.discoverySnapshot)
    }

    @Test
    fun applyDiscoveryToCurrentProject_appliesAndPersists() {
        val snap = snapshot(classification = AntennaClassification.YAGI)

        val updated = ProjectStorage.applyDiscoveryToCurrentProject(
            context = context,
            project = ProjectData(),
            discoverySnapshot = snap,
            historyEntry = ProjectSweepHistoryEntry(bestSwr = 1.4)
        )

        assertEquals(AntennaClassification.YAGI, updated.antennaClassification)
        assertEquals(snap, updated.discoverySnapshot)
        assertEquals(1, updated.sweepHistory.size)
        assertEquals(ProjectSweepHistoryMode.DISCOVERY_APPLIED, updated.sweepHistory.first().mode)

        val loaded = ProjectStorage.loadProject(context)
        assertEquals(snap, loaded.discoverySnapshot)
        assertEquals(AntennaClassification.YAGI, loaded.antennaClassification)
    }
}
