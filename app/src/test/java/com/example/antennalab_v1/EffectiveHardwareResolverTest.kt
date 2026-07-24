package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.EffectiveHardwareResolver
import com.example.antennalab_v1.model.HardwareModel
import com.example.antennalab_v1.model.TestHardwareProfile
import com.example.antennalab_v1.model.testing.InstrumentDataSourceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/*
Pure JVM tests for the SINGLE effective-hardware resolution point.

Covers the full three-tier precedence matrix, the HardwareModel mapping, the
frequency clamp / capability resolution that follows from it, and the fact that
"is LiteVNA" is DERIVED from the same resolution rather than being a parallel rule.
*/
class EffectiveHardwareResolverTest {

    private val nano = TestHardwareProfile.NANOVNA_H4
    private val lite = TestHardwareProfile.LITEVNA64_V0_3_3

    private fun resolve(
        kind: InstrumentDataSourceKind?,
        model: HardwareModel?,
        sessionOpen: Boolean,
        project: TestHardwareProfile?
    ) = EffectiveHardwareResolver.resolveEffectiveHardware(
        liveDataSourceKind = kind,
        liveSelectedModel = model,
        liveSessionOpen = sessionOpen,
        projectHardware = project
    )

    /*
    ------------------------------------------------------------
    TIER 1 — a live VALIDATED instrument wins
    ------------------------------------------------------------
    */

    // THE BUG THIS WHOLE CHANGE EXISTS FOR: a validated LiteVNA on a project whose
    // design-time profile is the NanoVNA default must resolve as a LiteVNA.
    @Test
    fun validatedLiteVna_beatsStaleNanoVnaProjectDefault() {
        assertEquals(
            lite,
            resolve(InstrumentDataSourceKind.REAL_INSTRUMENT, HardwareModel.LITE_VNA_64, true, nano)
        )
    }

    @Test
    fun validatedInstrumentWinsInBothDirections() {
        assertEquals(
            nano,
            resolve(InstrumentDataSourceKind.REAL_INSTRUMENT, HardwareModel.NANO_VNA_H4, true, lite)
        )
    }

    @Test
    fun validatedButUnmappableModel_fallsBackToProject() {
        assertEquals(
            nano,
            resolve(InstrumentDataSourceKind.REAL_INSTRUMENT, HardwareModel.UNKNOWN, true, nano)
        )
        assertEquals(
            lite,
            resolve(InstrumentDataSourceKind.REAL_INSTRUMENT, null, true, lite)
        )
    }

    /*
    ------------------------------------------------------------
    TIER 2 — selected + session open, not yet validated
    ------------------------------------------------------------
    This tier is deliberate: bring-up takes ~15 s and that is exactly the window an
    operator sits in while running Validate. Without it, the shipped sweep-width fix
    would silently regress to NanoVNA's +/-0.25 MHz mid-validation.
    */

    @Test
    fun selectedAndSessionOpen_notYetValidated_stillWins() {
        assertEquals(
            lite,
            resolve(InstrumentDataSourceKind.SIMULATED, HardwareModel.LITE_VNA_64, true, nano)
        )
    }

    // INTENTIONAL NARROWING vs the previous behaviour: a merely-selected profile with
    // NO open session no longer wins. Offline/simulated review keeps the project's
    // own configuration.
    @Test
    fun selectedButNoSession_fallsBackToProject() {
        assertEquals(
            nano,
            resolve(InstrumentDataSourceKind.SIMULATED, HardwareModel.LITE_VNA_64, false, nano)
        )
    }

    /*
    ------------------------------------------------------------
    TIER 3 / 4 — project profile, then deterministic default
    ------------------------------------------------------------
    */

    @Test
    fun noLiveDevice_keepsProjectProfile() {
        assertEquals(lite, resolve(InstrumentDataSourceKind.NONE, null, false, lite))
        assertEquals(lite, resolve(null, null, false, lite))
        assertEquals(nano, resolve(null, null, false, nano))
    }

    @Test
    fun nothingAtAll_usesDeterministicDefault() {
        assertEquals(nano, resolve(null, null, false, null))
    }

    // No behaviour change when the project profile and the live instrument agree.
    @Test
    fun agreeingSources_resolveToThatHardware() {
        assertEquals(
            lite,
            resolve(InstrumentDataSourceKind.REAL_INSTRUMENT, HardwareModel.LITE_VNA_64, true, lite)
        )
        assertEquals(
            nano,
            resolve(InstrumentDataSourceKind.REAL_INSTRUMENT, HardwareModel.NANO_VNA_H4, true, nano)
        )
    }

