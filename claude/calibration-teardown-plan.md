# Teardown Blueprint — calibration/live-state scoping

Blueprint as-of 55418d3. Line numbers drift on first cut — navigate by symbol. Two decisions pending before cutting: see end.

**STATUS: Tier 0, 1 and 2 are all DONE** — `f2f5d5e` (dead code), `227237b` (Tier 1 hard
teardown), `02cc9ec` (Tier 2 wipes). All line numbers below are historical and no longer resolve;
navigate by symbol.

**Tier 1/2 were re-scoped to a HARD TEARDOWN — calibration is live-only, persistence deleted.**
The keep-and-fix described in the original Tier 1/2 sections was never built. **The two "decisions
pending" are DISSOLVED, not answered** — see the end of this doc.

**Tier 3 is 3-of-4 closed.** The OSL gate is fixed, duplicate-carries-calibration dissolved, and
`storedNameMatchesHardware` decided KEEP. **The confident-wrong `NANOVNA_H4` factory default is
the only remaining open item** — plan mode, next full session.

---

## Baseline to re-verify before cutting

| | |
|---|---|
| HEAD | `55418d3` (clean, in sync with `origin/main`) |
| Unit suite | **506 tests, 0 failures, 0 errors, 0 skipped** — certified against this tree (no source file newer than the run) |
| Guard command | `.\gradlew.bat test` (set `JAVA_HOME` to the Studio JBR first) |

If HEAD or the suite total differs next session, re-run the §A/§B audit anchors before trusting the line numbers below.

**Line numbers are as-of `55418d3` and will drift as soon as the first cut lands.** Every anchor is given with its symbol name — navigate by symbol, use the line as a hint only. Tier 0 shifts everything below it in `ProjectData.kt`.

---

## Tier 0 — Pure dead code (zero behaviour change) — ✅ **DONE (f2f5d5e)**

All verified zero-call-site across `app/src/main` **and** `app/src/test`. Cutting these first shrinks the surface the real fixes have to reason about.

**Everything in the table below was cut in `f2f5d5e`.** Anchors are kept as the historical
record of what was removed; the line numbers no longer resolve. Tiers 1–3 anchors below have
shifted in `ProjectData.kt` (−32 lines above `:454`) and `ProjectStorage.kt` (−8 lines) —
navigate by symbol, as the header already warns.

| Symbol | Anchor | Notes |
|---|---|---|
| `ProjectData.storedCalibrationMatchesSelectedHardware` | `model/ProjectData.kt:155-158` | **Cut this one first.** It is a design-time-scoped, alias-*un*aware duplicate of `EffectiveHardwareResolver.storedNameMatchesHardware`. Dead today; a loaded gun tomorrow. |
| `ProjectData.hardwareFrequencyMinHzOrDefault` / `…MaxHzOrDefault` | `:118-122` | |
| `ProjectData.supportsSmithChartOrDefault` / `supportsPhaseOrDefault` / `supportsS21OrDefault` / `supportsTdrPreviewOrDefault` | `:124-134` | `HardwareMeasurementCapabilities.kt:82` already documents `supportsTdrPreviewOrDefault` as dead data — delete that comment with it. |
| `ProjectData.hardwareMeasurementCapabilities` | `:115-116` | Distinct from the `TestHardwareProfile.toHardwareMeasurementCapabilities()` extension, which **stays** (used at `SweepGraphScreen:99`). |
| `ProjectCalibrationData.lastCalibrationStatusSummary` | `model/ProjectData.kt:487` | Frozen live-trust sentence in storage. Written at `StoredCalibrationProducer:90`, serialized `ProjectStorage:882/893`. **No reader.** |
| `ProjectCalibrationData.restoredFromStorage` | `model/ProjectData.kt:490` | No reader. Also has a contradictory default: `false` in the model, `true` on missing JSON key (`ProjectStorage:898`). |

**Blast radius of Tier 0**

