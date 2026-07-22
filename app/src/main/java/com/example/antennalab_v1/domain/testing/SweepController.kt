package com.example.antennalab_v1.domain.testing

/*
########################################################################
FILE: SweepController.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing Control

LAST UPDATED 4/4/2026 00:08

SYSTEM ROLE
Controls the frequency sweep measurement process.

ARCHITECTURE ROLE (UPDATED)
Now tracks the latest structured sweep execution error so higher layers
can classify why a real sweep path was not used or why a real result was
rejected.

IMPORTANT CHANGE
This version now also tracks whether a later successful real sweep has
completed after the last failure. Higher UI layers can use that to clear
stale operator warnings instead of continuing to show an older failure
after a later live sweep succeeds.
########################################################################
*/

import com.example.antennalab_v1.model.testing.InstrumentError
import com.example.antennalab_v1.model.testing.InstrumentErrorCategory
import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

object SweepController {

    private val transport: UsbVnaTransport = FoundationUsbVnaTransport()

    private var lastExecutionError: InstrumentError? = null
    private var lastFailureSequence: Long = 0L
    private var lastSuccessfulRealSweepSequence: Long = 0L
    private var sequenceCounter: Long = 0L

    /*
    DEBUG-ONLY: when true, the next simulated sweep is truncated so it returns
    fewer points than requested (isComplete = false), letting the incomplete-
    sweep UI/persistence path be exercised without real hardware. Toggled from
    a debug-only control in the sweep workspace; never set in release builds.
    */
    var debugForceIncompleteSimulatedSweep: Boolean = false

    fun getLastExecutionError(): InstrumentError? {
        return lastExecutionError
    }

    fun hasSuccessfulRealSweepAfterLastFailure(): Boolean {
        return lastSuccessfulRealSweepSequence > 0L &&
                lastSuccessfulRealSweepSequence > lastFailureSequence
    }

    fun runSweep(
        startMHz: Double,
        endMHz: Double,
        stepMHz: Double
    ): SweepResult {
        val normalizedRequest = normalizeSweepRequest(
            startMHz = startMHz,
            endMHz = endMHz,
            stepMHz = stepMHz
        )

        return if (shouldUseRealSweepSource()) {
            runSelectedSweepSource(
                startMHz = normalizedRequest.startMHz,
                endMHz = normalizedRequest.endMHz,
                stepMHz = normalizedRequest.stepMHz
            )
        } else {
            markFailure(
                buildSimulatedFallbackError()
            )
            runSimulatedSweep(
                startMHz = normalizedRequest.startMHz,
                endMHz = normalizedRequest.endMHz,
                stepMHz = normalizedRequest.stepMHz
            )
        }
    }

    private fun shouldUseRealSweepSource(): Boolean {
        if (!HardwareSweepCapability.isRealSweepPossible()) {
            return false
        }

        return transport.isReady()
    }

    private fun runSelectedSweepSource(
        startMHz: Double,
        endMHz: Double,
        stepMHz: Double
    ): SweepResult {
        val selectedSource = SweepSourceSelector.selectSweepSource()

        if (selectedSource is DemoSweepDataSource) {
            markFailure(
                InstrumentError(
                    category = InstrumentErrorCategory.SWEEP,
                    code = "SWEEP_SOURCE_RESOLVED_TO_SIMULATION",
                    summary = "Sweep source resolved to simulation instead of a real instrument source.",
                    detail = "Source selection did not return a real instrument-backed data source.",
                    recoverable = true
                )
            )

            throw IllegalStateException(
                lastExecutionError?.detail ?: "Sweep source selection resolved to simulation."
            )
        }

        val realSweepResult = runCatching {
            selectedSource.runSweep(
                startMHz = startMHz,
                endMHz = endMHz,
                stepMHz = stepMHz
            )
        }.getOrElse { error ->
            markFailure(
                InstrumentError(
                    category = InstrumentErrorCategory.SWEEP,
                    code = "REAL_SWEEP_EXECUTION_EXCEPTION",
                    summary = "Real sweep execution threw an exception.",
                    detail = error.message ?: "Unknown real sweep execution exception.",
                    recoverable = true
                )
            )

            throw IllegalStateException(
                lastExecutionError?.detail ?: "Real sweep execution exception."
            )
        }

        ensureMinimumSafePointCount(realSweepResult)

        if (SweepValidator.validateSweep(realSweepResult)) {
            lastExecutionError = null
            markSuccessfulRealSweep()
            return realSweepResult
        }

        markFailure(
            InstrumentError(
                category = InstrumentErrorCategory.SWEEP,
                code = "REAL_SWEEP_VALIDATION_FAILED",
                summary = "Real sweep result failed validation.",
                detail = "Real sweep returned data, but SweepValidator rejected the result.",
                recoverable = true
            )
        )

        throw IllegalStateException(
            lastExecutionError?.detail ?: "Real sweep validation failed."
        )
    }

    private fun ensureMinimumSafePointCount(
        realSweepResult: SweepResult
    ) {
        if (realSweepResult.points.size >= MIN_SAFE_REAL_SWEEP_POINTS) {
            return
        }

        markFailure(
            InstrumentError(
                category = InstrumentErrorCategory.SWEEP,
                code = "REAL_SWEEP_TOO_FEW_POINTS",
                summary = "Real sweep returned too few usable sweep points.",
                detail = "Real sweep returned only ${realSweepResult.points.size} point(s). At least $MIN_SAFE_REAL_SWEEP_POINTS points are required for safe live graphing and analysis.",
                recoverable = true
            )
        )

        throw IllegalStateException(
            lastExecutionError?.detail ?: "Real sweep returned too few usable sweep points."
        )
    }

