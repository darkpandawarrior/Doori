package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.mileway.core.ui.components.sheet.ActionConfirmationToneType
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_delegation_active
import com.mileway.core.ui.resources.profile_delegation_add_cd
import com.mileway.core.ui.resources.profile_delegation_add_new
import com.mileway.core.ui.resources.profile_delegation_add_title
import com.mileway.core.ui.resources.profile_delegation_back
import com.mileway.core.ui.resources.profile_delegation_cancel
import com.mileway.core.ui.resources.profile_delegation_delegate_to
import com.mileway.core.ui.resources.profile_delegation_expires
import com.mileway.core.ui.resources.profile_delegation_my_subtitle
import com.mileway.core.ui.resources.profile_delegation_my_title
import com.mileway.core.ui.resources.profile_delegation_none_incoming
import com.mileway.core.ui.resources.profile_delegation_revoke
import com.mileway.core.ui.resources.profile_delegation_revoke_desc
import com.mileway.core.ui.resources.profile_delegation_revoke_title
import com.mileway.core.ui.resources.profile_delegation_select_member
import com.mileway.core.ui.resources.profile_delegation_submit
import com.mileway.core.ui.resources.profile_delegation_subtitle
import com.mileway.core.ui.resources.profile_delegation_title
import com.mileway.core.ui.resources.profile_delegation_to_me_subtitle
import com.mileway.core.ui.resources.profile_delegation_to_me_title
import com.mileway.core.ui.resources.profile_delegation_type
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.feature.profile.model.Delegation
import com.mileway.feature.profile.viewmodel.DelegationViewModel
import com.mileway.feature.profile.viewmodel.formatDelegationExpiry
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

private val DELEGATION_TYPES = listOf("View Only", "Approve", "Full Access")

private val TEAM_MEMBERS =
    listOf(
        "Priya Sharma",
        "Rahul Mehra",
        "Asha Verma",
        "Vikram Nair",
        "Sunita Pillai",
        "Anil Kumar",
        "Deepa Nair",
        "Harish Reddy",
    )

private data class DelegatedByEntry(
    val id: String,
    val delegatorName: String,
    val scope: String,
    val until: String,
)

/**
 * "Delegated To Me" (incoming coverage) has no persistence requirement in this task's scope —
 * P6.3 only Room-backs the outgoing "My Delegations" list — so this section keeps its existing
 * demo-fixture shape.
 */
private val DELEGATED_TO_ME =
    listOf(
        DelegatedByEntry("DB001", "Vikram Nair", "Travel", "Returning 20 Jun 2026"),
        DelegatedByEntry("DB002", "Sunita Pillai", "Expense & Advance", "Returning 25 Jun 2026"),
    )

