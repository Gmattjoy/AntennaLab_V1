package com.example.antennalab_v1.domain.testing

import com.example.antennalab_v1.model.ProjectCalibrationData
import com.example.antennalab_v1.model.ProjectData
import com.example.antennalab_v1.model.testing.CalibrationCaptureSource
import com.example.antennalab_v1.model.testing.CalibrationReadiness
import com.example.antennalab_v1.model.testing.InstrumentCalibrationState

/*
########################################################################
FILE: StoredCalibrationProducer.kt
PACKAGE: com.example.antennalab_v1.domain.testing
LAYER: Domain / Testing / Calibration persistence (producer)

WHY THIS EXISTS (§10c.6 — the missing producer)
Every OTHER half of project-scoped calibration persistence already
existed — the model (ProjectCalibrationData), the JSON round-trip
(ProjectStorage), the restore decision (AppRootController
.decideCalibrationRestore), the alias name-match (EffectiveHardwareResolver
.storedNameMatchesHardware) and the restore side-effect
(AppRootScreen.applyStoredCalibrationToSharedSession) — but NOTHING ever
wrote calibrationData.storedCalibrationSession. So A3/Block B were
unrunnable and yesterday's alias fix repaired an unreachable path.

This is that producer. It folds a live VALID calibration into a project's
calibrationData so decideCalibrationRestore can later consume it. Writer
and reader share ONE name vocabulary — the canonical capability
displayName from EffectiveHardwareResolver, the same resolution the
restore path uses — so the round-trip actually matches.

BOUNDARIES
- IN-MEMORY only. captureIntoProject returns an updated ProjectData; it
  does NOT write to disk. Persistence to disk follows the project's normal
  explicit-Save path, exactly like every other project edit (design,
  hardware selection, notes). An operator who calibrates then force-kills
  without Save loses it, consistent with any other unsaved edit.
- VALID only. A STALE / INVALID / partial / uncomputed calibration is
  never persisted (isPersistable). Trust is NOT stored: on restore,
  UsbSessionManager re-derives readiness from the live session, so what we
  store is capture data (O/S/L flags, correction coefficients, span,
  hardware identity), not a trust verdict.

The buildStoredCalibrationData core is JVM-pure and fully unit-tested;
captureIntoProject is the thin shared wrapper both wizard onFinish sites
call so the name resolution is single-sourced, not copied.
########################################################################
*/
object StoredCalibrationProducer {

    /**
     * A live calibration worth persisting: VALID, all three OSL standards
     * captured, and usable correction coefficients computed. Anything less
     * carries no trustworthy correction and must not be stored.
     */
    fun isPersistable(state: InstrumentCalibrationState): Boolean {
        val session = state.calibrationSession ?: return false
        return state.readiness == CalibrationReadiness.VALID &&
            session.isFullyCaptured() &&
            session.hasCorrectionData
    }

    /**
     * The ProjectCalibrationData to store, or null to leave the project
     * untouched. Normalises the stored name to [canonicalHardwareDisplayName]
     * (overwriting the live driver label), strips the volatile live session
     * key (restore re-binds it), stamps time + status, sets
     * restoredFromStorage = false (this is a fresh live capture), and
     * preserves the existing restorePolicy.
     *
     * [nowEpochMs] is injected so this stays pure and testable.
     */
    fun buildStoredCalibrationData(
        liveCalibration: InstrumentCalibrationState,
        canonicalHardwareDisplayName: String,
        nowEpochMs: Long,
        existing: ProjectCalibrationData
    ): ProjectCalibrationData? {
        if (!isPersistable(liveCalibration)) return null
        val session = liveCalibration.calibrationSession ?: return null

        val storedSession = session.copy(
            hardwareDisplayName = canonicalHardwareDisplayName,
            captureSource = CalibrationCaptureSource.WIZARD,
            capturedSessionKey = null
        )

        return existing.copy(
            storedCalibrationSession = storedSession,
            lastCalibrationSavedEpochMs = nowEpochMs,
            lastCalibrationStatusSummary = liveCalibration.statusSummary,
            restoredFromStorage = false
        )
    }

    /**
     * Shared wrapper both wizard onFinish sites call. Resolves the canonical
     * capability displayName ONCE via EffectiveHardwareResolver — the same
     * call the restore path uses — so writer and reader agree on the name.
     * Returns an updated in-memory ProjectData, or the untouched project when
     * the live calibration is not persistable. Never writes to disk.
     */
    fun captureIntoProject(
        project: ProjectData,
        liveCalibration: InstrumentCalibrationState,
        nowEpochMs: Long
    ): ProjectData {
        val canonicalName =
            EffectiveHardwareResolver.resolveCapabilityProfileForProject(project).displayName

        val updated = buildStoredCalibrationData(
            liveCalibration = liveCalibration,
            canonicalHardwareDisplayName = canonicalName,
            nowEpochMs = nowEpochMs,
            existing = project.calibrationData
        ) ?: return project

        return project.copy(calibrationData = updated)
    }
}
