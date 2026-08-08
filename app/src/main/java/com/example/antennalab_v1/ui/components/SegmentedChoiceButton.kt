package com.example.antennalab_v1.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.ui.theme.AntennaLabTheme

/*
########################################################################
FILE: SegmentedChoiceButton.kt
PACKAGE: com.example.antennalab_v1.ui.components
LAYER: UI / Design system / Shared primitives

SYSTEM ROLE
One option in a row of mutually-exclusive choices — the app's segmented
control, built from a plain Button rather than M3's SegmentedButton
(which is used nowhere in this codebase).

HISTORY
Lifted verbatim from SweepGraphWidgets.SweepWorkspaceModeButton in slice
5d, when the theme setting needed the same three-of-a-row shape the
sweep display-mode chips already had. It was always a general control
wearing a sweep-specific name: public, no instrument* colour params, and
every colour resolved from MaterialTheme.colorScheme. Pure move and
rename, no behaviour change.

SELECTED COLOUR
The selected fill is AntennaLabTheme.semantic.selectedIndicator (neon
orange #FF5C00), not colorScheme.primary. Primary is the general action
accent shared with ~50 other call sites (Back to Home et al), so selection
could not be re-tinted there without moving every action button too. The
label is the paired onSelectedIndicator so it stays legible on the fill.
UNSELECTED state is untouched and still resolves from colorScheme.
The orange/warning-amber clash was reviewed and accepted; rationale and
contrast numbers live in ui/theme/SemanticColors.kt.

KNOWN GAP, deliberately not fixed in 5d
No minimum height, so this renders at Material's default (~40 dp) —
BELOW the 48 dp AntennaLabTouch.min accessibility floor that
AppActionButton respects. Promoting it to a design-system primitive
arguably blesses that, so it is logged in claude/ui-audit.md rather than
left silent. Fixing it would resize the 13 sweep-stack call sites, which
is a pixel change that does not belong in a theme slice.
########################################################################
*/
@Composable
fun SegmentedChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val selectedIndicator = AntennaLabTheme.semantic.selectedIndicator

    val fillColor =
        if (selected) {
            selectedIndicator
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
        }

    val contentColor =
        if (selected) {
            AntennaLabTheme.semantic.onSelectedIndicator
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    // The border tracks the fill: a primary-green ring around an orange fill
    // would just read as a rendering bug.
    val borderColor =
        if (selected) {
            selectedIndicator.copy(alpha = 0.95f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
        }

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = fillColor,
            contentColor = contentColor
        ),
        border = BorderStroke(
            width = 1.dp,
            color = borderColor
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (selected) 4.dp else 2.dp,
            pressedElevation = 1.dp
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = text,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
