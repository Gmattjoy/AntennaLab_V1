# UI Design-System Audit

UI design-system audit as-of `be6f343`. Drift catalogued per screen; separated into defects
(fixed in `be6f343`) vs not-yet-redesigned (P4/P5/P6 roadmap).

Companion to `claude/ui-redesign-spec.md`: that doc holds the **agreed direction and phase
order**; this one holds the **measured current state** each phase starts from.

- **Audited:** 2026-08-07, whole `features/` + `project/` + `ui/` Compose tree.
- **Method:** every Compose file read or grepped. Counts are literal source occurrences, not
  estimates — see §Method to refresh them.
- **How to use it:** find the screen you are about to touch, read its row, then check the
  fix-now-vs-expected split before assuming something is a bug. Most drift here is *expected* —
  the screen has not reached its phase yet. Do not "fix" a Tier-A row outside its phase.

---

## 0. Baseline — what compliance means

Phase 0 (`claude/ui-redesign-spec.md` §Phase 0) established:

- **`AntennaLabSpacing`** — 4 dp grid: `xs 4 / sm 8 / md 12 / lg 16 / xl 24 / xxl 32`
- **`AntennaLabTouch`** — `min 48` (accessibility floor) / `comfortable 56` / `field 64`
  (the documented gloved dial)
- **`AntennaLabSemanticColors`** + `LocalAntennaLabSemanticColors`, theme-aware
- Reached via the **`AntennaLabTheme`** accessor (`spacing` / `touch` are plain object getters;
  only `semantic` reads a CompositionLocal)
- Shared primitives **`AppActionButton`**, **`MetricCard`**, **`StatusPill`**
- **Anti-drift rule:** instrument state → chip mapping is the one shared pure
  `InstrumentStatusPresenter.buildStatusChips`

### App-wide totals (as-of `be6f343`)

| Metric | Value | Note |
|---|---|---|
| Files reaching `AntennaLabTheme.*` | **14 of 28** | 46 of 93 refs are inside the token layer / previews / primitives themselves |
| Literal `.dp` | **361** across 28 files | 12 in the token layer → **349 at screen level** |
| Literal `.sp` | **76** across 5 files | 34 in `Type.kt` → **42 bypassing the type scale**, all in the wizard |
| Raw hex `Color(0x…)` | **89** across 7 files | 43 in the theme layer → **46 in screens** |
| `Instrument*` palette refs | **129** across 7 files | theme-blind, dark-only |
| `AppActionButton` call sites | **11** across 4 screens | |
| Raw `Button`/`TextButton`/`IconButton` | **49** across 18 files | |

---

## 1. Per-screen drift catalogue

Counts exclude the theme layer. "Local buttons" = screen-private button composables.

