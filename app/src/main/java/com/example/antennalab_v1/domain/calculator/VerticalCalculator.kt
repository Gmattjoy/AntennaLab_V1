package com.example.antennalab_v1.domain.calculator

/*
########################################################################
FILE: VerticalCalculator.kt
PACKAGE: com.example.antennalab_v1.domain.calculator
LAYER: Domain / Antenna Calculator

SYSTEM ROLE
Implements quarter-wave vertical / monopole antenna calculations.

This calculator determines radiator and radial reference lengths using
the operating wavelength and conductor/material correction factors.

It returns a CalculationEngineResult containing:

• CalculatedDesign (persisted engineering data)
• CalculationPreview (UI display summary)

ARCHITECTURE POSITION

CalculationEngine
        │
        ▼
VerticalCalculator (THIS FILE)
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

Quarter-wave vertical reference:

λ = 300 / f(MHz)

Radiator length:
0.25 × wavelength × materialFactor × thicknessFactor

Radial reference length:
≈ 0.97 × radiator length

Actual performance depends heavily on radial system quality and
installation environment.

DEPENDENCIES

Uses helper functions from:
- sanitizeConductorSizeMm
- wavelengthMetersForFrequency
- materialShorteningFactor
- conductorThicknessFactor
- buildMaterialEffectText
- buildPriorityNote

Uses mapping functions from:
- buildVerticalCalculatedDesign()
- buildVerticalPreview()

DEBUG NOTES

If radiator or radial lengths appear incorrect:

1. Verify targetFrequencyMHz in CalculationRequest
2. Confirm wavelength calculation
3. Check material shortening factor
4. Check conductor thickness factor
5. Verify mapper receives radiatorMeters correctly

Typical issues:
• Radial length misinterpreted as spacing
• Material factor applied twice
• Preview showing different values than stored design

EDIT RULES

SAFE
✔ Adjust vertical antenna formulas
✔ Improve radial reference model
✔ Improve preview wording

CAUTION
⚠ Changes affect all monopole / vertical calculations

DO NOT
✖ Put UI logic here
✖ Modify ProjectData here
✖ Modify routing logic (CalculationEngine)

This file must remain **vertical antenna geometry logic only**.

########################################################################
*/

fun calculateVertical(request: CalculationRequest): CalculationEngineResult {
    val conductorSizeMm = sanitizeConductorSizeMm(request.conductorSizeMm)
    val wavelengthMeters = wavelengthMetersForFrequency(request.targetFrequencyMHz)
    val materialFactor = materialShorteningFactor(request.conductorMaterial)
    val thicknessFactor = conductorThicknessFactor(
        conductorForm = request.conductorForm,
        conductorSizeMm = conductorSizeMm
    )
    val adjustmentFactor = materialFactor * thicknessFactor

    val radiatorMeters = wavelengthMeters * 0.25 * adjustmentFactor

    val materialEffect = buildMaterialEffectText(
        conductorMaterial = request.conductorMaterial,
        conductorForm = request.conductorForm
    )

    val buildNotes =
        "Vertical performance depends strongly on radial quality, installation height, and nearby conductive structure. " +
                buildPriorityNote(request.priorityMode)

    return CalculationEngineResult(
        calculatedDesign = buildVerticalCalculatedDesign(radiatorMeters),
        preview = buildVerticalPreview(
            request = request,
            wavelengthMeters = wavelengthMeters,
            radiatorMeters = radiatorMeters,
            materialEffect = materialEffect,
            buildNotes = buildNotes
        )
    )
}