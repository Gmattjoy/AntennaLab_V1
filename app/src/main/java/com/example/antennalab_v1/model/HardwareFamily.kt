package com.example.antennalab_v1.model

/*
########################################################################
FILE: HardwareFamily.kt
PACKAGE: com.example.antennalab_v1.model
LAYER: Model / Hardware Selection

LAST UPDATED 26/3/2026 18:20

SYSTEM ROLE
Defines the supported high-level hardware families used by AntennaLab.

ARCHITECTURE ROLE
This enum is intentionally family-level only.
It does NOT represent a seller, a specific product listing, or a USB
identity result.

IMPORTANT DESIGN RULE
Driver binding should be based on a chosen driver profile, not on brand
name alone. This family enum exists to support profile grouping,
operator selection, and future expansion.
########################################################################
*/

/*
########################################################################
SECTION 1000
SUPPORTED HARDWARE FAMILIES
------------------------------------------------------------------------
PURPOSE
Represents the main protocol-oriented hardware groups supported by the
app at this stage.
########################################################################
*/
enum class HardwareFamily {
    NANO_SHELL_FAMILY,
    LITE_VNA_FAMILY,
    EXPERIMENTAL_SERIAL_VNA
}