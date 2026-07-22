package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.testing.OslCalibrationCoefficients
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import kotlin.math.log10

/*
########################################################################
FILE: OslCalibrationEngine.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Calibration

SYSTEM ROLE
Computes a one-port Open/Short/Load (OSL) 3-term error model from three
captured standard sweeps, and provides the shared Gamma <-> impedance
conversions used by capture, correction, and the debug simulator.

ERROR MODEL (ideal standards)
Measured Gm relates to actual Ga by:
    Gm = e00 + (e10e01 * Ga) / (1 - e11 * Ga)
With ideal Open (Ga=+1), Short (Ga=-1), Load (Ga=0) the per-frequency
solution is:
    e00    = M_load                       (directivity)
    D_o    = M_open  - e00
    D_s    = M_short - e00
    e11    = (D_o + D_s) / (D_o - D_s)     (source match)
    e10e01 = D_o * (1 - e11)               (reflection tracking)

Pure math; no Android/framework refs.
########################################################################
*/
object OslCalibrationEngine {

    const val REFERENCE_IMPEDANCE_OHMS = 50.0

    /** Reflection coefficient from impedance: G = (Z - Z0) / (Z + Z0). */
    fun gammaFromImpedance(resistance: Double, reactance: Double): Complex {
        val z = Complex(resistance, reactance)
        val z0 = Complex.ofReal(REFERENCE_IMPEDANCE_OHMS)
        return (z - z0) / (z + z0)
    }

    fun gammaFromPoint(point: SweepPoint): Complex =
        gammaFromImpedance(point.resistance, point.reactance)

    /** Impedance from reflection coefficient: Z = Z0 * (1 + G) / (1 - G). */
    fun impedanceFromGamma(gamma: Complex): Complex {
        val z0 = Complex.ofReal(REFERENCE_IMPEDANCE_OHMS)
        return z0 * ((Complex.ONE + gamma) / (Complex.ONE - gamma))
    }

    /**
     * Builds a full [SweepPoint] from a (corrected or synthetic) reflection
     * coefficient, deriving every scalar the UI/analysis consumes. Shared by
     * the debug simulator and (later) the corrector so both agree exactly.
     */
    fun sweepPointFromGamma(frequencyMHz: Double, gamma: Complex): SweepPoint {
        val impedance = impedanceFromGamma(gamma)
        val magnitude = gamma.magnitude.coerceIn(0.0, MAX_USABLE_GAMMA_MAGNITUDE)
        val returnLossDb = if (magnitude > 0.0) -20.0 * log10(magnitude) else 120.0
        val swr = ((1.0 + magnitude) / (1.0 - magnitude)).coerceAtLeast(1.0)

        return SweepPoint(
            frequencyMHz = frequencyMHz,
            swr = swr,
            returnLossDb = returnLossDb,
            resistance = impedance.re.coerceAtLeast(0.0),
            reactance = impedance.im,
            s11MagnitudeDb = if (magnitude > 0.0) 20.0 * log10(magnitude) else -120.0,
            s11PhaseDegrees = gamma.phaseDegrees
        )
    }

    /**
     * Computes per-frequency error terms from three captured standard sweeps.
     * The three sweeps are assumed to share a frequency grid; alignment is by
     * index up to the shortest length. Frequencies are taken from the OPEN
     * sweep. Returns an empty (unusable) coefficient set if any sweep is empty.
     */
    fun computeCoefficients(
        open: SweepResult,
        short: SweepResult,
        load: SweepResult
    ): OslCalibrationCoefficients {
        val count = minOf(open.points.size, short.points.size, load.points.size)
        if (count == 0) return OslCalibrationCoefficients()

        val frequencyHz = ArrayList<Long>(count)
        val directivityRe = ArrayList<Double>(count)
        val directivityIm = ArrayList<Double>(count)
        val sourceMatchRe = ArrayList<Double>(count)
        val sourceMatchIm = ArrayList<Double>(count)
        val reflectionTrackingRe = ArrayList<Double>(count)
        val reflectionTrackingIm = ArrayList<Double>(count)

        for (i in 0 until count) {
            val measuredOpen = gammaFromPoint(open.points[i])
            val measuredShort = gammaFromPoint(short.points[i])
            val measuredLoad = gammaFromPoint(load.points[i])

            // e00 = M_load (directivity)
            val e00 = measuredLoad
            val dOpen = measuredOpen - e00
            val dShort = measuredShort - e00

            // e11 = (D_o + D_s) / (D_o - D_s); guard the degenerate denominator
            val denom = dOpen - dShort
            val e11 =
                if (denom.magnitude < DEGENERATE_EPSILON) Complex.ZERO
                else (dOpen + dShort) / denom

            // e10e01 = D_o * (1 - e11)
            val e10e01 = dOpen * (Complex.ONE - e11)

            frequencyHz.add((open.points[i].frequencyMHz * 1_000_000.0).toLong())
            directivityRe.add(e00.re)
            directivityIm.add(e00.im)
            sourceMatchRe.add(e11.re)
            sourceMatchIm.add(e11.im)
            reflectionTrackingRe.add(e10e01.re)
            reflectionTrackingIm.add(e10e01.im)
        }

        return OslCalibrationCoefficients(
            frequencyHz = frequencyHz,
            directivityRe = directivityRe,
            directivityIm = directivityIm,
            sourceMatchRe = sourceMatchRe,
            sourceMatchIm = sourceMatchIm,
            reflectionTrackingRe = reflectionTrackingRe,
            reflectionTrackingIm = reflectionTrackingIm
        )
    }

    private const val MAX_USABLE_GAMMA_MAGNITUDE = 0.9999999
    private const val DEGENERATE_EPSILON = 1e-12
}
