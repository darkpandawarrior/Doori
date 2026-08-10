package com.mileway.feature.cards.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.cards_card_holder
import com.mileway.core.ui.resources.cards_valid_thru
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewayRoles
import com.mileway.core.ui.theme.dataStyle
import com.mileway.feature.cards.model.CardModel
import org.jetbrains.compose.resources.stringResource

/**
 * Q+.2: the corporate card face. Mirrors the web styling: a squared-corner card with a 3.2dp
 * accent border, a gradient fill, masked PAN, holder, validity, brand, and a status badge. The
 * accent is the theme's primary — was a fixed indigo hex, so the card rendered identically under
 * every design direction.
 */
@Composable
fun CardFace(
    card: CardModel,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    // Contrast-driven, not a fixed white — the gradient runs from primaryContainer to accent, and
    // a pale accent on some directions would make literal white text unreadable. See
    // MilewayRoles.onFilled in theme/LAYERS.md.
    val onCard = MilewayRoles.onFilled(accent)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(1.586f)
                .clip(DesignTokens.Shape.roundedSm)
                .border(3.2.dp, accent, DesignTokens.Shape.roundedSm)
                .background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, accent)),
                )
                .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = card.cardType,
                color = onCard,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            CardStatusBadge(card.status)
        }

        Text(
            text = maskedNumber(card.cardNumber),
            color = onCard,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
            style = MaterialTheme.typography.titleMedium.dataStyle(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            LabelledValue(stringResource(Res.string.cards_card_holder), card.cardHolderName, onCard = onCard)
            LabelledValue(stringResource(Res.string.cards_valid_thru), card.validThru, isData = true, onCard = onCard)
            Text(
                text = card.scheme,
                color = onCard,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun LabelledValue(
    label: String,
    value: String,
    onCard: Color,
    isData: Boolean = false,
) {
    Column {
        Text(
            text = label,
            color = onCard.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = value,
            color = onCard,
            style = if (isData) MaterialTheme.typography.bodyMedium.dataStyle() else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