| # | Screen / surface | File | 1 · Buttons | 2 · Spacing | 3 · Colour | 4 · Touch | 5 · Cards | Status |
|---|---|---|---|---|---|---|---|---|
| 1 | **Dashboard** (`home`) | `features/app/DashboardScreen.kt` | `AppActionButton` ×3 (loop) + 1 raw `TextButton` ("See all") | **0 literal dp** — all `spacing.*` | Material roles only | via primitives (`field`/`comfortable`/`min`) | `MetricCard` + `StatusPill` | ✅ **P1 compliant** |
| 2 | **Device Connections** | `features/app/DeviceConnectionsScreen.kt` | `AppActionButton` ×7, no raw | **0 literal dp** (since `be6f343`) | Material roles only | via primitives | `MetricCard` + `StatusPill` | ✅ **P2 compliant** |
| 3 | Chart components | `features/testing/charts/*` (4 files) | none | tokens; 8 literal dp | tokens | tokens | thin, token-based | ✅ **P3 compliant — all 4 WIRED IN** (see note) |
| 4 | **Project workspace** — Overview / Design / Materials / Testing / Notes | `project/ProjectPageScreen.kt` | `AppActionButton` ×1 + **6 local** (`PrimaryActionButton`, `SecondaryActionButton`, `SmallActionButton`, `TabButton`, `ClassificationButton`, `HardwareButton`) + 10 raw | **20 literal dp**, 0 tokens | Material roles | none tokened | bespoke `SectionCard`/`DataRow` ×14 | ⛔ pre-redesign (**P5**) |
| 5 | Project dialogs (Save As, Clear calibration) | `project/ProjectPageScreen.kt:155,199` | raw `TextButton` ×4 | dialog defaults | defaults | defaults | `AlertDialog` | ⛔ pre-redesign (**P5**) |
| 6 | **Sweep Viewer** | `features/testing/SweepGraphScreen.kt` | `AppActionButton` ×2 (`S1pExportCard` only) + 9 raw | **12 literal dp**, 0 tokens | **95 `Instrument*` refs** | none | 20 bespoke cards/panels | ⛔ pre-redesign (**P4**) |
| 7 | Sweep widgets — display modes, controls, summary, session | `features/testing/SweepGraphWidgets.kt` (51 KB) | `SweepWorkspaceModeButton`, `SweepWorkspaceControlButton` + 2 raw | **40 literal dp** | 5 palette refs, threaded as **params** | none | 4 bespoke cards | ⛔ pre-redesign (**P4**) |
| 8 | Sweep tools — markers, trace memory, CSV | `features/testing/SweepToolsWidgets.kt` (36 KB) | `SweepWorkspaceActionButton`/`DisplayButton` (**duplicate #1, 48 dp**) + 2 raw | **33 literal dp** | 11 palette refs | `defaultMinSize(48.dp)` hardcoded | 4 panels + 4 shared table rows | ⛔ pre-redesign (**P4**) |
| 9 | Sweep instrument shell | `features/testing/SweepInstrumentUi.kt` | `SweepWorkspaceActionButton`/`DisplayButton` (**duplicate #2, 50 dp**) + 2 raw | **54 literal dp — highest in app** | 1 palette ref | `defaultMinSize(50.dp)` | `SharedInstrumentCard`, `SharedTwoValueRow`, `SweepStatusChip` | ⛔ pre-redesign (**P4**) |
| 10 | Sweep diagnostics | `features/testing/SweepTuningWidgets.kt` | none | 2 dp | 6 palette refs | — | 1 panel | ⛔ pre-redesign (**P4**) |
| 11 | Sweep dialog "Save partial sweep?" | `SweepGraphScreen.kt:595` | raw `TextButton` ×2 | defaults | defaults | defaults | `AlertDialog` | ⛔ pre-redesign (**P4**) |
| 12 | Discovery ("Identify antenna") | `SweepGraphScreen.kt:1122,1151` | inherits sweep buttons | inherits | `Instrument*` palette | none | 2 bespoke cards | ⛔ pre-redesign (**P4**) |
| 13 | **Calibration wizard** | `features/testing/CalibrationWizardScreen.kt` | **3 raw `Button`, no wrapper at all** | **12 literal dp**, 0 tokens | Material roles | none | 5 bespoke cards | ⛔ pre-redesign (**P6**) |
| 14 | **Project manager** | `features/app/LoadProjectScreen.kt` | **4 raw `Button`** — Load / Duplicate / **Delete styled identically** | **7 literal dp** (16/14/12/10) | Material defaults | none | bespoke `Card` + 5 bare `Text` | ⛔ pre-redesign (**P6**) |
| 15 | **Create-antenna wizard** shared | `features/wizard/components/WizardCommon.kt` | 2 raw `Button` in `WizardNav` | **25 literal dp** (off-grid 10/14/18) | **8 raw hex — 3rd palette** | none | 8 bespoke cards | ⛔ **no phase assigned** |
| 16 | Wizard Step 1 (type) | `features/wizard/steps/Step1AntennaTypeScreen.kt` | 2 raw `Button` | 6 dp + **2 token refs** | 5 raw hex | ✅ `touch.min` since `be6f343` | bespoke | ⛔ no phase |
| 17 | Wizard Step 2 (overview) | `.../Step2AntennaOverviewScreen.kt` | inherits `WizardNav` | **35 literal dp**, 17 `.sp` | 6 raw hex | none | 7 bespoke cards | ⛔ no phase |
| 18 | Wizard Step 3 (project / freq) | `.../Step3ProjectAndFrequencyScreen.kt` | inherits | 3 dp, 4 `.sp` | inherits wizard hex | none | inherits | ⛔ no phase |
| 19 | Wizard Step 4 (live design) | `.../Step4LiveDesignWorkspaceScreen.kt` | 3 raw `Button` (`FinishButtons`) | **22 literal dp** | inherits wizard hex | none | 6 bespoke cards | ⛔ no phase |
| 20 | Wizard graphics | `features/wizard/graphics/AntennaGraphics.kt` | — | — | 7 raw hex | — | Canvas | ⛔ no phase |
| 21 | Design workspace (project DESIGN tab) | `features/workspace/DesignWorkspaceScreen.kt` | 1 raw `Button` | 6 dp | defaults | none | 2 bespoke cards | ⛔ **no phase assigned** |
| 22 | Instrument Details | `features/app/InstrumentDetailsScreen.kt` | 2 raw `Button` | **9 dp — off-grid 18** | Material roles | none | 3 bespoke cards | ⛔ no phase |
| 23 | System menu (`settings`) | `features/app/SystemMenuScreen.kt` | 1 raw `Button` | **8 dp — off-grid 18/14** | Material roles | none | 3 bespoke card kinds | ⛔ no phase |
| 24 | Lab home | `features/lab/LabHomeScreen.kt` | **local** `PrimaryLabButton`/`SecondaryLabButton` + 2 raw | **27 literal dp — off-grid 10/14/18** | Material roles | `heightIn(min = 52.dp)` | 6 bespoke + own `StatusChip` | ⛔ pre-redesign, role superseded by Dashboard |
| 25 | Instrument status card (used **only** by Sweep Viewer) | `features/app/InstrumentStatusComponents.kt` | 1 raw `Button` | 6 dp | Material roles | none | bespoke | ⛔ pre-redesign |
| 26 | Home icons | `features/app/HomeIcons.kt` | — | 14 dp | — | — | Canvas | ⛔ no phase |

---

## 2. Cross-cutting findings

### F1 · Three parallel colour systems still live

| System | Where | Scope | Refs |
|---|---|---|---|
| `AntennaLabSemanticColors` (P0, theme-aware) | `ui/theme/SemanticColors.kt` | **canonical** | 14 files |
| `Instrument*` (dark-only, **theme-blind**) | `features/testing/SweepWorkspaceTheme.kt` | whole sweep stack | **129 refs / 7 files** |
| `Wizard*` / `Step1*` (dark-only hex) | `WizardCommon.kt` + step files | whole wizard | 26 raw hex |

`SemanticColors.kt:19-22` already names both as migration targets. The `Instrument*` set is
the harder one — it is not imported, it is **threaded as explicit parameters** through every
widget signature (`instrumentSurface =`, `instrumentAccent =`, `instrumentTextPrimary =` … at
`SweepGraphScreen.kt:352-355, 379-383, 408-413`). P4 must change those signatures, not just
swap constants.

### F2 · Duplicate primitives with *different* sizes

`SweepWorkspaceActionButton` and `SweepWorkspaceDisplayButton` are each defined **twice** —
`SweepInstrumentUi.kt:270 / :307` (public, `defaultMinSize(50.dp)`) and
`SweepToolsWidgets.kt:679 / :717` (private, `defaultMinSize(48.dp)`). Two buttons that look
like the same control are 2 dp apart depending on which file drew them.

### F3 · Dead code that will re-seed drift — ✅ CLOSED (`be6f343`)

- `DeviceConnectionsScreen.kt:499/524/540/564/580` — `CompactDataPanel`,
  `CompactDataGridRow`, `CompactDataCell`, `PrimaryActionButton`, `SecondaryActionButton`
  were defined but never called; pre-P2 leftovers sitting in the app's *reference* screen.
  (`CompactDataCell` was reachable only from `CompactDataGridRow`, so it fell with them —
  five, not four.) Deleted, along with the 8 imports they orphaned.
- `HomeScreen.kt:27` and `project/DesignWorkspaceCard.kt:45` — no call sites anywhere. Both
  files deleted.

### F4 · Touch-target violations (defects, not drift)

Only **`AppActionButton`** and **`MetricCard`** consume `AntennaLabTouch` at all. Every other
interactive surface relies on Material defaults or bare literals.

- ✅ **CLOSED (`be6f343`)** — `Step1AntennaTypeScreen.kt:237,256`: "Back" and "Expert User"
  set `.height(36.dp)`, a *hard* height (not `defaultMinSize`), 12 dp under the 48 dp
  Material/WCAG floor, with 12.sp labels. Now `AntennaLabTheme.touch.min`; measured 126 px =
  **48.0 dp** at 420 dpi on-device.
- ⚠ **OPEN** — `heightIn(min = 52.dp)` at `LabHomeScreen.kt:638`: above the floor but
  off-token (between `min 48` and `comfortable 56`). The identical `DeviceConnectionsScreen`
  instance went with the F3 deletion.

### F5 · Terminology / tone drift

| Concept | State |
|---|---|
| **App calibration** (honesty rule) | ✅ **CLOSED (`be6f343`)** — five live surfaces read bare "Calibration", ambiguous device-vs-app: `LabHomeScreen:297`, `InstrumentStatusComponents:69`, `ProjectPageScreen:978` ("Calibration Status"), `SweepGraphScreen:664`, `SweepGraphWidgets:560`. All now "App calibration" / "App Calibration Status", matching `InstrumentStatusPresenter:46-50`. **`ProjectPageScreen:778` deliberately unchanged** — that row is past-measurement provenance ("was this sweep calibrated"), not live instrument state, so bare "Calibration" is correct there. |
| Raw enum leakage | ✅ **CLOSED (`be6f343`)** — `ProjectPageScreen:982` rendered `completionState.name`, so the operator literally saw `NOT_STARTED`; `LabHomeScreen:140` did the same via `readiness.name` (`STALE`). Now `ProjectWorkspaceController.formatCalibrationCompletionState` and `InstrumentStatusUiMapper.buildCalibrationLabel`. |
| Sweep calibration state | ⛔ **OPEN** — `"Calibrated (OSL)"` / `"Uncalibrated"` (`ProjectPageScreen:778`) vs `"App calibration · Valid"`: same fact, two vocabularies |
| Back navigation | ⛔ **OPEN** — `"Back"` · `"Back Home"` (`LoadProjectScreen:103`) · `"Back to Home"` (`SystemMenuScreen:86`) · `"Back to Connections / Devices"` (`InstrumentDetailsScreen:107`) — four phrasings for one job |
| Case convention | ⛔ **OPEN** — Title Case (`"Start Calibration Wizard"`, `"Load Project"`) vs sentence case (`"Clear calibration"`, `"Measure now"`, `"New project"`). P0/P1 screens use sentence case; everything else Title Case. |

### F6 · Structural inconsistency — same job, different mechanism

| Job | Implementations |
|---|---|
| Status chip | `StatusPill` (P0) · `SweepStatusChip` (`SweepInstrumentUi:411`) · `StatusChip` (`LabHomeScreen:657`) — **three** |
| Label/value row | `MetricCard` · `DataRow` (`ProjectPageScreen:1069`) · `SharedTwoValueRow` (`SweepInstrumentUi:244`) · `TwoValueRow` + `StatusTwoValueRow` (`SweepGraphScreen:1078,1114`) · `DetailRow` (`InstrumentDetailsScreen:195`) · `QuickStatRow` (`Step2:611`) — **six** (was seven; `CompactDataCell` went with F3) |
| Section card | `MetricCard` · `SectionCard` · `SharedInstrumentCard` · `DetailsSectionCard` · `ScreenSectionCard` · `WorkflowCard` · `LabModeCard` — **seven** |
| Destructive confirm | `AlertDialog` on Clear calibration (`ProjectPageScreen:199`) · **nothing** on Delete Project (`LoadProjectScreen:91`) |
| Dialog state idiom | boolean flag (`ProjectPageScreen`) vs nullable-state (`SweepGraphScreen:595`) |
| Screen shell | `Scaffold` + `TopAppBar` on 8 screens; `DesignWorkspaceScreen` and `Step4` are bare `Column`s hosted by a parent |

---

## 3. Drift ranking

### Tier A — worst absolute drift (also the largest surfaces)

1. **Sweep Viewer stack** (`SweepGraphScreen` + `SweepGraphWidgets` + `SweepToolsWidgets` +
   `SweepInstrumentUi` + `SweepTuningWidgets`) — 141 literal dp, 129 theme-blind palette refs,
   5 bespoke row/card families, duplicate primitives, zero token consumption. ~180 KB across
   5 files. **Expected: P4 not yet run.**
2. **Create-antenna wizard** (5 files + graphics) — 91 literal dp, 42 raw `.sp` bypassing the
   type scale entirely, 26 raw hex. **Not expected — no phase covers it.**
3. **Project workspace** (`ProjectPageScreen`) — 7 button variants in one file, 20 literal dp,
   14 bespoke cards. **Expected: P5 not yet run.**

### Tier B — moderate, small surfaces, no phase assigned

4. Lab home (27 dp, own button + chip family; role already superseded by Dashboard)
5. Project manager / `LoadProjectScreen` (7 dp, 4 identical buttons incl. unguarded Delete)
6. Calibration wizard (12 dp, 3 bare `Button`s — the only screen with no button abstraction)
7. System menu · Instrument Details (off-grid 18 dp padding, bespoke card trio each)
8. Design workspace (6 dp, bare-`Column` shell)

### Tier C — compliant

9. **Device Connections** ✅ — full P2, now **0 literal dp** after the F3 deletion.
10. **Dashboard** ✅ — full P1, **0 literal dp**. Only nit: raw `TextButton` for "See all".
11. **Chart components** ✅ — P3-compliant and **all four now consumed by the viewer**:
    `SweepChartGrid` + `PhaseTraceCell` (P4 slice 1, `0d5eb60`), `MarkerReadoutTable`
    (slice 2, `e91b45d`), `BandAxisOverlay` (slice 3a, `8ef5520`). The Phase-3
    built-but-unwired backlog is closed. Slice 3b-i (`a3de767`) then unified the grid-cell
    plot geometry so every frequency-axis cell shares one inset
    (`ChartLayoutMath.plotInsetsFor`), and slice 3b-ii put a band strip on each of them —
    SWR, Return loss and Phase, never Smith (`ChartLayoutMath.hasFrequencyAxis`).

---

## 4. Fix now vs. expected

**Drift that needed fixing independent of the phase schedule — all closed in `be6f343`:**

| | Item | Why it could not wait | Outcome |
|---|---|---|---|
| 1 | `Step1AntennaTypeScreen:237,256` — 36 dp buttons | Accessibility floor violation; the wizard has no scheduled phase to fix it in | ✅ fixed |
| 2 | Five bare `"Calibration"` labels (F5) | Breaks the non-negotiable honesty rule on five live surfaces | ✅ fixed |
| 3 | Dead composables in `DeviceConnectionsScreen` | They sat in the reference screen and invited copy-paste re-drift | ✅ deleted |
| 4 | `completionState.name` / `readiness.name` raw enums | Operator-facing `NOT_STARTED` / `STALE` strings | ✅ fixed |
| 5 | Dead `HomeScreen.kt`, `DesignWorkspaceCard.kt` | Trivial deletion | ✅ deleted |
| 6 | `LoadProjectScreen:91` unguarded delete | Unrecoverable data loss | ⛔ still open — own follow-up |
| 7 | Duplicate 50 dp vs 48 dp sweep primitives | Two sources of truth will diverge further under P4 | ⛔ deferred to P4 |

**Not-yet-redesigned, expected — do not treat as bugs:** rows 4–12 (Sweep stack, Project
workspace, dialogs, discovery) and rows 13–14 (calibration wizard, project manager). These
are precisely what P4 / P5 / P6 exist to do.

---

## STATUS

### CLOSED — `be6f343` (the defects pass)

- **5 calibration-honesty labels** — `LabHomeScreen`, `InstrumentStatusComponents`,
  `ProjectPageScreen`, `SweepGraphScreen`, `SweepGraphWidgets` now read "App calibration" /
  "App Calibration Status".
- **Raw enum on screen** — `formatCalibrationCompletionState` (pure, tested with an
  underscore-leak guard) + `buildCalibrationLabel` reuse.
- **Step 1 48 dp floor** — `AntennaLabTouch.min`, verified 48.0 dp on device.
- **Dead-code deletion** — `HomeScreen.kt`, `DesignWorkspaceCard.kt`, five orphaned
  `DeviceConnections` composables + their unused imports.

Suite 482 → 483, 0 failures.

### OPEN — roadmap

- **P4 · Sweep Viewer stack** — the largest drift surface; also where the **P3 chart
  components** get consumed. **In progress, sliced:** slice 1 (`0d5eb60`) wired
  `SweepChartGrid` + `PhaseTraceCell` in as an additive "Chart grid" section beside the legacy
  "Active Display", plus an additive `compact` flag on `SweepScalarTraceView` so half-width
  cells stay legible; slice 2 (`e91b45d`) wired `MarkerReadoutTable`; slice 3a (`8ef5520`)
  established the shared plot-inset contract (`ChartLayoutMath.plotInsetsFor`) and drew the
  first `BandAxisOverlay` under the legacy trace; slice 3b-i unified the grid-cell geometry
  (`PhaseTraceCell` restructured to the scalar Surface+padding pattern, so PHASE and
  SCALAR-compact both inset 50/10) and aligned the frequency-tick row to the true plot extent
  on both sides; slice 3b-ii put a band strip on each frequency-axis cell in the grid, gated
  by the pure `hasFrequencyAxis` (Smith is excluded — it plots on the complex plane, so
  frequency is the path along the locus, not a screen axis). **Remaining:** slice 4
  tap-to-expand, slice 5 the Simple/Full toggle — which is where the 4 `ChartKind`s vs 12
  `SweepDisplayMode`s fork finally gets decided, and where the F2 duplicate 50/48 dp
  primitives get consolidated.
  **Off-bench, not bench-gated** — the debug simulated-sweep route (`1089e32`) produces a full
  `SweepResult` with no VNA attached, so the layout work is verifiable on an emulator. Only
  real-data *fidelity* wants hardware. (The spec's older "P4 will want the VNAs back" note
  predates that route.)
- **P5 · Project workspace** — `ProjectPageScreen` re-flow onto the new IA and tokens.
- **P6 · Project manager + calibration-wizard polish** — per-project badges, O/S/L visual
  throughline.

### OPEN — PLANNING GAP

The **create-antenna wizard** — Steps 1–4 + `WizardCommon` + `graphics`, **~9 files**,
**26 raw hex + 42 raw `.sp`**, the app's **second-largest palette** — is in **NO phase at
all**. `claude/ui-redesign-spec.md` Phase 6 covers only the project manager and calibration
wizard. This needs a phase (candidate **P7**), or an explicit decision to fold it into an
existing one. Same gap, smaller scale: `DesignWorkspaceScreen`, `SystemMenuScreen`,
`InstrumentDetailsScreen`, `LabHomeScreen`.

### OPEN — follow-ups

- **`LoadProjectScreen:91` — `deleteProject` unguarded.** Tap → permanent delete → list
  refresh, no dialog, no undo. Larger destructive-confirm gap than the calibration one, and
  the app now confirms before clearing a calibration but not before deleting a project. Also
  recorded in `CLAUDE.md` Known gaps.
- **50/48 dp duplicate Sweep primitives** (F2) — consolidate under **P4**, not before.

---

## Method — refreshing these numbers

From the repo root, over `app/src/main/java/com/example/antennalab_v1`:

| Metric | Pattern |
|---|---|
| Token adoption | `AntennaLabTheme\.` |
| Hardcoded spacing | `[0-9]+\.dp` |
| Hardcoded type | `[0-9]+\.sp` |
| Raw colour | `Color\(0x` |
| Sweep palette | `Instrument(Surface\|Accent\|TextPrimary\|TextSecondary\|Divider\|Background\|Blue\|Magenta\|Green)` |
| Mandated button | `AppActionButton\(` |
| Raw buttons | `^\s*(Button\|TextButton\|OutlinedButton\|IconButton)\(` |

Subtract the token layer (`ui/theme/`, `ui/components/`) for screen-level figures.

---

## Change log

- 2026-08-07 — Initial audit, all screens catalogued. Defect rows closed the same day by
  `be6f343`; counts in §0 and §1 are post-fix.
- 2026-08-08 — P4 started, sliced. Rows for the chart components updated from "NOT WIRED IN"
  to 3-of-4 consumed (`SweepChartGrid` + `PhaseTraceCell` in `0d5eb60`, `MarkerReadoutTable` in
  slice 2); `BandAxisOverlay` still previews-only. Dropped the "needs bench/VNA time" claim on
  the P4 roadmap entry — the debug simulated-sweep route removed that gate. **Per-screen dp /
  palette counts in §0 and §1 are NOT refreshed** and still describe the `be6f343` baseline; the
  sweep stack's 141 literal dp and 129 `Instrument*` refs are essentially untouched so far,
  since slices 1–2 added new sections rather than migrating old ones.
- 2026-08-08 — Slices 3a + 3b-i. **All four P3 chart components are now consumed**; the
  built-but-unwired backlog is closed. 3a added the pure `ChartLayoutMath.plotInsetsFor`
  contract, replacing three independently hardcoded plot insets (scalar gutter, a
  canvas-padding literal, `PhaseTraceCell`'s 40 dp). 3b-i then unified the geometry:
  `PhaseTraceCell` restructured to the scalar Surface-paints-background + padded-Canvas
  pattern, so PHASE and SCALAR-compact both inset 50/10 and the two traces in a grid row pair
  finally share one plotting extent. Same pass fixed the frequency-tick row, which had padded
  by gutter+gutterEnd (44/64) against a plot starting at 50/66 and had no end padding at all —
  labels sat 6 dp (cell) / 2 dp (full) left and the last tick overhung by 10 dp. **§0/§1 counts
  still NOT refreshed** — same reason as above; the sweep stack's palette migration has not
  started. **Known follow-up, deliberately not bundled:** `SweepScalarTraceView`'s y-label
  column has the same vertical version of the tick defect (labels ~10 dp off the plot they
  annotate, because the column is `heightDp` while the canvas pads inside it). Untouched.
- 2026-08-08 — Slice 3b-ii. Band strips now render per-cell in the grid on SWR, Return loss
  and Phase, all at the single 50/10 inset 3b-i produced — one value, no per-renderer branch.
  Smith is excluded by the new pure `ChartLayoutMath.hasFrequencyAxis`, an exhaustive `when`
  so a fifth `ChartKind` cannot compile without answering the question (a `!= SMITH` at the
  call site would have silently defaulted it to "has an axis"). Suite 494 → 495.
  **Observation, not actioned:** `PhaseTraceCell` has no frequency-tick row at all — it draws
  only the ±180° y-labels — so its strip sits directly under the plot border while the scalar
  cells' strips sit below their tick labels. Reads fine, but the phase cell has no x-axis
  labels of any kind; worth a decision when slice 4/5 revisits the cells.
  **CLOSED by slice 4b (2026-08-08)** — see below.
- 2026-08-08 — Slices 4a + 4b. 4a added tap-to-expand state (`expandedChartKind`) as a nullable
  OVERLAY on the layout mode, never a layout-enum value, per §2.2; nothing renders it yet.
  **4b closed the no-x-axis observation parked above.** `PhaseTraceCell` now takes
  `compact: Boolean = true` and draws a frequency-tick row in both states, reusing
  `buildFrequencyTicks` plus a `visibleFrequencyTicks` thinning rule extracted to
  `ChartLayoutMath` so the two renderers cannot drift. `plotInsetsFor(PHASE)` honours `compact`
  (50/10 grid, 66/10 full), which it had ignored because the cell had no full-width variant —
  an expanded phase chart would have sat at 50 while an expanded scalar sat at 66. SCALAR and
  PHASE now resolve through one merged `when` arm, so the 3b-i unification is guaranteed by
  construction rather than by two constants agreeing; `PHASE_GUTTER_DP` deleted, gutters
  renamed `TRACE_GUTTER_*` (values unchanged).
  **This was a predicted-change slice, and the change was vertical only.** Pixel-measured on
  the emulator: horizontal geometry byte-identical before/after (return-loss strip x=260..432,
  phase strip x=726..899, both unmoved; legacy 3a overlay still 66/10). The phase cell was
  32 dp shorter than its row-pair sibling because it had no tick row, so its band strip sat
  ~30 dp high; it now fills height the row already reserved and the two strips share a line.
  Grid total height unchanged. Suite 495 → 507 across 4a+4b.
  **Still open from the 3b-i entry:** `SweepScalarTraceView`'s y-label column vertical defect.
  `PhaseTraceCell` fixed its own version in 3b-i; the scalar view has not been touched.
- 2026-08-08 — **Slice 4c — DONE. Tap-to-expand ships.** Two commits of geometry
  (`d90873a`, `de192c2`) then one of wiring (`9712f58`). Suite 507 → 508.
  **4c-i** added the pure `cellsAreCompact(chartCount)` = `gridColumnCount > 1` so a sole
  chart and an expanded chart are one rule, made the per-cell band strip follow the cell's
  `compact` flag and name its renderer (discharging 4b's marker), and **split the overloaded
  chrome flag** — `SweepScalarTraceView.showHeaderAndFooter`, default `!compact`. The
  header/footer were being suppressed for a HOSTING reason ("the cell already titles the
  chart", "the grid always renders CURRENT_ONLY") that the code's own comments stated, but
  keyed on a WIDTH flag; conflated, a full-width sole chart could not take the wide gutter
  without also gaining a second title and a dead footer.
  **Multi-chart grid verified byte-identical** — return-loss strip x=260..432, phase strip
  x=726..899, the 4b baselines, both still on one line.
  **4c-ii** wired `onCellTap → toggleExpandedChart` and added `ExpandedChartPanel` beside the
  grid, with three return routes (tap the focused chart, a 56 dp "Back to grid"
  `AppActionButton`, and the app's first `BackHandler`). Behavioural gate ran six rows on the
  emulator against a sim sweep; the load-bearing pair both passed — system back while focused
  collapses and keeps the app open, system back unexpanded still exits, unchanged.
  **⚠ ARITHMETIC CORRECTION to `de192c2`'s commit message.** It records the sole-chart strip
  as `291..819 → 328..827`. Those were PREDICTED, not measured, and the prediction used the
  grid's 912 px content width instead of the **cell's inner width of 870 px** (the grid width
  less `ChartGridCell`'s own 8 dp padding on both sides). The correct figures at 66/10 are
  **307..806 → 345..814**. Verified twice, independently: the 4c-i sole-chart scratch capture
  and 4c-ii's expanded SWR panel both measure **345..814**, and both share the same cell
  shell. The code was always right; only the message's arithmetic was wrong. History was left
  alone rather than force-pushed — this entry is the correction of record.
  **⚠ Cosmetic blemish in `9712f58`'s commit message.** Backticks around a word were consumed
  by shell substitution, so one line reads "and  is load-bearing" with `enabled` dropped. The
  sentence should read "the `enabled` gate is load-bearing". Message only — code, behaviour
  and the pushed tree are unaffected.
  **Carried forward, still open and deliberately unbundled:**
  (1) `SweepScalarTraceView`'s y-label column vertical offset — the twin of the tick-row
  defect 3b-i fixed, and the version `PhaseTraceCell` fixed for itself. Untouched.
  (2) The 3a legacy overlay slot: the band strip lives INSIDE the `else` branch of the legacy
  `when` (`SweepGraphScreen`), so it appears only under a frequency-axis trace. There is no
  general below-the-axis slot in the legacy chart, and the placement is coupled to that
  branch rather than offered by the chart itself.
  (3) Pre-existing navigation gap, now documented by 4c-ii's `BackHandler` comment but NOT
  fixed: the Sweep Viewer has no `navigationIcon`, navigation is a `showSweep` boolean, so
  system back exits the app instead of returning to the project page. Larger than any P4
  slice; the in-content `onBack()` buttons are the only route back to the project.
