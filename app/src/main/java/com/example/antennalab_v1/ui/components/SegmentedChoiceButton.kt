package com.example.antennalab_v1.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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

COLOUR
Both states now come from SelectionButtonStyle — solid orange when
selected, orange outline on a transparent fill when not. This file no
longer decides anything about colour, which is the point: the same rule
drives the sweep display-mode row, the trace-math row, the marker row and
every plain action button. The orange/warning-amber clash was reviewed and
accepted; rationale and contrast numbers live in ui/theme/SemanticColors.kt.

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
    Button(
        onClick = onClick,
        colors = SelectionButtonStyle.colors(selected),
        border = SelectionButtonStyle.border(selected),
        elevation = SelectionButtonStyle.elevation(selected),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = text,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
