# Owner actions — things only the repo owner can do

Everything in this file needs repo-admin rights, a paid developer account, or a credential that
cannot live in the repo. None of it can be automated from a PR, which is why it is written down
rather than done.

**Nothing here is required for the repo to build, test or release on GitHub.** `main` is green and
`v2026.07.30.36.742` ships real artifacts without any of it. These items unlock *distribution* and
*enforcement*, and each one is independently useful — do them in any order, one sitting at a time.

Each task lists: **why**, **where** (the exact platform and page), **what you get** (the literal
value to paste), and **how to verify** it worked. Tick them off as you go.

> **Format of every secret below:** GitHub → your repo → **Settings** → **Secrets and variables** →
> **Actions** → **New repository secret**. Name must match EXACTLY (they are case-sensitive and are
> referenced literally in the workflow YAML). Anything ending `_B64` is base64 of a file:
> ```bash
> base64 -i path/to/file | tr -d '\n' | pbcopy    # macOS — now paste into the secret box
> ```

---

## Tier 0 — 5 minutes, no external account needed

These are pure GitHub settings. Highest value per minute; do these first.

### 0.1 Make `Build & Test` a required status check
- [ ] **Why:** the branch ruleset currently requires exactly ONE check
      (`ktlint · detekt · test · kover · dependency-guard`). `Build & Test` is the only job that runs
      `:server:test`, and nothing in `quality.yml` compiles `:server` at all — so a `:server` compile
      break or a failing server test can merge to `main` today.
- [ ] **Where:** GitHub → repo → **Settings** → **Rules** → **Rulesets** → *"Quality Gate required on main"*
- [ ] **Do:** under **Require status checks to pass**, click **Add checks**, search `Build & Test`,
      select it. Save.
- [ ] **Verify:** open any PR — its merge box should now list two required checks, not one.

### 0.2 Enable auto-delete of merged branches
- [ ] **Why:** ~24 remote branches exist; merged ones linger and make `git branch -r` noise.
- [ ] **Where:** **Settings** → **General** → **Pull Requests**
- [ ] **Do:** tick **Automatically delete head branches**.
- [ ] **Verify:** merge any PR; its branch disappears from the branch list.

---

## Tier 1 — the screenshot bot (fixes a recurring chore)

### 1.1 Give the screenshot workflow a token that isn't `GITHUB_TOKEN`
- [ ] **Why:** `screenshots.yml` re-records Roborazzi baselines on every push to `main` and opens a
      PR using `${{ github.token }}`. GitHub parks every workflow run triggered by a GITHUB_TOKEN
      event at **`action_required`** (the gate that stops Actions triggering Actions), so those PRs
      show no checks, can never satisfy the required check, and pile up. Seven such branches
      accumulated before this was diagnosed. Until this is done, **someone has to approve the runs by
      hand after every push to main.**
- [ ] **Where:** GitHub → **your profile** (not the repo) → **Settings** → **Developer settings** →
      **Personal access tokens** → **Fine-grained tokens** → **Generate new token**
- [ ] **What you get:**
      - Resource owner: your account · Repository access: **Only select repositories** → `Mileway`
      - Permissions → Repository:
        - **Contents:** Read and write  *(push the `bot/refresh-screenshot-baselines-*` branch)*
        - **Pull requests:** Read and write  *(open the refresh PR)*
        - **Workflows:** Read and write  *(only if the bot ever needs to touch `.github/workflows/`;
          otherwise leave it off — narrower is better)*
      - Expiration: 90 days is fine; set a calendar reminder, or use "No expiration" and accept the
        tradeoff.
- [ ] **Then:** add it as repository secret **`SCREENSHOT_BOT_TOKEN`**.
- [ ] **Then tell me** — one line in `screenshots.yml` swaps `${{ github.token }}` for
      `${{ secrets.SCREENSHOT_BOT_TOKEN }}`, and I'll open that PR. (I deliberately have NOT made
      that change yet: pointing the workflow at a secret that doesn't exist would break the job.)
- [ ] **Verify:** push anything to `main`, wait for the Screenshots workflow, and check the PR it
      opens **has checks running immediately** instead of sitting at "action_required".

**Manual workaround until then** — approve the parked runs:
```bash
for r in $(gh run list --branch bot/refresh-screenshot-baselines-<sha7> --limit 10 \
            --json databaseId,conclusion -q '.[] | select(.conclusion=="action_required") | .databaseId'); do
  gh api -X POST repos/darkpandawarrior/Mileway/actions/runs/$r/approve
done
```

---

## Tier 2 — crash symbolication (do before any real user installs the app)

