package com.example.antennalab_v1.features.lab

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.domain.testing.UsbPermissionManager
import com.example.antennalab_v1.domain.testing.UsbSessionManager
import com.example.antennalab_v1.features.app.AppTopRightMenu
import com.example.antennalab_v1.model.HardwareConnectionState
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.testing.InstrumentDataSourceKind
import com.example.antennalab_v1.model.testing.MeasurementTrustLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabHomeScreen(
    attachedProject: ProjectData?,
    selectedTemplateId: String,
    onTemplateSelected: (String) -> Unit,
    onBack: () -> Unit,
    onOpenConnections: () -> Unit,
    onOpenCalibration: () -> Unit,
    onOpenProjectAntennaTest: () -> Unit,
    onOpenUnknownAntennaDiscovery: () -> Unit,
    onOpenProjects: () -> Unit
) {
    val context = LocalContext.current

    var instrumentState by remember {
        mutableStateOf(UsbSessionManager.getLatestInstrumentSessionState())
    }

    DisposableEffect(context) {
        val usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context2: Context?, intent: Intent?) {
                instrumentState = UsbSessionManager.refreshLatestKnownSessionState(context)
            }
        }

        val filter = IntentFilter().apply {
            addAction(UsbPermissionManager.ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(UsbSessionManager.ACTION_SESSION_STATE_UPDATED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(usbReceiver, filter)
        }

        instrumentState = UsbSessionManager.refreshLatestKnownSessionState(context)

        onDispose {
            runCatching { context.unregisterReceiver(usbReceiver) }
        }
    }

    LaunchedEffect(Unit) {
        instrumentState = UsbSessionManager.refreshLatestKnownSessionState(context)
    }

    val connectionState = instrumentState?.connectionInfo?.state
    val sessionOpen = instrumentState?.connectionInfo?.sessionOpen == true
    val transportReady = instrumentState?.transportReady == true
    val liveInstrumentReady =
        instrumentState?.dataSourceKind == InstrumentDataSourceKind.REAL_INSTRUMENT

    val instrumentName =
        instrumentState?.instrumentIdentityText
            ?: instrumentState?.connectionInfo?.deviceName
            ?: "No instrument identified"

    val connectionChipText =
        when (connectionState) {
            HardwareConnectionState.READY -> "CONNECTED"
            HardwareConnectionState.PERMISSION_REQUIRED -> "PERMISSION"
            HardwareConnectionState.DEVICE_DETECTED -> "DETECTED"
            HardwareConnectionState.BUSY -> "BUSY"
            HardwareConnectionState.ERROR -> "ERROR"
            HardwareConnectionState.NOT_CONNECTED, null -> "DISCONNECTED"
        }

    val identityChipText =
        when {
            liveInstrumentReady -> "IDENTIFIED"
            sessionOpen && transportReady -> "IDENTIFYING"
            sessionOpen -> "SESSION OPEN"
            else -> "UNKNOWN"
        }

    val calibrationChipText =
        instrumentState?.calibrationState?.readiness?.name ?: "NOT_CALIBRATED"

    val measurementTrustText =
        when (instrumentState?.measurementTrust) {
            MeasurementTrustLevel.TRUSTED -> "Trusted"
            MeasurementTrustLevel.DEGRADED -> "Degraded"
            MeasurementTrustLevel.PARTIAL -> "Partial"
            MeasurementTrustLevel.SIMULATED -> "Simulated"
            MeasurementTrustLevel.UNKNOWN, null -> "Unknown"
        }

    val readinessSummary =
        when {
            liveInstrumentReady -> "Instrument ready for live lab work"
            sessionOpen && transportReady -> "Connection open, transport ready, identity pending"
            sessionOpen -> "Session open, transport not fully ready"
            connectionState == HardwareConnectionState.PERMISSION_REQUIRED -> "Grant USB permission to continue"
            connectionState == HardwareConnectionState.DEVICE_DETECTED -> "Device detected and waiting for permission or connection"
            else -> "No live instrument session is ready"
        }

    val selectedTemplate =
        LabTestTemplates.getTemplateById(selectedTemplateId)
            ?: LabTestTemplates.getDefaultTemplate()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LAB") },
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LabHeaderPanel(
                connectionChipText = connectionChipText,
                identityChipText = identityChipText,
                calibrationChipText = calibrationChipText,
                instrumentName = instrumentName,
                readinessSummary = readinessSummary
            )

            CompactDataPanel(
                title = "Testing Modes",
                highlighted = true
            ) {
                ProjectTemplateModeCard(
                    attachedProject = attachedProject,
                    selectedTemplate = selectedTemplate,
                    onTemplateSelected = onTemplateSelected,
                    onStart = onOpenProjectAntennaTest
                )

                LabModeCard(
                    title = "Unknown Antenna Discovery",
                    subtitle = "Standalone exploration path. Use live sweep without requiring a saved project first.",
                    rightValue = "Standalone",
                    primaryButtonText = "Discover",
                    enabled = true,
                    onPrimaryClick = onOpenUnknownAntennaDiscovery
                )

                LabModeCard(
                    title = "Cable / Feedline Test",
                    subtitle = "Planned instrument-assisted path for cable, feedline, and path-loss checks.",
                    rightValue = "Coming Later",
                    primaryButtonText = "Later",
                    enabled = false,
                    onPrimaryClick = {}
                )

                LabModeCard(
                    title = "Connector / Fitting Check",
                    subtitle = "Planned quick bench checks for connectors, fittings, and adapter quality.",
                    rightValue = "Coming Later",
                    primaryButtonText = "Later",
                    enabled = false,
                    onPrimaryClick = {}
                )
            }

            CompactDataPanel(
                title = "Operator Controls"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrimaryLabButton(
                        text = "Calibration Tools",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenCalibration
                    )

                    SecondaryLabButton(
                        text = "Connections",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenConnections
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryLabButton(
                        text = "Projects",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenProjects
                    )

                    SecondaryLabButton(
                        text = "Back To Home",
                        modifier = Modifier.weight(1f),
                        onClick = onBack
                    )
                }
            }

            CompactDataPanel(
                title = "Instrument Summary"
            ) {
                CompactDataGridRow(
                    leftLabel = "Instrument",
                    leftValue = instrumentName,
                    rightLabel = "Measurement Trust",
                    rightValue = measurementTrustText
                )

                CompactDataGridRow(
                    leftLabel = "Session",
                    leftValue = if (sessionOpen) "Open" else "Closed",
                    rightLabel = "Transport",
                    rightValue = if (transportReady) "Ready" else "Not Ready"
                )

                CompactDataGridRow(
                    leftLabel = "Live Path",
                    leftValue = if (liveInstrumentReady) "Real Instrument" else "Not Ready Yet",
                    rightLabel = "Connection",
                    rightValue = connectionChipText
                )
            }

            CompactDataPanel(
                title = "Calibration Summary"
            ) {
                CompactDataGridRow(
                    leftLabel = "Calibration",
                    leftValue = calibrationChipText,
                    rightLabel = "Trust",
                    rightValue = measurementTrustText
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = instrumentState?.calibrationStatusSummary
                        ?: "No calibration session is currently registered.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                instrumentState?.calibrationState?.operatorWarning
                    ?.takeIf { instrumentState?.calibrationState?.trustDowngraded == true }
                    ?.let { warning ->
                        Text(
                            text = warning,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
            }

            CompactDataPanel(
                title = "Attached Project"
            ) {
                if (attachedProject == null) {
                    CompactDataGridRow(
                        leftLabel = "Project",
                        leftValue = "No project attached",
                        rightLabel = "Mode",
                        rightValue = "Standalone LAB"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = "Project-linked testing becomes available when a project is attached. Discovery can still run without a project.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    CompactDataGridRow(
                        leftLabel = "Project",
                        leftValue = attachedProject.meta.projectName.ifBlank { "Unnamed Project" },
                        rightLabel = "Antenna Type",
                        rightValue = attachedProject.designInput.antennaType.name
                    )

                    CompactDataGridRow(
                        leftLabel = "Target Frequency",
                        leftValue = "${attachedProject.designInput.targetFrequencyMHz} MHz",
                        rightLabel = "Binding",
                        rightValue = "Attached"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectTemplateModeCard(
    attachedProject: ProjectData?,
    selectedTemplate: LabTestTemplate,
    onTemplateSelected: (String) -> Unit,
    onStart: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompactDataGridRow(
                leftLabel = "Mode",
                leftValue = "Project Antenna Test",
                rightLabel = "Status",
                rightValue = if (attachedProject != null) "Project Attached" else "No Project"
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = "${selectedTemplate.displayName} • ${selectedTemplate.bandLabel}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Test Template") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    LabTestTemplates.knownAntennaTemplates.forEach { template ->
                        DropdownMenuItem(
                            text = {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text("${template.displayName} • ${template.bandLabel}")
                                    Text(
                                        template.summary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            onClick = {
                                expanded = false
                                onTemplateSelected(template.id)
                            }
                        )
                    }
                }
            }

            Text(
                text = "This template will preload the attached project for known-band testing before entering the project-linked sweep path.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PrimaryLabButton(
                text = "Start",
                enabled = attachedProject != null,
                onClick = onStart
            )
        }
    }
}

@Composable
private fun LabHeaderPanel(
    connectionChipText: String,
    identityChipText: String,
    calibrationChipText: String,
    instrumentName: String,
    readinessSummary: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Lab Workspace",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = instrumentName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "TEST HUB",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusChip(connectionChipText)
                StatusChip(identityChipText)
                StatusChip(calibrationChipText)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = readinessSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LabModeCard(
    title: String,
    subtitle: String,
    rightValue: String,
    primaryButtonText: String,
    enabled: Boolean,
    onPrimaryClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompactDataGridRow(
                leftLabel = "Mode",
                leftValue = title,
                rightLabel = "Status",
                rightValue = rightValue
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PrimaryLabButton(
                text = primaryButtonText,
                enabled = enabled,
                onClick = onPrimaryClick
            )
        }
    }
}

@Composable
private fun CompactDataPanel(
    title: String,
    highlighted: Boolean = false,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
                if (highlighted) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            content()
        }
    }
}

@Composable
private fun CompactDataGridRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CompactDataCell(
            label = leftLabel,
            value = leftValue,
            modifier = Modifier.weight(1f)
        )

        CompactDataCell(
            label = rightLabel,
            value = rightValue,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CompactDataCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(min = 52.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusChip(
    text: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun PrimaryLabButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Text(text)
    }
}

@Composable
private fun SecondaryLabButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Text(text)
    }
}