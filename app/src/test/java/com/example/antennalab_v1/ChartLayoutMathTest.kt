package com.example.antennalab_v1

import com.example.antennalab_v1.domain.analysis.ChartKind
import com.example.antennalab_v1.domain.analysis.ChartLayoutMath
import com.example.antennalab_v1.model.HardwareMeasurementCapabilities
import com.example.antennalab_v1.model.IaruRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Coverage for [ChartLayoutMath] — the pure layout math behind the Phase-3
 * shared chart components: band overlay positions, the fixed phase axis, grid
 * shape, and capability gating.
 *
 * Fractions are hand-derived from the span arithmetic, not read back from the
 * object. Plain JVM, no Compose.
 */
class ChartLayoutMathTest {

    private val tol = 1e-9

    // ------------------------------------------------------------------
    // Band span fractions
    // ------------------------------------------------------------------

    @Test
    fun bandSpanFractions_fillTheAxisWhenTheBandExactlyMatchesIt() {
        // 20m is 14.000-14.350 in Region 3; an axis of exactly that spans 0..1.
        val spans = ChartLayoutMath.bandSpanFractions(14.000, 14.350, IaruRegion.REGION_3)
        assertEquals(1, spans.size)
        assertEquals("20m", spans.single().band.name)
        assertEquals(0.0, spans.single().startFraction, tol)
        assertEquals(1.0, spans.single().endFraction, tol)
        assertFalse(spans.single().isClippedLow)
        assertFalse(spans.single().isClippedHigh)
    }

    @Test
    fun bandSpanFractions_positionABandInsideAWiderAxis() {
        // Axis 13.9-14.5 is 0.6 MHz wide.
        //   start = (14.000 - 13.9) / 0.6 = 0.1666666...
        //   end   = (14.350 - 13.9) / 0.6 = 0.75
        val span = ChartLayoutMath
            .bandSpanFractions(13.9, 14.5, IaruRegion.REGION_3)
            .single()
        assertEquals(1.0 / 6.0, span.startFraction, 1e-9)
        assertEquals(0.75, span.endFraction, 1e-9)
        assertEquals(0.75 - 1.0 / 6.0, span.widthFraction, 1e-9)
    }

    @Test
    fun bandSpanFractions_clampAndFlagABandOverrunningBothEdges() {
        // Axis 14.1-14.2 sits wholly inside 20m, so the band clips at both ends:
        //   raw start = (14.000 - 14.1) / 0.1 = -1.0
        //   raw end   = (14.350 - 14.1) / 0.1 =  2.5
        val span = ChartLayoutMath
            .bandSpanFractions(14.1, 14.2, IaruRegion.REGION_3)
            .single()
        assertEquals(0.0, span.startFraction, tol)
        assertEquals(1.0, span.endFraction, tol)
        assertTrue(span.isClippedLow)
        assertTrue(span.isClippedHigh)
    }

    @Test
    fun bandSpanFractions_flagOnlyTheClippedEdge() {
        // Axis 14.2-14.5: 20m starts below the view but ends inside it.
        val span = ChartLayoutMath
            .bandSpanFractions(14.2, 14.5, IaruRegion.REGION_3)
            .single()
        assertTrue(span.isClippedLow)
        assertFalse(span.isClippedHigh)
        assertEquals(0.0, span.startFraction, tol)
        // (14.350 - 14.2) / 0.3 = 0.5
        assertEquals(0.5, span.endFraction, 1e-9)
    }

    @Test
    fun bandSpanFractions_returnEmptyForAZeroWidthAxis() {
        // Must not divide by zero.
        assertTrue(ChartLayoutMath.bandSpanFractions(14.2, 14.2).isEmpty())
    }

    @Test
    fun bandSpanFractions_normaliseReversedBounds() {
        val forward = ChartLayoutMath.bandSpanFractions(13.9, 14.5, IaruRegion.REGION_3)
        val reversed = ChartLayoutMath.bandSpanFractions(14.5, 13.9, IaruRegion.REGION_3)
        assertEquals(forward, reversed)
    }

    @Test
    fun bandSpanFractions_returnEmptyForASpanInABandGap() {
        assertTrue(ChartLayoutMath.bandSpanFractions(15.0, 16.0).isEmpty())
    }

