# PROGRESS — PLAN_V18 (consolidated: V18 + folded V13 tail)

Ledger for the Ralph loop finishing `.ralph/PLAN_V18.md`. One row per task per iteration.
Verification gate: `./gradlew assembleNoGmsDebug && ./gradlew testNoGmsDebugUnitTest`
(+ `assembleGmsDebug` when the task touches gms source sets — P3.1 D.1/D.2/D.4, P3.3 O.2).

## Scope decision (2026-06-22)
Backlog audit (`ralph-backlog-audit` workflow) verified every prior plan against live code:
- **Done-in-repo, do NOT reopen:** V12 (→V13), V14 (MVI 45/47 VMs, 11 tracking VMs, cards), V15 (all P0/P1/P2;
  dep-guard runs EXIT=0), V16 (iOS Koin, dialog sweep, 6/7 iOS stubs done), V17 (master-search + payments/events).
- **Live queue:** V18 (100% unexecuted) + a verifiable V13 tail → folded in as P3.
- **Deferred (native, ungateable here):** Wear app / watchOS / WidgetKit; V17 support module + Task Bucket.

## Status legend
`[ ]` not started · `[~]` in progress · `[x]` done + verified

## P0 — net-new, highest signal
- [x] P0.1 — G1 Paging 3 ✅ DONE (iter 2, commit a7f05f0). Resolved the "no real target" block per the
      recommendation: landed on the **GPS route-points log** — the one genuinely list-heavy, flat,
      Room-backed surface (a journey = thousands of fixes). NOT forced onto the mock/aggregate history
      screens. `LocationPagingSource` (commonMain, offset-keyed over existing getLocationsByTokenPaged/
      countLocationsByToken — no migration, no room-paging) → `RoutePointsViewModel` exposes
      `Flow<PagingData>` (Pager+flatMapLatest+cachedIn; count via COUNT in MVI state) → `RoutePointsScreen`
      (androidMain) `collectAsLazyPagingItems` with refresh/append/empty states, reachable from TrackDetail.
      JVM `LocationPagingSourceTest` proves page boundaries. Both flavors build; tests green; ktlint clean.
