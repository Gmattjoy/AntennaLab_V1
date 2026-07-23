package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.FeedlineLossSuspicion
import com.example.antennalab_v1.domain.testing.ImpedanceStability
import com.example.antennalab_v1.domain.testing.LikelyCondition
import com.example.antennalab_v1.domain.testing.MatchingQuality
import com.example.antennalab_v1.domain.testing.MismatchSeverity
import com.example.antennalab_v1.domain.testing.ReactanceTrend
import com.example.antennalab_v1.domain.testing.SweepDiagnosticsEngine
import com.example.antennalab_v1.domain.testing.SweepShape
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Coverage for [SweepDiagnosticsEngine.analyzeSweep] — the summary-metrics engine.
 *
 * Assertion discipline: the physically/mathematically determined outputs
 * (minimum SWR + its frequency, primary resonance = min-|reactance| point, and the
 * SWR≤2.0 / ≤1.5 bandwidth spans, including the exactly-2.0 boundary) are
 * HAND-DERIVED from each fixture and asserted as literals — never mirrored from the
 * engine. The threshold classifiers (matchingQuality, mismatchSeverity,
 * impedanceStability, sweepShape, reactanceTrend, likelyCondition,
 * feedlineLossSuspicion) are characterization tests that pin current behaviour.
 *
 * Plain JVM; real model types, no mocking.
 */
class SweepDiagnosticsEngineTest {

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

    /**
     * Canonical single-dip bowl (step 1.0 MHz, freq 10..14). Hand-derived:
     *  - min SWR 1.1 at 12.0 MHz
     *  - min |reactance| = 2 at 12.0 MHz  → primary resonance 12.0 MHz
     *  - SWR≤2.0 in-band: 11,12,13 → span 2.0 MHz
     *  - SWR≤1.5 in-band: only 12 → span 0.0 MHz
     */
    private fun canonicalDip() = sweep(
        listOf(
            point(10.0, swr = 3.0, resistance = 30.0, reactance = -40.0),
            point(11.0, swr = 1.6, resistance = 45.0, reactance = -15.0),
            point(12.0, swr = 1.1, resistance = 52.0, reactance = 2.0),
            point(13.0, swr = 1.6, resistance = 58.0, reactance = 18.0),
            point(14.0, swr = 3.0, resistance = 70.0, reactance = 45.0)
        )
    )

    // ==================================================================
    // Physically-determined outputs (hand-derived)
    // ==================================================================

    @Test
    fun canonicalDip_minSwrResonanceAndBandwidthsAreHandDerived() {
        val d = SweepDiagnosticsEngine.analyzeSweep(canonicalDip())
        assertTrue(d.hasUsableData)
        assertNull(d.error)

        assertEquals(1.1, d.minimumSwr, tol)
        assertEquals(12.0, d.minimumSwrFrequencyMHz, tol)
        // primary resonance = min |reactance| point (also 12.0 here)
        assertEquals(12.0, d.resonanceFrequencyMHz, tol)
        // SWR≤2.0 span: 13.0 − 11.0
        assertEquals(2.0, d.estimatedBandwidthMHz, tol)
        // SWR≤1.5 span: single in-band point → 0.0
        assertEquals(0.0, d.estimatedBandwidthAt15MHz, tol)
    }

    @Test
    fun resonanceFrequencyDivergesFromMinSwr_andBandwidthBoundaryIsInclusive() {
        // Hand-derived:
        //  - min SWR 1.2 at 10.0 MHz
        //  - min |reactance| = 1 at 11.0 MHz → resonance 11.0 (DIFFERENT freq)
        //  - SWR≤2.0 in-band: 10.0 (1.2) and 11.0 (exactly 2.0, inclusive) → span 1.0
        //    (were 2.0 exclusive, 11.0 would drop and the span would be 0.0)
        //  - SWR≤1.5 in-band: only 10.0 → span 0.0
        val d = SweepDiagnosticsEngine.analyzeSweep(
            sweep(
                listOf(
                    point(10.0, swr = 1.2, reactance = 30.0),
                    point(11.0, swr = 2.0, reactance = 1.0),
                    point(12.0, swr = 2.5, reactance = 40.0)
                )
            )
        )
        assertEquals(1.2, d.minimumSwr, tol)
        assertEquals(10.0, d.minimumSwrFrequencyMHz, tol)
        assertEquals(11.0, d.resonanceFrequencyMHz, tol)
        assertEquals(1.0, d.estimatedBandwidthMHz, tol)
        assertEquals(0.0, d.estimatedBandwidthAt15MHz, tol)
    }

