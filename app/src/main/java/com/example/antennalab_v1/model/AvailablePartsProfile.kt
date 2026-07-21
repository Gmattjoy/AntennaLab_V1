package com.example.antennalab_v1.model

/*
########################################################################
FILE: AvailablePartsProfile.kt
PACKAGE: com.example.antennalab_v1.model
LAYER: Model / Project Context

SYSTEM ROLE
Represents how many antenna-related components the builder is likely
to already have available before starting the project.

This does NOT assume the builder must purchase all parts.

It only informs engineering recommendations about the likely
availability of existing materials.

ASSUMPTION
Normal workshop hand tools are already available.

This profile concerns antenna-specific parts such as:

• wire
• coax
• connectors
• insulators
• clamps
• mounting hardware
• spacers
• feed hardware

RF calculations and antenna physics must NEVER change based on
this value. It only informs build guidance.

VALUES

MINIMAL
Most antenna components will need to be sourced.

SOME_EXISTING_PARTS
Some antenna materials and fittings are already available.

WELL_STOCKED_WORKSHOP
Most common antenna construction parts are already available.
########################################################################
*/

enum class AvailablePartsProfile {
    MINIMAL,
    SOME_EXISTING_PARTS,
    WELL_STOCKED_WORKSHOP
}