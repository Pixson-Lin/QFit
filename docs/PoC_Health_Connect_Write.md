# QFit PoC — Health Connect write (188 steps)

| Field | Value |
|---|---|
| Status | Implemented in `:app` |
| Goal | Prove no-login HC write on device |
| Package | `com.pixsonlin.qfit` |

## What it does

1. On launch, checks Health Connect availability.
2. Requests **write steps** permission (no Google Sign-In).
3. Writes **one** `StepsRecord` with **188** steps (`Metadata.manualEntry()`).
4. Shows a button that opens the **system Health Connect settings** UI.

## Download prebuilt (sideload)

Cursor Cloud **Artifacts** can show the APK but often has **no Download** (preview fails for `.apk`). Prefer GitHub:

| File | Link |
|---|---|
| ZIP (~9 MB) | https://github.com/Pixson-Lin/QFit/raw/cursor/hc-write-feasibility-fcd0/dist/qfit-poc-debug.zip |
| APK (~28 MB) | https://github.com/Pixson-Lin/QFit/raw/cursor/hc-write-feasibility-fcd0/dist/qfit-poc-debug.apk |

On the PR **Files** tab, open `dist/` and use GitHub’s download control.

## Build / install yourself

```bash
export ANDROID_HOME=/opt/android-sdk   # or your SDK path
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Requires a physical device or emulator with Health Connect (Android 14+ recommended).

## Verify

1. Open **QFit PoC**.
2. Allow write access when prompted.
3. Confirm status: “Wrote 188 steps to Health Connect.”
4. Tap **Open Health Connect** and look for the new steps entry from QFit.

## Out of scope for this PoC

- Distance / exercise records
- Guided multi-step onboarding UX polish
- Play Store listing / HC declaration
- Analytics or install counting (see [Install_Usage_Visibility.md](Install_Usage_Visibility.md))
