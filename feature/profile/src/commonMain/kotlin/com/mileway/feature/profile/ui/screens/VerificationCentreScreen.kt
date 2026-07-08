package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.verification.DocRequirement
import com.mileway.core.data.verification.DocStatus
import com.mileway.core.data.verification.VerificationDocument
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.verification_back
import com.mileway.core.ui.resources.verification_counter_pending
import com.mileway.core.ui.resources.verification_counter_rejected
import com.mileway.core.ui.resources.verification_counter_verified
import com.mileway.core.ui.resources.verification_optional
import com.mileway.core.ui.resources.verification_required
import com.mileway.core.ui.resources.verification_status_not_uploaded
import com.mileway.core.ui.resources.verification_status_pending
import com.mileway.core.ui.resources.verification_status_rejected
import com.mileway.core.ui.resources.verification_status_uploaded
import com.mileway.core.ui.resources.verification_status_verified
import com.mileway.core.ui.resources.verification_submit
import com.mileway.core.ui.resources.verification_submit_incomplete
import com.mileway.core.ui.resources.verification_subtitle
import com.mileway.core.ui.resources.verification_title
import com.mileway.core.ui.resources.verification_tnc_agree
import com.mileway.core.ui.resources.verification_tnc_body
import com.mileway.core.ui.resources.verification_tnc_cancel
import com.mileway.core.ui.resources.verification_tnc_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.viewmodel.VerificationCentreViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Shared status → label mapping used by the centre and detail screens. */
internal fun docStatusLabel(status: DocStatus): StringResource =
    when (status) {
        DocStatus.NOT_UPLOADED -> Res.string.verification_status_not_uploaded
        DocStatus.UPLOADED -> Res.string.verification_status_uploaded
        DocStatus.APPROVAL_PENDING -> Res.string.verification_status_pending
        DocStatus.VERIFIED -> Res.string.verification_status_verified
        DocStatus.REJECTED -> Res.string.verification_status_rejected
    }

/** Shared status → accent colour. */
internal fun docStatusColor(status: DocStatus): Color =
    when (status) {
        DocStatus.NOT_UPLOADED -> Color(0xFF6B7280)
        DocStatus.UPLOADED -> Color(0xFF2563EB)
        DocStatus.APPROVAL_PENDING -> Color(0xFFB45309)
        DocStatus.VERIFIED -> Color(0xFF16A34A)
        DocStatus.REJECTED -> Color(0xFFDC2626)
    }

/**
 * PLAN_V24 P4.2: the verification centre. Lists every document with its status chip (and rejection
 * reason line), aggregate counters, and a "Submit for verification" action gated on all mandatory
 * docs being complete, behind a T&C agreement dialog (the reference app `DocumentListFragment`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationCentreScreen(
    onBack: () -> Unit,
    onOpenDocument: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VerificationCentreViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var showTnc by remember { mutableStateOf(false) }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), modifier = modifier) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Box(
                modifier =
                    Modifier
                        .background(Brush.horizontalGradient(listOf(Color(0xFF1565C0), Color(0xFF0D47A1))))
                        .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.verification_back), tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(Res.string.verification_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            stringResource(Res.string.verification_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                CounterPill(stringResource(Res.string.verification_counter_verified, uiState.verifiedCount), Color(0xFF16A34A), Modifier.weight(1f))
                CounterPill(stringResource(Res.string.verification_counter_pending, uiState.pendingCount), Color(0xFFB45309), Modifier.weight(1f))
                CounterPill(stringResource(Res.string.verification_counter_rejected, uiState.rejectedCount), Color(0xFFDC2626), Modifier.weight(1f))
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                items(uiState.documents) { doc ->
                    DocumentRow(doc = doc, onClick = { onOpenDocument(doc.docType) })
                }
            }

            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(DesignTokens.Spacing.l)) {
                uiState.submitError?.let { failing ->
                    Text(
                        stringResource(Res.string.verification_submit_incomplete, failing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = DesignTokens.Spacing.s),
                    )
                }
                Button(
                    onClick = { showTnc = true },
                    enabled = uiState.canSubmit,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.verification_submit)) }
            }
        }
    }

    if (showTnc) {
        AlertDialog(
            onDismissRequest = { showTnc = false },
            title = { Text(stringResource(Res.string.verification_tnc_title)) },
            text = { Text(stringResource(Res.string.verification_tnc_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showTnc = false
                    viewModel.submit()
                }) { Text(stringResource(Res.string.verification_tnc_agree)) }
            },
            dismissButton = {
                TextButton(onClick = { showTnc = false }) { Text(stringResource(Res.string.verification_tnc_cancel)) }
            },
        )
    }
}

@Composable
private fun CounterPill(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(color = accent.copy(alpha = 0.12f), shape = DesignTokens.Shape.roundedMd, modifier = modifier) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            modifier = Modifier.padding(vertical = DesignTokens.Spacing.s, horizontal = DesignTokens.Spacing.m),
        )
    }
}

@Composable
private fun DocumentRow(
    doc: VerificationDocument,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(doc.docTypeText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(docStatusLabel(doc.status)),
                        style = MaterialTheme.typography.labelMedium,
                        color = docStatusColor(doc.status),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(DesignTokens.Spacing.s))
                    Text(
                        stringResource(
                            if (doc.requirement == DocRequirement.MANDATORY) Res.string.verification_required else Res.string.verification_optional,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (doc.status == DocStatus.REJECTED && doc.reason.isNotBlank()) {
                    Text(
                        doc.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
