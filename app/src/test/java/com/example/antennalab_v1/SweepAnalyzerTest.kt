package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.SweepAnalyzer
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Coverage for [SweepAnalyzer] — the thin resonance-detection engine
 * (findMinimumSWR, getResonantFrequencyMHz). Physically-determined outputs are
 * hand-derived from each fixture, not read back from the engine. Plain JVM; real
 * model types, no mocking.
 */
class SweepAnalyzerTest {

    private val tol = 1e-9

    private fun point(
        frequencyMHz: Double,
        swr: Double = 1.5,
        returnLossDb: Double = -10.0,
        resistance: Double = 50.0,
        reactance: Double = 0.0
    ) = SweepPoint(
        frequencyMHz = frequencyMHz,
        swr = swr,
        returnLossDb = returnLossDb,
        resistance = resistance,
        reactance = reactance
    )

    private fun sweep(points: List<SweepPoint>) = SweepResult(
        startFrequencyMHz = points.firstOrNull()?.frequencyMHz ?: 0.0,
        endFrequencyMHz = points.lastOrNull()?.frequencyMHz ?: 0.0,
        stepMHz = if (points.size > 1) points[1].frequencyMHz - points[0].frequencyMHz else 0.0,
        points = points
    )

    // ------------------------------------------------------------------
    // findMinimumSWR
    // ------------------------------------------------------------------

    @Test
    fun findMinimumSWR_picksGlobalLowestSwrPoint() {
        // Hand-derived: lowest swr is 1.3 at 14.1 MHz.
        val result = sweep(
            listOf(
                point(14.0, swr = 1.8),
                point(14.1, swr = 1.3),
                point(14.2, swr = 2.5)
            )
        )
        val min = SweepAnalyzer.findMinimumSWR(result)
        assertEquals(14.1, min!!.frequencyMHz, tol)
        assertEquals(1.3, min.swr, tol)
    }

    @Test
    fun findMinimumSWR_emptySweepIsNull() {
        assertNull(SweepAnalyzer.findMinimumSWR(sweep(emptyList())))
    }

    @Test
    fun findMinimumSWR_singlePointIsThatPoint() {
        val result = sweep(listOf(point(12.0, swr = 1.9)))
        assertEquals(12.0, SweepAnalyzer.findMinimumSWR(result)!!.frequencyMHz, tol)
    }

    @Test
    fun findMinimumSWR_tieReturnsFirstEncountered() {
        // minByOrNull returns the first of equal minima.
        val result = sweep(
            listOf(
                point(14.0, swr = 1.3),
                point(14.1, swr = 1.3)
            )
        )
        assertEquals(14.0, SweepAnalyzer.findMinimumSWR(result)!!.frequencyMHz, tol)
    }

    @Test
    fun findMinimumSWR_ignoresFrequencyOrderingAndPicksGlobalMin() {
        // Points not ascending by frequency; global min swr is 1.1 at 14.0 MHz.
        val result = sweep(
            listOf(
                point(14.2, swr = 2.0),
                point(14.0, swr = 1.1),
                point(14.1, swr = 1.5)
            )
        )
        assertEquals(14.0, SweepAnalyzer.findMinimumSWR(result)!!.frequencyMHz, tol)
    }

    // ------------------------------------------------------------------
    // getResonantFrequencyMHz
    // ------------------------------------------------------------------

    @Test
    fun getResonantFrequencyMHz_isMinimumSwrPointFrequency() {
        // Hand-derived: min swr 1.3 sits at 14.1 MHz.
        val result = sweep(
            listOf(
                point(14.0, swr = 1.8),
                point(14.1, swr = 1.3),
                point(14.2, swr = 2.5)
            )
        )
        assertEquals(14.1, SweepAnalyzer.getResonantFrequencyMHz(result)!!, tol)
    }

    @Test
    fun getResonantFrequencyMHz_emptySweepIsNull() {
        assertNull(SweepAnalyzer.getResonantFrequencyMHz(sweep(emptyList())))
    }
}
