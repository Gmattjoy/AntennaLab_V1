package com.example.antennalab_v1.model.settings

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
(lastOpenedSection / lastExpandedCard / hasSeenProjectIntro) is the
live example: serialized on every project save, read by nothing, and
per-project when it was never about a project. Scheduled for teardown
in slice 5b, landing here.

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
The settings surface
EDIT SECTION 1001
--------------------------------------------------------------------
*/
data class AppSettings(
    val layoutModePin: LayoutModePin = LayoutModePin.AUTO
)

/*
====================================================================
FUTURE SURFACE — add each field WITH its consumer, never before
====================================================================
These are known settings with an owning slice. They are listed rather
than declared on purpose.

  themePreference: ThemePreference           (5d)
  defaultTargetFrequencyMHz: Double          (5c)
  defaultInstrument: TestHardwareProfile     (5c)
  appAnalysisCollapsedDefault: Boolean       (5f)
  hasSeenProjectIntro: Boolean               (5b — out of ProjectUiState)
  readoutFormat: ReadoutFormat               (unscheduled)

WHY NOT DECLARE THEM NOW
A defaulted field that nothing reads still gets written to
settings.json, so the file would assert preferences the app does not
honour — an operator editing themePreference would see nothing happen.
This codebase has been bitten by exactly that twice:
HardwareMeasurementCapabilities.supportsTdrPreview was TRUE on both
profiles with nothing reading it (so the TDR velocity-factor fix had no
reachable UI), and ProjectUiState is the same defect in the project
file.

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
