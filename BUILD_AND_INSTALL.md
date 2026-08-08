# Build & Install

The APK is **already built and verified** on this machine — you do not need another
agent to compile it. `./gradlew assembleDebug` completed with `BUILD SUCCESSFUL`
(36 tasks) against the full set of changes.

```
android-app/app/build/outputs/apk/debug/app-debug.apk    (~71 MB)
```

---

## Option A — install the APK that already exists (fastest)

Nothing to build. Just get it onto the phone.

1. On the phone: **Settings → About phone → tap "Build number" 7 times** to unlock
   Developer options, then **Settings → Developer options → USB debugging → ON**.
2. Plug the phone in. Accept the "Allow USB debugging?" prompt on the phone screen.
3. Confirm the device is visible:

   ```bash
   "$ANDROID_HOME/platform-tools/adb" devices
   ```

   You want a line ending in `device`. `unauthorized` means you have not accepted
   the prompt yet; empty means the cable is charge-only or the driver is missing.
4. Install over the existing app, keeping its data:

   ```bash
   "$ANDROID_HOME/platform-tools/adb" install -r \
     android-app/app/build/outputs/apk/debug/app-debug.apk
   ```

   If that fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, the existing app was
   signed with a different key. Uninstall first (**this erases its local data**):

   ```bash
   "$ANDROID_HOME/platform-tools/adb" uninstall com.magazineforge.app
   ```

No cable? Copy the `.apk` to the phone via USB storage, Drive, or Telegram and tap
it. Android will ask you to allow installs from that app.

---

## Option B — download the APK from GitHub Actions

Every push to `main` builds a debug APK and uploads it as an artifact.

**GitHub → Actions → latest "Android CI/CD" run → Artifacts → `magazineforge-debug-apk`**

Retention is 30 days. One caveat: unless the repo secret
`GOOGLE_SERVICES_JSON_BASE64` is set, CI substitutes a **mock Firebase config**.
That APK compiles and the magazine pipeline works, but the Showcase feed and the
Firestore sync in `EditorViewModel` fail at runtime. To fix permanently:

```bash
base64 -w0 android-app/app/google-services.json
```

Paste the output into **Settings → Secrets and variables → Actions → New repository
secret**, named `GOOGLE_SERVICES_JSON_BASE64`.

---

## Option C — rebuild from source

Only needed if you change code. Requirements already satisfied here: JDK 17,
Android SDK at `C:\Users\adnan\AppData\Local\Android\Sdk` (platforms 34 & 35,
build-tools 34.0.0), Gradle 8.4 wrapper, dependency cache warm.

```bash
cd android-app
./gradlew assembleDebug
```

First clean build takes ~5 minutes; later ones are far faster. `local.properties`
already exists and is gitignored:

```properties
sdk.dir=C\:\\Users\\adnan\\AppData\\Local\\Android\\Sdk
HF_TOKEN=
```

`HF_TOKEN` is deliberately blank so a build succeeds without the secret. The app
falls back to credentials entered in Settings. **After deploying the Cloudflare
Worker, this becomes the app key instead** — see `cloudflare-worker/README.md`.

`app/google-services.json` is present locally and gitignored. It is your real
Firebase config for project `magazineforge-14d44`, package `com.magazineforge.app`.

### Release build

```bash
./gradlew assembleRelease   # -> app/build/outputs/apk/release/
```

Release enables R8 minification and resource shrinking; debug intentionally does
not, so debug stack traces stay readable.

---

## Release signing — only if you publish to Play

Sideloading needs none of this. The debug APK in Option A is signed with the local
debug key, which Android accepts for direct install, and CI only ever runs
`assembleDebug`. Read on only if you intend to upload to the Play Console.

### Where signing stands today

`app/build.gradle.kts` declares a real `release` signing config, and the keystore it
points at **is committed to this repo**:

```
android-app/app/release.keystore     alias "magboy", store and key password "android"
```

`assembleRelease` therefore produces a signed APK for anyone who clones. But that
private key and its password are both in git history (commit `c78ca4d`), so the key
is permanently compromised and must not be the one you publish under. It is fine as
a throwaway for local release testing. `.gitignore` now carries `*.keystore` and
`*.jks`, so a new keystore cannot be committed the same way — but that does not
untrack the existing one, which still needs the `git rm --cached` in step 4.

Migrating is **your call**, not something already wired up. Nothing below is applied
to the live build; it is the opt-in path, written out so you can decide.

### 1. Generate an upload keystore

```bash
keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -alias YOUR_ALIAS \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storetype PKCS12
```

It prompts for the store password, then the certificate fields (name, org,
locality). Use the same value for the key password when asked, or track both
separately. Keep this file **outside the repo** — put it somewhere backed up, like a
password manager attachment. Never `git add` it.

