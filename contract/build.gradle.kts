plugins {
    // PLAN_V33 A1: mirrors core:data's own target-plugin combo exactly (watchOS + desktop layered
    // on the AGP KMP-library base), so :contract publishes a target superset of every consumer —
    // core:data reaches watchOS/desktop, core:network doesn't, and a project dependency needs the
    // dependency to publish every target the consumer builds for.
    // ponytail: the task brief assumed `shared.kmp.pure` (external/kmp-build-logic), but that
    // convention deliberately has no android target (see its kdoc — "no Android or Compose
    // dependency"); applying it here would break core:data's Android compile classpath resolution
    // against this module (no matching variant). `shared.kmp.library`'s own kdoc calls out exactly
    // this future need ("shared DTOs/contracts consumed by a JVM backend, add jvm() yourself") —
    // that plain jvm() target is Lane B's job (:server, B1), skipped here since nothing consumes it
    // yet (YAGNI). Picked `mileway.kmp.library.watchos` + `mileway.kmp.desktop` instead: an
    // already-proven combo (core:data uses the exact same pair) rather than a novel, untested one.
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
