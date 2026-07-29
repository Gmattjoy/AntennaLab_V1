package com.example.antennalab_v1.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/*
########################################################################
FILE: AntennaLabTheme.kt
PACKAGE: com.example.antennalab_v1.ui.theme
LAYER: UI / Theme / Design tokens

One import surface for the extended design tokens, mirroring how
MaterialTheme exposes colorScheme/typography. Screens read:

    AntennaLabTheme.spacing.md
    AntennaLabTheme.touch.field
    AntennaLabTheme.semantic.warning

Colours/typography still come from MaterialTheme.colorScheme /
MaterialTheme.typography — this only adds what Material does not model
(spacing, touch targets, semantic status colours).
########################################################################
*/
object AntennaLabTheme {
    val spacing: AntennaLabSpacing
        get() = AntennaLabSpacing

    val touch: AntennaLabTouch
        get() = AntennaLabTouch

    val semantic: AntennaLabSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAntennaLabSemanticColors.current
}
