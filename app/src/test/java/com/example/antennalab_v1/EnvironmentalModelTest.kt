package com.example.antennalab_v1

import com.example.antennalab_v1.domain.prediction.EnvironmentalModel
import com.example.antennalab_v1.model.GroundType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure domain coverage for [EnvironmentalModel] — the per-influence resonance
 * shift approximations used by the SWR prediction engine. Verifies the reference
 * points (zero shift) and the linear/lookup behaviour each side of them.
 */
class EnvironmentalModelTest {

    private val tol = 1e-9

    // ------------------------------------------------------------------
    // Temperature: reference 20 C, delta * -8 Hz
    // ------------------------------------------------------------------

    @Test
    fun temperatureShift_isZeroAtReferenceAndLinearAround() {
        assertEquals(0.0, EnvironmentalModel.temperatureShiftHz(20.0), tol)
        assertEquals(-80.0, EnvironmentalModel.temperatureShiftHz(30.0), tol) // warmer -> lower
        assertEquals(80.0, EnvironmentalModel.temperatureShiftHz(10.0), tol)  // cooler -> higher
    }

    // ------------------------------------------------------------------
    // Humidity: reference 50 %, delta * -3 Hz
    // ------------------------------------------------------------------

    @Test
    fun humidityShift_isZeroAtReferenceAndLinearAround() {
        assertEquals(0.0, EnvironmentalModel.humidityShiftHz(50.0), tol)
        assertEquals(-150.0, EnvironmentalModel.humidityShiftHz(100.0), tol)
        assertEquals(150.0, EnvironmentalModel.humidityShiftHz(0.0), tol)
    }

    // ------------------------------------------------------------------
    // Ground: fixed per-type lookup
    // ------------------------------------------------------------------

    @Test
    fun groundShift_mapsEachGroundType() {
        assertEquals(0.0, EnvironmentalModel.groundShiftHz(GroundType.AVERAGE_SOIL), tol)
        assertEquals(120.0, EnvironmentalModel.groundShiftHz(GroundType.SANDY), tol)
        assertEquals(-140.0, EnvironmentalModel.groundShiftHz(GroundType.CLAY), tol)
        assertEquals(180.0, EnvironmentalModel.groundShiftHz(GroundType.ROCKY), tol)
        assertEquals(-220.0, EnvironmentalModel.groundShiftHz(GroundType.SALT_RICH), tol)
        assertEquals(0.0, EnvironmentalModel.groundShiftHz(GroundType.UNKNOWN), tol)
    }

    // ------------------------------------------------------------------
    // Structures: fixed offset when present
    // ------------------------------------------------------------------

    @Test
    fun structureShift_isOffsetOnlyWhenStructuresPresent() {
        assertEquals(250.0, EnvironmentalModel.structureShiftHz(true), tol)
        assertEquals(0.0, EnvironmentalModel.structureShiftHz(false), tol)
    }

    // ------------------------------------------------------------------
    // Height: reference 2 m, delta * -15 Hz
    // ------------------------------------------------------------------

    @Test
    fun heightShift_isZeroAtReferenceAndLinearAround() {
        assertEquals(0.0, EnvironmentalModel.heightShiftHz(2.0), tol)
        assertEquals(-150.0, EnvironmentalModel.heightShiftHz(12.0), tol) // higher -> lower
        assertEquals(30.0, EnvironmentalModel.heightShiftHz(0.0), tol)    // lower -> higher
    }
}
