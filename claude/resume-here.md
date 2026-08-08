# Resume here — session snapshot

Last updated: 2026-08-04 (office day). Updated in place each session; this is the
cold-start entry point.

## State

- `main` at **1089e32**, pushed, working tree clean.
- Suite **506 tests / 0 failures / 0 errors / 0 skipped**.
- Session type: **OFFICE DAY** — no VNA hardware attached. Off-bench export path
  (debug-sim sweep, provenance headers, MediaStore MIME fix) + a latent crash fix.

### Landed 2026-08-04

- **`b5b525d` — nested-scroller crash in the CSV preview panel, FIXED.**
  `SweepCsvPreviewPanel`'s `Column` carried `verticalScroll` nested under
  `SweepGraphScreen`'s outer `verticalScroll` (`:282`), which hands down an
  infinite max-height; `checkScrollableContainerConstraints` then threw
  `IllegalStateException`. De-nested — the preview is capped at 40 rows and never
  needed independent scroll. **Latent pre-existing bug:** the panel is gated on
  `sweepResult != null`, so it was unreachable off-bench until the debug-sim
  toggle made it reachable. Emulator API 36 repro passed — preview opens, pid
  stable, crash buffer empty.
- **`1089e32` — the off-bench export batch:** debug-sim toggle, CSV + `.s1p`
  provenance headers, lock-message fix, MediaStore MIME `.txt` fix, and the
  `runUsesSimulation = demoSweepAllowed && !liveSweepAllowed` live-wins guard.
- **Known accepted seam — `b5b525d` does not compile alone.**
  `SweepToolsWidgets.kt` legitimately belongs to both changes, and whole-file
  staging carried the `SweepCsvExport` import into commit 1 while the class itself
  lands in commit 2. The tip builds clean. History deliberately **not** rewritten;
  bisect across that seam is not a workflow used here. Do not offer to rebase it.

### Reading the suite total — XML only

`./gradlew test` reports `BUILD SUCCESSFUL` with every task `UP-TO-DATE` and runs
**zero tests** when inputs are unchanged. The console summary can reflect a run
from days earlier. Always:

```
.\gradlew.bat testDebugUnitTest --rerun
grep -ho 'tests="[0-9]*"' app/build/test-results/testDebugUnitTest/*.xml
```

Sum those, and confirm the task line reads `> Task :app:testDebugUnitTest`
without `UP-TO-DATE`. Check the report file mtimes are from this run.

---

## Where things live

Each topic has ONE home of record. This file points at them; it does not copy
them, so nothing here can drift out of sync with the source.

| Topic | Home |
|---|---|
| Bench procedure, findings, per-run results | `claude/hardware-bringup-litevna64.md` |
| — next bench day handover | §0 |
| — closed: ~201-point free-run (known HW limit) | §5 |
| — Findings: capability profile, TDR layers, trust | §7 (7.1–7.5) |
| — Findings from A2, incl. the data-integrity one | §10c (10c.1–10c.8) |
| — 50 Ω load / AR-771 test-antenna behaviour | §10c.5, §10b |
| — per-run results table | §11 |
| Test inventory, priorities, extraction backlog | `TESTING_ROADMAP.md` |
| Working rules (one task, plan mode, Robolectric) | `TESTING_ROADMAP.md` § Working rules |
| Architecture, layer rules, build/test, conventions | `CLAUDE.md` |
| UI redesign: state, direction, phases, open questions | `claude/ui-redesign-spec.md` |

### Finding numbers → where they are

The numbered Findings are cited in commit messages and conversation but are not
all in one section, and **the numbering is not applied consistently across the two
docs** — #6 and #10 are labelled in the bring-up doc itself, while #7 and #8 are
numbered only in the roadmap and appear in the bring-up doc as unlabelled
write-ups. Verified against the tree:

- **#6** — real LiteVNA sweeps persisted as `Hardware: SIMULATED` (data
  integrity). Labelled in the bring-up §10c.7 heading itself.
- **#7** — capability profile followed the STALE project profile. **RESOLVED**
  via `domain/testing/EffectiveHardwareResolver` (single resolution point,
  three-tier). Numbered in `TESTING_ROADMAP.md` § "▶ Next up"; the write-up is
  bring-up §7.1, which carries the title but **not** the number. §7.3
  back-references it as "same disease as Finding #7".
- **#8** — LiteVNA incomplete sweep + parser filtering. Closed as interim plus a
  known hardware limitation. Numbered in `TESTING_ROADMAP.md` Priority 4
  (checklist items) and § "▶ Next up"; the subject matter is bring-up §5, which
  is **not** labelled with the number.