    @Test
    fun bandSpanFractions_coverEveryBandAcrossAWideSweepInOrder() {
        val spans = ChartLayoutMath.bandSpanFractions(1.0, 30.0, IaruRegion.REGION_3)
        assertEquals(
            listOf("160m", "80m", "60m", "40m", "30m", "20m", "17m", "15m", "12m", "10m"),
            spans.map { it.band.name }
        )
        // Ascending and inside the plot.
        spans.zipWithNext().forEach { (lower, upper) ->
            assertTrue(upper.startFraction >= lower.startFraction)
        }
        spans.forEach { span ->
            assertTrue(span.startFraction in 0.0..1.0)
            assertTrue(span.endFraction in 0.0..1.0)
        }
    }

    @Test
    fun bandSpanFractions_honourTheRequestedRegion() {
        // 146.5-147.5 MHz is inside 2m in Region 3 but above Region 1's 146 MHz
        // edge, so Region 1 has nothing to draw.
        assertEquals(
            1,
            ChartLayoutMath.bandSpanFractions(146.5, 147.5, IaruRegion.REGION_3).size
        )
        assertTrue(
            ChartLayoutMath.bandSpanFractions(146.5, 147.5, IaruRegion.REGION_1).isEmpty()
        )
    }

    // ------------------------------------------------------------------
    // Phase axis — fixed, never data-scaled
    // ------------------------------------------------------------------

    @Test
    fun phaseAxis_isFixedAtPlusMinus180() {
        val bounds = ChartLayoutMath.phaseAxisBounds()
        assertEquals(-180.0, bounds.start, tol)
        assertEquals(180.0, bounds.endInclusive, tol)
    }

    @Test
    fun phaseFraction_mapsTheAxisEndsAndCentre() {
        assertEquals(0.0, ChartLayoutMath.phaseFraction(-180.0), tol)
        assertEquals(0.5, ChartLayoutMath.phaseFraction(0.0), tol)
        assertEquals(1.0, ChartLayoutMath.phaseFraction(180.0), tol)
        assertEquals(0.75, ChartLayoutMath.phaseFraction(90.0), tol)
        assertEquals(0.25, ChartLayoutMath.phaseFraction(-90.0), tol)
    }

    @Test
    fun phaseFraction_clampsOutOfRangeReadings() {
        // A wrapped or bogus value must not draw outside the plot.
        assertEquals(1.0, ChartLayoutMath.phaseFraction(360.0), tol)
        assertEquals(0.0, ChartLayoutMath.phaseFraction(-360.0), tol)
    }

    // ------------------------------------------------------------------
    // Grid shape
    // ------------------------------------------------------------------

    @Test
    fun gridColumnCount_givesOneChartFullWidthAndPairsTheRest() {
        assertEquals(0, ChartLayoutMath.gridColumnCount(0))
        assertEquals(1, ChartLayoutMath.gridColumnCount(1))
        assertEquals(2, ChartLayoutMath.gridColumnCount(2))
        assertEquals(2, ChartLayoutMath.gridColumnCount(3))
        assertEquals(2, ChartLayoutMath.gridColumnCount(4))
    }

    @Test
    fun gridColumnCount_isZeroForNegativeInput() {
        assertEquals(0, ChartLayoutMath.gridColumnCount(-1))
    }

    @Test
    fun gridRowCount_roundsUpAgainstTheColumnCount() {
        assertEquals(0, ChartLayoutMath.gridRowCount(0))
        assertEquals(1, ChartLayoutMath.gridRowCount(1))
        assertEquals(1, ChartLayoutMath.gridRowCount(2))
        // 3 charts in 2 columns needs 2 rows, not 1.
        assertEquals(2, ChartLayoutMath.gridRowCount(3))
        assertEquals(2, ChartLayoutMath.gridRowCount(4))
        assertEquals(3, ChartLayoutMath.gridRowCount(5))
    }

    // ------------------------------------------------------------------
    // Frequency axis — which kinds the band overlay belongs on
    // ------------------------------------------------------------------

