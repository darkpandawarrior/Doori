package com.mileway.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.SignalCellular0Bar
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalWifi0Bar
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.DesignTokens
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Represents the current network connectivity state.
 */
enum class NetworkConnectivityState {
    /** Device is connected to the internet */
    CONNECTED,

    /** Device is not connected to the internet */
    DISCONNECTED,

    /** Device is attempting to connect */
    CONNECTING,

    /** Connection state is unknown */
    UNKNOWN,
}

/**
 * Represents the type of network connection.
 */
enum class NetworkType {
    /** Connected via WiFi */
    WIFI,

    /** Connected via cellular/mobile data */
    CELLULAR,

    /** Connected via ethernet */
    ETHERNET,

    /** Connection type is unknown */
    UNKNOWN,
}

/**
 * Contains comprehensive information about the current network state.
 */
data class NetworkInfo(
    /** Current connectivity state */
    val state: NetworkConnectivityState,
    /** Type of network connection */
    val type: NetworkType = NetworkType.UNKNOWN,
    /** Whether the connection is metered (e.g., mobile data with data limits) */
    val isMetered: Boolean = false,
    /** Signal strength on a scale of 0-4 (0 = no signal, 4 = excellent) */
    val signalStrength: Int = 0,
)

/**
 * Creative animated network status indicator that shows connection state with visual effects.
 *
 * - Connected: network-type icon (or simple dot) with a gentle color pulse.
 * - Connecting: three dots orbiting over a faded background, faster pulse.
 * - Disconnected: close icon (or dot) shrunk to 0.8x scale.
 * - Airplane mode: flight icon on an error-colored circular background.
 *
 * @param label Optional text label rendered next to the indicator, tinted with the status color.
 */
@Composable
fun NetworkStatusIndicator(
    networkInfo: NetworkInfo,
    isAirplaneMode: Boolean = false,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    size: Dp = 24.dp,
    label: String? = null,
) {
    // State-based colors derived from theme
    val targetColor =
        when {
            isAirplaneMode -> MaterialTheme.colorScheme.error
            networkInfo.state == NetworkConnectivityState.CONNECTED -> MaterialTheme.colorScheme.primary
            networkInfo.state == NetworkConnectivityState.DISCONNECTED -> MaterialTheme.colorScheme.tertiary
            networkInfo.state == NetworkConnectivityState.CONNECTING -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    if (label == null) {
        NetworkStatusBadge(
            networkInfo = networkInfo,
            isAirplaneMode = isAirplaneMode,
            targetColor = targetColor,
            showIcon = showIcon,
            size = size,
            modifier = modifier,
        )
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NetworkStatusBadge(
                networkInfo = networkInfo,
                isAirplaneMode = isAirplaneMode,
                targetColor = targetColor,
                showIcon = showIcon,
                size = size,
            )
            Spacer(Modifier.width(DesignTokens.Spacing.s))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = targetColor,
            )
        }
    }
}

