package com.example.antennalab_v1.domain.testing

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import com.example.antennalab_v1.model.DriverProfile
import com.example.antennalab_v1.model.DriverProtocolType
import com.example.antennalab_v1.model.HardwareCapabilityProfiles
import com.example.antennalab_v1.model.HardwareConnectionState
import com.example.antennalab_v1.model.HardwareFamily
import com.example.antennalab_v1.model.HardwareMeasurementCapabilities
import com.example.antennalab_v1.model.UsbConnectionInfo
import com.example.antennalab_v1.model.UserHardwareConfig
import com.example.antennalab_v1.model.testing.CalibrationCompletionState
import com.example.antennalab_v1.model.testing.CalibrationReadiness
import com.example.antennalab_v1.model.testing.CalibrationSession
import com.example.antennalab_v1.model.testing.InstrumentCalibrationState
import com.example.antennalab_v1.model.testing.InstrumentDataSourceKind
import com.example.antennalab_v1.model.testing.InstrumentSessionState
import com.example.antennalab_v1.model.testing.MeasurementTrustLevel
import com.example.antennalab_v1.model.testing.UsbHardwareSession

/*
########################################################################
FILE: UsbSessionManager.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / USB

LAST UPDATED 4/4/2026 13:35

IMPORTANT CHANGE
Calibration registration now enriches the captured calibration session
with current session truth and guards against session/protocol/identity
mismatch more explicitly.
########################################################################
*/
object UsbSessionManager {

    private var activeConnection: UsbDeviceConnection? = null
    private var activeDeviceName: String? = null
    private var activeDeviceId: Int? = null
    private var activeTransportChannel: UsbTransportChannel? = null
    private var activeSessionGeneration: Int = 0

    const val ACTION_SESSION_STATE_UPDATED =
        "com.example.antennalab_v1.ACTION_SESSION_STATE_UPDATED"

    private var latestInstrumentSessionState: InstrumentSessionState? = null
    private var latestAnalyzerIdentityResult: UsbAnalyzerIdentityResult? = null
    private var latestTransportHealthSnapshot = UsbVnaTransportHealthSnapshot()
    private var latestInstrumentCalibrationState = InstrumentCalibrationState()

    private var latestLiteVnaBringUpResult: LiteVnaBringUpResult? = null
    private var latestLiteVnaIdentityResult: LiteVnaBringUpResult? = null
    private var latestLiteVnaCommandTestResult: LiteVnaBringUpResult? = null

    private var selectedUserHardwareConfig: UserHardwareConfig? = null
    private var selectedDriverProfile: DriverProfile? = null

    @Volatile
    private var identityProbeInFlight: Boolean = false

    @Volatile
    private var liteVnaBringUpInFlight: Boolean = false

    @Volatile
    private var latestLiteVnaBringUpSessionKey: String? = null

    fun getActiveTransportChannel(): UsbTransportChannel? = activeTransportChannel
    fun hasActiveTransportChannel(): Boolean = activeTransportChannel != null
    fun isTransportReady(): Boolean = activeConnection != null && activeTransportChannel != null
    fun hasOpenSession(): Boolean = activeConnection != null
    fun getLatestInstrumentSessionState(): InstrumentSessionState? = latestInstrumentSessionState
    fun getLatestAnalyzerIdentityResult(): UsbAnalyzerIdentityResult? = latestAnalyzerIdentityResult
    fun getLatestTransportHealthSnapshot(): UsbVnaTransportHealthSnapshot = latestTransportHealthSnapshot
    fun getLatestInstrumentCalibrationState(): InstrumentCalibrationState = latestInstrumentCalibrationState
    fun getSelectedUserHardwareConfig(): UserHardwareConfig? = selectedUserHardwareConfig
    fun getSelectedDriverProfile(): DriverProfile? = selectedDriverProfile
    fun getLatestLiteVnaBringUpResult(): LiteVnaBringUpResult? = latestLiteVnaBringUpResult
    fun getLatestLiteVnaIdentityResult(): LiteVnaBringUpResult? = latestLiteVnaIdentityResult
    fun getLatestLiteVnaCommandTestResult(): LiteVnaBringUpResult? = latestLiteVnaCommandTestResult

    fun getLiteVnaBringUpDebugSummary(): String {
        val bringUp = latestLiteVnaBringUpResult
        val identity = latestLiteVnaIdentityResult
        val command = latestLiteVnaCommandTestResult

        return buildString {
            appendLine("LiteVNA bring-up debug:")
            appendLine(
                "bringUp.success=${bringUp?.success} stage=${bringUp?.stage ?: "null"} summary=${bringUp?.summary ?: "null"}"
            )
            appendLine(
                "identity.success=${identity?.success} stage=${identity?.stage ?: "null"} summary=${identity?.summary ?: "null"}"
            )
            appendLine(
                "command.success=${command?.success} stage=${command?.stage ?: "null"} summary=${command?.summary ?: "null"}"
            )
            appendLine("sessionOpen=${hasOpenSession()} transportReady=${isTransportReady()}")
            appendLine(
                "selectedProfile=${selectedDriverProfile?.displayName ?: "null"} protocol=${selectedDriverProfile?.protocolType ?: "null"}"
            )
        }
    }

    fun registerSelectedHardwareConfig(
        hardwareConfig: UserHardwareConfig?
    ) {
        selectedUserHardwareConfig = hardwareConfig
        selectedDriverProfile = hardwareConfig?.selectedDriverProfileId
            ?.takeIf { it.isNotBlank() }
            ?.let { DriverProfileRegistry.getProfileById(it) }

        if (selectedDriverProfile?.protocolType != DriverProtocolType.LITE_VNA_V2_STYLE) {
            clearLiteVnaBringUpResults()
        }
    }

    fun clearSelectedHardwareConfig() {
        selectedUserHardwareConfig = null
        selectedDriverProfile = null
        clearLiteVnaBringUpResults()
    }

    fun refreshLatestKnownSessionState(
        context: Context
    ): InstrumentSessionState {
        val selectedHardwareName =
            latestInstrumentSessionState?.selectedHardwareName
                ?: selectedDriverProfile?.displayName
                ?: "Unknown USB Analyzer"

        return buildInstrumentSessionState(
            context = context,
            selectedHardwareName = selectedHardwareName
        )
    }

    fun registerAnalyzerIdentityResult(
        identityResult: UsbAnalyzerIdentityResult?
    ) {
        latestAnalyzerIdentityResult = identityResult
    }

    fun registerTransportHealthSnapshot(
        transportHealthSnapshot: UsbVnaTransportHealthSnapshot
    ) {
        latestTransportHealthSnapshot = transportHealthSnapshot
    }

    fun registerLiteVnaBringUpResults(
        bringUp: LiteVnaBringUpResult?,
        identity: LiteVnaBringUpResult?,
        commandTest: LiteVnaBringUpResult?
    ) {
        latestLiteVnaBringUpResult = bringUp
        latestLiteVnaIdentityResult = identity
        latestLiteVnaCommandTestResult = commandTest
    }

    fun startLiteVnaBringUpIfNeeded(
        context: Context,
        selectedHardwareName: String
    ) {
        ensureLiteVnaBringUpStarted(
            context = context,
            selectedHardwareName = selectedHardwareName,
            sessionOpen = hasOpenSession(),
            transportReady = isTransportReady()
        )
    }

    fun startBackgroundIdentityProbe(
        context: Context,
        selectedHardwareName: String
    ) {
        if (identityProbeInFlight) return
        if (!hasOpenSession() || getActiveTransportChannel() == null) return

        val isLiteProfile =
            selectedDriverProfile?.protocolType == DriverProtocolType.LITE_VNA_V2_STYLE

        if (isLiteProfile) {
            ensureLiteVnaBringUpStarted(
                context = context,
                selectedHardwareName = selectedHardwareName,
                sessionOpen = hasOpenSession(),
                transportReady = isTransportReady()
            )
            return
        }

        val cachedIdentity = latestAnalyzerIdentityResult
        if (cachedIdentity != null && cachedIdentity.success) {
            return
        }

        identityProbeInFlight = true

        kotlin.concurrent.thread(
            start = true,
            isDaemon = true,
            name = "UsbSessionIdentityProbe"
        ) {
            try {
                val commandChannel = UsbVnaCommandChannel()
                val identityResult = commandChannel.queryAnalyzerIdentity()

                registerAnalyzerIdentityResult(identityResult)

                refreshCurrentSessionState(
                    context = context,
                    selectedHardwareName = selectedHardwareName
                )

                publishSessionStateUpdated(context)
            } finally {
                identityProbeInFlight = false
            }
        }
    }

