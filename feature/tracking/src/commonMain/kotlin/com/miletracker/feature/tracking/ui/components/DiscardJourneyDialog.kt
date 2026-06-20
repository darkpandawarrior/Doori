package com.miletracker.feature.tracking.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import com.miletracker.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.miletracker.core.ui.components.sheet.ActionConfirmationToneType

/**
 * Discard-journey confirmation. Migrated from an AlertDialog to the shared
 * [ActionConfirmationBottomSheet] (Danger tone). Signature unchanged so callers are untouched.
 */
@Composable
fun DiscardJourneyDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isTracking: Boolean = true,
) {
    val body =
        if (isTracking) {
            "Are you sure you want to discard this journey? All tracking data will be lost."
        } else {
            "Are you sure you want to discard this journey setup? Your odometer readings will be removed."
        }
    ActionConfirmationBottomSheet(
        title = "Discard Journey?",
        description = "$body This action cannot be undone.",
        confirmLabel = "Discard",
        dismissLabel = "Cancel",
        icon = Icons.Filled.Delete,
        tone = ActionConfirmationToneType.Danger,
        onConfirm = { onConfirm() },
        onDismiss = onDismiss,
    )
}
