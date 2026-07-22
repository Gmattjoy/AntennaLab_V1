package com.example.antennalab_v1.model.testing

/*
########################################################################
FILE: SweepResult.kt
PACKAGE: com.example.antennalab_v1.model.testing
LAYER: Model / Testing Measurement

SYSTEM ROLE
Represents a full frequency sweep dataset.

This structure stores both the raw measurement points and metadata
about the sweep operation itself.

The goal is to make sweep datasets self-describing so they can later
support:

• saved sweep sessions
• debugging hardware behaviour
• exporting measurement data
• comparing sweeps
• advanced RF analysis

SAFE EDIT AREA
- extend with additional measurement metadata
- extend with calibration data
- extend with hardware driver information
########################################################################
*/

/*
########################################################################
EDIT SECTION 2001
------------------------------------------------------------------------
PURPOSE
Primary sweep dataset container.

Stores sweep configuration and all collected measurement points.
########################################################################
*/
data class SweepResult(

    /*
    --------------------------------------------------------------------
    Sweep start frequency
    EDIT SECTION 2002
    --------------------------------------------------------------------
    */
    val startFrequencyMHz: Double,

    /*
    --------------------------------------------------------------------
    Sweep end frequency
    EDIT SECTION 2003
    --------------------------------------------------------------------
    */
    val endFrequencyMHz: Double,

    /*
    --------------------------------------------------------------------
    Frequency step size
    EDIT SECTION 2004
    --------------------------------------------------------------------
    */
    val stepMHz: Double,

    /*
    --------------------------------------------------------------------
    Measurement points collected during the sweep
    EDIT SECTION 2005
    --------------------------------------------------------------------
    */
    val points: List<SweepPoint>,

    /*
    --------------------------------------------------------------------
    Total number of sweep points
    EDIT SECTION 2006
    --------------------------------------------------------------------
    PURPOSE
    Stores the intended number of points for the sweep. This allows
    detection of incomplete hardware sweeps later.
    --------------------------------------------------------------------
    */
    val sweepPointCount: Int = points.size,

    /*
    --------------------------------------------------------------------
    Sweep completeness metadata (flag, don't discard)
    --------------------------------------------------------------------
    PURPOSE
    Make partial hardware sweeps self-describing without dropping any
    measured data.
      requestedPointCount = points the sweep was configured to request
      actualPointCount    = points actually measured and retained
      isComplete          = false when fewer points came back than were
                            requested; the partial data is still kept

    Defaults assume a complete sweep so existing/simulated callers are
    unaffected. UI/CSV consumption of these fields is a later phase.
    --------------------------------------------------------------------
    */
    val requestedPointCount: Int = points.size,
    val actualPointCount: Int = points.size,
    val isComplete: Boolean = true,

    /*
    --------------------------------------------------------------------
    Sweep duration in milliseconds
    EDIT SECTION 2007
    --------------------------------------------------------------------
    PURPOSE
    Useful for real hardware sweeps and performance diagnostics.
    --------------------------------------------------------------------
    */
    val sweepDurationMs: Long = 0,

    /*
    --------------------------------------------------------------------
    Hardware identifier used for the sweep
    EDIT SECTION 2008
    --------------------------------------------------------------------
    PURPOSE
    Allows stored sweeps to record which hardware produced the
    measurement.
    --------------------------------------------------------------------
    */
    val hardwareProfile: String = "SIMULATED",

    /*
    --------------------------------------------------------------------
    Measurement capability flags snapshot
    EDIT SECTION 2009
    --------------------------------------------------------------------
    PURPOSE
    Stores a snapshot of measurement capability conditions at the time
    the sweep was performed.
    --------------------------------------------------------------------
    */
    val supportsS11: Boolean = true,
    val supportsS11Phase: Boolean = false,
    val supportsS21: Boolean = false,
    val supportsS21Phase: Boolean = false,

    /*
    --------------------------------------------------------------------
    Calibration state (flag, don't discard)
    --------------------------------------------------------------------
    PURPOSE
    Records whether OSL correction was applied to this sweep, mirroring
    the isComplete convention above.
      isCalibrated     = true when an active OSL calibration was applied
                         to the raw measurement before this result was
                         produced
      calibrationLabel = short human description of the calibration used
                         (e.g. hardware + capture timestamp); blank when
                         uncalibrated

    Defaults report uncalibrated so existing/simulated callers are
    unaffected. Correction is applied in domain (CalibrationCorrector).
    --------------------------------------------------------------------
    */
    val isCalibrated: Boolean = false,
    val calibrationLabel: String = ""
)