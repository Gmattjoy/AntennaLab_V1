# LiteVNA64 — Hardware Bring-Up & Device-In-Loop Test Procedure

Executable bench procedure for validating AntennaLab V1 against a real LiteVNA64.
Satisfies the `TESTING_ROADMAP.md` Priority 4 item "USB/VNA driver reliability —
needs device-in-loop; not pure unit-testable — plan a manual/instrumented test checklist".

Everything here is grounded in the shipped code paths; file:line references are cited so
this doc can be re-verified rather than trusted. **If a constant below disagrees with the
source, the source wins — fix this doc.**

- **Device:** LiteVNA64, HW 64-0.3.3, FW v1.4.06
  (the label string lives in `DeviceConnectionsController.buildProfileDisplayLabel:72`)
- **Build:** debug — every diagnostic in §3 is `BuildConfig.DEBUG`-gated
- **Needs:** USB-OTG cable, `adb logcat` reachable, a known load (50 Ω) and a real antenna

---

## 1. Preconditions

1. `res/xml/device_filter.xml` must match the unit's VID/PID — currently `0x0483`/`0x5740`
   (ST CDC). If Android never offers the device or never prompts for permission, check
   this **first**: a mismatched filter means the OS never routes the device to the app.
   `<uses-feature android:name="android.hardware.usb.host">` + the
   `USB_DEVICE_ATTACHED` filter on `MainActivity` are already declared.
2. Debug build installed (release strips the logcat diagnostics).
3. Driver profile selected = a LiteVNA-style profile
   (`DriverProtocolType.LITE_VNA_V2_STYLE`; `DeviceConnectionsController.isLiteProfile:67`
   drives every LiteVNA-specific branch, and `preferredDefaultProfile:62` prefers it).

---

## 2. Bring-up stages

Bring-up runs **automatically** on session open via
`UsbSessionManager.ensureLiteVnaBringUpStarted:229` → `runLiteVnaBringUp:263`, on a
daemon worker thread with a **15 s join** (`:301`). It executes, in order:

```
LiteVnaSweepProtocol.checkBringUpReadiness()  →  probeIdentity()  →  runBasicCommandTest()
```

Manual re-trigger: the **Validate Device** button (`DeviceConnectionsScreen.kt:317`,
shown when `showValidateLiteVna(isLiteProfile, sessionOpen, transportReady)` is true).

Results land in `UsbSessionManager` (`registerLiteVnaBringUpResults:157`) and are read
back by the screen as three `LiteVnaBringUpResult`s (bringUp / identity / commandTest).

### Stage codes

The UI keys off these literal `stage` strings — record the one you actually observe.

| `stage` | Meaning | Source |
|---|---|---|
| `SESSION` | No USB session open | `checkBringUpReadiness:91` |
| `TRANSPORT` | Transport not ready | `:99` |
| `TRANSPORT_CHANNEL` | No active transport channel | `:107` |
| `READY` | Foundation OK | `:114` |
| `IDENTITY_QUERY` | Identity query itself failed | `probeIdentity:130` |
| `IDENTITY_MISMATCH` | Responded, but not as a LiteVNA | `:152` |
| `IDENTITY_CONFIRMED` | Name contains "lite"/"litevna" | `:145` |
| `REGISTER_UNEXPECTED_VALUE` | `0xF0` ≠ `0x02` | `runBasicCommandTest:167` |
| `SWEEP_PROBE_FAILED` | 8-point mini sweep failed | `:179` |
| `SWEEP_PROBE_OK` | Mini sweep decoded — **full pass** | `:213` |
| `TIMED_OUT` | 15 s worker join expired | `runLiteVnaBringUp:310` |

Registers are read through
`UsbVnaCommandChannel.readLiteVnaRegisterByteForBringUp:138` (CDC only — a non-CDC
transport fails fast by design). Expected: **`0xF0` deviceVariant = `0x02`**; `0xF1` =
protocol version (informational).

### On-screen text ↔ stage

`DeviceConnectionsController.buildValidationLabel:210` maps state to the operator label —
verify the screen agrees with the stage you saw:

| Label | Condition |
|---|---|
| `Pending` | LiteVNA profile, nothing confirmed yet |
| `Running` | session + transport ready, no result and no timeout |
| `Partial` | identity **or** register confirmed, not both |
| `Passed` | identity **and** register confirmed |
| `Timed Out` | any of the three results has stage `TIMED_OUT` |
| `Not Required` / `Ready` | non-LiteVNA profile |

Note `liteRegisterConfirmed:93` accepts stage `REGISTER_READ_OK` **or** `SWEEP_PROBE_OK`.
Next-step guidance text comes from `buildNextHardwareStepText:179`.

---

## 3. Sweep-path verification via logcat

```
adb logcat -s LiteVnaFifo        # per-attempt reconstruction trace
adb logcat -s LiteVnaFifoRaw     # full FIFO payload as chunked Base64
```

What you should see, and what each line means:

| Log line | Emitter | Read it as |
|---|---|---|
| `sweepPointsReadBack requested=101 lowByte=0x65` | `UsbVnaCommandChannel:411` | Config landed. `0xC9` (201) = the write did **not** take. `0x65` with scattered freqIndex is the known v0.3.3 behaviour (§5), not a bug. |
| `attempt=N chunkBytes=… chunkRecords=… distinctInRange=k/101 missing=…` | `:541` | One readFIFO re-issue. Expect ~2-3 records per attempt. |
| `sweepReconstruct distinct=k/101 complete=… attempts=… missing=[…]` | `:574` | Final verdict for the sweep. |
| `parse requested=… decoded=… inRange=… outOfRange=… distinctInRange=… duplicateInRange=… min=… max=…` | `LiteVnaSweepProtocol:456` | Decode breakdown. `max` near 200 on a 101-point request = the free-running superset. |
| `select validPoints=… parsePath=DIRECT_INDEX\|SEQUENTIAL_FALLBACK` | `:499` | Which parse path produced the points. |

### Capturing a payload as a test fixture

`logRawFifoPayload:565` dumps the entire raw FIFO buffer as `NO_WRAP` Base64 in 900-char
chunks under tag `LiteVnaFifoRaw` (`b64[i/n] …`). Concatenate the chunks in order,
save under `app/src/test/resources/litevna/`, and drive it from a pure-JVM test — this is
exactly how `fifo_145mhz_101req.b64` (3232 bytes, the real 145 MHz / 101-point capture)
was produced and how `LiteVnaFifoParserTest` reproduces the parse off-device.

**Any future FIFO anomaly should become a fixture + JVM test, not a device-only mystery.**

---

## 4. Pass criteria — and what "pass" is *not*

A 101-point request converges to a **partial** distinct count and is flagged
`isComplete = false`. **That is a PASS** — flag-don't-reject (§5). Observed reference:
~77/101 distinct in ~44 s.

Pass requires all of:

- [ ] Stage reaches `SWEEP_PROBE_OK`; label reads `Passed`
- [ ] `0xF0` = `0x02`
- [ ] `sweepPointsReadBack` = `0x65` for a 101-point request
- [ ] Distinct count converges (rises steadily across attempts, not stuck)
- [ ] **Frequencies are correct even when the count is short** — points are placed by
      decoded `freqIndex` (`parseSweepPoint:630`: `start + freqIndex·step`), so every
      returned point sits at its true frequency; a partial sweep is lower-resolution,
      not wrong. Verify the resonance lands where the antenna actually resonates.
- [ ] Short sweep reports `isComplete=false` downstream (not silently claimed complete)

Fail if: no enumeration, `IDENTITY_MISMATCH`, `0xC9` read-back, distinct count flat at 0,
or a short sweep presented as complete.

### Timing constants you will observe

All in `LiteVnaFifoReadBudget.kt` and `UsbVnaCommandChannel.kt:107-119`:

| Constant | Value | Where |
|---|---|---|
| Distinct-collection budget | `4000 + 400·points` ms, cap **45 s** | `computeDistinctCollectionBudgetMs:81` |
| Per-call FIFO budget | `3000 + 150·records` ms, cap **20 s** | `computeFifoReadBudget:45` |
| Backstop read passes | `ceil(bytes/packet) + 8` | `:68` |
| Idle-read tolerance (K) | **2** consecutive | `:57` |
| Max FIFO re-issues | **250** (runaway backstop only) | `MAX_FIFO_ACCUMULATION_ATTEMPTS:110` |
| Per-read timeout | **120 ms** | `FIFO_READ_TIMEOUT_MS:114` |
| Read cadence | `8 ms + rand(0..40)` jitter | `:118-119` |

The jitter is deliberate: it decorrelates our sampling phase from the device's
free-running sweep so no target index is systematically starved by aliasing.

---

## 5. Closed: the ~201-point free-run (KNOWN HARDWARE LIMITATION)

**Not an open bug — do not "fix" it.** LiteVNA64 v0.3.3 accepts and correctly reads back
the USB `sweepPoints` register (0x20) but keeps free-running its native ~201-point sweep,
so `freqIndex` scatters 0..200 regardless. Root-caused against DiSlord firmware:

- writing 0 to valuesFIFO (0x30) only **flushes** the queue — it does **not** restart or
  reset the sweep;
- sweeps free-run on hardware timers;
- there is **no** single-shot / pause / hold USB command.

The device returns ~2-3 records per readFIFO (~28 rec/s) against ~330 pts/s production, so
one coherent pass can never be drained — even aggressive back-to-back reads sample at a
stride (~30) across 0..200.

**Shipped behaviour (correct, final for this firmware):**
`runLiteVnaConfiguredSweepRead:275` reconstructs the sweep by collecting **distinct
in-range `freqIndex`** across many jittered reads (`DistinctInRangeAccumulator`, sized by
`computeDistinctCollectionBudgetMs`), completing on all-present **or** wall-clock, then
honestly reporting the partial count. Real data, correctly frequenced, lower resolution,
flagged incomplete.

Revisit only if a future firmware adds a single-shot/hold command.

---

## 6. Fixed + verified on real hardware

Four defects found *and fixed* during LiteVNA bring-up. This is what the device-in-loop
work actually bought — the procedure above exists to keep these fixed.

| # | Bug | Fix |
|---|---|---|
| 1 | **ANR on calibration Open-capture** — a blocking USB sweep ran on the main thread | Moved off-main (`Dispatchers.IO`) + re-entrancy guard |
| 2 | **Sweep width wrong for LiteVNA** — used NanoVNA's ±0.25 MHz because it read the stale *design-time* project profile | Resolve span/step from the **live selected instrument** (`resolveSweepWindow` / `resolveEffectiveIsLiteVna` in `SweepGraphMath`; LiteVNA ±0.5/0.01, NanoVNA ±0.25/0.02, clamped to hardware limits) |
| 3 | **SWR graph auto-scaled to millions** on near-total-reflection points, crushing real data | Display axis clamped at `SWR_DISPLAY_CEILING` = 100; **raw sweep data untouched** |
| 4 | **FIFO read truncated at ~20 records** (pass-count-driven: 10 passes × 64 B ÷ 32) | Count-driven + wall-clock-bounded read (`LiteVnaFifoReadBudget`, `readRawBytesUntil` tolerating K idle reads) |

**OSL calibration ran against the real LiteVNA64 at 14.2 MHz and PASSED** — correction
applied, calibration state `VALID`, readiness "Live Ready".

---

## 7. Open findings

### 7.1 Capability profile follows the STALE project profile, not the live instrument — **top priority, next code task**

Same class of defect as fixed-bug #2, but broader. `ProjectData.hardwareCapabilityProfile`
(`ProjectData.kt:112-113`) derives from `testHardwareProfile`, which **defaults to
`NANOVNA_H4`** (`ProjectData.kt:94-95`, and is hardcoded to it in the
`AppRootController` project factories at `:87`, `:100`, `:117`). `SweepGraphScreen.kt:80-81`
then reads that stale value and uses it for:

- the **frequency clamp** — `hardwareMinMHz` / `hardwareMaxMHz` (`:104-105`)
- **feature tiers** — Smith chart, S21 estimate, TDR preview, markers/delta markers
  (`:431`, `:470`, `:476`), CSV preview (`:315`, `:525`)
