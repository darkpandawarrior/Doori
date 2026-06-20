@file:Suppress("ktlint:standard:function-naming")

package com.miletracker.core.ui.mvi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.miletracker.core.common.UiText
import com.miletracker.core.common.asString
import com.miletracker.core.ui.components.UnifiedListShimmer

/**
 * Single switchboard for any data-loading screen. Replaces ad-hoc
 * `if (isLoading) Shimmer() else Content()` patterns with one [ScreenState] consumer so loading,
 * empty, no-network and error are handled consistently everywhere.
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
                    UiText.Static("You're offline. Check your connection and try again."),
                    onRetry,
                    icon = Icons.Outlined.CloudOff,
                )
            is ScreenState.Error -> error(state.message)
            is ScreenState.Content -> content(state.data)
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
    title: String = "Nothing here yet",
    subtitle: String? = null,
    ctaLabel: String? = null,
    onCta: (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (ctaLabel != null && onCta != null) {
            OutlinedButton(
                onClick = onCta,
                modifier = Modifier.padding(top = 20.dp),
            ) {
                Text(ctaLabel)
            }
        }
    }
}

@Composable
fun DefaultErrorState(
    message: UiText,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.ErrorOutline,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = message.asString(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        if (onRetry != null) {
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.padding(top = 20.dp),
            ) {
                Text("Try Again")
            }
        }
    }
}
