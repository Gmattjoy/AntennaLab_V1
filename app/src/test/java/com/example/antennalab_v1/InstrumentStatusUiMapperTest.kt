package com.example.antennalab_v1

import android.content.Context
import com.example.antennalab_v1.domain.testing.UsbSessionManager
import com.example.antennalab_v1.features.app.InstrumentStatusUiMapper
import com.example.antennalab_v1.model.HardwareCapabilityProfiles
import com.example.antennalab_v1.model.HardwareConnectionState
import com.example.antennalab_v1.model.UserHardwareConfig
import com.example.antennalab_v1.model.UsbConnectionInfo
import com.example.antennalab_v1.model.testing.CalibrationReadiness
import com.example.antennalab_v1.model.testing.InstrumentCalibrationState
import com.example.antennalab_v1.model.testing.InstrumentDataSourceKind
import com.example.antennalab_v1.model.testing.InstrumentSessionState
import com.example.antennalab_v1.model.testing.MeasurementTrustLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Coverage for [InstrumentStatusUiMapper] — the shared instrument-status
 * presentation layer. The per-field mapping functions (made `internal`) are
 * tested directly against constructed InstrumentSessionState values; the two
 * public entry points are smoke-tested through a real Context under Robolectric.
 *
 * UsbSessionManager is a process-wide singleton (buildOperatorInstrumentTitle /
 * buildSelectedProfileLabel read the selected driver profile), so its selection
 * state is reset before each test.
 */
@RunWith(RobolectricTestRunner::class)
class InstrumentStatusUiMapperTest {

    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    @Before
    fun resetSessionManager() {
        UsbSessionManager.clearSelectedHardwareConfig()
        UsbSessionManager.clearCalibrationState()
    }

    private fun state(
        selectedHardwareName: String = "LiteVNA64 v0.3.3",
        connectionState: HardwareConnectionState = HardwareConnectionState.READY,
        sessionOpen: Boolean = false,
        permissionGranted: Boolean = false,
        deviceName: String = "USB analyzer",
        transportReady: Boolean = false,
        trust: MeasurementTrustLevel = MeasurementTrustLevel.UNKNOWN,
        dataSourceKind: InstrumentDataSourceKind = InstrumentDataSourceKind.NONE,
        calibrationReadiness: CalibrationReadiness = CalibrationReadiness.NOT_STARTED,
        instrumentIdentityText: String? = null,
        protocolGuess: String = "Unknown",
        supportTier: String = "Unknown"
    ) = InstrumentSessionState(
        selectedHardwareName = selectedHardwareName,
        connectionInfo = UsbConnectionInfo(
            state = connectionState,
            deviceName = deviceName,
            permissionGranted = permissionGranted,
            sessionOpen = sessionOpen
        ),
        transportReady = transportReady,
        transportStatusSummary = "transport summary",
        protocolFamily = "TestFamily",
        protocolGuess = protocolGuess,
        supportTier = supportTier,
        capabilityProfile = HardwareCapabilityProfiles.VNA_STANDARD,
        measurementTrust = trust,
        calibrationState = InstrumentCalibrationState(readiness = calibrationReadiness),
        dataSourceKind = dataSourceKind,
        instrumentIdentityText = instrumentIdentityText,
        sessionSummary = "session summary"
    )

    // ------------------------------------------------------------------
    // Simple per-field label mappers (pure over InstrumentSessionState?)
    // ------------------------------------------------------------------

    @Test
    fun connectionLabel_reflectsSessionThenPermissionThenRawState() {
        assertEquals("Disconnected", InstrumentStatusUiMapper.buildConnectionLabel(null))
        assertEquals("Session Open", InstrumentStatusUiMapper.buildConnectionLabel(state(sessionOpen = true)))
        assertEquals(
            "Detected",
            InstrumentStatusUiMapper.buildConnectionLabel(state(sessionOpen = false, permissionGranted = true))
        )
        assertEquals(
            "PERMISSION REQUIRED",
            InstrumentStatusUiMapper.buildConnectionLabel(
                state(connectionState = HardwareConnectionState.PERMISSION_REQUIRED)
            )
        )
    }

