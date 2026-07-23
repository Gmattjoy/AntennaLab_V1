package com.example.antennalab_v1

import com.example.antennalab_v1.domain.calculator.CalculationEngineResult
import com.example.antennalab_v1.domain.calculator.ConductorForm
import com.example.antennalab_v1.domain.calculator.calculateDesign
import com.example.antennalab_v1.features.app.FrequencyChoiceMethod
import com.example.antennalab_v1.features.wizard.CreateAntennaWizardController
import com.example.antennalab_v1.model.AntennaType
import com.example.antennalab_v1.model.ConductorMaterial
import com.example.antennalab_v1.model.PriorityMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Behavior coverage for the create-antenna wizard flow logic that was extracted
 * out of the Compose step screens into [CreateAntennaWizardController]. Exercises
 * the real controller against the real domain calculator + ProjectData model
 * (no mocking): antenna-type mapping, live calculation-request assembly, guided
 * readiness lines, finish gating + starter-project assembly, and the Step 3
 * frequency/continue/status/description helpers.
 */
@RunWith(RobolectricTestRunner::class)
class CreateAntennaWizardControllerTest {

    private fun sampleResult(): CalculationEngineResult {
        val request = CreateAntennaWizardController.buildCalculationRequest(
            antennaTypeDisplay = "Dipole",
            frequencyText = "14.2",
            conductorSizeText = "2.0",
            conductorMaterial = ConductorMaterial.COPPER,
            conductorForm = ConductorForm.WIRE,
            priority = PriorityMode.BALANCED
        )!!
        return calculateDesign(request)
    }

    // ------------------------------------------------------------------
    // Antenna-type mapping (Step 4)
    // ------------------------------------------------------------------

    @Test
    fun toModelAntennaType_mapsKnownFamiliesAndFallsBackToOther() {
        assertEquals(AntennaType.DIPOLE, CreateAntennaWizardController.toModelAntennaType("Dipole"))
        assertEquals(AntennaType.MONOPOLE, CreateAntennaWizardController.toModelAntennaType("Vertical"))
        assertEquals(AntennaType.YAGI, CreateAntennaWizardController.toModelAntennaType("Yagi"))
        assertEquals(AntennaType.LOOP, CreateAntennaWizardController.toModelAntennaType("Loop"))
        assertEquals(AntennaType.OTHER, CreateAntennaWizardController.toModelAntennaType("Helical"))
        assertEquals(AntennaType.OTHER, CreateAntennaWizardController.toModelAntennaType(""))
    }

    // ------------------------------------------------------------------
    // Live calculation-request assembly (Step 4)
    // ------------------------------------------------------------------

    @Test
    fun buildCalculationRequest_returnsNullForUnusableFrequency() {
        val invalidInputs = listOf("", "   ", "abc", "0", "-5")
        invalidInputs.forEach { freq ->
            assertNull(
                "expected null request for frequency='$freq'",
                CreateAntennaWizardController.buildCalculationRequest(
                    antennaTypeDisplay = "Dipole",
                    frequencyText = freq,
                    conductorSizeText = "2.0",
                    conductorMaterial = ConductorMaterial.COPPER,
                    conductorForm = ConductorForm.WIRE,
                    priority = PriorityMode.BALANCED
                )
            )
        }
    }

    @Test
    fun buildCalculationRequest_mapsInputsAndFallsBackConductorSize() {
        val request = CreateAntennaWizardController.buildCalculationRequest(
            antennaTypeDisplay = "Yagi",
            frequencyText = "144.3",
            conductorSizeText = "not-a-number",
            conductorMaterial = ConductorMaterial.ALUMINIUM,
            conductorForm = ConductorForm.TUBE,
            priority = PriorityMode.BANDWIDTH
        )

        assertNotNull(request)
        assertEquals(AntennaType.YAGI, request!!.antennaType)
        assertEquals(144.3, request.targetFrequencyMHz, 0.0)
        assertEquals(2.0, request.conductorSizeMm, 0.0) // invalid size -> default fallback
        assertEquals(ConductorMaterial.ALUMINIUM, request.conductorMaterial)
        assertEquals(ConductorForm.TUBE, request.conductorForm)
        assertEquals(PriorityMode.BANDWIDTH, request.priorityMode)
    }

