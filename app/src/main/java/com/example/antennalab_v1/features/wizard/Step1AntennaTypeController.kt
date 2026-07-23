package com.example.antennalab_v1.features.wizard

/*
########################################################################
FILE: Step1AntennaTypeController.kt
PACKAGE: com.example.antennalab_v1.features.wizard
LAYER: UI / Wizard / Step 1 Intake Decision Logic

SYSTEM ROLE
Owns the pure (non-Compose) decision logic for the guided Step 1 intake
screen so CreateWizardStep1Screen can stay UI-only. Extracted out of
Step1AntennaTypeScreen's Composable scope so it is unit-testable.

It currently owns:

• the first-pass antenna-family recommendation engine
• the Step 1 gating predicates (frequency / service completeness, and
  the overall "can proceed" rule that drives the Next button)

DESIGN INTENT
Keep this layer pure and easy to test/migrate. Mirrors the same UI-free
controller pattern used by CreateAntennaWizardController (Steps 3/4) and
SweepUiModelBuilder. No Compose APIs, no Android dependencies, no antenna
formulas — this is intake decision logic only.

SAFE EDIT AREA
- refine recommendation rules later
- add alternate families / confidence later
- add more service categories or frequency modes later
########################################################################
*/

/*
########################################################################
SECTION 2000
RECOMMENDATION MODEL
------------------------------------------------------------------------
PURPOSE
Simple recommendation output for the first guided intake pass.

SAFE EDIT AREA
- add alternate families later
- add confidence or difficulty later
########################################################################
*/
data class AntennaRecommendation(
    val primaryFamily: String,
    val title: String,
    val body: String
)

/*
########################################################################
SECTION 2100
STEP 1 CONTROLLER
------------------------------------------------------------------------
PURPOSE
Stateless intake helpers for the guided Step 1 wizard screen.
########################################################################
*/
object Step1AntennaTypeController {

    /*
    ------------------------------------------------------------
    SECTION 2110
    RECOMMENDATION ENGINE
    ------------------------------------------------------------
    PURPOSE
    Provides lightweight first-pass family guidance using the current
    intake answers. Branch order is deliberate — earlier matches win.
    ------------------------------------------------------------
    */
    fun recommendAntennaFamily(
        unitType: String,
        installationStyle: String,
        serviceType: String,
        frequencyMode: String,
        exactFrequency: String,
        lowerFrequency: String,
        upperFrequency: String
    ): AntennaRecommendation {
        val hasWideRangeRequest =
            frequencyMode == "General coverage" ||
                    (
                            frequencyMode == "Frequency range" &&
                                    lowerFrequency.isNotBlank() &&
                                    upperFrequency.isNotBlank()
                            )

        if (unitType == "Handheld unit") {
            return AntennaRecommendation(
                primaryFamily = "Vertical",
                title = "Recommended starting family: Vertical",
                body = "For handheld use, a vertical or whip-style family is usually the strongest starting point. It better matches practical handheld and mobile-style operation than larger fixed families."
            )
        }

        if (installationStyle == "Mounted on a vehicle") {
            return AntennaRecommendation(
                primaryFamily = "Vertical",
                title = "Recommended starting family: Vertical",
                body = "For vehicle-mounted use, a vertical family is usually the best first direction because it better matches practical mobile mounting and general all-round operation."
            )
        }

        if (serviceType == "Long range communication") {
            return AntennaRecommendation(
                primaryFamily = "Yagi",
                title = "Recommended starting family: Yagi",
                body = "For long-range communication goals, a directional family such as Yagi may be a better fit when installation conditions allow it. This can provide a stronger focused direction than a simple general-purpose antenna."
            )
        }

        if (hasWideRangeRequest && serviceType == "General scanning / receive") {
            return AntennaRecommendation(
                primaryFamily = "Vertical",
                title = "Recommended starting family: Vertical",
                body = "For broader receive coverage, a practical wider-coverage vertical-style direction is often a better starting point than a narrow tuned family."
            )
        }

        if (installationStyle == "Mounted in one place") {
            return AntennaRecommendation(
                primaryFamily = "Dipole",
                title = "Recommended starting family: Dipole",
                body = "For a simple fixed mounted setup, a dipole is often the safest general starting point. It is easy to understand and works well as a beginner-friendly base family."
            )
        }

        if (frequencyMode == "Single frequency" && exactFrequency.isNotBlank()) {
            return AntennaRecommendation(
                primaryFamily = "Dipole",
                title = "Recommended starting family: Dipole",
                body = "A single target frequency suits a tuned antenna family well. Dipole remains a strong first recommendation unless the mounting style or operating goal clearly points elsewhere."
            )
        }

        return AntennaRecommendation(
            primaryFamily = "Dipole",
            title = "Recommended starting family: Dipole",
            body = "Based on the current answers, dipole is the safest general starting point. You can still choose vertical, Yagi, or loop if your intended installation or performance goal fits them better."
        )
    }

    /*
    ------------------------------------------------------------
    SECTION 2120
    GATING PREDICATES
    ------------------------------------------------------------
    PURPOSE
    Drive section visibility and the Next button. Each is a pure
    function of the current intake answers.
    ------------------------------------------------------------
    */
    fun isFrequencySectionComplete(
        frequencyMode: String,
        exactFrequency: String,
        lowerFrequency: String,
        upperFrequency: String
    ): Boolean {
        return when (frequencyMode) {
            "Single frequency" -> exactFrequency.isNotBlank()
            "Frequency range" -> lowerFrequency.isNotBlank() && upperFrequency.isNotBlank()
            "General coverage" -> lowerFrequency.isNotBlank() && upperFrequency.isNotBlank()
            else -> false
        }
    }

    fun isServiceSectionComplete(
        serviceType: String,
        customServiceText: String
    ): Boolean {
        return when (serviceType) {
            "Other / custom" -> customServiceText.isNotBlank()
            else -> serviceType.isNotBlank()
        }
    }

    fun canProceed(
        unitType: String,
        installationStyle: String,
        serviceType: String,
        customServiceText: String,
        frequencyMode: String,
        exactFrequency: String,
        lowerFrequency: String,
        upperFrequency: String,
        selectedAntennaFamily: String
    ): Boolean {
        return unitType.isNotBlank() &&
                installationStyle.isNotBlank() &&
                isServiceSectionComplete(serviceType, customServiceText) &&
                frequencyMode.isNotBlank() &&
                isFrequencySectionComplete(
                    frequencyMode = frequencyMode,
                    exactFrequency = exactFrequency,
                    lowerFrequency = lowerFrequency,
                    upperFrequency = upperFrequency
                ) &&
                selectedAntennaFamily.isNotBlank()
    }
}
