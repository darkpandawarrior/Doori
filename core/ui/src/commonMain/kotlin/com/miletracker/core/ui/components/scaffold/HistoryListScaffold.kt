package com.miletracker.core.ui.components.scaffold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.mvi.DefaultEmptyState
import com.miletracker.core.ui.mvi.ScreenState
import com.miletracker.core.ui.mvi.ScreenStateContent

/**
 * Shared chrome for every V17 history / list surface (F0.4): a top bar with a collapsible search field and a
 * refresh action, an optional [tabs] [ScrollableTabRow] of status segments, an optional filter-chip [Row], and
 * a [ScreenStateContent] body that renders the loaded list (with a real empty state) or routes to the shared
 * loading / error / offline visuals. Each history screen supplies only its tabs, its row [itemContent], and
 * its filter chips.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun <T> HistoryListScaffold(
    title: String,
    onBack: () -> Unit,
    state: ScreenState<List<T>>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tabs: List<String> = emptyList(),
    selectedTab: Int = 0,
    onSelectTab: (Int) -> Unit = {},
    query: String = "",
    onQueryChange: ((String) -> Unit)? = null,
    searchPlaceholder: String = "Search…",
    emptyTitle: String = "Nothing here yet",
    emptySubtitle: String? = null,
    filterChips: (@Composable FlowRowScope.() -> Unit)? = null,
    itemKey: ((T) -> Any)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    var searchOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, fontWeight = FontWeight.SemiBold)
                        if (subtitle != null) {
                            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (onQueryChange != null) {
                        IconButton(onClick = { searchOpen = !searchOpen }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    }
                    IconButton(onClick = onRetry) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (searchOpen && onQueryChange != null) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text(searchPlaceholder) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (tabs.isNotEmpty()) {
                ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
                    tabs.forEachIndexed { index, label ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { onSelectTab(index) },
                            text = { Text(label) },
                        )
                    }
                }
            }

            if (filterChips != null) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    content = filterChips,
                )
            }

            ScreenStateContent(
                state = state,
                modifier = Modifier.fillMaxSize(),
                onRetry = onRetry,
            ) { list ->
                if (list.isEmpty()) {
                    DefaultEmptyState(title = emptyTitle, subtitle = emptySubtitle)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(list, key = itemKey) { item -> itemContent(item) }
                    }
                }
            }
        }
    }
}
