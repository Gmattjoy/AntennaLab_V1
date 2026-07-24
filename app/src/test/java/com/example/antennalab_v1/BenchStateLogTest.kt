package com.example.antennalab_v1

import com.example.antennalab_v1.features.app.BenchStateLog
import com.example.antennalab_v1.features.app.InstrumentStatusCardUiModel
import com.example.antennalab_v1.model.HardwareCapabilityProfiles
import com.example.antennalab_v1.model.HardwareConnectionState
import com.example.antennalab_v1.model.TestHardwareProfile
import com.example.antennalab_v1.model.UsbConnectionInfo
import com.example.antennalab_v1.model.testing.InstrumentDataSourceKind
import com.example.antennalab_v1.model.testing.InstrumentSessionState
import com.example.antennalab_v1.model.testing.MeasurementTrustLevel
import org.junit.Assert.assertTrue
import org.junit.Test

/*
Pure coverage for the BenchState diagnostic line (tag BenchState).

These exist because the line IS the bench protocol: for the rest of a hardware
session, verdicts are read from `adb logcat -s BenchState` rather than transcribed
off the tablet. A field silently missing from the line would mean a verdict recorded
from an incomplete picture, which is exactly the failure this replaced.

Plain JVM — BenchStateLog holds no Android/Compose references, so android.util.Log
never enters a unit-tested path (returnDefaultValues is not enabled).
*/
class BenchStateLogTest {

    private val card = InstrumentStatusCardUiModel(
        connectionLabel = "Session Open",
        transportLabel = "Ready",
        pathLabel = "Real Instrument",
        trustLabel = "Degraded",
        calibrationLabel = "NOT_STARTED",
        statusLabel = "LIVE READY"
    )

    private fun sessionState(
        supportTier: String = "Partial Support",
        dataSourceKind: InstrumentDataSourceKind = InstrumentDataSourceKind.REAL_INSTRUMENT
    ) = InstrumentSessionState(
        selectedHardwareName = "LiteVNA64 v0.3.3",
        connectionInfo = UsbConnectionInfo(
            state = HardwareConnectionState.READY,
            sessionOpen = true,
            permissionGranted = true
        ),
        transportReady = true,
        transportStatusSummary = "ready",
        protocolFamily = "LiteVNA",
        supportTier = supportTier,
        capabilityProfile = HardwareCapabilityProfiles.VNA_STANDARD,
        measurementTrust = MeasurementTrustLevel.DEGRADED,
        dataSourceKind = dataSourceKind,
        sessionSummary = "summary"
    )

    @Test
    fun instrumentLine_carriesEveryOperatorFacingField() {
        val line = BenchStateLog.buildInstrumentLine(
            state = sessionState(),
            card = card,
            validationLabel = "Passed",
            effectiveHardware = TestHardwareProfile.LITEVNA64_V0_3_3
        )

        assertTrue(line, line.startsWith("benchState scope=instrument"))
        assertTrue(line, line.contains("status='LIVE READY'"))
        assertTrue(line, line.contains("conn='Session Open'"))
        assertTrue(line, line.contains("transport='Ready'"))
        assertTrue(line, line.contains("path='Real Instrument'"))
        assertTrue(line, line.contains("trust='Degraded'"))
        assertTrue(line, line.contains("cal='NOT_STARTED'"))
        assertTrue(line, line.contains("validation='Passed'"))
        assertTrue(line, line.contains("dataSource=REAL_INSTRUMENT"))
        assertTrue(line, line.contains("sessionOpen=true"))
        assertTrue(line, line.contains("permission=true"))
        assertTrue(line, line.contains("transportReady=true"))
        // One line, or it isn't greppable.
        assertTrue(line, !line.contains("\n"))
    }

    /*
    The §7.2 distinction this log exists to expose: supportTier must be the RESOLVED
    session value that buildSweepRunContract gates on, never the static registry tier
    the Device Model card renders. Logging the wrong one would make the log agree with
    the card and hide the coupling.
    */
    @Test
    fun instrumentLine_logsResolvedSupportTierNotRegistryTier() {
        val line = BenchStateLog.buildInstrumentLine(
            state = sessionState(supportTier = "Partial Support"),
            card = card,
            validationLabel = "Passed",
            effectiveHardware = TestHardwareProfile.LITEVNA64_V0_3_3
        )

        assertTrue(line, line.contains("supportTier='Partial Support'"))
        assertTrue(line, !line.contains("EXPERIMENTAL"))
    }

    // A1's precondition (stale-NanoVNA project + live LiteVNA) must be checkable here.
    @Test
    fun instrumentLine_carriesEffectiveResolvedHardware() {
        val lite = BenchStateLog.buildInstrumentLine(
            state = sessionState(), card = card, validationLabel = "Passed",
            effectiveHardware = TestHardwareProfile.LITEVNA64_V0_3_3
        )
        assertTrue(lite, lite.contains("effective=LITEVNA64_V0_3_3"))

        val nano = BenchStateLog.buildInstrumentLine(
            state = sessionState(), card = card, validationLabel = "Passed",
            effectiveHardware = TestHardwareProfile.NANOVNA_H4
        )
        assertTrue(nano, nano.contains("effective=NANOVNA_H4"))
    }

    @Test
    fun instrumentLine_survivesNullSessionState() {
        val line = BenchStateLog.buildInstrumentLine(
            state = null,
            card = InstrumentStatusCardUiModel(),
            validationLabel = "Pending",
            effectiveHardware = TestHardwareProfile.NANOVNA_H4
        )

        assertTrue(line, line.contains("state=none"))
        assertTrue(line, line.contains("validation='Pending'"))
        assertTrue(line, !line.contains("\n"))
    }

    @Test
    fun sweepLine_carriesRunContractAndDistinguishesLiveFromDemo() {
        val live = BenchStateLog.buildSweepLine(
            runButtonText = "Run Live Sweep",
            runEnabled = true,
            runUsesRealInstrument = true,
            runUsesSimulation = false,
            calibrationStateLabel = "Not Started",
            trustDowngraded = true,
            blockReason = null,
            effectiveHardware = TestHardwareProfile.LITEVNA64_V0_3_3
        )

        assertTrue(live, live.startsWith("benchState scope=sweep"))
        assertTrue(live, live.contains("runButton='Run Live Sweep'"))
        assertTrue(live, live.contains("enabled=true"))
        assertTrue(live, live.contains("usesReal=true"))
        assertTrue(live, live.contains("usesSim=false"))
        assertTrue(live, live.contains("trustDowngraded=true"))
        assertTrue(live, live.contains("effective=LITEVNA64_V0_3_3"))
        assertTrue(live, live.contains("block=none"))

        val demo = BenchStateLog.buildSweepLine(
            runButtonText = "Run Demo Sweep",
            runEnabled = true,
            runUsesRealInstrument = false,
            runUsesSimulation = true,
            calibrationStateLabel = "Not Started",
            trustDowngraded = false,
            blockReason = "Real transport is not ready. Demo sweep is available instead.",
            effectiveHardware = TestHardwareProfile.NANOVNA_H4
        )

        assertTrue(demo, demo.contains("runButton='Run Demo Sweep'"))
        assertTrue(demo, demo.contains("usesReal=false"))
        assertTrue(demo, demo.contains("usesSim=true"))
        assertTrue(demo, demo.contains("block=Real transport is not ready."))
    }
}
