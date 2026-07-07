package com.mileway.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.action_continue
import com.mileway.core.ui.resources.action_not_now
import com.mileway.core.ui.resources.shared_welcome_disclaimer_body
import com.mileway.core.ui.resources.shared_welcome_disclaimer_title
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/** One disclosed permission row: icon, title, and the reason Mileway is asking for it. */
private data class PermissionDisclosure(
    val icon: ImageVector,
    val title: String,
    val reason: String,
)

private val DISCLOSURES =
    listOf(
        PermissionDisclosure(
            icon = Icons.Filled.LocationOn,
            title = "Location",
            reason = "to record your trip route",
        ),
        PermissionDisclosure(
            icon = Icons.Filled.Notifications,
            title = "Notifications",
            reason = "to show tracking status",
        ),
    )

/**
 * PLAN_V22 P7.5: shown once on [LoginScreen]'s first composition, before the reviewer signs in.
 * Mileway's core value prop (automatic trip tracking) needs `ACCESS_FINE_LOCATION` (and, on API
 * 33+, `POST_NOTIFICATIONS` for the tracking foreground-service status notification) — this sheet
 * discloses *why* before the system dialog appears, matching the reference app's welcome-disclaimer
 * pattern with Mileway's own [ModalBottomSheet] + [PermissionRow] design language.
 *
 * Non-blocking: dismissing ("Not now") proceeds without requesting anything; "Continue" launches
 * the real system permission dialog via [onRequestPermissions]. Either path calls [onDismiss] once
 * so the caller can resume any queued action (e.g. a tapped "Sign In").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeDisclaimerSheet(
    onRequestPermissions: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = DesignTokens.Spacing.xl)
                    .padding(bottom = DesignTokens.Spacing.xl, top = DesignTokens.Spacing.xs),
        ) {
            Text(
                text = stringResource(Res.string.shared_welcome_disclaimer_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            Text(
                text = stringResource(Res.string.shared_welcome_disclaimer_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.l))

            DISCLOSURES.forEach { disclosure ->
                PermissionRow(disclosure = disclosure)
                Spacer(Modifier.height(DesignTokens.Spacing.m))
            }

            Spacer(Modifier.height(DesignTokens.Spacing.s))

            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
            ) {
                Text(stringResource(Res.string.action_continue), fontWeight = FontWeight.SemiBold)
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.action_not_now))
            }
        }
    }
}

/** One permission disclosure row: tinted icon disc, title, and the reason copy. */
@Composable
private fun PermissionRow(disclosure: PermissionDisclosure) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = disclosure.icon,
                    contentDescription = null,
                    modifier = Modifier.size(DesignTokens.IconSize.actionTileLarge),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(Modifier.width(DesignTokens.Spacing.m))
        Column {
            Text(
                text = disclosure.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = disclosure.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
