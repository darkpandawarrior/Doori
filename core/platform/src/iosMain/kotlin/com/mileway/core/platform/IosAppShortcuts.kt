package com.mileway.core.platform

/**
 * iOS home-screen quick actions (SH.3).
 *
 * Left as a documented no-op: unlike the Android side (ShortcutManagerCompat, fully implemented), the iOS
 * path needs two things this Kotlin-only framework can't own, (1) `UIApplication.shortcutItems` is not
 * cleanly exposed in the Kotlin/Native iOS SDK binding used here, and (2) handling a tapped shortcut requires
 * AppDelegate Swift glue (`application(_:performActionFor:completionHandler:)`) that routes the item's deep
 * link, which lives in the iOS host app, not in shared code. Declaring the shortcuts in the iOS app's
 * `Info.plist` (`UIApplicationShortcutItems`) with the same `mileway://…` deep links is the static
 * equivalent. Kept as a no-op so the Koin binding + [AppShortcuts] contract stay uniform across platforms.
 */
class IosAppShortcuts : AppShortcuts {
    override fun setDynamicShortcuts(shortcuts: List<AppShortcut>) = Unit
}
