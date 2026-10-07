# Guide screenshots

Capture instructions for a local adb/scrcpy agent: [../Screenshot_Capture_Prompt.md](../Screenshot_Capture_Prompt.md)

Put PNGs here for `docs/index.html` figure placeholders:

| File | Content |
|---|---|
| `01-home.png` | Home screen |
| `02-health-connect.png` | Health Connect permission |
| `03-background-run.png` | 背景搖步 checked (+ optional notif permission) |
| `04a-battery-dialog.png` | Battery guide dialog |
| `04b-app-info.png` | App info → Battery |
| `04c-unrestricted.png` | Unrestricted / Optimized / Restricted |
| `05-type-duration.png` | Type + duration |
| `06-in-progress.png` | In-progress |
| `07-history.png` | History list |

After adding a file, in `docs/index.html` replace that step’s `.shot-placeholder` with:

```html
<img src="images/01-home.png" alt="…" />
```
