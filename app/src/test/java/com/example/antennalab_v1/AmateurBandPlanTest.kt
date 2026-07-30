package com.example.antennalab_v1

import com.example.antennalab_v1.domain.analysis.AmateurBandPlan
import com.example.antennalab_v1.model.IaruRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Coverage for [AmateurBandPlan] — the amateur-band reference tables and the
 * lookups the Phase-3 axis overlay and marker readout consume.
 *
 * The region-difference cases are the point of this file: they are the reason a
 * region enum exists at all, so each one pins a frequency that is in-band in one
 * region and out-of-band in another. Plain JVM, real model types, no mocking.
 */
class AmateurBandPlanTest {

    // ------------------------------------------------------------------
    // Default region
    // ------------------------------------------------------------------

    @Test
    fun defaultRegion_isRegion3() {
        // Melbourne / Asia-Pacific. Flipping this constant is the documented
        // way to reskin the overlay, so it is worth locking.
        assertEquals(IaruRegion.REGION_3, AmateurBandPlan.DEFAULT_REGION)
    }

    @Test
    fun lookupsWithoutRegion_useTheDefaultRegion() {
        // 147 MHz is 2m in Region 3 but above the Region 1 allocation.
        assertEquals("2m", AmateurBandPlan.bandLabelAt(147.0))
        assertEquals(
            AmateurBandPlan.bandsFor(IaruRegion.REGION_3),
            AmateurBandPlan.bandsFor()
        )
    }

    // ------------------------------------------------------------------
    // bandAt — plain hits and misses
    // ------------------------------------------------------------------

    @Test
    fun bandAt_findsTheContainingBand() {
        val band = AmateurBandPlan.bandAt(14.2, IaruRegion.REGION_3)
        assertNotNull(band)
        assertEquals("20m", band!!.name)
        assertEquals(14.000, band.startMHz, 1e-9)
        assertEquals(14.350, band.endMHz, 1e-9)
    }

    @Test
    fun bandAt_returnsNullBetweenAllocations() {
        // 16 MHz sits in the gap between 20m (ends 14.350) and 17m
        // (starts 18.068) in every region.
        IaruRegion.values().forEach { region ->
            assertNull(AmateurBandPlan.bandAt(16.0, region))
        }
    }

    @Test
    fun bandAt_bothEdgesAreInclusive() {
        // An operator tuned exactly to a band edge is in the band.
        assertEquals("20m", AmateurBandPlan.bandAt(14.000, IaruRegion.REGION_3)?.name)
        assertEquals("20m", AmateurBandPlan.bandAt(14.350, IaruRegion.REGION_3)?.name)
        assertNull(AmateurBandPlan.bandAt(13.999, IaruRegion.REGION_3))
        assertNull(AmateurBandPlan.bandAt(14.351, IaruRegion.REGION_3))
    }

    @Test
    fun bandAt_handlesFrequenciesBelowAndAboveEveryAllocation() {
        assertNull(AmateurBandPlan.bandAt(0.5, IaruRegion.REGION_3))
        assertNull(AmateurBandPlan.bandAt(9_999.0, IaruRegion.REGION_3))
    }

    // ------------------------------------------------------------------
    // Region differences — why the enum exists
    // ------------------------------------------------------------------

    @Test
    fun twoMetres_endsAt146InRegion1But148InRegions2And3() {
        // Hand-derived from the tables: R1 144.000-146.000,
        // R2/R3 144.000-148.000.
        assertNull(AmateurBandPlan.bandAt(147.0, IaruRegion.REGION_1))
        assertEquals("2m", AmateurBandPlan.bandAt(147.0, IaruRegion.REGION_2)?.name)
        assertEquals("2m", AmateurBandPlan.bandAt(147.0, IaruRegion.REGION_3)?.name)

        // 145 MHz is 2m everywhere.
        IaruRegion.values().forEach { region ->
            assertEquals("2m", AmateurBandPlan.bandAt(145.0, region)?.name)
        }
    }

    @Test
    fun fortyMetres_endsAt7point2InRegion1But7point3InRegions2And3() {
        assertNull(AmateurBandPlan.bandAt(7.250, IaruRegion.REGION_1))
        assertEquals("40m", AmateurBandPlan.bandAt(7.250, IaruRegion.REGION_2)?.name)
        assertEquals("40m", AmateurBandPlan.bandAt(7.250, IaruRegion.REGION_3)?.name)
    }

