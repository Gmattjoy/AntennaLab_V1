package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.testing.SweepPoint
import com.example.antennalab_v1.model.testing.SweepResult
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/*
########################################################################
FILE: NanoVnaSweepProtocol.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Sweep Logic

LAST UPDATED 19/3/2026 00:58

SYSTEM ROLE
Implements the sweep protocol for NanoVNA-family analyzers.

CURRENT DEVELOPMENT ROLE
Provides the first real analyzer sweep implementation that:

• configures sweep start
• configures sweep stop
• configures point count
• triggers a sweep
• reads returned sweep data
• detects truncated or malformed data
• detects repeated frames
• re-requests data before hard failure
• converts raw analyzer data into SweepResult

This file now also provides the NanoVNA driver entry required by the
central driver registry.

IMPORTANT NOTE
This version intentionally implements a safe first-pass protocol focused
on command sequencing, resilience, and architecture validation.

SAFE EDIT AREA
- add binary sweep parsing
- add streaming reads
- extend retry policy
- tune timeout handling
- add richer NanoVNA capability detection
########################################################################
*/

/*
########################################################################
EDIT SECTION 1000
NANOVNA DRIVER
------------------------------------------------------------------------
PURPOSE
Provides the NanoVNA driver contract implementation used by the central
driver registry.
########################################################################
*/
class NanoVnaInstrumentDriver : UsbVnaInstrumentDriver {

    override val driverId: String = "nanovna.driver.v1"
    override val protocolFamily: UsbVnaProtocolFamily = UsbVnaProtocolFamily.NANOVNA

    override fun matchIdentity(
        identityResult: UsbAnalyzerIdentityResult
    ): UsbVnaDriverMatchResult {
        if (!identityResult.success) {
            return UsbVnaDriverMatchResult(
                matched = false,
                supportTier = InstrumentSupportTier.DETECTED,
                matchSummary = "Identity query did not succeed, so NanoVNA driver cannot fully match."
            )
        }

        val rawText = identityResult.rawIdentityText.orEmpty().lowercase()
        val family = identityResult.protocolIdentity.family

        if (family == UsbVnaProtocolFamily.NANOVNA) {
            return UsbVnaDriverMatchResult(
                matched = true,
                supportTier = InstrumentSupportTier.FULL_SUPPORT,
                matchSummary = "NanoVNA protocol family matched directly."
            )
        }

        if (rawText.contains("nanovna")) {
            return UsbVnaDriverMatchResult(
                matched = true,
                supportTier = InstrumentSupportTier.PARTIAL_SUPPORT,
                matchSummary = "NanoVNA text markers found, but protocol-family match is not yet confirmed."
            )
        }

        return UsbVnaDriverMatchResult(
            matched = false,
            supportTier = InstrumentSupportTier.DETECTED,
            matchSummary = "NanoVNA driver did not match the returned analyzer identity."
        )
    }

    override fun getCapabilityProfile(): UsbVnaDriverCapabilityProfile {
        return UsbVnaDriverCapabilityProfile(
            supportsSweepExecution = true,
            supportsS11 = true,
            supportsS11Phase = false,
            supportsS21 = false,
            supportsS21Phase = false,
            measurementTrustSummary =
                "Real USB sweep path is available for first-pass transport and protocol validation. RF values remain provisional until true NanoVNA S-parameter conversion is implemented."
        )
    }

    override fun getTransportBehaviorProfile(): UsbVnaTransportBehaviorProfile {
        return UsbVnaTransportBehaviorProfile(
            requiresOpenSession = true,
            requiresBulkEndpoints = true,
            prefersTextCommands = true,
            behaviorSummary =
                "Requires open USB session with prepared BULK IN/BULK OUT transport and text command exchange."
        )
    }

    override fun acquireSweep(
        commandChannel: UsbVnaCommandChannel,
        startFrequencyHz: Long,
        stopFrequencyHz: Long,
        pointCount: Int
    ): SweepResult {
        return NanoVnaSweepProtocol().acquireSweep(
            commandChannel = commandChannel,
            startFrequencyHz = startFrequencyHz,
            stopFrequencyHz = stopFrequencyHz,
            pointCount = pointCount
        )
    }
}

