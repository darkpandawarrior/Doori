# Artefact Index

Every generated artefact in this repo, in one place: what it is, where it lands, the exact command
that regenerates it, what it depends on, and how you'd notice it went stale. Written 2026-08-09 as
part of the Milestone Release Program's Wave 0 ("make failure loud") — its absence is the reason
four working screenshot harnesses sat unaggregated, an iOS capture path silently wrote to a phantom
folder for months, and a broken desktop compile went unseen. See the root cause note at the top of
this section before reading the tables.

**How to read this file:** every command below was confirmed to exist by actually running
`./gradlew <module>:tasks --all` (or, for iOS, `xcodebuild -list`) against this repo — not copied
from a comment or a plan. Where I ran the command itself rather than just confirming it exists,
that's called out explicitly. This repo has other agents editing it concurrently (the Milestone
Release Program runs several waves in parallel), so treat any *count* here (module counts, PNG
counts, screen counts) as illustrative, not authoritative — `settings.gradle.kts` and the live
`docs/screenshots/` directory are the source of truth for counts, this file is the source of truth
for *mechanism*.

## The root cause this file exists to fix

Work existed; nothing ran it; nothing alerted. Four screenshot harnesses (app, wear, widget,
desktop) each worked in isolation, on four different task names, and no single command ran them
together — a change could break three of them while the one gate everyone watched
(`assembleNoGmsDebug` + `testNoGmsDebugUnitTest`) stayed green. Separately, the iOS capture code
hardcoded an absolute path from before the repo moved to `Repos/Android/Mileway`; because the
writer called `createDirectory(withIntermediateDirectories: true)`, every run silently *created*
the stale phantom folder and wrote PNGs into it — zero errors, for months, because nothing checked
where the output landed. An index that just lists what exists today rots the same way the
screenshots did. An index that says *how to regenerate it* and *how you'd know it's wrong* doesn't
— that's the difference this file is trying to be.

---

## Quick reference

