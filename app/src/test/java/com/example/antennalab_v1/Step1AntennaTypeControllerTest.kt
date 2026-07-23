package com.example.antennalab_v1

import com.example.antennalab_v1.features.wizard.Step1AntennaTypeController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavior coverage for [Step1AntennaTypeController] — the guided Step 1 intake
 * decision logic extracted out of CreateWizardStep1Screen's Composable scope:
 * the first-pass antenna-family recommendation engine and the section-gating
 * predicates that drive visibility and the Next button. Pure logic, so plain JVM
 * (no Robolectric).
 */
class Step1AntennaTypeControllerTest {

    private fun recommend(
        unitType: String = "",
        installationStyle: String = "",
        serviceType: String = "",
        frequencyMode: String = "",
        exactFrequency: String = "",
        lowerFrequency: String = "",
        upperFrequency: String = ""
    ) = Step1AntennaTypeController.recommendAntennaFamily(
        unitType = unitType,
        installationStyle = installationStyle,
        serviceType = serviceType,
        frequencyMode = frequencyMode,
        exactFrequency = exactFrequency,
        lowerFrequency = lowerFrequency,
        upperFrequency = upperFrequency
    )

    // ------------------------------------------------------------------
    // recommendAntennaFamily — branch coverage + precedence
    // ------------------------------------------------------------------

    @Test
    fun handheldUnit_recommendsVertical() {
        val result = recommend(unitType = "Handheld unit")
        assertEquals("Vertical", result.primaryFamily)
        assertEquals("Recommended starting family: Vertical", result.title)
    }

    @Test
    fun handheldUnit_winsOverVehicleAndLongRange() {
        // Handheld is the first branch, so it should take precedence over other
        // signals that would otherwise match later branches.
        val result = recommend(
            unitType = "Handheld unit",
            installationStyle = "Mounted on a vehicle",
            serviceType = "Long range communication"
        )
        assertEquals("Vertical", result.primaryFamily)
    }

    @Test
    fun vehicleMount_recommendsVerticalWhenNotHandheld() {
        val result = recommend(
            unitType = "Base / base-mobile unit",
            installationStyle = "Mounted on a vehicle"
        )
        assertEquals("Vertical", result.primaryFamily)
        assertTrue(result.body.contains("vehicle-mounted"))
    }

    @Test
    fun vehicleMount_winsOverLongRange() {
        val result = recommend(
            unitType = "Base / base-mobile unit",
            installationStyle = "Mounted on a vehicle",
            serviceType = "Long range communication"
        )
        assertEquals("Vertical", result.primaryFamily)
    }

    @Test
    fun longRange_recommendsYagi() {
        val result = recommend(
            unitType = "Base / base-mobile unit",
            installationStyle = "Mounted in one place",
            serviceType = "Long range communication"
        )
        assertEquals("Yagi", result.primaryFamily)
        assertEquals("Recommended starting family: Yagi", result.title)
    }

    @Test
    fun wideRangeGeneralScanning_recommendsVertical_generalCoverage() {
        val result = recommend(
            unitType = "Base / base-mobile unit",
            installationStyle = "Mounted in one place",
            serviceType = "General scanning / receive",
            frequencyMode = "General coverage"
        )
        assertEquals("Vertical", result.primaryFamily)
        assertTrue(result.body.contains("broader receive coverage"))
    }

    @Test
    fun wideRangeGeneralScanning_recommendsVertical_frequencyRangeWithBothBounds() {
        val result = recommend(
            unitType = "Base / base-mobile unit",
            installationStyle = "Mounted in one place",
            serviceType = "General scanning / receive",
            frequencyMode = "Frequency range",
            lowerFrequency = "140",
            upperFrequency = "150"
        )
        assertEquals("Vertical", result.primaryFamily)
    }

    @Test
    fun generalScanningWithoutWideRange_fallsThroughToMountedInOnePlaceDipole() {
        // Frequency range with only one bound is NOT a wide-range request, so the
        // General-scanning branch does not match; "Mounted in one place" wins.
        val result = recommend(
            unitType = "Base / base-mobile unit",
            installationStyle = "Mounted in one place",
            serviceType = "General scanning / receive",
            frequencyMode = "Frequency range",
            lowerFrequency = "140"
        )
        assertEquals("Dipole", result.primaryFamily)
        assertTrue(result.body.contains("fixed mounted setup"))
    }

    @Test
    fun mountedInOnePlace_recommendsDipole() {
        val result = recommend(
            unitType = "Base / base-mobile unit",
            installationStyle = "Mounted in one place",
            serviceType = "Ham radio"
        )
        assertEquals("Dipole", result.primaryFamily)
        assertTrue(result.body.contains("fixed mounted setup"))
    }

