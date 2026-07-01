<div align="center">

<img src="docs/brand/mileway-icon.png" width="120" alt="Mileway app icon" />

# Mileway

### Offline-first mileage, travel and expense tracking, built in Kotlin and Compose Multiplatform.

A standalone, fully offline app. It puts the location-engineering, offline-first and
multi-module architecture I care about into one place you can actually run.
Every screen draws from deterministic mock data, so there are zero backend calls.

[![CI](https://github.com/darkpandawarrior/Mileway/actions/workflows/ci.yml/badge.svg)](https://github.com/darkpandawarrior/Mileway/actions/workflows/ci.yml)
[![Quality](https://github.com/darkpandawarrior/Mileway/actions/workflows/quality.yml/badge.svg)](https://github.com/darkpandawarrior/Mileway/actions/workflows/quality.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.11.1-4285F4?logo=jetpackcompose&logoColor=white)
![Platforms](https://img.shields.io/badge/platforms-Android%20%7C%20iOS%20%7C%20Wear%20OS-3DDC84)
![Offline](https://img.shields.io/badge/network-zero%20backend-success)

**[Highlights](#highlights)** · **[Screenshots](#screenshots)** · **[Features](#features)** · **[Architecture](#architecture)** · **[Getting started](#getting-started)** · **[Roadmap](#roadmap)**

</div>

---

<details>
<summary><b>Table of contents</b></summary>

- [Why Mileway](#why-mileway)
- [Highlights](#highlights)
- [Screenshots](#screenshots)
- [Features](#features)
- [Architecture](#architecture)
  - [Module map](#module-map)
  - [Project structure](#project-structure)
- [Tech stack](#tech-stack)
- [Getting started](#getting-started)
- [Build flavors](#build-flavors)
- [Ralph-loop development](#ralph-loop-development)
- [Testing and quality](#testing-and-quality)
- [Roadmap](#roadmap)
- [iOS and Wear OS](#ios-and-wear-os)
- [The location engine](#the-location-engine)

</details>

## Why Mileway

Mileway is a self-contained, offline-first mileage tracker. The whole thing runs in airplane mode:
you track trips, log expenses, route approvals, and the data is still there after a restart. No
tracked code reaches for the network.

I also use it as a reference for how I build Android and KMP apps. That means Compose Multiplatform,
a multi-module clean architecture across 23 Gradle modules, MVI-style unidirectional state, Koin for
DI, Room (KMP) with DataStore, and a `gms`/`noGms` flavor split so the same code ships to both the
Play Store and F-Droid.

## Highlights

- 🛰️ **Real location engineering.** The tracking pipeline fights GPS jitter and recovers from spikes,
  with spike detection, four-bucket distance accounting and IMU fusion.
- 📴 **Genuinely offline.** No backend URLs, no API keys, no network calls in tracked code. It runs in
  airplane mode and keeps its state in Room and DataStore.
- 🧩 **23-module clean architecture.** Feature modules never touch each other. They meet only at the
  `:app` composition root, wired through Koin.
- 🌍 **Kotlin Multiplatform — iOS live (V19).** All feature screens run on Android *and* iOS from
  `commonMain`. Background scheduling uses [kmpworkmanager](https://github.com/brewkits/kmpworkmanager)
  (BGTask dispatcher + AppDelegate); platform services sit behind `expect`/`actual`.
- 🔀 **One codebase, two distributions.** A `gms` Play build and a FOSS `noGms` / F-Droid build, with
  a dependency-prefix guard that fails the build the moment a proprietary library leaks into FOSS.
- 🧪 **Quality gates in CI.** 96 Roborazzi screenshot tests on the JVM (no emulator, no network),
  Napier structured logging, detekt, ktlint and Kover, plus reproducible F-Droid release workflows.

## Screenshots

> All screens render from deterministic mock data. Images are recorded with
> [Roborazzi](https://github.com/takahirom/roborazzi) on the JVM, so **no emulator is required**
> (`./gradlew recordRoborazziNoGmsDebug`).

| Track Miles | Journey Detail | Trip Success |
|:---:|:---:|:---:|
| ![Track Miles ready-to-start screen with vehicle selector and distance card](docs/screenshots/track_miles_idle_screen.png) | ![Track detail with route stats, journey overview and GPS-point breakdown](docs/screenshots/track_detail_screen.png) | ![Tracking success summary with distance, reimbursement amount and voucher](docs/screenshots/tracking_success_screen.png) |

<details>
<summary><b>Full screen gallery</b>: every screen across the feature modules, grouped by area (96 images)</summary>

<br/>

#### Tracking

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Track Miles ready-to-start screen with vehicle selector](docs/screenshots/track_miles_idle_screen.png) | ![Tracking success summary with distance, reimbursement and voucher](docs/screenshots/tracking_success_screen.png) | ![Saved tracks journeys tab with date-grouped trip cards](docs/screenshots/saved_tracks_journeys_tab.png) |

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Track detail with route stats, speed and GPS-point breakdown](docs/screenshots/track_detail_screen.png) | ![Track insights with quality-score ring and activity breakdown](docs/screenshots/track_insights_screen.png) | ![Geo check-in with map overlay and radius indicator](docs/screenshots/geo_check_in_screen.png) |

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Location map surface with current-position marker](docs/screenshots/location_map_screen.png) | ![Manual check-in with location, notes, type and time](docs/screenshots/manual_check_in_screen.png) | ![Check-in history list with geo and manual entries](docs/screenshots/check_in_history_screen.png) |

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Hardware-events log with tracking lifecycle entries and filter chips](docs/screenshots/hardware_events_log_screen.png) | ![Track data preview overview tab with completeness metrics](docs/screenshots/track_data_preview_overview_tab.png) | ![Tracking settings with accuracy, interval and battery options](docs/screenshots/track_settings_screen.png) |

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Track customization with map style and overlay toggles](docs/screenshots/track_customization_screen.png) | ![Tracking setup guide walking through permissions](docs/screenshots/tracking_setup_guide_screen.png) | ![Tracking loading screen with progress sub-statuses](docs/screenshots/tracking_loading_screen.png) |

| &nbsp; | &nbsp; |
|:---:|:---:|
| ![Create voucher screen selecting reimbursable expenses](docs/screenshots/create_voucher_select_expenses.png) | ![Developer debug menu for tracking internals](docs/screenshots/debug_menu_screen.png) |

#### Logging & Expenses

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Spends home with totals and recent activity](docs/screenshots/spends_home_screen.png) | ![Manual log-miles step 1 with location search and route preview](docs/screenshots/log_miles_step1_screen.png) | ![Manual log-miles step 2 with travelled locations and amount](docs/screenshots/log_miles_step2_screen.png) |

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Log-miles history with date-grouped mileage entries](docs/screenshots/log_miles_history_screen.png) | ![Expense entry with category, amount and attachment](docs/screenshots/expense_entry_screen.png) | ![Expense details input with policy fields](docs/screenshots/expense_details_input_screen.png) |

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Expense detail view with status and receipt](docs/screenshots/expense_detail_screen.png) | ![Expense history with status filter chips and itemised cards](docs/screenshots/expense_history_screen.png) | ![Voucher history with settlement status](docs/screenshots/voucher_history_screen.png) |

#### Travel

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Travel hub with active trip and upcoming bookings](docs/screenshots/travel_home_screen.png) | ![Create trip request form with purpose and itinerary](docs/screenshots/create_trip_screen.png) | ![Create multi-journey plan with route legs](docs/screenshots/create_mjp_screen.png) |

| &nbsp; | &nbsp; |
|:---:|:---:|
| ![Trip-request history with status tabs and route cards](docs/screenshots/trip_history_screen.png) | ![Booking history with type tabs, status filter chips and fare cards](docs/screenshots/booking_history_screen.png) |

#### Approvals & Payables

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Approvals pending tab with policy-violation badges](docs/screenshots/approvals_screen_pending_tab.png) | ![Approval detail with a flagged policy violation](docs/screenshots/approval_details_screen_violation.png) | ![Payables hub with invoices, PRs and GINs](docs/screenshots/payables_home_screen.png) |

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Create purchase-request form with line items](docs/screenshots/create_purchase_request_screen.png) | ![Purchase-request detail with approval trail](docs/screenshots/purchase_request_details_screen.png) | ![Create invoice form with vendor and amount](docs/screenshots/create_invoice_screen.png) |

| &nbsp; |
|:---:|
| ![Unified payables history across document types](docs/screenshots/payables_history_screen.png) |

#### Payments, Events & Cards

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Pay or Request form with UPI payee, amount and mode toggle](docs/screenshots/create_payment_screen.png) | ![Payments history with status tabs and UPI pay/request cards](docs/screenshots/payments_history_screen.png) | ![Create event form with title, venue, category and capacity](docs/screenshots/create_event_screen.png) |

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Events history with status tabs and venue/attendee cards](docs/screenshots/events_history_screen.png) | ![Cards home with virtual card faces and balances](docs/screenshots/cards_home_screen.png) | ![Card detail with transactions and controls](docs/screenshots/card_detail_screen.png) |

| &nbsp; |
|:---:|
| ![Card request form with KYC-lite fields](docs/screenshots/card_request_screen.png) |

#### Profile & Account

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Account hub with persona switcher, deep-link demo and referral card](docs/screenshots/profile_account_hub.png) | ![Profile details with employee information](docs/screenshots/profile_details_screen.png) | ![Settings with sections for app, privacy and account](docs/screenshots/settings_screen.png) |

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Preferences with units, theme and notification toggles](docs/screenshots/preferences_screen.png) | ![Demo settings to seed and reset mock data](docs/screenshots/demo_settings_screen.png) | ![Analytics dashboard with Canvas-rendered charts](docs/screenshots/analytics_home_screen.png) |

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Mileage analytics detail with trend chart](docs/screenshots/analytics_detail_mileage_screen.png) | ![Advance-request history with status](docs/screenshots/advance_history_screen.png) | ![Ask-advance form step 1 with amount and reason](docs/screenshots/ask_advance_form_step1_screen.png) |

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Delegation screen assigning approver authority](docs/screenshots/delegation_screen.png) | ![Notification centre with grouped alerts](docs/screenshots/notification_centre_screen.png) | ![QR home for scan-to-pay and identity](docs/screenshots/qr_home_screen.png) |

| &nbsp; |
|:---:|
| ![Help and support with FAQs and contact](docs/screenshots/help_support_screen.png) |

#### Search

| &nbsp; | &nbsp; |
|:---:|:---:|
| ![Master search with query, category tabs and grouped results across trips, payments and events](docs/screenshots/search_masterSearch_results.png) | ![Master search empty state prompting across all record types](docs/screenshots/search_masterSearch_empty.png) |

#### Media & Assistant

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Attachment selection with camera and gallery sources](docs/screenshots/media_attachment_selection_screen.png) | ![Attachment preview before attaching](docs/screenshots/media_attachment_preview_screen.png) | ![Media library grid of saved attachments](docs/screenshots/media_cloud_library_screen.png) |

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Camera capture screen with permission prompt](docs/screenshots/media_camera_permission_required.png) | ![AI assistant chat answering an expense query](docs/screenshots/agent_chat_screen.png) | ![Assistant conversation history](docs/screenshots/agent_history_screen.png) |

| &nbsp; |
|:---:|
| ![Assistant home bottom sheet with suggested actions](docs/screenshots/assistant_home_sheet.png) |

#### App shell & Security

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Home dashboard with greeting and quick actions](docs/screenshots/home_screen_loaded.png) | ![Login screen with demo credentials](docs/screenshots/login_screen.png) | ![Branded splash screen](docs/screenshots/splash_screen.png) |

| &nbsp; | &nbsp; | &nbsp; |
|:---:|:---:|:---:|
| ![Shell placeholder for an in-progress destination](docs/screenshots/shell_placeholder_screen.png) | ![Root guard showing detected root signals in red](docs/screenshots/root_guard_screen.png) | ![Root guard confirming a clean, secure device](docs/screenshots/root_guard_screen_clean.png) |

<sub>Plus component matrices (status cards, booking cards, PO cards, success-state variants) in
<a href="docs/screenshots"><code>docs/screenshots/</code></a>, rendered from <code>@Preview</code> composables by
<code>ScreenshotCatalogTest</code>. Every full screen above is recorded by <code>ScreenshotGalleryTest</code>.</sub>

</details>

## Features

Every feature is fully interactive on mocked, offline data.

| Area | What's inside |
|---|---|
| **Tracking** | Live GPS trip tracking on a foreground service (jitter suppression, spike detection, four-bucket accounting); geofenced check-in with manual fallback; saved tracks (journey/submission tabs); trip insights; hardware-events log; GPX / CSV / KML / GeoJSON export. |
| **Logging &amp; Expenses** | Step-by-step manual trip logging; expense entry → detail → success chain. |
| **Travel** | Travel hub, active-trip card (flight / train), upcoming bookings, plus trip &amp; booking history surfaces. |
| **Approvals &amp; Payables** | Approval queue with policy-violation badges and seek-clarification sheet; payables hub, multi-step create-PR / invoice flows and history surfaces. |
| **Payments, Events &amp; Cards** | QR pay / request + history; event creation + history; card home / detail / request (KYC-lite). |
| **Profile &amp; Account** | Account hub, advance requests, Canvas-rendered analytics dashboards, an AI assistant sheet, notification centre, permission-health screen, and a MaterialKolor theme engine. |
| **Media** | CameraX capture (flash, pinch-zoom, tap-focus), on-device odometer OCR, attachment grid. |
| **Master search** | A registry-based search that fans a query across every feature module. |

## Architecture

Multi-module clean architecture. Feature modules never depend on one another; they meet only at the
`:app` composition root. State is unidirectional. Each screen exposes a single immutable state as a
`StateFlow`, collected with `collectAsStateWithLifecycle`, and a shared `ScreenState` wrapper models
the loading, empty, error and content cases.

```mermaid
graph TD
    APP[":app · composition root · navigation · Koin graph"]

    subgraph Features
      direction LR
      FT["tracking"]; FL["logging"]; FM["media"]; FP["profile"]
      FA["approvals"]; FPA["payables"]; FTR["travel"]; FAG["agent"]
      FC["cards"]; FPM["payments"]; FE["events"]
    end

    subgraph Core
      direction LR
      UI["core:ui · design system + theme engine"]
      DATA["core:data · Room (KMP) · DataStore"]
      NET["core:network · API contracts"]
      PLAT["core:platform · expect/actual services"]
      SEC["core:security · root detection"]
      MAPS["core:maps (+ krossmap / maplibre)"]
    end

    STUB[":stub · deterministic mock data"]
    WEAR[":wear · Wear OS tile"]

    APP --> Features
    APP --> STUB
    APP --> WEAR
    Features --> Core
    STUB --> DATA
    STUB --> NET
```

**Key patterns**

- **commonMain-first KMP.** Core modules compile for Android and iOS (`iosArm64`,
  `iosSimulatorArm64`). Platform-bound tech (FusedLocation, CameraX, ML Kit, WorkManager,
  BiometricPrompt, the foreground service) sits behind `expect`/`actual` interfaces in `:core:platform`.
- **Koin DI.** One module per feature, and the `InitKoin()` bootstrap is re-entrancy-safe for both the
  Android `Application` and the iOS entry point.
- **SearchProvider registry.** Each feature binds a `SearchProvider` into Koin. The master-search
  aggregator resolves `getAll<SearchProvider>()` and fans out, so search and the features stay decoupled.
- **Shared scaffolds.** `FormSubmissionScaffold` and `HistoryListScaffold` standardise the create and
  history flows that travel, payables, payments and events all reuse.
- **Navigation.** Type-safe JetBrains Compose Navigation, with per-feature graphs assembled at `:app`.

### Module map

| Module | Responsibility |
|---|---|
| `:app` | Composition root, navigation host, Koin graph assembly, build flavors |
| `:core:ui` | Compose design system, theme engine (MaterialKolor), Canvas charts, shared scaffolds |
| `:core:data` | Room (KMP) database, DAOs, entities, DataStore repositories |
| `:core:network` | API contract &amp; policy models (mocked) |
| `:core:platform` | `expect`/`actual` platform-service interfaces + Android/iOS impls |
| `:core:security` | Device-integrity (root) detection, encryption-ready storage |
| `:core:maps` · `-krossmap` · `-maplibre` | Map-surface contract + flavor-specific implementations |
| `:core:common` | Shared utilities / primitives |
| `:feature:*` | tracking · logging · media · profile · approvals · payables · travel · agent · cards · payments · events |
| `:stub` | Deterministic mock data for every repository (no backend) |
| `:wear` | Wear OS companion tile |
| `:shared` | iOS umbrella framework — re-exports `core:ui`, `feature:tracking` and `feature:agent` as the single `Mileway.framework` Xcode links against |
| `:widget` | Glance home-screen widget (mileage summary) |
| `:baselineprofile` | Macrobenchmark module generating the Baseline Profile via `:app:generateNoGmsReleaseBaselineProfile` |
| `build-logic` | Gradle convention plugins (centralised AGP / Kotlin / Compose config) |

### Project structure

```text
Mileway/
├── app/                      # Android application: composition root, navigation, DI, flavors
├── core/
│   ├── ui/                   # Compose design system, theme engine, shared scaffolds
│   ├── data/                 # Room (KMP) + DataStore
│   ├── network/              # API contracts (mocked)
│   ├── platform/             # expect/actual platform services
│   ├── security/             # root detection, encryption-ready storage
│   ├── maps/ maps-krossmap/ maps-maplibre/   # map-surface contract + impls
│   └── common/               # shared utilities
├── feature/                  # tracking · logging · media · profile · approvals
│                             # payables · travel · agent · cards · payments · events
├── stub/                     # deterministic mock data for every repository
├── wear/                     # Wear OS companion tile
├── shared/                   # iOS umbrella framework (re-exports core:ui, feature:tracking, feature:agent)
├── widget/                   # Glance home-screen widget
├── baselineprofile/          # macrobenchmark module for Baseline Profile generation
├── build-logic/              # Gradle convention plugins
├── docs/                     # README assets, screenshots, release & brand docs
└── fastlane/                 # store metadata + screenshots
```

## Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin **2.4.0** |
| UI | Compose Multiplatform **1.11.1**, Material 3 |
| Build | AGP **9.2.1**, Gradle Kotlin DSL, convention plugins, version catalog |
| DI | Koin **4.2.2** (multiplatform) |
| Database | Room **2.8.4** (KMP, bundled SQLite) |
| Settings / session | AndroidX DataStore |
| Networking | Ktor **3.5.0** (OkHttp + Darwin engines), mocked with no live backend |
| Concurrency | Coroutines + Flow (no LiveData); `kotlinx-datetime` **0.8.0** in commonMain |
| Maps | osmdroid / MapLibre (`noGms`, offline MBTiles) · KrossMap (`gms`) |
| Charts | Canvas-only (no MPAndroidChart / Vico) |
| Testing | JUnit, MockK, Turbine, Robolectric, Koin-Test, **Roborazzi 1.64.0** screenshots |
| Quality | detekt **1.23.8**, ktlint, Kover, dependency-guard |
| SDK | compileSdk **37**, minSdk **30**, JDK 21 |

## Getting started

```bash
git clone https://github.com/darkpandawarrior/Mileway.git
cd Mileway

# Assemble the offline-safe default build
./gradlew assembleNoGmsDebug

# Install on a device/emulator (API 30+)
adb install app/build/outputs/apk/noGms/debug/app-noGms-debug.apk
```

No network connection is required. The data is all mock and persists locally through Room and DataStore.

> **Offline check:** enable airplane mode, track a trip, kill and relaunch the app, and confirm the
> record persisted.

<details>
<summary><b>All build &amp; tooling commands</b></summary>

```bash
# Build variants
./gradlew assembleNoGmsDebug          # FOSS / offline build (default)
./gradlew assembleGmsDebug            # Google-services build
./gradlew assembleNoGmsRelease        # reproducible FOSS release (F-Droid)

# Tests & screenshots (noGms only; gms crashes Robolectric)
./gradlew testNoGmsDebugUnitTest      # JVM unit tests (88 test classes, no emulator)
./gradlew recordRoborazziNoGmsDebug   # (re)record screenshot baselines → docs/screenshots/

# Quality
./gradlew ktlintCheck detekt          # style + static analysis
./gradlew :app:koverXmlReport         # coverage report
```

</details>

## Build flavors

A `maps` flavor dimension splits the app into a proprietary and a FOSS build:

| Flavor | Maps | Google / Play / Firebase | Use case |
|---|---|---|---|
| `gms` | KrossMap (Google Maps / MapKit) | Firebase + Play services | Play Store build |
| `noGms` | MapLibre + offline MBTiles (no API key) | none (FOSS-clean) | F-Droid / fully offline |

A dependency-prefix guard fails the build if proprietary libraries leak into the `noGms` classpath.

## Ralph-loop development

This repo is built and evolved almost entirely through autonomous Ralph-loop iteration
(`.ralph/PLAN.md` plus versioned plans `PLAN_V3` … `PLAN_V20`, each one migration phase — KMP
hoisting, iOS parity, the AI assistant rebuild, etc.). Progress is tracked per iteration in
`.ralph/PROGRESS.md`.

- **Verification gate (current, flavored build):**
  ```bash
  ./gradlew assembleNoGmsDebug && ./gradlew testNoGmsDebugUnitTest
  ```
  (the `gms` flavor crashes Robolectric, so unit tests only run on `noGms`.)
- **Guardrail:** the Ralph Stop hook reverts uncommitted *tracked* edits between turns — each
  iteration must edit, build/test, and commit within the same turn, or the change is lost.
- Historical note: earlier plan revisions (and `CLAUDE.md`/`AGENTS.md`) reference the original,
  pre-flavor bootstrap commands `assembleDebug` / `testDebugUnitTest` from when this repo was a
  bare single-variant extraction of the mileage feature; those tasks are long complete and the
  flavored commands above are what CI and current Ralph runs actually use.

## Testing and quality

- **JVM unit tests.** 88 test classes covering ViewModels, repositories and feature logic with MockK
  and Turbine, run on the `noGms` flavor with no emulator.
- **Screenshot tests.** Roborazzi renders every screen across all feature modules plus the
  component-preview matrices on the JVM (`ScreenshotGalleryTest` and `ScreenshotCatalogTest`, 90+ PNGs
  in `docs/screenshots/`). They're deterministic and diff cleanly in PRs.
- **Static analysis.** detekt and ktlint across every module, with Kover for coverage.
- **CI.** `.github/workflows/ci.yml` runs `assembleGmsDebug` and `testNoGmsDebugUnitTest` on every push
  and PR. Separate `quality`, `release` and `publish-fdroid` workflows handle the gates and distribution.

## Roadmap

A snapshot of where Mileway is and where it's heading. This is a portfolio/demo project, so the
roadmap reflects direction rather than commitments.

**Shipped**

- [x] Offline-first app on deterministic mock data (zero backend calls)
- [x] 23-module clean architecture with Koin DI
- [x] Compose Multiplatform UI; `commonMain` core compiles for Android + iOS
- [x] `gms` / `noGms` flavor split + FOSS dependency-prefix guard
- [x] Room (KMP) + DataStore persistence
- [x] Location engine (jitter / spike / four-bucket / IMU fusion) with a simulated drive source
- [x] Master search: a registry across feature modules with an aggregator, results screen and navigation
- [x] Roborazzi screenshot suite (96 screens, JVM-only), detekt / ktlint / Kover, CI + release workflows
- [x] Wear OS companion tile
- [x] **iOS UI parity (V19).** All feature screens in `commonMain`; background scheduling via
      kmpworkmanager; AppDelegate + BGTask dispatcher; iOS builds and passes all CI gates.
- [x] Napier structured logging across all modules
- [x] **AI assistant / "agent" feature (V20).** Offline, retrieval-grounded chat over real local
      trip/expense/card data; Room-backed persistent history + 5-minute session resume; on-device
      voice I/O (STT/TTS); feedback, export and local usage analytics; full `commonMain` + iOS parity.
- [x] Matrix / terminal design-language pass across the whole UI (theme tokens, topbar, screenshots)
- [x] Renamed the project and package from MileTracker(Demo) to Mileway end-to-end

**Exploring**

- [ ] Baseline Profiles for startup/scroll performance
- [ ] watchOS companion &amp; iOS WidgetKit surfaces
- [ ] Instrumented (on-device) UI test tier alongside the JVM suite
- [ ] Accessibility pass (TalkBack, semantics, large-font / RTL)
- [ ] Larger bundled offline map packs
- [ ] Expand Roborazzi catalog to remaining edge-case states

## iOS and Wear OS

- **iOS.** Every `:core:*` module compiles to an iOS framework, with `expect`/`actual` services
  backed by CoreLocation, Vision (OCR), UserNotifications, LocalAuthentication and BackgroundTasks.
  A few proprietary integrations (in-app update, install-referrer) are stubbed with `TODO(ios)`
  markers, and the shared Compose UI renders through a minimal SwiftUI host.
- **Wear OS.** `:wear` ships a `MileageTileService` tile that surfaces today's distance.

## The location engine

The tracking pipeline is built to suppress jitter and recover from GPS spikes:

- **Jitter suppression.** Stationary drift gets filtered out while the anchor point is preserved.
- **Spike detection.** An implied-speed check flags teleporting fixes instead of silently dropping them.
- **Four-bucket accounting.** `original`, `cleaned`, `abnormal` and `mock` are each persisted per track.
- **Mock-location flagging.** Spoofing is detectable, not just blocked.
- **IMU fusion.** Accelerometer and gyroscope snapshots feed the post-hoc insight analyzers.

Set `SIMULATE_LOCATION = true` and a simulated drive source feeds believable fixes through the exact
same pipeline, so the whole tracking flow works on an emulator with no GPS hardware.

---

<div align="center">
<sub>Mileway is a portfolio / demo project. All companies, bookings, cards and amounts are fictional mock data.</sub>
</div>
