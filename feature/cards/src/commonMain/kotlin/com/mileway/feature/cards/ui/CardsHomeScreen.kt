package com.mileway.feature.cards.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.mvi.ScreenStateContent
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.cards_home_subtitle
import com.mileway.core.ui.resources.cards_request_a_card
import com.mileway.core.ui.resources.cards_tab_cards
import com.mileway.core.ui.resources.cards_tab_requests
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.cards.model.CardRequestModel
import com.mileway.feature.cards.ui.components.CardFace
import com.mileway.feature.cards.viewmodel.CardsHomeAction
import com.mileway.feature.cards.viewmodel.CardsHomeUiState
import com.mileway.feature.cards.viewmodel.CardsHomeViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CardsHomeScreen(
    onOpenCard: (Long) -> Unit,
    onRequestCard: () -> Unit,
    viewModel: CardsHomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CardsHomeContent(
        state = state,
        onAction = viewModel::onAction,
        onOpenCard = onOpenCard,
        onRequestCard = onRequestCard,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CardsHomeContent(
    state: CardsHomeUiState,
    onAction: (CardsHomeAction) -> Unit,
    onOpenCard: (Long) -> Unit,
    onRequestCard: () -> Unit,
) {
    val tabs = listOf(stringResource(Res.string.cards_tab_cards), stringResource(Res.string.cards_tab_requests))
    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.cards_tab_cards),
                subtitle = stringResource(Res.string.cards_home_subtitle),
                depth = NavigationDepth.LEVEL_1,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onRequestCard, shape = DesignTokens.Shape.button) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.cards_request_a_card))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = state.selectedTab) {
                val tabIcons = listOf(Icons.Filled.CreditCard, Icons.Filled.FactCheck)
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = state.selectedTab == index,
                        onClick = { onAction(CardsHomeAction.SelectTab(index)) },
                        text = { Text(title) },
                        icon = { Icon(tabIcons[index], contentDescription = null) },
                    )
                }
            }
            when (state.selectedTab) {
                0 ->
                    ScreenStateContent(
                        state = state.virtualCards,
                        modifier = Modifier.fillMaxSize(),
                        onRetry = { onAction(CardsHomeAction.Refresh) },
                    ) { cards ->
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(cards, key = { it.id }) { card ->
                                CardFace(
                                    card = card,
                                    modifier = Modifier.clickable { onOpenCard(card.id) },
                                )
                            }
                        }
                    }

                else ->
                    ScreenStateContent(
                        state = state.requests,
                        modifier = Modifier.fillMaxSize(),
                        onRetry = { onAction(CardsHomeAction.Refresh) },
                    ) { requests ->
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(requests, key = { it.id }) { request -> RequestRow(request) }
                        }
                    }
            }
        }
    }
}

@Composable
private fun RequestRow(request: CardRequestModel) {
    Card(shape = DesignTokens.Shape.roundedSm, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(request.cardType, style = MaterialTheme.typography.titleMedium)
            Text(
                text = request.status.name.replace('_', ' '),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            request.reason?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
