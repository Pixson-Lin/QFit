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

## Owner build

Sign and upload AAB locally (same as prior closed-test uploads):

```bash
./gradlew :app:bundleRelease
```

See [Release_Signing.md](Release_Signing.md).
