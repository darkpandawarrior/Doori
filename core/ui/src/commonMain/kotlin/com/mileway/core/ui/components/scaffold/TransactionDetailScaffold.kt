package com.mileway.core.ui.components.scaffold

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.mileway.core.common.UiText
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_cd_back
import com.mileway.core.ui.resources.detail_section_attachments
import com.mileway.core.ui.resources.detail_section_audit
import com.mileway.core.ui.resources.detail_section_clarification
import com.mileway.core.ui.resources.detail_section_comments
import com.mileway.core.ui.resources.detail_section_details
import com.mileway.core.ui.resources.detail_section_timeline
import com.mileway.core.ui.text.text
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/**
 * P25.A5.1: the shared tab set every transaction-detail-shaped screen (approvals detail, expense
 * detail, purchase-request details) picks a subset of — V28's T-SCAFFOLD rollout has each feature
 * opt its own state into the sections it actually has, not one giant cross-feature DTO.
 */
sealed interface DetailSection {
    val label: UiText

    data object Details : DetailSection {
        override val label: UiText = UiText.Res(Res.string.detail_section_details.key)
    }

    data object Timeline : DetailSection {
        override val label: UiText = UiText.Res(Res.string.detail_section_timeline.key)
    }

    data object Comments : DetailSection {
        override val label: UiText = UiText.Res(Res.string.detail_section_comments.key)
    }

    data object Clarification : DetailSection {
        override val label: UiText = UiText.Res(Res.string.detail_section_clarification.key)
    }

    data object Attachments : DetailSection {
        override val label: UiText = UiText.Res(Res.string.detail_section_attachments.key)
    }

    data object Audit : DetailSection {
        override val label: UiText = UiText.Res(Res.string.detail_section_audit.key)
    }
}

/**
 * P25.A5.1: thin Scaffold + [ScrollableTabRow] shell for any transaction-detail screen — mirrors
 * [HistoryListScaffold]'s idiom (top bar + tab row + a body slot the caller owns). Unlike
 * `HistoryListScaffold`'s string tabs, [selectedTab] is the typed [DetailSection] itself rather
 * than a parallel index, so callers don't have to keep an index and a section list in sync.
 * V28 rolls this out onto real screens; this task ships only the shell.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScaffold(
    title: String,
    tabs: List<DetailSection>,
    selectedTab: DetailSection,
    onSelectTab: (DetailSection) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleIcon: ImageVector? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable (DetailSection) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        titleIcon?.let { icon ->
                            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(DesignTokens.IconSize.actionTile))
                            Spacer(Modifier.width(DesignTokens.Spacing.s))
                        }
                        Column {
                            Text(title, fontWeight = FontWeight.SemiBold)
                            if (subtitle != null) {
                                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                navigationIcon = {
                    onBack?.let { back ->
                        IconButton(onClick = back) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.core_cd_back))
                        }
                    }
                },
                actions = actions,
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (tabs.size > 1) {
                val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
                ScrollableTabRow(selectedTabIndex = selectedIndex, edgePadding = DesignTokens.Spacing.m) {
                    tabs.forEach { section ->
                        Tab(
                            selected = section == selectedTab,
                            onClick = { onSelectTab(section) },
                            text = { Text(section.label.text()) },
                        )
                    }
                }
            }
            content(selectedTab)
        }
    }
}
