package com.example.antennalab_v1.domain.prediction

import com.example.antennalab_v1.model.GroundType

/*
########################################################################
FILE: EnvironmentalModel.kt
PACKAGE: com.example.antennalab_v1.domain.prediction
LAYER: Domain / Environmental Influence Models

FILE ROLE
This file provides reusable estimation models describing how real-world
environmental conditions may influence antenna behaviour

These models are intentionally lightweight engineering approximations.

They are used by:

• SWRPredictionEngine
• future tuning assistant systems
• testing and diagnostics tools
• environmental simulation features
• companion device telemetry analysis

This file must remain:
✔ UI independent
✔ fast to execute
✔ modular and easy to tune

EDITING GUIDE
Each section below models one type of environmental influence.

Safe edits usually involve:
• adjusting coefficients
• refining approximation formulas
• improving realism

Avoid:
✖ adding UI logic
✖ referencing Android framework classes
########################################################################
*/

object EnvironmentalModel {

    /*
    ####################################################################
    TEMPERATURE SHIFT MODEL
    --------------------------------------------------------------------
    PURPOSE
    Estimate resonance shift caused by thermal expansion of antenna
    conductors.

    CURRENT MODEL
    - Uses a simple linear approximation.
    - Higher temperatures slightly lengthen conductors.
    - Longer conductors lower resonant frequency.

    SAFE EDIT AREA
    Adjust the multiplier if experimental testing suggests a different
    sensitivity to temperature.
    ####################################################################
    */
    fun temperatureShiftHz(tempC: Double): Double {

        val referenceTemp = 20.0
        val delta = tempC - referenceTemp

        return delta * -8.0
    }

    /*
    ####################################################################
    HUMIDITY SHIFT MODEL
    --------------------------------------------------------------------
    PURPOSE
    Estimate the effect of atmospheric humidity on antenna behaviour.

    CURRENT MODEL
    - Uses a simple dielectric influence approximation.

    FUTURE POSSIBILITIES
    - incorporate frequency-dependent dielectric behaviour
    - incorporate fog / condensation conditions
    ####################################################################
    */
    fun humidityShiftHz(humidityPercent: Double): Double {

        val referenceHumidity = 50.0
        val delta = humidityPercent - referenceHumidity

        return delta * -3.0
    }

    /*
    ####################################################################
    GROUND INFLUENCE MODEL
    --------------------------------------------------------------------
    PURPOSE
    Estimate how different soil types influence antenna resonance.

    Ground conductivity and dielectric properties affect:

    • vertical antennas
    • low antennas near earth
    • ground-plane systems

    SAFE EDIT AREA
    Modify values to tune behaviour based on real-world testing.
    ####################################################################
    */
    fun groundShiftHz(groundType: GroundType): Double {

        return when (groundType) {
            GroundType.AVERAGE_SOIL -> 0.0
            GroundType.SANDY -> 120.0
            GroundType.CLAY -> -140.0
            GroundType.ROCKY -> 180.0
            GroundType.SALT_RICH -> -220.0
            GroundType.UNKNOWN -> 0.0
        }
    }

    /*
    ####################################################################
    STRUCTURE INFLUENCE MODEL
    --------------------------------------------------------------------
    PURPOSE
    Estimate detuning caused by nearby objects such as:

    • buildings
    • fences
    • metal structures
    • nearby antennas

    CURRENT MODEL
    - simple fixed offset when structures are present

    FUTURE POSSIBILITIES
    - distance-sensitive modelling
    - structure type modelling
    ####################################################################
    */
    fun structureShiftHz(nearbyStructures: Boolean): Double {

        return if (nearbyStructures) 250.0 else 0.0
    }

    /*
    ####################################################################
    ANTENNA HEIGHT MODEL
    --------------------------------------------------------------------
    PURPOSE
    Estimate resonance shift caused by height above ground.

    Height influences:
    • ground coupling
    • radiation pattern
    • impedance behaviour

    CURRENT MODEL
    - reference height = 2 meters
    - linear approximation

    SAFE EDIT AREA
    Adjust reference height or scaling multiplier.
    ####################################################################
    */
    fun heightShiftHz(heightMeters: Double): Double {

        val referenceHeight = 2.0
        val delta = heightMeters - referenceHeight

        return delta * -15.0
    }
}