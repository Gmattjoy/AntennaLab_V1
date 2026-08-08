package com.example.antennalab_v1

import android.content.Context
import com.example.antennalab_v1.model.DesignInput
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.ProjectMeta
import com.example.antennalab_v1.model.ProjectSweepHistoryEntry
import com.example.antennalab_v1.storage.ProjectStorage
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

/**
 * Robolectric coverage for ProjectStorage serialization — exercises the real
 * org.json-backed toJson/fromJson through the public save/load API, which plain
 * JVM tests can't reach.
 *
 * Calibration is NOT serialized (live-only). What is covered here is that sweep
 * PROVENANCE survives, and that legacy saves still containing a calibration blob
 * load without complaint.
 */
@RunWith(RobolectricTestRunner::class)
class ProjectStorageRoundTripTest {

    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    private fun projectWithHistory(): ProjectData {
        val historyEntry = ProjectSweepHistoryEntry(
            recordedAtEpochMs = 123L,
            hardwareName = "NanoVNA-H4",
            sweepStartMHz = 1.0,
            sweepEndMHz = 3.0,
            bestSwr = 1.1,
            isComplete = false,
            actualPointCount = 18,
            requestedPointCount = 26,
            isCalibrated = true
        )
        return ProjectData(
            meta = ProjectMeta(projectName = "Legacy 20 m Dipole"),
            designInput = DesignInput(targetFrequencyMHz = 14.2),
            sweepHistory = listOf(historyEntry)
        )
    }

    /** The saved project file (the JSON containing sweepHistory), located under filesDir. */
    private fun findSavedProjectFile(): File =
        context.filesDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .first { it.readText().contains("\"sweepHistory\"") }

    @Test
    fun saveLoad_preservesSweepProvenanceFlags() {
        ProjectStorage.saveProject(context, projectWithHistory())
        val loaded = ProjectStorage.loadProject(context)

        assertEquals(1, loaded.sweepHistory.size)
        val entry = loaded.sweepHistory.first()
        assertFalse(entry.isComplete)
        assertEquals(18, entry.actualPointCount)
        assertEquals(26, entry.requestedPointCount)
        // "This sweep was measured under calibration" is a fact about a past
        // measurement, and IS persisted — unlike calibration itself.
        assertTrue(entry.isCalibrated)
    }

    @Test
    fun saveLoad_doesNotWriteCalibrationData() {
        ProjectStorage.saveProject(context, projectWithHistory())
        val json = JSONObject(findSavedProjectFile().readText())

        assertFalse(
            "calibration must not be persisted — it is live-only",
            json.has("calibrationData")
        )
    }

    @Test
    fun saveLoad_doesNotWriteUiState() {
        ProjectStorage.saveProject(context, projectWithHistory())
        val json = JSONObject(findSavedProjectFile().readText())

        assertFalse(
            "uiState is a preference, not a project fact — settings never live in ProjectData",
            json.has("uiState")
        )
    }

    @Test
    fun load_defaultsWhenNewKeysAbsent_forLegacySaves() {
        ProjectStorage.saveProject(context, projectWithHistory())

        // Simulate an older save: strip the keys added in later phases.
        val file = findSavedProjectFile()
        val json = JSONObject(file.readText())

        val historyJson = json.getJSONArray("sweepHistory").getJSONObject(0)
        historyJson.remove("isComplete")
        historyJson.remove("actualPointCount")
        historyJson.remove("requestedPointCount")
        historyJson.remove("isCalibrated")

        file.writeText(json.toString())

        val loaded = ProjectStorage.loadProject(context)
        val entry = loaded.sweepHistory.first()

        // Reader defaults keep old saves valid.
        assertTrue("absent isComplete defaults to complete", entry.isComplete)
        assertFalse("absent isCalibrated defaults to uncalibrated", entry.isCalibrated)
    }

