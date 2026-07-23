package com.example.antennalab_v1.features.wizard.steps

/*
########################################################################
FILE: Step3ProjectAndFrequencyScreen.kt
PACKAGE: com.example.antennalab_v1.features.wizard.steps
LAYER: UI / Wizard Input Selection

SYSTEM ROLE
Wizard step 3 for project naming, frequency confirmation, and design
intent setup.

CURRENT DEVELOPMENT ROLE
This screen now acts as the project-definition stage after the guided
intake and antenna explanation steps.

Right now this file is responsible for:

• collecting the starter project name
• confirming the frequency-entry path
• supporting guided or direct project setup
• collecting design intent
• validating readiness for Step 4

RULES
- Validation may call helper functions from domain logic
- No formula blocks should be defined in this file

SAFE EDIT AREA
- expand guidance later
- add band suggestion shortcuts later
- improve project naming guidance later
- refine workspace handoff wording later
########################################################################
*/

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.antennalab_v1.features.app.FrequencyChoiceMethod
import com.example.antennalab_v1.features.wizard.CreateAntennaWizardController
import com.example.antennalab_v1.features.wizard.components.ChoicePillRow
import com.example.antennalab_v1.features.wizard.components.DecisionSupportCard
import com.example.antennalab_v1.features.wizard.components.OptionRow
import com.example.antennalab_v1.features.wizard.components.SectionTitle
import com.example.antennalab_v1.features.wizard.components.SelectionSummaryCard
import com.example.antennalab_v1.features.wizard.components.WizardHeader
import com.example.antennalab_v1.features.wizard.components.WizardNav

