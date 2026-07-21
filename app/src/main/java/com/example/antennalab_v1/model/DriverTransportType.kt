package com.example.antennalab_v1.model

/*
########################################################################
FILE: DriverTransportType.kt
PACKAGE: com.example.antennalab_v1.model
LAYER: Model / Driver Definition

LAST UPDATED 26/3/2026 18:20

SYSTEM ROLE
Defines the transport type expected by a driver profile.

ARCHITECTURE ROLE
Transport type is separate from protocol type.
This allows the app to keep physical transport truth and protocol truth
as different layers.

CURRENT DIRECTION
For now the project is centered on USB-connected analyzers that behave
through a USB serial style transport.
########################################################################
*/

/*
########################################################################
SECTION 1000
SUPPORTED DRIVER TRANSPORT TYPES
------------------------------------------------------------------------
PURPOSE
Represents the transport layer expected by a selected driver profile.
########################################################################
*/
enum class DriverTransportType {
    USB_SERIAL
}