    /**
     * THE TEARDOWN GUARD. A project saved before calibration persistence was
     * removed still carries a full `calibrationData` blob — stored session,
     * OSL coefficients, restore policy. Loading it must ignore the blob
     * entirely and preserve every other field. The reader builds ProjectData
     * from named optional lookups with no schema validation, so unknown keys
     * are dropped; this pins that behaviour so nobody adds a rejection path.
     */
    @Test
    fun load_legacyCalibrationBlob_isIgnored_andRestOfProjectSurvives() {
        ProjectStorage.saveProject(context, projectWithHistory())

        val file = findSavedProjectFile()
        val json = JSONObject(file.readText())

        // Exactly the shape the pre-teardown writer produced.
        val legacyCalibration = JSONObject().apply {
            put(
                "storedCalibrationSession",
                JSONObject().apply {
                    put("hardwareDisplayName", "NanoVNA-H4")
                    put("startFrequencyMHz", 1.0)
                    put("endFrequencyMHz", 3.0)
                    put("openCaptured", true)
                    put("shortCaptured", true)
                    put("loadCaptured", true)
                    put("timestampLabel", "12 Jan 2026  9:15 AM")
                    put("capturedAtEpochMs", 1_700_000_000_000L)
                    put("captureSource", "WIZARD")
                    put("capturedProtocolFamily", "NANOVNA")
                    put("capturedInstrumentIdentityText", "NanoVNA-H4 FW v1.2")
                    put("capturedSessionKey", "legacy-session-key")
                    put(
                        "correction",
                        JSONObject().apply {
                            put("frequencyHz", JSONArray(listOf(1_000_000L, 2_000_000L)))
                            put("directivityRe", JSONArray(listOf(0.05, 0.06)))
                            put("directivityIm", JSONArray(listOf(-0.03, -0.02)))
                            put("sourceMatchRe", JSONArray(listOf(0.10, 0.11)))
                            put("sourceMatchIm", JSONArray(listOf(0.08, 0.09)))
                            put("reflectionTrackingRe", JSONArray(listOf(0.85, 0.86)))
                            put("reflectionTrackingIm", JSONArray(listOf(0.05, 0.04)))
                        }
                    )
                }
            )
            put("lastCalibrationSavedEpochMs", 1_700_000_000_000L)
            put("lastCalibrationStatusSummary", "Complete calibration is available.")
            put("restorePolicy", "RESTORE_IF_COMPATIBLE")
            put("restoredFromStorage", true)
        }
        json.put("calibrationData", legacyCalibration)
        file.writeText(json.toString())

        // Must not throw.
        val loaded = ProjectStorage.loadProject(context)

        // Everything that is not calibration survives untouched.
        assertEquals("Legacy 20 m Dipole", loaded.meta.projectName)
        assertEquals(14.2, loaded.designInput.targetFrequencyMHz, 1e-9)
        assertEquals(1, loaded.sweepHistory.size)
        val entry = loaded.sweepHistory.first()
        assertEquals("NanoVNA-H4", entry.hardwareName)
        assertEquals(18, entry.actualPointCount)
        assertEquals(26, entry.requestedPointCount)
        assertTrue(entry.isCalibrated)
        assertFalse(entry.isComplete)
    }

    /**
     * THE BOUNDARY GUARD (slice 5b). A project saved before ProjectUiState was
     * torn out still carries a `uiState` blob — lastOpenedSection,
     * lastExpandedCard, hasSeenProjectIntro. Loading it must ignore the blob
     * entirely and preserve every other field.
     *
     * Same mechanism as the calibration guard above, and pinned for the same
     * reason: fromJson builds ProjectData from named optional lookups with no
     * schema validation, so a key nobody reads is simply dropped. There is
     * deliberately NO migration — the orphan key just disappears the next time
     * the project is saved, which [saveLoad_doesNotWriteUiState] covers.
     */
    @Test
    fun load_legacyUiStateBlob_isIgnored_andRestOfProjectSurvives() {
        ProjectStorage.saveProject(context, projectWithHistory())

        val file = findSavedProjectFile()
        val json = JSONObject(file.readText())

        // Exactly the shape the pre-teardown writer produced.
        json.put(
            "uiState",
            JSONObject().apply {
                put("lastOpenedSection", "TESTING")
                put("lastExpandedCard", "TEST_STATUS")
                put("hasSeenProjectIntro", true)
            }
        )
        file.writeText(json.toString())

        // Must not throw.
        val loaded = ProjectStorage.loadProject(context)

        // Everything that is not uiState survives untouched.
        assertEquals("Legacy 20 m Dipole", loaded.meta.projectName)
        assertEquals(14.2, loaded.designInput.targetFrequencyMHz, 1e-9)
        assertEquals(1, loaded.sweepHistory.size)
        val entry = loaded.sweepHistory.first()
        assertEquals("NanoVNA-H4", entry.hardwareName)
        assertEquals(18, entry.actualPointCount)
        assertEquals(26, entry.requestedPointCount)
        assertTrue(entry.isCalibrated)
        assertFalse(entry.isComplete)

        // And re-saving drops the orphan rather than preserving it.
        ProjectStorage.saveProject(context, loaded)
        assertFalse(JSONObject(findSavedProjectFile().readText()).has("uiState"))
    }
}