/*
########################################################################
EDIT SECTION 1001
TOP-LEVEL SCREEN
------------------------------------------------------------------------
PURPOSE
Collects the project name, frequency confirmation path, and design
intent before the user moves into the guided design workspace.

This is the definition step between:

• the antenna explanation stage
• the live design workspace handoff

SAFE EDIT AREA
- add more guided copy here
- add summary callouts here
- refine continue logic here
########################################################################
*/
@Composable
fun CreateWizardStep3Screen(
    projectName: String,
    onProjectNameChange: (String) -> Unit,
    frequencyChoiceMethod: FrequencyChoiceMethod?,
    onFrequencyChoiceMethodChange: (FrequencyChoiceMethod) -> Unit,
    unitType: String,
    onUnitTypeChange: (String) -> Unit,
    exactFrequency: String,
    onExactFrequencyChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {

    /*
    ####################################################################
    EDIT SECTION 1002
    VALIDATION MODEL
    --------------------------------------------------------------------
    PURPOSE
    Builds the validation state that controls hinting and next-step
    availability.
    ####################################################################
    */
    val exactFrequencyValid = CreateAntennaWizardController.isExactFrequencyValid(exactFrequency)
    val frequencyHint = CreateAntennaWizardController.describeFrequencyUse(exactFrequency)

    val canContinue = CreateAntennaWizardController.canContinueStep3(
        frequencyChoiceMethod = frequencyChoiceMethod,
        unitType = unitType,
        exactFrequencyValid = exactFrequencyValid
    )

    /*
    ####################################################################
    EDIT SECTION 1003
    SCREEN CONTENT
    --------------------------------------------------------------------
    PURPOSE
    Renders the guided project-definition flow.
    ####################################################################
    */
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        WizardHeader(
            step = "Step 3 of 4",
            title = "Project definition and frequency confirmation"
        )

        DecisionSupportCard(
            title = "Define the project before opening the workspace",
            body = "This step confirms how the project will start. You can use a guided path or enter an exact operating frequency for a stronger calculated starting point."
        )

        /*
        ################################################################
        EDIT SECTION 1004
        PROJECT NAME INPUT
        ----------------------------------------------------------------
        PURPOSE
        Collects the user-facing project name for the starter project.
        ################################################################
        */
        SectionTitle(
            title = "1. Project name",
            subtitle = "Give this design a simple name so it is easier to save, find, and compare later."
        )

        OutlinedTextField(
            value = projectName,
            onValueChange = onProjectNameChange,
            label = { Text("Project Name") },
            placeholder = { Text("Default") },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Leave blank to use Default.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f)
        )

        /*
        ################################################################
        EDIT SECTION 1005
        FREQUENCY PATH CHOICE
        ----------------------------------------------------------------
        PURPOSE
        Lets the user choose whether to continue with guided band-based
        setup or direct exact-frequency setup.
        ################################################################
        */
        SectionTitle(
            title = "2. How do you want to define the frequency?",
            subtitle = "Choose the setup path that best matches how much you already know."
        )

        ChoicePillRow(
            options = listOf(
                "Guided band path",
                "Exact frequency path"
            ),
            selectedOption = when (frequencyChoiceMethod) {
                FrequencyChoiceMethod.USE_COMMON_BAND -> "Guided band path"
                FrequencyChoiceMethod.ENTER_EXACT_FREQUENCY -> "Exact frequency path"
                null -> null
            },
            onOptionClick = {
                when (it) {
                    "Guided band path" -> {
                        onFrequencyChoiceMethodChange(FrequencyChoiceMethod.USE_COMMON_BAND)
                    }

                    "Exact frequency path" -> {
                        onFrequencyChoiceMethodChange(FrequencyChoiceMethod.ENTER_EXACT_FREQUENCY)
                    }
                }
            }
        )

        /*
        ################################################################
        EDIT SECTION 1006
        GUIDED PATH BLOCK
        ----------------------------------------------------------------
        PURPOSE
        Supports the simpler guided path by asking what kind of unit will
        use the antenna.
        ################################################################
        */
        if (frequencyChoiceMethod == FrequencyChoiceMethod.USE_COMMON_BAND) {
            DecisionSupportCard(
                title = "Guided band path",
                body = "Choose this if you want the wizard to continue in a simpler guided direction before fine tuning the exact design inside the workspace."
            )

            SectionTitle(
                title = "3. What type of unit will use this antenna?",
                subtitle = "This helps the next step choose a more suitable workspace starting point."
            )

            OptionRow(
                label = "Base Station",
                selected = unitType == "Base Station",
                onClick = { onUnitTypeChange("Base Station") }
            )

            OptionRow(
                label = "Handheld / Walkie Talkie",
                selected = unitType == "Handheld / Walkie Talkie",
                onClick = { onUnitTypeChange("Handheld / Walkie Talkie") }
            )

            DecisionSupportCard(
                title = "What happens next?",
                body = "The live design workspace can use this guided path to start from a simpler direction before more exact calculation details are finalized."
            )
        }

        /*
        ################################################################
        EDIT SECTION 1007
        EXACT FREQUENCY BLOCK
        ----------------------------------------------------------------
        PURPOSE
        Supports direct project setup by accepting an exact operating
        frequency in MHz.
        ################################################################
        */
        if (frequencyChoiceMethod == FrequencyChoiceMethod.ENTER_EXACT_FREQUENCY) {
            DecisionSupportCard(
                title = "Exact frequency path",
                body = "Choose this if you already know the target operating frequency. This gives the next step a stronger starting point for the live design workspace."
            )

            SectionTitle(
                title = "3. Enter the target frequency",
                subtitle = "Use MHz. Examples: 7.1, 27.185, 145.5, 433.92, 2400."
            )

            OutlinedTextField(
                value = exactFrequency,
                onValueChange = onExactFrequencyChange,
                label = { Text("Enter Frequency (MHz)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            if (exactFrequency.isNotBlank()) {
                Text(
                    text = frequencyHint,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f)
                )
            }

            Text(
                text = "Exact frequency entry creates a stronger starting point for the next guided design step.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f)
            )
        }

        /*
        ################################################################
        EDIT SECTION 1008
        DESIGN INTENT BLOCK
        ----------------------------------------------------------------
        PURPOSE
        Gives the user a small framing reminder about the kind of design
        start this step is preparing for.
        ################################################################
        */
        if (frequencyChoiceMethod != null) {
            SectionTitle(
                title = "4. Design start intention",
                subtitle = "This is a preparation check before the live design workspace opens."
            )

            when (frequencyChoiceMethod) {
                FrequencyChoiceMethod.USE_COMMON_BAND -> {
                    DecisionSupportCard(
                        title = "Guided start",
                        body = "The next step will continue with a simpler guided starting point that is easier for users who are still narrowing the exact design."
                    )
                }

                FrequencyChoiceMethod.ENTER_EXACT_FREQUENCY -> {
                    DecisionSupportCard(
                        title = "Calculated start",
                        body = "The next step will open with a more exact calculation direction because the operating frequency is already known."
                    )
                }

                null -> Unit
            }
        }

        /*
        ################################################################
        EDIT SECTION 1009
        STEP SUMMARY
        ----------------------------------------------------------------
        PURPOSE
        Gives the user a compact summary of what Step 3 currently knows.
        ################################################################
        */
        SelectionSummaryCard(
            title = "Current project setup",
            rows = buildList {
                add(
                    "Project name" to if (projectName.isBlank()) {
                        "Default"
                    } else {
                        projectName
                    }
                )

                add(
                    "Frequency setup path" to when (frequencyChoiceMethod) {
                        FrequencyChoiceMethod.USE_COMMON_BAND -> "Guided band path"
                        FrequencyChoiceMethod.ENTER_EXACT_FREQUENCY -> "Exact frequency path"
                        null -> "Not selected yet"
                    }
                )

                if (unitType.isNotBlank()) {
                    add("Unit type" to unitType)
                }

                if (exactFrequency.isNotBlank()) {
                    add("Exact frequency" to "$exactFrequency MHz")
                }
            }
        )

        /*
        ################################################################
        EDIT SECTION 1010
        STEP READINESS SUMMARY
        ----------------------------------------------------------------
        PURPOSE
        Gives the user a simple explanation of why the Next button is or
        is not ready.
        ################################################################
        */
        Text(
            text = CreateAntennaWizardController.buildStep3StatusText(
                frequencyChoiceMethod = frequencyChoiceMethod,
                unitType = unitType,
                exactFrequencyValid = exactFrequencyValid
            ),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.80f)
        )

        WizardNav(
            onBack = onBack,
            onNext = onNext,
            nextLabel = "Next",
            nextEnabled = canContinue
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

/*
########################################################################
EDIT SECTION 2001
FLOW LOGIC
------------------------------------------------------------------------
Frequency validity, continue gating, readiness status text, and the
frequency band/use description now live in the pure, testable
CreateAntennaWizardController (features/wizard). This screen delegates to
it so the flow logic stays UI-free.
########################################################################
*/