    private fun ensureLiteVnaBringUpStarted(
        context: Context,
        selectedHardwareName: String,
        sessionOpen: Boolean,
        transportReady: Boolean
    ) {
        val isLiteProfile =
            selectedDriverProfile?.protocolType == DriverProtocolType.LITE_VNA_V2_STYLE

        if (!isLiteProfile) {
            clearLiteVnaBringUpResults()
            return
        }

        if (!sessionOpen || !transportReady) return

        val activeSessionKey = buildActiveSessionKey()

        if (liteVnaBringUpInFlight) return

        if (
            latestLiteVnaBringUpSessionKey != null &&
            latestLiteVnaBringUpSessionKey == activeSessionKey &&
            latestLiteVnaCommandTestResult != null
        ) {
            return
        }

        runLiteVnaBringUp(
            context = context,
            selectedHardwareName = selectedHardwareName
        )
    }

    private fun runLiteVnaBringUp(
        context: Context,
        selectedHardwareName: String
    ) {
        if (liteVnaBringUpInFlight) return
        liteVnaBringUpInFlight = true

        val sessionKeyAtStart = buildActiveSessionKey()

        kotlin.concurrent.thread(
            start = true,
            isDaemon = true,
            name = "UsbSessionLiteVnaBringUp"
        ) {
            try {
                val resultRef =
                    java.util.concurrent.atomic.AtomicReference<
                            Triple<LiteVnaBringUpResult, LiteVnaBringUpResult, LiteVnaBringUpResult>?
                            >(null)

                val worker = kotlin.concurrent.thread(
                    start = true,
                    isDaemon = true,
                    name = "UsbSessionLiteVnaBringUpWorker"
                ) {
                    val commandChannel = UsbVnaCommandChannel()
                    val liteProtocol = LiteVnaSweepProtocol(commandChannel)

                    resultRef.set(
                        Triple(
                            liteProtocol.checkBringUpReadiness(),
                            liteProtocol.probeIdentity(),
                            liteProtocol.runBasicCommandTest()
                        )
                    )
                }

                runCatching {
                    worker.join(15000L)
                }

                val finalResult = resultRef.get()

                if (finalResult == null) {
                    val existingIdentity = latestLiteVnaIdentityResult
                    val existingCommandTest = latestLiteVnaCommandTestResult

                    val timedOut = LiteVnaBringUpResult(
                        success = false,
                        stage = "TIMED_OUT",
                        summary = "LiteVNA validation timed out after 15 seconds."
                    )

                    registerLiteVnaBringUpResults(
                        bringUp = timedOut,
                        identity = existingIdentity ?: timedOut,
                        commandTest = existingCommandTest ?: timedOut
                    )
                } else {
                    registerLiteVnaBringUpResults(
                        bringUp = finalResult.first,
                        identity = finalResult.second,
                        commandTest = finalResult.third
                    )
                }

                latestLiteVnaBringUpSessionKey = sessionKeyAtStart

                refreshCurrentSessionState(
                    context = context,
                    selectedHardwareName = selectedHardwareName
                )

                publishSessionStateUpdated(context)
            } finally {
                liteVnaBringUpInFlight = false
            }
        }
    }

    private fun publishSessionStateUpdated(
        context: Context
    ) {
        runCatching {
            context.sendBroadcast(Intent(ACTION_SESSION_STATE_UPDATED))
        }
    }

    fun registerCalibrationSession(
        calibrationSession: CalibrationSession
    ) {
        val currentSessionKey = buildActiveSessionKey()
        val currentInstrumentState = latestInstrumentSessionState
        val currentProtocolFamily =
            currentInstrumentState?.protocolFamily
                ?: selectedDriverProfile?.protocolType?.name

        val currentInstrumentIdentity =
            currentInstrumentState?.instrumentIdentityText
                ?: latestAnalyzerIdentityResult?.rawIdentityText

        if (currentSessionKey == null || !hasOpenSession() || !isTransportReady()) {
            latestInstrumentCalibrationState =
                InstrumentCalibrationState(
                    readiness = CalibrationReadiness.INVALID,
                    calibrationSession = calibrationSession,
                    sessionKeyAtCapture = null,
                    activeSessionKey = null,
                    statusSummary = "Calibration capture was attempted without an active live instrument session.",
                    operatorWarning = "Calibration is invalid because no active instrument session was available at capture time.",
                    sweepAllowed = true,
                    trustDowngraded = true
                )
            return
        }

        val enrichedCalibrationSession =
            calibrationSession.copy(
                hardwareDisplayName = currentInstrumentState?.selectedHardwareName
                    ?: calibrationSession.hardwareDisplayName,
                capturedProtocolFamily = calibrationSession.capturedProtocolFamily
                    ?: currentProtocolFamily,
                capturedInstrumentIdentityText = calibrationSession.capturedInstrumentIdentityText
                    ?: currentInstrumentIdentity,
                capturedSessionKey = currentSessionKey
            )

        val hardwareMatches =
            enrichedCalibrationSession.matchesHardwareDisplayName(
                currentInstrumentState?.selectedHardwareName
                    ?: enrichedCalibrationSession.hardwareDisplayName
            )

        val protocolMatches =
            enrichedCalibrationSession.matchesProtocolFamily(currentProtocolFamily)

        val identityMatches =
            enrichedCalibrationSession.matchesInstrumentIdentity(currentInstrumentIdentity)

        if (!hardwareMatches || !protocolMatches || !identityMatches) {
            latestInstrumentCalibrationState =
                InstrumentCalibrationState(
                    readiness = CalibrationReadiness.INVALID,
                    calibrationSession = enrichedCalibrationSession,
                    sessionKeyAtCapture = currentSessionKey,
                    activeSessionKey = currentSessionKey,
                    statusSummary = "Calibration capture does not match the active instrument truth for this session.",
                    operatorWarning = "Calibration is invalid because hardware identity or protocol truth changed during capture.",
                    sweepAllowed = true,
                    trustDowngraded = true
                )
            return
        }

        latestInstrumentCalibrationState = when (enrichedCalibrationSession.completionState) {
            CalibrationCompletionState.COMPLETE ->
                InstrumentCalibrationState(
                    readiness = CalibrationReadiness.VALID,
                    calibrationSession = enrichedCalibrationSession,
                    sessionKeyAtCapture = currentSessionKey,
                    activeSessionKey = currentSessionKey,
                    statusSummary = "Complete calibration is available for the current instrument session.",
                    operatorWarning = "Calibration is valid for the current session.",
                    sweepAllowed = true,
                    trustDowngraded = false
                )

            CalibrationCompletionState.PARTIAL ->
                InstrumentCalibrationState(
                    readiness = CalibrationReadiness.IN_PROGRESS,
                    calibrationSession = enrichedCalibrationSession,
                    sessionKeyAtCapture = currentSessionKey,
                    activeSessionKey = currentSessionKey,
                    statusSummary = "Calibration is only partially captured for the current instrument session.",
                    operatorWarning = "Calibration is incomplete. Sweeps may continue, but trust is downgraded until OPEN, SHORT, and LOAD are all captured.",
                    sweepAllowed = true,
                    trustDowngraded = true
                )

            CalibrationCompletionState.NOT_STARTED ->
                InstrumentCalibrationState(
                    readiness = CalibrationReadiness.NOT_STARTED,
                    calibrationSession = enrichedCalibrationSession,
                    sessionKeyAtCapture = currentSessionKey,
                    activeSessionKey = currentSessionKey,
                    statusSummary = "Calibration workflow exists but no calibration reference captures have been recorded yet.",
                    operatorWarning = "Measurements are currently uncalibrated and should be treated with reduced trust.",
                    sweepAllowed = true,
                    trustDowngraded = true
                )
        }
    }

