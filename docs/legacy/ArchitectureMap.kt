package com.example.antennalab_v1

/*
########################################################################
FILE: ArchitectureMap.kt
PACKAGE: com.example.antennalab_v1
LAYER: Project Documentation / Architecture Reference

FILE ROLE

This file exists purely as an in-code architecture reference for the
AntennaLab V1 project.

It does NOT participate in runtime behaviour.

Its purpose is to provide a quick overview of how the major systems
connect so developers and AI systems can understand the project
structure without reading many individual files.

########################################################################
EDIT SECTION 0-1
########################################################################

CORE APPLICATION FLOW

Wizard Screens
        ↓
ProjectData
        ↓
CalculationEngine
        ↓
CalculatedDesign
        ↓
Project Workspace
        ↓
Testing Tools
        ↓
Storage

ProjectData acts as the single source of truth for the entire project.

########################################################################
EDIT SECTION 1-2
########################################################################

MODEL LAYER

Location:
model/

Purpose:
Stores pure data models.

Important rule:
Model files must NOT contain:

• UI logic
• Android framework usage
• calculation algorithms

Primary model:

ProjectData

ProjectData contains:

meta
designInput
materialConfig
calculatedDesign
testData
uiState
versionInfo
buildCostProfile
availablePartsProfile
testHardwareProfile

########################################################################
EDIT SECTION 2-3
########################################################################

DOMAIN LAYER

Location:
domain/

Purpose:
Contains operational logic that acts on models.

Examples:

CalculationEngine
SweepController
SweepAnalyzer

Domain components:

• receive model data
• perform calculations
• return structured results

Domain code should remain independent of UI components.

########################################################################
EDIT SECTION 3-4
########################################################################

FEATURE LAYER

Location:
features/

Purpose:
Contains UI screens and workflows.

Examples:

wizard screens
testing screens
workspace screens

UI layer responsibilities:

• render data
• collect user input
• call domain logic

UI should avoid implementing engineering algorithms.

########################################################################
EDIT SECTION 4-5
########################################################################

PROJECT WORKSPACE SYSTEM

Primary screen:

ProjectPageScreen

Responsibilities:

• project overview
• workspace navigation
• hardware selection
• testing entry
• save / load
• navigation to project tools

The workspace acts as the central hub for an antenna project.

########################################################################
EDIT SECTION 5-6
########################################################################

TESTING SYSTEM

Primary testing UI:

SweepGraphScreen

Current capabilities include:

• SWR graph
• return loss graph
• resistance graph
• reactance graph
• Smith chart preview
• impedance locus
• S21 estimate
• marker A / marker B
• delta marker readout
• resonance detection
• cable fault / TDR preview estimate
• CSV export preview

########################################################################
EDIT SECTION 6-7
########################################################################

SWEEP DATA FLOW

SweepController
        ↓
SweepResult
        ↓
SweepGraphScreen

SweepAnalyzer provides interpretation helpers such as:

• resonance detection
• sweep summary metrics

########################################################################
EDIT SECTION 7-8
########################################################################

HARDWARE SUPPORT SYSTEM

Supported hardware currently includes:

NanoVNA-H4
LiteVNA64 v0.3.3

Hardware selection stored in:

ProjectData.testHardwareProfile

This resolves to a hardware capability profile which determines:

• which graphs appear
• which measurements are available
• which testing tools are enabled

This design prevents UI code from hardcoding hardware behaviour.

########################################################################
EDIT SECTION 8-9
########################################################################

PREDICTION SYSTEM

The prediction system estimates antenna behaviour based on:

environmental model

antenna design parameters

prediction engine logic

Predicted data is stored in:

CalculatedDesign.predictedPerformance

UI systems read predictions using safe helper accessors.

########################################################################
EDIT SECTION 9-10
########################################################################

FUTURE SYSTEM EXPANSION

Planned or expected systems include:

expanded hardware capability profiles

hardware auto-detection

calibration workflow support

engineering dashboard

tuning assistant improvements

unknown antenna discovery mode

companion device measurement mode

tablet UI expansion

########################################################################
END OF FILE
########################################################################
*/