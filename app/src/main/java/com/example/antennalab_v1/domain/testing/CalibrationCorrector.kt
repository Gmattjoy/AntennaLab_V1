package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.testing.OslCalibrationCoefficients
import com.example.antennalab_v1.model.testing.SweepResult

/*
########################################################################
FILE: CalibrationCorrector.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Calibration

SYSTEM ROLE
Applies a stored OSL calibration to a raw sweep as a single post-parse
pass. For each measured point it:
  1. reconstructs the raw reflection coefficient Gm from the point's
     R/X (exact: Gm = (Z - Z0)/(Z + Z0)),
  2. interpolates the per-frequency error terms to the point frequency,
  3. corrects: Ga = (Gm - e00) / (e10e01 + e11*(Gm - e00)),
  4. rebuilds the point from Ga via OslCalibrationEngine.

Correction runs over the finished SweepResult, so it does NOT touch the
two device parse seams (NanoVNA ASCII / LiteVNA binary) — both converge
on R/X, from which Gamma is exactly recoverable.

Pure math; no Android/framework refs.
########################################################################
*/
object CalibrationCorrector {

    /**
     * Returns a corrected copy of [raw] with [SweepResult.isCalibrated] = true.
     * If the coefficients are unusable the raw result is returned unchanged.
     */
    fun apply(
        raw: SweepResult,
        coefficients: OslCalibrationCoefficients,
        calibrationLabel: String = ""
    ): SweepResult {
        if (!coefficients.isUsable) return raw

        val correctedPoints = raw.points.map { point ->
            val frequencyHz = (point.frequencyMHz * 1_000_000.0).toLong()

            val e00 = interpolate(
                coefficients.frequencyHz,
                coefficients.directivityRe,
                coefficients.directivityIm,
                frequencyHz
            )
            val e11 = interpolate(
                coefficients.frequencyHz,
                coefficients.sourceMatchRe,
                coefficients.sourceMatchIm,
                frequencyHz
            )
            val e10e01 = interpolate(
                coefficients.frequencyHz,
                coefficients.reflectionTrackingRe,
                coefficients.reflectionTrackingIm,
                frequencyHz
            )

            val measured = OslCalibrationEngine.gammaFromPoint(point)
            val delta = measured - e00
            val denominator = e10e01 + (e11 * delta)
            val corrected =
                if (denominator.magnitude < DENOMINATOR_EPSILON) measured
                else delta / denominator

            OslCalibrationEngine.sweepPointFromGamma(point.frequencyMHz, corrected)
        }

        return raw.copy(
            points = correctedPoints,
            isCalibrated = true,
            calibrationLabel = calibrationLabel.ifBlank {
                "OSL (${coefficients.pointCount} pts)"
            }
        )
    }

    /**
     * Complex linear interpolation of an error term (parallel re/im arrays,
     * aligned to ascending [frequencies]) to [targetHz]. Clamps to the
     * endpoints outside the calibrated span.
     */
    private fun interpolate(
        frequencies: List<Long>,
        re: List<Double>,
        im: List<Double>,
        targetHz: Long
    ): Complex {
        val lastIndex = frequencies.lastIndex
        if (targetHz <= frequencies.first()) return Complex(re.first(), im.first())
        if (targetHz >= frequencies.last()) return Complex(re[lastIndex], im[lastIndex])

        var high = frequencies.indexOfFirst { it >= targetHz }
        if (high <= 0) return Complex(re.first(), im.first())
        val low = high - 1

        val f0 = frequencies[low]
        val f1 = frequencies[high]
        val t = if (f1 == f0) 0.0 else (targetHz - f0).toDouble() / (f1 - f0).toDouble()

        return Complex(
            re = re[low] + t * (re[high] - re[low]),
            im = im[low] + t * (im[high] - im[low])
        )
    }

    private const val DENOMINATOR_EPSILON = 1e-12
}
