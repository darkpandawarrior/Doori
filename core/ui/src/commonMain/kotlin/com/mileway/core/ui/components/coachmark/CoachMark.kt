package com.mileway.core.ui.components.coachmark

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.DesignTokens

/**
 * PLAN_V24 P12.5 — a minimal, reusable coach-mark overlay primitive. Not a framework: a controller
 * holding the step list + registered target bounds, a [Modifier.coachMarkTarget] that reports a real
 * composable's bounds so the overlay can anchor to it, and [CoachMarkOverlay] that dims everything
 * except the current target and floats a tooltip with Next/Skip. Colours come from the app theme, so
 * it reads correctly in light and dark. The interactive training tour (feature:profile) is its first
 * consumer, but nothing here is tour-specific.
 */
@Stable
class CoachMarkController {
    /** The ordered steps the overlay walks. Set by the caller before showing the overlay. */
    var steps: List<CoachStep> by mutableStateOf(emptyList())

    /** Index of the step currently shown. */
    var currentIndex: Int by mutableIntStateOf(0)

    /** Called when the user taps Next on the current step. */
    var onNext: () -> Unit = {}

    /** Called when the user taps Skip. */
    var onSkip: () -> Unit = {}

    private val targets: SnapshotStateMap<String, Rect> = mutableStateMapOf()

    /** The step currently displayed, or null when out of range. */
    val current: CoachStep?
        get() = steps.getOrNull(currentIndex)

    /** Report a target composable's root-space bounds (called by [coachMarkTarget]). */
    fun registerTarget(
        key: String,
        bounds: Rect,
    ) {
        targets[key] = bounds
    }

    /** The registered bounds for [key], or null if it has not been positioned yet. */
    fun targetBounds(key: String?): Rect? = key?.let { targets[it] }
}

/**
 * One coach-mark step. [targetKey] anchors the highlight to a [coachMarkTarget]-tagged composable; a
 * null key centres the tooltip (used for intro/completion steps with no single anchor).
 */
data class CoachStep(
    val targetKey: String?,
    val title: String,
    val body: String,
    val nextLabel: String,
)

/** Tag a composable as a coach-mark anchor: reports its root-space bounds to [controller]. */
fun Modifier.coachMarkTarget(
    key: String,
    controller: CoachMarkController,
): Modifier = onGloballyPositioned { controller.registerTarget(key, it.boundsInRoot()) }

/**
 * The full-screen overlay drawn above content: a dimmed scrim with a transparent cut-out around the
 * current step's target, plus an anchored tooltip [Card] with the step title/body and Next/Skip. The
 * scrim swallows touches so underlying UI is inert while the tour runs.
 */
@Composable
fun CoachMarkOverlay(
    controller: CoachMarkController,
    skipLabel: String,
    modifier: Modifier = Modifier,
) {
    val step = controller.current ?: return
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                // Focus the tour tooltip before the dimmed, touch-inert content behind it: a
                // traversal group with a negative index puts the overlay first in TalkBack order.
                .semantics {
                    isTraversalGroup = true
                    traversalIndex = -1f
                }
                .onGloballyPositioned { overlayOrigin = it.positionInRoot() }
                // Consume all gestures so the underlying screen cannot be tapped during the tour.
                .pointerInput(Unit) { awaitPointerEventScopeConsumeAll() },
    ) {
        val rootRect = controller.targetBounds(step.targetKey)
        // Translate the target's root-space rect into this overlay's local space.
        val hole = rootRect?.translate(-overlayOrigin.x, -overlayOrigin.y)
        val scrim = Color.Black.copy(alpha = 0.62f)

        Canvas(Modifier.fillMaxSize()) {
            if (hole == null) {
                drawRect(color = scrim, size = size)
            } else {
                val padPx = 8.dp.toPx()
                val left = (hole.left - padPx).coerceAtLeast(0f)
                val top = (hole.top - padPx).coerceAtLeast(0f)
                val right = (hole.right + padPx).coerceAtMost(size.width)
                val bottom = (hole.bottom + padPx).coerceAtMost(size.height)
                // Four scrim rects leaving the padded target transparent.
                drawRect(color = scrim, topLeft = Offset(0f, 0f), size = Size(size.width, top))
                drawRect(color = scrim, topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))
                drawRect(color = scrim, topLeft = Offset(0f, top), size = Size(left, bottom - top))
                drawRect(color = scrim, topLeft = Offset(right, top), size = Size(size.width - right, bottom - top))
            }
        }

        // Tooltip placement: below the hole if there is room, otherwise above; centred when no anchor.
        val estimatedTooltipHeightDp = 168.dp
        val screenHeightPx = constraints.maxHeight.toFloat()
        val density = androidx.compose.ui.platform.LocalDensity.current
        val tooltipHeightPx = with(density) { estimatedTooltipHeightDp.toPx() }
        val tooltipAlignment: Alignment
        val tooltipOffsetY: Int
        if (hole == null) {
            tooltipAlignment = Alignment.Center
            tooltipOffsetY = 0
        } else {
            val below = hole.bottom + tooltipHeightPx < screenHeightPx
            tooltipAlignment = Alignment.TopCenter
            val yPx = if (below) hole.bottom + with(density) { DesignTokens.Spacing.m.toPx() } else (hole.top - tooltipHeightPx).coerceAtLeast(0f)
            tooltipOffsetY = with(density) { yPx.toInt() }
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(DesignTokens.Spacing.l),
            contentAlignment = tooltipAlignment,
        ) {
            CoachMarkTooltip(
                step = step,
                stepIndex = controller.currentIndex,
                stepCount = controller.steps.size,
                skipLabel = skipLabel,
                onNext = controller.onNext,
                onSkip = controller.onSkip,
                modifier = if (hole == null) Modifier else Modifier.offset { androidx.compose.ui.unit.IntOffset(0, tooltipOffsetY) },
            )
        }
    }
}

@Composable
private fun CoachMarkTooltip(
    step: CoachStep,
    stepIndex: Int,
    stepCount: Int,
    skipLabel: String,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Text(
                text = "${stepIndex + 1} / $stepCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(step.title, style = MaterialTheme.typography.titleMedium)
            Text(
                step.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onSkip) { Text(skipLabel) }
                Button(onClick = onNext) { Text(step.nextLabel) }
            }
        }
    }
}

/** Swallow every pointer event in this region so gestures never reach the content behind the scrim. */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.awaitPointerEventScopeConsumeAll() {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            event.changes.forEach { it.consume() }
        }
    }
}
