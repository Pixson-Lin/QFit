# QFit — release signing & first Play upload (項目 7)

| Field | Value |
|---|---|
| Updated | 2026-10-09 |
| Choice | **Play App Signing** (Google holds the **app signing key**; you hold the **upload key**) |
| packageName | `com.pixsonlin.qfit` |

Agent / CI must **never** receive your keystore passwords or `.jks` bytes. Generate and keep them only on your machine (and a private backup you control).

---

## What you keep vs what Play keeps

| Key | Who holds it | Used for |
|---|---|---|
| **App signing key** | **Google Play** (recommended) | Signing what users install from Play |
| **Upload key** | **You** (local `.jks` + `keystore.properties`) | Signing the AAB you upload to Console |

First closed-testing upload: enable Play App Signing in Console (usually prompted), upload an AAB signed with your upload key.

---

## Owner steps (local machine)

### 1. Create upload keystore (once)

From the QFit repo root (PowerShell / bash):

```bash
mkdir -p keystore
keytool -genkeypair -v \
  -keystore keystore/qfit-upload.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias qfit_upload
```

Remember the store password, key password, and alias (`qfit_upload` if you used the command above).

### 2. Create `keystore.properties` (gitignored)

```bash
cp keystore.properties.example keystore.properties
# edit storePassword / keyPassword (and paths/alias if different)
```

Example contents:

```properties
storeFile=keystore/qfit-upload.jks
storePassword=...
keyAlias=qfit_upload
keyPassword=...
```

### 3. Build release AAB

Bump `versionCode` / `versionName` in `app/build.gradle.kts` if needed (see [Versioning.md](Versioning.md)), then:

```bash
./gradlew :app:bundleRelease
```

Output (typical):

`app/build/outputs/bundle/release/app-release.aab`

Confirm it used the upload key (when `keystore.properties` exists). Without that file, Gradle falls back to the **debug** keystore — **do not upload a debug-signed AAB** to Play for closed testing.

### 4. Upload in Play Console

1. **Testing → Closed testing** → create release  
2. Upload `app-release.aab`  
3. When asked, choose **Play App Signing** / let Google manage the app signing key  
4. Complete release notes → review → roll out to the closed test track  
5. After the release is available, copy the **opt-in link** and recruit testers (12 × 14 days)

---

## Gradle wiring (already in repo)

- `app/build.gradle.kts` reads root `keystore.properties` and sets `signingConfigs.release` when present.
- `.gitignore` excludes `keystore.properties`, `keystore/`, `*.jks`, `*.keystore`.
- Template: `keystore.properties.example`.

---

## Backup (important)

Back up `qfit-upload.jks` + passwords offline. If you lose the upload key, you must go through Play’s upload-key reset process (possible with Play App Signing, but annoying). Losing a self-managed **app** signing key (if you had not used Play App Signing) can be unrecoverable for updates.