    @Test
    fun bandwidthBoundary_swrExactlyAtThresholdIsInclusive() {
        // Endpoints sit exactly on the thresholds:
        //  - SWR≤2.0 in-band: 20.0 (1.5) and 21.0 (exactly 2.0) → span 1.0
        //  - SWR≤1.5 in-band: only 20.0 (exactly 1.5) → span 0.0
        val d = SweepDiagnosticsEngine.analyzeSweep(
            sweep(
                listOf(
                    point(20.0, swr = 1.5),
                    point(21.0, swr = 2.0),
                    point(22.0, swr = 2.5)
                )
            )
        )
        assertEquals(1.0, d.estimatedBandwidthMHz, tol)
        assertEquals(0.0, d.estimatedBandwidthAt15MHz, tol)
    }

    @Test
    fun bandwidth_noInBandPointsIsZero() {
        val d = SweepDiagnosticsEngine.analyzeSweep(
            sweep(
                listOf(
                    point(10.0, swr = 3.0),
                    point(11.0, swr = 3.5),
                    point(12.0, swr = 4.0)
                )
            )
        )
        assertEquals(0.0, d.estimatedBandwidthMHz, tol)
        assertEquals(0.0, d.estimatedBandwidthAt15MHz, tol)
    }

    // ==================================================================
    // Empty / degenerate input
    // ==================================================================

    @Test
    fun emptySweep_returnsStructuredErrorAndUnusableDefaults() {
        val d = SweepDiagnosticsEngine.analyzeSweep(sweep(emptyList()))
        assertFalse(d.hasUsableData)
        assertNotNull(d.error)
        assertEquals("SWEEP_DIAGNOSTICS_NO_POINTS", d.error!!.code)
        assertEquals(0.0, d.minimumSwr, tol)
        assertEquals("No sweep diagnostics available.", d.summary)
        assertEquals(MatchingQuality.UNKNOWN, d.matchingQuality)
        assertEquals(SweepShape.UNKNOWN, d.sweepShape)
    }

    @Test
    fun singlePoint_physicalOutputsHandDerived_andSizeGatedFieldsUnknown() {
        // One point at 12.0, swr 1.3, reactance 3.
        //  - min SWR 1.3 at 12.0; resonance 12.0; both bandwidths 0.0 (single in-band)
        val d = SweepDiagnosticsEngine.analyzeSweep(
            sweep(listOf(point(12.0, swr = 1.3, reactance = 3.0)))
        )
        assertTrue(d.hasUsableData)
        assertEquals(1.3, d.minimumSwr, tol)
        assertEquals(12.0, d.minimumSwrFrequencyMHz, tol)
        assertEquals(12.0, d.resonanceFrequencyMHz, tol)
        assertEquals(0.0, d.estimatedBandwidthMHz, tol)
        assertEquals(0.0, d.estimatedBandwidthAt15MHz, tol)
        assertNull(d.secondaryResonanceFrequencyMHz)
        // size-gated classifiers
        assertEquals(ImpedanceStability.UNKNOWN, d.impedanceStability)
        assertEquals(SweepShape.UNKNOWN, d.sweepShape)
        assertEquals(0, d.resonanceCountEstimate)
    }

    // ==================================================================
    // Secondary resonance (hand-derived selection)
    // ==================================================================

