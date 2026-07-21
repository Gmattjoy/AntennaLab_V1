package com.example.antennalab_v1.features.wizard

/*
########################################################################
FILE: CreateAntennaWizardScreen.kt
PACKAGE: com.example.antennalab_v1.features.wizard
LAYER: UI / Wizard Controller

LAST UPDATED 04/04/2026 20:25

SYSTEM ROLE
Central controller for the antenna creation wizard.

This screen:

• holds WizardState
• controls wizard step navigation
• passes data between steps
• creates the final ProjectData

CURRENT DEVELOPMENT ROLE
This version now acts as the wizard shell and provides the shared
top-right app navigation menu at controller level so the wizard remains
consistent with the rest of the root-routed app flow.

IMPORTANT UI RULE
The shared app menu belongs here at controller level, not duplicated
inside individual wizard step screens.
########################################################################
*/

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.antennalab_v1.features.app.AppTopRightMenu
import com.example.antennalab_v1.features.app.FrequencyChoiceMethod
import com.example.antennalab_v1.features.wizard.steps.CreateWizardStep1Screen
import com.example.antennalab_v1.features.wizard.steps.CreateWizardStep2OverviewScreen
import com.example.antennalab_v1.features.wizard.steps.CreateWizardStep3Screen
import com.example.antennalab_v1.features.wizard.steps.Step4LiveDesignWorkspaceScreen
import com.example.antennalab_v1.model.ProjectData

/*
########################################################################
SECTION 1000
WIZARD CONTROLLER ENTRY
------------------------------------------------------------------------
PURPOSE
Owns wizard-level state and provides the shared app-bar shell around
the step router.
########################################################################
*/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAntennaWizardScreen(
    onFinishProject: (ProjectData) -> Unit,
    onCancel: () -> Unit
) {

    /*
    --------------------------------------------------------------------
    WIZARD STATE
    --------------------------------------------------------------------
    */
    var wizardState by remember { mutableStateOf(WizardState()) }
    var currentStep by remember { mutableIntStateOf(1) }
    var frequencyChoiceMethod by remember { mutableStateOf<FrequencyChoiceMethod?>(null) }

    /*
    --------------------------------------------------------------------
    SHARED WIZARD SHELL
    --------------------------------------------------------------------
    */
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Create Antenna Wizard")
                },
                actions = {
                    AppTopRightMenu()
                }
            )
        }
    ) { innerPadding ->

        /*
        ----------------------------------------------------------------
        STEP ROUTER
        ----------------------------------------------------------------
        */
        CreateAntennaWizardStepRouter(
            contentPadding = innerPadding,
            currentStep = currentStep,
            wizardState = wizardState,
            frequencyChoiceMethod = frequencyChoiceMethod,
            onWizardStateChange = { newState ->
                wizardState = newState
            },
            onFrequencyChoiceMethodChange = { newMethod ->
                frequencyChoiceMethod = newMethod
                wizardState = wizardState.copy(
                    frequencyChoiceMethodName = newMethod?.name ?: ""
                )
            },
            onCurrentStepChange = { newStep ->
                currentStep = newStep
            },
            onFinishProject = onFinishProject,
            onCancel = onCancel
        )
    }
}

/*
########################################################################
SECTION 1100
STEP ROUTER
------------------------------------------------------------------------
PURPOSE
Routes the current wizard step to the correct screen while keeping the
shell and shared app menu outside the individual step implementations.
########################################################################
*/
@Composable
private fun CreateAntennaWizardStepRouter(
    contentPadding: PaddingValues,
    currentStep: Int,
    wizardState: WizardState,
    frequencyChoiceMethod: FrequencyChoiceMethod?,
    onWizardStateChange: (WizardState) -> Unit,
    onFrequencyChoiceMethodChange: (FrequencyChoiceMethod?) -> Unit,
    onCurrentStepChange: (Int) -> Unit,
    onFinishProject: (ProjectData) -> Unit,
    onCancel: () -> Unit
) {
    when (currentStep) {

        1 -> CreateWizardStep1Screen(
            antennaType = wizardState.finalSelectedAntennaFamily,
            onAntennaTypeChange = { selectedFamily ->
                onWizardStateChange(
                    wizardState.copy(
                        antennaType = selectedFamily,
                        finalSelectedAntennaFamily = selectedFamily
                    )
                )
            },
            onBack = onCancel,
            onNext = { onCurrentStepChange(2) }
        )

        2 -> CreateWizardStep2OverviewScreen(
            antennaType = wizardState.finalSelectedAntennaFamily,
            onAntennaTypeChange = { selectedFamily ->
                onWizardStateChange(
                    wizardState.copy(
                        antennaType = selectedFamily,
                        finalSelectedAntennaFamily = selectedFamily
                    )
                )
            },
            onBack = { onCurrentStepChange(1) },
            onNext = { onCurrentStepChange(3) }
        )

        3 -> CreateWizardStep3Screen(
            projectName = wizardState.projectName,
            onProjectNameChange = { newProjectName ->
                onWizardStateChange(
                    wizardState.copy(projectName = newProjectName)
                )
            },
            frequencyChoiceMethod = frequencyChoiceMethod,
            onFrequencyChoiceMethodChange = { newMethod ->
                onFrequencyChoiceMethodChange(newMethod)
            },
            unitType = wizardState.unitType,
            onUnitTypeChange = { newUnitType ->
                onWizardStateChange(
                    wizardState.copy(unitType = newUnitType)
                )
            },
            exactFrequency = wizardState.exactFrequency,
            onExactFrequencyChange = { newExactFrequency ->
                onWizardStateChange(
                    wizardState.copy(exactFrequency = newExactFrequency)
                )
            },
            onBack = { onCurrentStepChange(2) },
            onNext = { onCurrentStepChange(4) }
        )

        4 -> Step4LiveDesignWorkspaceScreen(
            antennaType = wizardState.finalSelectedAntennaFamily,
            projectName = wizardState.projectName,
            exactFrequency = wizardState.exactFrequency,
            onBack = { onCurrentStepChange(3) },
            onFinish = { project ->
                onFinishProject(project)
            }
        )
    }
}