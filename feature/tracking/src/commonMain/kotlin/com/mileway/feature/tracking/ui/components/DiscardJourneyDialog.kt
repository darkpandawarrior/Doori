package com.mileway.feature.tracking.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import com.mileway.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.mileway.core.ui.components.sheet.ActionConfirmationToneType
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_action_cancel
import com.mileway.core.ui.resources.tracking_action_discard
import com.mileway.core.ui.resources.tracking_discard_body_setup
import com.mileway.core.ui.resources.tracking_discard_body_tracking
import com.mileway.core.ui.resources.tracking_discard_confirm_suffix
import com.mileway.core.ui.resources.tracking_discard_journey_title
import org.jetbrains.compose.resources.stringResource

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
            stringResource(Res.string.tracking_discard_body_tracking)
        } else {
            stringResource(Res.string.tracking_discard_body_setup)
        }
    ActionConfirmationBottomSheet(
        title = stringResource(Res.string.tracking_discard_journey_title),
        description = stringResource(Res.string.tracking_discard_confirm_suffix, body),
        confirmLabel = stringResource(Res.string.tracking_action_discard),
        dismissLabel = stringResource(Res.string.tracking_action_cancel),
        icon = Icons.Filled.Delete,
        tone = ActionConfirmationToneType.Danger,
        onConfirm = { onConfirm() },
        onDismiss = onDismiss,
    )
}