    @Test
    fun transportLabel_readyOnlyWhenTransportReady() {
        assertEquals("Ready", InstrumentStatusUiMapper.buildTransportLabel(state(transportReady = true)))
        assertEquals("Not Ready", InstrumentStatusUiMapper.buildTransportLabel(state(transportReady = false)))
        assertEquals("Not Ready", InstrumentStatusUiMapper.buildTransportLabel(null))
    }

    @Test
    fun pathLabel_mapsDataSourceKind() {
        assertEquals("Real Instrument", InstrumentStatusUiMapper.buildPathLabel(state(dataSourceKind = InstrumentDataSourceKind.REAL_INSTRUMENT)))
        assertEquals("Simulated", InstrumentStatusUiMapper.buildPathLabel(state(dataSourceKind = InstrumentDataSourceKind.SIMULATED)))
        assertEquals("No Instrument", InstrumentStatusUiMapper.buildPathLabel(state(dataSourceKind = InstrumentDataSourceKind.NONE)))
        assertEquals("No Instrument", InstrumentStatusUiMapper.buildPathLabel(null))
    }

    @Test
    fun trustLabel_mapsEveryLevelAndNull() {
        assertEquals("Trusted", InstrumentStatusUiMapper.buildTrustLabel(state(trust = MeasurementTrustLevel.TRUSTED)))
        assertEquals("Degraded", InstrumentStatusUiMapper.buildTrustLabel(state(trust = MeasurementTrustLevel.DEGRADED)))
        assertEquals("Partial", InstrumentStatusUiMapper.buildTrustLabel(state(trust = MeasurementTrustLevel.PARTIAL)))
        assertEquals("Simulated", InstrumentStatusUiMapper.buildTrustLabel(state(trust = MeasurementTrustLevel.SIMULATED)))
        assertEquals("Unknown", InstrumentStatusUiMapper.buildTrustLabel(state(trust = MeasurementTrustLevel.UNKNOWN)))
        assertEquals("Unknown", InstrumentStatusUiMapper.buildTrustLabel(null))
    }

    @Test
    fun calibrationLabel_prettyPrintsReadinessOrDefaults() {
        assertEquals("Not started", InstrumentStatusUiMapper.buildCalibrationLabel(state(calibrationReadiness = CalibrationReadiness.NOT_STARTED)))
        assertEquals("In progress", InstrumentStatusUiMapper.buildCalibrationLabel(state(calibrationReadiness = CalibrationReadiness.IN_PROGRESS)))
        assertEquals("Valid", InstrumentStatusUiMapper.buildCalibrationLabel(state(calibrationReadiness = CalibrationReadiness.VALID)))
        assertEquals("Not Started", InstrumentStatusUiMapper.buildCalibrationLabel(null))
    }

    @Test
    fun statusLabel_reflectsReadinessLadder() {
        assertEquals("Unavailable", InstrumentStatusUiMapper.buildStatusLabel(null))
        assertEquals(
            "Live Ready",
            InstrumentStatusUiMapper.buildStatusLabel(state(transportReady = true, dataSourceKind = InstrumentDataSourceKind.REAL_INSTRUMENT))
        )
        assertEquals("Transport Ready", InstrumentStatusUiMapper.buildStatusLabel(state(transportReady = true)))
        assertEquals("Session Open", InstrumentStatusUiMapper.buildStatusLabel(state(sessionOpen = true)))
        assertEquals("Detected", InstrumentStatusUiMapper.buildStatusLabel(state(permissionGranted = true)))
        assertEquals("Disconnected", InstrumentStatusUiMapper.buildStatusLabel(state()))
    }

    @Test
    fun subtitle_and_operatorSummary_reflectReadiness() {
        assertEquals("No active instrument session.", InstrumentStatusUiMapper.buildInstrumentSubtitle(null))
        assertEquals(
            "Live transport path is ready.",
            InstrumentStatusUiMapper.buildInstrumentSubtitle(state(transportReady = true, dataSourceKind = InstrumentDataSourceKind.REAL_INSTRUMENT))
        )
        assertEquals("Open a device session to begin.", InstrumentStatusUiMapper.buildInstrumentSubtitle(state()))

        assertEquals("No instrument session state is available.", InstrumentStatusUiMapper.buildOperatorSummary(null))
        assertEquals(
            "Instrument is ready for live measurement.",
            InstrumentStatusUiMapper.buildOperatorSummary(state(transportReady = true, dataSourceKind = InstrumentDataSourceKind.REAL_INSTRUMENT))
        )
        // Falls through to the transport status summary when nothing else matches.
        assertEquals("transport summary", InstrumentStatusUiMapper.buildOperatorSummary(state()))
    }

