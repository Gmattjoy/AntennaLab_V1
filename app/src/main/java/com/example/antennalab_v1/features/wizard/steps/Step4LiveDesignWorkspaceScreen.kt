package com.example.antennalab_v1.features.wizard.steps

/*
########################################################################
FILE: Step4LiveDesignWorkspaceScreen.kt
PACKAGE: com.example.antennalab_v1.features.wizard.steps
LAYER: UI / Wizard Finalisation

SYSTEM ROLE
Final guided wizard step for turning a selected antenna concept into a
practical starter project.

DESIGN INTENT
This screen is not just a preview. It is the bridge between:

• wizard selection
• live design adjustment
• guided final confirmation
• project creation

It should feel like a simplified workspace for novice users while still
remaining useful on tablets and future larger layouts.

CURRENT DEVELOPMENT ROLE
This screen now acts as the clean handoff from wizard into the live
project workspace.

Right now this file is responsible for:

• showing a live calculation workspace inside the wizard
• helping the user confirm final design setup choices
• preparing a valid starter ProjectData object
• handing off into the main project workflow

SAFE EDIT AREA
- expand guided final-check logic
- add novice/expert layout split later
- improve project handoff details later
- connect more ProjectData fields when the broader model flow is ready
########################################################################
*/

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.domain.calculator.CalculationEngineResult
import com.example.antennalab_v1.domain.calculator.ConductorForm
import com.example.antennalab_v1.domain.calculator.calculateDesign
import com.example.antennalab_v1.domain.calculator.parseFrequencyMHz
import com.example.antennalab_v1.features.wizard.CreateAntennaWizardController
import com.example.antennalab_v1.model.ConductorMaterial
import com.example.antennalab_v1.model.PriorityMode
import com.example.antennalab_v1.model.ProjectData

