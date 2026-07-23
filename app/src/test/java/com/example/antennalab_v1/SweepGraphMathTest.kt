package com.example.antennalab_v1

import com.example.antennalab_v1.features.testing.SweepDisplayMode
import com.example.antennalab_v1.features.testing.TraceCompareMode
import com.example.antennalab_v1.features.testing.buildAxisLabels
import com.example.antennalab_v1.features.testing.buildCableFaultPreview
import com.example.antennalab_v1.features.testing.buildDifferenceValues
import com.example.antennalab_v1.features.testing.buildFrequencyTicks
import com.example.antennalab_v1.features.testing.buildTraceAxisBounds
import com.example.antennalab_v1.features.testing.estimateBandwidthAtOrBelowSwr
import com.example.antennalab_v1.features.testing.estimateS21Db
import com.example.antennalab_v1.features.testing.formatAxisLabel
import com.example.antennalab_v1.features.testing.getDisplayValue
import com.example.antennalab_v1.features.testing.getTraceAxisTitle
import com.example.antennalab_v1.features.testing.getTraceModeSummary
import com.example.antennalab_v1.features.testing.roundUpForInstrumentScale
import com.example.antennalab_v1.model.HardwareMeasurementCapabilities
import com.example.antennalab_v1.model.TestHardwareProfile
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure coverage for [SweepGraphMath] — the sweep graph math moved out of
 * SweepGraphWidgets (§17-18) into the shared helper file, now the single source
 * for per-point display value, axis bounds/labels/ticks, and the summary
 * (bandwidth / cable-fault) derivations. Plain JVM; real model types, no mocking.
 */
class SweepGraphMathTest {

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

    private fun sweep(points: List<SweepPoint>, startMHz: Double, endMHz: Double) = SweepResult(
        startFrequencyMHz = startMHz,
        endFrequencyMHz = endMHz,
        stepMHz = if (points.size > 1) (endMHz - startMHz) / (points.size - 1) else 0.0,
        points = points
    )

    // ------------------------------------------------------------------
    // Display value + S21
    // ------------------------------------------------------------------

    @Test
    fun getDisplayValue_selectsFieldPerMode() {
        val p = point(14.0, swr = 1.8, returnLossDb = -20.0, resistance = 42.0, reactance = -13.0)
        assertEquals(1.8, getDisplayValue(p, SweepDisplayMode.SWR), tol)
        assertEquals(1.8, getDisplayValue(p, SweepDisplayMode.ANALOG_SWR), tol)
        assertEquals(1.8, getDisplayValue(p, SweepDisplayMode.WATERFALL), tol)
        assertEquals(-20.0, getDisplayValue(p, SweepDisplayMode.RETURN_LOSS), tol)
        assertEquals(42.0, getDisplayValue(p, SweepDisplayMode.RESISTANCE), tol)
        assertEquals(-13.0, getDisplayValue(p, SweepDisplayMode.REACTANCE), tol)
        assertEquals(1.8, getDisplayValue(p, SweepDisplayMode.SMITH), tol)
        assertEquals(1.8, getDisplayValue(p, SweepDisplayMode.IMPEDANCE_LOCUS), tol)
        assertEquals(estimateS21Db(p), getDisplayValue(p, SweepDisplayMode.S21_ESTIMATE), tol)
    }

    @Test
    fun estimateS21Db_isNegativeAbsOfScaledReturnLoss() {
        assertEquals(-7.0, estimateS21Db(point(14.0, returnLossDb = -20.0)), tol)
        assertEquals(-3.5, estimateS21Db(point(14.0, returnLossDb = 10.0)), tol)
    }

    // ------------------------------------------------------------------
    // Bandwidth
    // ------------------------------------------------------------------

