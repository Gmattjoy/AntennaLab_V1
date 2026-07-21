package com.example.antennalab_v1.features.wizard.steps

/*
########################################################################
FILE: Step2AntennaOverviewScreen.kt
PACKAGE: com.example.antennalab_v1.features.wizard.steps
LAYER: UI / Wizard Exploration

SYSTEM ROLE
Wizard step 2 overview for the selected antenna type.

PROJECT DIRECTION
• Step 2 is the guided explanation and confirmation stage after the
  intake and recommendation step
• the user can still change antenna type here without going back
• this screen should explain how the chosen antenna works in plain
  language before the user commits to project setup
• beginners should leave this step understanding why the antenna was
  suggested and what tradeoffs it brings

CURRENT DEVELOPMENT ROLE
This screen sits between the guided intake step and the project /
frequency setup step.

Right now this file is responsible for:

• showing a visual overview of the selected antenna family
• letting the user compare and switch antenna families
• teaching beginner-friendly tradeoffs
• helping the user continue with more confidence into Step 3

SAFE EDIT AREA
- add more antenna types later
- improve visual teaching content later
- refine beginner language later
- add richer comparison content later
########################################################################
*/

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.antennalab_v1.features.wizard.components.DecisionSupportCard
import com.example.antennalab_v1.features.wizard.components.OverviewInfoCard
import com.example.antennalab_v1.features.wizard.components.SectionTitle
import com.example.antennalab_v1.features.wizard.components.SelectionSummaryCard
import com.example.antennalab_v1.features.wizard.components.WizardHeader
import com.example.antennalab_v1.features.wizard.components.WizardNav
import com.example.antennalab_v1.features.wizard.graphics.DipoleGraphic
import com.example.antennalab_v1.features.wizard.graphics.LoopGraphic
import com.example.antennalab_v1.features.wizard.graphics.UnknownGraphic
import com.example.antennalab_v1.features.wizard.graphics.VerticalGraphic
import com.example.antennalab_v1.features.wizard.graphics.YagiGraphic

/*
########################################################################
EDIT SECTION 1001
STEP VISUAL STYLE CONSTANTS
------------------------------------------------------------------------
PURPOSE
Defines the local visual styling used by the Step 2 exploration screen.

SAFE EDIT AREA
- refine colors later
- align wizard visual theme later
########################################################################
*/
private val Step2CardColor = Color(0xFF17181C)
private val Step2BorderColor = Color(0xFF23262D)
private val Step2TextColor = Color(0xFFF4F6F8)
private val Step2SubtleTextColor = Color(0xFFD8DDE3)
private val Step2MutedTextColor = Color(0xFFB4BBC4)
private val Step2ButtonBlendColor = Color(0xFF141518)

/*
########################################################################
EDIT SECTION 1002
OVERVIEW DATA MODEL
------------------------------------------------------------------------
PURPOSE
Defines the UI-facing data model used to describe an antenna family for
the explanation screen.

SAFE EDIT AREA
- add more educational fields later
- add richer comparison metrics later
########################################################################
*/
data class AntennaOverview(
    val title: String,
    val shortDescription: String,
    val howItWorks: String,
    val whyItFits: String,
    val frequencyGuidance: String,
    val commonUses: String,
    val limitations: String,
    val visualFrequencyRange: String,
    val visualTypicalSize: String,
    val learningFit: String,
    val directionStyle: String,
    val buildDifficulty: String,
    val beginnerSummary: String,
    val suitableFor: String,
    val notIdealFor: String,
    val buildOverview: String,
    val frequencyProgress: Float,
    val sizeProgress: Float,
    val difficultyProgress: Float
)