    @Test
    fun hasFrequencyAxis_isTrueForEveryKindExceptSmith() {
        // Stated as one case so the true-set and the false-set are asserted
        // together: adding a ChartKind must fail this test as well as the
        // exhaustive `when`.
        assertTrue(ChartLayoutMath.hasFrequencyAxis(ChartKind.SWR))
        assertTrue(ChartLayoutMath.hasFrequencyAxis(ChartKind.RETURN_LOSS))
        assertTrue(ChartLayoutMath.hasFrequencyAxis(ChartKind.PHASE))
        // Smith plots reflection on the complex plane — frequency is the path
        // along the locus, not a screen axis, so a band strip would be a lie.
        assertFalse(ChartLayoutMath.hasFrequencyAxis(ChartKind.SMITH))
    }

    // ------------------------------------------------------------------
    // Plot insets — the shared alignment contract
    // ------------------------------------------------------------------
    // These are the numbers anything drawn alongside a trace must inset by
    // to line up with it. They record today's three geometries exactly; a
    // change here moves pixels in the viewer, so it should be deliberate.

    @Test
    fun plotInsets_scalarCompactIsGutterPlusCanvasPadding() {
        // Grid cell: 40 dp label gutter + 10 dp canvas padding.
        val insets = ChartLayoutMath.plotInsetsFor(
            ChartLayoutMath.PlotRenderer.SCALAR,
            compact = true
        )
        assertEquals(50, insets.startDp)
        assertEquals(10, insets.endDp)
    }

    @Test
    fun plotInsets_scalarFullWidthUsesTheWiderGutter() {
        // Legacy "Active Display": 56 dp label gutter + 10 dp canvas padding.
        val insets = ChartLayoutMath.plotInsetsFor(
            ChartLayoutMath.PlotRenderer.SCALAR,
            compact = false
        )
        assertEquals(66, insets.startDp)
        assertEquals(10, insets.endDp)
    }

    @Test
    fun plotInsets_phaseUsesTheSameGeometryAsAScalarCell() {
        // 40 dp label gutter + 10 dp canvas padding, matching a scalar cell.
        val insets = ChartLayoutMath.plotInsetsFor(
            ChartLayoutMath.PlotRenderer.PHASE,
            compact = true
        )
        assertEquals(50, insets.startDp)
        assertEquals(10, insets.endDp)
    }

    @Test
    fun plotInsets_phaseFullWidthUsesTheWiderGutter() {
        // Slice 4b: PHASE honours compact, so an expanded phase chart lands on
        // the same 56 dp gutter as an expanded scalar one. Before 4b this
        // returned 50 — the gutter mismatch 4c would otherwise have shipped.
        val insets = ChartLayoutMath.plotInsetsFor(
            ChartLayoutMath.PlotRenderer.PHASE,
            compact = false
        )
        assertEquals(66, insets.startDp)
        assertEquals(10, insets.endDp)
    }

    @Test
    fun plotInsets_phaseAndScalarCompactAreIdentical() {
        // Since slice 4b both renderers resolve through ONE merged `when` arm,
        // so this is true by construction rather than by two constants
        // agreeing. That is the strengthening, not a weakening: what the test
        // guards now is the arm itself. Re-splitting `SCALAR, PHASE ->` to give
        // one renderer a different number fails here first — and SweepChartGrid
        // still assumes this equality in prose, hardcoding SCALAR for every
        // frequency-axis cell's band strip including the phase one.
        assertEquals(
            ChartLayoutMath.plotInsetsFor(ChartLayoutMath.PlotRenderer.SCALAR, compact = true),
            ChartLayoutMath.plotInsetsFor(ChartLayoutMath.PlotRenderer.PHASE, compact = true)
        )
    }

    @Test
    fun plotInsets_phaseAndScalarFullWidthAreIdentical() {
        // The same tripwire in the state slice 4c makes reachable: whichever
        // renderer gets expanded, one band inset serves it.
        assertEquals(
            ChartLayoutMath.plotInsetsFor(ChartLayoutMath.PlotRenderer.SCALAR, compact = false),
            ChartLayoutMath.plotInsetsFor(ChartLayoutMath.PlotRenderer.PHASE, compact = false)
        )
    }

