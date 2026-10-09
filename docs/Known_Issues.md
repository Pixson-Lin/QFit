# QFit — known issues

| Field | Value |
|---|---|
| Updated | 2026-10-09 |

## Mitigated: In-progress stuck, Cancel disabled

**Symptom:** Screen stays on「進行中 / 搖步中」, Cancel button stays disabled; only force-stop / swipe away and reopen recovers.

**Observed trigger:** Not fully deterministic, but more likely after repeatedly turning the screen off
or switching apps during a run, then returning to QFit after the planned end time.

**Cause:** The Run screen depended on two volatile assumptions:

- `RunSessionState` (in-memory) and the restored Navigation back stack were always in sync.
- Home was always below In-progress in the back stack, so `popBackStack(HOME)` could not fail.

Activity/process restoration can violate either assumption. In particular, an Activity restored
directly into In-progress has no Home destination to pop to. Once the service marks the in-memory
run finished, Cancel becomes disabled, but the failed pop leaves the Run screen visible.

**Mitigation:** On every foreground transition, reconcile against the Room `RUNNING` row (the
durable source of truth): resume an orphan run, or leave a stale In-progress screen. Completion and
Cancel now explicitly navigate to Home while removing In-progress, so they also work when Home was
not already in the restored back stack.

**Verification sequence:** Start a run → screen off or switch apps → return to QFit → screen off
again → wait beyond the planned end → wake directly into QFit. Expected: QFit returns to Home
instead of remaining on Run with disabled Cancel.

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
