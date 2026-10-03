# QFit MVP — product decisions

| Field | Value |
|---|---|
| Updated | 2026-10-03 |
| Status | Locked with owner |

## Product

- No Google Sign-In.
- APBFit = read-only reference ([AGENTS.md](../AGENTS.md)).
- Screens: Home + In-progress + History + About.
- Minimal run writes HC steps/distance/exercise.

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

## In-progress

- Rows are **display only**; **no chevrons**.
- Card uses `secondaryContainer` background.
- Cancel → **confirm dialog**; stop future writes; keep HC data already written.
- Status after end: 已完成 or 已取消.

## History

- Persist runs + segments in **Room** (no sample data; empty state when none).
- Card summary: start time, type, planned vs actual duration, steps, status.
- Card border: `1.5.dp` `primary` purple stroke for clearer separation.
- **詳細記錄** collapsed by default; label is **green + underline**; tap expands segment lines.
- Segment line: `#n HH:mm:ss-HH:mm:ss, N步, D公尺, 成功/失敗`.
- Buttons: **回主畫面** and **清空歷史** (with confirm) on the same bottom row.

## Not implemented yet

- **Wall-clock catch-up / long-run stability**: segments only `delay` until each segment end. After screen-off / process freeze, there is **no** fast-forward to wall clock or burst write of missed segments.
