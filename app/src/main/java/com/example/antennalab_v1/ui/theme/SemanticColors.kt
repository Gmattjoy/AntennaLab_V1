package com.example.antennalab_v1.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
########################################################################
FILE: SemanticColors.kt
PACKAGE: com.example.antennalab_v1.ui.theme
LAYER: UI / Theme / Design tokens

Canonical semantic colour roles — status meaning, not brand hue:
  success  live-ready / valid / good match
  warning  degraded / stale / caution
  danger   invalid / failed / bad
  neutral  not-started / unknown / inactive
  info     informational accent

The ONE source of truth. Two ad-hoc palettes exist today and are migrated
onto this later (not in Phase 0): the dark-only, currently-unused
StatusGood/Warning/Bad in Color.kt, and the theme-blind
InstrumentGreen/Accent/Magenta in features/testing/SweepWorkspaceTheme.kt.

Usage model for pills/badges: the semantic colour is the FOREGROUND (dot +
text/border) over a low-alpha tint of ITSELF, so no separate "on-semantic"
colours are needed. See ui/components/StatusPill.kt.

These vary by light/dark, and the app forces its own dark flag independent
of the system, so the active set is chosen by the theme's flag and provided
via LocalAntennaLabSemanticColors — not by isSystemInDarkTheme().
########################################################################
*/
data class AntennaLabSemanticColors(
    val success: Color,
    val warning: Color,
    val danger: Color,
    val neutral: Color,
    val info: Color
)

/*
DARK set — reuses the existing StatusGood/Warning/Bad values (nothing new
invented for dark), plus neutral/info drawn from the dark scheme. These are
bright-on-dark for contrast on the forced-dark instrument surfaces.
*/
val DarkAntennaLabSemanticColors = AntennaLabSemanticColors(
    success = StatusGood,                 // 0xFF4BD37B
    warning = StatusWarning,              // 0xFFE0A84F
    danger = StatusBad,                   // 0xFFE06767
    neutral = Color(0xFFA9BAC5),          // = DarkOnSurfaceVariant
    info = Color(0xFF5AB0E0)
)

/*
LIGHT set — PROPOSED values, to be approved off the swatch preview rendered
in both modes. Constraint: legible on light surfaces AND readable outdoors
under glare. On a near-white surface a bright amber washes out, so `warning`
is a DEEP amber/orange with real contrast, not a pale one — likewise success
and danger are dark-saturated rather than pastel. Tweak these three
foreground hues here after the visual review.
*/
val LightAntennaLabSemanticColors = AntennaLabSemanticColors(
    success = Color(0xFF157347),          // deep green, readable on white
    warning = Color(0xFFB45309),          // deep amber/orange — glare-proof, not washy
    danger = Color(0xFFB3261E),           // dark red
    neutral = Color(0xFF4B6574),          // = LightOnSurfaceVariant
    info = Color(0xFF1565C0)              // strong blue
)

/**
 * Pure selector so the choice is testable and Theme.kt has one place to call.
 * The app forces its own dark flag, so this keys off that flag, not the system.
 */
fun antennaLabSemanticColors(darkTheme: Boolean): AntennaLabSemanticColors =
    if (darkTheme) DarkAntennaLabSemanticColors else LightAntennaLabSemanticColors

/** Provided in AntennaLab_V1Theme; defaults to dark (the app's baseline). */
val LocalAntennaLabSemanticColors = staticCompositionLocalOf {
    DarkAntennaLabSemanticColors
}
