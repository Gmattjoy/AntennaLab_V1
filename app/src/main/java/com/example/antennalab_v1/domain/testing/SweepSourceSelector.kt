package com.example.antennalab_v1.domain.testing

/*
########################################################################
FILE: SweepSourceSelector.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Sweep Source Selection

LAST UPDATED 18/3/2026 00:40

SYSTEM ROLE
Central decision point for determining which sweep data source should be
used by the testing system.

ARCHITECTURE ROLE (UPDATED)
Now acts as a thin orchestration layer that selects between:

• Real instrument pipeline (transport + protocol + capability)
• Simulation pipeline

IMPORTANT DESIGN SHIFT
This class no longer owns hardware assumptions.
It only evaluates readiness and delegates identity/protocol resolution.

SAFE EDIT AREA
- add remote companion support later
- add multi-device selection later
- add manual source override later
########################################################################
*/

/*
########################################################################
SECTION 0500
SOURCE SELECTION DECISION MODEL
########################################################################
*/
data class SweepSourceSelectionDecision(
    val useRealHardware: Boolean,
    val sourceLabel: String,
    val reason: String
)

object SweepSourceSelector {

    /*
####################################################################
SECTION 0900
BUILD SOURCE DECISION
####################################################################
*/
    fun getSelectionDecision(): SweepSourceSelectionDecision {

        val sessionState = UsbSessionManager.getLatestInstrumentSessionState()
        val selectedProfile = UsbSessionManager.getSelectedDriverProfile()

        val transportReady = UsbSessionManager.isTransportReady()
        val realSweepStatus = HardwareSweepCapability.getRealSweepStatus()
        val realSweepPossible = realSweepStatus.ready

        val selectedProfileLabel = selectedProfile?.displayName
            ?: "No selected driver profile"

        val protocolLabel = sessionState?.protocolFamily
            ?: selectedProfile?.protocolType?.name
            ?: "Unknown Protocol"

        if (transportReady && realSweepPossible) {
            return SweepSourceSelectionDecision(
                useRealHardware = true,
                sourceLabel = "Real Instrument ($protocolLabel)",
                reason = "Real instrument path selected. profile=$selectedProfileLabel. ${realSweepStatus.summary}"
            )
        }

        return SweepSourceSelectionDecision(
            useRealHardware = false,
            sourceLabel = "Simulated Instrument",
            reason = "Simulation path selected. profile=$selectedProfileLabel. ${realSweepStatus.summary}"
        )
    }

    /*
    ####################################################################
    SECTION 1000
    SELECT SWEEP SOURCE
    ####################################################################
    */
    fun selectSweepSource(): SweepDataSource {

        val decision = getSelectionDecision()
        val selectedProfile = UsbSessionManager.getSelectedDriverProfile()

        if (decision.useRealHardware) {
            return UsbVnaSweepDataSource(
                selectedDriverProfile = selectedProfile
            )
        }

        return DemoSweepDataSource()
    }
}