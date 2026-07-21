package com.example.antennalab_v1.domain.testing

import kotlin.text.Charsets.UTF_8

/*
########################################################################
FILE: UsbCdcSerialChannel.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / USB Transport

LAST UPDATED 29/3/2026 10:30

SYSTEM ROLE
Provides a CDC-style serial stream wrapper over the prepared USB
transport channel.

CURRENT DEVELOPMENT ROLE
This version supports both:
- raw binary reads for LiteVNA V2 protocol traffic
- text reads for legacy shell-style command paths
########################################################################
*/
class UsbCdcSerialChannel(
    private val transportChannel: UsbTransportChannel
) {

    fun writeBytes(
        payload: ByteArray,
        timeoutMs: Int
    ): Int {
        return transportChannel.connection.bulkTransfer(
            transportChannel.bulkOutEndpoint,
            payload,
            payload.size,
            timeoutMs
        )
    }

    /*
    ####################################################################
    EDIT SECTION 1000
    RAW CDC READ
    ####################################################################
    */
    fun readRawBytes(
        timeoutMs: Int,
        maxReadPasses: Int,
        interReadDelayMs: Int
    ): ByteArray {
        val packetSize =
            transportChannel.maxReadPacketSize.coerceAtLeast(64)

        val collectedBytes = mutableListOf<Byte>()
        var readPassCount = 0
        var idleReadPasses = 0

        while (readPassCount < maxReadPasses) {
            readPassCount += 1

            val buffer = ByteArray(packetSize)

            val bytesRead = transportChannel.connection.bulkTransfer(
                transportChannel.bulkInEndpoint,
                buffer,
                buffer.size,
                timeoutMs
            )

            if (bytesRead <= 0) {
                idleReadPasses += 1

                if (collectedBytes.isNotEmpty() || idleReadPasses >= 2) {
                    break
                }

                if (interReadDelayMs > 0) {
                    runCatching {
                        Thread.sleep(interReadDelayMs.toLong())
                    }
                }
                continue
            }

            idleReadPasses = 0

            val safeBytesRead = bytesRead.coerceIn(0, buffer.size)
            for (index in 0 until safeBytesRead) {
                collectedBytes.add(buffer[index])
            }

            if (interReadDelayMs > 0) {
                runCatching {
                    Thread.sleep(interReadDelayMs.toLong())
                }
            }
        }

        return collectedBytes.toByteArray()
    }

    /*
    ####################################################################
    EDIT SECTION 1100
    TEXT CDC READ
    ####################################################################
    */
    fun readTextBytes(
        timeoutMs: Int,
        maxReadPasses: Int,
        interReadDelayMs: Int
    ): ByteArray {
        val packetSize =
            transportChannel.maxReadPacketSize.coerceAtLeast(64)

        val collectedBytes = mutableListOf<Byte>()
        var readPassCount = 0
        var idleReadPasses = 0

        while (readPassCount < maxReadPasses) {
            readPassCount += 1

            val buffer = ByteArray(packetSize)

            val bytesRead = transportChannel.connection.bulkTransfer(
                transportChannel.bulkInEndpoint,
                buffer,
                buffer.size,
                timeoutMs
            )

            if (bytesRead <= 0) {
                idleReadPasses += 1

                if (collectedBytes.isNotEmpty() || idleReadPasses >= 2) {
                    break
                }

                if (interReadDelayMs > 0) {
                    runCatching {
                        Thread.sleep(interReadDelayMs.toLong())
                    }
                }
                continue
            }

            idleReadPasses = 0

            val safeBytesRead = bytesRead.coerceIn(0, buffer.size)
            for (index in 0 until safeBytesRead) {
                collectedBytes.add(buffer[index])
            }

            val decoded =
                collectedBytes.toByteArray().toString(UTF_8)

            if (decoded.contains("\n") || decoded.contains("\r")) {
                break
            }

            if (interReadDelayMs > 0) {
                runCatching {
                    Thread.sleep(interReadDelayMs.toLong())
                }
            }
        }

        return collectedBytes.toByteArray()
    }

    /*
    ####################################################################
    EDIT SECTION 1200
    LEGACY ALIAS
    ####################################################################
    */
    fun readBytes(
        timeoutMs: Int,
        maxReadPasses: Int,
        interReadDelayMs: Int
    ): ByteArray {
        return readTextBytes(
            timeoutMs = timeoutMs,
            maxReadPasses = maxReadPasses,
            interReadDelayMs = interReadDelayMs
        )
    }
}
