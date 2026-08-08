package com.example.antennalab_v1.model.settings

import com.example.antennalab_v1.model.TestHardwareProfile

/*
########################################################################
FILE: AppSettings.kt
PACKAGE: com.example.antennalab_v1.model.settings
LAYER: Model / Settings

SYSTEM ROLE
The typed app-wide settings surface — "how I like the app", global to
the operator and independent of any project.

THE BOUNDARY RULE (non-negotiable)
--------------------------------------------------------------------
SETTINGS NEVER LIVE IN ProjectData. PROJECT FACTS NEVER LIVE HERE.

A project file describes ONE antenna: its design inputs, its materials,
its calculated geometry, its measurements. If a value would still be
true after every project on the device were deleted, it is a setting
and belongs in this model. If it describes a specific antenna, build or
measurement, it belongs in ProjectData and must not appear here.

The rule exists because the app grew without an app-wide store, so
preferences leaked into the project file. ProjectUiState
(lastOpenedSection / lastExpandedCard / hasSeenProjectIntro) was the
worked example: serialized on every project save, read by nothing, and
per-project when it was never about a project. REMOVED in slice 5b —
along with the "SAFE EDIT AREA: add new remember-last-view style
fields" comment that invited it, which was the actual drift mechanism.

None of its three fields moved here, because none had a consumer to
move. See the future-surface block below for hasSeenProjectIntro.

MISSING FIELDS ARE TOLERATED, BY DESIGN
--------------------------------------------------------------------
Every field is defaulted, and the store returns a fully-defaulted
instance for a missing, partial or unparseable file. That is what makes
this model additive forever: a later slice adds a field plus one
put/opt pair in AppSettingsStore and every older settings.json keeps
loading unchanged. There is no migration path because there is nothing
to migrate.

LAYER NOTE
Pure data. No Android, no org.json, no Compose — the store does the
serialising, this file only describes the shape and the value parsing.
########################################################################
*/

/*
--------------------------------------------------------------------
Layout-mode pin
EDIT SECTION 1000
--------------------------------------------------------------------
Spec 2.2's Simple/Full control. AUTO lets the app choose from the
window it is given; SIMPLE and FULL are the operator pinning that
choice and overriding AUTO.

This is a PERSISTENT layout preference and is deliberately NOT the
same thing as tap-to-expand, which is transient focus living on
SweepWorkspaceState.expandedChartKind. Spec 2.2 forbids conflating the
two "in the design OR the code" — they are separate fields in separate
stores for that reason, and slice 4a's nullable overlay is the other
half of the same guarantee.

The AUTO breakpoint itself (what picks Simple vs Full, and the pin's
persistence scope) is spec open question 1, settled in slice 5e. This
enum only records the operator's choice.
--------------------------------------------------------------------
*/
enum class LayoutModePin {
    AUTO,
    SIMPLE,
    FULL
}

/*
--------------------------------------------------------------------
Theme preference
EDIT SECTION 1000b
--------------------------------------------------------------------
SYSTEM defers to the device's own dark-mode setting; DARK and LIGHT
are the operator overriding it.

SYSTEM is the default because it is the only value that can be right
without asking. The app shipped dark-only before this, so DARK would
preserve today's look — but it would also mean a device in light mode
gets a dark app forever with no indication that a choice exists.

Note this REVERSES a documented decision: the app used to force its own
dark flag independent of the system (see SemanticColors' header, which
5d rewrites). The light palette was always fully built; only the
plumbing and a control were missing.
--------------------------------------------------------------------
*/
enum class ThemePreference {
    SYSTEM,
    DARK,
    LIGHT
}

/*
--------------------------------------------------------------------
The settings surface
EDIT SECTION 1001
--------------------------------------------------------------------
*/
data class AppSettings(
    val layoutModePin: LayoutModePin = LayoutModePin.AUTO,
    /*
    Seeds the target frequency of a project created on a project-less path
    (RF Test Mode, unknown discovery, the empty placeholder). It is a SEED,
    not an override: the factory writes it into designInput, and from then on
    the project owns its own value. A wizard-created or loaded project is
    never affected, because those paths never call the factories.

    146.0 = 2 m. Changed from the 14.2 that was hardcoded in three factories.
    */
    val defaultTargetFrequencyMHz: Double = DEFAULT_TARGET_FREQUENCY_MHZ,
    /*
    Same seeding role for the instrument. Deliberately NOT the same thing as
    EffectiveHardwareResolver's DEFAULT_HARDWARE, which stays the
    deterministic last resort when neither a live instrument nor a project
    profile exists — this one only decides what a NEW project-less session
    starts out claiming, and a live instrument still overrides it at
    resolver tiers 1-2.
    */
    val defaultInstrument: TestHardwareProfile = DEFAULT_INSTRUMENT,
    /*
    The first setting that must take effect LIVE. The others seed new
    sessions, so a stale read is harmless; a theme the operator just
    chose has to repaint immediately or the control looks broken.
    That requirement is what makes SettingsRepository observable.
    */
    val themePreference: ThemePreference = DEFAULT_THEME_PREFERENCE
)

