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
- (Only placeholder tests exist so far — ExampleUnitTest / ExampleInstrumentedTest)

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
`ProjectData.testHardwareProfile` → capability profile → controls which UI features show (Smith chart, S21 estimate, TDR preview, CSV export, marker types, sweep frequency limits). Supports NanoVNA-H4 and LiteVNA64 v0.3.3. Add new hardware by extending the capability profile — do NOT branch the UI.

## Conventions
- Respect layer boundaries strictly — no calc logic in `features/`, no UI/Android refs in `model/`
- Match the real file tree exactly — never invent files or assume a file exists because it's referenced
- Use plan mode before touching `ProjectData`, the capability system, or anything spanning multiple files
- Large files to handle carefully (all >35KB): `UsbSessionManager.kt`, `SweepGraphWidgets.kt`, `UsbVnaCommandChannel.kt`, `ProjectStorage.kt`, `ProjectPageScreen.kt`

## Known gaps / to verify
- USB host support IS now declared: `<uses-feature android:name="android.hardware.usb.host" android:required="false">` + a `USB_DEVICE_ATTACHED` intent filter on `MainActivity`, filtered by `res/xml/device_filter.xml`. The filter currently matches VID `0x0483`/PID `0x5740` (ST CDC — NanoVNA-H4 / LiteVNA64); verify against real hardware and widen if a unit reports different IDs.
- No real test coverage yet (only scaffolding tests).

## Legacy note
Earlier development used a manual numbered "EDIT SECTION" marker protocol plus full-file-replacement-by-hand (see the three `ANTENNALAB_V1_*.txt` docs, now archived under `docs/legacy/` alongside `ArchitectureMap.kt`). That solved a problem Claude Code handles natively (direct edits, plan mode, git diffs). Recommend retiring the marker system for new work rather than maintaining it. Flag if you'd rather keep it.
