package com.mileway.feature.whatsnew.data

import com.mileway.feature.whatsnew.model.WhatsNewEntry
import com.mileway.feature.whatsnew.model.WhatsNewMedia
import kotlinx.datetime.LocalDate

/**
 * PLAN_V36 P1 — the bundled What's New catalog. Deterministic on purpose: entries are
 * hand-authored below, never derived from a clock read, so the catalog is reproducible across
 * builds/tests (same discipline as `:stub` mock data).
 *
 * Seeded from the static changelog previously hardcoded in `WhatsNewViewModel`/`WhatsNewSheet`
 * (`:shared`'s `WHATS_NEW_ENTRIES`) — same copy, versions assigned 1..N by the git history of the
 * features they describe, `releasedOn` approximated to that month/day.
 *
 * PLAN_V36 P5: `media` paths point at `commonMain/composeResources/files/whatsnew/<id>/…` —
 * budget ≤500KB/image (APK bloat is the real ceiling; [WhatsNewCatalogTest] warns on oversize).
 *
 * Authors bump exactly one place when shipping a new release note: add an entry here with the
 * next monotonic [WhatsNewEntry.version] (drives [WhatsNewRepository.currentVersion], the badge).
 */
object WhatsNewCatalog {
    val entries: List<WhatsNewEntry> =
        listOf(
            WhatsNewEntry(
                id = "plugins-registry",
                version = 1,
                title = "Your plugins",
                description = "Turn any feature on or off from Settings → Plugins.",
                media =
                    listOf(
                        WhatsNewMedia(
                            path = "files/whatsnew/plugins-registry/01-plugins-screen.png",
                            caption = "Every feature, toggled live from Settings → Plugins",
                        ),
                    ),
                releasedOn = LocalDate(2026, 7, 8),
                modules = listOf("Settings"),
            ),
            WhatsNewEntry(
                id = "phone-signin",
                version = 2,
                title = "Phone sign-in",
                description = "Sign in with your phone number and a one-time code.",
                media =
                    listOf(
                        WhatsNewMedia(
                            path = "files/whatsnew/phone-signin/01-sign-in-screen.png",
                            caption = "Sign in to Mileway",
                        ),
                    ),
                releasedOn = LocalDate(2026, 7, 8),
                modules = listOf("Auth"),
            ),
            WhatsNewEntry(
                id = "two-factor-security",
                version = 3,
                title = "Two-factor & security",
                description = "Optional MFA plus a stronger PIN lockout keep your account safe.",
                media =
                    listOf(
                        WhatsNewMedia(
                            path = "files/whatsnew/two-factor-security/01-pin-lock-screen.png",
                            caption = "Stronger PIN lockout",
                        ),
                    ),
                releasedOn = LocalDate(2026, 7, 8),
                modules = listOf("Auth", "Security"),
            ),
        )
}