    @Test
    fun estimateBandwidthAtOrBelowSwr_spansInBandFreqsOrNull() {
        val result = sweep(
            listOf(
                point(10.0, swr = 3.0),
                point(11.0, swr = 1.5),
                point(12.0, swr = 1.2),
                point(13.0, swr = 1.4),
                point(14.0, swr = 3.0)
            ),
            startMHz = 10.0, endMHz = 14.0
        )
        assertEquals(2.0, estimateBandwidthAtOrBelowSwr(result, 2.0)!!, tol) // 13 - 11
        assertNull(estimateBandwidthAtOrBelowSwr(result, 1.0))
    }

    // ------------------------------------------------------------------
    // Scale rounding + label formatting
    // ------------------------------------------------------------------

    @Test
    fun roundUpForInstrumentScale_walksTheLadder() {
        assertEquals(1.0, roundUpForInstrumentScale(0.5), tol)
        assertEquals(2.0, roundUpForInstrumentScale(1.5), tol)
        assertEquals(5.0, roundUpForInstrumentScale(3.0), tol)
        assertEquals(10.0, roundUpForInstrumentScale(7.0), tol)
        assertEquals(20.0, roundUpForInstrumentScale(15.0), tol)
        assertEquals(50.0, roundUpForInstrumentScale(30.0), tol)
        assertEquals(100.0, roundUpForInstrumentScale(70.0), tol)
        assertEquals(200.0, roundUpForInstrumentScale(150.0), tol)
        assertEquals(300.0, roundUpForInstrumentScale(250.0), tol) // ((250/100)+1)*100
    }

    @Test
    fun formatAxisLabel_precisionByMagnitude() {
        assertEquals("5.00", formatAxisLabel(5.0))
        assertEquals("15.0", formatAxisLabel(15.0))
        assertEquals("150", formatAxisLabel(150.0))
    }

    // ------------------------------------------------------------------
    // Difference series
    // ------------------------------------------------------------------

    @Test
    fun buildDifferenceValues_handlesEmptyAndMismatchedSizes() {
        assertEquals(emptyList<Double>(), buildDifferenceValues(emptyList(), listOf(1.0)))
        assertEquals(listOf(1.0, 2.0, 3.0), buildDifferenceValues(listOf(1.0, 2.0, 3.0), emptyList()))
        assertEquals(listOf(0.0, 1.0, 2.0), buildDifferenceValues(listOf(1.0, 2.0, 3.0), listOf(1.0, 1.0, 1.0)))
        assertEquals(listOf(0.0, 1.0), buildDifferenceValues(listOf(1.0, 2.0, 3.0), listOf(1.0, 1.0)))
    }

    // ------------------------------------------------------------------
    // Axis bounds
    // ------------------------------------------------------------------

    @Test
    fun buildTraceAxisBounds_emptyIsUnitRange() {
        val b = buildTraceAxisBounds(SweepDisplayMode.SWR, TraceCompareMode.CURRENT_ONLY, emptyList(), emptyList(), emptyList())
        assertEquals(0.0, b.minimum, tol)
        assertEquals(1.0, b.maximum, tol)
        assertEquals(1.0, b.range, tol)
    }

    @Test
    fun buildTraceAxisBounds_perModeShapes() {
        val swr = buildTraceAxisBounds(
            SweepDisplayMode.SWR, TraceCompareMode.CURRENT_ONLY, listOf(1.2, 1.8), emptyList(), emptyList()
        )
        assertEquals(1.0, swr.minimum, tol)
        assertEquals(2.0, swr.maximum, tol)

        val reactance = buildTraceAxisBounds(
            SweepDisplayMode.REACTANCE, TraceCompareMode.CURRENT_ONLY, listOf(-30.0, 5.0), emptyList(), emptyList()
        )
        assertEquals(-50.0, reactance.minimum, tol) // symmetric around roundUp(30)=50
        assertEquals(50.0, reactance.maximum, tol)

        val returnLoss = buildTraceAxisBounds(
            SweepDisplayMode.RETURN_LOSS, TraceCompareMode.CURRENT_ONLY, listOf(10.0, 30.0), emptyList(), emptyList()
        )
        assertEquals(0.0, returnLoss.minimum, tol) // min(0, 10)
        assertEquals(50.0, returnLoss.maximum, tol) // roundUp(max(5, 30))

        val difference = buildTraceAxisBounds(
            SweepDisplayMode.SWR, TraceCompareMode.DIFFERENCE, listOf(1.0), emptyList(), listOf(-3.0, 4.0)
        )
        assertEquals(-5.0, difference.minimum, tol) // symmetric around roundUp(4)=5
        assertEquals(5.0, difference.maximum, tol)
    }

