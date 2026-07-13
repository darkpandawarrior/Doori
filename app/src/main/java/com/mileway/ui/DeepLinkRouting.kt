package com.mileway.ui

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.mileway.core.common.deeplink.DeepLinkTarget
import com.mileway.feature.logging.ui.navigation.LoggingRoutes
import com.mileway.feature.profile.ui.navigation.ProfileRoutes
import com.mileway.feature.tracking.ui.navigation.TrackingRoutes

/**
 * DL.4: maps a resolved [DeepLinkTarget] to a concrete nav route. Section targets go to their graph
 * route; sub-targets (checkin / expense / settings) go to the specific nested destination, the JetBrains
 * NavController rebuilds the parent-graph back stack to a nested node, so the bottom-bar hierarchy check
 * still resolves the right tab. [DeepLinkTarget.Unknown] → null (ignored).
 *
 * Chosen over threading navDeepLink() through every feature graph extension: the app already deep-links via
 * a single navigate(route) (cold start + onNewIntent), so a centralized, unit-tested mapping reaches the
 * same sub-destinations with far less surface area and risk.
 */
fun DeepLinkTarget.toAppRoute(): String? =
    when (this) {
        DeepLinkTarget.Home -> AppGraph.HOME
        DeepLinkTarget.Track -> AppGraph.TRACK
        DeepLinkTarget.TrackCheckIn -> TrackingRoutes.CHECK_IN_HISTORY
        DeepLinkTarget.Log -> AppGraph.LOG
        DeepLinkTarget.LogExpense -> LoggingRoutes.EXPENSE_HISTORY
        DeepLinkTarget.Profile -> AppGraph.PROFILE
        DeepLinkTarget.ProfileSettings -> ProfileRoutes.SETTINGS
        DeepLinkTarget.Approvals -> AppGraph.APPROVALS
        DeepLinkTarget.Payables -> AppGraph.PAYABLES
        // Referral redemption lives in Profile (RF.4); deep-link lands there.
        is DeepLinkTarget.Referral -> AppGraph.PROFILE
        is DeepLinkTarget.Unknown -> null
    }

/**
 * PLAN_V29 P29.S.1/S.5/S.6: the shared "best-effort resolve-and-navigate" tap handler — a tapped
 * search result, a tapped quick action, and a tapped notification card all reduce to the same
 * "route if resolvable, otherwise no-op" shape. A `null` [route] (an unresolved type/deeplink) is
 * silently ignored, matching the section-graph precedent already documented on [toAppRoute].
 */
fun NavHostController.navigateToSectionRoute(route: String?) {
    if (route == null) return
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
