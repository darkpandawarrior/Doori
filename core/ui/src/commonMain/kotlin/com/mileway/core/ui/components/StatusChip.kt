package com.mileway.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.DesignTokens

/**
 * The six status tones, a generalisation of cards' `CardStatusBadge`, reused by every
 * status chip / history list across the app.
 */
enum class StatusTone(val color: Color) {
    // Aligned with Design Language v2 semantic tokens (MilewayColors dark-surface values).
    Success(Color(0xFF46C46B)),
    Warning(Color(0xFFF2C14E)),
    Error(Color(0xFFF2545B)),
    Info(Color(0xFF5BA8F5)),
    Neutral(Color(0xFF9AA5A0)),
    Danger(Color(0xFFF2545B)),
}

/** A small tinted status pill: [tone]-coloured label on a 15%-alpha fill. */
@Composable
fun StatusChip(
    label: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = tone.color.copy(alpha = 0.15f),
        shape = DesignTokens.Shape.button,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tone.color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/**
 * Stepper progress bar for multi-step create wizards (F0.3), a generalisation of `CardRequestScreen`'s
 * stepper. Renders [total] segments with the first [step] (1-based) filled in the primary colour.
 */
@Composable
fun WizardProgressBar(
    step: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { index ->
            val active = index < step
            val color by animateColorAsState(
                if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                label = "wizardSeg",
            )
            Surface(
                modifier = Modifier.weight(1f).height(4.dp),
                color = color,
                shape = DesignTokens.Shape.button,
            ) {}
        }
    }
}
