package com.example.antennalab_v1.domain.prediction

import com.example.antennalab_v1.model.CalculatedDesign
import com.example.antennalab_v1.model.EnvironmentalConditions
import com.example.antennalab_v1.model.PredictedPerformance
import kotlin.math.abs

/*
########################################################################
FILE: SWRPredictionEngine.kt
PACKAGE: com.example.antennalab_v1.domain.prediction
LAYER: Domain / Prediction Engine

PURPOSE
Estimate how real-world environmental conditions may shift antenna
behaviour away from the ideal mathematical design.

This file converts:
- ideal design data
- environmental conditions

into:
- predicted SWR
- predicted resonance shift
- predicted efficiency
- human-readable environment summary

IMPORTANT DESIGN RULES
- This file does NOT modify CalculatedDesign.
- This file returns a separate PredictedPerformance model.
- This file must remain UI-independent.
- This file is a first-pass engineering approximation layer, not a full
  RF simulation system.

EDITING GUIDE
Use the titled sections below when making future edits.

Examples:
- Change summary wording in the SUMMARY BUILD section
- Change SWR behaviour in the SWR ESTIMATION section
- Change efficiency behaviour in the EFFICIENCY ESTIMATION section
- Change environmental combination logic in the TOTAL SHIFT COMBINATION
  section

FUTURE USES
- tuning assistant
- testing workflows
- environmental diagnostics
- live telemetry streaming
- companion device mode
########################################################################
*/

object SWRPredictionEngine {

    /*
    ####################################################################
    MAIN ENTRY POINT
    --------------------------------------------------------------------
    This is the single public function used by other parts of the app.

    It receives:
    - the ideal calculated antenna design
    - the current environmental conditions

    It returns:
    - a PredictedPerformance object containing adjusted estimates

    Future integrations:
    - CalculationEngine
    - testing session systems
    - tuning assistant
    - companion mode telemetry
    ####################################################################
    */
    fun predict(
        design: CalculatedDesign,
        environment: EnvironmentalConditions
    ): PredictedPerformance {

        /*
        ################################################################
        ENVIRONMENT INPUT MODELS
        ----------------------------------------------------------------
        Each line below asks EnvironmentalModel for one specific type of
        estimated environmental influence.

        These are kept separate so they are easy to:
        - debug
        - tune
        - replace later with better formulas
        ################################################################
        */
        val temperatureShift =
            EnvironmentalModel.temperatureShiftHz(environment.temperatureCelsius)

        val humidityShift =
            EnvironmentalModel.humidityShiftHz(environment.humidityPercent)

        val groundShift =
            EnvironmentalModel.groundShiftHz(environment.groundType)

        val structureShift =
            EnvironmentalModel.structureShiftHz(environment.nearbyStructures)

        val heightShift =
            EnvironmentalModel.heightShiftHz(environment.antennaHeightMeters)

        /*
        ################################################################
        TOTAL SHIFT COMBINATION
        ----------------------------------------------------------------
        This section combines all environmental influences into one total
        resonance shift estimate.

        If future work needs weighting or prioritisation, this is the
        first section to review.
        ################################################################
        */
        val totalShift =
            temperatureShift +
                    humidityShift +
                    groundShift +
                    structureShift +
                    heightShift

        /*
        ################################################################
        SWR ESTIMATION
        ----------------------------------------------------------------
        Converts the total resonance shift into a simple SWR estimate.

        Current behaviour:
        - larger environmental shift = larger SWR increase

        Future upgrades may use:
        - antenna-type-specific SWR logic
        - band-sensitive scaling
        - measurement-informed adjustment
        ################################################################
        */
        val predictedSWR =
            estimateSWR(design, totalShift)

        /*
        ################################################################
        EFFICIENCY ESTIMATION
        ----------------------------------------------------------------
        Converts the total shift into a simple estimated efficiency
        result.

        Current behaviour:
        - larger shift = slightly lower estimated efficiency
        ################################################################
        */
        val efficiency =
            estimateEfficiency(totalShift)

        /*
        ################################################################
        SUMMARY BUILD
        ----------------------------------------------------------------
        This section creates the human-readable summary text used by UI,
        logs, dashboards, or future telemetry systems.

        Safe future edits here:
        - wording
        - order of fields
        - additional displayed variables
        ################################################################
        */
        val summary =
            "Temp ${environment.temperatureCelsius}°C, " +
                    "Humidity ${environment.humidityPercent}%, " +
                    "Ground ${environment.groundType}"

        /*
        ################################################################
        FINAL RESULT BUILD
        ----------------------------------------------------------------
        Packages all calculated estimates into PredictedPerformance.

        This is the final output of the prediction engine.
        ################################################################
        */
        return PredictedPerformance(
            predictedSWR = predictedSWR,
            resonanceShiftHz = totalShift,
            efficiencyEstimate = efficiency,
            environmentSummary = summary
        )
    }

    /*
    ####################################################################
    SWR ESTIMATION HELPER
    --------------------------------------------------------------------
    PURPOSE
    Convert total environmental shift into a simplified SWR increase.

    CURRENT MODEL
    - Starts from a basic baseline SWR
    - Adds a small increase proportional to total shift magnitude

    SAFE EDIT AREA
    Change this function if you want to alter how aggressively SWR rises
    when environmental conditions become less ideal.
    ####################################################################
    */
    private fun estimateSWR(
        design: CalculatedDesign,
        shift: Double
    ): Double {

        /*
        Base SWR currently uses a fixed placeholder.
        In future this may be replaced with:
        - ideal design SWR
        - feedpoint mismatch estimate
        - measured live SWR baseline
        */
        val baseSWR = 1.0

        /*
        Scaling factor controlling how strongly total shift affects SWR.
        Larger number = SWR rises faster.
        */
        val increase = abs(shift) * 0.00002

        /*
        Final predicted SWR result.
        */
        return baseSWR + increase
    }

    /*
    ####################################################################
    EFFICIENCY ESTIMATION HELPER
    --------------------------------------------------------------------
    PURPOSE
    Convert total environmental shift into a simplified efficiency
    estimate.

    CURRENT MODEL
    - Starts from ideal efficiency of 1.0
    - applies a small loss proportional to shift magnitude
    - clamps result into valid range 0.0 to 1.0

    SAFE EDIT AREA
    Change this function if you want stronger or weaker efficiency loss.
    ####################################################################
    */
    private fun estimateEfficiency(
        shift: Double
    ): Double {

        /*
        Scaling factor controlling how strongly total shift reduces
        efficiency.
        */
        val loss = abs(shift) * 0.00001

        /*
        Clamp keeps result inside valid percentage-like range.
        */
        return (1.0 - loss).coerceIn(0.0, 1.0)
    }
}