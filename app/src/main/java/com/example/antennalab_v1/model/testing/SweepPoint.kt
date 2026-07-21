package com.example.antennalab_v1.model.testing

/*
########################################################################
FILE: SweepPoint.kt
PACKAGE: com.example.antennalab_v1.model.testing
LAYER: Model / Testing Measurement

SYSTEM ROLE
Represents a single measurement point in a frequency sweep.

Stores multiple RF values so the same dataset can support:

• SWR graph
• return loss
• impedance display
• Smith chart
• phase view
• S21 later

SAFE EDIT AREA
- add future RF parameters
########################################################################
*/

/*
########################################################################
EDIT SECTION 1001
------------------------------------------------------------------------
PURPOSE
Primary sweep point data structure for current and future RF
measurement fields.
########################################################################
*/
data class SweepPoint(

    /*
    --------------------------------------------------------------------
    Frequency of measurement
    EDIT SECTION 1002
    --------------------------------------------------------------------
    */
    val frequencyMHz: Double,

    /*
    --------------------------------------------------------------------
    Standing Wave Ratio
    EDIT SECTION 1003
    --------------------------------------------------------------------
    */
    val swr: Double,

    /*
    --------------------------------------------------------------------
    Return loss (dB)
    EDIT SECTION 1004
    --------------------------------------------------------------------
    */
    val returnLossDb: Double,

    /*
    --------------------------------------------------------------------
    Resistance (Ohms)
    EDIT SECTION 1005
    --------------------------------------------------------------------
    */
    val resistance: Double,

    /*
    --------------------------------------------------------------------
    Reactance (Ohms)
    EDIT SECTION 1006
    --------------------------------------------------------------------
    */
    val reactance: Double,

    /*
    --------------------------------------------------------------------
    S11 magnitude in dB
    EDIT SECTION 1007
    --------------------------------------------------------------------
    PURPOSE
    Stores direct reflection measurement magnitude when available from
    real hardware. For the current simulated system this can mirror
    return loss behaviour until real data arrives.
    --------------------------------------------------------------------
    */
    val s11MagnitudeDb: Double = returnLossDb,

    /*
    --------------------------------------------------------------------
    S11 phase in degrees
    EDIT SECTION 1008
    --------------------------------------------------------------------
    PURPOSE
    Stores reflection phase for Smith, phase, and future time-domain
    style analysis.
    --------------------------------------------------------------------
    */
    val s11PhaseDegrees: Double = 0.0,

    /*
    --------------------------------------------------------------------
    S21 magnitude in dB
    EDIT SECTION 1009
    --------------------------------------------------------------------
    PURPOSE
    Stores forward transmission magnitude when available from supported
    hardware.
    --------------------------------------------------------------------
    */
    val s21MagnitudeDb: Double = 0.0,

    /*
    --------------------------------------------------------------------
    S21 phase in degrees
    EDIT SECTION 1010
    --------------------------------------------------------------------
    PURPOSE
    Stores forward transmission phase when available from supported
    hardware.
    --------------------------------------------------------------------
    */
    val s21PhaseDegrees: Double = 0.0
)