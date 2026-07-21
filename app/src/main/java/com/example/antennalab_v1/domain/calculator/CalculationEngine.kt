package com.example.antennalab_v1.domain.calculator

import com.example.antennalab_v1.domain.prediction.SWRPredictionEngine
import com.example.antennalab_v1.model.AntennaType
import com.example.antennalab_v1.model.CalculatedDesign
import com.example.antennalab_v1.model.EnvironmentalConditions

/*
########################################################################
FILE: CalculationEngine.kt
PACKAGE: com.example.antennalab_v1.domain.calculator
LAYER: Domain / Calculation Engine Router

SYSTEM ROLE
Central routing point for all antenna calculations.

Receives a typed CalculationRequest and selects the correct antenna
calculator. Returns a unified CalculationEngineResult containing:

• CalculatedDesign (persisted engineering data)
• CalculationPreview (UI display data)

This file must contain routing logic only, not antenna math.

ARCHITECTURE POSITION

Wizard / Workspace Inputs
        │
        ▼
CalculationRequest
        │
        ▼
CalculationEngine (THIS FILE)
        │
        ├ DipoleCalculator
        ├ VerticalCalculator
        ├ LoopCalculator
        └ YagiCalculator
        │
        ▼
CalculationMappers
        │
        ▼
CalculatedDesign + CalculationPreview
        │
        ▼
Prediction Integration
        │
        ▼
ProjectData.calculatedDesign
        │
        ▼
UI Rendering (ProjectPage / Workspace)

DATA FLOW

DesignInput (ProjectData)
        ↓
CalculationRequest
        ↓
calculateDesign()
        ↓
Antenna Calculator
        ↓
CalculationMappers
        ↓
Prediction Attachment
        ↓
CalculationEngineResult
        ↓
ProjectData.calculatedDesign

UPSTREAM DEPENDENCIES
- CalculationRequest
- DesignInput (ProjectData)

DOWNSTREAM CONSUMERS
- DipoleCalculator
- VerticalCalculator
- LoopCalculator
- YagiCalculator
- CalculationMappers
- SWRPredictionEngine

UI CONSUMERS
- ProjectPageScreen
- DesignWorkspaceScreen
- ProCalculator (legacy compatibility)

DEBUG CHECKLIST

When calculation output is wrong, check in this order:

1. INPUT
   - Is ProjectData.designInput correct?
   - Is targetFrequencyMHz valid and non-zero?
   - Is antennaType correct?

2. REQUEST BUILD
   - Was CalculationRequest created correctly?
   - Were material and conductor settings passed through?

3. ROUTING
   - Did calculateDesign() route to the correct calculator?
   - Is the antenna type mapped to the intended function?

4. CALCULATOR
   - Did the antenna calculator produce valid raw dimensions?
   - Were material and thickness factors applied once only?

5. MAPPER
   - Did CalculationMappers populate:
     • elements
     • spacings
     • feedRecommendation
     • buildGuidance
     • designExplanation
   - Are legacy numeric fields also valid?

6. PREDICTION
   - Was PredictedPerformance attached to CalculatedDesign?
   - Were default environmental conditions used correctly?

7. RESULT STORAGE
   - Was CalculatedDesign written into ProjectData.calculatedDesign?

8. UI
   - Is ProjectPageScreen reading structured fields first?
   - Is fallback UI reading only when structured fields are empty?

COMMON FAILURE PATTERNS
- Wrong AntennaType routing
- Missing preview builder function
- Missing structured design mapping
- Meters/mm conversion mistake
- Copy/paste corruption between files
- Prediction not attached after calculation

EDIT RULES

SAFE
✔ Modify antenna routing
✔ Add new antenna calculator routes
✔ Adjust how prediction is attached after calculator output

CAUTION
⚠ Changes affect ALL antenna calculations

DO NOT
✖ Implement antenna math here
✖ Implement UI logic here
✖ Modify ProjectData directly here

This file must remain a pure calculation router with controlled
post-processing only.
########################################################################
*/

fun calculateDesign(request: CalculationRequest): CalculationEngineResult {

    /*
    ####################################################################
    CALCULATION TRACE BLOCK
    --------------------------------------------------------------------
    This section exists to assist debugging of antenna calculations.

    Typical trace order:

    1. Request received
       - antennaType
       - targetFrequencyMHz
       - conductorMaterial
       - conductorForm
       - conductorSizeMm
       - priorityMode

    2. Router decision
       - which antenna calculator was selected

    3. Calculator output
       - raw dimensions in meters

    4. Mapper output
       - CalculatedDesign
       - CalculationPreview

    5. Prediction attachment
       - PredictedPerformance added to CalculatedDesign

    6. Result returned to UI

    If incorrect dimensions appear in the UI, inspect the pipeline in
    this exact order.

    Future debug logging may be inserted here if calculation tracing is
    required.
    ####################################################################
    */

    /*
    ####################################################################
    ROUTER BLOCK
    --------------------------------------------------------------------
    This section selects the correct calculator for the requested antenna
    type and returns the base calculation result.

    SAFE EDIT AREA
    - add new antenna type routes
    - redirect an antenna type to a new calculator
    ####################################################################
    */
    val baseResult = when (request.antennaType) {
        AntennaType.DIPOLE ->
            calculateDipole(request)

        AntennaType.MONOPOLE ->
            calculateVertical(request)

        AntennaType.LOOP ->
            calculateLoop(request)

        AntennaType.YAGI ->
            calculateYagi3Element(request)

        else ->
            buildFallbackCalculationResult(request)
    }

    /*
    ####################################################################
    PREDICTION ATTACHMENT BLOCK
    --------------------------------------------------------------------
    This section attaches first-pass environmental prediction data to the
    calculated design.

    CURRENT BEHAVIOUR
    - uses default EnvironmentalConditions()
    - keeps prediction separate from core calculator math
    - stores result inside CalculatedDesign.predictedPerformance

    SAFE EDIT AREA
    - later replace default environment with project/test session data
    - later pass user-selected environment values into prediction
    ####################################################################
    */
    return attachPrediction(baseResult)
}

