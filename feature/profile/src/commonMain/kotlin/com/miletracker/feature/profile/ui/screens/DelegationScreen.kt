package com.miletracker.feature.profile.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.miletracker.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.miletracker.core.ui.components.sheet.ActionConfirmationToneType
import com.miletracker.core.ui.theme.DesignTokens
import kotlinx.coroutines.launch

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

private data class DelegationEntry(
    val id: String,
    val name: String,
    val scope: String,
    val expires: String,
    val isActive: Boolean,
)

private data class DelegatedByEntry(
    val id: String,
    val delegatorName: String,
    val scope: String,
    val until: String,
)

private val INITIAL_DELEGATIONS =
    listOf(
        DelegationEntry("D001", "Priya Sharma", "Mileage & Expense", "30 Jun 2026", true),
        DelegationEntry("D002", "Rahul Mehra", "All categories", "15 Jul 2026", true),
        DelegationEntry("D003", "Kavitha Rao", "Travel only", "01 Jun 2026", false),
    )

private val DELEGATED_TO_ME =
    listOf(
        DelegatedByEntry("DB001", "Vikram Nair", "Travel", "Returning 20 Jun 2026"),
        DelegatedByEntry("DB002", "Sunita Pillai", "Expense & Advance", "Returning 25 Jun 2026"),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelegationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val myDelegations = remember { mutableStateListOf(*INITIAL_DELEGATIONS.toTypedArray()) }
    val toggleStates = remember { mutableStateOf(myDelegations.associate { it.id to it.isActive }) }

    var showAddSheet by remember { mutableStateOf(false) }
    var revokeTarget by remember { mutableStateOf<DelegationEntry?>(null) }
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Delegation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Manage who acts on your behalf", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                    IconButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add delegation", tint = Color.White)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
            ) {
                item {
                    SectionHeader(icon = Icons.Default.PersonAddAlt, title = "My Delegations", subtitle = "People you've authorised to act for you")
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = DesignTokens.Shape.roundedMd,
                        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
                    ) {
                        Column {
                            myDelegations.forEachIndexed { index, entry ->
                                val active = toggleStates.value[entry.id] ?: entry.isActive
                                DelegationRow(
                                    entry = entry,
                                    isActive = active,
                                    onToggle = {
                                        toggleStates.value =
                                            toggleStates.value.toMutableMap().also { map ->
                                                map[entry.id] = !active
                                            }
                                    },
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
                    SectionHeader(icon = Icons.Default.Group, title = "Delegated To Me", subtitle = "You're covering for these people")
                }
                item {
                    if (DELEGATED_TO_ME.isEmpty()) {
                        Text(
                            "No active incoming delegations",
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
                        Text("Add New Delegation")
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState,
        ) {
            AddDelegationSheet(
                onSubmit = { name, delegationType ->
                    val newId = "D${kotlin.time.Clock.System.now().toEpochMilliseconds() % 10_000}"
                    myDelegations.add(
                        DelegationEntry(
                            id = newId,
                            name = name,
                            scope = delegationType,
                            expires = "31 Dec 2026",
                            isActive = true,
                        ),
                    )
                    toggleStates.value = toggleStates.value.toMutableMap().also { it[newId] = true }
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false }
                },
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false }
                },
            )
        }
    }

    revokeTarget?.let { target ->
        ActionConfirmationBottomSheet(
            title = "Revoke Delegation",
            description = "Remove ${target.name}'s access? This cannot be undone.",
            confirmLabel = "Revoke",
            tone = ActionConfirmationToneType.Danger,
            onConfirm = {
                myDelegations.removeAll { it.id == target.id }
                revokeTarget = null
            },
            onDismiss = { revokeTarget = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDelegationSheet(
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
        Text("Add Delegation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        ExposedDropdownMenuBox(
            expanded = memberExpanded,
            onExpandedChange = { memberExpanded = it },
        ) {
            OutlinedTextField(
                value = selectedMember,
                onValueChange = {},
                readOnly = true,
                label = { Text("Delegate To") },
                placeholder = { Text("Select team member") },
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

        Text("Delegation Type", style = MaterialTheme.typography.labelMedium)
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

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = { if (selectedMember.isNotBlank()) onSubmit(selectedMember, selectedType) },
                enabled = selectedMember.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) { Text("Submit") }
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
    entry: DelegationEntry,
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
                entry.name.first().toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(Modifier.width(DesignTokens.Spacing.m))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(entry.scope, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Expires ${entry.expires}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isActive) {
                TextButton(onClick = onRevoke, modifier = Modifier.height(28.dp)) {
                    Text("Revoke", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
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
            color = Color(0xFF4CAF50).copy(alpha = 0.15f),
        ) {
            Text(
                "Active",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4CAF50),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}