/*
########################################################################
EDIT SECTION 1003
OVERVIEW DATA SOURCE
------------------------------------------------------------------------
PURPOSE
Maps the current antenna type into a beginner-friendly overview model.

SAFE EDIT AREA
- add more antenna families later
- refine educational wording later
- improve comparison values later
########################################################################
*/
fun antennaOverviewFor(antennaType: String): AntennaOverview {
    return when (antennaType) {
        "Dipole" -> AntennaOverview(
            title = "Dipole",
            shortDescription = "A simple antenna with two straight sides fed in the middle.",
            howItWorks = "A dipole radiates from two conductors that work together around a center feed point. Its overall length is closely tied to the intended frequency.",
            whyItFits = "Dipole is often the safest general recommendation because it is simple to understand, easy to compare against calculations, and useful as a first tuning reference.",
            frequencyGuidance = "Works across many frequency ranges depending on its physical length.",
            commonUses = "General radio communication, amateur radio, and learning projects.",
            limitations = "Can become physically large at lower frequencies.",
            visualFrequencyRange = "HF to UHF, depending on length",
            visualTypicalSize = "Medium to large at lower bands",
            learningFit = "Excellent beginner antenna",
            directionStyle = "Mostly broadside, general coverage",
            buildDifficulty = "Easy",
            beginnerSummary = "This is one of the easiest antennas to understand and build. If you want a clean starting point for learning dimensions and tuning, this is usually the safest choice.",
            suitableFor = "Learning, simple builds, general-purpose radio work",
            notIdealFor = "Very compact low-frequency builds",
            buildOverview = "Typical builds use two straight conductors, a center feed point, and enough space to stretch the antenna to its required length.",
            frequencyProgress = 0.72f,
            sizeProgress = 0.68f,
            difficultyProgress = 0.22f
        )

        "Vertical" -> AntennaOverview(
            title = "Vertical",
            shortDescription = "A single upright radiator often used in practical installations.",
            howItWorks = "A vertical usually uses one main upright conductor and depends strongly on its return path, radials, or ground reference to work properly.",
            whyItFits = "Vertical is often recommended for handheld, mobile, and practical mounted installations because its form matches those use cases well.",
            frequencyGuidance = "Commonly used from HF through VHF and UHF depending on design.",
            commonUses = "Mobile antennas, base stations, and general outdoor radio use.",
            limitations = "Performance often depends strongly on the ground system.",
            visualFrequencyRange = "HF to UHF",
            visualTypicalSize = "Slim shape, size varies with band",
            learningFit = "Good beginner path",
            directionStyle = "Omnidirectional style coverage",
            buildDifficulty = "Easy to moderate",
            beginnerSummary = "A vertical is easy to picture because it is mostly one upright element. It is practical and common, but it depends more on radials or ground quality than many beginners first expect.",
            suitableFor = "Outdoor installs, mobile style use, all-round coverage",
            notIdealFor = "Situations with poor ground system options",
            buildOverview = "Typical builds use one upright radiator with a mounting arrangement that provides the required return path or radial support.",
            frequencyProgress = 0.76f,
            sizeProgress = 0.56f,
            difficultyProgress = 0.36f
        )

        "Yagi" -> AntennaOverview(
            title = "Yagi",
            shortDescription = "A directional antenna with multiple elements on a boom.",
            howItWorks = "A Yagi uses several elements working together so energy is favored in one main direction. Element spacing and tuning matter more than in simpler families.",
            whyItFits = "Yagi is often a better fit when the goal is more focused gain, point-to-point work, or directional performance instead of all-direction coverage.",
            frequencyGuidance = "Most common on VHF, UHF, and higher frequencies.",
            commonUses = "Repeater work, directional links, TV-style arrays, and signal hunting.",
            limitations = "Needs pointing direction and is more mechanically complex.",
            visualFrequencyRange = "Mostly VHF and UHF",
            visualTypicalSize = "Compact to medium",
            learningFit = "Best after basics are understood",
            directionStyle = "Directional, aimed at one direction",
            buildDifficulty = "Moderate",
            beginnerSummary = "A Yagi is powerful because it focuses signal in one direction. Beginners understand it faster when they can see the elements, boom length, and the trade-off between gain and pointing accuracy.",
            suitableFor = "Direction finding, target links, repeaters, focused gain",
            notIdealFor = "Wide area all-direction coverage",
            buildOverview = "Typical builds use a boom with several aligned elements, accurate spacing, and a mounting position that allows aiming.",
            frequencyProgress = 0.48f,
            sizeProgress = 0.40f,
            difficultyProgress = 0.58f
        )

        "Loop" -> AntennaOverview(
            title = "Loop",
            shortDescription = "A closed circular or square antenna shape.",
            howItWorks = "A loop uses a closed conductor path rather than open straight arms. Its behavior depends heavily on loop size, shape, and the exact loop type.",
            whyItFits = "Loop may be a better choice when the support shape, compact form, or specific behavior matters more than choosing the simplest general family.",
            frequencyGuidance = "Can be used across many ranges depending on design and size.",
            commonUses = "Compact installations, special-purpose builds, and some receiving antennas.",
            limitations = "Performance varies a lot between loop types.",
            visualFrequencyRange = "HF to UHF, design dependent",
            visualTypicalSize = "Compact to medium",
            learningFit = "Good once shape differences are understood",
            directionStyle = "Varies with loop style and mounting",
            buildDifficulty = "Moderate",
            beginnerSummary = "A loop is visually easy to recognise, but there are many loop types. It is useful when space, support shape, or specific behaviour matters more than using the simplest antenna style.",
            suitableFor = "Compact shapes, experiments, receiving and specialty builds",
            notIdealFor = "Users wanting one simple universal answer",
            buildOverview = "Typical builds use a closed conductor form with dimensions and feed arrangement chosen for the intended loop type.",
            frequencyProgress = 0.66f,
            sizeProgress = 0.46f,
            difficultyProgress = 0.52f
        )

        else -> AntennaOverview(
            title = "Other",
            shortDescription = "A flexible path for users who are not yet sure of the antenna type.",
            howItWorks = "This path is not tied to one clear family yet, so the next steps need to narrow the build direction first.",
            whyItFits = "This is useful when the user is still exploring and needs more help before committing to one family.",
            frequencyGuidance = "Frequency guidance depends on the final design choice.",
            commonUses = "Early-stage projects and uncertain starting points.",
            limitations = "The app will need more user input later.",
            visualFrequencyRange = "Not fixed yet",
            visualTypicalSize = "Unknown until design is chosen",
            learningFit = "Good for exploration",
            directionStyle = "Depends on the antenna selected later",
            buildDifficulty = "Unknown",
            beginnerSummary = "This option is useful when the user does not yet know the final antenna style. The next steps should guide the project toward a more specific design.",
            suitableFor = "Early planning and comparison",
            notIdealFor = "Fast final dimensioning without more decisions",
            buildOverview = "The build structure will depend entirely on the final family chosen in later steps.",
            frequencyProgress = 0.20f,
            sizeProgress = 0.20f,
            difficultyProgress = 0.50f
        )
    }
}

