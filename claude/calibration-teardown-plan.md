# Teardown Blueprint — calibration/live-state scoping

Blueprint as-of 55418d3. Line numbers drift on first cut — navigate by symbol. Two decisions pending before cutting: see end.

**Read-only map. Nothing written, nothing staged, nothing committed. No teardown begun.**

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

## Tier 0 — Pure dead code (zero behaviour change)

All verified zero-call-site across `app/src/main` **and** `app/src/test`. Cutting these first shrinks the surface the real fixes have to reason about.

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

Expected suite delta after Tier 0: **506 → 504** (two `StoredCalibrationProducerTest` assertions removed, no test methods lost) — assuming you delete assertions rather than whole `@Test` methods.

---

## Tier 1 — Correctness: stored data manufacturing a live trust verdict

**This is the highest-severity item in the audit and it is not a deletion — it is a behaviour fix.** Do it after Tier 0, before Tier 2.

**The defect.** `CalibrationRestorePolicy.RESTORE_AS_STALE` is the default in the model (`ProjectData.kt:489`) *and* the missing-key fallback (`ProjectStorage:896`), so it covers every legacy save. `AppRootController.decideCalibrationRestore` (`:243-285`) has **no branch for it** — both range predicates are gated on `== RESTORE_IF_COMPATIBLE` (`:266`, `:275`), so it falls to `else → RESTORE` (`:281-283`). The side effect then calls `registerCalibrationSession`, which re-stamps `capturedSessionKey = currentSessionKey` (`UsbSessionManager:387`) and, for a COMPLETE session on an open matching device, sets `readiness = VALID, trustDowngraded = false` (`:418-428`).

Result: a policy named *restore as stale* restores at **full trust, stamped as captured in the current session**, permanently immune to the previous-session staleness check at `UsbSessionManager:1319`. If the stored span doesn't cover the sweep, `SweepController:154` silently declines to apply the correction — **uncorrected sweep, VALID badge**.

**Decision required (yours, not mine):** `CalibrationRestoreAction` (`AppRootController.kt:57-60`) is `{ CLEAR, RESTORE }`. The honest fix needs a third member — `RESTORE_AS_STALE` — that maps to a `UsbSessionManager` entry point registering with `readiness = STALE, trustDowngraded = true` and **without** re-stamping `capturedSessionKey`. My recommendation: add it. It is also exactly the member Tier 2 needs, so the two fixes share one enum change.

**Touch list**

- `features/app/AppRootController.kt` — enum `:57-60`; add the `RESTORE_AS_STALE` branch in the `when` at `:243-285`; log-line builder `:305-329` (add the new action).
- `domain/testing/UsbSessionManager.kt` — new registration entry point alongside `registerCalibrationSession:351` / `registerSimulatedCalibrationSession:465`; must *not* overwrite `capturedSessionKey`.
- `features/app/AppRootScreen.kt:373-395` — third `when` branch.
- **`AppRootControllerTest.kt:270-279`** — `decideCalibrationRestore_restoresAsStaleWhenHardwareMatches` currently asserts `RESTORE` and deliberately passes a 999–1000 MHz calibration for a 14.2 MHz target ("range ignored for RESTORE_AS_STALE"). The test *name* already encodes the intent the code never implemented. Update the assertion to the new action; keep the out-of-range fixture — it is the exact case that proves the fix.

---

## Tier 2 — The scoping fix proper (CLEAR vs STALE)

The inconsistency named in the §A audit: `UsbSessionManager.refreshCalibrationStateForCurrentSession` (`:1282-1350`) resolves every scope violation as `STALE + trustDowngraded` and keeps the coefficients — "flag, don't reject". `decideCalibrationRestore` resolves the same class of violation by **deleting** the live calibration, in four of five branches (`:244-279`), including when the project simply has no stored calibration at all (`:244-246`).

Compounded by `StoredCalibrationProducer` being in-memory-only (`:32-36`): a fresh calibration isn't on disk until an explicit project Save, so *calibrate → open a project → wipe* loses it with nothing to restore from.

**Decision required:** which of the four `CLEAR` reasons should become `RESTORE_AS_STALE` (or "leave live state alone"), and specifically whether `no-stored-calibration` should stop touching live state entirely. My recommendation: `no-stored-calibration` → **no-op** (the project has nothing to say about the instrument); `hardware-name-mismatch` → stays `CLEAR` (applying a NanoVNA cal to a LiteVNA is worse than losing it — `EffectiveHardwareResolver:176-179` argues this explicitly and I agree); `policy-do-not-restore` → `CLEAR`; the two `RESTORE_IF_COMPATIBLE` range/completeness failures → `RESTORE_AS_STALE`.