    /**
     * DEBUG-ONLY: registers a calibration captured through the simulated
     * (no-hardware) wizard path. Unlike [registerCalibrationSession], this does
     * not require a live USB session — it binds to a synthetic session key so a
     * simulated capture is treated as usable (VALID when complete, IN_PROGRESS
     * when partial) for exercising the project display and the correction
     * pipeline without a VNA. The real capture path is unchanged and still
     * invalidates captures taken with no active session.
     */
    fun registerSimulatedCalibrationSession(
        calibrationSession: CalibrationSession
    ) {
        val simulatedSessionKey = "SIMULATED_CAL_SESSION"
        val enrichedCalibrationSession =
            calibrationSession.copy(
                capturedSessionKey = calibrationSession.capturedSessionKey
                    ?: simulatedSessionKey
            )

        latestInstrumentCalibrationState = when (enrichedCalibrationSession.completionState) {
            CalibrationCompletionState.COMPLETE ->
                InstrumentCalibrationState(
                    readiness = CalibrationReadiness.VALID,
                    calibrationSession = enrichedCalibrationSession,
                    sessionKeyAtCapture = simulatedSessionKey,
                    activeSessionKey = simulatedSessionKey,
                    statusSummary = "Simulated calibration (debug build) is available.",
                    operatorWarning = "Calibration is simulated (debug build), not captured from real hardware.",
                    sweepAllowed = true,
                    trustDowngraded = false
                )

            CalibrationCompletionState.PARTIAL ->
                InstrumentCalibrationState(
                    readiness = CalibrationReadiness.IN_PROGRESS,
                    calibrationSession = enrichedCalibrationSession,
                    sessionKeyAtCapture = simulatedSessionKey,
                    activeSessionKey = simulatedSessionKey,
                    statusSummary = "Simulated calibration (debug build) is partially captured.",
                    operatorWarning = "Simulated calibration is incomplete (debug build).",
                    sweepAllowed = true,
                    trustDowngraded = true
                )

            CalibrationCompletionState.NOT_STARTED ->
                InstrumentCalibrationState(
                    readiness = CalibrationReadiness.NOT_STARTED,
                    calibrationSession = enrichedCalibrationSession,
                    sessionKeyAtCapture = simulatedSessionKey,
                    activeSessionKey = simulatedSessionKey,
                    statusSummary = "Simulated calibration workflow started (debug build).",
                    operatorWarning = "Measurements are currently uncalibrated and should be treated with reduced trust.",
                    sweepAllowed = true,
                    trustDowngraded = true
                )
        }
    }

    fun clearCalibrationState() {
        latestInstrumentCalibrationState = InstrumentCalibrationState()
    }

    fun buildInstrumentSessionState(
        context: Context,
        selectedHardwareName: String
    ): InstrumentSessionState {
        val hardwareSession = refreshCurrentSessionState(
            context = context,
            selectedHardwareName = selectedHardwareName
        )

        return hardwareSession.instrumentSessionState
            ?: latestInstrumentSessionState
            ?: composeInstrumentSessionState(
                selectedHardwareName = selectedHardwareName,
                connectionInfo = hardwareSession.connectionInfo
            )
    }

    fun refreshCurrentSessionState(
        context: Context,
        selectedHardwareName: String
    ): UsbHardwareSession {
        val hasUsbHostFeature =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)

        if (!hasUsbHostFeature) {
            invalidateActiveSession("Calibration was marked stale because USB host support is unavailable and the prior session is no longer valid.")
            return buildUsbHardwareSession(
                context = context,
                selectedHardwareName = selectedHardwareName,
                connectionInfo = UsbConnectionInfo(
                    state = HardwareConnectionState.ERROR,
                    deviceName = "USB host unsupported",
                    permissionGranted = false,
                    portInUse = false,
                    sessionOpen = false,
                    vendorId = null,
                    productId = null,
                    interfaceCount = 0,
                    debugSummary = "This Android device does not report USB host support."
                )
            )
        }

        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager

        if (usbManager == null) {
            invalidateActiveSession("Calibration was marked stale because the Android UsbManager service is unavailable and the prior session can no longer be trusted.")
            return buildUsbHardwareSession(
                context = context,
                selectedHardwareName = selectedHardwareName,
                connectionInfo = UsbConnectionInfo(
                    state = HardwareConnectionState.ERROR,
                    deviceName = "UsbManager unavailable",
                    permissionGranted = false,
                    portInUse = false,
                    sessionOpen = false,
                    vendorId = null,
                    productId = null,
                    interfaceCount = 0,
                    debugSummary = "Android UsbManager service was not available on this device."
                )
            )
        }

        val deviceList = usbManager.deviceList.values.toList()

        if (deviceList.isEmpty()) {
            invalidateActiveSession("Calibration was marked stale because the previously tracked USB instrument is no longer attached.")
            return buildUsbHardwareSession(
                context = context,
                selectedHardwareName = selectedHardwareName,
                connectionInfo = UsbConnectionInfo(
                    state = HardwareConnectionState.NOT_CONNECTED,
                    deviceName = "No USB device detected",
                    permissionGranted = false,
                    portInUse = false,
                    sessionOpen = false,
                    vendorId = null,
                    productId = null,
                    interfaceCount = 0,
                    debugSummary = "USB state refreshed. No attached USB devices were reported. Any previously open session was invalidated."
                )
            )
        }

        val trackedActiveDevice = findTrackedActiveDevice(deviceList)

        if (activeConnection != null && trackedActiveDevice == null) {
            invalidateActiveSession("Calibration was marked stale because the previously tracked instrument session disappeared.")
        }

        if (activeConnection != null && trackedActiveDevice != null) {
            val activePermissionGranted = usbManager.hasPermission(trackedActiveDevice)
            if (!activePermissionGranted) {
                invalidateActiveSession("Calibration was marked stale because USB permission was lost for the active instrument session.")
            }
        }

        val firstDevice = deviceList.first()
        val firstDevicePermissionGranted = usbManager.hasPermission(firstDevice)

        if (activeConnection != null) {
            val activeDevice = findTrackedActiveDevice(deviceList)

            if (activeDevice == null) {
                invalidateActiveSession("Calibration was marked stale because the tracked active instrument could not be refreshed.")
            } else {
                ensureActiveTransportChannelFor(
                    device = activeDevice,
                    connection = activeConnection
                )

                return buildUsbHardwareSession(
                    context = context,
                    selectedHardwareName = selectedHardwareName,
                    connectionInfo = buildConnectionInfo(
                        device = activeDevice,
                        state = HardwareConnectionState.READY,
                        permissionGranted = true,
                        portInUse = false,
                        sessionOpen = true,
                        debugSummary = "USB state refreshed. Active session is still open for the tracked device. ${buildInterfaceSummary(activeDevice)} ${buildTransportChannelSummary()}"
                    )
                )
            }
        }

        if (!firstDevicePermissionGranted) {
            clearCachedInstrumentTruthOnly()

            return buildUsbHardwareSession(
                context = context,
                selectedHardwareName = selectedHardwareName,
                connectionInfo = buildConnectionInfo(
                    device = firstDevice,
                    state = HardwareConnectionState.PERMISSION_REQUIRED,
                    permissionGranted = false,
                    portInUse = false,
                    sessionOpen = false,
                    debugSummary = "USB state refreshed. Device is attached but permission is still required before opening a session. ${buildInterfaceSummary(firstDevice)}"
                )
            )
        }

