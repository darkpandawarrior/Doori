package com.mileway.core.data.banner

/**
 * PLAN_V24 P13.2: one typed row of the home banner carousel. Every carousel source (seeded
 * announcements, club, campaigns, offers) contributes [HomeBanner]s into a single auto-advancing
 * component, replacing the P5.4 marketing strip.
 *
 * @param id stable identity used for impression de-duplication + analytics.
 * @param title headline.
 * @param subtitle supporting line.
 * @param style the short category/badge label shown on the card (e.g. "OFFER", "CLUB").
 * @param deepLink an optional route/section key a tap routes to via the host's existing nav lambdas;
 *   null means the card is informational (no navigation).
 */
data class HomeBanner(
    val id: String,
    val title: String,
    val subtitle: String,
    val style: String,
    val deepLink: String? = null,
)
