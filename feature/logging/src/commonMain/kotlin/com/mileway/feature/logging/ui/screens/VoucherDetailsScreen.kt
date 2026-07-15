package com.mileway.feature.logging.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.mileway.core.data.model.db.VoucherCategory
import com.mileway.core.data.model.db.VoucherEntity
import com.mileway.core.ui.components.StatusChip
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.logging_amount
import com.mileway.core.ui.resources.logging_back_cd
import com.mileway.core.ui.resources.logging_category
import com.mileway.core.ui.resources.logging_note
import com.mileway.core.ui.resources.logging_voucher_created
import com.mileway.core.ui.resources.logging_voucher_details_title
import com.mileway.core.ui.resources.logging_voucher_linked_expenses
import com.mileway.core.ui.resources.logging_voucher_not_found
import com.mileway.core.ui.resources.tracking_voucher_category_fuel
import com.mileway.core.ui.resources.tracking_voucher_category_maintenance
import com.mileway.core.ui.resources.tracking_voucher_category_mileage
import com.mileway.core.ui.resources.tracking_voucher_category_other
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.logging.viewmodel.VoucherDetailsAction
import com.mileway.feature.logging.viewmodel.VoucherDetailsViewModel
import com.siddharth.kmp.common.formatDecimal
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * P27.E.12: voucher drill-down reachable from [VoucherHistoryScreen]'s cards — loads one row by
 * [voucherNumber] off the shared, Room-backed `VoucherDao` via [VoucherDetailsViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherDetailsScreen(
    voucherNumber: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VoucherDetailsViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()
    LaunchedEffect(voucherNumber) { viewModel.onAction(VoucherDetailsAction.Load(voucherNumber)) }
    val voucher = ui.voucher.dataOrNull

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.logging_voucher_details_title),
                subtitle = voucherNumber,
                depth = NavigationDepth.LEVEL_2,
                titleIcon = Icons.Filled.ConfirmationNumber,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.logging_back_cd))
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        if (voucher == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(Res.string.logging_voucher_not_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            VoucherDetailsContent(voucher, Modifier.padding(innerPadding).then(modifier))
        }
    }
}

@Composable
private fun VoucherDetailsContent(
    voucher: VoucherEntity,
    modifier: Modifier = Modifier,
) {
    val expenseIds = VoucherEntity.decodeExpenseRouteIds(voucher.expenseRouteIdsJson)

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DesignTokens.Spacing.l)
                .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.Spacing.s),
            shape = DesignTokens.Shape.roundedMd,
            elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(voucher.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    StatusChip(label = voucherStatusLabel(voucher.status), tone = voucherStatusTone(voucher.status))
                }
                DetailRow(stringResource(Res.string.logging_amount), "₹${voucher.totalAmount.formatDecimal(2)}")
                DetailRow(stringResource(Res.string.logging_category), voucher.category.localizedLabel())
                DetailRow(stringResource(Res.string.logging_voucher_created), formatFullDate(voucher.createdAtMs))
                if (voucher.notes.isNotBlank()) {
                    DetailRow(stringResource(Res.string.logging_note), voucher.notes)
                }
            }
        }

        if (expenseIds.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
                elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                ) {
                    Text(
                        stringResource(Res.string.logging_voucher_linked_expenses),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    expenseIds.forEach { id -> Text(id, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun VoucherCategory.localizedLabel(): String =
    when (this) {
        VoucherCategory.MILEAGE -> stringResource(Res.string.tracking_voucher_category_mileage)
        VoucherCategory.FUEL -> stringResource(Res.string.tracking_voucher_category_fuel)
        VoucherCategory.MAINTENANCE -> stringResource(Res.string.tracking_voucher_category_maintenance)
        VoucherCategory.OTHER -> stringResource(Res.string.tracking_voucher_category_other)
    }

private val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun formatFullDate(ms: Long): String {
    val ldt = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${ldt.dayOfMonth} ${MONTHS[ldt.monthNumber - 1]} ${ldt.year}"
}
