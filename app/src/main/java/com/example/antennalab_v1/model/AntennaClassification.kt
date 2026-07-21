package com.example.antennalab_v1.model

/*
########################################################################
FILE: AntennaClassification.kt
PACKAGE: com.example.antennalab_v1.model
LAYER: Core Data Model / Classification

SYSTEM ROLE
Defines the simplified antenna-shape classification used by unknown
antenna discovery and quick project identity workflows.

CURRENT DEVELOPMENT ROLE
This enum is intentionally separate from AntennaType.

AntennaType remains the design/calculation choice.
AntennaClassification is the visible/observed shape guess used for:

• discovery intake
• discovery result handoff
• quick project classification
• future filtering and presets

SAFE EDIT AREA
- add more observed-shape categories later
- keep stored enum names stable once persisted
########################################################################
*/
enum class AntennaClassification {
    NOT_YET_CLASSIFIED,
    DIPOLE,
    MONOPOLE,
    YAGI,
    LOOP,
    GROUND_PLANE,
    LONG_WIRE,
    VERTICAL,
    OTHER
}
