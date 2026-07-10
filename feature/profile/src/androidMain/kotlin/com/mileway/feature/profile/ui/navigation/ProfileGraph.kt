package com.mileway.feature.profile.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mileway.feature.profile.ui.screens.AccountDeletionScreen
import com.mileway.feature.profile.ui.screens.ActiveSessionsScreen
import com.mileway.feature.profile.ui.screens.AdvanceHistoryScreen
import com.mileway.feature.profile.ui.screens.AdvanceRequestDetailsScreen
import com.mileway.feature.profile.ui.screens.AnalyticsDetailScreen
import com.mileway.feature.profile.ui.screens.AnalyticsHomeScreen
import com.mileway.feature.profile.ui.screens.AskAdvanceFormScreen
import com.mileway.feature.profile.ui.screens.ClubBenefitsScreen
import com.mileway.feature.profile.ui.screens.ConnectedAccountsScreen
import com.mileway.feature.profile.ui.screens.CouponsScreen
import com.mileway.feature.profile.ui.screens.DelegationScreen
import com.mileway.feature.profile.ui.screens.DemoSettingsScreen
import com.mileway.feature.profile.ui.screens.DocumentDetailScreen
import com.mileway.feature.profile.ui.screens.EcoDashboardScreen
import com.mileway.feature.profile.ui.screens.EmergencyContactsScreen
import com.mileway.feature.profile.ui.screens.FavouriteRoutesScreen
import com.mileway.feature.profile.ui.screens.HelpScreen
import com.mileway.feature.profile.ui.screens.IncentiveProgramsScreen
import com.mileway.feature.profile.ui.screens.ManagerReporteeDetailScreen
import com.mileway.feature.profile.ui.screens.ManagerReporteesScreen
import com.mileway.feature.profile.ui.screens.MarketingHubScreen
import com.mileway.feature.profile.ui.screens.MySubscriptionScreen
import com.mileway.feature.profile.ui.screens.MyTicketsScreen
import com.mileway.feature.profile.ui.screens.NotificationCentreScreen
import com.mileway.feature.profile.ui.screens.OrgChartScreen
import com.mileway.feature.profile.ui.screens.PlansScreen
import com.mileway.feature.profile.ui.screens.PluginManagerScreen
import com.mileway.feature.profile.ui.screens.PreferencesScreen
import com.mileway.feature.profile.ui.screens.ProfileDetailsScreen
import com.mileway.feature.profile.ui.screens.ProfileScreen
import com.mileway.feature.profile.ui.screens.QrHomeScreen
import com.mileway.feature.profile.ui.screens.ReferralHubScreen
import com.mileway.feature.profile.ui.screens.RewardsScreen
import com.mileway.feature.profile.ui.screens.RootGuardScreen
import com.mileway.feature.profile.ui.screens.SavedPlacesScreen
import com.mileway.feature.profile.ui.screens.SelfAuditScreen
import com.mileway.feature.profile.ui.screens.SettingsScreen
import com.mileway.feature.profile.ui.screens.SupportChatScreen
import com.mileway.feature.profile.ui.screens.SupportHubScreen
import com.mileway.feature.profile.ui.screens.VehicleGarageScreen
import com.mileway.feature.profile.ui.screens.VerificationCentreScreen

