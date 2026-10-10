# QFit

Android「電子搖步機」：無 Google Sign-In，將步數寫入 Health Connect。

## Agent rule (mandatory)

**後續動作請視 APBFit 裡面的檔案為唯讀，所有變動都在 QFit 這邊。** See [AGENTS.md](AGENTS.md).

## Current (`0.5.2-coverage` / `versionCode` `2026100902`)

- Home: type / duration (**equal-spaced 26 stops** `1,3,5,10…240`, estimate on title row, default 20) / start / env checkboxes / **搖步提示列** toggle / history
- Segment coverage: last segment up to **+34s** past configured end; **≤5 min** runs use shorter segments; In-progress total follows the plan
- **i18n:** English default + Traditional Chinese (`zh-Hant`); non–Traditional-Chinese system locales use English. Glossary: [docs/I18n_Glossary.md](docs/I18n_Glossary.md)
- Default intensity: **超慢跑 / Slow Jogging**
- **搖步提示列 / Run notification** (default on): foreground notification; off = no notification, easier for OS to kill
- In-progress + cancel confirm (no chevrons)
- History: Room-persisted runs, purple card border, expandable 詳細記錄, clear history
- About（最下方顯示 `versionName` + `versionCode`）
- **Scheme C lite**: pre-plan segments, **batchSize=2**, AlarmManager next-deadline + SCREEN_ON catch-up, orphan resume

Decisions: [docs/MVP_Home_Decisions.md](docs/MVP_Home_Decisions.md) · Versioning: [docs/Versioning.md](docs/Versioning.md) · Issues: [docs/Known_Issues.md](docs/Known_Issues.md) · Play declarations: [docs/Play_Console_Declarations.md](docs/Play_Console_Declarations.md) · Release signing: [docs/Release_Signing.md](docs/Release_Signing.md)

### User guide & privacy (GitHub Pages)

- Guide: https://pixson-lin.github.io/QFit/ · source [docs/](docs/) (`en/` · `zh/`)
- Privacy Policy: https://pixson-lin.github.io/QFit/privacy/ · [en](docs/privacy/en/) · [zh](docs/privacy/zh/)
- Glossary: [docs/I18n_Glossary.md](docs/I18n_Glossary.md)

### Sideload

| File | Link |
|---|---|
| ZIP | https://github.com/Pixson-Lin/QFit/raw/main/dist/qfit-coverage-debug.zip |
| APK | https://github.com/Pixson-Lin/QFit/raw/main/dist/qfit-coverage-debug.apk |

```bash
adb install -r qfit-coverage-debug.apk
./gradlew :app:assembleDebug
# Play closed testing (needs local keystore.properties): ./gradlew :app:bundleRelease
```
