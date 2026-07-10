package com.mileway.core.ui.components.banner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.data.banner.Banner
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.banner_deletion_requested
import com.mileway.core.ui.resources.banner_dismiss
import com.mileway.core.ui.resources.banner_document_expiry
import com.mileway.core.ui.resources.banner_subscription_expiry
import com.mileway.core.ui.resources.banner_update_action
import com.mileway.core.ui.resources.banner_update_ready
import com.mileway.core.ui.resources.profile_delegation_acting_as
import com.mileway.core.ui.resources.profile_delegation_end
import org.jetbrains.compose.resources.stringResource

/**
 * PLAN_V24 P13.1: the single priority banner stack, hoisted at the composition root. Renders the
 * already-assembled, already-filtered [banners] top-to-bottom (the caller sorts them via
 * `BannerAssembler`). The P7.3 "Acting as <name>" banner is now the [Banner.Delegate] row here — one
 * banner mechanism, not two.
 *
 * Empty [banners] renders nothing, so the baseline root is byte-identical to before this task.
 */
@Composable
fun BannerHost(
    banners: List<Banner>,
    onDismiss: (Banner) -> Unit,
    onDelegateEnd: () -> Unit,
    modifier: Modifier = Modifier,
    onUpdate: () -> Unit = {},
) {
    if (banners.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth().statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        banners.forEach { banner ->
            when (banner) {
                is Banner.Delegate ->
                    BannerRow(
                        icon = Icons.Filled.SupervisorAccount,
                        text = stringResource(Res.string.profile_delegation_acting_as, banner.name),
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        onContent = MaterialTheme.colorScheme.onTertiaryContainer,
                        actionLabel = stringResource(Res.string.profile_delegation_end),
                        onAction = onDelegateEnd,
                    )
                Banner.UpdateReady ->
                    BannerRow(
                        icon = Icons.Filled.SystemUpdate,
                        text = stringResource(Res.string.banner_update_ready),
                        container = MaterialTheme.colorScheme.primaryContainer,
                        onContent = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionLabel = stringResource(Res.string.banner_update_action),
                        onAction = onUpdate,
                    )
                is Banner.Custom ->
                    BannerRow(
                        icon = Icons.Filled.Info,
                        text = banner.text,
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        onContent = MaterialTheme.colorScheme.onSecondaryContainer,
                        onDismiss = { onDismiss(banner) },
                    )
                Banner.DeletionRequested ->
                    BannerRow(
                        icon = Icons.Filled.Info,
                        text = stringResource(Res.string.banner_deletion_requested),
                        container = MaterialTheme.colorScheme.errorContainer,
                        onContent = MaterialTheme.colorScheme.onErrorContainer,
                        onDismiss = { onDismiss(banner) },
                    )
                is Banner.DocumentExpiry ->
                    BannerRow(
                        icon = Icons.Filled.Description,
                        text = stringResource(Res.string.banner_document_expiry, banner.count),
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        onContent = MaterialTheme.colorScheme.onSecondaryContainer,
                        onDismiss = { onDismiss(banner) },
                    )
                is Banner.SubscriptionExpiry ->
                    BannerRow(
                        icon = Icons.Filled.Workspaces,
                        text = stringResource(Res.string.banner_subscription_expiry, banner.daysLeft),
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        onContent = MaterialTheme.colorScheme.onSecondaryContainer,
                        onDismiss = { onDismiss(banner) },
                    )
            }
        }
    }
}

@Composable
private fun BannerRow(
    icon: ImageVector,
    text: String,
    container: Color,
    onContent: Color,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    Surface(color = container, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = onContent)
            Spacer(Modifier.width(12.dp))
            Text(
                text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = onContent,
                modifier = Modifier.weight(1f),
            )
            if (actionLabel != null && onAction != null) {
                Text(
                    actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = onContent,
                    modifier = Modifier.clickable(onClick = onAction).padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            if (onDismiss != null) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.banner_dismiss),
                    tint = onContent,
                    modifier = Modifier.clickable(onClick = onDismiss).padding(4.dp),
                )
            }
        }
    }
}