object ProfileRoutes {
    const val HOME = "profile_home"
    const val DETAILS = "profile_details"
    const val PREFERENCES = "preferences"
    const val SETTINGS = "profile_settings"
    const val SUPPORT_HUB = "profile/support"
    const val SUPPORT_CHAT = "profile/support/chat"
    const val HELP = "profile_help"
    const val MY_TICKETS = "profile/help/my_tickets"
    const val NOTIFICATIONS = "profile/notifications"
    const val ADVANCE_HISTORY = "profile/advance"
    const val ASK_ADVANCE = "profile/advance/new"
    const val ADVANCE_DETAILS = "profile/advance/{advanceId}"
    const val ANALYTICS_HOME = "profile/analytics"
    const val ANALYTICS_DETAIL = "profile/analytics/{category}"
    const val DELEGATION = "profile/delegation"
    const val ACTIVE_SESSIONS = "profile/sessions"
    const val CONNECTED_ACCOUNTS = "profile/connected_accounts"
    const val QR_HOME = "profile/qr"
    const val DEMO_SETTINGS = "profile/demo_settings"
    const val ROOT_GUARD = "profile/root_guard"
    const val ROOT_GUARD_DETECTED = "profile/root_guard_detected"
    const val ORG_CHART = "profile/org_chart"
    const val PLUGINS = "profile/plugins"
    const val SAVED_PLACES = "profile/saved_places"
    const val EMERGENCY_CONTACTS = "profile/emergency_contacts"
    const val VERIFICATION_CENTRE = "profile/verification"
    const val VERIFICATION_DOCUMENT = "profile/verification/{docType}"
    const val REFERRAL_HUB = "profile/referral"
    const val COUPONS = "profile/coupons"
    const val REWARDS = "profile/rewards"
    const val MARKETING_HUB = "profile/campaigns"
    const val CLUB = "profile/club"
    const val PLANS = "profile/plans"
    const val MY_SUBSCRIPTION = "profile/my_subscription"
    const val INCENTIVES = "profile/incentives"
    const val ACCOUNT_DELETION = "profile/account_deletion"
    const val MANAGER_REPORTEES = "profile/manager_reportees"
    const val MANAGER_REPORTEE = "profile/manager_reportees/{code}"
    const val GARAGE = "profile/garage"
    const val SELF_AUDIT = "profile/garage/{vehicleId}/audit"
    const val ECOMETER = "profile/ecometer"
    const val FAVOURITES = "profile/favourites"

    fun selfAuditRoute(vehicleId: String) = "profile/garage/$vehicleId/audit"

    fun managerReporteeRoute(code: String) = "profile/manager_reportees/$code"

    fun analyticsDetailRoute(category: String) = "profile/analytics/$category"

    fun advanceDetailsRoute(advanceId: String) = "profile/advance/$advanceId"

    fun verificationDocumentRoute(docType: String) = "profile/verification/$docType"
}

/**
 * Profile nav graph.
 *
 * The Account hub ([ProfileScreen]) routes into the new full-detail surfaces
 * ([ProfileDetailsScreen], [PreferencesScreen]) and the existing Settings/Help routes.
 * Notifications and About & Support reuse the Help route in this demo.
 *
 * [onOpenDebugMenu] and [onStartTripForAdvance] are callbacks supplied by the app shell so the
 * profile module itself does not need to depend on :feature:tracking. [onSignedOut] (P2.4) is
 * supplied by the app shell so this module does not need to depend on `:app`'s `AppStage`.
 */
