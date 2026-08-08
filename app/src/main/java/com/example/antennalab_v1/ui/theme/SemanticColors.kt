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

Plus one INTERACTION-state role, which is a different kind of token and is
documented separately below:
  selectedIndicator / onSelectedIndicator
      the app-wide "this option is the active choice" fill + its label

The ONE source of truth. Two ad-hoc palettes exist today and are migrated
onto this later (not in Phase 0): the dark-only, currently-unused
StatusGood/Warning/Bad in Color.kt, and the theme-blind
InstrumentGreen/Accent/Magenta in features/testing/SweepWorkspaceTheme.kt.

Usage model for pills/badges: the semantic colour is the FOREGROUND (dot +
text/border) over a low-alpha tint of ITSELF, so no separate "on-semantic"
colours are needed. See ui/components/StatusPill.kt.

`selectedIndicator` is the ONE exception to that usage model, and the reason
it carries an explicit `onSelectedIndicator`: it is a FILL, not a foreground.
It answers "which option is active?" for mutually-exclusive controls
app-wide — today ui/components/SegmentedChoiceButton.kt (the App Settings
theme selector and the sweep display-mode row). Named for the ROLE, not the
hue, so a future re-tint changes one value here and nothing else.

Deliberately NOT colorScheme.primary: primary is the general action accent
(Back to Home and ~50 other call sites), so selection could not be re-tinted
without dragging every action button with it. Selection now has its own
token; primary is untouched.

The clash between this orange and `warning` (#B45309 in light) was reviewed
and accepted — they never share a surface, and selection is a fill while
warning is a foreground.

These vary by light/dark. The active set is chosen by the theme's own flag
and provided via LocalAntennaLabSemanticColors, so consumers read the
CompositionLocal and never call isSystemInDarkTheme() themselves — that
call belongs at the composition root and nowhere else.

The flag used to be forced dark independent of the system. Slice 5d
reversed that: it now comes from AppSettings.themePreference, resolved by
resolveDarkTheme at MainActivity, where SYSTEM defers to
isSystemInDarkTheme() while DARK and LIGHT override it. The indirection
below is unchanged — only where the flag originates.
########################################################################
*/
data class AntennaLabSemanticColors(
    val success: Color,
    val warning: Color,
    val danger: Color,
    val neutral: Color,
    val info: Color,
    val selectedIndicator: Color,
    val onSelectedIndicator: Color
)

/*
Shared by both schemes: the selected/active indicator is a saturated neon
orange that holds up on the dark instrument shell AND on the near-white
light surfaces, so there is no light/dark split to maintain. Its label is a
warm near-black rather than white — on #FF5C00 white lands at ~2.6:1 (fails
AA) while this reaches ~5.3:1 (passes AA for normal text). Pinned by
DesignTokensTest, which recomputes the WCAG ratio: if the orange is ever
re-tinted, adjust the LABEL to keep the ratio, not the other way round.
*/
private val SelectedIndicatorOrange = Color(0xFFFF5C00)
private val OnSelectedIndicatorInk = Color(0xFF3A1500)

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
    info = Color(0xFF5AB0E0),
    selectedIndicator = SelectedIndicatorOrange,
    onSelectedIndicator = OnSelectedIndicatorInk
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
    info = Color(0xFF1565C0),             // strong blue
    selectedIndicator = SelectedIndicatorOrange,
    onSelectedIndicator = OnSelectedIndicatorInk
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