    @Test
    fun eightyMetres_hasThreeDifferentUpperEdges() {
        // R1 3.800, R3 3.900, R2 4.000 — all three regions differ here, so
        // 3.850 and 3.950 separate them.
        assertNull(AmateurBandPlan.bandAt(3.850, IaruRegion.REGION_1))
        assertEquals("80m", AmateurBandPlan.bandAt(3.850, IaruRegion.REGION_3)?.name)
        assertEquals("80m", AmateurBandPlan.bandAt(3.850, IaruRegion.REGION_2)?.name)

        assertNull(AmateurBandPlan.bandAt(3.950, IaruRegion.REGION_3))
        assertEquals("80m", AmateurBandPlan.bandAt(3.950, IaruRegion.REGION_2)?.name)
    }

    @Test
    fun sixtyMetres_isTheSameGlobalWrc15AllocationEverywhere() {
        IaruRegion.values().forEach { region ->
            assertEquals("60m", AmateurBandPlan.bandAt(5.360, region)?.name)
            assertNull(AmateurBandPlan.bandAt(5.400, region))
        }
    }

    // ------------------------------------------------------------------
    // bandLabelAt
    // ------------------------------------------------------------------

    @Test
    fun bandLabelAt_printsTheNameOrAnEmDash() {
        assertEquals("20m", AmateurBandPlan.bandLabelAt(14.2, IaruRegion.REGION_3))
        assertEquals("—", AmateurBandPlan.bandLabelAt(16.0, IaruRegion.REGION_3))
    }

    // ------------------------------------------------------------------
    // bandsOverlapping — the axis overlay
    // ------------------------------------------------------------------

    @Test
    fun bandsOverlapping_returnsTheContainingBandForANarrowSpan() {
        // The usual case: a 1 MHz sweep sitting wholly inside 20m.
        val bands = AmateurBandPlan.bandsOverlapping(14.0, 14.35, IaruRegion.REGION_3)
        assertEquals(listOf("20m"), bands.map { it.name })
    }

    @Test
    fun bandsOverlapping_includesABandThatMerelyClipsTheEdge() {
        // Span 14.300-15.000 runs off the top of 20m (ends 14.350) but still
        // overlaps it, and reaches no further band.
        val bands = AmateurBandPlan.bandsOverlapping(14.300, 15.000, IaruRegion.REGION_3)
        assertEquals(listOf("20m"), bands.map { it.name })
    }

    @Test
    fun bandsOverlapping_coversEveryHfBandForAFullHfSweep() {
        // Hand-derived from the Region 3 table: 1-30 MHz touches 160, 80, 60,
        // 40, 30, 20, 17, 15, 12 and 10m — ten bands, ascending.
        val bands = AmateurBandPlan.bandsOverlapping(1.0, 30.0, IaruRegion.REGION_3)
        assertEquals(
            listOf("160m", "80m", "60m", "40m", "30m", "20m", "17m", "15m", "12m", "10m"),
            bands.map { it.name }
        )
    }

    @Test
    fun bandsOverlapping_isIndifferentToArgumentOrder() {
        val forward = AmateurBandPlan.bandsOverlapping(14.0, 21.5, IaruRegion.REGION_3)
        val reversed = AmateurBandPlan.bandsOverlapping(21.5, 14.0, IaruRegion.REGION_3)
        assertEquals(forward, reversed)
    }

    @Test
    fun bandsOverlapping_returnsEmptyForASpanInAGap() {
        val bands = AmateurBandPlan.bandsOverlapping(15.0, 16.0, IaruRegion.REGION_3)
        assertTrue(bands.isEmpty())
    }

    @Test
    fun bandsOverlapping_treatsAZeroWidthSpanAsAPointLookup() {
        val bands = AmateurBandPlan.bandsOverlapping(14.2, 14.2, IaruRegion.REGION_3)
        assertEquals(listOf("20m"), bands.map { it.name })
    }

    // ------------------------------------------------------------------
    // Table invariants — hold for every region
    // ------------------------------------------------------------------

    @Test
    fun everyRegionTable_isAscendingAndNonOverlapping() {
        IaruRegion.values().forEach { region ->
            val bands = AmateurBandPlan.bandsFor(region)
            assertTrue("$region table is empty", bands.isNotEmpty())

            bands.forEach { band ->
                assertTrue(
                    "$region ${band.name} has a non-positive width",
                    band.endMHz > band.startMHz
                )
            }

            bands.zipWithNext().forEach { (lower, upper) ->
                assertTrue(
                    "$region ${lower.name} and ${upper.name} are out of order",
                    upper.startMHz > lower.startMHz
                )
                assertTrue(
                    "$region ${lower.name} overlaps ${upper.name}",
                    lower.endMHz < upper.startMHz
                )
            }
        }
    }

    @Test
    fun everyRegionTable_hasUniqueBandNames() {
        IaruRegion.values().forEach { region ->
            val names = AmateurBandPlan.bandsFor(region).map { it.name }
            assertEquals("$region has duplicate band names", names.size, names.toSet().size)
        }
    }
}