/*
########################################################################
EDIT SECTION 2000
NANOVNA SWEEP PROTOCOL IMPLEMENTATION
------------------------------------------------------------------------
PURPOSE
Implements the current first-pass NanoVNA sweep flow using the prepared
USB command channel.
########################################################################
*/
class NanoVnaSweepProtocol : UsbVnaSweepProtocol {

    companion object {
        private const val MAX_DATA_REQUEST_ATTEMPTS = 3
        private const val MAX_FREQUENCY_TOLERANCE_HZ = 2.0
        private const val MAX_NORMALIZED_MAGNITUDE = 0.999999
        private const val MIN_NORMALIZED_MAGNITUDE = 1e-9
    }

    override fun acquireSweep(
        commandChannel: UsbVnaCommandChannel,
        startFrequencyHz: Long,
        stopFrequencyHz: Long,
        pointCount: Int
    ): SweepResult {

        /*
        ----------------------------------------------------------------
        EDIT SECTION 2100
        REQUEST VALIDATION
        ----------------------------------------------------------------
        PURPOSE
        Validates sweep bounds before any device communication begins.
        ----------------------------------------------------------------
        */
        UsbVnaSweepProtocolRules.requireValidSweepRequest(
            startFrequencyHz = startFrequencyHz,
            stopFrequencyHz = stopFrequencyHz,
            pointCount = pointCount
        )

        /*
        ----------------------------------------------------------------
        EDIT SECTION 2200
        SWEEP CONFIGURATION COMMANDS
        ----------------------------------------------------------------
        PURPOSE
        Sends the current first-pass sweep setup commands to the analyzer.
        ----------------------------------------------------------------
        */
        sendRequiredCommand(
            commandChannel = commandChannel,
            commandText = "sweep start $startFrequencyHz",
            failurePrefix = "NanoVNA sweep start command failed."
        )

        sendRequiredCommand(
            commandChannel = commandChannel,
            commandText = "sweep stop $stopFrequencyHz",
            failurePrefix = "NanoVNA sweep stop command failed."
        )

        sendRequiredCommand(
            commandChannel = commandChannel,
            commandText = "sweep points $pointCount",
            failurePrefix = "NanoVNA sweep point-count command failed."
        )

        /*
        ----------------------------------------------------------------
        EDIT SECTION 2300
        SWEEP TRIGGER COMMAND
        ----------------------------------------------------------------
        PURPOSE
        Triggers the analyzer to run the configured sweep.
        ----------------------------------------------------------------
        */
        sendRequiredCommand(
            commandChannel = commandChannel,
            commandText = "sweep",
            failurePrefix = "NanoVNA sweep trigger command failed."
        )

        /*
        ----------------------------------------------------------------
        EDIT SECTION 2400
        SWEEP DATA REQUEST
        ----------------------------------------------------------------
        PURPOSE
        Requests the returned sweep data block from the analyzer.
        ----------------------------------------------------------------
        */
        val stepHz = UsbVnaSweepProtocolRules.calculateStepHz(
            startFrequencyHz = startFrequencyHz,
            stopFrequencyHz = stopFrequencyHz,
            pointCount = pointCount
        )

        val acquisitionResult = acquireValidatedSweepPoints(
            commandChannel = commandChannel,
            startFrequencyHz = startFrequencyHz,
            stepHz = stepHz,
            requestedPointCount = pointCount
        )

        /*
        ----------------------------------------------------------------
        EDIT SECTION 2500
        SWEEP RESULT BUILD
        ----------------------------------------------------------------
        PURPOSE
        Converts parsed points into the shared SweepResult model used by
        the rest of the testing stack.
        ----------------------------------------------------------------
        */
        return SweepResult(
            startFrequencyMHz = startFrequencyHz / 1_000_000.0,
            endFrequencyMHz = stopFrequencyHz / 1_000_000.0,
            stepMHz = stepHz / 1_000_000.0,
            points = acquisitionResult.points,
            sweepPointCount = acquisitionResult.points.size,
            hardwareProfile = "USB_NANOVNA_DRIVER",
            supportsS11 = true,
            supportsS11Phase = false,
            supportsS21 = false,
            supportsS21Phase = false
        )
    }

