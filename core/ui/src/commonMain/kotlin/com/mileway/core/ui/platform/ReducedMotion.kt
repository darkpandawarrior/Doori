package com.mileway.core.ui.platform

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * PLAN_V36 P6 (spec §6.3): app-level "skip decorative animation" signal — auto-advancing
 * carousels, infinite pulse loops and list-entry enter animations gate on this instead of running
 * unconditionally.
 *
 * No expect/actual here: this is a bare default, seeded per-platform at each app root (Android:
 * `LauncherActivity.AppEntry`; iOS: `MilewayAppViewController`) by reading the OS "reduce motion"
 * setting right there and wrapping with `CompositionLocalProvider` — exactly like
 * [LocalManagerProvider] seeds its managers, minus the expect/actual since there's no
 * Activity/UIViewController object to resolve, just a boolean. Defaults to `false` everywhere else
 * (desktop, previews, tests, commonTest) so a read outside a provider degrades to "animate
 * normally" instead of crashing.
 *
 * // ponytail: no core:platform expect/actual for the OS read itself (unlike e.g.
 * // `currentDeviceManufacturer()`) — the two platform roots already own their own bootstrap code,
 * // so reading Settings.Global/UIAccessibility right there is the smaller diff, and it's a
 * // one-shot read (not observed for live changes mid-session). Promote to a proper
 * // core:platform expect/actual (+ a ContentObserver/NSNotification for live updates) if a third
 * // call site ever needs it, or if a reviewer wants the setting to react without a relaunch.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }
