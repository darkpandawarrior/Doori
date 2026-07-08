package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.referral.ReferralActivity
import com.mileway.core.data.referral.ReferralLeaderboardEntry
import com.mileway.core.data.referral.ReferralStatus
import com.mileway.core.data.referral.ReferralTxn
import com.mileway.core.ui.components.buildReferralInvite
import com.mileway.core.ui.platform.LocalShareSheet
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.viewmodel.ReferralHubUiState
import com.mileway.feature.profile.viewmodel.ReferralHubViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V24 P5.1: the full referral hub — Overview (code + share + earnings), Referrals
 * (in-progress/completed buckets with per-referee target meters), Leaderboard (seeded top-10 with
 * the user's rank), and Activity (event feed). The lightweight `ReferralCard` stays as the entry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralHubScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReferralHubViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shareSheet = LocalShareSheet.current
    var tab by remember { mutableIntStateOf(0) }
    val tabs =
        listOf(
            rh("referral_hub_tab_overview", "Overview"),
            rh("referral_hub_tab_referrals", "Referrals"),
            rh("referral_hub_tab_leaderboard", "Leaderboard"),
            rh("referral_hub_tab_activity", "Activity"),
        )

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), modifier = modifier) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Box(
                modifier =
                    Modifier
                        .background(Brush.horizontalGradient(listOf(Color(0xFF7B1FA2), Color(0xFF4A148C))))
                        .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = rh("referral_hub_back", "Back"), tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            rh("referral_hub_title", "Referral Program"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            rh("referral_hub_subtitle", "Invite friends, earn rewards"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            PrimaryTabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title, maxLines = 1) })
                }
            }

            when (tab) {
                0 -> OverviewTab(state, onShare = { shareSheet.share(text = buildReferralInvite(state.code)) })
                1 -> ReferralsTab(state)
                2 -> LeaderboardTab(state)
                else -> ActivityTab(state)
            }
        }
    }
}

@Composable
private fun OverviewTab(
    state: ReferralHubUiState,
    onShare: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(DesignTokens.Spacing.l),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
                elevation = CardDefaults.cardElevation(DesignTokens.Elevation.card),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                    Text(
                        rh("referral_hub_your_code", "Your referral code"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(state.code, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Button(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(DesignTokens.Spacing.s))
                        Text(rh("referral_hub_share", "Share invite"))
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
                elevation = CardDefaults.cardElevation(DesignTokens.Elevation.card),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l)) {
                    Text(
                        rh("referral_hub_earnings", "Total earned"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        rhArg("referral_hub_credits", "%1\$d credits", state.totalCredits),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7B1FA2),
                    )
                    state.userRank?.let { rank ->
                        Text(rhArg("referral_hub_your_rank", "Your rank: #%1\$d", rank), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferralsTab(state: ReferralHubUiState) {
    if (state.pending.isEmpty() && state.completed.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                rh("referral_hub_empty", "No referrals yet."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(DesignTokens.Spacing.xl),
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(DesignTokens.Spacing.l),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        if (state.pending.isNotEmpty()) {
            item { BucketHeader(rh("referral_hub_pending", "In progress")) }
            items(state.pending) { txn -> ReferralRow(txn) }
        }
        if (state.completed.isNotEmpty()) {
            item { BucketHeader(rh("referral_hub_completed", "Completed")) }
            items(state.completed) { txn -> ReferralRow(txn) }
        }
    }
}

@Composable
private fun ReferralRow(txn: ReferralTxn) {
    Card(modifier = Modifier.fillMaxWidth(), shape = DesignTokens.Shape.roundedMd, elevation = CardDefaults.cardElevation(DesignTokens.Elevation.card)) {
        Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(txn.refereeName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                StatusPill(txn.status)
            }
            Text(txn.taskMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (txn.status == ReferralStatus.PENDING && txn.nextTargetRides > 0) {
                LinearProgressIndicator(progress = { txn.targetProgress }, modifier = Modifier.fillMaxWidth())
                Text(
                    rhArg2("referral_hub_trips_meter", "%1\$d of %2\$d trips", txn.userNumRides, txn.nextTargetRides),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (txn.nextTargetCredits > 0) {
                    Text(rhArg("referral_hub_next_reward", "Next reward: %1\$d credits", txn.nextTargetCredits), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: ReferralStatus) {
    val (label, color) =
        when (status) {
            ReferralStatus.PENDING -> rh("referral_hub_status_pending", "In progress") to Color(0xFFB45309)
            ReferralStatus.SUCCESS -> rh("referral_hub_status_success", "Rewarded") to Color(0xFF16A34A)
            ReferralStatus.FAILED -> rh("referral_hub_status_failed", "Expired") to Color(0xFFDC2626)
        }
    Surface(color = color.copy(alpha = 0.12f), shape = DesignTokens.Shape.roundedMd) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.s, vertical = 4.dp),
        )
    }
}

@Composable
private fun LeaderboardTab(state: ReferralHubUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(DesignTokens.Spacing.l),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        items(state.leaderboard) { entry -> LeaderboardRow(entry) }
    }
}

@Composable
private fun LeaderboardRow(entry: ReferralLeaderboardEntry) {
    val bg = if (entry.isCurrentUser) Color(0xFF7B1FA2).copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface
    Surface(color = bg, shape = DesignTokens.Shape.roundedMd, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l), verticalAlignment = Alignment.CenterVertically) {
            Text("#${entry.rank}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
            Text(
                entry.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (entry.isCurrentUser) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            Text(
                rhArg("referral_hub_credits", "%1\$d credits", entry.credits),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActivityTab(state: ReferralHubUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(DesignTokens.Spacing.l),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        items(state.activity) { event -> ActivityRow(event) }
    }
}

@Composable
private fun ActivityRow(event: ReferralActivity) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = DesignTokens.Spacing.s)) {
        Text(event.message, style = MaterialTheme.typography.bodyMedium)
        Text(event.relativeTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BucketHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = DesignTokens.Spacing.s))
}

@Composable
private fun rh(
    key: String,
    fallback: String,
): String = Res.allStringResources[key]?.let { stringResource(it) } ?: fallback

@Composable
private fun rhArg(
    key: String,
    fallback: String,
    arg: Int,
): String = Res.allStringResources[key]?.let { stringResource(it, arg) } ?: fallback.replace("%1\$d", arg.toString())

@Composable
private fun rhArg2(
    key: String,
    fallback: String,
    a: Int,
    b: Int,
): String =
    Res.allStringResources[key]?.let { stringResource(it, a, b) }
        ?: fallback.replace("%1\$d", a.toString()).replace("%2\$d", b.toString())
