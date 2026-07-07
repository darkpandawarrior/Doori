package com.mileway.feature.tracking.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import com.mileway.core.common.deeplink.DeepLinkAction
import com.mileway.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.mileway.core.ui.components.sheet.ActionConfirmationToneType
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_action_cancel
import com.mileway.core.ui.resources.tracking_action_discard
import com.mileway.core.ui.resources.tracking_action_stop
import com.mileway.core.ui.resources.tracking_deeplink_discard_desc
import com.mileway.core.ui.resources.tracking_deeplink_stop_desc
import com.mileway.core.ui.resources.tracking_discard_journey_title
import com.mileway.core.ui.resources.tracking_stop_tracking_title
import org.jetbrains.compose.resources.stringResource

/**
 * DL.5: confirmation gate for a destructive [DeepLinkAction] arriving from an external caller
 * (partner integration / Shortcuts / iOS App Intents) — shown before [DeepLinkAction.Stop] or
 * [DeepLinkAction.Discard] is actually dispatched to `TrackingController`. Non-destructive actions
 * (Start/Pause/CheckIn) never reach this composable; see
 * `DeepLinkActionDispatcher.requiresConfirmation`.
 *
 * Reuses [ActionConfirmationBottomSheet] (the project's confirmation idiom — see
 * [DiscardJourneyDialog]) rather than introducing a new dialog shape per action.
 */
@Composable
fun DeepLinkConfirmationDialog(
    action: DeepLinkAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (action) {
        DeepLinkAction.Stop ->
            ActionConfirmationBottomSheet(
                title = stringResource(Res.string.tracking_stop_tracking_title),
                description = stringResource(Res.string.tracking_deeplink_stop_desc),
                confirmLabel = stringResource(Res.string.tracking_action_stop),
                dismissLabel = stringResource(Res.string.tracking_action_cancel),
                icon = Icons.Filled.Stop,
                tone = ActionConfirmationToneType.Danger,
                onConfirm = { onConfirm() },
                onDismiss = onDismiss,
            )
        DeepLinkAction.Discard ->
            ActionConfirmationBottomSheet(
                title = stringResource(Res.string.tracking_discard_journey_title),
                description = stringResource(Res.string.tracking_deeplink_discard_desc),
                confirmLabel = stringResource(Res.string.tracking_action_discard),
                dismissLabel = stringResource(Res.string.tracking_action_cancel),
                icon = Icons.Filled.Delete,
                tone = ActionConfirmationToneType.Danger,
                onConfirm = { onConfirm() },
                onDismiss = onDismiss,
            )
        else -> Unit // ponytail: only destructive actions ever route here (see requiresConfirmation).
    }
}