@Composable
private fun NetworkStatusBadge(
    networkInfo: NetworkInfo,
    isAirplaneMode: Boolean,
    targetColor: Color,
    showIcon: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "network_indicator")

    // Animated color transition
    val animatedColor by infiniteTransition.animateColor(
        initialValue = targetColor,
        targetValue = targetColor.copy(alpha = 0.7f),
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = if (networkInfo.state == NetworkConnectivityState.CONNECTING) 800 else 2000,
                        easing = FastOutSlowInEasing,
                    ),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "color_pulse",
    )

    // Scale animation for connection state changes
    val targetScale =
        when (networkInfo.state) {
            NetworkConnectivityState.CONNECTED -> 1f
            NetworkConnectivityState.CONNECTING -> 1.2f
            NetworkConnectivityState.DISCONNECTED -> 0.8f
            NetworkConnectivityState.UNKNOWN -> 0.9f
        }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "scale_animation",
    )

    // Connecting animation (rotating dots)
    val connectingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "connecting_rotation",
    )

    // Accessibility description for screen readers
    val statusDesc =
        when {
            isAirplaneMode -> "Airplane mode"
            else ->
                when (networkInfo.state) {
                    NetworkConnectivityState.CONNECTED -> "Connected"
                    NetworkConnectivityState.DISCONNECTED -> "Disconnected"
                    NetworkConnectivityState.CONNECTING -> "Connecting"
                    NetworkConnectivityState.UNKNOWN -> "Unknown"
                }
        }
    val typeDesc =
        when (networkInfo.type) {
            NetworkType.WIFI -> "WiFi"
            NetworkType.CELLULAR -> "Cellular"
            NetworkType.ETHERNET -> "Ethernet"
            NetworkType.UNKNOWN -> "Unknown network type"
        }

    Box(
        modifier =
            modifier
                .size(size)
                .semantics { stateDescription = "$statusDesc. $typeDesc" },
        contentAlignment = Alignment.Center,
    ) {
        when {
            isAirplaneMode -> {
                // Airplane mode connected-state visual: flight icon on prominent background
                Box(
                    modifier =
                        Modifier
                            .size(size)
                            .clip(CircleShape)
                            .background(animatedColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Flight,
                        contentDescription = "Airplane mode",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.6f),
                    )
                }
            }

            networkInfo.state == NetworkConnectivityState.CONNECTED -> {
                // Connected: Show network type icon with pulse effect
                if (showIcon) {
                    // Background circle to improve legibility of small icons
                    Box(
                        modifier =
                            Modifier
                                .size(size)
                                .scale(animatedScale)
                                .clip(CircleShape)
                                .background(animatedColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector =
                                getNetworkTypeIcon(
                                    networkInfo.type,
                                    networkInfo.signalStrength,
                                ),
                            contentDescription = "Network Connected",
                            tint = Color.White,
                            modifier =
                                Modifier
                                    .size(size * 0.6f),
                        )
                    }
                } else {
                    // Simple dot with pulse
                    Box(
                        modifier =
                            Modifier
                                .size(size * 0.4f)
                                .scale(animatedScale)
                                .clip(CircleShape)
                                .background(animatedColor),
                    )
                }
            }

            networkInfo.state == NetworkConnectivityState.CONNECTING -> {
                // Connecting: Animated rotating dots over faded background
                Box(
                    modifier =
                        Modifier
                            .size(size)
                            .scale(animatedScale)
                            .clip(CircleShape)
                            .background(animatedColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.size(size)) {
                        val center = Offset(size.toPx() / 2, size.toPx() / 2)
                        val radius = size.toPx() * 0.3f
                        val dotRadius = size.toPx() * 0.05f

                        repeat(3) { index ->
                            val angle = (connectingRotation + index * 120) * PI / 180
                            val x = center.x + radius * cos(angle).toFloat()
                            val y = center.y + radius * sin(angle).toFloat()

                            drawCircle(
                                color = animatedColor,
                                radius = dotRadius,
                                center = Offset(x, y),
                            )
                        }
                    }
                }
            }

            networkInfo.state == NetworkConnectivityState.DISCONNECTED -> {
                // Disconnected: Fading dot or X icon
                if (showIcon) {
                    // Clear centered close icon with colored circular background for better legibility
                    Box(
                        modifier =
                            Modifier
                                .size(size)
                                .scale(animatedScale)
                                .clip(CircleShape)
                                .background(animatedColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Network Disconnected",
                            tint = Color.White,
                            modifier = Modifier.size(size * 0.5f),
                        )
                    }
                } else {
                    Box(
                        modifier =
                            Modifier
                                .size(size * 0.4f)
                                .scale(animatedScale)
                                .clip(CircleShape)
                                .background(animatedColor),
                    )
                }
            }

            networkInfo.state == NetworkConnectivityState.UNKNOWN -> {
                // Unknown: Network icon instead of question mark
                if (showIcon) {
                    Box(
                        modifier =
                            Modifier
                                .size(size)
                                .scale(animatedScale)
                                .clip(CircleShape)
                                .background(animatedColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = "Network Status Unknown",
                            tint = Color.White,
                            modifier =
                                Modifier
                                    .size(size * 0.6f),
                        )
                    }
                } else {
                    Box(
                        modifier =
                            Modifier
                                .size(size * 0.4f)
                                .scale(animatedScale)
                                .clip(CircleShape)
                                .background(animatedColor),
                    )
                }
            }
        }
    }
}