/*
########################################################################
EDIT SECTION 1004
TOP-LEVEL SCREEN
------------------------------------------------------------------------
PURPOSE
Renders the antenna explanation screen for Step 2.

This screen allows the user to:
• review the selected antenna type
• change antenna type without going back
• compare beginner-friendly tradeoffs
• continue into project/frequency setup

SAFE EDIT AREA
- add more teaching cards later
- add richer comparisons later
- add mode-specific paths later
########################################################################
*/
@Composable
fun CreateWizardStep2OverviewScreen(
    antennaType: String,
    onAntennaTypeChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val overview = antennaOverviewFor(antennaType)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        WizardHeader(
            step = "Step 2 of 4",
            title = "Understand the selected antenna"
        )

        DecisionSupportCard(
            title = "This step explains the family before project setup",
            body = "Review how this antenna works, why it may fit your use, and what tradeoffs to expect. You can still change family here before moving on."
        )

        /*
        ################################################################
        EDIT SECTION 1005
        NAVIGATION CONTROLS
        ----------------------------------------------------------------
        PURPOSE
        Provides simple back and next actions for the explanation step.
        ################################################################
        */
        WizardNav(
            onBack = onBack,
            onNext = onNext,
            nextLabel = "Next",
            nextEnabled = antennaType.isNotBlank()
        )

        /*
        ################################################################
        EDIT SECTION 1006
        FAMILY SWITCHER
        ----------------------------------------------------------------
        PURPOSE
        Lets the user compare and switch antenna families directly within
        Step 2.
        ################################################################
        */
        AntennaTypeSelectorCard(
            selectedType = antennaType,
            onAntennaTypeChange = onAntennaTypeChange
        )

        SelectionSummaryCard(
            title = "Current antenna decision",
            rows = listOf(
                "Selected family" to overview.title,
                "Difficulty" to overview.buildDifficulty,
                "Coverage style" to overview.directionStyle
            )
        )

        /*
        ################################################################
        EDIT SECTION 1007
        HERO OVERVIEW CARD
        ----------------------------------------------------------------
        PURPOSE
        Shows the selected antenna graphic and the main one-line
        description for the current type.
        ################################################################
        */
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Step2CardColor
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(92.dp)) {
                    when (antennaType) {
                        "Dipole" -> DipoleGraphic()
                        "Vertical" -> VerticalGraphic()
                        "Yagi" -> YagiGraphic()
                        "Loop" -> LoopGraphic()
                        else -> UnknownGraphic()
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = overview.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Step2TextColor
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = overview.shortDescription,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        color = Step2SubtleTextColor
                    )
                }
            }
        }

        OverviewInfoCard(
            title = "How this antenna works",
            body = overview.howItWorks
        )

        OverviewInfoCard(
            title = "Why this family may fit your project",
            body = overview.whyItFits
        )

        VisualQuickStatsCard(overview = overview)
        VisualBarsCard(overview = overview)
        BeginnerFitCard(overview = overview)

        OverviewInfoCard(
            title = "Typical Frequency Guidance",
            body = overview.frequencyGuidance
        )

        OverviewInfoCard(
            title = "Common Uses",
            body = overview.commonUses
        )

        OverviewInfoCard(
            title = "Limitations",
            body = overview.limitations
        )

        OverviewInfoCard(
            title = "Build Overview",
            body = overview.buildOverview
        )

        DecisionSupportCard(
            title = "Decision checkpoint",
            body = "If this antenna family matches the kind of installation and performance you want, continue to the next step. If not, switch family here before moving on."
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

/*
########################################################################
EDIT SECTION 2001
ANTENNA TYPE SELECTOR CARD
------------------------------------------------------------------------
PURPOSE
Lets the user compare and switch antenna types directly within Step 2.

SAFE EDIT AREA
- add more antenna types later
- add grouping or categories later
########################################################################
*/
@Composable
private fun AntennaTypeSelectorCard(
    selectedType: String,
    onAntennaTypeChange: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Step2CardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Try a different family",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Step2TextColor
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "You can change antenna family here without going back.",
                fontSize = 14.sp,
                color = Step2SubtleTextColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Dipole", "Vertical").forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { onAntennaTypeChange(type) },
                        label = { Text(type) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            selectedLabelColor = Step2TextColor,
                            containerColor = Step2ButtonBlendColor,
                            labelColor = Step2SubtleTextColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Yagi", "Loop").forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { onAntennaTypeChange(type) },
                        label = { Text(type) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            selectedLabelColor = Step2TextColor,
                            containerColor = Step2ButtonBlendColor,
                            labelColor = Step2SubtleTextColor
                        )
                    )
                }
            }
        }
    }
}

