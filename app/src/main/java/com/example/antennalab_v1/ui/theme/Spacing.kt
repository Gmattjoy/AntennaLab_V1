package com.example.antennalab_v1.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
########################################################################
FILE: Spacing.kt
PACKAGE: com.example.antennalab_v1.ui.theme
LAYER: UI / Theme / Design tokens

The single spacing scale for the app, on a 4 dp grid. Reach it through
AntennaLabTheme.spacing so every screen consumes one source of truth
instead of scattering ad-hoc dp values (some current screens use off-grid
10/18 dp — those migrate onto this scale as each screen is redesigned).

Spacing does not vary by light/dark, so this is a plain object (no
CompositionLocal needed).
########################################################################
*/
object AntennaLabSpacing {
    /** 4 dp — hairline gaps, icon-to-text. */
    val xs: Dp = 4.dp

    /** 8 dp — tight intra-component spacing. */
    val sm: Dp = 8.dp

    /** 12 dp — default gap between rows in a card. */
    val md: Dp = 12.dp

    /** 16 dp — card padding, section spacing. */
    val lg: Dp = 16.dp

    /** 24 dp — between major sections. */
    val xl: Dp = 24.dp

    /** 32 dp — page-level separation. */
    val xxl: Dp = 32.dp
}
