package com.mileway.feature.tracking.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.SectionCard
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_cd_open
import com.mileway.feature.tracking.model.LinkedContext
import com.mileway.feature.tracking.model.LinkedContextKind
import org.jetbrains.compose.resources.stringResource

/**
 * Submission-context card (parity §2.1/§3 Wave 3): shows what a track is linked to, voucher,
 * trip, booking (itinerary), or petty-cash event, via [LinkedContext]. Renders nothing when
 * [context] is null, so callers can place it unconditionally.
 */
@Composable
fun LinkedContextCard(
    context: LinkedContext?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (context == null) return

    SectionCard(
        modifier = modifier.clickable(onClick = onClick),
        leadingIcon = context.kind.icon(),
        trailingAction = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(Res.string.tracking_cd_open),
                modifier = Modifier.size(20.dp),
            )
        },
    ) {
        Text(
            text = context.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = context.value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun LinkedContextKind.icon(): ImageVector =
    when (this) {
        is LinkedContextKind.Voucher -> Icons.Default.LocalOffer
        is LinkedContextKind.Trip -> Icons.Default.Map
        is LinkedContextKind.Booking -> Icons.Default.CalendarMonth
        is LinkedContextKind.Event -> Icons.Default.EventNote
    }
