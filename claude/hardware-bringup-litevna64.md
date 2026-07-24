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

1. **Attach the VNA, then: Grant Permission → Refresh → Connect, in that order.**
   `device_filter.xml` does **not** match this LiteVNA64 (§10), so plugging in never
   auto-launches the app and there is **no Connect button until permission is granted**
   (`showConnect = permissionGranted && !sessionOpen`). A screen stuck at
   `PERMISSION_REQUIRED` with only "Grant Permission" is the expected pre-grant state, not
   a fault. The filter mismatch does not block enumeration or use — only auto-launch and
   grant persistence.
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

## 7. Findings

### 7.1 Capability profile followed the STALE project profile — **RESOLVED**

`ProjectData.hardwareCapabilityProfile` derived from `testHardwareProfile`, which
**defaults to `NANOVNA_H4`**, so a connected, validated LiteVNA was treated as a NanoVNA.
Fixed by `domain/testing/EffectiveHardwareResolver` — one resolution point, pure
three-tier precedence (validated live → selected+open live → project → default), with
every capability consumer routed through it.

**The headline was the calibration loss, not the TDR error.** In severity order:

1. **A verified OSL calibration was silently DISCARDED on project load.** Calibrations
   captured on a real LiteVNA were stored under the *driver label*
   (`"LiteVNA64 HW 64-0.3.3 FW v1.4.06"`, written via
   `DeviceConnectionsController.buildProfileDisplayLabel`) but compared against the
   project's *capability displayName* — `"NanoVNA-H4"` from the stale default. Two
   different name vocabularies, so `normalizeIdentity` never matched and
   `decideCalibrationRestore` returned `CLEAR`. This destroyed real operator work,
   including calibrations of the kind verified passing at 14.2 MHz (§6). Note the
   resolver alone did **not** fix this — the names still came from different
   vocabularies; it needed canonicalised captures plus family-scoped alias matching.
2. **TDR velocity factor 0.66 instead of 0.82** → ~24% cable-fault distance error, at any
   frequency (`buildCableFaultPreview`).
3. **Frequency clamp** 1.5 GHz instead of 6.3 GHz — only bites above 1.5 GHz (e.g. a
   2.4 GHz antenna), so correctness rather than a user-visible fix.
4. **Calibration fresh-session** was named from the live session but clamped by the
   project profile — internally inconsistent regardless of which VNA was attached.
5. **Feature tiers** (Smith/S21/TDR/markers/CSV/OSL) were **identical** between both
   profiles and both map to `VNA_STANDARD`, so that part was future-proofing, not a fix.
   Routed through the resolver anyway so the next divergence can't reintroduce the bug.

Alias matching loosens a comparison, so it is deliberately family-scoped and tested to be
disjoint: no NanoVNA alias may match LiteVNA and vice versa (incl. `LITE_VNA_84` and the
driver-label form). The mis-accept direction is the dangerous one — applying the wrong
correction silently beats a cleared calibration.

### 7.2 `liveSweepAllowed` is reachable ONLY via `litePartialSupportAvailable` — **OPEN, not fixed today**

Found while diagnosing §8. This is the amplifier that turned a slow probe into a total
loss of the live sweep path, and it outlives the timeout fix.

**The causal chain, in full:**

1. `DriverProfileRegistry.kt:94` registers the LiteVNA64 with
   `supportTier = DriverSupportTier.EXPERIMENTAL`.
2. `UsbSessionManager.kt:1052-1060` computes the session's `supportTier` string:
   `litePartialSupportAvailable → "Partial Support"`, else a discovery snapshot's tier,
   else **`chosenProfile.supportTier.name`** — which for this device is the literal string
   **`"EXPERIMENTAL"`**.
3. `SweepUiModelBuilder.buildSweepRunContract:88-92` allows a live sweep only when
   `supportTier ∈ {"Full Support", "Partial Support"}`.
4. Therefore the registered `EXPERIMENTAL` tier is **dead weight — it can never satisfy the
   run contract.** The only route to a live sweep is `litePartialSupportAvailable`, which
   requires `liteValidationConfirmed`, which requires bring-up to complete inside the 15 s
   join.

**Consequence:** any bring-up failure — however transient, and whatever its cause — does
not merely delay validation, it removes the live sweep path entirely and silently
substitutes demo data. A 7.2 s probe overrun should have cost seconds; instead it cost the
whole live path, and the operator's only signal was a "Run Demo Sweep" button.

