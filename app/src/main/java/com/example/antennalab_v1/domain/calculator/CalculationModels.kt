package com.example.antennalab_v1.domain.calculator

import com.example.antennalab_v1.model.AntennaType
import com.example.antennalab_v1.model.CalculatedDesign
import com.example.antennalab_v1.model.ConductorMaterial
import com.example.antennalab_v1.model.PriorityMode

/*
########################################################################
FILE: CalculationModels.kt
PURPOSE: Typed request/result contracts for the calculation engine.

ROLE:
- Defines calculator input models
- Defines preview models for UI display
- Separates preview data from persisted design data

RULES:
- No Compose UI here
- No storage logic here
- Domain-only data structures
########################################################################
*/

enum class ConductorForm {
    WIRE,
    ROD,
    TUBE,
    STRIP
}

data class CalculationRequest(
    val antennaType: AntennaType,
    val targetFrequencyMHz: Double,
    val conductorMaterial: ConductorMaterial = ConductorMaterial.COPPER,
    val conductorForm: ConductorForm = ConductorForm.WIRE,
    val conductorSizeMm: Double = 2.0,
    val priorityMode: PriorityMode = PriorityMode.BALANCED
)

data class CalculationPreview(
    val frequencyMHz: Double,
    val wavelengthMeters: Double,
    val primaryDimensionLabel: String,
    val primaryDimensionValue: String,
    val secondaryDimensionLabel: String,
    val secondaryDimensionValue: String,
    val tertiaryDimensionLabel: String,
    val tertiaryDimensionValue: String,
    val estimatedWorkingRange: String,
    val materialEffect: String,
    val layoutGuidance: String,
    val buildNotes: String
)

data class CalculationEngineResult(
    val calculatedDesign: CalculatedDesign,
    val preview: CalculationPreview
)