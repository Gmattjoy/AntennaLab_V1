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
import com.example.antennalab_v1.domain.calculator.safeFrequencyMHz
import com.example.antennalab_v1.features.app.FrequencyChoiceMethod
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
    val exactFrequencyValid = safeFrequencyMHz(exactFrequency) != null
    val frequencyHint = describeFrequencyUse(exactFrequency)

    val canContinue = when (frequencyChoiceMethod) {
        FrequencyChoiceMethod.USE_COMMON_BAND ->
            unitType.isNotBlank()

        FrequencyChoiceMethod.ENTER_EXACT_FREQUENCY ->
            exactFrequencyValid

        null -> false
    }

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
            text = buildStep3StatusText(
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
FREQUENCY DESCRIPTION HELPER
------------------------------------------------------------------------
PURPOSE
Provides a user-friendly explanation of the entered frequency and the
likely radio region or usage category.

SAFE EDIT AREA
- extend band descriptions later
- refine usage hints later
########################################################################
*/
private fun describeFrequencyUse(exactFrequency: String): String {
    val mhz = safeFrequencyMHz(exactFrequency)
        ?: return "Enter a valid frequency in MHz to estimate the radio band."

    val bandLabel = when {
        mhz < 0.3 -> "This is below the standard MF broadcast region and is in the LF area."
        mhz < 3.0 -> "This is in the MF band. It overlaps long-range AM style reception and lower-frequency listening."
        mhz < 30.0 -> "This is in the HF band. HF is often used for shortwave listening, AM reception, and SSB communication."
        mhz < 300.0 -> "This is in the VHF band. VHF often includes FM, air band, marine, and local two-way radio use."
        mhz < 3000.0 -> "This is in the UHF band. UHF is common for handhelds, repeaters, business radio, and compact antennas."
        mhz < 30000.0 -> "This is in the SHF band. This is microwave territory and can include high-frequency links and Wi-Fi style work."
        else -> "This is above the common SHF range used in most simple antenna projects."
    }

    val modeHint = when {
        mhz in 0.53..1.71 -> "Likely interest: AM broadcast reception."
        mhz in 3.0..30.0 -> "Likely interest: shortwave listening, AM, and SSB."
        mhz in 64.0..108.0 -> "Likely interest: FM broadcast reception."
        mhz in 118.0..137.0 -> "Likely interest: air band AM reception."
        mhz in 144.0..148.0 -> "Likely interest: VHF two-way and amateur FM / SSB depending on use."
        mhz in 156.0..163.0 -> "Likely interest: marine VHF."
        mhz in 420.0..470.0 -> "Likely interest: UHF handheld, repeater, and business radio use."
        mhz in 850.0..960.0 -> "Likely interest: high UHF services."
        mhz in 1240.0..1300.0 -> "Likely interest: upper UHF / low microwave amateur work."
        else -> "Mode depends on the exact service and intended use."
    }

    return "$bandLabel $modeHint"
}

/*
########################################################################
EDIT SECTION 2002
STEP STATUS HELPER
------------------------------------------------------------------------
PURPOSE
Explains the current readiness state for continuing to the final wizard
step.

SAFE EDIT AREA
- add richer readiness guidance later
- add novice/pro mode wording later
########################################################################
*/
private fun buildStep3StatusText(
    frequencyChoiceMethod: FrequencyChoiceMethod?,
    unitType: String,
    exactFrequencyValid: Boolean
): String {
    return when (frequencyChoiceMethod) {
        FrequencyChoiceMethod.USE_COMMON_BAND -> {
            if (unitType.isBlank()) {
                "Choose the type of unit to continue with the guided path."
            } else {
                "Guided path is ready. The next step can continue into the workspace with a simpler guided starting point."
            }
        }

        FrequencyChoiceMethod.ENTER_EXACT_FREQUENCY -> {
            if (!exactFrequencyValid) {
                "Enter a valid exact frequency in MHz to continue."
            } else {
                "Exact frequency path is ready. The next step can open the live design workspace with a calculated baseline."
            }
        }

        null -> "Choose either the guided band path or the exact frequency path to continue."
    }
}