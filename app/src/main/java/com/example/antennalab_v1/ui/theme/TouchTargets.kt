package com.example.antennalab_v1.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
########################################################################
FILE: TouchTargets.kt
PACKAGE: com.example.antennalab_v1.ui.theme
LAYER: UI / Theme / Design tokens

Minimum interactive sizes. Both bench and field are primary contexts, and
field means gloved hands outdoors, so these are first-class tokens, not an
afterthought. Reach them through AntennaLabTheme.touch.

Touch sizes do not vary by light/dark → plain object.
########################################################################
*/
object AntennaLabTouch {
    /**
     * 48 dp — the accessibility FLOOR (Material / WCAG minimum touch target).
     * Nothing tappable goes below this, including dense table rows and icon
     * buttons.
     */
    val min: Dp = 48.dp

    /** 56 dp — default size for ordinary interactive controls. */
    val comfortable: Dp = 56.dp

    /*
    ------------------------------------------------------------------
    field = 64 dp is THE dial to turn when the bench validates gloved use.
    It is the size of a PRIMARY field action (e.g. "Measure now") — big
    enough to hit reliably with a gloved fingertip outdoors. 64 dp is the
    STARTING value from field/gloved-use guidance (the 56–64 dp range above
    the 48 dp accessibility floor); it is NOT yet bench-validated. If gloved
    testing shows primary actions are still hard to hit, raise this one
    constant; if it dominates the layout, lower it toward `comfortable`. The
    rest of the touch scale is structural — this is the single knob.
    ------------------------------------------------------------------
    */
    val field: Dp = 64.dp
}
