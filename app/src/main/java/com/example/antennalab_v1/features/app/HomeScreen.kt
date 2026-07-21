package com.example.antennalab_v1.features.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenLab: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWizard: () -> Unit,
    onOpenTestAntenna: () -> Unit,
    onOpenProjects: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "AntennaLab V1",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Lab, design, test, and saved antenna model workflow",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HomeStructureCard()

            HomeSectionHeader(
                title = "Primary Areas",
                subtitle = "Choose the app area you want to work in"
            )

            HomeMenuCard(
                title = "LAB",
                subtitle = "Live instrument area for hardware connection, calibration, sweep workspace, and lab-style RF development",
                onClick = onOpenLab,
                isPrimary = true
            )

            HomeMenuCard(
                title = "SETTINGS",
                subtitle = "Hardware settings, app settings, measurement defaults, restore policy, and future diagnostics/settings areas",
                onClick = onOpenSettings
            )

            HomeMenuCard(
                title = "WIZARD",
                subtitle = "Design and build workflow that produces a saved antenna model",
                onClick = onOpenWizard,
                isPrimary = true
            )

            HomeMenuCard(
                title = "TEST ANTENNA",
                subtitle = "Input real antenna data and develop a saved antenna model from measured or known antenna information",
                onClick = onOpenTestAntenna
            )

            HomeMenuCard(
                title = "PROJECTS",
                subtitle = "Open, manage, and continue saved antenna models stored on this device",
                onClick = onOpenProjects
            )
        }
    }
}

@Composable
private fun HomeStructureCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Current App Structure",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Wizard → Saved Antenna Model",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Test Antenna → Saved Antenna Model",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Projects ↔ Saved Antenna Models",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Lab = live instrument and RF workspace",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    subtitle: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HomeMenuCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isPrimary: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isPrimary) {
                MaterialTheme.colorScheme.outline
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isPrimary) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPrimary) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}