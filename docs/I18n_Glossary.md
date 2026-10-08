# QFit i18n — decisions & glossary (draft v1)

| Field | Value |
|---|---|
| Updated | 2026-10-08 |
| Status | Awaiting owner sign-off on glossary |

## Locked decisions

1. **Locales:** `zh-TW` + `en` only for v1.
2. **Selection rule:** follow system language; **use `en` for anything that is not Traditional Chinese** (`zh-TW` / `zh-Hant`). (So `zh-CN` → English.)
3. **Persistence:** Room stores **enum keys** (and intensity enum name); UI translates at display time from current locale.
4. **No in-app language picker** in v1.
5. **Scope v1:** App UI only (guide HTML / Play listing later).

## Proper nouns (do not translate)

| Term | Notes |
|---|---|
| QFit | Product name |
| Health Connect | Google product name |
| Google | Brand |
| versionName / versionCode | Keep as-is in About (technical) |

## Intensity types (enum → UI)

| Enum key | zh-TW | en (proposed) | SPM |
|---|---|---|---|
| `STROLL` | 散步 | Walk | 80 |
| `SUPER_SLOW_JOG` | 超慢跑 | Easy jog | 140 |
| `JOG` | 慢跑 | Jog | 165 |
| `MARATHON` | 馬拉松 | Marathon pace | 180 |
| `SPRINT` | 衝刺 | Sprint | 210 |

**Menu line pattern**

| Locale | Pattern | Example |
|---|---|---|
| zh-TW | `%1$s（%2$d步/分）` | 超慢跑（140步/分） |
| en | `%1$s (%2$d spm)` | Easy jog (140 spm) |

> Note: `spm` = steps per minute. Avoid “steps/min” in the compact dropdown if length is tight; full phrase OK in longer copy.

**Why these English names**

- **Easy jog** for 超慢跑 — clearer to international users than “super slow jog”; still distinct from Jog.
- **Marathon pace** — “Marathon” alone can sound like race distance; “pace” signals cadence preset.
- Alternatives if you prefer: `SUPER_SLOW_JOG` → `Zone-2 jog` / `Very easy jog`; `MARATHON` → `Long run`.

## Run status

| Enum key | zh-TW | en (proposed) |
|---|---|---|
| `RUNNING` | 進行中 | In progress |
| `COMPLETED` | 已完成 | Completed |
| `CANCELLED` | 已取消 | Cancelled |

## Segment write status

| Enum key | zh-TW | en (proposed) |
|---|---|---|
| `PLANNED` | 待寫入 | Planned |
| `WRITTEN` | 成功 | Written |
| `FAILED` | 失敗 | Failed |
| `SKIPPED` | 略過 | Skipped |

## Core UI (high-visibility)

| Concept | zh-TW | en (proposed) |
|---|---|---|
| App bar / product line | QFit 電子搖步機 | QFit Step Simulator |
| Type | 類型 | Type |
| Duration | 時長 | Duration |
| Start | 開始 | Start |
| History | 歷史紀錄 | History |
| Background run | 背景搖步 | Background run |
| Battery optimization (checkbox) | 電池最佳化 | Battery |
| Battery guide title | 設定電池用量 | Set battery usage |
| Battery guide step “電池” | 電池 | Battery |
| Battery guide “不受限制” | 不受限制 | Unrestricted |
| Go to settings | 前往設定 | Open settings |
| Cancel (dismiss dialog) | 取消 | Cancel |
| Current steps | 目前步數 | Steps so far |
| Elapsed / total | 已進行/總共 | Elapsed / total |
| Details | 詳細記錄 | Details |
| Back home | 回主畫面 | Home |
| Clear history | 清空歷史 | Clear history |
| About | 關於 | About |
| Author line | 作者：Pixson Lin | Author: Pixson Lin |

**Tagline (marketing / About-aligned)**

| Locale | Text |
|---|---|
| zh-TW | 免登入，只要本機權限，資料不上傳給開發者 |
| en | No sign-in. On-device permissions only. Nothing uploaded to the developer. |

## Estimate / notification patterns

| Key idea | zh-TW | en (proposed) |
|---|---|---|
| Estimate on home | `%1$d 分 · 估計約 %2$s 步` | `%1$d min · ~%2$s steps` |
| Notification body | `已寫入 %1$d 步 · 計畫 %2$d 分` | `%1$d steps written · %2$d min planned` |
| Notification title | QFit 搖步中 | QFit running |

## Battery guide body (en sketch)

Use OEM-common words **Battery** and **Unrestricted**:

```
Next, QFit’s App info page will open.

1. Tap Battery
2. Choose Unrestricted
```

## About / disclaimer (en sketch — needs your review)

**Purpose**

> QFit is a step simulator: on this phone, it writes steps, distance, and exercise to Health Connect for other health or game apps to read. No Google sign-in. Your account is never given to the developer.

**Disclaimer**

> Disclaimer: QFit writes simulated step data for personal testing and entertainment only. Follow the terms of any downstream app or game. The developer does not guarantee third-party rewards, behavior, or account safety. Use at your own risk.

## Implementation notes (after glossary sign-off)

- Default resources: keep `values/` as **zh-TW** (current users), add `values-en/` for English.
- Locale resolution: `zh-Hant*` / `zh-TW` → Chinese resources; else → `en`.
- DB migration: stop writing localized `intensityDisplayName` for display; store `IntensityLevel.name` (or existing key field) and map via strings.
- History rows created before migration may still have Chinese display names stored — display layer should prefer enum key when present, fall back to stored string only if key missing.

## Open for owner

Please confirm or edit:

1. Intensity English names (especially **Easy jog** / **Marathon pace**).
2. Product subtitle **QFit Step Simulator** vs **QFit Pedometer** / **QFit Step Writer**.
3. Checkbox label **Battery** vs **Battery unrestricted** (longer, clearer for first-run).
