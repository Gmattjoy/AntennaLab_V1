package com.example.antennalab_v1.domain.calculator

import com.example.antennalab_v1.model.*

/*
########################################################################
FILE: CalculationMappers.kt
PACKAGE: com.example.antennalab_v1.domain.calculator
LAYER: Domain / Calculation Mapping Layer

SYSTEM ROLE
Transforms raw calculator outputs into two standard result forms:

1. CalculatedDesign
   - persisted engineering/design data stored in ProjectData
2. CalculationPreview
   - UI-friendly display summary for quick presentation

This file is the **normalisation layer** between antenna-specific math
and app-wide data structures.

ARCHITECTURE POSITION

Antenna Calculator
 ├ DipoleCalculator
 ├ VerticalCalculator
 ├ LoopCalculator
 └ YagiCalculator
        │
        ▼
CalculationMappers (THIS FILE)
        │
        ├ build...CalculatedDesign()
        └ build...Preview()
        │
        ▼
CalculatedDesign + CalculationPreview
        │
        ▼
CalculationEngineResult
        │
        ▼
ProjectData.calculatedDesign + UI display

STRUCTURE MAP

CalculationRequest
        ↓
Antenna-specific calculator
        ↓
raw geometry / derived values
        ↓
CalculationMappers
        ├ persisted design mapping
        └ preview text mapping
        ↓
CalculationEngineResult
        ↓
ProjectData + UI

MAPPING RESPONSIBILITIES

CalculatedDesign mapping:
- element lengths
- element spacing
- boom length
- feed details
- warnings
- structured design objects:
  • elements
  • spacings
  • feedRecommendation
  • buildGuidance
  • designExplanation

CalculationPreview mapping:
- headline dimensions
- quick interpretation text
- material effect wording
- layout guidance
- beginner-friendly build notes

UPSTREAM DEPENDENCIES
- DipoleCalculator
- VerticalCalculator
- LoopCalculator
- YagiCalculator
- CalculationSupport helpers

DOWNSTREAM CONSUMERS
- CalculationEngineResult
- ProjectData.calculatedDesign
- ProjectPageScreen
- Workspace preview systems

DEBUG NOTES

If UI shows missing or incomplete design data:

1. Confirm calculator produced expected raw values
2. Check matching build...CalculatedDesign() function
3. Check matching build...Preview() function
4. Confirm structured fields are populated:
   - elements
   - spacings
   - feedRecommendation
   - buildGuidance
   - designExplanation
5. Verify UI is reading structured fields first

Typical issues:
• Preview functions missing after copy/paste
• Structured fields left empty
• Wrong unit conversion (meters ↔ mm)
• Legacy fields updated but structured fields forgotten

EDIT RULES

SAFE
✔ Expand structured design mapping
✔ Improve preview wording
✔ Add new standard output sections

CAUTION
⚠ Changes affect both stored design data and UI preview output

DO NOT
✖ Put antenna-routing logic here
✖ Put UI composables here
✖ Put storage logic here

This file must remain the **mapping contract layer**.
########################################################################
*/

