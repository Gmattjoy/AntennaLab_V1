# Resume here — session snapshot

Last updated: 2026-08-09 (office day). Updated in place each session; this is the
cold-start entry point.

## State

- `main` at **853ce0b**, pushed, working tree clean (`?? .claude/` is untracked
  tooling config, not project work).
- Suite **552 tests / 0 failures / 0 errors / 0 skipped**, counted from JUnit XML
  after `--rerun` (see § Reading the suite total — the console line lies).
- Session type: **OFFICE DAY** — no VNA hardware attached. **P5 closed out**, plus
  the colour system and the app's first collapsible primitive.

### P5 — COMPLETE

Six pieces of work, all landed and pushed:

| Piece | What it did |
|---|---|
| **5a** settings store | `model/settings/AppSettings` + `storage/AppSettingsStore` (`files/settings.json`, hand-rolled `org.json`, no DataStore) behind `SettingsRepository`. `load()` never throws — it is on the launch path. |
| **5b** boundary teardown | Deleted `ProjectData.uiState` / `ProjectUiState` — serialized every save, read by zero consumers. **None of its three fields moved to settings**, because none had a consumer; shipping them inert would have repeated the defect. Established the boundary rule and the add-with-consumer discipline. |
| **5c** defaults | `defaultTargetFrequencyMHz` + `defaultInstrument`; factories seed from settings (default target became 146.0). Also fixed the simulated dip to track the sweep window instead of sitting at 20 m. Tier-3 hardware default only partly addressed — see OPEN. |
| **5d** theme toggle | `themePreference` SYSTEM/DARK/LIGHT resolved by the pure `resolveDarkTheme` at `MainActivity`. **The bigger change: settings became observable** — `SettingsRepository`'s cache is `by mutableStateOf`, the app's first observable state. Ended dark-only. |
| **colour system** | `9a6b1b7` / `bec034e` / `f18b34e`. One accent definition (`AccentOrange` `#FF5C00` / `OnAccentOrange` `#3A1500`), solid-selected vs orange-outlined owned by `SelectionButtonStyle`, hero-CTA rule keyed on group membership, neutral lighter headings. **Contains a deliberate reversal of the "don't touch `colorScheme.primary`" instruction** — read `ui-audit.md` §2b before restoring anything teal. Status green KEPT on purpose. |
| **5f** app-analysis collapse | `cb5b8b4` / `d6da85d` / `853ce0b`. Collapse state + settings seed, then **`CollapsibleSection`, the app's first collapsible primitive** (48 dp header), then the interpretation panels wrapped in "App analysis". |

Also landed: `999f341` deleted the dead `project/ProjectSection.kt` duplicate —
identical enum to `model/ProjectData.kt:660` with zero consumers, since
`ProjectPageScreen` imports the `model` one. Closes the item `ui-audit.md` had
just logged.

### Forward planning now has three companion docs

| Doc | Covers |
|---|---|
| `TESTING_ROADMAP.md` | hardening — test inventory, priorities, extraction backlog |
| `claude/ui-redesign-spec.md` | P0–P6 UI phases |
| **`claude/feature-backlog.md`** | **P7+ net-new capability** — the forward companion to the other two. Ranked ship-to-parity-first; P7 state-restore is the flagged top item. |

### Landed 2026-08-04

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
| Measured per-screen drift + the colour system (§2b) | `claude/ui-audit.md` |
| P7+ net-new capability backlog | `claude/feature-backlog.md` |

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

### ▶ Immediately next: slice 5e — the Simple/Full toggle

**The last of the original slice 5**, and the only piece of it still outstanding
now that 5f took "app analysis collapsed by default". **Plan mode** — it adds a
field and spans several files.

- A **layout-mode field**, and it must be a SEPARATE non-null field, never a value
  of the tap-to-expand overlay. §2.2's non-conflation is currently true *by
  construction* because 4a made `expandedChartKind` a nullable overlay; a
  layout-mode that folded "expanded" into itself would destroy that.
- **AUTO = 600 dp** breakpoint as the default.
- **Simple = SWR + Smith**, capability-gated (Smith is not universal — route the
  read through `EffectiveHardwareResolver`, do not branch the UI on profile).
- **Global pin via the store** — a "how I like the app" preference, so it is a
  settings field, not `ProjectData`. Add it with its consumer in the same slice.
- **Lands the homeless gesture from 4c-ii:** switching focus directly from one
  expanded chart to another. The controller case is real and 4a-tested
  (`different → switch`), but the expand panel replaces the grid, so no second
  chart is on screen to tap. Simple/Full is the layout that can show both.
- The `ChartKind` ↔ `SweepDisplayMode` fork gets decided here (measured at
  `9712f58`: **192 `SweepDisplayMode` occurrences across 13 files** vs 84
  `ChartKind`), and spec open questions 1–3 land.

**Then:** P6 (create-antenna wizard + project manager), then P7 from
`claude/feature-backlog.md`.

### Reusable now — check these before writing anything new

- **`CollapsibleSection`** (`5f`) — the app's first collapsible primitive, 48 dp
  header. Use it rather than a second collapse mechanism.
- **`SegmentedChoiceButton`** — the segmented control, with the
  solid-selected/orange-outlined rule already applied via `SelectionButtonStyle`.
- **`SettingsRepository`** — now observable; composables reading `current()`
  recompose on `update()`. No manual refresh plumbing needed.

### Open cleanups — small, unbundled, none blocked

- **`SegmentedChoiceButton` has no minimum height** — ~40 dp, below the 48 dp
  `AntennaLabTouch.min` floor. The colour work did NOT close it
  (`SelectionButtonStyle` sets colour and elevation, never size). Fixing it
  resizes 12 sweep-stack buttons plus the theme control; wants its own
  predicted-change gate.