| Artefact | Lives at | Regenerate with | Depends on | Staleness signal |
|---|---|---|---|---|
| App screenshots (Roborazzi) | `docs/screenshots/*.png` | `./gradlew :app:screenshotTestNoGmsDebug` | `com.mileway.Screenshot*Test` sources in `app/src/test`, `roborazzi.properties` | Pixel diff fails the task if code changed the render; `screenshotFreshnessCheck` fails if untouched >30d |
| Wear screenshots (Roborazzi) | `docs/screenshots/wear_*.png` | `./gradlew :wear:testNoGmsDebugUnitTest` | `WearScreenshotGalleryTest.kt` (+ other `captureRoboImage` call sites in `wear/src/test`) | Same two signals as app |
| Widget screenshots (Roborazzi) | `docs/screenshots/widget_*.png` | `./gradlew :widget:testDebugUnitTest` | `WidgetScreenshotTest.kt` | `screenshotFreshnessCheck` only — see caveat below |
| Desktop screenshots (ImageIO) | `docs/screenshots/desktop_*.png` | `./gradlew :desktopApp:desktopTest` | `DesktopScreenshotGalleryTest.kt`, `DesktopDashboardScreenshotTest.kt` | `screenshotFreshnessCheck` only — no pixel-diff gate at all, see caveat |
| Web preview screenshots (ImageIO) | `docs/screenshots/web_*.png` | `./gradlew :app-web-preview:screenshotTest` | `WebPreviewScreenshotTest.kt` | `screenshotFreshnessCheck` only, **and it isn't even wired into the aggregate task below yet** |
| iOS widget screenshots (XCTest/ImageRenderer) | `docs/screenshots/widget_ios_*.png` | `xcodebuild test -scheme MilewayWidgetsTests -sdk iphonesimulator -only-testing:MilewayWidgetsTests/WidgetScreenshotTests` (from `iosApp/`) | `WidgetScreenshotTests.swift`; needs `:shared:linkDebugFrameworkIosSimulatorArm64` built first | Nothing automated — no CI job runs this yet (see CI section) |
| watchOS screenshots (XCTest/ImageRenderer) | `docs/screenshots/watch_*.png` | `xcodebuild test -scheme MilewayWatch -sdk watchsimulator -destination '<sim id>' -only-testing:MilewayWatchTests/WatchScreenshotTests` (from `iosApp/`) | `WatchScreenshotTests.swift` | Same — nothing automated |
| One-command aggregate (app+wear+widget, auto-discovered) | n/a (runs the above) | `./gradlew screenshotTest` | root `build.gradle.kts`'s `screenshotTest` task | The task itself fails if any wired module fails |
| Flow GIFs | `docs/gifs/*.gif`, `docs/demo/*.gif`, `docs/assets/banner.gif` | `scripts/build-flow-gifs.sh <name> <frame1> <frame2> ...` per GIF | The PNGs above must exist and be current; `ffmpeg`/`ffprobe` at `/opt/homebrew/bin/` | Nothing automated — see caveat, this is a real gap |
| Freshness gate | n/a (a check, not a file) | `./gradlew screenshotFreshnessCheck` | `git log` dates on every `docs/screenshots/*.png` | Fails the build itself when it fires |
| Everything-must-be-green release gate | n/a | `./gradlew verifyAll` | `fullCheck` + `screenshotFreshnessCheck` + `:app:assembleNoGmsDebug` + `:app:dependencyGuard` | Fails the build itself |
| README fact spans | `README.md` (inside `<!-- AUTOGEN:x -->` markers) | `./scripts/gen-readme.sh` | `settings.gradle.kts`, `docs/screenshots/`, `core/data/.../MilewayDatabase.kt` | `.github/workflows/readme.yml` fails a PR if regenerating produces a diff |
| Versioning triple (FINGERPRINT/MARKETING/BUILDCODE) | Computed at configure time, not written to a file | `./gradlew -q :app:printFingerprint` / `:app:printMarketing` | `MILESTONE` file + `git rev-list --count HEAD` + today's date, via `gradle/versioning.gradle.kts` | Every build recomputes it live — cannot go stale, only `MILESTONE` can go un-bumped |
| Play/App Store changelogs | `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` | Any `fastlane android deploy_*` lane | git commit log since last tag | Regenerated on every deploy lane run; stale only if the lane hasn't run |
| F-Droid metadata | `metadata/com.mileway.fdroid.yml` | Hand-maintained, submitted to `fdroiddata` per release | — | Not auto-checked; see `docs/RELEASE.md` §4 |
| Code knowledge graph | `graphify-out/` (gitignored) | Owned by the `codegraph` AgentHarness skill, not a repo Gradle task | Repo source, rebuilt on demand | Out of this repo's scope — flagged so it isn't mistaken for a Mileway artefact |

---

## Screenshot surfaces, in detail

### App — `:app:screenshotTestNoGmsDebug`

Roborazzi captures under `app/src/test/java/com/mileway/` matching the class-name filter
`com.mileway.Screenshot*Test` (`app/build.gradle.kts`'s `screenshotTestFilter`). This suite is
forked out of the main `testNoGmsDebugUnitTest` task into its own JVM (`screenshotTestNoGmsDebug`)
because it runs `@GraphicsMode(NATIVE)` (real Skia rendering), and sharing a fork with the
assertion-based unit tests corrupted native teardown (`Z.5b`, documented at length in
`app/build.gradle.kts`). Output lands wherever `roborazzi.properties` at the repo root points —
today `../docs/screenshots` resolved relative to `:app`'s module directory, i.e. `docs/screenshots`
at the repo root.

Verify vs. record:
```bash
./gradlew :app:screenshotTestNoGmsDebug                              # compares against the committed PNGs, fails on pixel diff
./gradlew :app:screenshotTestNoGmsDebug -Proborazzi.test.record=true # overwrites the committed PNGs
```
This is real, verified Roborazzi Gradle-plugin behavior (`:app` applies `alias(libs.plugins.roborazzi)`), not an assumption.

### Wear — `:wear:testNoGmsDebugUnitTest`

`wear/src/test/kotlin/com/mileway/wear/WearScreenshotGalleryTest.kt` (and any other file calling
`captureRoboImage`) runs inside the module's normal unit-test task — unlike `:app`, there is no
separate forked screenshot task for `:wear` today. `:wear` also applies the `roborazzi` Gradle
plugin, so the same `-Proborazzi.test.record=true` flag applies. Output path resolves the same way
as `:app`'s (`../docs/screenshots` from the module dir lands at the repo root, since `:wear` is
also a top-level module).

