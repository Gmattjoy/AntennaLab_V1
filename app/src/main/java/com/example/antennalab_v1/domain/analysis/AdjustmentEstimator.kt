package com.example.antennalab_v1.domain.analysis

import kotlin.math.abs

/*
------------------------------------------------------------
EDIT SECTION 1000
FILE HEADER
------------------------------------------------------------
FILE: AdjustmentEstimator.kt
PACKAGE: com.example.antennalab_v1.domain.analysis
LAYER: Domain / Analysis

SYSTEM ROLE
Converts target-versus-resonance offset into rough physical adjustment
estimates.

CURRENT DEVELOPMENT ROLE
Provides a conservative first-pass estimator for tuning workflows,
engineering summaries, and UI guidance.

INTENDED USE
• Tuning suggestion refinement
• Future Tuning Assistant
• Engineering dashboard
• Guided trim / extend workflows
• Educational explanation layer

IMPORTANT MAKER RULE
For normal maker workflow after the first build and first test, tuning
should assume trim-down operation. A finished radiator that is already
too short should usually be rebuilt or replaced rather than treated as a
simple add-length correction.

SAFE EDIT AREA
- add antenna-type-specific estimation later
- add mm/inch output later
- add loading-coil and matching-network handling later
- add confidence scoring later
------------------------------------------------------------
*/
/*
------------------------------------------------------------
EDIT SECTION 1100
DIRECTION ENUM
------------------------------------------------------------
PURPOSE
Defines the physical tuning direction suggested by the resonance offset.
------------------------------------------------------------
*/
enum class AdjustmentDirection {
    SHORTEN,
    REBUILD_OR_REPLACE,
    RETARGET_HIGHER,
    NONE
}

/*
------------------------------------------------------------
EDIT SECTION 1200
SEVERITY ENUM
------------------------------------------------------------
PURPOSE
Provides a simple size band for the estimated tuning correction.
------------------------------------------------------------
*/
enum class AdjustmentSeverity {
    VERY_SMALL,
    SMALL,
    MEDIUM,
    LARGE,
    NONE
}

/*
------------------------------------------------------------
EDIT SECTION 1300
REPORT MODEL
------------------------------------------------------------
PURPOSE
Stable output model for UI, assistant, and future workflow systems.

SAFE EDIT AREA
- add confidence later
- add warnings and workflow grouping later
------------------------------------------------------------
*/
data class AdjustmentEstimate(
    val direction: AdjustmentDirection,
    val severity: AdjustmentSeverity,
    val frequencyOffsetMHz: Double,
    val frequencyOffsetPercent: Double,
    val estimatedLengthChangeMm: Double?,
    val estimatedLengthChangeInches: Double?,
    val summary: String,
    val notes: List<String>
)

/*
------------------------------------------------------------
EDIT SECTION 1400
SCALING ENUM
------------------------------------------------------------
PURPOSE
Provides a stable hook for future antenna-type-aware adjustment scaling.

CURRENT RULE
All current options intentionally use conservative neutral scaling until
validated per-antenna behaviour is introduced.
------------------------------------------------------------
*/
enum class AdjustmentScalingProfile {
    GENERIC,
    DIPOLE_LIKE,
    VERTICAL_LIKE,
    LOOP_LIKE,
    YAGI_DRIVEN_ELEMENT
}

/*
------------------------------------------------------------
EDIT SECTION 1500
ENGINE OBJECT
------------------------------------------------------------
PURPOSE
Public entry point for estimating physical tuning adjustment from
target and measured resonance.
------------------------------------------------------------
*/
object AdjustmentEstimator {

