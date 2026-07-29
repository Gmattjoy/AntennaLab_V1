package com.example.antennalab_v1.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.ui.theme.AntennaLabTheme

/*
########################################################################
FILE: StatusPill.kt
PACKAGE: com.example.antennalab_v1.ui.components
LAYER: UI / Shared components

The first shared primitive of the redesign: a compact status badge. It is
the atom the dashboard status card, device screen, project-list badges and
sweep viewer all reuse — and the real-widget proof that the semantic tokens
render in both light and dark.

Deliberately GENERIC (POSITIVE/CAUTION/NEGATIVE/NEUTRAL), not
calibration-specific — callers map their own state (live/simulated, cal
VALID/STALE/NONE, good/bad match) onto a level. Colour comes from
AntennaLabTheme.semantic as the foreground over a low-alpha tint of itself,
so no separate on-semantic colours are needed.
########################################################################
*/
enum class AppStatusLevel {
    POSITIVE,
    CAUTION,
    NEGATIVE,
    NEUTRAL
}

@Composable
fun StatusPill(
    text: String,
    level: AppStatusLevel,
    modifier: Modifier = Modifier
) {
    val semantic = AntennaLabTheme.semantic
    val color: Color = when (level) {
        AppStatusLevel.POSITIVE -> semantic.success
        AppStatusLevel.CAUTION -> semantic.warning
        AppStatusLevel.NEGATIVE -> semantic.danger
        AppStatusLevel.NEUTRAL -> semantic.neutral
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(color.copy(alpha = 0.14f))
            .padding(
                horizontal = AntennaLabTheme.spacing.md,
                vertical = AntennaLabTheme.spacing.xs
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status dot — filled with the solid semantic colour.
        Spacer(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(AntennaLabTheme.spacing.sm))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}
