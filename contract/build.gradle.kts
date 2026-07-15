plugins {
    // PLAN_V33 A1: mirrors core:data's own target-plugin combo exactly (watchOS + desktop layered
    // on the AGP KMP-library base), so :contract publishes a target superset of every consumer —
    // core:data reaches watchOS/desktop, core:network doesn't, and a project dependency needs the
    // dependency to publish every target the consumer builds for.
    // ponytail: the task brief assumed `shared.kmp.pure` (external/kmp-build-logic), but that
    // convention deliberately has no android target (see its kdoc — "no Android or Compose
    // dependency"); applying it here would break core:data's Android compile classpath resolution
    // against this module (no matching variant). `shared.kmp.library`'s own kdoc calls out a plain
    // `jvm()` target as the future seam for a JVM backend — but adding one alongside the existing
    // `jvm("desktop")` (from mileway.kmp.desktop) hits Kotlin 2.4's "only one target per platform
    // type" restriction ("Declaring multiple Kotlin Targets of the same type is not supported").
    // Lane B, B1 (:server) instead consumes this module's existing `jvm("desktop")` target
    // directly via `api(project(":contract"))` — Gradle attribute-matches a plain kotlin("jvm")
    // consumer to it with no ambiguity, since it's the only jvm-platform-type variant published
    // here. No bare `jvm()` was added. Picked `mileway.kmp.library.watchos` + `mileway.kmp.desktop`
    // instead of `shared.kmp.pure`: an already-proven combo (core:data uses the exact same pair)
    // rather than a novel, untested one.
    id("mileway.kmp.library.watchos")
    id("mileway.kmp.desktop")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    android {
        namespace = "com.mileway.contract"
        compileSdk = 37
        minSdk = 30
        // Runs commonTest (the serialization round-trip test) on the JVM host too, mirroring
        // core:network's rationale — a pure-serialization module's tests should count on the host
        // gate, not just the Android unit-test variant.
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
