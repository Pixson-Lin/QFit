# QFit

Android「電子搖步機」：無 Google Sign-In，將步數寫入 Health Connect。

## Agent rule (mandatory)

**後續動作請視 APBFit 裡面的檔案為唯讀，所有變動都在 QFit 這邊。** See [AGENTS.md](AGENTS.md).

## Current MVP (`0.2.0-mvp`)

- Home: type / duration / start + env checkboxes (HC, notification, battery, exact alarm)
- In-progress + cancel confirm
- About
- Minimal foreground run that writes steps/distance/exercise to HC

Decisions: [docs/MVP_Home_Decisions.md](docs/MVP_Home_Decisions.md)

### Sideload

| File | Link |
|---|---|
| ZIP | https://github.com/Pixson-Lin/QFit/raw/cursor/hc-write-feasibility-fcd0/dist/qfit-mvp-debug.zip |
| APK | https://github.com/Pixson-Lin/QFit/raw/cursor/hc-write-feasibility-fcd0/dist/qfit-mvp-debug.apk |

```bash
adb install -r qfit-mvp-debug.apk
```

```bash
./gradlew :app:assembleDebug
```
