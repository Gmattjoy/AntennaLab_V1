package com.example.antennalab_v1.features.wizard.steps

/*
########################################################################
FILE: Step1AntennaTypeScreen.kt
PACKAGE: com.example.antennalab_v1.features.wizard.steps
LAYER: UI / Wizard Entry

SYSTEM ROLE
Wizard step 1 for guided antenna intake and first recommendation.

PROJECT DIRECTION
• Step 1 now combines the earlier first three wizard ideas into one
  guided intake page
• the page should scroll naturally from real-world use case to service
  type, frequency entry, recommendation, and final antenna family choice
• the user should not need technical knowledge at the start
• the wizard should recommend likely antenna families before the final
  shape selection is made
• Expert User remains a direct fast-access action

RULES
- No antenna formulas here
- No save logic here
- Keep recommendation logic lightweight and UI-local for now

CURRENT DEVELOPMENT ROLE
This screen is the guided intake entry point into the wizard system.

Right now this file is responsible for:

• collecting platform and mounting context
• collecting service/use category
• collecting single-frequency or range-based intent
• presenting a first-pass antenna family recommendation
• allowing final antenna family choice
• handing the chosen antenna family into the rest of the wizard

SAFE EDIT AREA
- add more service categories later
- improve recommendation rules later
- add persistent wizard-state wiring later
- refine wording later
########################################################################
*/

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.antennalab_v1.features.wizard.Step1AntennaTypeController
import com.example.antennalab_v1.features.wizard.components.AntennaTypeOptionCard
import com.example.antennalab_v1.features.wizard.components.ChoicePillRow
import com.example.antennalab_v1.features.wizard.components.DecisionSupportCard
import com.example.antennalab_v1.features.wizard.components.OptionRow
import com.example.antennalab_v1.features.wizard.components.SectionTitle
import com.example.antennalab_v1.features.wizard.components.SelectionSummaryCard
import com.example.antennalab_v1.features.wizard.components.WizardHeader
import com.example.antennalab_v1.features.wizard.components.WizardNav
import com.example.antennalab_v1.features.wizard.graphics.DipoleGraphic
import com.example.antennalab_v1.features.wizard.graphics.LoopGraphic
import com.example.antennalab_v1.features.wizard.graphics.VerticalGraphic
import com.example.antennalab_v1.features.wizard.graphics.YagiGraphic

/*
########################################################################
EDIT SECTION 1001
LOCAL VISUAL STYLE
------------------------------------------------------------------------
PURPOSE
Defines simple local colors used by the step entry screen.

SAFE EDIT AREA
- align with broader wizard theming later
- replace with shared theme constants later
########################################################################
*/
private val Step1ButtonColor = Color(0xFF141518)
private val Step1ButtonTextColor = Color(0xFFF4F6F8)
private val Step1AccentButtonColor = Color(0xFF141518)
private val Step1AccentButtonTextColor = Color(0xFFF4F6F8)
private val Step1HintTextColor = Color(0xFFF4F6F8)

