package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CardTravel
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.network.model.CompletionCategory
import com.mileway.core.network.model.EmployeeProfile
import com.mileway.core.ui.components.CategoryCompletionDisplay
import com.mileway.core.ui.components.CollapsibleSectionCard
import com.mileway.core.ui.components.GridProfileTile
import com.mileway.core.ui.components.MissingItemDisplay
import com.mileway.core.ui.components.ProfileCompletionBanner
import com.mileway.core.ui.components.ProfileGridItem
import com.mileway.core.ui.components.ProfileItemStatus
import com.mileway.core.ui.components.ProfileSectionHeader
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.profile.viewmodel.ProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Profile Details, a full-detail editor surface pushed from the Account hub.
 *
 * Layout (top to bottom):
 *  - LEVEL_2 [com.mileway.core.ui.components.topbar.DepthAwareTopBar] with back
 *  - avatar + name + role/gender chips
 *  - "Contact Info" [CollapsibleSectionCard] (email / code / phone rows)
 *  - [ProfileCompletionBanner] fed by the repository's completion checklist
 *  - per-category [GridProfileTile] sections grouped by [ProfileSectionHeader]
 *
 * Full-screen flow: there is no bubble bar, so the pinned bottom of the grid uses
 * navigation-bars padding via the content insets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailsScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.profile
    val completion = state.completion

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = "Profile Details",
                subtitle = "Manage your information",
                depth = NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        val missingItems =
            completion.categories
                .filter { it.done < it.total }
                .map {
                    MissingItemDisplay(
                        id = it.name,
                        title = it.name,
                        isRequired = it.name in REQUIRED_CATEGORIES,
                    )
                }
        val categoryDisplays = completion.categories.map { it.toDisplay() }
        val detailItems = buildDetailItems(profile)
        val grouped = detailItems.groupBy { it.category }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(DesignTokens.Spacing.l),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            item(span = { GridItemSpan(2) }) {
                ProfileIdentityBlock(profile = profile)
            }

            item(span = { GridItemSpan(2) }) {
                CollapsibleSectionCard(
                    title = "Contact Info",
                    initiallyExpanded = true,
                    leadingIcon = Icons.Default.Person,
                ) {
                    ContactRow(icon = Icons.Default.Email, value = profile.email)
                    ContactRow(icon = Icons.Default.Badge, value = profile.employeeCode)
                    ContactRow(icon = Icons.Default.Phone, value = profile.phone)
                }
            }

            item(span = { GridItemSpan(2) }) {
                ProfileCompletionBanner(
                    completionPercentage = completion.percent,
                    completedCount = completion.categories.sumOf { it.done },
                    totalCount = completion.categories.sumOf { it.total },
                    missingItems = missingItems,
                    categories = categoryDisplays,
                )
            }

            grouped.forEach { (category, entries) ->
                item(span = { GridItemSpan(2) }) {
                    ProfileSectionHeader(
                        title = category,
                        itemCount = entries.size,
                        icon = entries.first().leadingIcon,
                    )
                }
                items(entries, key = { it.item.id }) { detail ->
                    GridProfileTile(item = detail.item)
                }
            }

            // Keep the last row above the system navigation bar (no bubble bar here).
            item(span = { GridItemSpan(2) }) {
                Box(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

/** Required categories drive the "X required items remaining" copy in the completion banner. */
private val REQUIRED_CATEGORIES = setOf("Personal Info", "Organization", "Policy & Compliance")

private fun CompletionCategory.toDisplay(): CategoryCompletionDisplay =
    CategoryCompletionDisplay(
        categoryLabel = name,
        completedCount = done,
        totalCount = total,
        percentage = if (total > 0) done * 100 / total else 0,
        isRequiredCategory = name in REQUIRED_CATEGORIES,
    )

@Composable
private fun ProfileIdentityBlock(profile: EmployeeProfile) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        Box(
            modifier =
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initialsOf(profile.name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            text = profile.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
            if (profile.role.isNotBlank()) DetailChip(profile.role)
            if (profile.gender.isNotBlank()) DetailChip(profile.gender)
        }
    }
}

@Composable
private fun DetailChip(text: String) {
    Surface(
        shape = DesignTokens.Shape.chip,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.s),
        )
    }
}

@Composable
private fun ContactRow(
    icon: ImageVector,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize.navigation),
        )
        Text(
            text = value.ifBlank { "Not set" },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun initialsOf(name: String): String =
    name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }

/** A detail tile plus its section icon, so the section header can reuse the same glyph. */
private data class DetailEntry(
    val category: String,
    val leadingIcon: ImageVector,
    val item: ProfileGridItem,
)

/**
 * Builds the per-category detail tiles from the [profile]. Values present on the profile render
 * as COMPLETE; absent values render as INCOMPLETE so the grid mirrors the completion state.
 */
private fun buildDetailItems(profile: EmployeeProfile): List<DetailEntry> {
    fun entry(
        category: String,
        sectionIcon: ImageVector,
        id: String,
        title: String,
        value: String,
        tileIcon: ImageVector,
    ): DetailEntry =
        DetailEntry(
            category = category,
            leadingIcon = sectionIcon,
            item =
                ProfileGridItem(
                    id = id,
                    title = title,
                    subtitle = value.ifBlank { "Not set" },
                    icon = tileIcon,
                    category = category,
                    status = if (value.isNotBlank()) ProfileItemStatus.COMPLETE else ProfileItemStatus.INCOMPLETE,
                    action = {},
                ),
        )

    return listOf(
        entry("Personal Info", Icons.Default.Person, "d_name", "Full Name", profile.name, Icons.Default.Person),
        entry("Personal Info", Icons.Default.Person, "d_gender", "Gender", profile.gender, Icons.Default.Badge),
        entry("Location & Assets", Icons.Default.Home, "d_home", "Home Location", profile.homeLocation, Icons.Default.Home),
        entry("Organization", Icons.Default.Business, "d_org", "Organization", profile.organization, Icons.Default.Business),
        entry("Organization", Icons.Default.Business, "d_manager", "Reporting Manager", profile.manager, Icons.Default.SupervisorAccount),
        entry("Policy & Compliance", Icons.Default.Gavel, "d_role", "Role", profile.role, Icons.Default.Gavel),
        entry("Travel", Icons.Default.CardTravel, "d_phone", "Phone", profile.phone, Icons.Default.Phone),
        entry("Apps & Activity", Icons.Default.Apps, "d_code", "Employee Code", profile.employeeCode, Icons.Default.Apps),
    )
}
