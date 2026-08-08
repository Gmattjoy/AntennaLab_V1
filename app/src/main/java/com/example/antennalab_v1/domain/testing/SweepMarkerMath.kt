package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.domain.analysis.AmateurBandPlan
import com.example.antennalab_v1.model.IaruRegion
import com.example.antennalab_v1.model.testing.SweepPoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot

/*
########################################################################
FILE: SweepMarkerMath.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Math

SYSTEM ROLE
The derived values a NanoVNA-Saver user expects to read at a marker
(UI redesign spec §2.3): |Z|, R + jX, Q, series-equivalent Cs / Ls,
return loss, reflection phase, and band. Pure math + formatting, no
Android refs, no Compose — the Phase-3 marker readout table renders
this and computes nothing itself.

REUSE, NOT REDERIVATION
Reflection coefficient comes from OslCalibrationEngine.gammaFromPoint,
the same exact R/X -> gamma path CalibrationCorrector uses. Phase is
therefore consistent with the corrected data and does NOT depend on
SweepPoint.s11PhaseDegrees being populated (it defaults to 0.0 and is
only meaningful when SweepResult.supportsS11Phase is true).
########################################################################
*/
object SweepMarkerMath {

    private const val MHZ_TO_HZ = 1_000_000.0

    /*
    Below this resistance Q and the series equivalents are meaningless
    rather than merely large, so they report as unavailable (null).
    */
    private const val MIN_RESISTANCE_OHMS = 1e-9

    /*
    Reactance closer to zero than this is treated as purely resistive:
    no meaningful series C or L to quote.
    */
    private const val MIN_REACTANCE_OHMS = 1e-9

    /*
    --------------------------------------------------------------------
    Impedance magnitude |Z|
    EDIT SECTION 1001
    --------------------------------------------------------------------
    */
    fun impedanceMagnitudeOhms(point: SweepPoint): Double =
        hypot(point.resistance, point.reactance)

    /*
    --------------------------------------------------------------------
    Q factor
    EDIT SECTION 1002
    --------------------------------------------------------------------
    Series-equivalent Q = |X| / R. Null when R is degenerate.
    --------------------------------------------------------------------
    */
    fun qFactor(point: SweepPoint): Double? {
        if (abs(point.resistance) < MIN_RESISTANCE_OHMS) return null
        return abs(point.reactance) / abs(point.resistance)
    }

    /*
    --------------------------------------------------------------------
    Series equivalent component
    EDIT SECTION 1003
    --------------------------------------------------------------------
    Reactance restated as the component that would produce it AT THIS
    FREQUENCY — capacitive when X < 0, inductive when X > 0. This is a
    per-frequency equivalent, not a claim about the physical antenna.

        X > 0 :  L = X / (2*pi*f)          [henries]
        X < 0 :  C = 1 / (2*pi*f*|X|)      [farads]

    Null when the point is purely resistive or the frequency is
    non-positive (nothing to divide by).
    --------------------------------------------------------------------
    */
    fun seriesEquivalent(point: SweepPoint): SeriesEquivalent? {
        val frequencyHz = point.frequencyMHz * MHZ_TO_HZ
        if (frequencyHz <= 0.0) return null
        if (abs(point.reactance) < MIN_REACTANCE_OHMS) return null

        val omega = 2.0 * PI * frequencyHz
        return if (point.reactance > 0.0) {
            SeriesEquivalent.Inductive(henries = point.reactance / omega)
        } else {
            SeriesEquivalent.Capacitive(farads = 1.0 / (omega * abs(point.reactance)))
        }
    }

    /*
    --------------------------------------------------------------------
    Reflection phase
    EDIT SECTION 1004
    --------------------------------------------------------------------
    Degrees, -180..180, derived from gamma so it holds for every device
    and for calibrated data alike.
    --------------------------------------------------------------------
    */
    fun reflectionPhaseDegrees(point: SweepPoint): Double =
        OslCalibrationEngine.gammaFromPoint(point).phaseDegrees

