@file:Suppress("ktlint:standard:function-naming")

package com.miletracker.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Subtle horizontal CRT-style scanline overlay.
 *
 * Place this as the topmost layer in a Box that wraps screen content:
 * ```
 * Box { Content(); ScanlineOverlay() }
 * ```
 * The default alpha is intentionally low so it reads as texture, not obstruction.
 */
@Composable
fun ScanlineOverlay(
    modifier: Modifier = Modifier,
    lineAlpha: Float = 0.035f,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawScanlines(lineAlpha)
    }
}

private fun DrawScope.drawScanlines(alpha: Float) {
    val lineH = 1.5f
    val step = 3.5f
    var y = 0f
    while (y < size.height) {
        drawRect(
            color = Color.Black.copy(alpha = alpha),
            topLeft = Offset(0f, y),
            size = Size(size.width, lineH),
        )
        y += step
    }
}

/**
 * Radial CRT vignette — darkens screen edges for a phosphor-monitor feel.
 * Overlay this on top of any full-screen terminal surface.
 */
@Composable
fun CrtVignette(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val radius = maxOf(size.width, size.height) * 0.65f
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                center = center,
                radius = radius,
            ),
        )
    }
}

/**
 * Phosphor glow horizontal bar — place behind active/highlighted elements.
 * Renders a subtle green bloom strip at full width.
 */
@Composable
fun PhosphorGlowBar(
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 2.dp,
    glowSpread: Dp = 12.dp,
) {
    Canvas(modifier = modifier) {
        val h = height.toPx()
        val glow = glowSpread.toPx()
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    color.copy(alpha = 0.15f),
                    color.copy(alpha = 0.6f),
                    color.copy(alpha = 0.15f),
                    Color.Transparent,
                ),
                startY = size.height / 2f - glow,
                endY = size.height / 2f + glow,
            ),
            size = Size(size.width, size.height),
        )
        drawRect(
            color = color,
            topLeft = Offset(0f, (size.height - h) / 2f),
            size = Size(size.width, h),
        )
    }
}

/**
 * Blinking block terminal cursor `▌`.
 *
 * Mimics a classic terminal cursor with a crisp 500ms on/off blink.
 * Tinted in the active accent color by default.
 */
@Composable
fun TerminalCursor(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00FF41),
    style: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
) {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 0
                1f at 450 using LinearEasing
                0f at 500 using LinearEasing
                0f at 950 using LinearEasing
                1f at 1000
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "cursor_alpha",
    )
    Text(
        text = "▌",
        color = color.copy(alpha = alpha),
        style = style,
        modifier = modifier,
    )
}

/**
 * Applies a thin phosphor-green border + corner radius to any composable.
 * Use on cards and panels to give them the terminal-panel look.
 */
fun Modifier.terminalBorder(
    color: Color = Color(0xFF1C3522),
    width: Dp = 1.dp,
    cornerRadius: Dp = 4.dp,
): Modifier = this.border(width, color, RoundedCornerShape(cornerRadius))

/**
 * Box that draws a dim green phosphor dot-grid background.
 * Looks like a terminal screen or LED matrix behind your content.
 */
@Composable
fun PhosphorDotGrid(
    modifier: Modifier = Modifier,
    dotColor: Color = Color(0xFF00FF41),
    dotAlpha: Float = 0.06f,
    spacing: Float = 18f,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val dotRadius = 1f
        val color = dotColor.copy(alpha = dotAlpha)
        var x = spacing / 2f
        while (x < size.width) {
            var y = spacing / 2f
            while (y < size.height) {
                drawCircle(color = color, radius = dotRadius, center = Offset(x, y))
                y += spacing
            }
            x += spacing
        }
    }
}

/**
 * Terminal header decoration: thin top border + optional bottom separator line
 * in accent phosphor green. Use above content blocks for section framing.
 */
@Composable
fun TerminalHeaderRule(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00FF41),
    alpha: Float = 0.4f,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            color = color.copy(alpha = alpha),
            topLeft = Offset(0f, 0f),
            size = Size(size.width, 1.dp.toPx()),
        )
    }
}
