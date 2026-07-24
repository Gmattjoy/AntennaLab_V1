package com.example.antennalab_v1.features.app

/*
########################################################################
FILE: BenchStateLog.kt
PACKAGE: com.example.antennalab_v1.features.app
LAYER: UI / App / Bench Diagnostics (pure formatters)

SYSTEM ROLE
Formats the operator-facing instrument/sweep state into ONE greppable
line per state change, so a bench session can be verified from
`adb logcat -s BenchState` instead of by reading fields off the tablet.

WHY
This state lives only in Compose and never reached logcat, so every
verdict needed a photo or a transcription. That round trip already
produced one misreported field and a false-start "pass" that cost ~35
minutes of bench time on 2026-07-24. The CalRestore line proved the
pattern: one DEBUG line turns an unobservable decision into evidence.

PURITY
No Compose, no Android, no session-singleton reach-through — the caller
passes everything in. android.util.Log must stay out of this file:
returnDefaultValues is NOT enabled (app/build.gradle.kts), so a Log call
here would throw "not mocked" in the plain-JVM tests. The Compose layer
supplies the sink and gates it on BuildConfig.DEBUG.

EMISSION
Call sites wrap the sink in LaunchedEffect(line), which fires exactly
once per DISTINCT line and never on mere recomposition.
########################################################################
*/

import com.example.antennalab_v1.model.TestHardwareProfile
import com.example.antennalab_v1.model.testing.InstrumentSessionState

object BenchStateLog {

    /*
    ------------------------------------------------------------
    INSTRUMENT STATE (Connections / Devices)
    ------------------------------------------------------------
    `supportTier` here is the RESOLVED InstrumentSessionState value — the
    one buildSweepRunContract gates a live sweep on, and deliberately NOT
    the static registry tier the Device Model card shows. Those two
    disagreeing is the documented §7.2 coupling; logging the resolved one
    makes the difference checkable at a glance.

    `effective` is the EffectiveHardwareResolver output. A1's precondition
    is "stale-NanoVNA project + live LiteVNA", and until now the only
    evidence of it was the CalRestore line at project load; having it here
    makes the precondition checkable at any moment.
    ------------------------------------------------------------
    */
    fun buildInstrumentLine(
        state: InstrumentSessionState?,
        card: InstrumentStatusCardUiModel,
        validationLabel: String,
        effectiveHardware: TestHardwareProfile
    ): String {
        if (state == null) {
            return "benchState scope=instrument state=none " +
                "status='${card.statusLabel}' validation='$validationLabel' " +
                "effective=$effectiveHardware"
        }

        return "benchState scope=instrument " +
            "status='${card.statusLabel}' " +
            "conn='${card.connectionLabel}' " +
            "transport='${card.transportLabel}' " +
            "path='${card.pathLabel}' " +
            "trust='${card.trustLabel}' " +
            "cal='${card.calibrationLabel}' " +
            "validation='$validationLabel' " +
            "supportTier='${state.supportTier}' " +
            "dataSource=${state.dataSourceKind} " +
            "effective=$effectiveHardware " +
            "sessionOpen=${state.connectionInfo.sessionOpen} " +
            "permission=${state.connectionInfo.permissionGranted} " +
            "transportReady=${state.transportReady}"
    }

    /*
    ------------------------------------------------------------
    SWEEP RUN CONTRACT (Sweep Viewer)
    ------------------------------------------------------------
    Primitives rather than SweepRunContract so features/app does not have
    to import features/testing. `runButton`/`enabled` are the real unblock
    signal: "Run Live Sweep" enabled means the run contract resolved a
    live path, which "Run Demo Sweep" or "Run Sweep Locked" does not.
    ------------------------------------------------------------
    */
    fun buildSweepLine(
        runButtonText: String,
        runEnabled: Boolean,
        runUsesRealInstrument: Boolean,
        runUsesSimulation: Boolean,
        calibrationStateLabel: String,
        trustDowngraded: Boolean,
        blockReason: String?,
        effectiveHardware: TestHardwareProfile
    ): String {
        return "benchState scope=sweep " +
            "runButton='$runButtonText' " +
            "enabled=$runEnabled " +
            "usesReal=$runUsesRealInstrument " +
            "usesSim=$runUsesSimulation " +
            "cal='$calibrationStateLabel' " +
            "trustDowngraded=$trustDowngraded " +
            "effective=$effectiveHardware " +
            "block=${blockReason ?: "none"}"
    }
}
