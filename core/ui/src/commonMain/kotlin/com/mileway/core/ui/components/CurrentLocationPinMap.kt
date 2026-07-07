package com.mileway.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.location_you_are_here
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * A geographic point to pin on the [CurrentLocationPinMap], with optional resolved labels.
 *
 * @property label short place label, e.g. "Koregaon Park, Pune".
 * @property coordinates formatted lat/lng line, e.g. "18.5204, 73.8567".
 */
data class LocationPin(
    val latitude: Double,
    val longitude: Double,
    val label: String? = null,
    val coordinates: String? = null,
)

/**
 * A [WorldMapBackdrop] with an interactive "you are here" pin at [pin]'s coordinate: a pulsing
 * radar ping that draws the eye, and — on tap — a springy callout showing the resolved place name
 * and coordinates. Pure Compose in commonMain, so it renders and behaves identically on Android
 * and iOS. [pin] == null draws the map with no pin (e.g. before location permission is granted).
 */
@Composable
fun CurrentLocationPinMap(
    modifier: Modifier = Modifier,
    pin: LocationPin?,
    dotColor: Color = Color.White,
    dotAlpha: Float = 0.20f,
    pinColor: Color = Color(0xFFFF5252),
) {
    BoxWithConstraints(modifier) {
        WorldMapBackdrop(
            modifier = Modifier.matchParentSize(),
            dotColor = dotColor,
            dotAlpha = dotAlpha,
            markerLatLng = null,
        )
        if (pin == null) return@BoxWithConstraints

        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val center = worldMapOffset(pin.latitude, pin.longitude, wPx, hPx)

        var open by remember(pin) { mutableStateOf(false) }

        // Pulsing radar ping + solid pin core (animated overlay).
        val transition = rememberInfiniteTransition(label = "radar")
        val pulse by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
            label = "pulse",
        )
        Canvas(Modifier.matchParentSize()) {
            val maxR = 22.dp.toPx()
            listOf(0f, 0.5f).forEach { phase ->
                val p = (pulse + phase) % 1f
                drawCircle(pinColor, radius = maxR * p, center = center, alpha = (1f - p) * 0.5f)
            }
            val coreR = 4.dp.toPx()
            drawCircle(pinColor, coreR * 1.9f, center, alpha = 0.30f) // glow
            drawCircle(pinColor, coreR, center) // core
            drawCircle(Color.White, coreR * 0.42f, center) // highlight
        }

        // Tap hotspot over the pin (invisible, no ripple).
        val hotspot = 44.dp
        Box(
            Modifier
                .offset {
                    val half = hotspot.toPx() / 2f
                    IntOffset((center.x - half).roundToInt(), (center.y - half).roundToInt())
                }
                .size(hotspot)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { open = !open },
        )

        // Springy "you are here" callout, anchored just below the pin and clamped on-screen.
        AnimatedVisibility(
            visible = open,
            enter =
                fadeIn() +
                    scaleIn(initialScale = 0.7f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
            exit = fadeOut() + scaleOut(targetScale = 0.7f),
            modifier =
                Modifier.offset {
                    val cw = 176.dp.toPx()
                    val x = (center.x - cw / 2f).coerceIn(0f, (wPx - cw).coerceAtLeast(0f))
                    val y = center.y + 14.dp.toPx()
                    IntOffset(x.roundToInt(), y.roundToInt())
                },
        ) {
            LocationCallout(pin = pin, accent = pinColor)
        }
    }
}

@Composable
private fun LocationCallout(
    pin: LocationPin,
    accent: Color,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(accent))
            Column {
                Text(
                    text = pin.label ?: stringResource(Res.string.location_you_are_here),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                pin.coordinates?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
