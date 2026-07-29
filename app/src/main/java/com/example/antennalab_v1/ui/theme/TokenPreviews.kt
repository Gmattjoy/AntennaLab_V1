package com.example.antennalab_v1.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.ui.components.AppStatusLevel
import com.example.antennalab_v1.ui.components.StatusPill

/*
########################################################################
FILE: TokenPreviews.kt
PACKAGE: com.example.antennalab_v1.ui.theme
LAYER: UI / Theme / Design tokens (dev previews)

The foundation-only proof: a swatch sheet rendered in BOTH light and dark
so the tokens can be eyeballed without any screen to host them. This is
where the proposed light-mode semantic hex gets approved. Preview-only —
no runtime behaviour.
########################################################################
*/

@Preview(name = "Design tokens — dark", showBackground = true, widthDp = 360)
@Composable
private fun TokenSwatchSheetDarkPreview() {
    AntennaLab_V1Theme(darkTheme = true) { TokenSwatchSheet() }
}

@Preview(name = "Design tokens — light", showBackground = true, widthDp = 360)
@Composable
private fun TokenSwatchSheetLightPreview() {
    AntennaLab_V1Theme(darkTheme = false) { TokenSwatchSheet() }
}

@Composable
private fun TokenSwatchSheet() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AntennaLabTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AntennaLabTheme.spacing.lg)
        ) {
            SectionLabel("Semantic colours")
            SemanticSwatch("success", AntennaLabTheme.semantic.success)
            SemanticSwatch("warning", AntennaLabTheme.semantic.warning)
            SemanticSwatch("danger", AntennaLabTheme.semantic.danger)
            SemanticSwatch("neutral", AntennaLabTheme.semantic.neutral)
            SemanticSwatch("info", AntennaLabTheme.semantic.info)

            SectionLabel("StatusPill")
            Row(horizontalArrangement = Arrangement.spacedBy(AntennaLabTheme.spacing.sm)) {
                StatusPill("Live Ready", AppStatusLevel.POSITIVE)
                StatusPill("Stale", AppStatusLevel.CAUTION)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AntennaLabTheme.spacing.sm)) {
                StatusPill("Invalid", AppStatusLevel.NEGATIVE)
                StatusPill("Not Started", AppStatusLevel.NEUTRAL)
            }

            SectionLabel("Spacing scale")
            SpacingBar("xs", AntennaLabTheme.spacing.xs)
            SpacingBar("sm", AntennaLabTheme.spacing.sm)
            SpacingBar("md", AntennaLabTheme.spacing.md)
            SpacingBar("lg", AntennaLabTheme.spacing.lg)
            SpacingBar("xl", AntennaLabTheme.spacing.xl)
            SpacingBar("xxl", AntennaLabTheme.spacing.xxl)

            SectionLabel("Touch targets")
            TouchBox("min 48", AntennaLabTheme.touch.min)
            TouchBox("comfortable 56", AntennaLabTheme.touch.comfortable)
            TouchBox("field 64", AntennaLabTheme.touch.field)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun SemanticSwatch(name: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(AntennaLabTheme.touch.min)
                .clip(RoundedCornerShape(AntennaLabTheme.spacing.sm))
                .background(color)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = AntennaLabTheme.spacing.md)
        )
    }
}

@Composable
private fun SpacingBar(name: String, value: Dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp)
        )
        Box(
            modifier = Modifier
                .width(value)
                .height(AntennaLabTheme.spacing.md)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun TouchBox(name: String, value: Dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(value)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = AntennaLabTheme.spacing.md)
        )
    }
}
