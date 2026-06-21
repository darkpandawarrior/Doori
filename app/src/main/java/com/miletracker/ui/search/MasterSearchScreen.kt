package com.miletracker.ui.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miletracker.core.data.search.SearchEntityType
import com.miletracker.core.data.search.SearchResult
import com.miletracker.core.data.search.displayLabel
import com.miletracker.core.ui.components.StatusChip
import com.miletracker.core.ui.components.StatusTone
import com.miletracker.core.ui.mvi.DefaultEmptyState
import com.miletracker.core.ui.mvi.ScreenState
import com.miletracker.core.ui.mvi.ScreenStateContent
import org.koin.compose.viewmodel.koinViewModel

/**
 * F0.5 — master-search Root. Owns the [MasterSearchViewModel], collects state + one-shot effects, and hands
 * the stateless [MasterSearchContent] everything it needs. A tapped result emits [MasterSearchEffect.OpenResult],
 * which the host maps to a section route via [onOpenResult].
 */
@Composable
fun MasterSearchRoute(
    onBack: () -> Unit,
    onOpenResult: (SearchResult) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MasterSearchViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MasterSearchEffect.OpenResult -> onOpenResult(effect.result)
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
        onClearFilters = { viewModel.onAction(MasterSearchAction.ClearFilters) },
        onResultClick = viewModel::openResult,
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
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Search", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search trips, payments, events…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
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

            ScreenStateContent(
                state = state.results,
                modifier = Modifier.fillMaxSize(),
            ) { groups ->
                when {
                    state.query.trim().length < 2 ->
                        DefaultEmptyState(
                            title = "Search everything",
                            subtitle = "Find trips, expenses, payments, bookings and more across the app.",
                        )
                    groups.isEmpty() ->
                        DefaultEmptyState(
                            title = "No matches",
                            subtitle = "Nothing matched \"${state.query.trim()}\". Try a different term or scope.",
                        )
                    else -> ResultList(groups = groups, onResultClick = onResultClick)
                }
            }
        }
    }
}

@Composable
private fun ResultList(
    groups: List<SearchResultGroup>,
    onResultClick: (SearchResult) -> Unit,
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
    }
}

@Composable
private fun SectionHeader(label: String, count: Int) {
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
private fun ResultRow(result: SearchResult, onClick: () -> Unit) {
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

