# QFit — versioning

| Field | Value |
|---|---|
| Updated | 2026-10-05 |
| Status | Locked with owner |

## Rules

| Field | Format | Role |
|---|---|---|
| `versionName` | `0.MINOR.PATCH[-label]`（例：`0.4.1-icon`） | 給人看的產品版號；標籤可選，描述這次重點 |
| `versionCode` | `YYYYMMDDNN` | Android 安裝／更新用的整數 build number |

### `versionCode` (`YYYYMMDDNN`)

- `YYYYMMDD`：發佈當日日期（**台北時間 `Asia/Taipei`**）
- `NN`：當天流水號，從 `01` 起；同一天每出一個可安裝 APK 就 +1
- 必須嚴格遞增（不可小於已安裝版本），且為 32-bit 整數範圍內
- 定義於 `app/build.gradle.kts` 的 `defaultConfig.versionCode`
- 不要再與「從 1 往上加」的舊式 `versionCode` 混用

### `versionName`

- 維持現有語意版號風格；有意義的功能／UI 變更時遞增 MINOR 或 PATCH，並可加短標籤
- 與 `versionCode` **分工**：名稱給人看，數字給系統判斷新舊

## About 顯示

關於頁最下方須同時顯示：

1. `versionName`
2. `versionCode`

來源：`BuildConfig.VERSION_NAME` / `BuildConfig.VERSION_CODE`（與 APK 一致）。

## Checklist（每次出可安裝包）

1. 依上表更新 `versionCode`（當天下一碼）與（若需要）`versionName`
2. `./gradlew :app:assembleDebug`（或 release）
3. 更新 `dist/`，並在關於頁確認兩個號碼正確