### Widget — `:widget:testDebugUnitTest`

`widget/src/test/kotlin/com/mileway/widget/WidgetScreenshotTest.kt` calls `captureRoboImage`
directly, but **`:widget`'s `build.gradle.kts` does not apply the `roborazzi` Gradle plugin** — it
only pulls in `libs.roborazzi.core` as a raw test dependency. Verified by reading the file: no
`alias(libs.plugins.roborazzi)` line. That means `-Proborazzi.test.record=true` is not wired for
this module the way it is for `:app`/`:wear` — regenerating widget PNGs means running the plain
unit-test task and relying on Roborazzi's own library-default behavior, not a project-configured
verify/record switch. This is the kind of asymmetry that's easy to miss by reading only `:app`'s
comments and assuming the other modules work the same way.

### Desktop — `:desktopApp:desktopTest`

`DesktopScreenshotGalleryTest.kt` and `DesktopDashboardScreenshotTest.kt` render via Compose
Desktop's `runDesktopComposeUiTest` and write straight to `docs/screenshots/desktop_*.png` with
plain `javax.imageio.ImageIO.write(...)` — no Roborazzi, no baseline comparison of any kind.
`repoRoot()` (in `DesktopDashboardScreenshotTest.kt`, shared with the gallery test) derives the
path from `System.getProperty("user.dir")`, walking up one level if the working directory is named
`desktopApp` — no hardcoded absolute path.

**Caveat worth naming plainly: this surface has no automatic regression detection.** Every run of
`:desktopApp:desktopTest` unconditionally overwrites the PNGs and the task passes regardless of
what changed — there is no pixel diff to fail on. The only thing that will ever flag a stale
desktop screenshot is `screenshotFreshnessCheck`'s git-age check, and that only proves the file
*exists and was touched recently*, not that it matches current code. This is a live instance of
"a target compiled by no gate" — the desktop target compiles and the test runs, but nothing asserts
the *rendered output* is correct.

### Web preview — `:app-web-preview:screenshotTest`

`app-web-preview/src/screenshotTest/kotlin/com/mileway/webpreview/WebPreviewScreenshotTest.kt`
runs on a JVM-only Kotlin target named `screenshot` (Compose Multiplatform's JVM screenshot-testing
source set — confirmed via `./gradlew :app-web-preview:tasks --all`, which lists a real
`screenshotTest` task alongside `wasmJsBrowserTest`). It renders the same `commonMain` composables
the wasmJs target ships and writes `web_*.png` via `ImageIO`, same no-diff caveat as desktop above.
**This does not prove the wasm binary runs in a browser** — the test class's own doc comment says
so; a real wasm-in-browser check would need Playwright against the built distribution, which does
not exist today.

**Verified gap, found while writing this file, not copied from a plan:** the root `screenshotTest`
aggregate task (below) discovers modules to pull in by matching task names against
`desktopTest` or `test*UnitTest` (see the `gradle.projectsEvaluated` block in root
`build.gradle.kts`). `:app-web-preview`'s task is literally named `screenshotTest`, which matches
neither pattern — so **running `./gradlew screenshotTest` today does not run the web preview
captures**, even though the harness itself works and was previously believed missing. The root
`build.gradle.kts` comment block above the task still says "wasm (`:app-web-preview`) — no test
source set" — that comment is now stale; the source set exists, it's just not wired in. This is the
exact "harness exists but nothing runs it" failure class the whole program is about, still live in
one place as of this writing.

