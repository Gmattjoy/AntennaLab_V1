# AntennaLab V1

Android app for antenna design, calculation, project management, and VNA-based sweep testing/tuning. Aimed at novice antenna builders through advanced radio operators — an antenna engineering workspace, not just a calculator.

## Working style
One task at a time, auto-accept edits (no manual per-file approval), commit after each change, verify via build + in-app testing. Short direct answers.

After completing any implementation phase, automatically stage and commit the change (code + any relevant control docs) with a descriptive message. Do not wait for user confirmation to commit and do not present a "commit this" suggestion — just do it and move to the next step.

## Control docs
These carry the detail deliberately kept out of this file. Read the relevant one before starting work in its area; update it as decisions land.
- `TESTING_ROADMAP.md` — test-suite inventory, priorities, extraction backlog. Work top-down; check off items as completed.
- `claude/ui-redesign-spec.md` — anchor doc for the app-wide UI redesign: current state, agreed direction (not to be re-litigated), phased rollout. Phases 0–2 done; **Phase 3 (shared chart components) is the next entry point.** Each phase is its own plan-mode task.
- `claude/hardware-bringup-litevna64.md` — bench procedure + device-in-loop results log.
- `claude/calibration-teardown-plan.md` — calibration/live-instrument teardown. **Tiers 0–2 DONE** (`f2f5d5e`, `227237b`, `02cc9ec`): calibration is now live-only, persistence deleted. Tier 1/2 were re-scoped from the blueprint's keep-and-fix to a hard teardown, so its "two decisions pending" are **dissolved, not answered** — do not resurrect them. **Tier 3 is 3-of-4 closed; the sole open item is the confident-wrong `NANOVNA_H4` factory default** (root cause of Finding #7, plan mode, ~10 files).

## Tech stack
- Kotlin 2.2.10, Jetpack Compose (Material3, Compose BOM 2024.09.00)
- Android Gradle Plugin 9.1.0, Gradle wrapper
- compileSdk 36 / minSdk 26 / targetSdk 36
- Java 11 source/target
- Package: `com.example.antennalab_v1`

## Build / test
Windows: set `JAVA_HOME` to Android Studio's bundled JDK (e.g. `C:\Program Files\Android\Android Studio\jbr`) first. Use `.\gradlew.bat` from PowerShell; the `./gradlew` form only works via the Bash tool.
- Build debug: `.\gradlew.bat assembleDebug`
- Unit tests: `.\gradlew.bat test`
- Instrumented tests: `.\gradlew.bat connectedAndroidTest` (needs device/emulator)

Test layout: plain JVM for pure logic; Robolectric for anything needing a real `Context` or Android framework classes (e.g. `org.json`) — `@RunWith(RobolectricTestRunner::class)`, emulated SDK pinned to 34 in `app/src/test/resources/robolectric.properties`, first run downloads the SDK jar (needs network). Current inventory and per-area coverage live in `TESTING_ROADMAP.md` — do not duplicate the list here.

## Architecture

Single source of truth: `ProjectData` (model layer).

```
Wizard → ProjectData → CalculationEngine → CalculatedDesign → ProjectPageScreen (workspace) → Testing Tools → Storage
```

Layers under `app/src/main/java/com/example/antennalab_v1/`:
- `model/` — pure data only. NO UI logic, NO Android framework refs, NO calculation logic.
- `domain/` — calculation, analysis, testing logic (`CalculationEngine`, `SweepController`, `SweepAnalyzer`, USB/VNA drivers). Subpackages: `analysis`, `calculator`, `prediction`, `testing`.
- `features/` — UI screens/workflows (`app`, `lab`, `project`, `testing`, `wizard`, `workspace`)
- `project/` — main workspace hub (`ProjectPageScreen`)
- `storage/` — save/load (`ProjectStorage`, `ProjectIndexManager`)
- `ui/theme/` + `ui/components/` — design system (see below)

Hardware is capability-based, not hardcoded:
`ProjectData.testHardwareProfile` → capability profile → controls which UI features show (Smith chart, S21 estimate, TDR preview, CSV export, marker types, sweep frequency limits, OSL calibration). Supports NanoVNA-H4 and LiteVNA64 v0.3.3. Add new hardware by extending the capability profile — do NOT branch the UI.

