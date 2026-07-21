package com.example.antennalab_v1.model.testing

/*
########################################################################
FILE: InstrumentError.kt
PACKAGE: com.example.antennalab_v1.model.testing
LAYER: Model / Testing / Error Taxonomy

LAST UPDATED 18/3/2026 02:28

SYSTEM ROLE
Defines the shared structured instrument error model used across
transport, identity, protocol, capability, and sweep layers.

ARCHITECTURE ROLE
Provides a single error taxonomy so failures can be:

• classified consistently
• surfaced truthfully in UI
• logged and debugged reliably
• extended later without string-based drift

SAFE EDIT AREA
- add numeric severity later
- add remediation guidance later
- add telemetry identifiers later
- add nested cause chains later
########################################################################
*/

/*
########################################################################
EDIT SECTION 1000
ERROR CATEGORY
########################################################################
*/
enum class InstrumentErrorCategory {
    TRANSPORT,
    PROTOCOL,
    IDENTITY,
    SWEEP,
    CAPABILITY,
    UNKNOWN
}

/*
########################################################################
EDIT SECTION 2000
ERROR MODEL
########################################################################
*/
data class InstrumentError(
    val category: InstrumentErrorCategory = InstrumentErrorCategory.UNKNOWN,
    val code: String = "UNKNOWN_ERROR",
    val summary: String = "Unknown instrument error.",
    val detail: String? = null,
    val recoverable: Boolean = true
)