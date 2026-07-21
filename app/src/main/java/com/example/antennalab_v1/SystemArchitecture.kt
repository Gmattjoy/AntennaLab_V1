package com.example.antennalab_v1

/*
###############################################################################
ANTENNALAB SYSTEM ARCHITECTURE MAP
###############################################################################

PURPOSE
This file documents the full internal architecture of the AntennaLab system.

It exists to help developers and AI tools quickly understand:

• system layers
• data flow
• file responsibilities
• debugging entry points

This file contains NO executable logic.


###############################################################################
PRIMARY ARCHITECTURE LAYERS
###############################################################################

UI LAYER
Handles screens, layout, and user interaction.

packages:
features.wizard
features.workspace
project

example files:
Step1AntennaTypeScreen.kt
Step4LiveDesignWorkspaceScreen.kt
DesignWorkspaceScreen.kt
ProjectPageScreen.kt


DOMAIN LAYER (CALCULATION ENGINE)
Contains all RF math and antenna design logic.

package:
domain.calculator

files:
CalculationEngine.kt
DipoleCalculator.kt
VerticalCalculator.kt
LoopCalculator.kt
YagiCalculator.kt
CalculationMappers.kt
CalculationSupport.kt


DATA MODEL LAYER
Contains the master project data structure.

package:
model

primary file:
ProjectData.kt

ProjectData is the SINGLE SOURCE OF TRUTH for project state.


STORAGE LAYER
Handles save/load of project files.

package:
storage

file:
ProjectStorage.kt


###############################################################################
CORE DATA FLOW
###############################################################################

USER INPUT

Wizard / Workspace
        │
        ▼

ProjectData.designInput
        │
        ▼

CalculationRequest
        │
        ▼

CalculationEngine
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

CalculatedDesign
        │
        ▼

ProjectData.calculatedDesign
        │
        ▼

UI Rendering
(ProjectPageScreen / Workspace)


###############################################################################
CALCULATED DESIGN STRUCTURE
###############################################################################

CalculatedDesign

Legacy fields
-------------
elementLengthsMm
elementSpacingMm
boomLengthMm
feedPointGapMm
estimatedGainDbI
estimatedBandwidthMHz


Structured design fields
------------------------
elements
spacings
feedRecommendation
buildGuidance
designExplanation


###############################################################################
DEBUGGING GUIDE
###############################################################################

IF ANTENNA DIMENSIONS ARE WRONG

Check in this order:

1) ProjectData.designInput
2) CalculationRequest creation
3) CalculationEngine routing
4) Antenna calculator logic
5) CalculationMappers
6) CalculatedDesign fields
7) UI rendering


IF UI SHOWS NO DESIGN DATA

Check:

ProjectData.calculatedDesign
    elements
    spacings
    feedRecommendation
    buildGuidance
    designExplanation


IF FREQUENCY LOOKS WRONG

Check:

CalculationSupport.kt

wavelengthMetersForFrequency()


###############################################################################
SAFE EDIT ZONES
###############################################################################

SAFE TO MODIFY

domain.calculator
    antenna math
    preview explanations
    material compensation

project UI
    layout
    display logic


CAUTION AREAS

ProjectData.kt
    affects ALL systems


DO NOT PLACE

• Android UI code inside domain.calculator
• antenna math inside UI
• storage code inside calculators


###############################################################################
FUTURE SYSTEM EXPANSIONS
###############################################################################

PLANNED COMPONENTS

DesignAnalyzer.kt
    design evaluation
    tuning advice
    improvement suggestions


TestingIntegration.kt
    compare measured SWR vs calculated design


MultiElementYagiSolver.kt
    advanced Yagi optimisation


###############################################################################
END OF SYSTEM MAP
###############################################################################
*/

object SystemArchitecture