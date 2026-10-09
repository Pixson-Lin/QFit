# Prompt for local agent — Play Console FGS declaration video

Copy everything below the line into a local agent that can drive an Android device with **adb + scrcpy** (or equivalent screen record). Goal: produce one short video for Google Play’s **前景服務權限 / FOREGROUND_SERVICE_DATA_SYNC** declaration.

---

## Mission

Record a **30–90 second** phone screen video that proves QFit uses a **foreground service with an ongoing notification** to keep writing simulated steps into **Health Connect on the device** while a user-started run is active (including briefly in the background).

This video will be uploaded as an **unlisted YouTube** or **Google Drive (anyone with the link can view)** URL and pasted into Play Console → App content / version declaration → **影片連結**.

**Do not** invent Play Console steps. **Do not** need network traffic or Google Sign-In. Focus only on the on-device demo.

---

## Preconditions (owner / agent setup)

1. Device or emulator with **Android 12+**, **Health Connect** installed.
2. **QFit** installed (sideload debug APK or Play closed-testing build). Prefer a build the owner already uses for demos.
3. First-launch permissions already granted if possible:
   - Health Connect: allow write **Steps / Distance / Exercise**
   - Notifications: **Allow** (needed so Run notification appears)
   - Battery Optimization checkbox ideally already checked (nice-to-have, not the focus of this video)
4. `adb devices` shows the target device.
5. Tooling available: `scrcpy` **or** `adb shell screenrecord` + `adb pull`.
6. Output folder on the PC, e.g. `~/Videos/qfit-fgs-demo/` or `C:\Users\<you>\Videos\qfit-fgs-demo\`.

---

## What Play must clearly see (acceptance checklist)

The video **must** make these visible without narration (on-screen text/UI is enough):

| # | Must show | Why |
|---|---|---|
| A | QFit **Home** with **搖步提示列 / Run notification** **checked** | Declares the ongoing notification path |
| B | User taps **開始 / Start** after a short duration (1–3 min) | User-initiated work |
| C | **進行中 / In progress** screen with **steps increasing** | Local processing happening |
| D | **Status bar / notification shade** showing the ongoing run notification (e.g. 搖步中 / run in progress) | Foreground service is perceivable |
| E | App **not** necessarily in the foreground the whole time: at least **leave Home / In progress** once (e.g. go to launcher or lock screen ~10–20s) and return with the run **still active** and steps still advancing | Shows FGS work continues when user is not interacting |

Nice-to-have (optional, if time allows):

| # | Optional | Why |
|---|---|---|
| F | Pull down notification shade so the ongoing notice is readable | Stronger evidence |
| G | End with **取消 / Cancel** or natural completion | Clean ending |

**Fail** if: no ongoing notification, steps never increase, or the video only shows static About/settings with no active run.

---

## Recording constraints

- Length: **30–90 seconds** (prefer ~45–60s).
- Orientation: portrait.
- Language: either **zh-Hant or English** UI is fine (match the device locale).
- No need for voiceover; if you add captions, keep them short and factual.
- Do **not** show passwords, Google account emails, or other personal data.
- Prefer **one continuous take**; light cuts OK if unavoidable.
- Resolution: phone native or scrcpy default; avoid tiny cropped windows.

---

## Recommended shot list (follow in order)

1. **Home (3–5s)**  
   - Show app bar title (QFit / QFit Step Simulator).  
   - Confirm **Run notification / 搖步提示列** is checked.  
   - Set **Duration** to **1** or **3** minutes (short demo). Type can stay default (Slow Jogging / 超慢跑).

2. **Start (2s)**  
   - Tap **Start / 開始**.

3. **In progress + notification (10–20s)**  
   - Stay on In progress until **Steps so far** increases at least once.  
   - Pull down the notification shade **or** ensure the status-bar ongoing icon/text is visible.  
   - Hold long enough that a reviewer can read “run in progress / 搖步中” (wording may vary).

4. **Background continuity (15–25s)**  
   - Press Home / go to launcher **or** lock the screen for **10–20 seconds**.  
   - Unlock / return to QFit.  
   - Show In progress again: run still active, steps **higher than before** (or at least still running).

5. **Close (5s)**  
   - Optional: tap **Cancel / 取消** and confirm, **or** leave on In progress.  
   - Stop recording.

---

## How to record (pick one)

### Option A — scrcpy (preferred if available)

```bash
# Example: record to a file while mirroring
scrcpy --record qfit-fgs-data-sync-demo.mp4
# Perform the shot list on the device, then stop scrcpy (Ctrl+C).
```

### Option B — adb screenrecord

```bash
adb shell screenrecord --time-limit 90 /sdcard/qfit-fgs-data-sync-demo.mp4
# Perform the shot list; recording auto-stops at 90s or interrupt with Ctrl+C.
adb pull /sdcard/qfit-fgs-data-sync-demo.mp4 .
adb shell rm /sdcard/qfit-fgs-data-sync-demo.mp4
```

### Option C — emulator UI “Record and play”

Use Android Studio’s screen recorder if adb/scrcpy is awkward; same shot list.

---

## Deliverables for the owner

1. Video file: `qfit-fgs-data-sync-demo.mp4` (or `.webm`).
2. Short note in a sibling `README.txt`:
   - Device model / Android version  
   - App versionName / versionCode if known  
   - Locale (en / zh-Hant)  
   - Confirmation of checklist A–E (yes/no each)
3. **Do not** commit the video into the QFit git repo unless the owner asks.  
4. Owner will upload to YouTube (unlisted) or Drive and paste the URL into Play Console.

---

## Context for the agent (do not put on screen)

- Play form category already chosen by owner: **本機處理 → 其他** (Local processing → Other).  
- Permission: `FOREGROUND_SERVICE_DATA_SYNC` / `foregroundServiceType="dataSync"`.  
- Purpose: keep writing planned Health Connect segments during an active run with an ongoing notification.  
- No Cloud Console / API keys involved.

---

## Done criteria

Stop when you have a single video where a stranger can answer “yes” to:

1. Did the user start a run from QFit?  
2. Is there an ongoing notification while the run is active?  
3. Do steps increase?  
4. Does the run continue after leaving the app / locking the screen briefly?
