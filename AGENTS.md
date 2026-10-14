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

## Upstream OpenCode frontend rules

- Before editing or reviewing frontend source, tests, or the complete patch, read the target OpenCode version's root `AGENTS.md` and every applicable nested `AGENTS.md`, especially `packages/app/`, `packages/app/e2e/`, and `packages/gui-extensions/`. This Android repository's rules do not replace upstream coding rules; reread them when migrating versions rather than relying on a copied summary.
- Prioritize stability, then simplicity, then performance. Preserve existing ownership, interfaces, and lifecycle handling; prefer the smallest localized change. Do not introduce cross-component callback chains, extra state, or abstractions merely to make a test pass. Explain concrete failures and complexity costs before expanding the requested scope.
- Follow upstream formatting, naming, typing, localization, Solid reactivity, extension boundaries, and cleanup rules. Avoid unrelated formatting churn; retain spacing required by upstream lint and unified-diff context markers required for patch application.
- Before changing session or timeline code, record a production benchmark baseline and compare it afterward. Keep measurements and logs in ignored local storage, not in the migration guide; do not infer a performance guarantee from a single run.
- Follow upstream E2E required reading and test ownership. Extend existing keeper suites, demonstrate that regression tests fail before the fix, use real input and observable readiness, and use controlled clocks for timer behavior. Do not use fixed sleeps, frame counts, weaker assertions, or retries to hide failures.
- Run the target version's canonical checks and relevant package checks. Every patched TypeScript file must pass upstream formatting and lint, including warnings. Report missing dependencies and unrelated upstream failures explicitly; do not describe focused checks as full-repository verification.
- Regenerate the complete patch from the customized source against the official archive, with file sections ordered by path. Verify application on freshly extracted source and byte-for-byte agreement with the customized files, then update the guide's checksum. Do not hand-edit patch hunks to hide source changes or remove required context.

## Frontend guide maintenance

- Keep `docs/frontend/前端修改与迁移指南.md` focused on the current patch baseline and checksum, customized behavior, source locations, migration constraints, build commands, and acceptance checklist.
- Do not turn the guide into a changelog or deployment log. Omit historical user feedback, troubleshooting narratives, machine-specific paths, archive sizes, deployment resource counts, and repeated verification details. Keep release status in `README.md` and local deployment records in ignored `updates/`.
- Do not record dated verification runs, pass counts, build logs, or release/package status in the guide. Keep only patch-related acceptance requirements and brief known limitations, preserving unresolved failures and pending real-device checks; never imply that patch application or a successful build means all regression tests passed.
- Describe platform scope from the implementation, not the device on which a problem was first reported. Shared mobile/touch behavior must not be labeled iOS-only or Android-only; retain explicit platform limits where the code actually has them.
- When frontend code changes, update the relevant behavior and migration constraints. Documentation-only edits must not change the patch or its checksum; baseline and checksum updates must reflect the actual patch.

## Frontend releases

- Fixed endpoints are `https://github.com/ynhuu/Android-OpenCode/releases/latest/download/manifest.json` and `https://github.com/ynhuu/Android-OpenCode/releases/latest/download/dist.zip`.
- The manifest `zipUrl` must exactly match the fixed ZIP endpoint. Downloads allow at most five redirects and must stay on HTTPS.
- Frontend version codes are independent of APK version codes; increase the frontend code when publishing new frontend content.
- Base the next frontend release on the latest published frontend version and increment the version and code by one. Do not increment either for unpublished builds or repeated website test deployments; reuse the same candidate version until it is actually released.
- Every release designated as latest must include both frontend assets, even for an APK-only update. Upload and verify all assets in a draft before publishing a non-prerelease release.
- Publishing or replacing remote assets requires user authorization; never overwrite an existing release implicitly.

## Verification

Requires JDK 17+ and Android SDK 35. Configure `JAVA_HOME` and `ANDROID_HOME` (or local `local.properties`) before building.

```bash
./gradlew :app:assembleRelease :app:lintRelease :app:testReleaseUnitTest
```

The tracked `tools/sync_dist.py` prepares the launcher icon and `tools/verify_apk.py` checks that frontend assets are not bundled. Frontend tools require an explicit dist path or `OPENCODE_DIST`; they are not required for a Gradle build.

Release APKs belong in the ignored `apk/` directory and should be named `OpenCode-<versionName>.apk`.