fun NavGraphBuilder.profileGraph(
    navController: NavHostController,
    onOpenDebugMenu: () -> Unit = {},
    onOpenCards: () -> Unit = {},
    onStartTripForAdvance: (advanceId: String, tripId: String) -> Unit = { _, _ -> },
    onSignedOut: () -> Unit = {},
) {
    composable(ProfileRoutes.HOME) {
        ProfileScreen(
            onOpenDetails = { navController.navigate(ProfileRoutes.DETAILS) },
            onOpenPreferences = { navController.navigate(ProfileRoutes.PREFERENCES) },
            onOpenNotifications = { navController.navigate(ProfileRoutes.NOTIFICATIONS) },
            onOpenSettings = { navController.navigate(ProfileRoutes.SETTINGS) },
            onOpenAboutSupport = { navController.navigate(ProfileRoutes.SUPPORT_HUB) },
            onOpenAdvance = { navController.navigate(ProfileRoutes.ADVANCE_HISTORY) },
            onOpenCards = onOpenCards,
            onOpenInsights = { navController.navigate(ProfileRoutes.ANALYTICS_HOME) },
            onOpenDelegation = { navController.navigate(ProfileRoutes.DELEGATION) },
            onOpenDemoSettings = { navController.navigate(ProfileRoutes.DEMO_SETTINGS) },
            onOpenQr = { navController.navigate(ProfileRoutes.QR_HOME) },
            onOpenSessions = { navController.navigate(ProfileRoutes.ACTIVE_SESSIONS) },
            onOpenSavedPlaces = { navController.navigate(ProfileRoutes.SAVED_PLACES) },
            onOpenEmergency = { navController.navigate(ProfileRoutes.EMERGENCY_CONTACTS) },
            onOpenVerification = { navController.navigate(ProfileRoutes.VERIFICATION_CENTRE) },
            onOpenReferral = { navController.navigate(ProfileRoutes.REFERRAL_HUB) },
            onOpenCoupons = { navController.navigate(ProfileRoutes.COUPONS) },
            onOpenRewards = { navController.navigate(ProfileRoutes.REWARDS) },
            onOpenCampaigns = { navController.navigate(ProfileRoutes.MARKETING_HUB) },
            onOpenClub = { navController.navigate(ProfileRoutes.CLUB) },
            onOpenSubscriptions = { navController.navigate(ProfileRoutes.PLANS) },
            onOpenIncentives = { navController.navigate(ProfileRoutes.INCENTIVES) },
            onOpenManagerView = { navController.navigate(ProfileRoutes.MANAGER_REPORTEES) },
            onOpenGarage = { navController.navigate(ProfileRoutes.GARAGE) },
            onOpenEcometer = { navController.navigate(ProfileRoutes.ECOMETER) },
            onOpenFavourites = { navController.navigate(ProfileRoutes.FAVOURITES) },
            onOpenAccountDeletion = { navController.navigate(ProfileRoutes.ACCOUNT_DELETION) },
            onSignedOut = onSignedOut,
        )
    }
    composable(ProfileRoutes.ECOMETER) {
        EcoDashboardScreen(onBack = { navController.popBackStack() })
    }
    composable(ProfileRoutes.FAVOURITES) {
        FavouriteRoutesScreen(onBack = { navController.popBackStack() })
    }
    composable(ProfileRoutes.GARAGE) {
        VehicleGarageScreen(
            onBack = { navController.popBackStack() },
            onOpenSelfAudit = { vehicleId -> navController.navigate(ProfileRoutes.selfAuditRoute(vehicleId)) },
        )
    }
    composable(
        route = ProfileRoutes.SELF_AUDIT,
        arguments = listOf(navArgument("vehicleId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val vehicleId = backStackEntry.arguments?.getString("vehicleId") ?: return@composable
        SelfAuditScreen(
            vehicleId = vehicleId,
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.MANAGER_REPORTEES) {
        ManagerReporteesScreen(
            onBack = { navController.popBackStack() },
            onOpenReportee = { code -> navController.navigate(ProfileRoutes.managerReporteeRoute(code)) },
        )
    }
    composable(
        route = ProfileRoutes.MANAGER_REPORTEE,
        arguments = listOf(navArgument("code") { type = NavType.StringType }),
    ) { backStackEntry ->
        val code = backStackEntry.arguments?.getString("code") ?: return@composable
        ManagerReporteeDetailScreen(
            code = code,
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.REWARDS) {
        RewardsScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.MARKETING_HUB) {
        MarketingHubScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.CLUB) {
        ClubBenefitsScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.PLANS) {
        PlansScreen(
            onBack = { navController.popBackStack() },
            onOpenManage = { navController.navigate(ProfileRoutes.MY_SUBSCRIPTION) },
        )
    }
    composable(ProfileRoutes.MY_SUBSCRIPTION) {
        MySubscriptionScreen(
            onBack = { navController.popBackStack() },
            onOpenPlans = { navController.navigate(ProfileRoutes.PLANS) },
        )
    }
    composable(ProfileRoutes.INCENTIVES) {
        IncentiveProgramsScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.ACCOUNT_DELETION) {
        AccountDeletionScreen(
            onBack = { navController.popBackStack() },
            onAccountDeleted = onSignedOut,
        )
    }
    composable(ProfileRoutes.REFERRAL_HUB) {
        ReferralHubScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.COUPONS) {
        CouponsScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.VERIFICATION_CENTRE) {
        VerificationCentreScreen(
            onBack = { navController.popBackStack() },
            onOpenDocument = { docType -> navController.navigate(ProfileRoutes.verificationDocumentRoute(docType)) },
        )
    }
    composable(
        route = ProfileRoutes.VERIFICATION_DOCUMENT,
        arguments = listOf(navArgument("docType") { type = NavType.StringType }),
    ) { backStackEntry ->
        val docType = backStackEntry.arguments?.getString("docType") ?: return@composable
        DocumentDetailScreen(
            docType = docType,
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.SAVED_PLACES) {
        SavedPlacesScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.EMERGENCY_CONTACTS) {
        EmergencyContactsScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.ACTIVE_SESSIONS) {
        ActiveSessionsScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.DETAILS) {
        ProfileDetailsScreen(
            onBack = { navController.popBackStack() },
            onOpenOrgChart = { navController.navigate(ProfileRoutes.ORG_CHART) },
        )
    }
    composable(ProfileRoutes.ORG_CHART) {
        OrgChartScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.PREFERENCES) {
        PreferencesScreen(
            onBack = { navController.popBackStack() },
            onOpenNotificationCenter = { navController.navigate(ProfileRoutes.NOTIFICATIONS) },
            onOpenConnectedAccounts = { navController.navigate(ProfileRoutes.CONNECTED_ACCOUNTS) },
        )
    }
    composable(ProfileRoutes.CONNECTED_ACCOUNTS) {
        ConnectedAccountsScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.SETTINGS) {
        SettingsScreen(
            onBack = { navController.popBackStack() },
            onOpenDebugMenu = onOpenDebugMenu,
            onOpenPlugins = { navController.navigate(ProfileRoutes.PLUGINS) },
            onOpenAccountDeletion = { navController.navigate(ProfileRoutes.ACCOUNT_DELETION) },
        )
    }
    composable(ProfileRoutes.PLUGINS) {
        PluginManagerScreen(
            onBack = { navController.popBackStack() },
        )
    }
    composable(ProfileRoutes.SUPPORT_HUB) {
        SupportHubScreen(
            onBack = { navController.popBackStack() },
            onOpenFaq = { navController.navigate(ProfileRoutes.HELP) },
            onOpenTickets = { navController.navigate(ProfileRoutes.MY_TICKETS) },
            onOpenChat = { navController.navigate(ProfileRoutes.SUPPORT_CHAT) },
        )
    }
    composable(ProfileRoutes.SUPPORT_CHAT) {
        SupportChatScreen(onBack = { navController.popBackStack() })
    }
    composable(ProfileRoutes.HELP) {
        HelpScreen(
            onBack = { navController.popBackStack() },
            onOpenMyTickets = { navController.navigate(ProfileRoutes.MY_TICKETS) },
        )
    }
    composable(ProfileRoutes.MY_TICKETS) {
        MyTicketsScreen(
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
            onOpenDetail = { advanceId -> navController.navigate(ProfileRoutes.advanceDetailsRoute(advanceId)) },
        )
    }
    composable(ProfileRoutes.ASK_ADVANCE) {
        AskAdvanceFormScreen(
            onBack = { navController.popBackStack() },
            onSubmitted = { navController.popBackStack() },
        )
    }
    composable(
        route = ProfileRoutes.ADVANCE_DETAILS,
        arguments = listOf(navArgument("advanceId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val advanceId = backStackEntry.arguments?.getString("advanceId") ?: return@composable
        AdvanceRequestDetailsScreen(
            advanceId = advanceId,
            onBack = { navController.popBackStack() },
            onStartTrip = onStartTripForAdvance,
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
