package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.BuildConfig
import kotlin.text.Charsets.UTF_8

/*
########################################################################
FILE: UsbVnaCommandChannel.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / USB Transport

LAST UPDATED 4/4/2026 00:35

CURRENT DEVELOPMENT ROLE
This version is transport-aware:
- BULK_GENERIC keeps previous text command behaviour
- CDC_DATA uses LiteVNA V2 binary handshake and register reads
- LiteVNA sweep reads now accumulate FIFO chunks across multiple reads
  instead of trusting one short FIFO response

IMPORTANT CHANGE
UsbVnaCommandResult now carries rawResponseBytes so parser layers can use
the full captured byte payload instead of a shortened hex preview string.

SECOND IMPORTANT CHANGE
LiteVNA configured sweep read now:
- writes requested sweep setup
- clears stale FIFO
- waits for capture settle
- repeatedly reads FIFO chunks
- accumulates complete records until target or timeout

THIRD IMPORTANT CHANGE
Sweep FIFO accumulation is now more patient:
- longer settle time
- more accumulation attempts
- a stall detector so reads continue while payload is still growing
########################################################################
*/

data class UsbVnaCommandResult(
    val success: Boolean,
    val summary: String,
    val responseText: String? = null,
    val rawResponseBytes: ByteArray = byteArrayOf(),
    val bytesTransferred: Int = 0,
    val readPassCount: Int = 0,
    val debugSummary: String = "",
    val lastReadSizeBytes: Int = 0,
    val lastErrorReason: String? = null
)

data class UsbVnaTransportHealthSnapshot(
    val lastCommandSucceeded: Boolean = false,
    val lastReadSizeBytes: Int = 0,
    val lastErrorReason: String? = null,
    val lastOperationSummary: String = "No USB VNA transport activity yet."
)

data class UsbAnalyzerIdentityResult(
    val success: Boolean,
    val summary: String,
    val rawIdentityText: String? = null,
    val protocolIdentity: UsbVnaProtocolIdentity = UsbVnaProtocolIdentity(
        family = UsbVnaProtocolFamily.UNKNOWN,
        displayName = "Unknown",
        rawIdentityText = null
    ),
    val commandDebugSummary: String = ""
)

data class LiteVnaSweepRequest(
    val startFrequencyHz: Long,
    val stepFrequencyHz: Long,
    val pointCount: Int,
    val valuesPerFrequency: Int = 1
)

