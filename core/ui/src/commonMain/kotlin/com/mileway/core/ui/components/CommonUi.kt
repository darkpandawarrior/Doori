@file:Suppress("ktlint:standard:function-naming")

package com.mileway.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.mvi.DefaultEmptyState
import com.mileway.core.ui.mvi.DefaultErrorState
import com.mileway.core.ui.mvi.ShimmerList
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_error_generic
import com.mileway.core.ui.resources.core_loading
import com.mileway.core.ui.theme.DesignTokens
import com.siddharth.kmp.common.UiText
import org.jetbrains.compose.resources.stringResource

/** Thin delegate to the designed skeleton; [message] is kept as the accessible label. */
@Composable
fun LoadingScreen(message: String = stringResource(Res.string.core_loading)) {
    Box(Modifier.fillMaxSize().semantics { contentDescription = message }) {
        ShimmerList()
    }
}

/** Thin delegate to [DefaultErrorState]; keeps the generic title above the specific [message]. */
@Composable
fun ErrorScreen(
    message: String,
    onRetry: (() -> Unit)? = null,
) {
    DefaultErrorState(
        message = UiText.of(message),
        onRetry = onRetry,
        title = stringResource(Res.string.core_error_generic),
    )
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.raised),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun TwoButtonRow(
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        MilewaySecondaryButton(text = secondaryText, onClick = onSecondary, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        MilewayPrimaryButton(text = primaryText, onClick = onPrimary, modifier = Modifier.weight(1f))
    }
}

/** Thin delegate to [DefaultEmptyState]; pass [ctaLabel] and [onCta] to give the empty screen an action. */
@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    ctaLabel: String? = null,
    onCta: (() -> Unit)? = null,
) {
    DefaultEmptyState(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        ctaLabel = ctaLabel,
        onCta = onCta,
    )
}
