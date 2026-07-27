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
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "dev.detekt")
    extensions.configure<dev.detekt.gradle.extensions.DetektExtension> {
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        baseline = file("detekt-baseline.xml")
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
        ":app:screenshotTestNoGmsDebug",
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

tasks.register("composeMetrics") {
    description = "Generate Compose compiler stability/recomposition reports for :app (release)."
    dependsOn(":app:assembleGmsRelease")
    doLast {
        println("Compose metrics written to: app/build/compose_metrics/")
    }
}