/*
########################################################################
EDIT SECTION 2002
VISUAL QUICK STATS CARD
------------------------------------------------------------------------
PURPOSE
Presents a compact at-a-glance summary of key antenna traits.

SAFE EDIT AREA
- add more quick stats later
- refine wording later
########################################################################
*/
@Composable
private fun VisualQuickStatsCard(
    overview: AntennaOverview
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Step2CardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick View",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Step2TextColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            QuickStatRow(
                label = "Frequency range",
                value = overview.visualFrequencyRange
            )

            Spacer(modifier = Modifier.height(10.dp))

            QuickStatRow(
                label = "Typical size",
                value = overview.visualTypicalSize
            )

            Spacer(modifier = Modifier.height(10.dp))

            QuickStatRow(
                label = "Coverage style",
                value = overview.directionStyle
            )

            Spacer(modifier = Modifier.height(10.dp))

            QuickStatRow(
                label = "Difficulty",
                value = overview.buildDifficulty
            )
        }
    }
}

/*
########################################################################
EDIT SECTION 2003
QUICK STAT ROW
------------------------------------------------------------------------
PURPOSE
Displays a label/value row used by the quick stats card.

SAFE EDIT AREA
- adjust spacing later
- align with shared component system later
########################################################################
*/
@Composable
private fun QuickStatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.widthIn(min = 104.dp),
            fontSize = 14.sp,
            color = Step2MutedTextColor
        )

        Text(
            text = value,
            fontSize = 14.sp,
            color = Step2SubtleTextColor
        )
    }
}

