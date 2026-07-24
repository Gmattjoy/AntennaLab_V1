# AntennaLab V1 — Testing & Hardening Roadmap

Prioritized plan for extending the test suite and hardening the app.
Follows the established **extract-and-test pattern**: pull non-UI decision logic
out of large Compose screens into pure controllers (thin private wrappers left in
the Composable so call sites don't move), then cover with JVM/Robolectric tests
against the real `ProjectData` model and shared `UsbSessionManager` truth — no
Android mocking.

Current baseline: 281 tests, 0 failures. Controllers extracted so far:
SweepWorkspaceController, CalibrationSessionLogic, CreateAntennaWizardController,
ProjectWorkspaceController, DesignWorkspaceController, LoadProjectController,
DeviceConnectionsController, AppRootController, CalibrationSessionFactory,
CalibrationWizardController, SweepUiModelBuilder, Step1AntennaTypeController. Also
covered: InstrumentStatusUiMapper (was already extracted; per-field mappers made
`internal` and tested).

### ▶ Next up (2026-07-24)
The LiteVNA hardware/sweep-pipeline saga is closed (Finding #8 + parts; interim
shipped, force-101-on-v0.3.3 logged as a known limitation), and the device-in-loop
bring-up procedure is now written up in `claude/hardware-bringup-litevna64.md`.

**DONE (Finding #7): the stale capability profile.** `domain/testing/EffectiveHardwareResolver`
is now the single resolution point (three-tier: validated live → selected+open live →
project → deterministic default), with every capability consumer routed through it and
design-time reads left alone. Headline fix was a verified OSL calibration being silently
discarded on project load — stored under the driver label, compared against the project's
capability displayName, so the names never matched. Also fixed the TDR velocity factor
(0.66 vs 0.82, ~24% cable-fault distance error) and the >1.5 GHz frequency clamp. Feature
tiers were identical between profiles, so that part is future-proofing. 25 new tests
(EffectiveHardwareResolverTest incl. cross-family alias-safety; AppRootControllerTest
restore-policy both directions; SweepGraphMathTest cable-fault regression).

**Next: Priority 3 — Save/load reliability** (the roadmap's flagged *highest
real-world risk* — losing a user's project; fully open, pure/JVM-testable by
extending `ProjectStorageRoundTripTest`, no device needed). Quick-win alternative if
a short task is wanted: **Priority 2 — CalculationEngine**. Then return to the
Priority 1b medium-value UI extractions (Step2AntennaOverviewScreen, LabHomeScreen,
SweepInstrumentUi, SweepToolsWidgets, SweepGraphScreen, ProjectPageScreen).

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
- [x] CalibrationWizardScreen — extracted the OSL capture state machine to
      CalibrationWizardController (findFirstIncompleteStepIndex, sweepStepMHz,
      buildCapturedStepSession with injected clock, applyCapturedStandard); side
      effects (sweep run, session registration, callbacks) stay in the Composable.
      CalibrationWizardControllerTest, 4 tests. (OSL math already in OslCalibrationEngine.)
- [x] SweepGraphWidgets §17-18 — moved the pure axis-bounds / bandwidth /
      cable-fault / display-value math into SweepGraphMath (now `internal`, single
      source), widgets left as thin renderers. Also folded the three duplicate copies
      (SweepInstrumentUi.gaugeDisplayValue, SweepToolsWidgets.estimateBandwidthAtOrBelowSwrLocal,
      SweepWorkspaceController's private getDisplayValue) into the shared math.
      SweepGraphMathTest, 16 tests. Bugfix (2026-07-23): the SWR display axis now
      clamps its maximum at `SWR_DISPLAY_CEILING` (100) so absurd near-total-reflection
      points (e.g. SWR ~20,000,000) can't blow the axis to millions and crush real
      data; display-only, raw sweep values untouched. Bugfix (2026-07-23): the
      LiteVNA sweep width no longer resolves from the project's design-time
      testHardwareProfile (which defaults to NanoVNA for all Lab/discovery/RF-test
      entries, so a connected LiteVNA wrongly got ±0.25). New pure
      `resolveSweepWindow` + `resolveEffectiveIsLiteVna` helpers resolve span/step
      from the live selected instrument (LiteVNA ±0.5, NanoVNA ±0.25, clamped),
      falling back to the project profile only when nothing is live.
- [x] SweepWorkspaceViewModel buildUiModel path — extracted the pure decision/
      formatting logic into `SweepUiModelBuilder` (run-contract decision engine
      buildSweepRunContract now takes sweepRunInProgress as a param; failure-message
      classifier buildOperatorSweepFailureMessage; diagnostics/source-label/
      selected-path/fallback/discovery formatting + formatAntennaClassificationLabel).
      The VM delegates to it; side-effecting bits (resolveActiveFailureMessage,
      instrument-session/status-card build, sweep execution) stay in the VM.
      SweepUiModelBuilderTest, 22 tests.
- [x] Step1AntennaTypeScreen — extracted the recommendAntennaFamily engine +
      AntennaRecommendation model + completeness/gating predicates into a pure
      `Step1AntennaTypeController` (recommendAntennaFamily, isFrequencySectionComplete,
      isServiceSectionComplete, canProceed); the Composable delegates via thin
      call sites. Step1AntennaTypeControllerTest, 20 tests (plain JVM).
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
- [x] getDisplayValue (SweepGraphWidgets) vs gaugeDisplayValue (SweepInstrumentUi) vs
  SweepWorkspaceController's private copy — unified in SweepGraphMath.getDisplayValue
  (S21 via estimateS21Db).
- [x] estimateBandwidthAtOrBelowSwr (SweepGraphWidgets) vs ...Local (SweepToolsWidgets)
  — unified in SweepGraphMath.estimateBandwidthAtOrBelowSwr.
- [x] formatAntennaClassificationLabel — folded into the shared
      `SweepUiModelBuilder.formatAntennaClassificationLabel`. The audit found THREE
      byte-identical copies (not two): the private ones in SweepGraphScreen and
      SweepWorkspaceController now delegate to the shared public copy (the
      SweepWorkspaceViewModel copy was already folded into SweepUiModelBuilder).

## Priority 2 — Pure domain-logic tests (fast, high value)
Test the engines directly — no UI involved.
- [x] SweepAnalyzer — resonance detection, summary metrics, edge cases. Covered
      both analysis engines: SweepAnalyzerTest (7 tests — findMinimumSWR /
      getResonantFrequencyMHz, empty/single/tie/unordered) and
      SweepDiagnosticsEngineTest (29 tests over analyzeSweep — hand-derived min-SWR /
      resonance / SWR≤2.0/≤1.5 bandwidth spans incl. the exactly-2.0 inclusive
      boundary, secondary-resonance selection, empty/single-point degenerate input,
      and characterization of every classifier: matching quality, mismatch severity,
      impedance stability, sweep shape, reactance trend, resonance count, likely
      condition, feedline-loss suspicion, summary string). Physical outputs are
      hand-derived, not mirrored from the engine.
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
- [~] USB/VNA driver reliability — NanoVNA-H4, LiteVNA64 (needs device-in-loop;
      not pure unit-testable). **Checklist written:
      `claude/hardware-bringup-litevna64.md`** — stage-by-stage bring-up procedure,
      logcat verification, pass criteria, the logcat→JVM-fixture capture recipe, the
      four bugs found+fixed on real hardware, open findings, and a results log.
      LiteVNA64 side is executable now; NanoVNA-H4 still needs a unit on the bench.
- [x] LiteVNA incomplete-sweep bug (Finding #8): a live 101-point sweep returned
      only ~20 points. Cause: the FIFO read was pass-count-driven — maxReadPasses(10)
      × packetSize(64) ÷ 32 capped each read at 20 records, plus readRawBytes bailed
      on the first idle read, plus a completeRecordCount≥8 stall early-out. Fixed by
      making the read COUNT-driven and wall-clock-bounded: new pure
      `LiteVnaFifoReadBudget` (records→bytes→backstop passes + wall-clock; unit-tested,
      LiteVnaFifoReadBudgetTest, 9 tests), a count-driven `UsbCdcSerialChannel.
      readRawBytesUntil` (tolerates K consecutive idle reads, not 1), and a refactored
      `UsbVnaCommandChannel` FIFO loop that re-issues readFIFO until all expected
      records arrive or the wall-clock deadline. On-device the LiteVNA dribbles ~2-3
      records per readFIFO, so tuning was needed (K=2 idle, 120 ms read timeout, ~18 s
      budget, 250-attempt backstop); verified on real hardware draining 3→101 in
      ~15 s (per-attempt `LiteVnaFifo` logcat, DEBUG-gated). USB IO stays device-only;
      only the read-budget arithmetic is unit-tested.
- [x] LiteVNA parser filtering (Finding #8 part 2, RESOLVED as interim + known limit):
      the FIFO read delivers 101/101 raw records, but the LiteVNA64 v0.3.3 free-runs its
      native ~201-point sweep and IGNORES the USB sweepPoints register (register
      read-back = 101, yet freqIndex scatters 0..200). Root-caused against DiSlord
      firmware: valuesFIFO write-0 only flushes the queue (does NOT restart the sweep),
      sweeps free-run on hardware timers, and there is no single-shot/pause command; the
      device returns ~2-3 records/readFIFO (~28 rec/s) vs ~330 pts/s production, so we
      cannot drain one coherent pass (aggressive back-to-back reads still stride ~30
      across 0..200, not sequential). Shipped the correct interim: reconstruct the sweep
      by collecting DISTINCT in-range freqIndex across many jittered reads
      (`DistinctInRangeAccumulator`, completion via `computeDistinctCollectionBudgetMs`),
      completing on all-present OR wall-clock, then honestly reporting the partial count
      (~77/101 in ~44 s) — real, correctly-frequenced, lower-res, flagged incomplete.
      Force-101-on-v0.3.3 is a documented known limitation (see CLAUDE.md). Pure helpers
      unit-tested (LiteVnaFifoParserTest convergence/starvation + LiteVnaFifoReadBudgetTest
      budget); DEBUG `LiteVnaFifo` logcat reports distinct-count / missing indices.

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
