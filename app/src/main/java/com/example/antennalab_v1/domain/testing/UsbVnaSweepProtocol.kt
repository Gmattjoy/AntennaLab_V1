package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.testing.SweepResult

/*
########################################################################
FILE: UsbVnaSweepProtocol.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Sweep Logic

LAST UPDATED 18/3/2026 00:47

SYSTEM ROLE
Defines the protocol-layer contract used to request a real analyzer sweep
through the prepared USB command channel.

CURRENT DEVELOPMENT ROLE
This file now establishes the stable driver-based boundary between:

• USB command transport
• analyzer-family-specific sweep implementations
• driver registry resolution
• discovery-mode classification
• higher-level sweep data source logic

DESIGN INTENT
This file is now the shared home for:

• sweep protocol contract
• driver contract
• support tier model
• discovery snapshot model
• driver registry
• shared sweep validation rules

SAFE EDIT AREA
- extend request validation later
- add timeout policy later
- add richer discovery heuristics later
- add binary / ascii mode metadata later
- add retry / recovery policy later
########################################################################
*/

/*
########################################################################
EDIT SECTION 1000
SUPPORT TIER MODEL
------------------------------------------------------------------------
PURPOSE
Formalizes the support tier reported by discovery and driver resolution.

IMPORTANT
These labels are intentionally stable so UI, session state, and error
systems can all speak the same language.
########################################################################
*/
enum class InstrumentSupportTier {
    DETECTED,
    PARTIAL_SUPPORT,
    FULL_SUPPORT
}

/*
########################################################################
EDIT SECTION 1100
DRIVER CAPABILITY PROFILE
------------------------------------------------------------------------
PURPOSE
Describes what a resolved driver can currently do.
########################################################################
*/
data class UsbVnaDriverCapabilityProfile(
    val supportsSweepExecution: Boolean,
    val supportsS11: Boolean,
    val supportsS11Phase: Boolean,
    val supportsS21: Boolean,
    val supportsS21Phase: Boolean,
    val measurementTrustSummary: String
)

/*
########################################################################
EDIT SECTION 1200
TRANSPORT BEHAVIOUR PROFILE
------------------------------------------------------------------------
PURPOSE
Describes the transport assumptions and expectations for a driver.
########################################################################
*/
data class UsbVnaTransportBehaviorProfile(
    val requiresOpenSession: Boolean,
    val requiresBulkEndpoints: Boolean,
    val prefersTextCommands: Boolean,
    val behaviorSummary: String
)

/*
########################################################################
EDIT SECTION 1300
DRIVER MATCH RESULT
------------------------------------------------------------------------
PURPOSE
Describes whether a driver believes an identity result matches it and how
strong that match is.
########################################################################
*/
data class UsbVnaDriverMatchResult(
    val matched: Boolean,
    val supportTier: InstrumentSupportTier,
    val matchSummary: String
)

/*
########################################################################
EDIT SECTION 1400
DISCOVERY SNAPSHOT
------------------------------------------------------------------------
PURPOSE
Captures discovery-mode output in one stable structure.

IMPORTANT
This must remain safe for unknown devices and partial classification.
########################################################################
*/
data class UsbVnaDiscoverySnapshot(
    val instrumentIdentity: String,
    val protocolFamilyDisplayName: String,
    val protocolGuess: String,
    val supportTier: InstrumentSupportTier,
    val driverId: String?,
    val dataSourceDisplayName: String,
    val transportStatusSummary: String,
    val measurementTrustSummary: String,
    val summary: String
)

/*
########################################################################
EDIT SECTION 1500
DRIVER RESOLUTION MODEL
------------------------------------------------------------------------
PURPOSE
Carries the selected driver and the discovery snapshot produced by the
registry.
########################################################################
*/
data class UsbVnaDriverResolution(
    val driver: UsbVnaInstrumentDriver?,
    val discoverySnapshot: UsbVnaDiscoverySnapshot
) {
    fun canExecuteRealSweep(): Boolean {
        return driver != null &&
                discoverySnapshot.supportTier == InstrumentSupportTier.FULL_SUPPORT &&
                driver.getCapabilityProfile().supportsSweepExecution
    }
}

