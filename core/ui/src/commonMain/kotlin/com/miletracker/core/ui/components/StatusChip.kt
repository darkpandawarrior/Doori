package com.miletracker.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The six the reference app status tones (F0.3) — a generalisation of cards' `CardStatusBadge`, reused by every V17
 * status chip / history list. Hex values match the the reference app design system.
 */
enum class StatusTone(val color: Color) {
    Success(Color(0xFF22C55E)),
    Warning(Color(0xFFF59E0B)),
    Error(Color(0xFFEF4444)),
    Info(Color(0xFF3B82F6)),
    Neutral(Color(0xFF6B7280)),
    Danger(Color(0xFFDC2626)),
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
        shape = RoundedCornerShape(6.dp),
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
 * Stepper progress bar for multi-step create wizards (F0.3) — a generalisation of `CardRequestScreen`'s
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
                shape = RoundedCornerShape(2.dp),
            ) {}
        }
    }
}
