package com.miletracker.core.ui.components.scaffold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Shared chrome for every V17 create / submission flow (F0.1): a top bar, a scrollable sectioned body slot,
 * a sticky bottom action bar (optional **Save draft** + a primary **Submit** gated by [canSubmit]), and a
 * blocking **submitting** overlay. Each flow supplies only its form sections via [content] and its
 * submit/draft lambdas, no per-flow chrome rework. Toasts are rendered app-wide by `AppToastHost`, so flows
 * just call `Toasts.show(...)`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormSubmissionScaffold(
    title: String,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    canSubmit: Boolean = true,
    isSubmitting: Boolean = false,
    submitLabel: String = "Submit",
    onSaveDraft: (() -> Unit)? = null,
    saveDraftLabel: String = "Save draft",
    content: @Composable (contentPadding: PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (onSaveDraft != null) {
                        OutlinedButton(
                            onClick = onSaveDraft,
                            enabled = !isSubmitting,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) { Text(saveDraftLabel) }
                    }
                    Button(
                        onClick = onSubmit,
                        enabled = canSubmit && !isSubmitting,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) { Text(submitLabel, fontWeight = FontWeight.Bold) }
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
            ) {
                content(innerPadding)
            }
            if (isSubmitting) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(color = Color.Black.copy(alpha = 0.35f), modifier = Modifier.fillMaxSize()) {}
                    CircularProgressIndicator()
                }
            }
        }
    }
}