- the **support-tier / display name** shown to the operator (`:86`, `:1039`)

Net effect: **a validated, connected LiteVNA is treated as a NanoVNA** for limits and
features. Fix in the same shape as #2 — resolve the capability profile from the live
selected instrument, falling back to the project profile only when nothing is live.

### 7.2 Lower priority

- **Fixed ±0.5 MHz span can't characterize a broadband or unknown antenna.** Needs a
  wide / user-settable scan span. Blocks the roadmap's "unknown-antenna discovery mode".
- **Lab screen has no frequency input** — presets / hardcoded 14.2 MHz only; the wizard is
  the only way to set a frequency.
- **Sweep speed ~15-44 s** — a consequence of §5, but still the dominant UX cost.
- **A readiness label may show "Simulated Sweep" on the real path** — verify and correct
  the source label.
- **`SweepDiagnosticsEngine.classifyFeedlineLossSuspicion` MODERATE branch is
  unreachable** (`SweepDiagnosticsEngine.kt:489-492`). It requires
  `minimumSwrPoint.swr > 2.0` **and** `bandwidthMHz >= fullSpanMHz * 0.6` with
  `fullSpanMHz > 0.0` — but `bandwidthMHz` is the SWR≤2.0 span, which is necessarily **0**
  whenever the minimum SWR exceeds 2.0. Dead branch: `MODERATE` can never be returned.

---

## 8. Hypothesis to confirm on the bench — bring-up may time out on healthy hardware

**Do not fix in this pass — measure it first.**

`runLiteVnaBringUp` joins its worker for **15 s** (`UsbSessionManager.kt:301`). But
`runBasicCommandTest` → `runLiteVnaMiniSweepProbe:607` → `runLiteVnaConfiguredSweepRead`
with `pointCount = 8` now budgets `4000 + 8·400 = 7200 ms` of distinct collection, **plus**
a CDC handshake (`runLiteVnaCdcHandshakeTest:292`), four config writes, a FIFO clear, and a
settle delay — all on top of `probeIdentity`'s own identity query.

That is plausibly close to, or over, the 15 s join → a **healthy** LiteVNA would report
`TIMED_OUT` / "Timed Out".

Secondary: collecting 8 distinct in-range indices out of a free-running ~201-point sweep is
coupon-collecting a narrow window, so the mini probe will likely burn its full budget and
land on `SEQUENTIAL_FALLBACK` (which needs ≥ `MINIMUM_USABLE_SWEEP_POINTS` = 8 records —
normally plentiful).

**Capture steps:**
1. `adb logcat -v time -s LiteVnaFifo` while opening a session with the device attached.
2. Timestamp the first `attempt=1` line and the `sweepReconstruct` line → mini-probe
   wall-clock.
3. Note the observed `stage` / on-screen label. `TIMED_OUT` on a device that otherwise
   answers registers correctly **confirms** the hypothesis.
4. Note `parsePath` from the `select …` line.

If confirmed, the follow-up fix is one of: raise the join, or give the mini probe a
dedicated (much shorter) budget rather than the full distinct-collection budget.

---

## 9. NanoVNA-H4 — UNVERIFIED

Same skeleton (enumerate → identity → configured sweep) via
`domain/testing/NanoVnaSweepProtocol.kt`. It has **never been run against real hardware**;
sweep span/step is ±0.25 MHz / 0.02. Treat every claim about it as untested until a unit is
on the bench, then extend this doc with an H4 section mirroring §2-§4.

---

## 10. Still unverified

- [ ] **OSL calibration at 145 MHz** (14.2 MHz passed — see §6)
- [ ] **NanoVNA-H4, entirely** (§9)
- [ ] **`device_filter.xml` VID/PID against real units** — currently `0x0483`/`0x5740`;
      widen if a unit reports different IDs

## 11. Results log

Fill in per bench run.

| Date | FW | Stage reached | Label | `0xF0` | Points read-back | Distinct / requested | Elapsed | Parse path | Notes |
|---|---|---|---|---|---|---|---|---|---|
| | | | | | | | | | |