`domain/testing/EffectiveHardwareResolver` is the single resolution point for "which hardware is actually measuring" (three-tier: validated live → selected+open live → project → deterministic default). Route capability reads through it. Design-time reads (hardware selector, build-sheet text, persistence, factory defaults) deliberately still use `project.testHardwareProfile`.

## Design system (UI)
Established by UI redesign Phases 0–2. New UI consumes these rather than hardcoding values or re-deriving state labels:
- Tokens in `ui/theme/`: `AntennaLabSpacing`, `AntennaLabTouch` (`field = 64.dp` — the documented gloved dial), `AntennaLabSemanticColors` (+ `LocalAntennaLabSemanticColors`), reached via the `AntennaLabTheme` accessor; provided additively in `Theme.kt`.
- Shared primitives in `ui/components/`: `StatusPill`, `MetricCard`, `AppActionButton`.
- **Anti-drift rule:** instrument state → chip mapping is the shared pure `InstrumentStatusPresenter.buildStatusChips`, consumed by both the dashboard and Device Connections. Add a state there, not per-screen.
- **Calibration honesty (non-negotiable):** calibration state shown in the UI is ALWAYS labelled as the APP's calibration, never ambiguous with the device's own — the known confusion trap for NanoVNA-Saver users.

## Calibration (OSL)

One-port Open/Short/Load calibration that captures reference measurements, holds
per-frequency correction coefficients **in memory**, and corrects raw S11 before it
becomes a `SweepResult`. Gated by the `supportsOslCalibration` capability flag (both
current devices enable it).

**LIVE-ONLY — calibration is never persisted (non-negotiable).** A calibration
belongs to the INSTRUMENT in the CURRENT session, not to a saved design file. There
is no `ProjectData.calibrationData`, no `ProjectCalibrationData`, no
`CalibrationRestorePolicy`, no restore path, and no calibration in project JSON. It
lives in `UsbSessionManager` and dies with the session. Restoring a stored
calibration into live state let a file manufacture a trust verdict — the exact
confusion the honesty rule exists to prevent. Torn out in `227237b` / `02cc9ec`;
rationale in `claude/calibration-teardown-plan.md`. **Do not reintroduce a stored
calibration field — add provenance instead.** Navigation and project loads must
never touch live calibration; `androidTest/…/LiveCalibrationSurvivesNavigationTest`
guards this and needs a device (`.\gradlew.bat connectedDebugAndroidTest`, NOT part
of `.\gradlew.bat test`).

Data flow:

```
CalibrationWizardScreen (capture O/S/L) → OslCalibrationEngine.computeCoefficients
  → CalibrationSession.correction (OslCalibrationCoefficients) → UsbSessionManager
      [in-memory, session-scoped — end of the line, nothing writes it to disk]
  → SweepController.runSweep → CalibrationCorrector.apply → SweepResult.isCalibrated
  → ProjectSweepHistoryEntry.isCalibrated  [PROVENANCE, persisted]
```

- **Model** (`model/testing/`): `OslCalibrationCoefficients` = per-frequency 3-term
  error model (directivity e00, source match e11, reflection tracking e10e01) as
  parallel re/im arrays. Hangs off `CalibrationSession.correction` (nullable) and is
  never serialized. `CalibrationSession.capturedSessionKey` /
  `capturedProtocolFamily` / `capturedInstrumentIdentityText` are live fields feeding
  the staleness detector — also not serialized.
- **What IS persisted is provenance, not state**: `SweepResult.isCalibrated` /
  `calibrationLabel` flag each sweep and `ProjectSweepHistoryEntry.isCalibrated`
  saves it. "This sweep was measured under calibration" is a fact about a
  measurement already taken — it is never read back into live state. Uncalibrated by
  default, so old saves load unchanged; a legacy `calibrationData` blob in an old
  save is silently ignored on read.
- **Domain** (`domain/testing/`): `OslCalibrationEngine` computes error terms from
  three captured standard sweeps (ideal standards: Open Γ=+1, Short Γ=−1, Load Γ=0)
  and holds the shared Γ↔impedance math. `CalibrationCorrector.apply()` is a single
  post-parse pass over a `SweepResult` — it reconstructs Γ from each point's R/X
  (exact, so it does NOT touch the two device parse seams), complex-interpolates the
  error terms to the point frequency, corrects, and rebuilds the point. `Complex` is
  the shared complex type. `SweepController.applyCalibrationIfAvailable` applies
  correction when the active calibration is VALID **or STALE** (a stale cal still has
  valid coefficients; reduced trust is surfaced separately).
