package com.example.antennalab_v1.model

/*
########################################################################
FILE: DriverProtocolType.kt
PACKAGE: com.example.antennalab_v1.model
LAYER: Model / Driver Definition

LAST UPDATED 26/3/2026 18:20

SYSTEM ROLE
Defines the protocol family used after transport has been opened.

ARCHITECTURE ROLE
Protocol type is the key software identity used to bind the active
command and sweep logic.

IMPORTANT DESIGN RULE
Protocol type must not be inferred from brand name alone.
It should come from the selected driver profile and later be validated
against the connected hardware.
########################################################################
*/

/*
########################################################################
SECTION 1000
SUPPORTED DRIVER PROTOCOL TYPES
------------------------------------------------------------------------
PURPOSE
Represents the current known protocol styles used by supported or
planned analyzer families.
########################################################################
*/
enum class DriverProtocolType {
    NANO_SHELL,
    LITE_VNA_V2_STYLE,
    EXPERIMENTAL_ASCII_SERIAL
}