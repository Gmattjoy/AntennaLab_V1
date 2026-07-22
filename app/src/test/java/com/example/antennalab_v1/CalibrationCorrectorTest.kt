package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.CalibrationCorrector
import com.example.antennalab_v1.domain.testing.DebugOslCalibrationSimulator
import com.example.antennalab_v1.domain.testing.OslCalibrationEngine
import com.example.antennalab_v1.model.testing.CalibrationStep
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end round-trip for OSL correction with no hardware:
 * calibrate through a known error network, measure a known antenna through the
 * SAME network, correct, and confirm the original antenna response is recovered.
 */
class CalibrationCorrectorTest {

    private val tolerance = 1e-6

    private fun cleanPoint(frequencyMHz: Double, resistance: Double, reactance: Double): SweepPoint =
        OslCalibrationEngine.sweepPointFromGamma(
            frequencyMHz,
            OslCalibrationEngine.gammaFromImpedance(resistance, reactance)
        )

    @Test
    fun correctionRecoversKnownAntennaSweep() {
        // Calibration captured over 1..30 MHz through the injected error network.
        val open = DebugOslCalibrationSimulator.simulateStandardCaptureSweep(
            CalibrationStep.OPEN, 1.0, 30.0, 51
        )
        val short = DebugOslCalibrationSimulator.simulateStandardCaptureSweep(
            CalibrationStep.SHORT, 1.0, 30.0, 51
        )
        val load = DebugOslCalibrationSimulator.simulateStandardCaptureSweep(
            CalibrationStep.LOAD, 1.0, 30.0, 51
        )
        val coefficients = OslCalibrationEngine.computeCoefficients(open, short, load)

        // A known antenna response (varies with frequency, all within cal range).
        val cleanPoints = listOf(
            cleanPoint(3.5, 40.0, -15.0),
            cleanPoint(7.1, 55.0, 8.0),
            cleanPoint(14.2, 50.0, 0.0),
            cleanPoint(21.0, 32.0, 18.0),
            cleanPoint(28.0, 68.0, -22.0)
        )
        val clean = SweepResult(
            startFrequencyMHz = 3.5,
            endFrequencyMHz = 28.0,
            stepMHz = 6.125,
            points = cleanPoints
        )

        // Measure the antenna through the same error network (raw, uncorrected).
        val measuredPoints = cleanPoints.map { point ->
            val trueGamma = OslCalibrationEngine.gammaFromPoint(point)
            val measuredGamma = DebugOslCalibrationSimulator.applyErrorNetwork(trueGamma)
            OslCalibrationEngine.sweepPointFromGamma(point.frequencyMHz, measuredGamma)
        }
        val measured = clean.copy(points = measuredPoints)

        // Raw measured data is distorted (not equal to the clean truth).
        assertFalse(measured.isCalibrated)
        assertTrue(
            "measured should differ from clean before correction",
            kotlin.math.abs(measured.points[0].resistance - cleanPoints[0].resistance) > 1e-3
        )

        val corrected = CalibrationCorrector.apply(measured, coefficients)

        assertTrue(corrected.isCalibrated)
        assertTrue(corrected.calibrationLabel.isNotBlank())
        for (i in cleanPoints.indices) {
            assertEquals(cleanPoints[i].resistance, corrected.points[i].resistance, tolerance)
            assertEquals(cleanPoints[i].reactance, corrected.points[i].reactance, tolerance)
            assertEquals(cleanPoints[i].swr, corrected.points[i].swr, tolerance)
            assertEquals(cleanPoints[i].returnLossDb, corrected.points[i].returnLossDb, tolerance)
        }
    }

    @Test
    fun unusableCoefficients_returnRawUnchanged() {
        val raw = SweepResult(
            startFrequencyMHz = 1.0,
            endFrequencyMHz = 2.0,
            stepMHz = 1.0,
            points = listOf(cleanPoint(1.0, 50.0, 0.0), cleanPoint(2.0, 50.0, 0.0))
        )

        val corrected = CalibrationCorrector.apply(
            raw,
            com.example.antennalab_v1.model.testing.OslCalibrationCoefficients()
        )

        assertFalse(corrected.isCalibrated)
        assertEquals(raw.points.size, corrected.points.size)
    }
}