    /*
    ####################################################################
    EDIT SECTION 3000
    ACQUIRE VALIDATED SWEEP POINTS
    --------------------------------------------------------------------
    PURPOSE
    Re-requests the sweep data block when the returned dataset looks too
    weak to trust as a real sweep.
    ####################################################################
    */
    private fun acquireValidatedSweepPoints(
        commandChannel: UsbVnaCommandChannel,
        startFrequencyHz: Long,
        stepHz: Double,
        requestedPointCount: Int
    ): SweepParseResult {
        var attemptIndex = 0
        var lastParseResult: SweepParseResult? = null
        var lastCommandResult: UsbVnaCommandResult? = null

        while (attemptIndex < MAX_DATA_REQUEST_ATTEMPTS) {
            val dataResult = commandChannel.sendCommandAndReadResponse(
                commandText = "data"
            )

            lastCommandResult = dataResult

            require(dataResult.success) {
                "NanoVNA sweep data request failed. ${dataResult.summary} ${dataResult.debugSummary}"
            }

            val responseText = dataResult.responseText?.trim()
            require(!responseText.isNullOrBlank()) {
                "NanoVNA sweep data request succeeded but returned no readable data. ${dataResult.debugSummary}"
            }

            val parseResult = parseSweepPoints(
                responseText = responseText,
                startFrequencyHz = startFrequencyHz,
                stepHz = stepHz,
                requestedPointCount = requestedPointCount
            )

            lastParseResult = parseResult

            if (isAcceptedSweepParseResult(
                    parseResult = parseResult,
                    requestedPointCount = requestedPointCount
                )
            ) {
                return parseResult
            }

            attemptIndex += 1
        }

        val parseDebug = lastParseResult?.debugSummary ?: "No parse result available."
        val commandDebug = lastCommandResult?.debugSummary ?: "No command debug available."

        error(
            "NanoVNA sweep parser rejected all returned data attempts. " +
                    "Attempts=$MAX_DATA_REQUEST_ATTEMPTS $parseDebug $commandDebug"
        )
    }

    /*
    ####################################################################
    EDIT SECTION 4000
    ACCEPTED SWEEP DECISION
    --------------------------------------------------------------------
    PURPOSE
    Applies conservative acceptance rules so obviously bad or repeated
    data is re-requested rather than returned to the UI as a false sweep.
    ####################################################################
    */
    private fun isAcceptedSweepParseResult(
        parseResult: SweepParseResult,
        requestedPointCount: Int
    ): Boolean {
        if (parseResult.points.isEmpty()) {
            return false
        }

        if (parseResult.points.size < 2) {
            return false
        }

        if (parseResult.points.size < calculateMinimumAcceptablePointCount(requestedPointCount)) {
            return false
        }

        if (parseResult.hasRepeatedFramePattern) {
            return false
        }

        if (parseResult.hasSevereMalformedData) {
            return false
        }

        if (parseResult.hasNonIncreasingFrequencies) {
            return false
        }

        if (parseResult.hasUnexpectedFrequencySpacing) {
            return false
        }

        if (parseResult.hasNonFiniteDerivedValues) {
            return false
        }

        return true
    }

    /*
    ####################################################################
    EDIT SECTION 5000
    SEND REQUIRED COMMAND
    --------------------------------------------------------------------
    PURPOSE
    Centralises required command execution so command failures remain
    consistent and easy to trace during protocol bring-up.
    ####################################################################
    */
    private fun sendRequiredCommand(
        commandChannel: UsbVnaCommandChannel,
        commandText: String,
        failurePrefix: String
    ) {
        val result = commandChannel.sendCommand(
            commandText = commandText
        )

        require(result.success) {
            "$failurePrefix ${result.summary} ${result.debugSummary}"
        }
    }

