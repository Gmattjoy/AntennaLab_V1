package com.example.antennalab_v1.features.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.antennalab_v1.domain.testing.UsbSessionManager
import com.example.antennalab_v1.model.ProjectListItem
import com.example.antennalab_v1.storage.ProjectStorage
import com.example.antennalab_v1.ui.components.AppActionButton
import com.example.antennalab_v1.ui.components.AppActionVariant
import com.example.antennalab_v1.ui.components.AppStatusLevel
import com.example.antennalab_v1.ui.components.MetricCard
import com.example.antennalab_v1.ui.components.StatusPill
import com.example.antennalab_v1.ui.theme.AntennaLabTheme
import com.example.antennalab_v1.ui.theme.AntennaLab_V1Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
########################################################################
FILE: DashboardScreen.kt
PACKAGE: com.example.antennalab_v1.features.app
LAYER: UI / App / Dashboard (Compose shell)

The redesign's landing screen — the actions ARE the navigation. A
device/calibration status card leads (wired to real UsbSessionManager
truth), then the quick actions, then recent projects.

Split into a STATEFUL shell (DashboardScreen — reads UsbSessionManager /
ProjectStorage, derives via the pure DashboardController) and a STATELESS
body (DashboardContent — pure rendering, previewable without Context/IO).
Recent-project badges load asynchronously and bounded so first paint never
blocks on disk; a failed/corrupt load simply leaves that row without badges.
########################################################################
*/
@Composable
fun DashboardScreen(
    selectedHardwareName: String,
    recentProjects: List<ProjectListItem>,
    onAction: (DashboardController.DashboardAction) -> Unit,
    onOpenProject: (String) -> Unit,
    onSeeAllProjects: () -> Unit,
    onOpenDeviceStatus: () -> Unit
) {
    val context = LocalContext.current

    val cards = remember(recentProjects) {
        DashboardController.recentProjects(recentProjects)
            .map(DashboardController::buildProjectCard)
    }

    // Badges fill in as the bounded full-project loads complete; the list is
    // rendered from `cards` immediately and never waits on this.
    val badges = remember { mutableStateMapOf<String, DashboardController.DashboardProjectBadge>() }
    LaunchedEffect(cards) {
        badges.clear()
        for (card in cards) {
            val project = withContext(Dispatchers.IO) {
                runCatching { ProjectStorage.loadProjectById(context, card.projectId) }.getOrNull()
            }
            DashboardController.buildProjectBadgeOrNull(project)?.let { badge ->
                badges[card.projectId] = badge
            }
        }
    }

    val cardModel = InstrumentStatusUiMapper.buildCardUiModel(context, selectedHardwareName)
    // Resolve the SAME state the card title/subtitle used (cached session, else a freshly
    // built one) so the pills can never disagree with the header — e.g. on a fresh open with
    // no cached session, the card would otherwise show a built title but all-neutral pills.
    val sessionState = UsbSessionManager.getLatestInstrumentSessionState()
        ?: UsbSessionManager.buildInstrumentSessionState(context, selectedHardwareName)
    val chips = InstrumentStatusPresenter.buildStatusChips(
        dataSourceKind = sessionState.dataSourceKind,
        calibrationReadiness = sessionState.calibrationState.readiness,
        trust = sessionState.measurementTrust
    )

    DashboardContent(
        statusTitle = cardModel.title,
        statusSubtitle = cardModel.subtitle,
        chips = chips,
        actions = DashboardController.quickActions(),
        cards = cards,
        badges = badges,
        onAction = onAction,
        onOpenProject = onOpenProject,
        onSeeAllProjects = onSeeAllProjects,
        onOpenDeviceStatus = onOpenDeviceStatus
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    statusTitle: String,
    statusSubtitle: String,
    chips: InstrumentStatusPresenter.InstrumentStatusChips,
    actions: List<DashboardController.DashboardActionSpec>,
    cards: List<DashboardController.DashboardProjectCard>,
    badges: Map<String, DashboardController.DashboardProjectBadge>,
    onAction: (DashboardController.DashboardAction) -> Unit,
    onOpenProject: (String) -> Unit,
    onSeeAllProjects: () -> Unit,
    onOpenDeviceStatus: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AntennaLab") },
                actions = { AppTopRightMenu() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(padding)
                .padding(AntennaLabTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AntennaLabTheme.spacing.lg)
        ) {
            // --- Device / calibration status (the anti-confusion centrepiece) ---
            MetricCard(
                title = statusTitle,
                subtitle = statusSubtitle,
                onClick = onOpenDeviceStatus
            ) {
                Column(
                    modifier = Modifier.padding(top = AntennaLabTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(AntennaLabTheme.spacing.sm)
                ) {
                    StatusPill(chips.dataSource.label, chips.dataSource.level)
                    StatusPill(chips.calibration.label, chips.calibration.level)
                    StatusPill(chips.trust.label, chips.trust.level)
                }
            }

            // --- Quick actions (three distinct destinations) ---
            Column(verticalArrangement = Arrangement.spacedBy(AntennaLabTheme.spacing.sm)) {
                actions.forEach { spec ->
                    AppActionButton(
                        text = spec.label,
                        onClick = { onAction(spec.action) },
                        variant =
                            if (spec.isPrimary) AppActionVariant.PRIMARY
                            else AppActionVariant.STANDARD
                    )
                }
            }

            // --- Recent projects ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent projects",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (cards.isNotEmpty()) {
                    TextButton(onClick = onSeeAllProjects) { Text("See all") }
                }
            }

            if (cards.isEmpty()) {
                MetricCard(title = "No projects yet") {
                    Text(
                        modifier = Modifier.padding(top = AntennaLabTheme.spacing.xs),
                        text = "Create one with New project, or Identify antenna to explore an unknown one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                cards.forEach { card ->
                    val badge = badges[card.projectId]
                    MetricCard(
                        title = card.name,
                        subtitle = "${card.antennaTypeLabel} · ${card.targetFrequencyText}",
                        onClick = { onOpenProject(card.projectId) }
                    ) {
                        Text(
                            modifier = Modifier.padding(top = AntennaLabTheme.spacing.xs),
                            text = card.lastEditedText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (badge != null) {
                            Row(
                                modifier = Modifier.padding(top = AntennaLabTheme.spacing.sm),
                                horizontalArrangement = Arrangement.spacedBy(AntennaLabTheme.spacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPill(badge.calLabel, badge.calLevel)
                                badge.lastMinSwrText?.let { swr ->
                                    Text(
                                        text = swr,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ---- Previews: fabricated state, no Context / no IO ---- */

private fun previewState(): Triple<
    InstrumentStatusPresenter.InstrumentStatusChips,
    List<DashboardController.DashboardProjectCard>,
    Map<String, DashboardController.DashboardProjectBadge>
    > {
    val chips = InstrumentStatusPresenter.buildStatusChips(
        dataSourceKind = com.example.antennalab_v1.model.testing.InstrumentDataSourceKind.REAL_INSTRUMENT,
        calibrationReadiness = com.example.antennalab_v1.model.testing.CalibrationReadiness.VALID,
        trust = com.example.antennalab_v1.model.testing.MeasurementTrustLevel.TRUSTED
    )
    val cards = listOf(
        DashboardController.DashboardProjectCard("1", "20 m Dipole", "DIPOLE", "14.200 MHz", "24 Jul 2026  2:14 PM"),
        DashboardController.DashboardProjectCard("2", "AR-771 Whip", "MONOPOLE", "145.000 MHz", "24 Jul 2026  11:02 AM")
    )
    val badges = mapOf(
        "1" to DashboardController.DashboardProjectBadge(AppStatusLevel.POSITIVE, "Calibrated", "Last SWR 1.585"),
        "2" to DashboardController.DashboardProjectBadge(AppStatusLevel.NEUTRAL, "No calibration", null)
    )
    return Triple(chips, cards, badges)
}

@Preview(name = "Dashboard — dark", showBackground = true, widthDp = 380, heightDp = 780)
@Composable
private fun DashboardDarkPreview() {
    val (chips, cards, badges) = previewState()
    AntennaLab_V1Theme(darkTheme = true) {
        DashboardContent(
            statusTitle = "LiteVNA64 v0.3.3",
            statusSubtitle = "Live transport path is ready.",
            chips = chips, actions = DashboardController.quickActions(),
            cards = cards, badges = badges,
            onAction = {}, onOpenProject = {}, onSeeAllProjects = {}, onOpenDeviceStatus = {}
        )
    }
}

@Preview(name = "Dashboard — light", showBackground = true, widthDp = 380, heightDp = 780)
@Composable
private fun DashboardLightPreview() {
    val (chips, cards, badges) = previewState()
    AntennaLab_V1Theme(darkTheme = false) {
        DashboardContent(
            statusTitle = "LiteVNA64 v0.3.3",
            statusSubtitle = "Live transport path is ready.",
            chips = chips, actions = DashboardController.quickActions(),
            cards = cards, badges = badges,
            onAction = {}, onOpenProject = {}, onSeeAllProjects = {}, onOpenDeviceStatus = {}
        )
    }
}
