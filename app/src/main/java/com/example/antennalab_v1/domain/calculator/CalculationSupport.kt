package com.example.antennalab_v1.domain.calculator

import com.example.antennalab_v1.model.ConductorMaterial
import com.example.antennalab_v1.model.PriorityMode

/*
########################################################################
FILE: CalculationSupport.kt
PACKAGE: com.example.antennalab_v1.domain.calculator
LAYER: Domain / Calculation Utilities

SYSTEM ROLE
Provides shared helper functions used by all antenna calculators.

This file contains reusable utilities for:

• wavelength calculations
• material shortening factors
• conductor thickness adjustments
• input validation
• unit formatting
• preview text helpers

These functions ensure all antenna calculators use **consistent physics
assumptions and formatting logic**.

ARCHITECTURE POSITION

CalculationEngine
        │
        ▼
Antenna Calculators
 ├ DipoleCalculator
 ├ VerticalCalculator
 ├ LoopCalculator
 └ YagiCalculator
        │
        ▼
CalculationSupport (THIS FILE)
        │
        ▼
CalculationMappers
        │
        ▼
CalculatedDesign + CalculationPreview

UTILITY GROUPS

INPUT VALIDATION
- parseFrequencyMHz()
- safeFrequencyMHz()
- safeSizeMm()
- sanitizeConductorSizeMm()

RF PHYSICS
- wavelengthMetersForFrequency()

MATERIAL CORRECTIONS
- materialShorteningFactor()

CONDUCTOR GEOMETRY CORRECTIONS
- conductorThicknessFactor()

PREVIEW TEXT HELPERS
- buildMaterialEffectText()
- estimateWorkingRangeText()
- buildPriorityNote()

UNIT FORMATTING
- formatMeters()

RF MODEL NOTES

Wavelength calculation:

λ = 300 / f(MHz)

Material shortening factors approximate real-world conductor effects.
Thicker conductors shorten electrical length slightly.

These helpers centralise assumptions so **all antenna calculators behave
consistently**.

UPSTREAM DEPENDENCIES
- CalculationRequest
- ConductorMaterial
- ConductorForm
- PriorityMode

DOWNSTREAM CONSUMERS
- DipoleCalculator
- VerticalCalculator
- LoopCalculator
- YagiCalculator
- CalculationMappers

DEBUG NOTES

If calculated antenna dimensions appear incorrect:

1. Verify frequency parsing
2. Confirm wavelength calculation
3. Check material shortening factor
4. Check conductor thickness factor
5. Confirm correct units (meters vs mm)

Typical issues:
• Frequency entered in wrong units
• Conductor diameter extremely small or zero
• Material factor applied twice
• Incorrect unit conversions

EDIT RULES

SAFE
✔ Improve validation logic
✔ Improve RF helper models
✔ Adjust material compensation values
✔ Improve formatting functions

CAUTION
⚠ Changes here affect ALL antenna calculations

DO NOT
✖ Implement antenna geometry here
✖ Implement UI code here
✖ Modify ProjectData here

This file must remain **shared calculation utilities only**.

########################################################################
*/

fun parseFrequencyMHz(value: String): Double? {
    val parsed = value.toDoubleOrNull() ?: return null
    if (parsed <= 0.0) return null
    return parsed
}

fun safeFrequencyMHz(value: String): Double? {
    return parseFrequencyMHz(value)
}

fun safeSizeMm(value: String): Double {
    return value.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 2.0
}

fun sanitizeConductorSizeMm(value: Double): Double {
    return value.coerceAtLeast(0.1)
}

fun formatMeters(value: Double): String {
    return if (value >= 1.0) {
        String.format("%.3f m", value)
    } else {
        String.format("%.1f mm", value * 1000.0)
    }
}

fun wavelengthMetersForFrequency(frequencyMHz: Double): Double {
    return 300.0 / frequencyMHz
}

fun materialShorteningFactor(material: ConductorMaterial): Double {
    return when (material) {
        ConductorMaterial.COPPER -> 0.950
        ConductorMaterial.ALUMINIUM -> 0.955
        ConductorMaterial.BRASS -> 0.948
        ConductorMaterial.STEEL -> 0.940
        ConductorMaterial.OTHER -> 0.945
    }
}

fun conductorThicknessFactor(
    conductorForm: ConductorForm,
    conductorSizeMm: Double
): Double {
    val reductionPer10mm = when (conductorForm) {
        ConductorForm.TUBE -> 0.006
        ConductorForm.ROD -> 0.004
        ConductorForm.STRIP -> 0.005
        ConductorForm.WIRE -> 0.003
    }

    val reduction = (conductorSizeMm / 10.0) * reductionPer10mm
    return (1.0 - reduction).coerceIn(0.985, 0.999)
}

fun buildMaterialEffectText(
    conductorMaterial: ConductorMaterial,
    conductorForm: ConductorForm
): String {
    return "Material compensation applied for $conductorMaterial using $conductorForm. Larger conductor size slightly shortens the cut length."
}

fun estimateWorkingRangeText(frequencyMHz: Double): String {
    val low = frequencyMHz * 0.98
    val high = frequencyMHz * 1.02
    return "${String.format("%.2f", low)} to ${String.format("%.2f", high)} MHz starting trim window"
}

fun buildPriorityNote(priorityMode: PriorityMode): String {
    return when (priorityMode) {
        PriorityMode.GAIN -> "Gain priority selected. Allow more space and structure for stronger directional or efficient designs."
        PriorityMode.SIZE -> "Size priority selected. Compactness matters more, so electrical compromise may be required."
        PriorityMode.BANDWIDTH -> "Bandwidth priority selected. Conductor size, matching, and geometry stability matter more."
        PriorityMode.SIMPLE_BUILD -> "Simple build priority selected. Keep geometry practical, repeatable, and easy to assemble."
        PriorityMode.BALANCED -> "Balanced priority selected. Aim for a compromise between size, performance, and ease of construction."
    }
}