### 2.1 Create the `CRASHLYTICS_UPLOAD` repository **variable**
- [ ] **Why:** `app/build.gradle.kts` gates `mappingFileUploadEnabled` on
      `System.getenv("CRASHLYTICS_UPLOAD") == "true"`. No workflow sets it, so **every release is
      built with mapping-file upload OFF** and any production crash arrives as an unreadable
      obfuscated stack trace.
- [ ] **Where:** **Settings** → **Secrets and variables** → **Actions** → **Variables** tab →
      **New repository variable** *(a variable, NOT a secret — it is not sensitive)*
- [ ] **What you get:** Name `CRASHLYTICS_UPLOAD`, Value `true`
- [ ] **Note:** this only has an effect once Firebase/Crashlytics secrets exist too
      (`GOOGLE_SERVICES_B64`, see 3.1) — the mapping upload needs a configured Firebase app.
- [ ] **Verify:** run the Release workflow; the Gradle log should show the Crashlytics
      `uploadCrashlyticsMappingFile…` task executing rather than being skipped.

---

## Tier 3 — Google Play + signing (the main distribution path)

Everything from here needs a **paid developer account**. Play is a one-time **$25**.

### 3.1 Android release signing + Firebase
- [ ] **Why:** without these, `release.yml` cannot produce a Play-signed AAB, and the GitHub Release
      APKs stay **debug-signed** (they install, but cannot upgrade a Play-installed build in place).
- [ ] **Where:** locally, then GitHub secrets.
- [ ] **What you get:**
      | Secret | Where it comes from |
      |---|---|
      | `KEYSTORE_B64` | `keytool -genkeypair -v -keystore mileway.jks -alias mileway -keyalg RSA -keysize 2048 -validity 10000`, then base64 the `.jks`. **Back this file up somewhere permanent — losing it means you can never update the app on Play.** |
      | `KEYSTORE_PASSWORD` | the store password you chose above |
      | `KEY_ALIAS` | `mileway` (or whatever `-alias` you used) |
      | `KEY_PASSWORD` | the key password you chose above |
      | `GOOGLE_SERVICES_B64` | Firebase console → project → Android app → download `google-services.json` → base64 |
      | `FIREBASE_APP_ID` | Firebase console → Project settings → Your apps → **App ID** (looks like `1:1234567890:android:abc123`) |
- [ ] **Verify:** run Release with `android_rung: internal`; the build log shows the release variant
      signed with your keystore rather than the debug key.