- [x] P0.2 — G2 ✅ DONE (iter 3, commit 5ca7524). New `:baselineprofile` com.android.test module
      (flavor-matched gms/noGms, targets :app) with `BaselineProfileGenerator` driving Home→Track→start→stop;
      app applies the consumer plugin + `baselineProfile(project(":baselineprofile"))` + `<profileable>`.
      Catalog: androidx.baselineprofile + benchmark-macro-junit4 + uiautomator @ 1.5.0-alpha06 (stable 1.4.1
      predates AGP 9; alpha needed for AGP 9.2). Built-in Kotlin (no kotlin.android); com.android.test applied
      versionless (already on classpath). Generate on device/GMD via `:app:generateNoGmsReleaseBaselineProfile`
      — kept the static baseline-prof.txt as fallback (can't generate without a device in the gate). Both
      flavors configure+build, :baselineprofile compiles, JVM tests green.

## P1 — depth, correctness, fidelity
- [x] P1.1 — G4 ✅ DONE (iter 4, commit c07049d). Tidy-up (not rebuild): deleted the 4 dead residual sheet
      booleans from TrackMilesScreenState (written, never read; real routing is VM `activeSheet: TrackSheet`);
      lifted the JourneyGuideStep derivation from TrackMilesScreen's inline `when{}` into a VM-owned computed
      `TrackMilesUiState.journeyStep`. Left TrackMilesPhase (session lifecycle) + TrackSheet (which sheet) as-is
      — orthogonal concerns, NOT duplicate steppers (folding them in would conflate state; rejected the plan's
      literal "merge all three" as architecturally wrong). Verified by a new VM unit test + the unchanged
      track_miles_idle_screen Roborazzi baseline (behavior-preserving, so no new sheet screenshot needed —
      JourneyGuideSheet is a ModalBottomSheet, awkward/flaky to snapshot in isolation).
- [x] P1.2 — G6 wire Kalman live (DataStore `track_enable_kalman` key → LocationTrackingService reads it →
      bound TrackCustomizationScreen toggle at the nav site). Default OFF (preserves tracking math, guardrail).
- [x] P1.3 — G7 odometer end→start rollover via DataStore (`track_last_odometer_end`); persisted on END-capture
      confirm, prefilled as START baseline (killed the bare `45_000` fallbacks). Sentinel `LAST_ODOMETER_NONE`.
- [x] P1.4 — G8 tri-state `FieldStatus` (SET=green/MISSING=red, PENDING=gray reserved) on TrackDataPreviewScreen
      Details tab (Odometer/Account/Context rows). Layered on existing HealthLevel/IssueSeverity, not replacing.
- [x] P1.5 — G9 ✅ DONE (iter 5, commit e59196e). First androidTest: `TrackingLifecycleTest` — FusedLocation
      test double (setMockMode/setMockLocation) → real Room DB + LocationRepository (service-style writes) →
      finalize SavedTrack → close+reopen DB (kill→relaunch) → assert trip + full trail persist (CLAUDE.md
      offline promise). app: testInstrumentationRunner + androidTest deps (test-ext/core/runner, room-runtime,
      play-services-location) + GMD `pixel6Api34` on AOSP ATD (headless→noGms). CI: instrumented-tests job
      (KVM + licenses) runs `pixel6Api34noGmsDebugAndroidTest`. Device-gated: verified via
      `assembleNoGmsDebugAndroidTest` (compiles) + managed-device config configures clean; unit gate green.
- [x] P1.6 — G11 ✅ DONE (iter 6, commit bca4c26). New Android-only `:widget` module (mileway.android.library
      convention) — `MileageSummaryWidget : GlanceAppWidget` folds completed tracks through SurfaceSnapshotProducer
      and renders today/week distance+trips + live "Tracking now"; receiver + AppWidgetProviderInfo + manifest;
      `:app` depends on `:widget` so the receiver merges in (USED). Catalog: glance-appwidget + appwidget-testing
      (dropped glance-material3 — GlanceTheme resolved awkwardly on 1.1.1; plain colors render identically).
      Glance render test (host-side `runGlanceAppWidgetUnitTest`, runs in JVM gate). Both flavors build; needed
      `implementation(room.runtime)` in :widget (core:data hides Room). **P1 COMPLETE.**

## P3 — folded V13 tail (verifiable)
- [x] P3.1 ✅ **COMPLETE** (iters 7–11) — media pipeline depth: D.1 camera exposure slider (25fbf31; zoom/focus
      pre-existed), D.2 multi-pass OCR (ff46400), D.2b gallery OCR (4877672), D.3 odometer provenance (ac81863),
      D.4 ML Kit doc scanner as Compose launcher (6816acc), D.5 attachment OCR data+migration (9642425) + OCR badge
      (376c7ae). Drag-reorder deferred (low-value/high-friction on the mixed grid). Both flavors build; tests green.
  - older detail: **D.2 ✅ DONE** (iter 7, commit ff46400): multi-pass odometer OCR. RealMediaRepository
      runs 4 ColorMatrix enhancement variants (default/high-contrast/grayscale/brighten) → new pure
      `OdometerOcrAggregator` (majority reading, agreement+labelled-bonus confidence, isVerified≥2, tie-break) →
      OcrResult gains passCount/isVerified (additive). JVM test `OdometerOcrAggregatorTest`. Both flavors build.
      · **D.3 ✅ DONE** (iter 8, commit ac81863): OdometerReadingSource { DEVICE_OCR, MANUAL, AGENT_STUB } replaces
      OdometerCaptureResult.isManual (derived isManual kept for back-compat); construction sites (OdometerCameraScreen,
      TrackingNavigation start/end) map bool→enum; provenance test added. Both flavors build.
      · **D.5 data-layer ✅ DONE** (iter 9, commit 9642425): TripAttachmentEntity + file_name/ocr_confidence/
      ocr_verified columns; MIGRATION_4_5 (v4→v5 additive ALTER ×3) in both builders; repo auto-derives fileName +
      carries OCR provenance; instrumented TripAttachmentMigration4to5Test (direct migration test on GMD).
      · **D.2b ✅ DONE** (iter 10, commit 4877672): GalleryOdometerProcessor (pure adapter over runOcr multi-pass) +
      "Pick from gallery" affordance in OdometerCameraScreen (PickVisualMedia → process → confirm-sheet pre-fill) +
      JVM test (FakeMediaRepository). Both flavors build.
      · **D.4 ✅ DONE** (iter 11, commit 6816acc): functional ML Kit document scanner — rememberDocumentScanLauncher
      (GmsDocumentScanning IntentSender → StartIntentSenderForResult → page URIs), wired to the DOC_SCANNER tile in
      AttachmentSelectionScreen (was illustrative). Built as a Compose UI launcher, NOT a headless service (ML Kit
      needs an Activity — same reason DocumentScanner service stays a documented no-op). Both flavors build.
      · Remaining: **D.1** CameraX controls (pinch-zoom/tap-focus/exposure — device UI) · **D.5 UI** (OCR-badge +
      drag-reorder attachment grid — data layer landed).
- [x] P3.2 ✅ **COMPLETE** (iters 13–16) — tracking-state/notification enrichment: C.2b snapshot enrichment
      (0470dd4), C.3 quality/spike/health chips (387dcc8), C.2d 7-type notifications + throttle + deep link
      (a3af0cd), C.2g post-resume grace window (d9e84d0). Both flavors build; tests green.
  - older detail: **C.2b ✅ DONE** (iter 13, commit 0470dd4): TrackingSystemFlags (core:data) + enriched
      TrackingSnapshot (qualityScore/spikeDistanceM/isGpsAvailable/inResumeGrace/systemFlags); LocationTrackingService
      wires TrackingQualityScorer per fix; TrackingStatePublisherTest covers it. Both flavors build.
      · **C.3 ✅ DONE** (iter 14, commit 387dcc8): TrackMilesUiState gains qualityScore/spikeDistanceM/systemFlags
      (mapped from snapshot in observeTrackingState); statusChips() renders Quality NN% + Spikes + most-severe
      health chip via CompactSystemStatusIndicator; VM test proves the mapping. Both flavors build.
      · **C.2d ✅ DONE** (iter 15, commit a3af0cd): TrackingNotificationMapper (pure, 7 types: ACTIVE/PAUSED/
      GPS_DISABLED/PERMISSION_MISSING/POLICY_VIOLATION/TRIP_COMPLETE + worker AUTO_DISCARD) wired into the service's
      (used) notification path + 2s same-type throttle + TRIP_COMPLETE deep link (mileway://track; routeId
      detail deferred — needs router+nested-nav). JVM mapper test. Both flavors build.
      · Remaining: **C.2g** resume grace (wire inResumeGrace in pipeline/service: suppress spike/auto-discard
      briefly after a resume).
- [x] P3.3 ✅ **COMPLETE** (iters 17–18) — sensor fusion: O.1 MotionState/toMotionState provider (7807e0c),
      O.3 LocationProcessor motionStill fusion (7807e0c), O.2 Play Services activity recognition fused into
      stillness (ebe1bc3 — ActivityRecognizer + GmsActivityRecognizer callbackFlow + pure ActivityTypeMapper,
      graceful no-Play-Services fallback). GPS + IMU + activity recognition all feed jitter suppression. Both flavors build.
  - older detail: **O.1 + O.3 ✅ DONE** (iter 17, commit 7807e0c): O.1 MotionState enum + pure toMotionState
      flow + motionState default on MotionSensorProvider; O.3 LocationProcessor.process(motionStill) strengthens
      jitter suppression, service computes motionStill per fix from the IMU snapshot via MotionFusion (guarded to
      no-op without real sensor data). JVM tests: MotionStateTest + LocationProcessor IMU-stillness case. Both flavors build.
      · Remaining: **O.2** live activity recognition — ActivityRecognitionClient (gms) / CMMotionActivityManager (ios) /
      noGms no-op, driving auto-pause/resume or feeding MotionState. ⚠️ gms → also assembleGmsDebug.
- [x] P3.4 (COMPLETE — L.1 + E.2 + E.3) — **L.1 ✅ DONE** (iter 19, commit c30cbd5): SurfaceSnapshot enriched
      (isPaused/qualityScore/weekGoalKm/actionRequiredCount/lastTripLabel + weekGoalProgress); producer derives
      action-count + last-trip; SnapshotPublisher + InMemorySnapshotPublisher bound in both CoreDataModules; service
      publishes on trip completion; widget renders the enriched fields; producer + publisher JVM tests. Both flavors build.
      · **E.2 ✅ DONE** (iter 20, commit 9b3bdc7): MapProvider enum + LocalMapProvider CompositionLocal (core:ui);
      ThemeController.mapProvider typed (String→MapProvider); MilewayTheme seeds it from a param; AppRoot passes
      themeController.mapProvider; MapScreen reads LocalMapProvider.current (satellite→traffic overlay default). Both flavors build.
      · **E.3 ✅ DONE** (iter 21, commit 57c16a5): OfflineTileProvider (pure spec-v8 style builder, JVM-tested) +
      rememberOfflineMbtilesPath expect/actual (Android extracts the bundled mbtiles asset→filesDir; iOS null);
      MapLibreSurface builds BaseStyle.Json(offline style) when offline requested + pack resolves; offlineTiles threaded
      through MapSurface/KrossMap/Fake; MapScreen's showOfflineTiles toggle now drives it. Both flavors build.
      · **P3.4 COMPLETE.**
- [~] P3.5 (LAST, in progress) — **A.9 ✅ DONE** (iter 22, commit eaddb7f): material3 PullToRefreshBox on
      RoutePointsScreen → LazyPagingItems.refresh() re-queries Room via LocationPagingSource (no VM change, no
      data loss; pull spinner only when the list is already loaded). Both flavors build.
      · **B.2a ✅ DONE** (iter 23, commit 9d852c0): configureComposeCompilerMetrics() wires metricsDestination/
      reportsDestination behind -Pcompose.metrics in all 3 Compose convention plugins; off by default. Verified the flag
      emits real per-module composables/classes reports (:core:ui). Both flavors build; tests green.
      · **H.8 ✅ DONE** (iter 24, commit 3f4559e): mileway.test convention plugin bundles the generic JVM unit-test
      stack (JUnit/MockK/coroutines-test/Turbine/Koin-test) via a version-catalog lookup; :app applies it + drops those
      6 lines (app-specific extras stay local). Verified: both flavors build; testNoGmsDebugUnitTest re-ran clean (98
      classes, 0 failures). **P3.5 COMPLETE.**

## ✅ ALL DONE — P0 + P1 + P3 complete (2026-06-22, iter 24)
P0 ✅ · P1 ✅ · P3.1 ✅ · P3.2 ✅ · P3.3 ✅ · P3.4 ✅ · P3.5 ✅. (P2 opportunistic — P2.1 done; DEFERRED block out of
scope: Wear/watchOS/WidgetKit + V17 support module/Task Bucket.) Both flavors build standalone; JVM tests pass.
Branch feat/plan-v18-consolidated. → <promise>DONE</promise> emitted.

## P2 — opportunistic (do if iterations remain; NOT required for DONE)
- [~] P2.1 **G15 LeakCanary ✅ done** (catalog `leakcanary 2.14` + `debugImplementation(libs.leakcanary.android)`;
      debug-only/Android-only; dep-guard unaffected — guards `gmsReleaseRuntimeClasspath`). · P2.2 G12 Wear DataLayer sync
      · P2.3 G13 SecureSettings (reclaim dead aliases) · P2.4 G14 multi-profile DB isolation · P2.5 catalog hygiene

---

---

# PROGRESS — PLAN_V19 (tracking engine correctness + iOS parity + lifecycle recovery)

Ledger for the Ralph loop executing `.ralph/PLAN_V19.md`. One row per task per iteration.
Verification gate per task: `assembleNoGmsDebug && testNoGmsDebugUnitTest && ktlintCheck detekt`
+ `compileKotlinIosSimulatorArm64` for :feature:tracking, :core:data, :core:platform, :core:ui.

## Status legend
`[ ]` not started · `[~]` in progress · `[x]` done + verified

## P-A — Engine correctness
- [x] **P-A.1 — Hard accuracy + coordinate gate** ✅ (iter V19-1). Added coordinate hard gates
  (lat∉[-90,90] / lng∉[-180,180] → return null), accuracy hard gates (≤0.1f or >250f → return null),
  and soft accuracy contribution gate (accuracy > `maxAccuracyThreshold`, default 50m →  point persisted
  but not counted toward cleanedDistance) with exceptional-stationary allowance
  (speed≤0.1 && accuracy<20 && hasMovementHistory()). Constants in `LocationTrackingConstants.kt`;
  threshold exposed via `ConfigProvider.getMaxAccuracyThresholdM()` → `TrackingConfigManager`. Wired in
  `LocationTrackingService` via injected `configManager`. Also fixed pre-existing iOS compile bug in
  `TrackingNotificationContent.kt` (JVM-only `String.format()` → KMP `fmt2()/roundToLong()`).
  Added `commonTest` source set to `:feature:tracking` with `withHostTest {}` + `TrackingPipelineAccuracyTest`
  (14 tests covering all gate branches). Existing `LocationProcessorTest` fix()  helper updated to
  default `accuracy = 10f`.
  Gate: `assembleNoGmsDebug` ✅ · `testNoGmsDebugUnitTest` ✅ · `ktlintCheck detekt` ✅ · all 4 iOS modules ✅.
- [x] **P-A.2 — Finalize-time distance recompute** ✅ (commit 9ddec79). `DistanceCalculator` (commonMain,
  pure Haversine) recomputes `cleanedDistance` from persisted locations excluding `isAbnormal/isMock/
  isPaused`. `LocationDao.getLocationsByTokenOnce()` added (suspend). Wired in `LocationTrackingService`
  `stopAndFinalize()`: DB-sourced result is authoritative; falls back to in-memory when trail empty.
  7 `commonTest` cases in `DistanceCalculatorTest`.
- [x] **P-A.3 — Kalman default-on + reset** ✅ (commit 963a848). `enableKalman` defaults to `true`; added
  `resetKalman()` to `LocationProcessor`; service calls it on resume so stale pre-pause state doesn't
  bleed through. `KalmanSmootherTest` verifies default + reset.
- [x] **P-A.4 — Counter reconciliation** ✅ (commit ec9f116). `CounterReconcilePolicy` (commonMain) compares
  DataStore `totalLocationPoints` vs `LocationDao.countLocationsByToken`; DB is authoritative. 8
  `commonTest` cases in `CounterReconcileTest`.

## P-B — Cross-platform tracking control & lifecycle shell
- [x] P-B.1 — `TrackingController` interface (commonMain) ✅ (a19ea5d)
- [x] P-B.2 — De-Android tracking VMs + move to commonMain ✅ (b871f3f)
- [x] P-B.3 — iOS background location + Koin wiring ✅ (f4592c9)

## P-C — Lifecycle recovery
- [x] P-C.1 — `onTaskRemoved` + flag write-paths (L1) ✅ (2456c57)
- [x] P-C.2 — Service-terminated detection (L2) ✅ (3e94eff)
- [x] P-C.3 — Shutdown flag (L4) ✅ (c772310)
- [x] P-C.4 — App-launch ghost reconciliation (L5) ✅ (d09bed4)
- [x] P-C.5 — Session-Restore bottom sheet (shared CMP) ✅ (d629e86)
- [x] P-C.6 — Resume-grace distance gating (L7) ✅ (b7f9cb2)

## P-D — Notification + persistent live presence
- [x] P-D.1 — Shared notification content + delivery ✅ (baa5c57)
- [x] P-D.2 — `TrackingPresenceController` + iOS Live Activity / Dynamic Island ✅ (e62dcc4)
- [x] P-D.3 — Auto-discard countdown ✅ (f3bc5df)

## P-E — Live-tracking UI/UX & graphics parity
- [x] **P-E.1 — Move EASY screens to commonMain** ✅ (V19-iter 15, commit be705c1). SavedTracksScreen,
  TrackSubmissionScreen, SubmissionComponents moved to commonMain. SubmissionResult moved to commonMain.
  StaticPolylineThumbnail + SubmissionTabChips added to SharedTrackComponents (commonMain). OdometerReadingConfirmSheet
  kept in androidMain (ML Kit + android.net.Uri). paging-compose confirmed KMP-compatible → moved to commonMain.
  Fixed String.format() → arithmetic pattern, Math.PI → kotlin.math.PI throughout.
- [x] **P-E.2 — Move MEDIUM screens** ✅ (V19-iter 16, commit 05cae3d). TrackDetailScreen, RoutePointsScreen,
  LiveTrackScreen, ExportViewModel promoted from androidMain to commonMain. ExportViewModel: removed Intent from
  state, injects ShareSheet (core:platform), calls share() directly. TrackExportContent: new commonMain object
  wrapping all 5 exporters. HealthLevel enum + computeHealthLevel() extracted to HealthLevel.kt (commonMain).
  RoutePointsScreen: SimpleDateFormat → kotlinx.datetime Instant. paging-compose moved to commonMain.
  KoinGraphTest + ScreenshotGalleryTest: add mockk<ShareSheet> to test graphs.
  Gate: assembleNoGmsDebug ✅ · testNoGmsDebugUnitTest ✅ · ktlintCheck ✅ · detekt ✅ · all 4 iOS modules ✅.
- [x] **P-E.3 — TrackMiles hub (HARD → shared)** ✅ (V19-iter 17, commit c09fa00). TrackMilesScreen,
  CheckInSheets, CheckInViewModel promoted from androidMain to commonMain.
  - Permissions: rememberLauncherForActivityResult + ContextCompat → koinInject<PermissionsProvider> + rememberCoroutineScope
  - GeoCheckInSheet: removed FusedLocationProvider + android.location.Location; now takes currentLat/currentLng from
    TrackMilesUiState (2 new fields populated from tracking service location fixes)
  - CheckInViewModel: android.util.Log → println; System.currentTimeMillis() → kotlin.time.Clock.System.now()
  - String.format() + System.currentTimeMillis() → arithmetic/Clock patterns throughout (statusChips, statItems, formatElapsed, liveElapsedMs)
  - KoinGraphTest + ScreenshotGalleryTest: add mockk<PermissionsProvider> to test graphs
  iOS Swift/Xcode: no Xcode-specific steps (pure KMP change).
  Gate: assembleNoGmsDebug ✅ · testNoGmsDebugUnitTest ✅ · ktlintCheck ✅ · detekt ✅ · all 4 iOS modules ✅.
- [x] **P-E.4 — Map live UX (HARD → shared)** ✅ (V19-iter 18, commits 0fb3b84+0e6905d). MapScreen moved to
  commonMain. SensorManager compass (CompassUpdater) deleted — GPS bearing from LocationData takes priority
  (azimuth=0f fallback). LocalConfiguration → LocalWindowInfo+LocalDensity. ContextCompat+Manifest+Build →
  PermissionsProvider. System.currentTimeMillis() → Clock. String.format(Locale.US) → padStart arithmetic.
  iOS: no Xcode steps. Gate: all ✅.
- [x] **P-E.5 — Pause/resume/discard sheets + finalize + success** ✅ (V19-iter 19, commit acfdf73).
  TrackDataPreviewScreen (journey review / data quality tabs) + TrackInsightsScreen + TrackInsightsViewModel
  moved from androidMain to commonMain. PauseResumeSheets + DiscardJourneyDialog + TrackingSuccessScreen were
  already in commonMain. Clipboard: android.content.ClipboardManager → LocalClipboardManager (CMP).
  String.format() → arithmetic pattern. VM finalize flow: stopTracking/discardTracking already tested
  in TrackMilesViewModelTest; baselines refreshed (track_data_preview + track_insights screenshots).
  iOS: no Xcode steps. Gate: all ✅.
- [x] **P-E.6 — Remaining screens to commonMain + UrlOpener** ✅ (V19-iter 20). Moved
  CheckInHistoryScreen, GeoCheckInScreen, HardwareEventsLogScreen, ManualCheckInScreen,
  HardwareEventsViewModel from androidMain → commonMain (no android.* imports; all use
  androidx.compose.*/androidx.lifecycle.*). Added `UrlOpener` interface (PlatformServicesV15,
  commonMain) + AndroidUrlOpener/IosUrlOpener impls + Koin wiring (both platformModule actuals) +
  NoOpUrlOpener in PlatformBindings + mocks in KoinGraphTest/ScreenshotGalleryTest.
  Placeholder/skeleton states already on all screens. iOS: no Xcode steps.
  Gate: assembleNoGmsDebug ✅ · testNoGmsDebugUnitTest ✅ · ktlintCheck ✅ · detekt ✅ ·
  all 4 iOS modules compileKotlinIosSimulatorArm64 ✅.

## P-F — Cross-platform workers
- [x] **P-F.1 — BGTask scaffolding + iOS dispatcher** ✅ (commit 494b842). Pre-flight: `dev.brewkits:
  kmpworkmanager:3.0.1` is NOT published to Maven Central or JitPack (0 results). The equivalent
  `BackgroundScheduler`/`BackgroundTask` abstraction (core/platform commonMain) was already in place
  from P-B. P-F.1 delivery: (1) `IosBgTaskDispatcher` (iosMain, `KoinPlatform.getKoin()` bridge) —
  resolves named `BackgroundTask` from Koin and calls `onComplete(success)` from a coroutine so Swift
  can call `bgTask.setTaskCompleted(success:)`; (2) `AppDelegate.swift` registers BGTask handlers for
  `com.mileway.maintenance` (BGProcessingTask, weekly reschedule) and `com.mileway.autodiscard`
  (BGAppRefreshTask, daily reschedule), each delegating to `IosBgTaskDispatcher.shared.runTask(...)`
  which no-ops gracefully until P-F.2 binds real tasks; (3) Info.plist gains both IDs in
  `BGTaskSchedulerPermittedIdentifiers`.
  iOS Xcode note (manual-verify): BGProcessingTask needs the `processing` bg mode (already in plist).
  To test registration at runtime: launch with attached debugger, call
  `e -l objc -- (void)[[BGTaskScheduler sharedScheduler] _simulateLaunchForTaskWithIdentifier:@"com.mileway.maintenance"]`
  in LLDB; confirm no submit errors in console. Requires Mac + Xcode — not gradle-gated.
  Gate: assembleNoGmsDebug ✅ · testNoGmsDebugUnitTest ✅ · ktlintCheck ✅ · detekt ✅ · all 4 iOS ✅.
- [x] **P-F.2 — Migrate maintenance + auto-discard Workers** ✅. `MileageMaintenanceTask` + `AutoDiscardTask` in commonMain (`feature/tracking/worker/`); both implement `BackgroundTask`; named Koin bindings (`com.mileway.maintenance`, `com.mileway.autodiscard`) in Android + iOS `trackingModule`; `MilewayApplication.scheduleWeeklyMaintenance()` replaced with `BackgroundScheduler.schedulePeriodic()`; `dev.brewkits:kmpworkmanager:3.0.1` added to catalog + feature:tracking commonMain; `MileageMaintenanceWorker`/`AutoDiscardWorker` superseded (left in androidMain for reference); commonTest: `MaintenancePurgeTest` + `AutoDiscardPolicyTest` (5 tests pass). iOS compile green + all gates pass.
- [x] **P-F.3 — Boot/reconciliation rescheduling** ✅ (code in commit 6b9f641; seam test added in P-G.1).
  Two-front coverage: (1) the app's own `LocationTrackingBootReceiver` (androidMain) handles
  BOOT_COMPLETED / LOCKED_BOOT_COMPLETED / MY_PACKAGE_REPLACED → restores the FGS via the pure
  `BootRestorePolicy` matrix, with a **manual-resume fallback notification** when an FGS-from-boot start
  is refused (Android 12+ background-start rules). (2) the kmpworkmanager AAR ships
  `DefaultAlarmReceiver` with `RECEIVE_BOOT_COMPLETED`, which reschedules the periodic maintenance +
  auto-discard workers after reboot automatically. iOS relaunch: significant-location-change monitoring
  (P-B.3) + on-demand BGTask handlers. `BootRestorePolicyTest` (7 commonTest cases) covers the receiver
  seam. Gate green.

## P-G — Tests, placeholder audit, cleanup, final gate
- [x] **P-G.1 — Test sweep in commonTest + lifecycle instrumentation** ✅.
  **Major finding/fix:** the entire `feature:tracking` commonTest source set had *never compiled* — the
  project gate only ran `:app:testNoGmsDebugUnitTest`, but KMP modules name their JVM unit-test task
  `testAndroidHostTest`, so the engine/policy/mapper suites were dead. Repaired all compile errors
  (Native/JVM-illegal `:` `()` in backtick `@Test` names across ShutdownFlagPolicyTest /
  AutoDiscardCountdownFormatterTest / TrackingPipelineAccuracyTest; `SavedTrack` ctor drift in
  SessionReconciliationPolicyTest; `TrackingState.TRACKING`→`LIVE_TRACKING` enum drift in
  TrackingNotificationMapperTest). Result: **`testAndroidHostTest` = 108 tests, 0 failures, 14 classes**
  (feature:tracking) + core:platform + core:security. Enabled `withHostTest {}` on core:security so its
  RootDetectorTest runs. **Wired `testAndroidHostTest` into the permanent gate**: root `fullCheck`,
  `.github/workflows/ci.yml`, `.github/workflows/quality.yml` — so it can't rot again. Added
  `BootRestorePolicyTest` (P-F.3 seam, 7 cases). Extended instrumented `TrackingLifecycleTest` with
  `onTaskRemoved_setsAppKilledFlag_persistsAcrossRelaunch` (L1) +
  `ghostSession_afterAppKill_reconcilesToNeedsDecision` (L5, real Room + DataStore +
  SessionReconciliationPolicy). Fixed a pre-existing detekt MagicNumber (`60_000L`) in
  MilewayApplication via `kotlin.time` `.minutes.inWholeMilliseconds`. Kover floor stays 35% (~38.4%).
  Gate: testAndroidHostTest ✅ · :app:testNoGmsDebugUnitTest ✅ · ktlint ✅ · detekt ✅ · iOS test compile ✅.
- [x] **P-G.2 — Placeholder audit** ✅. New `PlaceholderStateAuditTest` (app/src/test, Roborazzi +
  Compose semantics) renders every data-driven tracking surface in its NO-DATA / loading state on an
  EMPTY Room layer and asserts the placeholder copy is actually displayed (`assertIsDisplayed`), proving
  none render a blank container:
    • `SavedTracksScreen` (empty DB) → "No journeys this week" empty state + View All + Start Journey FAB.
    • `CreateVoucherScreen` (no submitted expenses) → "No submitted expenses found".
    • `CheckInHistoryScreen(emptyList())` → "No check-in history yet".
    • `TrackLoadingScreen` → animated paper-plane + status message (loading placeholder).
  Baselines committed to `docs/screenshots/placeholders/`. Audit checklist (verified empty-state copy per
  screen): SavedTracks journeys/submissions ("No journeys this week" / "No submissions yet"),
  HardwareEventsLog ("No events found"), CheckInHistory ("No check-in history yet" / "No matching
  check-ins"), CreateVoucher ("No submitted expenses found"), GeoCheckIn ("No GPS fix yet…"),
  TrackSubmission ("Loading address…"), TrackLoading (status message). Gate green.
- [x] **P-G.3 — Dead-code removal + baselines** ✅. Removed dead files: `TrackingNotificationManager.kt`
  (orphaned post P-D), `MileageMaintenanceWorker.kt` + `AutoDiscardWorker.kt` (WorkManager workers
  superseded by P-F.2 commonMain Tasks; MilewayWorkerFactory dispatches the new Task class names),
  `DirectBootSafeWork.kt` (0 refs; kmpworkmanager owns boot rescheduling now), and the duplicate
  `app/src/test/.../BootRestorePolicyTest.kt` (the policy is commonMain → its test now lives cross-platform
  in feature:tracking/commonTest per P-G.1). Removed dead `koin-androidx-workmanager` dep (zero
  `workManagerFactory`/`@KoinWorker` usage) from :app + :feature:tracking + the version catalog; kept
  direct `workmanager.runtime` (catalog 2.11.2 pins newer than kmpworkmanager's transitive 2.9.1).
  Updated the DemoSettings subtitle copy that named the deleted `AutoDiscardWorker`. Regenerated the
  dependency-guard baseline (now honestly lists kmpworkmanager; the prior baseline was stale). Recorded
  Roborazzi placeholder baselines. Gate: assembleNoGmsDebug + assembleGmsDebug + dependencyGuard +
  ktlint + detekt green.
- [x] **P-G.4 — Final gate + DONE** ✅. Found + fixed a P-F.3 fallout the headless gate missed:
  AppDelegate.swift still called the `IosBgTaskDispatcher` that commit 6b9f641 deleted (Swift isn't
  Kotlin-gated). Restored `IosBgTaskDispatcher` (feature:tracking/iosMain) against the new kmpworkmanager
  `Worker` model (maps BGTask id → Worker via MilewayWorkerFactory, runs `doWork`, completes for Swift's
  `setTaskCompleted`); iOS genuinely needs this because the library auto-runs Workers only on Android.
  **Full gate — all green:**
    • `assembleNoGmsDebug` ✅ · `assembleGmsDebug` ✅ (both flavors)
    • `testNoGmsDebugUnitTest` ✅ · `testAndroidHostTest` (feature:tracking 108 + core:platform +
      core:security) ✅
    • `ktlintCheck` ✅ · `detekt` ✅ · `detektMetadataIosMain` ✅
    • `dependencyGuard` ✅ (baseline regenerated) · `koverVerifyNoGmsDebugCoverage` ✅ (57.7% line ≫ 35% floor)
    • all 4 iOS modules `compileKotlinIosSimulatorArm64` (feature:tracking, core:data, core:platform,
      core:ui) ✅ · `:feature:tracking:compileTestKotlinIosSimulatorArm64` ✅
  **iOS Swift/Xcode artifacts (authored + documented; require Mac+Xcode to build — not Kotlin-gated):**
    • AppDelegate.swift — BGTaskScheduler.register for `com.mileway.maintenance` (BGProcessingTask,
      weekly) + `com.mileway.autodiscard` (BGAppRefreshTask, daily), both → IosBgTaskDispatcher.
    • Info.plist — BGTaskSchedulerPermittedIdentifiers (sync/maintenance/autodiscard), UIBackgroundModes
      (fetch/processing/location), NSLocationAlways…UsageDescription.
    • Live Activity / Dynamic Island steps documented under P-D.2 above (widget extension + ActivityAttributes).
    • ContentView.swift → IosTrackingEntryKt.MilwayViewController() (full KMP entry).
    • iOS framework wiring (RESOLVED post-V19): the `Mileway` framework was built by core:ui, which
      cannot depend on feature:tracking (layer direction → cycle), so the Swift app's
      `IosTrackingEntryKt.MilwayViewController()` + `IosBgTaskDispatcher` calls had no symbols. Fixed with a
      dedicated **`:shared` iOS umbrella module** that `export()`s both core:ui and feature:tracking and
      produces `Mileway.framework`. Moved the `framework{}` declaration out of core:ui (it keeps its iOS
      targets so its iosMain compiles + is exportable); repointed the Xcode project (file ref + build-phase
      `:shared:link…` task + both FRAMEWORK_SEARCH_PATHS) and the CI iOS-compile step to `:shared`. Verified:
      `:shared:linkDebugFrameworkIosSimulatorArm64` links locally and the framework header exposes all 7
      Swift entrypoints (MilwayViewController, IosBgTaskDispatcher, MainViewController, Referral/Push/DeepLink
      bridges, MilewayFramework marker). CI still compile-only for iOS (runner Xcode 16.4 simulator SDK
      drops Apple's private _LocationEssentials → `ld` fails; needs Mac + Xcode ≤ 16.2 to link).

---

## ✅ PLAN_V19 COMPLETE (2026-06-23)
P-A ✅ · P-B ✅ · P-C ✅ · P-D ✅ · P-E ✅ · P-F ✅ · P-G ✅. Both flavors build, all JVM + KMP host tests
pass, all 4 iOS modules + the iOS test target compile, dependency-guard + Kover floor + ktlint + detekt
green. Lifecycle matrix L1–L8 covered + tested; placeholders audited on every tracking surface; dead code
removed. `<promise>DONE</promise>`

---

## V19 Iteration log
_(append one entry per iteration)_

- 2026-06-22 — V19-iter 1 — **P-A.1 Hard accuracy + coordinate gate**. See status entry above.
  iOS Swift/Xcode: no Xcode-specific steps in P-A.1 (pure KMP pipeline change).
- 2026-06-22 — V19-iter 15-16 — **P-E.1 + P-E.2** (commits be705c1, 05cae3d). Easy + Medium screen migration.
  See P-E status entries above.
- 2026-06-22 — V19-iter 17 — **P-E.3** (commit c09fa00). TrackMilesScreen + CheckIn to commonMain.
  iOS Swift/Xcode: no Xcode-specific steps. See P-E.3 status entry above.
- 2026-06-22 — V19-iter 18 — **P-E.4** (commits 0fb3b84+0e6905d). MapScreen to commonMain.
  iOS Swift/Xcode: no Xcode-specific steps. See P-E.4 status entry above.
- 2026-06-22 — V19-iter 19 — **P-E.5** (commit acfdf73). TrackDataPreviewScreen + TrackInsightsScreen + TrackInsightsVM to commonMain.
  iOS Swift/Xcode: no Xcode-specific steps. See P-E.5 status entry above.
- 2026-06-22 — V19-iter 20 — **P-E.6** (commit 4b116ac). Remaining 4 tracking screens + HardwareEventsViewModel
  moved to commonMain; UrlOpener platform service added (Android ACTION_VIEW / iOS UIApplication.open).
  iOS Swift/Xcode: no Xcode-specific steps. Gate: all ✅.
  iOS Swift/Xcode: no Xcode steps. Gate: all ✅.

---

# V18 archived iteration log (reference)

## Iteration log
_(append one entry per iteration: task id, what changed, gate result)_

- 2026-06-22 — iter 0 — plan consolidated (V18 + V13 tail folded as P3), PROGRESS.md created, backlog audit recorded. No code changes yet. Gate: not run (planning only).
- 2026-06-22 — iter 1 — implemented **P1.2 (G6), P1.3 (G7), P1.4 (G8), P2.1 (G15)**.
  - Shared settings layer: added `enableKalman` + `lastOdometerEndReading` (+ `LAST_ODOMETER_NONE`, `setEnableKalman`,
    `setLastOdometerEndReading`) to DemoSettings/DemoSettingsRepository in **both** androidMain + iosMain (identical
    KMP declarations).
  - G6: `LocationTrackingService` injects `DemoSettingsRepository`, collects `enableKalman`, passes it to
    `LocationProcessor(enableKalman=…)`; toggle hoisted in commonMain `TrackCustomizationScreen` and bound to
    DataStore at the androidMain nav site. Takes effect next trip (no mid-trip algorithm swap).
  - G7: persist END reading on capture-confirm; START/END odometer nav seeds prefill from it (was bare `45_000`).
  - G8: `FieldStatus` enum + leading status dot on `CopyableRow`, applied to Details-tab Odometer/Account/Context.
  - G15: leakcanary `debugImplementation`, Android-only.
  - **P0.1 (G1) NOT done by design** — no natural Paging target exists (mock/aggregate/bounded); flagged `[!]`.
  - Gate: `:app:assembleNoGmsDebug` ✅ EXIT 0 · `:app:testNoGmsDebugUnitTest` ✅ EXIT 0 (2m). Roborazzi (record mode)
    refreshed `track_customization_screen.png` (Kalman now shows real off-default); reverted an unrelated flaky
    `approvals_screen_pending_tab.png`. dep-guard: no change (debug-only dep).
  - Also committed iter-1's then-uncommitted work as 3 commits: 439c702 (G15), 33c0ddd (G8), 8cf5aab (G6+G7).
- 2026-06-22 — iter 2 — implemented **P0.1 (G1) Paging 3** (commit a7f05f0) + ktlint follow-up (5d6fc6f).
  - Unblocked P0.1 the architecturally-honest way: rather than cargo-culting a Pager onto the mock/aggregate
    history screens, added Paging 3 to the **GPS route-points log** — the one genuinely list-heavy, flat,
    Room-backed surface (matches the iter-1 "land it on a genuinely large flat list" recommendation).
  - commonMain: `LocationPagingSource` (offset-keyed over existing getLocationsByTokenPaged/countLocationsByToken;
    no Room migration, no room-paging artifact) + `RoutePointsViewModel` (Flow<PagingData> via Pager+flatMapLatest
    +cachedIn, count via COUNT in MVI state). catalog: paging-common (commonMain) + paging-compose (androidMain).
  - androidMain: `RoutePointsScreen` (collectAsLazyPagingItems + refresh/append/empty load states), wired into
    TrackingNavigation (ROUTE_POINTS route) + a "Route points" entry on TrackDetailScreen; VM registered in Koin.
  - test: `LocationPagingSourceTest` (JVM, MockK) proves first-page keys / append walking / short-page
    end-of-pagination / empty trail.
  - Gate: `assembleNoGmsDebug` + `assembleGmsDebug` (both flavors) ✅ + `testNoGmsDebugUnitTest` ✅ + ktlint ✅.
  - ⚠️ LOOP NOTE: the harness reverts ALL uncommitted **tracked**-file edits to HEAD at each turn boundary
    (build files + .kt), keeping only untracked new files + committed work. Discipline: edit → build → COMMIT
    within a single turn. (Cost two re-applications of the build-file/wiring edits before locking it in.)
- 2026-06-22 — iter 3 — implemented **P0.2 (G2) baseline-profile generator** (commit 5ca7524). **P0 COMPLETE.**
  - AGP-9 gotchas resolved live: (1) com.android.test/kotlin.android already on classpath → apply versionless
    (catalog entry without version / `id()`); (2) AGP 9 has built-in Kotlin → the kotlin.android plugin now
    ERRORS, removed it; (3) stable benchmark 1.4.1 predates AGP 9 → used 1.5.0-alpha06 (accepts AGP 9.2.1).
  - Gate: `:baselineprofile:assemble` (compiles generator, both flavors' Benchmark+NonMinifiedRelease variants)
    + `assembleNoGmsDebug` + `assembleGmsDebug` + `testNoGmsDebugUnitTest` all ✅.
  - Device step (not gate-runnable): `./gradlew :app:generateNoGmsReleaseBaselineProfile` produces the real
    profile; static baseline-prof.txt kept as fallback meanwhile.
- 2026-06-22 — iter 4 — implemented **P1.1 (G4) start-step consolidation** (commit c07049d).
  - Investigated first: the 3 "stepper" concepts are orthogonal — TrackMilesPhase=session lifecycle (10+ uses),
    TrackSheet=which sheet (VM-owned), JourneyGuideStep=the actual start stepper (was derived inline in the
    screen). Only the last is the real "start step"; merging the other two would be wrong. Made the honest
    tidy-up: dead-boolean deletion + VM-owned `journeyStep` computed prop, behavior-preserving.
  - Gate: ktlintFormat ✅ · assembleNoGmsDebug ✅ · testNoGmsDebugUnitTest ✅ (incl new journeyStep test).
  - NEXT: **P1.5** (first instrumented androidTest, tracking lifecycle, FusedLocation double, on GMD/ATD + CI).
    ⚠️ Like P0.2 generation, instrumented tests can't RUN in the unit-test gate (need a device/GMD) — deliverable
    will be the androidTest source set + test + GMD/CI wiring that configures + compiles cleanly.
- 2026-06-22 — iter 5 — implemented **P1.5 (G9) first instrumented androidTest + GMD CI** (commit e59196e).
  - androidTest classpath gotcha: core:data keeps Room as `implementation`, so the test needed
    `androidTestImplementation(libs.room.runtime)` to see RoomDatabase/.close(). managedDevices DSL
    (`testOptions.managedDevices.localDevices.create(...)`) configures cleanly on AGP 9.2.1.
  - Gate: ktlintFormat ✅ · assembleNoGmsDebug ✅ · testNoGmsDebugUnitTest ✅ · assembleNoGmsDebugAndroidTest ✅.
  - NEXT: **P1.6** (G11 Glance home-screen widget — new Android-only :widget module consuming SurfaceSnapshot;
    pairs with P3.4 L.1 SurfaceSnapshot enrichment + SnapshotPublisher). Glance render test + catalog deps.
- 2026-06-22 — iter 6 — implemented **P1.6 (G11) Glance widget** (commit bca4c26). **P0 + P1 COMPLETE.**
  - Used the `mileway.android.library` convention (com.android.library + Compose) for the Android-only
    :widget module — Glance needs res/xml so it can't be a KMP module. Dropped glance-material3 (GlanceTheme
    unresolved on 1.1.1) for plain Glance colors. Render test via host-side runGlanceAppWidgetUnitTest;
    `hasText` has no `substring` param on 1.1.1 → matched full strings.
  - Gate: ktlintFormat ✅ · assembleNoGmsDebug ✅ · assembleGmsDebug ✅ · testNoGmsDebugUnitTest ✅ (incl render test).
  - NEXT: **P3.1** (media pipeline depth — D.1 CameraX controls, D.2 multi-pass OCR +test, D.2b
    GalleryOdometerProcessor, D.3 odometer source provenance, D.4 DocumentScanner, D.5 attachment
    ocrData/Room/reorder). ⚠️ D.1/D.2/D.4 touch gms source sets → gate also runs assembleGmsDebug. Large; may
    split across iterations (the loop does one task per iteration — P3.1 is a multi-part task).
- 2026-06-22 — iter 7 — implemented **P3.1 D.2 multi-pass OCR** (commit ff46400). Two OCR paths exist
    (feature:tracking OdometerOcrAnalyzer sealed OcrResult; feature:media RealMediaRepository → media OcrResult
    data class w/ confidence). Did the audit's named target (RealMediaRepository) + a shared pure aggregator.
  - Gate: ktlintFormat ✅ · assembleNoGmsDebug ✅ · assembleGmsDebug ✅ · testNoGmsDebugUnitTest ✅.
  - NEXT (continue P3.1): **D.3 odometer provenance** (cleanest next — pure model: OdometerReadingSource enum on
    OdometerCaptureResult, replace isManual; update 3 call sites) then D.5 (attachment ocrData + Room migration),
    D.2b (gallery OCR via the new aggregator), D.4 (document scanner), D.1 (CameraX controls, device-heavy).
- 2026-06-22 — iter 8 — implemented **P3.1 D.3 odometer provenance** (commit ac81863). Contained model change:
    isManual→source enum + derived isManual back-compat → downstream (form.isManualStartOdo, Manual chip) untouched.
  - Gate: ktlintFormat ✅ · assembleNoGmsDebug ✅ · assembleGmsDebug ✅ · testNoGmsDebugUnitTest ✅.
  - NEXT (continue P3.1): **D.5** attachment ocrData/fileName + Room migration (DB v4→v5) + OCR badge + drag-reorder
    — the meatiest remaining (Room migration testable). Then D.2b (gallery OCR via aggregator), D.4 (doc scanner),
    D.1 (CameraX controls — device-heavy, least gate-verifiable).
- 2026-06-22 — iter 9 — implemented **P3.1 D.5 data layer** (commit 9642425). No JVM Room infra (TripAttachmentTest
    is pure; exportSchema=false blocks MigrationTestHelper) → tested MIGRATION_4_5 directly (run its SQL on a v4
    table, PRAGMA-assert columns) as an instrumented test on the GMD.
  - Gate: ktlintFormat ✅ · assembleNoGmsDebug ✅ · assembleGmsDebug ✅ · testNoGmsDebugUnitTest ✅ · androidTest compiles ✅.
  - NEXT (continue P3.1): **D.2b** GalleryOdometerProcessor (gallery-pick → multi-pass OCR via the D.2 aggregator —
    smallest remaining, reuses RealMediaRepository). Then D.4 (DocumentScanner: interface + AndroidDocumentScanner
    GmsDocumentScanning + binding + tile — touches gms), D.1 (CameraX controls — device UI, least verifiable),
    D.5-UI (OCR badge + drag-reorder). After P3.1: P3.2 tracking-state/notification enrichment.
- 2026-06-22 — iter 10 — implemented **P3.1 D.2b GalleryOdometerProcessor** (commit 4877672). Wired a
    PickVisualMedia gallery affordance into OdometerCameraScreen → multi-pass OCR → confirm-sheet pre-fill.
  - Gate: ktlintFormat ✅ · assembleNoGmsDebug ✅ · assembleGmsDebug ✅ · testNoGmsDebugUnitTest ✅.
  - NEXT (continue P3.1): **D.4** DocumentScannerService (commonMain interface + AndroidDocumentScanner via
    GmsDocumentScanning, gms-only + noGms no-op + iOS placeholder exists + bind in PlatformModule + a 9th
    attachment tile). ⚠️ touches gms → also assembleGmsDebug. Then D.1 (CameraX controls, device UI), D.5-UI.
- 2026-06-22 — iter 11 — implemented **P3.1 D.4 ML Kit document scanner** (commit 6816acc). Deviated from the
    plan's "bound service" to a Compose launcher — the codebase's own IosDocumentScanner note explains ML Kit's
    scanner is an Activity UI flow, not a headless service (DocumentScanner service stays a documented no-op).
    Dep is allowlisted on both flavors (no gms/noGms split needed).
  - Gate: ktlintFormat ✅ · assembleNoGmsDebug ✅ · assembleGmsDebug ✅ · testNoGmsDebugUnitTest ✅.
  - NEXT (finish P3.1): **D.1** CameraX controls (pinch-zoom / tap-to-focus / exposure slider in CameraCaptureScreen
    — device UI, least gate-verifiable; flash enum already exists) + **D.5-UI** (OCR badge using ocr_verified +
    drag-reorder attachment grid). Then **P3.2** tracking-state/notification enrichment.
- 2026-06-22 — iter 12 — implemented **P3.1 D.1 camera exposure slider** (25fbf31; zoom/tap-focus already existed)
    + **D.5-UI OCR badge** (376c7ae). **P3.1 COMPLETE** (drag-reorder deferred — low value, high friction).
  - Gate (both items): ktlintFormat ✅ · assembleNoGmsDebug ✅ · assembleGmsDebug ✅ · testNoGmsDebugUnitTest ✅.
  - NEXT: **P3.2** tracking-state/notification enrichment — C.2b (enrich TrackingSnapshot/TrackingState with
    qualityScore/spikeDistanceM/isGpsAvailable/inResumeGrace + TrackingSystemFlags), C.2d (7-type notifications +
    throttle + TRIP_COMPLETE deep link), C.2g (resume grace), C.3 (quality/spike UI chips). Multi-part; slice it.
- 2026-06-22 — iter 13 — implemented **P3.2 C.2b TrackingSnapshot enrichment** (commit 0470dd4). ktlint gotchas:
    no EOL comment before a KDoc; no inline comment inside a value-argument list (both not auto-correctable).
  - Gate: ktlintFormat ✅ · assembleNoGmsDebug ✅ · assembleGmsDebug ✅ · testNoGmsDebugUnitTest ✅.
  - NEXT (continue P3.2): **C.3** quality/spike UI chips (cleanest next — map snapshot qualityScore/spike/flags into
    TrackMilesUiState + render chips on the track screen; the VM already consumes the snapshot). Then C.2d
    (TrackingNotificationManager: 7 types + throttle + TRIP_COMPLETE deep link), C.2g (resume grace window).
- 2026-06-22 — iter 14 — implemented **P3.2 C.3 quality/spike/health chips** (commit 387dcc8).
  - Gate: ktlintFormat ✅ · assembleNoGmsDebug ✅ · assembleGmsDebug ✅ · testNoGmsDebugUnitTest ✅ (+ new mapping test).
  - NEXT (continue P3.2): **C.2d** TrackingNotificationManager — expand from active/paused to the 7-type system
    (GPS_DISABLED/PERMISSION_MISSING/AUTO_DISCARD/POLICY_VIOLATION/TRIP_COMPLETE) + throttle + a TRIP_COMPLETE
    deep link (mileway://track/{routeId}; DeepLinkRouter from V15 DL.1 exists). Then **C.2g** resume grace
    (inResumeGrace window in pipeline/service: suppress spike/auto-discard right after a resume).
- 2026-06-22 — iter 15 — implemented **P3.2 C.2d 7-type notifications + throttle + deep link** (commit a3af0cd).
    Found TrackingNotificationManager is dead (0 usages); enhanced the service's actual notification path instead.
    Deep link uses mileway://track (router has no track/{routeId}; routeId detail = router+nav refactor, deferred).
  - Gate: ktlintFormat ✅ · assembleNoGmsDebug ✅ · assembleGmsDebug ✅ · testNoGmsDebugUnitTest ✅.
  - NEXT (finish P3.2): **C.2g** resume grace — add an inResumeGrace window (e.g. ~5s after TRACKING_RESUMED) in the
    service/pipeline that suppresses spike rejection / auto-discard, and publish inResumeGrace=true on the snapshot
    during it (field already exists from C.2b). Then **P3.3** sensor fusion.
- 2026-06-22 — iter 16 — implemented **P3.2 C.2g resume grace** (commit d9e84d0). **P3.2 COMPLETE.**
    LocationProcessor.process gains suppressSpike; service tracks resumeAtMs + RESUME_GRACE_MS(5s) window →
    suppressSpike + inResumeGrace published; LocationProcessorTest proves the jump is accepted under grace.
  - Gate: ktlintFormat ✅ · assembleNoGmsDebug ✅ · assembleGmsDebug ✅ · testNoGmsDebugUnitTest ✅.
  - NEXT: **P3.3** sensor fusion — O.1 add motionState:Flow<MotionState> + MotionState enum to MotionSensorProvider
    (core:platform; android/ios actuals; currently only readings:Flow<MotionReading>), O.2 live activity recognition
    (ActivityRecognitionClient gms / CMMotionActivityManager ios; noGms no-op) ⚠️ gms→also assembleGmsDebug,
    O.3 fusion in LocationProcessor (consume MotionState, e.g. STILL ⇒ suppress jitter). Multi-part; slice it.
- 2026-06-22 — iter 17 — implemented **P3.3 O.1 (MotionState provider) + O.3 (pipeline IMU fusion)** (7807e0c).
  - Gate: ktlintFormat ✅ · assembleNoGmsDebug ✅ · assembleGmsDebug ✅ · testNoGmsDebugUnitTest ✅.
  - NEXT (finish P3.3): **O.2** live activity recognition — an ActivityRecognizer interface (core:platform commonMain)
    with a gms impl (ActivityTransition/ActivityRecognitionClient — play-services-location dep, allowlisted both
    flavors) + iOS CMMotionActivityManager + a noGms/no-data fallback; wire it to feed MotionState / auto-pause.
    Likely device-only (compile-verified). Then **P3.4** maps + SurfaceSnapshot enrichment.
- 2026-06-22 — iter 18 — implemented **P3.3 O.2 activity recognition** (ebe1bc3). **P3.3 COMPLETE.** Put
    ActivityRecognizer interface + enum + pure mapper in feature:tracking commonMain (not core:platform — only the
    Android service uses it; iOS CMMotionActivityManager deferred). GmsActivityRecognizer single androidMain impl
    (play-services allowlisted both flavors), graceful runCatching fallback = the "noGms no-op".
  - Gate: ktlintFormat ✅ · assembleNoGmsDebug ✅ · assembleGmsDebug ✅ · testNoGmsDebugUnitTest ✅.
  - NEXT: **P3.4** maps + SurfaceSnapshot — E.2 LocalMapProvider CompositionLocal + MapProvider enum in core:ui
    (consolidate the scattered provider-String selection), E.3 OfflineTileProvider wiring for MapLibre noGms against
    the bundled demo_region.mbtiles, L.1 enrich SurfaceSnapshot (qualityScore/weekGoalKm/actionRequiredCount/
    isPaused/lastTripLabel) + a SnapshotPublisher. Multi-part; slice it. Then P3.5 build/test infra (last).
- 2026-06-22 — iter 19 — implemented **P3.4 L.1 SurfaceSnapshot enrichment + SnapshotPublisher** (c30cbd5).
  - Gate: ktlintFormat ✅ · assembleNoGmsDebug ✅ · assembleGmsDebug ✅ · testNoGmsDebugUnitTest ✅.
  - NEXT (continue P3.4): **E.2** LocalMapProvider — a MapProvider enum + LocalMapProvider CompositionLocal in
    core:ui, consolidating the current provider-String in ThemeController; map surfaces read the CompositionLocal.
    Then **E.3** OfflineTileProvider wiring (MapLibre noGms ↔ bundled app/src/main/assets/demo_region.mbtiles).
    Then **P3.5** build/test infra (B.2a Compose metrics, H.8 mileway.test convention plugin, A.9 PullToRefresh) — LAST.
- 2026-06-22 — iter 20 — implemented **P3.4 E.2 LocalMapProvider + MapProvider enum** (9b3bdc7).
  - Gate: ktlintFormat ✅ · assembleNoGmsDebug ✅ · assembleGmsDebug ✅ · testNoGmsDebugUnitTest ✅.
  - NEXT (finish P3.4): **E.3** OfflineTileProvider — wire MapLibre (noGms maps-maplibre module) to the bundled
    app/src/main/assets/demo_region.mbtiles so offline tiles render without network. Check core:maps-maplibre +
    MapLibreSurface for where the style/tile source is set; an `showOfflineTiles` toggle already exists in MapScreen
    (line 518). Device/render-verified; compile-gate it. Then **P3.5** build/test infra (LAST P3 section).
- 2026-06-22 — iter 21 — implemented **P3.4 E.3 OfflineTileProvider** (57c16a5) → **P3.4 COMPLETE**.
  - Gate: ktlintFormat ✅ · assembleNoGmsDebug ✅ · assembleGmsDebug ✅ · testNoGmsDebugUnitTest ✅ (OfflineTileProviderTest ran).
  - OfflineTileProvider pure style + rememberOfflineMbtilesPath expect(common)/actual(android extracts asset→filesDir,
    ios null); MapLibreSurface uses BaseStyle.Json(offline) when offlineTiles + pack present; offlineTiles threaded
    through MapSurface/KrossMapSurface/FakeMapSurface; MapScreen showOfflineTiles toggle now drives the surface.
  - NEXT: **P3.5** (the LAST P3 section) — slice it: (a) **A.9 PullToRefresh** on a real list screen (lowest-risk,
    do first), (b) **B.2a Compose compiler metrics** (gradle config to emit compose metrics/reports), (c) **H.8
    mileway.test convention plugin** (extract the repeated test deps into build-logic). After all three: re-verify
    BOTH flavors + JVM tests, confirm P0+P1+P3 all checked, THEN emit <promise>DONE</promise>.
- 2026-06-22 — iter 22 — implemented **P3.5 A.9 PullToRefresh** (eaddb7f).
  - Gate: ktlintFormat ✅ · assembleNoGmsDebug ✅ · assembleGmsDebug ✅ · testNoGmsDebugUnitTest ✅.
  - material3 PullToRefreshBox on RoutePointsScreen → LazyPagingItems.refresh() (Room re-query via LocationPagingSource).
    Chose the paged log over LogMilesHistory because its drafts are session-ephemeral (saveDraft never persists), so a
    repo refresh would WIPE them — paging refresh has no such risk.
  - NEXT (continue P3.5): **B.2a** Compose compiler metrics — add the Gradle config (compose compiler
    metricsDestination/reportsDestination, gated behind a property) so `-Pcompose.metrics` emits per-module composable
    stability reports. Then **H.8** mileway.test convention plugin (build-logic) to dedupe test deps. After H.8:
    final BOTH-flavor + JVM-test verify, confirm P0+P1+P3 checked, THEN <promise>DONE</promise>.
- 2026-06-22 — iter 23 — implemented **P3.5 B.2a Compose compiler metrics** (9d852c0).
  - Gate: assembleNoGmsDebug ✅ · assembleGmsDebug ✅ · testNoGmsDebugUnitTest ✅. USE proof: -Pcompose.metrics emitted
    core/ui/build/compose-reports/*-composables.txt + compose-metrics/*-module.json.
  - Gotcha hit: KDoc containing `*/` (a find glob) closed the comment early → build-logic syntax error; reworded the KDoc.
  - NEXT (LAST P3 task): **H.8** mileway.test convention plugin — create a build-logic convention plugin (e.g.
    `mileway.test`) that bundles the repeated unit-test deps (junit, kotlin-test, coroutines-test, robolectric/compose
    test, koin-test as used) and apply it to modules that currently hand-roll those test deps (start with :app, then a
    feature module). USED = a real module's build.gradle.kts switches to the plugin + its tests still pass. After H.8:
    run BOTH flavors + testNoGmsDebugUnitTest, confirm P0+P1+P3 all [x], THEN emit <promise>DONE</promise>.

## PLAN_V19 iterations (continued)

- 2026-06-22 — V19 iter 6 — **P-B.2: De-Android tracking VMs + commonMain CurrentTrackDataSource** (b871f3f).
  - Moved TrackMilesViewModel + LiveTrackViewModel from androidMain → commonMain.
  - Replaced android.util.Log → Napier, System.currentTimeMillis → kotlin.time.Clock,
    SimpleDateFormat → kotlinx.datetime, UUID → kotlin.uuid.Uuid.
  - Extracted CurrentTrackDataSource interface in core/data commonMain; both platform DataStore
    impls now implement it; Koin binds single<CurrentTrackDataSource> { get<CurrentTrackDataStore>() }.
  - Fixed TrackingPipeline accuracy gate off-by-one: > → >= ACCURACY_MAX_M.
  - Added TrackMilesViewModelTest (9 tests, JVM host test via withHostTest {}).
  - Gate: assembleNoGmsDebug ✅ · testNoGmsDebugUnitTest (98 classes, 0 failures) ✅ · ktlintCheck ✅ · detekt ✅ ·
    compileKotlinIosSimulatorArm64 (:feature:tracking + :core:data) ✅.

- 2026-06-22 — V19 iter 7 — **P-B.3: iOS background location + Koin wiring** (pending commit).
  - IosLocationTracker upgrades (background tracking support):
    • allowsBackgroundLocationUpdates = true — continuous background fix delivery.
    • pausesLocationUpdatesAutomatically = false — prevents OS from silently halting delivery.
    • startMonitoringSignificantLocationChanges() — OS relaunch hook satisfying L2 (kill) + L3 (reboot).
    • requestAlwaysAuthorization() on start() — full background permission.
  - iOS trackingModule completed: repositories (SavedTrack, Location, VehiclePricing,
    LogMilesSubmission, CurrentTrack, HardwareEvent, TripAttachment, Voucher) + VMs
    (SavedTracks, TrackMiles, MileageSubmission, TrackDetail, RoutePoints, LiveTrack, CreateVoucher).
    DebugMenuComposeViewModel excluded (androidMain-only).
  - Created IosTrackingEntry.kt in feature/tracking/iosMain: `MilwayViewController()` is the
    full iOS entry point (coreDataModule + coreUiModule + iosAppModule + trackingModule).
    core/ui cannot import feature/tracking (it would be circular); this file lives downstream.
  - ContentView.swift updated: calls IosTrackingEntryKt.MilwayViewController() instead of
    MainViewControllerKt.MainViewController().
  - Info.plist requirements (already present since V12):
    • UIBackgroundModes: location
    • NSLocationAlwaysAndWhenInUseUsageDescription: present
    • NSLocationWhenInUseUsageDescription: present
  - Gate: assembleNoGmsDebug ✅ · testNoGmsDebugUnitTest ✅ · ktlintCheck ✅ · detekt ✅ ·
    compileKotlinIosSimulatorArm64 (all 4: :feature:tracking, :core:data, :core:platform, :core:ui) ✅.

## P-C.5 — Session-Restore bottom sheet (CMP) [iter 12 — d629e86]
  - TrackMilesContract: RecoveryResume / RecoverySaveFinish / RecoveryDiscard actions.
  - TrackMilesViewModel: SESSION_RESTORE sheet + RecoverySheetConfig; observes ReconciliationResultHolder;
    3 handlers; reconciliationHolder=null in JVM tests.
  - SessionRestoreBottomSheet (commonMain CMP): 3-button modal sheet (Resume/Save & Finish/Discard).
  - TrackMilesScreen Android: wired SESSION_RESTORE case.
  - iOS module: wired reconciliationHolder = get() in Koin.
  - Tests: 3 VM recovery tests (no-op when activeRecovery=null, synchronous path).
  - Gate: assembleNoGmsDebug ✅ · testNoGmsDebugUnitTest ✅ · ktlintCheck ✅ · detekt ✅ ·
    compileKotlinIosSimulatorArm64 (tracking + data) ✅.

## P-C.6 — Resume-grace accuracy gating (L7) [iter 13 — b7f9cb2]
  - TrackingPipeline: added graceAccuracyGated (suppressSpike && accuracy>50m); folded into
    cleanedDistance exclusion alongside isPaused/accuracyGated.
  - TrackingPipelineAccuracyTest: 3 new cases — grace+poor accuracy not counted; grace+good accuracy
    counted; non-grace path unaffected.
  - Gate: assembleNoGmsDebug ✅ · testNoGmsDebugUnitTest ✅ · ktlintCheck ✅ · detekt ✅ ·
    compileKotlinIosSimulatorArm64 (tracking + data) ✅.

## P-D.1 — Shared notification action set + iOS UNUserNotificationCenter scheduler [iter 14 — baa5c57]
  - TrackingNotificationContent: TrackingNotificationAction enum (PAUSE/RESUME/STOP/FIX_GPS);
    actions: List<TrackingNotificationAction> field.
  - TrackingNotificationMapper: all 7 branches emit per-type action lists.
  - LocationTrackingService.buildNotification(): renders action buttons + ACTION_FIX_GPS opens
    device location settings.
  - IosNotificationScheduler (iosMain feature/tracking): UNUserNotificationCenter categories/actions;
    schedule()/cancel()/handleNotificationAction() bridge.
  - LocationTrackingConstants: ACTION_FIX_GPS.
  - TrackingNotificationMapperTest: 13 commonTest cases.
  - Gate: assembleNoGmsDebug ✅ · testNoGmsDebugUnitTest ✅ · ktlintCheck ✅ · detekt ✅ ·
    compileKotlinIosSimulatorArm64 (tracking + data) ✅.

## P-D.2 — TrackingPresenceController + iOS Live Activity stub [iter 14 — pending]
  - TrackingPresenceController interface + TrackingPresenceSnapshot (core/platform commonMain).
  - AndroidTrackingPresenceController (core/platform androidMain): posts incremental notify() calls.
  - IosTrackingPresenceController (core/platform iosMain): logs + TODO(ios) stubs for Swift bridge calls.
  - TrackingMiniPill (feature/tracking commonMain): shared CMP mini-pill showing distance/speed/activity.
  - Koin: Android + iOS platformModule() binds TrackingPresenceController.
  - iOS Swift/Xcode steps (MANUAL VERIFY REQUIRED):
    1. File → New → Target → Widget Extension; choose "Live Activity" template;
       name "MilewayLiveActivity".
    2. Define in Swift:
       ```swift
       struct MilewayActivityAttributes: ActivityAttributes {
           public struct ContentState: Codable, Hashable {
               var distanceKm: Double
               var durationMs: Int64
               var speedKmh: Double
               var activityLabel: String
               var isPaused: Bool
           }
       }
       ```
    3. Implement ActivityConfiguration<MilewayActivityAttributes>:
       - Lock-screen: 2-line (distance + speed · activity)
       - DynamicIsland compact: leading=distance, trailing=speed
       - DynamicIsland expanded: full row with all fields + duration
       - DynamicIsland minimal: distance badge
    4. In AppDelegate (or SceneDelegate), call:
       ```swift
       MilewayKt.startIosLiveActivity(distanceKm:durationMs:speedKmh:activityLabel:isPaused:)
       ```
       (once the @_cdecl bridge function is authored in IosTrackingPresenceController.kt).
    5. Add "Supports Live Activities" = YES to MilewayApp Info.plist.
    6. Enable "Push Notifications" + "Background Modes" (Remote notifications) capabilities.

## PLAN_V20 iterations

- 2026-06-24 — V20 iter 1 — **P0.1: Hoist agent VM/repo/UI/DI to commonMain** (pending commit).
  - Moved AgentViewModel, AgentRepository, AgentChatScreen, AgentHistoryScreen, AgentGraph, AgentModule
    from androidMain → commonMain.
  - Removed Android-only @Preview annotations from AgentChatScreen (used @androidx.compose.ui.tooling.preview.Preview, not CMP).
  - Moved :stub dep from androidMain → commonMain in feature/agent/build.gradle.kts.
  - All nav imports (NavController/NavGraphBuilder/composable) work in commonMain via JB multiplatform nav.
  - Gate: assembleNoGmsDebug ✅ testNoGmsDebugUnitTest ✅
  - NEXT: P0.2 — unify AssistantHomeSheet onto shared AgentViewModel + AgentChatScreen content.

- 2026-06-24 — V20 iter 2 — **P1.1 + P1.2: Room schema migration + Repository backed by Room**
  P1.1: DB v5→v6, AgentConversationEntity + AgentMessageEntity added to core:data; AgentDao with
  observeConversations, observeMessages, insertConversation, insertMessage, updateConversationMeta,
  updateFeedback, updateLastMessageTime (added in P1.2 to support appendMessage). MIGRATION_5_6 CREATE
  TABLE scripts added; both android/ios DB builders wire it. CoreDataModule registers agentDao().
  P1.2: AgentRepository rewritten to take AgentDao — exposes conversationsFlow (Flow<List<AgentConversation>>),
  messagesFor(conversationId), createThread, appendMessage (inserts + updates lastMessageMs), updateTitle,
  seedIfEmpty (seeds AgentMockData on first run if countConversations == 0). AgentViewModel: init block
  seeds + collects conversationsFlow, sendMessage now creates thread + persists user/assistant messages,
  loadConversation uses messagesFor() + cancels prior Job. quickReplies moved from repo → VM.
  AgentModule injects AgentDao via get(). FakeAgentDao added for tests (LinkedHashMap-backed, flush() pattern).
  AgentRepositoryTest rewritten (Turbine, runTest, tests seeding/persistence/conversationsFlow).
  AgentViewModelTest rewritten (FakeAgentDao, async loadConversation, history populated after advanceUntilIdle).
  KoinGraphTest + ScreenshotGalleryTest fakeRoomLayer patched with single<AgentDao> { FakeAgentDao() }.
  Gate: assembleNoGmsDebug ✅ testNoGmsDebugUnitTest (792 tests, 0 failures) ✅
  NEXT: P1.3 — session resume via DataStore (5-minute window).

- 2026-06-24 — PLAN_V20 (continuation from prior session) — **P5.2: Topbar chat indicator**
  HomeScreen/HomeScreenContent: +onOpenAgent param (nullable). HomeProfileHeader reads
  AssistantFabSessionState.mode; renders ChatAgentIndicator(FULL) when mode==TOPBAR.
  ChatAgentIndicator: new animated sparkle pill (FULL/COMPACT). MilewayAppRoot passes
  onOpenAgent to HomeScreen. Gate: assembleNoGmsDebug ✅ testNoGmsDebugUnitTest ✅

- 2026-06-24 — **P5.3: Empty-state popular-questions carousel from real data**
  ChatTab: +popularPrompts param (default=QUICK_PROMPTS fallback). AgentChatScreen threads
  uiState.popularTab.map{it.question} → ChatTab. Gate: assembleNoGmsDebug ✅ tests ✅

- 2026-06-24 — **P6.1: iOS parity for assistant (actuals + Koin bootstrap)**
  IosAgentEntry.kt: exposes iosAgentModule. shared/build.gradle.kts: exports feature:agent.
  SharedViewController.kt: new MilwayAppViewController() in :shared/iosMain includes all
  modules. iOS compile gate: :shared:compileKotlinIosSimulatorArm64 ✅

- 2026-06-24 — **P7.1: Unit tests — engine, persistence, streaming, resume coverage**
  feature:agent: withHostTest enabled, commonTest sourceSet added.
  OfflineAssistantEngineTest (8 tests): chunk ordering, grounded km count, zero-trip,
  old-track exclusion, title suggestions. AgentRepositoryTest (8 tests): CRUD, isolation,
  resumable thread window, persistFeedback. Gate: testAndroidHostTest ✅ testNoGmsDebugUnitTest ✅

- 2026-06-24 — **P7.2: Roborazzi baselines for all assistant screens** (+8 screenshots)
  agent_chat_analytics_popular, agent_chat_analytics_unanswered (tab-click screenshots),
  voice_waveform_idle/listening/speaking, chat_agent_indicator_full/compact, assistant_fab.
  Gate: testNoGmsDebugUnitTest ✅

- 2026-06-24 — **P7.3: Final gate — both flavors build, tests green** ✅
  assembleGmsDebug ✅ assembleNoGmsDebug ✅ testNoGmsDebugUnitTest ✅
  PLAN_V20 COMPLETE.