    @Test
    fun buildCalculationRequest_feedsRealCalculationEngine() {
        val result = sampleResult()
        assertNotNull(result.calculatedDesign)
        assertNotNull(result.preview)
        assertEquals(14.2, result.preview.frequencyMHz, 0.0)
    }

    // ------------------------------------------------------------------
    // Guided readiness lines (Step 4)
    // ------------------------------------------------------------------

    @Test
    fun buildReadinessLines_allReadyWhenInputsValid() {
        val lines = CreateAntennaWizardController.buildReadinessLines(
            projectName = "My Dipole",
            parsedFrequency = 14.2,
            conductorSize = "2.0",
            result = sampleResult()
        )

        assertEquals(
            listOf(
                "Project name is ready.",
                "Target frequency is valid.",
                "Conductor size is valid.",
                "Live design result is ready to become a starter project."
            ),
            lines
        )
    }

    @Test
    fun buildReadinessLines_reportsEachMissingInput() {
        val lines = CreateAntennaWizardController.buildReadinessLines(
            projectName = "",
            parsedFrequency = null,
            conductorSize = "abc",
            result = null
        )

        assertEquals(
            listOf(
                "Project name will default to Default.",
                "Target frequency still needs a valid numeric value.",
                "Conductor size is not a valid number. Default calculation fallback is being used.",
                "Live design result is not ready yet."
            ),
            lines
        )
    }

    // ------------------------------------------------------------------
    // Finish gating + starter-project assembly (Step 4)
    // ------------------------------------------------------------------

    @Test
    fun canFinish_requiresBothFrequencyAndResult() {
        val result = sampleResult()
        assertTrue(CreateAntennaWizardController.canFinish(14.2, result))
        assertFalse(CreateAntennaWizardController.canFinish(null, result))
        assertFalse(CreateAntennaWizardController.canFinish(14.2, null))
        assertFalse(CreateAntennaWizardController.canFinish(null, null))
    }

    @Test
    fun buildStarterProject_assemblesFromInputs() {
        val result = sampleResult()

        val project = CreateAntennaWizardController.buildStarterProject(
            projectName = "Field Dipole",
            antennaTypeDisplay = "Dipole",
            frequency = 14.2,
            result = result
        )

        assertEquals("Field Dipole", project.meta.projectName)
        assertEquals(AntennaType.DIPOLE, project.designInput.antennaType)
        assertEquals(14.2, project.designInput.targetFrequencyMHz, 0.0)
        assertEquals(result.calculatedDesign, project.calculatedDesign)
    }

    @Test
    fun buildStarterProject_appliesDefaultsForBlankNameAndMissingResult() {
        val project = CreateAntennaWizardController.buildStarterProject(
            projectName = "   ",
            antennaTypeDisplay = "Helical",
            frequency = null,
            result = null
        )

        assertEquals("Default", project.meta.projectName)
        assertEquals(AntennaType.OTHER, project.designInput.antennaType)
        assertEquals(0.0, project.designInput.targetFrequencyMHz, 0.0)
        // Falls back to the default calculated design when no live result exists.
        assertEquals(com.example.antennalab_v1.model.ProjectData().calculatedDesign, project.calculatedDesign)
    }

    // ------------------------------------------------------------------
    // Step 3: frequency validity + continue gating
    // ------------------------------------------------------------------

    @Test
    fun isExactFrequencyValid_matchesFrequencyParsingRules() {
        assertTrue(CreateAntennaWizardController.isExactFrequencyValid("7.1"))
        assertFalse(CreateAntennaWizardController.isExactFrequencyValid(""))
        assertFalse(CreateAntennaWizardController.isExactFrequencyValid("abc"))
        assertFalse(CreateAntennaWizardController.isExactFrequencyValid("0"))
    }

