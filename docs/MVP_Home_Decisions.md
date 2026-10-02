# QFit MVP — product decisions

| Field | Value |
|---|---|
| Updated | 2026-10-02 |
| Status | Locked with owner |

## Product

- No Google Sign-In.
- APBFit = read-only reference ([AGENTS.md](../AGENTS.md)).
- Screens: Home + In-progress + History + About.
- Minimal foreground run writes HC steps/distance/exercise.

## Home

| Item | Decision |
|---|---|
| Intensity | 5 presets; SPM: 散步 80, 超慢跑 140, 慢跑 165, 馬拉松 180, 衝刺 210 |
| Duration | 5 min – 4 h, **step 5 min**; first-run default **20 min** |
| Estimate | `durationMinutes × SPM` (actual run adds segment noise) |
| Env checkboxes | Checked = ready; tap = request permission / open system settings |
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
- **詳細記錄** collapsed by default; label is **green + underline**; tap expands segment lines.
- Segment line: `#n HH:mm:ss-HH:mm:ss, N步, D公尺, 成功/失敗`.
- Buttons: **回主畫面** and **清空歷史** (with confirm) on the same bottom row.
