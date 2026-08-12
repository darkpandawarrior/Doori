package com.mileway.feature.approvals.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.mileway.core.ui.theme.MilewayDomain
import com.mileway.core.ui.theme.MilewayDomainTheme
import com.mileway.feature.approvals.ui.screens.ApprovalDetailsScreen
import com.mileway.feature.approvals.ui.screens.ApprovalsScreen
import com.mileway.feature.approvals.ui.screens.ClarificationHistoryScreen

object ApprovalsRoutes {
    const val HOME = "approvals_home"
    const val DETAIL = "approval_detail/{id}"

    /** P28.2: top-level entry point, reachable from [ApprovalsScreen] independent of any one approval. */
    const val CLARIFICATION_HISTORY = "clarification_history"

    fun detail(id: String) = "approval_detail/$id"
}

// Domain-scoped once at the feature's nav entry (LAYERS.md Layer 3), not per screen — every
// composable below picks the APPROVALS accent up through MaterialTheme.colorScheme.primary with
// no colour at the call site. This is what replaced ApprovalsScreen's hard-coded purple gradient.
fun NavGraphBuilder.approvalsGraph(navController: NavHostController) {
    composable(ApprovalsRoutes.HOME) {
        MilewayDomainTheme(MilewayDomain.APPROVALS) {
            ApprovalsScreen(
                onOpenDetail = { id -> navController.navigate(ApprovalsRoutes.detail(id)) },
                onOpenClarificationHistory = { navController.navigate(ApprovalsRoutes.CLARIFICATION_HISTORY) },
            )
        }
    }

    composable(
        route = ApprovalsRoutes.DETAIL,
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) { backStack ->
        val id = backStack.arguments?.read { getStringOrNull("id") } ?: return@composable
        MilewayDomainTheme(MilewayDomain.APPROVALS) {
            ApprovalDetailsScreen(
                approvalId = id,
                onBack = { navController.popBackStack() },
            )
        }
    }

    composable(ApprovalsRoutes.CLARIFICATION_HISTORY) {
        MilewayDomainTheme(MilewayDomain.APPROVALS) {
            ClarificationHistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenApproval = { id -> navController.navigate(ApprovalsRoutes.detail(id)) },
            )
        }
    }
}