The §8 fix removes today's trigger but not this fragility: the next thing that makes
bring-up fail will have the same outsized blast radius. Options when addressed: let a
validated-transport `EXPERIMENTAL` device run a flagged live sweep, or make the run
contract key on transport readiness (as `SweepController.shouldUseRealSweepSource:171-177`
already does) with validation affecting *trust* rather than *permission* —
flag-don't-reject, as the rest of the app does.

### 7.3 TWO capability layers gave opposite answers about TDR — **RECONCILED 2026-07-24**

Same disease as Finding #7: **more than one source of truth for hardware capability.** Here
the two layers contradicted each other, and the UI happened to consult the one saying "no".

| Layer | Value | Consumers |
|---|---|---|
| `HardwareCapabilityProfile.supportsTdrPreview` | **`true`** for both profiles (`ProjectData.kt:598`, `:621`) | **NONE.** Its only accessor `supportsTdrPreviewOrDefault` (`ProjectData.kt:133-134`) has **zero call sites** — dead capability data. |
| `HardwareMeasurementCapabilities.supportsTDR` | **`false`** — `VNA_STANDARD` never set it (`HardwareMeasurementCapabilities.kt:67-79`) | The real ones: `SweepGraphMath.kt:652` (guard) and `SweepGraphWidgets.kt:569` (whether the row renders) |

Net effect: `buildCableFaultPreview` always returned **"TDR preview not supported by this
hardware."** on *every* device, so the velocity-factor fix (0.66 → 0.82) was correct and
unit-tested but had **no reachable UI surface**. That is what blocked A1.

**Resolved by setting `supportsTDR = true` in `VNA_STANDARD`** — recorded as reconciling a
contradiction, not as enabling a feature. `true` is correct on the merits: the preview is
derived from S11, which both one-port devices produce.

**Still open:** `supportsTdrPreview` remains dead data. Either wire it to the consumers or
delete it — leaving a populated, authoritative-looking flag that nothing reads is how this
contradiction survived unnoticed. Worth an audit of the other paired flags across the two
models for the same pattern.

### 7.3b Original finding (kept for the trace)

`buildCableFaultPreview` (`SweepGraphMath.kt:652-654`) returns
**"TDR preview not supported by this hardware."** unless `measurementCapabilities.supportsTDR`
is true. But `HardwareCapabilityProfiles.VNA_STANDARD`
(`HardwareMeasurementCapabilities.kt:67-79`) **never sets `supportsTDR`**, so it defaults to
`false` — and *both* `TestHardwareProfile` values map to `VNA_STANDARD` via
`toHardwareMeasurementCapabilities()`.

**So no device can ever display a cable-fault distance**, and the velocity-factor fix
(0.66 → 0.82) is correct in code and unit-tested but has no reachable UI surface.

Note the two-model inconsistency that hid this: `HardwareCapabilityProfile.supportsTdrPreview`
is **`true`** for both profiles (`ProjectData.kt:598`, `:621`), while
`HardwareMeasurementCapabilities.supportsTDR` is **`false`**. One capability model says the
feature is supported and the other says it isn't. This is exactly the divergence §7.1 #5
predicted would eventually bite.

Fix is one line — add `supportsTDR = true` to `VNA_STANDARD` — but it is a real behaviour
change (it makes the preview appear) and needs a rebuild, so it is a decision, not a
drive-by.

### 7.4 Measurement trust never reaches TRUSTED on a LiteVNA (expected, documented)

Bench-relevant so it isn't misread as a fault. `applyCalibrationTrustAdjustment:1202-1225`:
with session open and transport ready, a `VALID` calibration returns **base** trust
unchanged; every other readiness (`NOT_STARTED`/`IN_PROGRESS`/`STALE`/`INVALID`) maps
`PARTIAL`→`DEGRADED`. Base trust for a validated LiteVNA is `PARTIAL`
(`litePartialSupportAvailable`, `:1019`).

Therefore the expected progression is **Degraded → Partial** once OSL completes.
`TRUSTED` is unreachable on this path. A2's pass signal is "Partial", **not** "Validated";
trust remaining `Degraded` after a `VALID` calibration *would* be a defect.

### 7.5 Lower priority

- **Fixed ±0.5 MHz span can't characterize a broadband or unknown antenna.** Needs a
  wide / user-settable scan span. Blocks the roadmap's "unknown-antenna discovery mode".
