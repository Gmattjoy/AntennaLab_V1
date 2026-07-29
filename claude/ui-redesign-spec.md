# AntennaLab V1 — UI Redesign Spec

Anchor document for the app-wide UI redesign, the way
`claude/hardware-bringup-litevna64.md` anchors the bench work. It records the **current
state**, the **agreed direction** (decided in a design session — not to be re-litigated),
a **phased rollout order**, and the **open questions** still to settle.

- **Scope of this doc:** documentation only. No Compose, no production code.
- **Status:** direction agreed 2026-07-29; rollout not yet started.
- **How to use it:** each rollout phase below becomes its own plan-mode task. This doc is
  the shared reference each of those plans cites; update it as decisions land (mark items
  resolved, don't delete the history).

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
4. **Amateur-band overlay data source & scope** — which band plan / region, and how it
   renders across very wide vs very narrow spans without clutter.
5. **.s1p export fidelity** — S11 only vs S11+S21 when available; frequency units and format
   header conventions; where the file lands (share sheet vs project storage).
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
- 2026-07-29 — **Phase 0 landed.** Token layer under `ui/theme/` (`AntennaLabSpacing`,
  `AntennaLabTouch` with `field = 64.dp` as the documented gloved dial, `AntennaLabSemanticColors`
  + `LocalAntennaLabSemanticColors` + selector, `AntennaLabTheme` accessor) provided additively in
  `Theme.kt`; first shared primitive `ui/components/StatusPill.kt`; `TokenPreviews.kt` swatch sheet
  (light + dark); `DesignTokensTest` (5 tests) locking the touch floor / 4 dp grid / theme-aware
  semantic invariants. No screen touched. Light-mode semantic hex proposed, pending swatch review.
