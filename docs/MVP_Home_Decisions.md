# QFit MVP — Home / In-progress decisions

| Field | Value |
|---|---|
| Date | 2026-10-01 |
| Status | Locked with owner |

## Product

- No Google Sign-In.
- APBFit = read-only reference ([AGENTS.md](../AGENTS.md)).
- History screen: **next round**.
- This round: Home + In-progress + About + **minimal runnable run**.

## Home

| Item | Decision |
|---|---|
| Intensity | 5 presets; SPM from APBFit: 散步 80, 超慢跑 140, 慢跑 165, 馬拉松 180, 衝刺 210 |
| Duration | 5 min – 4 h; first-run default **20 min** |
| Estimate | `durationMinutes × SPM` (actual run adds segment noise) |
| Env checkboxes | Checked = ready; tap = request permission / open system settings |
| Health Connect checkbox | Manual re-check + jump; **also** auto-request on launch if missing (PoC flow) |
| Menu | About page |
| History button | Placeholder until next round |
| Start | Requires intensity + HC write permissions; starts minimal run → In-progress |

## In-progress

- Rows are **display only** (類型 / 目前步數 / 已進行／總共).
- Cancel → confirm dialog; stop future writes; leave HC data already written.

## HC permission model

**可行：** launch auto-prompt if missing; checkbox reflects readiness and forces request / HC settings when tapped.