- **`SweepScalarTraceView` y-label column vertical offset** — twin of the tick-row
  defect 3b-i fixed, and the version `PhaseTraceCell` already fixed for itself.
  Carried forward through 3b-i, 4b and 4c; still untouched.

### Older office-day items

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
  controller (`c5d0820`), slice 4b `PhaseTraceCell` full-width parity + tick row
  (`5b25fe3`), slice 4c-i strip-follows-flag (`d90873a` + `de192c2`), slice 4c-ii the
  expand layout (`9712f58`).
  **TAP-TO-EXPAND IS COMPLETE.** Repo at `9712f58`, suite **508 / 0 fail / 0 err**.
  **4a** put `expandedChartKind: ChartKind?` on `SweepWorkspaceState` as a nullable
  OVERLAY on the layout mode, never a value of it, so §2.2's non-conflation holds by
  construction.
  **4b** made `plotInsetsFor(PHASE)` honour `compact` (50/10 grid, 66/10 full) so an
  expanded phase chart lands on the same gutter as an expanded scalar one, and gave
  `PhaseTraceCell` the frequency-tick row it never had. SCALAR and PHASE share one
  merged `when` arm now, so the 3b-i unification is guaranteed by construction — the
  two guard tests are true-by-construction and repurposed as re-split tripwires.
  **4c-i (geometry)** added the pure `ChartLayoutMath.cellsAreCompact(chartCount)` =
  `gridColumnCount > 1`, so "expanded" and "sole chart" are one expression rather than
  two rules, and made the band strip follow the cell's own flag and name its renderer —
  discharging the marker 4b left. It also **split the overloaded chrome flag**:
  `SweepScalarTraceView.showHeaderAndFooter` (default `!compact`) separates cell-hosting
  from width, because the header/footer are suppressed since THE CELL TITLES THE CHART,
  which is true at any width. Without that split a full-width sole chart could not take
  the wide gutter without gaining a second title.
  **4c-ii (wiring)** wired `onCellTap → toggleExpandedChart` and added
  `ExpandedChartPanel` in `SweepChartGrid.kt` (kept there to reuse the private
  `chartTitle`/`scalarModeFor` and the colour plumbing rather than duplicate them into
  the 45 KB screen). Three return routes: tap the focused chart, a "Back to grid"
  `AppActionButton` (56 dp), and the **first `BackHandler` in the app**.
  **⚠ The `BackHandler`'s `enabled` gate is load-bearing beyond the usual reason:** this
  screen has no `navigationIcon` and navigation is a `showSweep` boolean in
  `ProjectPageScreen`, so system back here EXITS THE APP (verified on device). An ungated
  handler would swallow the only gesture that leaves. Gated, unexpanded back is unchanged.
  **Next: slice 5e — the Simple/Full toggle. Scoped now; see § ▶ Immediately next above,
  which supersedes the paragraph below.** Part of the original slice 5 has already
  shipped: "app analysis" collapsed-by-default landed as **5f**, so only the toggle
  itself remains.
  AUTO default; the `ChartKind` ↔ `SweepDisplayMode` fork finally gets decided here
  (measured at `9712f58`: **192 `SweepDisplayMode` occurrences across 13 files**, 154 in
  `main` alone, against 84 `ChartKind` — the reason slice 1 refused to extend the legacy
  enum); "app analysis" collapsed-by-default; and spec open questions 1–3 land. §2.2: the
  toggle and tap-to-expand are two distinct controls, do not conflate them — 4a's nullable
  overlay is what keeps that true in the code, so slice 5's layout mode must be a SEPARATE
  non-null field.
  Slice 5 also owns the one behaviour 4c-ii could not give a gesture: switching focus
  directly from one expanded chart to another. The controller case is real and 4a-tested
  (`different → switch`), but the panel replaces the grid, so no second chart is on screen
  to tap. It needs a layout that shows both.
  **Off-bench — this does NOT need a bench session.** That claim (still in the
  spec's older text) predates the debug simulated-sweep route (`1089e32`), which
  produces a full `SweepResult` with no VNA attached. Slices 1–3b-i were all built
  and verified on a bare API 36 emulator. Only real-data *fidelity* wants hardware.

## NEXT — bench day (needs LiteVNA64 + NanoVNA-H4)

Procedure and results log: `claude/hardware-bringup-litevna64.md`.
**Build + reinstall first**, then work this list:

1. **Finding #7 remainder — the confident-wrong `NANOVNA_H4` factory default.**
   Tier 3 of the calibration teardown is 3-of-4 closed; this is the sole open
   item. The nullable-profile question is the shape of the fix (~10 files, plan
   mode). See `claude/calibration-teardown-plan.md`.
2. **OSL at 145 MHz.** Passed on a real LiteVNA64 at 14.2 MHz (correction applied,
   VALID, "Live Ready"); 145 MHz is still unverified.
3. **Status green — visual confirm.** Live / OK / TRUSTED is **code-verified
   only**: with no device the status text reads "Simulated" / "ERROR", which
   routes to the accent and magenta branches, so the green path never fires on the
   emulator. `InstrumentGreen` intact at `SweepGraphWidgets.kt:151,180` and
   `SweepGraphScreen.kt:1248`, `semantic.success` untouched. Until a bench run,
   treat "green still means good in the UI" as asserted, not demonstrated.
4. **Finding #10 / `sweepPoints=101`** — does the NanoVNA-H4 honour a host-set
   point count, or free-run like the LiteVNA? Still unanswered, no independent
   corroboration. Also H4 identity / Block C, and the §10b re-run.

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
