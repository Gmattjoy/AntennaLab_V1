package com.example.antennalab_v1

import com.example.antennalab_v1.features.app.FrequencyChoiceMethod
import com.example.antennalab_v1.features.wizard.CreateAntennaWizardNavigator
import com.example.antennalab_v1.features.wizard.WizardState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Behavior coverage for the create-antenna wizard's step-navigation and
 * state-transition logic, extracted out of the Compose router into the pure
 * [CreateAntennaWizardNavigator]. Verifies bounded step advance/go-back and the
 * antenna-family / frequency-method state transitions against the real
 * WizardState model (no mocking).
 */
class CreateAntennaWizardNavigatorTest {

    // ------------------------------------------------------------------
    // Step navigation (bounded 1..4)
    // ------------------------------------------------------------------

    @Test
    fun nextStep_advancesThroughEveryStep() {
        assertEquals(2, CreateAntennaWizardNavigator.nextStep(1))
        assertEquals(3, CreateAntennaWizardNavigator.nextStep(2))
        assertEquals(4, CreateAntennaWizardNavigator.nextStep(3))
    }

    @Test
    fun nextStep_clampsAtLastStep() {
        assertEquals(
            CreateAntennaWizardNavigator.LAST_STEP,
            CreateAntennaWizardNavigator.nextStep(CreateAntennaWizardNavigator.LAST_STEP)
        )
        // Out-of-range input is still clamped into the valid range.
        assertEquals(4, CreateAntennaWizardNavigator.nextStep(99))
    }

    @Test
    fun previousStep_goesBackThroughEveryStep() {
        assertEquals(3, CreateAntennaWizardNavigator.previousStep(4))
        assertEquals(2, CreateAntennaWizardNavigator.previousStep(3))
        assertEquals(1, CreateAntennaWizardNavigator.previousStep(2))
    }

    @Test
    fun previousStep_clampsAtFirstStep() {
        assertEquals(
            CreateAntennaWizardNavigator.FIRST_STEP,
            CreateAntennaWizardNavigator.previousStep(CreateAntennaWizardNavigator.FIRST_STEP)
        )
        assertEquals(1, CreateAntennaWizardNavigator.previousStep(-3))
    }

    @Test
    fun forwardThenBack_returnsToOriginalStep() {
        val start = 2
        val advanced = CreateAntennaWizardNavigator.nextStep(start)
        assertEquals(start, CreateAntennaWizardNavigator.previousStep(advanced))
    }

    // ------------------------------------------------------------------
    // Antenna-family selection transition (Step 1 / Step 2)
    // ------------------------------------------------------------------

    @Test
    fun applyAntennaType_setsBothTypeAndFinalSelection() {
        val result = CreateAntennaWizardNavigator.applyAntennaType(WizardState(), "Yagi")

        assertEquals("Yagi", result.antennaType)
        assertEquals("Yagi", result.finalSelectedAntennaFamily)
    }

    @Test
    fun applyAntennaType_overwritesAPreviousSelection() {
        val first = CreateAntennaWizardNavigator.applyAntennaType(WizardState(), "Dipole")
        val second = CreateAntennaWizardNavigator.applyAntennaType(first, "Loop")

        assertEquals("Loop", second.antennaType)
        assertEquals("Loop", second.finalSelectedAntennaFamily)
    }

    @Test
    fun applyAntennaType_leavesUnrelatedStateUntouched() {
        val base = WizardState(projectName = "Field Antenna", exactFrequency = "14.2")
        val result = CreateAntennaWizardNavigator.applyAntennaType(base, "Yagi")

        assertEquals("Field Antenna", result.projectName)
        assertEquals("14.2", result.exactFrequency)
    }

    // ------------------------------------------------------------------
    // Frequency-method selection transition (Step 3)
    // ------------------------------------------------------------------

    @Test
    fun applyFrequencyMethod_storesMethodName() {
        val common = CreateAntennaWizardNavigator.applyFrequencyMethod(
            WizardState(), FrequencyChoiceMethod.USE_COMMON_BAND
        )
        assertEquals(FrequencyChoiceMethod.USE_COMMON_BAND.name, common.frequencyChoiceMethodName)

        val exact = CreateAntennaWizardNavigator.applyFrequencyMethod(
            WizardState(), FrequencyChoiceMethod.ENTER_EXACT_FREQUENCY
        )
        assertEquals(FrequencyChoiceMethod.ENTER_EXACT_FREQUENCY.name, exact.frequencyChoiceMethodName)
    }

    @Test
    fun applyFrequencyMethod_nullClearsMethodName() {
        val seeded = CreateAntennaWizardNavigator.applyFrequencyMethod(
            WizardState(), FrequencyChoiceMethod.USE_COMMON_BAND
        )
        val cleared = CreateAntennaWizardNavigator.applyFrequencyMethod(seeded, null)

        assertEquals("", cleared.frequencyChoiceMethodName)
    }
}
