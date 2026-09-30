# QFit

Android app concept: APBFit-class Health Connect step writing **without** Google Sign-In or collecting the user’s email.

## Agent rule (mandatory)

**後續動作請視 APBFit 裡面的檔案為唯讀，所有變動都在 QFit 這邊。**

See [AGENTS.md](AGENTS.md). APBFit is reference-only; never modify it from QFit work.

## Status

- Feasibility: [docs/HC_Write_Feasibility.md](docs/HC_Write_Feasibility.md)
- PoC app: no-login write of **188** steps + open system Health Connect — [docs/PoC_Health_Connect_Write.md](docs/PoC_Health_Connect_Write.md)
- Install/usage visibility: [docs/Install_Usage_Visibility.md](docs/Install_Usage_Visibility.md)

## Build PoC

```bash
./gradlew :app:assembleDebug
```

## Reference

[APBFit](https://github.com/Pixson-Lin/APBFit) is a prior project used only as technical reference (read-only for agents).