    /*
    --------------------------------------------------------------------
    Reflection magnitude
    EDIT SECTION 1005
    --------------------------------------------------------------------
    Linear |gamma| (0 = perfect match, 1 = total reflection). Kept
    alongside return loss because the Smith-chart cell wants the linear
    form and the readout table wants the dB form.
    --------------------------------------------------------------------
    */
    fun reflectionMagnitude(point: SweepPoint): Double =
        OslCalibrationEngine.gammaFromPoint(point).magnitude

    /*
    --------------------------------------------------------------------
    Whole-row builder
    EDIT SECTION 1006
    --------------------------------------------------------------------
    One call per marker, returning both raw values (for charts) and
    display strings (for the table) so the Composable branches on
    nothing.
    --------------------------------------------------------------------
    */
    fun buildMarkerReadout(
        point: SweepPoint,
        region: IaruRegion = AmateurBandPlan.DEFAULT_REGION
    ): MarkerReadout {
        val q = qFactor(point)
        val series = seriesEquivalent(point)
        return MarkerReadout(
            frequencyMHz = point.frequencyMHz,
            swr = point.swr,
            resistanceOhms = point.resistance,
            reactanceOhms = point.reactance,
            impedanceMagnitudeOhms = impedanceMagnitudeOhms(point),
            qFactor = q,
            seriesEquivalent = series,
            returnLossDb = point.returnLossDb,
            reflectionMagnitude = reflectionMagnitude(point),
            phaseDegrees = reflectionPhaseDegrees(point),
            bandLabel = AmateurBandPlan.bandLabelAt(point.frequencyMHz, region),
            frequencyText = formatFrequencyMHz(point.frequencyMHz),
            swrText = formatSwr(point.swr),
            impedanceText = formatImpedance(point.resistance, point.reactance),
            impedanceMagnitudeText =
                formatOhms(impedanceMagnitudeOhms(point)),
            qText = formatQ(q),
            seriesEquivalentText = formatSeriesEquivalent(series),
            returnLossText = formatReturnLossDb(point.returnLossDb),
            phaseText = formatDegrees(reflectionPhaseDegrees(point))
        )
    }

    /*
    --------------------------------------------------------------------
    A/B -> labelled rows
    EDIT SECTION 1006b
    --------------------------------------------------------------------
    Pairs each PRESENT marker with ITS OWN label before the list is
    flattened, because the readout table labels positionally
    (MarkerReadoutTable: markerLabels.getOrElse(index)). A bare
    listOfNotNull(a, b) therefore renders a lone marker B as "Marker A" —
    an unplaced marker's index is -1, so getOrNull yields null and that
    case is reachable in normal use. Label and readout travel together
    from here on.
    --------------------------------------------------------------------
    */
    fun buildLabelledMarkerReadouts(
        markerAPoint: SweepPoint?,
        markerBPoint: SweepPoint?,
        region: IaruRegion = AmateurBandPlan.DEFAULT_REGION
    ): List<LabelledMarkerReadout> = buildList {
        markerAPoint?.let {
            add(LabelledMarkerReadout(MARKER_A_LABEL, buildMarkerReadout(it, region)))
        }
        markerBPoint?.let {
            add(LabelledMarkerReadout(MARKER_B_LABEL, buildMarkerReadout(it, region)))
        }
    }

    const val MARKER_A_LABEL = "Marker A"
    const val MARKER_B_LABEL = "Marker B"

    /*
    ====================================================================
    FORMATTERS
    EDIT SECTION 1007
    ====================================================================
    Locale-independent on purpose (Locale.ROOT): these strings are
    engineering notation shown next to a trace, and a comma decimal
    separator would misread against the axis labels.
    --------------------------------------------------------------------
    */

    fun formatFrequencyMHz(frequencyMHz: Double): String =
        String.format(java.util.Locale.ROOT, "%.3f MHz", frequencyMHz)

    fun formatSwr(swr: Double): String =
        String.format(java.util.Locale.ROOT, "%.2f", swr)

