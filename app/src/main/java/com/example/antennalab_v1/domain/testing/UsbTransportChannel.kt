package com.example.antennalab_v1.domain.testing

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface

/*
########################################################################
FILE: UsbTransportChannel.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / USB Transport

SYSTEM ROLE
Represents a prepared live USB transport path for future analyzer
command exchange.

CURRENT DEVELOPMENT ROLE
Holds the claimed communication candidates discovered from the open USB
device so transport logic can later perform transfers without
re-scanning interface structure every time.

ARCHITECTURE NOTE
This channel now carries transport-type metadata so the shared USB
foundation can support multiple protocol families cleanly:

• generic bulk-style analyzer access
• CDC-data analyzer access
• future non-VNA USB transports later

IMPORTANT LIMITATION
This object still does not perform command execution itself. It only
stores the selected live channel and enough metadata for higher layers
to choose the correct protocol path.

SAFE EDIT AREA
- add claim state later
- add timeout policy later
- add protocol family tagging later
- add packet framing helpers later
########################################################################
*/
enum class UsbTransportKind {
    BULK_GENERIC,
    CDC_DATA
}

data class UsbTransportChannel(
    val connection: UsbDeviceConnection,
    val usbInterface: UsbInterface,
    val bulkInEndpoint: UsbEndpoint,
    val bulkOutEndpoint: UsbEndpoint,
    val maxReadPacketSize: Int,
    val maxWritePacketSize: Int,
    val transportKind: UsbTransportKind = UsbTransportKind.BULK_GENERIC,
    val transportLabel: String = "Generic USB Bulk"
)