### 2. Move it into GitHub secrets

```bash
base64 -w0 upload-keystore.jks
```

Add four repository secrets under **Settings → Secrets and variables → Actions**,
matching the naming already used by `HF_TOKEN`, `APP_KEY` and
`GOOGLE_SERVICES_JSON_BASE64`:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | the output of the command above |
| `KEYSTORE_PASSWORD` | the store password from step 1 |
| `KEY_ALIAS` | `YOUR_ALIAS` |
| `KEY_PASSWORD` | the key password from step 1 |

### 3. Decode it in CI

A step in `.github/workflows/android-ci.yml`, before the build, alongside the
existing `Decode google-services.json`:

```yaml
      - name: Decode upload keystore
        # Skipped silently when the secret is unset, so forks and fresh clones
        # still build. The Gradle block below then falls through to unsigned.
        if: ${{ secrets.KEYSTORE_BASE64 != '' }}
        run: |
          echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > android-app/app/upload-keystore.jks
          cat >> android-app/local.properties <<EOF
          KEYSTORE_FILE=upload-keystore.jks
          KEYSTORE_PASSWORD=${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS=${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD=${{ secrets.KEY_PASSWORD }}
          EOF
```

Appending to `local.properties` reuses the mechanism the build already reads secrets
through, so Gradle needs no new plumbing and the values never appear in a build
argument that would show up in process listings.

### 4. Replace the signing config in `build.gradle.kts`

The current block hardcodes the committed keystore. This version reads the four
values from `local.properties` and returns `null` when they are absent:

```kotlin
    signingConfigs {
        create("release") {
            val storeFileName = localProperties.getProperty("KEYSTORE_FILE") ?: ""
            if (storeFileName.isNotEmpty() && file(storeFileName).exists()) {
                storeFile = file(storeFileName)
                storePassword = localProperties.getProperty("KEYSTORE_PASSWORD") ?: ""
                keyAlias = localProperties.getProperty("KEY_ALIAS") ?: ""
                keyPassword = localProperties.getProperty("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            // ... isMinifyEnabled / proguardFiles unchanged ...
            signingConfig = signingConfigs.getByName("release")
                .takeIf { it.storeFile?.exists() == true }
        }
    }
```

`localProperties` is already loaded at the top of `defaultConfig`; hoist that block
above `signingConfigs` so both can use it. The `takeIf` is the load-bearing part —
with no keystore, `signingConfig` is `null` and `assembleRelease` emits an
**unsigned** APK instead of failing. That mirrors the `?: ""` fallbacks for
`HF_TOKEN` and `APP_KEY`: someone without your secrets still gets a green build,
just not a shippable artifact.

Then `git rm --cached android-app/app/release.keystore`, delete the file, and add to
`.gitignore`:

```gitignore
*.jks
*.keystore
```

Removing it from the index does not remove it from history — the compromised key
stays retrievable in commit `c78ca4d` forever. That is exactly why the replacement
must be a newly generated one.

### If you lose the keystore

You cannot ship another update to the same Play listing. Play identifies an app by
its signing key, and an APK signed with a different key is rejected as a different
app — you would have to publish under a new package name and lose your install base,
reviews and ratings. Enrolling in **Play App Signing** at first upload softens this:
Google holds the app signing key and your upload key becomes replaceable via a
support request. Do that, and still back the keystore up.

---

## What to check on the device

- **Splash** — the folded-ribbon brand mark draws in, tints fill, the wordmark's
  letter-spacing opens. ~1.4s total (was 2.2s). Should feel continuous from the
  system splash, with no colour seam.
- **Themes** — Settings → five themes: Paper & Ink (default), Obsidian & Gold,
  Cool Slate, Press Mono, Jewel Aubergine. Switch each and visit **My Magazines**,
  **Showcase**, and the **LaTeX notebook**: those three previously ignored theming
  entirely. The editor must stay dark and legible even under the light themes.
- **My Magazines** — card titles and dates sit *below* the cover art. They were
  previously painted on top of it.
- **Cancel** — start a Full AI run, expand the progress tracker, cancel it. It
  should confirm first, then stop; the run must not resurrect on the next poll.
- **Continue Editing** (Home) — shows a real title and real progress counts, or is
  hidden entirely. It must never show "Summer Travel Guide".
- **PDF viewer** — pinch to zoom, pan while zoomed, double-tap to reset. Scrolling
  a long magazine should not OOM.
- **Full AI magazine** — back cover must be full-bleed, carry a tagline, and use a
  *different* photo from the front cover. Articles should read as one publication:
  distinct bylines, no repeated openings.

Report back any crash with the full logcat:

```bash
"$ANDROID_HOME/platform-tools/adb" logcat -d > crash.txt
```
