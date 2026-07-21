package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.HardwareCapabilityProfiles
import com.example.antennalab_v1.model.HardwareMeasurementCapabilities

/*
########################################################################
FILE: UsbVnaProtocolResolver.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Protocol Resolution

LAST UPDATED 18/3/2026 01:18

SYSTEM ROLE
Resolves connected hardware into:

• protocol family
• capability profile
• support tier

ARCHITECTURE ROLE
Foundation for driver registry.

IMPORTANT
This file is now the SINGLE resolver location.
UsbVnaProtocol.kt must not also declare a resolver object.

SAFE EDIT AREA
- introduce driver registry later
- replace heuristic detection later
- integrate discovery mode later
########################################################################
*/

/*
########################################################################
EDIT SECTION 1000
IDENTITY MODEL
########################################################################
*/
data class InstrumentIdentity(
    val protocolFamily: String,
    val capability: HardwareMeasurementCapabilities,
    val supportTier: String
)

/*
########################################################################
EDIT SECTION 2000
RESOLVER
########################################################################
*/
object UsbVnaProtocolResolver {

    private val knownProtocols: List<UsbVnaProtocol> = listOf(
        LiteVnaProtocol,
        NanoVnaProtocol
    )

    /*
    --------------------------------------------------------------------
    RESOLVE FROM RAW IDENTITY TEXT
    --------------------------------------------------------------------
    PURPOSE
    Maps raw analyzer identity text into a known protocol family record.
    */
    fun resolveFromIdentityText(
        rawIdentityText: String?
    ): UsbVnaProtocolIdentity {
        val matchedProtocol =
            knownProtocols.firstOrNull { protocol ->
                protocol.matchesIdentityText(rawIdentityText)
            } ?: UnknownUsbVnaProtocol

        return UsbVnaProtocolIdentity(
            family = matchedProtocol.family,
            displayName = matchedProtocol.displayName,
            rawIdentityText = rawIdentityText
        )
    }

    /*
    --------------------------------------------------------------------
    CURRENT IDENTITY
    --------------------------------------------------------------------
    PURPOSE
    Provides the current architecture-level identity snapshot used by
    source selection and future session-state modeling.
    */
    fun resolveCurrentIdentity(): InstrumentIdentity {

        /*
        ------------------------------------------------------------
        CURRENT STRATEGY (TEMPORARY BUT SAFE)
        ------------------------------------------------------------
        We do NOT assume a specific device model here.

        For the stabilization pass we return a general VNA-compatible
        identity profile so the system architecture no longer depends on
        NanoVNA/LiteVNA-specific capability ownership.

        Future:
        - USB VID/PID matching
        - command probing
        - discovery mode classification
        - support tier escalation
        ------------------------------------------------------------
        */
        return InstrumentIdentity(
            protocolFamily = "VNA-Compatible",
            capability = HardwareCapabilityProfiles.VNA_STANDARD,
            supportTier = "Partial Support"
        )
    }
}