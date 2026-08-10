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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewayRoles

/**
 * The six status tones, a generalisation of cards' `CardStatusBadge`, reused by every
 * status chip / history list across the app.
 *
 * [color] is a static, non-composable fallback — same shape as [DesignTokens.StatusColors], and
 * kept in lock-step with it on purpose, for the callers (several feature-module screens, plus
 * [com.mileway.core.ui.components.DistanceLedgerBar]'s [com.mileway.core.ui.components.Deduction])
 * that build a [StatusTone] outside composition and can't reach the theme. Inside a composable,
 * use [roleColor] instead — it resolves to the real Layer-2 role, so it follows the active design
 * direction; [color] renders identically under all ten. [StatusChip] itself already does this.
 */
enum class StatusTone(val color: Color) {
    // Values delegate to DesignTokens.StatusColors — the single declared home for this static
    // fallback — rather than repeating the hexes here.
    Success(DesignTokens.StatusColors.success),
    Warning(DesignTokens.StatusColors.warning),
    Error(DesignTokens.StatusColors.error),
    Info(DesignTokens.StatusColors.info),
    Neutral(DesignTokens.StatusColors.neutral),
    Danger(DesignTokens.StatusColors.error),
}

/** This tone's Layer-2 role colour — theme-aware, unlike the static [StatusTone.color]. */
@Composable
@ReadOnlyComposable
fun StatusTone.roleColor(): Color =
    when (this) {
        StatusTone.Success -> MilewayRoles.approved
        StatusTone.Warning -> MilewayRoles.pending
        StatusTone.Error -> MilewayRoles.rejected
        StatusTone.Info -> MilewayRoles.informational
        StatusTone.Neutral -> MilewayRoles.inactive
        StatusTone.Danger -> MilewayRoles.rejected
    }

/** A small tinted status pill: [tone]-coloured label on the one sanctioned tint fill. */
@Composable
fun StatusChip(
    label: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val role = tone.roleColor()
    Surface(
        modifier = modifier,
        color = MilewayRoles.tint(role),
        shape = DesignTokens.Shape.button,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = role,
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
