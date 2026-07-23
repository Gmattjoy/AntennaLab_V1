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
    EDIT SECTION 1050
    COUNT-DRIVEN RAW CDC READ (LiteVNA FIFO)
    --------------------------------------------------------------------
    PURPOSE
    Drain a KNOWN payload (expectedBytes) rather than stopping after a
    fixed number of passes or on the first idle read. This is how the
    LiteVNA values-FIFO must be read: keep accumulating until we have all
    the expected bytes, tolerating up to maxConsecutiveIdleReads empty
    reads (a momentary inter-packet gap must not end the read), bounded by
    a hard wall-clock budget so a dead/slow device fails cleanly. The
    maxReadPasses argument is only a backstop ceiling.
    ####################################################################
    */
    fun readRawBytesUntil(
        expectedBytes: Int,
        maxReadPasses: Int,
        readTimeoutMs: Int,
        interReadDelayMs: Int,
        maxConsecutiveIdleReads: Int,
        wallClockBudgetMs: Long
    ): ByteArray {
        val packetSize =
            transportChannel.maxReadPacketSize.coerceAtLeast(64)

        val collectedBytes = mutableListOf<Byte>()
        var readPassCount = 0
        var consecutiveIdleReads = 0
        val deadlineMs = System.currentTimeMillis() + wallClockBudgetMs.coerceAtLeast(0)

        while (
            collectedBytes.size < expectedBytes &&
            readPassCount < maxReadPasses &&
            System.currentTimeMillis() < deadlineMs
        ) {
            readPassCount += 1

            val buffer = ByteArray(packetSize)

            val bytesRead = transportChannel.connection.bulkTransfer(
                transportChannel.bulkInEndpoint,
                buffer,
                buffer.size,
                readTimeoutMs
            )

            if (bytesRead <= 0) {
                consecutiveIdleReads += 1

                // Tolerate a small run of empty reads (the device may pause between
                // packets while it is still measuring); only give up after K idle
                // reads in a row, never after the first.
                if (consecutiveIdleReads >= maxConsecutiveIdleReads) {
                    break
                }

                if (interReadDelayMs > 0) {
                    runCatching {
                        Thread.sleep(interReadDelayMs.toLong())
                    }
                }
                continue
            }

            consecutiveIdleReads = 0

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
