package com.example.antennalab_v1.model

/*
########################################################################
FILE: BuildCostProfile.kt
PACKAGE: com.example.antennalab_v1.model
LAYER: Model / Project Context

SYSTEM ROLE
Represents the budget strategy context for an antenna project.

This does NOT control spending. It only informs the system whether
cost constraints may influence construction choices.

The value may affect engineering advice such as:

• fabrication vs purchased components
• reuse of available materials
• durability vs cost trade-offs
• convenience vs build effort

RF calculations and antenna physics must NEVER change based on this
value. It only influences engineering recommendations.

VALUES

BUDGET
Cost limitations likely affect build decisions.

STANDARD
Balanced cost and performance decisions.

PREMIUM
Cost is unlikely to restrict build choices.
########################################################################
*/

enum class BuildCostProfile {
    BUDGET,
    STANDARD,
    PREMIUM
}