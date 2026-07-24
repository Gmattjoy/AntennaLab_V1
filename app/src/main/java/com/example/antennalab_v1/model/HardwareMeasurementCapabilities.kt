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
        // RECONCILING A CONTRADICTION, not enabling a new feature. Two capability
        // layers described the same feature oppositely:
        //   HardwareCapabilityProfile.supportsTdrPreview = TRUE for both profiles
        //     (ProjectData.kt:598, :621) — but its only accessor,
        //     supportsTdrPreviewOrDefault (:133), has ZERO call sites: dead data.
        //   HardwareMeasurementCapabilities.supportsTDR = omitted here, so FALSE —
        //     and this is the one actually consumed (SweepGraphMath.kt:652 guard,
        //     SweepGraphWidgets.kt:569 render).
        // Net effect: no device could ever show a cable-fault distance, so the TDR
        // velocity-factor fix had no reachable UI surface. TRUE is correct on the
        // merits — the preview is derived from S11, which both one-port devices
        // produce. Same disease as Finding #7: more than one source of truth for
        // hardware capability.
        supportsTDR = true,
        supportsPhaseAnalysis = true
    )
}