fun buildDipoleCalculatedDesign(totalLengthMeters: Double): CalculatedDesign {

    val totalLengthMm = totalLengthMeters * 1000.0
    val legLengthMm = totalLengthMm / 2.0

    val elements = listOf(
        CalculatedElement(
            role = ElementRole.RADIATOR,
            label = "Left Leg",
            lengthMm = legLengthMm,
            notes = "Keep straight and clear of nearby metal."
        ),
        CalculatedElement(
            role = ElementRole.RADIATOR,
            label = "Right Leg",
            lengthMm = legLengthMm,
            notes = "Match length with opposite leg."
        )
    )

    return CalculatedDesign(
        elementLengthsMm = listOf(legLengthMm, legLengthMm),
        elementSpacingMm = emptyList(),
        boomLengthMm = 0.0,
        feedPointGapMm = 5.0,
        matchingMethod = "Direct center feed",
        estimatedGainDbI = 2.15,
        estimatedFrontToBackDb = 0.0,
        estimatedBandwidthMHz = 0.0,
        designWarnings = listOf(
            "Final resonance will shift with height, insulation, and nearby objects."
        ),

        elements = elements,
        spacings = emptyList(),

        feedRecommendation = FeedRecommendation(
            feedMethod = "Center feed",
            balunRecommendation = "1:1 current balun recommended",
            matchingNotes = "Keep feedline perpendicular to element for a short distance."
        ),

        buildGuidance = BuildGuidance(
            sizeClass = SizeClass.MEDIUM,
            buildDifficulty = BuildDifficulty.EASY,
            tuningSensitivity = TuningSensitivity.MEDIUM,
            recommendedRadialCount = 0,
            supportNotes = "Support center feedpoint with a non-conductive mount.",
            mountingNotes = "Higher mounting generally improves performance."
        ),

        designExplanation = DesignExplanation(
            designSummary = "Half-wave dipole antenna.",
            intendedUse = "General purpose single-band antenna.",
            noviceGuidance = "Cut slightly long and trim both sides evenly.",
            tuningAdvice = "Adjust both legs equally to shift resonance."
        )
    )
}

fun buildVerticalCalculatedDesign(radiatorMeters: Double): CalculatedDesign {

    val radiatorMm = radiatorMeters * 1000.0
    val radialMm = radiatorMm * 0.97

    val elements = listOf(
        CalculatedElement(
            role = ElementRole.RADIATOR,
            label = "Vertical Radiator",
            lengthMm = radiatorMm,
            notes = "Quarter-wave radiator."
        )
    )

    val spacings = listOf(
        CalculatedSpacing("Radial 1", radialMm),
        CalculatedSpacing("Radial 2", radialMm),
        CalculatedSpacing("Radial 3", radialMm),
        CalculatedSpacing("Radial 4", radialMm)
    )

    return CalculatedDesign(
        elementLengthsMm = listOf(radiatorMm),
        elementSpacingMm = listOf(radialMm, radialMm, radialMm, radialMm),
        boomLengthMm = 0.0,
        feedPointGapMm = 0.0,
        matchingMethod = "Quarter-wave ground plane",
        estimatedGainDbI = 2.15,
        estimatedFrontToBackDb = 0.0,
        estimatedBandwidthMHz = 0.0,
        designWarnings = listOf(
            "Radial quality and installation height strongly affect performance."
        ),

        elements = elements,
        spacings = spacings,

        feedRecommendation = FeedRecommendation(
            feedMethod = "Base feed",
            balunRecommendation = "Usually not required",
            matchingNotes = "Radial angle affects feed impedance."
        ),

        buildGuidance = BuildGuidance(
            sizeClass = SizeClass.MEDIUM,
            buildDifficulty = BuildDifficulty.EASY,
            tuningSensitivity = TuningSensitivity.MEDIUM,
            recommendedRadialCount = 4,
            supportNotes = "Use a rigid base mount.",
            mountingNotes = "Keep radials evenly spaced."
        ),

        designExplanation = DesignExplanation(
            designSummary = "Quarter-wave vertical antenna.",
            intendedUse = "Omnidirectional communication.",
            noviceGuidance = "Start with four radials.",
            tuningAdvice = "Radial angle can adjust feed impedance."
        )
    )
}

