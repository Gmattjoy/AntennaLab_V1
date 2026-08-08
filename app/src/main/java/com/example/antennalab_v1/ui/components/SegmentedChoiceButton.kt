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
    val fillColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
        }

    val contentColor =
        if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    val borderColor =
        if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
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
