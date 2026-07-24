package com.example.antennalab_v1.domain.testing

/*
########################################################################
FILE: EffectiveHardwareResolver.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Hardware Resolution

SYSTEM ROLE
THE single resolution point for "which hardware is actually measuring
right now" — as opposed to "which hardware this project was configured
for at design time".

WHY THIS EXISTS
ProjectData.testHardwareProfile defaults to NANOVNA_H4 and is a
design-time choice. Every capability consumer used to read it directly,
so a connected, validated LiteVNA was treated as a NanoVNA: wrong TDR
velocity factor (0.66 instead of 0.82 — a ~24% cable-fault distance
error), wrong frequency clamp (1.5 GHz instead of 6.3 GHz), wrong
display name, and — once feature flags diverge — the wrong feature tier.

THE DISTINCTION CALLERS MUST KEEP
- DESIGN-TIME (project.testHardwareProfile): what this project is
  configured for. Persisted, operator-editable, describes intent.
  Used for the hardware selector, build-sheet text, and storage.
- EFFECTIVE (this file): what will actually measure right now. Drives
  math, limits, and feature gates.

PURITY
The resolution rules are pure — parameters in, no UsbSessionManager
reach-through, no Compose, no Android. `resolveForProject` is the one
clearly-marked impure adapter that reads the shared session singleton so
call sites don't each repeat it.
########################################################################
*/

import com.example.antennalab_v1.model.HardwareModel
import com.example.antennalab_v1.model.HardwareCapabilityProfile
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.TestHardwareProfile
import com.example.antennalab_v1.model.testing.InstrumentDataSourceKind
import com.example.antennalab_v1.model.toHardwareCapabilityProfile

object EffectiveHardwareResolver {

    /*
    ------------------------------------------------------------
    SECTION 1100
    PRECEDENCE (pure)
    ------------------------------------------------------------
    PURPOSE
    Three-tier resolution:

      1. A live VALIDATED instrument (REAL_INSTRUMENT) whose model maps
         confidently — the strongest signal.
      2. A live SELECTED driver profile with an open session that has not
         finished validation yet. This tier is deliberate: bring-up takes
         ~15 s, and that is exactly the window an operator sits in while
         running Validate. Dropping it would silently regress the shipped
         sweep-width fix.
      3. The project's design-time profile.
      4. A deterministic default when there is nothing at all.

    Note tier 2 requires an OPEN SESSION. A merely-selected profile with
    no session falls through to the project profile (offline/simulated
    review keeps the project's own configuration).
    ------------------------------------------------------------
    */
    fun resolveEffectiveHardware(
        liveDataSourceKind: InstrumentDataSourceKind?,
        liveSelectedModel: HardwareModel?,
        liveSessionOpen: Boolean,
        projectHardware: TestHardwareProfile?
    ): TestHardwareProfile {
        val liveHardware = mapHardwareModel(liveSelectedModel)

        return when {
            liveHardware != null &&
                liveDataSourceKind == InstrumentDataSourceKind.REAL_INSTRUMENT -> liveHardware

            liveHardware != null && liveSessionOpen -> liveHardware

            else -> projectHardware ?: DEFAULT_HARDWARE
        }
    }

    /*
    The effective capability profile (frequency limits + feature flags +
    display name). Every capability consumer should read THIS, never
    project.hardwareCapabilityProfile.
    */
    fun resolveEffectiveCapabilityProfile(
        liveDataSourceKind: InstrumentDataSourceKind?,
        liveSelectedModel: HardwareModel?,
        liveSessionOpen: Boolean,
        projectHardware: TestHardwareProfile?
    ): HardwareCapabilityProfile {
        return resolveEffectiveHardware(
            liveDataSourceKind = liveDataSourceKind,
            liveSelectedModel = liveSelectedModel,
            liveSessionOpen = liveSessionOpen,
            projectHardware = projectHardware
        ).toHardwareCapabilityProfile()
    }

    /*
    DERIVED from resolveEffectiveHardware — deliberately not a parallel
    rule. This replaces the old SweepGraphMath.resolveEffectiveIsLiteVna
    so sweep width and capability resolution can never disagree.
    */
    fun resolveEffectiveIsLiteVna(
        liveDataSourceKind: InstrumentDataSourceKind?,
        liveSelectedModel: HardwareModel?,
        liveSessionOpen: Boolean,
        projectHardware: TestHardwareProfile?
    ): Boolean {
        return resolveEffectiveHardware(
            liveDataSourceKind = liveDataSourceKind,
            liveSelectedModel = liveSelectedModel,
            liveSessionOpen = liveSessionOpen,
            projectHardware = projectHardware
        ) == TestHardwareProfile.LITEVNA64_V0_3_3
    }

    /*
    ------------------------------------------------------------
    SECTION 1200
    MODEL MAPPING (pure)
    ------------------------------------------------------------
    PURPOSE
    Map a live driver profile's HardwareModel onto a stored
    TestHardwareProfile. Returns null when there is no confident mapping,
    which makes the caller fall through to the project profile rather
    than guess.

    LITE_VNA_84 maps onto the LiteVNA profile: it is the same protocol
    family and the rest of the app (isLiteProfile / LITE_VNA_V2_STYLE)
    already treats both LiteVNA models identically.
    ------------------------------------------------------------
    */
    internal fun mapHardwareModel(model: HardwareModel?): TestHardwareProfile? {
        return when (model) {
            HardwareModel.NANO_VNA_H,
            HardwareModel.NANO_VNA_H4 -> TestHardwareProfile.NANOVNA_H4

            HardwareModel.LITE_VNA_64,
            HardwareModel.LITE_VNA_84 -> TestHardwareProfile.LITEVNA64_V0_3_3

            HardwareModel.UNKNOWN, null -> null
        }
    }

    /** Deterministic fallback when neither a live instrument nor a project profile exists. */
    private val DEFAULT_HARDWARE = TestHardwareProfile.NANOVNA_H4

    /*
    ------------------------------------------------------------
    SECTION 1300
    LIVE-SESSION ADAPTER (impure — reads the shared singleton)
    ------------------------------------------------------------
    PURPOSE
    The one place that reads UsbSessionManager for hardware resolution,
    so call sites don't each repeat the same three reads. Keeping this
    here is what makes "one resolution point" true in practice.

    Tests target the pure functions above, not this adapter.
    ------------------------------------------------------------
    */
    fun resolveForProject(project: ProjectData?): TestHardwareProfile {
        return resolveEffectiveHardware(
            liveDataSourceKind = UsbSessionManager.getLatestInstrumentSessionState()?.dataSourceKind,
            liveSelectedModel = UsbSessionManager.getSelectedDriverProfile()?.hardwareModel,
            liveSessionOpen = UsbSessionManager.hasOpenSession(),
            projectHardware = project?.testHardwareProfile
        )
    }

    /** Effective capability profile for a project against the live session. */
    fun resolveCapabilityProfileForProject(project: ProjectData?): HardwareCapabilityProfile {
        return resolveForProject(project).toHardwareCapabilityProfile()
    }
}
