package com.example.antennalab_v1.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.ui.theme.AntennaLabTheme

/*
########################################################################
FILE: SelectionButtonStyle.kt
PACKAGE: com.example.antennalab_v1.ui.components
LAYER: UI / Design system / Shared primitives

SYSTEM ROLE
The ONE definition of what a selected vs unselected button looks like.

  SELECTED    solid selectedIndicator fill, onSelectedIndicator label,
              matching border — "this option is the active choice"
  UNSELECTED  transparent fill, selectedIndicator border AND label —
              "this option is available"

  HERO       solid too, but for a STANDALONE action that belongs to no
             option group — see heroActionColors below for why that is
             not ambiguous

Secondary and tool actions (Set Reference, Clear Ref, marker nudges) take
the UNSELECTED treatment. No button anywhere carries a grey or green
border.

ANTI-DRIFT
Several button composables predate the design system and each rolled their
own colour logic (SegmentedChoiceButton, SweepWorkspaceActionButton,
SweepWorkspaceDisplayButton, AppActionButton, ProjectPageScreen's
primary/secondary pair). They now all call through here, so re-tinting or
restyling selection is one edit, not six. Add a state HERE, not per-screen.

Disabled colours are folded in because the sweep workspace buttons need
them; callers that never disable simply never see them.
########################################################################
*/
object SelectionButtonStyle {

    /** Border width shared by both states, so the two never jump by a pixel. */
    val BorderWidth = 1.dp

    @Composable
    fun colors(selected: Boolean): ButtonColors {
        val indicator = AntennaLabTheme.semantic.selectedIndicator
        val ink = AntennaLabTheme.semantic.onSelectedIndicator
        return if (selected) {
            ButtonDefaults.buttonColors(
                containerColor = indicator,
                contentColor = ink,
                disabledContainerColor = indicator.copy(alpha = 0.38f),
                disabledContentColor = ink.copy(alpha = 0.62f)
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = indicator,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = indicator.copy(alpha = 0.38f)
            )
        }
    }

    @Composable
    fun border(selected: Boolean, enabled: Boolean = true): BorderStroke {
        val indicator = AntennaLabTheme.semantic.selectedIndicator
        val alpha = if (enabled) if (selected) 0.98f else 0.85f else 0.32f
        return BorderStroke(BorderWidth, indicator.copy(alpha = alpha))
    }

    /**
     * Unselected buttons sit flat so the outline reads as an outline; a shadow
     * under a transparent fill just muddies the edge.
     */
    @Composable
    fun elevation(selected: Boolean) =
        ButtonDefaults.buttonElevation(
            defaultElevation = if (selected) 4.dp else 0.dp,
            pressedElevation = if (selected) 1.dp else 0.dp,
            disabledElevation = 0.dp
        )

    /*
    HERO ACTIONS — the one sanctioned way to be solid without being selected.

    A standalone call-to-action (Measure now, Back to Home, Back to grid) is
    not a member of any option group, so there is no sibling it could be
    confused with: nothing next to it is "the unselected one". That is what
    makes solid safe here and unsafe inside a group, where an outlined
    neighbour would read as merely unchosen.

    The rule is therefore about GROUP MEMBERSHIP, not importance. Do not
    reach for this to make one option in a row stand out — that is exactly
    the ambiguity the selected/unselected split exists to prevent.

    Delegates to the selected treatment rather than duplicating it, so the
    two can never drift apart visually.
    */
    @Composable
    fun heroActionColors(): ButtonColors = colors(selected = true)

    @Composable
    fun heroActionBorder(enabled: Boolean = true): BorderStroke =
        border(selected = true, enabled = enabled)

    @Composable
    fun heroActionElevation() = elevation(selected = true)

    /*
    M3 chips take their own colour type rather than ButtonColors, so they get
    their own accessors — same rule, same token, different plumbing. Without
    these, FilterChip falls back to the Material defaults: a grey outline and
    a surface-variant label, which is exactly the stray non-orange chrome the
    rest of this object exists to eliminate.
    */
    @Composable
    fun chipColors(): SelectableChipColors {
        val indicator = AntennaLabTheme.semantic.selectedIndicator
        val ink = AntennaLabTheme.semantic.onSelectedIndicator
        return FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = indicator,
            selectedContainerColor = indicator,
            selectedLabelColor = ink,
            disabledContainerColor = Color.Transparent,
            disabledLabelColor = indicator.copy(alpha = 0.38f)
        )
    }

    @Composable
    fun chipBorder(selected: Boolean, enabled: Boolean = true): BorderStroke =
        border(selected, enabled)

    /** For M3 OutlinedButton, whose border is a plain BorderStroke. */
    @Composable
    fun outlinedBorder(enabled: Boolean = true): BorderStroke =
        border(selected = false, enabled = enabled)
}