private fun buildFallbackCalculationResult(
    request: CalculationRequest
): CalculationEngineResult {

    /*
    ####################################################################
    FALLBACK REFERENCE CALCULATION BLOCK
    --------------------------------------------------------------------
    This section builds a safe fallback result when a dedicated antenna
    calculator does not yet exist.

    SAFE EDIT AREA
    - improve fallback wording
    - refine preview labels
    - refine generic wavelength references
    ####################################################################
    */
    val wavelengthMeters =
        wavelengthMetersForFrequency(request.targetFrequencyMHz)

    val quarterWaveMeters =
        wavelengthMeters * 0.25

    val halfWaveMeters =
        wavelengthMeters * 0.50

    val fullWaveMeters =
        wavelengthMeters * 1.00

    val materialEffect =
        buildMaterialEffectText(
            conductorMaterial = request.conductorMaterial,
            conductorForm = request.conductorForm
        )

    val buildNotes =
        "This antenna type is not yet migrated to a dedicated calculator. " +
                buildPriorityNote(request.priorityMode)

    /*
    ####################################################################
    FALLBACK RESULT BUILD BLOCK
    --------------------------------------------------------------------
    This section packages fallback design data and preview data into a
    CalculationEngineResult.

    SAFE EDIT AREA
    - add more fallback warnings
    - improve preview wording
    ####################################################################
    */
    return CalculationEngineResult(
        calculatedDesign =
            CalculatedDesign(
                elementLengthsMm = listOf(halfWaveMeters * 1000.0),
                elementSpacingMm = emptyList(),
                boomLengthMm = 0.0,
                feedPointGapMm = 0.0,
                matchingMethod = "Not yet defined",
                estimatedGainDbI = 0.0,
                estimatedFrontToBackDb = 0.0,
                estimatedBandwidthMHz = 0.0,
                designWarnings = listOf(
                    "Using fallback wavelength references only.",
                    "Dedicated calculator not yet implemented for ${request.antennaType}."
                )
            ),
        preview =
            CalculationPreview(
                frequencyMHz = request.targetFrequencyMHz,
                wavelengthMeters = wavelengthMeters,
                primaryDimensionLabel = "Half-Wave Reference",
                primaryDimensionValue = formatMeters(halfWaveMeters),
                secondaryDimensionLabel = "Quarter-Wave Reference",
                secondaryDimensionValue = formatMeters(quarterWaveMeters),
                tertiaryDimensionLabel = "Full-Wave Reference",
                tertiaryDimensionValue = formatMeters(fullWaveMeters),
                estimatedWorkingRange =
                    estimateWorkingRangeText(request.targetFrequencyMHz),
                materialEffect = materialEffect,
                layoutGuidance =
                    "Use these wavelength references as a starting point while the final geometry is still undecided.",
                buildNotes = buildNotes
            )
    )
}

private fun attachPrediction(
    result: CalculationEngineResult
): CalculationEngineResult {

    /*
    ####################################################################
    DEFAULT ENVIRONMENT BLOCK
    --------------------------------------------------------------------
    This section creates the temporary default environment used for
    prediction until real project environment data is passed in.

    SAFE EDIT AREA
    - later replace with DesignInput/TestData/environment tab values
    ####################################################################
    */
    val defaultEnvironment = EnvironmentalConditions()

    /*
    ####################################################################
    PREDICTION EXECUTION BLOCK
    --------------------------------------------------------------------
    This section runs the SWR prediction engine using the calculated
    design plus the current environment profile.

    SAFE EDIT AREA
    - later swap to more advanced prediction inputs
    ####################################################################
    */
    val predictedPerformance =
        SWRPredictionEngine.predict(
            design = result.calculatedDesign,
            environment = defaultEnvironment
        )

    /*
    ####################################################################
    UPDATED DESIGN BUILD BLOCK
    --------------------------------------------------------------------
    This section stores predicted performance inside CalculatedDesign
    without changing the original base geometry values.

    SAFE EDIT AREA
    - add extra post-processing fields later if needed
    ####################################################################
    */
    val updatedDesign: CalculatedDesign =
        result.calculatedDesign.copy(
            predictedPerformance = predictedPerformance
        )

    /*
    ####################################################################
    FINAL RESULT RETURN BLOCK
    --------------------------------------------------------------------
    This section returns the original preview plus the updated calculated
    design containing prediction data.

    SAFE EDIT AREA
    - preserve preview unless a future design explicitly adds predicted
      preview fields
    ####################################################################
    */
    return result.copy(
        calculatedDesign = updatedDesign
    )
}