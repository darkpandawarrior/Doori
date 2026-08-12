package com.mileway.feature.whatsnew.model

/*
 * In-app update domain model. Behaviour ported from the reference app's force/soft-update flow
 * (remote-authored min-supported version + release notes, gated behind the platform store's own
 * availability check) — never its code or package names, see AGENTS.md's reference-app rule.
 *
 * Mileway already has a 2-state (FORCED/FLEXIBLE) gate wired end to end — core/ui's UpdateGate.kt +
 * MaintenanceGate.kt, core/network's ConfigProvider.getUpdateConfig(), :stub's DemoConfigManager,
 * app-shell's AppUpdateManager/UpdateConfig/UpdateAvailability, LauncherActivity's CF.5/UP.4/P7.6
 * wiring — none of it in this module's partition. This file adds the richer 4-state model
 * (NONE/OPTIONAL/RECOMMENDED/FORCED, with release notes + a display version) that system doesn't
 * have; [UpdateChecker] composes it with the existing [com.siddharth.kmp.appshell.AppUpdateManager]
 * rather than re-wrapping Play Core / the App Store lookup a second time.
 */

/** How urgently the running build should update. */
enum class UpdateUrgency { NONE, OPTIONAL, RECOMMENDED, FORCED }

/**
 * Remote-authored update signal. [latestBuildCode]/[minSupportedBuildCode] are BUILDCODE values —
 * the same monotonic int Mileway's own versioning scheme already uses as Android `versionCode` /
 * iOS `CFBundleVersion` (docs/RELEASE.md §1; `gradle/versioning.gradle.kts`;
 * `core/ui/platform/MaintenanceGate.isUnderMaintenance` compares on it today). Compare on the
 * BUILDCODE ints, never on [latestMarketingVersion] — MARKETING (`YYYY.M.MILESTONE`) is
 * display-only and isn't zero-padded, so a plain string/lexicographic compare gets it wrong (see
 * [MarketingVersion] for the fix, used only where a raw BUILDCODE isn't available).
 */
data class UpdateInfo(
    val latestBuildCode: Int,
    val minSupportedBuildCode: Int,
    val latestMarketingVersion: String,
    val releaseNotes: String,
    val storeUrl: String,
    // Mirrors the existing UpdateConfig.mode boolean split (FORCED vs FLEXIBLE) rather than
    // inventing a numeric priority scale nobody asked for: true nudges a non-forced update from
    // OPTIONAL to RECOMMENDED (same dialog, stronger copy/tone), never overrides FORCED.
    val recommended: Boolean = false,
)

/** The derived requirement for one installed build. */
data class UpdateRequirement(
    val urgency: UpdateUrgency,
    val currentBuildCode: Int,
    val info: UpdateInfo,
)

/**
 * The version-comparison + requirement-derivation logic (the part with real branching — see
 * [com.mileway.feature.whatsnew.data.UpdateChecker]'s KDoc for how the store's own availability
 * check still gates whether this ever reaches the UI).
 *
 * FORCED always wins: below the minimum supported build, the app must update regardless of
 * [UpdateInfo.recommended]. Otherwise NONE once caught up, else RECOMMENDED/OPTIONAL by the
 * remote flag.
 */
fun UpdateInfo.deriveRequirement(currentBuildCode: Int): UpdateRequirement {
    val urgency =
        when {
            currentBuildCode < minSupportedBuildCode -> UpdateUrgency.FORCED
            currentBuildCode >= latestBuildCode -> UpdateUrgency.NONE
            recommended -> UpdateUrgency.RECOMMENDED
            else -> UpdateUrgency.OPTIONAL
        }
    return UpdateRequirement(urgency, currentBuildCode, this)
}

/**
 * Parses Mileway's MARKETING version string (`YYYY.M.MILESTONE`, docs/RELEASE.md §1) into
 * comparable integer components. Exists because MARKETING is *not* zero-padded and *not* strict
 * semver: a plain string compare orders `"2026.10.5"` before `"2026.9.6"` (lexicographic `'1' <
 * '9'`), which is backwards — numeric-component comparison fixes it. [UpdateChecker] still prefers
 * a raw BUILDCODE (an unambiguous monotonic int) wherever one is available; this is the fallback
 * for a source that only ever hands back a marketing-style string.
 */
data class MarketingVersion(
    val year: Int,
    val month: Int,
    val milestone: Int,
) : Comparable<MarketingVersion> {
    override fun compareTo(other: MarketingVersion): Int =
        compareValuesBy(this, other, MarketingVersion::year, MarketingVersion::month, MarketingVersion::milestone)

    override fun toString(): String = "$year.$month.$milestone"

    companion object {
        /** Null on anything that isn't exactly 3 dot-separated integers. */
        fun parse(marketing: String): MarketingVersion? {
            val parts = marketing.trim().split(".")
            if (parts.size != 3) return null
            val year = parts[0].toIntOrNull() ?: return null
            val month = parts[1].toIntOrNull() ?: return null
            val milestone = parts[2].toIntOrNull() ?: return null
            return MarketingVersion(year, month, milestone)
        }
    }
}
