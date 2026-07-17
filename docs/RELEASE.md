# Release guide

How to ship Mileway to Play (gms), the App Store / TestFlight (iOS), and F-Droid (noGms). Every
credential below is an **env-activated placeholder**. The build degrades to a no-op when a key is absent, so
nothing here is required to build or run the demo.

## 1. Versioning (three-tier, computed)

Three repo-root files are the source of truth: `VERSION` + `BUILD_NUMBER` (legacy semver +
monotonic counter, kept for continuity, no longer drive Gradle) and `MILESTONE` (an integer —
bump this to cut a release). Every build derives three computed values from `MILESTONE` + the
live `git rev-list --count HEAD` + today's date — never hand-typed, never written to a file
(`gradle/versioning.gradle.kts`, applied by `:app`, `:wear`, `:server`, `:desktopApp`):

| Value | Format | Used for |
|---|---|---|
| **FINGERPRINT** | `YYYY.0M.0W.<MILESTONE>.<commitCount>` (e.g. `2026.07.29.36.724`) | git tag (`v<FINGERPRINT>`), GitHub release title, `BuildConfig.FINGERPRINT` (Android)/`MilewayFingerprint` (iOS Info.plist)/`/version` (server), debug `versionNameSuffix` |
| **MARKETING** | `YYYY.M.<MILESTONE>` (e.g. `2026.7.36`), ≤3 integer components (iOS `CFBundleShortVersionString` hard limit) | Android release `versionName`, iOS `CFBundleShortVersionString` |
| **desktopPackageVersion** | `<MILESTONE>.0.<commitCount>` (e.g. `36.0.724`) | `:desktopApp` native installer `packageVersion` only — Compose Desktop rejects MARKETING (MAJOR=year>255) at configure time |
| **BUILDCODE** | `VERSION_CODE_BASE(1) + commitCount` (monotonic, < 2.1e9) | Android `versionCode`, iOS `CFBundleVersion` |

```bash
scripts/bump_version.sh --milestone   # MILESTONE += 1 — the actual release-cut step
scripts/bump_version.sh --commit      # print the current computed FINGERPRINT/MARKETING/BUILDCODE, writes nothing
scripts/bump_version.sh --patch       # legacy VERSION/BUILD_NUMBER bump (kept for continuity only)
```

