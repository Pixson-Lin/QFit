# QFit — known issues

| Field | Value |
|---|---|
| Updated | 2026-10-05 |

## Rare: In-progress stuck, Cancel disabled

**Symptom:** Screen stays on「進行中 / 搖步中」, Cancel button stays disabled; only force-stop / swipe away and reopen recovers.

**Frequency:** Very rare during smoke test; **no reliable repro**.

**Likely cause (unconfirmed):** Cancel is `enabled = run != null && run.finished != true`. Stuck UI with disabled Cancel implies `RunSessionState.active` is `null` (or already `finished`) while navigation did not leave In-progress — e.g. process/UI desync after kill, or finished flag without successful home navigate.

**Action:** Recorded only; no fix until reproducible.

## Wall-clock catch-up (0.4.0+)

**Shipped** in `0.4.0-catchup` (Scheme C lite). After screen-off / Doze delay, SCREEN_ON or the next alarm should burst-write due planned segments so steps approach wall clock.

**How to verify:** Start a run with **背景搖步** on → lock screen 2–5 minutes → unlock → In-progress steps should jump toward elapsed × cadence (not stay frozen at pre-lock value).

**Still weaker when:** **背景搖步** is off (no FGS notification), exact alarms revoked, or OEM kills the process aggressively — orphan resume on next app open still finalizes/catches up from Room plan.

## Cannot deep-link to app Battery three-option page (all OEMs)

**Symptom:** Third-party apps often cannot open Settings → Apps → QFit → Battery (不受限制 / 最佳化 / 受限) in one tap.

**Evidence (owner Samsung, SDK 35, 2026-10-05):**
- Target UI is `SubSettings` + fragment `PowerBackgroundUsageDetail` with `extra_package_name`.
- `SubSettings` is **not exported** → `SecurityException` / Permission Denial from QFit.
- Public trampoline `AdvancedPowerUsageDetailActivity` starts then Settings **crashes** (`NullPointerException` in `AppButtonsPreferenceController.updateArchiveButton`) → screen flash and return.

**QFit behavior (`0.4.13+`):** On **all** OEMs, open app info and show snackbar「請點選「電池」，再選「不受限制」」— consistent UX; no manufacturer branching.
