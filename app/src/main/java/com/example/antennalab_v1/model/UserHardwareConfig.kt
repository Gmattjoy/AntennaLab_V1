package com.example.antennalab_v1.model

/*
########################################################################
FILE: UserHardwareConfig.kt
PACKAGE: com.example.antennalab_v1.model
LAYER: Model / User Hardware Setup

LAST UPDATED 26/3/2026 18:20

SYSTEM ROLE
Stores a user-selected hardware setup that can be reused across
connection attempts and future project work.

ARCHITECTURE ROLE
This record captures the operator's chosen hardware metadata and chosen
driver profile.
It does not prove that transport or protocol validation has succeeded.
Those truths belong in live session state.

IMPORTANT DESIGN RULE
Saved setup and live validated connection state are different things and
must remain separate in the architecture.
########################################################################
*/

/*
########################################################################
SECTION 1000
USER HARDWARE CONFIG MODEL
------------------------------------------------------------------------
PURPOSE
Represents one saved or active user-selected hardware setup.
########################################################################
*/
data class UserHardwareConfig(
    val selectedBrand: HardwareBrand = HardwareBrand.UNKNOWN,
    val selectedModel: HardwareModel = HardwareModel.UNKNOWN,
    val enteredFirmwareVersion: String = "",
    val selectedDriverProfileId: String = "",
    val userNotes: String = "",
    val enableProbeValidation: Boolean = true,
    val allowExperimentalFallback: Boolean = false
)