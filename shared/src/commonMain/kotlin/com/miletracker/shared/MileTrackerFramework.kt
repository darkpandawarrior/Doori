package com.miletracker.shared

/**
 * Marker for the `MileTracker` iOS umbrella framework.
 *
 * The umbrella module only re-`export`s `core:ui` + `feature:tracking`; a Kotlin/Native framework that
 * has no source of its own links as `NO-SOURCE` and is skipped, so this gives the module a compilation
 * unit. It also gives Swift a trivial way to confirm the shared framework is actually linked:
 * `MileTrackerFramework().frameworkName`.
 */
class MileTrackerFramework {
    val frameworkName: String = "MileTracker"
}
