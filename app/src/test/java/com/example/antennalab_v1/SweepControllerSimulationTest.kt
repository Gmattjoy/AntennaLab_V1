package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.SweepController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure coverage for the simulated antenna's response shape.
 *
 * The point of every assertion here is that the synthetic resonance follows the
 * CENTRE IT IS GIVEN. Before slice 5c each helper hardcoded 14.2 while the sweep
 * window followed the project's target frequency, so the dip only landed inside
 * the window for 20 m work — at 146 MHz the whole trace came out flat near
 * SWR 660 with no resonance at all. Every case below therefore uses an off-20 m
 * centre, so a regression back to a pinned 14.2 fails rather than passes.
 *
 * No Robolectric: these are arithmetic helpers with no Android surface.
 */
class SweepControllerSimulationTest {

    private companion object {
        /** 2 m — deliberately nowhere near the old hardcoded 14.2. */
        const val CENTER_MHZ = 146.0
        const val HALF_WIDTH_MHZ = 0.25
    }

    /** The window resolveSweepWindow would produce for a 146.0 target. */
    private fun windowFrequencies(): List<Double> =
        generateSequence(CENTER_MHZ - HALF_WIDTH_MHZ) { it + 0.01 }
            .takeWhile { it <= CENTER_MHZ + HALF_WIDTH_MHZ + 1e-9 }
            .toList()

    @Test
    fun simulateSWR_minimumLandsAtTheGivenCentre() {
        val best = windowFrequencies().minBy { SweepController.simulateSWR(it, CENTER_MHZ) }

        assertEquals(CENTER_MHZ, best, 1e-6)
        // And it is a real match at the dip, not merely the least-bad point.
        assertEquals(1.05, SweepController.simulateSWR(CENTER_MHZ, CENTER_MHZ), 1e-9)
    }

    @Test
    fun simulateSWR_isNoLongerPinnedToTwentyMetres() {
        // The regression guard stated directly: at a 146 MHz centre the trace
        // must be usable, not the ~660 flat line a 14.2-pinned dip produced.
        val worst = windowFrequencies().maxOf { SweepController.simulateSWR(it, CENTER_MHZ) }

        assertTrue("worst-case SWR in window was $worst", worst < 5.0)
    }

    @Test
    fun simulateReactance_crossesZeroAtTheGivenCentre() {
        // Reactance through zero IS the definition of resonance, so this is the
        // load-bearing one: the electrical resonance and the dip must coincide.
        assertEquals(0.0, SweepController.simulateReactance(CENTER_MHZ, CENTER_MHZ), 1e-9)

        // Capacitive below, inductive above — sign flips across the centre.
        assertTrue(SweepController.simulateReactance(CENTER_MHZ - 0.1, CENTER_MHZ) < 0.0)
        assertTrue(SweepController.simulateReactance(CENTER_MHZ + 0.1, CENTER_MHZ) > 0.0)
    }

    @Test
    fun simulateResistance_isMinimumAtTheGivenCentre() {
        val best = windowFrequencies().minBy { SweepController.simulateResistance(it, CENTER_MHZ) }

        assertEquals(CENTER_MHZ, best, 1e-6)
        // 50 Ω at resonance — a matched antenna, which is what makes the
        // simulated sweep useful as a reference trace.
        assertEquals(50.0, SweepController.simulateResistance(CENTER_MHZ, CENTER_MHZ), 1e-9)
    }

    @Test
    fun simulation_tracksAnyCentre_notOneSpecialCase() {
        // Three decades apart, to pin that the centre is genuinely a parameter
        // and not a second hardcoded value that happens to suit 2 m.
        listOf(3.7, 14.2, 146.0, 435.0).forEach { center ->
            assertEquals(1.05, SweepController.simulateSWR(center, center), 1e-9)
            assertEquals(0.0, SweepController.simulateReactance(center, center), 1e-9)
            assertEquals(50.0, SweepController.simulateResistance(center, center), 1e-9)
        }
    }
}
