plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

/**
 * P3.3: watchOS umbrella module — produces the headless `SharedWatch.framework` the future
 * SwiftUI watch app (Phase 4) links against. Mirrors `:shared`'s iOS-umbrella shape (same "sits
 * above the modules it re-exports" reasoning), but:
 *  - **watchos* targets only** — no `iosArm64`/`iosSimulatorArm64`, no Android. This module is
 *    watchOS-exclusive; the phone/iOS app keeps using `:shared`.
 *  - **static framework**, not an XCFramework — PLAN_V23 §7's "static framework ⇒ no embed phase"
 *    gotcha; `linkerOpts("-lsqlite3")` links the bundled-SQLite driver's native dependency the same
 *    way `core:data`'s `appleMain` `buildMilewayDatabase()` needs on iOS.
 *  - **exports only `core:data`** (the watchos-safe domain: `SnapshotPublisher`, `SavedTrackDao`,
 *    `WatchSyncPayload`/`SurfaceSnapshot`, `WatchSyncBridge`), never `feature:tracking` or
 *    `core:ui` — `feature:tracking` is a Compose (`mileway.cmp.feature`) module with NO watchos
 *    target (PLAN_V23 §6: "Compose Multiplatform UI on watchOS — not a thing"), so it cannot be
 *    part of this framework's dependency graph. `WatchFacade` (the phone/Wear-OS-Compose-facing
 *    facade from P1.2, which needs `feature:tracking`'s `SavedTrackRepository`/`TrackingController`)
 *    is therefore NOT reused here; this module's own [WatchDomainFacade] is a read-only sibling
 *    with the same shape, built directly on `core:data` types only. Documented deviation from the
 *    plan's literal "export WatchFacade" line, for the same module-boundary reason P1.2 already
 *    flagged when it placed `WatchFacade` in `feature:tracking` instead of `core:data`.
 */
kotlin {
    // The `watchosMain` intermediate source set (shared by all 3 watchos targets below) is where
    // WatchFacadeFactory lives — it needs core:data's appleMain-only `buildMilewayDatabase()`.
    applyDefaultHierarchyTemplate()

    listOf(
        watchosArm64(),
        watchosSimulatorArm64(),
        watchosDeviceArm64(),
    ).forEach { watchosTarget ->
        watchosTarget.binaries.framework {
            // P4.3: named SharedWatch, NOT MilewayWatch — the watchOS Xcode *app target* is itself
            // named MilewayWatch (P4.1), and a Swift module can't import a framework sharing its
            // own module name (`import MilewayWatch` from inside target MilewayWatch self-resolves
            // and never sees the Kotlin framework's symbols). Distinct names avoid the collision.
            baseName = "SharedWatch"
            isStatic = true
            // dependencies.project(path), not project(path): Gradle 10 refuses a Project
            // object as a dependency notation, and KGP's export() takes Any so the old form
            // compiled fine while deprecating underneath.
            export(dependencies.project(":core:data"))
            linkerOpts("-lsqlite3")
        }
    }

    sourceSets {
        commonMain.dependencies {
            // api(...) is required for export(...) above to surface core:data's public API in the framework.
            api(project(":core:data"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
