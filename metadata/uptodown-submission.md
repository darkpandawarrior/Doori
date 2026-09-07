# Uptodown submission — Mileway

## Verdict: worth doing

Uptodown runs a genuine free self-serve developer console (sign up, "Add new app", attach a file
— [en.uptodown.com/developers-console](https://en.uptodown.com/developers-console)), no per-app
fee, no country restriction found for India. Every file is scanned across 75+ VirusTotal engines and
Uptodown identity-verifies registering developers, which puts it at a 100/100 trust score with no
malware/phishing blacklist hits (source:
[mywot.com/scorecard/uptodown.com](https://www.mywot.com/scorecard/uptodown.com)). That's the
opposite of the reputational profile that ruled out APKPure for this app family — see the channel
verdict summary for the comparison.

## Not done yet (owner-only, needs a real Uptodown account)

- Actual account signup and identity verification — cannot be scripted or done on your behalf.
- A 1024×500 featured banner graphic. None exists in the repo; `featureGraphic.png` in fastlane is a
  different aspect ratio for a different store. Uptodown lists this as recommended, not mandatory.
- Whether a privacy policy URL is mandatory on the form — not confirmed from outside the console.
  Mileway does GPS tracking and stores expense/receipt data locally; if the form asks, point at
  whatever privacy policy already ships for the other stores rather than writing a new one here.

## Account setup (do this yourself)

1. Go to https://en.uptodown.com/developers-console and register — free.
2. Complete Uptodown's developer verification step before your first submission goes live.

## Submit the app

1. Developers Console → Apps → **Add new app**.
2. Package name: `com.mileway` (must match exactly).
3. Upload the signed APK. Use the exact release asset, not a glob — the release also carries
   unsigned `-gms-release` / `-noGms-release` variants:
   `https://github.com/darkpandawarrior/Mileway/releases/download/v2026.08.35.36.840/Mileway-v2026.08.35.36.840.apk`
   (signing cert SHA-256: `e3cd9ed25baaa6db5501621a2a7399edc0878022f9b64b5d95446db0348dd19c` — verify
   with `apksigner verify --print-certs` before uploading).
4. Icon: Uptodown wants a square PNG, ≥256×256, corners rounded by the site itself. Already built,
   reuse it rather than re-exporting:
   `/Users/darkpandawarrior/Repos/Android/Mileway/fastlane/metadata/android/en-US/images/icon.png`
5. Screenshots: vertical/portrait preferred. Reuse the existing set at
   `/Users/darkpandawarrior/Repos/Android/Mileway/fastlane/metadata/android/en-US/images/phoneScreenshots/`
   — `01_home_screen_loaded.png` through `08_ocr.png` (8 files).
6. Category: closest fit is Finance/Business, matching the F-Droid `Categories: Money, Navigation`
   entry — confirm against Uptodown's own category list in the console, not independently verified.
7. **Disclose the anti-feature.** This build (`noGms`) still pulls in `play-services-location` and
   the Play Services ML Kit document scanner/text recognizer at the dependency level, even though
   the Firebase/Google-Services Gradle plugins are stripped — that's why F-Droid tags it
   `NonFreeDep`. The full description below already states this plainly; keep that paragraph in
   whatever you paste, don't trim it out for length.

## Copy to paste

**Title**
```
Mileway
```

**Short description**
```
Offline-first mileage and expense tracking, no account needed
```

**Full description** (trim if the form enforces a shorter limit than this — not confirmed)
```
Mileway is an offline-first mileage and expense tracker. Core tracking works with no account and no network connection: log a trip, record an expense, and everything is saved straight to your device.

It targets four platforms from one Kotlin Multiplatform codebase: Android, iOS, watchOS, and Wear OS. Start a trip on your phone or your wrist, and the record lands in the same local store either way.

What it does:

- GPS trip tracking, start to stop, with distance and duration logged automatically.
- Expense entries with receipt photos and a document scanner for pulling numbers off paper receipts.
- Geo check-ins for logging a location visit without a full trip.
- An approvals view for reimbursement, so a saved trip can move from logged to submitted to approved.
- Local export of your trip and expense history.

Who it is for: anyone who tracks mileage or expenses for reimbursement, whether that is a daily commute, client visits, or field work, and would rather that data stay on the device than sync to a server by default.

What is technically interesting: the whole app, UI included, is shared Kotlin across Android, iOS, and the two watch platforms, with a local-first data layer that does not require a backend to be useful on day one.

Caveat, stated plainly: this build includes Google Play Services components for location (play-services-location) and on-device text recognition (Play Services ML Kit document scanner and text recognition). The noGms build flavor removes the Firebase and Google Services Gradle plugins, but the location and OCR features still pull in these proprietary runtime dependencies. If you need a build with zero proprietary dependencies, those two features are the parts to look at first.

Source: https://github.com/darkpandawarrior/Mileway, GPL-3.0-or-later.
```

## What I could not confirm (verify in the console before relying on it)

- Exact character limits for title / short description / full description.
- Whether a privacy policy URL field is mandatory.
- Review/approval turnaround time.
