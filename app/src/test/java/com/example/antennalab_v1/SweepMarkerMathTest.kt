package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.SeriesEquivalent
import com.example.antennalab_v1.domain.testing.SweepMarkerMath
import com.example.antennalab_v1.model.IaruRegion
import com.example.antennalab_v1.model.testing.SweepPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Coverage for [SweepMarkerMath] — the derived marker values (|Z|, R+jX, Q,
 * series-equivalent Cs/Ls, return loss, phase, band) behind the Phase-3 readout
 * table.
 *
 * Every physical expectation is hand-derived, not read back from the object.
 * The primary fixture is R = 50, X = +50 at 14.2 MHz because it lands on exact
 * values throughout:
 *
 *   |Z|   = sqrt(50^2 + 50^2)                 = 70.710678 ohm
 *   Q     = |X| / R = 50 / 50                 = 1.0
 *   gamma = (Z - 50) / (Z + 50)
 *         = j50 / (100 + j50)                 = 0.2 + j0.4   (exact)
 *   |gamma| = sqrt(0.04 + 0.16)               = 0.4472136
 *   phase = atan2(0.4, 0.2)                   = 63.434949 deg
 *   Ls    = X / (2*pi*f) = 50 / 8.9221e7      = 560.4 nH
 *
 * Plain JVM, real model types, no mocking.
 */
class SweepMarkerMathTest {

    private val tol = 1e-6

