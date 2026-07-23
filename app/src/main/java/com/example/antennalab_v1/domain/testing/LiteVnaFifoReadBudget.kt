package com.example.antennalab_v1.domain.testing

import kotlin.math.ceil

/*
########################################################################
FILE: LiteVnaFifoReadBudget.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / LiteVNA FIFO read sizing

SYSTEM ROLE
Pure, device-free sizing math for draining the LiteVNA values-FIFO. A
LiteVNA sweep streams N fixed 32-byte records; the read must be driven by
the EXPECTED record count and a wall-clock budget, not by a fixed number
of USB read passes (the old fixed maxReadPasses=10 × 64-byte packets
capped every read at exactly 20 records → incomplete 20/101 sweeps).

This file is the unit-tested arithmetic seam (records → bytes → required
read budget at a given USB packet size). The blocking USB IO that consumes
these numbers lives in UsbCdcSerialChannel / UsbVnaCommandChannel and stays
device-only.
########################################################################
*/

internal const val LITEVNA_FIFO_RECORD_SIZE_BYTES = 32

internal data class FifoReadBudget(
    // Bytes we expect to drain for the outstanding records.
    val expectedBytes: Int,
    // Generous BACKSTOP ceiling on read passes — never the primary stop condition.
    val maxReadPasses: Int,
    // Hard wall-clock cap so a dead/slow device fails cleanly instead of hanging.
    val wallClockBudgetMs: Long,
    // How many consecutive empty/timeout reads to tolerate before concluding the
    // stream has ended (K, not 1 — a single inter-packet gap must not end the read).
    val maxConsecutiveIdleReads: Int
)

/*
Compute the read budget for `expectedRecordCount` records at USB packet size
`packetSizeBytes`. maxReadPasses is sized to comfortably drain the whole payload
(passes to cover expectedBytes at the packet size, plus a margin) — it exists only
as a backstop; completion is decided by byte/record count and the wall-clock budget.
*/
internal fun computeFifoReadBudget(
    expectedRecordCount: Int,
    packetSizeBytes: Int,
    recordSizeBytes: Int = LITEVNA_FIFO_RECORD_SIZE_BYTES,
    marginPasses: Int = 8,
    // The LiteVNA answers each readFIFO with only a small burst (~2-3 records) then
    // goes silent, so draining N records needs ~N/2 rapid re-issues. The wall-clock
    // budget is therefore sized generously (per-record round-trip cost), and idle
    // tolerance is SMALL: once a burst ends we want to re-issue immediately, not wait.
    baseWallClockMs: Long = 3000,
    perRecordWallClockMs: Long = 150,
    maxWallClockMs: Long = 20000,
    maxConsecutiveIdleReads: Int = 2
): FifoReadBudget {
    val records = expectedRecordCount.coerceAtLeast(1)
    val packet = packetSizeBytes.coerceAtLeast(1)
    val expectedBytes = records * recordSizeBytes
    val passesToDrain = ceil(expectedBytes.toDouble() / packet.toDouble()).toInt()
    val wallClock =
        (baseWallClockMs + records * perRecordWallClockMs).coerceAtMost(maxWallClockMs)

    return FifoReadBudget(
        expectedBytes = expectedBytes,
        maxReadPasses = passesToDrain + marginPasses,
        wallClockBudgetMs = wallClock,
        maxConsecutiveIdleReads = maxConsecutiveIdleReads
    )
}

// Number of COMPLETE fixed-size records in a byte buffer (a trailing partial record
// is not counted).
internal fun fifoRecordCount(
    byteCount: Int,
    recordSizeBytes: Int = LITEVNA_FIFO_RECORD_SIZE_BYTES
): Int = if (recordSizeBytes <= 0) 0 else byteCount / recordSizeBytes

/*
Accumulation stop predicate: keep reading while records are still expected AND the
wall-clock budget remains. This is the count-driven completion rule — the loop must
NOT stop early just because a batch stalled while more records are still expected and
time remains.
*/
internal fun shouldContinueFifoAccumulation(
    completeRecordCount: Int,
    expectedRecordCount: Int,
    nowMs: Long,
    deadlineMs: Long
): Boolean = completeRecordCount < expectedRecordCount && nowMs < deadlineMs
