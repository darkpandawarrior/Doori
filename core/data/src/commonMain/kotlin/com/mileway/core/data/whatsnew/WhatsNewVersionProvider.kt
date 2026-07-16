package com.mileway.core.data.whatsnew

/**
 * PLAN_V36 P2 — minimal cross-feature contract for "the current What's New release version",
 * the badge comparand for [com.mileway.core.data.session.SessionState.whatsNewLastSeenVersion].
 *
 * Lives in core:data (not core:ui, not `:feature:whatsnew`) so a feature module that only needs
 * the version number for a badge — Settings (`feature:profile`) — can depend on it without
 * pulling in `:feature:whatsnew`'s catalog/UI. Mirrors how
 * [com.mileway.core.data.plugin.PluginRegistry] lets features consume cross-cutting state
 * without a feature-to-feature dependency. `:feature:whatsnew`'s `WhatsNewRepository` implements
 * this; the app's Koin graph binds it once so every consumer resolves the same instance.
 */
interface WhatsNewVersionProvider {
    val currentVersion: Int
}
