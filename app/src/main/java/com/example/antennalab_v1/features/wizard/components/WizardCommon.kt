package com.example.antennalab_v1.features.wizard.components

/*
########################################################################
FILE: WizardCommon.kt
PURPOSE: Shared UI components used across the antenna wizard.

CONTAINS
- WizardHeader
- WorkflowCard
- OverviewInfoCard
- SectionTitle
- DecisionSupportCard
- SelectionSummaryCard
- WizardNav
- OptionRow
- ChoicePillRow
- AntennaTypeOptionCard
########################################################################
*/

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
########################################################################
SECTION: WIZARD THEME COLORS
PURPOSE: Central visual constants shared by wizard cards and controls.
SAFE EDITS:
- adjust dark theme shades
- refine border contrast
- refine subtle text balance
########################################################################
*/
private val WizardCardColor = Color(0xFF17181C)
private val WizardBorderColor = Color(0xFF23262D)
private val WizardTextColor = Color(0xFFF4F6F8)
private val WizardSubtleTextColor = Color(0xFFD8DDE3)
private val WizardMutedCardColor = Color(0xFF111318)
private val ButtonBlendColor = Color(0xFF141518)

/*
########################################################################
SECTION: WIZARD HEADER
PURPOSE: Shared top header for each wizard step.
SAFE EDITS:
- add optional subtitle later
- add progress indicators later
########################################################################
*/
@Composable
fun WizardHeader(
    step: String,
    title: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = WizardCardColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = step,
                fontSize = 13.sp,
                color = WizardSubtleTextColor
            )

            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = WizardTextColor
            )
        }
    }
}

/*
########################################################################
SECTION: SECTION TITLE
PURPOSE: Small reusable heading block used inside wizard pages to break
the page into clearer decision groups.
SAFE EDITS:
- add optional supporting text later
- add icon support later
########################################################################
*/
@Composable
fun SectionTitle(
    title: String,
    subtitle: String? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = WizardTextColor
        )

        subtitle?.let {
            Text(
                text = it,
                fontSize = 14.sp,
                color = WizardSubtleTextColor
            )
        }
    }
}

/*
########################################################################
SECTION: WORKFLOW CARD
PURPOSE: Explains what the current wizard step is doing.
SAFE EDITS:
- add emphasis styling later
- add compact variant later
########################################################################
*/
@Composable
fun WorkflowCard(
    title: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = WizardCardColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = WizardTextColor
            )

            Text(
                text = body,
                fontSize = 14.sp,
                color = WizardSubtleTextColor
            )
        }
    }
}

/*
########################################################################
SECTION: OVERVIEW INFO CARD
PURPOSE: Displays readable explanatory text for the current wizard
context.
SAFE EDITS:
- add optional action row later
- add bullet formatting later
########################################################################
*/
@Composable
fun OverviewInfoCard(
    title: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = WizardCardColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = WizardTextColor
            )

            Text(
                text = body,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = WizardSubtleTextColor
            )
        }
    }
}

/*
########################################################################
SECTION: DECISION SUPPORT CARD
PURPOSE: Gives short guided help near important choice areas. Useful for
novice flow and mid-step final selection guidance.
SAFE EDITS:
- add severity styles later
- add optional bullet list later
########################################################################
*/
@Composable
fun DecisionSupportCard(
    title: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = WizardMutedCardColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = WizardTextColor
            )

            Text(
                text = body,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = WizardSubtleTextColor
            )
        }
    }
}

/*
########################################################################
SECTION: SELECTION SUMMARY CARD
PURPOSE: Shows a compact summary of the choices made so far before the
user moves forward.
SAFE EDITS:
- add icons later
- add warning rows later
########################################################################
*/
@Composable
fun SelectionSummaryCard(
    title: String,
    rows: List<Pair<String, String>>
) {
    if (rows.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = WizardCardColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = WizardTextColor
            )

            rows.forEach { row ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = row.first,
                        fontSize = 12.sp,
                        color = WizardSubtleTextColor
                    )

                    Text(
                        text = row.second,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WizardTextColor
                    )
                }
            }
        }
    }
}

/*
########################################################################
SECTION: WIZARD NAVIGATION
PURPOSE: Shared bottom navigation row for wizard pages.
SAFE EDITS:
- add optional tertiary action later
- add disabled-state refinements later
########################################################################
*/
@Composable
fun WizardNav(
    onBack: () -> Unit,
    onNext: () -> Unit,
    nextLabel: String = "Next",
    nextEnabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Button(
            onClick = onBack,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonBlendColor,
                contentColor = WizardTextColor
            )
        ) {
            Text("Back")
        }

        Button(
            onClick = onNext,
            enabled = nextEnabled,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color(0xFF03131C)
            )
        ) {
            Text(nextLabel)
        }
    }
}

/*
########################################################################
SECTION: OPTION ROW
PURPOSE: Shared full-width row for simple single-choice selection.
SAFE EDITS:
- add trailing badges later
- add compact layout later
########################################################################
*/
@Composable
fun OptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        WizardBorderColor
    }

    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        WizardCardColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = WizardTextColor
        )
    }
}

/*
########################################################################
SECTION: CHOICE PILL ROW
PURPOSE: Shared horizontally scrollable small-choice control for things
like experience level, build style, or guidance mode.
SAFE EDITS:
- add icons later
- add multi-line labels later
########################################################################
*/
@Composable
fun ChoicePillRow(
    options: List<String>,
    selectedOption: String?,
    onOptionClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val selected = option == selectedOption
            val shape = RoundedCornerShape(50)

            val borderColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                WizardBorderColor
            }

            val backgroundColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            } else {
                WizardMutedCardColor
            }

            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(backgroundColor)
                    .border(1.dp, borderColor, shape)
                    .clickable { onOptionClick(option) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = WizardTextColor
                )
            }
        }
    }
}

/*
########################################################################
SECTION: ANTENNA TYPE OPTION CARD
PURPOSE: Larger option card used for antenna type selection with a
graphic and summary text.
SAFE EDITS:
- add tag rows later
- add difficulty badges later
########################################################################
*/
@Composable
fun AntennaTypeOptionCard(
    label: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    graphic: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    } else {
        WizardBorderColor
    }

    val backgroundColor = if (selected) {
        Color(0xFF1A1D23)
    } else {
        WizardCardColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            graphic()
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = WizardTextColor
            )

            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = WizardSubtleTextColor
            )
        }
    }
}