/*
########################################################################
EDIT SECTION 1001
TOP-LEVEL SCREEN
------------------------------------------------------------------------
PURPOSE
Provides the final guided workspace-style wizard step with:

• live calculation
• guided confirmation
• project handoff

This is the main screen orchestration area. It decides layout mode,
builds the live calculation request, and controls which guided panels
are shown to the user.

SAFE EDIT AREA
- add more guided state here
- expand layout rules for tablet/desktop use
- add future mode toggles here
########################################################################
*/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step4LiveDesignWorkspaceScreen(
    antennaType: String,
    projectName: String,
    exactFrequency: String,
    onBack: () -> Unit,
    onFinish: (ProjectData) -> Unit
) {

    /*
    ####################################################################
    EDIT SECTION 1002
    LOCAL WORKSPACE STATE
    --------------------------------------------------------------------
    PURPOSE
    Stores the editable live-input values used by the final wizard step.
    These values are intentionally simple so the screen behaves like a
    guided starter workspace rather than a full engineering editor.
    ####################################################################
    */
    var frequency by remember { mutableStateOf(exactFrequency) }
    var conductorSize by remember { mutableStateOf("2.0") }

    var conductorMaterial by remember { mutableStateOf(ConductorMaterial.COPPER) }
    var conductorForm by remember { mutableStateOf(ConductorForm.WIRE) }
    var priority by remember { mutableStateOf(PriorityMode.BALANCED) }

    /*
    ####################################################################
    EDIT SECTION 1003
    LIVE CALCULATION INPUT
    --------------------------------------------------------------------
    PURPOSE
    Converts current user input into a calculation request. This lets the
    wizard function like a light live-design workspace before the full
    project is created.
    ####################################################################
    */
    val parsedFrequency = parseFrequencyMHz(frequency)

    val request = CreateAntennaWizardController.buildCalculationRequest(
        antennaTypeDisplay = antennaType,
        frequencyText = frequency,
        conductorSizeText = conductorSize,
        conductorMaterial = conductorMaterial,
        conductorForm = conductorForm,
        priority = priority
    )

    val result = request?.let { calculateDesign(it) }

    /*
    ####################################################################
    EDIT SECTION 1004
    GUIDED STATUS MODEL
    --------------------------------------------------------------------
    PURPOSE
    Builds the readiness lines used by the guided confirmation card. This
    helps the wizard explain whether the user is ready to create the
    starter project.
    ####################################################################
    */
    val readinessLines = CreateAntennaWizardController.buildReadinessLines(
        projectName = projectName,
        parsedFrequency = parsedFrequency,
        conductorSize = conductorSize,
        result = result
    )

    /*
    ####################################################################
    EDIT SECTION 1005
    SCREEN LAYOUT
    --------------------------------------------------------------------
    PURPOSE
    Renders the screen in a wider two-column format on large layouts and
    a stacked guided flow on smaller layouts.
    ####################################################################
    */
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guided Design Finalisation") }
            )
        }
    ) { padding ->

        BoxWithConstraints(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            val wideLayout = maxWidth > 700.dp

            if (wideLayout) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        WorkspaceLaunchCard(
                            projectName = projectName,
                            antennaType = antennaType,
                            parsedFrequency = parsedFrequency,
                            modifier = Modifier.fillMaxWidth()
                        )

                        ControlPanel(
                            antennaType = antennaType,
                            frequency = frequency,
                            conductorSize = conductorSize,
                            conductorMaterial = conductorMaterial,
                            conductorForm = conductorForm,
                            priority = priority,
                            onFrequencyChange = { frequency = it },
                            onSizeChange = { conductorSize = it },
                            onMaterialChange = { conductorMaterial = it },
                            onFormChange = { conductorForm = it },
                            onPriorityChange = { priority = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        GuidedFinalCheckCard(
                            projectName = projectName,
                            antennaType = antennaType,
                            parsedFrequency = parsedFrequency,
                            conductorSize = conductorSize,
                            conductorMaterial = conductorMaterial,
                            conductorForm = conductorForm,
                            priority = priority,
                            readinessLines = readinessLines,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ResultPanel(
                            result = result,
                            modifier = Modifier.fillMaxWidth()
                        )

                        NextStageCard(
                            result = result,
                            modifier = Modifier.fillMaxWidth()
                        )

                        FinishButtons(
                            projectName = projectName,
                            antennaType = antennaType,
                            frequency = parsedFrequency,
                            result = result,
                            onBack = onBack,
                            onFinish = onFinish
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WorkspaceLaunchCard(
                        projectName = projectName,
                        antennaType = antennaType,
                        parsedFrequency = parsedFrequency,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ControlPanel(
                        antennaType = antennaType,
                        frequency = frequency,
                        conductorSize = conductorSize,
                        conductorMaterial = conductorMaterial,
                        conductorForm = conductorForm,
                        priority = priority,
                        onFrequencyChange = { frequency = it },
                        onSizeChange = { conductorSize = it },
                        onMaterialChange = { conductorMaterial = it },
                        onFormChange = { conductorForm = it },
                        onPriorityChange = { priority = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    GuidedFinalCheckCard(
                        projectName = projectName,
                        antennaType = antennaType,
                        parsedFrequency = parsedFrequency,
                        conductorSize = conductorSize,
                        conductorMaterial = conductorMaterial,
                        conductorForm = conductorForm,
                        priority = priority,
                        readinessLines = readinessLines,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ResultPanel(
                        result = result,
                        modifier = Modifier.fillMaxWidth()
                    )

                    NextStageCard(
                        result = result,
                        modifier = Modifier.fillMaxWidth()
                    )

                    FinishButtons(
                        projectName = projectName,
                        antennaType = antennaType,
                        frequency = parsedFrequency,
                        result = result,
                        onBack = onBack,
                        onFinish = onFinish
                    )
                }
            }
        }
    }
}

/*
########################################################################
EDIT SECTION 1500
WORKSPACE LAUNCH CARD
------------------------------------------------------------------------
PURPOSE
Explains that the wizard is now transitioning into a real starter
project and live workspace handoff.

SAFE EDIT AREA
- add more handoff explanation later
- add novice/expert wording split later
########################################################################
*/
@Composable
private fun WorkspaceLaunchCard(
    projectName: String,
    antennaType: String,
    parsedFrequency: Double?,
    modifier: Modifier
) {
    Card(modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Workspace Launch Summary")
            HorizontalDivider()

            Text("Project")
            Text(projectName.ifBlank { "Default" })

            Text("Antenna Family")
            Text(antennaType)

            Text("Starting Frequency")
            Text(
                parsedFrequency?.let { String.format("%.3f MHz", it) }
                    ?: "Waiting for valid frequency"
            )

            HorizontalDivider()

            Text("After project creation")
            Text("1. A starter project will be created.")
            Text("2. The current calculation becomes the initial design baseline.")
            Text("3. The project can then move into the main workspace for further design, materials, testing, and tuning.")
        }
    }
}

/*
########################################################################
EDIT SECTION 2001
CONTROL PANEL
------------------------------------------------------------------------
PURPOSE
Collects the main live design inputs used by the final wizard step.

This section is the guided input area for:
• frequency
• conductor details
• design priority
• antenna type confirmation

SAFE EDIT AREA
- add more guided inputs here
- add contextual hints here
- add recommended defaults by antenna type later
########################################################################
*/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlPanel(
    antennaType: String,
    frequency: String,
    conductorSize: String,
    conductorMaterial: ConductorMaterial,
    conductorForm: ConductorForm,
    priority: PriorityMode,
    onFrequencyChange: (String) -> Unit,
    onSizeChange: (String) -> Unit,
    onMaterialChange: (ConductorMaterial) -> Unit,
    onFormChange: (ConductorForm) -> Unit,
    onPriorityChange: (PriorityMode) -> Unit,
    modifier: Modifier
) {
    Card(modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Design Inputs")

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Frequency")

                OutlinedTextField(
                    value = frequency,
                    onValueChange = onFrequencyChange,
                    label = { Text("Target Frequency (MHz)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Conductor")

                OutlinedTextField(
                    value = conductorSize,
                    onValueChange = onSizeChange,
                    label = { Text("Diameter (mm)") },
                    modifier = Modifier.fillMaxWidth()
                )

                DropdownSelector(
                    label = "Material",
                    options = ConductorMaterial.values().toList(),
                    selected = conductorMaterial,
                    onSelect = onMaterialChange
                )

                DropdownSelector(
                    label = "Form",
                    options = ConductorForm.values().toList(),
                    selected = conductorForm,
                    onSelect = onFormChange
                )
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Design Priority")

                DropdownSelector(
                    label = "Priority",
                    options = PriorityMode.values().toList(),
                    selected = priority,
                    onSelect = onPriorityChange
                )
            }

            HorizontalDivider()

            Text("Selected Antenna Type")
            Text(antennaType)
        }
    }
}

/*
########################################################################
EDIT SECTION 2002
DROPDOWN SELECTOR
------------------------------------------------------------------------
PURPOSE
Reusable dropdown selector used by the guided input area.

SAFE EDIT AREA
- improve display formatting later
- replace text labels with user-friendly labels later
########################################################################
*/
@Composable
private fun <T> DropdownSelector(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label)

        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selected.toString())
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.toString()) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/*
########################################################################
EDIT SECTION 2003
GUIDED FINAL CHECK CARD
------------------------------------------------------------------------
PURPOSE
Gives the novice user a clearer summary of what they are about to turn
into a saved working project.

This is the wizard's last confidence-building check before project
creation.

SAFE EDIT AREA
- add warnings and recommended checks here
- add beginner guidance text here
- add quick validation badges later
########################################################################
*/
@Composable
private fun GuidedFinalCheckCard(
    projectName: String,
    antennaType: String,
    parsedFrequency: Double?,
    conductorSize: String,
    conductorMaterial: ConductorMaterial,
    conductorForm: ConductorForm,
    priority: PriorityMode,
    readinessLines: List<String>,
    modifier: Modifier
) {
    Card(modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Guided Final Check")
            HorizontalDivider()

            Text("Project")
            Text(projectName.ifBlank { "Default" })

            Text("Antenna Type")
            Text(antennaType)

            Text("Target Frequency")
            Text(
                parsedFrequency?.let { String.format("%.3f MHz", it) }
                    ?: "Enter a valid frequency"
            )

            Text("Conductor Setup")
            Text(
                buildString {
                    append(conductorMaterial.name)
                    append(" / ")
                    append(conductorForm.name)
                    append(" / ")
                    append(conductorSize)
                    append(" mm")
                }
            )

            Text("Priority")
            Text(priority.name)

            HorizontalDivider()

            Text("Readiness")
            readinessLines.forEach { line ->
                Text("• $line")
            }
        }
    }
}

/*
########################################################################
EDIT SECTION 2004
RESULT PANEL
------------------------------------------------------------------------
PURPOSE
Displays the current calculated design result from the live input state.

This gives the user a practical design baseline before the project is
created.

SAFE EDIT AREA
- expand calculation detail here
- add future layout drawings or graphics here
- add manufacturing-oriented notes later
########################################################################
*/
@Composable
private fun ResultPanel(
    result: CalculationEngineResult?,
    modifier: Modifier
) {
    Card(modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Calculated Results")

            if (result != null) {
                val p = result.preview

                HorizontalDivider()

                Text("Dimensions")
                Text(p.primaryDimensionLabel)
                Text(p.primaryDimensionValue)
                Text(p.secondaryDimensionLabel)
                Text(p.secondaryDimensionValue)
                Text(p.tertiaryDimensionLabel)
                Text(p.tertiaryDimensionValue)

                HorizontalDivider()

                Text("Operating Range")
                Text(p.estimatedWorkingRange)

                HorizontalDivider()

                Text("Construction Notes")
                Text(p.materialEffect)
                Text(p.layoutGuidance)
                Text(p.buildNotes)
            } else {
                Text("Enter a valid frequency to calculate.")
            }
        }
    }
}

