package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult

/*
########################################################################
FILE: SweepAnalyzer.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing Analysis

SYSTEM ROLE
Analyzes SWR sweep data to determine antenna characteristics.

Primary responsibilities:

• Find minimum SWR point
• Determine resonant frequency
• Estimate bandwidth
• Provide data for tuning assistant
• Provide data for sweep graph display

ARCHITECTURE POSITION

Hardware Interface
        │
        ▼
SweepController
        │
        ▼
SweepResult
        │
        ▼
SweepAnalyzer (THIS FILE)
        │
        ▼
Testing UI / Graph / Tuning Assistant

Future capabilities:

• antenna type recognition
• fault detection
• prediction vs measurement comparison
########################################################################
*/

object SweepAnalyzer {

    /*
    --------------------------------------------------------------------
    Find minimum SWR point in the sweep
    --------------------------------------------------------------------
    */
    fun findMinimumSWR(result: SweepResult): SweepPoint? {

        if (result.points.isEmpty()) return null

        return result.points.minByOrNull { it.swr }
    }

    /*
    --------------------------------------------------------------------
    Get resonant frequency
    (frequency where SWR is lowest)
    --------------------------------------------------------------------
    */
    fun getResonantFrequencyMHz(result: SweepResult): Double? {

        val minPoint = findMinimumSWR(result)

        return minPoint?.frequencyMHz
    }

}