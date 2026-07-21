package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.DriverProfile
import com.example.antennalab_v1.model.testing.SweepResult
import kotlin.math.round
import kotlin.math.roundToInt

/*
########################################################################
FILE: UsbVnaSweepDataSource.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Sweep Logic

LAST UPDATED 05/04/2026 12:35

IMPORTANT CHANGE
Adds deeper LiteVNA decode/validation diagnostics so when a sweep read
succeeds but no usable SweepResult is produced, the failure reason can be
seen in higher-level operator and troubleshooting surfaces.
########################################################################
*/

class UsbVnaSweepDataSource(
    private val selectedDriverProfile: DriverProfile? = null,
    private val commandChannel: UsbVnaCommandChannel = UsbVnaCommandChannel()
) : SweepDataSource {

    private var latestTransportHealthSnapshot = UsbVnaTransportHealthSnapshot()

    private var latestDiscoverySnapshot = UsbVnaDiscoverySnapshot(
        instrumentIdentity = "No discovery has been run yet.",
        protocolFamilyDisplayName = "Unknown",
        protocolGuess = "Unknown",
        supportTier = InstrumentSupportTier.DETECTED,
        driverId = null,
        dataSourceDisplayName = "USB Discovery Mode",
        transportStatusSummary = "No transport behaviour available yet.",
        measurementTrustSummary = "No measurement trust available yet.",
        summary = "Discovery has not been executed yet."
    )

    fun getLatestTransportHealthSnapshot(): UsbVnaTransportHealthSnapshot {
        return latestTransportHealthSnapshot
    }

    fun getLatestDiscoverySnapshot(): UsbVnaDiscoverySnapshot {
        return latestDiscoverySnapshot
    }

    override fun runSweep(
        startMHz: Double,
        endMHz: Double,
        stepMHz: Double
    ): SweepResult {

        validateSweepInputs(startMHz, endMHz, stepMHz)

        val selectedProfileLabel = selectedDriverProfile?.displayName
            ?: "No selected driver profile"

        val transportStatus = FoundationUsbVnaTransport(
            commandChannel = commandChannel
        ).evaluateTransportStatus()

        require(transportStatus.ready) {
            "Transport not ready. profile=$selectedProfileLabel. ${transportStatus.summary}"
        }

        require(UsbSessionManager.hasOpenSession()) {
            "No USB session open. profile=$selectedProfileLabel."
        }

        require(UsbSessionManager.isTransportReady()) {
            "USB transport not ready. profile=$selectedProfileLabel."
        }

        require(UsbSessionManager.getActiveTransportChannel() != null) {
            "No active transport channel. profile=$selectedProfileLabel."
        }

        val startFrequencyHz = (startMHz * 1_000_000.0).roundToLongSafe()
        val endFrequencyHz = (endMHz * 1_000_000.0).roundToLongSafe()

        val pointCount = calculatePointCount(startMHz, endMHz, stepMHz)
        val stepFrequencyHz = (stepMHz * 1_000_000.0).roundToLongSafe()

        val identityResult = commandChannel.queryAnalyzerIdentity()
        latestTransportHealthSnapshot = commandChannel.getLatestTransportHealthSnapshot()

        val executionPath = selectedDriverProfile?.let {
            ProtocolDriverFactory.createExecutionPath(it)
        }

        when (executionPath) {

            is ProtocolExecutionPath.LiteVna -> {

                val liteProtocol = LiteVnaSweepProtocol(commandChannel)

                val bringUpResult = liteProtocol.checkBringUpReadiness()
                val identityProbe = liteProtocol.probeIdentity()
                val commandTest = liteProtocol.runBasicCommandTest()

                UsbSessionManager.registerLiteVnaBringUpResults(
                    bringUp = bringUpResult,
                    identity = identityProbe,
                    commandTest = commandTest
                )

                val parsedSweepResult = liteProtocol.buildSweepResult(
                    startMHz = startMHz,
                    endMHz = endMHz,
                    stepMHz = stepMHz
                )

                latestDiscoverySnapshot = UsbVnaDiscoverySnapshot(
                    instrumentIdentity = identityProbe.rawIdentityText.ifBlank { "Unknown" },
                    protocolFamilyDisplayName = "LiteVNA",
                    protocolGuess = "LiteVNA",
                    supportTier =
                        if (parsedSweepResult != null) {
                            InstrumentSupportTier.PARTIAL_SUPPORT
                        } else {
                            InstrumentSupportTier.DETECTED
                        },
                    driverId = selectedDriverProfile?.id,
                    dataSourceDisplayName = selectedProfileLabel,
                    transportStatusSummary = latestTransportHealthSnapshot.lastOperationSummary,
                    measurementTrustSummary =
                        if (parsedSweepResult != null) {
                            "LiteVNA live sweep data is flowing through the USB path and valuesFIFO parser."
                        } else {
                            "LiteVNA validation succeeded, but live sweep parsing did not return usable points yet."
                        },
                    summary =
                        buildString {
                            appendLine("LiteVNA execution result:")
                            appendLine("Ready: ${bringUpResult.success}")
                            appendLine("Identity: ${identityProbe.rawIdentityText}")
                            appendLine("Identity stage: ${identityProbe.stage}")
                            appendLine("Command test: ${commandTest.success}")
                            appendLine("Command stage: ${commandTest.stage}")
                            appendLine("Requested point count: $pointCount")
                            appendLine("Sweep start Hz: $startFrequencyHz")
                            appendLine("Sweep end Hz: $endFrequencyHz")
                            appendLine("Decoded sweep points: ${parsedSweepResult?.points?.size ?: 0}")
                            appendLine("Reason: ${commandTest.summary}")
                            appendLine()
                            appendLine(liteProtocol.getLastConfiguredSweepDiagnostics())
                        }
                )

                latestTransportHealthSnapshot = commandChannel.getLatestTransportHealthSnapshot()

                if (parsedSweepResult != null) {
                    val validationFailure = SweepValidator.explainValidationFailure(parsedSweepResult)

                    require(validationFailure == null) {
                        "LiteVNA SweepResult failed validation. $validationFailure"
                    }

                    return parsedSweepResult
                }

                throw IllegalStateException(
                    "LiteVNA live sweep parsing returned no usable sweep points.\n\n" +
                            liteProtocol.getLastConfiguredSweepDiagnostics()
                )
            }

            is ProtocolExecutionPath.NanoShell,
            is ProtocolExecutionPath.ExperimentalAsciiSerial,
            null -> {
                // Allowed
            }
        }

        require(identityResult.success) {
            "Identity query failed. profile=$selectedProfileLabel. ${identityResult.summary}"
        }

        val driverResolution = UsbVnaDriverRegistry.resolveDriver(identityResult)
        latestDiscoverySnapshot = driverResolution.discoverySnapshot

        if (executionPath is ProtocolExecutionPath.NanoShell) {
            val identitySummary = identityResult.summary
            val isNanoIdentity =
                identitySummary.contains("nano", ignoreCase = true)

            if (!isNanoIdentity) {
                latestDiscoverySnapshot = UsbVnaDiscoverySnapshot(
                    instrumentIdentity = identityResult.rawIdentityText ?: "Unknown",
                    protocolFamilyDisplayName = "Mismatch",
                    protocolGuess = identityResult.protocolIdentity.displayName,
                    supportTier = InstrumentSupportTier.DETECTED,
                    driverId = selectedDriverProfile?.id,
                    dataSourceDisplayName = selectedProfileLabel,
                    transportStatusSummary = latestTransportHealthSnapshot.lastOperationSummary,
                    measurementTrustSummary =
                        "Selected profile requires NanoVNA-style device but detected identity does not match.",
                    summary =
                        "HARD BLOCK: Profile/device mismatch. Profile=$selectedProfileLabel, identity=$identitySummary"
                )

                throw IllegalStateException(
                    "Profile/device mismatch.\n" +
                            "Selected profile: $selectedProfileLabel\n" +
                            "Detected identity: $identitySummary\n\n" +
                            "Sweep blocked to prevent incorrect protocol execution."
                )
            }
        }

        require(driverResolution.canExecuteRealSweep()) {
            "Driver not at full support tier. profile=$selectedProfileLabel. ${driverResolution.discoverySnapshot.summary}"
        }

        val resolvedDriver = requireNotNull(driverResolution.driver) {
            "Driver missing after resolution. profile=$selectedProfileLabel."
        }

        val sweepResult = acquireValidatedSweepWithRetry(
            resolvedDriver = resolvedDriver,
            startFrequencyHz = startFrequencyHz,
            endFrequencyHz = endFrequencyHz,
            pointCount = pointCount
        )

        latestTransportHealthSnapshot = commandChannel.getLatestTransportHealthSnapshot()

        val validationFailure = SweepValidator.explainValidationFailure(sweepResult)

        require(validationFailure == null) {
            "Invalid SweepResult returned. $validationFailure"
        }

        return sweepResult
    }

    private fun validateSweepInputs(
        startMHz: Double,
        endMHz: Double,
        stepMHz: Double
    ) {
        require(endMHz > startMHz)
        require(stepMHz > 0.0)
    }

    private fun calculatePointCount(
        startMHz: Double,
        endMHz: Double,
        stepMHz: Double
    ): Int {
        val spanMHz = endMHz - startMHz
        val stepCount = (spanMHz / stepMHz).roundToInt()

        return (stepCount + 1)
            .coerceAtLeast(UsbVnaSweepProtocolRules.MIN_POINT_COUNT)
            .coerceAtMost(UsbVnaSweepProtocolRules.MAX_POINT_COUNT)
    }

    private fun acquireValidatedSweepWithRetry(
        resolvedDriver: UsbVnaInstrumentDriver,
        startFrequencyHz: Long,
        endFrequencyHz: Long,
        pointCount: Int
    ): SweepResult {

        var attemptIndex = 0
        var lastFailureReason = "Unknown sweep failure."

        while (attemptIndex < 2) {
            val result = runCatching {
                resolvedDriver.acquireSweep(
                    commandChannel,
                    startFrequencyHz,
                    endFrequencyHz,
                    pointCount
                )
            }.getOrElse {
                latestTransportHealthSnapshot = commandChannel.getLatestTransportHealthSnapshot()
                lastFailureReason = it.message ?: "Unknown error"
                attemptIndex++
                continue
            }

            latestTransportHealthSnapshot = commandChannel.getLatestTransportHealthSnapshot()

            val validationFailure = SweepValidator.explainValidationFailure(result)
            if (validationFailure == null) {
                return result
            }

            lastFailureReason = validationFailure
            attemptIndex++
        }

        throw IllegalStateException(lastFailureReason)
    }

    private fun Double.roundToLongSafe(): Long {
        return round(this).toLong()
    }
}