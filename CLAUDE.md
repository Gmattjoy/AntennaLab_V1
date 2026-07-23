# AntennaLab V1

Android app for antenna design, calculation, project management, and VNA-based sweep testing/tuning. Aimed at novice antenna builders through advanced radio operators — an antenna engineering workspace, not just a calculator.

## Tech stack
- Kotlin 2.2.10, Jetpack Compose (Material3, Compose BOM 2024.09.00)
- Android Gradle Plugin 9.1.0, Gradle wrapper
- compileSdk 36 / minSdk 26 / targetSdk 36
- Java 11 source/target
- Package: `com.example.antennalab_v1`

## Build / test
- Build debug: `./gradlew assembleDebug`
- Unit tests: `./gradlew test`
- Instrumented tests: `./gradlew connectedAndroidTest` (needs device/emulator)
- Real unit tests: `OslCalibrationEngineTest`, `CalibrationCorrectorTest` (plain JVM, calibration math); `ProjectStorageRoundTripTest`, `ProjectIndexManagerTest`, `DiscoverySnapshotPersistenceTest` (Robolectric, save/load + index serialization); `SweepWorkspaceControllerTest` (workspace state mutation + marker-tool logic, real simulated sweep path); `CalibrationSessionLogicTest` (Robolectric, calibration wizard session registration into `UsbSessionManager` + `CalibrationSession` readiness/matching helpers); `CreateAntennaWizardControllerTest` (create-antenna wizard flow logic — antenna-type mapping, live calc-request assembly, readiness lines, finish gating + starter-project assembly, Step 3 frequency/continue/status/description helpers); `CreateAntennaWizardNavigatorTest` (plain JVM, wizard step navigation + antenna-family/frequency-method state transitions); `ProjectWorkspaceControllerTest` (Robolectric, ProjectPageScreen workspace logic — sweep-return merge, calibration-session building via `UsbSessionManager`, workflow guidance, formatting); `DesignWorkspaceControllerTest` (plain JVM, DesignWorkspaceScreen display logic — summary sections, conditional calculated-results lines, mm formatting); `LoadProjectControllerTest` (plain JVM, LoadProjectScreen/Project Manager logic — initial list resolution, frequency/last-edited formatting, stored-calibration derivation). Scaffolding only elsewhere (ExampleUnitTest / ExampleInstrumentedTest).
- Robolectric is set up (`testImplementation(libs.robolectric)`, `testOptions.unitTests.isIncludeAndroidResources = true`, emulated SDK pinned to 34 in `app/src/test/resources/robolectric.properties`). Use `@RunWith(RobolectricTestRunner::class)` for tests needing a real `Context` / Android framework classes (e.g. `org.json`); the first run downloads the SDK jar (needs network).
- Windows: set `JAVA_HOME` to Android Studio's bundled JDK (e.g. `C:\Program Files\Android\Android Studio\jbr`) before running gradlew.

## Architecture

Single source of truth: `ProjectData` (model layer).

```
Wizard → ProjectData → CalculationEngine → CalculatedDesign → ProjectPageScreen (workspace) → Testing Tools → Storage
```

Layers under `app/src/main/java/com/example/antennalab_v1/`:
- `model/` — pure data only. NO UI logic, NO Android framework refs, NO calculation logic.
- `domain/` — calculation, analysis, testing logic (`CalculationEngine`, `SweepController`, `SweepAnalyzer`, USB/VNA drivers)
- `features/` — UI screens/workflows (wizard, testing, lab, app, workspace)
- `project/` — main workspace hub (`ProjectPageScreen`)
- `storage/` — save/load (`ProjectStorage`, `ProjectIndexManager`)
- `ui/theme/` — Compose theme

Hardware is capability-based, not hardcoded:
`ProjectData.testHardwareProfile` → capability profile → controls which UI features show (Smith chart, S21 estimate, TDR preview, CSV export, marker types, sweep frequency limits, OSL calibration). Supports NanoVNA-H4 and LiteVNA64 v0.3.3. Add new hardware by extending the capability profile — do NOT branch the UI.

## Calibration (OSL)

One-port Open/Short/Load calibration that captures reference measurements, stores
per-frequency correction coefficients, and corrects raw S11 before it becomes a
`SweepResult`. Gated by the `supportsOslCalibration` capability flag (both current
devices enable it). Data flow:

```
CalibrationWizardScreen (capture O/S/L) → OslCalibrationEngine.computeCoefficients
  → CalibrationSession.correction (OslCalibrationCoefficients) → UsbSessionManager
  → SweepController.runSweep → CalibrationCorrector.apply → SweepResult.isCalibrated
```

- **Model** (`model/testing/`): `OslCalibrationCoefficients` = per-frequency 3-term
  error model (directivity e00, source match e11, reflection tracking e10e01) as
  parallel re/im arrays. Hangs off `CalibrationSession.correction` (nullable).
  `SweepResult.isCalibrated` / `calibrationLabel` flag each sweep;
  `ProjectSweepHistoryEntry.isCalibrated` persists it. All additive, complete/
  uncalibrated-by-default so old saves load unchanged.
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
  end-to-end. Unit tests: `OslCalibrationEngineTest`, `CalibrationCorrectorTest`.
- **Philosophy**: flag, don't reject — an uncalibrated (or partial) sweep still runs
  and is flagged, never discarded. Real NanoVNA-H4 / LiteVNA64 hardware validation is
  still pending (verified so far by unit tests + the simulated debug path).

## Conventions
- Respect layer boundaries strictly — no calc logic in `features/`, no UI/Android refs in `model/`
- Match the real file tree exactly — never invent files or assume a file exists because it's referenced
- Use plan mode before touching `ProjectData`, the capability system, or anything spanning multiple files
- Large files to handle carefully (all >35KB): `UsbSessionManager.kt`, `SweepGraphWidgets.kt`, `UsbVnaCommandChannel.kt`, `ProjectStorage.kt`, `ProjectPageScreen.kt`

## Known gaps / to verify
- USB host support IS now declared: `<uses-feature android:name="android.hardware.usb.host" android:required="false">` + a `USB_DEVICE_ATTACHED` intent filter on `MainActivity`, filtered by `res/xml/device_filter.xml`. The filter currently matches VID `0x0483`/PID `0x5740` (ST CDC — NanoVNA-H4 / LiteVNA64); verify against real hardware and widen if a unit reports different IDs.
- Test coverage is still thin: OSL calibration math + `ProjectStorage` save/load serialization are covered, but most of the app (UI, sweep pipeline, wizard flow) has none. Robolectric is now available to cover Context-dependent code.
- OSL calibration is verified by unit tests + the simulated debug path only — not yet against real NanoVNA-H4 / LiteVNA64 hardware.

##.Working style: One task at a time, auto-accept edits (no manual per-file approval), commit after each change, verify via build + in-app testing. Short direct answers.

After completing any implementation phase, automatically stage and commit the change (code + any relevant control docs, e.g. ANTENNALAB_V1_* files) with a descriptive message. Do not wait for user confirmation to commit and do not present a "commit this" suggestion — just do it and move to the next step

## Roadmap
Current testing/hardening plan and priorities: see TESTING_ROADMAP.md. Work top-down; check off items as completed.
