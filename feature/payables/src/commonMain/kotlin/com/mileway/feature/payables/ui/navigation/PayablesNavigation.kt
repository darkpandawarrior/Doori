package com.mileway.feature.payables.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.mileway.core.ui.theme.MilewayDomain
import com.mileway.core.ui.theme.MilewayDomainTheme
import com.mileway.feature.payables.ui.screens.CreatePurchaseRequestScreen
import com.mileway.feature.payables.ui.screens.PayablesHomeScreen
import com.mileway.feature.payables.ui.screens.PurchaseRequestDetailsScreen
import com.mileway.feature.payables.ui.screens.PurchaseRequestSuccessScreen
import org.koin.compose.viewmodel.koinViewModel

object PayablesRoutes {
    const val HOME = "payables_home"
    const val CREATE = "payables/create"
    const val SUCCESS = "payables/success"
    const val DETAIL = "payables/detail/{id}"

    fun detailRoute(id: String) = "payables/detail/$id"
}

// Domain-scoped once at the feature's nav entry (LAYERS.md Layer 3) — PAYABLES is the quietest
// domain (back-office finance), and every screen below picks up its accent through
// MaterialTheme.colorScheme.primary with no colour at the call site.
fun NavGraphBuilder.payablesGraph(navController: NavHostController) {
    composable(PayablesRoutes.HOME) {
        MilewayDomainTheme(MilewayDomain.PAYABLES) {
            PayablesHomeScreen(
                onNewRequest = { navController.navigate(PayablesRoutes.CREATE) },
                onOpenPo = { id -> navController.navigate(PayablesRoutes.detailRoute(id)) },
            )
        }
    }

    composable(PayablesRoutes.CREATE) { entry ->
        val viewModel =
            koinViewModel<com.mileway.feature.payables.viewmodel.PayablesViewModel>(
                viewModelStoreOwner = entry,
            )
        MilewayDomainTheme(MilewayDomain.PAYABLES) {
            CreatePurchaseRequestScreen(
                onBack = { navController.popBackStack() },
                onSubmitted = {
                    navController.navigate(PayablesRoutes.SUCCESS) {
                        popUpTo(PayablesRoutes.CREATE) { inclusive = true }
                    }
                },
                viewModel = viewModel,
            )
        }
    }

    composable(PayablesRoutes.SUCCESS) {
        val createEntry =
            runCatching {
                navController.getBackStackEntry(PayablesRoutes.CREATE)
            }.getOrNull()

        val viewModel =
            if (createEntry != null) {
                koinViewModel<com.mileway.feature.payables.viewmodel.PayablesViewModel>(
                    viewModelStoreOwner = createEntry,
                )
            } else {
                koinViewModel()
            }

        MilewayDomainTheme(MilewayDomain.PAYABLES) {
            PurchaseRequestSuccessScreen(
                onCreateAnother = {
                    navController.navigate(PayablesRoutes.CREATE) {
                        popUpTo(PayablesRoutes.CREATE) { inclusive = true }
                    }
                },
                onBackToPayables = {
                    navController.navigate(PayablesRoutes.HOME) {
                        popUpTo(PayablesRoutes.HOME) { inclusive = true }
                    }
                },
                viewModel = viewModel,
            )
        }
    }

    composable(
        route = PayablesRoutes.DETAIL,
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) { backStackEntry ->
        val id = backStackEntry.arguments?.read { getStringOrNull("id") } ?: return@composable
        MilewayDomainTheme(MilewayDomain.PAYABLES) {
            PurchaseRequestDetailsScreen(
                poId = id,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
