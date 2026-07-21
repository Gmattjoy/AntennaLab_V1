package com.example.antennalab_v1.model

/*
########################################################################
FILE: DriverProfile.kt
PACKAGE: com.example.antennalab_v1.model
LAYER: Model / Driver Definition

LAST UPDATED 26/3/2026 18:20

SYSTEM ROLE
Defines the full operator-selectable driver profile used by AntennaLab
to bind a chosen hardware setup to a known transport and protocol path.

ARCHITECTURE ROLE
This is the central software truth for hardware setup.
Brand and model help describe the target hardware, but the driver
profile is the actual configuration record used by the connection layer.

IMPORTANT DESIGN RULE
User selection should choose a DriverProfile first, then validate that
profile against the connected instrument.
########################################################################
*/

/*
########################################################################
SECTION 1000
SUPPORT TIER ENUM
------------------------------------------------------------------------
PURPOSE
Describes how complete or production-ready a driver profile currently is.
########################################################################
*/
enum class DriverSupportTier {
    STABLE,
    EXPERIMENTAL,
    PLANNED
}

/*
########################################################################
SECTION 1100
DRIVER PROFILE MODEL
------------------------------------------------------------------------
PURPOSE
Stores the full software-facing setup definition for one supported
hardware profile.
########################################################################
*/
data class DriverProfile(
    val id: String,
    val displayName: String,
    val hardwareFamily: HardwareFamily,
    val hardwareBrand: HardwareBrand,
    val hardwareModel: HardwareModel,
    val transportType: DriverTransportType,
    val protocolType: DriverProtocolType,
    val firmwareHint: String = "",
    val supportsAutoValidation: Boolean = true,
    val supportTier: DriverSupportTier = DriverSupportTier.EXPERIMENTAL,
    val notes: String = ""
)