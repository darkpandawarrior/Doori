package com.miletracker.core.ui.components.tracking

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miletracker.core.ui.theme.DesignTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Public data / enum types (defined locally so the file compiles standalone)
// ---------------------------------------------------------------------------

/**
 * Which gauge the [HeroTrackingCard] is currently rendering.
 *
 * The card is stateless: the host owns the current [GaugeMode] and flips it via the
 * `onToggleMode` callback. Tapping the gauge or the Compass pill should advance to the
 * [next] mode.
 */
enum class GaugeMode {
    /** Circular bearing gauge with a rotating needle and a center speed readout. */
    COMPASS,

    /** Horizontal segmented breakdown of how time was split across activity types. */
    ACTIVITY,

    ;

    /** The mode that follows this one when the user toggles the gauge. */
    fun next(): GaugeMode =
        when (this) {
            COMPASS -> ACTIVITY
            ACTIVITY -> COMPASS
        }
}

/**
 * GPS / sensor signal quality, used to color the gauge's outer accent ring.
 *
 * - [GOOD] → success green
 * - [FAIR] → warning amber
 * - [POOR] → error red
 */
enum class GaugeSignal {
    GOOD,
    FAIR,
    POOR,
}

/** Resolve a [GaugeSignal] to its accent color from the shared status palette. */
private fun GaugeSignal.accentColor(): Color =
    when (this) {
        GaugeSignal.GOOD -> DesignTokens.StatusColors.success
        GaugeSignal.FAIR -> DesignTokens.StatusColors.warning
        GaugeSignal.POOR -> DesignTokens.StatusColors.error
    }

/** Activity classifications used by the [ActivityTimeline] gauge. */
enum class ActivityType {
    WALKING,
    CYCLING,
    DRIVING,
    IDLE,
}

/**
 * One proportional slice of the activity timeline.
 *
 * @param type the activity this segment represents.
 * @param fraction the share of the timeline this segment occupies, in `0f..1f`. Fractions
 *   across all segments are expected to sum to roughly `1f`; the timeline renders them
 *   proportionally regardless, so they need not be exact.
 */
data class ActivitySegment(
    val type: ActivityType,
    val fraction: Float,
)

/** Stable display color for an [ActivityType] (independent of theme for legibility). */
private fun ActivityType.color(): Color =
    when (this) {
        ActivityType.WALKING -> Color(0xFF43A047) // green
        ActivityType.CYCLING -> Color(0xFF1E88E5) // blue
        ActivityType.DRIVING -> Color(0xFFFFA726) // amber
        ActivityType.IDLE -> Color(0xFF90A4AE) // blue-grey
    }

/** Human-readable label for an [ActivityType]. */
private fun ActivityType.label(): String =
    when (this) {
        ActivityType.WALKING -> "Walking"
        ActivityType.CYCLING -> "Cycling"
        ActivityType.DRIVING -> "Driving"
        ActivityType.IDLE -> "Idle"
    }

// ---------------------------------------------------------------------------
// 1. CompassGauge, circular bearing gauge with center speed readout
// ---------------------------------------------------------------------------

/**
 * A circular compass gauge drawn on a single [Canvas].
 *
 * Renders, from outside in:
 * - a colored signal-quality accent ring (mapped from [signalQuality]),
 * - a degree ring with 12 minor ticks and four major cardinal ticks,
 * - N / E / S / W cardinal labels,
 * - a bearing needle that rotates to [bearingDegrees] (animated with a spring), and
 * - a center speed readout: [speedKmh] large with a small `km/h` unit beneath.
 *
 * Ported faithfully from the source compass: same radius fractions for ticks, needle and
 * labels, and the same low-stiffness settle spring. The cardinal letters are drawn with the
 * multiplatform [TextMeasurer] API rather than a platform-native canvas so the component is
 * Compose-Multiplatform-pure.
 *
 * Stateless: pass [bearingDegrees], [speedKmh] and [signalQuality]; no internal model.
 *
 * @param bearingDegrees current heading in degrees (0 = North, clockwise). Animated.
 * @param speedKmh current speed in km/h, or `null` to show the `--` placeholder.
 * @param signalQuality drives the outer accent ring color.
 * @param isActive when true, the needle settles a touch more crisply (tracking is live).
 * @param diameter the overall gauge size; the source hero used 64dp inline and this hero
 *   renders it at ~200dp as the focal element.
 */
