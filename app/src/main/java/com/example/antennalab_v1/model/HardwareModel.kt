package com.example.antennalab_v1.model

/*
########################################################################
FILE: HardwareModel.kt
PACKAGE: com.example.antennalab_v1.model
LAYER: Model / Hardware Selection

LAST UPDATED 26/3/2026 18:20

SYSTEM ROLE
Defines the known user-visible hardware models currently recognized by
AntennaLab.

ARCHITECTURE ROLE
Model is narrower than family, but still not the full software truth.
The selected driver profile remains the real binding record used by the
connection system.

IMPORTANT DESIGN RULE
New models can be added here without changing the transport architecture.
########################################################################
*/

/*
########################################################################
SECTION 1000
SUPPORTED HARDWARE MODELS
------------------------------------------------------------------------
PURPOSE
Provides the current curated list of models the app can present to the
user during hardware setup.
########################################################################
*/
enum class HardwareModel {
    NANO_VNA_H,
    NANO_VNA_H4,
    LITE_VNA_64,
    LITE_VNA_84,
    UNKNOWN
}