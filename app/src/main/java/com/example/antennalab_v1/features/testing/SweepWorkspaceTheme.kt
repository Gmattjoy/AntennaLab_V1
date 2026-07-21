package com.example.antennalab_v1.features.testing

/*
########################################################################
FILE: SweepWorkspaceTheme.kt
PACKAGE: com.example.antennalab_v1.features.testing
LAYER: UI / Testing Tools / Theme

LAST UPDATED 16/3/2026 06:36

SYSTEM ROLE
Provides the shared instrument-style color palette for the sweep
workspace and related testing UI files.

CURRENT DEVELOPMENT ROLE
This file centralizes the dark RF instrument palette so screen, widget,
and tool-panel files can reference one consistent theme source.

SAFE EDIT AREA
- add spacing tokens later
- add typography tokens later
- add light theme or alternate instrument themes later
########################################################################
*/

import androidx.compose.ui.graphics.Color

/*
########################################################################
SECTION 1
INSTRUMENT COLOR TOKENS
########################################################################
PURPOSE
Defines the shared sweep workspace instrument palette.
########################################################################
*/

val InstrumentBackground = Color(0xFF000000)
val InstrumentSurface = Color(0xFF1F1F23)
val InstrumentSurfaceVariant = Color(0xFF2A2A2E)
val InstrumentAccent = Color(0xFFE8C547)
val InstrumentTextPrimary = Color(0xFFF4F4F5)
val InstrumentTextSecondary = Color(0xFF8E8E93)
val InstrumentDivider = Color(0xFF34343A)
val InstrumentBlue = Color(0xFF4EA1FF)
val InstrumentMagenta = Color(0xFFD66BFF)
val InstrumentGreen = Color(0xFF43D17A)