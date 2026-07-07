package com.mileway.feature.tracking.debug

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.mileway.core.network.netlog.NetworkLogEntry
import com.mileway.core.network.netlog.toCurl
import com.mileway.core.ui.components.EmptyState
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import org.koin.compose.viewmodel.koinViewModel

/**
 * V21 §3 Wave 4: debug-only network log — lists recorded [NetworkLogEntry]s from the shared
 * NetworkLogStore, a tap-through detail with a "Copy as curl" replay action, and a minimal API
 * tester. Slot into the host app's debug menu (see DebugMenuScreen's `onOpenNetworkLog`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkLogScreen(
    onBack: () -> Unit,
    viewModel: NetworkLogViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = "Network Log",
                subtitle = "${ui.entries.size} recorded",
                depth = NavigationDepth.LEVEL_2,
                navigationIcon = {
                    val onNavigationClick =
                        if (ui.selected != null) {
                            { viewModel.onAction(NetworkLogAction.SelectEntry(null)) }
                        } else {
                            onBack
                        }
                    IconButton(onClick = onNavigationClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (ui.selected == null) {
                        IconButton(onClick = { viewModel.onAction(NetworkLogAction.ClearLog) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Clear log")
                        }
                    }
                },
            )
        },
    ) { padding ->
        val selected = ui.selected
        if (selected != null) {
            NetworkLogDetail(entry = selected, modifier = Modifier.padding(padding))
        } else {
            NetworkLogList(
                uiState = ui,
                onEntryClick = { viewModel.onAction(NetworkLogAction.SelectEntry(it)) },
                onAction = viewModel::onAction,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun NetworkLogList(
    uiState: NetworkLogUiState,
    onEntryClick: (NetworkLogEntry) -> Unit,
    onAction: (NetworkLogAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(DesignTokens.Spacing.l),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        item { ApiTesterCard(uiState = uiState, onAction = onAction) }

        if (uiState.entries.isEmpty()) {
            item {
                EmptyState(
                    title = "No requests logged yet",
                    subtitle = "Traffic through an HttpClient with NetworkLogPlugin installed shows up here.",
                )
            }
        } else {
            items(uiState.entries, key = { it.timestamp.toString() + it.url }) { entry ->
                NetworkLogRow(entry = entry, onClick = { onEntryClick(entry) })
            }
        }
    }
}

@Composable
private fun NetworkLogRow(
    entry: NetworkLogEntry,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.m)) {
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                Text(entry.method, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = entry.status?.toString() ?: "—",
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        when (entry.status) {
                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                            in 200..299 -> DesignTokens.StatusColors.success
                            else -> DesignTokens.StatusColors.error
                        },
                )
                Text(
                    text = entry.durationMs?.let { "${it}ms" } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = entry.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun NetworkLogDetail(
    entry: NetworkLogEntry,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(DesignTokens.Spacing.l),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        item {
            Text(
                text = "${entry.method} ${entry.url}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Button(
                shape = DesignTokens.Shape.button,
                onClick = { clipboardManager.setText(AnnotatedString(entry.toCurl())) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(DesignTokens.IconSize.inline),
                )
                Spacer(Modifier.width(DesignTokens.Spacing.s))
                Text("Copy as curl")
            }
        }
        item {
            val headerText = entry.requestHeaders.entries.joinToString("\n") { "${it.key}: ${it.value}" }
            DetailSection(title = "Request headers", body = headerText)
        }
        item { DetailSection(title = "Request body", body = entry.requestBody ?: "—") }
        item { DetailSection(title = "Response (${entry.status ?: "—"})", body = entry.responseBody ?: "—") }
    }
}

@Composable
private fun DetailSection(
    title: String,
    body: String,
) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(DesignTokens.Spacing.xs))
        Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ApiTesterCard(
    uiState: NetworkLogUiState,
    onAction: (NetworkLogAction) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = DesignTokens.Shape.roundedMd) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Text("API Tester", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = uiState.testerMethod,
                onValueChange = { onAction(NetworkLogAction.TesterMethodChanged(it)) },
                label = { Text("Method") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.testerUrl,
                onValueChange = { onAction(NetworkLogAction.TesterUrlChanged(it)) },
                label = { Text("URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.testerBody,
                onValueChange = { onAction(NetworkLogAction.TesterBodyChanged(it)) },
                label = { Text("Body (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                shape = DesignTokens.Shape.button,
                onClick = { onAction(NetworkLogAction.TesterSend) },
                enabled = !uiState.testerRunning && uiState.testerUrl.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.testerRunning) "Sending…" else "Send")
            }
            uiState.testerResult?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
