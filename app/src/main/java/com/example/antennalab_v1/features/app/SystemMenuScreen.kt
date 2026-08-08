package com.example.antennalab_v1.features.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.model.settings.ThemePreference
import com.example.antennalab_v1.ui.components.SegmentedChoiceButton
import com.example.antennalab_v1.ui.components.SelectionButtonStyle
import com.example.antennalab_v1.ui.theme.AntennaLabTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemMenuScreen(
    onOpenConnectionsDevices: () -> Unit,
    onOpenInstrumentDetails: () -> Unit,
    onBackHome: () -> Unit,
    /*
    Hoisted rather than read here. This screen's signature is callbacks-only
    and it should stay that way — it has no business importing storage. The
    caller already holds the settings and a Context.
    */
    themePreference: ThemePreference,
    onThemePreferenceSelected: (ThemePreference) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "System", style = MaterialTheme.typography.headlineSmall) },
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
            ScreenSectionCard(
                title = "Hardware Layer",
                subtitle = "Manage connected instruments, session state, calibration visibility, and global instrument truth from one app-level system area."
            )
            SystemMenuCard(
                title = "Connections / Devices",
                subtitle = "Connected instrument status, permission, connection state, session state, and calibration readiness",
                isPrimary = true,
                onClick = onOpenConnectionsDevices
            )
            SystemMenuCard(
                title = "Instrument Details",
                subtitle = "Global instrument identity, transport readiness, trust, calibration, support tier, and engineering details",
                isPrimary = false,
                onClick = onOpenInstrumentDetails
            )
            ScreenSectionCard(
                title = "System Areas",
                subtitle = "App-wide controls for hardware behaviour, RF defaults, interface behaviour, and future diagnostics."
            )
            SystemMenuStaticCard("Hardware Settings", "Default instrument type, connection preferences, calibration restore policy, and future companion-device settings")
            SystemMenuStaticCard("Measurement Settings", "Default sweep ranges, point counts, graph behaviour, smoothing, markers, and future measurement preferences")
            SystemMenuStaticCard("Calculation Settings", "Default units, modelling assumptions, safety margins, and future advanced RF calculation options")
            AppSettingsCard(
                themePreference = themePreference,
                onThemePreferenceSelected = onThemePreferenceSelected
            )
            SystemMenuStaticCard("Advanced Diagnostics", "Future raw protocol tools, transport health, engineering capture tools, and development diagnostics")
            Button(
                onClick = onBackHome,
                modifier = Modifier.fillMaxWidth(),
                colors = SelectionButtonStyle.heroActionColors(),
                border = SelectionButtonStyle.heroActionBorder(),
                elevation = SelectionButtonStyle.heroActionElevation()
            ) {
                Text("Back to Home")
            }
        }
    }
}

@Composable
private fun ScreenSectionCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Section headings are neutral, never accented — same rule as the
            // sweep workspace's SharedInstrumentSectionHeader.
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SystemMenuCard(title: String, subtitle: String, isPrimary: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isPrimary) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isPrimary) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPrimary) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/*
The App Settings card, which was a SystemMenuStaticCard placeholder whose
subtitle already named Theme first — this fills in the first of the things it
promised. Kept as ONE card for ONE concern, matching the cards around it: the
control sits inline rather than routing to a sub-screen, because a single
three-way choice does not earn a screen.

Reads AntennaLabTheme tokens and MaterialTheme.colorScheme, not the
instrument* colour-param convention the sweep stack uses — that convention is
pre-Phase-0 and confined to that stack.
*/
@Composable
private fun AppSettingsCard(
    themePreference: ThemePreference,
    onThemePreferenceSelected: (ThemePreference) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(AntennaLabTheme.spacing.sm)
        ) {
            Text(
                text = "App Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Theme, workflow mode, UI behaviour, performance options, and startup behaviour",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Theme",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(AntennaLabTheme.spacing.sm)
            ) {
                // System first: it is the default, and the only option that can
                // be right without the operator deciding anything.
                ThemePreference.entries.forEach { preference ->
                    SegmentedChoiceButton(
                        text = themeOptionLabel(preference),
                        selected = preference == themePreference,
                        onClick = { onThemePreferenceSelected(preference) }
                    )
                }
            }
        }
    }
}

private fun themeOptionLabel(preference: ThemePreference): String =
    when (preference) {
        ThemePreference.SYSTEM -> "System"
        ThemePreference.DARK -> "Dark"
        ThemePreference.LIGHT -> "Light"
    }

@Composable
private fun SystemMenuStaticCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}