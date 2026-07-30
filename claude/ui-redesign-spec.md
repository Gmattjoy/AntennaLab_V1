# AntennaLab V1 — UI Redesign Spec

Anchor document for the app-wide UI redesign, the way
`claude/hardware-bringup-litevna64.md` anchors the bench work. It records the **current
state**, the **agreed direction** (decided in a design session — not to be re-litigated),
a **phased rollout order**, and the **open questions** still to settle.

- **Scope of this doc:** documentation only. No Compose, no production code.
- **Status:** Phases 0–2 DONE (2026-07-29). Phase 3 is the next entry point.
- **How to use it:** each rollout phase below becomes its own plan-mode task. This doc is
  the shared reference each of those plans cites; update it as decisions land (mark items
  resolved, don't delete the history).

---

## Session handover (2026-07-29) — where to pick up

**A UI day and a bench day are both pickable from here.**

### Done — Phases 0–2 (suite 379 green, 0 failures)
- **P0 · Design tokens + primitives.** `ui/theme/` `AntennaLabSpacing` / `AntennaLabTouch`
  (`field = 64.dp` = the documented gloved dial) / `AntennaLabSemanticColors` (+ selector +
  `LocalAntennaLabSemanticColors`) / `AntennaLabTheme` accessor, provided additively in `Theme.kt`.
  Shared primitives: `ui/components/StatusPill`, `MetricCard`, `AppActionButton`. `DesignTokensTest`.
- **P1 · Dashboard.** `DashboardScreen` replaced `HomeScreen` on the `"home"` route (the ⋮ overflow is
  kept, so no route lost its entry point). Real-truth device/calibration status card ("App calibration ·
  …"), three distinct quick actions (Measure now / New project / Identify antenna), recent projects with
  async bounded badges. Pure `DashboardController` + tests.
- **P2 · Device Connections.** Re-skinned onto the tokens; the state→chip mapping is the shared pure
  `InstrumentStatusPresenter.buildStatusChips` that **both** the dashboard and this screen consume (the
  anti-drift guarantee). Foregrounds PERMISSION_REQUIRED, the validation timeline, and app-calibration.
  Connect/validate/permission logic + `BenchState` logging untouched. `DeviceConnectionsController` level
  mappers + tests.

### Phase 3 (shared chart components) — IN PROGRESS
**Slice A · pure helpers + tests — DONE (2026-07-30, suite 443 green, 0 failures, +64 tests).**
No Compose, no existing file touched; three additive source files:
- `model/AmateurBand.kt` — `AmateurBand` + `IaruRegion`, pure data.
- `domain/analysis/AmateurBandPlan.kt` — all three IARU region tables, `bandAt` /
  `bandLabelAt` / `bandsOverlapping` (the axis-overlay lookup). `DEFAULT_REGION = REGION_3`
  is the single constant to flip. Doc comment is explicit that these are region-level
  allocations, **not** a legal band-plan reference (country/licence limits are narrower).
- `domain/testing/SweepMarkerMath.kt` — the §2.3 readout row: |Z|, R+jX, Q, series-equivalent
  Cs/Ls (auto-scaled pF/nF, nH/µH), RL, phase, band, as raw values **and** display strings via
  `buildMarkerReadout`. Γ comes from `OslCalibrationEngine.gammaFromPoint` — the same exact
  R/X→Γ path `CalibrationCorrector` uses — so phase does not depend on
  `SweepPoint.s11PhaseDegrees` being populated.
- `domain/testing/TouchstoneExport.kt` — `buildS1p` (pure string building, no IO) +
  `suggestFileName`. Carries provenance comments incl. calibration state and the
  incomplete-sweep count, so an exported file stays auditable.

**Slice B · Compose + delivery — CODE DONE, DEVICE VERIFICATION OUTSTANDING (2026-07-30,
suite 489 green, 0 failures, +46 tests).**

*Delivery (save-to-Downloads then share).* Decision layer is pure and tested:
- `domain/testing/SweepExportNaming.kt` — the single naming home
  (`<project>_<centre freq>_<timestamp>.s1p`, sanitising, truncation, `nextAvailableName`
  collision suffixing). `TouchstoneExport.suggestFileName` now delegates here.
- `domain/testing/SweepExportPlan.kt` — the API-tier decision as a pure function of `sdkInt`.
- `storage/SweepExportWriter.kt` — executes a plan; no decisions of its own.
- Manifest `<provider>` for `androidx.core.content.FileProvider` +
  `res/xml/file_paths.xml` (scoped to the app-specific Downloads dir only).
  **No storage permission added** — verified against the merged manifest.

**⚠ minSdk 26 vs `MediaStore.Downloads` (API 29+).** The original "public Downloads via
MediaStore, no legacy `WRITE_EXTERNAL_STORAGE`" is impossible on Android 8.0/8.1/9. Resolved
as two tiers: **API 29+** MediaStore public Downloads (`Download/AntennaLab`), sharing the
`content://` URI MediaStore returns — FileProvider not involved. **API 26–28** app-specific
`getExternalFilesDir(DIRECTORY_DOWNLOADS)` (no permission since API 19), shared via
FileProvider. `Outcome.Saved.isPublicDownloads` carries which happened and the UI states it
plainly — it must never claim a public save on the fallback tier.

*Compose components* (previews only, NOT wired into the viewer — that is Phase 4):
`features/testing/charts/` — `SweepChartGrid` (capability-gated, wraps the EXISTING
`SweepScalarTraceView` / `SweepSmithChartView` rather than redrawing), `PhaseTraceCell`
(the only genuinely new renderer), `MarkerReadoutTable`, `BandAxisOverlay`. All thin: every
value is computed in `domain/` first. `domain/analysis/ChartLayoutMath.kt` holds band-span
fractions, the fixed phase axis, grid shape, and `availableChartKinds` capability gating.

*One existing-UI touch:* an "Export .s1p" card in `SweepGraphScreen` beside the CSV preview,
so save+share is reachable for device verification. Plus additive `heightDp` params on the two
existing chart views (defaults preserve current behaviour; they hardcoded 240/280.dp and so
could not be sized into a grid cell).

**`ChartKind` is deliberately NOT `SweepDisplayMode`.** That enum has no PHASE value and
`SweepGraphMath.getDisplayValue` is an exhaustive `when` over it, so adding one would ripple
through ~7 files and drag the axis math in (phase wants a fixed −180..180 axis, unlike every
auto-scaled SWR/RL/R/X axis). **Unifying the two is a Phase 4 decision** — take it when the
viewer's chart set is settled, not before.

**STILL UNVERIFIED — needs a handset, do this before trusting export:**
1. API 29+ device: sweep → Export .s1p → file really appears in `Downloads/AntennaLab` →
   share sheet delivers it → header reads `# Hz S RI R 50`, first data column is whole Hz.
2. **API 26–28 device/emulator (the riskiest branch):** confirm the wording does NOT claim a
   public Downloads save, and that FileProvider sharing actually works. This tier has never
   run; the tier *decision* is unit-tested but the IO is not.
3. Android Studio preview pane for the four components (grid sizing, overlay clutter).

*Deferred, unchanged:* CSV row building is still inline in the Composable. It should ride the
same `SweepExportWriter` seam and move to a pure builder beside `TouchstoneExport` —
deliberately kept as a separate task.

**Then Phase 4 (Sweep Viewer) will want the VNAs back** — the multi-chart grid, markers and `.s1p` export
need real-data verification on hardware, so schedule P4 review against a bench session, not headless.

### Still-open HARDWARE items (a bench day) — see `claude/hardware-bringup-litevna64.md`
- **H4 identity / Block C** (§0 handover, §10c.6 next steps) — does the NanoVNA-H4 reach Full Support and
  honour `sweepPoints=101` (C5), or free-run like the LiteVNA? Still unanswered, no corroboration.
- **A3** — calibrate a real LiteVNA → Finish → **Save** → kill → reload; expect `CalRestore`
  `decision=RESTORE reason=ok storedName='LiteVNA64 v0.3.3'`. Now runnable (the §10c.6 producer shipped).
- **Block B** — reopen with the wrong hardware selected → expect `CLEAR reason=hardware-name-mismatch`.
- Also re-verify off-bench fixes on silicon: §10c.7 (real sweep persists as real hardware), §10b resonance
  count (50 Ω → 0; AR-771 count == detected).

---

## 0. Cross-cutting principles (apply to every phase)

These are constraints on the whole redesign, decided up front.

- **Calibration honesty (non-negotiable).** Calibration state shown in the UI is ALWAYS
  labelled as the APP's calibration, never ambiguous with the device's own. This is the
  known confusion trap carried over from NanoVNA-Saver users (device-cal vs app-cal). Every
  surface that shows calibration — dashboard status card, sweep viewer, device screen —
  names it unambiguously (e.g. "App calibration: VALID", not "Calibrated"). The
  live-vs-project calibration split is surfaced, not hidden.
- **Field AND bench are both primary contexts.** Not a bench tool that also runs in the
  field. Design for gloved hands, sunlight, and one-handed use as first-class.
- **Shared design tokens.** One token set consumed everywhere: colour roles, a type scale,
  a spacing scale, and — explicitly — **touch-target sizes** sized for field/gloved use.
  Today `ui/theme/` holds colours + Material3 theme only; button sizes are ad-hoc
  (`SweepWorkspaceActionButton` min-height 50dp; `PrimaryLabButton`/`SecondaryLabButton`
  their own). The redesign centralises these. Minimum touch target ≥ 48dp (Material floor);
  primary field actions larger (target 56–64dp) — exact values fixed in Phase 0.
- **Capability-driven UI stays.** The existing capability system
  (`HardwareMeasurementCapabilities` → which charts/features show) is preserved. New chart
  layouts are gated by capability flags, never hardcoded per device. Do NOT branch the UI
  on device model — extend the capability profile.
- **Extract-and-test discipline preserved.** Any non-UI logic a redesigned screen needs is
  pulled into a pure controller/helper under `domain/` or a `*Controller`/`*UiModelBuilder`
  in `features/`, with unit tests — matching the pattern already used
  (`SweepWorkspaceController`, `SweepUiModelBuilder`, `SweepGraphMath`, etc.). UI files stay
  free of calc/decision logic. The full test suite stays green between phases (confirm from
  JUnit XML, not BUILD SUCCESSFUL).

---

## 1. Current-state inventory (starting point — NO redesign here)

What each screen does today and its specific density/hierarchy problems. Recorded so the
redesign is measured against reality, not a guess.

### 1.1 Home / Lab menu — `features/lab/LabHomeScreen.kt`
**Today:** a menu of buttons. "Operator Controls" panel (Calibration Tools, Connections,
Projects, Back To Home), an "Instrument Summary" data panel (instrument name, measurement
trust, session), and `LabModeCard`s (Antenna testing; "Connector / Fitting Check — Coming
Later"). Routing is string-keyed in `AppRootScreen` (`"lab"`, `"project"`, `"projects"`,
`"device_connections"`, `"calibration_wizard"`, `"instrument_details"`, `"settings"`).
**Problems:** it's a navigation index, not a workspace — the operator lands on a list of
places to go rather than the state of their instrument or a one-tap path to measure.
Instrument/calibration state is buried in a mid-page summary panel, not the first thing
seen. No recent-projects affordance. Disabled "Coming Later" cards take prime space.

### 1.2 Project workspace — `project/ProjectPageScreen.kt`
**Today:** the hub (`ProjectData` single source of truth). Sections
`OVERVIEW / DESIGN / MATERIALS / TESTING / NOTES` (`ProjectSection`), each rendering data
rows/cards; hosts Save / Save As (explicit-save model — edits apply to `workingProject`,
disk write on Save), the calibration wizard entry, and the sweep entry. Large file (>35KB).
**Problems:** dense section stacks of `DataRow`s with uniform weight — no visual hierarchy
between "what is this antenna" and "how is it performing." The path from project → measure
is several taps deep. Save/local-edit semantics ("Save project to keep them") are correct
but easy to miss.

### 1.3 Sweep Viewer — `features/testing/SweepGraphScreen.kt` (+ `SweepGraphWidgets.kt`, `SweepTuningWidgets.kt`)
**Today:** a **single chart at a time**, chosen from `SweepDisplayMode`
(`SWR, RETURN_LOSS, RESISTANCE, REACTANCE, ANALOG_SWR, …, SMITH, IMPEDANCE_LOCUS,
S21_ESTIMATE`); two-marker system (`WorkspaceMarkerTarget.A/B`) with nudge/jump tools;
a "Sweep Summary" card (`SweepGraphWidgets`) and an "app" Diagnostics card
(`SweepTuningWidgets`) stacked below; CSV export; capability-gated features (Smith, S21,
TDR, markers). State in `SweepWorkspaceState`/`SweepWorkspaceViewModel`; math already pulled
into `SweepGraphMath`, decisions into `SweepWorkspaceController`/`SweepUiModelBuilder`.
**Problems:** one-chart-at-a-time fights VNA muscle memory (NanoVNA-Saver shows a multi-chart
grid). Derived values a VNA user expects (|Z|, R+jX, Q, Cs/Ls, RL, phase) are not presented
as a first-class marker table. No amateur-band context on the frequency axis. No Touchstone
(.s1p) export — only CSV. Summary + Diagnostics cards compete with the trace for vertical
space with no way to focus one chart. (The two-numbers-labelling confusion was fixed
separately — §10b item 1 — but the density remains.)

### 1.4 Device Connections — `features/app/DeviceConnectionsScreen.kt` (+ `DeviceConnectionsController.kt`)
**Today:** hardware-profile selection, permission grant, Refresh, Connect, and the
LiteVNA Validate flow; shows a driver display label
(`buildProfileDisplayLabel`, e.g. "LiteVNA64 HW 64-0.3.3 FW v1.4.06") and validation state.
**Problems:** dense operator flow (Grant → Refresh → Connect) with state spread across the
screen; the connect path is the working one but not obviously sequenced. Calibration/trust
wording lives here and must stay consistent with the dashboard's honesty principle.

### 1.5 Calibration wizard — `features/testing/CalibrationWizardScreen.kt` (+ `CalibrationWizardController.kt`)
**Today:** guided OSL capture (Open/Short/Load), per-step session build, live registration
into `UsbSessionManager`; a debug "Simulate O/S/L" path. Shows the connected-device label.
**Problems:** functional but visually a form; the O/S/L progress and the resulting
calibration validity aren't a strong visual throughline. (Persistence/naming correctness was
handled in the §10c work; this is presentation only.)

### 1.6 Project manager — `features/app/LoadProjectScreen.kt` (+ `LoadProjectController.kt`)
**Today:** a saved-projects list (open by id → workspace). Derives frequency / last-edited /
stored-calibration text (`LoadProjectController`).
**Problems:** a flat list; per-project state (calibration presence, last min-SWR, band) is
not surfaced as at-a-glance badges, so choosing a project is low-information.

---

## 2. Agreed direction (decided — do not re-litigate)

### 2.1 Home → DASHBOARD
Replace the navigation menu with a dashboard where **the actions ARE the navigation**.

- **Device status card at the TOP — the first thing visible.** Connected device;
  live vs simulated data source; **app calibration state** (VALID / STALE / NONE, and the
  live-vs-project split when they differ). This placement is deliberate: the live-vs-project
  calibration split is the known confusion trap from NanoVNA-Saver users, so it leads.
- **Quick actions:**
  - **Measure now** — accent, one tap to a sweep (the primary action).
  - **New project.**
  - **Identify antenna** — the unknown-antenna discovery flow; a product differentiator, so
    it gets a top-level action, not a buried menu item.
  - **Design / calculate.**
- **Recent projects list** carrying **per-project state** — calibration badge, last min-SWR
  (and band where known) — so the list is a launchpad, not an index.

### 2.2 Sweep Viewer → trace-centric, with TWO distinct behaviours (keep them separate)
- **(a) Simple / Full view toggle — a persistent MODE choice** about HOW MANY charts show.
  Simple = the essential trace(s); Full = the multi-chart grid. Defaults to **AUTO** (picks
  Simple or Full from orientation / screen size); the user can **pin** it to Simple or Full.
- **(b) Tap-to-expand — TRANSIENT focus** on ONE chart. Works within either mode; expanding
  is temporary and returns to the underlying layout. It is NOT the same control as the
  toggle and must not be conflated with it in the design or the code.

### 2.3 Chart conventions → align with NanoVNA-Saver (respect VNA muscle memory)
- **Multi-chart grid:** SWR, Smith, return loss, phase — S11. (Capability-gated; S21 where
  supported.)
- **Marker readout table as a first-class element** showing the derived values those users
  expect at each marker: **|Z|, R + jX, Q, Cs / Ls, return loss, phase, band.**
- **Amateur-band overlay on the frequency axis.**
- **.s1p Touchstone export** (in addition to the existing CSV).
- **The app's own diagnostics summary stays**, but is reframed as **"app analysis"** — the
  value-add on top of the familiar layout — and is **collapsed by default**.

### 2.4 Cross-cutting (restating §0 as direction)
- A **shared design-token set**: colours, type scale, spacing, and explicit **touch-target
  sizes** for field/gloved use (bench and field are both primary).
- **Calibration honesty:** calibration state always clearly the APP's, never ambiguous with
  the device's.

---

## 3. Phased rollout order (one screen per plan-mode task)

**Discipline for every phase:** its own plan-mode task; extract any new non-UI logic into
pure controllers/helpers with unit tests; the whole suite stays green (JUnit XML) between
phases; commit per phase.

**Recommended order (dashboard-led):**

- **Phase 0 — Design tokens + shared primitives (foundation).** The one non-screen slice.
  Centralise colours/type/spacing and the touch-target scale in `ui/theme/`, plus the shared
  primitives every screen reuses (status card, quick-action button, project row, the
  existing `SharedTwoValueRow`/muted-caption family). Small, mostly theme, little logic —
  but doing it first means every subsequent screen consumes the same tokens instead of
  re-deriving them. *Justification for it existing separately: without it, Phase 1 invents
  tokens ad-hoc and Phases 2–6 either copy or diverge.*
- **Phase 1 — Dashboard (Home).** The navigation shell + device-status card + quick actions
  + recent-projects. **Recommended first screen.**
- **Phase 2 — Device Connections + calibration-state surfacing.** The dashboard status card
  and this screen read the same instrument/calibration truth; nail the honesty presentation
  here right after the shell so the principle is coherent before the big sweep work.
- **Phase 3 — Shared chart components (NanoVNA-Saver primitives).** Multi-chart grid cell,
  marker readout table, amateur-band axis overlay, .s1p export. Extract chart/marker math
  into pure helpers (building on `SweepGraphMath`) with tests. Foundation the sweep viewer
  consumes — big enough to precede it.
- **Phase 4 — Sweep Viewer.** Trace-centric layout: Simple/Full toggle (AUTO default),
  tap-to-expand, multi-chart grid from Phase 3, "app analysis" collapsed. The largest,
  riskiest surface — done once tokens (P0) and chart primitives (P3) are proven.
- **Phase 5 — Project workspace.** Re-flow `ProjectPageScreen` to the new IA/tokens and the
  shortened project→measure path.
- **Phase 6 — Project manager + calibration wizard polish.** Per-project badges on the list;
  strengthen the O/S/L visual throughline. Lowest risk, last.

**Why dashboard first (the case, both ways).**
- *For (recommended):* it is the navigation shell every other screen hangs off — building it
  first establishes the new information architecture that Phases 2–6 plug into, and it
  front-loads the single biggest UX win (the device/calibration status card, which attacks
  the known confusion trap). It is self-contained enough to be a clean first slice and, with
  Phase 0, proves the design language in a real screen cheaply.
- *Against (and the rebuttal):* the Sweep Viewer is where the measurement value and the worst
  density problems live, so "viewer first" delivers the core workflow improvement soonest.
  But the viewer is the largest, highest-risk surface (charts, markers, capability gating,
  export); leading with it means designing the token set and shared components under the
  pressure of the hardest screen. Better to prove tokens (P0) and the shell (P1) first, build
  the chart primitives deliberately (P3), then land the viewer (P4). Net: **dashboard first.**

*(Phases 3 and 4 may merge or split further once we reach them; noted, not fixed.)*

---

## 4. Open design questions (settle when we reach them — not silently)

1. **Simple/Full AUTO breakpoint.** What exactly triggers AUTO to pick Full vs Simple —
   width dp threshold, orientation, aspect ratio, or a combination? And what is the pinned
   override's persistence scope (per session, per project, global)?
2. **Marker table density in Full mode on a tablet.** A first-class |Z|/R+jX/Q/Cs·Ls/RL/
   phase/band table alongside a 4-chart grid may be cramped, or may crowd the charts. Does
   the table dock, overlay, or become a bottom sheet in Full mode? Decide with a real tablet
   layout.
3. **Which charts are "essential" in Simple mode**, and are they capability-dependent
   (e.g. Smith only when `supportsSmithChart`)?
4. ~~**Amateur-band overlay data source & scope**~~ — **RESOLVED (2026-07-30, Phase 3 slice A).**
   All three IARU regions ship as pure data (`domain/analysis/AmateurBandPlan`) with
   `DEFAULT_REGION = REGION_3` (Melbourne). Rationale: it is a static table either way, and a
   single-region table has no concept to extend, so adding regions later would mean reworking
   the model — pay the trivial cost once. Region-level allocations only, explicitly not a legal
   reference. *Still open (rendering, not data):* how the overlay draws across very wide vs
   very narrow spans without clutter — settle in slice B against real layouts.
5. ~~**.s1p export fidelity**~~ — **RESOLVED (2026-07-30, Phase 3 slice A).** S11 only, because
   `.s1p` is by definition a one-port file and cannot carry S21; `.s2p` would need the full
   2×2 set (S11 S21 S12 S22) and both devices measure forward S21 only, so a valid `.s2p` is
   not producible without fabricating terms. Closed by the file format, not by preference.
   Header `# Hz S RI R 50` (NanoVNA-Saver's own convention, per §2.3), frequency in whole Hz,
   6-decimal RI pairs, CRLF. *Still open:* delivery — the plan of record is a real file via
   FileProvider + share sheet in slice B (a `.s1p` that cannot leave the device is useless,
   since the point is loading it into NanoVNA-Saver or a simulator). **Now BUILT in slice B
   as save-to-Downloads-then-share, two API tiers** (see the Phase 3 entry above) — code
   complete and unit-tested, but not yet exercised on a handset.
6. **Dashboard "Measure now" target** when no instrument/calibration is present — does it
   route to a simulated sweep, to Device Connections, or offer a choice?
7. **Touch-target exact values** (primary vs secondary vs dense-table rows) — pin numbers in
   Phase 0 against a gloved-use check, not just the 48dp Material floor.
8. **Recent-projects state badges** — which one or two signals fit at-a-glance (calibration
   badge + last min-SWR proposed); avoid turning the row into its own dense panel.

---

## 5. Change log
- 2026-07-29 — Initial spec: current-state inventory, agreed direction, dashboard-led rollout
  order, open questions. Doc only.
- 2026-07-30 — Phase 3 slice A (pure helpers + tests) landed; open questions #4 and #5 marked
  resolved with rationale. Suite 379 → 443 green.
- 2026-07-30 — Phase 3 slice B: Android delivery layer (two API tiers, FileProvider, share
  sheet) + the four shared chart components, previews only. Suite 443 → 489 green. Device
  verification of the export path is outstanding; the API 26–28 tier has never run.
- 2026-07-29 — **Phase 0 landed.** Token layer under `ui/theme/` (`AntennaLabSpacing`,
  `AntennaLabTouch` with `field = 64.dp` as the documented gloved dial, `AntennaLabSemanticColors`
  + `LocalAntennaLabSemanticColors` + selector, `AntennaLabTheme` accessor) provided additively in
  `Theme.kt`; first shared primitive `ui/components/StatusPill.kt`; `TokenPreviews.kt` swatch sheet
  (light + dark); `DesignTokensTest` (5 tests) locking the touch floor / 4 dp grid / theme-aware
  semantic invariants. No screen touched. Light-mode semantic hex approved off the swatch.
- 2026-07-29 — **Phase 2 landed (Device Connections + calibration surfacing).** Anti-drift: the
  state→status-chip mapping is relocated (verbatim, behaviour-preserving) out of `DashboardController`
  into the shared pure `features/app/InstrumentStatusPresenter.buildStatusChips`; BOTH the dashboard and
  Device Connections now call that one function, so they cannot present the same `InstrumentSessionState`
  differently. Device Connections re-skinned to `MetricCard` + `StatusPill` + `AppActionButton` + tokens;
  it foregrounds PERMISSION_REQUIRED (caution + accent Grant Permission), the validation timeline
  (Pending/Running/Passed/Timed Out as coloured pills via `DeviceConnectionsController.validationLevel`),
  and "App calibration · …". **All connect/validate/permission side effects, the `BroadcastReceiver`, gating
  predicates, `buildValidationLabel`, profile registration, and `BenchState` logging preserved verbatim.**
  New pure `connection/permission/transport/validation` level mappers + tests; chip tests moved to
  `InstrumentStatusPresenterTest` (same assertions). `AppActionButton.onClick` moved to last param
  (trailing-lambda idiom). `@Preview`s of the bench-confusing states in both modes. Suite 379, 0 failures.
- 2026-07-29 — **Phase 1 landed (Dashboard).** `DashboardScreen` replaces the `"home"` route's
  `HomeScreen` (now unused); the ⋮ overflow (`AppTopRightMenu`) is kept so no route loses its entry
  point. Device/calibration status card reads real `UsbSessionManager` truth via the existing
  `InstrumentStatusUiMapper`, with the calibration pill labelled "App calibration · …". Three
  distinct quick actions (Measure now / New project / Identify antenna → existing `enter*` handlers);
  Test Antenna + design extras stay in ⋮ / Lab. Recent projects render from the cheap index
  immediately; cal + last-SWR badges load bounded (top 4) off the main thread, a failed load just
  drops that row's badges. New primitives `ui/components/MetricCard.kt` + `AppActionButton.kt`
  (first token consumers, `touch.field` for the accent action). Pure `DashboardController` +
  `DashboardControllerTest` (12 tests). `@Preview` of `DashboardContent` in light + dark. Suite
  376 tests, 0 failures. Open items still standing: recent-badge index enrichment (Fork 2/B) if
  perf bites; the sweep/Simple-Full/marker-table questions belong to later phases.
