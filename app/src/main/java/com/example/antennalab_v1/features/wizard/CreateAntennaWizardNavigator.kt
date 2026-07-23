package com.example.antennalab_v1.features.wizard

/*
########################################################################
FILE: CreateAntennaWizardNavigator.kt
PACKAGE: com.example.antennalab_v1.features.wizard
LAYER: UI / Wizard / Navigation Control

SYSTEM ROLE
Owns the pure (non-Compose) step-navigation and state-transition rules
for the create-antenna wizard so CreateAntennaWizardScreen can stay a
thin Compose shell + router.

It currently owns:

• bounded step advance / go-back (Step 1..4)
• antenna-family selection state transition (Step 1 / Step 2)
• frequency-method selection state transition (Step 3)

DESIGN INTENT
Keep this layer pure and easy to test/migrate. Mirrors the same UI-free
controller pattern used by SweepWorkspaceController and
CreateAntennaWizardController.

IMPORTANT
This file intentionally avoids Compose APIs and Android dependencies.
########################################################################
*/

import com.example.antennalab_v1.features.app.FrequencyChoiceMethod

/*
########################################################################
SECTION 1000
NAVIGATOR OBJECT
########################################################################
PURPOSE
Stateless navigation + state-transition helpers for the wizard router.
########################################################################
*/
object CreateAntennaWizardNavigator {

    const val FIRST_STEP = 1
    const val LAST_STEP = 4

    /*
    ------------------------------------------------------------
    SECTION 1100
    STEP NAVIGATION
    ------------------------------------------------------------
    PURPOSE
    Advance / go back one step, clamped to the valid step range so the
    router can never route to a non-existent step.
    ------------------------------------------------------------
    */
    fun nextStep(currentStep: Int): Int {
        return (currentStep + 1).coerceIn(FIRST_STEP, LAST_STEP)
    }

    fun previousStep(currentStep: Int): Int {
        return (currentStep - 1).coerceIn(FIRST_STEP, LAST_STEP)
    }

    /*
    ------------------------------------------------------------
    SECTION 1200
    STATE TRANSITIONS
    ------------------------------------------------------------
    PURPOSE
    Structured wizard-state updates for the choices that flow between
    steps.
    ------------------------------------------------------------
    */
    fun applyAntennaType(
        state: WizardState,
        antennaFamily: String
    ): WizardState {
        return state.copy(
            antennaType = antennaFamily,
            finalSelectedAntennaFamily = antennaFamily
        )
    }

    fun applyFrequencyMethod(
        state: WizardState,
        method: FrequencyChoiceMethod?
    ): WizardState {
        return state.copy(
            frequencyChoiceMethodName = method?.name ?: ""
        )
    }
}
