package com.mileway.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.DesignTokens
import kotlin.math.roundToInt

/**
 * A confidence score in `0f..1f`, or [Unknown] when it was never measured.
 *
 * Modelled as its own type rather than a nullable `Float` so "unknown" can never be silently
 * mistaken for "measured and zero" at a call site — that distinction is the entire reason
 * [ConfidenceMeter] exists rather than just a progress bar.
 */
sealed interface Confidence {
    data class Known(val value: Float) : Confidence {
        init {
            require(value in 0f..1f) { "confidence must be in 0f..1f, was $value" }
        }
    }

    data object Unknown : Confidence
}

/** Pure state derivation from [Confidence] — kept apart from the composable so it is unit-testable. */
internal data class ConfidenceMeterState(
    val fraction: Float?,
    val label: String,
    val tone: StatusTone,
)

internal fun Confidence.toMeterState(): ConfidenceMeterState =
    when (this) {
        is Confidence.Unknown -> ConfidenceMeterState(fraction = null, label = "Not measured", tone = StatusTone.Neutral)
        is Confidence.Known ->
            ConfidenceMeterState(
                fraction = value,
                label = "${(value * 100).roundToInt()}%",
                tone =
                    when {
                        value >= 0.75f -> StatusTone.Success
                        value >= 0.4f -> StatusTone.Warning
                        else -> StatusTone.Error
                    },
            )
    }

/**
 * Renders a `0..1` [confidence] as a short tone-coloured label plus a bar.
 *
 * [Confidence.Unknown] renders as "Not measured" with a neutral tone and an empty bar — never as
 * 0% (a real low measurement) and never as 100% (a real high one). Collapsing "we didn't measure
 * this" into either extreme would misrepresent the data as more certain than it actually is, in
 * whichever direction happens to be convenient for the caller.
 *
 * ponytail: a Canvas bar, no chart library — same call as [DistanceLedgerBar] and for the same
 * reason: a receipt-style figure doesn't need one, and per [DesignTokens.Motion]'s rule it must
 * not animate while it's being read, so there's no animation state to justify one either.
 */
@Composable
fun ConfidenceMeter(
    confidence: Confidence,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val state = confidence.toMeterState()
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { contentDescription = listOfNotNull(label, state.label).joinToString(": ") },
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            label?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                state.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = state.tone.color,
            )
        }
        ConfidenceTrack(fraction = state.fraction, color = state.tone.color)
    }
}

@Composable
private fun ConfidenceTrack(
    fraction: Float?,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
    ) {
        drawRect(color = track, size = size)
        // fraction == null means unknown: draw no fill at all, never a 0-width bar dressed up as
        // a real measurement.
        if (fraction != null) {
            val w = size.width * fraction.coerceIn(0f, 1f)
            drawRect(color = color, size = Size(w, size.height), topLeft = Offset.Zero)
        }
    }
}
