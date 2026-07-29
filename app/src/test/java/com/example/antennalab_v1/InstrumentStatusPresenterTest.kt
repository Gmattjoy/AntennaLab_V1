package com.example.antennalab_v1

import com.example.antennalab_v1.features.app.InstrumentStatusPresenter
import com.example.antennalab_v1.model.testing.CalibrationReadiness
import com.example.antennalab_v1.model.testing.InstrumentDataSourceKind
import com.example.antennalab_v1.model.testing.MeasurementTrustLevel
import com.example.antennalab_v1.ui.components.AppStatusLevel
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The ONE shared state→status-chip mapping consumed by both the dashboard and
 * Device Connections. These assertions are relocated VERBATIM from
 * DashboardControllerTest (same inputs, same expected outputs) so the move from
 * DashboardController.buildStatusChips is provably behaviour-preserving.
 */
class InstrumentStatusPresenterTest {

    @Test
    fun statusChips_liveValidTrusted() {
        val chips = InstrumentStatusPresenter.buildStatusChips(
            InstrumentDataSourceKind.REAL_INSTRUMENT,
            CalibrationReadiness.VALID,
            MeasurementTrustLevel.TRUSTED
        )
        assertEquals(InstrumentStatusPresenter.InstrumentStatusChip("Live", AppStatusLevel.POSITIVE), chips.dataSource)
        assertEquals("App calibration · Valid", chips.calibration.label)
        assertEquals(AppStatusLevel.POSITIVE, chips.calibration.level)
        assertEquals(AppStatusLevel.POSITIVE, chips.trust.level)
    }

    @Test
    fun statusChips_simulatedStaleDegraded() {
        val chips = InstrumentStatusPresenter.buildStatusChips(
            InstrumentDataSourceKind.SIMULATED,
            CalibrationReadiness.STALE,
            MeasurementTrustLevel.DEGRADED
        )
        assertEquals("Simulated", chips.dataSource.label)
        assertEquals(AppStatusLevel.NEUTRAL, chips.dataSource.level)
        assertEquals("App calibration · Stale", chips.calibration.label)
        assertEquals(AppStatusLevel.CAUTION, chips.calibration.level)
        assertEquals(AppStatusLevel.CAUTION, chips.trust.level)
    }

    @Test
    fun statusChips_noInstrumentInvalidUnknown_nullSafe() {
        val chips = InstrumentStatusPresenter.buildStatusChips(null, CalibrationReadiness.INVALID, null)
        assertEquals("No instrument", chips.dataSource.label)
        assertEquals(AppStatusLevel.NEGATIVE, chips.calibration.level)
        assertEquals("App calibration · Invalid", chips.calibration.label)
        assertEquals("Unknown", chips.trust.label)
        assertEquals(AppStatusLevel.NEUTRAL, chips.trust.level)
    }

    @Test
    fun statusChips_notStartedAndInProgressAndPartial() {
        val notStarted = InstrumentStatusPresenter.buildStatusChips(
            InstrumentDataSourceKind.NONE, CalibrationReadiness.NOT_STARTED, MeasurementTrustLevel.SIMULATED
        )
        assertEquals("App calibration · Not started", notStarted.calibration.label)
        assertEquals(AppStatusLevel.NEUTRAL, notStarted.calibration.level)
        assertEquals(AppStatusLevel.NEUTRAL, notStarted.trust.level)

        val inProgress = InstrumentStatusPresenter.buildStatusChips(
            InstrumentDataSourceKind.REAL_INSTRUMENT, CalibrationReadiness.IN_PROGRESS, MeasurementTrustLevel.PARTIAL
        )
        assertEquals(AppStatusLevel.CAUTION, inProgress.calibration.level)
        assertEquals("App calibration · In progress", inProgress.calibration.label)
        assertEquals(AppStatusLevel.CAUTION, inProgress.trust.level)
    }
}
