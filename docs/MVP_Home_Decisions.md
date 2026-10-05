# QFit MVP — product decisions

| Field | Value |
|---|---|
| Updated | 2026-10-03 |
| Status | Locked with owner |

## Product

- No Google Sign-In.
- APBFit = read-only reference ([AGENTS.md](../AGENTS.md)).
- Screens: Home + In-progress + History + About.
- Run engine writes HC steps/distance/exercise.

## Home

| Item | Decision |
|---|---|
| Intensity | 5 presets; SPM: 散步 80, 超慢跑 140, 慢跑 165, 馬拉松 180, 衝刺 210 |
| Duration | 5 min – 4 h, **step 5 min**; first-run default **20 min** |
| Estimate | `durationMinutes × SPM` (actual run adds segment noise) |
| Env checkboxes | HC / battery / exact-alarm: checked = ready; tap = request / settings |
| **背景搖步** | Preference (default **on**). On = foreground service + ongoing notification (harder for OS to kill). Off = plain service, **no notification bar**, higher chance of being killed in background. Persisted in `RunConfigStore`. Turning on may request POST_NOTIFICATIONS on API 33+. |
| Health Connect | Auto-request on launch if missing; checkbox = readiness + manual jump |
| Menu | About |
| History button | Opens History screen |
| Start | Requires intensity + HC write permissions |

## About

- Bottom of screen shows `versionName` and `versionCode` from `BuildConfig`.
- Versioning rules: [Versioning.md](Versioning.md).

## In-progress

- Rows are **display only**; **no chevrons**.
- Card uses `secondaryContainer` background.
- Cancel → **confirm dialog**; stop future writes; keep HC data already written.
- Status after end: 已完成 or 已取消.

## History

- Persist runs + segments in **Room** (no sample data; empty state when none).
- History list hides in-flight `RUNNING` rows.
- Card summary: start time, type, planned vs actual duration, steps, status.
- Card border: `1.5.dp` `primary` purple stroke for clearer separation.
- **詳細記錄** collapsed by default; label is **green + underline**; tap expands segment lines (PLANNED hidden).
- Segment line: `#n HH:mm:ss-HH:mm:ss, N步, D公尺, 成功/失敗/略過`.
- Buttons: **回主畫面** and **清空歷史** (with confirm) on the same bottom row.

## Run engine (Scheme C lite)

Ported from APBFit v1.2 ideas (single-account only):

1. **Pre-plan** all segments into Room as `PLANNED` at start.
2. **Catch-up** writes batches with `endTime <= now` (throttled: 3 batches/round, 1s gap, ≤20 segments/round).
3. **AlarmManager** exact (or inexact + session WakeLock) for next batch deadline.
4. **SCREEN_ON** receiver triggers catch-up when screen turns on after black screen.
5. **Write WakeLock** around HC inserts.
6. **Orphan resume**: cold start finds `RUNNING` → resume service; if past planned end → catch-up remaining due + finalize.

Exact-alarm permission still recommended (home checkbox「計時」) for tighter schedule while screen is off.
