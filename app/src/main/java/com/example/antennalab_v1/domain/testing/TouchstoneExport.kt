package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.testing.SweepResult
import java.util.Locale

/*
########################################################################
FILE: TouchstoneExport.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Export

SYSTEM ROLE
Renders a SweepResult as Touchstone .s1p text (UI redesign spec §2.3).
Pure string building — no file IO, no Android refs. Whatever surface
delivers the bytes (share sheet, preview panel, project storage) calls
this and owns the IO.

WHY S11 ONLY — NOT A LIMITATION WE CHOSE
.s1p is by definition a ONE-port Touchstone file: one frequency column
plus one S-parameter pair. It cannot carry S21. Carrying S21 would mean
.s2p, which requires the full 2x2 set (S11 S21 S12 S22); both supported
devices measure forward S21 only, with no S12/S22, so a valid .s2p is
not producible without fabricating terms. Spec open question #5 is
therefore closed by the file format, not by a product decision.

FORMAT CHOICE
Header `# Hz S RI R 50` — frequency in Hz, S-parameters, real/imaginary
pairs, 50 ohm reference. This is NanoVNA-Saver's own convention, chosen
under spec §2.3 (respect VNA muscle memory) so files interchange with
the tool these users already run.

S11 comes from OslCalibrationEngine.gammaFromPoint — the same exact
R/X -> gamma reconstruction CalibrationCorrector uses, so an exported
file carries calibrated values whenever the sweep was calibrated.
########################################################################
*/
object TouchstoneExport {

    /*
    --------------------------------------------------------------------
    Option line
    EDIT SECTION 1001
    --------------------------------------------------------------------
    */
    const val OPTION_LINE: String = "# Hz S RI R 50"

    const val FILE_EXTENSION: String = "s1p"

    private const val HZ_PER_MHZ = 1_000_000.0

    /*
    --------------------------------------------------------------------
    Whole-file builder
    EDIT SECTION 1002
    --------------------------------------------------------------------
    Emits, in order:
      - provenance comments (! lines) — instrument, span, calibration
        state, and any incomplete-sweep count, so a file that left the
        app can still be audited later
      - the option line
      - a column-header comment
      - one row per point: frequency_Hz  Re(S11)  Im(S11)

    Line endings are CRLF: Touchstone is a DOS-era format and some
    analysers still parse it strictly.

    An empty sweep still produces a valid header-only file rather than
    an exception — flag-don't-reject, consistent with the rest of the
    sweep pipeline.
    --------------------------------------------------------------------
    */
    fun buildS1p(result: SweepResult): String {
        val lines = mutableListOf<String>()

        lines += "! AntennaLab V1 — Touchstone S11 export"
        lines += "! Instrument: ${instrumentLabel(result)}"
        lines += "! Span: ${formatMhz(result.startFrequencyMHz)} MHz to " +
            "${formatMhz(result.endFrequencyMHz)} MHz"
        lines += "! Points: ${result.points.size}"
        lines += "! Calibration: ${calibrationLabel(result)}"

        if (!result.isComplete) {
            lines += "! WARNING: incomplete sweep — " +
                "${result.actualPointCount} of ${result.requestedPointCount} " +
                "points captured"
        }

        lines += OPTION_LINE
        lines += "! freq(Hz)          Re(S11)        Im(S11)"

        result.points.forEach { point ->
            val gamma = OslCalibrationEngine.gammaFromPoint(point)
            lines += String.format(
                Locale.ROOT,
                "%-18s %-14s %s",
                formatHz(point.frequencyMHz),
                formatComponent(gamma.re),
                formatComponent(gamma.im)
            ).trimEnd()
        }

        return lines.joinToString(separator = "\r\n", postfix = "\r\n")
    }

    /*
    --------------------------------------------------------------------
    Suggested filename
    EDIT SECTION 1003
    --------------------------------------------------------------------
    Caller supplies the project name and a timestamp — this object stays
    pure, so it does NOT read the clock.

    Sanitises to a conservative set safe on external storage and in a
    share-sheet target; an empty or fully-stripped name falls back to
    "sweep" rather than producing a dotfile.
    --------------------------------------------------------------------
    */
    fun suggestFileName(projectName: String, timestampLabel: String): String {
        val safeProject = sanitise(projectName).ifBlank { "sweep" }
        val safeStamp = sanitise(timestampLabel)
        val stem = if (safeStamp.isBlank()) safeProject else "${safeProject}_$safeStamp"
        return "$stem.$FILE_EXTENSION"
    }

    private fun sanitise(raw: String): String =
        raw.trim()
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .trim('_')

    private fun instrumentLabel(result: SweepResult): String =
        result.hardwareProfile.ifBlank { "unknown" }

    private fun calibrationLabel(result: SweepResult): String =
        if (result.isCalibrated) {
            result.calibrationLabel.ifBlank { "applied" }
        } else {
            "none (uncalibrated)"
        }

    /*
    Frequency as whole Hz. Touchstone frequency columns are conveniently
    integral for MHz-domain sweeps, and printing 14200000 rather than
    1.42E7 keeps the file diff-friendly and human-readable.
    */
    private fun formatHz(frequencyMHz: Double): String =
        String.format(Locale.ROOT, "%.0f", frequencyMHz * HZ_PER_MHZ)

    /*
    Six decimals is well inside the noise floor of either device and is
    what NanoVNA-Saver writes.
    */
    private fun formatComponent(value: Double): String =
        String.format(Locale.ROOT, "%.6f", value)

    private fun formatMhz(value: Double): String =
        String.format(Locale.ROOT, "%.3f", value)
}
