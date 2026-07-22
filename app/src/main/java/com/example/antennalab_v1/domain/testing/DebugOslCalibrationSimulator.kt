package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.testing.CalibrationStep
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult

/*
########################################################################
FILE: DebugOslCalibrationSimulator.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Calibration (DEBUG TEST TOOLING)

SYSTEM ROLE
Debug-only synthesis of Open/Short/Load standard sweeps AS SEEN THROUGH
a fixed, known error network. Lets the calibration wizard and the OSL
math be exercised with no VNA connected.

Because every standard here is generated with the SAME injected error
terms, OslCalibrationEngine.computeCoefficients() on these three sweeps
must recover those injected terms, and (later phases) correcting an
antenna sweep generated through the same network must recover the true
antenna response. This is the synthetic-error test path from the plan.

Gated at the call site by BuildConfig.DEBUG — never invoked in release.
Pure math; no Android/framework refs.
########################################################################
*/
object DebugOslCalibrationSimulator {

    /**
     * Fixed, non-trivial known error network (frequency-independent for a
     * clean test). These are the terms recovery is checked against.
     */
    val injectedDirectivity = Complex(0.05, -0.03)      // e00
    val injectedSourceMatch = Complex(0.10, 0.08)       // e11
    val injectedReflectionTracking = Complex(0.85, 0.05) // e10e01

    /** Ideal actual reflection of each standard. */
    fun idealStandardGamma(step: CalibrationStep): Complex =
        when (step) {
            CalibrationStep.OPEN -> Complex.ONE
            CalibrationStep.SHORT -> Complex(-1.0, 0.0)
            CalibrationStep.LOAD -> Complex.ZERO
        }

    /** Applies the known error network: Gm = e00 + (e10e01 * Ga) / (1 - e11 * Ga). */
    fun applyErrorNetwork(actualGamma: Complex): Complex {
        val numerator = injectedReflectionTracking * actualGamma
        val denominator = Complex.ONE - (injectedSourceMatch * actualGamma)
        return injectedDirectivity + (numerator / denominator)
    }

    /**
     * Synthesizes the measured sweep of one OSL standard across the range,
     * converting the error-networked reflection coefficient to a full
     * [SweepPoint] at each frequency. The measured Gamma is constant across
     * the band (ideal standards + frequency-independent error), which is
     * exactly what a clean bench capture of a standard looks like.
     */
    fun simulateStandardCaptureSweep(
        step: CalibrationStep,
        startMHz: Double,
        endMHz: Double,
        pointCount: Int
    ): SweepResult {
        val safePointCount = pointCount.coerceAtLeast(2)
        val measuredGamma = applyErrorNetwork(idealStandardGamma(step))
        val stepMHz =
            if (safePointCount > 1) (endMHz - startMHz) / (safePointCount - 1) else 0.0

        val points = ArrayList<SweepPoint>(safePointCount)
        for (index in 0 until safePointCount) {
            val frequencyMHz = startMHz + index * stepMHz
            points.add(
                OslCalibrationEngine.sweepPointFromGamma(
                    frequencyMHz = frequencyMHz,
                    gamma = measuredGamma
                )
            )
        }

        return SweepResult(
            startFrequencyMHz = startMHz,
            endFrequencyMHz = endMHz,
            stepMHz = stepMHz,
            points = points,
            sweepPointCount = points.size,
            requestedPointCount = safePointCount,
            actualPointCount = points.size,
            isComplete = true,
            hardwareProfile = "SIMULATED_CAL_${step.name}"
        )
    }
}
