package com.example.antennalab_v1.storage

/*
------------------------------------------------------------
EDIT SECTION 1000
FILE: AppSettingsStore.kt
PACKAGE: com.example.antennalab_v1.storage
LAYER: Storage / App-wide settings persistence

SYSTEM ROLE
Reads and writes the app-wide settings file. The ONLY thing that
serialises AppSettings; everything else goes through
SettingsRepository and never touches a JSON key or a File.

STORAGE
- Settings file: files/settings.json

Top level, deliberately NOT under files/projects/ — that folder is
per-project data and this is global. filesDir always exists, so unlike
ProjectStorage's getProjectsDirectory there is no mkdirs to do.

THE BOUNDARY RULE
Settings never live in ProjectData; project facts never live here. See
the header of model/settings/AppSettings.kt for the full statement.

LOAD MUST NEVER THROW
This sits on the launch path. A missing, empty, truncated, hand-edited
or otherwise unparseable file returns fully-defaulted settings rather
than propagating — a corrupt preferences file must never be able to
stop the app starting, and losing a layout preference is not worth a
crash. ProjectStorage.loadProjectById already uses runCatching for the
same reason; it returns null because a caller can meaningfully handle
"no such project", whereas there is no meaningful "no settings" state.

SAFE EDIT AREA
- add a field: one put in toJson, one opt in fromJson. Nothing else,
  and no migration — absent keys already fall back to the model
  defaults, so every older settings.json keeps loading.
------------------------------------------------------------
*/

import android.content.Context
import com.example.antennalab_v1.model.settings.AppSettings
import com.example.antennalab_v1.model.settings.defaultInstrumentFromStoredName
import com.example.antennalab_v1.model.settings.defaultTargetFrequencyMHzFromStored
import com.example.antennalab_v1.model.settings.layoutModePinFromStoredName
import com.example.antennalab_v1.model.settings.themePreferenceFromStoredName
import org.json.JSONObject
import java.io.File

object AppSettingsStore {

    private const val SETTINGS_FILE_NAME = "settings.json"

    private const val KEY_LAYOUT_MODE_PIN = "layoutModePin"
    private const val KEY_DEFAULT_TARGET_FREQUENCY_MHZ = "defaultTargetFrequencyMHz"
    private const val KEY_DEFAULT_INSTRUMENT = "defaultInstrument"
    private const val KEY_THEME_PREFERENCE = "themePreference"

    /*
    ------------------------------------------------------------
    EDIT SECTION 1001
    READ
    ------------------------------------------------------------
    */
    fun load(context: Context): AppSettings {
        val file = settingsFile(context)

        if (!file.exists()) {
            return AppSettings()
        }

        return runCatching {
            fromJson(JSONObject(file.readText()))
        }.getOrElse {
            // Corrupt FILE. Corrupt VALUES are handled a layer down by
            // layoutModePinFromStoredName, so a file that parses but carries
            // an unknown enum name still yields the rest of the settings
            // rather than being discarded wholesale.
            AppSettings()
        }
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 1002
    WRITE
    ------------------------------------------------------------
    Whole-file rewrite, matching ProjectStorage.saveProject. Settings
    are small and written rarely, so there is nothing to gain from a
    partial update.
    ------------------------------------------------------------
    */
    fun save(context: Context, settings: AppSettings) {
        settingsFile(context).writeText(
            toJson(settings).toString(2)
        )
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 1003
    SERIALISATION
    ------------------------------------------------------------
    */
    private fun toJson(settings: AppSettings): JSONObject {
        return JSONObject().apply {
            put(KEY_LAYOUT_MODE_PIN, settings.layoutModePin.name)
            put(KEY_DEFAULT_TARGET_FREQUENCY_MHZ, settings.defaultTargetFrequencyMHz)
            put(KEY_DEFAULT_INSTRUMENT, settings.defaultInstrument.name)
            put(KEY_THEME_PREFERENCE, settings.themePreference.name)
        }
    }

    private fun fromJson(json: JSONObject): AppSettings {
        return AppSettings(
            layoutModePin = layoutModePinFromStoredName(
                json.optStringOrNull(KEY_LAYOUT_MODE_PIN)
            ),
            defaultTargetFrequencyMHz = defaultTargetFrequencyMHzFromStored(
                json.optDoubleOrNull(KEY_DEFAULT_TARGET_FREQUENCY_MHZ)
            ),
            defaultInstrument = defaultInstrumentFromStoredName(
                json.optStringOrNull(KEY_DEFAULT_INSTRUMENT)
            ),
            themePreference = themePreferenceFromStoredName(
                json.optStringOrNull(KEY_THEME_PREFERENCE)
            )
        )
    }

    /*
    optString returns "" for an absent key, which the parser cannot tell
    apart from a genuinely empty value. Null-normalising here keeps the
    "absent key falls back to the model default" contract honest.
    Same shape as ProjectStorage's private optOptionalString, restated
    rather than shared because that one is private to project
    persistence and settings should not depend on it.
    */
    private fun JSONObject.optStringOrNull(key: String): String? {
        return if (has(key) && !isNull(key)) optString(key) else null
    }

    /*
    Null-normalised for the same reason: optDouble hands back NaN for an
    absent key, and a non-numeric value parses to NaN too. Both are rejected
    downstream by defaultTargetFrequencyMHzFromStored, but returning null for
    "absent" keeps the two cases distinguishable here.
    */
    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        return if (has(key) && !isNull(key)) optDouble(key) else null
    }

    /*
    Visible for tests: they assert on missing / corrupt / partial files
    and need to write those states directly.
    */
    internal fun settingsFile(context: Context): File {
        return File(context.filesDir, SETTINGS_FILE_NAME)
    }
}