fun buildLoopCalculatedDesign(circumferenceMeters: Double): CalculatedDesign {

    val circumferenceMm = circumferenceMeters * 1000.0
    val sideMm = circumferenceMm / 4.0

    val elements = listOf(
        CalculatedElement(ElementRole.LOOP_SIDE, "Side A", sideMm),
        CalculatedElement(ElementRole.LOOP_SIDE, "Side B", sideMm),
        CalculatedElement(ElementRole.LOOP_SIDE, "Side C", sideMm),
        CalculatedElement(ElementRole.LOOP_SIDE, "Side D", sideMm)
    )

    return CalculatedDesign(
        elementLengthsMm = listOf(sideMm, sideMm, sideMm, sideMm),
        elementSpacingMm = emptyList(),
        boomLengthMm = 0.0,
        feedPointGapMm = 5.0,
        matchingMethod = "Loop feedpoint depends on impedance",
        estimatedGainDbI = 2.5,
        estimatedFrontToBackDb = 0.0,
        estimatedBandwidthMHz = 0.0,
        designWarnings = listOf(
            "Loop shape and mounting environment affect tuning."
        ),

        elements = elements,
        spacings = emptyList(),

        feedRecommendation = FeedRecommendation(
            feedMethod = "Side feed",
            balunRecommendation = "1:1 current balun recommended",
            matchingNotes = "Feedpoint position influences impedance."
        ),

        buildGuidance = BuildGuidance(
            sizeClass = SizeClass.MEDIUM,
            buildDifficulty = BuildDifficulty.MODERATE,
            tuningSensitivity = TuningSensitivity.MEDIUM,
            recommendedRadialCount = 0,
            supportNotes = "Maintain square or circular symmetry.",
            mountingNotes = "Avoid metal supports near loop edges."
        ),

        designExplanation = DesignExplanation(
            designSummary = "Full-wave loop antenna.",
            intendedUse = "Low-noise reception and stable resonance.",
            noviceGuidance = "Square loops are easiest to construct.",
            tuningAdvice = "Feedpoint position can adjust impedance."
        )
    )
}

fun buildYagiCalculatedDesign(
    reflectorMeters: Double,
    drivenMeters: Double,
    directorMeters: Double,
    boomMeters: Double
): CalculatedDesign {

    val reflectorMm = reflectorMeters * 1000.0
    val drivenMm = drivenMeters * 1000.0
    val directorMm = directorMeters * 1000.0
    val boomMm = boomMeters * 1000.0

    val elements = listOf(
        CalculatedElement(ElementRole.REFLECTOR, "Reflector", reflectorMm),
        CalculatedElement(ElementRole.DRIVEN, "Driven Element", drivenMm),
        CalculatedElement(ElementRole.DIRECTOR, "Director", directorMm)
    )

    val spacings = listOf(
        CalculatedSpacing("Reflector → Driven", boomMm * 0.5),
        CalculatedSpacing("Driven → Director", boomMm * 0.5)
    )

    return CalculatedDesign(
        elementLengthsMm = listOf(reflectorMm, drivenMm, directorMm),
        elementSpacingMm = listOf(boomMm * 0.5, boomMm * 0.5),
        boomLengthMm = boomMm,
        feedPointGapMm = 5.0,
        matchingMethod = "Driven element feed",
        estimatedGainDbI = 6.0,
        estimatedFrontToBackDb = 10.0,
        estimatedBandwidthMHz = 0.0,
        designWarnings = listOf(
            "Starter 3-element Yagi geometry. Final optimisation requires tuning."
        ),

        elements = elements,
        spacings = spacings,

        feedRecommendation = FeedRecommendation(
            feedMethod = "Driven element center feed",
            balunRecommendation = "1:1 current balun recommended",
            matchingNotes = "Gamma or hairpin match may improve SWR."
        ),

        buildGuidance = BuildGuidance(
            sizeClass = SizeClass.LARGE,
            buildDifficulty = BuildDifficulty.MODERATE,
            tuningSensitivity = TuningSensitivity.HIGH,
            recommendedRadialCount = 0,
            supportNotes = "Rigid boom recommended.",
            mountingNotes = "Ensure elements remain perpendicular to boom."
        ),

        designExplanation = DesignExplanation(
            designSummary = "Basic 3-element Yagi beam.",
            intendedUse = "Directional gain and front-to-back rejection.",
            noviceGuidance = "Start with accurate element spacing.",
            tuningAdvice = "Driven element length has strongest tuning effect."
        )
    )
}