@Composable
fun CompassGauge(
    bearingDegrees: Float,
    speedKmh: Float?,
    signalQuality: GaugeSignal,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    diameter: Dp = 132.dp,
) {
    val animatedHeading by animateFloatAsState(
        targetValue = bearingDegrees,
        animationSpec =
            spring(
                dampingRatio = if (isActive) 0.7f else 0.8f,
                stiffness = Spring.StiffnessLow,
            ),
        label = "compassHeading",
    )

    val accentColor = signalQuality.accentColor()
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val errorColor = MaterialTheme.colorScheme.error
    val labelColor = onSurfaceColor.copy(alpha = 0.80f)

    val textMeasurer = rememberTextMeasurer()

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(diameter)) {
        Canvas(modifier = Modifier.size(diameter)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerRadius = min(size.width, size.height) / 2f
            val accentStroke = outerRadius * 0.05f
            val radius = outerRadius - accentStroke

            drawCircle(
                color = accentColor.copy(alpha = 0.85f),
                radius = outerRadius - accentStroke / 2f,
                center = Offset(cx, cy),
                style = Stroke(width = accentStroke),
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.05f),
                radius = radius,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = onSurfaceColor.copy(alpha = 0.10f),
                radius = radius,
                style = Stroke(width = radius * 0.012f),
            )
            drawCircle(
                color = onSurfaceColor.copy(alpha = 0.06f),
                radius = radius * 0.78f,
                style = Stroke(width = radius * 0.009f),
            )

            val majorTickLength = radius * 0.12f
            listOf(0f, 90f, 180f, 270f).forEach { angle ->
                val rad = angle.toRadians()
                drawLine(
                    color = onSurfaceColor.copy(alpha = 0.18f),
                    start = Offset(cx + (radius - majorTickLength) * cos(rad), cy + (radius - majorTickLength) * sin(rad)),
                    end = Offset(cx + radius * cos(rad), cy + radius * sin(rad)),
                    strokeWidth = radius * 0.014f,
                )
            }
            val minorTickLength = radius * 0.06f
            for (idx in 0 until 12) {
                val rad = (idx * 30f).toRadians()
                drawLine(
                    color = onSurfaceColor.copy(alpha = 0.10f),
                    start = Offset(cx + (radius - minorTickLength) * cos(rad), cy + (radius - minorTickLength) * sin(rad)),
                    end = Offset(cx + radius * cos(rad), cy + radius * sin(rad)),
                    strokeWidth = radius * 0.009f,
                )
            }

            drawCardinalLabel(textMeasurer, "N", cx, cy - radius * 0.72f, radius, labelColor)
            drawCardinalLabel(textMeasurer, "E", cx + radius * 0.72f, cy - radius * 0.06f, radius, labelColor)
            drawCardinalLabel(textMeasurer, "S", cx, cy + radius * 0.50f, radius, labelColor)
            drawCardinalLabel(textMeasurer, "W", cx - radius * 0.72f, cy - radius * 0.06f, radius, labelColor)

            val theta = animatedHeading.toRadians()
            val sinT = sin(theta)
            val cosT = cos(theta)

            fun rotate(
                px: Float,
                py: Float,
            ): Offset {
                val dx = px - cx
                val dy = py - cy
                return Offset(cx + dx * cosT - dy * sinT, cy + dx * sinT + dy * cosT)
            }
            val halfWidth = radius * 0.08f
            val tip = rotate(cx, cy - radius * 0.62f)
            val left = rotate(cx - halfWidth, cy + radius * 0.28f)
            val rightPt = rotate(cx + halfWidth, cy + radius * 0.28f)
            val needle =
                Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(left.x, left.y)
                    lineTo(rightPt.x, rightPt.y)
                    close()
                }
            drawPath(needle, color = primaryColor, style = Fill)
            drawPath(needle, color = onSurfaceColor.copy(alpha = 0.14f), style = Stroke(width = radius * 0.008f))
            drawCircle(color = errorColor, radius = max(2f, radius * 0.06f), center = tip)
        }
    }
}

/** Draw a single centered cardinal letter at [x], [y] using the multiplatform text API. */
private fun DrawScope.drawCardinalLabel(
    measurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    radius: Float,
    color: Color,
) {
    val style =
        TextStyle(
            color = color,
            fontSize = (radius * 0.24f / density).sp,
            fontWeight = FontWeight.Bold,
        )
    val layout = measurer.measure(text, style)
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(x - layout.size.width / 2f, y - layout.size.height / 2f),
    )
}

