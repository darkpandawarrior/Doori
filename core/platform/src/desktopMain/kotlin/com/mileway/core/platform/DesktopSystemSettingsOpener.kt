package com.mileway.core.platform

/**
 * Desktop has no per-app permission page to deep-link into, and no runtime location permission to
 * recover from. This is a genuine no-op rather than a stub awaiting an implementation — the UI that
 * calls it should not be reachable here at all, and a comment saying so is more useful to the next
 * reader than a TODO that will never be done.
 */
class DesktopSystemSettingsOpener : SystemSettingsOpener {
    override fun openAppSettings() = Unit
}
