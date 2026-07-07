package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_details_back
import com.mileway.core.ui.resources.profile_org_name_you
import com.mileway.core.ui.resources.profile_org_subtitle
import com.mileway.core.ui.resources.profile_org_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.profile.model.OrgChartBuilder
import com.mileway.feature.profile.model.OrgChartNode
import com.mileway.feature.profile.viewmodel.ProfileViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V22 P6.2: a small (root → manager → reports), fully local org chart pushed from the
 * "Reporting Manager" tile on [ProfileDetailsScreen] — replaces the prior no-op tap. Built from
 * the already-seeded personas ([OrgChartBuilder]), not a real org-chart backend. Own Matrix/
 * terminal design language (indented rows + a highlighted "you" row), not a port of any reference
 * app org-chart UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgChartScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val root = OrgChartBuilder.build(state.profile, state.accounts, state.selectedAccountId)
    val flatRows = remember(root) { flattenOrgChart(root) }

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.profile_org_title),
                subtitle = stringResource(Res.string.profile_org_subtitle),
                depth = NavigationDepth.LEVEL_3_PLUS,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.profile_details_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(innerPadding),
            contentPadding = PaddingValues(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            items(flatRows, key = { it.node.id }) { row ->
                OrgChartRowContent(depth = row.depth, node = row.node)
            }
        }
    }
}

private data class OrgChartRow(val depth: Int, val node: OrgChartNode)

private fun flattenOrgChart(
    node: OrgChartNode,
    depth: Int = 0,
): List<OrgChartRow> = listOf(OrgChartRow(depth, node)) + node.reports.flatMap { flattenOrgChart(it, depth + 1) }

@Composable
private fun OrgChartRowContent(
    depth: Int,
    node: OrgChartNode,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width((depth * 24).dp))
        Surface(
            shape = DesignTokens.Shape.roundedMd,
            color =
                if (node.isCurrentUser) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(DesignTokens.Spacing.m),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(DesignTokens.Spacing.xs),
                    )
                }
                Column {
                    Text(
                        text = if (node.isCurrentUser) stringResource(Res.string.profile_org_name_you, node.name) else node.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = node.role,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
