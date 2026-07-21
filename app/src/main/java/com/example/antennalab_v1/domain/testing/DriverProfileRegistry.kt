package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.*

/*
########################################################################
FILE: DriverProfileRegistry.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Driver Registry

LAST UPDATED 26/3/2026 18:35

SYSTEM ROLE
Provides the central list of supported driver profiles.

ARCHITECTURE ROLE
This is the single source of truth for:
- what hardware is supported
- which protocol each setup uses
- which transport is expected

IMPORTANT DESIGN RULE
Do NOT scatter hardware support logic across UI or transport layers.
All supported hardware configurations must be declared here.
########################################################################
*/

/*
########################################################################
SECTION 1000
DRIVER PROFILE REGISTRY
------------------------------------------------------------------------
PURPOSE
Defines all supported driver profiles available to the app.
########################################################################
*/
object DriverProfileRegistry {

    /*
    ####################################################################
    SECTION 1100
    REGISTERED DRIVER PROFILES
    --------------------------------------------------------------------
    PURPOSE
    Curated list of supported hardware profiles.
    ####################################################################
    */
    val profiles: List<DriverProfile> = listOf(

        // ----------------------------------------------------------------
        // NanoVNA-H
        // ----------------------------------------------------------------
        DriverProfile(
            id = "nano_h_default",
            displayName = "NanoVNA-H (Default Shell)",
            hardwareFamily = HardwareFamily.NANO_SHELL_FAMILY,
            hardwareBrand = HardwareBrand.GENERIC,
            hardwareModel = HardwareModel.NANO_VNA_H,
            transportType = DriverTransportType.USB_SERIAL,
            protocolType = DriverProtocolType.NANO_SHELL,
            firmwareHint = "Standard NanoVNA-H firmware",
            supportTier = DriverSupportTier.STABLE,
            notes = "Uses ASCII shell command interface"
        ),

        // ----------------------------------------------------------------
        // NanoVNA-H4
        // ----------------------------------------------------------------
        DriverProfile(
            id = "nano_h4_default",
            displayName = "NanoVNA-H4 (Default Shell)",
            hardwareFamily = HardwareFamily.NANO_SHELL_FAMILY,
            hardwareBrand = HardwareBrand.GENERIC,
            hardwareModel = HardwareModel.NANO_VNA_H4,
            transportType = DriverTransportType.USB_SERIAL,
            protocolType = DriverProtocolType.NANO_SHELL,
            firmwareHint = "Standard NanoVNA-H4 firmware",
            supportTier = DriverSupportTier.STABLE,
            notes = "Uses ASCII shell command interface"
        ),

        // ----------------------------------------------------------------
        // LiteVNA64
        // ----------------------------------------------------------------
        DriverProfile(
            id = "litevna64_standard",
            displayName = "LiteVNA64 (Standard)",
            hardwareFamily = HardwareFamily.LITE_VNA_FAMILY,
            hardwareBrand = HardwareBrand.GENERIC,
            hardwareModel = HardwareModel.LITE_VNA_64,
            transportType = DriverTransportType.USB_SERIAL,
            protocolType = DriverProtocolType.LITE_VNA_V2_STYLE,
            firmwareHint = "LiteVNA standard firmware",
            supportTier = DriverSupportTier.EXPERIMENTAL,
            notes = "Binary / V2-style protocol"
        ),

        // ----------------------------------------------------------------
        // LiteVNA84
        // ----------------------------------------------------------------
        DriverProfile(
            id = "litevna84_standard",
            displayName = "LiteVNA84 (Standard)",
            hardwareFamily = HardwareFamily.LITE_VNA_FAMILY,
            hardwareBrand = HardwareBrand.GENERIC,
            hardwareModel = HardwareModel.LITE_VNA_84,
            transportType = DriverTransportType.USB_SERIAL,
            protocolType = DriverProtocolType.LITE_VNA_V2_STYLE,
            firmwareHint = "LiteVNA standard firmware",
            supportTier = DriverSupportTier.EXPERIMENTAL,
            notes = "Binary / V2-style protocol"
        ),

        // ----------------------------------------------------------------
        // Generic fallback
        // ----------------------------------------------------------------
        DriverProfile(
            id = "generic_ascii_serial",
            displayName = "Generic ASCII Serial VNA",
            hardwareFamily = HardwareFamily.EXPERIMENTAL_SERIAL_VNA,
            hardwareBrand = HardwareBrand.UNKNOWN,
            hardwareModel = HardwareModel.UNKNOWN,
            transportType = DriverTransportType.USB_SERIAL,
            protocolType = DriverProtocolType.EXPERIMENTAL_ASCII_SERIAL,
            firmwareHint = "Unknown",
            supportTier = DriverSupportTier.EXPERIMENTAL,
            notes = "Fallback for unknown serial-based VNAs"
        )
    )

    /*
    ####################################################################
    SECTION 1200
    LOOKUP BY ID
    --------------------------------------------------------------------
    PURPOSE
    Returns a driver profile by its unique ID.
    ####################################################################
    */
    fun getProfileById(id: String): DriverProfile? {
        return profiles.find { it.id == id }
    }

    /*
    ####################################################################
    SECTION 1300
    FILTER BY FAMILY
    --------------------------------------------------------------------
    PURPOSE
    Returns profiles matching a selected hardware family.
    ####################################################################
    */
    fun getProfilesForFamily(family: HardwareFamily): List<DriverProfile> {
        return profiles.filter { it.hardwareFamily == family }
    }
}