package com.example.antennalab_v1.features.testing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.antennalab_v1.domain.testing.UsbSessionManager
import com.example.antennalab_v1.features.app.AppTopRightMenu
import com.example.antennalab_v1.model.testing.CalibrationCaptureSource
import com.example.antennalab_v1.model.testing.CalibrationSession
import com.example.antennalab_v1.model.testing.CalibrationStep
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationWizardScreen(
    calibrationSession: CalibrationSession,
    onSessionChange: (CalibrationSession) -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit
) {
    val steps = listOf(
        CalibrationStep.OPEN,
        CalibrationStep.SHORT,
        CalibrationStep.LOAD
    )

    var workingSession by remember(
        calibrationSession.hardwareDisplayName,
        calibrationSession.startFrequencyMHz,
        calibrationSession.endFrequencyMHz,
        calibrationSession.openCaptured,
        calibrationSession.shortCaptured,
        calibrationSession.loadCaptured,
        calibrationSession.capturedAtEpochMs
    ) {
        mutableStateOf(calibrationSession)
    }

    var currentStepIndex by remember(
        calibrationSession.hardwareDisplayName,
        calibrationSession.startFrequencyMHz,
        calibrationSession.endFrequencyMHz,
        calibrationSession.openCaptured,
        calibrationSession.shortCaptured,
        calibrationSession.loadCaptured
    ) {
        mutableIntStateOf(findFirstIncompleteStepIndex(calibrationSession))
    }

    LaunchedEffect(calibrationSession) {
        workingSession = calibrationSession
        currentStepIndex = findFirstIncompleteStepIndex(calibrationSession)
    }

    val currentStep = steps[currentStepIndex]
    val currentInstrumentState = UsbSessionManager.getLatestInstrumentSessionState()
    val sessionReadyForCapture =
        UsbSessionManager.hasOpenSession() && UsbSessionManager.isTransportReady()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calibration Wizard") },
                actions = { AppTopRightMenu() }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            CalibrationWizardSummaryCard(
                calibrationSession = workingSession,
                currentStep = currentStep,
                currentStepIndex = currentStepIndex,
                totalSteps = steps.size
            )

            CalibrationInstructionCard(
                currentStep = currentStep
            )

            CalibrationProgressCard(
                calibrationSession = workingSession
            )

            CalibrationSessionTruthCard(
                sessionReadyForCapture = sessionReadyForCapture,
                selectedHardwareName = currentInstrumentState?.selectedHardwareName ?: workingSession.hardwareDisplayName,
                protocolFamily = currentInstrumentState?.protocolFamily ?: "Unknown",
                instrumentIdentityText = currentInstrumentState?.instrumentIdentityText ?: "Unknown"
            )

            Button(
                onClick = {
                    val updatedSession = buildCapturedStepSession(
                        existingSession = workingSession,
                        currentStep = currentStep,
                        selectedHardwareName = currentInstrumentState?.selectedHardwareName ?: workingSession.hardwareDisplayName,
                        protocolFamily = currentInstrumentState?.protocolFamily,
                        instrumentIdentityText = currentInstrumentState?.instrumentIdentityText
                    )

                    workingSession = updatedSession
                    onSessionChange(updatedSession)
                    UsbSessionManager.registerCalibrationSession(updatedSession)

                    currentStepIndex =
                        if (updatedSession.loadCaptured) {
                            steps.lastIndex
                        } else {
                            findFirstIncompleteStepIndex(updatedSession)
                        }

                    if (updatedSession.openCaptured &&
                        updatedSession.shortCaptured &&
                        updatedSession.loadCaptured
                    ) {
                        onFinish()
                    }
                },
                enabled = sessionReadyForCapture,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (currentStep == CalibrationStep.LOAD) {
                        "Capture LOAD and Finish"
                    } else {
                        "Capture ${currentStep.name} and Continue"
                    }
                )
            }

            Button(
                onClick = {
                    if (currentStepIndex > 0) {
                        currentStepIndex -= 1
                    }
                },
                enabled = currentStepIndex > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Previous Step")
            }

            Button(
                onClick = {
                    if (!workingSession.hasAnyCapturedStep()) {
                        UsbSessionManager.clearCalibrationState()
                    }

                    onCancel()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}

private fun findFirstIncompleteStepIndex(
    calibrationSession: CalibrationSession
): Int {
    return when {
        !calibrationSession.openCaptured -> 0
        !calibrationSession.shortCaptured -> 1
        !calibrationSession.loadCaptured -> 2
        else -> 2
    }
}

private fun buildCapturedStepSession(
    existingSession: CalibrationSession,
    currentStep: CalibrationStep,
    selectedHardwareName: String,
    protocolFamily: String?,
    instrumentIdentityText: String?
): CalibrationSession {
    val captureTimeMs = System.currentTimeMillis()

    val timeLabel = SimpleDateFormat(
        "dd/MM/yyyy HH:mm:ss",
        Locale.getDefault()
    ).format(Date(captureTimeMs))

    val baseSession = existingSession.copy(
        hardwareDisplayName = selectedHardwareName,
        timestampLabel = "Captured $timeLabel",
        capturedAtEpochMs = captureTimeMs,
        captureSource = CalibrationCaptureSource.WIZARD,
        capturedProtocolFamily = protocolFamily,
        capturedInstrumentIdentityText = instrumentIdentityText
    )

    return when (currentStep) {
        CalibrationStep.OPEN -> baseSession.copy(openCaptured = true)
        CalibrationStep.SHORT -> baseSession.copy(shortCaptured = true)
        CalibrationStep.LOAD -> baseSession.copy(loadCaptured = true)
    }
}

@Composable
private fun CalibrationWizardSummaryCard(
    calibrationSession: CalibrationSession,
    currentStep: CalibrationStep,
    currentStepIndex: Int,
    totalSteps: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Calibration Session")

            HorizontalDivider()

            Text("Hardware")
            Text(calibrationSession.hardwareDisplayName)

            Text("Range")
            Text(
                String.format(
                    "%.3f MHz → %.3f MHz",
                    calibrationSession.startFrequencyMHz,
                    calibrationSession.endFrequencyMHz
                )
            )

            Text("Step")
            Text("${currentStepIndex + 1} / $totalSteps  •  ${currentStep.name}")

            Text("Capture Timestamp")
            Text(calibrationSession.timestampLabel)
        }
    }
}

