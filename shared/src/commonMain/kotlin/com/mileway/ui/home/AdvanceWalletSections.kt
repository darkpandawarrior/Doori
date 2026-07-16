package com.mileway.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.shared_home_advance_cards
import com.mileway.core.ui.resources.shared_home_qr_cards
import com.mileway.core.ui.resources.shared_home_request_card
import com.mileway.feature.advances.data.AdvancesRepository
import com.mileway.feature.advances.data.QrCardsRepository
import com.mileway.feature.advances.ui.components.PettyAdvanceCardFace
import com.mileway.feature.advances.ui.components.QrCardFace
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Width of a wallet card in the home carousels — matches the mock MyCards card width. */
private val WalletCardWidth = 280.dp

/**
 * PLAN_V35: the real advance-wallet home sections (petty cash + QR cards), replacing the "My
 * Cards" mocks for these two products. Blueprint structure (section header + card carousel +
 * request CTA), Mileway primitives; data straight off :feature:advances' repositories. Empty
 * lists render nothing (section omitted, reference behaviour for home sections).
 */
@Composable
fun AdvanceCardsHomeSection(
    onOpenAdvances: () -> Unit,
    onRequestAdvance: () -> Unit,
    modifier: Modifier = Modifier,
    repository: AdvancesRepository = koinInject(),
) {
    val cards by repository.activePettyCards().collectAsStateWithLifecycle(initialValue = emptyList())
    if (cards.isEmpty()) return
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(com.mileway.core.ui.theme.DesignTokens.Spacing.m),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HomeSectionHeader(stringResource(Res.string.shared_home_advance_cards), Icons.Filled.AccountBalanceWallet)
            TextButton(onClick = onRequestAdvance) {
                Text(stringResource(Res.string.shared_home_request_card), style = MaterialTheme.typography.labelMedium)
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(com.mileway.core.ui.theme.DesignTokens.Spacing.m),
            contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
            items(cards, key = { it.id }) { card ->
                PettyAdvanceCardFace(
                    card = card,
                    modifier =
                        Modifier
                            .width(WalletCardWidth)
                            .clip(com.mileway.core.ui.theme.DesignTokens.Shape.roundedLg)
                            .clickable(onClick = onOpenAdvances),
                )
            }
        }
    }
}

/** PLAN_V35: QR-card home carousel — same shape as [AdvanceCardsHomeSection]. */
@Composable
fun QrCardsHomeSection(
    onOpenAdvances: () -> Unit,
    onRequestQrCard: () -> Unit,
    modifier: Modifier = Modifier,
    repository: QrCardsRepository = koinInject(),
) {
    val cards by repository.activeQrCards().collectAsStateWithLifecycle(initialValue = emptyList())
    if (cards.isEmpty()) return
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(com.mileway.core.ui.theme.DesignTokens.Spacing.m),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HomeSectionHeader(stringResource(Res.string.shared_home_qr_cards), Icons.Filled.QrCode)
            TextButton(onClick = onRequestQrCard) {
                Text(stringResource(Res.string.shared_home_request_card), style = MaterialTheme.typography.labelMedium)
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(com.mileway.core.ui.theme.DesignTokens.Spacing.m),
            contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
            items(cards, key = { it.id }) { card ->
                QrCardFace(
                    card = card,
                    onScan = onOpenAdvances,
                    modifier =
                        Modifier
                            .width(WalletCardWidth)
                            .clip(com.mileway.core.ui.theme.DesignTokens.Shape.roundedLg)
                            .clickable(onClick = onOpenAdvances),
                )
            }
        }
    }
}