class UsbVnaCommandChannel(
    private val sessionManager: UsbSessionManagerFacade = DefaultUsbSessionManagerFacade()
) {

    companion object {
        private const val DEFAULT_WRITE_TIMEOUT_MS = 600
        private const val DEFAULT_READ_TIMEOUT_MS = 800
        private const val DEFAULT_POST_WRITE_DELAY_MS = 80
        private const val DEFAULT_INTER_READ_DELAY_MS = 15
        private const val MINIMUM_READ_BUFFER_SIZE = 64
        private const val MAX_READ_PASSES = 14
        private const val MAX_RESPONSE_BYTES = 32 * 1024
        private const val RAW_PREVIEW_LIMIT = 512

        private const val LITEVNA_INDICATE_COMMAND: Byte = 0x0D
        private const val LITEVNA_READ1_COMMAND: Byte = 0x10
        private const val LITEVNA_WRITE_COMMAND: Byte = 0x20
        private const val LITEVNA_WRITE2_COMMAND: Byte = 0x21
        private const val LITEVNA_WRITE8_COMMAND: Byte = 0x23
        private const val LITEVNA_READFIFO_COMMAND: Byte = 0x18

        private const val REG_SWEEP_START: Byte = 0x00
        private const val REG_SWEEP_STEP: Byte = 0x10
        private const val REG_SWEEP_POINTS: Byte = 0x20
        private const val REG_VALUES_PER_FREQUENCY: Byte = 0x22
        private const val REG_VALUES_FIFO: Byte = 0x30
        private const val LITEVNA_FIFO_RECORD_SIZE_BYTES = 32
        // The LiteVNA dribbles ~2-3 records per readFIFO, so ~40-50 re-issues are
        // needed to drain 101 points. This cap is a runaway backstop only; the real
        // stop conditions are all-records-collected or the wall-clock deadline.
        private const val MAX_FIFO_ACCUMULATION_ATTEMPTS = 250
        // Per-bulk-read timeout for the count-driven FIFO drain. Short so an ended
        // burst is detected fast and the next readFIFO is re-issued promptly; the
        // overall bound is the wall-clock budget.
        private const val FIFO_READ_TIMEOUT_MS = 120
    }

    private var latestTransportHealthSnapshot = UsbVnaTransportHealthSnapshot()

    fun isChannelAvailable(): Boolean {
        return sessionManager.hasOpenSession() &&
                sessionManager.getActiveTransportChannel() != null
    }

    fun getLatestTransportHealthSnapshot(): UsbVnaTransportHealthSnapshot {
        return latestTransportHealthSnapshot
    }

    fun isUsingCdcSerialTransport(): Boolean {
        return sessionManager.getActiveTransportChannel()?.transportKind ==
                UsbTransportKind.CDC_DATA
    }

    fun readLiteVnaRegisterByteForBringUp(
        registerAddress: Int
    ): UsbVnaCommandResult {
        if (!isUsingCdcSerialTransport()) {
            return buildFailureResult(
                summary = "LiteVNA register read is only implemented through CDC transport in this version.",
                debugSummary = "Active transport kind was not CDC_DATA.",
                errorReason = "Unsupported non-CDC LiteVNA register path."
            )
        }

        val registerResult = executeLiteVnaBinaryCommand(
            commandBytes = byteArrayOf(
                LITEVNA_READ1_COMMAND,
                registerAddress.toByte()
            ),
            expectedMinimumResponseBytes = 1,
            wallClockTimeoutMs = 1200L,
            readTimeoutMs = 400,
            maxReadPasses = 4,
            postWriteDelayMs = 10
        )

        if (!registerResult.success) {
            return buildFailureResult(
                summary = "LiteVNA register read failed for address 0x${registerAddress.toString(16).uppercase()}. ${registerResult.summary}",
                responseText = registerResult.responseText,
                rawResponseBytes = registerResult.rawResponseBytes,
                bytesTransferred = registerResult.bytesTransferred,
                readPassCount = registerResult.readPassCount,
                debugSummary = registerResult.debugSummary,
                lastReadSizeBytes = registerResult.lastReadSizeBytes,
                errorReason = registerResult.lastErrorReason
            )
        }

        val rawBytes = registerResult.rawResponseBytes

        if (rawBytes.isEmpty()) {
            return buildFailureResult(
                summary = "LiteVNA register read returned no decodable binary bytes.",
                responseText = registerResult.responseText,
                rawResponseBytes = registerResult.rawResponseBytes,
                bytesTransferred = registerResult.bytesTransferred,
                readPassCount = registerResult.readPassCount,
                debugSummary = registerResult.debugSummary,
                lastReadSizeBytes = registerResult.lastReadSizeBytes,
                errorReason = "No decodable binary response bytes."
            )
        }

        val registerValue =
            rawBytes.first().toUByte().toString(16)
                .uppercase()
                .padStart(2, '0')

        return buildSuccessResult(
            summary = "LiteVNA register read succeeded for address 0x${registerAddress.toString(16).uppercase()}.",
            responseText = registerValue,
            rawResponseBytes = rawBytes,
            bytesTransferred = registerResult.bytesTransferred,
            readPassCount = registerResult.readPassCount,
            debugSummary = registerResult.debugSummary + " | registerAddress=0x${registerAddress.toString(16).uppercase()} value=0x$registerValue",
            lastReadSizeBytes = registerResult.lastReadSizeBytes
        )
    }

    fun runLiteVnaCdcHandshakeTest(): UsbVnaCommandResult {
        val channel = sessionManager.getActiveTransportChannel()
            ?: return buildFailureResult(
                summary = "LiteVNA CDC handshake cannot run because no active transport channel exists.",
                debugSummary = "runLiteVnaCdcHandshakeTest found no active channel.",
                errorReason = "No active transport channel."
            )

        if (channel.transportKind != UsbTransportKind.CDC_DATA) {
            return buildFailureResult(
                summary = "LiteVNA CDC handshake cannot run because active transport is not CDC.",
                debugSummary = "runLiteVnaCdcHandshakeTest transport=${channel.transportKind}",
                errorReason = "Non-CDC transport."
            )
        }

        val result = executeLiteVnaBinaryCommand(
            commandBytes = byteArrayOf(LITEVNA_INDICATE_COMMAND),
            expectedMinimumResponseBytes = 1,
            wallClockTimeoutMs = 1800L,
            readTimeoutMs = 600,
            maxReadPasses = 8,
            postWriteDelayMs = 20
        )

        if (!result.success) {
            return buildFailureResult(
                summary = "LiteVNA CDC binary handshake failed. ${result.summary}",
                responseText = result.responseText,
                rawResponseBytes = result.rawResponseBytes,
                bytesTransferred = result.bytesTransferred,
                readPassCount = result.readPassCount,
                debugSummary = result.debugSummary,
                lastReadSizeBytes = result.lastReadSizeBytes,
                errorReason = result.lastErrorReason
            )
        }

        val rawBytes = result.rawResponseBytes

        if (rawBytes.isEmpty()) {
            return buildFailureResult(
                summary = "LiteVNA CDC binary handshake returned no decodable bytes.",
                responseText = result.responseText,
                rawResponseBytes = result.rawResponseBytes,
                bytesTransferred = result.bytesTransferred,
                readPassCount = result.readPassCount,
                debugSummary = result.debugSummary,
                lastReadSizeBytes = result.lastReadSizeBytes,
                errorReason = "No decodable CDC binary handshake bytes."
            )
        }

        val firstByte = rawBytes.first().toUByte().toInt()

        return buildSuccessResult(
            summary = if (firstByte == 0x32) {
                "LiteVNA CDC binary handshake succeeded. INDICATE returned 0x32."
            } else {
                "LiteVNA CDC binary handshake returned byte 0x${firstByte.toString(16).uppercase()}."
            },
            responseText = rawBytePreview(rawBytes),
            rawResponseBytes = rawBytes,
            bytesTransferred = result.bytesTransferred,
            readPassCount = result.readPassCount,
            debugSummary = result.debugSummary + " | indicateFirstByte=0x${firstByte.toString(16).uppercase()}",
            lastReadSizeBytes = result.lastReadSizeBytes
        )
    }

    fun runLiteVnaConfiguredSweepRead(
        request: LiteVnaSweepRequest
    ): UsbVnaCommandResult {
        if (!isUsingCdcSerialTransport()) {
            return buildFailureResult(
                summary = "LiteVNA configured sweep read is only implemented through CDC transport in this version.",
                debugSummary = "Active transport kind was not CDC_DATA.",
                errorReason = "Unsupported non-CDC LiteVNA sweep path."
            )
        }

        val validatedPointCount =
            request.pointCount.coerceIn(1, 255)

        val validatedValuesPerFrequency =
            request.valuesPerFrequency.coerceAtLeast(1)

        val handshake = runLiteVnaCdcHandshakeTest()
        if (!handshake.success) {
            return buildFailureResult(
                summary = "LiteVNA configured sweep read cannot start because handshake failed. ${handshake.summary}",
                responseText = handshake.responseText,
                rawResponseBytes = handshake.rawResponseBytes,
                bytesTransferred = handshake.bytesTransferred,
                readPassCount = handshake.readPassCount,
                debugSummary = handshake.debugSummary,
                lastReadSizeBytes = handshake.lastReadSizeBytes,
                errorReason = handshake.lastErrorReason
            )
        }

        val clearFifo = executeLiteVnaBinaryCommand(
            commandBytes = byteArrayOf(
                LITEVNA_WRITE_COMMAND,
                REG_VALUES_FIFO,
                0x00
            ),
            expectedMinimumResponseBytes = 0,
            wallClockTimeoutMs = 800L,
            readTimeoutMs = 120,
            maxReadPasses = 1,
            postWriteDelayMs = 10
        )

        if (!clearFifo.success) {
            return buildFailureResult(
                summary = "LiteVNA configured sweep read failed while clearing stale FIFO data. ${clearFifo.summary}",
                responseText = clearFifo.responseText,
                rawResponseBytes = clearFifo.rawResponseBytes,
                bytesTransferred = clearFifo.bytesTransferred,
                readPassCount = clearFifo.readPassCount,
                debugSummary = clearFifo.debugSummary,
                lastReadSizeBytes = clearFifo.lastReadSizeBytes,
                errorReason = clearFifo.lastErrorReason
            )
        }

        val sweepStartWrite = executeLiteVnaBinaryCommand(
            commandBytes = byteArrayOf(
                LITEVNA_WRITE8_COMMAND,
                REG_SWEEP_START
            ) + encodeLittleEndian8(request.startFrequencyHz),
            expectedMinimumResponseBytes = 0,
            wallClockTimeoutMs = 800L,
            readTimeoutMs = 120,
            maxReadPasses = 1,
            postWriteDelayMs = 10
        )

        if (!sweepStartWrite.success) {
            return buildFailureResult(
                summary = "LiteVNA configured sweep read failed while writing sweep start. ${sweepStartWrite.summary}",
                responseText = sweepStartWrite.responseText,
                rawResponseBytes = sweepStartWrite.rawResponseBytes,
                bytesTransferred = sweepStartWrite.bytesTransferred,
                readPassCount = sweepStartWrite.readPassCount,
                debugSummary = sweepStartWrite.debugSummary,
                lastReadSizeBytes = sweepStartWrite.lastReadSizeBytes,
                errorReason = sweepStartWrite.lastErrorReason
            )
        }

        val sweepStepWrite = executeLiteVnaBinaryCommand(
            commandBytes = byteArrayOf(
                LITEVNA_WRITE8_COMMAND,
                REG_SWEEP_STEP
            ) + encodeLittleEndian8(request.stepFrequencyHz),
            expectedMinimumResponseBytes = 0,
            wallClockTimeoutMs = 800L,
            readTimeoutMs = 120,
            maxReadPasses = 1,
            postWriteDelayMs = 10
        )

        if (!sweepStepWrite.success) {
            return buildFailureResult(
                summary = "LiteVNA configured sweep read failed while writing sweep step. ${sweepStepWrite.summary}",
                responseText = sweepStepWrite.responseText,
                rawResponseBytes = sweepStepWrite.rawResponseBytes,
                bytesTransferred = sweepStepWrite.bytesTransferred,
                readPassCount = sweepStepWrite.readPassCount,
                debugSummary = sweepStepWrite.debugSummary,
                lastReadSizeBytes = sweepStepWrite.lastReadSizeBytes,
                errorReason = sweepStepWrite.lastErrorReason
            )
        }

        val sweepPointsWrite = executeLiteVnaBinaryCommand(
            commandBytes = byteArrayOf(
                LITEVNA_WRITE2_COMMAND,
                REG_SWEEP_POINTS
            ) + encodeLittleEndian2(validatedPointCount),
            expectedMinimumResponseBytes = 0,
            wallClockTimeoutMs = 800L,
            readTimeoutMs = 120,
            maxReadPasses = 1,
            postWriteDelayMs = 10
        )

        if (!sweepPointsWrite.success) {
            return buildFailureResult(
                summary = "LiteVNA configured sweep read failed while writing sweep points. ${sweepPointsWrite.summary}",
                responseText = sweepPointsWrite.responseText,
                rawResponseBytes = sweepPointsWrite.rawResponseBytes,
                bytesTransferred = sweepPointsWrite.bytesTransferred,
                readPassCount = sweepPointsWrite.readPassCount,
                debugSummary = sweepPointsWrite.debugSummary,
                lastReadSizeBytes = sweepPointsWrite.lastReadSizeBytes,
                errorReason = sweepPointsWrite.lastErrorReason
            )
        }

        val valuesPerFrequencyWrite = executeLiteVnaBinaryCommand(
            commandBytes = byteArrayOf(
                LITEVNA_WRITE2_COMMAND,
                REG_VALUES_PER_FREQUENCY
            ) + encodeLittleEndian2(validatedValuesPerFrequency),
            expectedMinimumResponseBytes = 0,
            wallClockTimeoutMs = 800L,
            readTimeoutMs = 120,
            maxReadPasses = 1,
            postWriteDelayMs = 10
        )

        if (!valuesPerFrequencyWrite.success) {
            return buildFailureResult(
                summary = "LiteVNA configured sweep read failed while writing values-per-frequency. ${valuesPerFrequencyWrite.summary}",
                responseText = valuesPerFrequencyWrite.responseText,
                rawResponseBytes = valuesPerFrequencyWrite.rawResponseBytes,
                bytesTransferred = valuesPerFrequencyWrite.bytesTransferred,
                readPassCount = valuesPerFrequencyWrite.readPassCount,
                debugSummary = valuesPerFrequencyWrite.debugSummary,
                lastReadSizeBytes = valuesPerFrequencyWrite.lastReadSizeBytes,
                errorReason = valuesPerFrequencyWrite.lastErrorReason
            )
        }

        applyDelay(
            delayMs = calculateSweepSettleDelayMs(
                pointCount = validatedPointCount
            )
        )

        // Count-driven, wall-clock-bounded FIFO drain. The read is governed by the
        // EXPECTED record count (validatedPointCount) and a hard wall-clock deadline,
        // NOT by a fixed number of USB read passes — the old fixed maxReadPasses=10 ×
        // 64-byte packets capped every read at 20 records, so a 101-point sweep only
        // ever returned ~20. maxReadPasses is now sized as a generous backstop.
        val readPacketSize =
            sessionManager.getActiveTransportChannel()?.maxReadPacketSize?.coerceAtLeast(64) ?: 64
        val overallBudget = computeFifoReadBudget(
            expectedRecordCount = validatedPointCount,
            packetSizeBytes = readPacketSize
        )
        val deadlineMs = System.currentTimeMillis() + overallBudget.wallClockBudgetMs

        val accumulatedBytes = mutableListOf<Byte>()
        val accumulatedDebug = mutableListOf<String>()
        var accumulatedReadPassCount = 0
        var lastReadSizeBytes = 0
        var attemptIndex = 0
        var stallAttemptCount = 0
        var previousByteCount = 0
        var lastFailure: UsbVnaCommandResult? = null

        // Attempt cap is only a runaway backstop; the real stop conditions are
        // "all expected records" or "wall-clock exhausted" (shouldContinueFifoAccumulation).
        while (
            attemptIndex < MAX_FIFO_ACCUMULATION_ATTEMPTS &&
            shouldContinueFifoAccumulation(
                completeRecordCount = fifoRecordCount(accumulatedBytes.size),
                expectedRecordCount = validatedPointCount,
                nowMs = System.currentTimeMillis(),
                deadlineMs = deadlineMs
            )
        ) {
            attemptIndex += 1

            val currentCompleteRecordCount = fifoRecordCount(accumulatedBytes.size)

            val remainingRecordCount =
                (validatedPointCount - currentCompleteRecordCount)
                    .coerceAtLeast(1)
                    .coerceAtMost(validatedPointCount)

            val remainingMs = (deadlineMs - System.currentTimeMillis()).coerceAtLeast(0)
            val attemptBudget = computeFifoReadBudget(
                expectedRecordCount = remainingRecordCount,
                packetSizeBytes = readPacketSize
            )

            val fifoRead = executeLiteVnaFifoRead(
                commandBytes = byteArrayOf(
                    LITEVNA_READFIFO_COMMAND,
                    REG_VALUES_FIFO,
                    remainingRecordCount.toByte()
                ),
                expectedBytes = remainingRecordCount * LITEVNA_FIFO_RECORD_SIZE_BYTES,
                maxReadPasses = attemptBudget.maxReadPasses,
                readTimeoutMs = FIFO_READ_TIMEOUT_MS,
                maxConsecutiveIdleReads = overallBudget.maxConsecutiveIdleReads,
                wallClockBudgetMs = remainingMs,
                postWriteDelayMs = 10
            )

            if (!fifoRead.success) {
                lastFailure = fifoRead

                if (accumulatedBytes.isNotEmpty()) {
                    // Partial already in hand: keep trying within the wall-clock budget
                    // (the loop guard enforces the deadline); never early-break on stall.
                    accumulatedDebug.add(
                        "attempt=$attemptIndex chunkFailure=true accumulatedBytes=${accumulatedBytes.size}"
                    )
                    applyDelay(55)
                    continue
                }

                return buildFailureResult(
                    summary = "LiteVNA configured sweep read failed during FIFO accumulation. ${fifoRead.summary}",
                    responseText = fifoRead.responseText,
                    rawResponseBytes = fifoRead.rawResponseBytes,
                    bytesTransferred = fifoRead.bytesTransferred,
                    readPassCount = fifoRead.readPassCount,
                    debugSummary = fifoRead.debugSummary,
                    lastReadSizeBytes = fifoRead.lastReadSizeBytes,
                    errorReason = fifoRead.lastErrorReason
                )
            }

            accumulatedReadPassCount += fifoRead.readPassCount
            lastReadSizeBytes = fifoRead.lastReadSizeBytes

            fifoRead.rawResponseBytes.forEach { byte ->
                accumulatedBytes.add(byte)
            }

            val completeRecordCount = fifoRecordCount(accumulatedBytes.size)

            val attemptLine =
                "attempt=$attemptIndex requestedRemaining=$remainingRecordCount chunkBytes=${fifoRead.rawResponseBytes.size} accumulatedBytes=${accumulatedBytes.size} completeRecords=$completeRecordCount expected=$validatedPointCount"
            accumulatedDebug.add(attemptLine)
            if (BuildConfig.DEBUG) {
                android.util.Log.i("LiteVnaFifo", attemptLine)
            }

            if (accumulatedBytes.size > previousByteCount) {
                stallAttemptCount = 0
                previousByteCount = accumulatedBytes.size
            } else {
                // No progress this attempt: give the device a moment to fill the FIFO,
                // then continue. We do NOT break — completion is count/wall-clock-driven.
                stallAttemptCount += 1
                applyDelay(55)
            }
        }

        val finalBytes = accumulatedBytes.toByteArray()
        val completeRecordCount = fifoRecordCount(finalBytes.size)

        if (finalBytes.isEmpty()) {
            val lastFailureSummary = lastFailure?.summary ?: "No FIFO bytes were accumulated."

            return buildFailureResult(
                summary = "LiteVNA configured sweep read completed with no FIFO payload. $lastFailureSummary",
                responseText = null,
                rawResponseBytes = byteArrayOf(),
                bytesTransferred = 0,
                readPassCount = accumulatedReadPassCount,
                debugSummary = accumulatedDebug.joinToString(separator = " | "),
                lastReadSizeBytes = lastReadSizeBytes,
                errorReason = "No accumulated FIFO payload."
            )
        }

        return buildSuccessResult(
            summary =
                "LiteVNA configured sweep read succeeded. Requested $validatedPointCount point(s); received ${finalBytes.size} byte(s) / $completeRecordCount complete 32-byte record(s).",
            responseText = rawBytePreview(finalBytes),
            rawResponseBytes = finalBytes,
            bytesTransferred = finalBytes.size,
            readPassCount = accumulatedReadPassCount,
            debugSummary =
                listOf(
                    "configuredSweep=true",
                    "startHz=${request.startFrequencyHz}",
                    "stepHz=${request.stepFrequencyHz}",
                    "pointCount=$validatedPointCount",
                    "valuesPerFrequency=$validatedValuesPerFrequency",
                    "completeRecordCount=$completeRecordCount",
                    "stallAttemptCount=$stallAttemptCount"
                ).plus(accumulatedDebug).joinToString(separator = " | "),
            lastReadSizeBytes = lastReadSizeBytes
        )
    }

    fun runLiteVnaMiniSweepProbe(): UsbVnaCommandResult {
        return runLiteVnaConfiguredSweepRead(
            request = LiteVnaSweepRequest(
                startFrequencyHz = 14_000_000L,
                stepFrequencyHz = 100_000L,
                pointCount = 8,
                valuesPerFrequency = 1
            )
        )
    }

    fun sendCommand(
        commandText: String,
        appendNewline: Boolean = true,
        timeoutMs: Int = DEFAULT_WRITE_TIMEOUT_MS
    ): UsbVnaCommandResult {
        if (commandText.isBlank()) {
            return buildFailureResult(
                summary = "USB VNA command channel rejected a blank command.",
                debugSummary = "Command rejected before transport use because command text was blank.",
                errorReason = "Blank command text."
            )
        }

        val channel = sessionManager.getActiveTransportChannel()
            ?: return buildFailureResult(
                summary = "USB VNA command channel is unavailable because no active transport channel exists.",
                debugSummary = "No active transport channel was available at send time.",
                errorReason = "No active transport channel."
            )

        val payloadBytes = buildPayloadBytes(
            commandText = commandText,
            appendNewline = appendNewline
        )

        return if (channel.transportKind == UsbTransportKind.CDC_DATA) {
            sendCdcCommand(channel, payloadBytes, commandText, timeoutMs)
        } else {
            sendBulkCommand(channel, payloadBytes, commandText, timeoutMs)
        }
    }

    fun readResponse(
        timeoutMs: Int = DEFAULT_READ_TIMEOUT_MS,
        expectedCommandEcho: String? = null,
        maxReadPassesOverride: Int = MAX_READ_PASSES,
        interReadDelayMs: Int = DEFAULT_INTER_READ_DELAY_MS
    ): UsbVnaCommandResult {
        val channel = sessionManager.getActiveTransportChannel()
            ?: return buildFailureResult(
                summary = "USB VNA response read is unavailable because no active transport channel exists.",
                debugSummary = "No active transport channel was available at read time.",
                errorReason = "No active transport channel."
            )

        return if (channel.transportKind == UsbTransportKind.CDC_DATA) {
            readCdcResponse(
                channel = channel,
                timeoutMs = timeoutMs,
                expectedCommandEcho = expectedCommandEcho,
                maxReadPassesOverride = maxReadPassesOverride,
                interReadDelayMs = interReadDelayMs
            )
        } else {
            readBulkResponse(
                channel = channel,
                timeoutMs = timeoutMs,
                expectedCommandEcho = expectedCommandEcho,
                maxReadPassesOverride = maxReadPassesOverride,
                interReadDelayMs = interReadDelayMs
            )
        }
    }

    fun sendCommandAndReadResponse(
        commandText: String,
        appendNewline: Boolean = true,
        writeTimeoutMs: Int = DEFAULT_WRITE_TIMEOUT_MS,
        readTimeoutMs: Int = DEFAULT_READ_TIMEOUT_MS,
        postWriteDelayMs: Int = DEFAULT_POST_WRITE_DELAY_MS
    ): UsbVnaCommandResult {
        val writeResult = sendCommand(
            commandText = commandText,
            appendNewline = appendNewline,
            timeoutMs = writeTimeoutMs
        )

        if (!writeResult.success) {
            return writeResult
        }

        applyDelay(postWriteDelayMs)

        val readResult = readResponse(
            timeoutMs = readTimeoutMs,
            expectedCommandEcho = commandText,
            maxReadPassesOverride = MAX_READ_PASSES,
            interReadDelayMs = DEFAULT_INTER_READ_DELAY_MS
        )

        if (!readResult.success) {
            return buildFailureResult(
                summary = writeResult.summary + " " + readResult.summary,
                responseText = readResult.responseText,
                rawResponseBytes = readResult.rawResponseBytes,
                bytesTransferred = readResult.bytesTransferred,
                readPassCount = readResult.readPassCount,
                debugSummary = writeResult.debugSummary + " | " + readResult.debugSummary,
                lastReadSizeBytes = readResult.lastReadSizeBytes,
                errorReason = readResult.lastErrorReason
            )
        }

        return buildSuccessResult(
            summary = writeResult.summary + " " + readResult.summary,
            responseText = readResult.responseText,
            rawResponseBytes = readResult.rawResponseBytes,
            bytesTransferred = readResult.bytesTransferred,
            readPassCount = readResult.readPassCount,
            debugSummary = writeResult.debugSummary + " | " + readResult.debugSummary,
            lastReadSizeBytes = readResult.lastReadSizeBytes
        )
    }

    fun queryAnalyzerIdentity(): UsbAnalyzerIdentityResult {
        val cached = UsbSessionManager.getLatestAnalyzerIdentityResult()
        if (cached != null && cached.success) {
            return cached
        }

        if (isUsingCdcSerialTransport()) {
            val handshake = runLiteVnaCdcHandshakeTest()

            if (!handshake.success) {
                val failure = UsbAnalyzerIdentityResult(
                    success = false,
                    summary = handshake.summary,
                    rawIdentityText = handshake.responseText,
                    protocolIdentity = UsbVnaProtocolResolver.resolveFromIdentityText(
                        rawIdentityText = handshake.responseText
                    ),
                    commandDebugSummary = handshake.debugSummary
                )
                UsbSessionManager.registerAnalyzerIdentityResult(failure)
                return failure
            }

            val deviceVariant = readLiteVnaRegisterByteForBringUp(0xF0)
            if (!deviceVariant.success) {
                val failure = UsbAnalyzerIdentityResult(
                    success = false,
                    summary = "LiteVNA identity failed at deviceVariant register. ${deviceVariant.summary}",
                    rawIdentityText = deviceVariant.responseText,
                    protocolIdentity = UsbVnaProtocolResolver.resolveFromIdentityText(
                        rawIdentityText = deviceVariant.responseText
                    ),
                    commandDebugSummary = deviceVariant.debugSummary
                )
                UsbSessionManager.registerAnalyzerIdentityResult(failure)
                return failure
            }

            val protocolVersion = readLiteVnaRegisterByteForBringUp(0xF1)

            val rawIdentityText =
                "LiteVNA deviceVariant=0x${deviceVariant.responseText ?: "??"} " +
                        "protocolVersion=0x${protocolVersion.responseText ?: "??"}"

            val success = UsbAnalyzerIdentityResult(
                success = deviceVariant.responseText == "02",
                summary = "LiteVNA identity query completed over CDC binary protocol.",
                rawIdentityText = rawIdentityText,
                protocolIdentity = UsbVnaProtocolIdentity(
                    family = UsbVnaProtocolFamily.LITEVNA,
                    displayName = "LiteVNA",
                    rawIdentityText = rawIdentityText
                ),
                commandDebugSummary = listOf(
                    handshake.debugSummary,
                    deviceVariant.debugSummary,
                    protocolVersion.debugSummary
                ).joinToString(separator = " | ")
            )

            UsbSessionManager.registerAnalyzerIdentityResult(success)
            return success
        }

        val commands = listOf("version", "info", "v")
        var lastFailure: UsbAnalyzerIdentityResult? = null

        for (command in commands) {
            val roundTripResult = sendCommandAndReadResponse(
                commandText = command,
                appendNewline = true,
                writeTimeoutMs = 800,
                readTimeoutMs = 1200,
                postWriteDelayMs = 120
            )

            val rawIdentityText = sanitizeIdentityText(
                rawIdentityText = roundTripResult.responseText?.trim()
            )

            val protocolIdentity = UsbVnaProtocolResolver.resolveFromIdentityText(
                rawIdentityText = rawIdentityText
            )

            val currentResult =
                if (!roundTripResult.success) {
                    UsbAnalyzerIdentityResult(
                        success = false,
                        summary = "Analyzer identity command \"$command\" failed. ${roundTripResult.summary}",
                        rawIdentityText = rawIdentityText,
                        protocolIdentity = protocolIdentity,
                        commandDebugSummary = roundTripResult.debugSummary
                    )
                } else if (rawIdentityText.isNullOrBlank()) {
                    UsbAnalyzerIdentityResult(
                        success = false,
                        summary = "Analyzer identity command \"$command\" completed, but no readable identity text was returned.",
                        rawIdentityText = null,
                        protocolIdentity = protocolIdentity,
                        commandDebugSummary = roundTripResult.debugSummary
                    )
                } else if (protocolIdentity.family == UsbVnaProtocolFamily.UNKNOWN) {
                    UsbAnalyzerIdentityResult(
                        success = false,
                        summary = "Analyzer identity command \"$command\" returned readable text, but the protocol family is still unknown.",
                        rawIdentityText = rawIdentityText,
                        protocolIdentity = protocolIdentity,
                        commandDebugSummary = roundTripResult.debugSummary
                    )
                } else {
                    UsbAnalyzerIdentityResult(
                        success = true,
                        summary = "Analyzer identity handshake succeeded using \"$command\". Detected family: ${protocolIdentity.displayName}.",
                        rawIdentityText = rawIdentityText,
                        protocolIdentity = protocolIdentity,
                        commandDebugSummary = roundTripResult.debugSummary
                    )
                }

            if (currentResult.success) {
                UsbSessionManager.registerAnalyzerIdentityResult(currentResult)
                return currentResult
            }

            lastFailure = currentResult
        }

        val fallback = lastFailure ?: UsbAnalyzerIdentityResult(
            success = false,
            summary = "Analyzer identity handshake failed for an unknown reason.",
            rawIdentityText = null,
            protocolIdentity = UsbVnaProtocolResolver.resolveFromIdentityText(
                rawIdentityText = null
            ),
            commandDebugSummary = "Identity command list completed without a concrete result."
        )

        UsbSessionManager.registerAnalyzerIdentityResult(fallback)
        return fallback
    }

    private fun executeLiteVnaBinaryCommand(
        commandBytes: ByteArray,
        expectedMinimumResponseBytes: Int,
        wallClockTimeoutMs: Long,
        readTimeoutMs: Int,
        maxReadPasses: Int,
        postWriteDelayMs: Int
    ): UsbVnaCommandResult {
        val channel = sessionManager.getActiveTransportChannel()
            ?: return buildFailureResult(
                summary = "No active CDC transport channel exists for LiteVNA binary command.",
                debugSummary = "executeLiteVnaBinaryCommand found no active channel.",
                errorReason = "No active CDC transport channel."
            )

        if (channel.transportKind != UsbTransportKind.CDC_DATA) {
            return buildFailureResult(
                summary = "LiteVNA binary command requested on non-CDC transport.",
                debugSummary = "Active transport kind was ${channel.transportKind}.",
                errorReason = "Non-CDC transport."
            )
        }

        val cdcChannel = UsbCdcSerialChannel(channel)
        val startTimeMs = System.currentTimeMillis()

        val bytesWritten = runCatching {
            cdcChannel.writeBytes(
                payload = commandBytes,
                timeoutMs = DEFAULT_WRITE_TIMEOUT_MS
            )
        }.getOrElse { error ->
            return buildFailureResult(
                summary = "LiteVNA binary CDC write threw an exception: ${error.message ?: "unknown error"}.",
                debugSummary = "binaryWriteException=${error.message ?: "unknown error"} commandPreview=${rawBytePreview(commandBytes)}",
                errorReason = error.message ?: "LiteVNA binary CDC write exception."
            )
        }

        if (bytesWritten <= 0) {
            return buildFailureResult(
                summary = "LiteVNA binary CDC write failed. bulkTransfer returned $bytesWritten.",
                bytesTransferred = bytesWritten.coerceAtLeast(0),
                debugSummary = "bytesWritten=$bytesWritten commandPreview=${rawBytePreview(commandBytes)}",
                errorReason = "LiteVNA binary CDC write failed."
            )
        }

        applyDelay(postWriteDelayMs)

        val responseBytes = runCatching {
            cdcChannel.readRawBytes(
                timeoutMs = readTimeoutMs,
                maxReadPasses = maxReadPasses,
                interReadDelayMs = DEFAULT_INTER_READ_DELAY_MS
            )
        }.getOrElse { error ->
            return buildFailureResult(
                summary = "LiteVNA binary CDC read threw an exception: ${error.message ?: "unknown error"}.",
                bytesTransferred = bytesWritten,
                debugSummary = "binaryReadException=${error.message ?: "unknown error"} commandPreview=${rawBytePreview(commandBytes)}",
                errorReason = error.message ?: "LiteVNA binary CDC read exception."
            )
        }

        val elapsedMs = System.currentTimeMillis() - startTimeMs

        if (elapsedMs > wallClockTimeoutMs) {
            return buildFailureResult(
                summary = "LiteVNA binary command exceeded wall-clock timeout of ${wallClockTimeoutMs}ms.",
                bytesTransferred = bytesWritten,
                rawResponseBytes = responseBytes,
                debugSummary = "elapsedMs=$elapsedMs commandPreview=${rawBytePreview(commandBytes)}",
                lastReadSizeBytes = responseBytes.size,
                errorReason = "Wall-clock timeout during LiteVNA binary command."
            )
        }

        if (responseBytes.size < expectedMinimumResponseBytes) {
            return buildFailureResult(
                summary = "LiteVNA binary CDC read did not return enough bytes. Expected at least $expectedMinimumResponseBytes, got ${responseBytes.size}.",
                responseText = rawBytePreview(responseBytes),
                rawResponseBytes = responseBytes,
                bytesTransferred = responseBytes.size,
                readPassCount = 1,
                debugSummary = "commandPreview=${rawBytePreview(commandBytes)} rawPreview=${rawBytePreview(responseBytes)}",
                lastReadSizeBytes = responseBytes.size,
                errorReason = "Insufficient LiteVNA binary response bytes."
            )
        }

        return buildSuccessResult(
            summary = "LiteVNA binary CDC command succeeded.",
            responseText = rawBytePreview(responseBytes),
            rawResponseBytes = responseBytes,
            bytesTransferred = responseBytes.size,
            readPassCount = 1,
            debugSummary = "commandPreview=${rawBytePreview(commandBytes)} rawPreview=${rawBytePreview(responseBytes)} elapsedMs=$elapsedMs",
            lastReadSizeBytes = responseBytes.size
        )
    }

    /*
    ####################################################################
    COUNT-DRIVEN FIFO READ COMMAND
    --------------------------------------------------------------------
    PURPOSE
    Writes a LiteVNA command (a readFIFO request) and drains up to
    `expectedBytes` from the values-FIFO using the count-driven serial
    read. Unlike executeLiteVnaBinaryCommand (fixed passes, first-idle
    bail), this keeps reading until the expected byte count arrives, K
    consecutive idle reads occur, or the wall-clock budget expires. A
    partial payload is NOT a failure — the accumulation loop judges
    completeness by record count, so we succeed as long as some bytes
    came back.
    ####################################################################
    */
    private fun executeLiteVnaFifoRead(
        commandBytes: ByteArray,
        expectedBytes: Int,
        maxReadPasses: Int,
        readTimeoutMs: Int,
        maxConsecutiveIdleReads: Int,
        wallClockBudgetMs: Long,
        postWriteDelayMs: Int
    ): UsbVnaCommandResult {
        val channel = sessionManager.getActiveTransportChannel()
            ?: return buildFailureResult(
                summary = "No active CDC transport channel exists for LiteVNA FIFO read.",
                debugSummary = "executeLiteVnaFifoRead found no active channel.",
                errorReason = "No active CDC transport channel."
            )

        if (channel.transportKind != UsbTransportKind.CDC_DATA) {
            return buildFailureResult(
                summary = "LiteVNA FIFO read requested on non-CDC transport.",
                debugSummary = "Active transport kind was ${channel.transportKind}.",
                errorReason = "Non-CDC transport."
            )
        }

        val cdcChannel = UsbCdcSerialChannel(channel)

        val bytesWritten = runCatching {
            cdcChannel.writeBytes(
                payload = commandBytes,
                timeoutMs = DEFAULT_WRITE_TIMEOUT_MS
            )
        }.getOrElse { error ->
            return buildFailureResult(
                summary = "LiteVNA FIFO CDC write threw an exception: ${error.message ?: "unknown error"}.",
                debugSummary = "fifoWriteException=${error.message ?: "unknown error"} commandPreview=${rawBytePreview(commandBytes)}",
                errorReason = error.message ?: "LiteVNA FIFO CDC write exception."
            )
        }

        if (bytesWritten <= 0) {
            return buildFailureResult(
                summary = "LiteVNA FIFO CDC write failed. bulkTransfer returned $bytesWritten.",
                bytesTransferred = bytesWritten.coerceAtLeast(0),
                debugSummary = "bytesWritten=$bytesWritten commandPreview=${rawBytePreview(commandBytes)}",
                errorReason = "LiteVNA FIFO CDC write failed."
            )
        }

        applyDelay(postWriteDelayMs)

        val responseBytes = runCatching {
            cdcChannel.readRawBytesUntil(
                expectedBytes = expectedBytes,
                maxReadPasses = maxReadPasses,
                readTimeoutMs = readTimeoutMs,
                interReadDelayMs = DEFAULT_INTER_READ_DELAY_MS,
                maxConsecutiveIdleReads = maxConsecutiveIdleReads,
                wallClockBudgetMs = wallClockBudgetMs
            )
        }.getOrElse { error ->
            return buildFailureResult(
                summary = "LiteVNA FIFO CDC read threw an exception: ${error.message ?: "unknown error"}.",
                bytesTransferred = bytesWritten,
                debugSummary = "fifoReadException=${error.message ?: "unknown error"} commandPreview=${rawBytePreview(commandBytes)}",
                errorReason = error.message ?: "LiteVNA FIFO CDC read exception."
            )
        }

        if (responseBytes.isEmpty()) {
            return buildFailureResult(
                summary = "LiteVNA FIFO CDC read returned no bytes.",
                responseText = null,
                rawResponseBytes = byteArrayOf(),
                bytesTransferred = 0,
                readPassCount = 1,
                debugSummary = "commandPreview=${rawBytePreview(commandBytes)} expectedBytes=$expectedBytes",
                lastReadSizeBytes = 0,
                errorReason = "Empty LiteVNA FIFO response."
            )
        }

        return buildSuccessResult(
            summary = "LiteVNA FIFO CDC read returned ${responseBytes.size} byte(s).",
            responseText = rawBytePreview(responseBytes),
            rawResponseBytes = responseBytes,
            bytesTransferred = responseBytes.size,
            readPassCount = 1,
            debugSummary = "commandPreview=${rawBytePreview(commandBytes)} expectedBytes=$expectedBytes gotBytes=${responseBytes.size}",
            lastReadSizeBytes = responseBytes.size
        )
    }

    private fun calculateSweepSettleDelayMs(
        pointCount: Int
    ): Int {
        return (260 + (pointCount * 18))
            .coerceIn(260, 3200)
    }

    private fun encodeLittleEndian8(
        value: Long
    ): ByteArray {
        return ByteArray(8) { index ->
            ((value shr (index * 8)) and 0xFF).toByte()
        }
    }

    private fun encodeLittleEndian2(
        value: Int
    ): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte()
        )
    }

    private fun applyDelay(delayMs: Int) {
        if (delayMs <= 0) return

        runCatching {
            Thread.sleep(delayMs.toLong())
        }
    }

    private fun decodeResponseText(
        bytes: ByteArray
    ): String? {
        val decodedText = bytes
            .toString(UTF_8)
            .trim('\u0000', '\r', '\n', ' ')

        return decodedText.ifBlank { null }
    }

    private fun stripExpectedEcho(
        responseText: String?,
        expectedCommandEcho: String?
    ): String? {
        if (responseText.isNullOrBlank()) {
            return null
        }

        val normalizedText = responseText
            .replace('\u0000', '\n')
            .replace("\r\n", "\n")
            .replace('\r', '\n')

        val lines = normalizedText
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            return null
        }

        if (expectedCommandEcho.isNullOrBlank()) {
            return lines.joinToString(separator = "\n").trim().ifBlank { null }
        }

        val normalizedEcho = expectedCommandEcho.trim()

        return lines
            .filterNot { line ->
                line.equals(normalizedEcho, ignoreCase = true) ||
                        line.equals("$normalizedEcho>", ignoreCase = true) ||
                        line.equals(">$normalizedEcho", ignoreCase = true) ||
                        line.equals("$normalizedEcho:", ignoreCase = true) ||
                        line.equals(":$normalizedEcho", ignoreCase = true)
            }
            .joinToString(separator = "\n")
            .trim()
            .ifBlank { null }
    }

    private fun sanitizeIdentityText(
        rawIdentityText: String?
    ): String? {
        if (rawIdentityText.isNullOrBlank()) {
            return null
        }

        return rawIdentityText
            .replace('\u0000', ' ')
            .replace("\r", "\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(separator = " | ")
            .trim()
            .ifBlank { null }
    }

    private fun buildPreviewText(
        value: String?
    ): String {
        if (value.isNullOrBlank()) {
            return "<empty>"
        }

        return value
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .take(RAW_PREVIEW_LIMIT)
    }

    private fun rawBytePreview(
        bytes: ByteArray
    ): String {
        if (bytes.isEmpty()) {
            return "<no-bytes>"
        }

        return bytes
            .take(RAW_PREVIEW_LIMIT)
            .joinToString(separator = " ") { byte ->
                byte.toUByte().toString(16).padStart(2, '0')
            }
    }

    private fun buildPayloadBytes(
        commandText: String,
        appendNewline: Boolean
    ): ByteArray {
        val payloadText = if (appendNewline) {
            commandText.trimEnd() + "\n"
        } else {
            commandText
        }

        return payloadText.toByteArray(UTF_8)
    }

    private fun sendBulkCommand(
        channel: UsbTransportChannel,
        payloadBytes: ByteArray,
        commandText: String,
        timeoutMs: Int
    ): UsbVnaCommandResult {
        val bytesWritten = runCatching {
            channel.connection.bulkTransfer(
                channel.bulkOutEndpoint,
                payloadBytes,
                payloadBytes.size,
                timeoutMs
            )
        }.getOrElse { error ->
            return buildFailureResult(
                summary = "USB VNA bulk write threw an exception: ${error.message ?: "unknown error"}.",
                debugSummary = "Command=\"$commandText\" timeoutMs=$timeoutMs payloadBytes=${payloadBytes.size} writeException=${error.message ?: "unknown error"}",
                errorReason = error.message ?: "Write exception."
            )
        }

        if (bytesWritten <= 0) {
            return buildFailureResult(
                summary = "USB VNA bulk write failed. bulkTransfer returned $bytesWritten.",
                bytesTransferred = bytesWritten.coerceAtLeast(0),
                debugSummary = "Command=\"$commandText\" timeoutMs=$timeoutMs payloadBytes=${payloadBytes.size} bytesWritten=$bytesWritten",
                errorReason = "Bulk write failed with result $bytesWritten."
            )
        }

        if (bytesWritten < payloadBytes.size) {
            return buildFailureResult(
                summary = "USB VNA bulk write was partial. Wrote $bytesWritten of ${payloadBytes.size} bytes.",
                bytesTransferred = bytesWritten,
                debugSummary = "Command=\"$commandText\" timeoutMs=$timeoutMs payloadBytes=${payloadBytes.size} bytesWritten=$bytesWritten",
                errorReason = "Partial bulk write."
            )
        }

        return buildSuccessResult(
            summary = "USB VNA bulk command write succeeded. Wrote $bytesWritten bytes.",
            bytesTransferred = bytesWritten,
            debugSummary = "Command=\"$commandText\" timeoutMs=$timeoutMs payloadBytes=${payloadBytes.size} bytesWritten=$bytesWritten transport=BULK_GENERIC"
        )
    }

    private fun sendCdcCommand(
        channel: UsbTransportChannel,
        payloadBytes: ByteArray,
        commandText: String,
        timeoutMs: Int
    ): UsbVnaCommandResult {
        val cdcChannel = UsbCdcSerialChannel(channel)

        val bytesWritten = runCatching {
            cdcChannel.writeBytes(payloadBytes, timeoutMs)
        }.getOrElse { error ->
            return buildFailureResult(
                summary = "USB CDC write threw an exception: ${error.message ?: "unknown error"}.",
                debugSummary = "Command=\"$commandText\" timeoutMs=$timeoutMs payloadBytes=${payloadBytes.size} cdcWriteException=${error.message ?: "unknown error"}",
                errorReason = error.message ?: "CDC write exception."
            )
        }

        if (bytesWritten <= 0) {
            return buildFailureResult(
                summary = "USB CDC write failed. bulkTransfer returned $bytesWritten.",
                bytesTransferred = bytesWritten.coerceAtLeast(0),
                debugSummary = "Command=\"$commandText\" timeoutMs=$timeoutMs payloadBytes=${payloadBytes.size} bytesWritten=$bytesWritten transport=CDC_DATA",
                errorReason = "CDC write failed with result $bytesWritten."
            )
        }

        return buildSuccessResult(
            summary = "USB CDC command write succeeded. Wrote $bytesWritten bytes.",
            bytesTransferred = bytesWritten,
            debugSummary = "Command=\"$commandText\" timeoutMs=$timeoutMs payloadBytes=${payloadBytes.size} bytesWritten=$bytesWritten transport=CDC_DATA"
        )
    }

    private fun readBulkResponse(
        channel: UsbTransportChannel,
        timeoutMs: Int,
        expectedCommandEcho: String?,
        maxReadPassesOverride: Int,
        interReadDelayMs: Int
    ): UsbVnaCommandResult {
        val packetSize = channel.maxReadPacketSize.coerceAtLeast(MINIMUM_READ_BUFFER_SIZE)
        val collectedBytes = mutableListOf<Byte>()
        var readPassCount = 0
        var lastReadSizeBytes = 0

        while (readPassCount < maxReadPassesOverride && collectedBytes.size < MAX_RESPONSE_BYTES) {
            readPassCount += 1
            val buffer = ByteArray(packetSize)

            val bytesRead = runCatching {
                channel.connection.bulkTransfer(
                    channel.bulkInEndpoint,
                    buffer,
                    buffer.size,
                    timeoutMs
                )
            }.getOrElse { error ->
                return buildFailureResult(
                    summary = "USB VNA bulk read threw an exception on pass $readPassCount: ${error.message ?: "unknown error"}.",
                    bytesTransferred = collectedBytes.size,
                    readPassCount = readPassCount - 1,
                    debugSummary = "Read timeoutMs=$timeoutMs packetSize=$packetSize pass=$readPassCount collectedBytes=${collectedBytes.size} readException=${error.message ?: "unknown error"}",
                    lastReadSizeBytes = lastReadSizeBytes,
                    errorReason = error.message ?: "Read exception."
                )
            }

            if (bytesRead <= 0) {
                if (collectedBytes.isNotEmpty()) {
                    break
                }
                applyDelay(interReadDelayMs)
                continue
            }

            val safeBytesRead = bytesRead.coerceIn(0, buffer.size)
            lastReadSizeBytes = safeBytesRead

            for (index in 0 until safeBytesRead) {
                collectedBytes.add(buffer[index])
            }

            val cleanedResponseText = stripExpectedEcho(
                responseText = decodeResponseText(collectedBytes.toByteArray()),
                expectedCommandEcho = expectedCommandEcho
            )

            if (!cleanedResponseText.isNullOrBlank()) {
                return buildSuccessResult(
                    summary = "USB VNA bulk response read succeeded. Collected ${collectedBytes.size} byte(s).",
                    responseText = cleanedResponseText,
                    rawResponseBytes = collectedBytes.toByteArray(),
                    bytesTransferred = collectedBytes.size,
                    readPassCount = readPassCount,
                    debugSummary = "transport=BULK_GENERIC collectedBytes=${collectedBytes.size} preview=${buildPreviewText(cleanedResponseText)}",
                    lastReadSizeBytes = lastReadSizeBytes
                )
            }

            applyDelay(interReadDelayMs)
        }

        return buildFailureResult(
            summary = "USB VNA bulk read did not produce usable response text.",
            rawResponseBytes = collectedBytes.toByteArray(),
            bytesTransferred = collectedBytes.size,
            readPassCount = readPassCount,
            debugSummary = "transport=BULK_GENERIC rawPreview=${rawBytePreview(collectedBytes.toByteArray())}",
            lastReadSizeBytes = lastReadSizeBytes,
            errorReason = "No usable readable response text."
        )
    }

    private fun readCdcResponse(
        channel: UsbTransportChannel,
        timeoutMs: Int,
        expectedCommandEcho: String?,
        maxReadPassesOverride: Int,
        interReadDelayMs: Int
    ): UsbVnaCommandResult {
        val cdcChannel = UsbCdcSerialChannel(channel)

        val responseBytes = runCatching {
            cdcChannel.readTextBytes(
                timeoutMs = timeoutMs,
                maxReadPasses = maxReadPassesOverride,
                interReadDelayMs = interReadDelayMs
            )
        }.getOrElse { error ->
            return buildFailureResult(
                summary = "USB CDC read threw an exception: ${error.message ?: "unknown error"}.",
                debugSummary = "transport=CDC_DATA timeoutMs=$timeoutMs readException=${error.message ?: "unknown error"}",
                errorReason = error.message ?: "CDC read exception."
            )
        }

        val cleanedResponseText = stripExpectedEcho(
            responseText = decodeResponseText(responseBytes),
            expectedCommandEcho = expectedCommandEcho
        )

        if (cleanedResponseText.isNullOrBlank()) {
            return buildFailureResult(
                summary = "USB CDC read did not produce usable response text.",
                rawResponseBytes = responseBytes,
                bytesTransferred = responseBytes.size,
                readPassCount = 0,
                debugSummary = "transport=CDC_DATA rawPreview=${rawBytePreview(responseBytes)}",
                lastReadSizeBytes = responseBytes.size,
                errorReason = "No usable CDC response text."
            )
        }

        return buildSuccessResult(
            summary = "USB CDC response read succeeded. Collected ${responseBytes.size} byte(s).",
            responseText = cleanedResponseText,
            rawResponseBytes = responseBytes,
            bytesTransferred = responseBytes.size,
            readPassCount = 1,
            debugSummary = "transport=CDC_DATA preview=${buildPreviewText(cleanedResponseText)}",
            lastReadSizeBytes = responseBytes.size
        )
    }

    private fun buildSuccessResult(
        summary: String,
        responseText: String? = null,
        rawResponseBytes: ByteArray = byteArrayOf(),
        bytesTransferred: Int = 0,
        readPassCount: Int = 0,
        debugSummary: String = "",
        lastReadSizeBytes: Int = 0
    ): UsbVnaCommandResult {
        latestTransportHealthSnapshot = UsbVnaTransportHealthSnapshot(
            lastCommandSucceeded = true,
            lastReadSizeBytes = lastReadSizeBytes,
            lastErrorReason = null,
            lastOperationSummary = summary
        )

        UsbSessionManager.registerTransportHealthSnapshot(
            transportHealthSnapshot = latestTransportHealthSnapshot
        )

        return UsbVnaCommandResult(
            success = true,
            summary = summary,
            responseText = responseText,
            rawResponseBytes = rawResponseBytes,
            bytesTransferred = bytesTransferred,
            readPassCount = readPassCount,
            debugSummary = debugSummary,
            lastReadSizeBytes = lastReadSizeBytes,
            lastErrorReason = null
        )
    }

    private fun buildFailureResult(
        summary: String,
        responseText: String? = null,
        rawResponseBytes: ByteArray = byteArrayOf(),
        bytesTransferred: Int = 0,
        readPassCount: Int = 0,
        debugSummary: String = "",
        lastReadSizeBytes: Int = 0,
        errorReason: String? = null
    ): UsbVnaCommandResult {
        latestTransportHealthSnapshot = UsbVnaTransportHealthSnapshot(
            lastCommandSucceeded = false,
            lastReadSizeBytes = lastReadSizeBytes,
            lastErrorReason = errorReason,
            lastOperationSummary = summary
        )

        UsbSessionManager.registerTransportHealthSnapshot(
            transportHealthSnapshot = latestTransportHealthSnapshot
        )

        return UsbVnaCommandResult(
            success = false,
            summary = summary,
            responseText = responseText,
            rawResponseBytes = rawResponseBytes,
            bytesTransferred = bytesTransferred,
            readPassCount = readPassCount,
            debugSummary = debugSummary,
            lastReadSizeBytes = lastReadSizeBytes,
            lastErrorReason = errorReason
        )
    }
}

interface UsbSessionManagerFacade {
    fun hasOpenSession(): Boolean
    fun getActiveTransportChannel(): UsbTransportChannel?
}

class DefaultUsbSessionManagerFacade : UsbSessionManagerFacade {
    override fun hasOpenSession(): Boolean {
        return UsbSessionManager.hasOpenSession()
    }

    override fun getActiveTransportChannel(): UsbTransportChannel? {
        return UsbSessionManager.getActiveTransportChannel()
    }
}