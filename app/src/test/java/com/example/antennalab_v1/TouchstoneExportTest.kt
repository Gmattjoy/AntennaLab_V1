package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.TouchstoneExport
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Coverage for [TouchstoneExport] — the pure .s1p text builder.
 *
 * The S11 pair is hand-derived, not read back from the engine. For R = 50,
 * X = +50 against a 50 ohm reference:
 *
 *   gamma = (Z - Z0) / (Z + Z0) = j50 / (100 + j50)
 *         = j50 * (100 - j50) / (100^2 + 50^2)
 *         = (2500 + j5000) / 12500
 *         = 0.2 + j0.4            (exact)
 *
 * and its capacitive mirror (X = -50) is 0.2 - j0.4.
 *
 * Plain JVM, real model types, no file IO — this object writes no files.
 */
class TouchstoneExportTest {

    private fun point(
        frequencyMHz: Double,
        resistance: Double = 50.0,
        reactance: Double = 0.0
    ) = SweepPoint(
        frequencyMHz = frequencyMHz,
        swr = 1.5,
        returnLossDb = -10.0,
        resistance = resistance,
        reactance = reactance
    )

    private fun sweep(
        points: List<SweepPoint>,
        hardwareProfile: String = "LiteVNA64 v0.3.3",
        isCalibrated: Boolean = false,
        calibrationLabel: String = "",
        isComplete: Boolean = true,
        requestedPointCount: Int = points.size,
        actualPointCount: Int = points.size
    ) = SweepResult(
        startFrequencyMHz = points.firstOrNull()?.frequencyMHz ?: 0.0,
        endFrequencyMHz = points.lastOrNull()?.frequencyMHz ?: 0.0,
        stepMHz = if (points.size > 1) points[1].frequencyMHz - points[0].frequencyMHz else 0.0,
        points = points,
        requestedPointCount = requestedPointCount,
        actualPointCount = actualPointCount,
        isComplete = isComplete,
        hardwareProfile = hardwareProfile,
        isCalibrated = isCalibrated,
        calibrationLabel = calibrationLabel
    )

    /** Data rows only — every line that is not a comment or the option line. */
    private fun dataRows(text: String): List<String> =
        text.split("\r\n")
            .filter { it.isNotBlank() && !it.startsWith("!") && !it.startsWith("#") }

    // ------------------------------------------------------------------
    // Option line and framing
    // ------------------------------------------------------------------

    @Test
    fun optionLine_isTheNanoVnaSaverConvention() {
        // Hz, S-parameters, real/imaginary, 50 ohm reference — the muscle
        // memory this export exists to respect (spec 2.3).
        assertEquals("# Hz S RI R 50", TouchstoneExport.OPTION_LINE)
    }

    @Test
    fun buildS1p_containsExactlyOneOptionLine() {
        val text = TouchstoneExport.buildS1p(sweep(listOf(point(14.2))))
        val optionLines = text.split("\r\n").filter { it.startsWith("#") }
        assertEquals(listOf("# Hz S RI R 50"), optionLines)
    }

    @Test
    fun buildS1p_usesCrlfLineEndingsAndEndsWithOne() {
        val text = TouchstoneExport.buildS1p(sweep(listOf(point(14.2))))
        assertTrue(text.contains("\r\n"))
        assertTrue(text.endsWith("\r\n"))
        // No bare LF anywhere: every \n must be preceded by \r.
        assertFalse(Regex("(?<!\r)\n").containsMatchIn(text))
    }

    @Test
    fun buildS1p_putsCommentsBeforeTheOptionLine() {
        val lines = TouchstoneExport.buildS1p(sweep(listOf(point(14.2)))).split("\r\n")
        val optionIndex = lines.indexOfFirst { it.startsWith("#") }
        val firstDataIndex = lines.indexOfFirst {
            it.isNotBlank() && !it.startsWith("!") && !it.startsWith("#")
        }
        assertTrue("option line must precede data", optionIndex < firstDataIndex)
        assertTrue("provenance comments must precede the option line", optionIndex > 0)
    }

    // ------------------------------------------------------------------
    // Data rows — the hand-derived gamma
    // ------------------------------------------------------------------

    @Test
    fun buildS1p_writesFrequencyInWholeHzAndGammaAsRealImaginary() {
        val text = TouchstoneExport.buildS1p(
            sweep(listOf(point(14.2, resistance = 50.0, reactance = 50.0)))
        )
        val rows = dataRows(text)
        assertEquals(1, rows.size)

        val fields = rows.single().trim().split(Regex("\\s+"))
        assertEquals(3, fields.size)
        // 14.2 MHz -> 14200000 Hz, not 1.42E7.
        assertEquals("14200000", fields[0])
        assertEquals("0.200000", fields[1])
        assertEquals("0.400000", fields[2])
    }

    @Test
    fun buildS1p_writesANegativeImaginaryPartForACapacitivePoint() {
        val text = TouchstoneExport.buildS1p(
            sweep(listOf(point(14.2, resistance = 50.0, reactance = -50.0)))
        )
        val fields = dataRows(text).single().trim().split(Regex("\\s+"))
        assertEquals("0.200000", fields[1])
        assertEquals("-0.400000", fields[2])
    }