/**
 * Get the appropriate network type icon based on type and signal strength.
 */
private fun getNetworkTypeIcon(
    type: NetworkType,
    signalStrength: Int,
): ImageVector {
    return when (type) {
        NetworkType.WIFI ->
            when (signalStrength) {
                0 -> Icons.Default.SignalWifi0Bar
                in 1..2 -> Icons.Default.Wifi // Medium strength
                else -> Icons.Default.SignalWifi4Bar // High strength
            }

        NetworkType.CELLULAR ->
            when (signalStrength) {
                0 -> Icons.Default.SignalCellular0Bar
                in 1..2 -> Icons.Default.SignalCellularAlt // Medium strength
                else -> Icons.Default.SignalCellular4Bar // High strength
            }

        NetworkType.ETHERNET -> Icons.Default.NetworkCheck
        NetworkType.UNKNOWN -> Icons.Default.NetworkCheck
    }
}

/**
 * Lightweight confetti burst overlay. Automatically hides after [durationMs].
 *
 * Particles fall with an eased (gravity-like) vertical drop, sway sinusoidally,
 * rotate (square pieces), and the whole burst fades out at the end. Particle
 * positions are deterministic for a given [seed] so the burst is reproducible.
 */
@Composable
fun ConfettiBurst(
    modifier: Modifier = Modifier,
    colorPalette: List<Color> =
        listOf(
            Color(0xFFFFC107),
            Color(0xFFFF5722),
            Color(0xFF8BC34A),
            Color(0xFF03A9F4),
            Color(0xFFE91E63),
        ),
    particleCount: Int = 45,
    durationMs: Long = 1600,
    seed: Int = 0x5EED,
) {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(durationMs)
        visible = false
    }

    AnimatedVisibility(visible = visible, exit = fadeOut(animationSpec = tween(300))) {
        Box(modifier = modifier.fillMaxSize()) {
            val infinite = rememberInfiniteTransition(label = "confetti")
            val anim by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMs.toInt(), easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "confetti_fall",
            )

            // Deterministic pseudo-random seeds so particles remain consistent during this burst
            val seeds =
                remember(seed, particleCount) {
                    val rng = Random(seed)
                    List(particleCount) { rng.nextFloat() to rng.nextFloat() }
                }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                seeds.forEachIndexed { idx, particleSeed ->
                    val (sx, sy) = particleSeed
                    val startX = sx * width
                    val phase = (sy * 0.5f)
                    val frac = (((anim + phase) % 1f) + 1f) % 1f
                    val y = frac * height
                    val x = startX + sin((anim + idx * 0.07f) * 6.28f) * 30f
                    val color = colorPalette[idx % colorPalette.size]
                    translate(left = x, top = y) {
                        // mix of rotating squares and circles
                        val sizePx = 6.dp.toPx()
                        if (idx % 3 == 0) {
                            rotate(
                                degrees = (anim + idx * 0.07f) * 360f,
                                pivot = Offset(sizePx / 2f, sizePx / 2f),
                            ) {
                                drawRect(
                                    color = color,
                                    size = Size(sizePx, sizePx),
                                )
                            }
                        } else {
                            drawCircle(color = color, radius = sizePx / 2)
                        }
                    }
                }
            }
        }
    }
}
