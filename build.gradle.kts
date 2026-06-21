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
// Detekt: static analysis; maxIssues=0 enforced in config/detekt/detekt.yml
// --------------------------------------------------------------------------
detekt {
    config.setFrom(files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
}

// --------------------------------------------------------------------------
// Kover: test coverage aggregation from every subproject
// --------------------------------------------------------------------------
dependencies {
    // Aggregate coverage from :app which has the unit tests + kover applied
    kover(project(":app"))
}

kover {
    reports {
        filters {
            excludes {
                packages("*.BuildConfig", "*.R")
            }
        }
    }
}

// --------------------------------------------------------------------------
// ktlint: code style; applied to all subprojects
// --------------------------------------------------------------------------
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
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
        ":app:koverXmlReportNoGmsDebugCoverage",
        ":app:koverVerifyNoGmsDebugCoverage",
    )
}

tasks.register("composeMetrics") {
    description = "Generate Compose compiler stability/recomposition reports for :app (release)."
    dependsOn(":app:assembleGmsRelease")
    doLast {
        println("Compose metrics written to: app/build/compose_metrics/")
    }
}