- **#10** — does the NanoVNA-H4 honour `sweepPoints=101`, or free-run like the
  LiteVNA? **Still unanswered, no independent corroboration.** Labelled in
  bring-up §0 (Step 3) and §9b. Same item as the H4 entry under OPEN below.
- **There is no Finding #9** — recorded so nobody hunts for one. (Grepping the
  tree for it now matches only this line.)

---

## Shipped this session — UI Phase 3

Spec: `claude/ui-redesign-spec.md` (Phase 3 entry + §2.3). Phases 0–2 were done
previously.

### Slice A — `5790f80` — pure builders, no Compose, no IO

- `model/AmateurBand.kt` — `AmateurBand`, `IaruRegion`.
- `domain/analysis/AmateurBandPlan.kt` — all three IARU region tables,
  `bandAt` / `bandLabelAt` / `bandsOverlapping`. **`DEFAULT_REGION = REGION_3`**
  (Melbourne). Region enum carried from the start deliberately: a single-region
  table has no concept to extend. Documented as region-level allocations,
  explicitly **not** a legal band-plan reference.
- `domain/testing/SweepMarkerMath.kt` — the §2.3 readout row (|Z|, R+jX, Q,
  series-equivalent Cs/Ls auto-scaled pF/nF and nH/µH, RL, phase, band) as raw
  values **and** display strings via `buildMarkerReadout`.
- `domain/testing/TouchstoneExport.kt` — `buildS1p`, pure string building.
  Provenance comments carry instrument, span, calibration state and any
  incomplete-sweep count.

**Two facts that constrain future work here:**

1. **Γ comes from `OslCalibrationEngine.gammaFromPoint`, not
   `SweepPoint.s11PhaseDegrees`.** That field defaults to `0.0` and is only
   meaningful when the device reports it (`SweepResult.supportsS11Phase`). Using
   it would silently produce a flat/wrong phase trace on devices that don't.
   `gammaFromPoint` is the same exact R/X→Γ path `CalibrationCorrector` uses, so
   marker phase and `.s1p` values are correct on calibrated sweeps.
