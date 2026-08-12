@file:Suppress("ktlint:standard:function-naming")

package com.mileway.core.ui.mvi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.UnifiedListShimmer
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_cd_error
import com.mileway.core.ui.resources.core_empty_default
import com.mileway.core.ui.resources.core_offline_message
import com.mileway.core.ui.resources.core_offline_title
import com.mileway.core.ui.resources.core_permission_action_grant
import com.mileway.core.ui.resources.core_permission_action_open_settings
import com.mileway.core.ui.resources.core_permission_denied_permanently_hint
import com.mileway.core.ui.resources.core_permission_required_title
import com.mileway.core.ui.resources.core_try_again
import com.mileway.core.ui.text.text
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewayColors
import com.siddharth.kmp.common.UiText
import org.jetbrains.compose.resources.stringResource

/**
 * Single switchboard for any data-loading screen. Replaces ad-hoc
 * `if (isLoading) Shimmer() else Content()` patterns with one [ScreenState] consumer so loading,
 * empty, no-network, error, offline-stale and partial-failure all render consistently everywhere
 * that plugs into [ScreenState] — no per-screen re-implementation needed.
 */
@Composable
fun <T> ScreenStateContent(
    state: ScreenState<T>,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    loading: @Composable () -> Unit = { DefaultLoadingState() },
    empty: @Composable () -> Unit = { DefaultEmptyState() },
    error: @Composable (UiText) -> Unit = { DefaultErrorState(it, onRetry) },
    content: @Composable (T) -> Unit,
) {
    Box(modifier = modifier) {
        when (state) {
            is ScreenState.Loading -> loading()
            is ScreenState.Empty -> empty()
            is ScreenState.NoNetwork ->
                DefaultErrorState(
                    UiText.Res(Res.string.core_offline_message.key),
                    onRetry,
                    icon = Icons.Outlined.CloudOff,
                    title = stringResource(Res.string.core_offline_title),
                )
            is ScreenState.Error -> error(state.message)
            is ScreenState.Content ->
                Column(modifier = Modifier.fillMaxSize()) {
                    // Refresh-while-showing-stale-data: cache is on screen, a live refresh is in
                    // flight. Distinct from initial [ScreenState.Loading] — never blanks the screen.
                    if (state.isStale) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    // PARTIAL: some data loaded, some failed. An inline banner over the loaded
                    // content — not a full-screen error, which would hide the data that did load.
                    if (state.partialError != null) {
                        PartialErrorBanner(message = state.partialError, onRetry = onRetry)
                    }
                    Box(modifier = Modifier.weight(1f)) { content(state.data) }
                }
        }
    }
}

@Composable
fun DefaultLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/** Skeleton list for list-shaped screens; thin wrapper over the shared list shimmer. */
@Composable
fun ShimmerList(
    itemCount: Int = 5,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        UnifiedListShimmer(itemCount = itemCount)
    }
}

@Composable
fun DefaultEmptyState(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Inbox,
    title: String = stringResource(Res.string.core_empty_default),
    subtitle: String? = null,
    ctaLabel: String? = null,
    onCta: (() -> Unit)? = null,
) {
    StateMessageLayout(
        modifier = modifier,
        icon = icon,
        iconContentDescription = null,
        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
        iconContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        title = title,
        subtitle = subtitle,
        primaryLabel = ctaLabel,
        onPrimary = onCta,
    )
}

@Composable
fun DefaultErrorState(
    message: UiText,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.ErrorOutline,
    title: String? = null,
) {
    StateMessageLayout(
        modifier = modifier,
        icon = icon,
        iconContentDescription = stringResource(Res.string.core_cd_error),
        iconTint = MaterialTheme.colorScheme.error,
        iconContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        title = title,
        subtitle = message.text(),
        titleStyle = if (title == null) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
        primaryLabel = if (onRetry != null) stringResource(Res.string.core_try_again) else null,
        onPrimary = onRetry,
    )
}

/**
 * PERMISSION state: a screen whose feature depends on a runtime permission the user hasn't
 * granted. [isPermanentlyDenied] swaps the action from "ask again" to "open settings" (Android
 * won't show the system prompt a second time once the user has denied-and-checked "don't ask
 * again"), and surfaces the hint explaining why the button changed.
 */
@Composable
fun PermissionRequiredState(
    message: UiText,
    isPermanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Lock,
    title: String = stringResource(Res.string.core_permission_required_title),
) {
    StateMessageLayout(
        modifier = modifier,
        icon = icon,
        iconContentDescription = title,
        iconTint = MaterialTheme.colorScheme.error,
        iconContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        title = title,
        subtitle =
            if (isPermanentlyDenied) {
                message.text() + " " + stringResource(Res.string.core_permission_denied_permanently_hint)
            } else {
                message.text()
            },
        primaryLabel =
            if (isPermanentlyDenied) {
                stringResource(Res.string.core_permission_action_open_settings)
            } else {
                stringResource(Res.string.core_permission_action_grant)
            },
        onPrimary = if (isPermanentlyDenied) onOpenSettings else onRequestPermission,
    )
}

/**
 * Shared centered icon+title+subtitle+CTA layout behind [DefaultEmptyState], [DefaultErrorState]
 * and [PermissionRequiredState] — one place to keep spacing on [DesignTokens] and typography
 * consistent instead of three near-identical composables drifting apart.
 */
@Composable
private fun StateMessageLayout(
    icon: ImageVector,
    iconContentDescription: String?,
    iconTint: Color,
    iconContainerColor: Color,
    title: String?,
    subtitle: String?,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(DesignTokens.Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .background(iconContainerColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconContentDescription,
                tint = iconTint,
                modifier = Modifier.size(32.dp),
            )
        }
        if (title != null) {
            Text(
                text = title,
                style = titleStyle,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = DesignTokens.Spacing.l),
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier.padding(
                        // Icon-to-message gap when there's no title above; title-to-subtitle gap
                        // (tighter) when there is.
                        top = if (title != null) DesignTokens.Spacing.s else DesignTokens.Spacing.l,
                    ),
            )
        }
        if (primaryLabel != null && onPrimary != null) {
            OutlinedButton(
                onClick = onPrimary,
                modifier = Modifier.padding(top = DesignTokens.Spacing.xl),
            ) {
                Text(primaryLabel)
            }
        }
    }
}

/** Inline retry banner for [ScreenState.Content.partialError] — sits above the loaded content. */
@Composable
private fun PartialErrorBanner(
    message: UiText,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MilewayColors.warning.copy(alpha = 0.12f))
                .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = MilewayColors.warning,
            modifier = Modifier.size(DesignTokens.IconSize.badge),
        )
        Spacer(Modifier.width(DesignTokens.Spacing.s))
        Text(
            text = message.text(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (onRetry != null) {
            Spacer(Modifier.width(DesignTokens.Spacing.s))
            TextButton(onClick = onRetry) {
                Text(stringResource(Res.string.core_try_again), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