    /*
    ####################################################################
    EDIT SECTION 6000
    PARSE SWEEP POINTS
    --------------------------------------------------------------------
    PURPOSE
    Converts temporary text response lines into shared SweepPoint models.
    ####################################################################
    */
    private fun parseSweepPoints(
        responseText: String,
        startFrequencyHz: Long,
        stepHz: Double,
        requestedPointCount: Int
    ): SweepParseResult {
        val rawLines = responseText.lines()

        val candidateLines = rawLines
            .map { line -> sanitizeSweepLine(line) }
            .filter { line -> isCandidateSweepDataLine(line) }

        val points = mutableListOf<SweepPoint>()
        val acceptedPointFingerprints = mutableSetOf<String>()
        var acceptedPointIndex = 0
        var rejectedLineCount = 0
        var malformedLineCount = 0
        var repeatedFrameCount = 0

        candidateLines.forEach { line ->
            if (acceptedPointIndex >= requestedPointCount) {
                return@forEach
            }

            val fingerprint = buildPointFingerprint(line)
            val frequencyHz = startFrequencyHz.toDouble() + (acceptedPointIndex * stepHz)

            val parsedPoint = parseSweepPointLine(
                line = line,
                frequencyHz = frequencyHz
            )

            if (parsedPoint == null) {
                rejectedLineCount += 1
                malformedLineCount += 1
                return@forEach
            }

            if (!acceptedPointFingerprints.add(fingerprint)) {
                repeatedFrameCount += 1
                return@forEach
            }

            points.add(parsedPoint)
            acceptedPointIndex += 1
        }

        val hasSevereMalformedData =
            malformedLineCount >= (requestedPointCount / 4).coerceAtLeast(4)

        val hasRepeatedFramePattern =
            repeatedFrameCount >= (requestedPointCount / 5).coerceAtLeast(3)

        val hasNonIncreasingFrequencies = hasNonIncreasingFrequencies(points)
        val hasUnexpectedFrequencySpacing = hasUnexpectedFrequencySpacing(
            points = points,
            expectedStepHz = stepHz
        )
        val hasNonFiniteDerivedValues = hasNonFiniteDerivedValues(points)

        return SweepParseResult(
            points = points,
            rejectedLineCount = rejectedLineCount,
            malformedLineCount = malformedLineCount,
            repeatedFrameCount = repeatedFrameCount,
            hasSevereMalformedData = hasSevereMalformedData,
            hasRepeatedFramePattern = hasRepeatedFramePattern,
            hasNonIncreasingFrequencies = hasNonIncreasingFrequencies,
            hasUnexpectedFrequencySpacing = hasUnexpectedFrequencySpacing,
            hasNonFiniteDerivedValues = hasNonFiniteDerivedValues,
            debugSummary =
                "RawLines=${rawLines.size} CandidateLines=${candidateLines.size} ParsedPoints=${points.size} " +
                        "RejectedCandidateLines=$rejectedLineCount MalformedLines=$malformedLineCount " +
                        "RepeatedFrames=$repeatedFrameCount RequestedPoints=$requestedPointCount " +
                        "SevereMalformed=$hasSevereMalformedData RepeatedPattern=$hasRepeatedFramePattern " +
                        "NonIncreasingFreq=$hasNonIncreasingFrequencies UnexpectedSpacing=$hasUnexpectedFrequencySpacing " +
                        "NonFiniteDerived=$hasNonFiniteDerivedValues"
        )
    }

    /*
    ####################################################################
    EDIT SECTION 7000
    POINT FINGERPRINTING
    --------------------------------------------------------------------
    PURPOSE
    Produces a normalized line signature so repeated text frames can be
    detected even before true binary framing exists.
    ####################################################################
    */
    private fun buildPointFingerprint(
        line: String
    ): String {
        return line
            .replace('\u0000', ' ')
            .trim()
            .lowercase()
    }

