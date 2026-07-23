package com.example.antennalab_v1

import com.example.antennalab_v1.features.workspace.DesignWorkspaceController
import com.example.antennalab_v1.model.CalculatedDesign
import com.example.antennalab_v1.model.ConductorMaterial
import com.example.antennalab_v1.model.DesignInput
import com.example.antennalab_v1.model.MaterialConfig
import com.example.antennalab_v1.model.PriorityMode
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.ProjectMeta
import com.example.antennalab_v1.model.AntennaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavior coverage for the DesignWorkspaceScreen display logic extracted into
 * the pure [DesignWorkspaceController]: the locked-summary sections, the
 * conditional calculated-results line building, and millimetre formatting.
 * Real ProjectData / CalculatedDesign model, no mocking. Pure logic, so plain
 * JVM (no Robolectric needed).
 */
class DesignWorkspaceControllerTest {

    // ------------------------------------------------------------------
    // Locked design summary
    // ------------------------------------------------------------------

    @Test
    fun buildDesignSummarySections_groupsIdentityAndMaterialLines() {
        val project = ProjectData(
            meta = ProjectMeta(projectName = "Field Dipole"),
            designInput = DesignInput(
                antennaType = AntennaType.DIPOLE,
                targetFrequencyMHz = 14.2,
                priorityMode = PriorityMode.BALANCED
            ),
            materialConfig = MaterialConfig(
                conductorMaterial = ConductorMaterial.COPPER,
                conductorDiameterMm = 2.0,
                buildNotes = "Solder the feed carefully"
            )
        )

        val sections = DesignWorkspaceController.buildDesignSummarySections(project)

        assertEquals(2, sections.size)
        assertEquals(
            listOf(
                "Project: Field Dipole",
                "Antenna Type: DIPOLE",
                "Target Frequency: 14.2 MHz",
                "Priority Mode: BALANCED"
            ),
            sections[0]
        )
        assertEquals(
            listOf(
                "Conductor Material: COPPER",
                "Conductor Diameter: 2.0 mm",
                "Build Notes: Solder the feed carefully"
            ),
            sections[1]
        )
    }

    @Test
    fun buildDesignSummarySections_blankBuildNotesShowNone() {
        val project = ProjectData(materialConfig = MaterialConfig(buildNotes = ""))

        val notesLine = DesignWorkspaceController.buildDesignSummarySections(project)[1].last()
        assertEquals("Build Notes: None", notesLine)
    }

    // ------------------------------------------------------------------
    // Calculated-results line building
    // ------------------------------------------------------------------

    @Test
    fun buildElementLengthLines_emptyForNoElements_numberedWhenPresent() {
        assertEquals(emptyList<String>(), DesignWorkspaceController.buildElementLengthLines(CalculatedDesign()))

        val design = CalculatedDesign(elementLengthsMm = listOf(1000.0, 950.5))
        assertEquals(
            listOf("Element 1: 1000.0 mm (1.000 m)", "Element 2: 950.5 mm"),
            DesignWorkspaceController.buildElementLengthLines(design)
        )
    }

    @Test
    fun buildElementSpacingLines_emptyForNoSpacing_numberedWhenPresent() {
        assertEquals(emptyList<String>(), DesignWorkspaceController.buildElementSpacingLines(CalculatedDesign()))

        val design = CalculatedDesign(elementSpacingMm = listOf(300.0))
        assertEquals(
            listOf("Spacing 1: 300.0 mm"),
            DesignWorkspaceController.buildElementSpacingLines(design)
        )
    }

    @Test
    fun boomLengthLine_nullWhenNonPositive_formattedWhenPresent() {
        assertNull(DesignWorkspaceController.boomLengthLine(CalculatedDesign(boomLengthMm = 0.0)))
        assertEquals(
            "Boom Length: 1200.0 mm (1.200 m)",
            DesignWorkspaceController.boomLengthLine(CalculatedDesign(boomLengthMm = 1200.0))
        )
    }

    @Test
    fun feedPointGapLine_nullWhenNonPositive_formattedWhenPresent() {
        assertNull(DesignWorkspaceController.feedPointGapLine(CalculatedDesign(feedPointGapMm = 0.0)))
        assertEquals(
            "Feed Point Gap: 5.0 mm",
            DesignWorkspaceController.feedPointGapLine(CalculatedDesign(feedPointGapMm = 5.0))
        )
    }

    @Test
    fun matchingMethodLine_nullWhenBlank_labelledWhenPresent() {
        assertNull(DesignWorkspaceController.matchingMethodLine(CalculatedDesign(matchingMethod = "")))
        assertEquals(
            "Matching Method: Gamma Match",
            DesignWorkspaceController.matchingMethodLine(CalculatedDesign(matchingMethod = "Gamma Match"))
        )
    }

    // ------------------------------------------------------------------
    // Formatting
    // ------------------------------------------------------------------

    @Test
    fun formatMillimetres_switchesToMetresAboveOneThousand() {
        assertEquals("250.0 mm", DesignWorkspaceController.formatMillimetres(250.0))
        assertTrue(DesignWorkspaceController.formatMillimetres(1500.0).contains("(1.500 m)"))
    }
}
