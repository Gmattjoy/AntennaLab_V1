package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import kotlin.math.hypot
import kotlin.math.log10

/*
########################################################################
FILE: LiteVnaSweepProtocol.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Sweep Logic

LAST UPDATED 05/04/2026 14:50

CURRENT DEVELOPMENT ROLE
This version now supports:
- binary CDC identity query
- real 0xF0 validation via READ1
- configurable LiteVNA sweep reads through the command channel
- real valuesFIFO parsing using documented 32-byte records
- rawResponseBytes handoff so parser layers use the real accumulated
  byte payload instead of a shortened preview string

IMPORTANT CHANGE
Configured sweep reads now retry and keep the best decodable sweep seen
during the retry cycle.

NEW PARSE HARDENING
Some returned configured sweep records appear to carry implausible
freqIndex values even though the FIFO read itself succeeds and the point
payload looks usable.

This version now:
- first tries the decoded freqIndex path
- if too few usable records survive that filter, it falls back to
  sequential record ordering
- records which path was used in diagnostics
########################################################################
*/

data class LiteVnaBringUpResult(
    val success: Boolean,
    val stage: String,
    val summary: String,
    val rawIdentityText: String = ""
)

data class LiteVnaCommand(
    val commandBytes: ByteArray,
    val description: String
)

private data class LiteVnaFifoRecord(
    val fwd0Re: Int,
    val fwd0Im: Int,
    val rev0Re: Int,
    val rev0Im: Int,
    val rev1Re: Int,
    val rev1Im: Int,
    val freqIndex: Int
)

private data class ComplexValue(
    val re: Double,
    val im: Double
)

private data class ParsedSweepPointResult(
    val point: SweepPoint?,
    val rejectionReason: String? = null
)

