# AntennaLab V1 — Testing & Hardening Roadmap

Prioritized plan for extending the test suite and hardening the app.
Follows the established **extract-and-test pattern**: pull non-UI decision logic
out of large Compose screens into pure controllers (thin private wrappers left in
the Composable so call sites don't move), then cover with JVM/Robolectric tests
against the real `ProjectData` model and shared `UsbSessionManager` truth — no
Android mocking.

Current baseline: 107 tests, 0 failures. Controllers extracted so far:
SweepWorkspaceController, CalibrationSessionLogic, CreateAntennaWizardController,
ProjectWorkspaceController, DesignWorkspaceController, LoadProjectController.

## Priority 1 — Finish the extract-and-test sweep
Goal: no meaningful logic left buried in Compose files.
- [x] DesignWorkspaceScreen — extract to pure controller + tests (DesignWorkspaceController, 8 tests)
- [x] Storage / save-load screen (LoadProjectScreen) — extract to pure controller
      + tests (LoadProjectController, 7 tests)
- [ ] Remaining large Compose screens holding logic: prediction display,
      hardware-selection UI — extract + test each
- [ ] Verify all UI files are thin (delegate-only) after extraction

## Priority 2 — Pure domain-logic tests (fast, high value)
Test the engines directly — no UI involved.
- [ ] SweepAnalyzer — resonance detection, summary metrics, edge cases
- [ ] CalculationEngine — calc correctness across antenna types / frequency ranges
- [ ] Prediction engine + environmental model — predicted performance outputs

## Priority 3 — Save/load reliability (highest real-world risk)
Most likely failure to lose a user's work. Extend the existing
ProjectStorageRoundTripTest pattern.
- [ ] Round-trip ProjectData serialization — full model fidelity
- [ ] Edge cases: missing fields, corrupt data, partial writes
- [ ] Version migration — older saved projects load cleanly

## Priority 4 — Calibration & hardware reliability
- [ ] Calibration workflow end-to-end (beyond session logic already covered)
- [ ] USB/VNA driver reliability — NanoVNA-H4, LiteVNA64 (needs device-in-loop;
      not pure unit-testable — plan a manual/instrumented test checklist)

## Priority 5 — Feature work (after the safety net is solid)
- [ ] Guided tuning assistant
- [ ] Calibration workflow UI
- [ ] Hardware auto-detection
- [ ] Unknown-antenna discovery mode

## Working rules
- One task at a time; commit after each; keep the full suite green.
- UI/production-code changes: use plan mode, approve before applying.
- Test files / mechanical edits: auto-accept is fine.
- Update the CLAUDE.md test inventory after each new test batch.
- Robolectric conventions: `@RunWith(RobolectricTestRunner::class)` when a test
  needs real Context / Android framework (e.g. org.json). First run downloads
  the SDK jar (needs network). Windows: set JAVA_HOME to Android Studio's
  bundled JDK before running gradlew.

_Last updated 2026-07-24._