- `ProjectStorage.kt:881-884` and `:892-898` — drop the two `put`/read pairs. Old saves still load (unknown JSON keys are ignored on read).
- `StoredCalibrationProducer.kt:89-91` — drop the two field assignments; keep `lastCalibrationSavedEpochMs` (see do-not-cut).
- `StoredCalibrationProducerTest.kt:138-139` — two assertions to delete.
- **`ProjectData.hardwareCapabilityProfile` (`:112-113`) is NOT free.** One live use (`:157`, inside the accessor you're deleting) and one test use (`AppRootControllerTest.kt:93`, a canonical-name helper). Once `:155-158` is gone, the only remaining consumer is that test helper — rewrite it as `hardware.toHardwareCapabilityProfile().displayName`, then the getter can go too. Cut it *last* in Tier 0, not first.

~~Expected suite delta after Tier 0: **506 → 504**~~ — **wrong, and self-contradictory as
written.** JUnit's `tests=` attribute counts `@Test` *methods*, not assertions; "no test methods
lost" and a falling total cannot both hold. Actual after Tier 0: **506 / 0 / 0, unchanged**
(`StoredCalibrationProducerTest` kept all 5 methods; the two deleted assertions lived inside one
of them). Do not treat a steady total as a failed cut — the real safety net for a deletion-only
change is that `assembleDebug` compiles, since Kotlin turns any missed reference into a hard error.

**Two call sites this blast-radius list missed** (both found and handled during the cut):

- **`ProjectStorage.kt:270-272`** — `duplicateProject` reset `restoredFromStorage` via
  `calibrationData = original.calibrationData.copy(...)`. Real code, not optional: the whole
  now-empty argument was removed. Nothing read the field, so no observable change — but nothing
  resets on duplicate any more, which *unmasks* the Tier 3 "duplicate carries another project's
  calibration" item rather than altering it. Tier 3 should now decide the whole question.
- **`StoredCalibrationProducer.kt:67`** — KDoc documented `restoredFromStorage = false` as part
  of the producer contract; reworded with the field.

Two comment-only references also updated: `EffectiveHardwareResolver.kt:87-91` (doctrine comment
repointed at `testHardwareProfile`, since the rule outlives the symbol it named) and
`HardwareMeasurementCapabilities.kt:80-82` (stale clause trimmed; the surrounding `supportsTDR`
rationale block was deliberately kept).

**Legacy-load tolerance confirmed:** `toProjectCalibrationData` builds via named args from
`opt*` calls, and the only key-presence checks in `ProjectStorage` (`:1073`, `:1081`) are
optional-read helpers. No schema validation, no unknown-key rejection — old saves carrying
`lastCalibrationStatusSummary` / `restoredFromStorage` load unchanged; the keys are never
consulted. New saves omit them.

---

## Tier 1 + Tier 2 — HARD TEARDOWN — ✅ **DONE (`227237b`, `02cc9ec`)**

> **The blueprint's Tier 1/2 below were superseded before any of it was written.**
> They scoped a *keep-and-fix*: add `RESTORE_AS_STALE` to `CalibrationRestoreAction`,
> soften some `CLEAR` reasons, keep persistence. The decision taken instead was a
> **hard teardown: calibration is live-only, persistence deleted entirely.**
>
> **The two "decisions pending" at the end of this doc are therefore DISSOLVED,
> not answered.** There is no restore policy to choose because there is no
> restore. Do not revisit them as open questions.

### Why live-only

A calibration is a property of the INSTRUMENT in the CURRENT session. It is not a
property of a saved design file. Persisting it created a path by which a file
manufactured a live trust verdict — the exact confusion the calibration-honesty
rule exists to prevent.

The defect the old Tier 1 described was real and is now unreachable rather than
patched: `RESTORE_AS_STALE` was the model default *and* the missing-key fallback,
`decideCalibrationRestore` had no branch for it, so every legacy save fell through
to `else → RESTORE`, which re-stamped `capturedSessionKey` to the current session
and set `readiness = VALID`. A calibration read off disk became permanently immune
to the previous-session staleness check. If its span missed the sweep,
`SweepController` silently declined to correct — uncorrected sweep, VALID badge.

### What was deleted (Tier 1, `227237b`)

`ProjectData.calibrationData`, `ProjectCalibrationData`, `CalibrationRestorePolicy`,
`ProjectData.hasStoredCalibration` / `storedCalibrationOrNull`; all calibration
serialization in `ProjectStorage` (including the `CalibrationSession` and
`OslCalibrationCoefficients` JSON adapters, which were private to that file and
reachable only through `ProjectCalibrationData`, so they orphaned with it, plus the
legacy prefs path's `ProjectCalibrationData()` and the orphaned `toLongList`);
`StoredCalibrationProducer` and both callers; `decideCalibrationRestore`,
`CalibrationRestoreAction`, `buildCalibrationRestoreLogLine`;
`applyStoredCalibrationToSharedSession` and its four call sites; and the two
stored-calibration UI surfaces (dashboard badge chip, saved-project card lines)
with `LoadProjectController`'s two helpers.

**The field/serialization split:** `CalibrationSession.capturedSessionKey`,
`capturedProtocolFamily`, `capturedInstrumentIdentityText` **stay as live fields** —
the staleness detector still needs them. Only their persistence died.

**Two call sites this doc's blast radius missed**, both compiler-surfaced:
`DashboardScreen` rendered `StatusPill(calLabel, calLevel)`, and
`ProjectStorage.toLongList()` orphaned with the coefficient reader.

### What was removed (Tier 2, `02cc9ec`)

The four unconditional wipes: `AppRootScreen.enterWizardMode`,
`enterRfTestWizardMode`, `enterUnknownDiscoveryMode`, and `CalibrationWizardScreen`'s
Cancel button. Loading a project now touches live calibration **not at all**.

### Off-bench verification (emulator, no VNA)

Instrumented guard: `app/src/androidTest/.../LiveCalibrationSurvivesNavigationTest.kt`.

| Step | Result |
|---|---|
| simulate O/S/L capture | VALID |
| enter wizard mode | survived, VALID |
| enter RF-test mode | survived, VALID |
| enter unknown discovery | survived, **STALE** |
| open saved project | survived, VALID |

All four previously wiped it.

**Why the test asserts survival, not VALID.** Off-bench there is no open USB
session, so entering discovery routes through the project screen and the staleness
detector downgrades `VALID → STALE`. That is
`refreshCalibrationStateForCurrentSession` doing `copy(readiness = STALE)` — the
coefficients are KEPT and `SweepController` still applies correction for STALE. The
Tier 2 bug was different in kind: `clearCalibrationState()` replaced the whole
state, leaving `readiness = NOT_STARTED` and `calibrationSession = null`. The test
therefore checks the session object is retained, all three standards are still
captured, and readiness is not `NOT_STARTED`.

### Suite

506 → **479**, 0 failures. 27 test methods deleted with the symbols they covered;
the dashboard-badge and storage round-trip cases were **rewritten, not dropped**,
because they also cover behaviour that stays.

**Legacy saves still load.** Pinned by
`ProjectStorageRoundTripTest.load_legacyCalibrationBlob_isIgnored_andRestOfProjectSurvives`,
which injects a full pre-teardown `calibrationData` blob — stored session, OSL
coefficient arrays, `capturedSessionKey`, restore policy, and the two Tier 0 fields —
and asserts it is ignored while every other field survives.
`saveLoad_doesNotWriteCalibrationData` pins that new saves omit the key.

---

## Tier 3 — Latent leaks

**One item remains OPEN: the confident-wrong hardware default.** The other three are
closed — (a) fixed, (b) dissolved, (c) decided-keep.

| Item | Status | Detail |
|---|---|---|
| Design-time OSL gate | ✅ **DONE** | `ProjectPageScreen` gated `CalibrationStatusCard` on `project.supportsOslCalibrationOrDefault` — the project's *design-time* profile, so a LiteVNA attached to a NanoVNA-saved project answered for the wrong device. Now `EffectiveHardwareResolver.resolveCapabilityProfileForProject(project).supportsOslCalibration`. Harmless in outcome (both profiles set the flag `true`) but wrong by construction. **This was the last hardware-capability `…OrDefault` accessor**; it is deleted and the block header in `ProjectData.kt` now records why nothing lives there. Suite unchanged at 479. |
| ~~Duplicate carries another project's calibration~~ | ⛔ **DISSOLVED** | Hard teardown — there is no stored calibration left to duplicate. |
| `storedNameMatchesHardware` production-dead | ✅ **CLOSED — decided KEEP** | Its only non-test caller was `decideCalibrationRestore`, deleted in `227237b`, so it and the `hardwareNameAliases` / `normalizeHardwareName` pair beneath it have zero production callers. **Kept deliberately.** The alias sets encode a safety invariant the code states at `EffectiveHardwareResolver.kt:176-181`: the per-family sets MUST NOT overlap, because *applying the wrong correction silently is worse than clearing a good calibration*. ~22 references across 5 `@Test` methods guard it (`aliasSets_areDisjointAfterNormalisation`, `aliases_neverMatchAcrossFamilies`, `blankOrUnknownStoredName_doesNotMatchAnything`, `driverLabelAndCapabilityDisplayName_bothResolveToTheSameFamily`, `storedNameMatching_normalisesCaseSpacingAndPunctuation`). Deleting the function would delete that guard along with the natural home for any future name matching. **Not an open question — do not re-litigate.** |
| **Confident-wrong hardware default** | 🔴 **OPEN — the only one** | `features/app/AppRootController.kt:71`, `:84`, `:101` — all three factories hardcode `testHardwareProfile = NANOVNA_H4`, so every Lab / RF-test / discovery project *claims* a NanoVNA. Safe **only** because `EffectiveHardwareResolver` overrides at read time, which is exactly why any direct project-profile read is wrong by construction. **Root cause of the recurring Finding #7.** Fix is a nullable / `UNSPECIFIED` profile. **Scope: ~10 files, PLAN MODE** (touches `ProjectData` and the capability system — both require it per CLAUDE.md). `TestHardwareProfile` is a 2-value enum, so a third member forces a `when` audit at `ProjectData.kt:520`/`:583`, `ProjectWorkspaceController:167`, `ProjectPageScreen:835`; the 8 deliberate design-time reads (selector, build-sheet, persistence, factories) each need a decision on what "unspecified" displays as; `EffectiveHardwareResolver:83` already handles null, so the resolver side is nearly free. **Next full session.** |

---

## Do NOT cut

- **`CalibrationSession.capturedSessionKey`, `capturedProtocolFamily`, `capturedInstrumentIdentityText`** (`model/testing/CalibrationSession.kt:76-78`) — these feed the staleness detector at `UsbSessionManager:1319/1337`. **Still live fields after the hard teardown; only their serialization was removed.**
- ~~**`ProjectCalibrationData.lastCalibrationSavedEpochMs`**~~ — **gone with `ProjectCalibrationData` in `227237b`.** An age-based staleness rule, if ever wanted, must derive from live session state, not from a saved file.
- **`SweepHardwareIdentity` and the whole provenance path** (`domain/testing/SweepHardwareIdentity.kt`, `SweepWorkspaceController:840/878`, `SweepWorkspaceViewModel:290`) — this is the **correct** discipline and the template for the calibration fix: *resolve from live truth at write time, store the resolved fact, never restore it into live state.* It is what §10c.7 fixed; don't disturb it.
- **`ProjectSweepHistoryEntry` / `DiscoverySnapshot` / `TestData`** — records of past measurements, correctly persisted.
- **Sweep config path** — point count derived at `UsbVnaSweepDataSource:249-260`, sweep window at `SweepGraphScreen:114-122`, calibration span at `CalibrationSessionFactory:81-99`. All already read effective/live truth. Clean; leave alone.

---

## Verification recipe

1. `.\gradlew.bat test` — Tier 0 held at **506**; after the hard teardown, **479**, 0 failures.
   (Those are teardown-era snapshots, not a standing target — the clear-calibration control since
   took it to **482**.)
2. `.\gradlew.bat assembleDebug` — the real safety net for deletion work, since Kotlin turns
   every missed reference into a hard error.
3. **`.\gradlew.bat connectedDebugAndroidTest` — NEEDS A DEVICE OR EMULATOR, and is NOT part of
   `.\gradlew.bat test`.** `LiveCalibrationSurvivesNavigationTest` is the repo's first real
   instrumented test (`androidTest` previously held only the template `ExampleInstrumentedTest`).
   Run it after any change to navigation or calibration lifetime, or the Tier 2 guard is dead
   weight.
4. Bench-only: real LiteVNA64 at 14.2 MHz, per `claude/hardware-bringup-litevna64.md`.
5. Commit per tier, each independently bisectable — no history rewrites.

**Sequencing note (as executed):** Tier 0 first because it is reversible and de-risks reading the
code. Then Tier 1 (deletion, no runtime behaviour change beyond persistence going away), then
Tier 2 **as its own commit** because it is the one that changes runtime behaviour — if live
calibration ever starts surviving something it should not, bisect lands there. `AppRootScreen.kt`
carried both tiers, so Tier 1 was committed with an intermediate version of that file that still
held the wipes; that intermediate was built and tested before committing, so the bisect point is
sound.

---

## Open items

- ~~**No operator path to clear a live calibration.**~~ — **PARTIALLY CLOSED.** The workspace
  `CalibrationStatusCard` now carries a "Clear calibration" control behind a confirm dialog. It is
  offered whenever readiness is past `NOT_STARTED` on OSL-capable hardware, and clearing wipes live
  state *and* re-derives the screen's shadowed `calibrationSession` in one call
  (`ProjectWorkspaceController.clearCalibrationAndRebuildSession`) — clearing the manager alone
  would leave the card showing "Captured" over a wiped instrument, the exact honesty failure the
  teardown exists to prevent. `clearCalibrationState()` therefore has a production caller again.
  Verified on the emulator via the simulate-O/S/L path.
  **Still open: the Device Connections entry.** That screen does not own the `ProjectPageScreen`
  shadow, so clearing from there needs cross-screen state plumbing before it can be honest.
- **Tier 2 step 2c bench caveat.** Entering unknown-discovery shows **STALE** on the emulator
  because there is no open USB session. On a real LiteVNA with an open session it should stay
  **VALID** — the downgrade is the staleness detector, not a wipe. **Verify on the bench**; if it
  is STALE there too, the refresh is firing on a session that is genuinely open and that is a
  separate bug.
- **`connectedDebugAndroidTest` now needs a device** — see verification step 3.
- ~~**`EffectiveHardwareResolver.storedNameMatchesHardware` is production-dead**~~ — **CLOSED,
  decided KEEP.** It guards the never-match-across-families invariant; see the Tier 3 table.
  Not an open question.

---

## Decisions pending — NONE

~~**(a)** Add `RESTORE_AS_STALE` to `CalibrationRestoreAction`.~~
~~**(b)** Which of the four `CLEAR` reasons become non-destructive.~~

**Both DISSOLVED by the hard teardown, not answered.** `CalibrationRestoreAction` and the whole
restore decision no longer exist, so neither question has a subject. Recorded here so a future
session does not resurrect them as open work.