    /*
    ------------------------------------------------------------
    MODEL MAPPING
    ------------------------------------------------------------
    */

    @Test
    fun mapHardwareModel_mapsBothLiteModelsAndBothNanoModels() {
        assertEquals(nano, EffectiveHardwareResolver.mapHardwareModel(HardwareModel.NANO_VNA_H))
        assertEquals(nano, EffectiveHardwareResolver.mapHardwareModel(HardwareModel.NANO_VNA_H4))
        assertEquals(lite, EffectiveHardwareResolver.mapHardwareModel(HardwareModel.LITE_VNA_64))
        // LiteVNA84 is the same protocol family; the rest of the app already treats
        // both LiteVNA models identically.
        assertEquals(lite, EffectiveHardwareResolver.mapHardwareModel(HardwareModel.LITE_VNA_84))
    }

    @Test
    fun mapHardwareModel_returnsNullWhenThereIsNoConfidentMapping() {
        assertEquals(null, EffectiveHardwareResolver.mapHardwareModel(HardwareModel.UNKNOWN))
        assertEquals(null, EffectiveHardwareResolver.mapHardwareModel(null))
    }

    /*
    ------------------------------------------------------------
    CAPABILITY RESOLUTION — the frequency clamp follows the effective hardware
    ------------------------------------------------------------
    */

    @Test
    fun capabilityProfile_clampFollowsValidatedLiteVnaNotStaleProject() {
        val profile = EffectiveHardwareResolver.resolveEffectiveCapabilityProfile(
            liveDataSourceKind = InstrumentDataSourceKind.REAL_INSTRUMENT,
            liveSelectedModel = HardwareModel.LITE_VNA_64,
            liveSessionOpen = true,
            projectHardware = nano
        )

        // LiteVNA reaches 6.3 GHz; the stale NanoVNA default would have capped at 1.5 GHz.
        assertEquals(6_300_000_000L, profile.maxFrequencyHz)
        assertEquals(100_000L, profile.minFrequencyHz)
        assertEquals("LiteVNA64 v0.3.3", profile.displayName)
    }

    @Test
    fun capabilityProfile_fallsBackToProjectLimitsWithNoLiveDevice() {
        val profile = EffectiveHardwareResolver.resolveEffectiveCapabilityProfile(
            liveDataSourceKind = null,
            liveSelectedModel = null,
            liveSessionOpen = false,
            projectHardware = nano
        )

        assertEquals(1_500_000_000L, profile.maxFrequencyHz)
        assertEquals(50_000L, profile.minFrequencyHz)
        assertEquals("NanoVNA-H4", profile.displayName)
    }

    /*
    ------------------------------------------------------------
    isLiteVna IS DERIVED — never a parallel rule
    ------------------------------------------------------------
    */

    @Test
    fun isLiteVna_agreesWithResolveEffectiveHardwareAcrossTheMatrix() {
        val kinds = listOf(
            InstrumentDataSourceKind.REAL_INSTRUMENT,
            InstrumentDataSourceKind.SIMULATED,
            InstrumentDataSourceKind.NONE,
            null
        )
        val models = listOf(
            HardwareModel.LITE_VNA_64,
            HardwareModel.LITE_VNA_84,
            HardwareModel.NANO_VNA_H4,
            HardwareModel.UNKNOWN,
            null
        )

        for (kind in kinds) {
            for (model in models) {
                for (sessionOpen in listOf(true, false)) {
                    for (project in listOf(nano, lite, null)) {
                        val expected = resolve(kind, model, sessionOpen, project) == lite
                        val actual = EffectiveHardwareResolver.resolveEffectiveIsLiteVna(
                            liveDataSourceKind = kind,
                            liveSelectedModel = model,
                            liveSessionOpen = sessionOpen,
                            projectHardware = project
                        )
                        assertEquals(
                            "kind=$kind model=$model open=$sessionOpen project=$project",
                            expected,
                            actual
                        )
                    }
                }
            }
        }
    }

    @Test
    fun isLiteVna_trueForValidatedLiteVnaOnNanoVnaProject() {
        assertTrue(
            EffectiveHardwareResolver.resolveEffectiveIsLiteVna(
                liveDataSourceKind = InstrumentDataSourceKind.REAL_INSTRUMENT,
                liveSelectedModel = HardwareModel.LITE_VNA_64,
                liveSessionOpen = true,
                projectHardware = nano
            )
        )
        assertFalse(
            EffectiveHardwareResolver.resolveEffectiveIsLiteVna(
                liveDataSourceKind = null,
                liveSelectedModel = null,
                liveSessionOpen = false,
                projectHardware = nano
            )
        )
    }
}