### iOS phone widgets — `MilewayWidgetsTests`

```bash
cd iosApp
xcodebuild test -scheme MilewayWidgetsTests -sdk iphonesimulator \
  -only-testing:MilewayWidgetsTests/WidgetScreenshotTests
```
Confirmed this scheme exists by running `xcodebuild -list -project iosApp/iosApp.xcodeproj`
(schemes: `iosApp`, `MilewayWatch`, `MilewayWidgets`, `MilewayWidgetsTests`) — I did not run the
test itself; per the Milestone Release Program's Wave 1.2 note it needs
`:shared:linkDebugFrameworkIosSimulatorArm64` built first so the test bundle can resolve the
`MilewayWidgets` module. `WidgetScreenshotTests.swift` renders WidgetKit views via `ImageRenderer`
(fixed layout, no home-screen placement needed) and writes to
`$SCREENSHOT_OUT_DIR` if set, else a fallback path. That fallback is currently the *correct*
absolute path (`/Users/darkpandawarrior/Repos/Android/Mileway/docs/screenshots`) — the stale
`/Repos/Mileway/...` path from before the repo moved has already been fixed — but see the guard-test
caveat below, because that fallback string is itself a `/Users/...` literal.

### watchOS — `MilewayWatchTests`

```bash
cd iosApp
xcodebuild test -scheme MilewayWatch -sdk watchsimulator \
  -destination 'platform=watchOS Simulator,name=<sim name>' \
  -only-testing:MilewayWatchTests/WatchScreenshotTests
```
Note the scheme is `MilewayWatch`, not `MilewayWatchTests` — there is no separate test-only scheme
(`xcodebuild -list` confirms `MilewayWatchTests` is a *target*, not a *scheme*; the test target is
run through the `MilewayWatch` app scheme's test action). `-destination` needs a concrete simulator
— list one with `xcrun simctl list devices available | grep -i watch`. Same
`SCREENSHOT_OUT_DIR`-or-fallback pattern as the widget tests, same "not run by CI yet" status.

### Verified guard on the fallback paths themselves — currently RED

`app/src/test/java/com/mileway/OutputPathGuardTest.kt` scans all `.kt`/`.kts`/`.swift`/`.java`
source for `/Users/[^"'\s]+` and fails if it finds one outside itself — the mechanical guard against
the exact phantom-directory bug described at the top of this file (root cause item 0.4 in the
Milestone Release Program). I ran `./gradlew :app:testNoGmsDebugUnitTest --tests
"com.mileway.OutputPathGuardTest"` while writing this file: **it currently fails to compile**
(`createTempDir()` is deprecated and this repo treats that as a compile error), which is a
transient, in-flight state from a concurrently-editing wave, not something I touched or fixed —
file ownership for this task is docs-only. Two things worth knowing regardless of when that
compiles again: (1) the two iOS Swift files' `SCREENSHOT_OUT_DIR ?? "/Users/darkpandawarrior/..."`
fallback literals are real `/Users/` strings in tracked source, so once this test compiles again it
is very likely to fire against exactly the files it exists to protect — that's either an intentional
carve-out that hasn't been added yet, or a real conflict between two Wave 0/1 changes; and (2) this
is exactly why this index states "verify by running," not "verify by reading a comment" — the
comment in both `.swift` files says the phantom-path bug is fixed, and the *content* is fixed, but
the guard meant to enforce it wasn't (as of this snapshot) even compiling.

---

## The aggregate tasks

### `./gradlew screenshotTest` — one command, app + wear + widget

Registered in root `build.gradle.kts`. `:app:screenshotTestNoGmsDebug` is always a direct
dependency. Every other subproject is auto-discovered after project evaluation: it walks each
subproject's `src/` tree for `.kt` files containing `captureRoboImage` or `ImageIO`, and if found,
wires in whichever of that module's tasks is named `desktopTest` or matches `test*UnitTest` (noGms
variant only — the gms flavor crashes Robolectric, per `AGENTS.md`). This is discovery, not a
hardcoded module list, specifically so a new screenshot suite gets picked up the day it's written —
see the verified gap above for the one place that discovery currently misses
(`:app-web-preview`, whose task isn't named either pattern).

```bash
./gradlew screenshotTest                              # verify against baselines
./gradlew screenshotTest -Proborazzi.test.record=true  # re-record app+wear (widget/desktop always overwrite regardless of this flag)
```

Not covered by this task, by design, and not faked as if it were: iOS/watchOS (Swift, needs
Xcode — Gradle cannot run it) and, today, the web preview (see the gap above).

### `./gradlew screenshotFreshnessCheck`

Fails if any `docs/screenshots/*.png` hasn't had a commit touching it in the last N days
(`-PscreenshotMaxAgeDays=N`, default 30). Uses `git log -1 --format=%ct -- <path>`, not filesystem
mtime — mtime resets on every clean checkout, so it would be meaningless as a staleness signal the
moment CI does a fresh clone. What this catches: a harness that still runs clean but hasn't
actually been re-run against current code in a month. What it does **not** catch: a screenshot
re-recorded today against code that's already wrong (freshness says nothing about correctness — see
the desktop/web no-diff caveat above, which is the sharper version of this same limitation), or
one module quietly going stale while a *different* module's source changed (there's no PNG→module
mapping; the comment in `build.gradle.kts` explains why a flat `docs/screenshots/` directory makes
that mapping a guess rather than a fact).

