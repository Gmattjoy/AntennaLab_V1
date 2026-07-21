package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.DriverProfile
import com.example.antennalab_v1.model.DriverProtocolType

/*
########################################################################
FILE: ProtocolDriverFactory.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Driver Factory

LAST UPDATED 26/3/2026 19:15

SYSTEM ROLE
Creates the correct protocol execution path based on a selected
DriverProfile.

ARCHITECTURE ROLE
Separates:
- user-selected driver profile
from
- actual protocol execution path

IMPORTANT DESIGN RULE
UI and session layers must not directly decide protocol execution
classes. All execution-path creation should flow through this factory.
########################################################################
*/

/*
########################################################################
SECTION 1000
EXECUTION PATH TYPE
------------------------------------------------------------------------
PURPOSE
Represents the protocol execution path chosen from the selected driver
profile.
########################################################################
*/
sealed class ProtocolExecutionPath {

    /*
    ####################################################################
    SECTION 1100
    NANO SHELL PATH
    --------------------------------------------------------------------
    PURPOSE
    Real Nano shell execution path.
    ####################################################################
    */
    data object NanoShell : ProtocolExecutionPath()

    /*
    ####################################################################
    SECTION 1200
    LITEVNA PATH
    --------------------------------------------------------------------
    PURPOSE
    Reserved LiteVNA execution path.
    ####################################################################
    */
    data object LiteVna : ProtocolExecutionPath()

    /*
    ####################################################################
    SECTION 1300
    EXPERIMENTAL ASCII PATH
    --------------------------------------------------------------------
    PURPOSE
    Controlled fallback path for unknown ASCII-style serial analyzers.
    ####################################################################
    */
    data object ExperimentalAsciiSerial : ProtocolExecutionPath()
}

/*
########################################################################
SECTION 2000
PROTOCOL DRIVER FACTORY
------------------------------------------------------------------------
PURPOSE
Maps DriverProfile → protocol execution path.
########################################################################
*/
object ProtocolDriverFactory {

    /*
    ####################################################################
    SECTION 2100
    CREATE EXECUTION PATH
    --------------------------------------------------------------------
    PURPOSE
    Returns the protocol execution path for the selected profile.
    ####################################################################
    */
    fun createExecutionPath(
        profile: DriverProfile
    ): ProtocolExecutionPath {
        return when (profile.protocolType) {

            DriverProtocolType.NANO_SHELL -> {
                ProtocolExecutionPath.NanoShell
            }

            DriverProtocolType.LITE_VNA_V2_STYLE -> {
                ProtocolExecutionPath.LiteVna
            }

            DriverProtocolType.EXPERIMENTAL_ASCII_SERIAL -> {
                ProtocolExecutionPath.ExperimentalAsciiSerial
            }
        }
    }
}