    @Test
    fun technicalIdentityLabel_prefersIdentityThenDeviceName() {
        assertEquals("No technical identity available.", InstrumentStatusUiMapper.buildTechnicalIdentityLabel(null))
        assertEquals("LiteVNA #42", InstrumentStatusUiMapper.buildTechnicalIdentityLabel(state(instrumentIdentityText = "LiteVNA #42")))
        assertEquals(
            "USB analyzer",
            InstrumentStatusUiMapper.buildTechnicalIdentityLabel(state(instrumentIdentityText = null, deviceName = "USB analyzer"))
        )
    }

    // ------------------------------------------------------------------
    // Title / profile mappers (read the selected driver profile singleton)
    // ------------------------------------------------------------------

    @Test
    fun operatorInstrumentTitle_usesSelectedHardwareNameWhenNotNanoAndNoLiteProfile() {
        // No profile selected (reset), so it falls to the passed hardware name.
        assertEquals(
            "LiteVNA64 v0.3.3",
            InstrumentStatusUiMapper.buildOperatorInstrumentTitle(
                state = null,
                selectedHardwareName = "LiteVNA64 v0.3.3"
            )
        )
        // A "nano" name is skipped in favour of state's non-nano hardware name.
        assertEquals(
            "LiteVNA64 v0.3.3",
            InstrumentStatusUiMapper.buildOperatorInstrumentTitle(
                state = state(selectedHardwareName = "LiteVNA64 v0.3.3"),
                selectedHardwareName = "NanoVNA-H4"
            )
        )
    }

    @Test
    fun operatorInstrumentTitle_prefersLiteProfileWhenSelected() {
        UsbSessionManager.registerSelectedHardwareConfig(
            UserHardwareConfig(selectedDriverProfileId = "litevna64_standard")
        )

        val title = InstrumentStatusUiMapper.buildOperatorInstrumentTitle(
            state = null,
            selectedHardwareName = "NanoVNA-H4"
        )
        assertTrue(title.contains("Lite", ignoreCase = true))
    }

    @Test
    fun selectedProfileLabel_dependsOnSelectedProfile() {
        assertEquals("No profile selected", InstrumentStatusUiMapper.buildSelectedProfileLabel(null))

        val lite = com.example.antennalab_v1.domain.testing.DriverProfileRegistry.getProfileById("litevna64_standard")
        assertEquals("LiteVNA Profile", InstrumentStatusUiMapper.buildSelectedProfileLabel(lite))

        val nano = com.example.antennalab_v1.domain.testing.DriverProfileRegistry.getProfileById("nano_h_default")
        assertEquals(nano!!.displayName, InstrumentStatusUiMapper.buildSelectedProfileLabel(nano))
    }

    // ------------------------------------------------------------------
    // Public entry points (Context-coupled) — smoke test through Robolectric
    // ------------------------------------------------------------------

    @Test
    fun buildCardUiModel_wiresUpAndUsesSelectedHardwareName() {
        val model = InstrumentStatusUiMapper.buildCardUiModel(context, "LiteVNA64 v0.3.3")

        assertEquals("LiteVNA64 v0.3.3", model.title) // no lite profile selected, non-nano name
        assertEquals("Not Ready", model.transportLabel) // no hardware attached under Robolectric
        assertEquals("Open Instrument Details", model.detailsButtonLabel)
        assertTrue(model.statusSummary.isNotBlank())
    }

    @Test
    fun buildDetailsUiModel_wiresUpWithDefaultsWhenNoProfileSelected() {
        val model = InstrumentStatusUiMapper.buildDetailsUiModel(context, "LiteVNA64 v0.3.3")

        assertEquals("Instrument Details", model.headerTitle)
        assertEquals("LiteVNA64 v0.3.3", model.instrumentTitle)
        assertEquals("No profile selected", model.selectedProfileLabel)
        assertTrue(model.liteVnaDebugSummary.isNotBlank())
    }
}
