plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.navgraph) apply false
    alias(libs.plugins.dependency.guard) apply false
    // Phase 12: Code quality
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.storytale) apply false
    alias(libs.plugins.roborazzi) apply false
    // V15: Firebase plugins, applied in :app (gms path); F-Droid strips them in the build prebuild (FLFD).
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    // Build health, applied to root only
    alias(libs.plugins.gradle.doctor)
}

// --------------------------------------------------------------------------
// Detekt: static analysis; fails on any finding by default (see config/detekt/detekt.yml)
// --------------------------------------------------------------------------
detekt {
    config.setFrom(files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
}

// --------------------------------------------------------------------------
// Kover: test coverage aggregation from every subproject
//
// Previously this only pulled in :app, whose own local coverage floor (see
// app/build.gradle.kts) already excludes the UI layer — so the badge measured
// ~2 dozen classes out of ~500 across core/feature modules. Expanded to the
// modules that actually hold logic worth measuring; :app's own local
// noGmsDebugCoverage floor is unchanged and still gates separately.
// --------------------------------------------------------------------------
dependencies {
    kover(project(":app"))
    kover(project(":core:data"))
    kover(project(":core:network"))
    kover(project(":contract"))
    kover(project(":feature:tracking"))
    kover(project(":feature:logging"))
}

kover {
    reports {
        filters {
            excludes {
                packages("*.BuildConfig", "*.R")
                // Same rationale as app/build.gradle.kts: UI is exercised by screenshot tests, not
                // the assertion-based unit-test floor; excluding it keeps the number about logic
                // coverage. No-op in modules that don't have these packages.
                packages(
                    "*.ui.screens",
                    "*.ui.components",
                    "*.ui.sheets",
                    "*.ui.navigation",
                    "*.ui.theme",
                    "*.ui.previews",
                )
                classes("*Screen*Kt", "*Preview*", "*ComposableSingletons*")
                // Belt-and-braces for the point above: catches any @Composable that ends up
                // outside a *.ui.* package or a *Screen*/*Preview*-named file (e.g. a composable
                // dropped in a debug/ or root package) so the exclusion doesn't silently miss it.
                annotatedBy("androidx.compose.runtime.Composable")
                // core:data, feature:tracking and feature:logging each ship a `di` package of pure
                // Koin module declarations (`single { FooImpl(get()) }`) — declarative wiring, not
                // logic; counting it dilutes the number same as counting UI would. No-op in
                // modules without one (:core:network, :contract).
                // "*.di", not "*.di.*": Kover matches the pattern against the WHOLE package name, and
                // nothing follows `di` in com.mileway.core.data.di — so the trailing `.*` matched
                // nothing and this exclusion was silently inert. Every other entry in this block
                // already uses the no-trailing-wildcard form.
                packages("*.di")
                // Room (core:data) KSP-generates a `<Dao/Database>_Impl` class per @Dao/@Database
                // in the same package as the source — exercised by Room's own instrumented/
                // migration tests, not the unit-test floor. No-op in modules without Room.
                classes("*_Impl")
            }
        }
        total {
            verify {
                rule {
                    // ponytail: placeholder floor. This is the FIRST run of the expanded aggregation
                    // (:app + core:data + core:network + contract + feature:tracking + feature:logging)
                    // — nobody has measured the real percentage yet, so this is deliberately set low
                    // enough to certainly clear rather than guessed. NOTE: neither `fullCheck` nor
                    // .github/workflows/quality.yml currently runs this rule — the "Coverage report +
                    // floor" CI step and the `kover-report` artifact are both scoped to `:app:kover*
                    // NoGmsDebugCoverage` only, so this `total { verify {} }` rule is unenforced today
                    // (see task report). To get the real number, run the root's default aggregate
                    // task — `./gradlew koverXmlReport` (writes build/reports/kover/report.xml) and/or
                    // `./gradlew koverVerify` — then ratchet this up to a couple of points below the
                    // measured value, the same way app/build.gradle.kts's 20 tracks its own 22.66%
                    // measured number. Wiring that task into the gate is out of this task's file scope
                    // (root `build.gradle.kts`'s `fullCheck` task + quality.yml, not the kover block).
                    minBound(5)
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// ktlint: code style; applied to all subprojects
// --------------------------------------------------------------------------
// Kotlin/Native per-architecture source-set names, e.g. detektIosArm64MainSourceSet.
val archLeafPattern = Regex("(Arm64|X64|X86)")

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "dev.detekt")
    extensions.configure<dev.detekt.gradle.extensions.DetektExtension> {
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        baseline = file("detekt-baseline.xml")
    }

    // `./gradlew detekt` analyses nothing on a KMP module: the per-module `detekt` task looks for
    // src/main/kotlin, which KMP does not have. The tasks that see the code are per-source-set, and
    // were never attached to it — so every "detekt passed" on a KMP module was vacuous.
    tasks.matching { it.name == "detekt" }.configureEach {
        dependsOn(
            tasks.matching {
                it.name.startsWith("detekt") &&
                    it.name.endsWith("SourceSet") &&
                    !it.name.startsWith("detektBaseline") &&
                    // Skip the per-ARCHITECTURE native leaves (IosArm64, WatchosSimulatorArm64,
                    // ...). They hold no source of their own — they re-analyse what iosMain and
                    // watchosMain already cover, plus generated cinterop stubs, and each reported
                    // the same 8,535 issues. Five copies of one analysis, none of it ours.
                    !archLeafPattern.containsMatchIn(it.name)
            },
        )
    }
}

// --------------------------------------------------------------------------
// Gradle Doctor, catches common build health issues (Rosetta, JDK mismatch,
// Kotlin daemon fallback, Jetifier still on, etc.)
// --------------------------------------------------------------------------
doctor {
    warnWhenJetifierEnabled = true
    warnIfKotlinCompileDaemonFallback = true
    javaHome {
        ensureJavaHomeIsSet = true
        ensureJavaHomeMatches = true
        failOnError = false // warn only until team is aligned on JDK toolchain
    }
    // P2.1: :wear is now a second com.android.application module (phone :app + watch :wear), so the
    // repo-wide `assembleNoGmsDebug`/`assembleGmsDebug` gate legitimately fans out to both — Doctor's
    // "did you really mean to build multiple apps" heuristic would otherwise fail every gate run.
    allowBuildingAllAndroidAppsSimultaneously = true
}

// --------------------------------------------------------------------------
// Workflow task aliases, convenience entry points for the local dev loop
// --------------------------------------------------------------------------
tasks.register("devBuild") {
    description = "Clean + debug APK + unit tests: full local dev loop."
    // noGms is the JVM-safe unit-test variant (gms Play Services maps crash Robolectric).
    dependsOn(":app:clean", ":app:assembleGmsDebug", ":app:testNoGmsDebugUnitTest")
}

tasks.register("quickBuild") {
    description = "Debug APK only (no tests): fastest iteration cycle."
    dependsOn(":app:assembleGmsDebug")
}

tasks.register("fullCheck") {
    description = "ktlint + detekt + tests + kover coverage floor: all quality gates."
    // noGms is the JVM-safe unit-test variant; kover floor verified on the same variant.
    dependsOn(
        "ktlintCheck",
        "detekt",
        ":app:testNoGmsDebugUnitTest",
        // Z.5b: the @GraphicsMode(NATIVE) Roborazzi screenshot tests are excluded from the task above
        // (native Skia + forkEvery restart boundaries crash the JVM); they run in their own isolated
        // single fork here so they still gate but never destabilise the main unit-test fork.
        // Was :app-only, which is how a broken :wear or :widget capture could sit unnoticed.
        "screenshotTest",
        ":app:koverXmlReportNoGmsDebugCoverage",
        ":app:koverVerifyNoGmsDebugCoverage",
    )
}

// KMP modules name their JVM unit-test task `testAndroidHostTest` (opt-in per module via the
// android target's `withHostTest {}` — AGP's KMP library plugin doesn't register the task
// otherwise), not the variant-specific `testNoGmsDebugUnitTest` used by :app, so the aggregate
// above never runs them and the unqualified task name doesn't resolve at the root project (Z.5a).
// This used to be a hardcoded list of 7 module paths here, which silently drifted from the real set
// as more KMP modules picked up `withHostTest {}` — :contract, :core:network and :stub (three
// wire-critical modules) were missing, so the local gate was quietly weaker than CI. Derive it
// instead: once every subproject is configured, wire in every subproject that actually registers a
// `testAndroidHostTest` task, so a new commonTest module is picked up automatically and the list
// can't drift again. `gradle.projectsEvaluated` still runs at configuration time (before the task
// graph is built/cached), and `dependsOn(Task)` on a cross-project task is the standard
// config-cache-safe way to wire this in — no `Project` reference is captured for execution.
gradle.projectsEvaluated {
    tasks.named("fullCheck") {
        subprojects.forEach { sub ->
            sub.tasks.findByName("testAndroidHostTest")?.let { dependsOn(it) }
        }
    }
}

// --------------------------------------------------------------------------
// Screenshot freshness guard (Wave 0.3): stale captures are the visible symptom of "work existed,
// nothing ran it, nothing alerted" — so staleness must be a red build, not a discovery made months
// later.
//
// Went with a simple max-age over "PNG newer than the newest source file of the module that
// produces it": docs/screenshots/ is one flat directory with no naming convention that maps 1:1
// onto a Gradle module path (e.g. is "advance_history_screen.png" owned by :feature:advances or
// :feature:approvals? "booking_history_screen.png" by :feature:travel? there is no rule, only a
// guess), so a per-module comparison would be brittle guesswork wearing precision's clothes. Age
// since last commit needs no such mapping and can't silently miss a module that was never mapped.
//
// Uses `git log`, not filesystem mtime: a fresh checkout stamps every file with the checkout time,
// so mtime is meaningless for staleness the moment CI does a clean clone. Git's
// last-commit-that-touched-this-path date is the real "when was this last regenerated" signal and
// survives a checkout.
//
// What this catches: a screenshot nobody has re-recorded in screenshotMaxAgeDays even though the
// harness that produces it still runs clean — the exact silent-rot failure mode from Wave 0.
// What this does NOT catch: a screenshot re-recorded today against code that is itself wrong
// (freshness says nothing about correctness), or one module's screenshot going stale while only
// that module's source changed and others didn't (no per-module mapping, see above — a max-age
// check is blind to which module a PNG belongs to by design).
// --------------------------------------------------------------------------
tasks.register("screenshotFreshnessCheck") {
    group = "verification"
    description = "Fails if any docs/screenshots/*.png hasn't been re-recorded in screenshotMaxAgeDays."
    val maxAgeDays = (project.findProperty("screenshotMaxAgeDays") as String?)?.toIntOrNull() ?: 30
    val screenshotsDir = layout.projectDirectory.dir("docs/screenshots").asFile
    val repoRoot = rootDir
    doLast {
        val pngs = screenshotsDir.listFiles { f -> f.isFile && f.extension.equals("png", ignoreCase = true) }.orEmpty()
        val nowSeconds = System.currentTimeMillis() / 1000
        val maxAgeSeconds = maxAgeDays * 24L * 60 * 60
        val stale =
            pngs.mapNotNull { png ->
                val proc =
                    ProcessBuilder("git", "log", "-1", "--format=%ct", "--", png.absolutePath)
                        .directory(repoRoot)
                        .redirectErrorStream(true)
                        .start()
                val commitEpoch = proc.inputStream.bufferedReader().readText().trim().toLongOrNull()
                proc.waitFor()
                // No git history for the file (freshly added, not yet committed) -> not stale.
                val ageSeconds = commitEpoch?.let { nowSeconds - it } ?: return@mapNotNull null
                if (ageSeconds > maxAgeSeconds) png.name to (ageSeconds / 86400) else null
            }
        if (stale.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("${stale.size} screenshot(s) haven't been re-recorded in $maxAgeDays days:")
                    stale.sortedByDescending { it.second }.forEach { (name, days) -> appendLine("  $name (${days}d old)") }
                    appendLine()
                    appendLine("Re-record with: ./gradlew screenshotTest -Proborazzi.test.record=true")
                    appendLine("(or pass -PscreenshotMaxAgeDays=N if $maxAgeDays days is intentionally tight for this run)")
                },
            )
        }
    }
}

// --------------------------------------------------------------------------
// verifyAll (Wave 0.1): THE one task that must be green before anything ships.
//
// Superset of `fullCheck`, not a competitor to it: `fullCheck` stays as the faster local
// quality-loop subset (style + unit/KMP tests + screenshots + coverage — no full assemble, no
// dependency-guard, no freshness check), useful for a tight local dev cycle. `verifyAll` depends on
// `fullCheck` and adds exactly the pieces that make it the real release gate: the actual build,
// the dependency-guard, and the screenshot-freshness check. There is exactly one thing to watch
// before shipping — this task — and it is a strict superset of the other, not a second independent
// definition of "done" that can silently drift from it.
//
// The absolute-path guard (OutputPathGuardTest) needs no separate wiring here: it is a JUnit test
// under app/src/test, so testNoGmsDebugUnitTest (pulled in via fullCheck) already runs it. Adding
// it again as its own dependency would recreate the "two gates, only one watched" bug this whole
// program exists to kill.
// --------------------------------------------------------------------------
tasks.register("verifyAll") {
    group = "verification"
    description = "THE release gate: assemble + fullCheck (lint/detekt/unit tests/screenshots/coverage) + freshness + dependency-guard."
    dependsOn(
        ":app:assembleNoGmsDebug",
        "fullCheck",
        "screenshotFreshnessCheck",
        ":app:dependencyGuard",
    )
}

tasks.register("composeMetrics") {
    description = "Generate Compose compiler stability/recomposition reports for :app (release)."
    dependsOn(":app:assembleGmsRelease")
    doLast {
        println("Compose metrics written to: app/build/compose_metrics/")
    }
}

// ---------------------------------------------------------------------------
// One task that runs EVERY screenshot harness in the repo: app + wear + widget + desktop.
//
// Why this exists: the harnesses were never the problem — :app, :wear and
// :widget each had a working Roborazzi suite. The problem was that they live on
// three different task names and no single command ran them together, so a
// change could turn a gate green while silently breaking a surface nobody ran.
// That happened on 2026-08-09: injecting SystemSettingsOpener into
// TrackMilesScreen broke every :app capture at composition, and
// assembleNoGmsDebug + testNoGmsDebugUnitTest stayed green throughout, because
// :app:screenshotTestNoGmsDebug is deliberately forked out of the main suite.
//
//   ./gradlew screenshotTest                              # verify against baselines
//   ./gradlew screenshotTest -Proborazzi.test.record=true # re-record them
//
// NOT covered here, and deliberately not faked as if it were:
//   - iOS  (iosApp/MilewayWidgetsTests, MilewayWatchTests) — Swift snapshot
//     tests that need Xcode; Gradle cannot run them.
//   - desktop (:desktopApp) — Roborazzi is Robolectric-based and Android-only;
//     Compose Desktop needs ImageComposeScene render-to-PNG instead.
//   - wasm (:app-web-preview) — no test source set; needs a browser harness.
//
// Desktop WAS in this list until 2026-08-09, wrongly: :desktopApp already had
// DesktopScreenshotGalleryTest writing 7 PNGs via ImageIO. It was never uncovered, just never
// run by anything — which is the same failure this whole task exists to end.
// ---------------------------------------------------------------------------
// One task that runs EVERY screenshot harness in the repo.
//
//   ./gradlew screenshotTest                              # verify against baselines
//   ./gradlew screenshotTest -Proborazzi.test.record=true # re-record them
//
// The gap this closes: :app, :wear and :widget each had a working Roborazzi suite, on three
// different task names, with no single command running them. On 2026-08-09 injecting
// SystemSettingsOpener into TrackMilesScreen broke all 155 :app captures at composition while
// assembleNoGmsDebug + testNoGmsDebugUnitTest stayed green throughout, because :app's screenshot
// suite is deliberately forked out of the main one. Two gates existed; only one was watched.
//
// Aggregating them was flaky until the cause was found: MockK self-attaches a ByteBuddy agent,
// modern JDKs restrict self-attach, so it fell back to spawning an external attach process that
// lost a race whenever several test JVMs ran at once. app/build.gradle.kts now pre-loads
// byte-buddy-agent via -javaagent, removing runtime attachment entirely. 5/5 clean runs under
// --rerun-tasks, against roughly 2-in-3 failures before.
//
// NOT covered here, and deliberately not faked as if it were:
//   - iOS (iosApp/MilewayWidgetsTests, MilewayWatchTests) — Swift snapshot tests needing Xcode.
//   - wasm (:app-web-preview) — no test source set; needs a browser harness.
tasks.register("screenshotTest") {
    group = "verification"
    description = "Runs every screenshot harness (app + wear + widget) in one command."
    dependsOn(":app:screenshotTestNoGmsDebug")
}

gradle.projectsEvaluated {
    tasks.named("screenshotTest") {
        // Discovered rather than hardcoded, for the same reason fullCheck discovers
        // testAndroidHostTest: a hardcoded list is a list that drifts. Any module whose
        // test sources actually call captureRoboImage gets pulled in automatically, so a
        // new screenshot suite is covered the day it is written rather than the day
        // someone remembers this file.
        subprojects.forEach { sub ->
            if (sub.path == ":app") return@forEach // already wired to its dedicated forked task
            // Both layouts: Android modules keep tests in src/test, KMP-JVM modules like
            // :desktopApp use a custom-named source set (src/desktopTest). Walking the whole
            // src/ dir covers either without hardcoding which module uses which.
            val hasCaptures =
                sub.projectDir.resolve("src").walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    // Two capture mechanisms in this repo: Roborazzi on the Android/Wear/widget
                    // side, and plain ImageIO writes from Compose Desktop's renderComposeScene.
                    // Match either, so a harness is discovered by what it DOES rather than by
                    // which library it happens to use.
                    .any { f -> f.readText().let { it.contains("captureRoboImage") || it.contains("ImageIO") } }
            if (!hasCaptures) return@forEach
            sub.tasks.matching {
                // :desktopApp's test task is "desktopTest", not "test*UnitTest".
                // noGms only. AGENTS.md: "the gms flavor crashes Robolectric" — pulling in the gms
                // variant here would make the unified task fail for a reason that has nothing to do
                // with the screenshots it is meant to guard.
                (it.name == "desktopTest" || (it.name.startsWith("test") && it.name.endsWith("UnitTest"))) &&
                    // "NoGmsDebug" contains "Gms", so match the flavour, not the substring.
                    !(it.name.contains("Gms") && !it.name.contains("NoGms"))
            }.forEach { t ->
                dependsOn(t)
                // Ordered, not just aggregated. :app's screenshot suite runs @GraphicsMode(NATIVE)
                // Skia in its own single fork precisely because it is fragile about sharing a build
                // with other test JVMs — running these concurrently reproducibly kills its class
                // init. Sequencing costs a few seconds and buys a task that does not flake.
                t.mustRunAfter(":app:screenshotTestNoGmsDebug")
            }
        }
    }
}
