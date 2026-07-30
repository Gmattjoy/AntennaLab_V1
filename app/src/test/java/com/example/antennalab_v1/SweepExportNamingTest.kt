package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.SweepExportNaming
import com.example.antennalab_v1.domain.testing.TouchstoneExport
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Coverage for [SweepExportNaming] — the single home for export filename
 * derivation.
 *
 * Naming is where a quiet bug produces an unopenable file or a silent
 * overwrite, so the degenerate inputs (blank, all-illegal, over-long, non-ASCII)
 * get as much attention as the happy path. Plain JVM, no IO.
 */
class SweepExportNamingTest {

    private fun sweep(startMHz: Double, endMHz: Double) = SweepResult(
        startFrequencyMHz = startMHz,
        endFrequencyMHz = endMHz,
        stepMHz = 0.01,
        points = listOf(
            SweepPoint(
                frequencyMHz = startMHz,
                swr = 1.5,
                returnLossDb = -10.0,
                resistance = 50.0,
                reactance = 0.0
            )
        )
    )

    // ------------------------------------------------------------------
    // Happy path
    // ------------------------------------------------------------------

    @Test
    fun buildFileName_joinsProjectFrequencyAndTimestamp() {
        assertEquals(
            "Dipole20m_14.200MHz_2026-07-30T1815.s1p",
            SweepExportNaming.buildFileName(
                projectName = "Dipole20m",
                centreFrequencyMHz = 14.2,
                timestampLabel = "2026-07-30T1815"
            )
        )
    }

    @Test
    fun buildFileName_usesTheSpanCentreNotTheStartEdge() {
        // 14.0..14.4 -> centre 14.2. A name built off startFrequencyMHz would
        // read 14.000MHz and mislabel the sweep.
        assertEquals(
            "Dipole_14.200MHz_stamp.s1p",
            SweepExportNaming.buildFileName(
                projectName = "Dipole",
                result = sweep(14.0, 14.4),
                timestampLabel = "stamp"
            )
        )
    }

    @Test
    fun centreFrequencyMHz_averagesTheSpan() {
        assertEquals(14.2, SweepExportNaming.centreFrequencyMHz(sweep(14.0, 14.4)), 1e-9)
        assertEquals(145.0, SweepExportNaming.centreFrequencyMHz(sweep(144.0, 146.0)), 1e-9)
    }

    @Test
    fun buildFileName_formatsFrequencyToThreeDecimalsInRootLocale() {
        // A comma-decimal locale would emit "14,200MHz", which reads as a
        // thousands separator in a filename.
        val name = SweepExportNaming.buildFileName(
            projectName = "p",
            centreFrequencyMHz = 14.2,
            timestampLabel = ""
        )
        assertTrue(name.contains("14.200MHz"))
        assertFalse(name.contains(","))
    }

    // ------------------------------------------------------------------
    // Sanitising
    // ------------------------------------------------------------------

    @Test
    fun buildFileName_collapsesUnsafeCharacterRunsToSingleUnderscores() {
        assertEquals(
            "My_Project_14.200MHz_2026.s1p",
            SweepExportNaming.buildFileName(
                projectName = "My   Project!!!",
                centreFrequencyMHz = 14.2,
                timestampLabel = "2026"
            )
        )
    }

    @Test
    fun buildFileName_leavesNoLeadingOrTrailingUnderscore() {
        val name = SweepExportNaming.buildFileName(
            projectName = "  ///dipole///  ",
            centreFrequencyMHz = 0.0,
            timestampLabel = ""
        )
        assertEquals("dipole.s1p", name)
        assertFalse(name.startsWith("_"))
        assertFalse(name.substringBefore('.').endsWith("_"))
    }

    @Test
    fun sanitiseSegment_stripsEdgesAndCollapsesRuns() {
        assertEquals("a_b", SweepExportNaming.sanitiseSegment("  a / b  "))
        assertEquals("", SweepExportNaming.sanitiseSegment("///"))
        assertEquals("keep.dots-and_dashes", SweepExportNaming.sanitiseSegment("keep.dots-and_dashes"))
    }

    // ------------------------------------------------------------------
    // Degenerate input — must never produce a dotfile or an empty stem
    // ------------------------------------------------------------------

    @Test
    fun buildFileName_fallsBackForABlankProjectName() {
        assertEquals(
            "sweep.s1p",
            SweepExportNaming.buildFileName(
                projectName = "",
                centreFrequencyMHz = 0.0,
                timestampLabel = ""
            )
        )
    }

    @Test
    fun buildFileName_fallsBackWhenTheNameSanitisesAwayEntirely() {
        assertEquals(
            "sweep.s1p",
            SweepExportNaming.buildFileName(
                projectName = "///",
                centreFrequencyMHz = 0.0,
                timestampLabel = ""
            )
        )
    }