/** Degrees to radians without any `java.*` dependency (KMP-pure). */
private fun Float.toRadians(): Float = (this * PI / 180.0).toFloat()

// ---------------------------------------------------------------------------
// 2. ActivityTimeline, segmented horizontal activity breakdown
// ---------------------------------------------------------------------------

/**
 * The alternate gauge mode: a horizontal segmented timeline of activity types.
 *
 * Each [ActivitySegment] becomes a proportionally-weighted colored bar (animated as its
 * fraction changes), followed by a legend of the segments shown. Ported from the source
 * activity timeline: pill-shaped track, 2dp inter-segment gaps, animated weights, and a
 * compact legend below.
 *
 * Stateless: pass [segments]; an empty list renders an "analyzing" placeholder.
 *
 * @param segments proportional slices; fractions need not sum to exactly 1f.
 * @param currentActivityLabel optional emphasized label for the live activity (right-aligned
 *   in the header). Pass `null` to hide it.
 * @param maxLegendItems how many legend entries to show (source showed the top three).
 */
@Composable
fun ActivityTimeline(
    segments: List<ActivitySegment>,
    modifier: Modifier = Modifier,
    currentActivityLabel: String? = null,
    maxLegendItems: Int = 3,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Activity",
                style = MaterialTheme.typography.labelMedium,
                color = onSurface.copy(alpha = 0.6f),
            )
            if (currentActivityLabel != null) {
                Text(
                    text = currentActivityLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (segments.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(trackColor.copy(alpha = 0.4f), RoundedCornerShape(50)),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Analyzing activity…",
                style = MaterialTheme.typography.bodySmall,
                color = onSurface.copy(alpha = 0.6f),
            )
            return@Column
        }

        // Proportional segmented track.
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(trackColor.copy(alpha = 0.25f), RoundedCornerShape(50))
                    .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            segments.forEach { segment ->
                val animatedWeight by animateFloatAsState(
                    targetValue = segment.fraction.coerceAtLeast(0.01f),
                    animationSpec = tween(durationMillis = 600),
                    label = "activitySegment",
                )
                Box(
                    modifier =
                        Modifier
                            .weight(animatedWeight)
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(segment.type.color(), RoundedCornerShape(50)),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Legend.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            segments.take(maxLegendItems).forEach { segment ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .size(6.dp)
                                .background(segment.type.color(), RoundedCornerShape(50)),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${segment.type.label()} ${(segment.fraction * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurface.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. HeroTrackingCard, the gradient card hosting the gauge
// ---------------------------------------------------------------------------

/**
 * The hero tracking card that hosts the gauge.
 *
 * A gradient/surface card (using [DesignTokens.topBarGradientBrush] like the rest of the
 * demo hero surfaces) containing:
 * - the gauge, [CompassGauge] or [ActivityTimeline] depending on [gaugeMode]; tapping the
 *   gauge area toggles between them via [onToggleMode],
 * - an optional vehicle glyph slot ([vehicleIcon]),
 * - a Distance / Duration / Vehicle metric row, and
 * - a small history-count chip plus a "Compass / Activity" pill that also toggles the mode.
 *
 * Faithfully ports the source hero layout (gauge + speed on the left, vehicle glyph on the
 * right, a divider, then a metrics row) but kept fully stateless: every value arrives via a
 * parameter and every interaction leaves via a callback. There is no ViewModel, navigation,
 * or platform dependency inside this file, the integrator wires those.
 *
 * Matches the two reference states:
 * - idle: compass, `-- km/h`, `-- min` duration, `--` vehicle;
 * - active: `0 km/h`, "Stationary" activity, "4 Wheeler" vehicle.
 *
 * @param distanceText formatted distance, e.g. "0.0 km".
 * @param durationText formatted duration, e.g. "00:00:57" or "-- min".
 * @param vehicleName vehicle label, or `null` to show `--`.
 * @param bearingDegrees heading for the compass needle.
 * @param speedKmh current speed, or `null` for the `--` placeholder.
 * @param signalQuality drives the compass accent ring color.
 * @param segments timeline slices for the activity gauge.
 * @param gaugeMode which gauge is currently shown.
 * @param onToggleMode invoked when the user taps the gauge or the mode pill.
 * @param isActive true when tracking is live (enables the subtle breathing animation).
 * @param isPaused true when tracking is paused; surfaces [pauseReason] if provided.
 * @param historyCount number shown on the small history chip.
 * @param trackingActivity emphasized activity label shown while active (e.g. "Stationary").
 * @param vehicleIcon optional vehicle glyph slot; defaults to a car icon when `null`.
 * @param onVehicleClick optional tap handler for the vehicle glyph (e.g. open vehicle picker).
 * @param historyIcon icon for the history chip.
 * @param compassPillIcon icon shown inside the mode pill.
 */
@Composable
fun HeroTrackingCard(
    distanceText: String,
    durationText: String,
    vehicleName: String?,
    bearingDegrees: Float,
    speedKmh: Float?,
    signalQuality: GaugeSignal,
    segments: List<ActivitySegment>,
    gaugeMode: GaugeMode,
    onToggleMode: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPaused: Boolean = false,
    historyCount: Int = 0,
    trackingActivity: String = "",
    vehicleIcon: (@Composable () -> Unit)? = null,
    onVehicleClick: (() -> Unit)? = null,
    pauseReason: String? = null,
    historyIcon: ImageVector = Icons.Filled.History,
    compassPillIcon: ImageVector = Icons.Filled.Explore,
    shapeRadius: Dp = 20.dp,
) {
    val shape = RoundedCornerShape(shapeRadius)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val heroTopColor = if (isActive) Color(0xFFA9DCAE) else Color(0xFFD3EAD6)
    val gradient =
        Brush.verticalGradient(
            colors = listOf(heroTopColor, heroTopColor.copy(alpha = 0.45f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        )

    val breath =
        if (isActive) {
            val infinite = rememberInfiniteTransition(label = "heroBreath")
            infinite.animateFloat(
                initialValue = 0.99f,
                targetValue = 1.01f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 1400, easing = EaseOutCubic),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "heroBreathAnim",
            ).value
        } else {
            1f
        }

    // Flat card, no elevation shadow, gradient provides all the depth.
    Card(
        modifier =
            modifier.clip(shape).graphicsLayer {
                scaleX = breath
                scaleY = breath
            },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().clip(shape).background(gradient)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            ) {
                // ── Gauge + speed column ────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                ) {
                    // Left: compass or activity timeline (tap to toggle)
                    Box(
                        modifier = Modifier.clickable(onClickLabel = "Switch gauge mode") { onToggleMode() },
                        contentAlignment = Alignment.Center,
                    ) {
                        when (gaugeMode) {
                            GaugeMode.COMPASS ->
                                CompassGauge(
                                    bearingDegrees = bearingDegrees,
                                    speedKmh = null,
                                    signalQuality = signalQuality,
                                    isActive = isActive,
                                    diameter = 132.dp,
                                )
                            GaugeMode.ACTIVITY ->
                                ActivityTimeline(
                                    segments = segments,
                                    currentActivityLabel = trackingActivity.takeIf { it.isNotBlank() },
                                    modifier = Modifier.width(132.dp),
                                )
                        }
                    }

                    // Right: speed readout + activity label + vehicle
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        // Prominent speed number
                        val speedText = speedKmh?.let { round(it.coerceAtLeast(0f)).roundToInt().toString() } ?: "--"
                        val speedColor = if (speedText == "--") onSurface.copy(alpha = 0.45f) else onSurface
                        AnimatedContent(
                            targetState = speedText,
                            transitionSpec = {
                                (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                            },
                            label = "heroSpeed",
                        ) { value ->
                            Text(
                                text = value,
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                color = speedColor,
                                maxLines = 1,
                            )
                        }
                        Text(
                            text = "km/h",
                            style = MaterialTheme.typography.labelMedium,
                            color = onSurface.copy(alpha = 0.55f),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Vehicle glyph
                        Box(
                            modifier =
                                if (onVehicleClick != null) {
                                    Modifier.clickable(onClickLabel = "Select vehicle") { onVehicleClick() }
                                } else {
                                    Modifier
                                },
                        ) {
                            vehicleIcon?.invoke() ?: Icon(
                                imageVector = Icons.Filled.DirectionsCar,
                                contentDescription = vehicleName?.let { "Vehicle: $it" } ?: "Select vehicle",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                }

                // ── Activity chips + gauge toggle ──────────────────────────────
                ActivityChipsRow(
                    currentActivity = trackingActivity,
                    gaugeMode = gaugeMode,
                    isActive = isActive,
                    historyCount = historyCount,
                    historyIcon = historyIcon,
                    compassPillIcon = compassPillIcon,
                    onToggleMode = onToggleMode,
                )

                // ── Metrics row ────────────────────────────────────────────────
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    HeroMetric("Distance", distanceText, Alignment.Start, Modifier.weight(1f))
                    HeroMetric("Duration", durationText, Alignment.CenterHorizontally, Modifier.weight(1f))
                    HeroMetric("Vehicle", vehicleName ?: "--", Alignment.End, Modifier.weight(1.1f))
                }

                // ── Paused banner ──────────────────────────────────────────────
                if (isPaused && !pauseReason.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PauseCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Column {
                                Text(
                                    "Paused",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                )
                                Text(
                                    pauseReason,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A compact row with a scrollable activity-chip strip on the left and a fixed gauge-toggle
 * pill on the right. Activity chips are icon+label (selected) or icon-only (unselected) to
 * stay compact even on narrow screens.
 */
@Composable
private fun ActivityChipsRow(
    currentActivity: String,
    gaugeMode: GaugeMode,
    isActive: Boolean,
    historyCount: Int,
    historyIcon: ImageVector,
    compassPillIcon: ImageVector,
    onToggleMode: () -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Scrollable strip: history chip + activity chips
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item { HistoryCountChip(count = historyCount, icon = historyIcon) }

            if (isActive) {
                val normalizedActivity = currentActivity.lowercase()
                val activityChips =
                    listOf(
                        Triple(ActivityType.WALKING, Icons.AutoMirrored.Filled.DirectionsWalk, "Walking"),
                        Triple(ActivityType.DRIVING, Icons.Filled.DirectionsCar, "Driving"),
                        Triple(ActivityType.CYCLING, Icons.AutoMirrored.Filled.DirectionsBike, "Cycling"),
                    )
                items(activityChips) { (type, icon, label) ->
                    val isSelected =
                        when (type) {
                            ActivityType.WALKING -> normalizedActivity.contains("walk")
                            ActivityType.DRIVING ->
                                normalizedActivity.contains("driv") ||
                                    normalizedActivity.contains("vehicle") ||
                                    normalizedActivity.contains("auto")
                            ActivityType.CYCLING ->
                                normalizedActivity.contains("cycl") ||
                                    normalizedActivity.contains("bike")
                            else -> false
                        }
                    Surface(
                        color =
                            if (isSelected) {
                                type.color().copy(alpha = 0.18f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                        shape = RoundedCornerShape(50),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(12.dp),
                                tint = if (isSelected) type.color() else onSurface.copy(alpha = 0.5f),
                            )
                            Text(
                                text = label,
                                style =
                                    MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    ),
                                color = if (isSelected) type.color() else onSurface.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))
        // Fixed-width pill always fully visible
        GaugeModePill(gaugeMode = gaugeMode, icon = compassPillIcon, onClick = onToggleMode)
    }
}

/** One labeled metric in the hero card's bottom row. */
@Composable
private fun HeroMetric(
    label: String,
    value: String,
    alignment: Alignment.Horizontal,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = onSurface.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Small pill chip showing the saved-trip history count. */
@Composable
private fun HistoryCountChip(
    count: Int,
    icon: ImageVector,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(50),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Compact inline gauge-mode pill: `[icon]  [mode name]  ↻`.
 *
 * A pulsing primary border aids discoverability without changing the chip's size, ported
 * from the source toggle chip (same 950ms ease-out-cubic reverse pulse).
 */
@Composable
private fun GaugeModePill(
    gaugeMode: GaugeMode,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val label =
        when (gaugeMode) {
            GaugeMode.COMPASS -> "Compass"
            GaugeMode.ACTIVITY -> "Activity"
        }
    val leadingIcon =
        when (gaugeMode) {
            GaugeMode.COMPASS -> icon
            GaugeMode.ACTIVITY -> Icons.Filled.Timeline
        }

    val infinite = rememberInfiniteTransition(label = "pillPulse")
    val borderAlpha by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.8f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 950, easing = EaseOutCubic),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pillBorderAlpha",
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha)),
        modifier = Modifier.clickable(onClickLabel = "Switch gauge mode") { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "↻",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// Keep the directional activity glyphs referenced so they are available to integrators
// wiring custom vehicle slots without importing them separately. Not rendered here.
@Suppress("unused")
private val activityGlyphReferences =
    listOf(
        Icons.AutoMirrored.Filled.DirectionsWalk,
        Icons.AutoMirrored.Filled.DirectionsBike,
        Icons.Filled.DirectionsCar,
    )