- **Trust state**: `UsbSessionManager` holds `InstrumentCalibrationState`
  (readiness NOT_STARTED/IN_PROGRESS/VALID/STALE/INVALID + trust downgrade). Real
  captures bind to a live USB session (`registerCalibrationSession`); a capture with
  no live session is INVALID.
- **Debug (no hardware, `BuildConfig.DEBUG` only)**: `DebugOslCalibrationSimulator`
  synthesizes O/S/L and antenna sweeps through a fixed known error network. The
  calibration wizard's "Simulate O/S/L capture" chip captures without hardware and
  registers via `registerSimulatedCalibrationSession` (marks VALID); the sweep
  workspace's "Inject calibration error" chip (`SweepController.debugInjectCalibrationError`)
  passes the simulated sweep through that error network so correction can be verified
  end-to-end.
- **Philosophy**: flag, don't reject — an uncalibrated (or partial) sweep still runs
  and is flagged, never discarded.

## Conventions
- Respect layer boundaries strictly — no calc logic in `features/`, no UI/Android refs in `model/`
- **Extract-and-test pattern** (how new work gets written): pull non-UI decision logic out of large Compose screens into pure controllers, leaving thin private wrappers in the Composable so call sites don't move, then cover with JVM/Robolectric tests against the real `ProjectData` model and shared `UsbSessionManager` truth — **no Android mocking**. For a new UI phase, do the pure extraction and its tests *before* any Compose.
- Match the real file tree exactly — never invent files or assume a file exists because it's referenced
- Use plan mode before touching `ProjectData`, the capability system, or anything spanning multiple files
- Large files to handle carefully (>35KB): `UsbSessionManager.kt` (74KB), `UsbVnaCommandChannel.kt` (67KB), `ProjectStorage.kt` (52KB), `SweepGraphWidgets.kt` (51KB), `ProjectPageScreen.kt` (45KB), `SweepGraphScreen.kt` (44KB), `SweepToolsWidgets.kt` (36KB). Several are the roadmap's pending extraction targets.
- `DEBUGGING_PLAYBOOK.kt` and `SystemArchitecture.kt` in the root package are prose docs in `.kt` files, not code — the playbook describes the prescribed pipeline-tracing method for bug hunts.

## Known gaps / to verify
- **Hardware still unverified:** OSL at 145 MHz; NanoVNA-H4 entirely (does it reach Full Support and honour `sweepPoints=101`, or free-run like the LiteVNA?); `device_filter.xml` VID/PID against real units. OSL **passed** on a real LiteVNA64 at 14.2 MHz (correction applied, VALID, "Live Ready"). Open bench items and procedure: `claude/hardware-bringup-litevna64.md`.
- USB host support IS declared: `<uses-feature android:name="android.hardware.usb.host" android:required="false">` + a `USB_DEVICE_ATTACHED` intent filter on `MainActivity`, filtered by `res/xml/device_filter.xml` (currently VID `0x0483` / PID `0x5740` — ST CDC; widen if a unit reports different IDs).
- **LiteVNA64 v0.3.3 cannot be forced to a host-set point count (KNOWN LIMITATION, not a bug to re-diagnose).** The `sweepPoints` register (0x20) reads back correctly but the device free-runs its native ~201-point sweep; writing 0 to valuesFIFO (0x30) only *flushes*, and the firmware has no single-shot/pause/hold command. readFIFO drains far slower than the sweep produces points, so one coherent pass is unobtainable. `UsbVnaCommandChannel.runLiteVnaConfiguredSweepRead` therefore reconstructs the sweep by collecting DISTINCT in-range freqIndex across many reads (`DistinctInRangeAccumulator`), completes on all-present or a wall-clock budget, and honestly reports the partial count (e.g. ~77/101 in ~44 s) — real, correctly-frequenced, lower-resolution, flagged incomplete. Revisit only if a future firmware adds single-shot/hold. Full diagnosis: bring-up doc.