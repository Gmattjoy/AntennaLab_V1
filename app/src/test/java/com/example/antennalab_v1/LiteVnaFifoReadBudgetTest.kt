package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.computeDistinctCollectionBudgetMs
import com.example.antennalab_v1.domain.testing.computeFifoReadBudget
import com.example.antennalab_v1.domain.testing.fifoRecordCount
import com.example.antennalab_v1.domain.testing.shouldContinueFifoAccumulation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure coverage for the LiteVNA FIFO read-budget math (records → bytes → required
 * read budget at a given USB packet size) and the count-driven accumulation stop
 * predicate. The blocking USB IO that consumes these numbers stays device-only.
 */
class LiteVnaFifoReadBudgetTest {

    // ------------------------------------------------------------------
    // computeFifoReadBudget
    // ------------------------------------------------------------------

    @Test
    fun budget_fullSweepAt64BytePackets() {
        // 101 records × 32 = 3232 bytes; passes = ceil(3232/64)=51, +8 margin = 59.
        // wall clock = 3000 + 101×150 = 18150 (< 20000 cap). Idle tolerance is small
        // (2) so an ended burst re-issues the next readFIFO promptly.
        val b = computeFifoReadBudget(expectedRecordCount = 101, packetSizeBytes = 64)
        assertEquals(3232, b.expectedBytes)
        assertEquals(59, b.maxReadPasses)
        assertEquals(18150L, b.wallClockBudgetMs)
        assertEquals(2, b.maxConsecutiveIdleReads)
    }

    @Test
    fun budget_scalesPassesWithPacketSize() {
        // Larger packets need far fewer passes: ceil(3232/512)=7, +8 = 15.
        val b = computeFifoReadBudget(expectedRecordCount = 101, packetSizeBytes = 512)
        assertEquals(3232, b.expectedBytes)
        assertEquals(15, b.maxReadPasses)
    }

    @Test
    fun budget_miniProbe() {
        // 8 records × 32 = 256 bytes; passes = ceil(256/64)=4, +8 = 12.
        val b = computeFifoReadBudget(expectedRecordCount = 8, packetSizeBytes = 64)
        assertEquals(256, b.expectedBytes)
        assertEquals(12, b.maxReadPasses)
        assertEquals(4200L, b.wallClockBudgetMs) // 3000 + 8×150
    }

    @Test
    fun budget_wallClockIsCappedForHugeCounts() {
        // 255 records → 3000 + 255×150 = 41250, capped at 20000.
        val b = computeFifoReadBudget(expectedRecordCount = 255, packetSizeBytes = 64)
        assertEquals(20000L, b.wallClockBudgetMs)
    }

    @Test
    fun budget_coercesNonPositiveInputs_noDivideByZero() {
        val zeroRecords = computeFifoReadBudget(expectedRecordCount = 0, packetSizeBytes = 64)
        assertEquals(32, zeroRecords.expectedBytes) // records coerced to 1 → 1×32
        val zeroPacket = computeFifoReadBudget(expectedRecordCount = 101, packetSizeBytes = 0)
        // packet coerced to 1 → passes = 3232 + 8 margin
        assertEquals(3240, zeroPacket.maxReadPasses)
    }

    // ------------------------------------------------------------------
    // computeDistinctCollectionBudgetMs
    // ------------------------------------------------------------------

    @Test
    fun distinctCollectionBudget_scalesPerPointAndCaps() {
        // 101 points → 4000 + 101×400 = 44400 (< 45000 cap).
        assertEquals(44400L, computeDistinctCollectionBudgetMs(101))
        // 8-point probe → 4000 + 8×400 = 7200.
        assertEquals(7200L, computeDistinctCollectionBudgetMs(8))
        // Huge count caps at 45000.
        assertEquals(45000L, computeDistinctCollectionBudgetMs(1000))
        // Non-positive coerced to 1 → 4400.
        assertEquals(4400L, computeDistinctCollectionBudgetMs(0))
    }

    // ------------------------------------------------------------------
    // fifoRecordCount
    // ------------------------------------------------------------------

    @Test
    fun fifoRecordCount_dropsTrailingPartialRecord() {
        assertEquals(20, fifoRecordCount(640))   // the old truncation point
        assertEquals(101, fifoRecordCount(3232))
        assertEquals(1, fifoRecordCount(33))      // 1 complete + 1 leftover byte
        assertEquals(0, fifoRecordCount(31))
        assertEquals(0, fifoRecordCount(640, recordSizeBytes = 0)) // guard
    }

    // ------------------------------------------------------------------
    // shouldContinueFifoAccumulation
    // ------------------------------------------------------------------

    @Test
    fun shouldContinue_keepsGoingWhileRecordsExpectedAndBudgetRemains() {
        assertTrue(
            shouldContinueFifoAccumulation(
                completeRecordCount = 20, expectedRecordCount = 101,
                nowMs = 100, deadlineMs = 5000
            )
        )
    }

    @Test
    fun shouldContinue_stopsWhenAllRecordsCollected() {
        assertFalse(
            shouldContinueFifoAccumulation(
                completeRecordCount = 101, expectedRecordCount = 101,
                nowMs = 100, deadlineMs = 5000
            )
        )
    }

    @Test
    fun shouldContinue_stopsWhenWallClockExhausted() {
        assertFalse(
            shouldContinueFifoAccumulation(
                completeRecordCount = 20, expectedRecordCount = 101,
                nowMs = 5000, deadlineMs = 5000 // now == deadline → stop
            )
        )
        assertFalse(
            shouldContinueFifoAccumulation(
                completeRecordCount = 20, expectedRecordCount = 101,
                nowMs = 6000, deadlineMs = 5000
            )
        )
    }
}
