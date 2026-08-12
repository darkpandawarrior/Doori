package com.mileway.core.platform

/**
 * Deep-links into the OS settings screens this app cannot substitute for.
 *
 * This exists for exactly one situation, and it is the situation that decides whether a mileage
 * app works at all: once a user has *permanently* denied background location, an in-app permission
 * request silently does nothing on both Android and iOS. There is no API to ask again. The only
 * recovery is to send them to the system settings page and say what to change.
 *
 * A permission primer that cannot do that has a dead end where its most important path should be,
 * and a disabled or no-op "Open Settings" button is worse than none — it tells the user the app
 * tried, when it did not.
 *
 * Deliberately not folded into [UrlOpener]: on iOS the target genuinely is a URL, but on Android it
 * is an Intent with a `package:` data URI that `ACTION_VIEW` will not resolve. One interface whose
 * behaviour silently differs per platform is how you get a feature that works in the simulator and
 * fails on a phone.
 */
interface SystemSettingsOpener {
    /**
     * The app's own settings page, where location permission can be re-granted.
     *
     * Implementations must fail quietly rather than crash: a device with no resolvable settings
     * activity is rare, but the app being unable to render a permission screen because of it is
     * unacceptable.
     */
    fun openAppSettings()

    /**
     * The battery-optimisation exemption screen.
     *
     * Aggressive OEM power management is a leading cause of a tracking service being killed
     * mid-drive, which shows up to the user as lost distance rather than as a crash. Falls back to
     * [openAppSettings] where no dedicated screen exists — on iOS there is no equivalent at all.
     */
    fun openBatteryOptimisationSettings() = openAppSettings()
}