### `./gradlew fullCheck` and `./gradlew verifyAll`

`fullCheck` = `ktlintCheck` + `detekt` + `:app:testNoGmsDebugUnitTest` + `screenshotTest` +
`:app:koverXmlReportNoGmsDebugCoverage` + `:app:koverVerifyNoGmsDebugCoverage`, plus every
subproject's `testAndroidHostTest` task (KMP modules that opt into `withHostTest {}` register JVM
tests under that name instead of `testNoGmsDebugUnitTest`; this list is also discovered via
`gradle.projectsEvaluated`, not hardcoded, for the same drift reason as the screenshot discovery).

`verifyAll` is a strict superset: `fullCheck` + `screenshotFreshnessCheck` +
`:app:assembleNoGmsDebug` + `:app:dependencyGuard`. Per the root `build.gradle.kts` comment, this
is meant to be *the one thing to watch* — not a second, independently-drifting definition of "done."

Both tasks are confirmed to exist (`./gradlew tasks --all` at the repo root lists both, with their
real descriptions). **As of this writing, neither is wired into any `.github/workflows/*.yml`** —
`quality.yml` still runs `ktlintCheck`, `detekt`, `testAndroidHostTest`,
`koverXmlReportNoGmsDebugCoverage`/`koverVerifyNoGmsDebugCoverage` and `dependencyGuard` as separate
steps, and `screenshots.yml` records screenshots through its own `scripts/run-jvm-tests.sh` wrapper
rather than through `screenshotTest`. That's Gradle-side work landed ahead of the CI wiring that
points at it — worth checking whether that's still true by the time you read this, with
`grep -rn "verifyAll" .github/workflows/`.

---

## Versioning artefacts

Three repo-root files: `VERSION` (`0.25.0`, legacy semver, no longer drives Gradle), `MILESTONE`
(`36`, an integer — bump this to cut a release), `BUILD_NUMBER` (`25`, legacy monotonic counter).
None of these are written by a Gradle task — `MILESTONE` is bumped by hand or via
`scripts/bump_version.sh --milestone`.