/*
########################################################################
EDIT SECTION 1002
TOP-LEVEL SCREEN
------------------------------------------------------------------------
PURPOSE
Renders the new guided first wizard step.

SAFE EDIT AREA
- add more guided sections here later
- refine recommendation presentation later
########################################################################
*/
@Composable
fun CreateWizardStep1Screen(
    antennaType: String,
    onAntennaTypeChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    /*
    ####################################################################
    EDIT SECTION 1003
    LOCAL STEP STATE
    --------------------------------------------------------------------
    PURPOSE
    Temporary UI-local intake state used until broader wizard-state
    wiring is expanded into this screen.
    ####################################################################
    */
    var unitType by remember { mutableStateOf("") }
    var installationStyle by remember { mutableStateOf("") }
    var serviceType by remember { mutableStateOf("") }
    var customServiceText by remember { mutableStateOf("") }
    var frequencyMode by remember { mutableStateOf("") }
    var exactFrequency by remember { mutableStateOf("") }
    var lowerFrequency by remember { mutableStateOf("") }
    var upperFrequency by remember { mutableStateOf("") }
    var selectedAntennaFamily by remember { mutableStateOf(antennaType) }

    /*
    ####################################################################
    EDIT SECTION 1004
    DERIVED STATE
    --------------------------------------------------------------------
    PURPOSE
    Computes the first-pass recommendation and completion state.
    ####################################################################
    */
    val recommendation = remember(
        unitType,
        installationStyle,
        serviceType,
        frequencyMode,
        exactFrequency,
        lowerFrequency,
        upperFrequency
    ) {
        Step1AntennaTypeController.recommendAntennaFamily(
            unitType = unitType,
            installationStyle = installationStyle,
            serviceType = serviceType,
            frequencyMode = frequencyMode,
            exactFrequency = exactFrequency,
            lowerFrequency = lowerFrequency,
            upperFrequency = upperFrequency
        )
    }

    val frequencySectionComplete =
        Step1AntennaTypeController.isFrequencySectionComplete(
            frequencyMode = frequencyMode,
            exactFrequency = exactFrequency,
            lowerFrequency = lowerFrequency,
            upperFrequency = upperFrequency
        )

    val serviceSectionComplete =
        Step1AntennaTypeController.isServiceSectionComplete(
            serviceType = serviceType,
            customServiceText = customServiceText
        )

    val canProceed =
        Step1AntennaTypeController.canProceed(
            unitType = unitType,
            installationStyle = installationStyle,
            serviceType = serviceType,
            customServiceText = customServiceText,
            frequencyMode = frequencyMode,
            exactFrequency = exactFrequency,
            lowerFrequency = lowerFrequency,
            upperFrequency = upperFrequency,
            selectedAntennaFamily = selectedAntennaFamily
        )

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
            step = "Step 1 of 4",
            title = "Use case and antenna direction"
        )

        DecisionSupportCard(
            title = "Start with the real job this antenna must do",
            body = "This step collects the radio type, mounting style, service use, and frequency target first. Then the wizard suggests antenna families before you make the final shape choice."
        )

        /*
        ################################################################
        EDIT SECTION 1005
        TOP ACTION ROW
        ----------------------------------------------------------------
        PURPOSE
        Provides back navigation and expert fast access.
        ################################################################
        */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Step1ButtonColor,
                    contentColor = Step1ButtonTextColor
                )
            ) {
                Text(
                    text = "Back",
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = {
                    onAntennaTypeChange("Expert User")
                    onNext()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Step1AccentButtonColor,
                    contentColor = Step1AccentButtonTextColor
                )
            ) {
                Text(
                    text = "Expert User",
                    fontSize = 12.sp
                )
            }
        }

        /*
        ################################################################
        EDIT SECTION 1006
        RADIO PLATFORM SECTION
        ----------------------------------------------------------------
        PURPOSE
        Collects the broad radio platform type.
        ################################################################
        */
        SectionTitle(
            title = "1. What radio type is this antenna for?",
            subtitle = "Choose the main equipment style first."
        )

        ChoicePillRow(
            options = listOf("Handheld unit", "Base / base-mobile unit"),
            selectedOption = unitType,
            onOptionClick = { unitType = it }
        )

        /*
        ################################################################
        EDIT SECTION 1007
        INSTALLATION STYLE SECTION
        ----------------------------------------------------------------
        PURPOSE
        Collects how the antenna will actually be used or mounted.
        ################################################################
        */
        if (unitType.isNotBlank()) {
            SectionTitle(
                title = "2. How will the antenna be used?",
                subtitle = "Choose the installation style that best matches the real setup."
            )

            val installationOptions =
                if (unitType == "Handheld unit") {
                    listOf(
                        "Attached directly to handheld radio",
                        "Remote antenna for handheld radio",
                        "Mounted antenna used with handheld radio"
                    )
                } else {
                    listOf(
                        "Mounted on a vehicle",
                        "Mounted in one place",
                        "Portable mounted setup"
                    )
                }

            installationOptions.forEach { option ->
                OptionRow(
                    label = option,
                    selected = installationStyle == option,
                    onClick = { installationStyle = option }
                )
            }
        }

        /*
        ################################################################
        EDIT SECTION 1008
        SERVICE TYPE SECTION
        ----------------------------------------------------------------
        PURPOSE
        Collects the main service or use category after platform and
        installation are known.
        ################################################################
        */
        if (unitType.isNotBlank() && installationStyle.isNotBlank()) {
            SectionTitle(
                title = "3. What type of radio use is this antenna for?",
                subtitle = "Choose the use case that is closest to the intended job."
            )

            OptionRow(
                label = "Ham radio",
                selected = serviceType == "Ham radio",
                onClick = { serviceType = "Ham radio" }
            )

            OptionRow(
                label = "Air band",
                selected = serviceType == "Air band",
                onClick = { serviceType = "Air band" }
            )

            OptionRow(
                label = "Fire / rescue",
                selected = serviceType == "Fire / rescue",
                onClick = { serviceType = "Fire / rescue" }
            )

            OptionRow(
                label = "Marine",
                selected = serviceType == "Marine",
                onClick = { serviceType = "Marine" }
            )

            OptionRow(
                label = "General scanning / receive",
                selected = serviceType == "General scanning / receive",
                onClick = { serviceType = "General scanning / receive" }
            )

            OptionRow(
                label = "Long range communication",
                selected = serviceType == "Long range communication",
                onClick = { serviceType = "Long range communication" }
            )

            OptionRow(
                label = "Telemetry / data",
                selected = serviceType == "Telemetry / data",
                onClick = { serviceType = "Telemetry / data" }
            )

            OptionRow(
                label = "Other / custom",
                selected = serviceType == "Other / custom",
                onClick = { serviceType = "Other / custom" }
            )

            when (serviceType) {
                "Ham radio" -> {
                    DecisionSupportCard(
                        title = "Ham radio",
                        body = "Amateur radio use across one or more permitted bands. The wizard may suggest general-purpose or directional families depending on mounting style and frequency intent."
                    )
                }

                "Air band" -> {
                    DecisionSupportCard(
                        title = "Air band",
                        body = "Aviation communication or listening. Frequency coverage and installation style often matter more here than unusual antenna shapes."
                    )
                }

                "Fire / rescue" -> {
                    DecisionSupportCard(
                        title = "Fire / rescue",
                        body = "Emergency-service style communications often need practical mounting, reliability, and the correct operating band."
                    )
                }

                "Marine" -> {
                    DecisionSupportCard(
                        title = "Marine",
                        body = "Marine radio use usually favors practical mounted solutions and the correct service band over experimental antenna forms."
                    )
                }

                "General scanning / receive" -> {
                    DecisionSupportCard(
                        title = "General scanning / receive",
                        body = "Receive-only monitoring may prefer broader frequency coverage and simpler compromises rather than a narrow optimized transmit design."
                    )
                }

                "Long range communication" -> {
                    DecisionSupportCard(
                        title = "Long range communication",
                        body = "Long-distance use may favor more efficient designs, stronger mounting, or directional families depending on the station type."
                    )
                }

                "Telemetry / data" -> {
                    DecisionSupportCard(
                        title = "Telemetry / data",
                        body = "Data links often care about reliability, mounting consistency, and the intended frequency span."
                    )
                }

                "Other / custom" -> {
                    OutlinedTextField(
                        value = customServiceText,
                        onValueChange = { customServiceText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Describe the intended use") }
                    )
                }
            }
        }

        /*
        ################################################################
        EDIT SECTION 1009
        FREQUENCY SECTION
        ----------------------------------------------------------------
        PURPOSE
        Collects single-frequency or range-based design intent.
        ################################################################
        */
        if (unitType.isNotBlank() && installationStyle.isNotBlank() && serviceSectionComplete) {
            SectionTitle(
                title = "4. What frequency target do you want?",
                subtitle = "Choose whether this is for one frequency or a range."
            )

            ChoicePillRow(
                options = listOf(
                    "Single frequency",
                    "Frequency range",
                    "General coverage"
                ),
                selectedOption = frequencyMode,
                onOptionClick = { frequencyMode = it }
            )

            when (frequencyMode) {
                "Single frequency" -> {
                    OutlinedTextField(
                        value = exactFrequency,
                        onValueChange = { exactFrequency = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Target frequency MHz") }
                    )
                }

                "Frequency range" -> {
                    OutlinedTextField(
                        value = lowerFrequency,
                        onValueChange = { lowerFrequency = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Lower frequency MHz") }
                    )

                    OutlinedTextField(
                        value = upperFrequency,
                        onValueChange = { upperFrequency = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Upper frequency MHz") }
                    )
                }

                "General coverage" -> {
                    OutlinedTextField(
                        value = lowerFrequency,
                        onValueChange = { lowerFrequency = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Preferred low frequency MHz") }
                    )

                    OutlinedTextField(
                        value = upperFrequency,
                        onValueChange = { upperFrequency = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Preferred high frequency MHz") }
                    )
                }
            }
        }

        /*
        ################################################################
        EDIT SECTION 1010
        RECOMMENDATION SECTION
        ----------------------------------------------------------------
        PURPOSE
        Shows the first-pass suggested antenna family after the intake
        sections are complete.
        ################################################################
        */
        if (
            unitType.isNotBlank() &&
            installationStyle.isNotBlank() &&
            serviceSectionComplete &&
            frequencyMode.isNotBlank() &&
            frequencySectionComplete
        ) {
            SectionTitle(
                title = "5. Recommended antenna family",
                subtitle = "The wizard suggests a starting direction before you make the final family choice."
            )

            DecisionSupportCard(
                title = recommendation.title,
                body = recommendation.body
            )

            if (selectedAntennaFamily.isBlank()) {
                selectedAntennaFamily = recommendation.primaryFamily
            }
        }

        /*
        ################################################################
        EDIT SECTION 1011
        FINAL FAMILY CHOICE
        ----------------------------------------------------------------
        PURPOSE
        Lets the user make the final antenna family choice after the
        recommendation is shown.
        ################################################################
        */
        if (
            unitType.isNotBlank() &&
            installationStyle.isNotBlank() &&
            serviceSectionComplete &&
            frequencyMode.isNotBlank() &&
            frequencySectionComplete
        ) {
            SectionTitle(
                title = "6. Choose the antenna family",
                subtitle = "You can accept the recommended family or choose another suitable shape."
            )

            AntennaTypeOptionCard(
                label = "Dipole",
                subtitle = "Two straight elements. A strong general starting point for many fixed and balanced builds.",
                selected = selectedAntennaFamily == "Dipole",
                onClick = { selectedAntennaFamily = "Dipole" },
                graphic = { DipoleGraphic() }
            )

            AntennaTypeOptionCard(
                label = "Vertical",
                subtitle = "Single upright radiator. Often a strong fit for handheld, mobile, and practical mounted use.",
                selected = selectedAntennaFamily == "Vertical",
                onClick = { selectedAntennaFamily = "Vertical" },
                graphic = { VerticalGraphic() }
            )

            AntennaTypeOptionCard(
                label = "Yagi",
                subtitle = "Directional multi-element antenna. Useful when focused direction or more gain is part of the goal.",
                selected = selectedAntennaFamily == "Yagi",
                onClick = { selectedAntennaFamily = "Yagi" },
                graphic = { YagiGraphic() }
            )

            AntennaTypeOptionCard(
                label = "Loop",
                subtitle = "Closed loop style. Sometimes useful when the intended build direction naturally fits a loop form.",
                selected = selectedAntennaFamily == "Loop",
                onClick = { selectedAntennaFamily = "Loop" },
                graphic = { LoopGraphic() }
            )
        }

        /*
        ################################################################
        EDIT SECTION 1012
        SUMMARY SECTION
        ----------------------------------------------------------------
        PURPOSE
        Shows the current intake result before moving to the next stage.
        ################################################################
        */
        if (
            unitType.isNotBlank() ||
            installationStyle.isNotBlank() ||
            serviceType.isNotBlank() ||
            frequencyMode.isNotBlank() ||
            selectedAntennaFamily.isNotBlank()
        ) {
            SelectionSummaryCard(
                title = "Current intake summary",
                rows = buildList {
                    if (unitType.isNotBlank()) add("Unit type" to unitType)
                    if (installationStyle.isNotBlank()) add("Installation" to installationStyle)
                    if (serviceType.isNotBlank()) {
                        val serviceValue =
                            if (serviceType == "Other / custom" && customServiceText.isNotBlank()) {
                                customServiceText
                            } else {
                                serviceType
                            }
                        add("Service type" to serviceValue)
                    }

                    if (frequencyMode == "Single frequency" && exactFrequency.isNotBlank()) {
                        add("Frequency target" to "$exactFrequency MHz")
                    }

                    if (frequencyMode == "Frequency range" &&
                        lowerFrequency.isNotBlank() &&
                        upperFrequency.isNotBlank()
                    ) {
                        add("Frequency range" to "$lowerFrequency MHz to $upperFrequency MHz")
                    }

                    if (frequencyMode == "General coverage" &&
                        lowerFrequency.isNotBlank() &&
                        upperFrequency.isNotBlank()
                    ) {
                        add("Coverage range" to "$lowerFrequency MHz to $upperFrequency MHz")
                    }

                    if (recommendation.primaryFamily.isNotBlank()) {
                        add("Recommended family" to recommendation.primaryFamily)
                    }

                    if (selectedAntennaFamily.isNotBlank()) {
                        add("Chosen family" to selectedAntennaFamily)
                    }
                }
            )
        }

        /*
        ################################################################
        EDIT SECTION 1013
        BOTTOM NAV
        ----------------------------------------------------------------
        PURPOSE
        Moves to the next wizard stage only after the key guided intake
        data has been collected.
        ################################################################
        */
        WizardNav(
            onBack = onBack,
            onNext = {
                onAntennaTypeChange(selectedAntennaFamily)
                onNext()
            },
            nextLabel = "Next",
            nextEnabled = canProceed
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "This step will later be wired directly into full wizard state so the recommendation inputs persist across the full project flow.",
            fontSize = 12.sp,
            color = Step1HintTextColor.copy(alpha = 0.60f)
        )
    }
}