// Shared three-tier versioning script plugin. Apply via:
//   apply(from = rootProject.file("gradle/versioning.gradle.kts"))
// wherever a target (app, wear, server, ...) needs computed version strings — keeps the formula in
// ONE place instead of re-deriving it per module. See docs/RELEASE.md for the full model.
//
// Source of truth: repo-root MILESTONE (integer, bumped via `scripts/bump_version.sh --milestone`)
// + the live git commit count. VERSION/BUILD_NUMBER (legacy semver + monotonic counter) are kept
// for continuity but no longer drive the computed values below — MILESTONE + date + commit count do.
//
// - FINGERPRINT = YYYY.0M.0W.<MILESTONE>.<commitCount> — tag / release title / BuildConfig / debug suffix.
// - MARKETING   = YYYY.M.<MILESTONE> — Android versionName / iOS CFBundleShortVersionString (≤3 components).
// - BUILDCODE   = VERSION_CODE_BASE + commitCount — Android versionCode / iOS CFBundleVersion.
val milestoneFile = rootProject.file("MILESTONE")
val mileawayMilestone = if (milestoneFile.exists()) milestoneFile.readText().trim().toIntOrNull() ?: 1 else 1

val milewayCommitCount =
    providers.exec { commandLine("git", "rev-list", "--count", "HEAD") }
        .standardOutput.asText.get().trim().toIntOrNull() ?: 0

val milewayToday = java.time.LocalDate.now()
val milewayIsoWeek = milewayToday.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())

val milewayVersionCodeBase = 1

extra["mileway.fingerprint"] =
    "%d.%02d.%02d.%d.%d".format(
        milewayToday.year,
        milewayToday.monthValue,
        milewayIsoWeek,
        mileawayMilestone,
        milewayCommitCount,
    )
extra["mileway.marketing"] = "${milewayToday.year}.${milewayToday.monthValue}.$mileawayMilestone"
extra["mileway.buildCode"] = milewayVersionCodeBase + milewayCommitCount

// ponytail: Compose Desktop validates the native-installer packageVersion at CONFIGURE time as
// MAJOR.MINOR.BUILD with MAJOR ≤ 255 — MARKETING (YYYY.M.MILESTONE, MAJOR=year>255) throws and
// fails ALL Mileway CI (Gradle configures every project). This desktop-only value keeps the
// milestone visible while staying legal: valid until MILESTONE>255 or commitCount>65535, years out.
extra["mileway.desktopPackageVersion"] = "$mileawayMilestone.0.$milewayCommitCount"
