package com.example.antennalab_v1.features.app

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstrumentDetailsScreen(
    model: InstrumentDetailsUiModel,
    onBack: () -> Unit,
    onOpenConnectionsDevices: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(model.headerTitle) },
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
            DetailsSectionCard(
                title = model.instrumentTitle,
                subtitle = model.headerSubtitle
            )

            DetailsDataCard(
                title = "Operator Status",
                rows = listOf(
                    "Selected Profile" to model.selectedProfileLabel,
                    "Protocol" to model.protocolLabel,
                    "Support Tier" to model.supportTierLabel,
                    "Connection" to model.connectionLabel,
                    "Transport" to model.transportLabel,
                    "Data Path" to model.dataPathLabel,
                    "Trust" to model.trustLabel,
                    "Calibration" to model.calibrationLabel
                ),
                summary = model.operatorSummary
            )

            DetailsDataCard(
                title = "Technical Identity",
                rows = listOf(
                    "Identity" to model.technicalIdentityLabel
                ),
                summary = "Use this section for protocol-level verification and instrument identity troubleshooting."
            )

            DetailsDataCard(
                title = "Calibration and Session",
                rows = listOf(
                    "Calibration" to model.calibrationLabel
                ),
                summary = model.calibrationSummary + "\n\n" + model.sessionSummary
            )

            DetailsDataCard(
                title = "Transport Health",
                rows = listOf(
                    "Last Command" to model.lastCommandStatusLabel,
                    "Last Read Size" to model.lastReadSizeLabel,
                    "Last Error" to model.lastErrorLabel
                ),
                summary = model.transportSummary
            )

            DetailsDataCard(
                title = "Advanced Debug / Troubleshooting",
                rows = emptyList(),
                summary = model.liteVnaDebugSummary
            )

            TextButton(
                onClick = onOpenConnectionsDevices,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Connections / Devices")
            }

            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun DetailsSectionCard(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailsDataCard(
    title: String,
    rows: List<Pair<String, String>>,
    summary: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            rows.forEach { (label, value) ->
                DetailRow(label = label, value = value)
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )

            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}