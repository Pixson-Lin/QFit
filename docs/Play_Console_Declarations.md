# QFit — Play Console declarations (owner checklist)

| Field | Value |
|---|---|
| Updated | 2026-10-09 |
| Privacy policy URL | https://pixson-lin.github.io/QFit/privacy/ |
| packageName | `com.pixsonlin.qfit` |
| HC types (actual code) | **Write** Steps, Distance, Exercise only (no HC read) |

Agent cannot operate Play Console. Everything under **Owner must click** needs you in [Play Console](https://play.google.com/console). Suggested answers below match current QFit behavior and the published privacy policy.

---

## Owner must click (項目 6)

Do these in Play Console for the **QFit** app (not APBFit leftovers):

1. **Create / select the QFit app** (`com.pixsonlin.qfit`) if it does not exist yet.
2. **App content → Privacy policy** → set URL to  
   `https://pixson-lin.github.io/QFit/privacy/`
3. **App content → Data safety** → fill using § Data safety answers below → save / submit.
4. **App content → Health apps** (wording may be “Health Connect” / health data access) → declare write types in § Health apps answers below → save / submit.
5. **Store listing → Privacy policy** → same URL as above (if shown separately).

Optional later (not blocking the form text itself, but needed before closed testing goes live):

6. Complete any remaining **App content** questionnaires still marked incomplete (Ads, Target audience, News, etc.) — for QFit: usually **No ads**, not a news app, not primarily for children.
7. Upload a **signed AAB** (項目 7) before testers can install from Play — see [Release_Signing.md](Release_Signing.md).

---

## Data safety — suggested answers

Google’s definition: **“Collected”** ≈ data transmitted **off** the user’s device.  
QFit has **no developer backend**, **no analytics SDK**, **no Google Sign-In**, and **no `INTERNET` permission** for uploading user data. Health Connect records stay on the device.

### Overview

| Question (paraphrased) | Suggested answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all user data encrypted in transit? | N/A if no collection; if the form still asks and you answered No overall, skip |
| Do you provide a way for users to request deletion? | N/A if no collection; local history can still be cleared in-app / by uninstall (already in privacy policy) |

If Console forces a “yes” path because you touch health APIs, keep disclosures aligned with the privacy policy:

- Data stays **on device** (Health Connect + local Room history).
- **Not** shared with other companies / for advertising.
- Purpose: app functionality (user-initiated simulated activity write).

Prefer the clean **No collection / No sharing** path when the UI allows it for on-device-only apps.

### Explicitly do **not** declare as collected/shared

- Location
- Personal info / name / email / user IDs (no sign-in)
- Financial info
- Photos / video / audio
- Files / docs
- Calendar / contacts
- App activity analytics / web browsing
- Advertising ID

---

## Health apps / Health Connect — suggested answers

Path: **App content → Health apps** (label varies).

### Data types to declare

Declare that QFit accesses Health Connect to **write** only:

| Type | Access | Notes |
|---|---|---|
| Steps | **Write** | `WRITE_STEPS` |
| Distance | **Write** | `WRITE_DISTANCE` |
| Exercise / Exercise sessions | **Write** | `WRITE_EXERCISE` |

Do **not** declare HC **read** for QFit (unlike APBFit’s older read-steps verification path).

### Purpose (short text you can paste)

English:

> QFit lets the user start a local simulated walk/run. The app writes the resulting steps, distance, and exercise session records into Health Connect on the same device so other user-authorized apps can read them. Data is not uploaded to the developer. No Google Sign-In.

繁中（若表單有自由文字欄）：

> QFit 讓使用者在本機啟動模擬步行／跑步，並把產生的步數、距離、運動紀錄寫入同一裝置上的 Health Connect，供使用者授權的其他 App 讀取。資料不上傳給開發者，也不使用 Google 登入。

### Privacy policy URL (same everywhere)

`https://pixson-lin.github.io/QFit/privacy/`

---

## Consistency check (already true in repo)

- Manifest: write Steps / Distance / Exercise only; no HC read permissions.
- Privacy policy live on GitHub Pages (EN + zh-Hant).
- Tagline: no sign-in; on-device permissions; nothing uploaded to the developer.

---

## After you finish 項目 6

Reply with any Console warning/error screenshots or exact question text that does not match this doc, and we adjust answers.  
Next engineering track: **項目 7** signed release keystore + AAB (needs your keystore decisions / secrets).
