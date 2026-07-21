package com.example.antennalab_v1.domain.testing

/*
########################################################################
FILE: HardwareSweepCapability.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Hardware Capability

LAST UPDATED 18/3/2026 00:40

ARCHITECTURE ROLE (UPDATED)
Now represents ONLY transport readiness + minimum viability.

IMPORTANT CHANGE
Capability is no longer tied to specific hardware types.
Protocol + capability mapping will be handled by resolver/driver layer.

SAFE EDIT AREA
- extend with calibration readiness later
- extend with protocol validation later
- extend with capability negotiation later
########################################################################
*/
object HardwareSweepCapability {

    private val foundationTransport = FoundationUsbVnaTransport()

    /*
    ####################################################################
    SECTION 1000
    REAL SWEEP READINESS
    ####################################################################
    */
    fun isRealSweepPossible(): Boolean {
        return getRealSweepStatus().ready
    }

    /*
    ####################################################################
    SECTION 2000
    REAL SWEEP STATUS
    ####################################################################
    */
    fun getRealSweepStatus(): UsbVnaTransportStatus {
        return foundationTransport.evaluateTransportStatus()
    }

    /*
    ####################################################################
    SECTION 3000
    TRANSPORT STATE HELPERS
    ####################################################################
    */
    fun hasOpenUsbSession(): Boolean {
        return UsbSessionManager.hasOpenSession()
    }

    fun hasPreparedTransportChannel(): Boolean {
        return UsbSessionManager.hasActiveTransportChannel()
    }

    fun isTransportPreparedButNotReady(): Boolean {
        return hasOpenUsbSession() &&
                hasPreparedTransportChannel() &&
                !isRealSweepPossible()
    }

    /*
    ####################################################################
    SECTION 4000
    UI LABEL HELPERS
    ####################################################################
    */
    fun getRealSweepStatusLabel(): String {
        return if (isRealSweepPossible()) "Ready" else "Not Ready"
    }

    fun getRealSweepStatusSummary(): String {
        return getRealSweepStatus().summary
    }
}