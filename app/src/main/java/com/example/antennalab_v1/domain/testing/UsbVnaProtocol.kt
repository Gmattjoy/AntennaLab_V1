package com.example.antennalab_v1.domain.testing

/*
########################################################################
FILE: UsbVnaProtocol.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / USB Protocol

LAST UPDATED 18/3/2026 01:18

SYSTEM ROLE
Defines the analyzer protocol family boundary used by USB VNA transport
integration.

CURRENT DEVELOPMENT ROLE
Provides a clean protocol-selection layer between the low-level USB
command channel and future real sweep command implementations.

IMPORTANT LIMITATION
This version does not yet implement real sweep commands. It only
classifies analyzer family identity and provides a stable protocol model
for the next protocol step.

SAFE EDIT AREA
- add real sweep commands later
- add calibration commands later
- add device capability queries later
- add family-specific parsing later
########################################################################
*/

/*
########################################################################
EDIT SECTION 1000
ANALYZER FAMILY ENUM
------------------------------------------------------------------------
PURPOSE
Represents the currently recognized analyzer protocol families.
########################################################################
*/
enum class UsbVnaProtocolFamily {
    NANOVNA,
    LITEVNA,
    UNKNOWN
}

/*
########################################################################
EDIT SECTION 1100
PROTOCOL IDENTITY MODEL
------------------------------------------------------------------------
PURPOSE
Provides a stable protocol-resolution result based on analyzer identity
text returned from the command layer.
########################################################################
*/
data class UsbVnaProtocolIdentity(
    val family: UsbVnaProtocolFamily,
    val displayName: String,
    val rawIdentityText: String?
) {
    fun isKnownFamily(): Boolean {
        return family != UsbVnaProtocolFamily.UNKNOWN
    }
}

/*
########################################################################
EDIT SECTION 2000
PROTOCOL CONTRACT
------------------------------------------------------------------------
PURPOSE
Defines the minimum behaviour expected from an analyzer protocol family
implementation.
########################################################################
*/
interface UsbVnaProtocol {

    val family: UsbVnaProtocolFamily

    val displayName: String

    fun matchesIdentityText(
        rawIdentityText: String?
    ): Boolean
}

/*
########################################################################
EDIT SECTION 3000
NANOVNA PROTOCOL
------------------------------------------------------------------------
PURPOSE
Represents NanoVNA-family analyzers.

CURRENT LIMITATION
Real command implementation is intentionally not added yet.
########################################################################
*/
object NanoVnaProtocol : UsbVnaProtocol {

    override val family: UsbVnaProtocolFamily =
        UsbVnaProtocolFamily.NANOVNA

    override val displayName: String = "NanoVNA"

    override fun matchesIdentityText(
        rawIdentityText: String?
    ): Boolean {
        val normalized = rawIdentityText?.trim()?.lowercase()
            ?: return false

        return "nanovna" in normalized
    }
}

/*
########################################################################
EDIT SECTION 3100
LITEVNA PROTOCOL
------------------------------------------------------------------------
PURPOSE
Represents LiteVNA-family analyzers.

CURRENT LIMITATION
Real command implementation is intentionally not added yet.
########################################################################
*/
object LiteVnaProtocol : UsbVnaProtocol {

    override val family: UsbVnaProtocolFamily =
        UsbVnaProtocolFamily.LITEVNA

    override val displayName: String = "LiteVNA"

    override fun matchesIdentityText(
        rawIdentityText: String?
    ): Boolean {
        val normalized = rawIdentityText?.trim()?.lowercase()
            ?: return false

        return "litevna" in normalized
    }
}

/*
########################################################################
EDIT SECTION 3200
UNKNOWN PROTOCOL
------------------------------------------------------------------------
PURPOSE
Safe fallback protocol record used when identity text does not match a
known family.
########################################################################
*/
object UnknownUsbVnaProtocol : UsbVnaProtocol {

    override val family: UsbVnaProtocolFamily =
        UsbVnaProtocolFamily.UNKNOWN

    override val displayName: String = "Unknown"

    override fun matchesIdentityText(
        rawIdentityText: String?
    ): Boolean {
        return false
    }
}