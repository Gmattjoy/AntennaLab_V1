package com.example.antennalab_v1

import android.content.Context
import com.example.antennalab_v1.model.TestHardwareProfile
import com.example.antennalab_v1.model.settings.AppSettings
import com.example.antennalab_v1.model.settings.LayoutModePin
import com.example.antennalab_v1.model.settings.ThemePreference
import com.example.antennalab_v1.storage.AppSettingsStore
import com.example.antennalab_v1.storage.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Robolectric coverage for the settings file itself: round-tripping, and the
 * four ways a settings file can be less than perfect. Robolectric is required
 * because [AppSettingsStore] uses org.json and a real [Context] filesDir; the
 * value-parsing half is pure and covered without it in
 * [AppSettingsTest].
 *
 * SettingsRepository caches in a process-global `object`, so each test clears
 * the cache and deletes the file first — same reason
 * CalibrationSessionLogicTest resets UsbSessionManager in @Before.
 */
@RunWith(RobolectricTestRunner::class)
class AppSettingsStoreTest {

    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    @Before
    fun resetSettings() {
        SettingsRepository.clearCache()
        AppSettingsStore.settingsFile(context).delete()
    }

    @Test
    fun save_thenLoad_roundTripsANonDefaultPin() {
        // Non-default on purpose: a round-trip test that stores the default
        // value passes even if save() writes nothing at all.
        AppSettingsStore.save(context, AppSettings(layoutModePin = LayoutModePin.SIMPLE))

        assertEquals(
            AppSettings(layoutModePin = LayoutModePin.SIMPLE),
            AppSettingsStore.load(context)
        )
    }

    @Test
    fun load_returnsDefaultsWhenTheFileIsMissing() {
        // First launch on a fresh install. Nothing has ever been written.
        assertFalse(AppSettingsStore.settingsFile(context).exists())

        assertEquals(AppSettings(), AppSettingsStore.load(context))
    }

    @Test
    fun load_returnsDefaultsWithoutThrowingOnACorruptFile() {
        // Truncated write, hand-edit, or a file that is not JSON at all. This
        // sits on the launch path, so the only acceptable behaviour is to fall
        // back — a corrupt preferences file must never stop the app starting.
        AppSettingsStore.settingsFile(context).writeText("{not json at all")

        assertEquals(AppSettings(), AppSettingsStore.load(context))
    }

    @Test
    fun load_defaultsEveryAbsentKeyOnAPartialFile() {
        // The shape every settings.json written before a future field was added
        // will have. Valid JSON, just missing keys this build knows about.
        AppSettingsStore.settingsFile(context).writeText("""{"unrelatedKey":"ignored"}""")

        assertEquals(AppSettings(), AppSettingsStore.load(context))
    }

    @Test
    fun load_defaultsAnUnknownEnumValueWithoutDiscardingTheFile() {
        // Corrupt VALUE, not corrupt FILE — well-formed JSON carrying a name
        // this build does not know (newer build, or a constant since renamed).
        // Handled a layer down by layoutModePinFromStoredName, which is why the
        // file is parsed rather than thrown away wholesale.
        AppSettingsStore.settingsFile(context).writeText("""{"layoutModePin":"SPLIT_SCREEN"}""")

        assertEquals(LayoutModePin.AUTO, AppSettingsStore.load(context).layoutModePin)
    }

    @Test
    fun save_thenLoad_roundTripsTheSliceFiveCFields() {
        // Non-defaults on both, so a writer that dropped either key fails here.
        val stored = AppSettings(
            defaultTargetFrequencyMHz = 7.1,
            defaultInstrument = TestHardwareProfile.LITEVNA64_V0_3_3
        )
        AppSettingsStore.save(context, stored)

        assertEquals(stored, AppSettingsStore.load(context))
    }

    @Test
    fun load_honoursOnePresentKeyAndDefaultsTheOthers() {
        // The shape every settings.json written by slice 5a has: layoutModePin
        // only, with 5c's two keys absent. Both must fall back without
        // disturbing the key that IS there.
        AppSettingsStore.settingsFile(context).writeText("""{"defaultInstrument":"LITEVNA64_V0_3_3"}""")

        val loaded = AppSettingsStore.load(context)

        assertEquals(TestHardwareProfile.LITEVNA64_V0_3_3, loaded.defaultInstrument)
        assertEquals(146.0, loaded.defaultTargetFrequencyMHz, 0.0)
        assertEquals(LayoutModePin.AUTO, loaded.layoutModePin)
    }

    @Test
    fun save_thenLoad_roundTripsTheThemePreference() {
        AppSettingsStore.save(context, AppSettings(themePreference = ThemePreference.LIGHT))

        assertEquals(
            ThemePreference.LIGHT,
            AppSettingsStore.load(context).themePreference
        )
    }

    @Test
    fun load_honoursThemePreferenceInAnOtherwiseEmptyFile() {
        // The shape a settings.json written before 5d has: this key absent, or
        // present alone. Either way it must not disturb the other fields.
        AppSettingsStore.settingsFile(context).writeText("""{"themePreference":"DARK"}""")

        val loaded = AppSettingsStore.load(context)

        assertEquals(ThemePreference.DARK, loaded.themePreference)
        assertEquals(146.0, loaded.defaultTargetFrequencyMHz, 0.0)
        assertEquals(LayoutModePin.AUTO, loaded.layoutModePin)
    }

    @Test
    fun load_defaultsAHandEditedNonsenseFrequency() {
        // Well-formed JSON, meaningless value — the corrupt-VALUE case for a
        // Double rather than an enum.
        AppSettingsStore.settingsFile(context).writeText("""{"defaultTargetFrequencyMHz":-1}""")

        assertEquals(146.0, AppSettingsStore.load(context).defaultTargetFrequencyMHz, 0.0)
    }

    @Test
    fun repository_writesThroughAndSurvivesACacheDrop() {
        SettingsRepository.setLayoutModePin(context, LayoutModePin.FULL)

        // Write-through: the cached value moved with the file, so a read
        // straight after a write cannot serve the stale one.
        assertEquals(LayoutModePin.FULL, SettingsRepository.current(context).layoutModePin)

        // And it really reached disk — drop the cache and the next read has to
        // re-parse the file to answer.
        SettingsRepository.clearCache()
        assertEquals(LayoutModePin.FULL, SettingsRepository.current(context).layoutModePin)
        assertEquals(LayoutModePin.FULL, AppSettingsStore.load(context).layoutModePin)
    }
}
