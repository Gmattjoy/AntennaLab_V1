package com.example.antennalab_v1.model

/*
########################################################################
FILE: HardwareMeasurementCapabilities.kt
PACKAGE: com.example.antennalab_v1.model
LAYER: Model / Hardware Capability Definition

LAST UPDATED 18/3/2026 00:40

ARCHITECTURE ROLE (UPDATED)

Represents a capability PROFILE — NOT a device.

IMPORTANT CHANGE
Capabilities are now:

• protocol-derived
• driver-provided
• not hardcoded to specific product names

########################################################################
EDIT SECTION 0-1
########################################################################
*/

data class HardwareMeasurementCapabilities(

    val supportsS11: Boolean = false,
    val supportsS11Phase: Boolean = false,

    val supportsS21: Boolean = false,
    val supportsS21Phase: Boolean = false,

    val supportsResistance: Boolean = false,
    val supportsReactance: Boolean = false,

    val supportsSmithChart: Boolean = false,
    val supportsImpedanceLocus: Boolean = false,

    val supportsReturnLoss: Boolean = false,
    val supportsSWR: Boolean = false,

    val supportsTDR: Boolean = false,
    val supportsPhaseAnalysis: Boolean = false
)

/*
########################################################################
EDIT SECTION 1-2
########################################################################
*/

object HardwareCapabilityProfiles {

    /*
    --------------------------------------------------------------------
    GENERIC BASELINES (IMPORTANT FOR DISCOVERY MODE)
    --------------------------------------------------------------------
    */

    val UNKNOWN_MINIMAL = HardwareMeasurementCapabilities(
        supportsS11 = true,
        supportsSWR = true
    )

    val VNA_STANDARD = HardwareMeasurementCapabilities(
        supportsS11 = true,
        supportsS11Phase = true,
        supportsS21 = true,
        supportsS21Phase = true,
        supportsResistance = true,
        supportsReactance = true,
        supportsSmithChart = true,
        supportsImpedanceLocus = true,
        supportsReturnLoss = true,
        supportsSWR = true,
        supportsPhaseAnalysis = true
    )
}