- **Lab screen has no frequency input** — presets / hardcoded 14.2 MHz only; the wizard is
  the only way to set a frequency.
- **Sweep speed ~15-44 s** — a consequence of §5, but still the dominant UX cost.
- ~~A readiness label may show "Simulated Sweep" on the real path~~ — **RESOLVED as a
  duplicate of §8** (confirmed 2026-07-24). Not a labelling bug: the timed-out validation
  genuinely drops `supportTier` to `EXPERIMENTAL`, so the run contract really does select
  the simulated path. One finding, tracked in §8.
- **`SweepDiagnosticsEngine.classifyFeedlineLossSuspicion` MODERATE branch is
  unreachable** (`SweepDiagnosticsEngine.kt:489-492`). It requires
  `minimumSwrPoint.swr > 2.0` **and** `bandwidthMHz >= fullSpanMHz * 0.6` with
  `fullSpanMHz > 0.0` — but `bandwidthMHz` is the SWR≤2.0 span, which is necessarily **0**
  whenever the minimum SWR exceeds 2.0. Dead branch: `MODERATE` can never be returned.

---

## 8. CONFIRMED 2026-07-24 — bring-up times out on healthy hardware (BLOCKER)

**Measured: 15.2 s → `TIMED_OUT` / "Timed Out", on a device answering `0xF0 = 0x02`
correctly.** Elapsed pinned at the `worker.join(15000L)` expiry, and the status card read
"Transport Ready" rather than "Live Ready" — both exactly as predicted below.

### Why this blocks the whole bench day

The timeout is not cosmetic. `liteValidationConfirmed = false` →
`litePartialSupportAvailable = false` → `supportTier` falls through to
`chosenProfile.supportTier.name` = **`"EXPERIMENTAL"`** (`DriverProfileRegistry.kt:94`
registers the LiteVNA as `DriverSupportTier.EXPERIMENTAL`). But
`SweepUiModelBuilder.buildSweepRunContract:88-92` requires
`supportTier ∈ {"Full Support", "Partial Support"}` for `liveSweepAllowed`, and
`"Partial Support"` is produced **only** by `litePartialSupportAvailable`
(`UsbSessionManager.kt:1053`). So a timed-out validation makes a live sweep
**unreachable from the UI**:

- `dataSourceKind == SIMULATED` → button "Run Demo Sweep", `runUsesSimulation = true`
- `dataSourceKind == REAL_INSTRUMENT` → `runEnabled = false`, "Run Sweep Locked"

Note the execution layer is *not* the problem: `SweepController.shouldUseRealSweepSource:171-177`
gates purely on transport readiness and never consults `dataSourceKind` or support tier.
The block is entirely in the UI run contract. Likewise the effective-hardware resolver is
unaffected — its tier 2 (selected + session open) still yields `LITEVNA64_V0_3_3` without
validation, so capability/TDR/calibration-range resolution stays correct.

**This also subsumes the old §7.3 item "a readiness label may show Simulated Sweep on the
real path" — same root cause, one finding, not two.** The support-tier coupling that made
this so damaging is tracked separately as **§7.2**, and is *not* fixed by the timeout fix.

### Original hypothesis (now confirmed)

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
- [x] **`device_filter.xml` VID/PID against real units — ANSWERED 2026-07-24, and the
      declared IDs are WRONG for the LiteVNA64.** The real unit reports:

      ```
      manufacturer_name=ZeenKo.tech   product_name=LiteVNA6
      vendor_id=1204 (0x04B4, Cypress)   product_id=8 (0x0008)
      class=2 (CDC) → if0 CDC-control (2/2/1), if1 CDC-data (10), bulk max_packet_size=64
      ```

      `device_filter.xml:14` declares `1155`/`22336` = `0x0483`/`0x5740` (STMicro), and the
      file's comment asserting both VNAs enumerate as ST CDC is incorrect — at least for
      this LiteVNA64. It does **not** block enumeration or use — `UsbSessionManager.kt:582`
      and `UsbPermissionManager.kt:65` read `usbManager.deviceList` unfiltered. Fix by
      adding a `0x04B4`/`0x0008` entry (keep the ST entry until the H4 is measured).

      **It DOES change operator procedure every session (confirmed on the bench).**
      Android grants USB permission two ways: (1) the **attach-intent route** — a device
      matching `device_filter.xml` fires `USB_DEVICE_ATTACHED`, the app is launched with the
      `UsbDevice` in the intent and is implicitly permitted, and this is the only route that
      offers the *"use by default for this USB device"* checkbox; (2) the **runtime route**,
      `UsbManager.requestPermission()`. With the filter not matching, route 1 never fires, so:

      - plugging in never auto-launches the app;
      - **"Grant Permission" is the mandatory first step of every session** — the screen sits
        at `PERMISSION_REQUIRED` with *no* Connect button until it is tapped
        (`showConnect = permissionGranted && !sessionOpen`, `DeviceConnectionsController:154`);
      - because the persistence checkbox only exists on route 1, the grant is not reliably
        durable across a replug/re-enumeration — every physical reconnect starts over.

      This cost real bench time on 2026-07-24: the operator looked for a Connect button that
      cannot appear pre-grant, and three A0 runs were reported that could not have happened
      (no session → no bring-up → zero `LiteVnaFifo` output → `CalRestore` logging
      `effective=NANOVNA_H4`). **Bench procedure: after attaching the VNA, always
      Grant Permission → Refresh → Connect, in that order.**