    @Test
    fun plotInsets_areNeverNegative() {
        // A negative inset would push a trace outside its own plot.
        ChartLayoutMath.PlotRenderer.entries.forEach { renderer ->
            listOf(true, false).forEach { compact ->
                val insets = ChartLayoutMath.plotInsetsFor(renderer, compact)
                assertTrue(insets.startDp >= 0)
                assertTrue(insets.endDp >= 0)
            }
        }
    }

    // ------------------------------------------------------------------
    // Frequency-tick thinning
    // ------------------------------------------------------------------
    // Extracted from SweepScalarTraceView in slice 4b so PhaseTraceCell could
    // reuse it rather than restate it. Thinning is geometry: the tick row is
    // SpaceBetween across the plot extent, so the count places every label.

    private val fiveTicks =
        listOf("14.00", "14.05", "14.10", "14.15", "14.20")

    @Test
    fun visibleFrequencyTicks_fullWidthKeepsEveryTick() {
        assertEquals(
            fiveTicks,
            ChartLayoutMath.visibleFrequencyTicks(fiveTicks, compact = false)
        )
    }

    @Test
    fun visibleFrequencyTicks_compactKeepsOnlyTheSpanEndpoints() {
        // ~160 dp of cell fits two labels; three wrap into digit fragments.
        assertEquals(
            listOf("14.00", "14.20"),
            ChartLayoutMath.visibleFrequencyTicks(fiveTicks, compact = true)
        )
    }

    @Test
    fun visibleFrequencyTicks_compactCannotDuplicateALoneTick() {
        // The reason this is not listOfNotNull(first, last), which would render
        // the same label at both ends of the row.
        assertEquals(
            listOf("14.00"),
            ChartLayoutMath.visibleFrequencyTicks(listOf("14.00"), compact = true)
        )
    }

    @Test
    fun visibleFrequencyTicks_emptyStaysEmpty() {
        assertEquals(
            emptyList<String>(),
            ChartLayoutMath.visibleFrequencyTicks(emptyList(), compact = true)
        )
    }

    // ------------------------------------------------------------------
    // Capability gating
    // ------------------------------------------------------------------

    @Test
    fun availableChartKinds_returnsNothingForAnInstrumentThatReportsNothing() {
        assertTrue(
            ChartLayoutMath.availableChartKinds(HardwareMeasurementCapabilities()).isEmpty()
        )
    }

    @Test
    fun availableChartKinds_ordersChartsForOperatorScanning() {
        val kinds = ChartLayoutMath.availableChartKinds(
            HardwareMeasurementCapabilities(
                supportsSWR = true,
                supportsSmithChart = true,
                supportsReturnLoss = true,
                supportsPhaseAnalysis = true,
                supportsS11Phase = true
            )
        )
        assertEquals(
            listOf(ChartKind.SWR, ChartKind.SMITH, ChartKind.RETURN_LOSS, ChartKind.PHASE),
            kinds
        )
    }

    @Test
    fun availableChartKinds_gatesEachChartOnItsOwnCapability() {
        val swrOnly = ChartLayoutMath.availableChartKinds(
            HardwareMeasurementCapabilities(supportsSWR = true)
        )
        assertEquals(listOf(ChartKind.SWR), swrOnly)

        val noSmith = ChartLayoutMath.availableChartKinds(
            HardwareMeasurementCapabilities(
                supportsSWR = true,
                supportsReturnLoss = true
            )
        )
        assertFalse(noSmith.contains(ChartKind.SMITH))
    }

    @Test
    fun availableChartKinds_requiresBothPhaseFlagsForThePhaseChart() {
        // Phase is derived from gamma, but a device reporting no S11 phase at
        // all has nothing meaningful to plot — so both flags are required.
        val analysisOnly = ChartLayoutMath.availableChartKinds(
            HardwareMeasurementCapabilities(supportsPhaseAnalysis = true)
        )
        assertFalse(analysisOnly.contains(ChartKind.PHASE))

        val sourceOnly = ChartLayoutMath.availableChartKinds(
            HardwareMeasurementCapabilities(supportsS11Phase = true)
        )
        assertFalse(sourceOnly.contains(ChartKind.PHASE))

        val both = ChartLayoutMath.availableChartKinds(
            HardwareMeasurementCapabilities(
                supportsPhaseAnalysis = true,
                supportsS11Phase = true
            )
        )
        assertTrue(both.contains(ChartKind.PHASE))
    }
}
