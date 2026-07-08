package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.coupon.Coupon
import com.mileway.core.data.coupon.CouponApplyResult
import com.mileway.core.data.coupon.CouponStatus
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.viewmodel.CouponsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V24 P5.2: coupons/promotions — active vs expired/used sections and a "Have a code?" entry
 * validating a typed code against the seeded set (invalid/expired/already-used/success). A
 * successful redeem logs a Notification Centre entry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CouponsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val notifTitle = cp("coupons_notif_title", "Coupon redeemed")
    val notifTime = cp("coupons_notif_time", "Just now")
    val successMsg = cp("coupons_result_success", "Coupon applied successfully!")
    // Log a Notification Centre entry once, when an apply succeeds.
    LaunchedEffect(state.applyResult) {
        if (state.applyResult == CouponApplyResult.SUCCESS) {
            viewModel.logRedeem(notifTitle, successMsg, notifTime)
        }
    }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), modifier = modifier) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Box(
                modifier =
                    Modifier
                        .background(Brush.horizontalGradient(listOf(Color(0xFF00695C), Color(0xFF004D40))))
                        .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = cp("coupons_back", "Back"), tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            cp("coupons_title", "Coupons & Offers"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            cp("coupons_subtitle", "Promo codes and rewards"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                item { ApplyCodeCard(state.code, state.canApply, state.applyResult, viewModel::onCodeChange, viewModel::applyCode) }

                if (state.active.isNotEmpty()) {
                    item { SectionLabel(cp("coupons_active", "Active offers")) }
                    items(state.active) { coupon -> CouponRow(coupon, dimmed = false) }
                }
                if (state.inactive.isNotEmpty()) {
                    item { SectionLabel(cp("coupons_inactive", "Expired & used")) }
                    items(state.inactive) { coupon -> CouponRow(coupon, dimmed = true) }
                }
                if (state.active.isEmpty() && state.inactive.isEmpty()) {
                    item {
                        Text(
                            cp("coupons_empty", "No coupons available."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplyCodeCard(
    code: String,
    canApply: Boolean,
    result: CouponApplyResult?,
    onCodeChange: (String) -> Unit,
    onApply: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = DesignTokens.Shape.roundedMd, elevation = CardDefaults.cardElevation(DesignTokens.Elevation.card)) {
        Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
            Text(cp("coupons_have_code", "Have a code?"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    label = { Text(cp("coupons_code_hint", "Enter promo code")) },
                    singleLine = true,
                    isError = result != null && result != CouponApplyResult.SUCCESS,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onApply, enabled = canApply) { Text(cp("coupons_apply", "Apply")) }
            }
            result?.let { r ->
                val (msg, color) = applyResultMessage(r)
                Text(msg, style = MaterialTheme.typography.bodySmall, color = color)
            }
        }
    }
}

@Composable
private fun applyResultMessage(result: CouponApplyResult): Pair<String, Color> =
    when (result) {
        CouponApplyResult.SUCCESS -> cp("coupons_result_success", "Coupon applied!") to Color(0xFF16A34A)
        CouponApplyResult.INVALID -> cp("coupons_result_invalid", "Invalid code.") to MaterialTheme.colorScheme.error
        CouponApplyResult.EXPIRED -> cp("coupons_result_expired", "This code has expired.") to MaterialTheme.colorScheme.error
        CouponApplyResult.ALREADY_USED -> cp("coupons_result_used", "Code already used.") to MaterialTheme.colorScheme.error
    }

@Composable
private fun CouponRow(
    coupon: Coupon,
    dimmed: Boolean,
) {
    val alpha = if (dimmed) 0.6f else 1f
    Card(modifier = Modifier.fillMaxWidth(), shape = DesignTokens.Shape.roundedMd, elevation = CardDefaults.cardElevation(DesignTokens.Elevation.card)) {
        Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    coupon.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                    modifier = Modifier.weight(1f),
                )
                StatusChip(coupon.status)
            }
            Text(coupon.code, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF00695C).copy(alpha = alpha))
            Text(coupon.terms, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
            Text(coupon.expiryLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
        }
    }
}

@Composable
private fun StatusChip(status: CouponStatus) {
    val (label, color) =
        when (status) {
            CouponStatus.ACTIVE -> cp("coupons_status_active", "Active") to Color(0xFF16A34A)
            CouponStatus.EXPIRED -> cp("coupons_status_expired", "Expired") to Color(0xFF6B7280)
            CouponStatus.REDEEMED -> cp("coupons_status_redeemed", "Used") to Color(0xFF2563EB)
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
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = DesignTokens.Spacing.s))
}

@Composable
private fun cp(
    key: String,
    fallback: String,
): String = Res.allStringResources[key]?.let { stringResource(it) } ?: fallback
