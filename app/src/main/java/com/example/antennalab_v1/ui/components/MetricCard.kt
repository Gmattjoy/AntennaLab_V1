package com.example.antennalab_v1.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.ui.theme.AntennaLabTheme

/*
########################################################################
FILE: MetricCard.kt
PACKAGE: com.example.antennalab_v1.ui.components
LAYER: UI / Shared components

A titled surface with an optional subtitle and a content slot. The dashboard
status card and recent-project rows are built from it. Consumes the Phase-0
spacing/touch tokens and the Material colour roles (surfaces come from the
colour scheme; MetricCard adds structure, not new colours).

When onClick is set the whole card is tappable and honours the minimum
touch target so it stays hittable with a gloved hand.
########################################################################
*/
@Composable
fun MetricCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val clickModifier =
        if (onClick != null) {
            Modifier
                .clickable(onClick = onClick)
                .heightIn(min = AntennaLabTheme.touch.min)
        } else {
            Modifier
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AntennaLabTheme.spacing.md),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .then(clickModifier)
                .fillMaxWidth()
                .padding(AntennaLabTheme.spacing.lg)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    modifier = Modifier.padding(top = AntennaLabTheme.spacing.xs),
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}
