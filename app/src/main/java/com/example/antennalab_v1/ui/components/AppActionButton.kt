package com.example.antennalab_v1.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
  PRIMARY  — accent fill, sized to the FIELD touch target (gloved primary
             action, e.g. "Measure now")
  STANDARD — secondary fill, sized to the COMFORTABLE touch target

Sizes come from the Phase-0 touch tokens so field usability is a token
decision, not a per-call guess. Colours come from the Material scheme.
########################################################################
*/
enum class AppActionVariant { PRIMARY, STANDARD }

@Composable
fun AppActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppActionVariant = AppActionVariant.STANDARD,
    enabled: Boolean = true
) {
    val minHeight =
        if (variant == AppActionVariant.PRIMARY) AntennaLabTheme.touch.field
        else AntennaLabTheme.touch.comfortable

    val colors =
        if (variant == AppActionVariant.PRIMARY) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight),
        shape = RoundedCornerShape(AntennaLabTheme.spacing.md),
        colors = colors
    ) {
        Text(
            text = text,
            style =
                if (variant == AppActionVariant.PRIMARY) MaterialTheme.typography.titleMedium
                else MaterialTheme.typography.titleSmall
        )
    }
}
