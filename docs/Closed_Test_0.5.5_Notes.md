# Closed test — `0.5.5-hc-session` release notes

| Field | Value |
|---|---|
| versionName | `0.5.5-hc-session` |
| versionCode | `2026102801` |
| Track | Closed testing |
| Date | 2026-10-28 (Asia/Taipei) |

## Before you update

**Please finish or cancel any in-progress run**, then install this build. Updating mid-run can leave an incomplete Health Connect activity history for that session.

請先**結束或取消進行中的跑步**再更新。若更新時還有進行中的場次，該場的 Health Connect 活動／距離紀錄可能不完整。

## What changed

### EN

- While a run is active, QFit now writes **steps only** to Health Connect (no longer one exercise + distance every ~30s).
- When the run **ends** (complete or cancel), if **Simulate activity & distance** is checked (default on), QFit writes **one** distance record and **one** exercise session for the whole written span.
- Exercise type: **Walk** → Walking; all other intensities → Running.
- New Home checkbox (2×2 layout): **Simulate activity & distance**. Unchecked = steps only for that run.
- Unchecking does **not** restore the old per-segment triple write.

### 繁中

- 進行中改為只寫入 Health Connect **步數**（不再每約 30 秒寫一筆活動＋距離）。
- 跑步**結束**時（完成或取消），若有勾選 **模擬活動與距離**（預設開啟），會為整段已寫入區間寫入 **一筆距離** 與 **一筆活動**。
- 活動類型：散步 → 健走／Walking；其餘強度 → 跑步／Running。
- 主畫面新增第四選項（2×2）：**模擬活動與距離**。取消勾選＝該場只寫步數。
- 取消勾選**不會**退回舊的「每小段三寫」。

## Play Console — paste into release notes (both locales)

Play “版本資訊 / Release notes” language tags (same pattern as `0.5.4-stuckfix`):

```
<en-US>
• Please finish or cancel any in-progress run before updating
• During a run, QFit now writes steps only to Health Connect (no longer one exercise + distance every ~30s)
• When the run ends, if “Simulate activity & distance” is on (default), writes one distance + one exercise for the whole session
• Walk → Walking; other intensities → Running
• New Home checkbox (2×2): Simulate activity & distance (off = steps only)
</en-US>
<zh-TW>
• 更新前請先結束或取消進行中的跑步
• 進行中改為只寫入 Health Connect 步數（不再每約 30 秒寫一筆活動＋距離）
• 結束時若有勾選「模擬活動與距離」（預設開），會為整場寫入一筆距離與一筆活動
• 散步 → 健走；其餘強度 → 跑步
• 主畫面新增選項（2×2）：模擬活動與距離（取消勾選＝只寫步數）
</zh-TW>
```

Shorter (if the Console field is tight):

```
<en-US>
Finish any in-progress run before updating. Steps only mid-run; one distance + one exercise at end when “Simulate activity & distance” is on (default). Walk→Walking; others→Running.
</en-US>
<zh-TW>
更新前請先結束進行中的跑步。進行中只寫步數；結束時若勾選「模擬活動與距離」（預設開）才寫一筆距離與活動。散步→健走；其餘→跑步。
</zh-TW>
```

## Surfaces to update (UI + write behavior)

| # | Surface | Needed? | Notes |
|---|---|---|---|
| 1 | **Google Group** (`qfit-testers`) | **Yes** | Notify opted-in testers; draft below |
| 2 | **Play Store listing** (short/full description) | **Yes** | Owner: mention new checkbox + end-of-run activity |
| 3 | **User guide** (`docs/en/`, `docs/zh/`) | **Yes** | Still says “three checkboxes”; need 4th option + Steps mid-run / activity at end |
| 4 | **Guide screenshots** (`docs/images/01-home*.png`) | **Yes** | Home is now 2×2 with 4 checks |
| 5 | **Play Console store screenshots** (if Home shot uploaded) | **If used** | Same as guide Home shot |
| 6 | Privacy policy | No | Permissions unchanged |
| 7 | Play Health / FGS declarations | No | Same write perms / FGS |
| 8 | Recruit posts (FB / Reddit / etc.) | Optional | Only if still recruiting with old “writes exercise every segment” wording |

## Google Group — English draft

**Subject:** QFit closed test update — please update (new Home option + Health Connect write change)

```
Hi everyone,

A new closed-testing build of QFit is rolling out on Play (0.5.5-hc-session).

Please update when Play offers it. Before you update, finish or cancel any run that is still in progress.

What’s new
• Home now has a fourth checkbox: “Simulate activity & distance” (on by default), in a 2×2 layout with Health Connect / Run notification / Battery Optimization.
• During a run, QFit writes steps only to Health Connect.
• When the run ends (complete or cancel), if that checkbox is on, QFit writes one distance record and one exercise session for the whole session—not one every ~30 seconds like before.
• Walk → Walking in Health Connect; other intensities → Running.
• If you turn the new checkbox off, that run writes steps only (no activity/distance).

Why this changed
Some apps that sync from Health Connect struggled when a single run created dozens of short exercise sessions. Writing one activity at the end is closer to how real workouts are recorded and should be gentler on those apps.

What you can try
1. Update from Play.
2. Run ~3 minutes with the new checkbox on → confirm many steps appear during the run, then one activity + distance when it ends.
3. Optional: turn the checkbox off for a short run → steps only, no activity/distance at the end.

Thanks again for helping with closed testing. If anything looks wrong on your device (OEM / Android version welcome), reply to this thread.

— Pixson / QFit
```

## Owner build

Sign and upload AAB locally (same as prior closed-test uploads):

```bash
./gradlew :app:bundleRelease
```

See [Release_Signing.md](Release_Signing.md).
