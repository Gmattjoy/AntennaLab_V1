package com.example.antennalab_v1.domain.testing

import android.hardware.usb.UsbConstants
import com.example.antennalab_v1.model.testing.InstrumentError
import com.example.antennalab_v1.model.testing.InstrumentErrorCategory

/*
########################################################################
FILE: UsbBulkTransferTester.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / USB Transport

LAST UPDATED 18/3/2026 02:28

SYSTEM ROLE
Performs the first low-level USB bulk transport checks against the
currently prepared transport channel.

CURRENT DEVELOPMENT ROLE
Provides a safe first communication-layer test before real analyzer
protocol commands are introduced.

ARCHITECTURE ROLE (UPDATED)
Now returns structured transport errors in addition to human-readable
summary text so higher layers can classify failures consistently.

IMPORTANT LIMITATION
This file does not yet send real NanoVNA or LiteVNA protocol commands.
It only verifies that the active transport channel is structurally ready
for future bulk I/O work.

SAFE EDIT AREA
- add write test later
- add read test later
- add protocol handshake later
- add timeout tuning later
########################################################################
*/

/*
########################################################################
EDIT SECTION 1000
TEST RESULT MODEL
------------------------------------------------------------------------
PURPOSE
Provides a compact result for low-level USB transport validation.
########################################################################
*/
data class UsbBulkTransferTestResult(
    val success: Boolean,
    val summary: String,
    val error: InstrumentError? = null
)

/*
########################################################################
EDIT SECTION 2000
TRANSFER TESTER
------------------------------------------------------------------------
PURPOSE
Checks whether the currently active transport channel is structurally
valid for future bulk transfer work.
########################################################################
*/
object UsbBulkTransferTester {

    /*
    ####################################################################
    EDIT SECTION 2001
    STRUCTURAL CHANNEL TEST
    --------------------------------------------------------------------
    PURPOSE
    Confirms that the active transport channel has the minimum shape
    required for future USB bulk transfers.
    ####################################################################
    */
    fun runStructuralChannelTest(): UsbBulkTransferTestResult {
        if (!UsbSessionManager.hasOpenSession()) {
            return UsbBulkTransferTestResult(
                success = false,
                summary = "USB bulk transfer test failed because no USB session is currently open.",
                error = InstrumentError(
                    category = InstrumentErrorCategory.TRANSPORT,
                    code = "USB_SESSION_NOT_OPEN",
                    summary = "USB session is not open.",
                    detail = "Bulk transport structure cannot be validated until a USB session is open.",
                    recoverable = true
                )
            )
        }

        val channel = UsbSessionManager.getActiveTransportChannel()
            ?: return UsbBulkTransferTestResult(
                success = false,
                summary = "USB bulk transfer test failed because no active transport channel is available.",
                error = InstrumentError(
                    category = InstrumentErrorCategory.TRANSPORT,
                    code = "USB_TRANSPORT_CHANNEL_MISSING",
                    summary = "No active transport channel is available.",
                    detail = "Session exists, but no BULK IN/BULK OUT transport pair has been prepared.",
                    recoverable = true
                )
            )

        val bulkInOk =
            channel.bulkInEndpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                    channel.bulkInEndpoint.direction == UsbConstants.USB_DIR_IN

        if (!bulkInOk) {
            return UsbBulkTransferTestResult(
                success = false,
                summary = "USB bulk transfer test failed because the selected read endpoint is not a BULK IN endpoint.",
                error = InstrumentError(
                    category = InstrumentErrorCategory.TRANSPORT,
                    code = "USB_BULK_IN_INVALID",
                    summary = "Selected read endpoint is not a valid BULK IN endpoint.",
                    detail = "Transport channel endpoint classification did not match required BULK IN behaviour.",
                    recoverable = true
                )
            )
        }

        val bulkOutOk =
            channel.bulkOutEndpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                    channel.bulkOutEndpoint.direction == UsbConstants.USB_DIR_OUT

        if (!bulkOutOk) {
            return UsbBulkTransferTestResult(
                success = false,
                summary = "USB bulk transfer test failed because the selected write endpoint is not a BULK OUT endpoint.",
                error = InstrumentError(
                    category = InstrumentErrorCategory.TRANSPORT,
                    code = "USB_BULK_OUT_INVALID",
                    summary = "Selected write endpoint is not a valid BULK OUT endpoint.",
                    detail = "Transport channel endpoint classification did not match required BULK OUT behaviour.",
                    recoverable = true
                )
            )
        }

        if (channel.maxReadPacketSize <= 0 || channel.maxWritePacketSize <= 0) {
            return UsbBulkTransferTestResult(
                success = false,
                summary = "USB bulk transfer test failed because one or more packet sizes are invalid.",
                error = InstrumentError(
                    category = InstrumentErrorCategory.TRANSPORT,
                    code = "USB_PACKET_SIZE_INVALID",
                    summary = "One or more USB packet sizes are invalid.",
                    detail = "Read or write packet size was zero or negative.",
                    recoverable = true
                )
            )
        }

        return UsbBulkTransferTestResult(
            success = true,
            summary = "USB transport channel structure is valid for future bulk transfer work. Interface class=${channel.usbInterface.interfaceClass} subclass=${channel.usbInterface.interfaceSubclass} protocol=${channel.usbInterface.interfaceProtocol} readPacket=${channel.maxReadPacketSize} writePacket=${channel.maxWritePacketSize}.",
            error = null
        )
    }
}