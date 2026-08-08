package com.example.antennalab_v1.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.features.app.HomeIcons
import com.example.antennalab_v1.ui.theme.AntennaLabTheme

/*
########################################################################
FILE: CollapsibleSection.kt
PACKAGE: com.example.antennalab_v1.ui.components
LAYER: UI / Design system / Shared primitives

SYSTEM ROLE
A titled section that expands and collapses in place. The app's first
collapsible container, introduced in slice 5f for spec 2.3's "app
analysis" — the diagnostics + tuning-interpretation panels, collapsed by
default so the app's interpretation never outranks the measurement.

A SECTION, NOT A CARD, and the name says so deliberately.
Its content is typically already-carded panels, each with its own
surface, border, elevation and title. Wrapping those in another Card
would double every one of those. The codebase already ruled on exactly
this: SweepGraphScreen renders MarkerReadoutTable bare because it
"supplies its own bordered Surface and its own title, so wrapping it
would duplicate both". So this draws a header row and nothing else, and
reads like the screen's existing section headers with an affordance
attached.

STATELESS
The caller owns `expanded`. That matches how collapse state is held in
this app — SweepWorkspaceState, via a pure controller toggle — rather
than an internal remember that would be invisible to tests and lost on
navigation.

TOUCH TARGET
The whole header row is the target and it is pinned to
AntennaLabTouch.min (48 dp). Deliberately NOT inheriting
SegmentedChoiceButton's sub-floor default: that gap is on the audit's
open-cleanup list precisely so it does not get repeated in new work.

CHEVRON COLOUR
Neutral (onSurfaceVariant), never the orange indicator. Orange means
SELECTED in this app; expand/collapse is structural state, not a
selection, and colouring it would dilute the one thing orange says.

DEPENDENCY NOTE
androidx.compose.animation is not declared in app/build.gradle.kts — it
arrives transitively on the compile classpath via material3, which
exposes it as `api`. Verified before use rather than assumed. If a future
material3 stops exposing it, the fix is a one-line implementation plus a
version-catalog entry, not a redesign of this file.
########################################################################
*/
@Composable
fun CollapsibleSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // 90 deg points the chevron down when open: the affordance shows the
    // direction the content went, which is the convention users already read.
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        label = "collapsibleSectionChevron"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AntennaLabTheme.spacing.sm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                /*
                clickable BEFORE heightIn so the ripple and the hit area both
                cover the full 48 dp, not just the text's own height.
                */
                .clickable(onClick = onToggle)
                .heightIn(min = AntennaLabTheme.touch.min),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            /*
            Matches SharedInstrumentSectionHeader's treatment — normal weight,
            primary text colour, no accent — so this sits level with the other
            section headers rather than announcing itself as something new.
            */
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal
            )

            Icon(
                imageVector = HomeIcons.ChevronRight,
                contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(chevronRotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(AntennaLabTheme.spacing.sm)
            ) {
                content()
            }
        }
    }
}