    @Test
    fun secondaryResonance_picksFarNearNullReactancePoint() {
        // step 1.0, freq 10..16. Primary resonance (min |reactance|) = 12.0 (0.5).
        // Separation threshold = 3×step = 3.0 → far points need |f−12| ≥ 3.0 (f≤9 or f≥15).
        // Far candidates: 15.0 (|8|) and 16.0 (|22|); min |reactance| = 15.0, and 8 ≤ 25.
        val d = SweepDiagnosticsEngine.analyzeSweep(
            sweep(
                listOf(
                    point(10.0, swr = 2.0, reactance = 20.0),
                    point(11.0, swr = 1.8, reactance = 10.0),
                    point(12.0, swr = 1.1, reactance = 0.5),
                    point(13.0, swr = 1.9, reactance = 12.0),
                    point(14.0, swr = 2.5, reactance = 30.0),
                    point(15.0, swr = 1.7, reactance = 8.0),
                    point(16.0, swr = 2.6, reactance = 22.0)
                )
            )
        )
        assertEquals(15.0, d.secondaryResonanceFrequencyMHz!!, tol)
    }

    @Test
    fun secondaryResonance_nullWhenFarCandidatesReactanceTooHigh() {
        // Same layout but every far point has |reactance| > 25 → takeIf drops it.
        val d = SweepDiagnosticsEngine.analyzeSweep(
            sweep(
                listOf(
                    point(10.0, swr = 2.0, reactance = 20.0),
                    point(11.0, swr = 1.8, reactance = 10.0),
                    point(12.0, swr = 1.1, reactance = 0.5),
                    point(13.0, swr = 1.9, reactance = 12.0),
                    point(14.0, swr = 2.5, reactance = 30.0),
                    point(15.0, swr = 1.7, reactance = 40.0),
                    point(16.0, swr = 2.6, reactance = 50.0)
                )
            )
        )
        assertNull(d.secondaryResonanceFrequencyMHz)
    }

    @Test
    fun secondaryResonance_nullWhenFewerThanFivePoints() {
        // canonicalDip has 5 points but no point far enough from the primary →
        // also null; use a 4-point sweep to hit the explicit size guard.
        val d = SweepDiagnosticsEngine.analyzeSweep(
            sweep(
                listOf(
                    point(10.0, swr = 2.0, reactance = 20.0),
                    point(11.0, swr = 1.2, reactance = 1.0),
                    point(12.0, swr = 1.9, reactance = 12.0),
                    point(13.0, swr = 2.5, reactance = 25.0)
                )
            )
        )
        assertNull(d.secondaryResonanceFrequencyMHz)
    }

    // ==================================================================
    // Threshold classifiers (characterization)
    // ==================================================================

    private fun swrSweep(minSwr: Double) = sweep(
        listOf(
            point(10.0, swr = minSwr),
            point(11.0, swr = minSwr + 0.5),
            point(12.0, swr = minSwr + 1.0)
        )
    )

    @Test
    fun matchingQuality_acrossThresholds() {
        assertEquals(MatchingQuality.EXCELLENT, SweepDiagnosticsEngine.analyzeSweep(swrSweep(1.1)).matchingQuality)
        assertEquals(MatchingQuality.GOOD, SweepDiagnosticsEngine.analyzeSweep(swrSweep(1.35)).matchingQuality)
        assertEquals(MatchingQuality.FAIR, SweepDiagnosticsEngine.analyzeSweep(swrSweep(1.8)).matchingQuality)
        assertEquals(MatchingQuality.POOR, SweepDiagnosticsEngine.analyzeSweep(swrSweep(2.5)).matchingQuality)
    }

    @Test
    fun mismatchSeverity_acrossThresholds() {
        assertEquals(MismatchSeverity.LOW, SweepDiagnosticsEngine.analyzeSweep(swrSweep(1.1)).mismatchSeverity)
        assertEquals(MismatchSeverity.MODERATE, SweepDiagnosticsEngine.analyzeSweep(swrSweep(1.8)).mismatchSeverity)
        assertEquals(MismatchSeverity.HIGH, SweepDiagnosticsEngine.analyzeSweep(swrSweep(2.5)).mismatchSeverity)
        assertEquals(MismatchSeverity.EXTREME, SweepDiagnosticsEngine.analyzeSweep(swrSweep(3.5)).mismatchSeverity)
    }

