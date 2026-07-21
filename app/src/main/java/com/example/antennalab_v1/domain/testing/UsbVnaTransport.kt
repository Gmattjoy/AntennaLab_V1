package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.testing.MeasurementTrustLevel

/*
########################################################################
FILE: UsbVnaTransport.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / USB Transport

LAST UPDATED 18/3/2026 01:42

SYSTEM ROLE
Defines the transport boundary between the current live USB stack and
the analyzer sweep protocol layer.

CURRENT DEVELOPMENT ROLE
This file now validates the real current transport foundation:

• USB transport channel prepared
• bulk transfer structure test available
• command channel available
• analyzer identity handshake available

ARCHITECTURE ROLE (UPDATED)
Now also provides transport-level measurement trust grading so later UI
and session-state models can expose whether results should be treated as
trusted, partial, degraded, or unknown.

SAFE EDIT AREA
- add interface claim detail later
- add endpoint detail later
- add retry / recovery later
- add transport timing diagnostics later
- add richer readiness grading later
########################################################################
*/

/*
########################################################################
EDIT SECTION 1000
TRANSPORT STATUS MODEL
------------------------------------------------------------------------
PURPOSE
Provides a compact readiness summary for transport-level sweep
availability decisions.
########################################################################
*/
data class UsbVnaTransportStatus(
    val ready: Boolean,
    val summary: String
)

/*
########################################################################
EDIT SECTION 2000
TRANSPORT CONTRACT
------------------------------------------------------------------------
PURPOSE
Defines the minimum transport behaviour needed by a live USB-backed
sweep source.
########################################################################
*/
interface UsbVnaTransport {

    fun evaluateTransportStatus(): UsbVnaTransportStatus

    fun evaluateMeasurementTrust(): MeasurementTrustLevel

    fun isReady(): Boolean {
        return evaluateTransportStatus().ready
    }
}
/*

EDIT SECTION 3000
FOUNDATION TRANSPORT
------------------------------------------------------------------------
PURPOSE
Provides the first real transport-readiness implementation using the
current USB stack and command channel.
########################################################################
*/
class FoundationUsbVnaTransport(
    private val commandChannel: UsbVnaCommandChannel = UsbVnaCommandChannel()
) : UsbVnaTransport {

    override fun evaluateTransportStatus(): UsbVnaTransportStatus {
        val preparedChannelStatus = evaluatePreparedChannelStatus()
        if (preparedChannelStatus != null) {
            return preparedChannelStatus
        }

        val structuralTestResult = UsbBulkTransferTester.runStructuralChannelTest()
        if (!structuralTestResult.success) {
            return UsbVnaTransportStatus(
                ready = false,
                summary = structuralTestResult.summary
            )
        }

        val commandChannelStatus = evaluateCommandChannelStatus(
            structuralSummary = structuralTestResult.summary
        )
        if (commandChannelStatus != null) {
            return commandChannelStatus
        }

        val cachedIdentityResult = UsbSessionManager.getLatestAnalyzerIdentityResult()

        if (cachedIdentityResult == null) {
            return UsbVnaTransportStatus(
                ready = false,
                summary = structuralTestResult.summary +
                        " Transport is structurally available, but analyzer identity has not been verified yet."
            )
        }

        if (!cachedIdentityResult.success) {
            return UsbVnaTransportStatus(
                ready = false,
                summary = structuralTestResult.summary +
                        " " + cachedIdentityResult.summary
            )
        }

        if (cachedIdentityResult.protocolIdentity.family == UsbVnaProtocolFamily.UNKNOWN) {
            return UsbVnaTransportStatus(
                ready = false,
                summary = structuralTestResult.summary +
                        " Analyzer identity is cached, but the protocol family is still unknown."
            )
        }

        return UsbVnaTransportStatus(
            ready = true,
            summary = structuralTestResult.summary +
                    " " + cachedIdentityResult.summary +
                    " Real sweep transport path is ready."
        )
    }

    /*
    ####################################################################
    EDIT SECTION 3500
    MEASUREMENT TRUST EVALUATION
    --------------------------------------------------------------------
    PURPOSE
    Grades the current transport-backed measurement trust so higher
    layers can distinguish between trusted, partial, degraded, and
    unknown states.
    ####################################################################
    */
    override fun evaluateMeasurementTrust(): MeasurementTrustLevel {
        if (!UsbSessionManager.hasOpenSession()) {
            return MeasurementTrustLevel.UNKNOWN
        }

        if (!UsbSessionManager.hasActiveTransportChannel()) {
            return MeasurementTrustLevel.UNKNOWN
        }

        val structuralTestResult = UsbBulkTransferTester.runStructuralChannelTest()
        if (!structuralTestResult.success) {
            return MeasurementTrustLevel.DEGRADED
        }

        if (!commandChannel.isChannelAvailable()) {
            return MeasurementTrustLevel.PARTIAL
        }

        val cachedIdentityResult = UsbSessionManager.getLatestAnalyzerIdentityResult()
        if (cachedIdentityResult == null) {
            return MeasurementTrustLevel.PARTIAL
        }

        if (!cachedIdentityResult.success && !cachedIdentityResult.rawIdentityText.isNullOrBlank()) {
            return MeasurementTrustLevel.PARTIAL
        }

        if (!cachedIdentityResult.success) {
            return MeasurementTrustLevel.DEGRADED
        }

        if (cachedIdentityResult.protocolIdentity.family == UsbVnaProtocolFamily.UNKNOWN) {
            return MeasurementTrustLevel.PARTIAL
        }

        return MeasurementTrustLevel.TRUSTED
    }

    /*
    ####################################################################
    EDIT SECTION 4000
    PREPARED CHANNEL STATUS
    --------------------------------------------------------------------
    PURPOSE
    Verifies that the session layer has already prepared an active
    transport channel before deeper transport tests begin.
    ####################################################################
    */
    private fun evaluatePreparedChannelStatus(): UsbVnaTransportStatus? {
        if (!UsbSessionManager.hasOpenSession()) {
            return UsbVnaTransportStatus(
                ready = false,
                summary = "USB transport is not ready because no USB session is currently open."
            )
        }

        if (!UsbSessionManager.hasActiveTransportChannel()) {
            return UsbVnaTransportStatus(
                ready = false,
                summary = "USB transport is not ready because no active transport channel has been prepared."
            )
        }

        return null
    }

    /*
    ####################################################################
    EDIT SECTION 5000
    COMMAND CHANNEL STATUS
    --------------------------------------------------------------------
    PURPOSE
    Confirms that the command-layer wrapper can access the active
    transport channel after the structural transport check succeeds.
    ####################################################################
    */
    private fun evaluateCommandChannelStatus(
        structuralSummary: String
    ): UsbVnaTransportStatus? {
        if (!commandChannel.isChannelAvailable()) {
            return UsbVnaTransportStatus(
                ready = false,
                summary = structuralSummary + " Command layer is not available."
            )
        }

        return null
    }
}
