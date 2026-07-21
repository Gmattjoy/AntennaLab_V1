package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.testing.SweepResult

/*
########################################################################
SECTION 1000
SWEEP DATA SOURCE INTERFACE
------------------------------------------------------------------------
PURPOSE
Defines the contract for any sweep-producing backend.
########################################################################
*/
interface SweepDataSource {
    fun runSweep(
        startMHz: Double,
        endMHz: Double,
        stepMHz: Double
    ): SweepResult
}

/*
########################################################################
SECTION 2000
DEMO SWEEP DATA SOURCE
------------------------------------------------------------------------
PURPOSE
Provides the current stable simulated sweep implementation by routing to
the existing SweepController.

This is the first concrete SweepDataSource used by the app until live
hardware sources are added.
########################################################################
*/
class DemoSweepDataSource : SweepDataSource {
    override fun runSweep(
        startMHz: Double,
        endMHz: Double,
        stepMHz: Double
    ): SweepResult {
        return SweepController.runSweep(
            startMHz = startMHz,
            endMHz = endMHz,
            stepMHz = stepMHz
        )
    }
}