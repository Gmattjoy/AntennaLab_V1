package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.testing.SweepResult
import java.util.Locale

/*
########################################################################
FILE: SweepExportNaming.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Export

SYSTEM ROLE
The single home for export filename derivation. Pure string work — no
Android refs, no file IO, and deliberately NO clock read: the caller
passes a timestamp label, so the same inputs always produce the same
name and the helper stays unit-testable.

SHAPE
    <project>_<centre frequency>_<timestamp>.<ext>
    Dipole20m_14.200MHz_2026-07-30T1815.s1p

Any segment that sanitises away is dropped rather than leaving a doubled
separator, and a fully-stripped project name falls back to "sweep" so
the result can never be a dotfile.

TouchstoneExport.suggestFileName delegates here, so there is one naming
implementation rather than two that drift.
########################################################################
*/
object SweepExportNaming {

    /*
    Conservative allow-list: safe on FAT32/exFAT external storage, in a
    MediaStore DISPLAY_NAME, and as a share-sheet attachment name.
    */
    private val UNSAFE_CHARACTERS = Regex("[^A-Za-z0-9._-]+")

    /*
    Filesystem limits are usually 255 bytes; 120 leaves generous room for
    the extension and any collision suffix.
    */
    private const val MAX_STEM_LENGTH = 120

    private const val FALLBACK_STEM = "sweep"

    /*
    --------------------------------------------------------------------
    Build a name from a sweep
    EDIT SECTION 1001
    --------------------------------------------------------------------
    Centre frequency is (start + end) / 2 — the middle of what was
    actually swept, not the start edge, so a name identifies the sweep
    rather than its lower bound.
    --------------------------------------------------------------------
    */
    fun buildFileName(
        projectName: String,
        result: SweepResult,
        timestampLabel: String,
        extension: String = TouchstoneExport.FILE_EXTENSION
    ): String = buildFileName(
        projectName = projectName,
        centreFrequencyMHz = centreFrequencyMHz(result),
        timestampLabel = timestampLabel,
        extension = extension
    )

    /*
    --------------------------------------------------------------------
    Build a name from explicit parts
    EDIT SECTION 1002
    --------------------------------------------------------------------
    A non-positive centre frequency (an empty sweep) drops the frequency
    segment instead of writing "0.000MHz".
    --------------------------------------------------------------------
    */
    fun buildFileName(
        projectName: String,
        centreFrequencyMHz: Double,
        timestampLabel: String,
        extension: String = TouchstoneExport.FILE_EXTENSION
    ): String {
        val segments = mutableListOf<String>()

        segments += sanitiseSegment(stripExtension(projectName, extension))
            .ifBlank { FALLBACK_STEM }

        if (centreFrequencyMHz > 0.0) {
            segments += formatFrequencySegment(centreFrequencyMHz)
        }

        val stamp = sanitiseSegment(timestampLabel)
        if (stamp.isNotBlank()) {
            segments += stamp
        }

        val stem = segments
            .filter { it.isNotBlank() }
            .joinToString(separator = "_")
            .take(MAX_STEM_LENGTH)
            .trim('_')
            .ifBlank { FALLBACK_STEM }

        return "$stem.$extension"
    }

    /*
    --------------------------------------------------------------------
    Centre of a swept span
    EDIT SECTION 1003
    --------------------------------------------------------------------
    */
    fun centreFrequencyMHz(result: SweepResult): Double =
        (result.startFrequencyMHz + result.endFrequencyMHz) / 2.0

    /*
    --------------------------------------------------------------------
    Collision avoidance
    EDIT SECTION 1004
    --------------------------------------------------------------------
    Returns `candidate` when free, else the first "<stem>_N.<ext>" that
    is not in `existingNames` (starting at 2, so the first duplicate
    reads as the second file).

    Needed for the app-specific-directory path, where we own the
    directory and must not silently overwrite. MediaStore uniquifies its
    own inserts on API 29+, so that path does not call this.
    --------------------------------------------------------------------
    */
    fun nextAvailableName(candidate: String, existingNames: Set<String>): String {
        if (candidate !in existingNames) return candidate

        val dotIndex = candidate.lastIndexOf('.')
        val stem = if (dotIndex > 0) candidate.substring(0, dotIndex) else candidate
        val suffix = if (dotIndex > 0) candidate.substring(dotIndex) else ""

        var counter = 2
        while (true) {
            val next = "${stem}_$counter$suffix"
            if (next !in existingNames) return next
            counter++
        }
    }

    /*
    --------------------------------------------------------------------
    Segment sanitiser
    EDIT SECTION 1005
    --------------------------------------------------------------------
    Runs of unsafe characters collapse to a single underscore, and no
    leading or trailing underscore survives. Non-ASCII (e.g. a CJK
    project name) reduces away entirely, which is why the caller applies
    the fallback.
    --------------------------------------------------------------------
    */
    fun sanitiseSegment(raw: String): String =
        raw.trim()
            .replace(UNSAFE_CHARACTERS, "_")
            .trim('_')

    /*
    Locale.ROOT so a comma-decimal locale cannot produce "14,200MHz",
    which would read as a thousands separator in a filename.
    */
    private fun formatFrequencySegment(frequencyMHz: Double): String =
        String.format(Locale.ROOT, "%.3fMHz", frequencyMHz)

    /*
    Drops one trailing ".<ext>" (case-insensitive) so a project literally
    named "dipole.s1p" does not become "dipole.s1p_....s1p".
    */
    private fun stripExtension(raw: String, extension: String): String {
        val suffix = ".$extension"
        return if (raw.trim().endsWith(suffix, ignoreCase = true)) {
            raw.trim().dropLast(suffix.length)
        } else {
            raw
        }
    }
}
