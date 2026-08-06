package com.example.antennalab_v1

import com.example.antennalab_v1.features.app.LoadProjectController
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.ProjectListItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavior coverage for the LoadProjectScreen (Project Manager) display logic
 * extracted into the pure [LoadProjectController]: initial saved-project list
 * resolution, target-frequency / last-edited formatting, and stored-calibration
 * derivation. Real ProjectData / ProjectListItem model, no mocking. Pure logic,
 * so plain JVM (no Robolectric needed).
 */
class LoadProjectControllerTest {

    private fun item(id: String, lastEdited: Long = 1_000L) = ProjectListItem(
        projectId = id,
        name = "Project $id",
        antennaType = "DIPOLE",
        targetFrequencyHz = 14_200_000L,
        lastEditedEpochMillis = lastEdited
    )

    // ------------------------------------------------------------------
    // Initial list resolution
    // ------------------------------------------------------------------

    @Test
    fun resolveInitialProjects_prefersPassedInList() {
        val passedIn = listOf(item("a"))
        val fromIndex = listOf(item("b"), item("c"))

        assertEquals(passedIn, LoadProjectController.resolveInitialProjects(passedIn, fromIndex))
    }

    @Test
    fun resolveInitialProjects_fallsBackToIndexWhenNothingPassedIn() {
        val fromIndex = listOf(item("b"), item("c"))

        assertEquals(fromIndex, LoadProjectController.resolveInitialProjects(emptyList(), fromIndex))
    }

    @Test
    fun resolveInitialProjects_emptyWhenBothEmpty() {
        assertTrue(LoadProjectController.resolveInitialProjects(emptyList(), emptyList()).isEmpty())
    }

    // ------------------------------------------------------------------
    // Formatting
    // ------------------------------------------------------------------

    @Test
    fun formatTargetFrequencyMHz_convertsHzToMHzWithThreeDecimals() {
        assertEquals("14.200", LoadProjectController.formatTargetFrequencyMHz(14_200_000L))
        assertEquals("0.000", LoadProjectController.formatTargetFrequencyMHz(0L))
        assertEquals("144.300", LoadProjectController.formatTargetFrequencyMHz(144_300_000L))
    }

    @Test
    fun formatLastEdited_rendersHumanReadableDate() {
        // 2023-11-14 (mid-November, so the calendar year is stable across time zones).
        val text = LoadProjectController.formatLastEdited(1_700_000_000_000L)
        assertTrue(text.contains("2023"))
        assertTrue(text.contains(":")) // includes a time component
    }

    // No stored-calibration derivation coverage: calibration is live-only, so a
    // saved project carries none and the controller no longer reports on it.
}
