# QFit — known issues

| Field | Value |
|---|---|
| Updated | 2026-10-03 |

## Rare: In-progress stuck, Cancel disabled

**Symptom:** Screen stays on「進行中 / 搖步中」, Cancel button stays disabled; only force-stop / swipe away and reopen recovers.

**Frequency:** Very rare during smoke test; **no reliable repro**.

**Likely cause (unconfirmed):** Cancel is `enabled = run != null && run.finished != true`. Stuck UI with disabled Cancel implies `RunSessionState.active` is `null` (or already `finished`) while navigation did not leave In-progress — e.g. process/UI desync after kill, or finished flag without successful home navigate.

**Action:** Recorded only; no fix until reproducible.

## Missing: wall-clock catch-up after doze / screen off

**Symptom:** After power button / long screen-off, returning to the app does not quickly “catch up” planned steps to wall clock.

**Cause:** By design of current MVP — `RunForegroundService` waits with coroutine `delay` per segment only. No logic compares wall clock to planned segment timeline and burst-writes missed intervals.

**Action:** Future work (“長跑穩定 / 追上牆鐘”); not a regression.
