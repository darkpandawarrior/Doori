package com.mileway.ui.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.search.AppAction
import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchResult
import com.mileway.core.data.search.displayLabel
import com.mileway.core.ui.components.StatusChip
import com.mileway.core.ui.components.StatusTone
import com.mileway.core.ui.mvi.DefaultEmptyState
import com.mileway.core.ui.mvi.ScreenStateContent
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_cd_back
import com.mileway.core.ui.resources.core_cd_clear
import com.mileway.core.ui.resources.shared_search_empty_subtitle
import com.mileway.core.ui.resources.shared_search_empty_title
import com.mileway.core.ui.resources.shared_search_no_matches_subtitle
import com.mileway.core.ui.resources.shared_search_no_matches_title
import com.mileway.core.ui.resources.shared_search_placeholder
import com.mileway.core.ui.resources.shared_search_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * F0.5: master-search Root. Owns the [MasterSearchViewModel], collects state + one-shot effects, and hands
 * the stateless [MasterSearchContent] everything it needs. A tapped result emits [MasterSearchEffect.OpenResult],
 * which the host maps to a section route via [onOpenResult].
 */
@Composable
fun MasterSearchRoute(
    onBack: () -> Unit,
    onOpenResult: (SearchResult) -> Unit,
    onOpenAction: (AppAction) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MasterSearchViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MasterSearchEffect.OpenResult -> onOpenResult(effect.result)
                is MasterSearchEffect.OpenAction -> onOpenAction(effect.action)
            }
        }
    }

    MasterSearchContent(
        state = ui,
        availableTypes = viewModel.availableTypes,
        onBack = onBack,
        onQueryChange = { viewModel.onAction(MasterSearchAction.SetQuery(it)) },
        onSelectScope = { viewModel.onAction(MasterSearchAction.SelectScope(it)) },
        onToggleType = { viewModel.onAction(MasterSearchAction.ToggleType(it)) },
        onToggleStatus = { viewModel.onAction(MasterSearchAction.ToggleStatus(it)) },
        onSelectDateRange = { viewModel.onAction(MasterSearchAction.SelectDateRange(it)) },
        onDismissDetectionHint = { viewModel.onAction(MasterSearchAction.DismissDetectionHint) },
        onClearFilters = { viewModel.onAction(MasterSearchAction.ClearFilters) },
        onRetryFailed = { viewModel.onAction(MasterSearchAction.Refresh) },
        onResultClick = viewModel::openResult,
        onQuickActionClick = viewModel::openAction,
        modifier = modifier,
    )
}

/**
 * Stateless master-search UI: a persistent search field, scope tabs, type filter chips, and a grouped result
 * list. Pure inputs/outputs so previews and screenshot tests can drive it with fixed data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterSearchContent(
    state: MasterSearchUiState,
    availableTypes: List<SearchEntityType>,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSelectScope: (Int) -> Unit,
    onToggleType: (SearchEntityType) -> Unit,
    onClearFilters: () -> Unit,
    onResultClick: (SearchResult) -> Unit,
    onToggleStatus: (String) -> Unit = {},
    onSelectDateRange: (DateRangePreset) -> Unit = {},
    onDismissDetectionHint: () -> Unit = {},
    onRetryFailed: () -> Unit = {},
    onQuickActionClick: (AppAction) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.shared_search_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.core_cd_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(Res.string.shared_search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.core_cd_clear))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            ScrollableTabRow(selectedTabIndex = state.scopeIndex, edgePadding = 12.dp) {
                SEARCH_SCOPE_TABS.forEachIndexed { index, scope ->
                    Tab(
                        selected = state.scopeIndex == index,
                        onClick = { onSelectScope(index) },
                        text = { Text(scope.displayLabel) },
                    )
                }
            }

            if (availableTypes.isNotEmpty()) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    availableTypes.forEach { type ->
                        FilterChip(
                            selected = type in state.activeTypes,
                            onClick = { onToggleType(type) },
                            label = { Text(type.displayLabel) },
                        )
                    }
                }
            }

            // PLAN_V29 P29.S.3: date-range preset chips — always visible, unlike status chips which
            // only appear once a search has actually surfaced statuses to filter by.
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DateRangePreset.entries.forEach { preset ->
                    FilterChip(
                        selected = state.dateRangePreset == preset,
                        onClick = { onSelectDateRange(preset) },
                        label = { Text(preset.labelKey) },
                    )
                }
            }

            if (state.availableStatuses.isNotEmpty()) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.availableStatuses.forEach { status ->
                        FilterChip(
                            selected = status in state.activeStatuses,
                            onClick = { onToggleStatus(status) },
                            label = { Text(status) },
                        )
                    }
                }
            }

            // PLAN_V29 P29.S.2: dismissible id-pattern detection hint.
            if (state.showDetectionHint) {
                Surface(
                    onClick = onDismissDetectionHint,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Searching as " + state.detectedTypes.joinToString(", ") { it.displayLabel },
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.core_cd_clear), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // PLAN_V29 P29.S.5: command-palette quick actions, distinct from data-record results.
            if (state.quickActions.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.quickActions, key = { it.id }) { action ->
                        OutlinedButton(onClick = { onQuickActionClick(action) }) {
                            Text(action.label)
                        }
                    }
                }
            }

            ScreenStateContent(
                state = state.results,
                modifier = Modifier.fillMaxSize(),
            ) { groups ->
                when {
                    state.query.trim().length < 2 ->
                        DefaultEmptyState(
                            title = stringResource(Res.string.shared_search_empty_title),
                            subtitle = stringResource(Res.string.shared_search_empty_subtitle),
                        )
                    groups.isEmpty() && state.failedTypes.isEmpty() ->
                        DefaultEmptyState(
                            title = stringResource(Res.string.shared_search_no_matches_title),
                            subtitle = stringResource(Res.string.shared_search_no_matches_subtitle, state.query.trim()),
                        )
                    else ->
                        ResultList(
                            groups = groups,
                            failedTypes = state.failedTypes,
                            onResultClick = onResultClick,
                            onRetryFailed = onRetryFailed,
                        )
                }
            }
        }
    }
}

@Composable
private fun ResultList(
    groups: List<SearchResultGroup>,
    onResultClick: (SearchResult) -> Unit,
    failedTypes: Set<SearchEntityType> = emptySet(),
    onRetryFailed: () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        groups.forEach { group ->
            item(key = "header_${group.type}") {
                SectionHeader(label = group.type.displayLabel, count = group.results.size)
            }
            items(group.results, key = { "${it.type}_${it.id}" }) { result ->
                ResultRow(result = result, onClick = { onResultClick(result) })
            }
        }
        // PLAN_V29 P29.S.4: a group whose provider threw gets its own retry row instead of being
        // indistinguishable from "genuinely no results".
        items(failedTypes.toList(), key = { "failed_$it" }) { type ->
            FailedGroupRow(type = type, onRetry = onRetryFailed)
        }
    }
}

@Composable
private fun FailedGroupRow(
    type: SearchEntityType,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Couldn't load ${type.displayLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun SectionHeader(
    label: String,
    count: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            " · $count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ResultRow(
    result: SearchResult,
    onClick: () -> Unit,
) {
    val status = result.status
    val amount = result.amount
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    result.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (status != null) {
                    StatusChip(label = status, tone = StatusTone.Neutral)
                }
            }
            Text(
                result.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (amount != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "₹${amount.toLong()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
