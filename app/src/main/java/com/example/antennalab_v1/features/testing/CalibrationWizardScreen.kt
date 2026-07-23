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
import androidx.compose.material3.FilterChip
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
import com.example.antennalab_v1.BuildConfig
import com.example.antennalab_v1.domain.testing.DebugOslCalibrationSimulator
import com.example.antennalab_v1.domain.testing.SweepController
import com.example.antennalab_v1.domain.testing.UsbSessionManager
import com.example.antennalab_v1.features.app.AppTopRightMenu
import com.example.antennalab_v1.model.testing.CalibrationSession
import com.example.antennalab_v1.model.testing.CalibrationStep
import com.example.antennalab_v1.model.testing.SweepResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationWizardScreen(
    calibrationSession: CalibrationSession,
    onSessionChange: (CalibrationSession) -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit
) {
    val steps = CalibrationWizardController.steps

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
        mutableIntStateOf(CalibrationWizardController.findFirstIncompleteStepIndex(calibrationSession))
    }

    LaunchedEffect(calibrationSession) {
        workingSession = calibrationSession
        currentStepIndex = CalibrationWizardController.findFirstIncompleteStepIndex(calibrationSession)
    }

    // Raw captured sweep of each standard, used to compute error terms once all
    // three are present.
    var capturedStandards by remember(
        calibrationSession.hardwareDisplayName,
        calibrationSession.startFrequencyMHz,
        calibrationSession.endFrequencyMHz
    ) {
        mutableStateOf(emptyMap<CalibrationStep, SweepResult>())
    }

    // Debug-only: synthesize O/S/L captures through a known error network so the
    // wizard and OSL math can be exercised with no VNA connected.
    var debugSimulateCapture by remember { mutableStateOf(false) }

    val currentStep = steps[currentStepIndex]
    val currentInstrumentState = UsbSessionManager.getLatestInstrumentSessionState()
    val sessionReadyForCapture =
        UsbSessionManager.hasOpenSession() && UsbSessionManager.isTransportReady()
    val canCapture = sessionReadyForCapture || debugSimulateCapture

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

            if (BuildConfig.DEBUG) {
                CalibrationDebugCard(
                    debugSimulateCapture = debugSimulateCapture,
                    onToggle = { debugSimulateCapture = it }
                )
            }

            Button(
                onClick = {
                    val capturedSweep = captureStandardSweep(
                        step = currentStep,
                        startMHz = workingSession.startFrequencyMHz,
                        endMHz = workingSession.endFrequencyMHz,
                        useSimulatedCapture = debugSimulateCapture
                    )

                    if (capturedSweep != null) {
                        // Pure state machine decides the new session/standards/step;
                        // this Composable performs the side effects below.
                        val result = CalibrationWizardController.applyCapturedStandard(
                            currentSession = workingSession,
                            capturedStandards = capturedStandards,
                            currentStep = currentStep,
                            capturedSweep = capturedSweep,
                            selectedHardwareName = currentInstrumentState?.selectedHardwareName ?: workingSession.hardwareDisplayName,
                            protocolFamily = currentInstrumentState?.protocolFamily,
                            instrumentIdentityText = currentInstrumentState?.instrumentIdentityText,
                            captureTimeMs = System.currentTimeMillis()
                        )

                        capturedStandards = result.updatedCapturedStandards
                        workingSession = result.updatedSession
                        onSessionChange(result.updatedSession)
                        // Debug-simulated captures have no live session; register
                        // them through the debug path so they are treated as usable
                        // (this runs after onSessionChange's registration and wins).
                        if (debugSimulateCapture) {
                            UsbSessionManager.registerSimulatedCalibrationSession(result.updatedSession)
                        } else {
                            UsbSessionManager.registerCalibrationSession(result.updatedSession)
                        }

                        currentStepIndex = result.nextStepIndex

                        if (result.isComplete) {
                            onFinish()
                        }
                    }
                },
                enabled = canCapture,
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

/**
 * Captures one OSL standard as a sweep. In debug-simulated mode this synthesizes
 * the standard through a known error network (no hardware); otherwise it runs a
 * real sweep of whatever standard is physically connected. Returns null if a real
 * capture fails.
 */
private fun captureStandardSweep(
    step: CalibrationStep,
    startMHz: Double,
    endMHz: Double,
    useSimulatedCapture: Boolean
): SweepResult? {
    if (useSimulatedCapture) {
        return DebugOslCalibrationSimulator.simulateStandardCaptureSweep(
            step = step,
            startMHz = startMHz,
            endMHz = endMHz,
            pointCount = CalibrationWizardController.CALIBRATION_POINT_COUNT
        )
    }

    return runCatching {
        SweepController.runSweep(
            startMHz = startMHz,
            endMHz = endMHz,
            stepMHz = CalibrationWizardController.sweepStepMHz(startMHz, endMHz)
        )
    }.getOrNull()
}

@Composable
private fun CalibrationDebugCard(
    debugSimulateCapture: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Debug Tools")

            HorizontalDivider()

            Text(
                "Debug builds only. Synthesizes each O/S/L capture through a known " +
                    "error network so the wizard and calibration math can be tested " +
                    "with no VNA connected."
            )

            FilterChip(
                selected = debugSimulateCapture,
                onClick = { onToggle(!debugSimulateCapture) },
                label = { Text("Simulate O/S/L capture (no hardware)") }
            )
        }
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