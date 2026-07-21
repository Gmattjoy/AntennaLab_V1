package com.example.antennalab_v1.domain.calculator

/*
########################################################################
FILE: YagiCalculator.kt
PACKAGE: com.example.antennalab_v1.domain.calculator
LAYER: Domain / Antenna Calculator

SYSTEM ROLE
Implements basic directional Yagi antenna calculations.

This calculator generates a starter geometry for a **3-element Yagi**
using reflector, driven element, and director reference lengths.

The result is returned as:

• CalculatedDesign (persisted engineering geometry)
• CalculationPreview (UI display summary)

ARCHITECTURE POSITION

CalculationEngine
        │
        ▼
YagiCalculator (THIS FILE)
        │
        ▼
CalculationMappers
        │
        ▼
CalculatedDesign + CalculationPreview
        │
        ▼
ProjectData.calculatedDesign
        │
        ▼
UI Rendering (ProjectPage / Workspace)

CALCULATION MODEL

Directional Yagi starter geometry:

λ = 300 / f(MHz)

Typical element length references:

Reflector ≈ 0.505 × wavelength
Driven ≈ 0.475 × wavelength
Director ≈ 0.450 × wavelength

Spacing references:

Reflector spacing ≈ 0.20 × wavelength
Director spacing ≈ 0.15 × wavelength

Boom length = reflector spacing + director spacing

Material and conductor corrections are applied using:

materialFactor × thicknessFactor

This implementation intentionally produces a **stable starting layout**
rather than a full optimisation solver.

DEPENDENCIES

Uses helper functions from:
- sanitizeConductorSizeMm
- wavelengthMetersForFrequency
- materialShorteningFactor
- conductorThicknessFactor
- buildMaterialEffectText
- buildPriorityNote

Uses mapping functions from:
- buildYagiCalculatedDesign()
- buildYagiPreview()

DEBUG NOTES

If Yagi dimensions appear incorrect:

1. Verify targetFrequencyMHz in CalculationRequest
2. Confirm wavelength calculation
3. Check material shortening factor
4. Check conductor thickness factor
5. Verify spacing and boom length calculations
6. Confirm mapper receives reflector/driven/director values

Typical issues:
• Reflector and director swapped
• Boom spacing incorrectly interpreted
• Preview dimensions different from stored design

EDIT RULES

SAFE
✔ Adjust element ratios
✔ Extend to multi-director Yagi models
✔ Improve preview explanations

CAUTION
⚠ Changes affect all Yagi calculations

DO NOT
✖ Put UI logic here
✖ Modify ProjectData here
✖ Modify routing logic (CalculationEngine)

This file must remain **directional antenna geometry logic only**.

########################################################################
*/

fun calculateYagi3Element(request: CalculationRequest): CalculationEngineResult {

    val conductorSizeMm =
        sanitizeConductorSizeMm(request.conductorSizeMm)

    val wavelengthMeters =
        wavelengthMetersForFrequency(request.targetFrequencyMHz)

    val materialFactor =
        materialShorteningFactor(request.conductorMaterial)

    val thicknessFactor =
        conductorThicknessFactor(
            conductorForm = request.conductorForm,
            conductorSizeMm = conductorSizeMm
        )

    val adjustmentFactor =
        materialFactor * thicknessFactor

    val reflectorMeters =
        wavelengthMeters * 0.505 * adjustmentFactor

    val drivenMeters =
        wavelengthMeters * 0.475 * adjustmentFactor

    val directorMeters =
        wavelengthMeters * 0.450 * adjustmentFactor

    val reflectorSpacing =
        wavelengthMeters * 0.20

    val directorSpacing =
        wavelengthMeters * 0.15

    val boomMeters =
        reflectorSpacing + directorSpacing

    val materialEffect =
        buildMaterialEffectText(
            conductorMaterial = request.conductorMaterial,
            conductorForm = request.conductorForm
        )

    val buildNotes =
        "This is a practical starting layout, not a final gain-optimised Yagi solver. " +
                buildPriorityNote(request.priorityMode)

    return CalculationEngineResult(

        calculatedDesign =
            buildYagiCalculatedDesign(
                reflectorMeters = reflectorMeters,
                drivenMeters = drivenMeters,
                directorMeters = directorMeters,
                boomMeters = boomMeters
            ),

        preview =
            buildYagiPreview(
                request = request,
                wavelengthMeters = wavelengthMeters,
                reflectorMeters = reflectorMeters,
                drivenMeters = drivenMeters,
                directorMeters = directorMeters,
                boomMeters = boomMeters,
                materialEffect = materialEffect,
                buildNotes = buildNotes
            )
    )
}