package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.testing.InstrumentDataSourceKind

/*
########################################################################
FILE: SweepHardwareIdentity.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Sweep provenance

SYSTEM ROLE
Pure resolver for the hardware name PERSISTED with a sweep record.

WHY THIS EXISTS (§10c.7 — data integrity)
Real LiteVNA sweeps were saved as `Hardware: SIMULATED` and survived
save/reload, corrupting stored project data. Root cause: the LiteVNA
driver never set SweepResult.hardwareProfile, so it inherited the old
confident-wrong default "SIMULATED", and the save-path writer copied it
verbatim without ever consulting the live session that BenchState was
already reporting as REAL_INSTRUMENT.

The fix pairs a NEUTRAL model default (SweepResult.hardwareProfile = "")
with this resolver: a driver that named itself wins; otherwise the LIVE
data-source kind decides, so an unnamed sweep from a real instrument is
never persisted as simulated (and vice-versa). Driver-agnostic on
purpose — the next driver that forgets to identify itself is classified
correctly instead of silently lying.

This file is JVM-pure: no Android, no Compose, no IO.
########################################################################
*/
object SweepHardwareIdentity {

    /** Canonical simulated tag, as stamped by the simulated sweep path. */
    const val SIMULATED = "SIMULATED"

    /** Shown when a sweep is unnamed AND no live source can classify it. */
    const val UNKNOWN_INSTRUMENT = "Unknown Instrument"

    /** Generic real label used when a real source is live but unnamed. */
    const val REAL_INSTRUMENT = "Real Instrument"

    /**
     * True when [profile] carries no real driver identity. Treats null,
     * blank, and a literal "UNKNOWN" sentinel as unspecified so a future
     * driver can signal "not set" in either form.
     */
    fun isUnspecified(profile: String?): Boolean =
        profile.isNullOrBlank() || profile.trim().equals("UNKNOWN", ignoreCase = true)

    /**
     * The hardware name to PERSIST for a sweep record.
     *
     *  - A driver that named itself wins (NanoVNA tag, the sim path's
     *    explicit "SIMULATED", the debug-cal "SIMULATED_CAL_*"): all of
     *    these already match the data source they claim, so trusting them
     *    cannot mislabel.
     *  - Otherwise the sweep is unnamed, so the LIVE [liveDataSourceKind]
     *    classifies it. A real instrument gets its [liveHardwareName]
     *    (e.g. "LiteVNA64 v0.3.3"), falling back to a generic real label;
     *    a simulated session gets "SIMULATED"; nothing live yields a
     *    visible "Unknown Instrument" rather than a false claim.
     */
    fun resolvePersistedHardwareName(
        reportedProfile: String?,
        liveDataSourceKind: InstrumentDataSourceKind?,
        liveHardwareName: String?
    ): String = when {
        !isUnspecified(reportedProfile) -> reportedProfile!!.trim()
        liveDataSourceKind == InstrumentDataSourceKind.REAL_INSTRUMENT ->
            liveHardwareName?.trim()?.ifBlank { null } ?: REAL_INSTRUMENT
        liveDataSourceKind == InstrumentDataSourceKind.SIMULATED -> SIMULATED
        else -> UNKNOWN_INSTRUMENT
    }
}
