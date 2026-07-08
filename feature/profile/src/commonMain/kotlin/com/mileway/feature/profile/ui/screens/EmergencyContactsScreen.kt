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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.data.emergency.EmergencyContact
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_emergency_add_cd
import com.mileway.core.ui.resources.profile_emergency_add_title
import com.mileway.core.ui.resources.profile_emergency_back
import com.mileway.core.ui.resources.profile_emergency_cancel
import com.mileway.core.ui.resources.profile_emergency_country_code
import com.mileway.core.ui.resources.profile_emergency_delete
import com.mileway.core.ui.resources.profile_emergency_edit_title
import com.mileway.core.ui.resources.profile_emergency_empty
import com.mileway.core.ui.resources.profile_emergency_name
import com.mileway.core.ui.resources.profile_emergency_phone
import com.mileway.core.ui.resources.profile_emergency_save
import com.mileway.core.ui.resources.profile_emergency_subtitle
import com.mileway.core.ui.resources.profile_emergency_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.viewmodel.EmergencyContactsViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V24 P3.5: manage the emergency-contacts list (the reference app/the reference app `emergency/`). Room-backed list
 * capped at 5, with an add/edit bottom sheet (name + dial code + phone, phone validated) and
 * delete. Reachable from the Account hub's plugin-gated Emergency tile. The contacts feed the
 * tracking SOS sheet.
 *
 * ponytail: device-contacts picker deferred (manual entry only, both platforms) — noted in
 * PROGRESS; iOS is manual-only by design anyway, and standing up the Android picker needs a first
 * iosMain source set in this module. Manual entry is a complete local implementation, not a stub.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmergencyContactsViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsState()

    var editing by remember { mutableStateOf<EmergencyContact?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun closeSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            showSheet = false
            editing = null
            viewModel.clearSubmitError()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            Box(
                modifier =
                    Modifier
                        .background(Brush.horizontalGradient(listOf(Color(0xFFB91C1C), Color(0xFF7F1D1D))))
                        .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.profile_emergency_back), tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(Res.string.profile_emergency_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            stringResource(Res.string.profile_emergency_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                    if (!uiState.isAtCapacity) {
                        IconButton(onClick = {
                            editing = null
                            showSheet = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.profile_emergency_add_cd), tint = Color.White)
                        }
                    }
                }
            }

            if (uiState.contacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(Res.string.profile_emergency_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(DesignTokens.Spacing.xl),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(DesignTokens.Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                ) {
                    items(uiState.contacts) { contact ->
                        EmergencyContactCard(
                            contact = contact,
                            onEdit = {
                                editing = contact
                                showSheet = true
                            },
                            onDelete = { viewModel.delete(contact.id) },
                        )
                    }
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { closeSheet() }, sheetState = sheetState) {
            EmergencyContactEditor(
                initial = editing,
                submitError = uiState.submitError,
                onSave = { name, code, phone ->
                    val accepted = viewModel.save(id = editing?.id ?: "", name = name, phone = phone, countryCode = code)
                    if (accepted) closeSheet()
                },
                onCancel = { closeSheet() },
            )
        }
    }
}

@Composable
private fun EmergencyContactCard(
    contact: EmergencyContact,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(DesignTokens.Spacing.m))
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    contact.displayNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.profile_emergency_edit_title))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.profile_emergency_delete))
            }
        }
    }
}

@Composable
private fun EmergencyContactEditor(
    initial: EmergencyContact?,
    submitError: String?,
    onSave: (String, String, String) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var code by remember { mutableStateOf(initial?.countryCode ?: "+91") }
    var phone by remember { mutableStateOf(initial?.phoneNo ?: "") }

    Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(DesignTokens.Spacing.l),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        Text(
            stringResource(if (initial == null) Res.string.profile_emergency_add_title else Res.string.profile_emergency_edit_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(Res.string.profile_emergency_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text(stringResource(Res.string.profile_emergency_country_code)) },
                singleLine = true,
                modifier = Modifier.width(96.dp),
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(stringResource(Res.string.profile_emergency_phone)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        if (submitError != null) {
            Text(submitError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.profile_emergency_cancel))
            }
            Button(onClick = { onSave(name, code, phone) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.profile_emergency_save))
            }
        }
        Spacer(Modifier.height(DesignTokens.Spacing.m))
    }
}