/*
====================================================================
FUTURE SURFACE — add each field WITH its consumer, never before
====================================================================
These are known settings with an owning slice. They are listed rather
than declared on purpose.

  appAnalysisCollapsedDefault: Boolean       (5f)
  hasSeenProjectIntro: Boolean               (when an intro gate exists)
  readoutFormat: ReadoutFormat               (unscheduled)

defaultTargetFrequencyMHz and defaultInstrument left this list in slice 5c,
which is the first slice where the rule was SATISFIED rather than deferred:
their consumer (the three AppRootController factories) landed in the same
commit as the fields. That is the bar — a field arrives with the code that
reads it, or it does not arrive.

WHY NOT DECLARE THEM NOW
A defaulted field that nothing reads still gets written to
settings.json, so the file would assert preferences the app does not
honour — an operator editing themePreference would see nothing happen.
This codebase has been bitten by exactly that twice:
HardwareMeasurementCapabilities.supportsTdrPreview was TRUE on both
profiles with nothing reading it (so the TDR velocity-factor fix had no
reachable UI), and ProjectUiState was the same defect in the project
file — which is why slice 5b deleted it rather than relocating it here.
hasSeenProjectIntro in particular has NO consumer: there is no intro or
onboarding screen anywhere in the app, so a field for it would be inert
on arrival. It waits for the gate it is supposed to gate.

Deferring costs nothing: fields are defaulted and absent keys already
fall back, so adding one later is a field plus one put/opt pair, with
no migration. Add the field in the slice that adds its consumer.
====================================================================
*/

/*
--------------------------------------------------------------------
Stored-value parsing
EDIT SECTION 1002
--------------------------------------------------------------------
Corrupt-VALUE tolerance, which is a different concern from the
corrupt-FILE tolerance AppSettingsStore owns. A settings file can be
well-formed JSON and still carry a name this build does not know —
hand-edited, written by a newer build, or an enum constant since
renamed. None of those may throw.

Pure and Android-free so it is unit-testable without Robolectric,
which the org.json layer above it cannot be.

Deliberately NOT reusing ProjectStorage's enumValueOrDefault: that one
is private to ProjectStorage, and widening its visibility to share it
would couple the settings model to project persistence.
--------------------------------------------------------------------
*/
fun layoutModePinFromStoredName(raw: String?): LayoutModePin {
    return LayoutModePin.entries.firstOrNull { it.name == raw }
        ?: LayoutModePin.AUTO
}

/*
Rejects more than just an absent key. A settings file is hand-editable, so a
zero, a negative, or a NaN can reach here — and any of those would poison
resolveSweepWindow and every axis derived from it, since the window is
target +/- a half-width. A frequency has to be a positive real number to mean
anything, so anything else falls back rather than propagating.
*/
fun defaultTargetFrequencyMHzFromStored(raw: Double?): Double {
    if (raw == null || raw.isNaN() || raw.isInfinite() || raw <= 0.0) {
        return DEFAULT_TARGET_FREQUENCY_MHZ
    }
    return raw
}

fun defaultInstrumentFromStoredName(raw: String?): TestHardwareProfile {
    return TestHardwareProfile.entries.firstOrNull { it.name == raw }
        ?: DEFAULT_INSTRUMENT
}

fun themePreferenceFromStoredName(raw: String?): ThemePreference {
    return ThemePreference.entries.firstOrNull { it.name == raw }
        ?: DEFAULT_THEME_PREFERENCE
}

/*
--------------------------------------------------------------------
Theme resolution
EDIT SECTION 1003
--------------------------------------------------------------------
Collapses the three-way preference onto the Boolean the theme actually
takes. Extracted rather than written inline in MainActivity for two
reasons: it is the one piece of real logic in the theme path, and
AntennaLab_V1Theme's `darkTheme: Boolean` parameter is deliberately NOT
becoming a ThemePreference — 18 call sites across 8 files pass it, and
all but MainActivity are @Previews that have no business knowing about
settings.

`systemInDark` is supplied by the caller (isSystemInDarkTheme() at the
composition root) so this stays pure and testable with no Android
surface.
--------------------------------------------------------------------
*/
fun resolveDarkTheme(
    preference: ThemePreference,
    systemInDark: Boolean
): Boolean {
    return when (preference) {
        ThemePreference.SYSTEM -> systemInDark
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
    }
}

/*
Named so the fallback and the data-class default cannot drift apart — both
read the same constant rather than restating the literal.
*/
const val DEFAULT_TARGET_FREQUENCY_MHZ = 146.0
val DEFAULT_INSTRUMENT = TestHardwareProfile.NANOVNA_H4
val DEFAULT_THEME_PREFERENCE = ThemePreference.SYSTEM
