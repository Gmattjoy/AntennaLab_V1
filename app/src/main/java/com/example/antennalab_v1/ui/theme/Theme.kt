package com.example.antennalab_v1.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/*
########################################################################
FILE: Theme.kt
PACKAGE: com.example.antennalab_v1.ui.theme
LAYER: UI / Theme / Material Theme

LAST UPDATED 4/4/2026 20:28

SYSTEM ROLE
Applies the active AntennaLab V1 Material theme.

CURRENT DEVELOPMENT ROLE
This version keeps the theme structure while applying a hard visual
change:

• dark mode forced during active development
• blue-slate instrument surfaces
• teal/cyan primary actions
• cooler text hierarchy
• yellow/brass removed from primary UI emphasis

SAFE EDIT AREA
- tune light scheme later
- add optional user-selectable theme override later
- add panel elevation tokens later
########################################################################
*/

/*
########################################################################
SECTION 1000
DARK COLOR SCHEME
########################################################################
*/
private val AntennaLabDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

/*
########################################################################
SECTION 2000
LIGHT COLOR SCHEME
########################################################################
*/
private val AntennaLabLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

/*
########################################################################
SECTION 3000
APP THEME ENTRY POINT
########################################################################
PURPOSE
Dark mode remains the active design baseline for development while the
instrument theme is rolled out to the rest of the app.
########################################################################
*/
@Composable
fun AntennaLab_V1Theme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme =
        if (darkTheme) {
            AntennaLabDarkColorScheme
        } else {
            AntennaLabLightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}