/*
########################################################################
EDIT SECTION 2000
SWEEP PROTOCOL CONTRACT
------------------------------------------------------------------------
PURPOSE
Defines the common analyzer sweep operation required by the USB-backed
sweep data source.
########################################################################
*/
interface UsbVnaSweepProtocol {

    /*
    --------------------------------------------------------------------
    FUNCTION ROLE
    Executes one complete analyzer sweep using the provided command
    channel and returns a parsed SweepResult model.
    --------------------------------------------------------------------
    */
    fun acquireSweep(
        commandChannel: UsbVnaCommandChannel,
        startFrequencyHz: Long,
        stopFrequencyHz: Long,
        pointCount: Int
    ): SweepResult
}

/*
########################################################################
EDIT SECTION 3000
DRIVER CONTRACT
------------------------------------------------------------------------
PURPOSE
Defines the commercial-grade driver boundary for registry-based hardware
resolution and execution.

IMPORTANT
Controllers and UI should not decide hardware type. They should consume
registry output instead.
########################################################################
*/
interface UsbVnaInstrumentDriver {

    val driverId: String
    val protocolFamily: UsbVnaProtocolFamily

    fun matchIdentity(
        identityResult: UsbAnalyzerIdentityResult
    ): UsbVnaDriverMatchResult

    fun getCapabilityProfile(): UsbVnaDriverCapabilityProfile

    fun getTransportBehaviorProfile(): UsbVnaTransportBehaviorProfile

    fun acquireSweep(
        commandChannel: UsbVnaCommandChannel,
        startFrequencyHz: Long,
        stopFrequencyHz: Long,
        pointCount: Int
    ): SweepResult
}

/*
########################################################################
EDIT SECTION 4000
DRIVER REGISTRY
------------------------------------------------------------------------
PURPOSE
Central registry for protocol-family-to-driver resolution.

IMPORTANT RULE
No scattered when-logic should exist above this layer for deciding which
driver owns the hardware.
########################################################################
*/
object UsbVnaDriverRegistry {

    /*
    --------------------------------------------------------------------
    SECTION 4100
    REGISTERED DRIVERS
    --------------------------------------------------------------------
    PURPOSE
    Keeps the known driver list centralized.
    --------------------------------------------------------------------
    */
    private val registeredDrivers: List<UsbVnaInstrumentDriver> = listOf(
        NanoVnaInstrumentDriver()
    )

    /*
    --------------------------------------------------------------------
    SECTION 4200
    RESOLVE DRIVER
    --------------------------------------------------------------------
    PURPOSE
    Converts identity handshake output into a discovery snapshot and, if
    possible, a concrete driver.
    --------------------------------------------------------------------
    */
    fun resolveDriver(
        identityResult: UsbAnalyzerIdentityResult
    ): UsbVnaDriverResolution {
        val identityText = identityResult.rawIdentityText
            ?.takeIf { it.isNotBlank() }
            ?: "No readable identity text returned."

        val protocolDisplayName = identityResult.protocolIdentity.displayName
        val matchedDrivers = registeredDrivers.map { driver ->
            driver to driver.matchIdentity(identityResult)
        }

        val winningMatch = matchedDrivers
            .filter { it.second.matched }
            .maxByOrNull { it.second.supportTier.priority }

        val resolvedDriver = winningMatch?.first
        val resolvedMatch = winningMatch?.second

        val supportTier = when {
            !identityResult.success -> InstrumentSupportTier.DETECTED
            resolvedMatch != null -> resolvedMatch.supportTier
            identityResult.protocolIdentity.family != UsbVnaProtocolFamily.UNKNOWN ->
                InstrumentSupportTier.DETECTED
            else -> InstrumentSupportTier.DETECTED
        }

        val capabilityProfile = resolvedDriver?.getCapabilityProfile()
        val transportProfile = resolvedDriver?.getTransportBehaviorProfile()

        val summary = buildString {
            append("Identity → Protocol → Driver resolution complete. ")
            append("ProtocolGuess=$protocolDisplayName. ")
            append("SupportTier=${supportTier.displayName}. ")
            append(
                if (resolvedDriver != null) {
                    "Driver=${resolvedDriver.driverId}. "
                } else {
                    "Driver=Unresolved. "
                }
            )
            append(
                resolvedMatch?.matchSummary
                    ?: "No full driver match available for this identity yet."
            )
        }

        return UsbVnaDriverResolution(
            driver = resolvedDriver,
            discoverySnapshot = UsbVnaDiscoverySnapshot(
                instrumentIdentity = identityText,
                protocolFamilyDisplayName = protocolDisplayName,
                protocolGuess = protocolDisplayName,
                supportTier = supportTier,
                driverId = resolvedDriver?.driverId,
                dataSourceDisplayName =
                    if (resolvedDriver != null) "USB Driver Registry"
                    else "USB Discovery Mode",
                transportStatusSummary =
                    transportProfile?.behaviorSummary
                        ?: "No driver transport behaviour available.",
                measurementTrustSummary =
                    capabilityProfile?.measurementTrustSummary
                        ?: "Discovery-only classification. Real sweep data is not trusted yet.",
                summary = summary
            )
        )
    }
}

