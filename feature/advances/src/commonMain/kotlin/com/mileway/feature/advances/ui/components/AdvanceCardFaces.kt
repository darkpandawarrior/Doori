package com.mileway.feature.advances.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.StatusChip
import com.mileway.core.ui.components.StatusTone
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.advances_balance_format
import com.mileway.core.ui.resources.advances_cd_scan_qr
import com.mileway.core.ui.resources.advances_health_active
import com.mileway.core.ui.resources.advances_health_critical
import com.mileway.core.ui.resources.advances_health_low_balance
import com.mileway.core.ui.resources.advances_percent_remaining
import com.mileway.core.ui.resources.advances_qr_available_balance_format
import com.mileway.core.ui.resources.advances_qr_valid_till
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.advances.model.CardHealth
import com.mileway.feature.advances.model.PettyCard
import com.mileway.feature.advances.model.QrCard
import com.mileway.feature.advances.model.cardHealth
import org.jetbrains.compose.resources.stringResource

/*
 * PLAN_V35.P4: petty-advance and QR card faces. Mileway primitives only — DesignTokens 12dp
 * squared shapes (never CircleShape/pills), StatusChip for the health badge.
 */

@Composable
private fun CardHealth.tone(): StatusTone =
    when (this) {
        CardHealth.ACTIVE -> StatusTone.Success
        CardHealth.LOW_BALANCE -> StatusTone.Warning
        CardHealth.CRITICAL -> StatusTone.Error
    }

@Composable
private fun CardHealth.label(): String =
    when (this) {
        CardHealth.ACTIVE -> stringResource(Res.string.advances_health_active)
        CardHealth.LOW_BALANCE -> stringResource(Res.string.advances_health_low_balance)
        CardHealth.CRITICAL -> stringResource(Res.string.advances_health_critical)
    }

/** Balance ratio in [0f, 1f]; a non-positive [total] reads as fully spent. */
private fun ratioOf(
    balance: Double,
    total: Double,
): Float = if (total > 0.0) (balance / total).toFloat().coerceIn(0f, 1f) else 0f

@Composable
internal fun HealthProgressBar(
    health: CardHealth,
    ratio: Float,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { ratio },
        modifier = modifier.fillMaxWidth().height(6.dp).clip(DesignTokens.Shape.button),
        color = health.tone().color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

@Composable
fun PettyAdvanceCardFace(
    card: PettyCard,
    modifier: Modifier = Modifier,
) {
    val health = cardHealth(card.balance, card.amount)
    val ratio = ratioOf(card.balance, card.amount)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(DesignTokens.Shape.roundedSm)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(DesignTokens.Spacing.l),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(card.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            StatusChip(label = health.label(), tone = health.tone())
        }
        Text(
            stringResource(Res.string.advances_balance_format, formatMoney(card.balance), formatMoney(card.amount)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HealthProgressBar(health, ratio)
        Text(
            stringResource(Res.string.advances_percent_remaining, (ratio * 100).toInt()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun QrCardFace(
    card: QrCard,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val health = cardHealth(card.balance, card.total)
    val ratio = ratioOf(card.balance, card.total)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(DesignTokens.Shape.roundedSm)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(DesignTokens.Spacing.l),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(card.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            StatusChip(label = health.label(), tone = health.tone())
        }
        Text(
            stringResource(Res.string.advances_qr_valid_till, formatDate(card.validUntilMs)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(Res.string.advances_qr_available_balance_format, formatMoney(card.balance), formatMoney(card.total)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            HealthProgressBar(health, ratio, modifier = Modifier.weight(1f))
            ScanShortcutButton(
                enabled = card.balance > 0.0,
                onClick = onScan,
                modifier = Modifier.padding(start = DesignTokens.Spacing.s),
            )
        }
    }
}

/** Squared (never circular) icon-button shortcut into the QR scanner; disabled at zero/negative balance. */
@Composable
private fun ScanShortcutButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val tint =
        if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    Box(
        modifier =
            modifier
                .size(DesignTokens.ActionTileSize.compactContainer)
                .clip(DesignTokens.Shape.button)
                .background(background)
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.QrCodeScanner,
            contentDescription = stringResource(Res.string.advances_cd_scan_qr),
            tint = tint,
        )
    }
}