        return buildUsbHardwareSession(
            context = context,
            selectedHardwareName = selectedHardwareName,
            connectionInfo = buildConnectionInfo(
                device = firstDevice,
                state = HardwareConnectionState.DEVICE_DETECTED,
                permissionGranted = true,
                portInUse = false,
                sessionOpen = false,
                debugSummary = "USB state refreshed. Device is attached and permission is granted, but no active session is open. ${buildInterfaceSummary(firstDevice)}"
            )
        )
    }

    fun openFirstDetectedSession(
        context: Context,
        selectedHardwareName: String
    ): UsbHardwareSession {
        val hasUsbHostFeature =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)

        if (!hasUsbHostFeature) {
            invalidateActiveSession("Calibration was marked stale because USB host support is unavailable and no valid instrument session can be opened.")
            return buildUsbHardwareSession(
                context = context,
                selectedHardwareName = selectedHardwareName,
                connectionInfo = UsbConnectionInfo(
                    state = HardwareConnectionState.ERROR,
                    deviceName = "USB host unsupported",
                    permissionGranted = false,
                    portInUse = false,
                    sessionOpen = false,
                    vendorId = null,
                    productId = null,
                    interfaceCount = 0,
                    debugSummary = "This Android device does not report USB host support."
                )
            )
        }

        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager

        if (usbManager == null) {
            invalidateActiveSession("Calibration was marked stale because the Android UsbManager service is unavailable and no valid instrument session can be opened.")
            return buildUsbHardwareSession(
                context = context,
                selectedHardwareName = selectedHardwareName,
                connectionInfo = UsbConnectionInfo(
                    state = HardwareConnectionState.ERROR,
                    deviceName = "UsbManager unavailable",
                    permissionGranted = false,
                    portInUse = false,
                    sessionOpen = false,
                    vendorId = null,
                    productId = null,
                    interfaceCount = 0,
                    debugSummary = "Android UsbManager service was not available on this device."
                )
            )
        }

        val deviceList = usbManager.deviceList.values.toList()

        if (deviceList.isEmpty()) {
            invalidateActiveSession("Calibration was marked stale because there is no attached USB instrument to keep the prior session valid.")
            return buildUsbHardwareSession(
                context = context,
                selectedHardwareName = selectedHardwareName,
                connectionInfo = UsbConnectionInfo(
                    state = HardwareConnectionState.NOT_CONNECTED,
                    deviceName = "No USB device detected",
                    permissionGranted = false,
                    portInUse = false,
                    sessionOpen = false,
                    vendorId = null,
                    productId = null,
                    interfaceCount = 0,
                    debugSummary = "USB session open not possible because no attached USB devices were reported."
                )
            )
        }

        if (activeConnection != null) {
            val trackedActiveDevice = findTrackedActiveDevice(deviceList)
            if (trackedActiveDevice == null || !usbManager.hasPermission(trackedActiveDevice)) {
                invalidateActiveSession("Calibration was marked stale because the previous USB session could not be confirmed before opening a session.")
            }
        }

        val firstDevice = deviceList.first()

        if (!usbManager.hasPermission(firstDevice)) {
            clearCachedInstrumentTruthOnly()
            return buildUsbHardwareSession(
                context = context,
                selectedHardwareName = selectedHardwareName,
                connectionInfo = buildConnectionInfo(
                    device = firstDevice,
                    state = HardwareConnectionState.PERMISSION_REQUIRED,
                    permissionGranted = false,
                    portInUse = false,
                    sessionOpen = false,
                    debugSummary = "USB permission is still required before a device session can be opened."
                )
            )
        }

        if (activeConnection != null &&
            activeDeviceId == firstDevice.deviceId &&
            activeDeviceName == firstDevice.deviceName
        ) {
            ensureActiveTransportChannelFor(
                device = firstDevice,
                connection = activeConnection
            )

            val existingSession = buildUsbHardwareSession(
                context = context,
                selectedHardwareName = selectedHardwareName,
                connectionInfo = buildConnectionInfo(
                    device = firstDevice,
                    state = HardwareConnectionState.READY,
                    permissionGranted = true,
                    portInUse = false,
                    sessionOpen = true,
                    debugSummary = buildOpenSessionSummary(firstDevice)
                )
            )

            startBackgroundIdentityProbe(
                context = context,
                selectedHardwareName = selectedHardwareName
            )

            return existingSession
        }

        if (activeConnection != null) {
            return buildUsbHardwareSession(
                context = context,
                selectedHardwareName = selectedHardwareName,
                connectionInfo = buildConnectionInfo(
                    device = firstDevice,
                    state = HardwareConnectionState.BUSY,
                    permissionGranted = true,
                    portInUse = true,
                    sessionOpen = false,
                    debugSummary = "Another USB device session is already open in this process. Close it before opening a new one."
                )
            )
        }

        val openedConnection = usbManager.openDevice(firstDevice)

        if (openedConnection == null) {
            clearCachedInstrumentTruthOnly()
            clearActiveTransportChannel()

            return buildUsbHardwareSession(
                context = context,
                selectedHardwareName = selectedHardwareName,
                connectionInfo = buildConnectionInfo(
                    device = firstDevice,
                    state = HardwareConnectionState.ERROR,
                    permissionGranted = true,
                    portInUse = false,
                    sessionOpen = false,
                    debugSummary = "UsbManager.openDevice() returned null. The device could not be opened."
                )
            )
        }

        activeConnection = openedConnection
        activeDeviceName = firstDevice.deviceName
        activeDeviceId = firstDevice.deviceId
        activeSessionGeneration += 1

        ensureActiveTransportChannelFor(
            device = firstDevice,
            connection = openedConnection
        )

        latestInstrumentCalibrationState =
            refreshCalibrationStateForCurrentSession(
                connectionState = HardwareConnectionState.READY,
                sessionOpen = true,
                activeSessionKey = buildActiveSessionKey()
            )

        val openedSession = buildUsbHardwareSession(
            context = context,
            selectedHardwareName = selectedHardwareName,
            connectionInfo = buildConnectionInfo(
                device = firstDevice,
                state = HardwareConnectionState.READY,
                permissionGranted = true,
                portInUse = false,
                sessionOpen = true,
                debugSummary = buildOpenSessionSummary(firstDevice)
            )
        )

        startBackgroundIdentityProbe(
            context = context,
            selectedHardwareName = selectedHardwareName
        )

        ensureLiteVnaBringUpStarted(
            context = context,
            selectedHardwareName = selectedHardwareName,
            sessionOpen = true,
            transportReady = isTransportReady()
        )

        return openedSession
    }

    fun closeSession(
        context: Context,
        selectedHardwareName: String
    ): UsbHardwareSession {
        invalidateActiveSession("Calibration was marked stale because the active USB session was closed.")

        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager

        if (usbManager == null) {
            return buildUsbHardwareSession(
                context = context,
                selectedHardwareName = selectedHardwareName,
                connectionInfo = UsbConnectionInfo(
                    state = HardwareConnectionState.ERROR,
                    deviceName = "UsbManager unavailable",
                    permissionGranted = false,
                    portInUse = false,
                    sessionOpen = false,
                    vendorId = null,
                    productId = null,
                    interfaceCount = 0,
                    debugSummary = "Android UsbManager service was not available while closing the USB session."
                )
            )
        }

        val deviceList = usbManager.deviceList.values.toList()

        if (deviceList.isEmpty()) {
            return buildUsbHardwareSession(
                context = context,
                selectedHardwareName = selectedHardwareName,
                connectionInfo = UsbConnectionInfo(
                    state = HardwareConnectionState.NOT_CONNECTED,
                    deviceName = "No USB device detected",
                    permissionGranted = false,
                    portInUse = false,
                    sessionOpen = false,
                    vendorId = null,
                    productId = null,
                    interfaceCount = 0,
                    debugSummary = "USB session closed. No attached USB devices are currently reported."
                )
            )
        }

        val firstDevice = deviceList.first()
        val permissionGranted = usbManager.hasPermission(firstDevice)

        return buildUsbHardwareSession(
            context = context,
            selectedHardwareName = selectedHardwareName,
            connectionInfo = buildConnectionInfo(
                device = firstDevice,
                state = if (permissionGranted) HardwareConnectionState.DEVICE_DETECTED else HardwareConnectionState.PERMISSION_REQUIRED,
                permissionGranted = permissionGranted,
                portInUse = false,
                sessionOpen = false,
                debugSummary = "USB session closed. Device is still attached but no active session is open. ${buildInterfaceSummary(firstDevice)}"
            )
        )
    }

    private fun buildUsbHardwareSession(
        context: Context,
        selectedHardwareName: String,
        connectionInfo: UsbConnectionInfo
    ): UsbHardwareSession {
        val instrumentSessionState = composeInstrumentSessionState(
            selectedHardwareName = selectedHardwareName,
            connectionInfo = connectionInfo
        )

        latestInstrumentSessionState = instrumentSessionState

        ensureLiteVnaBringUpStarted(
            context = context,
            selectedHardwareName = selectedHardwareName,
            sessionOpen = connectionInfo.sessionOpen,
            transportReady = instrumentSessionState.transportReady
        )

        return UsbHardwareSession(
            selectedHardwareName = selectedHardwareName,
            connectionInfo = connectionInfo,
            instrumentSessionState = instrumentSessionState
        )
    }

    private fun composeInstrumentSessionState(
        selectedHardwareName: String,
        connectionInfo: UsbConnectionInfo
    ): InstrumentSessionState {
        val chosenProfile = selectedDriverProfile

        val cachedIdentityResult = latestAnalyzerIdentityResult
        val driverResolution = cachedIdentityResult?.let { identityResult ->
            if (identityResult.success) {
                UsbVnaDriverRegistry.resolveDriver(identityResult = identityResult)
            } else {
                null
            }
        }

        val discoverySnapshot = driverResolution?.discoverySnapshot
        val transportReady = connectionInfo.sessionOpen && activeTransportChannel != null
        val activeSessionKey = buildActiveSessionKey()

        latestInstrumentCalibrationState = refreshCalibrationStateForCurrentSession(
            connectionState = connectionInfo.state,
            sessionOpen = connectionInfo.sessionOpen,
            activeSessionKey = activeSessionKey
        )

        val calibrationState = latestInstrumentCalibrationState

        val liteProfileSelected =
            chosenProfile?.protocolType == DriverProtocolType.LITE_VNA_V2_STYLE

        val liteBringUpSucceeded =
            latestLiteVnaBringUpResult?.success == true

        val liteIdentitySucceeded =
            latestLiteVnaIdentityResult?.success == true

        val liteCommandTestSucceeded =
            latestLiteVnaCommandTestResult?.success == true

        val liteValidationConfirmed =
            liteBringUpSucceeded &&
                    liteIdentitySucceeded &&
                    liteCommandTestSucceeded

        val litePartialSupportAvailable =
            liteProfileSelected &&
                    connectionInfo.sessionOpen &&
                    transportReady &&
                    liteValidationConfirmed

        val baseMeasurementTrust = when {
            litePartialSupportAvailable -> MeasurementTrustLevel.PARTIAL
            discoverySnapshot != null -> mapDiscoveryTrustToMeasurementTrust(discoverySnapshot)
            latestTransportHealthSnapshot.lastCommandSucceeded && transportReady -> MeasurementTrustLevel.PARTIAL
            connectionInfo.sessionOpen && transportReady -> MeasurementTrustLevel.PARTIAL
            connectionInfo.state == HardwareConnectionState.NOT_CONNECTED -> MeasurementTrustLevel.UNKNOWN
            else -> MeasurementTrustLevel.SIMULATED
        }

        val measurementTrust = applyCalibrationTrustAdjustment(
            baseTrust = baseMeasurementTrust,
            calibrationState = calibrationState,
            connectionInfo = connectionInfo,
            transportReady = transportReady
        )

        val protocolFamily = when {
            litePartialSupportAvailable -> "LiteVNA"
            discoverySnapshot != null -> discoverySnapshot.protocolFamilyDisplayName
            chosenProfile != null -> chosenProfile.hardwareFamily.name
            else -> deriveFallbackProtocolFamily(
                connectionState = connectionInfo.state,
                sessionOpen = connectionInfo.sessionOpen
            )
        }

        val protocolGuess = when {
            litePartialSupportAvailable -> "LiteVNA"
            discoverySnapshot != null -> discoverySnapshot.protocolGuess
            cachedIdentityResult != null -> cachedIdentityResult.protocolIdentity.displayName
            chosenProfile != null -> chosenProfile.protocolType.name
            else -> protocolFamily
        }

        val supportTier = when {
            litePartialSupportAvailable -> "Partial Support"
            discoverySnapshot != null -> discoverySnapshot.supportTier.displayName
            chosenProfile != null -> chosenProfile.supportTier.name
            else -> deriveFallbackSupportTier(
                connectionState = connectionInfo.state,
                sessionOpen = connectionInfo.sessionOpen
            )
        }

        val resolvedDriverId = when {
            litePartialSupportAvailable -> chosenProfile?.id
            else -> discoverySnapshot?.driverId ?: chosenProfile?.id
        }

        val capabilityProfile =
            driverResolution?.driver?.getCapabilityProfile()?.toHardwareMeasurementCapabilities()
                ?: deriveProfileCapabilityFallback(
                    profile = chosenProfile,
                    connectionState = connectionInfo.state,
                    permissionGranted = connectionInfo.permissionGranted,
                    sessionOpen = connectionInfo.sessionOpen
                )

        val measurementTrustSummary = buildMeasurementTrustSummary(
            discoverySummary = when {
                litePartialSupportAvailable ->
                    "LiteVNA validation and provisional sweep path are available for the current live USB session."
                else ->
                    discoverySnapshot?.measurementTrustSummary
            },
            connectionInfo = connectionInfo,
            transportReady = transportReady,
            calibrationState = calibrationState
        )

        val dataSourceKind = when {
            litePartialSupportAvailable -> InstrumentDataSourceKind.REAL_INSTRUMENT
            driverResolution?.canExecuteRealSweep() == true &&
                    connectionInfo.sessionOpen &&
                    transportReady -> InstrumentDataSourceKind.REAL_INSTRUMENT
            connectionInfo.state == HardwareConnectionState.NOT_CONNECTED -> InstrumentDataSourceKind.NONE
            else -> InstrumentDataSourceKind.SIMULATED
        }

        val transportSummary = buildTransportStatusSummary(
            connectionInfo = connectionInfo,
            transportReady = transportReady
        )

        val discoverySummary =
            when {
                litePartialSupportAvailable ->
                    buildString {
                        append("LiteVNA provisional live path is active. ")
                        append(latestLiteVnaCommandTestResult?.summary ?: "Validation complete.")
                    }
                discoverySnapshot != null ->
                    discoverySnapshot.summary
                cachedIdentityResult != null ->
                    cachedIdentityResult.summary
                chosenProfile != null ->
                    chosenProfile.notes
                else ->
                    "No discovery summary available yet."
            }

        val instrumentIdentityText =
            when {
                latestLiteVnaIdentityResult != null &&
                        latestLiteVnaIdentityResult!!.rawIdentityText.isNotBlank() ->
                    latestLiteVnaIdentityResult!!.rawIdentityText
                discoverySnapshot != null ->
                    discoverySnapshot.instrumentIdentity
                cachedIdentityResult != null ->
                    cachedIdentityResult.rawIdentityText
                else ->
                    chosenProfile?.displayName
            }

        return InstrumentSessionState(
            selectedHardwareName = selectedHardwareName,
            connectionInfo = connectionInfo,
            transportReady = transportReady,
            transportStatusSummary = transportSummary,
            protocolFamily = protocolFamily,
            protocolGuess = protocolGuess,
            supportTier = supportTier,
            resolvedDriverId = resolvedDriverId,
            capabilityProfile = capabilityProfile,
            measurementTrust = measurementTrust,
            measurementTrustSummary = measurementTrustSummary,
            calibrationState = calibrationState,
            calibrationStatusSummary = calibrationState.statusSummary,
            dataSourceKind = dataSourceKind,
            discoverySummary = discoverySummary,
            instrumentIdentityText = instrumentIdentityText,
            sessionSummary = buildInstrumentSessionSummary(
                connectionInfo = connectionInfo,
                transportReady = transportReady,
                protocolFamily = protocolFamily,
                protocolGuess = protocolGuess,
                supportTier = supportTier,
                resolvedDriverId = resolvedDriverId,
                measurementTrust = measurementTrust,
                measurementTrustSummary = measurementTrustSummary,
                calibrationState = calibrationState,
                dataSourceKind = dataSourceKind,
                transportStatusSummary = transportSummary
            )
        )
    }

    private fun mapDiscoveryTrustToMeasurementTrust(
        discoverySnapshot: UsbVnaDiscoverySnapshot
    ): MeasurementTrustLevel {
        return when (discoverySnapshot.supportTier) {
            InstrumentSupportTier.FULL_SUPPORT -> MeasurementTrustLevel.TRUSTED
            InstrumentSupportTier.PARTIAL_SUPPORT -> MeasurementTrustLevel.PARTIAL
            InstrumentSupportTier.DETECTED -> MeasurementTrustLevel.UNKNOWN
        }
    }

    private fun UsbVnaDriverCapabilityProfile.toHardwareMeasurementCapabilities(): HardwareMeasurementCapabilities {
        return HardwareMeasurementCapabilities(
            supportsS11 = supportsS11,
            supportsS11Phase = supportsS11Phase,
            supportsS21 = supportsS21,
            supportsS21Phase = supportsS21Phase
        )
    }

    private fun deriveProfileCapabilityFallback(
        profile: DriverProfile?,
        connectionState: HardwareConnectionState,
        permissionGranted: Boolean,
        sessionOpen: Boolean
    ): HardwareMeasurementCapabilities {
        return when (profile?.hardwareFamily) {
            HardwareFamily.NANO_SHELL_FAMILY,
            HardwareFamily.LITE_VNA_FAMILY -> HardwareCapabilityProfiles.UNKNOWN_MINIMAL
            HardwareFamily.EXPERIMENTAL_SERIAL_VNA -> HardwareCapabilityProfiles.UNKNOWN_MINIMAL
            null -> deriveFallbackCapabilityProfile(
                connectionState = connectionState,
                permissionGranted = permissionGranted,
                sessionOpen = sessionOpen
            )
        }
    }

    private fun applyCalibrationTrustAdjustment(
        baseTrust: MeasurementTrustLevel,
        calibrationState: InstrumentCalibrationState,
        connectionInfo: UsbConnectionInfo,
        transportReady: Boolean
    ): MeasurementTrustLevel {
        if (!connectionInfo.sessionOpen || !transportReady) {
            return baseTrust
        }

        return when (calibrationState.readiness) {
            CalibrationReadiness.VALID -> baseTrust
            CalibrationReadiness.NOT_STARTED,
            CalibrationReadiness.IN_PROGRESS,
            CalibrationReadiness.STALE,
            CalibrationReadiness.INVALID -> {
                when (baseTrust) {
                    MeasurementTrustLevel.TRUSTED -> MeasurementTrustLevel.DEGRADED
                    MeasurementTrustLevel.PARTIAL -> MeasurementTrustLevel.DEGRADED
                    else -> baseTrust
                }
            }
        }
    }

    private fun buildMeasurementTrustSummary(
        discoverySummary: String?,
        connectionInfo: UsbConnectionInfo,
        transportReady: Boolean,
        calibrationState: InstrumentCalibrationState
    ): String {
        val baseSummary = discoverySummary
            ?: buildFallbackMeasurementTrustSummary(
                connectionInfo = connectionInfo,
                transportReady = transportReady
            )

        return "$baseSummary Calibration: ${calibrationState.statusSummary}"
    }

    private fun buildFallbackMeasurementTrustSummary(
        connectionInfo: UsbConnectionInfo,
        transportReady: Boolean
    ): String {
        return when {
            latestTransportHealthSnapshot.lastCommandSucceeded && transportReady ->
                latestTransportHealthSnapshot.lastOperationSummary
            connectionInfo.sessionOpen && transportReady ->
                "Session is open and a transport channel is prepared, but analyzer identity has not been verified yet."
            connectionInfo.state == HardwareConnectionState.PERMISSION_REQUIRED ->
                "Permission is required before measurement trust can be established."
            connectionInfo.state == HardwareConnectionState.NOT_CONNECTED ->
                "No instrument is currently connected."
            else ->
                "Measurements are currently simulated until live instrument verification completes."
        }
    }

    private fun buildTransportStatusSummary(
        connectionInfo: UsbConnectionInfo,
        transportReady: Boolean
    ): String {
        return when {
            transportReady && latestTransportHealthSnapshot.lastOperationSummary.isNotBlank() ->
                "Transport prepared. ${latestTransportHealthSnapshot.lastOperationSummary}"
            transportReady ->
                "Transport prepared. No transport activity summary is available yet."
            connectionInfo.sessionOpen ->
                "USB session is open, but no active transport channel has been prepared."
            connectionInfo.state == HardwareConnectionState.PERMISSION_REQUIRED ->
                "USB transport is not ready because permission is still required."
            connectionInfo.state == HardwareConnectionState.NOT_CONNECTED ->
                "USB transport is not ready because no USB device is currently connected."
            connectionInfo.state == HardwareConnectionState.ERROR ->
                "USB transport is not ready because the USB session is in an error state."
            else ->
                "USB transport is not ready because no active session is open."
        }
    }

    private fun refreshCalibrationStateForCurrentSession(
        connectionState: HardwareConnectionState,
        sessionOpen: Boolean,
        activeSessionKey: String?
    ): InstrumentCalibrationState {
        val currentState = latestInstrumentCalibrationState

        if (!sessionOpen || activeSessionKey == null) {
            return when (currentState.readiness) {
                CalibrationReadiness.VALID,
                CalibrationReadiness.IN_PROGRESS -> currentState.copy(
                    readiness = CalibrationReadiness.STALE,
                    activeSessionKey = null,
                    statusSummary = "Calibration was captured earlier, but it is now stale because the instrument session is no longer open.",
                    operatorWarning = "Calibration is stale. Sweeps may continue, but trust is downgraded until calibration is recaptured for the active session.",
                    sweepAllowed = true,
                    trustDowngraded = true
                )
                else -> currentState.copy(activeSessionKey = null)
            }
        }

        if (currentState.readiness == CalibrationReadiness.NOT_STARTED &&
            currentState.calibrationSession == null
        ) {
            return currentState.copy(
                activeSessionKey = activeSessionKey,
                statusSummary = "No calibration has been captured for the current instrument session.",
                operatorWarning = "Measurements are currently uncalibrated and should be treated with reduced trust.",
                sweepAllowed = true,
                trustDowngraded = true
            )
        }

        val calibrationSession = currentState.calibrationSession
        val capturedSessionKey = currentState.sessionKeyAtCapture

        if (capturedSessionKey != null && capturedSessionKey != activeSessionKey) {
            return currentState.copy(
                readiness = CalibrationReadiness.STALE,
                activeSessionKey = activeSessionKey,
                statusSummary = "Calibration was captured for a previous instrument session and is now stale for the current session.",
                operatorWarning = "Calibration belongs to an older session. Sweeps may continue, but trust is downgraded until calibration is recaptured.",
                sweepAllowed = true,
                trustDowngraded = true
            )
        }

        if (calibrationSession != null) {
            val currentProtocolFamily = latestInstrumentSessionState?.protocolFamily
                ?: selectedDriverProfile?.protocolType?.name

            val currentInstrumentIdentity = latestInstrumentSessionState?.instrumentIdentityText
                ?: latestAnalyzerIdentityResult?.rawIdentityText

            if (!calibrationSession.matchesProtocolFamily(currentProtocolFamily) ||
                !calibrationSession.matchesInstrumentIdentity(currentInstrumentIdentity)
            ) {
                return currentState.copy(
                    readiness = CalibrationReadiness.STALE,
                    activeSessionKey = activeSessionKey,
                    statusSummary = "Calibration no longer matches the active instrument protocol/identity truth for this session.",
                    operatorWarning = "Calibration looks stale because the live instrument identity or protocol truth changed.",
                    sweepAllowed = true,
                    trustDowngraded = true
                )
            }
        }

        return when (currentState.readiness) {
            CalibrationReadiness.VALID -> currentState.copy(
                activeSessionKey = activeSessionKey,
                statusSummary = "Complete calibration is available for the current instrument session.",
                operatorWarning = "Calibration is valid for the current session.",
                sweepAllowed = true,
                trustDowngraded = false
            )
            CalibrationReadiness.IN_PROGRESS -> currentState.copy(
                activeSessionKey = activeSessionKey,
                statusSummary = "Calibration is only partially captured for the current instrument session.",
                operatorWarning = "Calibration is incomplete. Sweeps may continue, but trust is downgraded until OPEN, SHORT, and LOAD are all captured.",
                sweepAllowed = true,
                trustDowngraded = true
            )
            CalibrationReadiness.STALE -> currentState.copy(
                activeSessionKey = activeSessionKey,
                sweepAllowed = true,
                trustDowngraded = true
            )
            CalibrationReadiness.INVALID -> currentState.copy(
                activeSessionKey = activeSessionKey,
                sweepAllowed = true,
                trustDowngraded = true
            )
            CalibrationReadiness.NOT_STARTED -> currentState.copy(
                activeSessionKey = activeSessionKey,
                statusSummary = when (connectionState) {
                    HardwareConnectionState.READY ->
                        "No calibration has been captured for the current instrument session."
                    else ->
                        "No active calibrated instrument session is currently available."
                },
                operatorWarning = "Measurements are currently uncalibrated and should be treated with reduced trust.",
                sweepAllowed = true,
                trustDowngraded = true
            )
        }
    }

    private fun markCalibrationStale(
        staleReason: String
    ) {
        val currentState = latestInstrumentCalibrationState

        latestInstrumentCalibrationState =
            if (currentState.calibrationSession != null ||
                currentState.readiness != CalibrationReadiness.NOT_STARTED
            ) {
                currentState.copy(
                    readiness = CalibrationReadiness.STALE,
                    activeSessionKey = null,
                    statusSummary = staleReason,
                    operatorWarning = "Calibration is stale. Sweeps may continue, but trust is downgraded until calibration is recaptured for the next active session.",
                    sweepAllowed = true,
                    trustDowngraded = true
                )
            } else {
                currentState.copy(activeSessionKey = null)
            }
    }

    private fun buildActiveSessionKey(): String? {
        val deviceId = activeDeviceId ?: return null
        val deviceName = activeDeviceName ?: return null
        return "USB-$deviceId-$deviceName-G$activeSessionGeneration"
    }

    private data class TransportCandidate(
        val usbInterface: UsbInterface,
        val bulkInEndpoint: UsbEndpoint,
        val bulkOutEndpoint: UsbEndpoint,
        val transportKind: UsbTransportKind,
        val transportLabel: String,
        val score: Int
    )

    private fun invalidateActiveSession(
        staleReason: String
    ) {
        if (activeConnection != null) {
            markCalibrationStale(staleReason)
        }

        runCatching { activeConnection?.close() }

        activeConnection = null
        activeDeviceName = null
        activeDeviceId = null
        clearActiveTransportChannel()
        clearCachedInstrumentTruthOnly()
    }

    private fun clearCachedInstrumentTruthOnly() {
        latestAnalyzerIdentityResult = null
        latestTransportHealthSnapshot = UsbVnaTransportHealthSnapshot()
        latestInstrumentSessionState = null
        clearLiteVnaBringUpResults()
    }

    private fun clearLiteVnaBringUpResults() {
        latestLiteVnaBringUpResult = null
        latestLiteVnaIdentityResult = null
        latestLiteVnaCommandTestResult = null
        latestLiteVnaBringUpSessionKey = null
        liteVnaBringUpInFlight = false
    }

    private fun clearActiveTransportChannel() {
        activeTransportChannel = null
    }

    private fun findTrackedActiveDevice(
        deviceList: List<UsbDevice>
    ): UsbDevice? {
        val trackedDeviceId = activeDeviceId
        val trackedDeviceName = activeDeviceName

        if (trackedDeviceId == null || trackedDeviceName == null) {
            return null
        }

        return deviceList.firstOrNull { device ->
            device.deviceId == trackedDeviceId &&
                    device.deviceName == trackedDeviceName
        }
    }

    private fun ensureActiveTransportChannelFor(
        device: UsbDevice,
        connection: UsbDeviceConnection?
    ) {
        activeTransportChannel =
            if (connection == null) null
            else findBestTransportChannel(device = device, connection = connection)
    }

    private fun findBestTransportChannel(
        device: UsbDevice,
        connection: UsbDeviceConnection
    ): UsbTransportChannel? {
        val candidates = mutableListOf<TransportCandidate>()

        for (interfaceIndex in 0 until device.interfaceCount) {
            val usbInterface = device.getInterface(interfaceIndex)
            buildTransportCandidate(usbInterface)?.let { candidates.add(it) }
        }

        if (candidates.isEmpty()) return null

        val orderedCandidates =
            candidates.sortedWith(
                compareByDescending<TransportCandidate> { it.score }
                    .thenByDescending { it.usbInterface.endpointCount }
            )

        orderedCandidates.forEach { candidate ->
            val claimed = connection.claimInterface(candidate.usbInterface, true)
            if (claimed) {
                return UsbTransportChannel(
                    connection = connection,
                    usbInterface = candidate.usbInterface,
                    bulkInEndpoint = candidate.bulkInEndpoint,
                    bulkOutEndpoint = candidate.bulkOutEndpoint,
                    maxReadPacketSize = candidate.bulkInEndpoint.maxPacketSize,
                    maxWritePacketSize = candidate.bulkOutEndpoint.maxPacketSize,
                    transportKind = candidate.transportKind,
                    transportLabel = candidate.transportLabel
                )
            }
        }

        return null
    }

    private fun buildTransportCandidate(
        usbInterface: UsbInterface
    ): TransportCandidate? {
        var bulkInEndpoint: UsbEndpoint? = null
        var bulkOutEndpoint: UsbEndpoint? = null

        for (endpointIndex in 0 until usbInterface.endpointCount) {
            val endpoint = usbInterface.getEndpoint(endpointIndex)
            if (endpoint.type != UsbConstants.USB_ENDPOINT_XFER_BULK) continue

            when (endpoint.direction) {
                UsbConstants.USB_DIR_IN -> if (bulkInEndpoint == null) bulkInEndpoint = endpoint
                UsbConstants.USB_DIR_OUT -> if (bulkOutEndpoint == null) bulkOutEndpoint = endpoint
            }
        }

        if (bulkInEndpoint == null || bulkOutEndpoint == null) return null

        val interfaceClass = usbInterface.interfaceClass
        val interfaceSubclass = usbInterface.interfaceSubclass
        val interfaceProtocol = usbInterface.interfaceProtocol

        val isLikelyCdcData =
            interfaceClass == UsbConstants.USB_CLASS_CDC_DATA ||
                    interfaceClass == UsbConstants.USB_CLASS_COMM

        val transportKind =
            if (isLikelyCdcData) UsbTransportKind.CDC_DATA else UsbTransportKind.BULK_GENERIC

        val transportLabel =
            if (isLikelyCdcData) "USB CDC Data" else "Generic USB Bulk"

        val score =
            when {
                interfaceClass != UsbConstants.USB_CLASS_CDC_DATA &&
                        interfaceClass != UsbConstants.USB_CLASS_COMM -> 400
                interfaceClass == UsbConstants.USB_CLASS_CDC_DATA -> 200
                interfaceClass == UsbConstants.USB_CLASS_COMM -> 150
                else -> 100
            } + usbInterface.endpointCount +
                    (if (interfaceSubclass != 0) 3 else 0) +
                    (if (interfaceProtocol != 0) 2 else 0)

        return TransportCandidate(
            usbInterface = usbInterface,
            bulkInEndpoint = bulkInEndpoint,
            bulkOutEndpoint = bulkOutEndpoint,
            transportKind = transportKind,
            transportLabel = transportLabel,
            score = score
        )
    }

    private fun buildConnectionInfo(
        device: UsbDevice,
        state: HardwareConnectionState,
        permissionGranted: Boolean,
        portInUse: Boolean,
        sessionOpen: Boolean,
        debugSummary: String
    ): UsbConnectionInfo {
        return UsbConnectionInfo(
            state = state,
            deviceName = device.deviceName ?: "Unnamed USB device",
            permissionGranted = permissionGranted,
            portInUse = portInUse,
            sessionOpen = sessionOpen,
            vendorId = device.vendorId,
            productId = device.productId,
            interfaceCount = device.interfaceCount,
            debugSummary = debugSummary
        )
    }

    private fun deriveFallbackProtocolFamily(
        connectionState: HardwareConnectionState,
        sessionOpen: Boolean
    ): String {
        return when {
            sessionOpen -> "Unresolved"
            connectionState == HardwareConnectionState.NOT_CONNECTED -> "No Instrument"
            connectionState == HardwareConnectionState.PERMISSION_REQUIRED -> "Permission Required"
            connectionState == HardwareConnectionState.ERROR -> "Error"
            else -> "Unknown"
        }
    }

    private fun deriveFallbackSupportTier(
        connectionState: HardwareConnectionState,
        sessionOpen: Boolean
    ): String {
        return when {
            sessionOpen -> "Transport Pending"
            connectionState == HardwareConnectionState.NOT_CONNECTED -> "No Instrument"
            connectionState == HardwareConnectionState.PERMISSION_REQUIRED -> "Detected"
            connectionState == HardwareConnectionState.DEVICE_DETECTED -> "Detected"
            connectionState == HardwareConnectionState.BUSY -> "Busy"
            connectionState == HardwareConnectionState.ERROR -> "Error"
            else -> "Detected"
        }
    }

    private fun deriveFallbackCapabilityProfile(
        connectionState: HardwareConnectionState,
        permissionGranted: Boolean,
        sessionOpen: Boolean
    ): HardwareMeasurementCapabilities {
        return when {
            sessionOpen -> HardwareCapabilityProfiles.UNKNOWN_MINIMAL
            permissionGranted -> HardwareCapabilityProfiles.UNKNOWN_MINIMAL
            connectionState == HardwareConnectionState.NOT_CONNECTED -> HardwareMeasurementCapabilities()
            else -> HardwareMeasurementCapabilities()
        }
    }

    private fun buildInstrumentSessionSummary(
        connectionInfo: UsbConnectionInfo,
        transportReady: Boolean,
        protocolFamily: String,
        protocolGuess: String,
        supportTier: String,
        resolvedDriverId: String?,
        measurementTrust: MeasurementTrustLevel,
        measurementTrustSummary: String,
        calibrationState: InstrumentCalibrationState,
        dataSourceKind: InstrumentDataSourceKind,
        transportStatusSummary: String
    ): String {
        return "Device=${connectionInfo.deviceName}; sessionOpen=${connectionInfo.sessionOpen}; transportReady=$transportReady; protocol=$protocolFamily; protocolGuess=$protocolGuess; supportTier=$supportTier; driver=${resolvedDriverId ?: "none"}; trust=$measurementTrust; trustSummary=$measurementTrustSummary; calibration=${calibrationState.readiness}; calibrationSummary=${calibrationState.statusSummary}; dataSource=$dataSourceKind; transport=$transportStatusSummary"
    }

    private fun buildOpenSessionSummary(
        device: UsbDevice
    ): String {
        val interfaceSummary = buildInterfaceSummary(device)
        val endpointMatchSummary = buildEndpointMatchSummary(device)
        val transportChannelSummary = buildTransportChannelSummary()

        return "USB device connection opened successfully. Shared session foundation is active. $interfaceSummary $endpointMatchSummary $transportChannelSummary"
    }

    private fun buildTransportChannelSummary(): String {
        val channel = activeTransportChannel
            ?: return "No active transport channel was prepared."

        return "Active transport channel prepared. type=${channel.transportKind} label=${channel.transportLabel} interfaceClass=${channel.usbInterface.interfaceClass} subclass=${channel.usbInterface.interfaceSubclass} protocol=${channel.usbInterface.interfaceProtocol} readPacket=${channel.maxReadPacketSize} writePacket=${channel.maxWritePacketSize}."
    }

    private fun buildInterfaceSummary(
        device: UsbDevice
    ): String {
        if (device.interfaceCount <= 0) {
            return "No USB interfaces were reported by the attached device."
        }

        val interfaceLines = buildList {
            for (interfaceIndex in 0 until device.interfaceCount) {
                val usbInterface = device.getInterface(interfaceIndex)
                add(describeInterface(interfaceIndex, usbInterface))
            }
        }

        return "Interfaces: ${interfaceLines.joinToString(separator = " | ")}"
    }

    private fun buildEndpointMatchSummary(
        device: UsbDevice
    ): String {
        val endpointDetails = mutableListOf<String>()
        var bulkInCount = 0
        var bulkOutCount = 0

        for (interfaceIndex in 0 until device.interfaceCount) {
            val usbInterface = device.getInterface(interfaceIndex)

            for (endpointIndex in 0 until usbInterface.endpointCount) {
                val endpoint = usbInterface.getEndpoint(endpointIndex)

                if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    when (endpoint.direction) {
                        UsbConstants.USB_DIR_IN -> {
                            bulkInCount += 1
                            endpointDetails.add("IF$interfaceIndex EP$endpointIndex BULK IN maxPacket=${endpoint.maxPacketSize}")
                        }
                        UsbConstants.USB_DIR_OUT -> {
                            bulkOutCount += 1
                            endpointDetails.add("IF$interfaceIndex EP$endpointIndex BULK OUT maxPacket=${endpoint.maxPacketSize}")
                        }
                    }
                }
            }
        }

        return if (bulkInCount > 0 && bulkOutCount > 0) {
            "Candidate transport endpoints found. ${endpointDetails.joinToString(separator = " | ")}"
        } else {
            "No full BULK IN/BULK OUT endpoint pair found yet."
        }
    }

    private fun describeInterface(
        interfaceIndex: Int,
        usbInterface: UsbInterface
    ): String {
        val endpointDescriptions = buildList {
            for (endpointIndex in 0 until usbInterface.endpointCount) {
                add(describeEndpoint(endpointIndex, usbInterface.getEndpoint(endpointIndex)))
            }
        }

        return "IF$interfaceIndex class=${usbInterface.interfaceClass} subclass=${usbInterface.interfaceSubclass} protocol=${usbInterface.interfaceProtocol} endpoints=${usbInterface.endpointCount}${if (endpointDescriptions.isNotEmpty()) " [${endpointDescriptions.joinToString()}]" else ""}"
    }

    private fun describeEndpoint(
        endpointIndex: Int,
        endpoint: UsbEndpoint
    ): String {
        val directionText =
            when (endpoint.direction) {
                UsbConstants.USB_DIR_IN -> "IN"
                UsbConstants.USB_DIR_OUT -> "OUT"
                else -> "UNKNOWN"
            }

        val typeText =
            when (endpoint.type) {
                UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "CONTROL"
                UsbConstants.USB_ENDPOINT_XFER_ISOC -> "ISO"
                UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
                UsbConstants.USB_ENDPOINT_XFER_INT -> "INT"
                else -> "UNKNOWN"
            }

        return "EP$endpointIndex $directionText $typeText addr=${endpoint.address} maxPacket=${endpoint.maxPacketSize}"
    }
}