package com.mileway.core.ui.support

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.platform.ShakeGestureDetector
import com.mileway.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.mileway.core.ui.components.sheet.ActionConfirmationToneType
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * P31.MISC.1: the shake-to-report quick-actions sheet — a title field plus the standard
 * [ActionConfirmationBottomSheet] remarks field for the description, saved to the local
 * `bug_reports` table via [BugReportViewModel]. Debug/demo surface: no backend, "saved locally"
 * only (see CLAUDE.md "The backend"), so labels are plain strings rather than the shared
 * localizable string table other user-facing sheets use.
 */
@Composable
fun BugReportSheet(
    onDismiss: () -> Unit,
    screen: String = "unknown",
    viewModel: BugReportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.didSubmit) {
        if (state.didSubmit) onDismiss()
    }

    ActionConfirmationBottomSheet(
        title = "Report a bug",
        description = "Saved on this device only — nothing is sent anywhere.",
        confirmLabel = if (state.isSubmitting) "Saving…" else "Submit",
        dismissLabel = "Cancel",
        icon = Icons.Default.BugReport,
        tone = ActionConfirmationToneType.Info,
        showRemarksField = true,
        isRemarksMandatory = true,
        remarksPlaceholder = "What went wrong?",
        onConfirm = { remarks ->
            viewModel.updateDescription(remarks)
            viewModel.submit(screen)
        },
        onDismiss = onDismiss,
    ) {
        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::updateTitle,
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Mounts app-wide: collects [ShakeGestureDetector.shakeEvents] and pops [BugReportSheet] open on
 * each completed shake. A single host per app root (see `MilewayAppRoot`) — no visible UI of its
 * own until shaken.
 */
@Composable
fun ShakeReportHost(
    screen: String = "unknown",
    shakeGestureDetector: ShakeGestureDetector = koinInject(),
) {
    var showSheet by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        shakeGestureDetector.shakeEvents.collect { showSheet = true }
    }
    if (showSheet) {
        BugReportSheet(onDismiss = { showSheet = false }, screen = screen)
    }
}