    @Test
    fun buildS1p_writesZeroGammaForAPerfectMatch() {
        val text = TouchstoneExport.buildS1p(
            sweep(listOf(point(14.2, resistance = 50.0, reactance = 0.0)))
        )
        val fields = dataRows(text).single().trim().split(Regex("\\s+"))
        assertEquals("0.000000", fields[1])
        assertEquals("0.000000", fields[2])
    }

    @Test
    fun buildS1p_writesOneRowPerPointInOrder() {
        val text = TouchstoneExport.buildS1p(
            sweep(listOf(point(14.0), point(14.1), point(14.2)))
        )
        val rows = dataRows(text)
        assertEquals(3, rows.size)
        assertEquals(
            listOf("14000000", "14100000", "14200000"),
            rows.map { it.trim().split(Regex("\\s+"))[0] }
        )
    }

    // ------------------------------------------------------------------
    // Provenance comments
    // ------------------------------------------------------------------

    @Test
    fun buildS1p_recordsInstrumentSpanAndPointCount() {
        val text = TouchstoneExport.buildS1p(
            sweep(listOf(point(14.0), point(14.2)))
        )
        assertTrue(text.contains("! Instrument: LiteVNA64 v0.3.3"))
        assertTrue(text.contains("! Span: 14.000 MHz to 14.200 MHz"))
        assertTrue(text.contains("! Points: 2"))
    }

    @Test
    fun buildS1p_recordsAnUncalibratedSweepHonestly() {
        val text = TouchstoneExport.buildS1p(sweep(listOf(point(14.2))))
        assertTrue(text.contains("! Calibration: none (uncalibrated)"))
    }

    @Test
    fun buildS1p_recordsTheCalibrationLabelWhenCalibrated() {
        val text = TouchstoneExport.buildS1p(
            sweep(
                listOf(point(14.2)),
                isCalibrated = true,
                calibrationLabel = "OSL · LiteVNA64 v0.3.3"
            )
        )
        assertTrue(text.contains("! Calibration: OSL · LiteVNA64 v0.3.3"))
    }

    @Test
    fun buildS1p_fallsBackWhenCalibratedButUnlabelled() {
        val text = TouchstoneExport.buildS1p(
            sweep(listOf(point(14.2)), isCalibrated = true, calibrationLabel = "")
        )
        assertTrue(text.contains("! Calibration: applied"))
    }

    @Test
    fun buildS1p_fallsBackWhenTheInstrumentIsUnknown() {
        val text = TouchstoneExport.buildS1p(
            sweep(listOf(point(14.2)), hardwareProfile = "")
        )
        assertTrue(text.contains("! Instrument: unknown"))
    }

    @Test
    fun buildS1p_flagsAnIncompleteSweepWithBothCounts() {
        // The LiteVNA partial-sweep case: the file must carry the same honesty
        // the in-app flag does.
        val text = TouchstoneExport.buildS1p(
            sweep(
                listOf(point(14.2)),
                isComplete = false,
                requestedPointCount = 101,
                actualPointCount = 77
            )
        )
        assertTrue(text.contains("! WARNING: incomplete sweep — 77 of 101 points captured"))
    }

    @Test
    fun buildS1p_omitsTheWarningForACompleteSweep() {
        val text = TouchstoneExport.buildS1p(sweep(listOf(point(14.2))))
        assertFalse(text.contains("WARNING"))
    }

    // ------------------------------------------------------------------
    // Degenerate input
    // ------------------------------------------------------------------

    @Test
    fun buildS1p_producesAValidHeaderOnlyFileForAnEmptySweep() {
        // Flag-don't-reject: no exception, no data rows, still parseable.
        val text = TouchstoneExport.buildS1p(sweep(emptyList()))
        assertTrue(text.contains("# Hz S RI R 50"))
        assertTrue(text.contains("! Points: 0"))
        assertTrue(dataRows(text).isEmpty())
        assertTrue(text.endsWith("\r\n"))
    }

    // ------------------------------------------------------------------
    // Filename suggestion
    // ------------------------------------------------------------------

    @Test
    fun suggestFileName_joinsProjectAndTimestampWithTheS1pExtension() {
        assertEquals(
            "Dipole20m_2026-07-30.s1p",
            TouchstoneExport.suggestFileName("Dipole20m", "2026-07-30")
        )
    }

    @Test
    fun suggestFileName_sanitisesUnsafeCharacters() {
        // Spaces and punctuation collapse to single underscores; no stray
        // leading or trailing underscore.
        assertEquals(
            "My_Project_2026-07-30.s1p",
            TouchstoneExport.suggestFileName("My Project!", "2026-07-30")
        )
        assertEquals(
            "a_b.s1p",
            TouchstoneExport.suggestFileName("a / b", "")
        )
    }

    @Test
    fun suggestFileName_fallsBackRatherThanProducingADotfile() {
        assertEquals("sweep.s1p", TouchstoneExport.suggestFileName("", ""))
        assertEquals("sweep.s1p", TouchstoneExport.suggestFileName("///", ""))
        assertEquals("sweep_2026.s1p", TouchstoneExport.suggestFileName("!!!", "2026"))
    }

    @Test
    fun fileExtension_isS1p() {
        assertEquals("s1p", TouchstoneExport.FILE_EXTENSION)
    }
}