- [ ] **NanoVNA-H4 VID/PID** — still unmeasured. **Read it with
      `adb shell dumpsys usb`** (`host_manager.devices`), which needs no root and also
      reports manufacturer/product names. Do **not** use the `/sys/bus/usb/devices/*/idVendor`
      route — those reads are permission-denied without root on this tablet.

## 11. Results log

Fill in per bench run.

| Date | FW | Stage reached | Label | `0xF0` | Points read-back | Distinct / requested | Elapsed | Parse path | Notes |
|---|---|---|---|---|---|---|---|---|---|
| 2026-07-24 | v1.4.06 | `TIMED_OUT` | Timed Out | `0x02` | — | — | **15.2 s** | — | **A0 run 1, pre-fix — §8 CONFIRMED.** Healthy device: `0xF0` answered `0x02` correctly, status card read "Transport Ready" (not "Live Ready") exactly as predicted. Elapsed pinned at the 15 s join. **BLOCKER** — see §8. Runs 2-3 skipped on this build: the code trace was decisive and re-measuring an expired join buys nothing. |
| 2026-07-24 | v1.4.06 | `SWEEP_PROBE_OK` | **Passed** | `0x02` | `0x08` (8-pt probe) | 8/8 records (`distinctInRange=2/8`) | probe **2.03 s**; <15 s total | `SEQUENTIAL_FALLBACK` | **A0 PASS on both gates — build `e479d5d`, §8 fix VERIFIED.** Status "LIVE READY", Data Source `REAL_INSTRUMENT`, and the sweep screen offered **"Run Live Sweep"** enabled — the actual unblock. Mechanism confirmed: `probeReconstruct stop=records-satisfied rawRecords=8/8 budgetMs=2500 attempts=4`. Collection took **1.084 s** (`attempt=1` 11:51:21.372 → `probeReconstruct` 11:51:22.456). **Diagnosis proven by `distinctInRange=2/8`**: after 4 reads only indices 0,1 of the needed 0..7 had arrived (`freqSeq=[0,1,122,123,48,49,173,174]`, `max=174` — the §5 free-run scatter), so the old all-distinct rule would have burned its full 7.2 s and still not completed. ~6.1 s saved per bring-up. `LITEVNA_PROBE_MIN_RECORDS=8` validated in situ: `parsePath=SEQUENTIAL_FALLBACK` with `validPoints=8` — the probe passes *because* 8 records is exactly what the fallback needs. Elapsed later measured via the `BenchState` line on a second run: session-open + `validation='Running'` 12:25:05.854 → `validation='Passed'` 12:25:11.126 = **5.27 s** (slight over-count — the session opens just before that render), vs 15.2 s pre-fix. Trust `Degraded` — expected, calibration `NOT_STARTED` (see §7.4). |
| 2026-07-24 | — | — | — | — | — | — | — | — | *False start, kept as a process note.* An earlier A0 pass was reported that the device log contradicted — zero `LiteVnaFifo` lines across two app launches and `CalRestore` reporting `effective=NANOVNA_H4`, which the resolver only returns when no LiteVNA session is open. **Cause: the screen was at `PERMISSION_REQUIRED`, so no Connect button existed and no session was ever opened** (see §1 and §10 — a consequence of the VID/PID mismatch). Cost ~35 min. Lesson applied: verdicts are read from logcat, not reported from the UI. |