The values that actually drive builds are **computed at Gradle configure time and never written to
a file**, by `gradle/versioning.gradle.kts` (applied from `:app`, `:wear`, `:server`,
`:desktopApp`): `FINGERPRINT` (`YYYY.0M.0W.<MILESTONE>.<commitCount>`), `MARKETING`
(`YYYY.M.<MILESTONE>`), `BUILDCODE` (`1 + commitCount`), and a desktop-only
`desktopPackageVersion` (`<MILESTONE>.0.<commitCount>` — Compose Desktop's native-installer
`packageVersion` validator rejects `MARKETING` outright because its year-based major component
exceeds 255). Because these derive from `MILESTONE` + live `git rev-list --count HEAD` + today's
date, they cannot go stale in the way a written file can — the only way to move them is to bump
`MILESTONE` or make another commit. Full formulas and the release-cut procedure live in
[`docs/RELEASE.md`](RELEASE.md) §1; don't duplicate that table here, it's one door away.

```bash
./gradlew -q :app:printFingerprint   # confirmed to exist and run — prints the live FINGERPRINT
./gradlew -q :app:printMarketing     # confirmed to exist and run — prints the live MARKETING
scripts/bump_version.sh --milestone  # the actual release-cut step
scripts/bump_version.sh --commit     # prints the computed triple, writes nothing
```

---

## Flow GIFs

`scripts/build-flow-gifs.sh <output-name> <frame1> <frame2> ...` stitches an ordered list of
`docs/screenshots/*.png` frames into one crossfaded GIF at `docs/gifs/<output-name>.gif`, via
`ffmpeg`/`ffprobe` (hardcoded to `/opt/homebrew/bin/`, overridable with the `FFMPEG`/`FFPROBE` env
vars). README's Screenshots section says to regenerate frames with
`./gradlew :app:screenshotTestNoGmsDebug` first, then this script over them.

**Verified gap:** the *frame list* for each of the eleven GIFs in `docs/gifs/` (plus the three in
`docs/demo/` and `docs/assets/banner.gif`) is not recorded anywhere in the repo — I grepped for
every call site of `build-flow-gifs.sh` and found exactly one hit outside the script itself
(a mention in README prose, not an invocation). Regenerating an existing GIF today means someone
reconstructing, from the GIF's own name and content, which screenshot names belong in it and in
what order. That is the same "nothing runs the harness" failure mode as the screenshot gates, one
layer up: the stitching mechanism works and is documented, but there is no driver script recording
*which frames go where*, so the fourteen existing GIFs are each one missed detail away from being
unregeneratable except by eye.

---

## README fact spans

`scripts/gen-readme.sh` rewrites only the text between `<!-- AUTOGEN:stats -->` /
`<!-- /AUTOGEN:stats -->` in `README.md` — module counts from `settings.gradle.kts`
(`include(...)` lines split into local feature/core vs. composed-via-`includeBuild`), the
Roborazzi screenshot count (`find docs/screenshots -maxdepth 1 -name '*.png' | wc -l`), and the
Room schema version (grepped from `core/data/.../MilewayDatabase.kt`). Everything outside those
markers is hand-written prose and is never touched by the script. `.github/workflows/readme.yml`
runs on any PR touching `settings.gradle.kts`, `docs/screenshots/**`, `MilewayDatabase.kt`,
`scripts/gen-readme.sh` or `README.md` itself, re-runs the script, and fails the PR if that produces
a diff — so a stale fact span is a red PR check, not a silent drift, *for the one span that exists
today*. Only `stats` is wired up; if a future prose block wants the same treatment it needs its own
`<!-- AUTOGEN:x -->` pair and a case in the script.

```bash
./scripts/gen-readme.sh   # rewrites README.md in place; git diff it before committing
```

---

## Other generated artefacts