/** Default new-delegation expiry: 6 months out, matching the reference app's default grant window. */
private fun defaultExpiryMillis(): Long =
    Clock.System
        .now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .plus(6, DateTimeUnit.MONTH)
        .atStartOfDayIn(TimeZone.currentSystemDefault())
        .toEpochMilliseconds()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelegationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DelegationViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsState()
    val myDelegations = uiState.delegations

    var showAddSheet by remember { mutableStateOf(false) }
    var revokeTarget by remember { mutableStateOf<Delegation?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            Box(
                modifier =
                    Modifier
                        .background(Brush.horizontalGradient(listOf(Color(0xFF1565C0), Color(0xFF7B1FA2))))
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.profile_delegation_back), tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(Res.string.profile_delegation_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            stringResource(Res.string.profile_delegation_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                    IconButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.profile_delegation_add_cd), tint = Color.White)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
            ) {
                item {
                    SectionHeader(
                        icon = Icons.Default.PersonAddAlt,
                        title = stringResource(Res.string.profile_delegation_my_title),
                        subtitle = stringResource(Res.string.profile_delegation_my_subtitle),
                    )
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = DesignTokens.Shape.roundedMd,
                        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
                    ) {
                        Column {
                            myDelegations.forEachIndexed { index, entry ->
                                DelegationRow(
                                    entry = entry,
                                    isActive = entry.isActive,
                                    onToggle = { viewModel.setActive(entry.id, !entry.isActive) },
                                    onRevoke = { revokeTarget = entry },
                                )
                                if (index < myDelegations.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(DesignTokens.Spacing.s)) }

                item {
                    SectionHeader(
                        icon = Icons.Default.Group,
                        title = stringResource(Res.string.profile_delegation_to_me_title),
                        subtitle = stringResource(Res.string.profile_delegation_to_me_subtitle),
                    )
                }
                item {
                    if (DELEGATED_TO_ME.isEmpty()) {
                        Text(
                            stringResource(Res.string.profile_delegation_none_incoming),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = DesignTokens.Shape.roundedMd,
                            elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
                        ) {
                            Column {
                                DELEGATED_TO_ME.forEachIndexed { index, entry ->
                                    DelegatedByRow(entry = entry)
                                    if (index < DELEGATED_TO_ME.lastIndex) {
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { showAddSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(DesignTokens.Spacing.s))
                        Text(stringResource(Res.string.profile_delegation_add_new))
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                viewModel.clearSubmitError()
                showAddSheet = false
            },
            sheetState = sheetState,
        ) {
            AddDelegationSheet(
                submitError = uiState.submitError,
                onSubmit = { name, delegationType ->
                    viewModel.add(delegateName = name, scope = delegationType, expiresAtMillis = defaultExpiryMillis())
                    // Only closes the sheet on a valid submit — a blank name/scope leaves
                    // submitError set on the next recomposition and the sheet stays open.
                    if (name.isNotBlank() && delegationType.isNotBlank()) {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false }
                    }
                },
                onDismiss = {
                    viewModel.clearSubmitError()
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false }
                },
            )
        }
    }

    revokeTarget?.let { target ->
        ActionConfirmationBottomSheet(
            title = stringResource(Res.string.profile_delegation_revoke_title),
            description = stringResource(Res.string.profile_delegation_revoke_desc, target.delegateName),
            confirmLabel = stringResource(Res.string.profile_delegation_revoke),
            tone = ActionConfirmationToneType.Danger,
            onConfirm = {
                viewModel.revoke(target.id)
                revokeTarget = null
            },
            onDismiss = { revokeTarget = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDelegationSheet(
    submitError: String?,
    onSubmit: (name: String, delegationType: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var memberExpanded by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(DELEGATION_TYPES[0]) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(Res.string.profile_delegation_add_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        ExposedDropdownMenuBox(
            expanded = memberExpanded,
            onExpandedChange = { memberExpanded = it },
        ) {
            OutlinedTextField(
                value = selectedMember,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(Res.string.profile_delegation_delegate_to)) },
                placeholder = { Text(stringResource(Res.string.profile_delegation_select_member)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(memberExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = memberExpanded,
                onDismissRequest = { memberExpanded = false },
            ) {
                TEAM_MEMBERS.forEach { member ->
                    DropdownMenuItem(
                        text = { Text(member) },
                        onClick = {
                            selectedMember = member
                            memberExpanded = false
                        },
                    )
                }
            }
        }

        Text(stringResource(Res.string.profile_delegation_type), style = MaterialTheme.typography.labelMedium)
        DELEGATION_TYPES.forEach { type ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                RadioButton(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                )
                Text(type, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (submitError != null) {
            Text(submitError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(Res.string.profile_delegation_cancel)) }
            Button(
                onClick = { onSubmit(selectedMember, selectedType) },
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(Res.string.profile_delegation_submit)) }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DelegationRow(
    entry: Delegation,
    isActive: Boolean,
    onToggle: () -> Unit,
    onRevoke: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                entry.delegateName.first().toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(Modifier.width(DesignTokens.Spacing.m))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.delegateName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(entry.scope, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(Res.string.profile_delegation_expires, formatDelegationExpiry(entry.expiresAtMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isActive) {
                TextButton(onClick = onRevoke, modifier = Modifier.height(28.dp)) {
                    Text(
                        stringResource(Res.string.profile_delegation_revoke),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        Switch(checked = isActive, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun DelegatedByRow(entry: DelegatedByEntry) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6A1B9A).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                entry.delegatorName.first().toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6A1B9A),
            )
        }
        Spacer(Modifier.width(DesignTokens.Spacing.m))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.delegatorName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(entry.scope, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(entry.until, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = MilewayColors.success.copy(alpha = 0.15f),
        ) {
            Text(
                stringResource(Res.string.profile_delegation_active),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MilewayColors.success,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}