### 3.2 Google Play Console upload credentials
- [ ] **Where:** [play.google.com/console](https://play.google.com/console) → **Setup → API access**
      → link a Google Cloud project → **Create service account** → grant it *Release manager* →
      create a **JSON key**.
- [ ] **What you get:** `PLAYSTORE_CREDS_B64` = base64 of that JSON key file.
- [ ] **Also:** you must upload the **first** AAB to Play by hand — the API refuses to create the
      very first release for an app.
- [ ] **Verify:** run Release with `android_rung: internal`; the build appears on the Play Console
      internal track.

### 3.3 `play-wear` environment (only if shipping the Wear app)
- [ ] **Why:** `release.yml`'s `wear` job declares `environment: play-wear`. The job now builds a real
      Wear AAB (`:wear:bundleRelease` — verified to produce `wear-gms-release.aab`), but the upload
      step needs that environment's secrets.
- [ ] **Where:** **Settings** → **Environments** → **New environment** → name it exactly `play-wear`
      → add the same `PLAYSTORE_CREDS_B64` as an environment secret.
- [ ] **Verify:** run Release with `android_rung: beta`; the wear job uploads to the Play `wear:alpha` track.

---

## Tier 4 — iOS / App Store (needs a Mac and $99/yr)

### 4.1 App Store Connect API + code signing
- [ ] **Why:** `release.yml`'s iOS rungs (`testflight` / `appstore`) cannot sign or upload without these.
- [ ] **Where:** [appstoreconnect.apple.com](https://appstoreconnect.apple.com) → **Users and Access**
      → **Integrations** → **App Store Connect API** → **Generate API Key** (role: *App Manager*).
- [ ] **What you get:**
      | Secret | Value |
      |---|---|
      | `ASC_KEY_ID` | the Key ID shown next to the key |
      | `ASC_ISSUER_ID` | the Issuer ID at the top of that page |
      | `APPSTORE_AUTH_KEY_B64` | base64 of the downloaded `AuthKey_XXXX.p8` — **downloadable only once** |
      | `GOOGLE_SERVICES_IOS_B64` | base64 of `GoogleService-Info.plist` from Firebase (iOS app) |
      | `MATCH_PASSWORD` | passphrase for the fastlane *match* certificate repo |
      | `MATCH_GIT_PRIVATE_KEY` | deploy key for that private match repo |
- [ ] **Note:** *match* needs its own private git repo for certificates. If you'd rather not run
      match, the alternative is manual signing — tell me and I'll adjust `release.yml`.
- [ ] **Verify:** run Release with `ios_rung: testflight`; the build appears in TestFlight.

---

## Tier 5 — alternative Android stores (optional, do any subset)

Each is **independent**. Every workflow is gated on its own secret being non-empty, so an
unconfigured store simply skips — configuring one never affects the others. All of them also need the
Tier 3.1 signing secrets.

> These workflows have never executed. Their action input keys were corrected in PR #84 (Amazon
> expected `releaseFile` not `apkFile`; Indus expected `file_path`/`file_type`/`api_token`), so they
> *should* work first time — but treat the first run of each as a test, not a release.

| Store | Console | Secrets to create |
|---|---|---|
| **Amazon Appstore** | [developer.amazon.com](https://developer.amazon.com) → App Submission API → Security Profile | `AMAZON_APPSTORE_CLIENT_ID`, `AMAZON_APPSTORE_CLIENT_SECRET`, `AMAZON_APPSTORE_APP_ID` |
| **Samsung Galaxy Store** | [seller.samsungapps.com](https://seller.samsungapps.com) → Assist → API service | `SAMSUNG_SERVICE_ACCOUNT_ID`, `SAMSUNG_ACCESS_TOKEN`, `SAMSUNG_CONTENT_ID` |
| **Huawei AppGallery** | [developer.huawei.com](https://developer.huawei.com) → AppGallery Connect → API client | `HUAWEI_CLIENT_ID`, `HUAWEI_CLIENT_KEY`, `HUAWEI_APP_ID` |
| **Indus Appstore** | [indusappstore.com](https://indusappstore.com) developer console | `INDUS_API_KEY` |
| **Aptoide** | [aptoide.com](https://aptoide.com) developer console | `APTOIDE_API_KEY` |

- [ ] **Verify (each):** the store's workflow run should show its steps *executing* rather than
      skipped, and the build should appear in that store's console.

---

## Tier 6 — F-Droid (optional, FOSS distribution)

### 6.1 Publish the FOSS build
- [ ] **Prerequisite, and it is a real one:** the `noGms` build is **not currently GMS-free**. Its
      dependency-guard baseline (`app/dependencies/noGmsReleaseRuntimeClasspath.txt`) contains **15**
      `com.google.android.gms`/`com.google.mlkit` entries plus **4** `com.google.firebase` — 19
      proprietary entries. F-Droid's inclusion policy will reject that.
- [ ] **The blocking decision is yours** (I've flagged it repeatedly and not decided it for you):
      ML Kit powers OCR and document scanning — the app's prime capture feature — and is currently
      *allowlisted into* the FOSS build. Pick one:
      1. **Ship FOSS without OCR/doc-scan** (degraded FOSS flavor, ~40h to replace with tesseract4android)
      2. **Keep ML Kit and stop calling it FOSS** — rename the flavor's claim honestly, drop F-Droid
      3. **Drop the F-Droid track** entirely
- [ ] Only after that: `publish-fdroid.yml` needs the Tier 3.1 signing secrets, then submit the
      metadata to [fdroiddata](https://gitlab.com/fdroid/fdroiddata).
- [ ] **Verify:** `grep -cE 'play-services|com\.google\.mlkit' app/dependencies/noGmsReleaseRuntimeClasspath.txt`
      should read **0** before you submit.

---

## Optional extras

- [ ] **`RELEASE_SCRUBLIST_B64`** — a base64 newline-separated list of terms scrubbed from generated
      release notes. A *second* safety layer only; commit messages are already guard-clean via the
      `no-reference-leak` pre-commit hook. Skip unless you want belt-and-braces.
- [ ] **`IOS_SCHEME` / `RELEASE_BUILD_CMD`** (Variables, not secrets) — override the auto-detected
      Xcode scheme and the release build command. Both have working defaults; leave unset.

---

## Quick reference — is it a secret or a variable?

| Kind | Where | Examples |
|---|---|---|
| **Secret** (masked, write-only) | Settings → Secrets and variables → Actions → *Secrets* | everything ending `_B64`, all passwords, all API keys |
| **Variable** (plain, readable) | same page → *Variables* tab | `CRASHLYTICS_UPLOAD`, `IOS_SCHEME`, `RELEASE_BUILD_CMD` |
| **Environment secret** | Settings → Environments → `play-wear` | `PLAYSTORE_CREDS_B64` for the wear job |

## How to check what's already configured

```bash
gh secret list                       # repository secrets (names only — values are never readable)
gh variable list                     # repository variables
gh api repos/darkpandawarrior/Mileway/rules/branches/main \
  --jq '.[].parameters.required_status_checks[].context'   # which checks are actually required
```