    @Test
    fun canContinueStep3_gatesByPathAndInputs() {
        // No path chosen yet.
        assertFalse(
            CreateAntennaWizardController.canContinueStep3(null, unitType = "Base Station", exactFrequencyValid = true)
        )

        // Guided band path needs a unit type.
        assertFalse(
            CreateAntennaWizardController.canContinueStep3(
                FrequencyChoiceMethod.USE_COMMON_BAND, unitType = "", exactFrequencyValid = true
            )
        )
        assertTrue(
            CreateAntennaWizardController.canContinueStep3(
                FrequencyChoiceMethod.USE_COMMON_BAND, unitType = "Base Station", exactFrequencyValid = false
            )
        )

        // Exact frequency path needs a valid frequency.
        assertFalse(
            CreateAntennaWizardController.canContinueStep3(
                FrequencyChoiceMethod.ENTER_EXACT_FREQUENCY, unitType = "", exactFrequencyValid = false
            )
        )
        assertTrue(
            CreateAntennaWizardController.canContinueStep3(
                FrequencyChoiceMethod.ENTER_EXACT_FREQUENCY, unitType = "", exactFrequencyValid = true
            )
        )
    }

    // ------------------------------------------------------------------
    // Step 3: readiness status text
    // ------------------------------------------------------------------

    @Test
    fun buildStep3StatusText_coversEachBranch() {
        assertEquals(
            "Choose either the guided band path or the exact frequency path to continue.",
            CreateAntennaWizardController.buildStep3StatusText(null, unitType = "", exactFrequencyValid = false)
        )
        assertEquals(
            "Choose the type of unit to continue with the guided path.",
            CreateAntennaWizardController.buildStep3StatusText(
                FrequencyChoiceMethod.USE_COMMON_BAND, unitType = "", exactFrequencyValid = false
            )
        )
        assertTrue(
            CreateAntennaWizardController.buildStep3StatusText(
                FrequencyChoiceMethod.USE_COMMON_BAND, unitType = "Base Station", exactFrequencyValid = false
            ).startsWith("Guided path is ready.")
        )
        assertEquals(
            "Enter a valid exact frequency in MHz to continue.",
            CreateAntennaWizardController.buildStep3StatusText(
                FrequencyChoiceMethod.ENTER_EXACT_FREQUENCY, unitType = "", exactFrequencyValid = false
            )
        )
        assertTrue(
            CreateAntennaWizardController.buildStep3StatusText(
                FrequencyChoiceMethod.ENTER_EXACT_FREQUENCY, unitType = "", exactFrequencyValid = true
            ).startsWith("Exact frequency path is ready.")
        )
    }

    // ------------------------------------------------------------------
    // Step 3: frequency description
    // ------------------------------------------------------------------

    @Test
    fun describeFrequencyUse_promptsForInvalidInput() {
        assertEquals(
            "Enter a valid frequency in MHz to estimate the radio band.",
            CreateAntennaWizardController.describeFrequencyUse("")
        )
        assertEquals(
            "Enter a valid frequency in MHz to estimate the radio band.",
            CreateAntennaWizardController.describeFrequencyUse("abc")
        )
    }

    @Test
    fun describeFrequencyUse_classifiesBandAndUse() {
        val hf = CreateAntennaWizardController.describeFrequencyUse("7.1")
        assertTrue(hf.contains("HF band"))
        assertTrue(hf.contains("shortwave"))

        val vhfAmateur = CreateAntennaWizardController.describeFrequencyUse("145.5")
        assertTrue(vhfAmateur.contains("VHF band"))
        assertTrue(vhfAmateur.contains("amateur"))

        val uhf = CreateAntennaWizardController.describeFrequencyUse("433.92")
        assertTrue(uhf.contains("UHF band"))
    }
}
