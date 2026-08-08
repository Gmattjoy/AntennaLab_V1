package com.example.antennalab_v1.storage

/*
------------------------------------------------------------
EDIT SECTION 1000
FILE: SettingsRepository.kt
PACKAGE: com.example.antennalab_v1.storage
LAYER: Storage / App-wide settings access

SYSTEM ROLE
The app-facing seam for settings. Everything outside this file reads
and writes a typed AppSettings and never sees a JSON key, a File, or
AppSettingsStore.

WHY A SEAM RATHER THAN CALLING THE STORE DIRECTLY
Two jobs, both load-bearing:

1. CACHE. Settings will be read from Compose, where a read can happen
   on every recomposition. Going to the filesystem each time is not
   acceptable, so the parsed value is held in memory and the file is
   touched once per process (plus once per write).

2. REVERSIBILITY. Hand-rolled org.json was chosen over androidx
   DataStore to avoid a new dependency, matching ProjectStorage. That
   choice should stay reversible: swapping the backing store means
   rewriting AppSettingsStore and this file's internals, with no call
   site anywhere else in the app changing. If callers reached for the
   store directly that property would be gone within a slice or two.

PATTERN
`object` with Context passed per call, matching UsbSessionManager,
ProjectStorage and ProjectIndexManager. The app has no Application
subclass and no DI, so this is how app-wide state is held here.

The in-memory cache makes this process-global mutable state, exactly
like UsbSessionManager's session state — hence clearCache(), which
tests call for the same reason CalibrationSessionLogicTest calls
UsbSessionManager.clearCalibrationState() in @Before.

THE BOUNDARY RULE
Settings never live in ProjectData; project facts never live here. See
the header of model/settings/AppSettings.kt.
------------------------------------------------------------
*/

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.antennalab_v1.model.settings.AppSettings
import com.example.antennalab_v1.model.settings.LayoutModePin

object SettingsRepository {

    /*
    ------------------------------------------------------------
    OBSERVABLE, and that is the point (slice 5d)
    ------------------------------------------------------------
    `by mutableStateOf` rather than a plain var. Compose's snapshot
    system then tracks reads that happen through current(), so ANY
    composable calling it recomposes when update() reassigns this.

    Slice 5c had to log the opposite as a known limitation: a settings
    change mid-session changed the field and nothing repainted. That was
    tolerable while every setting only seeded NEW sessions. The theme is
    the first one that must take effect immediately — a toggle that does
    not repaint reads as broken — so this line is what makes 5d possible,
    and it fixes 5c's limitation for free at the same time.

    NOT a StateFlow: that would add a coroutines-flow dependency, need a
    scope and collectAsState at every reader, and would leave 5c's
    existing current() call site non-reactive until separately migrated.
    This makes it reactive with no edit.

    ARCHITECTURAL FIRST, deliberate: androidx.compose.runtime appears
    only in features/ elsewhere. The runtime is a state/snapshot library,
    not the UI toolkit (compose.ui / material3) — this file already
    exists to serve Compose readers and already imports Context, and
    CLAUDE.md's no-UI-refs rule binds model/, which stays clean.

    Non-Compose callers are unaffected: a snapshot state object reads and
    writes normally outside a composition, which is why the existing
    repository tests keep passing untouched.
    ------------------------------------------------------------
    */
    private var cached: AppSettings? by mutableStateOf(null)

    /*
    ------------------------------------------------------------
    EDIT SECTION 1001
    READ
    ------------------------------------------------------------
    Cheap enough to call from a composable. First call per process
    parses the file; every later call is a field read.
    ------------------------------------------------------------
    */
    fun current(context: Context): AppSettings {
        return cached ?: AppSettingsStore.load(context).also { cached = it }
    }

    /*
    ------------------------------------------------------------
    EDIT SECTION 1002
    WRITE
    ------------------------------------------------------------
    Write-through: the file and the cache move together, so a read
    immediately after a write cannot see the old value. Returns the
    settings actually stored so a caller does not have to re-read.

    Transform-shaped rather than take-a-whole-AppSettings so a caller
    changing one field cannot silently revert another it never knew
    about — the read-modify-write happens here, against the current
    value, instead of against whatever the caller last saw.
    ------------------------------------------------------------
    */
    fun update(
        context: Context,
        transform: (AppSettings) -> AppSettings
    ): AppSettings {
        val updated = transform(current(context))
        AppSettingsStore.save(context, updated)
        cached = updated
        return updated
    }

    fun setLayoutModePin(context: Context, pin: LayoutModePin) {
        update(context) { it.copy(layoutModePin = pin) }
    }

    /*
    Drops the in-memory copy so the next read re-parses the file.
    Tests use it to isolate from each other; nothing in the app should
    need it, since every write already goes through update().
    */
    fun clearCache() {
        cached = null
    }
}
