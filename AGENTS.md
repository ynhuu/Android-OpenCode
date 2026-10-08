# Agent notes

## Project

- Android application written in Kotlin; package namespace: `cn.olonet.opencode`.
- Installed application ID: `cn.olonet.opencode`.
- The UI runs in Android System WebView. The web frontend is downloaded at runtime and is not bundled in the APK.
- Main update and archive handling code is in `app/src/main/java/cn/olonet/opencode/FrontendStore.kt` and `LocalContent.kt`; activity/UI code is in `MainActivity.kt`.

## Development rules

- Follow the existing Kotlin and Gradle conventions; keep frontend archive validation and safe staging behavior intact.
- Never commit signing keys, passwords, local SDK configuration, generated APKs, `updates/`, or `scripts/`.
- `apk/`, `updates/`, and `scripts/` are intentionally gitignored. The latter two may exist only in a developer's local checkout.
- Reusable build, frontend packaging, patch generation, and APK verification utilities live in tracked `tools/`. Keep machine-specific scratch scripts in ignored `scripts/`.
- Do not change the fixed frontend update endpoints or update manifest format without an explicit requirement.
- Increment `versionCode` and `versionName` in `app/build.gradle` when preparing an APK release; update the README release information accordingly.
- Keep user-facing installation/build information in `README.md`, agent rules here, and frontend customization details in `docs/frontend/`. Do not edit the frontend patch merely to change documentation.
- Use portable paths and distinguish recorded verification results from checks performed in the current task.

## Frontend releases

- Fixed endpoints are `https://github.com/ynhuu/Android-OpenCode/releases/latest/download/manifest.json` and `https://github.com/ynhuu/Android-OpenCode/releases/latest/download/dist.zip`.
- The manifest `zipUrl` must exactly match the fixed ZIP endpoint. Downloads allow at most five redirects and must stay on HTTPS.
- Frontend version codes are independent of APK version codes; increase the frontend code when publishing new frontend content.
- Every release designated as latest must include both frontend assets, even for an APK-only update. Upload and verify all assets in a draft before publishing a non-prerelease release.
- Publishing or replacing remote assets requires user authorization; never overwrite an existing release implicitly.

## Verification

Requires JDK 17+ and Android SDK 35. Configure `JAVA_HOME` and `ANDROID_HOME` (or local `local.properties`) before building.

```bash
./gradlew :app:assembleRelease :app:lintRelease :app:testReleaseUnitTest
```

The tracked `tools/sync_dist.py` prepares the launcher icon and `tools/verify_apk.py` checks that frontend assets are not bundled. Frontend tools require an explicit dist path or `OPENCODE_DIST`; they are not required for a Gradle build.

Release APKs belong in the ignored `apk/` directory and should be named `OpenCode-<versionName>.apk`.
