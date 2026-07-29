package com.example.antennalab_v1.features.app

import com.example.antennalab_v1.model.testing.CalibrationReadiness
import com.example.antennalab_v1.model.testing.InstrumentDataSourceKind
import com.example.antennalab_v1.model.testing.MeasurementTrustLevel
import com.example.antennalab_v1.ui.components.AppStatusLevel

/*
########################################################################
FILE: InstrumentStatusPresenter.kt
PACKAGE: com.example.antennalab_v1.features.app
LAYER: UI / App / shared status presentation (pure)

THE single state→status-chip mapping. Both the dashboard status card and the
Device Connections screen call this ONE function, so they can never present
the same InstrumentSessionState differently (the anti-drift requirement).

Relocated verbatim from DashboardController.buildStatusChips — same labels,
same levels — so the move is behaviour-preserving, not a rewrite. Calibration
is labelled unambiguously as the APP's ("App calibration · …").

Pure: enums in, chips out. No Compose, no Context.
########################################################################
*/
object InstrumentStatusPresenter {

    data class InstrumentStatusChip(val label: String, val level: AppStatusLevel)

    data class InstrumentStatusChips(
        val dataSource: InstrumentStatusChip,
        val calibration: InstrumentStatusChip,
        val trust: InstrumentStatusChip
    )

    fun buildStatusChips(
        dataSourceKind: InstrumentDataSourceKind?,
        calibrationReadiness: CalibrationReadiness?,
        trust: MeasurementTrustLevel?
    ): InstrumentStatusChips = InstrumentStatusChips(
        dataSource = when (dataSourceKind) {
            InstrumentDataSourceKind.REAL_INSTRUMENT -> InstrumentStatusChip("Live", AppStatusLevel.POSITIVE)
            InstrumentDataSourceKind.SIMULATED -> InstrumentStatusChip("Simulated", AppStatusLevel.NEUTRAL)
            InstrumentDataSourceKind.NONE, null -> InstrumentStatusChip("No instrument", AppStatusLevel.NEUTRAL)
        },
        calibration = when (calibrationReadiness) {
            CalibrationReadiness.VALID -> InstrumentStatusChip("App calibration · Valid", AppStatusLevel.POSITIVE)
            CalibrationReadiness.STALE -> InstrumentStatusChip("App calibration · Stale", AppStatusLevel.CAUTION)
            CalibrationReadiness.IN_PROGRESS -> InstrumentStatusChip("App calibration · In progress", AppStatusLevel.CAUTION)
            CalibrationReadiness.INVALID -> InstrumentStatusChip("App calibration · Invalid", AppStatusLevel.NEGATIVE)
            CalibrationReadiness.NOT_STARTED, null -> InstrumentStatusChip("App calibration · Not started", AppStatusLevel.NEUTRAL)
        },
        trust = when (trust) {
            MeasurementTrustLevel.TRUSTED -> InstrumentStatusChip("Trusted", AppStatusLevel.POSITIVE)
            MeasurementTrustLevel.DEGRADED -> InstrumentStatusChip("Degraded", AppStatusLevel.CAUTION)
            MeasurementTrustLevel.PARTIAL -> InstrumentStatusChip("Partial", AppStatusLevel.CAUTION)
            MeasurementTrustLevel.SIMULATED -> InstrumentStatusChip("Simulated", AppStatusLevel.NEUTRAL)
            MeasurementTrustLevel.UNKNOWN, null -> InstrumentStatusChip("Unknown", AppStatusLevel.NEUTRAL)
        }
    )
}
