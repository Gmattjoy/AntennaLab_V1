package com.example.antennalab_v1.domain.analysis

import com.example.antennalab_v1.model.AmateurBand
import com.example.antennalab_v1.model.IaruRegion

/*
########################################################################
FILE: AmateurBandPlan.kt
PACKAGE: com.example.antennalab_v1.domain.analysis
LAYER: Domain / Analysis

SYSTEM ROLE
The amateur band tables and the lookups the UI needs from them:
  - which band a marker frequency falls in (marker readout "band")
  - which bands overlap a sweep span (frequency-axis overlay, spec §2.3)
Pure Kotlin, no Android refs, no Compose. Data classes live in
`model/AmateurBand`.

HONESTY NOTE — READ BEFORE TRUSTING THESE EDGES
These are IARU *region-level* allocations. Actual privileges are
narrower and vary by country and by licence class (an Australian
Foundation licence, for instance, reaches nowhere near all of these).
This table exists to orient a trace on the frequency axis — it is
operator guidance, NOT a legal band-plan reference, and nothing in the
app should gate transmission or make a compliance claim from it.
Segment-level detail (CW/digital/phone sub-bands) is deliberately absent.

60m is the WRC-15 global 5.3515–5.3665 MHz allocation. Channelised and
country-specific 60m arrangements are not modelled.
########################################################################
*/
object AmateurBandPlan {

    /*
    --------------------------------------------------------------------
    Default region
    EDIT SECTION 1001
    --------------------------------------------------------------------
    Region 3 = Asia-Pacific, including Australia (this build's home
    region). Change this ONE constant to reskin the whole app's overlay;
    every lookup below defaults to it.
    --------------------------------------------------------------------
    */
    val DEFAULT_REGION: IaruRegion = IaruRegion.REGION_3

    /*
    --------------------------------------------------------------------
    Region 1 — Europe, Africa, Middle East, northern Asia
    EDIT SECTION 1002
    --------------------------------------------------------------------
    */
    private val REGION_1_BANDS: List<AmateurBand> = listOf(
        AmateurBand("160m", 1.810, 2.000),
        AmateurBand("80m", 3.500, 3.800),
        AmateurBand("60m", 5.3515, 5.3665),
        AmateurBand("40m", 7.000, 7.200),
        AmateurBand("30m", 10.100, 10.150),
        AmateurBand("20m", 14.000, 14.350),
        AmateurBand("17m", 18.068, 18.168),
        AmateurBand("15m", 21.000, 21.450),
        AmateurBand("12m", 24.890, 24.990),
        AmateurBand("10m", 28.000, 29.700),
        AmateurBand("6m", 50.000, 52.000),
        AmateurBand("2m", 144.000, 146.000),
        AmateurBand("70cm", 430.000, 440.000),
        AmateurBand("23cm", 1240.000, 1300.000),
        AmateurBand("13cm", 2300.000, 2450.000)
    )

    /*
    --------------------------------------------------------------------
    Region 2 — the Americas
    EDIT SECTION 1003
    --------------------------------------------------------------------
    */
    private val REGION_2_BANDS: List<AmateurBand> = listOf(
        AmateurBand("160m", 1.800, 2.000),
        AmateurBand("80m", 3.500, 4.000),
        AmateurBand("60m", 5.3515, 5.3665),
        AmateurBand("40m", 7.000, 7.300),
        AmateurBand("30m", 10.100, 10.150),
        AmateurBand("20m", 14.000, 14.350),
        AmateurBand("17m", 18.068, 18.168),
        AmateurBand("15m", 21.000, 21.450),
        AmateurBand("12m", 24.890, 24.990),
        AmateurBand("10m", 28.000, 29.700),
        AmateurBand("6m", 50.000, 54.000),
        AmateurBand("2m", 144.000, 148.000),
        AmateurBand("70cm", 430.000, 450.000),
        AmateurBand("23cm", 1240.000, 1300.000),
        AmateurBand("13cm", 2300.000, 2450.000)
    )

    /*
    --------------------------------------------------------------------
    Region 3 — Asia-Pacific (DEFAULT)
    EDIT SECTION 1004
    --------------------------------------------------------------------
    */
    private val REGION_3_BANDS: List<AmateurBand> = listOf(
        AmateurBand("160m", 1.800, 2.000),
        AmateurBand("80m", 3.500, 3.900),
        AmateurBand("60m", 5.3515, 5.3665),
        AmateurBand("40m", 7.000, 7.300),
        AmateurBand("30m", 10.100, 10.150),
        AmateurBand("20m", 14.000, 14.350),
        AmateurBand("17m", 18.068, 18.168),
        AmateurBand("15m", 21.000, 21.450),
        AmateurBand("12m", 24.890, 24.990),
        AmateurBand("10m", 28.000, 29.700),
        AmateurBand("6m", 50.000, 54.000),
        AmateurBand("2m", 144.000, 148.000),
        AmateurBand("70cm", 430.000, 450.000),
        AmateurBand("23cm", 1240.000, 1300.000),
        AmateurBand("13cm", 2300.000, 2450.000)
    )

    /*
    --------------------------------------------------------------------
    Table accessor
    EDIT SECTION 1005
    --------------------------------------------------------------------
    Returned ascending by startMHz. Callers must treat the list as
    read-only reference data.
    --------------------------------------------------------------------
    */
    fun bandsFor(region: IaruRegion = DEFAULT_REGION): List<AmateurBand> =
        when (region) {
            IaruRegion.REGION_1 -> REGION_1_BANDS
            IaruRegion.REGION_2 -> REGION_2_BANDS
            IaruRegion.REGION_3 -> REGION_3_BANDS
        }

    /*
    --------------------------------------------------------------------
    Which band is this frequency in?
    EDIT SECTION 1006
    --------------------------------------------------------------------
    Both edges inclusive. Returns null when the frequency falls between
    allocations (the common case for a wide sweep) — callers decide how
    to render "no band", rather than this returning a magic string.
    --------------------------------------------------------------------
    */
    fun bandAt(
        frequencyMHz: Double,
        region: IaruRegion = DEFAULT_REGION
    ): AmateurBand? =
        bandsFor(region).firstOrNull { band ->
            frequencyMHz >= band.startMHz && frequencyMHz <= band.endMHz
        }

    /*
    --------------------------------------------------------------------
    Marker-table label
    EDIT SECTION 1007
    --------------------------------------------------------------------
    The display form of bandAt(). Out-of-band reads as an em dash so the
    marker table keeps a stable column width.
    --------------------------------------------------------------------
    */
    fun bandLabelAt(
        frequencyMHz: Double,
        region: IaruRegion = DEFAULT_REGION
    ): String = bandAt(frequencyMHz, region)?.name ?: "—"

    /*
    --------------------------------------------------------------------
    Which bands does a sweep span touch?
    EDIT SECTION 1008
    --------------------------------------------------------------------
    Drives the frequency-axis overlay: any band overlapping the visible
    span at all, including one that merely clips the edge, and including
    a band that wholly contains a narrow span (the usual case — a 1 MHz
    sweep sitting inside 20m).

    Tolerates a reversed span (start > end) by normalising, so callers
    passing axis bounds in either order get the same answer.
    --------------------------------------------------------------------
    */
    fun bandsOverlapping(
        startMHz: Double,
        endMHz: Double,
        region: IaruRegion = DEFAULT_REGION
    ): List<AmateurBand> {
        val low = minOf(startMHz, endMHz)
        val high = maxOf(startMHz, endMHz)
        return bandsFor(region).filter { band ->
            band.startMHz <= high && band.endMHz >= low
        }
    }
}