fun buildDipolePreview(
    request: CalculationRequest,
    wavelengthMeters: Double,
    totalLengthMeters: Double,
    materialEffect: String,
    buildNotes: String
): CalculationPreview {

    val legLengthMeters = totalLengthMeters / 2.0

    return CalculationPreview(
        frequencyMHz = request.targetFrequencyMHz,
        wavelengthMeters = wavelengthMeters,
        primaryDimensionLabel = "Total Element Length",
        primaryDimensionValue = formatMeters(totalLengthMeters),
        secondaryDimensionLabel = "Each Side From Feedpoint",
        secondaryDimensionValue = formatMeters(legLengthMeters),
        tertiaryDimensionLabel = "Build Start Note",
        tertiaryDimensionValue = "Start slightly long, then trim evenly",
        estimatedWorkingRange = estimateWorkingRangeText(request.targetFrequencyMHz),
        materialEffect = materialEffect,
        layoutGuidance = "Center-fed straight dipole. Keep both sides equal and maintain a clean feedpoint.",
        buildNotes = buildNotes
    )
}

fun buildVerticalPreview(
    request: CalculationRequest,
    wavelengthMeters: Double,
    radiatorMeters: Double,
    materialEffect: String,
    buildNotes: String
): CalculationPreview {

    val radialMeters = radiatorMeters * 0.97

    return CalculationPreview(
        frequencyMHz = request.targetFrequencyMHz,
        wavelengthMeters = wavelengthMeters,
        primaryDimensionLabel = "Radiator Length",
        primaryDimensionValue = formatMeters(radiatorMeters),
        secondaryDimensionLabel = "Suggested Radial Length",
        secondaryDimensionValue = formatMeters(radialMeters),
        tertiaryDimensionLabel = "Ground System",
        tertiaryDimensionValue = "Use 4 radials minimum",
        estimatedWorkingRange = estimateWorkingRangeText(request.targetFrequencyMHz),
        materialEffect = materialEffect,
        layoutGuidance = "Quarter-wave vertical with evenly spaced radials.",
        buildNotes = buildNotes
    )
}

fun buildLoopPreview(
    request: CalculationRequest,
    wavelengthMeters: Double,
    circumferenceMeters: Double,
    materialEffect: String,
    buildNotes: String
): CalculationPreview {

    val sideMeters = circumferenceMeters / 4.0

    return CalculationPreview(
        frequencyMHz = request.targetFrequencyMHz,
        wavelengthMeters = wavelengthMeters,
        primaryDimensionLabel = "Loop Circumference",
        primaryDimensionValue = formatMeters(circumferenceMeters),
        secondaryDimensionLabel = "Square Loop Side Length",
        secondaryDimensionValue = formatMeters(sideMeters),
        tertiaryDimensionLabel = "Feedpoint Note",
        tertiaryDimensionValue = "Feedpoint position affects impedance",
        estimatedWorkingRange = estimateWorkingRangeText(request.targetFrequencyMHz),
        materialEffect = materialEffect,
        layoutGuidance = "Square full-wave loop is the simplest starting shape for support and measurement.",
        buildNotes = buildNotes
    )
}

fun buildYagiPreview(
    request: CalculationRequest,
    wavelengthMeters: Double,
    reflectorMeters: Double,
    drivenMeters: Double,
    directorMeters: Double,
    boomMeters: Double,
    materialEffect: String,
    buildNotes: String
): CalculationPreview {

    return CalculationPreview(
        frequencyMHz = request.targetFrequencyMHz,
        wavelengthMeters = wavelengthMeters,
        primaryDimensionLabel = "Driven Element Length",
        primaryDimensionValue = formatMeters(drivenMeters),
        secondaryDimensionLabel = "Reflector / Director",
        secondaryDimensionValue =
            "${formatMeters(reflectorMeters)} / ${formatMeters(directorMeters)}",
        tertiaryDimensionLabel = "Boom Length",
        tertiaryDimensionValue = formatMeters(boomMeters),
        estimatedWorkingRange = estimateWorkingRangeText(request.targetFrequencyMHz),
        materialEffect = materialEffect,
        layoutGuidance = "Basic 3-element Yagi starter geometry.",
        buildNotes = buildNotes
    )
}