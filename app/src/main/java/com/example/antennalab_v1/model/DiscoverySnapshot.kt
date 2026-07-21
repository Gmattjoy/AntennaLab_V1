package com.example.antennalab_v1.model

/*
########################################################################
FILE: DiscoverySnapshot.kt
PACKAGE: com.example.antennalab_v1.model
LAYER: Core Data Model / Discovery Capture

SYSTEM ROLE
Stores the current compact unknown-antenna discovery result.

CURRENT DEVELOPMENT ROLE
This file provides the detached discovery summary used by:

• unknown antenna discovery handoff
• apply-to-current-project workflow
• save-as-new-project workflow
• future discovery history expansion

IMPORTANT
This snapshot is intentionally lightweight. It stores the summary needed
for handoff decisions without persisting dense raw sweep arrays.

SAFE EDIT AREA
- add visual-description fields later
- add connector/feedpoint metadata later
- add linked raw sweep references later
########################################################################
*/

data class DiscoverySnapshot(
    val capturedAtEpochMs: Long = 0L,
    val antennaClassificationGuess: AntennaClassification =
        AntennaClassification.NOT_YET_CLASSIFIED,
    val hardwareName: String = "",
    val sweepStartMHz: Double = 0.0,
    val sweepEndMHz: Double = 0.0,
    val bestFrequencyMHz: Double = 0.0,
    val bestSwr: Double = 0.0,
    val returnLossDb: Double? = null,
    val summaryLabel: String = "",
    val operatorNotes: String = ""
)
