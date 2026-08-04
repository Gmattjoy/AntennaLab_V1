package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.SweepCsvExport
import com.example.antennalab_v1.domain.testing.TouchstoneExport
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Coverage for [SweepCsvExport] — the CSV provenance header.
 *
 * The leak this plugs: CSV previously carried no hardware or calibration field at
 * all, so a simulated sweep's CSV was indistinguishable from a real measurement.
 * That is the §10c.7 class of defect, and it matters more now that a debug toggle
 * can mint synthetic sweeps with no hardware attached.
 *
 * Plain JVM, no Compose, no IO.
 */
class SweepCsvExportTest {

    private fun sweep(
        hardwareProfile: String,
        isCalibrated: Boolean = false
    ) = SweepResult(
        startFrequencyMHz = 14.0,
        endFrequencyMHz = 14.4,
        stepMHz = 0.01,
        points = listOf(
            SweepPoint(
                frequencyMHz = 14.2,
                swr = 1.5,
                returnLossDb = -10.0,
                resistance = 50.0,
                reactance = 0.0
            )
        ),
        hardwareProfile = hardwareProfile,
        isCalibrated = isCalibrated
    )

    @Test
    fun header_marksASimulatedSweepAsSimulated() {
        assertEquals(
            "# hardware=SIMULATED  calibrated=false",
            SweepCsvExport.buildProvenanceHeader(sweep("SIMULATED"))
        )
    }

    @Test
    fun header_namesRealHardwareWhenTheDriverIdentifiedItself() {
        assertEquals(
            "# hardware=LiteVNA64 v0.3.3  calibrated=true",
            SweepCsvExport.buildProvenanceHeader(
                sweep("LiteVNA64 v0.3.3", isCalibrated = true)
            )
        )
    }

    @Test
    fun header_saysUnknownRatherThanGuessingForAnUnnamedSweep() {
        // SweepResult.hardwareProfile defaults to "" (the neutral default from the
        // §10c.7 fix). Inventing "real" or "simulated" here is exactly the bug
        // that default exists to prevent.
        assertEquals(
            "# hardware=unknown  calibrated=false",
            SweepCsvExport.buildProvenanceHeader(sweep(""))
        )
    }

    @Test
    fun header_reportsTheCalibrationFlagBothWays() {
        assertTrue(
            SweepCsvExport.buildProvenanceHeader(sweep("X", isCalibrated = true))
                .contains("calibrated=true")
        )
        assertTrue(
            SweepCsvExport.buildProvenanceHeader(sweep("X", isCalibrated = false))
                .contains("calibrated=false")
        )
    }

    @Test
    fun header_isCommentPrefixedSoParsersSkipIt() {
        assertTrue(SweepCsvExport.buildProvenanceHeader(sweep("SIMULATED")).startsWith("#"))
    }

    @Test
    fun header_agreesWithTheTouchstoneInstrumentLineForTheSameSweep() {
        // The two export formats must not disagree about what produced a sweep.
        listOf("SIMULATED", "LiteVNA64 v0.3.3", "").forEach { profile ->
            val result = sweep(profile)
            val csvName = SweepCsvExport.buildProvenanceHeader(result)
                .substringAfter("hardware=")
                .substringBefore("  calibrated=")
            val s1pName = TouchstoneExport.buildS1p(result)
                .lineSequence()
                .first { it.startsWith("! Instrument:") }
                .removePrefix("! Instrument:")
                .trim()
            assertEquals("profile '$profile' disagrees between CSV and .s1p", s1pName, csvName)
        }
    }
}
