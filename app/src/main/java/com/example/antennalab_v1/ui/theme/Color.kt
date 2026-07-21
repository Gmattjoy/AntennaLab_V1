package com.example.antennalab_v1.ui.theme

import androidx.compose.ui.graphics.Color

/*
########################################################################
FILE: Color.kt
PACKAGE: com.example.antennalab_v1.ui.theme
LAYER: UI / Theme / Color System

LAST UPDATED 4/4/2026 21:20

SYSTEM ROLE
Defines the shared AntennaLab V1 color palette for light and dark
themes.

CURRENT DEVELOPMENT ROLE
This version replaces the brighter cyan accent with a calmer metallic
instrument tone:

• deep navy-black shell
• blue-slate panel surfaces
• metallic teal-grey primary accents
• cooler readable text hierarchy
• restrained warm metal tones only as secondary support

DESIGN DIRECTION
Modern RF instrument UI first.
Vintage influence reduced to minor supporting tones only.

SAFE EDIT AREA
- refine light palette later
- add semantic graph colors later
- add richer shadow / elevation support later
########################################################################
*/

/*
########################################################################
SECTION 1000
DEFAULT DARK THEME COLORS
########################################################################
PURPOSE
Primary instrument theme inspired by modernized lab gear and late-analog
high-tech control panels.
########################################################################
*/
val DarkPrimary = Color(0xFF6F9792)
val DarkOnPrimary = Color(0xFF0B1416)

val DarkSecondary = Color(0xFF8198A6)
val DarkOnSecondary = Color(0xFF0E171D)

val DarkTertiary = Color(0xFF6E858E)
val DarkOnTertiary = Color(0xFFEDF5F7)

val DarkBackground = Color(0xFF091018)
val DarkOnBackground = Color(0xFFE4EDF2)

val DarkSurface = Color(0xFF101A24)
val DarkOnSurface = Color(0xFFE4EDF2)

val DarkSurfaceVariant = Color(0xFF172633)
val DarkOnSurfaceVariant = Color(0xFFA9BAC5)

val DarkOutline = Color(0xFF4B6471)

/*
########################################################################
SECTION 1100
DARK PANEL EXTENSIONS
########################################################################
PURPOSE
Additional console colors for cards, rails, and status lamp use.
########################################################################
*/
val DarkPanelTop = Color(0xFF1B2D3B)
val DarkPanelMid = Color(0xFF13212D)
val DarkPanelLow = Color(0xFF0B141D)

val DarkBrass = Color(0xFF8E7652)
val DarkBronze = Color(0xFF6C5940)
val DarkIvory = Color(0xFFE4EDF2)

val StatusGood = Color(0xFF4BD37B)
val StatusWarning = Color(0xFFE0A84F)
val StatusBad = Color(0xFFE06767)

/*
########################################################################
SECTION 2000
LIGHT THEME COLORS
########################################################################
PURPOSE
Clean technical drafting / instrument-panel look for light mode.
########################################################################
*/
val LightPrimary = Color(0xFF5E8884)
val LightOnPrimary = Color(0xFFF7FEFE)

val LightSecondary = Color(0xFF5D7787)
val LightOnSecondary = Color(0xFFFFFFFF)

val LightTertiary = Color(0xFF6E858E)
val LightOnTertiary = Color(0xFFFFFFFF)

val LightBackground = Color(0xFFF0F6F8)
val LightOnBackground = Color(0xFF12202A)

val LightSurface = Color(0xFFFAFDFE)
val LightOnSurface = Color(0xFF12202A)

val LightSurfaceVariant = Color(0xFFD7E4EA)
val LightOnSurfaceVariant = Color(0xFF4B6574)

val LightOutline = Color(0xFF89A2AE)