**Cutting a release:** bump `MILESTONE`, commit it, tag `v<FINGERPRINT>` (use `./gradlew -q
:app:printFingerprint` to get the exact value for the commit you're tagging), push the tag. The
[`github-release.yml`](../.github/workflows/github-release.yml) workflow verifies the tag matches
the FINGERPRINT computed at that commit (refuses to publish otherwise), builds every flavor +
build type, and attaches the debug/release APKs (+ an unsigned iOS archive, gated on `iosApp/`
existing) to a GitHub Release.

## 2. Android: Play Store (fastlane, gms flavor)

| Lane | What it does |
|------|--------------|
| `fastlane android deploy_firebase`   | `assembleGmsStaging` → Firebase App Distribution ("testers") |
| `fastlane android deploy_internal`   | `bundleGmsRelease` → Play **internal** |
| `fastlane android deploy_beta`       | → Play **beta** (closed testing) |
| `fastlane android deploy_production` | → Play **production**, staged `ROLLOUT` (default 0.1), git-tags `v<VERSION>` |
| `fastlane android promote from:beta to:production` | promote an existing build between tracks |

Each `deploy_*` regenerates `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` from git commits
(bullet-aware, truncated to Play's 500-char limit).

## 3. iOS: TestFlight / App Store (fastlane)

| Lane | What it does |
|------|--------------|
| `fastlane ios beta`        | `match` → `build_app` (app-store) → TestFlight (external + beta review) |
| `fastlane ios release`     | → App Store `deliver` (submit + automatic + phased release) |
| `fastlane ios renew_certs` | regenerate signing certs/profiles via `match` |

iOS push needs the Firebase SPM package added once. See [docs/ios-push-setup.md](ios-push-setup.md).

## 4. F-Droid (noGms / FOSS)

The `noGms` flavor is the FOSS build: MapLibre maps, **no** Google/Firebase runtime. Enforced by the
`verifyNoGmsDependencyPrefixes` Gradle task (fails `check` if a proprietary dep leaks in).

1. Tag the release `v<VERSION>-fdroid`.
2. Run the **Publish F-Droid APK** workflow (`.github/workflows/publish-fdroid.yml`) with that tag, and it builds
   `assembleNoGmsRelease -Pfdroid` (reproducible, no R8), signs with the pinned `apksigner`, and attaches
   `Mileway-<tag>.apk` + `.sha256` to a GitHub Release.
3. Submit `metadata/com.mileway.fdroid.yml` to `fdroiddata` (Binaries approach; the prebuild strips the
   Firebase Gradle plugins and removes `google-services.json`).

## 5. Orchestrated release (`.github/workflows/release.yml`)

`workflow_dispatch` with rung inputs. Lower rungs are protected by GitHub **Environments** (required reviewers):

- `android_rung`: `skip | firebase | internal | beta | production`  (env: `play-internal` / `play-beta` / `play-production`)
- `ios_rung`: `skip | testflight | appstore`  (env: `testflight` / `app-store`)
- `fdroid`: build the reproducible noGms APK
- `production_rollout`: Play staged-rollout fraction (default `0.10`)

A `wear` job builds `:wear` (a library today; see the inline note for the standalone-Wear-app `wear:alpha`
upload + `versionCode = phone + 1`).

## 6. Secrets / env placeholders

Materialized from base64 env in CI (the `Materialize … secrets` steps); never committed real.

| Secret | Used by | Materializes to |
|--------|---------|-----------------|
| `GOOGLE_SERVICES_B64` | FCM / Analytics / Crashlytics (gms) | `app/google-services.json` |
| `GOOGLE_SERVICES_IOS_B64` | iOS Firebase / APNs | `iosApp/iosApp/GoogleService-Info.plist` |
| `PLAYSTORE_CREDS_B64` | Play upload (service account) | `secrets/play-service-account.json` (`PLAY_STORE_KEY_FILE_PATH`) |
| `KEYSTORE_B64` + `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` | release signing + F-Droid `apksigner` | `secrets/upload.keystore` (+ `RELEASE_*` env) |
| `APPSTORE_AUTH_KEY_B64` + `ASC_KEY_ID` / `ASC_ISSUER_ID` | iOS TestFlight / App Store (ASC API key) | `secrets/AuthKey.p8` |
| `MATCH_GIT_PRIVATE_KEY` + `MATCH_PASSWORD` | iOS signing (`match`) | match certs repo access |
| `FIREBASE_APP_ID` | Firebase App Distribution | env |
| `CRASHLYTICS_UPLOAD=true` | enable Crashlytics mapping upload on gms release | env (off by default) |

`KEYSTORE_B64`/`KEYSTORE_PASSWORD`/`KEY_ALIAS`/`KEY_PASSWORD` is the **canonical** signing-secret
naming — every deploy workflow (`amazon-appstore-deploy.yml`, `aptoide-deploy.yml`,
`huawei-appgallery-deploy.yml`, `indus-deploy.yml`, `samsung-galaxy-store-deploy.yml`,
`publish-fdroid.yml`, `release.yml`) already uses this one set consistently — there is no
`ANDROID_KEYSTORE_*` variant to unify away in this repo. All six store-deploy workflows already
build the **release** variant (`assembleGmsRelease` / `bundleGmsRelease` / `assembleNoGmsRelease`
for F-Droid), never debug.

App/Universal-Links: replace the SHA-256 in `docs/deeplinks/assetlinks.json` and the TeamID in
`docs/deeplinks/apple-app-site-association`, then host them at the verified domain's `/.well-known/`.

## 7. Server / desktop build-info

`:server` bakes FINGERPRINT into a `version.properties` resource at build time
(`generateVersionResource` in `server/build.gradle.kts`) and exposes it at `GET /version`
alongside the existing `GET /health`. `:desktopApp`'s native installer `packageVersion` is
desktopPackageVersion (`MILESTONE.0.commitCount`), not a separate hand-typed string — MARKETING
can't be used here because Compose Desktop validates the installer version as `MAJOR.MINOR.BUILD`
with `MAJOR ≤ 255` at configure time, and MARKETING's MAJOR is the year.