    fun formatOhms(ohms: Double): String =
        String.format(java.util.Locale.ROOT, "%.1f Ω", ohms)

    /*
    R + jX in the form operators read it: "50.0 + j50.0 Ω", with a minus
    sign folded into the j term rather than printed as "+ j-50.0".
    */
    fun formatImpedance(resistanceOhms: Double, reactanceOhms: Double): String {
        val sign = if (reactanceOhms < 0.0) "-" else "+"
        return String.format(
            java.util.Locale.ROOT,
            "%.1f %s j%.1f Ω",
            resistanceOhms,
            sign,
            abs(reactanceOhms)
        )
    }

    fun formatQ(q: Double?): String =
        if (q == null) "—" else String.format(java.util.Locale.ROOT, "%.2f", q)

    fun formatDegrees(degrees: Double): String =
        String.format(java.util.Locale.ROOT, "%.1f°", degrees)

    /*
    Return loss is quoted as a positive dB figure by convention. A
    perfect match produces a non-finite value upstream, which prints as
    an em dash rather than "Infinity".
    */
    fun formatReturnLossDb(returnLossDb: Double): String =
        if (!returnLossDb.isFinite()) {
            "—"
        } else {
            String.format(java.util.Locale.ROOT, "%.2f dB", abs(returnLossDb))
        }

    /*
    Auto-scaled so HF and UHF both read naturally: pF/nF, nH/uH.
    */
    fun formatSeriesEquivalent(series: SeriesEquivalent?): String =
        when (series) {
            null -> "—"
            is SeriesEquivalent.Capacitive -> {
                val pf = series.farads * 1e12
                if (pf < 1000.0) {
                    String.format(java.util.Locale.ROOT, "%.1f pF", pf)
                } else {
                    String.format(java.util.Locale.ROOT, "%.2f nF", pf / 1000.0)
                }
            }
            is SeriesEquivalent.Inductive -> {
                val nh = series.henries * 1e9
                if (nh < 1000.0) {
                    String.format(java.util.Locale.ROOT, "%.1f nH", nh)
                } else {
                    String.format(java.util.Locale.ROOT, "%.2f µH", nh / 1000.0)
                }
            }
        }
}

/*
--------------------------------------------------------------------
Series-equivalent component
EDIT SECTION 1008
--------------------------------------------------------------------
A sealed pair rather than a nullable-pF/nullable-nH struct, so a point
cannot claim to be capacitive and inductive at once.
--------------------------------------------------------------------
*/
sealed interface SeriesEquivalent {
    data class Capacitive(val farads: Double) : SeriesEquivalent
    data class Inductive(val henries: Double) : SeriesEquivalent
}

/*
--------------------------------------------------------------------
One marker row
EDIT SECTION 1009
--------------------------------------------------------------------
Raw values for charts, *Text values for the readout table.
--------------------------------------------------------------------
*/
/*
--------------------------------------------------------------------
A readout with the marker it belongs to
EDIT SECTION 1009b
--------------------------------------------------------------------
Keeps the name attached to the row so no downstream index arithmetic
can rename a marker. See buildLabelledMarkerReadouts.
--------------------------------------------------------------------
*/
data class LabelledMarkerReadout(
    val label: String,
    val readout: MarkerReadout
)

data class MarkerReadout(
    val frequencyMHz: Double,
    val swr: Double,
    val resistanceOhms: Double,
    val reactanceOhms: Double,
    val impedanceMagnitudeOhms: Double,
    val qFactor: Double?,
    val seriesEquivalent: SeriesEquivalent?,
    val returnLossDb: Double,
    val reflectionMagnitude: Double,
    val phaseDegrees: Double,
    val bandLabel: String,
    val frequencyText: String,
    val swrText: String,
    val impedanceText: String,
    val impedanceMagnitudeText: String,
    val qText: String,
    val seriesEquivalentText: String,
    val returnLossText: String,
    val phaseText: String
)
