package com.example.antennalab_v1.model

/*
########################################################################
FILE: AmateurBand.kt
PACKAGE: com.example.antennalab_v1.model
LAYER: Model / Pure data

SYSTEM ROLE
Reference data for the amateur-band overlay on the sweep frequency axis
(UI redesign spec §2.3) and the "band" column of the marker readout
table. Pure data only — the tables, lookups, and overlap math live in
`domain/analysis/AmateurBandPlan`.

WHY A REGION ENUM EXISTS AT ALL
Band edges differ by IARU region (2m ends at 146 MHz in Region 1 but
148 MHz in Regions 2/3; 80m ends at 3.800 / 4.000 / 3.900). A
single-region table has no concept to extend, so adding a second region
later would mean reworking every call site. The enum is carried from the
start; the default is pinned in one constant.
########################################################################
*/

/*
--------------------------------------------------------------------
IARU region
EDIT SECTION 1001
--------------------------------------------------------------------
The three IARU regions. Region 3 (Asia-Pacific, incl. Australia) is
this build's default — see AmateurBandPlan.DEFAULT_REGION.
--------------------------------------------------------------------
*/
enum class IaruRegion {
    REGION_1,
    REGION_2,
    REGION_3
}

/*
--------------------------------------------------------------------
A single amateur allocation
EDIT SECTION 1002
--------------------------------------------------------------------
Half-open at neither end: both edges are inclusive, because band edges
are the frequencies operators actually tune to and a marker sitting
exactly on 14.000 MHz is in the 20m band.
--------------------------------------------------------------------
*/
data class AmateurBand(

    /*
    Short operator-facing name, e.g. "20m", "70cm". This is what the
    axis overlay and the marker table print.
    */
    val name: String,

    /*
    Inclusive lower edge, MHz.
    */
    val startMHz: Double,

    /*
    Inclusive upper edge, MHz.
    */
    val endMHz: Double
)
