# Resume here — session snapshot

Last updated: 2026-07-30 (office day). Updated in place each session; this is the
cold-start entry point.

## State

- `main` at **730a112**, pushed, working tree clean.
- Suite **489 tests / 0 failures / 0 errors / 0 skipped**, 41 classes.
- Session type: **OFFICE DAY** — no VNA hardware attached. UI Phase 3 (chart
  components + `.s1p` export delivery layer).

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

**Compose components — previews only, NOT wired into the viewer** (that is
Phase 4). `features/testing/charts/`: `SweepChartGrid`, `PhaseTraceCell`,
`MarkerReadoutTable`, `BandAxisOverlay`.

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
  over it, so adding one ripples through ~7 files and drags the axis math in
  (phase wants a fixed symmetric axis, unlike every auto-scaled SWR/RL/R/X
  axis). **Unification is deferred to Phase 4.**

---

## OPEN — device-verification items

All at standing **CODED, UNVERIFIED, PENDING DEVICE**. Treat as unproven, not
assumed-good. Tracked in `claude/ui-redesign-spec.md` under "Still-open
DEVICE-VERIFICATION items" — that section was retitled from "Still-open HARDWARE
items" this session, because the old heading let a coded-but-never-run IO path
sit outside the list and read as done.

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
- **Phase 4 — Sweep Viewer.** Simple/Full toggle (AUTO default), tap-to-expand
  (transient focus — §2.2 says these are two distinct controls, do not conflate
  them), the multi-chart grid from Phase 3, "app analysis" collapsed by default,
  and unifying `ChartKind` + `SweepDisplayMode`. Largest and riskiest surface;
  wants real devices for data verification, so schedule its review against a
  bench session rather than headless.

## NEXT — bench day (needs LiteVNA64 + NanoVNA-H4)

Unchanged. Procedure and results log: `claude/hardware-bringup-litevna64.md`.
**Build + reinstall first**, then: H4 identity / Block C, A3 calibration restore,
Block B cross-family `CLEAR`, §10b re-run, `sweepPoints=101`.

---

## ON HOLD — awaiting a bug-or-intended call. Do NOT action.

**Restore-precedence collision.** Opening a project via Project Manager
unconditionally resets live calibration to the project's context, *including
clearing it* when the project carries none. Live-state only — no data
corruption, nothing persisted wrongly. Needs the user's decision on whether
project-open should defer to a live calibration or keep overriding it; do not
change the precedence until that call is made.
