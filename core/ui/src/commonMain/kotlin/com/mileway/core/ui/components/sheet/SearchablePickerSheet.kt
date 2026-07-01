package com.mileway.core.ui.components.sheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Reusable searchable-picker scaffold: a modal sheet with a title
 * and a search field, that hands the caller the live [query] and the synchronously-[filter]ed items to lay
 * out however it likes (a list, a grid, sections, …) via [results]. Replaces the bespoke modal+search+filter
 * boilerplate every picker re-implemented (e.g. VehiclePickerSheet). Generic over the item type [T].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SearchablePickerSheet(
    title: String,
    items: List<T>,
    filter: (item: T, query: String) -> Boolean,
    onDismiss: () -> Unit,
    searchPlaceholder: String = "Search…",
    results: @Composable ColumnScope.(filtered: List<T>, query: String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, items) { if (query.isBlank()) items else items.filter { filter(it, query) } }

    PickerScaffold(title = title, query = query, onQueryChange = { query = it }, searchPlaceholder = searchPlaceholder, onDismiss = onDismiss) {
        results(filtered, query)
    }
}

/** Async-search state surfaced to the [AsyncSearchablePickerSheet] result slot. */
data class PickerSearchState<T>(
    val query: String,
    val results: List<T>,
    val isSearching: Boolean,
)

/**
 * Async variant of [SearchablePickerSheet]: the caller supplies a suspending [onSearch] (a network or DB
 * lookup); keystrokes are debounced ([debounceMs]) and the [results] slot receives a [PickerSearchState]
 * with the loading flag so it can show a spinner. The blank query short-circuits to an empty result set.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AsyncSearchablePickerSheet(
    title: String,
    onSearch: suspend (String) -> List<T>,
    onDismiss: () -> Unit,
    searchPlaceholder: String = "Search…",
    debounceMs: Long = 300L,
    results: @Composable ColumnScope.(state: PickerSearchState<T>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<T>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(debounceMs)
        searchResults = onSearch(query)
        isSearching = false
    }

    PickerScaffold(title = title, query = query, onQueryChange = { query = it }, searchPlaceholder = searchPlaceholder, onDismiss = onDismiss) {
        if (isSearching) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        results(PickerSearchState(query, searchResults, isSearching))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerScaffold(
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    searchPlaceholder: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(searchPlaceholder) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}
