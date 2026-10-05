# QFit — Health Connect 寫入可行性分析

| Field | Value |
|---|---|
| Document | HC write under no-login / no-email / sideload-or-Play constraints |
| Status | Feasibility confirmed (desk research + APBFit reference) |
| Date | 2026-09-30 |
| Audience | Owner / agents scoping QFit |
| Reference | APBFit `HealthConnectWriter`, `HC_migration.md`, Android Health Connect docs (APBFit = **read-only**; see [AGENTS.md](../AGENTS.md)) |

---

## 1. 問題

QFit 希望功能上對齊 APBFit（把模擬步數／距離／運動寫進裝置），但避開 APBFit 募集試用者時的阻力：

1. App 內 **不要求登入帳號**
2. 使用者 **不必把 email / Google account 交給開發者**
3. **Sideload 或 Play Store** 都能發佈與使用

本文件只回答：**Google Health Connect（HC）能否在上述條件下寫入資料？**

---

## 2. 結論（先看這段）

| 條件 | 可行？ | 說明 |
|---|---|---|
| 1. 不須登入帳號 | **是** | HC 寫入是裝置端權限模型，不走 Google Sign-In / OAuth |
| 2. 不用把 email 給任何人 | **是（就 HC 與 sideload 而言）** | HC 不需要 OAuth test users；sideload 也不需要把 email 給開發者 |
| 3. Sideload 或 Play 都可以 | **是（路徑不同）** | Sideload／本機除錯可直接請求 HC 權限；上架 Play 另需 Health apps 宣告與隱私政策 |

**總結：QFit 可以做成「零 Google Sign-In、只靠 Health Connect 執行期權限」的寫入 App。**  
APBFit 現在仍要求 Sign-In，是產品決策（D2），**不是** HC API 的技術必要條件。

---

## 3. 技術依據

### 3.1 Health Connect 是什麼

- HC 是 **裝置上的健康資料交換層**，不是雲端 Fitness API。
- 寫入流程：宣告 manifest 權限 → 用 `PermissionController` 向使用者要同意 → `HealthConnectClient.insertRecords(...)`。
- 官方／實務共識：HC **不需要** Google 帳號登入才能讀寫；資料留在本機（除非使用者另外把 HC 連到會雲端同步的 App，例如 Google Health）。

### 3.2 APBFit 已驗證的寫入路徑

APBFit 的 `HealthConnectWriter` 已能寫入：

- `StepsRecord`
- `DistanceRecord`
- `ExerciseSessionRecord`（running）
- metadata：`Metadata.manualEntry()`

原始碼註解已寫明：

> Health Connect access is **device-scoped** and does **not** use the Google Fit account for writes.

介面上仍傳入 `GoogleSignInAccount`，只是為了相容舊的 `FitWriter` 介面；**HC insert 本身不使用該帳號**。

APBFit 遷移文件（`HC_migration.md`）也寫：

> Health Connect does **not** use Google Cloud Fitness API.  
> HC writes remain **device-scoped** and do not use the GF account binding.

APBFit 仍保留 Google Sign-In（決策 **D2**）的理由是：下游驗證 App 也可能有登入、以及「先留著再說」——**不是因為 HC 寫入需要帳號**。QFit 可以拿掉這層。

### 3.3 真正需要的使用者動作（取代「登入」）

| 步驟 | 誰授權 | 開發者會拿到什麼 |
|---|---|---|
| 安裝 App（sideload 或 Play） | 使用者 | 無帳號 |
| 授予 HC 寫入權限（steps / distance / exercise） | 使用者在 HC 權限畫面 | 無 email；僅裝置上的 package 權限 |
| （可選）通知／前景服務等系統權限 | Android 執行期權限 | 無帳號 |

沒有「把 Google account 加到開發者的 OAuth test users」這一步。

---

## 4. 對三個條件的逐條判定

### 條件 1 — 不須在 Android 手機登入

| 項目 | 判定 |
|---|---|
| HC `insertRecords` | 不需要登入 |
| Google Sign-In / Credential Manager | **可省略** |
| GCP OAuth client / Fitness scopes | **不需要**（HC 不走 Fitness API） |
| 手機系統本身可能已登入 Google（Play 服務） | 與 QFit App 無關；App 不向使用者要帳號 |

**風險／邊界：** 下游讀步數的第三方 App／遊戲若自己要求登入，那是對方產品，不是 HC 或 QFit 寫入的條件。

### 條件 2 — 使用者不用把 email account 給任何人

拆成兩層：

