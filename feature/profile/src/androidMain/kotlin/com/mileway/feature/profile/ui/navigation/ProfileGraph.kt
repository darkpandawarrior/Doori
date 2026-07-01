package com.mileway.feature.profile.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mileway.feature.profile.ui.screens.AdvanceHistoryScreen
import com.mileway.feature.profile.ui.screens.AnalyticsDetailScreen
import com.mileway.feature.profile.ui.screens.AnalyticsHomeScreen
import com.mileway.feature.profile.ui.screens.AskAdvanceFormScreen
import com.mileway.feature.profile.ui.screens.DelegationScreen
import com.mileway.feature.profile.ui.screens.DemoSettingsScreen
import com.mileway.feature.profile.ui.screens.HelpScreen
import com.mileway.feature.profile.ui.screens.NotificationCentreScreen
import com.mileway.feature.profile.ui.screens.PreferencesScreen
import com.mileway.feature.profile.ui.screens.ProfileDetailsScreen
import com.mileway.feature.profile.ui.screens.ProfileScreen
import com.mileway.feature.profile.ui.screens.QrHomeScreen
import com.mileway.feature.profile.ui.screens.RootGuardScreen
import com.mileway.feature.profile.ui.screens.SettingsScreen

object ProfileRoutes {
    const val HOME = "profile_home"
    const val DETAILS = "profile_details"
    const val PREFERENCES = "preferences"
    const val SETTINGS = "profile_settings"
    const val HELP = "profile_help"
    const val NOTIFICATIONS = "profile/notifications"
    const val ADVANCE_HISTORY = "profile/advance"
    const val ASK_ADVANCE = "profile/advance/new"
    const val ANALYTICS_HOME = "profile/analytics"
    const val ANALYTICS_DETAIL = "profile/analytics/{category}"
    const val DELEGATION = "profile/delegation"
    const val QR_HOME = "profile/qr"
    const val DEMO_SETTINGS = "profile/demo_settings"
    const val ROOT_GUARD = "profile/root_guard"
    const val ROOT_GUARD_DETECTED = "profile/root_guard_detected"

    fun analyticsDetailRoute(category: String) = "profile/analytics/$category"
}

/**
 * Profile nav graph.
 *
 * The Account hub ([ProfileScreen]) routes into the new full-detail surfaces
 * ([ProfileDetailsScreen], [PreferencesScreen]) and the existing Settings/Help routes.
 * Notifications and About & Support reuse the Help route in this demo.
 *
 * [onOpenDebugMenu] is a callback supplied by the app shell so the profile
 * module itself does not need to depend on :feature:tracking.
 */
fun NavGraphBuilder.profileGraph(
    navController: NavHostController,
    onOpenDebugMenu: () -> Unit = {},
    onOpenCards: () -> Unit = {},
) {
    composable(ProfileRoutes.HOME) {
        ProfileScreen(
            onOpenDetails = { navController.navigate(ProfileRoutes.DETAILS) },
            onOpenPreferences = { navController.navigate(ProfileRoutes.PREFERENCES) },
            onOpenNotifications = { navController.navigate(ProfileRoutes.NOTIFICATIONS) },
            onOpenSettings = { navController.navigate(ProfileRoutes.SETTINGS) },
            onOpenAboutSupport = { navController.navigate(ProfileRoutes.HELP) },
            onOpenAdvance = { navController.navigate(ProfileRoutes.ADVANCE_HISTORY) },
            onOpenCards = onOpenCards,
            onOpenInsights = { navController.navigate(ProfileRoutes.ANALYTICS_HOME) },
            onOpenDelegation = { navController.navigate(ProfileRoutes.DELEGATION) },
            onOpenDemoSettings = { navController.navigate(ProfileRoutes.DEMO_SETTINGS) },
            onOpenQr = { navController.navigate(ProfileRoutes.QR_HOME) },
        )
    }
    composable(ProfileRoutes.DETAILS) {
        ProfileDetailsScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.PREFERENCES) {
        PreferencesScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.SETTINGS) {
        SettingsScreen(
            onBack = { navController.popBackStack() },
            onOpenDebugMenu = onOpenDebugMenu,
        )
    }
    composable(ProfileRoutes.HELP) {
        HelpScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.NOTIFICATIONS) {
        NotificationCentreScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.ADVANCE_HISTORY) {
        AdvanceHistoryScreen(
            onBack = { navController.popBackStack() },
            onRequestAdvance = { navController.navigate(ProfileRoutes.ASK_ADVANCE) },
        )
    }
    composable(ProfileRoutes.ASK_ADVANCE) {
        AskAdvanceFormScreen(
            onBack = { navController.popBackStack() },
            onSubmitted = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.ANALYTICS_HOME) {
        AnalyticsHomeScreen(
            onBack = { navController.popBackStack() },
            onOpenDetail = { category -> navController.navigate(ProfileRoutes.analyticsDetailRoute(category)) },
        )
    }
    composable(
        route = ProfileRoutes.ANALYTICS_DETAIL,
        arguments = listOf(navArgument("category") { type = NavType.StringType }),
    ) { backStackEntry ->
        val category = backStackEntry.arguments?.getString("category") ?: return@composable
        AnalyticsDetailScreen(
            category = category,
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.DELEGATION) {
        DelegationScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.QR_HOME) {
        QrHomeScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.DEMO_SETTINGS) {
        DemoSettingsScreen(
            onBack = { navController.popBackStack() },
            onOpenRootGuard = { navController.navigate(ProfileRoutes.ROOT_GUARD) },
            onOpenRootGuardDetected = { navController.navigate(ProfileRoutes.ROOT_GUARD_DETECTED) },
        )
    }
    composable(ProfileRoutes.ROOT_GUARD) {
        RootGuardScreen(
            onContinue = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.ROOT_GUARD_DETECTED) {
        RootGuardScreen(
            onContinue = { navController.popBackStack() },
            signals = listOf("su binary found at /system/xbin/su", "test-keys build", "Magisk detected"),
        )
    }
}
