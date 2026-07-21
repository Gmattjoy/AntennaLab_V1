package com.example.antennalab_v1.domain.testing

import android.content.Context
import com.example.antennalab_v1.model.testing.UsbHardwareSession

/*
########################################################################
FILE: UsbDeviceScanner.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / USB

LAST UPDATED 19/3/2026 09:45

SYSTEM ROLE
Provides a device scan trigger for USB-connected hardware.

CURRENT DEVELOPMENT ROLE
This scanner is now intentionally STATELESS.

ARCHITECTURE RULE (ENFORCED)
• This file must NOT define connection truth
• This file must NOT cache device state
• This file must NOT attempt session logic

ALL truth MUST come from:
→ UsbSessionManager

This prevents:
• stale device lists
• ghost connections
• split-brain session state
• incorrect reconnect behaviour

SAFE EDIT AREA
- add multi-device selection later (still defer to SessionManager)
- add filtered scan views later (read-only only)
########################################################################
*/

class UsbDeviceScanner {

    /*
    ####################################################################
    EDIT SECTION 1000
    SCAN DEVICES
    --------------------------------------------------------------------
    PURPOSE
    Triggers a USB state refresh and returns the unified session truth.

    IMPORTANT
    This does NOT perform scanning logic itself anymore.
    It delegates completely to UsbSessionManager.
    ####################################################################
    */
    fun scanDevices(
        context: Context,
        selectedHardwareName: String
    ): UsbHardwareSession {

        return UsbSessionManager.refreshCurrentSessionState(
            context = context,
            selectedHardwareName = selectedHardwareName
        )
    }
}