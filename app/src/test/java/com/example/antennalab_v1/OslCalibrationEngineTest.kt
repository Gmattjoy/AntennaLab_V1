package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.Complex
import com.example.antennalab_v1.domain.testing.DebugOslCalibrationSimulator
import com.example.antennalab_v1.domain.testing.OslCalibrationEngine
import com.example.antennalab_v1.model.testing.CalibrationStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Synthetic-error verification of the OSL math (no hardware). The debug
 * simulator generates Open/Short/Load through a KNOWN error network; the engine
 * must recover those exact error terms, and the correction formula must recover
 * a known antenna response measured through the same network.
 */
class OslCalibrationEngineTest {

    private val tolerance = 1e-9

    @Test
    fun computeCoefficients_recoversInjectedErrorTerms() {
        val startMHz = 1.0
        val endMHz = 30.0
        val points = 51

        val open = DebugOslCalibrationSimulator.simulateStandardCaptureSweep(
            CalibrationStep.OPEN, startMHz, endMHz, points
        )
        val short = DebugOslCalibrationSimulator.simulateStandardCaptureSweep(
            CalibrationStep.SHORT, startMHz, endMHz, points
        )
        val load = DebugOslCalibrationSimulator.simulateStandardCaptureSweep(
            CalibrationStep.LOAD, startMHz, endMHz, points
        )

        val coeffs = OslCalibrationEngine.computeCoefficients(open, short, load)

        assertTrue("coefficients should be usable", coeffs.isUsable)
        assertEquals(points, coeffs.pointCount)

        // Every frequency point was generated with the same injected terms.
        for (i in 0 until coeffs.pointCount) {
            assertEquals(
                DebugOslCalibrationSimulator.injectedDirectivity.re,
                coeffs.directivityRe[i], tolerance
            )
            assertEquals(
                DebugOslCalibrationSimulator.injectedDirectivity.im,
                coeffs.directivityIm[i], tolerance
            )
            assertEquals(
                DebugOslCalibrationSimulator.injectedSourceMatch.re,
                coeffs.sourceMatchRe[i], tolerance
            )
            assertEquals(
                DebugOslCalibrationSimulator.injectedSourceMatch.im,
                coeffs.sourceMatchIm[i], tolerance
            )
            assertEquals(
                DebugOslCalibrationSimulator.injectedReflectionTracking.re,
                coeffs.reflectionTrackingRe[i], tolerance
            )
            assertEquals(
                DebugOslCalibrationSimulator.injectedReflectionTracking.im,
                coeffs.reflectionTrackingIm[i], tolerance
            )
        }
    }

    @Test
    fun errorModel_correctionRecoversKnownAntennaGamma() {
        // A known "true" antenna reflection (Za = 35 - j10 -> Gamma).
        val trueGamma = OslCalibrationEngine.gammaFromImpedance(35.0, -10.0)

        // Measure it through the injected error network.
        val measured = DebugOslCalibrationSimulator.applyErrorNetwork(trueGamma)

        // Correct with the standard 3-term inverse:
        // Ga = (Gm - e00) / (e10e01 + e11*(Gm - e00))
        val e00 = DebugOslCalibrationSimulator.injectedDirectivity
        val e11 = DebugOslCalibrationSimulator.injectedSourceMatch
        val e10e01 = DebugOslCalibrationSimulator.injectedReflectionTracking
        val d = measured - e00
        val corrected = d / (e10e01 + (e11 * d))

        assertEquals(trueGamma.re, corrected.re, tolerance)
        assertEquals(trueGamma.im, corrected.im, tolerance)
    }

    @Test
    fun gammaImpedanceRoundTrip_isLossless() {
        val gamma = Complex(0.3, -0.45)
        val z = OslCalibrationEngine.impedanceFromGamma(gamma)
        val back = OslCalibrationEngine.gammaFromImpedance(z.re, z.im)

        assertEquals(gamma.re, back.re, tolerance)
        assertEquals(gamma.im, back.im, tolerance)
    }
}
