package com.example.antennalab_v1

import com.example.antennalab_v1.domain.testing.SweepHardwareIdentity
import com.example.antennalab_v1.model.testing.InstrumentDataSourceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure coverage for [SweepHardwareIdentity] — the save-path resolver that
 * decides which hardware name a sweep record persists (§10c.7).
 *
 * Load-bearing invariant: an unnamed sweep from a REAL_INSTRUMENT session is
 * NEVER persisted as "SIMULATED"; a driver that named itself always wins; and
 * the two only meet when they agree, because the neutral model default means
 * no producer stamps a device-like name on a simulated source.
 */
class SweepHardwareIdentityTest {

    // ---- isUnspecified ------------------------------------------------

    @Test
    fun isUnspecified_treatsNullBlankAndUnknownAsUnspecified() {
        assertTrue(SweepHardwareIdentity.isUnspecified(null))
        assertTrue(SweepHardwareIdentity.isUnspecified(""))
        assertTrue(SweepHardwareIdentity.isUnspecified("   "))
        assertTrue(SweepHardwareIdentity.isUnspecified("UNKNOWN"))
        assertTrue(SweepHardwareIdentity.isUnspecified("unknown"))
    }

    @Test
    fun isUnspecified_realNamesAreSpecified() {
        assertFalse(SweepHardwareIdentity.isUnspecified("SIMULATED"))
        assertFalse(SweepHardwareIdentity.isUnspecified("USB_NANOVNA_DRIVER"))
        assertFalse(SweepHardwareIdentity.isUnspecified("LiteVNA64 v0.3.3"))
    }

    // ---- explicit profile wins ----------------------------------------

    @Test
    fun namedDriver_wins_overLiveClassification() {
        // NanoVNA tag survives regardless of the live source.
        assertEquals(
            "USB_NANOVNA_DRIVER",
            SweepHardwareIdentity.resolvePersistedHardwareName(
                reportedProfile = "USB_NANOVNA_DRIVER",
                liveDataSourceKind = InstrumentDataSourceKind.NONE,
                liveHardwareName = null
            )
        )
    }

    @Test
    fun explicitSimulated_wins_andStaysSimulated() {
        assertEquals(
            "SIMULATED",
            SweepHardwareIdentity.resolvePersistedHardwareName(
                reportedProfile = "SIMULATED",
                liveDataSourceKind = InstrumentDataSourceKind.SIMULATED,
                liveHardwareName = null
            )
        )
    }

    @Test
    fun namedProfile_isTrimmed() {
        assertEquals(
            "LiteVNA64 v0.3.3",
            SweepHardwareIdentity.resolvePersistedHardwareName(
                reportedProfile = "  LiteVNA64 v0.3.3  ",
                liveDataSourceKind = null,
                liveHardwareName = null
            )
        )
    }

    // ---- unnamed → classified by live data-source kind ----------------

    @Test
    fun unnamedRealInstrument_persistsLiveDeviceName_neverSimulated() {
        val name = SweepHardwareIdentity.resolvePersistedHardwareName(
            reportedProfile = "",
            liveDataSourceKind = InstrumentDataSourceKind.REAL_INSTRUMENT,
            liveHardwareName = "LiteVNA64 v0.3.3"
        )
        assertEquals("LiteVNA64 v0.3.3", name)
        assertFalse(name.equals("SIMULATED", ignoreCase = true))
    }

    @Test
    fun unnamedRealInstrument_blankLiveName_fallsBackToGenericReal() {
        assertEquals(
            "Real Instrument",
            SweepHardwareIdentity.resolvePersistedHardwareName(
                reportedProfile = null,
                liveDataSourceKind = InstrumentDataSourceKind.REAL_INSTRUMENT,
                liveHardwareName = "  "
            )
        )
    }

    @Test
    fun unnamedSimulatedSession_persistsSimulated() {
        assertEquals(
            "SIMULATED",
            SweepHardwareIdentity.resolvePersistedHardwareName(
                reportedProfile = "",
                liveDataSourceKind = InstrumentDataSourceKind.SIMULATED,
                liveHardwareName = null
            )
        )
    }

    @Test
    fun unnamedNoLiveSession_persistsVisibleUnknown_notSimulated() {
        for (kind in listOf(InstrumentDataSourceKind.NONE, null)) {
            val name = SweepHardwareIdentity.resolvePersistedHardwareName(
                reportedProfile = "",
                liveDataSourceKind = kind,
                liveHardwareName = null
            )
            assertEquals("Unknown Instrument", name)
            assertFalse(name.equals("SIMULATED", ignoreCase = true))
        }
    }
}