@Composable
private fun CalibrationInstructionCard(
    currentStep: CalibrationStep
) {
    val instructionText =
        when (currentStep) {
            CalibrationStep.OPEN ->
                "Disconnect the antenna and leave the calibration port open. This captures the OPEN reference."

            CalibrationStep.SHORT ->
                "Connect the SHORT reference to the calibration port. This captures the SHORT reference."

            CalibrationStep.LOAD ->
                "Connect the 50 Ω LOAD reference to the calibration port. This captures the LOAD reference."
        }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Step Instructions")

            HorizontalDivider()

            Text(instructionText)

            Text("Professional workflow note")
            Text("Use the same cable, adapter path, and hardware setup you will use for measurement.")
        }
    }
}

@Composable
private fun CalibrationProgressCard(
    calibrationSession: CalibrationSession
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Progress")

            HorizontalDivider()

            Text("OPEN")
            Text(if (calibrationSession.openCaptured) "Captured" else "Pending")

            Text("SHORT")
            Text(if (calibrationSession.shortCaptured) "Captured" else "Pending")

            Text("LOAD")
            Text(if (calibrationSession.loadCaptured) "Captured" else "Pending")

            HorizontalDivider()

            Text("Completion")
            Text(calibrationSession.completionState.name)
        }
    }
}

@Composable
private fun CalibrationSessionTruthCard(
    sessionReadyForCapture: Boolean,
    selectedHardwareName: String,
    protocolFamily: String,
    instrumentIdentityText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Active Session Truth")

            HorizontalDivider()

            Text("Session Ready")
            Text(if (sessionReadyForCapture) "Yes" else "No")

            Text("Selected Hardware")
            Text(selectedHardwareName)

            Text("Protocol Family")
            Text(protocolFamily)

            Text("Instrument Identity")
            Text(instrumentIdentityText)

            if (!sessionReadyForCapture) {
                HorizontalDivider()
                Text("Open and validate the live instrument session before capturing calibration.")
            }
        }
    }
}