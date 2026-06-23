package com.miletracker.feature.agent.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.miletracker.feature.agent.ui.screens.AgentChatScreen
import com.miletracker.feature.agent.ui.screens.AgentHistoryScreen

object AgentRoutes {
    const val CHAT = "agent/chat"
    const val HISTORY = "agent/history"
}

fun NavGraphBuilder.agentGraph(navController: NavController) {
    composable(AgentRoutes.CHAT) {
        AgentChatScreen(
            onBack = { navController.popBackStack() },
            onOpenHistory = { navController.navigate(AgentRoutes.HISTORY) },
        )
    }
    composable(AgentRoutes.HISTORY) {
        AgentHistoryScreen(
            onBack = { navController.popBackStack() },
            onConversationSelected = { navController.navigate(AgentRoutes.CHAT) { popUpTo(AgentRoutes.CHAT) { inclusive = true } } },
        )
    }
}
