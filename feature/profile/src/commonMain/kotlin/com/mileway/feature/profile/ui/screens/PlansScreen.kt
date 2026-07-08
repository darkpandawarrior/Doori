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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.subscription.SubscriptionPeriod
import com.mileway.core.data.subscription.SubscriptionPlan
import com.mileway.core.data.util.CommonUtils
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.viewmodel.SubscriptionViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V24 P6.2: subscription plan picker (source: the reference app `subscriptions/` plan list). Cards show
 * price / period / savings copy / features. Tapping a non-active plan opens a mock purchase confirm
 * sheet (NO payment). An existing subscription surfaces a "manage" entry to [MySubscriptionScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    onBack: () -> Unit,
    onOpenManage: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubscriptionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hasActive = state.active != null

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), modifier = modifier) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Box(
                modifier =
                    Modifier
                        .background(Brush.horizontalGradient(listOf(Color(0xFF1D4ED8), Color(0xFF0B2A6B))))
                        .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = sb("plans_back", "Back"), tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(sb("plans_title", "Plans"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            sb("plans_subtitle", "Pick the plan that fits you"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }

            Column(
                modifier =
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                if (hasActive) {
                    Surface(
                        color = Color(0xFF1D4ED8).copy(alpha = 0.10f),
                        shape = DesignTokens.Shape.roundedMd,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    sb("plans_active_banner", "You have an active subscription"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                state.activePlan?.let {
                                    Text(
                                        it.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            TextButton(onClick = onOpenManage) { Text(sb("plans_manage", "Manage")) }
                        }
                    }
                }

                state.plans.forEach { plan ->
                    PlanCard(
                        plan = plan,
                        isCurrent = state.active?.planId == plan.id,
                        hasActive = hasActive,
                        onSelect = { confirmPlan = plan },
                    )
                }
            }
        }
    }

    confirmPlan?.let { plan ->
        ModalBottomSheet(onDismissRequest = { confirmPlan = null }, sheetState = sheetState) {
            PurchaseConfirm(
                plan = plan,
                isUpgrade = hasActive,
                onConfirm = {
                    if (hasActive) viewModel.upgrade(plan.id) else viewModel.purchase(plan.id)
                    confirmPlan = null
                },
                onCancel = { confirmPlan = null },
            )
        }
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    isCurrent: Boolean,
    hasActive: Boolean,
    onSelect: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = DesignTokens.Shape.roundedMd, elevation = CardDefaults.cardElevation(DesignTokens.Elevation.card)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(plan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(priceLabel(plan), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(plan.savingsCopy, style = MaterialTheme.typography.bodySmall, color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            plan.features.forEach { f ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.height(18.dp))
                    Spacer(Modifier.width(DesignTokens.Spacing.s))
                    Text(f, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            if (isCurrent) {
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text(sb("plans_current", "Current plan")) }
            } else {
                Button(onClick = onSelect, modifier = Modifier.fillMaxWidth()) {
                    Text(if (hasActive) sb("plans_switch", "Switch to this plan") else sb("plans_choose", "Choose plan"))
                }
            }
        }
    }
}

@Composable
private fun PurchaseConfirm(
    plan: SubscriptionPlan,
    isUpgrade: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(DesignTokens.Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        Text(
            if (isUpgrade) sb("plans_confirm_switch_title", "Switch plan") else sb("plans_confirm_title", "Confirm subscription"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text("${plan.name} · ${priceLabel(plan)}", style = MaterialTheme.typography.titleMedium)
        Text(
            sb("plans_confirm_mock", "This is a demo — no real payment is taken."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
            Text(if (isUpgrade) sb("plans_confirm_switch_cta", "Switch now") else sb("plans_confirm_cta", "Confirm"))
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text(sb("plans_confirm_cancel", "Cancel")) }
        Spacer(Modifier.height(DesignTokens.Spacing.s))
    }
}

private fun priceLabel(plan: SubscriptionPlan): String {
    val amount = CommonUtils.formatCurrencyAmount(plan.priceAmount)
    val per = if (plan.period == SubscriptionPeriod.YEARLY) "/yr" else "/mo"
    return "$amount$per"
}

@Composable
private fun sb(
    key: String,
    fallback: String,
): String = Res.allStringResources[key]?.let { stringResource(it) } ?: fallback
