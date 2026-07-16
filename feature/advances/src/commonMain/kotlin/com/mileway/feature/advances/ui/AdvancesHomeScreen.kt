package com.mileway.feature.advances.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.mvi.ScreenStateContent
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.advances_empty_petty
import com.mileway.core.ui.resources.advances_empty_qr
import com.mileway.core.ui.resources.advances_home_subtitle
import com.mileway.core.ui.resources.advances_home_title
import com.mileway.core.ui.resources.advances_request_a_card
import com.mileway.core.ui.resources.advances_section_active
import com.mileway.core.ui.resources.advances_section_past
import com.mileway.core.ui.resources.advances_tab_petty
import com.mileway.core.ui.resources.advances_tab_qr
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.advances.model.PettyCard
import com.mileway.feature.advances.model.QrCard
import com.mileway.feature.advances.ui.components.PettyAdvanceCardFace
import com.mileway.feature.advances.ui.components.QrCardFace
import com.mileway.feature.advances.viewmodel.AdvancesHomeAction
import com.mileway.feature.advances.viewmodel.AdvancesHomeUiState
import com.mileway.feature.advances.viewmodel.AdvancesHomeViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** PLAN_V35.P4: advances home — Petty Advances / QR Cards tabs, each an active carousel + past list. */
@Composable
fun AdvancesHomeScreen(
    onOpenPettyCard: (Long) -> Unit,
    onOpenQrCard: (Long) -> Unit,
    onRequestPettyAdvance: () -> Unit,
    onRequestQrCard: () -> Unit,
    viewModel: AdvancesHomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AdvancesHomeContent(
        state = state,
        onAction = viewModel::onAction,
        onOpenPettyCard = onOpenPettyCard,
        onOpenQrCard = onOpenQrCard,
        onRequestPettyAdvance = onRequestPettyAdvance,
        onRequestQrCard = onRequestQrCard,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdvancesHomeContent(
    state: AdvancesHomeUiState,
    onAction: (AdvancesHomeAction) -> Unit,
    onOpenPettyCard: (Long) -> Unit,
    onOpenQrCard: (Long) -> Unit,
    onRequestPettyAdvance: () -> Unit,
    onRequestQrCard: () -> Unit,
) {
    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.advances_home_title),
                subtitle = stringResource(Res.string.advances_home_subtitle),
                depth = NavigationDepth.LEVEL_1,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = if (state.selectedTab == 0) onRequestPettyAdvance else onRequestQrCard,
                shape = DesignTokens.Shape.button,
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.advances_request_a_card))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = state.selectedTab) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { onAction(AdvancesHomeAction.SelectTab(0)) },
                    text = { Text(stringResource(Res.string.advances_tab_petty)) },
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { onAction(AdvancesHomeAction.SelectTab(1)) },
                    text = { Text(stringResource(Res.string.advances_tab_qr)) },
                )
            }
            when (state.selectedTab) {
                0 ->
                    PettyTab(
                        active = state.activePettyCards,
                        past = state.pastPettyCards,
                        onOpen = onOpenPettyCard,
                        onRetry = { onAction(AdvancesHomeAction.Refresh) },
                    )
                else ->
                    QrTab(
                        active = state.activeQrCards,
                        past = state.pastQrCards,
                        onOpen = onOpenQrCard,
                        onRetry = { onAction(AdvancesHomeAction.Refresh) },
                    )
            }
        }
    }
}

@Composable
private fun PettyTab(
    active: ScreenState<List<PettyCard>>,
    past: ScreenState<List<PettyCard>>,
    onOpen: (Long) -> Unit,
    onRetry: () -> Unit,
) {
    ScreenStateContent(
        state = active,
        modifier = Modifier.fillMaxSize(),
        onRetry = onRetry,
        empty = { EmptySection(stringResource(Res.string.advances_empty_petty)) },
    ) { activeCards ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
        ) {
            item { SectionLabel(stringResource(Res.string.advances_section_active)) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.carouselSpacing)) {
                    items(activeCards, key = { it.id }) { card ->
                        PettyAdvanceCardFace(
                            card = card,
                            modifier = Modifier.width(280.dp).clickable { onOpen(card.id) },
                        )
                    }
                }
            }
            val pastCards = (past as? ScreenState.Content)?.data.orEmpty()
            if (pastCards.isNotEmpty()) {
                item { SectionLabel(stringResource(Res.string.advances_section_past)) }
                items(pastCards, key = { it.id }) { card ->
                    PettyAdvanceCardFace(card = card, modifier = Modifier.fillMaxWidth().clickable { onOpen(card.id) })
                }
            }
        }
    }
}

@Composable
private fun QrTab(
    active: ScreenState<List<QrCard>>,
    past: ScreenState<List<QrCard>>,
    onOpen: (Long) -> Unit,
    onRetry: () -> Unit,
) {
    ScreenStateContent(
        state = active,
        modifier = Modifier.fillMaxSize(),
        onRetry = onRetry,
        empty = { EmptySection(stringResource(Res.string.advances_empty_qr)) },
    ) { activeCards ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
        ) {
            item { SectionLabel(stringResource(Res.string.advances_section_active)) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.carouselSpacing)) {
                    items(activeCards, key = { it.id }) { card ->
                        QrCardFace(
                            card = card,
                            onScan = { onOpen(card.id) },
                            modifier = Modifier.width(280.dp).clickable { onOpen(card.id) },
                        )
                    }
                }
            }
            val pastCards = (past as? ScreenState.Content)?.data.orEmpty()
            if (pastCards.isNotEmpty()) {
                item { SectionLabel(stringResource(Res.string.advances_section_past)) }
                items(pastCards, key = { it.id }) { card ->
                    QrCardFace(card = card, onScan = { onOpen(card.id) }, modifier = Modifier.fillMaxWidth().clickable { onOpen(card.id) })
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun EmptySection(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.xxl),
    )
}
