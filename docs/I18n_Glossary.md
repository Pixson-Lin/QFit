# QFit i18n — decisions & glossary (v1 signed-off)

| Field | Value |
|---|---|
| Updated | 2026-10-08 |
| Status | Signed-off; implemented in `0.5.0-i18n` |

## Locked decisions

1. **Locales:** English (default `values/`) + Traditional Chinese (`values-b+zh+Hant/`).
2. **Selection rule:** follow system language; **any non–Traditional-Chinese locale uses English** (including `zh-CN`).
3. **Persistence:** Room stores **enum keys** (`intensityName` / status names); UI translates at display time. New runs also store the key in `intensityDisplayName` (legacy rows may still hold old Chinese labels — UI prefers `intensityName`).
4. **No in-app language picker** in v1.
5. **Scope:** App UI + user guide HTML (`docs/en/`, `docs/zh/`; root `docs/index.html` redirects by browser language).

## Proper nouns (do not translate)

| Term | Notes |
|---|---|
| QFit | Product name |
| Health Connect | Google product name |
| Google | Brand |
| versionName / versionCode | Keep as-is in About |

## Intensity types

| Enum key | zh-Hant | en |
|---|---|---|
| `STROLL` | 散步 | Walk |
| `SUPER_SLOW_JOG` | 超慢跑 | Slow Jogging |
| `JOG` | 慢跑 | Jog |
| `MARATHON` | 馬拉松 | Marathon |
| `SPRINT` | 衝刺 | Sprint |

**Menu pattern:** zh `%1$s（%2$d步/分）` · en `%1$s (%2$d spm)`

## Run / segment status

| Key | zh-Hant | en |
|---|---|---|
| `RUNNING` | 進行中 | In progress |
| `COMPLETED` | 已完成 | Completed |
| `CANCELLED` | 已取消 | Cancelled |
| `PLANNED` | 待寫入 | Planned |
| `WRITTEN` | 成功 | Written |
| `FAILED` | 失敗 | Failed |
| `SKIPPED` | 略過 | Skipped |

## Core UI (signed-off)

| Concept | zh-Hant | en |
|---|---|---|
| App bar | QFit 電子搖步機 | QFit Step Simulator |
| Run notification (was 背景搖步) | 搖步提示列 | Run notification |
| Battery checkbox | 電池最佳化 | Battery Optimization |
| Tagline | 免登入，只要本機權限，資料不上傳給開發者 | No sign-in. On-device permissions only. Nothing uploaded to the developer. |
