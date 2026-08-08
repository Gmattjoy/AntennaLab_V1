package com.example.antennalab_v1.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.antennalab_v1.ui.theme.AntennaLabTheme

/*
########################################################################
FILE: AppActionButton.kt
PACKAGE: com.example.antennalab_v1.ui.components
LAYER: UI / Shared components

The shared action button. Two variants:
  PRIMARY  — sized to the FIELD touch target (gloved primary action,
             e.g. "Measure now"), title-medium label
  STANDARD — sized to the COMFORTABLE touch target, title-small label

Sizes come from the Phase-0 touch tokens so field usability is a token
decision, not a per-call guess.

BOTH variants are SOLID. Every AppActionButton is a standalone
call-to-action — it triggers something, it never represents "the chosen
one of these". With no unselected sibling beside it, a solid fill cannot
be misread as selection, so the discriminator is group membership rather
than emphasis (see SelectionButtonStyle.heroActionColors). The variants
therefore still differ in SIZE and TYPE WEIGHT only.

Controls that DO live in an option group — SegmentedChoiceButton, the
sweep display-mode / trace-math / marker rows — keep the solid-selected,
outlined-unselected split and must not use this button.
########################################################################
*/
enum class AppActionVariant { PRIMARY, STANDARD }

@Composable
fun AppActionButton(
    text: String,
    modifier: Modifier = Modifier,
    variant: AppActionVariant = AppActionVariant.STANDARD,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val minHeight =
        if (variant == AppActionVariant.PRIMARY) AntennaLabTheme.touch.field
        else AntennaLabTheme.touch.comfortable

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight),
        shape = RoundedCornerShape(AntennaLabTheme.spacing.md),
        colors = SelectionButtonStyle.heroActionColors(),
        border = SelectionButtonStyle.heroActionBorder(enabled = enabled),
        elevation = SelectionButtonStyle.heroActionElevation()
    ) {
        Text(
            text = text,
            style =
                if (variant == AppActionVariant.PRIMARY) MaterialTheme.typography.titleMedium
                else MaterialTheme.typography.titleSmall
        )
    }
}