    /*
    ####################################################################
    EDIT SECTION 8000
    PARSE SWEEP POINT LINE
    --------------------------------------------------------------------
    PURPOSE
    Parses one temporary text-format sweep line into a shared SweepPoint.
    ####################################################################
    */
    private fun parseSweepPointLine(
        line: String,
        frequencyHz: Double
    ): SweepPoint? {
        val sanitizedLine = sanitizeSweepLine(line)

        val parts = splitSweepDataParts(sanitizedLine)

        if (parts.size < 2) {
            return null
        }

        val real = parts[0].toDoubleOrNull() ?: return null
        val imag = parts[1].toDoubleOrNull() ?: return null

        if (!real.isFinite() || !imag.isFinite() || !frequencyHz.isFinite()) {
            return null
        }

        /*
        ----------------------------------------------------------------
        REAL NANOVNA COMPLEX REFLECTION CONVERSION
        ----------------------------------------------------------------
        PURPOSE
        Interprets the returned pair as the real/imaginary parts of the
        reflection coefficient Γ and derives RF display values from that.

        DERIVED VALUES
        • |Γ|
        • S11 magnitude in dB
        • return loss
        • SWR
        • S11 phase
        • impedance using Z = Z0 * (1 + Γ) / (1 - Γ)

        ASSUMPTION
        This path assumes the returned NanoVNA data line is the complex
        reflection coefficient for the active reflection channel.
        ----------------------------------------------------------------
        */
        val gammaMagnitude = sqrt((real * real) + (imag * imag))
            .coerceIn(MIN_NORMALIZED_MAGNITUDE, MAX_NORMALIZED_MAGNITUDE)

        val s11MagnitudeDb = 20.0 * log10(gammaMagnitude)
        val returnLossDb = -s11MagnitudeDb

        val swr = ((1.0 + gammaMagnitude) / (1.0 - gammaMagnitude))
            .coerceAtLeast(1.0)

        val phaseRadians = kotlin.math.atan2(imag, real)
        val s11PhaseDegrees = Math.toDegrees(phaseRadians)

        val denominatorReal = 1.0 - real
        val denominatorImag = -imag
        val denominatorMagnitudeSquared =
            (denominatorReal * denominatorReal) + (denominatorImag * denominatorImag)

        if (denominatorMagnitudeSquared <= 1e-12 || !denominatorMagnitudeSquared.isFinite()) {
            return null
        }

        val numeratorReal = 1.0 + real
        val numeratorImag = imag

        val normalizedImpedanceReal =
            ((numeratorReal * denominatorReal) + (numeratorImag * denominatorImag)) /
                    denominatorMagnitudeSquared

        val normalizedImpedanceImag =
            ((numeratorImag * denominatorReal) - (numeratorReal * denominatorImag)) /
                    denominatorMagnitudeSquared

        val resistance = 50.0 * normalizedImpedanceReal
        val reactance = 50.0 * normalizedImpedanceImag

        if (
            !swr.isFinite() ||
            !returnLossDb.isFinite() ||
            !s11MagnitudeDb.isFinite() ||
            !s11PhaseDegrees.isFinite() ||
            !resistance.isFinite() ||
            !reactance.isFinite()
        ) {
            return null
        }

        return SweepPoint(
            frequencyMHz = frequencyHz / 1_000_000.0,
            swr = swr,
            returnLossDb = returnLossDb,
            resistance = resistance.coerceAtLeast(0.0),
            reactance = reactance,
            s11MagnitudeDb = s11MagnitudeDb,
            s11PhaseDegrees = s11PhaseDegrees
        )
    }

    /*
    ####################################################################
    EDIT SECTION 8500
    LINE SANITATION
    --------------------------------------------------------------------
    PURPOSE
    Normalizes raw response lines before sweep candidate filtering and
    numeric parsing.
    ####################################################################
    */
    private fun sanitizeSweepLine(
        line: String
    ): String {
        return line
            .replace('\u0000', ' ')
            .replace('\t', ' ')
            .replace(";", " ")
            .replace(",", " ")
            .trim()
    }

