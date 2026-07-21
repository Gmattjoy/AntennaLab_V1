package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.testing.SweepResult

object SweepValidator {

    fun validateSweep(result: SweepResult): Boolean {
        return explainValidationFailure(result) == null
    }

    fun explainValidationFailure(result: SweepResult): String? {
        return when {
            !hasValidFrequencyRange(result) ->
                "Invalid frequency range. start=${result.startFrequencyMHz} end=${result.endFrequencyMHz}"
            !hasValidStep(result) ->
                "Invalid step size. step=${result.stepMHz}"
            !hasPoints(result) ->
                "Too few sweep points. count=${result.points.size}"
            !allPointsAreFinite(result) ->
                "Sweep contains non-finite numeric values."
            !pointsStayInsideSweepRange(result) ->
                buildOutOfRangeFailureText(result)
            else ->
                null
        }
    }

    fun hasValidFrequencyRange(result: SweepResult): Boolean {
        return result.endFrequencyMHz > result.startFrequencyMHz
    }

    fun hasValidStep(result: SweepResult): Boolean {
        return result.stepMHz.isFinite() && result.stepMHz > 0.0
    }

    fun hasPoints(result: SweepResult): Boolean {
        return result.points.size >= 2
    }

    fun allPointsAreFinite(result: SweepResult): Boolean {
        return result.points.all { point ->
            point.frequencyMHz.isFinite() &&
                    point.swr.isFinite() &&
                    point.returnLossDb.isFinite() &&
                    point.resistance.isFinite() &&
                    point.reactance.isFinite()
        }
    }

    fun pointsStayInsideSweepRange(result: SweepResult): Boolean {
        return result.points.all { point ->
            point.frequencyMHz >= result.startFrequencyMHz &&
                    point.frequencyMHz <= result.endFrequencyMHz
        }
    }

    private fun buildOutOfRangeFailureText(result: SweepResult): String {
        val firstOutsidePoint =
            result.points.firstOrNull { point ->
                point.frequencyMHz < result.startFrequencyMHz ||
                        point.frequencyMHz > result.endFrequencyMHz
            }

        return if (firstOutsidePoint == null) {
            "Sweep points fell outside the requested sweep range."
        } else {
            "Point frequency outside requested sweep range. point=${firstOutsidePoint.frequencyMHz} range=${result.startFrequencyMHz}..${result.endFrequencyMHz}"
        }
    }
}

private fun Double.isFinite(): Boolean {
    return !this.isNaN() && !this.isInfinite()
}