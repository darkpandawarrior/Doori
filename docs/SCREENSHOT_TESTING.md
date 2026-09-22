# Screenshot testing — the house pattern

This is the reference implementation for the family. Mileway has 372 committed PNGs across four
surfaces and two CI gates; every other repo that wants visual regression testing should copy from
here rather than rediscover it. Everything below was read out of this repo on 2026-09-22, and the
claims marked **measured** were run, not inferred.

If you only read one thing: **recording is an environment variable, `ROBORAZZI_RECORD=true`, not
`-Proborazzi.test.record=true`.** See [The record flag](#the-record-flag-the-trap-that-cost-weeks).

---

## What is actually here

| Surface | Task | Test sources | Notes |
|---|---|---|---|
| `:app` | `:app:screenshotTestNoGmsDebug` | `app/src/test/java/com/mileway/Screenshot*Test.kt` | 4 classes, the bulk of the PNGs. Forked into its own JVM. |
| `:wear` | `:wear:testNoGmsDebugUnitTest` | `WearScreenshotGalleryTest.kt` | Ordinary unit-test task, no fork. |
| `:widget` | `:widget:testDebugUnitTest` | `WidgetScreenshotTest.kt` | Glance/RemoteViews host render. |
| `:desktopApp` | `:desktopApp:desktopTest` | `DesktopScreenshotGalleryTest.kt` | Not Roborazzi — Compose Desktop + `ImageIO`. |
| all of the above | `./gradlew screenshotTest` | — | Aggregator, root `build.gradle.kts`. |

Output goes to `docs/screenshots/`, set once in `roborazzi.properties` at the repo root:

```properties
roborazzi.output.dir=../docs/screenshots   # relative to the :app module dir
roborazzi.test.verify=true                 # see below — this line is load-bearing
```

`roborazzi.test.verify=true` is not decoration. Roborazzi's **default mode is record**, so a repo
that simply never passes a record flag still overwrites its baseline on every run and the suite
passes no matter what changed. This was proven here on 2026-08-09 by corrupting `widget_glance.png`
and re-running: `BUILD SUCCESSFUL`. Verify has to be turned on explicitly; recording is then the
deliberate act.

Not covered, and deliberately not faked as if it were: iOS and watchOS (Swift snapshot tests, need
Xcode — Gradle cannot run them) and `:app-web-preview` (no test source set).

---

## The record flag: the trap that cost weeks

```bash
./gradlew screenshotTest                          # verify against the committed PNGs
ROBORAZZI_RECORD=true ./gradlew screenshotTest    # re-record them
```

**Measured 2026-09-22.** `docs/screenshots/coreui_statusChip_tones.png` was deleted, then
`:app:screenshotTestNoGmsDebug --tests "…coreui_statusChip_tones" -Proborazzi.test.record=true` was
run with `ROBORAZZI_RECORD` unset. The test executed (`tests="1"` in the JUnit XML), the build
reported `BUILD SUCCESSFUL`, and the golden was **not** recreated.

Why, structurally: the Roborazzi Gradle plugin injects `roborazzi.test.*` as system properties onto
the **per-variant unit-test tasks it knows about**. `screenshotTestNoGmsDebug` is a hand-registered
`Test` task that copies `systemProperties` from `testNoGmsDebugUnitTest` inside `afterEvaluate`, so
it never sees them. A Gradle property does not cross into a forked test JVM on its own.

The fix every capture site in this repo already uses — copy this verbatim:

```kotlin
private fun capture(name: String) {
    // Env var, not -P: a Gradle property does not reach a forked test JVM.
    if (System.getenv("ROBORAZZI_RECORD") == "true") {
        System.setProperty("roborazzi.test.record", "true")
    }
    composeRule.onRoot().captureRoboImage(File(screenshotsDir, "$name.png").absolutePath)
}
```

**Second measured consequence: a deleted golden passes verify.** Roborazzi has no baseline to
compare against, so it reports success. Roborazzi alone therefore cannot tell you a capture was
removed. That hole is covered by the second gate, below — which is the reason there are two.

---

## Two gates, and why one is not enough

### `quality.yml` — the crash gate, on every PR

Runs `screenshotTest`. Proves every surface still **composes and renders**. It does not compare
pixels. A screen that throws at composition fails here.

This is the gate that a 2026-08-09 regression slipped past when it did not exist: injecting
`SystemSettingsOpener` into `TrackMilesScreen` broke all 155 `:app` captures at composition while
`assembleNoGmsDebug` and `testNoGmsDebugUnitTest` stayed green throughout — because `:app`'s
screenshot suite is deliberately forked out of the main one. Two suites existed; only one was
watched.

### `design-sentinel.yml` — the pixel and integrity gate, nightly *and* on PR

```yaml
on:
  schedule: [{ cron: '17 3 * * *' }]   # a check that only runs on demand is a check that stops running
  pull_request:
  workflow_dispatch:
permissions:
  contents: read                        # never write: a workflow that rewrites baselines destroys the evidence
```

It re-records, then runs `scripts/design-sentinel.mjs --no-ai`, which fails when a capture is
**blank** (decoded with Pillow — a blank tile is worse than a missing one, it looks covered and is
not) or **removed** (`regressed = broken.length > 0 || removed.length > 0`). That removed-capture
check is what closes the deleted-golden hole Roborazzi leaves open.

`permissions: contents: read` is the important line to copy. Recording and verifying must never
happen in the same job that can commit: a workflow that auto-commits regenerated baselines on the
same run that verifies them masks a real regression by rewriting the evidence out from under
itself. Baseline refresh is a separate workflow (`screenshots.yml`, push to main) that opens a PR
rather than pushing.

---

## The five gotchas that are not obvious from Roborazzi's README

1. **Pin the device qualifiers.** Without them Robolectric uses environment defaults and ~110
   baselines re-record nondeterministically on every run.
   ```kotlin
   @Config(sdk = [33], application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
   @GraphicsMode(GraphicsMode.Mode.NATIVE)
   ```

2. **Fork the screenshot suite away from the assertion suite.** `@GraphicsMode(NATIVE)` is real
   Skia; sharing a fork with the ordinary unit tests corrupted native teardown. `:app` registers
   `screenshotTestNoGmsDebug`, excludes `com.mileway.Screenshot*Test` from
   `testNoGmsDebugUnitTest`, and sets `forkEvery(1)`.

3. **Pre-load ByteBuddy's agent on that fork only.** MockK self-attaches at runtime; modern JDKs
   restrict that, so it spawns an external attach process that loses a race whenever several Gradle
   test JVMs run at once — the flake that failed the aggregate task roughly two runs in three.
   `jvmArgs("-javaagent:${mockkAgent.singleFile.absolutePath}")` on the screenshot task fixes it.
   Applying it to *every* test task instead breaks the main suite with "class redefinition failed",
   because the pre-loaded agent conflicts with MockK's inline instrumentation across `forkEvery`
   restarts. Narrow it deliberately.

4. **Make image loading synchronous.** Coil3 fetches and decodes on `Dispatchers.IO`, a pool
   Robolectric's compose-idle check does not track, so a capture races the decode and flakes to a
   blank hero image. Install a deterministic loader in `@Before`:
   ```kotlin
   SingletonImageLoader.setUnsafe(
       ImageLoader.Builder(context).coroutineContext(Dispatchers.Unconfined).build(),
   )
   ```

5. **Compose Multiplatform resources need a Context under Robolectric.** Previews reading
   `Res.string.*` throw `MissingResourceException` because the auto-init ContentProvider does not
   run. Call `setResourceReaderAndroidContext(ApplicationProvider.getApplicationContext())` in a
   per-test `@Before` (not `@BeforeClass` — Robolectric's per-test environment must be ready first).

Also worth knowing: bump `roborazzi` and `robolectric` **together, never apart**. Roborazzi renders
through Robolectric, so a pixel diff is unattributable if both moved in one commit. The version
catalog says so at the pin.

---

## Discovery, not a hardcoded module list

The aggregator does not name its modules. It walks each subproject's `src/` and pulls in any module
whose test sources call `captureRoboImage` **or** `ImageIO`:

```kotlin
val hasCaptures = sub.projectDir.resolve("src").walkTopDown()
    .filter { it.isFile && it.extension == "kt" }
    .any { f -> f.readText().let { it.contains("captureRoboImage") || it.contains("ImageIO") } }
```

Matching on what a module *does* rather than which library it uses is why `:desktopApp`'s
ImageIO-based harness is covered without a second mechanism. A hardcoded list is a list that
drifts, and a new screenshot suite is then covered the day it is written rather than the day
someone remembers this file.

Sequencing matters too: the discovered tasks get `mustRunAfter(":app:screenshotTestNoGmsDebug")`,
because `:app`'s NATIVE-Skia fork reproducibly dies when it shares a build with other test JVMs.

---

## Where the previews come in

Previews are the input to the catalog, not a separate exercise. `ScreenshotCatalogTest` renders
`@Preview` composables from the feature modules, so a preview written for the IDE becomes a gated
PNG for free. Two things to know before copying:

- **The annotation is `androidx.compose.ui.tooling.preview.Preview`, in `commonMain`.** Since
  Compose Multiplatform 1.10 the AndroidX annotation *is* the multiplatform one, shipped by
  `org.jetbrains.compose.ui:ui-tooling-preview`. `org.jetbrains.compose.ui.tooling.preview.Preview`
  is the deprecated one. Every preview in this repo already uses the androidx FQN, and the detekt
  `ignoreAnnotated` list naming only that FQN is correct.
- **The catalog's preview list is hand-maintained on purpose.** `roborazzi-compose-preview-scanner-support`
  is on the classpath but nothing switches it on — there is no `generateComposePreviewRobolectricTests`
  block anywhere in the repo. Autodiscovery would gate *every* preview rather than the curated set,
  and would rename the goldens. If you turn it on, do it in the same change as the re-record, not
  as a drive-by.

Adding a preview to the gate is three lines:

```kotlin
@Test
fun coreui_statusChip_tones() {
    composeRule.setContent { PreviewStatusChipTones() }
    capture("coreui_statusChip_tones")
}
```

Record just that one without churning the other 371:

```bash
ROBORAZZI_RECORD=true ./gradlew :app:screenshotTestNoGmsDebug \
  --tests "com.mileway.ScreenshotCatalogTest.coreui_statusChip_tones"
```

Then **open the PNG**. A recorded golden that nobody looked at is a regression laundered into an
approval, and that is the failure mode that makes a screenshot suite worth less than no suite.

---

## Copying this to another repo

Minimum viable port, in order:

1. `roborazzi.properties` at the repo root, with `roborazzi.test.verify=true`.
2. `alias(libs.plugins.roborazzi)` on every module that captures — the plugin, not just
   `roborazzi-core`. `:widget` had the library without the plugin for months and its
   verify/record switch simply did not exist.
3. The `ROBORAZZI_RECORD` env-var `capture()` helper above. Do not use `-P`.
4. Device qualifiers pinned on every screenshot class.
5. One CI job in verify mode on PRs. Stop there if that is all the repo warrants.
6. Only then the sentinel: blank-capture and removed-capture detection, `contents: read`, and a
   separate recording workflow that opens a PR.

Steps 1–4 are the ones without which the suite silently does nothing. Steps 5–6 are what make it a
gate rather than a gallery.

For a Kotlin Multiplatform design-system module with no Android target, none of this applies as
written — Roborazzi renders through Robolectric, which is Android-only. The path there is
`roborazzi-compose-desktop-preview-scanner-support` against a `jvm()` target, and its goldens are a
separate corpus: desktop renders through the host OS's Skia font stack, Robolectric through the
Android graphics stack, so the same preview yields different images and the two sets can never be
cross-checked.
