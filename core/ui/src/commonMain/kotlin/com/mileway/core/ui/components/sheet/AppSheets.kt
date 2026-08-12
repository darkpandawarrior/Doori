package com.mileway.core.ui.components.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.mileway.core.ui.theme.DesignTokens

/**
 * Generic modal surface. Prefer this (or [ActionConfirmationBottomSheet] for confirmations) over
 * `AlertDialog` for every modal, project convention is bottom sheets over dialogs. Wraps
 * [ModalBottomSheet] with the standard insets + an optional title; the caller fills [content] (lists,
 * pickers, forms, detail views, …).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = DesignTokens.Shape.sheet,
        // Matches MilewayActionSheet's raised container so every sheet in the app — tier 1 or this
        // generic one — reads as the same elevated surface, not a flat panel blending into the scrim.
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier =
                modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = DesignTokens.Spacing.xl)
                    .padding(bottom = DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            if (title != null) {
                // Bold, matching MilewayActionSheet's tier-1 header weight — one consistent title
                // treatment across every sheet in the app instead of a slightly softer one here.
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}