2. **`.s1p` is S11 only — closed by the file format, not by preference.** It is a
   one-port file by definition. S21 would need `.s2p`, which requires the full
   2×2 set (S11 S21 S12 S22); both devices measure forward S21 only, no S12/S22,
   so a valid `.s2p` is not producible without fabricating terms. Header is
   `# Hz S RI R 50` (NanoVNA-Saver's convention, per §2.3 muscle memory), whole
   Hz, 6-decimal RI, CRLF. Do not reopen this as an open question.

### Slice B — `a802f38` (code) + `730a112` (spec) — delivery + Compose

**Delivery: save-to-Downloads THEN share.** Two API tiers, because
`MediaStore.Downloads` is API 29+ and this project's **minSdk is 26** — public
Downloads without legacy `WRITE_EXTERNAL_STORAGE` is impossible on Android
8.0/8.1/9.

| Tier | Write target | URI source | FileProvider |
|---|---|---|---|
| 29+ | public Downloads, `Download/AntennaLab` | MediaStore `insert()` returns a `content://` URI | **no** |
| 26–28 | `getExternalFilesDir(DIRECTORY_DOWNLOADS)`, no permission needed | `FileProvider.getUriForFile` | **yes** |

- `domain/testing/SweepExportPlan.kt` — the tier branch as a **pure function of
  `sdkInt`**, which is why the least-exercised path is JVM-tested rather than
  device-only. `storage/SweepExportWriter.kt` only *executes* a plan.
- `domain/testing/SweepExportNaming.kt` — single naming home
  (`<project>_<centre freq>_<timestamp>.s1p`, sanitising, truncation,
  `nextAvailableName` collision suffixing). `TouchstoneExport.suggestFileName`
  delegates here.
- `Outcome.Saved.isPublicDownloads` carries which tier ran, so the UI states
  where the file actually landed. **It must never claim a public Downloads save
  on the 26–28 tier.**
- Manifest `<provider>` for `androidx.core.content.FileProvider` +
  `res/xml/file_paths.xml`, scoped to the app-specific Downloads dir only.
  **No storage permission on any tier** — verified against the merged manifest.

**Compose components — ALL FOUR NOW WIRED INTO THE VIEWER** (Phase 4 slices 1–3a;
this said "previews only" until 2026-08-08). `features/testing/charts/`:
`SweepChartGrid` + `PhaseTraceCell` (`0d5eb60`), `MarkerReadoutTable` (`e91b45d`),
`BandAxisOverlay` (`8ef5520`).

- The grid **wraps** the existing `SweepScalarTraceView` / `SweepSmithChartView`
  rather than redrawing traces. `PhaseTraceCell` is the only new renderer.
- Components are thin: every value is computed in `domain/` first.
  `domain/analysis/ChartLayoutMath.kt` holds band-span fractions, the fixed
  −180..180 phase axis, grid shape, and `availableChartKinds` capability gating.
- **One existing-UI touch:** an "Export .s1p" card in `SweepGraphScreen` beside
  the CSV preview — the single reachable entry point, needed so the export can
  be device-verified at all. Plus additive `heightDp` params on the two chart
  views (they hardcoded 240/280.dp and could not be sized into a cell; defaults
  preserve behaviour, no call site changed).
- **`ChartKind` deliberately does NOT extend `SweepDisplayMode`.** That enum has
  no PHASE value and `SweepGraphMath.getDisplayValue` is an exhaustive `when`
  over it (`:289-310`, no `else`), so adding one is an immediate compile break and
  ripples through 11 main + 2 test files (183 occurrences), dragging the axis math
  in (phase wants a fixed symmetric axis, unlike every auto-scaled SWR/RL/R/X
  axis). **Unification is deferred to Phase 4 slice 5**, where the Simple/Full
  toggle actually needs one chart list. Wiring the grid never required it:
  `SweepChartGrid.scalarModeFor` already maps SWR/RETURN_LOSS onto the legacy
  enum, while SMITH and PHASE bypass it entirely.
- **Plot geometry is a shared contract now** — `ChartLayoutMath.plotInsetsFor`
  (slice 3a, unified in 3b-i). Three renderers used to hardcode independently
  where their plotting area starts; anything drawn alongside a trace must inset by
  the same amount or it silently mis-aligns. PHASE and SCALAR-compact are
  deliberately identical (50/10) — a test pins that equality.

---

## OPEN — device-verification items

All at standing **CODED, UNVERIFIED, PENDING DEVICE**. Treat as unproven, not
assumed-good. Tracked in `claude/ui-redesign-spec.md` under "Still-open
DEVICE-VERIFICATION items" — that section was retitled from "Still-open HARDWARE
items" this session, because the old heading let a coded-but-never-run IO path
sit outside the list and read as done.

### Two stale verification assumptions — CORRECTED 2026-08-04

1. **There is no CSV file export. Do not write a step that pulls a CSV file.**
   The "Export CSV" button in the Controls strip is a preview **toggle** — it
   flips to "Hide CSV" and shows the on-screen panel. CSV *file* export is Slice C
   and is **unbuilt**: `domain/testing/SweepCsvExport.kt` is a provenance-header
   builder only (its own header says so), and CSV row building is still inline in
   `SweepCsvPreviewPanel`. So any step phrased "pull the CSV file and confirm the
   `# hardware=` / `calibrated=` line" is **not performable** — that line exists
   on screen, not on disk.
   **Provenance-on-disk is verified through the `.s1p` writer instead**, which
   carries the matching `! Instrument: SIMULATED` and `! Calibration: none
   (uncalibrated)` lines. The two formats are deliberately kept in agreement.
2. **"Real sweep wins over sim" (this session's "Step 5") is NOT dischargeable on
   the current emulator.** `Medium_Phone_API_36.1`'s USB session sits in **ERROR**,
   and that state yields `dataSourceKind == SIMULATED` on its own — so the
   simulated path is reachable *without* the debug toggle, and there is no live
   path present for a real instrument to displace. The emulator therefore cannot
   stand in for this check. It stays genuinely **hardware-pending**; see bring-up
   §10.

- **Export, API 29+ tier.** Run a sweep (simulated is fine) → Export .s1p →
  confirm the status names public Downloads → file really appears in
  `Downloads/AntennaLab` via a file manager → share to one target → open the file
  and check the header is `# Hz S RI R 50` and the first data row's frequency is
  whole Hz. The `IS_PENDING` set-then-clear is what makes the file visible; if
  that regresses the entry exists but is permanently invisible, and no unit test
  can catch it.
- **Export, API 26–28 tier.** Emulator is fine. The branch has **never
  executed** — only its tier *decision* is unit-tested (`SweepExportPlanTest`:
  sdkInt 26/27/28 → `APP_SPECIFIC`, `isPublicDownloads = false`). The IO, the
  FileProvider grant and the share hand-off have not run once. Verify the UI does
  **NOT** claim public Downloads here; the honesty labelling is the whole reason
  the tier is distinguished.
- **H4 identity fix** — routes on `DriverProfile.protocolType`
  (`domain/testing/IdentityProbeRouting.kt`) rather than on model name. Unchanged
  from before this session; still needs a NanoVNA-H4.

---

## NEXT — office day (no hardware needed)

- **Slice C — CSV extraction.** CSV row building is still inline in the
  Composable (`SweepCsvPreviewPanel`, `SweepToolsWidgets.kt:572`, and
  `CsvPreviewCard` in `SweepGraphScreen.kt`). Extract to a pure helper beside
  `TouchstoneExport` so both formats share one tested seam, and route it through
  `SweepExportWriter` so CSV becomes a real export too. **Touches an existing
  file → use plan mode.** Deliberately excluded from slice B.
- **Phase 4 — Sweep Viewer. IN PROGRESS, sliced.** Landed: slice 1 the multi-chart
  grid + phase cell (`0d5eb60`), slice 2 the marker readout table (`e91b45d`),
  slice 3a the shared plot-inset contract + first band overlay (`8ef5520`), slice
  3b-i grid-cell geometry unification + tick-row alignment (`a3de767`), slice 3b-ii
  per-cell band strips in the grid (SWR/RETURN_LOSS/PHASE, never SMITH — gated by
  the pure `ChartLayoutMath.hasFrequencyAxis`), slice 4a tap-to-expand state +
  controller (`c5d0820`), slice 4b `PhaseTraceCell` full-width parity + tick row.
  Suite 483 → 507 across the seven.
  **4a** put `expandedChartKind: ChartKind?` on `SweepWorkspaceState` as a nullable
  OVERLAY on the layout mode, never a value of it, so §2.2's non-conflation holds by
  construction. Nothing renders it — `onCellTap` is still `null`.
  **4b** made `plotInsetsFor(PHASE)` honour `compact` (50/10 grid, 66/10 full) so an
  expanded phase chart lands on the same gutter as an expanded scalar one, and gave
  `PhaseTraceCell` the frequency-tick row it never had. SCALAR and PHASE share one
  merged `when` arm now, so the 3b-i unification is guaranteed by construction — the
  two guard tests are true-by-construction and repurposed as re-split tripwires.
  **Next: slice 4c** the expanded layout — `onCellTap` wiring, focused render,
  `BackHandler`, return affordance. **Read the marker at `SweepChartGrid`'s band-strip
  block first:** it hardcodes `compact = true`, correct while every cell is half-width,
  but an expanded cell at `compact = false` would keep its strip at 50 while its plot
  starts at 66 — a 16 dp misalignment on the focused chart. Same latent case already
  live: a lone supported chart takes the full row yet still renders compact, so decide
  "expanded" and "sole chart" together rather than twice. Then **slice 5** the
  Simple/Full toggle (AUTO default), which is also where `ChartKind` +
  `SweepDisplayMode` unification and "app analysis" collapsed-by-default get
  decided, and where spec open questions 1–3 land. §2.2: the toggle and
  tap-to-expand are two distinct controls, do not conflate them.
  **Off-bench — this does NOT need a bench session.** That claim (still in the
  spec's older text) predates the debug simulated-sweep route (`1089e32`), which
  produces a full `SweepResult` with no VNA attached. Slices 1–3b-i were all built
  and verified on a bare API 36 emulator. Only real-data *fidelity* wants hardware.

## NEXT — bench day (needs LiteVNA64 + NanoVNA-H4)

Procedure and results log: `claude/hardware-bringup-litevna64.md`.
**Build + reinstall first**, then: H4 identity / Block C, §10b re-run,
`sweepPoints=101`.

> **STRUCK 2026-08-08 — ~~A3 calibration restore~~ and ~~Block B cross-family
> `CLEAR`~~ are dead items, not pending ones.** Both describe calibration-RESTORE
> behaviour that the teardown deleted (`227237b` / `02cc9ec`): there is no
> `ProjectData.calibrationData`, no `CalibrationRestorePolicy`, no restore path.
> `CalRestore` now survives only as prose in `BenchStateLog.kt` comments. They
> cannot be verified because the behaviour no longer exists — do not schedule
> bench time for them. Calibration is live-only; see
> `claude/calibration-teardown-plan.md`.

---

## ON HOLD — awaiting a bug-or-intended call. Do NOT action.

**Restore-precedence collision.** Opening a project via Project Manager
unconditionally resets live calibration to the project's context, *including
clearing it* when the project carries none. Live-state only — no data
corruption, nothing persisted wrongly. Needs the user's decision on whether
project-open should defer to a live calibration or keep overriding it; do not
change the precedence until that call is made.