- **Fastlane changelogs** — `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`,
  regenerated from git commit history (bullet-aware, truncated to Play's 500-char limit) by every
  `fastlane android deploy_*` lane. See `docs/RELEASE.md` §2.
- **F-Droid metadata** — `metadata/com.mileway.fdroid.yml`, hand-maintained and submitted to the
  `fdroiddata` repo per release; not regenerated by anything in this repo. See `docs/RELEASE.md`
  §4.
- **`graphify-out/`** — the AgentHarness `codegraph` skill's code-knowledge-graph cache. Gitignored,
  derived, and owned by tooling outside this repo (`~/Tools/DevTools/AgentHarness`), not by a
  Mileway Gradle task. Flagged here only so it isn't mistaken for a Mileway-owned artefact by
  someone scanning the repo tree.
- **`CHANGELOG.md`** — hand-written, not generated by any script. Listed here to say explicitly
  that it is *not* an artefact this index covers regeneration for.

---

## How this rots (the specific failure modes already seen here)

1. **A harness exists, but nothing runs it.** `:app`, `:wear` and `:widget` each had a working
   Roborazzi suite on three different task names with no single command running all three — a
   change could break two of them while the one gate everyone watched stayed green. `screenshotTest`
   fixes this for those three by auto-discovery; the verified gap above shows the same pattern is
   still live for `:app-web-preview` today, because auto-discovery matches on task-name pattern and
   that module's task doesn't match the pattern.
2. **An output path points outside the repo, and the writer creates the phantom directory instead
   of failing.** The iOS captures hardcoded a path from before the repo moved; because the code
   called `createDirectory(withIntermediateDirectories: true)`, every run silently created that
   stale folder elsewhere on disk and wrote there — zero errors, for months. The fix pattern that
   actually holds: derive the path from something durable (`user.dir` walked up to the module or
   repo root, as desktop/web preview do) or an env var with a *correct* fallback, plus a mechanical
   guard (`OutputPathGuardTest`) that fails the build if a literal `/Users/...` reappears anywhere
   in source — not a code-review habit, because code review is exactly what missed this the first
   time.
3. **A target compiles or renders, but no gate asserts the output is right.** `:core:platform`'s
   desktop target had a missing import sit unseen because nothing compiled it. The sharper version
   of the same failure lives in the desktop and web-preview screenshot surfaces today: both compile,
   both run, both "pass" every single time, because neither has a pixel-diff — they unconditionally
   overwrite the PNG. A green `desktopTest` run proves the code renders *something*, never that it
   renders the *right* something.
4. **Two gates exist; only one is watched.** `assembleNoGmsDebug` + `testNoGmsDebugUnitTest` never
   ran a screenshot, because `:app`'s screenshot suite is deliberately forked out into
   `screenshotTestNoGmsDebug` (for the JVM-teardown reason documented in `app/build.gradle.kts`).
   On 2026-08-09 a real UI regression broke every `:app` capture at composition while the watched
   gate stayed green throughout. `fullCheck`/`verifyAll` closing that gap on the Gradle side doesn't
   help if CI keeps running the old, narrower step list — check whether `verifyAll` is actually
   wired into `.github/workflows/*.yml` before trusting that this failure mode is closed; as of this
   writing (see the aggregate-tasks section above) it is not yet.

---

## Verification log

Everything above a command was checked one of these ways, stated per section rather than repeated
here:

- **Ran the command** — `xcodebuild -list -project iosApp/iosApp.xcodeproj`; the four
  `./gradlew ... :tasks --all` invocations against `:app-web-preview`, `:app`+`:wear`+`:widget`+
  `:desktopApp` (combined), and the repo root; `./gradlew --no-daemon :app:testNoGmsDebugUnitTest
  --tests "com.mileway.OutputPathGuardTest"` (currently fails to compile — reported above, not
  fixed, out of this task's file ownership).
- **Confirmed the task name exists in `./gradlew tasks --all` output**, without executing it —
  every command in the Quick Reference table and the aggregate-tasks section.
- **Read the source that produces or consumes the artefact** — every `repoRoot()`/output-path
  derivation, the Roborazzi-plugin-vs-not distinction between `:app`/`:wear` and `:widget`, the
  `screenshotTest` discovery heuristic, `gen-readme.sh`, `versioning.gradle.kts`, `build-flow-gifs.sh`.
- **Did not run**: any full screenshot re-record, the iOS/watchOS XCTest suites end-to-end, or
  `verifyAll`/`fullCheck` in full — each takes real wall-clock time against a repo other agents are
  actively editing, and this task's job was the index, not re-proving Wave 0/1's work.

No command in this file was written down without at least one of the first two checks above.
