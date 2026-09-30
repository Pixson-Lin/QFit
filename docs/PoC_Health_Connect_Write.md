# QFit PoC — Health Connect write

| Field | Value |
|---|---|
| Status | Implemented in `:app` |
| Goal | Prove no-login HC write; verify non-overlap, multi-segment, distance/exercise |
| Package | `com.pixsonlin.qfit` |

## What it does

1. On launch, checks Health Connect and requests **write** permissions for steps, distance, and exercise (no Google Sign-In).
2. **Write 1 segment** — one non-overlapping 1-minute window: **188** steps + distance (~131.6 m) + running exercise.
3. **Write short run** — **4** adjacent segments: **752** steps total (+ distance + exercise each).
4. Time windows are **non-overlapping** (cursor in SharedPreferences). Rapid double-taps should **add** (376), not merge.
5. Button opens the **system Health Connect settings** UI.

## Download prebuilt (sideload)

Cursor Cloud **Artifacts** often has **no Download** for `.apk`. Prefer GitHub:

| File | Link |
|---|---|
| ZIP | https://github.com/Pixson-Lin/QFit/raw/cursor/hc-write-feasibility-fcd0/dist/qfit-poc-debug.zip |
| APK | https://github.com/Pixson-Lin/QFit/raw/cursor/hc-write-feasibility-fcd0/dist/qfit-poc-debug.apk |

```bash
adb install -r qfit-poc-debug.apk
```

## Verify on device (owner)

### 1–3 (covered by this PoC build)

| # | Check | How |
|---|---|---|
| 1 | Non-overlapping totals | Tap **Write 1 segment** twice quickly → HC / downstream total should increase by **376**, not ~197 |
| 2 | Multi-segment run | Tap **Write short run** → **4** step records (188 each), total **+752** |
| 3 | Distance + exercise | In HC (and downstream if applicable), confirm distance and exercise/running entries from QFit |

### 4 (manual on phone — not automated in PoC)

| # | Check | How |
|---|---|---|
| 4 | Deny / revoke / re-grant | HC → App permissions → QFit → turn off write → reopen QFit (should re-prompt or show denied) → grant again → write succeeds |

## Build yourself

```bash
export ANDROID_HOME=/opt/android-sdk   # or your SDK path
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Out of scope

- Full APBFit run engine / foreground service
- Guided multi-step onboarding polish
- Play Store listing / HC declaration
- Analytics (see [Install_Usage_Visibility.md](Install_Usage_Visibility.md))
