# QFit — known issues

| Field | Value |
|---|---|
| Updated | 2026-10-09 |

## Mitigated: In-progress stuck, Cancel disabled

**Symptom:** Screen stays on「進行中 / 搖步中」, Cancel button stays disabled; only force-stop / swipe away and reopen recovers.

**Observed trigger:** Not 100% deterministic, but more likely after:

1. Enter Run
2. Screen off (power) or switch apps
3. Bring QFit to foreground
4. Screen off again
5. After planned end time, wake with QFit already in the foreground

That pattern matches Activity teardown while the run FGS keeps the process (and in-memory
`RunSessionState`) alive.

**Cause:** The Run screen depended on two volatile assumptions:

- `RunSessionState` (in-memory) and the restored Navigation back stack were always in sync.
- Home was always below In-progress in the back stack, so `popBackStack(HOME)` could not fail.

When In-progress was chosen as `startDestination` (because `RunSessionState` still held an
active run after Activity death), the restored stack had no Home entry. After the service marked
`finished = true`, Cancel became `enabled = false`, but the failed pop left the Run screen up.
A second race: `collectAsStateWithLifecycle` pauses while STOPPED, so finish-driven navigation
could miss the transition that happened during screen-off until a later resume path recovered it.

**Mitigation:**

- NavHost always roots at Home; open In-progress with an explicit navigate.
- Completion / Cancel navigate to Home with `popUpTo(IN_PROGRESS)`.
- On every foreground transition, reconcile against the Room `RUNNING` row (resume orphan or leave
  stale In-progress), with one delayed recheck when waking past the planned end.
- Observe `RunSessionState` finished via the raw Flow (not lifecycle-paused UI state).
- If Cancel would be disabled, the button becomes「回主畫面 / Home」so the screen is never a dead end.

**Verification sequence:** Start a run → screen off or switch apps → return to QFit → screen off
again → wait beyond the planned end → wake directly into QFit. Expected: QFit returns to Home
(or shows Home on the primary button) instead of remaining on Run with a disabled Cancel.

## Wall-clock catch-up (0.4.0+)

**Shipped** in `0.4.0-catchup` (Scheme C lite). After screen-off / Doze delay, SCREEN_ON or the next alarm should burst-write due planned segments so steps approach wall clock.

**How to verify:** Start a run with **搖步提示列** on → lock screen 2–5 minutes → unlock → In-progress steps should jump toward elapsed × cadence (not stay frozen at pre-lock value).

**Still weaker when:** **搖步提示列** is off (no FGS notification), exact alarms revoked, or OEM kills the process aggressively — orphan resume on next app open still finalizes/catches up from Room plan.

## Cannot deep-link to app Battery three-option page (all OEMs)

**Symptom:** Third-party apps often cannot open Settings → Apps → QFit → Battery (不受限制 / 最佳化 / 受限) in one tap.

**Evidence (owner Samsung, SDK 35, 2026-10-05):**
- Target UI is `SubSettings` + fragment `PowerBackgroundUsageDetail` with `extra_package_name`.
- `SubSettings` is **not exported** → `SecurityException` / Permission Denial from QFit.
- Public trampoline `AdvancedPowerUsageDetailActivity` starts then Settings **crashes** (`NullPointerException` in `AppButtonsPreferenceController.updateArchiveButton`) → screen flash and return.

**QFit behavior (`0.4.14+`):** On **all** OEMs, show a guide dialog first, then open app info. Snackbar cannot appear over the Settings app.
