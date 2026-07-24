package com.example.antennalab_v1.domain.testing

/*
########################################################################
FILE: CalibrationSessionFactory.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Calibration

SYSTEM ROLE
Single source of truth for building the CalibrationSession handed to the
calibration wizard, so every entry point (Lab and Project page) captures
over the same frequency span.

WHY A FOCUSED RANGE
The wizard always captures a FIXED number of points per OSL standard
(CalibrationWizardScreen.CALIBRATION_POINT_COUNT = 101). A calibration
built over the full hardware range spreads those 101 points across the
whole band (tens of MHz between points), so the per-frequency error terms
interpolated by CalibrationCorrector at the actual measurement frequency
are a coarse guess. A narrow, target-focused span (target ± 0.5 MHz)
keeps the 101 points dense right where the measurement sweep happens
(SweepGraphScreen sweeps target ± 0.25/0.5 MHz), which is what correction
accuracy needs.

IMPORTANT
No UI/Compose here. Reads shared UsbSessionManager truth for the current
hardware name and any reusable live calibration.
########################################################################
*/

import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.testing.CalibrationReadiness
import com.example.antennalab_v1.model.testing.CalibrationSession

object CalibrationSessionFactory {

    /** Half-width (MHz) of the fresh calibration span on each side of the target. */
    private const val CALIBRATION_HALF_SPAN_MHZ = 0.5

    /*
    ------------------------------------------------------------
    WIZARD SESSION
    ------------------------------------------------------------
    PURPOSE
    Reuse the shared live calibration while it is still usable
    (valid or partially captured); otherwise start a fresh session.
    ------------------------------------------------------------
    */
    fun buildWizardSession(project: ProjectData): CalibrationSession {
        val calibrationState = UsbSessionManager.getLatestInstrumentCalibrationState()
        val sharedCalibration = calibrationState.calibrationSession

        return if (
            sharedCalibration != null &&
            (
                calibrationState.readiness == CalibrationReadiness.VALID ||
                    calibrationState.readiness == CalibrationReadiness.IN_PROGRESS
                )
        ) {
            sharedCalibration
        } else {
            buildFreshSession(project)
        }
    }

    /*
    ------------------------------------------------------------
    FRESH SESSION
    ------------------------------------------------------------
    PURPOSE
    A brand-new, uncaptured session focused on the project's target
    frequency (target ± 0.5 MHz), clamped to the hardware's usable range
    so extreme targets near a band edge can't produce an invalid span.
    ------------------------------------------------------------
    */
    fun buildFreshSession(project: ProjectData): CalibrationSession {
        // Clamp against the instrument that will ACTUALLY capture the standards.
        // This previously used the project's design-time profile while the display
        // name came from the live session — the same session was named after one
        // instrument but bounded by another.
        val effectiveCapabilities =
            EffectiveHardwareResolver.resolveCapabilityProfileForProject(project)

        // Record the CANONICAL capability displayName, not the live session's driver
        // label ("LiteVNA64 HW 64-0.3.3 FW v1.4.06"). A calibration stored under the
        // driver label never matched the capability name it was later compared
        // against, so real calibrations were silently cleared on project load.
        // Legacy sessions already stored under a driver label are still recognised —
        // see EffectiveHardwareResolver.hardwareNameAliases.
        val selectedHardwareName = effectiveCapabilities.displayName

        val hardwareMinMHz = effectiveCapabilities.minFrequencyHz / 1_000_000.0
        val hardwareMaxMHz = effectiveCapabilities.maxFrequencyHz / 1_000_000.0
        val targetMHz = project.designInput.targetFrequencyMHz

        val startFrequencyMHz =
            (targetMHz - CALIBRATION_HALF_SPAN_MHZ).coerceIn(hardwareMinMHz, hardwareMaxMHz)
        val endFrequencyMHz =
            (targetMHz + CALIBRATION_HALF_SPAN_MHZ).coerceIn(hardwareMinMHz, hardwareMaxMHz)

        return CalibrationSession(
            hardwareDisplayName = selectedHardwareName,
            startFrequencyMHz = startFrequencyMHz,
            endFrequencyMHz = endFrequencyMHz,
            openCaptured = false,
            shortCaptured = false,
            loadCaptured = false,
            timestampLabel = "Not captured yet",
            capturedAtEpochMs = 0L,
            capturedProtocolFamily = null,
            capturedInstrumentIdentityText = null,
            capturedSessionKey = null
        )
    }
}
