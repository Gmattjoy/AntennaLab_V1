package com.example.antennalab_v1

import android.content.Context
import com.example.antennalab_v1.model.ProjectCalibrationData
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.ProjectSweepHistoryEntry
import com.example.antennalab_v1.model.testing.CalibrationSession
import com.example.antennalab_v1.model.testing.OslCalibrationCoefficients
import com.example.antennalab_v1.storage.ProjectStorage
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
 */
@RunWith(RobolectricTestRunner::class)
class ProjectStorageRoundTripTest {

    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    private val delta = 1e-9

    private fun sampleCoefficients() = OslCalibrationCoefficients(
        frequencyHz = listOf(1_000_000L, 2_000_000L, 3_000_000L),
        directivityRe = listOf(0.05, 0.06, 0.07),
        directivityIm = listOf(-0.03, -0.02, -0.01),
        sourceMatchRe = listOf(0.10, 0.11, 0.12),
        sourceMatchIm = listOf(0.08, 0.09, 0.10),
        reflectionTrackingRe = listOf(0.85, 0.86, 0.87),
        reflectionTrackingIm = listOf(0.05, 0.04, 0.03)
    )

    private fun projectWithCalibrationAndHistory(): ProjectData {
        val session = CalibrationSession(
            hardwareDisplayName = "NanoVNA-H4",
            startFrequencyMHz = 1.0,
            endFrequencyMHz = 3.0,
            openCaptured = true,
            shortCaptured = true,
            loadCaptured = true,
            correction = sampleCoefficients()
        )
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
            sweepHistory = listOf(historyEntry),
            calibrationData = ProjectCalibrationData(storedCalibrationSession = session)
        )
    }

    /** The saved project file (the JSON containing sweepHistory), located under filesDir. */
    private fun findSavedProjectFile(): File =
        context.filesDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .first { it.readText().contains("\"sweepHistory\"") }

    @Test
    fun saveLoad_preservesCalibrationCoefficientsAndSweepFlags() {
        ProjectStorage.saveProject(context, projectWithCalibrationAndHistory())
        val loaded = ProjectStorage.loadProject(context)

        // Sweep history flags survive.
        assertEquals(1, loaded.sweepHistory.size)
        val entry = loaded.sweepHistory.first()
        assertFalse(entry.isComplete)
        assertEquals(18, entry.actualPointCount)
        assertEquals(26, entry.requestedPointCount)
        assertTrue(entry.isCalibrated)

        // Calibration coefficients survive, array-for-array.
        val original = sampleCoefficients()
        val restored = loaded.calibrationData.storedCalibrationSession?.correction
        assertNotNull("correction coefficients should survive", restored)
        requireNotNull(restored)
        assertTrue(restored.isUsable)
        assertEquals(original.frequencyHz, restored.frequencyHz)
        assertDoubleList(original.directivityRe, restored.directivityRe)
        assertDoubleList(original.directivityIm, restored.directivityIm)
        assertDoubleList(original.sourceMatchRe, restored.sourceMatchRe)
        assertDoubleList(original.sourceMatchIm, restored.sourceMatchIm)
        assertDoubleList(original.reflectionTrackingRe, restored.reflectionTrackingRe)
        assertDoubleList(original.reflectionTrackingIm, restored.reflectionTrackingIm)
    }

    @Test
    fun load_defaultsWhenNewKeysAbsent_forLegacySaves() {
        ProjectStorage.saveProject(context, projectWithCalibrationAndHistory())

        // Simulate an older save: strip the keys added in later phases.
        val file = findSavedProjectFile()
        val json = JSONObject(file.readText())

        val historyJson = json.getJSONArray("sweepHistory").getJSONObject(0)
        historyJson.remove("isComplete")
        historyJson.remove("actualPointCount")
        historyJson.remove("requestedPointCount")
        historyJson.remove("isCalibrated")

        json.getJSONObject("calibrationData")
            .getJSONObject("storedCalibrationSession")
            .remove("correction")

        file.writeText(json.toString())

        val loaded = ProjectStorage.loadProject(context)
        val entry = loaded.sweepHistory.first()

        // Reader defaults keep old saves valid.
        assertTrue("absent isComplete defaults to complete", entry.isComplete)
        assertFalse("absent isCalibrated defaults to uncalibrated", entry.isCalibrated)
        assertNull(
            "absent correction defaults to null",
            loaded.calibrationData.storedCalibrationSession?.correction
        )
    }

    private fun assertDoubleList(expected: List<Double>, actual: List<Double>) {
        assertEquals(expected.size, actual.size)
        expected.forEachIndexed { i, value -> assertEquals(value, actual[i], delta) }
    }
}
