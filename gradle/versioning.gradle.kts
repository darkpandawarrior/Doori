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

// A shallow clone reports 1 commit, so versionCode silently becomes 2. That is exactly what
// shipped: every APK published to F-Droid carried versionCode 2, so no client could ever offer
// an upgrade. actions/checkout defaults to fetch-depth 1, so any workflow that forgets
// fetch-depth: 0 reintroduces it. Refuse rather than guess.
val dooriIsShallow =
    providers.exec { commandLine("git", "rev-parse", "--is-shallow-repository") }
        .standardOutput.asText.get().trim() == "true"

val dooriCommitCount =
    providers.exec { commandLine("git", "rev-list", "--count", "HEAD") }
        .standardOutput.asText.get().trim().toIntOrNull() ?: 0

require(!dooriIsShallow) {
    "Shallow clone: git rev-list reports $dooriCommitCount commits, so versionCode would be " +
        "${1 + dooriCommitCount}. Set `fetch-depth: 0` on actions/checkout."
}

val dooriToday = java.time.LocalDate.now()
val dooriIsoWeek = dooriToday.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())

val dooriVersionCodeBase = 1

extra["mileway.fingerprint"] =
    "%d.%02d.%02d.%d.%d".format(
        dooriToday.year,
        dooriToday.monthValue,
        dooriIsoWeek,
        mileawayMilestone,
        dooriCommitCount,
    )
extra["doori.marketing"] = "${dooriToday.year}.${dooriToday.monthValue}.$mileawayMilestone"
extra["doori.buildCode"] = dooriVersionCodeBase + dooriCommitCount

// ponytail: Compose Desktop validates the native-installer packageVersion at CONFIGURE time as
// MAJOR.MINOR.BUILD with MAJOR ≤ 255 — MARKETING (YYYY.M.MILESTONE, MAJOR=year>255) throws and
// fails ALL Doori CI (Gradle configures every project). This desktop-only value keeps the
// milestone visible while staying legal: valid until MILESTONE>255 or commitCount>65535, years out.
extra["doori.desktopPackageVersion"] = "$mileawayMilestone.0.$dooriCommitCount"