    @Test
    fun buildFileName_fallsBackForANonAsciiName() {
        // A CJK project name reduces away under the ASCII allow-list; the
        // result must still be a usable filename.
        val name = SweepExportNaming.buildFileName(
            projectName = "天線",
            centreFrequencyMHz = 0.0,
            timestampLabel = ""
        )
        assertEquals("sweep.s1p", name)
    }

    @Test
    fun buildFileName_dropsTheFrequencySegmentWhenThereIsNoRealSpan() {
        // An empty sweep centres on 0.0; "0.000MHz" in the name would be a lie
        // dressed as data.
        val name = SweepExportNaming.buildFileName(
            projectName = "Dipole",
            centreFrequencyMHz = 0.0,
            timestampLabel = "2026"
        )
        assertEquals("Dipole_2026.s1p", name)
        assertFalse(name.contains("MHz"))
    }

    @Test
    fun buildFileName_omitsABlankTimestampWithoutDoublingTheSeparator() {
        val name = SweepExportNaming.buildFileName(
            projectName = "Dipole",
            centreFrequencyMHz = 14.2,
            timestampLabel = "   "
        )
        assertEquals("Dipole_14.200MHz.s1p", name)
        assertFalse(name.contains("__"))
    }

    @Test
    fun buildFileName_appliesTheExtensionOnlyOnce() {
        // A project literally named "dipole.s1p" must not yield
        // "dipole.s1p_....s1p".
        val name = SweepExportNaming.buildFileName(
            projectName = "dipole.s1p",
            centreFrequencyMHz = 0.0,
            timestampLabel = ""
        )
        assertEquals("dipole.s1p", name)
    }

    @Test
    fun buildFileName_truncatesAnOverLongNameKeepingTheExtension() {
        val name = SweepExportNaming.buildFileName(
            projectName = "a".repeat(400),
            centreFrequencyMHz = 14.2,
            timestampLabel = "2026-07-30T1815"
        )
        assertTrue(name.endsWith(".s1p"))
        // Stem capped at 120, so the whole name stays well inside the usual
        // 255-byte filesystem limit.
        assertEquals(120, name.substringBeforeLast('.').length)
        assertTrue(name.length < 130)
    }

    // ------------------------------------------------------------------
    // Collision avoidance
    // ------------------------------------------------------------------

    @Test
    fun nextAvailableName_returnsTheCandidateWhenFree() {
        assertEquals(
            "sweep.s1p",
            SweepExportNaming.nextAvailableName("sweep.s1p", emptySet())
        )
    }

    @Test
    fun nextAvailableName_suffixesFromTwoUpwards() {
        assertEquals(
            "sweep_2.s1p",
            SweepExportNaming.nextAvailableName("sweep.s1p", setOf("sweep.s1p"))
        )
        assertEquals(
            "sweep_3.s1p",
            SweepExportNaming.nextAvailableName(
                "sweep.s1p",
                setOf("sweep.s1p", "sweep_2.s1p")
            )
        )
        assertEquals(
            "sweep_4.s1p",
            SweepExportNaming.nextAvailableName(
                "sweep.s1p",
                setOf("sweep.s1p", "sweep_2.s1p", "sweep_3.s1p")
            )
        )
    }

    @Test
    fun nextAvailableName_isStableForTheSameInputs() {
        val existing = setOf("sweep.s1p")
        assertEquals(
            SweepExportNaming.nextAvailableName("sweep.s1p", existing),
            SweepExportNaming.nextAvailableName("sweep.s1p", existing)
        )
    }

    @Test
    fun nextAvailableName_handlesANameWithNoExtension() {
        assertEquals(
            "sweep_2",
            SweepExportNaming.nextAvailableName("sweep", setOf("sweep"))
        )
    }

    // ------------------------------------------------------------------
    // Slice-A regression — TouchstoneExport delegates here now
    // ------------------------------------------------------------------

    @Test
    fun touchstoneSuggestFileName_stillMatchesItsSliceABehaviour() {
        // Delegation must not change these results.
        assertEquals(
            "Dipole20m_2026-07-30.s1p",
            TouchstoneExport.suggestFileName("Dipole20m", "2026-07-30")
        )
        assertEquals(
            "My_Project_2026-07-30.s1p",
            TouchstoneExport.suggestFileName("My Project!", "2026-07-30")
        )
        assertEquals("a_b.s1p", TouchstoneExport.suggestFileName("a / b", ""))
        assertEquals("sweep.s1p", TouchstoneExport.suggestFileName("", ""))
        assertEquals("sweep.s1p", TouchstoneExport.suggestFileName("///", ""))
        assertEquals("sweep_2026.s1p", TouchstoneExport.suggestFileName("!!!", "2026"))
    }
}