/*
########################################################################
EDIT SECTION 2005
NEXT STAGE CARD
------------------------------------------------------------------------
PURPOSE
Explains what happens after the project is created so the wizard feels
like a guided handoff instead of an abrupt finish.

SAFE EDIT AREA
- add project handoff explanation here
- add novice-path guidance here
- add saved-project workflow explanation later
########################################################################
*/
@Composable
private fun NextStageCard(
    result: CalculationEngineResult?,
    modifier: Modifier
) {
    Card(modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("What Happens Next")
            HorizontalDivider()

            if (result == null) {
                Text("Complete the design inputs to unlock project creation.")
                Text("After project setup you will be able to continue into the project workspace.")
            } else {
                Text("1. A starter project will be created from this design.")
                Text("2. The calculated design becomes your first working baseline.")
                Text("3. You can later refine materials, layout, testing, and tuning.")
                Text("4. This creates a practical checkpoint before hardware testing.")
            }
        }
    }
}

/*
########################################################################
EDIT SECTION 2006
FINISH BUTTONS
------------------------------------------------------------------------
PURPOSE
Creates the starter project and hands the user into the next stage of
the app flow.

SAFE EDIT AREA
- add more project fields during handoff later
- add smarter finish validation here
- add future save/load bridge logic here
########################################################################
*/
@Composable
private fun FinishButtons(
    projectName: String,
    antennaType: String,
    frequency: Double?,
    result: CalculationEngineResult?,
    onBack: () -> Unit,
    onFinish: (ProjectData) -> Unit
) {
    val canFinish = CreateAntennaWizardController.canFinish(frequency, result)

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                val project = CreateAntennaWizardController.buildStarterProject(
                    projectName = projectName,
                    antennaTypeDisplay = antennaType,
                    frequency = frequency,
                    result = result
                )

                onFinish(project)
            },
            enabled = canFinish,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Starter Project")
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }

        if (!canFinish) {
            Text("Enter a valid frequency to create the starter project.")
        }
    }
}

/*
########################################################################
EDIT SECTION 3001
FLOW LOGIC
------------------------------------------------------------------------
The guided readiness lines, antenna-type mapping, finish gating, and
starter-project assembly now live in the pure, testable
CreateAntennaWizardController (features/wizard). This screen delegates to
it so the flow logic stays UI-free.
########################################################################
*/