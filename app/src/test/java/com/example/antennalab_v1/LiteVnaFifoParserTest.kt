package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.DistinctInRangeAccumulator
import com.example.antennalab_v1.domain.testing.LiteVnaFifoRecord
import com.example.antennalab_v1.domain.testing.analyzeFreqIndices
import com.example.antennalab_v1.domain.testing.parseLiteVnaFifoRecords
import com.example.antennalab_v1.domain.testing.selectDirectRecords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

/**
 * Pure-JVM reproduction of the LiteVNA "43/101" parse bug from a REAL captured FIFO
 * payload (a live 145 MHz, 101-point sweep, tag `LiteVnaFifoRaw`, saved as a base64
 * fixture). Confirms the acquisition read is complete (101 × 32-byte records) but the
 * device's freqIndex is a scattered sampling of a free-running sweep (values span
 * 0..193, in consecutive pairs), so the direct-index filter keeps only ~40 points.
 *
 * This LOCKS the bug: the corrective fix should make a coherent 101-point sweep parse
 * cleanly (this test will then be updated to assert the fixed behaviour).
 */
class LiteVnaFifoParserTest {

    private fun fixtureBytes(): ByteArray {
        val b64 = this::class.java.getResourceAsStream("/litevna/fifo_145mhz_101req.b64")!!
            .readBytes().toString(Charsets.US_ASCII).trim()
        return Base64.getDecoder().decode(b64)
    }

    @Test
    fun capturedPayload_isAFullyReadButScatteredSweep() {
        val bytes = fixtureBytes()
        // Acquisition delivered the full payload: 101 complete 32-byte records.
        assertEquals(3232, bytes.size)
        val records = parseLiteVnaFifoRecords(bytes)
        assertEquals(101, records.size)

        val analysis = analyzeFreqIndices(records, requestedPointCount = 101)
        assertEquals(101, analysis.decodedCount)
        // freqIndex ranges 0..193 — a free-running sweep sampled across >1 pass, NOT a
        // contiguous 0..100. That is why so many records fall out of range.
        assertEquals(0, analysis.minIndex)
        assertEquals(193, analysis.maxIndex)
        assertEquals(50, analysis.inRangeCount)       // only ~half land in 0..100
        assertEquals(51, analysis.outOfRangeCount)    // freqIndex 101..193
        assertEquals(40, analysis.distinctInRangeCount)
    }

    @Test
    fun directIndexSelection_reproducesTheTruncatedPointCount() {
        val records = parseLiteVnaFifoRecords(fixtureBytes())
        val direct = selectDirectRecords(records, requestedPointCount = 101)
        // The current parser keeps only the distinct in-range indices → far fewer than
        // the requested 101 (the on-device "43/101" symptom; 40 for this exact capture).
        assertEquals(40, direct.size)
        assertTrue(
            "direct selection must be a strict subset of the requested points",
            direct.size < 101
        )
        // Decode is correct (not the cause): records are strictly ordered 0,1,2,... by
        // the selection, and the first captured record decodes to freqIndex 0.
        assertEquals(0, records.first().freqIndex)
        assertEquals(1, records[1].freqIndex)
        assertEquals(105, records[2].freqIndex) // the +104 jump to a later sweep pass
    }

    // ------------------------------------------------------------------
    // DistinctInRangeAccumulator — the reconstruction seam
    // ------------------------------------------------------------------

    private fun rec(freqIndex: Int) = LiteVnaFifoRecord(0, 0, 0, 0, 0, 0, freqIndex)

    @Test
    fun accumulator_convergesToAllPointsFromStrideSampledInput() {
        // Simulate the device dribbling scattered pairs (a coprime stride so every
        // residue 0..100 eventually appears) plus out-of-range noise (150..199, ignored).
        val acc = DistinctInRangeAccumulator(pointCount = 101)
        var k = 0
        var reads = 0
        while (!acc.isComplete && reads < 2000) {
            acc.addRecords(
                listOf(rec((7 * k) % 101), rec((7 * k + 1) % 101), rec(150 + (k % 50)))
            )
            k += 1
            reads += 1
        }
        assertTrue("must converge within the read cap", acc.isComplete)
        assertEquals(101, acc.distinctInRangeCount)
        assertTrue(acc.missingIndices().isEmpty())
    }

    @Test
    fun accumulator_terminatesHonestlyWhenAnIndexIsStarved() {
        // Every index except 50 is delivered, repeatedly — the accumulator must NOT
        // report complete and must name the starved index.
        val acc = DistinctInRangeAccumulator(pointCount = 101)
        repeat(500) { k ->
            val idx = k % 101
            if (idx != 50) acc.addFreqIndex(idx)
            acc.addFreqIndex(150 + (k % 40)) // out-of-range noise, ignored
        }
        assertFalse(acc.isComplete)
        assertEquals(100, acc.distinctInRangeCount)
        assertEquals(listOf(50), acc.missingIndices())
    }

    @Test
    fun accumulator_singleRealDrainIsInsufficient() {
        // The real captured payload (one drain of the free-running superset sweep) only
        // covers 40 of the 101 target indices — which is exactly why the read loop must
        // keep collecting distinct indices across many reads.
        val acc = DistinctInRangeAccumulator(pointCount = 101)
        acc.addRecords(parseLiteVnaFifoRecords(fixtureBytes()))
        assertEquals(40, acc.distinctInRangeCount)
        assertFalse(acc.isComplete)
        assertEquals(61, acc.missingIndices().size)
    }
}
