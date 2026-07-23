package com.example.antennalab_v1.features.wizard

/*
########################################################################
FILE: CreateAntennaWizardController.kt
PACKAGE: com.example.antennalab_v1.features.wizard
LAYER: UI / Wizard / Flow Control

SYSTEM ROLE
Owns the pure (non-Compose) decision logic for the create-antenna
wizard flow so the step screens can stay UI-only.

It currently owns:

• antenna-family display -> model AntennaType mapping (Step 4)
• live calculation request assembly (Step 4)
• guided readiness lines (Step 4)
• finish gating + starter ProjectData assembly (Step 4)
• Step 3 frequency validity / continue gating
• Step 3 readiness status text
• Step 3 frequency band/use description

DESIGN INTENT
Keep this layer pure and easy to test/migrate. Mirrors the same
UI-free controller pattern used by SweepWorkspaceController.

IMPORTANT
This file intentionally avoids Compose APIs and Android dependencies.
It orchestrates the domain calculator; it does NOT define formulas.
########################################################################
*/

import com.example.antennalab_v1.domain.calculator.CalculationEngineResult
import com.example.antennalab_v1.domain.calculator.CalculationRequest
import com.example.antennalab_v1.domain.calculator.ConductorForm
import com.example.antennalab_v1.domain.calculator.calculateDesign
import com.example.antennalab_v1.domain.calculator.parseFrequencyMHz
import com.example.antennalab_v1.domain.calculator.safeFrequencyMHz
import com.example.antennalab_v1.features.app.FrequencyChoiceMethod
import com.example.antennalab_v1.model.AntennaType
import com.example.antennalab_v1.model.ConductorMaterial
import com.example.antennalab_v1.model.PriorityMode
import com.example.antennalab_v1.model.ProjectData

/*
########################################################################
SECTION 1000
CONTROLLER OBJECT
########################################################################
PURPOSE
Stateless flow helpers for the create-antenna wizard steps.
########################################################################
*/
object CreateAntennaWizardController {

    /*
    ------------------------------------------------------------
    SECTION 1100
    ANTENNA TYPE MAPPING (Step 4)
    ------------------------------------------------------------
    PURPOSE
    Maps wizard display text into the model antenna type used by the
    core project state.
    ------------------------------------------------------------
    */
    fun toModelAntennaType(display: String): AntennaType {
        return when (display) {
            "Dipole" -> AntennaType.DIPOLE
            "Vertical" -> AntennaType.MONOPOLE
            "Yagi" -> AntennaType.YAGI
            "Loop" -> AntennaType.LOOP
            else -> AntennaType.OTHER
        }
    }

    /*
    ------------------------------------------------------------
    SECTION 1200
    LIVE CALCULATION (Step 4)
    ------------------------------------------------------------
    PURPOSE
    Builds a calculation request from the current live inputs. Returns
    null when the frequency text is not a usable value, mirroring the
    wizard's "waiting for a valid frequency" state.
    ------------------------------------------------------------
    */
    fun buildCalculationRequest(
        antennaTypeDisplay: String,
        frequencyText: String,
        conductorSizeText: String,
        conductorMaterial: ConductorMaterial,
        conductorForm: ConductorForm,
        priority: PriorityMode
    ): CalculationRequest? {
        val parsedFrequency = parseFrequencyMHz(frequencyText) ?: return null

        return CalculationRequest(
            antennaType = toModelAntennaType(antennaTypeDisplay),
            targetFrequencyMHz = parsedFrequency,
            conductorMaterial = conductorMaterial,
            conductorForm = conductorForm,
            conductorSizeMm = conductorSizeText.toDoubleOrNull() ?: 2.0,
            priorityMode = priority
        )
    }

    /*
    ------------------------------------------------------------
    SECTION 1300
    GUIDED READINESS LINES (Step 4)
    ------------------------------------------------------------
    PURPOSE
    Builds user-facing readiness feedback for the final wizard step.
    ------------------------------------------------------------
    */
    fun buildReadinessLines(
        projectName: String,
        parsedFrequency: Double?,
        conductorSize: String,
        result: CalculationEngineResult?
    ): List<String> {
        val lines = mutableListOf<String>()

        if (projectName.isBlank()) {
            lines += "Project name will default to Default."
        } else {
            lines += "Project name is ready."
        }

        if (parsedFrequency == null) {
            lines += "Target frequency still needs a valid numeric value."
        } else {
            lines += "Target frequency is valid."
        }

        if (conductorSize.toDoubleOrNull() == null) {
            lines += "Conductor size is not a valid number. Default calculation fallback is being used."
        } else {
            lines += "Conductor size is valid."
        }

        if (result == null) {
            lines += "Live design result is not ready yet."
        } else {
            lines += "Live design result is ready to become a starter project."
        }

        return lines
    }

    /*
    ------------------------------------------------------------
    SECTION 1400
    FINISH GATING + STARTER PROJECT (Step 4)
    ------------------------------------------------------------
    PURPOSE
    Decides whether the starter project can be created and assembles a
    valid starter ProjectData from the final wizard inputs.
    ------------------------------------------------------------
    */
    fun canFinish(
        frequency: Double?,
        result: CalculationEngineResult?
    ): Boolean {
        return frequency != null && result != null
    }

    fun buildStarterProject(
        projectName: String,
        antennaTypeDisplay: String,
        frequency: Double?,
        result: CalculationEngineResult?
    ): ProjectData {
        return ProjectData(
            meta = ProjectData().meta.copy(
                projectName = projectName.ifBlank { "Default" }
            ),
            designInput = ProjectData().designInput.copy(
                antennaType = toModelAntennaType(antennaTypeDisplay),
                targetFrequencyMHz = frequency ?: 0.0
            ),
            calculatedDesign = result?.calculatedDesign
                ?: ProjectData().calculatedDesign
        )
    }

    /*
    ------------------------------------------------------------
    SECTION 1500
    STEP 3 FREQUENCY + CONTINUE GATING
    ------------------------------------------------------------
    PURPOSE
    Validity and next-step availability for the project-definition step.
    ------------------------------------------------------------
    */
    fun isExactFrequencyValid(exactFrequency: String): Boolean {
        return safeFrequencyMHz(exactFrequency) != null
    }

    fun canContinueStep3(
        frequencyChoiceMethod: FrequencyChoiceMethod?,
        unitType: String,
        exactFrequencyValid: Boolean
    ): Boolean {
        return when (frequencyChoiceMethod) {
            FrequencyChoiceMethod.USE_COMMON_BAND ->
                unitType.isNotBlank()

            FrequencyChoiceMethod.ENTER_EXACT_FREQUENCY ->
                exactFrequencyValid

            null -> false
        }
    }

    /*
    ------------------------------------------------------------
    SECTION 1600
    STEP 3 READINESS STATUS TEXT
    ------------------------------------------------------------
    PURPOSE
    Explains the current readiness state for continuing to the final
    wizard step.
    ------------------------------------------------------------
    */
    fun buildStep3StatusText(
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

    /*
    ------------------------------------------------------------
    SECTION 1700
    STEP 3 FREQUENCY DESCRIPTION
    ------------------------------------------------------------
    PURPOSE
    Provides a user-friendly explanation of the entered frequency and
    the likely radio region or usage category.
    ------------------------------------------------------------
    */
    fun describeFrequencyUse(exactFrequency: String): String {
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
}
