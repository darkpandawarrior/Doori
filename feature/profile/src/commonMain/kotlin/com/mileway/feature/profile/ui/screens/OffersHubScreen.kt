package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.campaign.Campaign
import com.mileway.core.data.coupon.Coupon
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.viewmodel.OffersHubViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V24 P12.9: the offers hub — two tabs (Coupons | Offers) over the existing P5.2 coupons + P5.4
 * campaigns repos (no new data). Tapping a row opens an offer-detail landing sheet. Reached from a
 * plugin-gated profile-hub tile (`offersHubEnabled`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersHubScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OffersHubViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var detailTitle by remember { mutableStateOf<String?>(null) }
    var detailBody by remember { mutableStateOf("") }

    fun openDetail(
        title: String,
        body: String,
    ) {
        detailTitle = title
        detailBody = body
    }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), modifier = modifier) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = ofr("offers_back", "Back"))
                }
                Text(ofr("offers_title", "Offers & rewards"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(ofr("offers_tab_coupons", "Coupons")) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(ofr("offers_tab_campaigns", "Offers")) })
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                if (selectedTab == 0) {
                    if (state.coupons.isEmpty()) {
                        item { EmptyOffers(ofr("offers_no_coupons", "No coupons available right now.")) }
                    }
                    items(state.coupons, key = { it.id }) { coupon ->
                        CouponRow(coupon = coupon, onClick = { openDetail(coupon.title, "${coupon.code}\n\n${coupon.terms}\n\n${coupon.expiryLabel}") })
                    }
                } else {
                    if (state.campaigns.isEmpty()) {
                        item { EmptyOffers(ofr("offers_no_campaigns", "No offers available right now.")) }
                    }
                    items(state.campaigns, key = { it.id }) { campaign ->
                        CampaignRow(campaign = campaign, onClick = { openDetail(campaign.name, campaign.description) })
                    }
                }
                item { Spacer(Modifier.navigationBarsPadding()) }
            }
        }
    }

    detailTitle?.let { title ->
        ModalBottomSheet(onDismissRequest = { detailTitle = null }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = DesignTokens.Spacing.l).padding(bottom = DesignTokens.Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(detailBody, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyOffers(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun CouponRow(
    coupon: Coupon,
    onClick: () -> Unit,
) {
    OfferCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(coupon.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(coupon.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Text(coupon.expiryLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AssistChip(onClick = onClick, label = { Text(coupon.status.name) })
        }
    }
}

@Composable
private fun CampaignRow(
    campaign: Campaign,
    onClick: () -> Unit,
) {
    OfferCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(campaign.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(campaign.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (campaign.badge.isNotBlank()) AssistChip(onClick = onClick, label = { Text(campaign.badge) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfferCard(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = DesignTokens.Shape.roundedMd,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l)) { content() }
    }
}

/** Screen-internal labels via the dynamic resolver with an English fallback (no generated symbols). */
@Composable
private fun ofr(
    key: String,
    fallback: String,
): String = Res.allStringResources[key]?.let { stringResource(it) } ?: fallback