    private fun runSimulatedSweep(
        startMHz: Double,
        endMHz: Double,
        stepMHz: Double
    ): SweepResult {
        val startTime = System.currentTimeMillis()
        val points = mutableListOf<SweepPoint>()

        val spanMHz = endMHz - startMHz
        val estimatedPointCount =
            ((spanMHz / stepMHz).toInt() + 1).coerceAtLeast(1)
        val safePointCount = estimatedPointCount.coerceAtMost(MAX_SIMULATED_POINTS)

        for (index in 0 until safePointCount) {
            val rawFrequency = startMHz + (index * stepMHz)
            val frequencyMHz = min(rawFrequency, endMHz)

            val swr = simulateSWR(frequencyMHz)
            val returnLossDb = calculateReturnLossDb(swr)
            val resistance = simulateResistance(frequencyMHz)
            val reactance = simulateReactance(frequencyMHz)

            points.add(
                SweepPoint(
                    frequencyMHz = frequencyMHz,
                    swr = swr,
                    returnLossDb = returnLossDb,
                    resistance = resistance,
                    reactance = reactance,
                    s11MagnitudeDb = returnLossDb,
                    s11PhaseDegrees = reactance,
                    s21MagnitudeDb = 0.0,
                    s21PhaseDegrees = 0.0
                )
            )

            if (frequencyMHz >= endMHz) {
                break
            }
        }

        val duration = System.currentTimeMillis() - startTime

        // DEBUG: optionally return a partial sweep to exercise the incomplete
        // path. Mirrors real hardware (Phase 1): the end frequency derives from
        // the last measured point, while requestedPointCount keeps the full
        // intended count so isComplete resolves to false.
        val requestedPointCount = points.size
        if (debugForceIncompleteSimulatedSweep && requestedPointCount >= 4) {
            val keptPointCount = (requestedPointCount * 0.7).toInt().coerceAtLeast(2)
            val partialPoints = points.take(keptPointCount)

            return SweepResult(
                startFrequencyMHz = startMHz,
                endFrequencyMHz = partialPoints.last().frequencyMHz,
                stepMHz = stepMHz,
                points = partialPoints,
                sweepPointCount = partialPoints.size,
                requestedPointCount = requestedPointCount,
                actualPointCount = partialPoints.size,
                isComplete = false,
                sweepDurationMs = duration,
                hardwareProfile = "SIMULATED",
                supportsS11 = true,
                supportsS11Phase = true,
                supportsS21 = false,
                supportsS21Phase = false
            )
        }

        return SweepResult(
            startFrequencyMHz = startMHz,
            endFrequencyMHz = endMHz,
            stepMHz = stepMHz,
            points = points,
            sweepPointCount = points.size,
            sweepDurationMs = duration,
            hardwareProfile = "SIMULATED",
            supportsS11 = true,
            supportsS11Phase = true,
            supportsS21 = false,
            supportsS21Phase = false
        )
    }

    private fun simulateSWR(
        freq: Double
    ): Double {
        val center = 14.2
        val distance = abs(freq - center)

        return 1.05 + (distance * 5.0)
    }

    private fun calculateReturnLossDb(
        swr: Double
    ): Double {
        val gamma = (swr - 1.0) / (swr + 1.0)
        val safeGamma = max(gamma, 0.000001)

        return -20.0 * ln(safeGamma) / ln(10.0)
    }

    private fun simulateResistance(
        freq: Double
    ): Double {
        val center = 14.2
        val distance = abs(freq - center)

        return 50.0 + (distance * 40.0)
    }

    private fun simulateReactance(
        freq: Double
    ): Double {
        val center = 14.2
        val delta = freq - center

        return delta * 120.0
    }

    private fun normalizeSweepRequest(
        startMHz: Double,
        endMHz: Double,
        stepMHz: Double
    ): NormalizedSweepRequest {
        val safeStart = min(startMHz, endMHz)
        val safeEnd = max(startMHz, endMHz)
        val safeStep =
            if (stepMHz > MIN_STEP_MHZ) {
                stepMHz
            } else {
                DEFAULT_STEP_MHZ
            }

        return NormalizedSweepRequest(
            startMHz = safeStart,
            endMHz = safeEnd,
            stepMHz = safeStep
        )
    }

    private fun buildSimulatedFallbackError(): InstrumentError {
        val transportStatus = transport.evaluateTransportStatus()

        return InstrumentError(
            category = InstrumentErrorCategory.TRANSPORT,
            code = "REAL_SWEEP_PATH_NOT_READY",
            summary = "Real sweep path is not ready, so simulation was used.",
            detail = transportStatus.summary,
            recoverable = true
        )
    }

    private fun markFailure(
        error: InstrumentError
    ) {
        lastExecutionError = error
        sequenceCounter += 1L
        lastFailureSequence = sequenceCounter
    }

    private fun markSuccessfulRealSweep() {
        sequenceCounter += 1L
        lastSuccessfulRealSweepSequence = sequenceCounter
    }

    private data class NormalizedSweepRequest(
        val startMHz: Double,
        val endMHz: Double,
        val stepMHz: Double
    )

    private const val DEFAULT_STEP_MHZ = 0.01
    private const val MIN_STEP_MHZ = 0.000001
    private const val MAX_SIMULATED_POINTS = 20001
    private const val MIN_SAFE_REAL_SWEEP_POINTS = 8
}