package com.example.antennalab_v1.domain.calculator

/*
########################################################################
FILE: LoopCalculator.kt
PACKAGE: com.example.antennalab_v1.domain.calculator
LAYER: Domain / Antenna Calculator

SYSTEM ROLE
Implements full-wave loop antenna calculations.

This calculator determines the total loop circumference and reference
side lengths based on operating wavelength and conductor/material
correction factors.

It produces both:

• CalculatedDesign (persisted engineering design data)
• CalculationPreview (UI display summary)

ARCHITECTURE POSITION

CalculationEngine
        │
        ▼
LoopCalculator (THIS FILE)
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

Full-wave loop reference:

λ = 300 / f(MHz)

Loop circumference:
1.0 × wavelength × materialFactor × thicknessFactor

Typical square loop side length:
Circumference / 4

Actual loop behaviour depends on:

• shape (square / triangle / circular)
• feedpoint position
• mounting height
• nearby structures

DEPENDENCIES

Uses helper functions from:
- sanitizeConductorSizeMm
- wavelengthMetersForFrequency
- materialShorteningFactor
- conductorThicknessFactor
- buildMaterialEffectText
- buildPriorityNote

Uses mapping functions from:
- buildLoopCalculatedDesign()
- buildLoopPreview()

DEBUG NOTES

If loop dimensions appear incorrect:

1. Verify targetFrequencyMHz in CalculationRequest
2. Confirm wavelength calculation
3. Check material shortening factor
4. Check conductor thickness factor
5. Verify mapper receives circumferenceMeters correctly

Typical issues:
• Loop side values confused with circumference
• Material factor applied twice
• Preview values not matching persisted design

EDIT RULES

SAFE
✔ Adjust loop geometry formulas
✔ Add additional loop shapes
✔ Improve preview explanation text

CAUTION
⚠ Changes affect all loop calculations

DO NOT
✖ Put UI logic here
✖ Modify ProjectData here
✖ Modify routing logic (CalculationEngine)

This file must remain **loop antenna geometry logic only**.

########################################################################
*/

fun calculateLoop(request: CalculationRequest): CalculationEngineResult {
    val conductorSizeMm = sanitizeConductorSizeMm(request.conductorSizeMm)

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

    val circumferenceMeters =
        wavelengthMeters * 1.00 * adjustmentFactor

    val materialEffect =
        buildMaterialEffectText(
            conductorMaterial = request.conductorMaterial,
            conductorForm = request.conductorForm
        )

    val buildNotes =
        "Loop behaviour changes with shape, feedpoint, height, and nearby supports. " +
                buildPriorityNote(request.priorityMode)

    return CalculationEngineResult(
        calculatedDesign =
            buildLoopCalculatedDesign(circumferenceMeters),

        preview =
            buildLoopPreview(
                request = request,
                wavelengthMeters = wavelengthMeters,
                circumferenceMeters = circumferenceMeters,
                materialEffect = materialEffect,
                buildNotes = buildNotes
            )
    )
}