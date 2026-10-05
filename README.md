# QFit

Android「電子搖步機」：無 Google Sign-In，將步數寫入 Health Connect。

## Agent rule (mandatory)

**後續動作請視 APBFit 裡面的檔案為唯讀，所有變動都在 QFit 這邊。** See [AGENTS.md](AGENTS.md).

## Current (`0.4.4-duration-stops` / `versionCode` `2026100503`)

- Home: type / duration (**time-linear slider**, stops `1,3,5,10…240`, default 20) / start / env checkboxes / **背景搖步** toggle / history
- **背景搖步** (default on): foreground notification; off = no notification, easier for OS to kill
- In-progress + cancel confirm (no chevrons)
- History: Room-persisted runs, purple card border, expandable 詳細記錄, clear history
- About（最下方顯示 `versionName` + `versionCode`）
- **Scheme C lite**: pre-plan segments, **batchSize=2**, AlarmManager next-deadline + SCREEN_ON catch-up, orphan resume

Decisions: [docs/MVP_Home_Decisions.md](docs/MVP_Home_Decisions.md) · Versioning: [docs/Versioning.md](docs/Versioning.md) · Issues: [docs/Known_Issues.md](docs/Known_Issues.md)

### Sideload

| File | Link |
|---|---|
| ZIP | https://github.com/Pixson-Lin/QFit/raw/cursor/hc-write-feasibility-fcd0/dist/qfit-mvp-debug.zip |
| APK | https://github.com/Pixson-Lin/QFit/raw/cursor/hc-write-feasibility-fcd0/dist/qfit-mvp-debug.apk |

```bash
adb install -r qfit-mvp-debug.apk
./gradlew :app:assembleDebug
```
