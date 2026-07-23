package com.example.antennalab_v1

import com.example.antennalab_v1.domain.prediction.SWRPredictionEngine
import com.example.antennalab_v1.model.CalculatedDesign
import com.example.antennalab_v1.model.EnvironmentalConditions
import com.example.antennalab_v1.model.GroundType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure domain coverage for [SWRPredictionEngine.predict] — how the per-influence
 * environmental shifts combine into a PredictedPerformance (total resonance
 * shift, SWR, efficiency, summary). Real CalculatedDesign / EnvironmentalConditions
 * models, no mocking.
 */
class SWRPredictionEngineTest {

    private val tol = 1e-6

    // ------------------------------------------------------------------
    // Reference (ideal) conditions produce no shift
    // ------------------------------------------------------------------

    @Test
    fun predict_referenceConditions_produceNoShiftBaselineSwrAndFullEfficiency() {
        val result = SWRPredictionEngine.predict(
            design = CalculatedDesign(),
            environment = EnvironmentalConditions() // all defaults sit on the reference points
        )

        assertEquals(0.0, result.resonanceShiftHz, tol)
        assertEquals(1.0, result.predictedSWR, tol)      // baseSWR 1.0 + 0
        assertEquals(1.0, result.efficiencyEstimate, tol) // 1.0 - 0
    }

    // ------------------------------------------------------------------
    // Total shift = sum of all five environmental influences
    // ------------------------------------------------------------------

    @Test
    fun predict_totalShift_sumsAllFiveInfluences() {
        val result = SWRPredictionEngine.predict(
            design = CalculatedDesign(),
            environment = EnvironmentalConditions(
                temperatureCelsius = 30.0,      // (30-20)*-8  = -80
                humidityPercent = 50.0,         // reference    =   0
                groundType = GroundType.SANDY,  //              = 120
                nearbyStructures = true,        //              = 250
                antennaHeightMeters = 2.0       // reference    =   0
            )
        )

        val expectedTotal = -80.0 + 0.0 + 120.0 + 250.0 + 0.0 // = 290
        assertEquals(expectedTotal, result.resonanceShiftHz, tol)
        assertEquals(1.0 + kotlin.math.abs(expectedTotal) * 0.00002, result.predictedSWR, tol)
        assertEquals(1.0 - kotlin.math.abs(expectedTotal) * 0.00001, result.efficiencyEstimate, tol)
    }

    // ------------------------------------------------------------------
    // SWR rises with shift magnitude regardless of sign (uses abs)
    // ------------------------------------------------------------------

    @Test
    fun predict_swrRisesForNegativeTotalShiftToo() {
        val result = SWRPredictionEngine.predict(
            design = CalculatedDesign(),
            environment = EnvironmentalConditions(
                humidityPercent = 100.0,       // -150
                groundType = GroundType.CLAY   // -140
            )
        )

        // Total is -290; SWR must still be above baseline.
        assertEquals(-290.0, result.resonanceShiftHz, tol)
        assertTrue(result.predictedSWR > 1.0)
        assertEquals(1.0 + 290.0 * 0.00002, result.predictedSWR, tol)
    }

    // ------------------------------------------------------------------
    // Efficiency is clamped into [0, 1] for extreme shifts
    // ------------------------------------------------------------------

    @Test
    fun predict_efficiencyClampsToZeroForExtremeShift() {
        val result = SWRPredictionEngine.predict(
            design = CalculatedDesign(),
            environment = EnvironmentalConditions(temperatureCelsius = 1_000_020.0)
        )

        // Loss would be far greater than 1.0; efficiency must clamp to 0, not go negative.
        assertEquals(0.0, result.efficiencyEstimate, tol)
        assertTrue(result.efficiencyEstimate >= 0.0)
    }

    // ------------------------------------------------------------------
    // Summary reflects the environment inputs
    // ------------------------------------------------------------------

    @Test
    fun predict_summaryReflectsEnvironmentInputs() {
        val result = SWRPredictionEngine.predict(
            design = CalculatedDesign(),
            environment = EnvironmentalConditions(
                temperatureCelsius = 25.0,
                humidityPercent = 60.0,
                groundType = GroundType.ROCKY
            )
        )

        assertTrue(result.environmentSummary.contains("25.0"))
        assertTrue(result.environmentSummary.contains("60.0"))
        assertTrue(result.environmentSummary.contains("ROCKY"))
    }
}
