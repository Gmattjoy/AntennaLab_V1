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
SECTION 0900
ACCENT ORANGE — THE ONE DEFINITION
########################################################################
PURPOSE
The app's single accent hue. Declared FIRST because everything below
initialises from it (Kotlin top-level vals initialise in file order).

Every orange in the app traces back to these two values: the Material
primary/onPrimary roles below, and the selectedIndicator semantic token
in SemanticColors.kt. Re-tinting the app is a one-line edit here.

Theme-blind on purpose — it holds up on the dark instrument shell and on
the near-white light surfaces, so there is no light/dark split. The ink is
a warm near-black rather than white: on this orange, white measures about
2.6:1 (fails AA) while the ink measures 5.27:1 (passes AA for normal
text). DesignTokensTest recomputes that ratio — if the orange is ever
re-tinted, the INK is what gets adjusted to keep it.
########################################################################
*/
val AccentOrange = Color(0xFFFF5C00)
val OnAccentOrange = Color(0xFF3A1500)

/*
########################################################################
SECTION 1000
DEFAULT DARK THEME COLORS
########################################################################
PURPOSE
Primary instrument theme inspired by modernized lab gear and late-analog
high-tech control panels.

The primary role WAS a metallic teal-green (0xFF6F9792). It is now the
accent orange: green is gone from the app's chrome entirely, and routing
the change through the Material role moves all ~50 primary call sites at
once instead of leaving per-screen stragglers. Status greens are NOT
affected — those live in the semantic colours and still mean "good".
########################################################################
*/
val DarkPrimary = AccentOrange
val DarkOnPrimary = OnAccentOrange

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
// Same accent in light mode — see SECTION 0900. The light ON-colour used to
// be near-white (0xFFF7FEFE), which would fail contrast on the orange.
val LightPrimary = AccentOrange
val LightOnPrimary = OnAccentOrange

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