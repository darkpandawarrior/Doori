package com.mileway.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.whats_new_button
import com.mileway.core.ui.resources.whats_new_indicator_desc
import org.jetbrains.compose.resources.stringResource

/*
 * PLAN_V24 P12.4 — the "What's new" entry indicators (extends the P2.2 changelog). Presentational
 * only: hasUnseen drives the pulse (a fresh release is waiting); tapping opens the changelog. The
 * pulse is an infinite transition, so a screenshot (frozen clock) renders a stable frame.
 */

/** Prominent animated variant for a home/hub entry: a tonal button with a pulsing "new" dot. */
@Composable
fun WhatsNewAnimatedButton(
    hasUnseen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val desc = stringResource(Res.string.whats_new_indicator_desc)
    FilledTonalButton(onClick = onClick, modifier = modifier.semantics { contentDescription = desc }) {
        Icon(Icons.Filled.NewReleases, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(
            stringResource(Res.string.whats_new_button),
            modifier = Modifier.padding(start = 8.dp),
            fontWeight = FontWeight.SemiBold,
        )
        if (hasUnseen) {
            PulsingDot(modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** Compact variant for a settings row: label + a pulsing dot when a release is unseen. */
@Composable
fun CompactWhatsNewButton(
    hasUnseen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Filled.NewReleases, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(stringResource(Res.string.whats_new_button), modifier = Modifier.padding(start = 6.dp))
        if (hasUnseen) {
            PulsingDot(modifier = Modifier.padding(start = 6.dp), size = 8.dp)
        }
    }
}

@Composable
private fun PulsingDot(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 10.dp,
    color: Color = MaterialTheme.colorScheme.error,
) {
    val transition = rememberInfiniteTransition(label = "whatsNewPulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "scale",
    )
    val fade by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(size).scale(scale).alpha(fade),
        ) {}
    }
}

/** The current changelog version (single source of truth, shared by Home + Settings). */
object WhatsNewVersion {
    const val CURRENT: Int = 24
}