| 層級 | 需要 email 給開發者？ |
|---|---|
| HC 權限與寫入 | **否** |
| Sideload 安裝（直接給 APK） | **否** |
| Play **Internal testing** 邀請名單 | **通常是**（Play 營運慣例，與 HC 無關） |
| Play Open testing / Production | **否**（使用者從商店安裝即可；不必把 email 私下交給開發者） |

QFit 若要以「不蒐集試用者 email」為招募原則：

- 短期／熟人驗證 → **sideload**
- 對外招募 → 優先 **Open testing 或正式上架**，避免 Internal testing 的 email 名單摩擦  
  （這正是 APBFit 募集階段的痛點來源之一，與 HC 無關）

### 條件 3 — Sideload 或 Play Store 都可以

| 通路 | HC 寫入 | 額外義務 |
|---|---|---|
| Sideload / 本機 debug | **可以** | 官方說明：Play 的 data-type 宣告是上架／公開發行所需，**不阻擋**本機開發與測試整合 |
| Play（含 Internal / Open / Prod） | **可以** | 必須完成：Privacy policy、Data safety、**Health apps / HC data-type 宣告**；宣告用途需與實際寫入的資料型別一致 |

**裝置前提（兩種通路相同）：**

- Android 9+ 且具備 Google Play services／HC 可用性
- Android **14+**：HC 為系統元件（APBFit 目前招募目標亦為此）
- Android 13 以下：可能需另從 Play 安裝 Health Connect App

**Sideload 對「一般大眾」的摩擦：** 開啟「未知來源安裝」對非重度使用者仍可能比 Play 難。可行性上 OK，體驗上建議仍規劃 Play 通路；只是 Play 不該再綁「把 email 給開發者」。

---

## 5. QFit 相對 APBFit 可刪／可留

| APBFit 現況 | QFit 建議 |
|---|---|
| Google Sign-In（email / profile） | **刪除** — 非 HC 必要 |
| OAuth test users ≤100 | **刪除** — 募集摩擦主因 |
| Play Internal testers email 名單 | **避免作為主招募路徑** |
| HC 權限引導 / rationale activity | **保留並強化** — 取代登入成為主 onboarding |
| `HealthConnectWriter` 寫入邏輯 | **可重用概念**（steps / distance / exercise + `manualEntry`） |
| 多帳號／歷史依帳號過濾 | **不需要** |
| 跨裝置雲端同步 | **不可承諾**（HC 僅本機；與 APBFit 已知限制相同） |

---

## 6. 仍須注意的非帳號風險

這些不否定可行性，但會影響「一般大眾」能否順利用起來：

1. **引導式 HC 權限** — 必須比 APBFit 更白話、逐步；權限被拒時要有明確補救路徑（開 HC 設定）。
2. **Play Health 宣告審核** — 上架時可能延遲；sideload 可當備援驗證通路。
3. **隱私政策 URL** — Play／HC 權限說明頁需要可連到的政策（即使 App 不蒐集帳號，仍應說明「本機寫入 HC、不上傳帳號」）。
4. **下游 App** — 若驗證依賴某個要登入的遊戲，招募文案要分開講「QFit 本身不登入」vs「該遊戲自己的帳號」。
5. **前景服務／電池優化** — 長時間 Run 仍可能需要通知權限等；這是系統權限，不是 Google 帳號。

---

## 7. 建議的下一步（可行性之後）

1. **產品決策鎖定：** QFit = 無 Sign-In + HC-only 寫入 + 引導式權限 + 精簡主畫面。
2. **最小驗證（PoC）：** 無登入的空殼 App，只做 HC 權限請求 + 寫一筆 `StepsRecord`，用系統 Health Connect App 目視確認。Sideload 即可。
3. **通路策略：** 驗證用 sideload；對外招募用 Open testing／正式上架，避開 email 名單。
4. **文件：** 另開「一般大眾」版安裝與權限指引（本文件不涵蓋文案）。

---

## 8. 參考來源

- Android Developers — [Get started with Health Connect](https://developer.android.com/health-and-fitness/guides/health-connect/develop/get-started)
- Android Developers — [Write data](https://developer.android.com/health-and-fitness/health-connect/write-data)
- Android Developers — [Publish your health app on Google Play](https://developer.android.com/health-and-fitness/health-connect/publish)（Play 宣告義務；本機測試不阻擋）
- APBFit — `app/.../domain/fit/HealthConnectWriter.kt`（device-scoped；account 參數未用於 HC）
- APBFit — `docs/HC_migration.md` §2 D2、§3（Sign-In 為產品決策；HC 不需 Fitness API）
- APBFit — `docs/APBFit_Health_Connect_Writer.md`（Account model gap）