    private fun point(
        frequencyMHz: Double = 14.2,
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

    // ------------------------------------------------------------------
    // |Z|
    // ------------------------------------------------------------------

    @Test
    fun impedanceMagnitude_isTheHypotenuseOfRAndX() {
        assertEquals(
            70.710678,
            SweepMarkerMath.impedanceMagnitudeOhms(point(resistance = 50.0, reactance = 50.0)),
            1e-6
        )
    }

    @Test
    fun impedanceMagnitude_ignoresReactanceSign() {
        val inductive = SweepMarkerMath.impedanceMagnitudeOhms(
            point(resistance = 30.0, reactance = 40.0)
        )
        val capacitive = SweepMarkerMath.impedanceMagnitudeOhms(
            point(resistance = 30.0, reactance = -40.0)
        )
        // 3-4-5 triangle both ways.
        assertEquals(50.0, inductive, tol)
        assertEquals(50.0, capacitive, tol)
    }

    // ------------------------------------------------------------------
    // Q
    // ------------------------------------------------------------------

    @Test
    fun qFactor_isReactanceOverResistance() {
        assertEquals(1.0, SweepMarkerMath.qFactor(point(resistance = 50.0, reactance = 50.0))!!, tol)
        assertEquals(4.0, SweepMarkerMath.qFactor(point(resistance = 25.0, reactance = 100.0))!!, tol)
    }

    @Test
    fun qFactor_isZeroForAPurelyResistivePoint() {
        assertEquals(0.0, SweepMarkerMath.qFactor(point(resistance = 50.0, reactance = 0.0))!!, tol)
    }

    @Test
    fun qFactor_ignoresReactanceSign() {
        // A capacitive point has the same Q magnitude as its inductive mirror.
        assertEquals(2.0, SweepMarkerMath.qFactor(point(resistance = 25.0, reactance = -50.0))!!, tol)
    }

    @Test
    fun qFactor_isUnavailableWhenResistanceIsDegenerate() {
        // A short: dividing by zero would report Infinity, which is not a
        // number an operator can act on.
        assertNull(SweepMarkerMath.qFactor(point(resistance = 0.0, reactance = 50.0)))
    }

    // ------------------------------------------------------------------
    // Series equivalent
    // ------------------------------------------------------------------

    @Test
    fun seriesEquivalent_isInductiveForPositiveReactance() {
        // Hand-derived: 2*pi*14.2e6 = 8.922123136e7 rad/s,
        // so L = 50 / 8.922123136e7 = 5.6040473e-7 H.
        val series = SweepMarkerMath.seriesEquivalent(
            point(frequencyMHz = 14.2, reactance = 50.0)
        )
        assertTrue(series is SeriesEquivalent.Inductive)
        assertEquals(5.6040473e-7, (series as SeriesEquivalent.Inductive).henries, 1e-13)
    }

    @Test
    fun seriesEquivalent_isCapacitiveForNegativeReactance() {
        // Hand-derived: C = 1 / (8.922123136e7 * 50) = 2.2416189e-10 F.
        // Cross-check against the inductive case: L / C must equal |X|^2 =
        // 2500 exactly, since L = X/w and C = 1/(w*X).
        val series = SweepMarkerMath.seriesEquivalent(
            point(frequencyMHz = 14.2, reactance = -50.0)
        )
        assertTrue(series is SeriesEquivalent.Capacitive)
        assertEquals(2.2416189e-10, (series as SeriesEquivalent.Capacitive).farads, 1e-17)
    }

    @Test
    fun seriesEquivalent_inductiveOverCapacitive_equalsReactanceSquared() {
        // Precision-independent invariant: at one frequency with equal |X|,
        // L = X/w and C = 1/(w*X), so L / C == X^2 exactly regardless of what
        // w works out to. Catches a swapped or dropped omega that a
        // hand-rounded constant might not.
        val inductive = SweepMarkerMath.seriesEquivalent(
            point(frequencyMHz = 14.2, reactance = 50.0)
        ) as SeriesEquivalent.Inductive
        val capacitive = SweepMarkerMath.seriesEquivalent(
            point(frequencyMHz = 14.2, reactance = -50.0)
        ) as SeriesEquivalent.Capacitive

        assertEquals(2500.0, inductive.henries / capacitive.farads, 1e-6)
    }

    @Test
    fun seriesEquivalent_isUnavailableForAPurelyResistivePoint() {
        assertNull(SweepMarkerMath.seriesEquivalent(point(reactance = 0.0)))
    }

    @Test
    fun seriesEquivalent_isUnavailableAtNonPositiveFrequency() {
        // Guards the 2*pi*f divide rather than returning Infinity.
        assertNull(SweepMarkerMath.seriesEquivalent(point(frequencyMHz = 0.0, reactance = 50.0)))
    }

    @Test
    fun seriesEquivalent_scalesWithFrequency() {
        // Same reactance at half the frequency is twice the inductance.
        val low = SweepMarkerMath.seriesEquivalent(
            point(frequencyMHz = 7.1, reactance = 50.0)
        ) as SeriesEquivalent.Inductive
        val high = SweepMarkerMath.seriesEquivalent(
            point(frequencyMHz = 14.2, reactance = 50.0)
        ) as SeriesEquivalent.Inductive
        assertEquals(2.0, low.henries / high.henries, 1e-9)
    }

    // ------------------------------------------------------------------
    // Reflection coefficient — magnitude and phase
    // ------------------------------------------------------------------

    @Test
    fun reflection_matchesTheHandDerivedGamma() {
        val p = point(resistance = 50.0, reactance = 50.0)
        // gamma = 0.2 + j0.4 exactly.
        assertEquals(0.4472136, SweepMarkerMath.reflectionMagnitude(p), 1e-6)
        assertEquals(63.434949, SweepMarkerMath.reflectionPhaseDegrees(p), 1e-6)
    }

    @Test
    fun reflection_isZeroAtAPerfectMatch() {
        val p = point(resistance = 50.0, reactance = 0.0)
        assertEquals(0.0, SweepMarkerMath.reflectionMagnitude(p), tol)
    }

    @Test
    fun reflectionPhase_isNegativeForACapacitivePoint() {
        // Mirror of the inductive fixture: gamma = 0.2 - j0.4.
        val p = point(resistance = 50.0, reactance = -50.0)
        assertEquals(-63.434949, SweepMarkerMath.reflectionPhaseDegrees(p), 1e-6)
    }

    @Test
    fun reflectionPhase_doesNotDependOnTheStoredS11PhaseField() {
        // s11PhaseDegrees defaults to 0.0 and is only meaningful when the
        // device reports it; the readout must still be correct without it.
        val p = SweepPoint(
            frequencyMHz = 14.2,
            swr = 1.5,
            returnLossDb = -10.0,
            resistance = 50.0,
            reactance = 50.0,
            s11PhaseDegrees = 0.0
        )
        assertEquals(63.434949, SweepMarkerMath.reflectionPhaseDegrees(p), 1e-6)
    }

    // ------------------------------------------------------------------
    // Formatters
    // ------------------------------------------------------------------

    @Test
    fun formatImpedance_foldsTheSignIntoTheJTerm() {
        assertEquals("50.0 + j50.0 Ω", SweepMarkerMath.formatImpedance(50.0, 50.0))
        // Not "50.0 + j-50.0".
        assertEquals("50.0 - j50.0 Ω", SweepMarkerMath.formatImpedance(50.0, -50.0))
        assertEquals("50.0 + j0.0 Ω", SweepMarkerMath.formatImpedance(50.0, 0.0))
    }

    @Test
    fun formatSeriesEquivalent_autoScalesCapacitance() {
        // 224.2 pF stays in pF; 1.77 nF crosses to nF.
        assertEquals(
            "224.2 pF",
            SweepMarkerMath.formatSeriesEquivalent(SeriesEquivalent.Capacitive(2.241659e-10))
        )
        assertEquals(
            "1.77 nF",
            SweepMarkerMath.formatSeriesEquivalent(SeriesEquivalent.Capacitive(1.768e-9))
        )
    }

    @Test
    fun formatSeriesEquivalent_autoScalesInductance() {
        // 560.4 nH stays in nH; an 80m-band equivalent crosses to uH.
        assertEquals(
            "560.4 nH",
            SweepMarkerMath.formatSeriesEquivalent(SeriesEquivalent.Inductive(5.60415e-7))
        )
        assertEquals(
            "4.42 µH",
            SweepMarkerMath.formatSeriesEquivalent(SeriesEquivalent.Inductive(4.421e-6))
        )
    }

    @Test
    fun formatters_printAnEmDashForUnavailableValues() {
        assertEquals("—", SweepMarkerMath.formatQ(null))
        assertEquals("—", SweepMarkerMath.formatSeriesEquivalent(null))
        // A perfect match can produce a non-finite return loss upstream; it
        // must not print as "Infinity".
        assertEquals("—", SweepMarkerMath.formatReturnLossDb(Double.NEGATIVE_INFINITY))
        assertEquals("—", SweepMarkerMath.formatReturnLossDb(Double.NaN))
    }

    @Test
    fun formatReturnLossDb_quotesAPositiveDbFigure() {
        // Stored as negative, read by operators as positive.
        assertEquals("10.00 dB", SweepMarkerMath.formatReturnLossDb(-10.0))
        assertEquals("10.00 dB", SweepMarkerMath.formatReturnLossDb(10.0))
    }

    // ------------------------------------------------------------------
    // buildMarkerReadout — the whole row
    // ------------------------------------------------------------------

    @Test
    fun buildMarkerReadout_fillsEveryFieldFromTheOneFixture() {
        val readout = SweepMarkerMath.buildMarkerReadout(
            point(frequencyMHz = 14.2, swr = 2.618, returnLossDb = -7.0,
                resistance = 50.0, reactance = 50.0),
            region = IaruRegion.REGION_3
        )

        // Raw values.
        assertEquals(14.2, readout.frequencyMHz, tol)
        assertEquals(50.0, readout.resistanceOhms, tol)
        assertEquals(50.0, readout.reactanceOhms, tol)
        assertEquals(70.710678, readout.impedanceMagnitudeOhms, 1e-6)
        assertEquals(1.0, readout.qFactor!!, tol)
        assertEquals(0.4472136, readout.reflectionMagnitude, 1e-6)
        assertEquals(63.434949, readout.phaseDegrees, 1e-6)
        assertTrue(readout.seriesEquivalent is SeriesEquivalent.Inductive)

        // Display strings.
        assertEquals("14.200 MHz", readout.frequencyText)
        assertEquals("2.62", readout.swrText)
        assertEquals("50.0 + j50.0 Ω", readout.impedanceText)
        assertEquals("70.7 Ω", readout.impedanceMagnitudeText)
        assertEquals("1.00", readout.qText)
        assertEquals("560.4 nH", readout.seriesEquivalentText)
        assertEquals("7.00 dB", readout.returnLossText)
        assertEquals("63.4°", readout.phaseText)

        // 14.2 MHz is inside 20m.
        assertEquals("20m", readout.bandLabel)
    }

    @Test
    fun buildMarkerReadout_labelsAnOutOfBandMarker() {
        val readout = SweepMarkerMath.buildMarkerReadout(
            point(frequencyMHz = 16.0),
            region = IaruRegion.REGION_3
        )
        assertEquals("—", readout.bandLabel)
    }

    @Test
    fun buildMarkerReadout_honoursTheRequestedRegion() {
        // 147 MHz: 2m in Region 3, out of band in Region 1.
        assertEquals(
            "2m",
            SweepMarkerMath.buildMarkerReadout(
                point(frequencyMHz = 147.0), IaruRegion.REGION_3
            ).bandLabel
        )
        assertEquals(
            "—",
            SweepMarkerMath.buildMarkerReadout(
                point(frequencyMHz = 147.0), IaruRegion.REGION_1
            ).bandLabel
        )
    }

    @Test
    fun buildMarkerReadout_survivesADegenerateShortPoint() {
        // R = 0, X = 0: Q and the series equivalent are unavailable, but the
        // row still builds — flag, don't reject.
        val readout = SweepMarkerMath.buildMarkerReadout(
            point(resistance = 0.0, reactance = 0.0)
        )
        assertNull(readout.qFactor)
        assertNull(readout.seriesEquivalent)
        assertEquals("—", readout.qText)
        assertEquals("—", readout.seriesEquivalentText)
        assertEquals(0.0, readout.impedanceMagnitudeOhms, tol)
    }

    // ------------------------------------------------------------------
    // buildLabelledMarkerReadouts — A/B pairing
    // ------------------------------------------------------------------
    // MarkerReadoutTable labels POSITIONALLY (markerLabels.getOrElse(index)),
    // so a bare listOfNotNull(a, b) renders a lone marker B as "Marker A".
    // These pin the pairing. The B-only case is the one that would regress
    // silently: it still shows a row, just under the wrong name.

    @Test
    fun buildLabelledMarkerReadouts_labelsMarkerAWhenOnlyAIsPlaced() {
        val rows = SweepMarkerMath.buildLabelledMarkerReadouts(
            markerAPoint = point(frequencyMHz = 14.2),
            markerBPoint = null
        )
        assertEquals(listOf("Marker A"), rows.map { it.label })
        assertEquals(14.2, rows.single().readout.frequencyMHz, tol)
    }

    @Test
    fun buildLabelledMarkerReadouts_labelsMarkerBWhenOnlyBIsPlaced() {
        // THE TRAP: one row, and it must NOT be called "Marker A".
        val rows = SweepMarkerMath.buildLabelledMarkerReadouts(
            markerAPoint = null,
            markerBPoint = point(frequencyMHz = 14.3)
        )
        assertEquals(listOf("Marker B"), rows.map { it.label })
        assertEquals(14.3, rows.single().readout.frequencyMHz, tol)
    }

    @Test
    fun buildLabelledMarkerReadouts_keepsAThenBOrderAndPairsEachWithItsOwnValues() {
        val rows = SweepMarkerMath.buildLabelledMarkerReadouts(
            markerAPoint = point(frequencyMHz = 14.2, resistance = 50.0, reactance = 50.0),
            markerBPoint = point(frequencyMHz = 14.3, resistance = 25.0, reactance = -25.0)
        )
        assertEquals(listOf("Marker A", "Marker B"), rows.map { it.label })

        // Values travel with their own label — a re-order cannot swap them.
        val a = rows[0].readout
        val b = rows[1].readout
        assertEquals(14.2, a.frequencyMHz, tol)
        assertEquals(50.0, a.reactanceOhms, tol)
        assertEquals(14.3, b.frequencyMHz, tol)
        assertEquals(-25.0, b.reactanceOhms, tol)
    }

    @Test
    fun buildLabelledMarkerReadouts_isEmptyWhenNeitherMarkerIsPlaced() {
        assertTrue(
            SweepMarkerMath.buildLabelledMarkerReadouts(null, null).isEmpty()
        )
    }

    @Test
    fun buildLabelledMarkerReadouts_honoursTheRequestedRegion() {
        val rows = SweepMarkerMath.buildLabelledMarkerReadouts(
            markerAPoint = point(frequencyMHz = 147.0),
            markerBPoint = point(frequencyMHz = 147.0),
            region = IaruRegion.REGION_1
        )
        assertEquals(listOf("—", "—"), rows.map { it.readout.bandLabel })
    }
}
