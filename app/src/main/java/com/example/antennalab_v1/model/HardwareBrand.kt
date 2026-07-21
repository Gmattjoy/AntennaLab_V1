package com.example.antennalab_v1.model

/*
########################################################################
FILE: HardwareBrand.kt
PACKAGE: com.example.antennalab_v1.model
LAYER: Model / Hardware Selection

LAST UPDATED 26/3/2026 18:20

SYSTEM ROLE
Defines the user-visible hardware brand or seller label.

ARCHITECTURE ROLE
Brand is metadata for display, saved setup reuse, and operator guidance.
Brand must NOT be treated as the core protocol truth.

IMPORTANT DESIGN RULE
Different brands may map to the same driver profile.
The app should not assume brand equals protocol.
########################################################################
*/

/*
########################################################################
SECTION 1000
SUPPORTED HARDWARE BRANDS
------------------------------------------------------------------------
PURPOSE
Provides a controlled set of known brand labels for user selection and
saved hardware profiles.
########################################################################
*/
enum class HardwareBrand {
    ZEENKO,
    HUGEN,
    GENERIC,
    UNKNOWN
}