    /*
    ------------------------------------------------------------
    EDIT SECTION 1501
    ESTIMATE ENTRY
    ------------------------------------------------------------
    PURPOSE
    Produces a conservative first-pass physical correction estimate.

    DESIGN RULE
    A usable physical cut amount is meaningful when a measured active
    radiator length is known. For maker workflow, a resonance-above-
    target result after first build should be interpreted mainly as
    rebuild/replace or retarget, not simple trim-up.
    ------------------------------------------------------------
    */
    fun estimate(
        targetFrequencyMHz: Double,
        detectedResonanceMHz: Double?,
        activeRadiatorLengthMm: Double? = null,
        scalingProfile: AdjustmentScalingProfile = AdjustmentScalingProfile.GENERIC
    ): AdjustmentEstimate {

        /*
        ------------------------------------------------------------
        EDIT SECTION 1502
        INPUT GUARD
        ------------------------------------------------------------
        */
        if (targetFrequencyMHz <= 0.0 || detectedResonanceMHz == null) {
            return AdjustmentEstimate(
                direction = AdjustmentDirection.NONE,
                severity = AdjustmentSeverity.NONE,
                frequencyOffsetMHz = 0.0,
                frequencyOffsetPercent = 0.0,
                estimatedLengthChangeMm = null,
                estimatedLengthChangeInches = null,
                summary = "Adjustment estimate unavailable.",
                notes = listOf(
                    "A valid target frequency and detected resonance are required."
                )
            )
        }

        /*
        ------------------------------------------------------------
        EDIT SECTION 1503
        CORE VALUES
        ------------------------------------------------------------
        */
        val frequencyOffsetMHz = detectedResonanceMHz - targetFrequencyMHz
        val frequencyOffsetPercent = abs((frequencyOffsetMHz / targetFrequencyMHz) * 100.0)

        val direction = when {
            frequencyOffsetMHz < 0.0 -> AdjustmentDirection.SHORTEN
            frequencyOffsetMHz > 0.0 -> AdjustmentDirection.REBUILD_OR_REPLACE
            else -> AdjustmentDirection.NONE
        }

        val severity = classifySeverity(frequencyOffsetPercent)
        val scalingFactor = resolveScalingFactor(scalingProfile)

        /*
        ------------------------------------------------------------
        EDIT SECTION 1504
        LENGTH ESTIMATE
        ------------------------------------------------------------
        */
        val estimatedLengthChangeMm =
            if (
                direction == AdjustmentDirection.SHORTEN &&
                activeRadiatorLengthMm != null &&
                activeRadiatorLengthMm > 0.0
            ) {
                activeRadiatorLengthMm * (frequencyOffsetPercent / 100.0) * scalingFactor
            } else {
                null
            }

        val estimatedLengthChangeInches =
            estimatedLengthChangeMm?.let { convertMillimetresToInches(it) }

        /*
        ------------------------------------------------------------
        EDIT SECTION 1505
        RESULT BUILD
        ------------------------------------------------------------
        */
        val summary = buildSummary(
            targetFrequencyMHz = targetFrequencyMHz,
            detectedResonanceMHz = detectedResonanceMHz,
            direction = direction,
            frequencyOffsetPercent = frequencyOffsetPercent,
            estimatedLengthChangeMm = estimatedLengthChangeMm,
            estimatedLengthChangeInches = estimatedLengthChangeInches
        )

        val notes = buildNotes(
            targetFrequencyMHz = targetFrequencyMHz,
            detectedResonanceMHz = detectedResonanceMHz,
            frequencyOffsetMHz = frequencyOffsetMHz,
            frequencyOffsetPercent = frequencyOffsetPercent,
            direction = direction,
            estimatedLengthChangeMm = estimatedLengthChangeMm,
            estimatedLengthChangeInches = estimatedLengthChangeInches,
            severity = severity,
            scalingProfile = scalingProfile
        )

        return AdjustmentEstimate(
            direction = direction,
            severity = severity,
            frequencyOffsetMHz = frequencyOffsetMHz,
            frequencyOffsetPercent = frequencyOffsetPercent,
            estimatedLengthChangeMm = estimatedLengthChangeMm,
            estimatedLengthChangeInches = estimatedLengthChangeInches,
            summary = summary,
            notes = notes
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2000
    SEVERITY HELPER
    ------------------------------------------------------------
    PURPOSE
    Converts percentage correction into a simple severity band.
    ------------------------------------------------------------
    */
    private fun classifySeverity(
        correctionPercent: Double
    ): AdjustmentSeverity {
        return when {
            correctionPercent == 0.0 -> AdjustmentSeverity.NONE
            correctionPercent < 0.25 -> AdjustmentSeverity.VERY_SMALL
            correctionPercent < 1.0 -> AdjustmentSeverity.SMALL
            correctionPercent < 3.0 -> AdjustmentSeverity.MEDIUM
            else -> AdjustmentSeverity.LARGE
        }
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 2100
    SCALING HELPER
    ------------------------------------------------------------
    PURPOSE
    Resolves a conservative scaling factor for future antenna-aware
    adjustment behaviour.
    ------------------------------------------------------------
    */
    private fun resolveScalingFactor(
        scalingProfile: AdjustmentScalingProfile
    ): Double {
        return when (scalingProfile) {
            AdjustmentScalingProfile.GENERIC -> 1.0
            AdjustmentScalingProfile.DIPOLE_LIKE -> 1.0
            AdjustmentScalingProfile.VERTICAL_LIKE -> 1.0
            AdjustmentScalingProfile.LOOP_LIKE -> 1.0
            AdjustmentScalingProfile.YAGI_DRIVEN_ELEMENT -> 1.0
        }
    }

    private fun convertMillimetresToInches(
        millimetres: Double
    ): Double {
        return millimetres / 25.4
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 3000
    SUMMARY HELPER
    ------------------------------------------------------------
    PURPOSE
    Produces a short user-facing explanation.
    ------------------------------------------------------------
    */
    private fun buildSummary(
        targetFrequencyMHz: Double,
        detectedResonanceMHz: Double,
        direction: AdjustmentDirection,
        frequencyOffsetPercent: Double,
        estimatedLengthChangeMm: Double?,
        estimatedLengthChangeInches: Double?
    ): String {
        val offsetText =
            " Frequency offset: ${String.format("%.3f%%", frequencyOffsetPercent)}."

        return when (direction) {
            AdjustmentDirection.SHORTEN -> {
                val measurementText =
                    if (estimatedLengthChangeMm != null && estimatedLengthChangeInches != null) {
                        " Estimated physical change: ${String.format("%.2f mm", estimatedLengthChangeMm)} (${String.format("%.3f in", estimatedLengthChangeInches)})."
                    } else {
                        " Physical trim amount requires the active radiator length measurement."
                    }

                "Resonance is below target. Consider shortening. " +
                        "Target ${String.format("%.3f MHz", targetFrequencyMHz)}, " +
                        "detected ${String.format("%.3f MHz", detectedResonanceMHz)}." +
                        offsetText +
                        measurementText
            }

            AdjustmentDirection.REBUILD_OR_REPLACE ->
                "Resonance is above target. The finished radiator appears too short." +
                        " Target ${String.format("%.3f MHz", targetFrequencyMHz)}, " +
                        "detected ${String.format("%.3f MHz", detectedResonanceMHz)}." +
                        offsetText +
                        " Stop trim-down tuning on this radiator. Prefer rebuild, replacement length, or retargeting rather than assuming length can be added back cleanly."

            AdjustmentDirection.RETARGET_HIGHER ->
                "Resonance is above target and may be better suited to a higher operating frequency." +
                        " Target ${String.format("%.3f MHz", targetFrequencyMHz)}, " +
                        "detected ${String.format("%.3f MHz", detectedResonanceMHz)}." +
                        offsetText

            AdjustmentDirection.NONE ->
                "Detected resonance is already aligned with target frequency."
        }
    }
    /*
    ------------------------------------------------------------
    EDIT SECTION 3100
    NOTES HELPER
    ------------------------------------------------------------
    PURPOSE
    Builds supporting notes for UI and future assistant systems.
    ------------------------------------------------------------
    */
    private fun buildNotes(
        targetFrequencyMHz: Double,
        detectedResonanceMHz: Double,
        frequencyOffsetMHz: Double,
        frequencyOffsetPercent: Double,
        direction: AdjustmentDirection,
        estimatedLengthChangeMm: Double?,
        estimatedLengthChangeInches: Double?,
        severity: AdjustmentSeverity,
        scalingProfile: AdjustmentScalingProfile
    ): List<String> {

        val notes = mutableListOf<String>()

        notes += "Target frequency: ${String.format("%.3f MHz", targetFrequencyMHz)}."
        notes += "Detected resonance: ${String.format("%.3f MHz", detectedResonanceMHz)}."
        notes += "Frequency offset: ${String.format("%.3f MHz", frequencyOffsetMHz)}."
        notes += "Frequency offset percent: ${String.format("%.3f%%", frequencyOffsetPercent)}."
        notes += "Estimated severity: ${severity.name}."
        notes += "Scaling profile: ${scalingProfile.name}."

        if (direction == AdjustmentDirection.SHORTEN) {
            if (estimatedLengthChangeMm != null && estimatedLengthChangeInches != null) {
                notes += "Estimated physical trim: ${String.format("%.2f mm", estimatedLengthChangeMm)}."
                notes += "Estimated physical trim: ${String.format("%.3f in", estimatedLengthChangeInches)}."
            } else {
                notes += "Physical trim amount is not shown because the active radiator length measurement is not yet available."
            }
        } else if (direction == AdjustmentDirection.REBUILD_OR_REPLACE) {
            notes += "The finished antenna appears electrically short relative to target."
            notes += "For normal maker workflow, do not assume length can be added back neatly after first build."
            notes += "Stop trim-down tuning on this radiator because further cutting will worsen the error."
            notes += "Preferred path is replacement length, replacement part, rebuild, or retargeting to a higher frequency."
        } else if (direction == AdjustmentDirection.RETARGET_HIGHER) {
            notes += "The current radiator may be better suited to a higher target frequency."
        } else {
            notes += "No directional physical correction is currently indicated."
        }

        when (severity) {
            AdjustmentSeverity.VERY_SMALL -> {
                notes += "Only a very small correction appears necessary."
            }

            AdjustmentSeverity.SMALL -> {
                notes += "A small correction is likely enough."
            }

            AdjustmentSeverity.MEDIUM -> {
                notes += "A moderate correction may be required."
            }

            AdjustmentSeverity.LARGE -> {
                notes += "Large estimated corrections should be verified carefully before cutting or rebuilding."
            }

            AdjustmentSeverity.NONE -> {
                notes += "No correction estimate is currently required."
            }
        }

        notes += "This estimate is approximate and should be treated as a tuning guide, not a final mechanical instruction."

        return notes
    }
}

/*
------------------------------------------------------------
END EDIT SECTIONS 3999
------------------------------------------------------------
*/