package com.example.antennalab_v1.model

/*
########################################################################
FILE: UsbConnectionInfo.kt
PACKAGE: com.example.antennalab_v1.model
LAYER: Model / USB Connection

LAST UPDATED 19/3/2026 10:42

SYSTEM ROLE
Stores structured USB connection facts for analyzer access.

ARCHITECTURE ROLE (CLEANED UP)
This model now represents ONLY the physical/session connection layer.

THIS FILE MUST CONTAIN
• physical connection state
• permission state
• session-open state
• vendor/product identifiers
• interface count
• debug summary

THIS FILE MUST NOT CONTAIN
• protocol classification
• support tier
• discovery truth
• driver resolution
• measurement trust

Those belong to:
→ InstrumentSessionState

SAFE EDIT AREA
- add manufacturer/product strings later
- add endpoint/interface details later
- add transport error details later
- add per-device unique identifiers later
########################################################################
*/

enum class HardwareConnectionState {
    NOT_CONNECTED,
    DEVICE_DETECTED,
    PERMISSION_REQUIRED,
    READY,
    BUSY,
    ERROR
}

/*
########################################################################
EDIT SECTION 2000
USB CONNECTION MODEL
########################################################################
*/
data class UsbConnectionInfo(

    val state: HardwareConnectionState = HardwareConnectionState.NOT_CONNECTED,

    val deviceName: String = "No USB analyzer detected",

    val permissionGranted: Boolean = false,
    val portInUse: Boolean = false,
    val sessionOpen: Boolean = false,

    val vendorId: Int? = null,
    val productId: Int? = null,

    val interfaceCount: Int = 0,

    val debugSummary: String = "USB scan not started."
)