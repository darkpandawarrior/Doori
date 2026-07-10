package com.mileway.feature.profile.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.review.ReviewResult
import com.mileway.core.data.vehicle.VehicleAudit
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.viewmodel.SelfAuditViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V24 P12.6: the vehicle self-audit — a seeded per-type inspection checklist (tyres, lights,
 * horn, cleanliness, documents; four-wheelers also seatbelts), each item photographed, an optional
 * issue note, submit → a verdict resolved by the shared review simulator, and the per-vehicle audit
 * history below. Reached from the garage; gated by the `selfAudit` plugin (Gig persona).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfAuditScreen(
    vehicleId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SelfAuditViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(vehicleId) { viewModel.load(vehicleId) }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), modifier = modifier) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = sav("self_audit_back", "Back"))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(sav("self_audit_title", "Self-audit"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (state.vehicleName.isNotBlank()) {
                        Text(state.vehicleName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                item {
                    Text(
                        sav("self_audit_checklist", "Inspection checklist"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                items(state.checklist, key = { it }) { item ->
                    ChecklistRow(
                        item = item,
                        photographed = item in state.photos,
                        onCapture = { uri -> viewModel.setPhoto(item, uri) },
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = viewModel::setNote,
                        label = { Text(sav("self_audit_note", "Issue note (optional)")) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Button(
                        onClick = viewModel::submit,
                        enabled = state.canSubmit,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(sav("self_audit_submit", "Submit audit"))
                    }
                }
                if (state.history.isNotEmpty()) {
                    item {
                        Text(
                            sav("self_audit_history", "Audit history"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    items(state.history, key = { it.id }) { audit -> AuditHistoryCard(audit) }
                }
                item { Spacer(Modifier.navigationBarsPadding()) }
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    item: String,
    photographed: Boolean,
    onCapture: (String) -> Unit,
) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) onCapture(uri.toString())
        }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (photographed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (photographed) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(DesignTokens.Spacing.s))
            Text(checklistLabel(item), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            IconButton(
                onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = sav("self_audit_capture", "Capture photo"))
            }
        }
    }
}

@Composable
private fun AuditHistoryCard(audit: VehicleAudit) {
    val (label, color, icon) =
        when (val v = audit.verdict) {
            is ReviewResult.Pending -> Triple(sav("self_audit_pending", "Under review"), Color(0xFFEA580C), Icons.Default.HourglassEmpty)
            is ReviewResult.Approved -> Triple(sav("self_audit_passed", "Passed"), Color(0xFF16A34A), Icons.Default.CheckCircle)
            is ReviewResult.Rejected -> Triple(sav("self_audit_failed", "Failed") + ": " + v.reason, Color(0xFFB91C1C), Icons.Default.Warning)
        }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.m)) {
            AssistChip(
                onClick = {},
                label = { Text(label) },
                leadingIcon = { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp)) },
                colors = AssistChipDefaults.assistChipColors(labelColor = color),
            )
            Text(
                sav("self_audit_items_covered", "{n} checks covered").replace("{n}", audit.checkedItems.size.toString()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun checklistLabel(item: String): String = item.replaceFirstChar { it.uppercase() }

/** Screen-internal labels via the dynamic resolver with an English fallback (no generated symbols). */
@Composable
private fun sav(
    key: String,
    fallback: String,
): String = Res.allStringResources[key]?.let { stringResource(it) } ?: fallback