    private fun splitSweepDataParts(
        line: String
    ): List<String> {
        return line
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun isCandidateSweepDataLine(
        line: String
    ): Boolean {
        if (line.isBlank()) {
            return false
        }

        val normalized = line.trim().lowercase()

        if (normalized.startsWith("#")) {
            return false
        }

        if (normalized == "data") {
            return false
        }

        if (normalized == "ok") {
            return false
        }

        if (normalized == "sweep") {
            return false
        }

        if (normalized.startsWith("sweep ")) {
            return false
        }

        if (normalized.startsWith("version")) {
            return false
        }

        if (normalized.startsWith("help")) {
            return false
        }

        if (normalized.startsWith("scan")) {
            return false
        }

        if (normalized.contains("error")) {
            return false
        }

        val parts = splitSweepDataParts(normalized)

        if (parts.size < 2) {
            return false
        }

        return parts[0].toDoubleOrNull() != null &&
                parts[1].toDoubleOrNull() != null
    }
    /*
    ####################################################################
    EDIT SECTION 9000
    TRUNCATION SAFETY
    --------------------------------------------------------------------
    PURPOSE
    Applies a conservative minimum accepted parsed point count so badly
    truncated reads do not silently look like valid sweeps.
    ####################################################################
    */
    private fun calculateMinimumAcceptablePointCount(
        requestedPointCount: Int
    ): Int {
        return when {
            requestedPointCount <= 8 -> requestedPointCount
            else -> (requestedPointCount * 0.80).toInt().coerceAtLeast(8)
        }
    }

    /*
    ####################################################################
    EDIT SECTION 9500
    FREQUENCY VALIDATION
    --------------------------------------------------------------------
    PURPOSE
    Applies simple ordering and spacing checks so malformed text frames do
    not silently pass as a trusted sweep.
    ####################################################################
    */
    private fun hasNonIncreasingFrequencies(
        points: List<SweepPoint>
    ): Boolean {
        if (points.size < 2) {
            return false
        }

        for (index in 1 until points.size) {
            if (points[index].frequencyMHz <= points[index - 1].frequencyMHz) {
                return true
            }
        }

        return false
    }

    private fun hasUnexpectedFrequencySpacing(
        points: List<SweepPoint>,
        expectedStepHz: Double
    ): Boolean {
        if (points.size < 3 || expectedStepHz <= 0.0) {
            return false
        }

        for (index in 1 until points.size) {
            val previousHz = points[index - 1].frequencyMHz * 1_000_000.0
            val currentHz = points[index].frequencyMHz * 1_000_000.0
            val actualStepHz = currentHz - previousHz

            if (abs(actualStepHz - expectedStepHz) > MAX_FREQUENCY_TOLERANCE_HZ) {
                return true
            }
        }

        return false
    }

    private fun hasNonFiniteDerivedValues(
        points: List<SweepPoint>
    ): Boolean {
        return points.any { point ->
            !point.frequencyMHz.isFinite() ||
                    !point.swr.isFinite() ||
                    !point.returnLossDb.isFinite() ||
                    !point.resistance.isFinite() ||
                    !point.reactance.isFinite()
        }
    }

    /*
    ####################################################################
    EDIT SECTION 10000
    PARSE RESULT MODEL
    --------------------------------------------------------------------
    PURPOSE
    Keeps parser diagnostics grouped with the returned point list during
    hardware bring-up and parser tuning.
    ####################################################################
    */
    private data class SweepParseResult(
        val points: List<SweepPoint>,
        val rejectedLineCount: Int,
        val malformedLineCount: Int,
        val repeatedFrameCount: Int,
        val hasSevereMalformedData: Boolean,
        val hasRepeatedFramePattern: Boolean,
        val hasNonIncreasingFrequencies: Boolean,
        val hasUnexpectedFrequencySpacing: Boolean,
        val hasNonFiniteDerivedValues: Boolean,
        val debugSummary: String
    )
}