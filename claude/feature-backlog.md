# AntennaLab V1 — Feature Backlog (P7+)

_Post-parity feature roadmap. Ranked "ship-to-parity first, then differentiate."
Sequencing logic at the bottom. Complements `claude/testing-roadmap.md` (hardening)
and `claude/ui-redesign-spec.md` (P0–P6 UI). This doc = net-new capability._

Anchor: settings feature now exists — it is the home for state persistence, axis
prefs, and cal-kit storage. Several items below are near-free because of it.

---

## P7 — Parity table-stakes (users expect these; cheap wins)
Ship these first — they're the connective tissue and the loudest complaints.

- **Last-session state restore** ⭐ TOP — persist markers, span, sweep points,
  display toggles across app restarts, separate from projects. #1 recurring
  NanoVNA-Saver complaint ("lose my markers, number of points, start/stop
  frequencies every restart"). Near-free now that the settings feature exists +
  ProjectData persistence is already built.
- **Touchstone .s1p/.s2p import + export** — the RF interchange standard; unlocks
  NanoVNA-Saver / scikit-rf interop. Higher leverage than CSV alone.
- **Band / frequency presets** — ham bands, ISM, etc. Trivial; high daily value.
- **Don't clobber user settings on connect** — a toggle to preserve start/stop/
  sweep on device connect. Same class as our known restore-precedence collision
  (project-open resets live calibration). Fix the surprise-state-stomping pattern.
- **Continuous / repeat sweep** — already queued; blocked by Findings #9/#10.
- **Screenshot / PDF report export.**

## P8 — Trace system (one architectural push; many features fall out)
Build the memory-trace substrate once; overlays + trace math + save/recall all
derive from it. Do before P10 analysis (matching solver + overlays want this
underneath).

- Memory traces + trace math (A, B, A−B, A/B).
- Before/after tuning overlay (falls out of memory traces).
- Save/recall trace sets independent of projects.
- Marker table export.

## P9 — Calibration depth
- **Port extension / electrical delay / reference-plane offset** — do FIRST here:
  ~80% of the accuracy benefit for ~10% of the work.
- **Cal notes** — free-text describing cal conditions, saved with the cal
  (NanoVNA-Saver parity). Stored via settings feature.
- **Characterized cal-standard sets** — enter real cal-kit coefficients (not
  ideal-standard assumptions), save named sets. Stored via settings feature.
- **Multiple cal kits + coefficient editing.**
- **Guided calibration assistant** — extends the guided-tuning direction to cal
  (NanoVNA-Saver has a "Calibration Assistant"). Differentiator-adjacent.
- **Full 12-term / e-cal** — heavy; only where hardware supports.

## P10 — Analysis engine (differentiators)
- **Q factor + bandwidth-at-SWR auto-callout** — easy; extends existing
  SweepAnalyzer.
- **Cable loss / velocity-factor solver** from measured data.
- **Matching-network solver** ⭐ (L / Pi / T component values → 50 Ω) — flagship
  differentiator.
- **Antenna efficiency / gain estimate** — hardest; needs modelling assumptions.

## P11 — Capture depth (hardware-heavy)
- **Multi-segment / stitched wideband sweep** — also solves Finding #5 (discovery
  needs a wide/user-settable span) and is the community workaround for fixed
  point-count limits (relevant to Finding #10).
- **Averaging / smoothing** across sweeps to cut noise.
- **Full 2-port S11 + S21 simultaneous** (where hardware allows).
- **Time-domain gating** (beyond current TDR preview).
- **Single-frequency live read** — already queued; feeds guided tuning.
- **Logscale frequency axis** — matters for wideband / discovery. Pref lives in
  settings.

## P12 — Platform
- **Firmware detection + per-firmware behavior** — directly relevant to Finding
  #10 (v0.3.3 free-run). Route behavior on detected firmware, not assumptions.
- **Cloud / backup sync.**

---

## Sequencing logic
- **Last-session state restore + Touchstone I/O first** — cheapest high-impact
  parity; state restore is the top real-world complaint and now near-free.
- **Trace system (P8) before analysis (P10)** — matching solver + overlays want
  memory traces underneath.
- **Port extension before full cal (P9)** — 80% of accuracy gain for 10% of work.
- **Multi-segment sweep (P11)** is the real unlock for unknown-antenna discovery +
  Finding #5.
- **Cross-cutting theme from the forums:** stability + not losing user state beats
  new features. The 507-green suite covers the stability half; state persistence
  covers the other half — our easiest differentiator vs. incumbents.

## Cross-references
- Settings feature: home for state restore, axis prefs, cal notes, cal-kit sets.
- Finding #5 (wide-scan span) → P11 multi-segment.
- Finding #10 (firmware free-run) → P12 firmware detection.
- Restore-precedence collision (live-cal reset on project-open) → P7 "don't clobber
  on connect" pattern.
- Guided tuning assistant (existing direction) → P9 guided cal + P10 matching
  solver + P11 single-freq live read.

_Created 2026-08-08._
