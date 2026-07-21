package com.example.antennalab_v1

/*
###############################################################################
ANTENNALAB DEBUGGING PLAYBOOK
###############################################################################

PURPOSE
This document describes the official debugging method for AntennaLab.

It exists to prevent wasted time searching randomly through the codebase.

All bugs should be traced using the structured pipeline below.


###############################################################################
CORE SYSTEM PIPELINE
###############################################################################

User Input
    ↓
Wizard / Workspace
    ↓
ProjectData.designInput
    ↓
CalculationRequest
    ↓
CalculationEngine
    ↓
Antenna Calculator
    ↓
CalculationMappers
    ↓
CalculatedDesign
    ↓
DesignAnalyzer
    ↓
ProjectPageScreen UI


###############################################################################
DEBUGGING ORDER (MANDATORY)
###############################################################################

Always debug in this order.

STEP 1 — INPUT

Check:

ProjectData.designInput

Fields:
- antennaType
- targetFrequencyMHz
- frequency range values
- conductor material
- conductor size
- priority mode


STEP 2 — REQUEST BUILD

Check:

CalculationRequest

Verify correct values passed to calculation engine.


STEP 3 — ENGINE ROUTING

File:

domain.calculator.CalculationEngine.kt

Verify router selected correct calculator:

- DipoleCalculator
- VerticalCalculator
- LoopCalculator
- YagiCalculator


STEP 4 — CALCULATOR

Files:

DipoleCalculator.kt
VerticalCalculator.kt
LoopCalculator.kt
YagiCalculator.kt

Check:

- wavelength calculation
- material shortening factor
- conductor thickness factor
- geometry formulas


STEP 5 — MAPPER

File:

CalculationMappers.kt

Verify:

CalculatedDesign fields populated

Important fields:

elements
spacings
feedRecommendation
buildGuidance
designExplanation


STEP 6 — STORED RESULT

Check:

ProjectData.calculatedDesign


STEP 7 — ANALYSIS LAYER

File:

DesignAnalyzer.kt

Check:

- tuningAdvice
- buildWarnings
- improvementIdeas


STEP 8 — UI

File:

ProjectPageScreen.kt

Verify:

- correct ProjectSection selected
- structured fields displayed


###############################################################################
COMMON FAILURE PATTERNS
###############################################################################

ROUTING ERROR

Symptoms:
Wrong antenna calculation used.

Cause:
Incorrect AntennaType in CalculationEngine router.



MISSING DESIGN DATA

Symptoms:
UI shows "No calculated design yet".

Cause:
CalculationMappers did not populate structured fields.



UNIT ERROR

Symptoms:
Element lengths extremely large or tiny.

Cause:
Meters vs millimeters conversion mistake.



COPY / PASTE CORRUPTION

Symptoms:
Unresolved references or missing functions.

Cause:
Partial file replacement or missing functions.



###############################################################################
KNOWN HIGH-RISK FILES
###############################################################################

ProjectData.kt
    Changes affect the entire application.

CalculationMappers.kt
    Incorrect mapping breaks both UI and storage.

CalculationEngine.kt
    Incorrect routing breaks all calculations.



###############################################################################
DEBUGGING PRINCIPLE
###############################################################################

Never debug the UI first.

Always debug in this order:

Input → Engine → Calculator → Mapper → Model → UI



###############################################################################
END OF DEBUGGING PLAYBOOK
###############################################################################
*/

object DEBUGGING_PLAYBOOK