    // ------------------------------------------------------------------
    // Labels, ticks, titles
    // ------------------------------------------------------------------

    @Test
    fun buildAxisLabels_descendsMaxToMin() {
        assertEquals(listOf("10.0"), buildAxisLabels(10.0, 0.0, 1))
        assertEquals(listOf("10.0", "5.00", "0.00"), buildAxisLabels(10.0, 0.0, 3))
    }

    @Test
    fun buildFrequencyTicks_fiveQuartileTicks() {
        assertEquals(
            listOf("10.00", "11.00", "12.00", "13.00", "14.00"),
            buildFrequencyTicks(10.0, 14.0)
        )
    }

    @Test
    fun traceTitleAndSummary_mapModes() {
        assertEquals("SWR", getTraceAxisTitle(SweepDisplayMode.SWR, TraceCompareMode.CURRENT_ONLY))
        assertEquals("Δ SWR", getTraceAxisTitle(SweepDisplayMode.SWR, TraceCompareMode.DIFFERENCE))
        assertEquals("Return Loss (dB)", getTraceAxisTitle(SweepDisplayMode.RETURN_LOSS, TraceCompareMode.CURRENT_ONLY))

        assertEquals("Current", getTraceModeSummary(TraceCompareMode.CURRENT_ONLY))
        assertEquals("Overlay", getTraceModeSummary(TraceCompareMode.CURRENT_PLUS_REFERENCE))
        assertEquals("Difference", getTraceModeSummary(TraceCompareMode.DIFFERENCE))
    }

    // ------------------------------------------------------------------
    // Cable-fault preview
    // ------------------------------------------------------------------

    private val tdrCaps = HardwareMeasurementCapabilities(supportsTDR = true)
    private val noTdrCaps = HardwareMeasurementCapabilities(supportsTDR = false)

    private fun threePointSweep() = sweep(
        listOf(
            point(14.0, reactance = 0.0),
            point(14.5, reactance = -50.0),
            point(15.0, reactance = 10.0)
        ),
        startMHz = 14.0, endMHz = 15.0
    )

    @Test
    fun buildCableFaultPreview_guardsUnsupportedAndTooFewPoints() {
        assertEquals(
            "TDR preview not supported by this hardware.",
            buildCableFaultPreview(threePointSweep(), noTdrCaps, TestHardwareProfile.NANOVNA_H4)
        )
        val twoPoints = sweep(listOf(point(14.0), point(15.0)), startMHz = 14.0, endMHz = 15.0)
        assertEquals(
            "Not enough sweep points for preview estimate.",
            buildCableFaultPreview(twoPoints, tdrCaps, TestHardwareProfile.NANOVNA_H4)
        )
    }

    @Test
    fun buildCableFaultPreview_distanceScalesWithVelocityFactor() {
        // span = 1 MHz -> distance = (3e8 * vf) / (2 * 1e6)
        val nano = buildCableFaultPreview(threePointSweep(), tdrCaps, TestHardwareProfile.NANOVNA_H4)
        assertTrue(nano.startsWith("Preview only."))
        assertTrue(nano.contains("99.00 m")) // vf 0.66

        val lite = buildCableFaultPreview(threePointSweep(), tdrCaps, TestHardwareProfile.LITEVNA64_V0_3_3)
        assertTrue(lite.contains("123.00 m")) // vf 0.82
    }
}
