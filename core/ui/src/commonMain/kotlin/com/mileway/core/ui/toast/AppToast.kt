package com.mileway.core.ui.toast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.MilewayColors

/** Icon for each [ToastType]. */
private fun ToastType.icon(): ImageVector =
    when (this) {
        ToastType.Success -> Icons.Filled.CheckCircle
        ToastType.Error -> Icons.Filled.Error
        ToastType.Info -> Icons.Filled.Info
        ToastType.Warning -> Icons.Filled.Warning
    }

/** Theme-aware accent tint for each [ToastType]. */
@Composable
@ReadOnlyComposable
private fun ToastType.tint(): Color =
    when (this) {
        ToastType.Success -> MilewayColors.success
        ToastType.Error -> MilewayColors.danger
        ToastType.Info -> MilewayColors.info
        ToastType.Warning -> MilewayColors.warning
    }

/**
 * Rich toast card, icon + title + description + optional actions. Multiplatform (Android + iOS),
 * replaces the former koffee `AppToast`. Rendered by [AppToastHost].
 */
@Composable
fun AppToast(
    data: ToastData,
    onDismiss: () -> Unit,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = data.type.icon()
    val tint = data.type.tint()
    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(28.dp).padding(end = 12.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (data.description.isNotBlank()) {
                        Text(
                            text = data.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (data.primaryAction != null || data.secondaryAction != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    data.secondaryAction?.let {
                        TextButton(onClick = {
                            it.onClick()
                            onSecondary()
                        }) { Text(it.label) }
                    }
                    data.primaryAction?.let {
                        TextButton(onClick = {
                            it.onClick()
                            onPrimary()
                        }) { Text(it.label) }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Dismiss") }
                }
            }
        }
    }
}