/*
########################################################################
EDIT SECTION 2004
VISUAL BARS CARD
------------------------------------------------------------------------
PURPOSE
Shows simple progress-bar style comparisons for flexibility, size, and
difficulty.

SAFE EDIT AREA
- add more comparison bars later
- refine scales later
########################################################################
*/
@Composable
private fun VisualBarsCard(
    overview: AntennaOverview
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Step2CardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Visual Guide",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Step2TextColor
            )

            Spacer(modifier = Modifier.height(14.dp))

            VisualMeterRow(
                label = "Frequency flexibility",
                valueText = overview.visualFrequencyRange,
                progress = overview.frequencyProgress
            )

            Spacer(modifier = Modifier.height(14.dp))

            VisualMeterRow(
                label = "Physical size",
                valueText = overview.visualTypicalSize,
                progress = overview.sizeProgress
            )

            Spacer(modifier = Modifier.height(14.dp))

            VisualMeterRow(
                label = "Build difficulty",
                valueText = overview.buildDifficulty,
                progress = overview.difficultyProgress
            )
        }
    }
}

/*
########################################################################
EDIT SECTION 2005
VISUAL METER ROW
------------------------------------------------------------------------
PURPOSE
Displays a single labelled progress comparison row.

SAFE EDIT AREA
- add scales or labels later
- improve visual clarity later
########################################################################
*/
@Composable
private fun VisualMeterRow(
    label: String,
    valueText: String,
    progress: Float
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Step2TextColor
            )

            Text(
                text = valueText,
                fontSize = 13.sp,
                color = Step2MutedTextColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Step2BorderColor
        )
    }
}

/*
########################################################################
EDIT SECTION 2006
BEGINNER FIT CARD
------------------------------------------------------------------------
PURPOSE
Summarises whether the selected antenna style is a good learning fit
and what it is best or less suited for.

SAFE EDIT AREA
- add more beginner guidance later
- add recommendation scoring later
########################################################################
*/
@Composable
private fun BeginnerFitCard(
    overview: AntennaOverview
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Step2CardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Beginner Summary",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Step2TextColor
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = overview.beginnerSummary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Step2SubtleTextColor
            )

            Spacer(modifier = Modifier.height(14.dp))

            SuitabilityRow(
                title = "Good for",
                body = overview.suitableFor
            )

            Spacer(modifier = Modifier.height(10.dp))

            SuitabilityRow(
                title = "Less ideal for",
                body = overview.notIdealFor
            )

            Spacer(modifier = Modifier.height(10.dp))

            SuitabilityRow(
                title = "Learning fit",
                body = overview.learningFit
            )
        }
    }
}

/*
########################################################################
EDIT SECTION 2007
SUITABILITY ROW
------------------------------------------------------------------------
PURPOSE
Displays a simple bullet-style label/body row for the beginner summary
card.

SAFE EDIT AREA
- replace with shared component later
- improve styling later
########################################################################
*/
@Composable
private fun SuitabilityRow(
    title: String,
    body: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )

        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Step2TextColor
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = body,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Step2SubtleTextColor
            )
        }
    }
}