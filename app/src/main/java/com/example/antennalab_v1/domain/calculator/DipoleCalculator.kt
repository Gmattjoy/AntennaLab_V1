package com.example.antennalab_v1.domain.calculator

/*
########################################################################
FILE: DipoleCalculator.kt
PACKAGE: com.example.antennalab_v1.domain.calculator
LAYER: Domain / Antenna Calculator

SYSTEM ROLE
Implements dipole antenna geometry calculations.

This calculator converts a CalculationRequest into physical dipole
dimensions using wavelength, material shortening, and conductor
thickness compensation.

It returns a CalculationEngineResult containing:

• CalculatedDesign (persisted engineering design)
• CalculationPreview (UI display summary)

ARCHITECTURE POSITION

CalculationEngine
        │
        ▼
DipoleCalculator (THIS FILE)
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

Half-wave dipole reference:

λ = 300 / f(MHz)

Total element length:
0.5 × wavelength × materialFactor × thicknessFactor

Each leg:
TotalLength / 2

Material and conductor effects are applied using helpers from
CalculationSupport.

DEPENDENCIES

Uses helper functions from:
- sanitizeConductorSizeMm
- wavelengthMetersForFrequency
- materialShorteningFactor
- conductorThicknessFactor
- buildMaterialEffectText
- buildPriorityNote

Uses mapping functions from:
- buildDipoleCalculatedDesign()
- buildDipolePreview()

DEBUG NOTES

If dipole length appears incorrect:

1. Verify targetFrequencyMHz in CalculationRequest
2. Confirm wavelength calculation
3. Check material shortening factor
4. Check conductor thickness factor
5. Confirm mapper receives correct totalLengthMeters

Typical issues:
• Frequency units incorrect
• Material factor applied twice
• Preview using different length than persisted design

EDIT RULES

SAFE
✔ Adjust dipole geometry formulas
✔ Improve compensation models
✔ Improve preview text

CAUTION
⚠ Changes affect all dipole calculations

DO NOT
✖ Put UI logic here
✖ Modify ProjectData here
✖ Modify routing logic (CalculationEngine)

This file must remain **dipole-only geometry logic**.

########################################################################
*/

fun calculateDipole(request: CalculationRequest): CalculationEngineResult {
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

    val totalLengthMeters =
        wavelengthMeters * 0.50 * adjustmentFactor

    val materialEffect =
        buildMaterialEffectText(
            conductorMaterial = request.conductorMaterial,
            conductorForm = request.conductorForm
        )

    val buildNotes =
        "Dipole tuning will move with mounting height, insulation, feedline routing, and nearby objects. " +
                buildPriorityNote(request.priorityMode)

    return CalculationEngineResult(
        calculatedDesign =
            buildDipoleCalculatedDesign(totalLengthMeters),

        preview =
            buildDipolePreview(
                request = request,
                wavelengthMeters = wavelengthMeters,
                totalLengthMeters = totalLengthMeters,
                materialEffect = materialEffect,
                buildNotes = buildNotes
            )
    )
}