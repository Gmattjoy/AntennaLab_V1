# AntennaLab V1 — Testing & Hardening Roadmap

Prioritized plan for extending the test suite and hardening the app.
Follows the established **extract-and-test pattern**: pull non-UI decision logic
out of large Compose screens into pure controllers (thin private wrappers left in
the Composable so call sites don't move), then cover with JVM/Robolectric tests
against the real `ProjectData` model and shared `UsbSessionManager` truth — no
Android mocking.

Current baseline: 164 tests, 0 failures. Controllers extracted so far:
SweepWorkspaceController, CalibrationSessionLogic, CreateAntennaWizardController,
ProjectWorkspaceController, DesignWorkspaceController, LoadProjectController,
DeviceConnectionsController, AppRootController, CalibrationSessionFactory. Also
covered: InstrumentStatusUiMapper (was already extracted; per-field mappers made
`internal` and tested).

## Priority 1 — Finish the extract-and-test sweep
Goal: no meaningful logic left buried in Compose files. (Main pass complete; audit
found a straggler backlog — see Priority 1b.)
- [x] DesignWorkspaceScreen — extract to pure controller + tests (DesignWorkspaceController, 8 tests)
- [x] Storage / save-load screen (LoadProjectScreen) — extract to pure controller
      + tests (LoadProjectController, 7 tests)
- [~] Prediction display — N/A: no screen renders PredictedPerformance yet, so
      nothing to extract. Covered instead as domain tests under Priority 2.
- [x] Hardware-selection UI (DeviceConnectionsScreen) — extract to pure controller
      + tests (DeviceConnectionsController, 12 tests). ProjectPageScreen's
      HardwareSelectionCard was already thin (logic in ProjectWorkspaceController).
- [x] InstrumentStatusUiMapper — covered (per-field mappers made `internal` and
      unit-tested; public entry points smoke-tested via Robolectric).
      InstrumentStatusUiMapperTest, 13 tests.
- [x] Verify all UI files are thin (delegate-only) after extraction — audit pass
      done across all ~38 features/ + project/ Compose files (2026-07-27). Confirmed
      thin: InstrumentDetailsScreen, HomeScreen, SystemMenuScreen, HomeIcons,
      WizardCommon, AntennaGraphics, LabTestTemplates (already a pure model),
      DesignWorkspaceCard, SweepTuningWidgets, Step4LiveDesignWorkspaceScreen
      (delegates to CreateAntennaWizardController), and the already-extracted
      controllers/models. The audit found remaining inline logic — tracked in
      Priority 1b below rather than fixed in the audit pass.

## Priority 1b — UI extraction backlog (from the 2026-07-27 audit)
Remaining non-UI logic still inline in Compose files. Do one at a time (extract to
a pure controller/model + tests), same pattern.

High value:
- [ ] CalibrationWizardScreen — no controller yet; capture state machine + session
      orchestration inline (button onClick 153-215, captureStandardSweep,
      findFirstIncompleteStepIndex, buildCapturedStepSession). Extract a
      CalibrationWizardController. (OSL math already in OslCalibrationEngine.)
- [ ] SweepGraphWidgets §17-18 (lines ~1496-1802) — pure axis-bounds / bandwidth /
      cable-fault / display-value math sitting in a widget file (violates its own
      header). Move into SweepGraphMath + tests.
- [ ] SweepWorkspaceViewModel buildUiModel path — run-contract decision engine
      (buildSweepRunContract 741-817), failure-message classifier
      (buildOperatorSweepFailureMessage), diagnostics/label/discovery formatting.
      Extract a pure UiModel builder/controller so it's testable off the VM.
- [ ] Step1AntennaTypeScreen — recommendAntennaFamily engine + AntennaRecommendation
      model (706-796) + completeness/gating predicates (173-193). Extract a wizard
      recommendation controller/model.
- [x] AppRootScreen — extracted to AppRootController + AppRootControllerTest
      (14 tests): project factories, template application, wizard finish
      normalization, calibration-wizard session build, and the stored-calibration
      restore-policy decision (side effects stay in the screen). NOTE: the audit
      called buildWizardCalibrationSession a "duplicate" of
      ProjectWorkspaceController's, but the fresh branch genuinely differed
      (target ± 0.5 MHz here vs full hardware range there).
- [x] Fresh-calibration range divergence RESOLVED (2026-07-29): unified both paths
      on the target-focused span (target ± 0.5 MHz, clamped to hardware limits) via a
      new shared `domain/testing/CalibrationSessionFactory`; AppRootController and
      ProjectWorkspaceController both delegate to it. Investigation confirmed the
      narrow span is correct — the wizard captures a fixed 101 points, so the full
      hardware range diluted them (~63 MHz spacing) and degraded correction, while
      every real sweep is target ± 0.25/0.5 MHz. CalibrationSessionFactoryTest
      (8 tests) + updated ProjectWorkspaceControllerTest fresh-range assertion.

Medium value:
- [ ] Step2AntennaOverviewScreen — antennaOverviewFor mapping + AntennaOverview model.
- [ ] LabHomeScreen — instrument-state → chip/summary mappings (110-159).
- [ ] SweepInstrumentUi — gauge model (gaugeDisplayValue/gaugeSubtitle/scale).
- [ ] SweepToolsWidgets — bandwidth calc, point-summary formatting, marker deltas.
- [ ] SweepGraphScreen — sweep-range derivation (89-124), hardware display-name
      mapping, StatusValueText keyword→status classifier.
- [ ] ProjectPageScreen — remaining inline: sweep-return "what changed" message
      decision (231-245), Save-As duplication/name-normalisation (165-176),
      totalConductorLength sum, per-numeric sweep-history formatting.

Cross-file duplication to fold in during the above:
- getDisplayValue (SweepGraphWidgets) vs gaugeDisplayValue (SweepInstrumentUi) + the
  inlined S21 formula.
- estimateBandwidthAtOrBelowSwr (SweepGraphWidgets) vs ...Local (SweepToolsWidgets).
- formatAntennaClassificationLabel (SweepGraphScreen vs SweepWorkspaceViewModel).

## Priority 2 — Pure domain-logic tests (fast, high value)
Test the engines directly — no UI involved.
- [ ] SweepAnalyzer — resonance detection, summary metrics, edge cases
- [ ] CalculationEngine — calc correctness across antenna types / frequency ranges
- [x] Prediction engine + environmental model — predicted performance outputs
      (EnvironmentalModelTest + SWRPredictionEngineTest, 10 tests)

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

_Last updated 2026-07-29._
