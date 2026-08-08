package com.example.antennalab_v1

import com.example.antennalab_v1.model.TestHardwareProfile
import com.example.antennalab_v1.model.settings.AppSettings
import com.example.antennalab_v1.model.settings.LayoutModePin
import com.example.antennalab_v1.model.settings.ThemePreference
import com.example.antennalab_v1.model.settings.defaultInstrumentFromStoredName
import com.example.antennalab_v1.model.settings.defaultTargetFrequencyMHzFromStored
import com.example.antennalab_v1.model.settings.layoutModePinFromStoredName
import com.example.antennalab_v1.model.settings.resolveDarkTheme
import com.example.antennalab_v1.model.settings.themePreferenceFromStoredName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure coverage for the settings model. Deliberately NOT Robolectric: this
 * layer is Android-free and org.json-free, which is the point of splitting
 * value parsing out of [com.example.antennalab_v1.storage.AppSettingsStore].
 * The store's file handling is covered separately, and it does need
 * Robolectric.
 */
class AppSettingsTest {

    @Test
    fun appSettings_defaultsToAutoLayoutPin() {
        // The default has to be AUTO rather than a concrete layout: a fresh
        // install has no opinion yet, and AUTO is "let the app decide".
        assertEquals(LayoutModePin.AUTO, AppSettings().layoutModePin)
    }

    @Test
    fun layoutModePinFromStoredName_mapsEveryValidName() {
        // Round-trips every constant by its stored name, so adding a value to
        // the enum without teaching the store about it fails here.
        LayoutModePin.entries.forEach { pin ->
            assertEquals(pin, layoutModePinFromStoredName(pin.name))
        }
    }

    @Test
    fun layoutModePinFromStoredName_fallsBackToAutoOnAnUnknownName() {
        // Hand-edited file, a name written by a newer build, or a constant
        // renamed since. None of these may throw — the operator's settings
        // file must never be able to break launch.
        assertEquals(LayoutModePin.AUTO, layoutModePinFromStoredName("SPLIT_SCREEN"))
        assertEquals(LayoutModePin.AUTO, layoutModePinFromStoredName(""))
        assertEquals(LayoutModePin.AUTO, layoutModePinFromStoredName("auto"))
    }

    @Test
    fun layoutModePinFromStoredName_fallsBackToAutoOnNull() {
        // The absent-key case: JSONObject hands back null for a key that was
        // never written, which is every settings.json older than this field.
        assertEquals(LayoutModePin.AUTO, layoutModePinFromStoredName(null))
    }

    // ------------------------------------------------------------------
    // Default target frequency (slice 5c)
    // ------------------------------------------------------------------

    @Test
    fun appSettings_defaultsToTwoMetres() {
        assertEquals(146.0, AppSettings().defaultTargetFrequencyMHz, 0.0)
    }

    @Test
    fun defaultTargetFrequencyMHzFromStored_keepsAValidFrequency() {
        assertEquals(7.1, defaultTargetFrequencyMHzFromStored(7.1), 0.0)
        assertEquals(435.0, defaultTargetFrequencyMHzFromStored(435.0), 0.0)
    }

    @Test
    fun defaultTargetFrequencyMHzFromStored_rejectsAnythingThatIsNotAPositiveReal() {
        // A settings file is hand-editable, so all of these can genuinely
        // arrive. Each would poison resolveSweepWindow — the window is
        // target +/- a half-width — and every axis derived from it.
        assertEquals(146.0, defaultTargetFrequencyMHzFromStored(null), 0.0)
        assertEquals(146.0, defaultTargetFrequencyMHzFromStored(Double.NaN), 0.0)
        assertEquals(146.0, defaultTargetFrequencyMHzFromStored(0.0), 0.0)
        assertEquals(146.0, defaultTargetFrequencyMHzFromStored(-14.2), 0.0)
        assertEquals(146.0, defaultTargetFrequencyMHzFromStored(Double.POSITIVE_INFINITY), 0.0)
    }

    // ------------------------------------------------------------------
    // Default instrument (slice 5c)
    // ------------------------------------------------------------------

    @Test
    fun appSettings_defaultsToNanoVnaH4() {
        assertEquals(TestHardwareProfile.NANOVNA_H4, AppSettings().defaultInstrument)
    }

    @Test
    fun defaultInstrumentFromStoredName_mapsEveryValidName() {
        TestHardwareProfile.entries.forEach { profile ->
            assertEquals(profile, defaultInstrumentFromStoredName(profile.name))
        }
    }

    // ------------------------------------------------------------------
    // Theme preference + resolution (slice 5d)
    // ------------------------------------------------------------------

    @Test
    fun appSettings_defaultsToSystemTheme() {
        // SYSTEM is the only value that can be right without asking. DARK
        // would preserve the app's historical look, but would also leave a
        // light-mode device dark forever with no hint a choice exists.
        assertEquals(ThemePreference.SYSTEM, AppSettings().themePreference)
    }

    @Test
    fun themePreferenceFromStoredName_mapsEveryValidName() {
        ThemePreference.entries.forEach { preference ->
            assertEquals(preference, themePreferenceFromStoredName(preference.name))
        }
    }

    @Test
    fun themePreferenceFromStoredName_fallsBackToSystemOnUnknownOrNull() {
        assertEquals(ThemePreference.SYSTEM, themePreferenceFromStoredName("HIGH_CONTRAST"))
        assertEquals(ThemePreference.SYSTEM, themePreferenceFromStoredName(null))
        assertEquals(ThemePreference.SYSTEM, themePreferenceFromStoredName(""))
        assertEquals(ThemePreference.SYSTEM, themePreferenceFromStoredName("system"))
    }

    @Test
    fun resolveDarkTheme_systemFollowsTheDevice() {
        // Both directions, because "follows the system" is the only case with
        // a second input — an implementation that ignored systemInDark would
        // still pass one of these.
        assertTrue(resolveDarkTheme(ThemePreference.SYSTEM, systemInDark = true))
        assertFalse(resolveDarkTheme(ThemePreference.SYSTEM, systemInDark = false))
    }

    @Test
    fun resolveDarkTheme_explicitChoicesOverrideTheDevice() {
        // The override is the point: DARK stays dark on a light device and
        // LIGHT stays light on a dark one.
        assertTrue(resolveDarkTheme(ThemePreference.DARK, systemInDark = false))
        assertTrue(resolveDarkTheme(ThemePreference.DARK, systemInDark = true))
        assertFalse(resolveDarkTheme(ThemePreference.LIGHT, systemInDark = true))
        assertFalse(resolveDarkTheme(ThemePreference.LIGHT, systemInDark = false))
    }

    @Test
    fun appSettings_defaultsToAppAnalysisCollapsed() {
        // Spec 2.3: the app's own interpretation is value-add ON TOP OF the
        // familiar VNA layout, so it starts out of the way. A default of false
        // here would silently invert the whole point of the slice.
        assertTrue(AppSettings().appAnalysisCollapsedDefault)
    }

    @Test
    fun defaultInstrumentFromStoredName_fallsBackOnUnknownOrNull() {
        // A profile retired between builds, or a key never written.
        assertEquals(TestHardwareProfile.NANOVNA_H4, defaultInstrumentFromStoredName("NANOVNA_H5"))
        assertEquals(TestHardwareProfile.NANOVNA_H4, defaultInstrumentFromStoredName(null))
        assertEquals(TestHardwareProfile.NANOVNA_H4, defaultInstrumentFromStoredName(""))
    }
}