    @Test
    fun singleFrequencyWithValue_recommendsDipole() {
        val result = recommend(
            unitType = "Base / base-mobile unit",
            installationStyle = "Portable mounted setup",
            serviceType = "Ham radio",
            frequencyMode = "Single frequency",
            exactFrequency = "14.2"
        )
        assertEquals("Dipole", result.primaryFamily)
        assertTrue(result.body.contains("single target frequency"))
    }

    @Test
    fun noStrongSignal_fallsBackToDipole() {
        val result = recommend(
            unitType = "Base / base-mobile unit",
            installationStyle = "Portable mounted setup",
            serviceType = "Ham radio",
            frequencyMode = "Single frequency",
            exactFrequency = "" // blank, so the single-frequency branch does not fire
        )
        assertEquals("Dipole", result.primaryFamily)
        assertTrue(result.body.contains("safest general starting point"))
    }

    // ------------------------------------------------------------------
    // isFrequencySectionComplete
    // ------------------------------------------------------------------

    @Test
    fun frequencySectionComplete_singleFrequency() {
        assertFalse(
            Step1AntennaTypeController.isFrequencySectionComplete("Single frequency", "", "", "")
        )
        assertTrue(
            Step1AntennaTypeController.isFrequencySectionComplete("Single frequency", "14.2", "", "")
        )
    }

    @Test
    fun frequencySectionComplete_rangeAndCoverageNeedBothBounds() {
        assertFalse(
            Step1AntennaTypeController.isFrequencySectionComplete("Frequency range", "", "140", "")
        )
        assertTrue(
            Step1AntennaTypeController.isFrequencySectionComplete("Frequency range", "", "140", "150")
        )
        assertFalse(
            Step1AntennaTypeController.isFrequencySectionComplete("General coverage", "", "", "150")
        )
        assertTrue(
            Step1AntennaTypeController.isFrequencySectionComplete("General coverage", "", "140", "150")
        )
    }

    @Test
    fun frequencySectionComplete_unknownModeIsIncomplete() {
        assertFalse(
            Step1AntennaTypeController.isFrequencySectionComplete("", "14.2", "140", "150")
        )
    }

    // ------------------------------------------------------------------
    // isServiceSectionComplete
    // ------------------------------------------------------------------

    @Test
    fun serviceSectionComplete_customNeedsText() {
        assertFalse(Step1AntennaTypeController.isServiceSectionComplete("Other / custom", ""))
        assertTrue(Step1AntennaTypeController.isServiceSectionComplete("Other / custom", "weather balloon"))
    }

    @Test
    fun serviceSectionComplete_namedServicePasses_blankFails() {
        assertTrue(Step1AntennaTypeController.isServiceSectionComplete("Ham radio", ""))
        assertFalse(Step1AntennaTypeController.isServiceSectionComplete("", ""))
    }

    // ------------------------------------------------------------------
    // canProceed
    // ------------------------------------------------------------------

    private fun canProceed(
        unitType: String = "Handheld unit",
        installationStyle: String = "Remote antenna for handheld radio",
        serviceType: String = "Ham radio",
        customServiceText: String = "",
        frequencyMode: String = "Single frequency",
        exactFrequency: String = "14.2",
        lowerFrequency: String = "",
        upperFrequency: String = "",
        selectedAntennaFamily: String = "Dipole"
    ) = Step1AntennaTypeController.canProceed(
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

    @Test
    fun canProceed_trueWhenEverythingSatisfied() {
        assertTrue(canProceed())
    }

    @Test
    fun canProceed_falseWhenAnyRequiredInputMissing() {
        assertFalse(canProceed(unitType = ""))
        assertFalse(canProceed(installationStyle = ""))
        assertFalse(canProceed(frequencyMode = ""))
        assertFalse(canProceed(selectedAntennaFamily = ""))
    }

    @Test
    fun canProceed_falseWhenFrequencyValueMissingForMode() {
        assertFalse(canProceed(frequencyMode = "Single frequency", exactFrequency = ""))
        assertFalse(
            canProceed(
                frequencyMode = "Frequency range",
                exactFrequency = "",
                lowerFrequency = "140",
                upperFrequency = ""
            )
        )
    }

    @Test
    fun canProceed_falseWhenCustomServiceTextMissing() {
        assertFalse(canProceed(serviceType = "Other / custom", customServiceText = ""))
        assertTrue(canProceed(serviceType = "Other / custom", customServiceText = "weather balloon"))
    }
}
