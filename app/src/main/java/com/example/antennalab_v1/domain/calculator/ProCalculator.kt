package com.example.antennalab_v1.domain.calculator

/*
########################################################################
FILE: ProCalculator.kt
PURPOSE: Legacy compatibility bridge for the old workspace calculator.

ROLE:
- Preserves the old API used by earlier UI code
- Internally forwards calculations to the new CalculationEngine
- Allows gradual migration without breaking the app

NOTE
This file will eventually be removed once all callers are migrated
to the new typed calculation engine.
########################################################################
*/

data class ProCalculationResult(
    val frequencyMHz: Double,
    val wavelengthMeters: Double,
    val primaryDimensionLabel: String,
    val primaryDimensionValue: String,
    val secondaryDimensionLabel: String,
    val secondaryDimensionValue: String,
    val tertiaryDimensionLabel: String,
    val tertiaryDimensionValue: String,
    val estimatedRangeLabel: String,
    val materialEffect: String,
    val layoutGuidance: String,
    val buildNotes: String
)

fun calculateProResult(
    antennaType: String,
    exactFrequency: String,
    materialType: String,
    conductorForm: String,
    conductorSize: String,
    buildIntent: String
): ProCalculationResult? {

    val parsedFrequency = parseFrequencyMHz(exactFrequency) ?: return null

    val request = CalculationRequest(
        antennaType = when (antennaType) {
            "Dipole" -> com.example.antennalab_v1.model.AntennaType.DIPOLE
            "Vertical" -> com.example.antennalab_v1.model.AntennaType.MONOPOLE
            "Yagi" -> com.example.antennalab_v1.model.AntennaType.YAGI
            "Loop" -> com.example.antennalab_v1.model.AntennaType.LOOP
            else -> com.example.antennalab_v1.model.AntennaType.OTHER
        },
        targetFrequencyMHz = parsedFrequency
    )

    val result = calculateDesign(request)

    val preview = result.preview

    return ProCalculationResult(
        frequencyMHz = preview.frequencyMHz,
        wavelengthMeters = preview.wavelengthMeters,
        primaryDimensionLabel = preview.primaryDimensionLabel,
        primaryDimensionValue = preview.primaryDimensionValue,
        secondaryDimensionLabel = preview.secondaryDimensionLabel,
        secondaryDimensionValue = preview.secondaryDimensionValue,
        tertiaryDimensionLabel = preview.tertiaryDimensionLabel,
        tertiaryDimensionValue = preview.tertiaryDimensionValue,
        estimatedRangeLabel = preview.estimatedWorkingRange,
        materialEffect = preview.materialEffect,
        layoutGuidance = preview.layoutGuidance,
        buildNotes = preview.buildNotes
    )
}