/*
########################################################################
EDIT SECTION 5000
SUPPORT TIER HELPERS
------------------------------------------------------------------------
PURPOSE
Provides stable ranking and display helpers for support tiers.
########################################################################
*/
private val InstrumentSupportTier.priority: Int
    get() = when (this) {
        InstrumentSupportTier.DETECTED -> 1
        InstrumentSupportTier.PARTIAL_SUPPORT -> 2
        InstrumentSupportTier.FULL_SUPPORT -> 3
    }

val InstrumentSupportTier.displayName: String
    get() = when (this) {
        InstrumentSupportTier.DETECTED -> "Detected"
        InstrumentSupportTier.PARTIAL_SUPPORT -> "Partial Support"
        InstrumentSupportTier.FULL_SUPPORT -> "Full Support"
    }

/*
########################################################################
EDIT SECTION 6000
SHARED SWEEP RULES
------------------------------------------------------------------------
PURPOSE
Provides central validation and helper rules shared by concrete protocol
implementations.
########################################################################
*/
object UsbVnaSweepProtocolRules {

    /*
    --------------------------------------------------------------------
    SECTION 6100
    CONSTANTS
    --------------------------------------------------------------------
    NOTE
    These are conservative first-pass limits. Device-specific protocol
    classes can apply tighter limits later if needed.
    --------------------------------------------------------------------
    */
    const val MIN_POINT_COUNT: Int = 2
    const val MAX_POINT_COUNT: Int = 2001

    /*
    --------------------------------------------------------------------
    SECTION 6200
    VALIDATE REQUEST
    --------------------------------------------------------------------
    PURPOSE
    Validates the requested sweep bounds before a protocol implementation
    attempts device communication.
    --------------------------------------------------------------------
    */
    fun requireValidSweepRequest(
        startFrequencyHz: Long,
        stopFrequencyHz: Long,
        pointCount: Int
    ) {
        require(startFrequencyHz > 0L) {
            "Sweep start frequency must be greater than 0 Hz."
        }

        require(stopFrequencyHz > 0L) {
            "Sweep stop frequency must be greater than 0 Hz."
        }

        require(stopFrequencyHz > startFrequencyHz) {
            "Sweep stop frequency must be greater than start frequency."
        }

        require(pointCount in MIN_POINT_COUNT..MAX_POINT_COUNT) {
            "Sweep point count must be between $MIN_POINT_COUNT and $MAX_POINT_COUNT."
        }
    }

    /*
    --------------------------------------------------------------------
    SECTION 6300
    CALCULATE STEP
    --------------------------------------------------------------------
    PURPOSE
    Calculates the nominal frequency step for a sweep request.
    --------------------------------------------------------------------
    */
    fun calculateStepHz(
        startFrequencyHz: Long,
        stopFrequencyHz: Long,
        pointCount: Int
    ): Double {
        requireValidSweepRequest(
            startFrequencyHz = startFrequencyHz,
            stopFrequencyHz = stopFrequencyHz,
            pointCount = pointCount
        )

        return (stopFrequencyHz - startFrequencyHz).toDouble() /
                (pointCount - 1).toDouble()
    }
}