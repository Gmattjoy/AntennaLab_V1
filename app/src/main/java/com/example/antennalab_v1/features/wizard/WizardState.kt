package com.example.antennalab_v1.features.wizard

/*
########################################################################
FILE: WizardState.kt
PURPOSE: Holds the main wizard data in one place.
ROLE: Shared state model for the antenna creation workflow.

RULES:
- Data only.
- No Compose UI here.
- No calculation logic here.

CURRENT DEVELOPMENT ROLE
This state now supports the expanded guided wizard intake flow.

It stores:

• project naming
• selected antenna family
• frequency entry mode
• radio platform type
• installation style
• service / use category
• single-frequency or range-based entry
• recommended antenna family
• final selected antenna family
• material and conductor preferences
• project action status

SAFE EDIT AREA
- add richer validation state later
- add beginner/expert flow flags later
- add recommendation explanation fields later
########################################################################
*/

data class WizardState(

    /*
    ####################################################################
    SECTION 1000
    PROJECT IDENTITY
    --------------------------------------------------------------------
    PURPOSE
    Basic project naming and user-facing status text.
    ####################################################################
    */
    val projectName: String = "",
    val projectActionMessage: String = "",

    /*
    ####################################################################
    SECTION 1100
    ANTENNA SELECTION
    --------------------------------------------------------------------
    PURPOSE
    Stores both the final antenna family chosen by the user and the
    current recommendation produced by the guided wizard.
    ####################################################################
    */
    val antennaType: String = "",
    val recommendedAntennaFamily: String = "",
    val finalSelectedAntennaFamily: String = "",

    /*
    ####################################################################
    SECTION 1200
    RADIO PLATFORM AND INSTALLATION
    --------------------------------------------------------------------
    PURPOSE
    Captures how the radio is used in the real world before antenna
    family selection is finalized.
    ####################################################################
    */
    val unitType: String = "",
    val installationStyle: String = "",

    /*
    ####################################################################
    SECTION 1300
    SERVICE AND USE CASE
    --------------------------------------------------------------------
    PURPOSE
    Stores the main intended service or application category for the
    antenna.
    ####################################################################
    */
    val serviceType: String = "",
    val customServiceDescription: String = "",

    /*
    ####################################################################
    SECTION 1400
    FREQUENCY ENTRY MODE
    --------------------------------------------------------------------
    PURPOSE
    Stores whether the user is designing for one frequency or a range.
    ####################################################################
    */
    val frequencyChoiceMethodName: String = "",

    /*
    ####################################################################
    SECTION 1500
    FREQUENCY VALUES
    --------------------------------------------------------------------
    PURPOSE
    Supports single-frequency and range-based workflows.
    ####################################################################
    */
    val exactFrequency: String = "",
    val lowerFrequency: String = "",
    val upperFrequency: String = "",

    /*
    ####################################################################
    SECTION 1600
    BUILD PREFERENCES
    --------------------------------------------------------------------
    PURPOSE
    Existing material and conductor choices used later in the design
    flow.
    ####################################################################
    */
    val materialType: String = "Copper",
    val conductorForm: String = "Wire",
    val conductorSize: String = "2.0",
    val buildIntent: String = "Balanced",
    val dimensionsLocked: Boolean = false
)