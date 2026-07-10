package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mileway.core.data.verification.DocInfoField
import com.mileway.core.media.model.CaptureMode
import com.mileway.core.media.model.MediaCaptureConfig
import com.mileway.core.media.model.MediaCaptureResult
import com.mileway.core.media.rememberMediaCaptureLauncher
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.verification_back
import com.mileway.core.ui.resources.verification_detail_add_photo
import com.mileway.core.ui.resources.verification_detail_info
import com.mileway.core.ui.resources.verification_detail_locked
import com.mileway.core.ui.resources.verification_detail_save
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.viewmodel.VerificationCentreViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V24 P4.2: a single document's detail — instructions, up to `docCount` image slots (uploaded
 * with the P3.3 PickVisualMedia picker), and editable info fields. Locked (verified / under review)
 * documents are read-only.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DocumentDetailScreen(
    docType: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VerificationCentreViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val doc = uiState.documents.firstOrNull { it.docType == docType }

    // V26 P26.SITE.3: KYC document upload via core:media's shared launcher. CaptureMode.Document
    // (real GMS document scanner with a file-picker fallback on unavailable devices) instead of
    // the prior plain gallery pick — a genuinely better fit for a KYC document than a raw photo
    // picker, and safe here since this screen is Android-only (no iOS actual gap to worry about).
    val picker =
        rememberMediaCaptureLauncher(
            config = MediaCaptureConfig(allowedModes = setOf(CaptureMode.Document)),
            onResult = { result ->
                if (result is MediaCaptureResult.Attachments) {
                    result.items.firstOrNull()?.let { viewModel.uploadSlot(docType, it.uri) }
                }
            },
        )

    // Local edit buffer for info fields, keyed by field key, seeded from the doc.
    val fieldEdits = remember(docType) { mutableStateMapOf<String, String>() }

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
                    Text(
                        doc?.docTypeText ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }

            if (doc == null) return@Column

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                Text(doc.instructions, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(docStatusLabel(doc.status)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = docStatusColor(doc.status),
                    )
                }
                if (doc.status.isLocked) {
                    Text(
                        stringResource(Res.string.verification_detail_locked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (doc.reason.isNotBlank()) {
                    Text(doc.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                // Image slots: existing uploads + an add button while unlocked and below the cap.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                ) {
                    doc.docUrls.forEach { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }
                    if (!doc.status.isLocked && doc.docUrls.size < doc.docCount) {
                        Box(
                            modifier =
                                Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable(onClick = picker),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.verification_detail_add_photo))
                        }
                    }
                }

                // Editable info fields.
                if (doc.docInfo.isNotEmpty()) {
                    Text(stringResource(Res.string.verification_detail_info), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    doc.docInfo.forEach { field ->
                        DocInfoRow(
                            field = field,
                            enabled = doc.isDocInfoEditable && field.editable && !doc.status.isLocked,
                            currentValue = fieldEdits[field.key] ?: field.value,
                            onChange = { fieldEdits[field.key] = it },
                            onSave = { viewModel.updateInfoField(docType, field.key, fieldEdits[field.key] ?: field.value) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocInfoRow(
    field: DocInfoField,
    enabled: Boolean,
    currentValue: String,
    onChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.m), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
            OutlinedTextField(
                value = currentValue,
                onValueChange = onChange,
                label = { Text(field.label) },
                enabled = enabled,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (enabled) {
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.verification_detail_save))
                }
            }
        }
    }
}