**Also in scope here:** the three unconditional wipes with no guard whatsoever — `AppRootScreen.kt:83` (`enterWizardMode`), `:92` (`enterRfTestWizardMode`), `:114` (`enterUnknownDiscoveryMode`, reached from `:217`). These destroy a live VALID calibration on mere navigation, consulting nothing. They are the harshest form of the bug and the cheapest to fix.

**Guard tests:** `AppRootControllerTest.kt` (30 `@Test`), plus `DashboardControllerTest:90/108` and `LoadProjectControllerTest:89` construct `ProjectCalibrationData` fixtures and will surface signature changes.

---

## Tier 3 — Latent leaks (defer if next session runs short)

| Item | Anchor | Fix |
|---|---|---|
| Design-time OSL gate | `project/ProjectPageScreen.kt:889` — `project.supportsOslCalibrationOrDefault` | Route through `EffectiveHardwareResolver.resolveCapabilityProfileForProject(project).supportsOslCalibration`. **This is the last real consumer of the `…OrDefault` family** — fix it and Tier 0 can take `supportsOslCalibrationOrDefault` too. |
| Duplicate carries another project's calibration | `storage/ProjectStorage.kt:263-276` | Copies `storedCalibrationSession` verbatim (coefficients + `capturedSessionKey`), resetting only `restoredFromStorage`. Decide: strip stored calibration on duplicate, or keep. |
| Confident-wrong hardware default | `features/app/AppRootController.kt:88`, `:101`, `:118` | All three factories hardcode `testHardwareProfile = NANOVNA_H4`, so every Lab / RF-test / discovery project claims NanoVNA. Safe *only* because `EffectiveHardwareResolver` overrides at read time — which is why any direct project-profile read is wrong by construction. **Root cause of the recurring Finding #7.** Consider a nullable/UNSPECIFIED profile. Touches `ProjectData` → plan mode per CLAUDE.md. |

---

## Do NOT cut

- **`CalibrationSession.capturedSessionKey`, `capturedProtocolFamily`, `capturedInstrumentIdentityText`** (`model/testing/CalibrationSession.kt:76-78`) and their serialization (`ProjectStorage:913-915/934-936`) — these feed the staleness detector at `UsbSessionManager:1319/1337`. They look like frozen live state; they are the evidence Tier 1/2 need.
- **`ProjectCalibrationData.lastCalibrationSavedEpochMs`** — currently unread, but it is the natural input to any future age-based staleness rule. Keep unless you decide against that rule.
- **`SweepHardwareIdentity` and the whole provenance path** (`domain/testing/SweepHardwareIdentity.kt`, `SweepWorkspaceController:840/878`, `SweepWorkspaceViewModel:290`) — this is the **correct** discipline and the template for the calibration fix: *resolve from live truth at write time, store the resolved fact, never restore it into live state.* It is what §10c.7 fixed; don't disturb it.
- **`ProjectSweepHistoryEntry` / `DiscoverySnapshot` / `TestData`** — records of past measurements, correctly persisted.
- **Sweep config path** — point count derived at `UsbVnaSweepDataSource:249-260`, sweep window at `SweepGraphScreen:114-122`, calibration span at `CalibrationSessionFactory:81-99`. All already read effective/live truth. Clean; leave alone.

---

## Verification recipe

1. `.\gradlew.bat test` after **each** tier — expect 504 after Tier 0 (see delta note), then a net increase as Tier 1/2 add cases.
2. `.\gradlew.bat assembleDebug`.
3. Off-bench repro for Tier 2, no VNA required: calibration wizard → "Simulate O/S/L capture" chip → confirm VALID → open a project with no stored calibration → observe whether live calibration survives. `adb logcat -s CalRestore` prints the deciding predicate (`AppRootScreen:366-371`).
4. Bench-only: real LiteVNA64 at 14.2 MHz, per `claude/hardware-bringup-litevna64.md`.
5. Commit per tier (CLAUDE.md auto-commit), and keep each tier independently bisectable — no history rewrites.

**Sequencing rationale:** Tier 0 is reversible and touches no behaviour, so it de-risks reading the code during Tier 1/2. Tier 1 and Tier 2 share the `CalibrationRestoreAction` enum change — doing Tier 1 first means Tier 2 inherits a working `RESTORE_AS_STALE` path instead of inventing one. Tier 3's `ProjectPageScreen:889` fix is what finally frees the last `…OrDefault` accessor, so a second small Tier 0 sweep at the end is expected.

---

## Decisions pending

Two decisions are yours before cutting starts:

**(a)** Add `RESTORE_AS_STALE` to `CalibrationRestoreAction`.

**(b)** Which of the four `CLEAR` reasons become non-destructive.

My recommendations are inline above.