    @Test
    fun impedanceStability_acrossSpreadBands() {
        val stable = sweep(
            listOf(
                point(10.0, resistance = 48.0, reactance = -5.0),
                point(11.0, resistance = 52.0, reactance = 5.0)
            )
        )
        assertEquals(ImpedanceStability.STABLE, SweepDiagnosticsEngine.analyzeSweep(stable).impedanceStability)

        val moderate = sweep(
            listOf(
                point(10.0, resistance = 40.0, reactance = -20.0),
                point(11.0, resistance = 90.0, reactance = 20.0)
            )
        )
        assertEquals(ImpedanceStability.MODERATE, SweepDiagnosticsEngine.analyzeSweep(moderate).impedanceStability)

        val unstable = sweep(
            listOf(
                point(10.0, resistance = 10.0, reactance = -40.0),
                point(11.0, resistance = 120.0, reactance = 40.0)
            )
        )
        assertEquals(ImpedanceStability.UNSTABLE, SweepDiagnosticsEngine.analyzeSweep(unstable).impedanceStability)
    }

    @Test
    fun sweepShape_flatResponse() {
        val d = SweepDiagnosticsEngine.analyzeSweep(
            sweep(
                listOf(
                    point(10.0, swr = 1.5),
                    point(11.0, swr = 1.6),
                    point(12.0, swr = 1.7)
                )
            )
        )
        assertEquals(SweepShape.FLAT_RESPONSE, d.sweepShape)
    }

    @Test
    fun sweepShape_weakCoupling() {
        val d = SweepDiagnosticsEngine.analyzeSweep(
            sweep(
                listOf(
                    point(10.0, swr = 3.2),
                    point(11.0, swr = 3.6),
                    point(12.0, swr = 4.0)
                )
            )
        )
        assertEquals(SweepShape.WEAK_COUPLING, d.sweepShape)
    }

    @Test
    fun sweepShape_multipleDips_andResonanceCount() {
        // Two interior local dips (swr ≤ 3.0, |reactance| ≤ 20).
        val d = SweepDiagnosticsEngine.analyzeSweep(
            sweep(
                listOf(
                    point(10.0, swr = 2.8),
                    point(11.0, swr = 1.2),
                    point(12.0, swr = 2.8),
                    point(13.0, swr = 2.9),
                    point(14.0, swr = 1.3),
                    point(15.0, swr = 2.8),
                    point(16.0, swr = 3.0)
                )
            )
        )
        assertEquals(SweepShape.MULTIPLE_DIPS, d.sweepShape)
        assertEquals(2, d.resonanceCountEstimate)
    }

    @Test
    fun sweepShape_sharpSingleDip() {
        val d = SweepDiagnosticsEngine.analyzeSweep(canonicalDip())
        // single dip; SWR≤2.0 bandwidth 2.0 ≤ 4×step(4.0) → SHARP
        assertEquals(SweepShape.SHARP_SINGLE_DIP, d.sweepShape)
        assertEquals(1, d.resonanceCountEstimate)
    }

    @Test
    fun sweepShape_broadSingleDip() {
        // Strict V bowl, step 1.0, freq 1..9. SWR≤2.0 in-band 2.0..8.0 → span 6.0 > 4×step.
        val d = SweepDiagnosticsEngine.analyzeSweep(
            sweep(
                listOf(
                    point(1.0, swr = 2.5, reactance = -8.0),
                    point(2.0, swr = 2.0, reactance = -6.0),
                    point(3.0, swr = 1.7, reactance = -4.0),
                    point(4.0, swr = 1.4, reactance = -2.0),
                    point(5.0, swr = 1.1, reactance = 0.0),
                    point(6.0, swr = 1.4, reactance = 2.0),
                    point(7.0, swr = 1.7, reactance = 4.0),
                    point(8.0, swr = 2.0, reactance = 6.0),
                    point(9.0, swr = 2.5, reactance = 8.0)
                )
            )
        )
        assertEquals(6.0, d.estimatedBandwidthMHz, tol) // hand-derived span
        assertEquals(SweepShape.BROAD_SINGLE_DIP, d.sweepShape)
    }

