package com.mileway.feature.cards.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.cards.model.CardStatus

/** Web's primary indigo accent (`#6367FA`) reused across the cards UI. */
internal val CardAccent = Color(0xFF6367FA)

/** Thousands-grouped money string (commonMain-safe; no java.text). */
internal fun formatMoney(
    amount: Double,
    currency: String,
): String {
    val whole = amount.toLong()
    val grouped =
        whole.toString()
            .reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()
    return "$currency $grouped"
}

/** Masked PAN from the stored last-4. */
internal fun maskedNumber(last4: String): String = "•••• •••• •••• $last4"

private fun statusColor(status: CardStatus): Color =
    when (status) {
        CardStatus.ACTIVE, CardStatus.PHYSICAL_ISSUED -> DesignTokens.StatusColors.success
        CardStatus.BLOCKED, CardStatus.EXPIRED -> DesignTokens.StatusColors.error
        CardStatus.FROZEN -> DesignTokens.StatusColors.info
        CardStatus.KYC_PENDING, CardStatus.PENDING -> DesignTokens.StatusColors.warning
    }

private fun statusLabel(status: CardStatus): String =
    when (status) {
        CardStatus.ACTIVE -> "Active"
        CardStatus.BLOCKED -> "Blocked"
        CardStatus.FROZEN -> "Frozen"
        CardStatus.KYC_PENDING -> "KYC Pending"
        CardStatus.PHYSICAL_ISSUED -> "Physical Issued"
        CardStatus.EXPIRED -> "Expired"
        CardStatus.PENDING -> "Pending"
    }

@Composable
internal fun CardStatusBadge(
    status: CardStatus,
    modifier: Modifier = Modifier,
) {
    val color = statusColor(status)
    Text(
        text = statusLabel(status),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier =
            modifier
                .clip(RoundedCornerShape(50))
                .background(color)
                .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}
