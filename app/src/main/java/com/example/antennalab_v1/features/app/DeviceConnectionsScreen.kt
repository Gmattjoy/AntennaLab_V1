package com.example.antennalab_v1.features.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.domain.testing.DriverProfileRegistry
import com.example.antennalab_v1.domain.testing.UsbPermissionManager
import com.example.antennalab_v1.domain.testing.UsbSessionManager
import com.example.antennalab_v1.model.DriverProfile
import com.example.antennalab_v1.model.HardwareConnectionState
import com.example.antennalab_v1.model.UserHardwareConfig
import com.example.antennalab_v1.model.testing.InstrumentDataSourceKind
import com.example.antennalab_v1.model.testing.UsbHardwareSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceConnectionsScreen(
    onBack: () -> Unit,
    onOpenInstrumentDetails: () -> Unit
) {
    val context = LocalContext.current

    val availableProfiles = remember { DriverProfileRegistry.profiles }
    val preferredDefaultProfile = remember(availableProfiles) {
        DeviceConnectionsController.preferredDefaultProfile(availableProfiles)
    }

    var selectedDriverProfile by remember {
        mutableStateOf(UsbSessionManager.getSelectedDriverProfile() ?: preferredDefaultProfile)
    }
    var usbHardwareSession by remember {
        mutableStateOf(
            UsbHardwareSession(
                selectedHardwareName = buildProfileDisplayLabel(selectedDriverProfile)
            )
        )
    }
    var profileDropdownExpanded by remember { mutableStateOf(false) }

    DisposableEffect(context, selectedDriverProfile.id) {
        UsbSessionManager.registerSelectedHardwareConfig(
            UserHardwareConfig(
                selectedBrand = selectedDriverProfile.hardwareBrand,
                selectedModel = selectedDriverProfile.hardwareModel,
                selectedDriverProfileId = selectedDriverProfile.id,
                enableProbeValidation = true,
                allowExperimentalFallback = false
            )
        )

        val usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context2: Context?, intent: Intent?) {
                usbHardwareSession = UsbSessionManager.refreshCurrentSessionState(
                    context = context,
                    selectedHardwareName = buildProfileDisplayLabel(selectedDriverProfile)
                )
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

        usbHardwareSession = UsbSessionManager.refreshCurrentSessionState(
            context = context,
            selectedHardwareName = buildProfileDisplayLabel(selectedDriverProfile)
        )

        onDispose { runCatching { context.unregisterReceiver(usbReceiver) } }
    }

    val instrumentState =
        usbHardwareSession.instrumentSessionState
            ?: UsbSessionManager.getLatestInstrumentSessionState()

    val connectionState = instrumentState?.connectionInfo?.state
    val sessionOpen = instrumentState?.connectionInfo?.sessionOpen == true
    val permissionGranted = instrumentState?.connectionInfo?.permissionGranted == true
    val transportReady = instrumentState?.transportReady == true
    val liveInstrumentReady =
        instrumentState?.dataSourceKind == InstrumentDataSourceKind.REAL_INSTRUMENT
    val selectedProfileLabel = buildProfileDisplayLabel(selectedDriverProfile)
    val statusCardModel = InstrumentStatusUiMapper.buildCardUiModel(context, selectedProfileLabel)

    val isLiteProfile = DeviceConnectionsController.isLiteProfile(selectedDriverProfile)

    val liteBringUp = UsbSessionManager.getLatestLiteVnaBringUpResult()
    val liteIdentity = UsbSessionManager.getLatestLiteVnaIdentityResult()
    val liteCommandTest = UsbSessionManager.getLatestLiteVnaCommandTestResult()

    val liteIdentityConfirmed = DeviceConnectionsController.liteIdentityConfirmed(liteIdentity)
    val liteRegisterConfirmed = DeviceConnectionsController.liteRegisterConfirmed(liteCommandTest)
    val liteTimedOut = DeviceConnectionsController.liteTimedOut(liteIdentity, liteCommandTest, liteBringUp)

    val liteValidationRunning = DeviceConnectionsController.liteValidationRunning(
        isLiteProfile = isLiteProfile,
        sessionOpen = sessionOpen,
        transportReady = transportReady,
        liteIdentityConfirmed = liteIdentityConfirmed,
        liteRegisterConfirmed = liteRegisterConfirmed,
        liteTimedOut = liteTimedOut
    )

    val trustText = DeviceConnectionsController.trustText(instrumentState?.measurementTrust)

    val calibrationStateLabel =
        DeviceConnectionsController.calibrationStateLabel(instrumentState?.calibrationState?.readiness)

    val showRequestPermission = DeviceConnectionsController.showRequestPermission(connectionState)
    val showConnect = DeviceConnectionsController.showConnect(permissionGranted, sessionOpen)
    val showDisconnect = DeviceConnectionsController.showDisconnect(sessionOpen)
    val showValidateLiteVna =
        DeviceConnectionsController.showValidateLiteVna(isLiteProfile, sessionOpen, transportReady)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connections / Devices") },
                actions = { AppTopRightMenu() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(padding)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InstrumentStatusCard(
                model = statusCardModel,
                onOpenDetails = onOpenInstrumentDetails
            )

            CompactDataPanel(
                title = "Preparation Workflow",
                highlighted = true
            ) {
                Text(
                    text = buildNextHardwareStepText(
                        connectionState = connectionState,
                        permissionGranted = permissionGranted,
                        sessionOpen = sessionOpen,
                        transportReady = transportReady,
                        isLiteProfile = isLiteProfile,
                        liveInstrumentReady = liveInstrumentReady,
                        liteValidationRunning = liteValidationRunning,
                        liteIdentityConfirmed = liteIdentityConfirmed,
                        liteRegisterConfirmed = liteRegisterConfirmed
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                CompactDataGridRow(
                    "Connection",
                    connectionState?.name ?: "UNKNOWN",
                    "Permission",
                    if (permissionGranted) "Granted" else "Required"
                )
                CompactDataGridRow(
                    "Session",
                    if (sessionOpen) "Open" else "Closed",
                    "Transport",
                    if (transportReady) "Ready" else "Not Ready"
                )
                CompactDataGridRow(
                    "Validation",
                    buildValidationLabel(
                        isLiteProfile = isLiteProfile,
                        liveInstrumentReady = liveInstrumentReady,
                        liteValidationRunning = liteValidationRunning,
                        liteIdentityConfirmed = liteIdentityConfirmed,
                        liteRegisterConfirmed = liteRegisterConfirmed,
                        liteTimedOut = liteTimedOut
                    ),
                    "Trust",
                    trustText
                )
                CompactDataGridRow(
                    "Calibration",
                    calibrationStateLabel,
                    "Data Source",
                    instrumentState?.dataSourceKind?.name ?: "UNKNOWN"
                )
            }

            CompactDataPanel(
                title = "Operator Controls"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrimaryActionButton(
                        text = "Refresh",
                        modifier = Modifier.weight(1f)
                    ) {
                        usbHardwareSession = UsbSessionManager.refreshCurrentSessionState(
                            context = context,
                            selectedHardwareName = selectedProfileLabel
                        )
                    }

                    SecondaryActionButton(
                        text = "Back",
                        modifier = Modifier.weight(1f),
                        onClick = onBack
                    )
                }

                if (showRequestPermission || showConnect || showDisconnect) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (showRequestPermission) {
                            PrimaryActionButton(
                                text = "Grant Permission",
                                modifier = Modifier.weight(1f)
                            ) {
                                usbHardwareSession =
                                    UsbPermissionManager.requestPermission(
                                        context = context,
                                        selectedHardwareName = selectedProfileLabel
                                    )
                            }
                        }

                        if (showConnect) {
                            PrimaryActionButton(
                                text = "Connect Device",
                                modifier = Modifier.weight(1f)
                            ) {
                                usbHardwareSession =
                                    UsbSessionManager.openFirstDetectedSession(
                                        context = context,
                                        selectedHardwareName = selectedProfileLabel
                                    )
                            }
                        }

                        if (showDisconnect) {
                            SecondaryActionButton(
                                text = "Disconnect Device",
                                modifier = Modifier.weight(1f)
                            ) {
                                usbHardwareSession =
                                    UsbSessionManager.closeSession(
                                        context = context,
                                        selectedHardwareName = selectedProfileLabel
                                    )
                            }
                        }
                    }
                }

                if (showValidateLiteVna) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PrimaryActionButton(
                            text = "Validate Device",
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            UsbSessionManager.startLiteVnaBringUpIfNeeded(
                                context = context,
                                selectedHardwareName = selectedProfileLabel
                            )

                            usbHardwareSession = UsbSessionManager.refreshCurrentSessionState(
                                context = context,
                                selectedHardwareName = selectedProfileLabel
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SecondaryActionButton(
                        text = "Instrument Details / Troubleshooting",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOpenInstrumentDetails
                    )
                }
            }

            CompactDataPanel(title = "Device Model") {
                ExposedDropdownMenuBox(
                    expanded = profileDropdownExpanded,
                    onExpandedChange = {
                        profileDropdownExpanded = !profileDropdownExpanded
                    }
                ) {
                    OutlinedTextField(
                        value = selectedProfileLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Selected Profile") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = profileDropdownExpanded
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = profileDropdownExpanded,
                        onDismissRequest = {
                            profileDropdownExpanded = false
                        }
                    ) {
                        availableProfiles.forEach { profile ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(buildProfileDisplayLabel(profile))
                                        Text(
                                            text = "${profile.protocolType.name} • ${profile.supportTier.name}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                onClick = {
                                    profileDropdownExpanded = false
                                    selectedDriverProfile = profile
                                    UsbSessionManager.registerSelectedHardwareConfig(
                                        UserHardwareConfig(
                                            selectedBrand = profile.hardwareBrand,
                                            selectedModel = profile.hardwareModel,
                                            selectedDriverProfileId = profile.id,
                                            enableProbeValidation = true,
                                            allowExperimentalFallback = false
                                        )
                                    )
                                    usbHardwareSession =
                                        UsbSessionManager.refreshCurrentSessionState(
                                            context = context,
                                            selectedHardwareName = buildProfileDisplayLabel(profile)
                                        )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                CompactDataGridRow(
                    "Family",
                    selectedDriverProfile.hardwareFamily.name,
                    "Protocol",
                    selectedDriverProfile.protocolType.name
                )
                CompactDataGridRow(
                    "Transport",
                    selectedDriverProfile.transportType.name,
                    "Support",
                    selectedDriverProfile.supportTier.name
                )
            }

            CompactDataPanel(title = "Readiness Summary") {
                Text(
                    text = instrumentState?.calibrationStatusSummary
                        ?: "No calibration session is currently registered.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/*
The hardware-selection decision logic (profile label, next-step guidance,
validation label, plus the flag/gating derivations inlined above) lives in the
pure, testable DeviceConnectionsController. These thin wrappers keep the existing
call sites in the Composable unchanged.
*/
private fun buildProfileDisplayLabel(profile: DriverProfile): String =
    DeviceConnectionsController.buildProfileDisplayLabel(profile)

private fun buildNextHardwareStepText(
    connectionState: HardwareConnectionState?,
    permissionGranted: Boolean,
    sessionOpen: Boolean,
    transportReady: Boolean,
    isLiteProfile: Boolean,
    liveInstrumentReady: Boolean,
    liteValidationRunning: Boolean,
    liteIdentityConfirmed: Boolean,
    liteRegisterConfirmed: Boolean
): String = DeviceConnectionsController.buildNextHardwareStepText(
    connectionState = connectionState,
    permissionGranted = permissionGranted,
    sessionOpen = sessionOpen,
    transportReady = transportReady,
    isLiteProfile = isLiteProfile,
    liveInstrumentReady = liveInstrumentReady,
    liteValidationRunning = liteValidationRunning,
    liteIdentityConfirmed = liteIdentityConfirmed,
    liteRegisterConfirmed = liteRegisterConfirmed
)

private fun buildValidationLabel(
    isLiteProfile: Boolean,
    liveInstrumentReady: Boolean,
    liteValidationRunning: Boolean,
    liteIdentityConfirmed: Boolean,
    liteRegisterConfirmed: Boolean,
    liteTimedOut: Boolean
): String = DeviceConnectionsController.buildValidationLabel(
    isLiteProfile = isLiteProfile,
    liveInstrumentReady = liveInstrumentReady,
    liteValidationRunning = liteValidationRunning,
    liteIdentityConfirmed = liteIdentityConfirmed,
    liteRegisterConfirmed = liteRegisterConfirmed,
    liteTimedOut = liteTimedOut
)

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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
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
        CompactDataCell(leftLabel, leftValue, Modifier.weight(1f))
        CompactDataCell(rightLabel, rightValue, Modifier.weight(1f))
    }
}

@Composable
private fun CompactDataCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.heightIn(min = 52.dp),
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
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PrimaryActionButton(
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
private fun SecondaryActionButton(
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(text)
    }
}