package com.example.antennalab_v1.domain.testing

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import com.example.antennalab_v1.model.testing.UsbHardwareSession

/*
########################################################################
FILE: UsbPermissionManager.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / USB

LAST UPDATED 19/3/2026 10:42

SYSTEM ROLE
Handles first-pass Android USB permission requests for analyzer access.

CURRENT DEVELOPMENT ROLE
Starts a USB permission request for the detected device and returns the
refreshed unified session truth immediately.

ARCHITECTURE RULE
This file must NOT fabricate connection or discovery state.
It may only:
• trigger Android permission flow
• return UsbSessionManager truth

SAFE EDIT AREA
- add explicit device selection later
- add permission result receiver wiring later
- add analyzer filtering by VID/PID later
- add multi-device permission routing later
########################################################################
*/
object UsbPermissionManager {
    const val ACTION_USB_PERMISSION =
        "com.example.antennalab_v1.USB_PERMISSION"

    /*
    ####################################################################
    EDIT SECTION 2000
    REQUEST PERMISSION
    --------------------------------------------------------------------
    PURPOSE
    Requests Android USB permission for the first detected device and
    then returns refreshed shared session truth.
    ####################################################################
    */
    fun requestPermission(
        context: Context,
        selectedHardwareName: String
    ): UsbHardwareSession {
        val usbManager =
            context.getSystemService(Context.USB_SERVICE) as? UsbManager

        if (usbManager == null) {
            return UsbSessionManager.refreshCurrentSessionState(
                context = context,
                selectedHardwareName = selectedHardwareName
            )
        }

        val deviceList = usbManager.deviceList.values.toList()

        if (deviceList.isEmpty()) {
            return UsbSessionManager.refreshCurrentSessionState(
                context = context,
                selectedHardwareName = selectedHardwareName
            )
        }

        val firstDevice = deviceList.first()

        if (usbManager.hasPermission(firstDevice)) {
            return UsbSessionManager.refreshCurrentSessionState(
                context = context,
                selectedHardwareName = selectedHardwareName
            )
        }

        val permissionIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        usbManager.requestPermission(firstDevice, permissionIntent)

        return UsbSessionManager.refreshCurrentSessionState(
            context = context,
            selectedHardwareName = selectedHardwareName
        )
    }
}