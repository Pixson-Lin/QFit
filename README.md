# QFit

Android「電子搖步機」：無 Google Sign-In，將步數寫入 Health Connect。

## Agent rule (mandatory)

**後續動作請視 APBFit 裡面的檔案為唯讀，所有變動都在 QFit 這邊。** See [AGENTS.md](AGENTS.md).

## Current (`0.5.0-i18n` / `versionCode` `2026100801`)

- Home: type / duration (**equal-spaced 26 stops** `1,3,5,10…240`, estimate on title row, default 20) / start / env checkboxes / **搖步提示列** toggle / history
- **i18n:** English default + Traditional Chinese (`zh-Hant`); non–Traditional-Chinese system locales use English. Glossary: [docs/I18n_Glossary.md](docs/I18n_Glossary.md)
- Default intensity: **超慢跑 / Slow Jogging**
- **搖步提示列 / Run notification** (default on): foreground notification; off = no notification, easier for OS to kill
- In-progress + cancel confirm (no chevrons)
- History: Room-persisted runs, purple card border, expandable 詳細記錄, clear history
- About（最下方顯示 `versionName` + `versionCode`）
- **Scheme C lite**: pre-plan segments, **batchSize=2**, AlarmManager next-deadline + SCREEN_ON catch-up, orphan resume

Decisions: [docs/MVP_Home_Decisions.md](docs/MVP_Home_Decisions.md) · Versioning: [docs/Versioning.md](docs/Versioning.md) · Issues: [docs/Known_Issues.md](docs/Known_Issues.md)

### User guide (GitHub Pages)

- Entry (auto zh-Hant → `zh/`, else `en/`): [docs/index.html](docs/index.html)
- English: [docs/en/](docs/en/) · 繁中: [docs/zh/](docs/zh/)
- Glossary (shared with App): [docs/I18n_Glossary.md](docs/I18n_Glossary.md)

啟用 Pages：Settings → Pages → Deploy from branch → folder `/docs`。

### Sideload

| File | Link |
|---|---|
| ZIP | https://github.com/Pixson-Lin/QFit/raw/main/dist/qfit-mvp-debug.zip |
| APK | https://github.com/Pixson-Lin/QFit/raw/main/dist/qfit-mvp-debug.apk |

```bash
adb install -r qfit-mvp-debug.apk
./gradlew :app:assembleDebug
```
