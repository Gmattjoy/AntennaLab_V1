package com.example.antennalab_v1.model.testing

/*
########################################################################
FILE: OslCalibrationCoefficients.kt
PACKAGE: com.example.antennalab_v1.model.testing
LAYER: Model / Testing / Calibration

SYSTEM ROLE
Stores the REAL per-frequency correction data produced by a completed
Open/Short/Load (OSL) one-port calibration.

Unlike CalibrationSession — which only records that O/S/L steps were
captured — this holds the computed 3-term error model per frequency:
  directivity          (e00)
  source match         (e11)
  reflection tracking  (e10e01)

Each error term is complex and stored as parallel real/imaginary arrays
aligned to frequencyHz. All six coefficient lists and frequencyHz share
the same length (one entry per calibrated frequency point).

This is a pure data holder — no signal math and no Android/framework
refs. Computation lives in domain (OslCalibrationEngine); application to
sweeps lives in domain (CalibrationCorrector).
########################################################################
*/
data class OslCalibrationCoefficients(
    val frequencyHz: List<Long> = emptyList(),
    val directivityRe: List<Double> = emptyList(),
    val directivityIm: List<Double> = emptyList(),
    val sourceMatchRe: List<Double> = emptyList(),
    val sourceMatchIm: List<Double> = emptyList(),
    val reflectionTrackingRe: List<Double> = emptyList(),
    val reflectionTrackingIm: List<Double> = emptyList()
) {
    val pointCount: Int
        get() = frequencyHz.size

    /**
     * True only when every parallel array is populated and the same length,
     * i.e. the coefficient set is internally consistent and usable.
     */
    val isUsable: Boolean
        get() = frequencyHz.isNotEmpty() &&
                directivityRe.size == frequencyHz.size &&
                directivityIm.size == frequencyHz.size &&
                sourceMatchRe.size == frequencyHz.size &&
                sourceMatchIm.size == frequencyHz.size &&
                reflectionTrackingRe.size == frequencyHz.size &&
                reflectionTrackingIm.size == frequencyHz.size

    val calibratedStartHz: Long
        get() = frequencyHz.firstOrNull() ?: 0L

    val calibratedEndHz: Long
        get() = frequencyHz.lastOrNull() ?: 0L

    /**
     * Whether this calibration spans the requested sweep range, so its error
     * terms can be interpolated across it. Mirrors
     * [CalibrationSession.coversFrequencyRange] but works in Hz.
     */
    fun coversRange(requestedStartHz: Long, requestedEndHz: Long): Boolean {
        return isUsable &&
                calibratedStartHz <= requestedStartHz &&
                calibratedEndHz >= requestedEndHz
    }
}
