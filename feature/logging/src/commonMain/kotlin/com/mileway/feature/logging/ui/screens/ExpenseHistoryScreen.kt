package com.mileway.feature.logging.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.sheet.FilterBottomSheet
import com.mileway.core.ui.components.sheet.FilterOption
import com.mileway.core.ui.components.sheet.FilterSection
import com.mileway.core.ui.components.sheet.FilterSelectionMode
import com.mileway.core.ui.components.sheet.SortBottomSheet
import com.mileway.core.ui.components.sheet.SortOption
import com.mileway.core.ui.mvi.DefaultEmptyState
import com.mileway.core.ui.mvi.ScreenStateContent
import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.logging_back_cd
import com.mileway.core.ui.resources.logging_category
import com.mileway.core.ui.resources.logging_expense_history
import com.mileway.core.ui.resources.logging_filter_cd
import com.mileway.core.ui.resources.logging_no_expenses_subtitle
import com.mileway.core.ui.resources.logging_no_expenses_title
import com.mileway.core.ui.resources.logging_sort_cd
import com.mileway.core.ui.resources.logging_sort_expenses
import com.mileway.core.ui.resources.logging_sort_highest_amount
import com.mileway.core.ui.resources.logging_sort_merchant
import com.mileway.core.ui.resources.logging_sort_most_recent
import com.mileway.core.ui.resources.logging_status_approved
import com.mileway.core.ui.resources.logging_status_draft
import com.mileway.core.ui.resources.logging_status_pending
import com.mileway.core.ui.resources.logging_status_rejected
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.StatusColors
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.model.ExpenseRecord
import com.mileway.feature.logging.model.ExpenseStatus
import com.mileway.feature.logging.viewmodel.ExpenseAction
import com.mileway.feature.logging.viewmodel.ExpenseFilter
import com.mileway.feature.logging.viewmodel.ExpenseListData
import com.mileway.feature.logging.viewmodel.ExpenseSort
import com.mileway.feature.logging.viewmodel.ExpenseViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExpenseHistoryScreen(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()
    val state = ui.listState.dataOrNull ?: ExpenseListData()
    var showSortSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    if (showFilterSheet) {
        val categorySection =
            FilterSection(
                key = "category",
                title = stringResource(Res.string.logging_category),
                icon = Icons.Filled.FilterList,
                mode = FilterSelectionMode.MULTI,
                options = ExpenseCategory.entries.map { FilterOption(it.name, it.label) },
            )
        FilterBottomSheet(
            sections = listOf(categorySection),
            initialSelected = mapOf("category" to state.selectedCategories.map { it.name }.toSet()),
            onApply = { selected ->
                val categories = selected["category"].orEmpty().map { ExpenseCategory.valueOf(it) }.toSet()
                viewModel.onAction(ExpenseAction.SetCategories(categories))
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false },
        )
    }

    if (showSortSheet) {
        SortBottomSheet(
            title = stringResource(Res.string.logging_sort_expenses),
            options =
                listOf(
                    SortOption(ExpenseSort.DATE, stringResource(Res.string.logging_sort_most_recent), Icons.Filled.CalendarMonth),
                    SortOption(ExpenseSort.AMOUNT, stringResource(Res.string.logging_sort_highest_amount), Icons.Filled.CurrencyRupee),
                    SortOption(ExpenseSort.MERCHANT, stringResource(Res.string.logging_sort_merchant), Icons.Filled.SortByAlpha),
                ),
            selected = state.activeSort,
            onSelect = {
                viewModel.onAction(ExpenseAction.SetSort(it))
                showSortSheet = false
            },
            onDismiss = { showSortSheet = false },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            Box(
                modifier =
                    Modifier
                        .background(Brush.horizontalGradient(listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC))))
                        .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.logging_back_cd), tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.logging_expense_history),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            text = "${state.records.size} expense${if (state.records.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Filled.FilterList, contentDescription = stringResource(Res.string.logging_filter_cd), tint = Color.White)
                    }
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(Res.string.logging_sort_cd), tint = Color.White)
                    }
                }
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.s),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            ) {
                ExpenseFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.activeFilter == filter,
                        onClick = { viewModel.onAction(ExpenseAction.SetFilter(filter)) },
                        label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                    )
                }
            }

            ScreenStateContent(
                state = ui.listState,
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                onRetry = { viewModel.onAction(ExpenseAction.Refresh) },
            ) { data ->
                if (data.records.isEmpty()) {
                    DefaultEmptyState(
                        title = stringResource(Res.string.logging_no_expenses_title),
                        subtitle = stringResource(Res.string.logging_no_expenses_subtitle),
                    )
                } else {
                    // Records arrive already sorted from the VM. Keep date-bucket headers only when sorting
                    // by date; amount / merchant sorts read better as a flat ordered list.
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                            androidx.compose.foundation.layout.PaddingValues(
                                horizontal = DesignTokens.Spacing.l,
                                vertical = DesignTokens.Spacing.m,
                            ),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                    ) {
                        if (data.activeSort == ExpenseSort.DATE) {
                            data.records.groupBy { dateBucket(it.dateMs) }.forEach { (bucket, records) ->
                                item(key = bucket) {
                                    Text(
                                        text = bucket,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(vertical = DesignTokens.Spacing.s),
                                    )
                                }
                                items(records, key = { it.id }) { expense ->
                                    ExpenseCard(expense = expense, onClick = { onOpenDetail(expense.id) })
                                }
                            }
                        } else {
                            items(data.records, key = { it.id }) { expense ->
                                ExpenseCard(expense = expense, onClick = { onOpenDetail(expense.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseCard(
    expense: ExpenseRecord,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(DesignTokens.Shape.roundedMd)
                .clickable(onClick = onClick),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = expense.category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.merchantName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Text(
                    text = "${expense.category.label} · ${formatDate(expense.dateMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${expense.amountRupees.toLong()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                ExpenseStatusChip(status = expense.status)
            }
        }
    }
}

@Composable
private fun ExpenseStatusChip(status: ExpenseStatus) {
    val (label, color) =
        when (status) {
            ExpenseStatus.DRAFT -> stringResource(Res.string.logging_status_draft) to StatusColors.neutral
            ExpenseStatus.PENDING -> stringResource(Res.string.logging_status_pending) to StatusColors.warning
            ExpenseStatus.APPROVED -> stringResource(Res.string.logging_status_approved) to StatusColors.success
            ExpenseStatus.REJECTED -> stringResource(Res.string.logging_status_rejected) to StatusColors.error
        }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

private fun dateBucket(ms: Long): String {
    val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val ldt = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${ldt.dayOfMonth} ${months[ldt.monthNumber - 1]} ${ldt.year}"
}

private fun formatDate(ms: Long): String {
    val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val ldt = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${ldt.dayOfMonth} ${months[ldt.monthNumber - 1]}"
}
