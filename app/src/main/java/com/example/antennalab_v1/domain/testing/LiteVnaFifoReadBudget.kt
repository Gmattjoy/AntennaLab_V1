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

/*
Wall-clock budget for the DISTINCT-in-range collection mode. Because the LiteVNA
free-runs a superset sweep and we only get ~2 records per read, gathering every
target index is a coupon-collector problem — it needs many more reads than the
record count. Sized generously (per-target cost) and hard-capped so a starved index
fails cleanly with an honest partial count instead of hanging.
*/
internal fun computeDistinctCollectionBudgetMs(
    pointCount: Int,
    baseMs: Long = 4000,
    perPointMs: Long = 400,
    maxMs: Long = 45000
): Long {
    val points = pointCount.coerceAtLeast(1)
    return (baseMs + points * perPointMs).coerceAtMost(maxMs)
}

/*
PROBE MODE.

The bring-up mini sweep probe is a LIVENESS check, not a measurement: it only has to
prove the device streams decodable records. It was previously held to the measurement
rule above (`computeDistinctCollectionBudgetMs` + "all target indices seen"), which on
this firmware is coupon-collecting indices 0..7 out of a free-running ~201-point sweep —
roughly 4% of what the device produces. The probe therefore burned its entire 7200 ms
budget almost every time and pushed total bring-up past the 15 s worker join, which
reported TIMED_OUT on healthy hardware and (via the support-tier coupling) removed the
live sweep path altogether. Measured 15.2 s on real hardware, 2026-07-24.

8 records is the right threshold because buildParsedSweepOutcomeFromBytes already falls
back to SEQUENTIAL_FALLBACK once it has MINIMUM_USABLE_SWEEP_POINTS = 8 records at ANY
indices — so 8 raw records is precisely what makes the probe pass.
*/
internal const val LITEVNA_PROBE_MIN_RECORDS = 8

/*
Backstop so a dead or silent device still fails FAST and well inside the 15 s join,
rather than overrunning it. Deliberately much smaller than the measurement budget.
*/
internal fun computeProbeCollectionBudgetMs(): Long = 2500

/*
Probe stop predicate: keep reading only until enough records prove the path, or the
(short) probe budget expires. Unlike the measurement rule this ignores WHICH indices
arrived — coverage is not what a liveness probe is establishing.
*/
internal fun shouldContinueProbeAccumulation(
    completeRecordCount: Int,
    minRecords: Int,
    nowMs: Long,
    deadlineMs: Long
): Boolean = completeRecordCount < minRecords && nowMs < deadlineMs

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
