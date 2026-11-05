# Flowstudio · Foxy’s private production desk

A native Android app for one creator. No account, hosting bill, ads, tracking SDKs, broad storage permissions, or embedded API credentials. Android 10+.

## What works in this version

- Offline storyboard editor with an original Foxy example and persistent drafts.
- Optional Gemini text generation using your own key and configurable model. Free allowances depend on Google; no free access is promised. No quota evasion or rotating accounts.
- API vault encrypted with AES-GCM using Android Keystore; device screen-lock confirmation before opening the vault, masked key status, replacement and deletion. No backups or screenshots. A compromised device remains a risk.
- System image picker imports 1–4 JPEG/PNG/WebP scene images without storage permission.
- Offline H.264 MP4 export: 720 × 1280, 24 fps, 20 seconds, equal-duration still scenes, optional short caption. **Silent slideshow, not AI animation.** Images fit without cropping. Add sound/music in an editor or YouTube, with appropriate rights.
- Local storyboard queue, video preview/share/delete, and 90-day production guidance.

## Get the APK on your Android phone

1. Open this repository → **Actions → Android APK**.
2. Open the latest successful run. Under **Artifacts**, download **Flowstudio-test-APK**.
3. Extract the ZIP using Android Files, tap the APK, and follow Android's installation prompts.
4. This is a **debug-signed testing build**. Its signing certificate may change across CI runs; a permanent release key is required for reliable updates without uninstalling.
5. Configure a device PIN/password before opening Vault. You can use the editor and exporter without an API key.

Play Protect may scan or warn about sideloaded APKs. This project does not bypass scanning or require disabling Play Protect. Android compatibility and signing cannot guarantee Play Protect approval.

## Permanent release signing

A permanent signing key is necessary for a production installation. Keep a private offline backup: losing it prevents installing future updates over the existing release.

Generate a key in a trusted Java environment (not committed to Git):

```sh
keytool -genkeypair -keystore flowstudio.jks -alias flowstudio -keyalg RSA -keysize 3072 -validity 10000
```

In repository **Settings → Secrets and variables → Actions**, add:

- `ANDROID_KEYSTORE_BASE64`: base64-encoded keystore, one line.
- `ANDROID_STORE_PASSWORD`: keystore password.
- `ANDROID_KEY_ALIAS`: `flowstudio`.
- `ANDROID_KEY_PASSWORD`: key password.

Run **Actions → Android APK → Run workflow**. With these secrets configured, a **Flowstudio-release** artifact contains a signed APK and an optional AAB. No Play Store account is required for direct APK installation. Never paste keys or signing passwords into issues, source files, or chat. The workflow removes its temporary signing file; logs do not print secret values.

## Daily workflow

1. Draft or generate a story; review its originality and pacing.
2. Create scene images in a service of your choice using the storyboard prompts and your Foxy reference. Image/video generation is **not integrated** in this version.
3. Import the scene images in story order, add a caption of at most 38 characters, and export. Keep the app open while rendering; background/resumable rendering is not implemented.
4. Preview the MP4, then share/save it. Add licensed audio and upload/schedule manually in YouTube Studio.
5. Queue the next story. Track results in YouTube Analytics; posting cadence does not guarantee a minimum view count or monetization.

The queue stores storyboards, not scheduled uploads. No YouTube OAuth or upload integration is included. No cloud processing is required. App data is device-local; uninstalling deletes it. Export important videos first.

## Development and validation

Java 17, Gradle 8.9, Android SDK 35. Build with `gradle assembleDebug lintDebug`. GitHub Actions provisions these tools, builds the APK, runs lint, and runs Android integration checks in an API 35 emulator. The integration runner tests real Keystore encryption/deletion and renders/decodes a 20-second MP4, checking duration, dimensions, and color conversion. Physical-device encoding compatibility must also be tested before relying on daily production.

Only the Google Gemini HTTPS endpoint receives network requests, and only when generation or a connection test is requested. The connection test uses quota. The one-hour local pause after a 429 is a minimum backoff, not a promise of provider quota reset. Keys never go into the GitHub repository or APK source. Scene images are not uploaded by the app.

See [PRIVACY.md](PRIVACY.md) for data handling and [SECURITY.md](SECURITY.md) for the security model.