class LiteVnaSweepProtocol(
    private val commandChannel: UsbVnaCommandChannel = UsbVnaCommandChannel()
) {

    companion object {
        private const val LITEVNA_FIFO_RECORD_SIZE_BYTES = 32
        private const val DEFAULT_PROBE_START_HZ = 14_000_000L
        private const val DEFAULT_PROBE_STEP_HZ = 100_000L
        private const val DEFAULT_PROBE_POINT_COUNT = 8
        private const val REFERENCE_IMPEDANCE_OHMS = 50.0
        private const val EPSILON = 1e-12
        private const val MAX_USABLE_GAMMA_MAGNITUDE = 0.999999
        private const val MAX_DISPLAY_SWR = 99.0
        private const val MINIMUM_USABLE_SWEEP_POINTS = 8
        private const val MAX_CONFIGURED_SWEEP_RETRIES = 3
    }

    private var lastConfiguredSweepDiagnostics: String =
        "No configured sweep diagnostics captured yet."

    fun getLastConfiguredSweepDiagnostics(): String {
        return lastConfiguredSweepDiagnostics
    }

    fun checkBringUpReadiness(): LiteVnaBringUpResult {
        if (!UsbSessionManager.hasOpenSession()) {
            return LiteVnaBringUpResult(
                success = false,
                stage = "SESSION",
                summary = "LiteVNA bring-up cannot continue because no USB session is currently open."
            )
        }

        if (!UsbSessionManager.isTransportReady()) {
            return LiteVnaBringUpResult(
                success = false,
                stage = "TRANSPORT",
                summary = "LiteVNA bring-up cannot continue because the USB transport is not ready."
            )
        }

        if (UsbSessionManager.getActiveTransportChannel() == null) {
            return LiteVnaBringUpResult(
                success = false,
                stage = "TRANSPORT_CHANNEL",
                summary = "LiteVNA bring-up cannot continue because no active USB transport channel is available."
            )
        }

        return LiteVnaBringUpResult(
            success = true,
            stage = "READY",
            summary = "USB session and transport foundation are ready for LiteVNA bring-up."
        )
    }

    fun probeIdentity(): LiteVnaBringUpResult {
        val readiness = checkBringUpReadiness()
        if (!readiness.success) {
            return readiness
        }

        val identityResult = commandChannel.queryAnalyzerIdentity()

        if (!identityResult.success) {
            return LiteVnaBringUpResult(
                success = false,
                stage = "IDENTITY_QUERY",
                summary = identityResult.summary,
                rawIdentityText = identityResult.rawIdentityText.orEmpty()
            )
        }

        val protocolName = identityResult.protocolIdentity.displayName
        val rawText = identityResult.rawIdentityText.orEmpty()

        return if (
            protocolName.contains("lite", ignoreCase = true) ||
            rawText.contains("litevna", ignoreCase = true)
        ) {
            LiteVnaBringUpResult(
                success = true,
                stage = "IDENTITY_CONFIRMED",
                summary = "LiteVNA identity confirmed through active transport.",
                rawIdentityText = rawText
            )
        } else {
            LiteVnaBringUpResult(
                success = false,
                stage = "IDENTITY_MISMATCH",
                summary = "Identity response did not resolve as LiteVNA.",
                rawIdentityText = rawText
            )
        }
    }

    fun runBasicCommandTest(): LiteVnaBringUpResult {
        val identityProbe = probeIdentity()
        if (!identityProbe.success) {
            return identityProbe
        }

        if (!identityProbe.rawIdentityText.contains("deviceVariant=0x02", ignoreCase = true)) {
            return LiteVnaBringUpResult(
                success = false,
                stage = "REGISTER_UNEXPECTED_VALUE",
                summary = "LiteVNA identity completed, but 0xF0 was not confirmed as 0x02.",
                rawIdentityText = identityProbe.rawIdentityText
            )
        }

        val miniSweepProbe = commandChannel.runLiteVnaMiniSweepProbe()

        if (!miniSweepProbe.success) {
            return LiteVnaBringUpResult(
                success = false,
                stage = "SWEEP_PROBE_FAILED",
                summary = miniSweepProbe.summary,
                rawIdentityText = identityProbe.rawIdentityText
            )
        }

        val decodePreview = buildMiniSweepDecodePreviewFromBytes(
            rawBytes = miniSweepProbe.rawResponseBytes
        )

        val parsedSweep = buildParsedSweepResultFromBytes(
            rawBytes = miniSweepProbe.rawResponseBytes,
            startFrequencyHz = DEFAULT_PROBE_START_HZ,
            stepFrequencyHz = DEFAULT_PROBE_STEP_HZ,
            requestedPointCount = DEFAULT_PROBE_POINT_COUNT
        )

        val summaryText =
            if (parsedSweep == null) {
                "Mini sweep probe succeeded, but LiteVNA sweep parsing returned no usable points. $decodePreview"
            } else {
                buildString {
                    append("Mini sweep probe succeeded. ")
                    append("Decoded ")
                    append(parsedSweep.points.size)
                    append(" real point(s). ")
                    append(buildPointPreviewSummary(parsedSweep))
                    append(" ")
                    append(decodePreview)
                }
            }

        return LiteVnaBringUpResult(
            success = true,
            stage = "SWEEP_PROBE_OK",
            summary = summaryText,
            rawIdentityText = identityProbe.rawIdentityText
        )
    }

    private fun buildMiniSweepDecodePreviewFromBytes(
        rawBytes: ByteArray
    ): String {
        if (rawBytes.isEmpty()) {
            return "Mini sweep probe succeeded but returned no decodable FIFO bytes."
        }

        val completeRecordCount =
            rawBytes.size / LITEVNA_FIFO_RECORD_SIZE_BYTES

        val trailingBytes =
            rawBytes.size % LITEVNA_FIFO_RECORD_SIZE_BYTES

        val firstRecordBytes =
            rawBytes.take(minOf(LITEVNA_FIFO_RECORD_SIZE_BYTES, rawBytes.size)).toByteArray()

        return buildString {
            append("FIFO bytes=")
            append(rawBytes.size)
            append(" recordSize=")
            append(LITEVNA_FIFO_RECORD_SIZE_BYTES)
            append(" completeRecords=")
            append(completeRecordCount)
            append(" trailingBytes=")
            append(trailingBytes)
            append(" firstRecord=")
            append(formatBytePreview(firstRecordBytes))
        }
    }

    private fun buildConfiguredSweepDiagnostics(
        rawBytes: ByteArray,
        requestedPointCount: Int,
        directDecodedRecordCount: Int,
        directFilteredRecordCount: Int,
        parsePathLabel: String,
        validPoints: List<SweepPoint>,
        rejectedReasons: Map<String, Int>,
        freqIndexPreview: String
    ): String {
        if (rawBytes.isEmpty()) {
            return "Configured sweep diagnostics: no raw bytes returned."
        }

        val completeRecordCount = rawBytes.size / LITEVNA_FIFO_RECORD_SIZE_BYTES
        val trailingBytes = rawBytes.size % LITEVNA_FIFO_RECORD_SIZE_BYTES

        return buildString {
            appendLine("LiteVNA configured sweep diagnostics:")
            appendLine("rawBytes=${rawBytes.size}")
            appendLine("recordSize=$LITEVNA_FIFO_RECORD_SIZE_BYTES")
            appendLine("completeRecords=$completeRecordCount")
            appendLine("trailingBytes=$trailingBytes")
            appendLine("requestedPointCount=$requestedPointCount")
            appendLine("decodedRecords=$directDecodedRecordCount")
            appendLine("filteredRecords=$directFilteredRecordCount")
            appendLine("parsePath=$parsePathLabel")
            appendLine("validPoints=${validPoints.size}")
            appendLine("freqIndexPreview=$freqIndexPreview")
            if (rejectedReasons.isNotEmpty()) {
                appendLine("rejectedReasons=$rejectedReasons")
            }
            validPoints.firstOrNull()?.let { appendLine("firstPoint=${formatPointPreview(it)}") }
            validPoints.lastOrNull()?.let { appendLine("lastPoint=${formatPointPreview(it)}") }
        }.trim()
    }

    fun buildSweepResult(
        startMHz: Double,
        endMHz: Double,
        stepMHz: Double
    ): SweepResult? {
        val startFrequencyHz = (startMHz * 1_000_000.0).toLong()
        val stepFrequencyHz = (stepMHz * 1_000_000.0).toLong()

        if (stepFrequencyHz <= 0L) {
            lastConfiguredSweepDiagnostics =
                "Configured sweep diagnostics: invalid stepFrequencyHz=$stepFrequencyHz"
            return null
        }

        val pointCount =
            (((endMHz - startMHz) / stepMHz).toInt() + 1)
                .coerceAtLeast(1)
                .coerceAtMost(255)

        var bestParsedSweepResult: SweepResult? = null
        var bestCompleteRecordCount = 0
        var bestDiagnostics = "Configured sweep diagnostics: no attempts completed."

        repeat(MAX_CONFIGURED_SWEEP_RETRIES) { attemptIndex ->
            val sweepRead = commandChannel.runLiteVnaConfiguredSweepRead(
                request = LiteVnaSweepRequest(
                    startFrequencyHz = startFrequencyHz,
                    stepFrequencyHz = stepFrequencyHz,
                    pointCount = pointCount,
                    valuesPerFrequency = 1
                )
            )

            if (!sweepRead.success) {
                bestDiagnostics =
                    "Configured sweep attempt ${attemptIndex + 1} failed before decoding. ${sweepRead.summary}"
                return@repeat
            }

            val parseOutcome = buildParsedSweepOutcomeFromBytes(
                rawBytes = sweepRead.rawResponseBytes,
                startFrequencyHz = startFrequencyHz,
                stepFrequencyHz = stepFrequencyHz,
                requestedPointCount = pointCount
            )

            val attemptDiagnostics = buildConfiguredSweepDiagnostics(
                rawBytes = sweepRead.rawResponseBytes,
                requestedPointCount = pointCount,
                directDecodedRecordCount = parseOutcome.decodedRecordCount,
                directFilteredRecordCount = parseOutcome.directFilteredRecordCount,
                parsePathLabel = parseOutcome.parsePathLabel,
                validPoints = parseOutcome.validPoints,
                rejectedReasons = parseOutcome.rejectedReasons,
                freqIndexPreview = parseOutcome.freqIndexPreview
            )

            val parsedSweepResult = parseOutcome.sweepResult

            if (parsedSweepResult == null) {
                bestDiagnostics =
                    "Configured sweep attempt ${attemptIndex + 1} produced no usable SweepResult.\n$attemptDiagnostics"
                return@repeat
            }

            val parsedPointCount = parsedSweepResult.points.size

            if (parsedPointCount > bestCompleteRecordCount) {
                bestCompleteRecordCount = parsedPointCount
                bestParsedSweepResult = parsedSweepResult
                bestDiagnostics =
                    "Configured sweep attempt ${attemptIndex + 1} was the best decodable result.\n$attemptDiagnostics"
            }

            if (parsedPointCount >= pointCount) {
                lastConfiguredSweepDiagnostics =
                    "Configured sweep completed at requested point count.\n$attemptDiagnostics"
                return parsedSweepResult
            }

            if (parsedPointCount >= MINIMUM_USABLE_SWEEP_POINTS) {
                lastConfiguredSweepDiagnostics =
                    "Configured sweep returned minimum usable point count.\n$attemptDiagnostics"
                return parsedSweepResult
            }
        }

        lastConfiguredSweepDiagnostics = bestDiagnostics
        return bestParsedSweepResult?.takeIf { it.points.size >= MINIMUM_USABLE_SWEEP_POINTS }
    }

    fun buildProvisionalSweepResult(): SweepResult? {
        val miniSweepProbe = commandChannel.runLiteVnaMiniSweepProbe()
        if (!miniSweepProbe.success) {
            return null
        }

        return buildParsedSweepResultFromBytes(
            rawBytes = miniSweepProbe.rawResponseBytes,
            startFrequencyHz = DEFAULT_PROBE_START_HZ,
            stepFrequencyHz = DEFAULT_PROBE_STEP_HZ,
            requestedPointCount = DEFAULT_PROBE_POINT_COUNT
        )
    }

    private data class ParsedSweepOutcome(
        val sweepResult: SweepResult?,
        val parsePathLabel: String,
        val decodedRecordCount: Int,
        val directFilteredRecordCount: Int,
        val validPoints: List<SweepPoint>,
        val rejectedReasons: Map<String, Int>,
        val freqIndexPreview: String
    )

    private fun buildParsedSweepOutcomeFromBytes(
        rawBytes: ByteArray,
        startFrequencyHz: Long,
        stepFrequencyHz: Long,
        requestedPointCount: Int
    ): ParsedSweepOutcome {
        if (rawBytes.size < LITEVNA_FIFO_RECORD_SIZE_BYTES) {
            return ParsedSweepOutcome(
                sweepResult = null,
                parsePathLabel = "RAW_TOO_SHORT",
                decodedRecordCount = 0,
                directFilteredRecordCount = 0,
                validPoints = emptyList(),
                rejectedReasons = emptyMap(),
                freqIndexPreview = "[]"
            )
        }

        val records = parseFifoRecords(rawBytes)
        if (records.isEmpty()) {
            return ParsedSweepOutcome(
                sweepResult = null,
                parsePathLabel = "NO_DECODED_RECORDS",
                decodedRecordCount = 0,
                directFilteredRecordCount = 0,
                validPoints = emptyList(),
                rejectedReasons = emptyMap(),
                freqIndexPreview = "[]"
            )
        }

        val freqIndexPreview =
            records.take(8).joinToString(prefix = "[", postfix = "]") { it.freqIndex.toString() }

        val directRecords =
            records
                .filter { it.freqIndex in 0 until requestedPointCount }
                .distinctBy { it.freqIndex }
                .sortedBy { it.freqIndex }

        val sequentialRecords =
            records
                .take(requestedPointCount)
                .mapIndexed { index, record ->
                    record.copy(freqIndex = index)
                }

        val useSequentialFallback =
            directRecords.size < MINIMUM_USABLE_SWEEP_POINTS &&
                    sequentialRecords.size >= MINIMUM_USABLE_SWEEP_POINTS

        val selectedRecords =
            if (useSequentialFallback) sequentialRecords else directRecords

        if (selectedRecords.isEmpty()) {
            return ParsedSweepOutcome(
                sweepResult = null,
                parsePathLabel = "NO_RECORDS_AFTER_FILTER",
                decodedRecordCount = records.size,
                directFilteredRecordCount = directRecords.size,
                validPoints = emptyList(),
                rejectedReasons = emptyMap(),
                freqIndexPreview = freqIndexPreview
            )
        }

        val parsedResults =
            selectedRecords.map { record ->
                parseSweepPoint(
                    record = record,
                    startFrequencyHz = startFrequencyHz,
                    stepFrequencyHz = stepFrequencyHz
                )
            }

        val validPoints = parsedResults.mapNotNull { it.point }
        val rejectedReasons =
            parsedResults.mapNotNull { it.rejectionReason }
                .groupingBy { it }
                .eachCount()

        if (validPoints.isEmpty()) {
            return ParsedSweepOutcome(
                sweepResult = null,
                parsePathLabel = if (useSequentialFallback) "SEQUENTIAL_FALLBACK_ZERO_POINTS" else "DIRECT_INDEX_ZERO_POINTS",
                decodedRecordCount = records.size,
                directFilteredRecordCount = directRecords.size,
                validPoints = emptyList(),
                rejectedReasons = rejectedReasons,
                freqIndexPreview = freqIndexPreview
            )
        }

        val startFrequencyMHz = validPoints.first().frequencyMHz
        val endFrequencyMHz = validPoints.last().frequencyMHz
        val stepMHz = stepFrequencyHz / 1_000_000.0

        return ParsedSweepOutcome(
            sweepResult = SweepResult(
                // startFrequencyMHz/endFrequencyMHz/points already derive
                // from the ACTUAL decoded points. A short sweep is flagged
                // (isComplete = false) but its data is preserved, not rejected.
                startFrequencyMHz = startFrequencyMHz,
                endFrequencyMHz = endFrequencyMHz,
                stepMHz = stepMHz,
                points = validPoints,
                sweepPointCount = validPoints.size,
                requestedPointCount = requestedPointCount,
                actualPointCount = validPoints.size,
                isComplete = validPoints.size >= requestedPointCount
            ),
            parsePathLabel = if (useSequentialFallback) "SEQUENTIAL_FALLBACK" else "DIRECT_INDEX",
            decodedRecordCount = records.size,
            directFilteredRecordCount = directRecords.size,
            validPoints = validPoints,
            rejectedReasons = rejectedReasons,
            freqIndexPreview = freqIndexPreview
        )
    }

    private fun buildParsedSweepResultFromBytes(
        rawBytes: ByteArray,
        startFrequencyHz: Long,
        stepFrequencyHz: Long,
        requestedPointCount: Int
    ): SweepResult? {
        return buildParsedSweepOutcomeFromBytes(
            rawBytes = rawBytes,
            startFrequencyHz = startFrequencyHz,
            stepFrequencyHz = stepFrequencyHz,
            requestedPointCount = requestedPointCount
        ).sweepResult
    }

    private fun parseFifoRecords(
        rawBytes: ByteArray
    ): List<LiteVnaFifoRecord> {
        val completeRecordCount =
            rawBytes.size / LITEVNA_FIFO_RECORD_SIZE_BYTES

        if (completeRecordCount <= 0) {
            return emptyList()
        }

        return buildList {
            for (recordIndex in 0 until completeRecordCount) {
                val start = recordIndex * LITEVNA_FIFO_RECORD_SIZE_BYTES
                val record = rawBytes.copyOfRange(
                    fromIndex = start,
                    toIndex = start + LITEVNA_FIFO_RECORD_SIZE_BYTES
                )

                add(
                    LiteVnaFifoRecord(
                        fwd0Re = decodeLittleEndianInt32(record, 0x00),
                        fwd0Im = decodeLittleEndianInt32(record, 0x04),
                        rev0Re = decodeLittleEndianInt32(record, 0x08),
                        rev0Im = decodeLittleEndianInt32(record, 0x0C),
                        rev1Re = decodeLittleEndianInt32(record, 0x10),
                        rev1Im = decodeLittleEndianInt32(record, 0x14),
                        freqIndex = decodeLittleEndianUInt16(record, 0x18)
                    )
                )
            }
        }
    }

    private fun parseSweepPoint(
        record: LiteVnaFifoRecord,
        startFrequencyHz: Long,
        stepFrequencyHz: Long
    ): ParsedSweepPointResult {
        val reference = ComplexValue(
            re = record.fwd0Re.toDouble(),
            im = record.fwd0Im.toDouble()
        )

        val reflected = ComplexValue(
            re = record.rev0Re.toDouble(),
            im = record.rev0Im.toDouble()
        )

        val referenceMagnitude = complexMagnitude(reference)
        if (referenceMagnitude <= EPSILON) {
            return ParsedSweepPointResult(
                point = null,
                rejectionReason = "REFERENCE_MAGNITUDE_ZERO"
            )
        }

        val s11 = complexDivide(
            numerator = reflected,
            denominator = reference
        )

        val gammaMagnitude =
            complexMagnitude(s11)
                .coerceIn(0.0, MAX_USABLE_GAMMA_MAGNITUDE)

        val impedance = calculateImpedance(
            reflectionCoefficient = s11
        )

        val returnLossDb =
            if (gammaMagnitude <= EPSILON) {
                120.0
            } else {
                (-20.0 * log10(gammaMagnitude))
                    .coerceAtLeast(0.0)
            }

        val swr =
            if (gammaMagnitude >= MAX_USABLE_GAMMA_MAGNITUDE) {
                MAX_DISPLAY_SWR
            } else {
                ((1.0 + gammaMagnitude) / (1.0 - gammaMagnitude))
                    .coerceAtLeast(1.0)
                    .coerceAtMost(MAX_DISPLAY_SWR)
            }

        val frequencyHz =
            startFrequencyHz + (record.freqIndex.toLong() * stepFrequencyHz)

        return ParsedSweepPointResult(
            point = SweepPoint(
                frequencyMHz = frequencyHz / 1_000_000.0,
                swr = swr,
                returnLossDb = returnLossDb,
                resistance = impedance.re,
                reactance = impedance.im
            )
        )
    }

    private fun calculateImpedance(
        reflectionCoefficient: ComplexValue
    ): ComplexValue {
        val onePlusGamma = ComplexValue(
            re = 1.0 + reflectionCoefficient.re,
            im = reflectionCoefficient.im
        )

        val oneMinusGamma = ComplexValue(
            re = 1.0 - reflectionCoefficient.re,
            im = -reflectionCoefficient.im
        )

        val normalizedImpedance = complexDivide(
            numerator = onePlusGamma,
            denominator = oneMinusGamma
        )

        return ComplexValue(
            re = normalizedImpedance.re * REFERENCE_IMPEDANCE_OHMS,
            im = normalizedImpedance.im * REFERENCE_IMPEDANCE_OHMS
        )
    }

    private fun complexDivide(
        numerator: ComplexValue,
        denominator: ComplexValue
    ): ComplexValue {
        val divisor =
            (denominator.re * denominator.re) +
                    (denominator.im * denominator.im)

        if (divisor <= EPSILON) {
            return ComplexValue(0.0, 0.0)
        }

        return ComplexValue(
            re = ((numerator.re * denominator.re) + (numerator.im * denominator.im)) / divisor,
            im = ((numerator.im * denominator.re) - (numerator.re * denominator.im)) / divisor
        )
    }

    private fun complexMagnitude(
        value: ComplexValue
    ): Double {
        return hypot(value.re, value.im)
    }

    private fun formatBytePreview(
        bytes: ByteArray
    ): String {
        if (bytes.isEmpty()) {
            return "<no-bytes>"
        }

        return bytes.joinToString(separator = " ") { byte ->
            byte.toUByte().toString(16).padStart(2, '0')
        }
    }

    private fun decodeLittleEndianInt32(
        bytes: ByteArray,
        startIndex: Int
    ): Int {
        if (startIndex + 3 >= bytes.size) return 0

        val b0 = bytes[startIndex].toUByte().toInt()
        val b1 = bytes[startIndex + 1].toUByte().toInt()
        val b2 = bytes[startIndex + 2].toUByte().toInt()
        val b3 = bytes[startIndex + 3].toUByte().toInt()

        return b0 or
                (b1 shl 8) or
                (b2 shl 16) or
                (b3 shl 24)
    }

    private fun decodeLittleEndianUInt16(
        bytes: ByteArray,
        startIndex: Int
    ): Int {
        if (startIndex + 1 >= bytes.size) return 0

        val b0 = bytes[startIndex].toUByte().toInt()
        val b1 = bytes[startIndex + 1].toUByte().toInt()

        return b0 or (b1 shl 8)
    }

    private fun buildPointPreviewSummary(
        sweepResult: SweepResult
    ): String {
        val firstPoint = sweepResult.points.firstOrNull()
            ?: return "No decoded points available."

        val middlePoint = sweepResult.points.getOrNull(sweepResult.points.size / 2)
            ?: firstPoint

        val lastPoint = sweepResult.points.lastOrNull()
            ?: firstPoint

        return buildString {
            append("First=")
            append(formatPointPreview(firstPoint))
            append(" Mid=")
            append(formatPointPreview(middlePoint))
            append(" Last=")
            append(formatPointPreview(lastPoint))
            append(".")
        }
    }

    private fun formatPointPreview(
        point: SweepPoint
    ): String {
        return buildString {
            append(String.format("%.3f", point.frequencyMHz))
            append("MHz")
            append(" SWR=")
            append(String.format("%.3f", point.swr))
            append(" R=")
            append(String.format("%.3f", point.resistance))
            append(" X=")
            append(String.format("%.3f", point.reactance))
        }
    }
}