    @Test
    fun sweepShape_unknownWhenTooFewPoints() {
        val d = SweepDiagnosticsEngine.analyzeSweep(
            sweep(listOf(point(10.0, swr = 1.5), point(11.0, swr = 3.0)))
        )
        assertEquals(SweepShape.UNKNOWN, d.sweepShape)
    }

    @Test
    fun reactanceTrend_variants() {
        val crosses = sweep(
            listOf(
                point(10.0, reactance = -10.0),
                point(11.0, reactance = 0.0),
                point(12.0, reactance = 10.0)
            )
        )
        assertEquals(ReactanceTrend.CROSSES_RESONANCE, SweepDiagnosticsEngine.analyzeSweep(crosses).reactanceTrend)

        val inductive = sweep(
            listOf(
                point(10.0, reactance = 10.0),
                point(11.0, reactance = 20.0),
                point(12.0, reactance = 30.0)
            )
        )
        assertEquals(ReactanceTrend.MOSTLY_INDUCTIVE, SweepDiagnosticsEngine.analyzeSweep(inductive).reactanceTrend)

        val capacitive = sweep(
            listOf(
                point(10.0, reactance = -10.0),
                point(11.0, reactance = -20.0),
                point(12.0, reactance = -30.0)
            )
        )
        assertEquals(ReactanceTrend.MOSTLY_CAPACITIVE, SweepDiagnosticsEngine.analyzeSweep(capacitive).reactanceTrend)

        // pos + neg, both |x| > 5, no near-zero → MIXED (not CROSSES)
        val mixed = sweep(
            listOf(
                point(10.0, reactance = 10.0),
                point(11.0, reactance = -10.0)
            )
        )
        assertEquals(ReactanceTrend.MIXED, SweepDiagnosticsEngine.analyzeSweep(mixed).reactanceTrend)

        // all within ±5, none negative → neither inductive nor capacitive, not crossing → UNKNOWN
        val unknown = sweep(
            listOf(
                point(10.0, reactance = 0.0),
                point(11.0, reactance = 2.0),
                point(12.0, reactance = 4.0)
            )
        )
        assertEquals(ReactanceTrend.UNKNOWN, SweepDiagnosticsEngine.analyzeSweep(unknown).reactanceTrend)
    }

    // ==================================================================
    // Likely condition
    // ==================================================================

    @Test
    fun likelyCondition_closeToTarget() {
        assertEquals(LikelyCondition.CLOSE_TO_TARGET, SweepDiagnosticsEngine.analyzeSweep(canonicalDip()).likelyCondition)
    }

    @Test
    fun likelyCondition_complexMultiResonance() {
        val d = SweepDiagnosticsEngine.analyzeSweep(
            sweep(
                listOf(
                    point(10.0, swr = 2.8),
                    point(11.0, swr = 1.2),
                    point(12.0, swr = 2.8),
                    point(13.0, swr = 2.9),
                    point(14.0, swr = 1.3),
                    point(15.0, swr = 2.8),
                    point(16.0, swr = 3.0)
                )
            )
        )
        assertEquals(LikelyCondition.COMPLEX_MULTI_RESONANCE, d.likelyCondition)
    }

    @Test
    fun likelyCondition_highLossOrWeakRadiation() {
        val d = SweepDiagnosticsEngine.analyzeSweep(
            sweep(
                listOf(
                    point(10.0, swr = 3.2),
                    point(11.0, swr = 3.6),
                    point(12.0, swr = 4.0)
                )
            )
        )
        assertEquals(LikelyCondition.HIGH_LOSS_OR_WEAK_RADIATION, d.likelyCondition)
    }

    @Test
    fun likelyCondition_likelyTooLong() {
        // Resonance below midpoint + MOSTLY_CAPACITIVE, min swr > 1.5 (not close-to-target).
        val d = SweepDiagnosticsEngine.analyzeSweep(
            sweep(
                listOf(
                    point(10.0, swr = 1.8, reactance = -6.0),
                    point(11.0, swr = 2.0, reactance = -8.0),
                    point(12.0, swr = 2.2, reactance = -15.0),
                    point(13.0, swr = 2.5, reactance = -25.0),
                    point(14.0, swr = 2.8, reactance = -35.0)
                )
            )
        )
        assertEquals(LikelyCondition.LIKELY_TOO_LONG, d.likelyCondition)
    }

