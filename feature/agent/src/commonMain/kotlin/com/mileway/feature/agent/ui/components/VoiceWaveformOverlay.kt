package com.mileway.feature.agent.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.DesignTokens
import kotlin.math.sin

enum class WaveformState {
    Idle,
    Listening,
    Speaking,
    Processing,
}

@Composable
fun VoiceWaveformOverlay(
    state: WaveformState,
    rms: Float,
    transcript: String,
    modifier: Modifier = Modifier,
) {
    val barCount = 32
    val baseHeight = 4.dp
    val maxHeight = 48.dp

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(barCount) { index ->
                val targetFraction =
                    when (state) {
                        WaveformState.Idle -> 0.1f
                        WaveformState.Listening -> {
                            val norm = (rms / 10f).coerceIn(0f, 1f)
                            val wave = sin(index * 0.6 + rms * 0.3).toFloat() * 0.5f + 0.5f
                            (norm * wave).coerceIn(0.05f, 1f)
                        }
                        WaveformState.Speaking -> {
                            val wave = sin(index * 0.4 + rms * 0.5).toFloat() * 0.5f + 0.5f
                            (wave * 0.7f).coerceIn(0.1f, 0.7f)
                        }
                        WaveformState.Processing -> {
                            val pulse = sin(index * 0.3).toFloat() * 0.3f + 0.4f
                            pulse.coerceIn(0.1f, 0.7f)
                        }
                    }

                val animatedFraction by animateFloatAsState(
                    targetValue = targetFraction,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    label = "bar_$index",
                )

                val barHeight = baseHeight + (maxHeight - baseHeight) * animatedFraction
                val barColor =
                    when (state) {
                        WaveformState.Listening -> MaterialTheme.colorScheme.primary
                        WaveformState.Speaking -> MaterialTheme.colorScheme.secondary
                        WaveformState.Processing -> MaterialTheme.colorScheme.tertiary
                        WaveformState.Idle -> MaterialTheme.colorScheme.outlineVariant
                    }

                Box(
                    modifier =
                        Modifier
                            .width(3.dp)
                            .height(barHeight)
                            .clip(DesignTokens.Shape.button)
                            .background(barColor),
                )
            }
        }

        if (transcript.isNotBlank() && state == WaveformState.Listening) {
            Text(
                text = transcript,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(top = 56.dp)
                        .fillMaxWidth(),
            )
        }
    }
}
