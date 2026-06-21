# Release guide

How to ship Mileway to Play (gms), the App Store / TestFlight (iOS), and F-Droid (noGms). Every
credential below is an **env-activated placeholder**. The build degrades to a no-op when a key is absent, so
nothing here is required to build or run the demo.

## 1. Versioning (single source)

`VERSION` (semver) and `BUILD_NUMBER` at the repo root are the only place versions change.
`versionCode = 1 (VERSION_CODE_BASE) + BUILD_NUMBER`; `versionName = VERSION`.

```bash
scripts/bump_version.sh --patch   # or --minor / --major   → bumps VERSION + BUILD_NUMBER
```

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
3. Submit `metadata/com.miletracker.fdroid.yml` to `fdroiddata` (Binaries approach; the prebuild strips the
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

App/Universal-Links: replace the SHA-256 in `docs/deeplinks/assetlinks.json` and the TeamID in
`docs/deeplinks/apple-app-site-association`, then host them at the verified domain's `/.well-known/`.