    @Test
    fun likelyCondition_likelyTooShort() {
        // Resonance above midpoint + MOSTLY_INDUCTIVE, min swr > 1.5.
        val d = SweepDiagnosticsEngine.analyzeSweep(
            sweep(
                listOf(
                    point(10.0, swr = 2.8, reactance = 35.0),
                    point(11.0, swr = 2.5, reactance = 25.0),
                    point(12.0, swr = 2.2, reactance = 15.0),
                    point(13.0, swr = 2.0, reactance = 8.0),
                    point(14.0, swr = 1.8, reactance = 6.0)
                )
            )
        )
        assertEquals(LikelyCondition.LIKELY_TOO_SHORT, d.likelyCondition)
    }

    // ==================================================================
    // Feedline loss suspicion
    // ==================================================================

    @Test
    fun feedlineLoss_high() {
        // min swr > 2.5 and swr spread < 0.5
        val d = SweepDiagnosticsEngine.analyzeSweep(
            sweep(
                listOf(
                    point(10.0, swr = 2.6),
                    point(11.0, swr = 2.7),
                    point(12.0, swr = 2.8)
                )
            )
        )
        assertEquals(FeedlineLossSuspicion.HIGH, d.feedlineLossSuspicion)
    }

    @Test
    fun feedlineLoss_low() {
        // canonicalDip min swr 1.1 ≤ 1.8
        assertEquals(FeedlineLossSuspicion.LOW, SweepDiagnosticsEngine.analyzeSweep(canonicalDip()).feedlineLossSuspicion)
    }

    @Test
    fun feedlineLoss_unknownWhenBetweenBandsAndNotFlat() {
        // min swr 2.3 (> 1.8, ≤ 2.5) with a wide swr spread: HIGH needs spread < 0.5;
        // the MODERATE branch requires SWR≤2.0 bandwidth ≥ 0.6×span, but no point is
        // ≤ 2.0 here so that bandwidth is 0 → falls through to UNKNOWN.
        val d = SweepDiagnosticsEngine.analyzeSweep(
            sweep(
                listOf(
                    point(10.0, swr = 2.3),
                    point(11.0, swr = 3.0),
                    point(12.0, swr = 3.6)
                )
            )
        )
        assertEquals(FeedlineLossSuspicion.UNKNOWN, d.feedlineLossSuspicion)
    }

    // ==================================================================
    // Summary string
    // ==================================================================

    @Test
    fun summary_containsHandDerivedNumericLines() {
        val summary = SweepDiagnosticsEngine.analyzeSweep(canonicalDip()).summary
        assertTrue(summary.contains("Minimum SWR 1.100 at 12.000 MHz."))
        assertTrue(summary.contains("Primary resonance estimate near 12.000 MHz."))
        assertTrue(summary.contains("Estimated SWR≤2.0 bandwidth 2.000 MHz."))
        assertTrue(summary.contains("Estimated SWR≤1.5 bandwidth 0.000 MHz."))
        // no secondary resonance in this fixture
        assertFalse(summary.contains("Secondary resonance"))
        assertTrue(summary.contains("Matching quality: EXCELLENT."))
        assertTrue(summary.contains("Mismatch severity: LOW."))
        assertTrue(summary.contains("Likely condition: CLOSE_TO_TARGET."))
    }

    @Test
    fun summary_includesSecondaryResonanceLineWhenPresent() {
        val summary = SweepDiagnosticsEngine.analyzeSweep(
            sweep(
                listOf(
                    point(10.0, swr = 2.0, reactance = 20.0),
                    point(11.0, swr = 1.8, reactance = 10.0),
                    point(12.0, swr = 1.1, reactance = 0.5),
                    point(13.0, swr = 1.9, reactance = 12.0),
                    point(14.0, swr = 2.5, reactance = 30.0),
                    point(15.0, swr = 1.7, reactance = 8.0),
                    point(16.0, swr = 2.6, reactance = 22.0)
                )
            )
        ).summary
        assertTrue(summary.contains("Secondary resonance estimate near 15.000 MHz."))
    }
}
