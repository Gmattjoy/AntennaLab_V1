package com.example.antennalab_v1.features.app

/*
########################################################################
FILE: FrequencyChoiceMethod.kt
PACKAGE: com.example.antennalab_v1.features.app
LAYER: UI Support / Wizard State

SYSTEM ROLE
Defines how the wizard selects frequency entry behaviour.

This enum matches the current Step3ProjectAndFrequencyScreen logic.

SAFE EDIT AREA
- keep names stable while current wizard screen depends on them
- expand carefully if new frequency entry modes are added later
########################################################################
*/

enum class FrequencyChoiceMethod {
    USE_COMMON_BAND,
    ENTER_EXACT_FREQUENCY
}