package com.miletracker.core.ui.components.bottombar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Vertical drag threshold (in pixels) for the FAB throw-up gesture. When the user drags the
 * FAB upward by more than this amount, [BubbleBottomBar]'s `onFabThrowUp` is triggered instead
 * of navigating between tabs.
 */
private const val FAB_THROW_UP_THRESHOLD_PX = 120f
private const val FAB_COLLAPSE_DOWN_THRESHOLD_PX = 100f
private const val COLLAPSED_WHEEL_START_ANGLE_DEGREES = 194f
private const val COLLAPSED_WHEEL_END_ANGLE_DEGREES = 308f

// CompositionLocal to provide item count throughout the bottom bar components
private val LocalItemCount = compositionLocalOf { 0 }

private fun normalizeAngle(angleDegrees: Float): Float {
    val normalized = angleDegrees % 360f
    return if (normalized < 0f) normalized + 360f else normalized
}

private fun angleDistanceDegrees(
    first: Float,
    second: Float,
): Float {
    val delta = abs(normalizeAngle(first) - normalizeAngle(second))
    return if (delta > 180f) 360f - delta else delta
}

private fun radiansToDegrees(radians: Float): Float = radians * 180f / PI.toFloat()

private fun degreesToRadians(degrees: Float): Float = degrees * PI.toFloat() / 180f

/**
 * Navigation item for the bubble bottom bar with badge support.
 *
 * Icons come either from [selectedIcon]/[unselectedIcon] vectors or from a [painter] slot
 * (used for brand-mark tabs like the centre logo action, where the caller resolves a
 * platform drawable into a [Painter]).
 *
 * @property label Display label for the navigation item
 * @property selectedIcon Icon shown when item is selected (null if using [painter])
 * @property unselectedIcon Icon shown when item is unselected (null if using [painter])
 * @property painter Composable slot resolving a [Painter] for both states (null if using vectors)
 * @property badgeCount Optional badge count to display on the item (null = no badge)
 * @property isHome Whether this item is the home/centre action (rendered in the floating FAB)
 */
data class BubbleNavItem(
    val label: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null,
    val painter: (@Composable () -> Painter)? = null,
    val badgeCount: Int? = null,
    val isHome: Boolean = false,
)

/**
 * Custom shape that creates a pill with a circular cutout that can animate position.
 * The cutout smoothly moves to follow the selected item.
 */
private class CutoutPillShape(
    // 0f to 1f, position of cutout center
    private val cutoutCenterXFraction: Float,
    private val cutoutRadius: Dp,
    private val cutoutDepth: Dp,
    private val cornerRadius: Dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val cutoutRadiusPx = with(density) { cutoutRadius.toPx() }
        val cutoutDepthPx = with(density) { cutoutDepth.toPx() }
        val cornerRadiusPx = with(density) { cornerRadius.toPx() }

        val cutoutCenterX = size.width * cutoutCenterXFraction

        val path =
            Path().apply {
                // Start with the main rounded rectangle
                addRoundRect(
                    RoundRect(
                        rect = Rect(0f, 0f, size.width, size.height),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    ),
                )

                // Create the curved cutout at the top
                val cutoutPath =
                    Path().apply {
                        moveTo(cutoutCenterX - cutoutRadiusPx - cutoutDepthPx, 0f)
                        // Curve down into the cutout (left side)
                        cubicTo(
                            cutoutCenterX - cutoutRadiusPx,
                            0f,
                            cutoutCenterX - cutoutRadiusPx,
                            cutoutDepthPx,
                            cutoutCenterX,
                            cutoutDepthPx,
                        )
                        // Curve up from the cutout (right side)
                        cubicTo(
                            cutoutCenterX + cutoutRadiusPx,
                            cutoutDepthPx,
                            cutoutCenterX + cutoutRadiusPx,
                            0f,
                            cutoutCenterX + cutoutRadiusPx + cutoutDepthPx,
                            0f,
                        )
                        lineTo(cutoutCenterX - cutoutRadiusPx - cutoutDepthPx, 0f)
                        close()
                    }

                // Subtract the cutout from the main shape
                op(this, cutoutPath, PathOperation.Difference)
            }

        return Outline.Generic(path)
    }
}

/**
 * Floating bubble bottom navigation bar with dynamic cutout effect and drag-to-select support.
 *
 * Design features:
 * - Floating pill-shaped bar with a smooth animated cutout
 * - Cutout follows the selected item creating a dynamic effect
 * - FAB floats in the cutout position showing the selected item
 * - Press and hold the FAB to drag it to other items
 * - **Throw-up gesture**: drag the FAB upward past [FAB_THROW_UP_THRESHOLD_PX] to trigger
 *   [onFabThrowUp] (e.g. open a global assistant action)
 * - **Collapse gesture**: drag the FAB downward past [FAB_COLLAPSE_DOWN_THRESHOLD_PX] to
 *   trigger [onCollapseRequested] (pair with [CollapsedBottomPuck])
 * - Clean icons with labels and dot indicators, spring animations, haptics, badges
 *
 * @param items List of navigation items to display
 * @param selectedItemIndex Currently selected item index
 * @param onItemSelected Callback when a different item is selected
 * @param onItemReselected Callback when the currently selected item is tapped again
 * @param onFabThrowUp Optional callback triggered when the user throws the FAB upward.
 *   When non-null and the gesture is detected, [onItemSelected] is NOT called for that drag.
 * @param onCollapseRequested Optional callback triggered when the selected FAB is dragged down
 *   past [FAB_COLLAPSE_DOWN_THRESHOLD_PX] or the collapse affordance is tapped.
 * @param modifier Optional modifier for the bottom bar container
 * @param showLabels Whether to show labels below icons (default: true)
 * @param enableDragToSelect Enable drag-to-select functionality (default: true)
 */
@Composable
fun BubbleBottomBar(
    items: List<BubbleNavItem>,
    selectedItemIndex: Int,
    onItemSelected: (Int) -> Unit,
    onItemReselected: ((Int) -> Unit)? = null,
    onFabThrowUp: (() -> Unit)? = null,
    onCollapseRequested: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true,
    enableDragToSelect: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val collapseAction = onCollapseRequested
    val hasCollapseAction = collapseAction != null

    // Drag state management
    var isDragging by remember { mutableStateOf(false) }
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    var lastHapticIndex by remember { mutableIntStateOf(-1) }

    // Get the currently selected item - this will be shown in the floating FAB
    val selectedItem = items.getOrNull(selectedItemIndex)

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        val screenWidth = maxWidth
        val isCompact = screenWidth < 360.dp

        // Make additional adjustments when there are many items
        // to ensure they all fit on screen properly
        val hasLotsOfItems = items.size >= 6

        // Adjust padding and sizes based on item count
        val barHeight =
            when {
                hasLotsOfItems -> if (showLabels) 68.dp else 60.dp
                else -> if (showLabels) 72.dp else 64.dp
            }

        val fabSize =
            when {
                hasLotsOfItems -> if (isCompact) 50.dp else 56.dp
                isCompact -> 54.dp
                else -> 60.dp
            }

        val cutoutRadius = fabSize / 2 + 8.dp
        val cutoutDepth = 12.dp

        val itemCount = items.size

        // Calculate cutout center position based on selected index or drag position
        val targetCutoutPosition =
            if (isDragging && hoveredIndex != null) {
                (hoveredIndex!! + 0.5f) / itemCount
            } else {
                (selectedItemIndex + 0.5f) / itemCount
            }

        // Animate the cutout position with spring animation
        val animatedCutoutPosition by animateFloatAsState(
            targetValue = targetCutoutPosition,
            animationSpec =
                spring(
                    dampingRatio = if (isDragging) Spring.DampingRatioLowBouncy else Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessVeryLow,
                ),
            label = "cutout_position",
        )

        // Calculate FAB offset in dp
        val fabOffsetX =
            with(density) {
                val totalWidthPx = screenWidth.toPx()
                val centerPx = totalWidthPx / 2
                val targetPx = totalWidthPx * animatedCutoutPosition
                (targetPx - centerPx).toDp()
            }

        CompositionLocalProvider(LocalItemCount provides items.size) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                // Main pill-shaped bar with animated cutout
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(barHeight),
                    shape =
                        CutoutPillShape(
                            cutoutCenterXFraction = animatedCutoutPosition,
                            cutoutRadius = cutoutRadius,
                            cutoutDepth = cutoutDepth,
                            cornerRadius = 36.dp,
                        ),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 12.dp,
                    tonalElevation = 3.dp,
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = if (hasLotsOfItems) 2.dp else 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items.forEachIndexed { index, item ->
                            val isSelected = selectedItemIndex == index
                            val isHovered = isDragging && hoveredIndex == index

                            // The selected item's icon is shown in the floating FAB while a
                            // spacer reserves its position in the bar.
                            if (isSelected && !isDragging) {
                                Spacer(modifier = Modifier.width(fabSize + 8.dp))
                            } else if (isHovered) {
                                Spacer(modifier = Modifier.width(fabSize + 8.dp))
                            } else {
                                DynamicNavItem(
                                    item = item,
                                    isSelected = false,
                                    showLabel = showLabels,
                                    isCompact = isCompact,
                                    isHovered = isHovered,
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                        onItemSelected(index)
                                    },
                                )
                            }
                        }
                    }
                }

                // Floating FAB shows the currently selected item
                if (selectedItem != null) {
                    DraggableFloatingFab(
                        currentItem =
                            if (isDragging && hoveredIndex != null) {
                                items.getOrNull(hoveredIndex!!) ?: selectedItem
                            } else {
                                selectedItem
                            },
                        fabSize = fabSize,
                        offsetX = fabOffsetX,
                        isDragging = isDragging,
                        enableDragToSelect = enableDragToSelect,
                        showCollapseAffordance = hasCollapseAction,
                        onDragStart = {
                            isDragging = true
                            lastHapticIndex = selectedItemIndex
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDrag = { offset ->
                            // Calculate which item we're hovering over
                            with(density) {
                                val totalWidthPx = screenWidth.toPx()
                                val itemWidth = totalWidthPx / itemCount

                                // Absolute position of the FAB; fabOffsetX is relative to centre
                                val fabAbsoluteX = (totalWidthPx / 2f) + fabOffsetX.toPx() + offset

                                // Find the closest item center to the current FAB position
                                var closestIndex = 0
                                var minDistance = Float.MAX_VALUE
                                for (i in 0 until itemCount) {
                                    val itemCenterX = (i + 0.5f) * itemWidth
                                    val distance = abs(fabAbsoluteX - itemCenterX)
                                    if (distance < minDistance) {
                                        minDistance = distance
                                        closestIndex = i
                                    }
                                }

                                if (closestIndex != hoveredIndex) {
                                    hoveredIndex = closestIndex
                                    // Haptic feedback when crossing item boundaries
                                    if (closestIndex != lastHapticIndex) {
                                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                        lastHapticIndex = closestIndex
                                    }
                                }
                            }
                        },
                        onDragEnd = { thrownUp, collapsedDown ->
                            if (thrownUp && onFabThrowUp != null) {
                                // Upward throw gesture, launch the global action
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onFabThrowUp()
                            } else if (collapsedDown && onCollapseRequested != null) {
                                haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                onCollapseRequested()
                            } else if (hoveredIndex != null && hoveredIndex != selectedItemIndex) {
                                haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                onItemSelected(hoveredIndex!!)
                            }
                            isDragging = false
                            hoveredIndex = null
                            lastHapticIndex = -1
                        },
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            onItemReselected?.invoke(selectedItemIndex)
                        },
                        onCollapseClick =
                            collapseAction?.let { action ->
                                {
                                    haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                    action()
                                }
                            },
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-16).dp)
                                .zIndex(1f),
                    )
                }
            }
        }
    }
}

/**
 * Draggable floating FAB that displays the currently selected item and supports drag-to-select.
 * Moves horizontally with the cutout and can be dragged to other positions.
 *
 * ### Vertical gestures
 * - Upward drag past [FAB_THROW_UP_THRESHOLD_PX] triggers the throw-up action.
 * - Downward drag past [FAB_COLLAPSE_DOWN_THRESHOLD_PX] triggers collapse when enabled.
 *
 * @param onDragEnd Callback invoked on drag release.
 *   `thrownUp = true` when the upward throw gesture was detected.
 *   `collapsedDown = true` when the downward collapse gesture was detected.
 */
@Composable
fun DraggableFloatingFab(
    currentItem: BubbleNavItem,
    fabSize: Dp,
    offsetX: Dp,
    isDragging: Boolean,
    enableDragToSelect: Boolean,
    showCollapseAffordance: Boolean,
    onDragStart: () -> Unit,
    onDrag: (offset: Float) -> Unit,
    onDragEnd: (thrownUp: Boolean, collapsedDown: Boolean) -> Unit,
    onClick: () -> Unit,
    onCollapseClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val primaryColor = MaterialTheme.colorScheme.primary

    // Track drag offsets, X for tab selection, Y for throw-up gesture
    var currentDragOffset by remember { mutableFloatStateOf(0f) }
    var cumulativeDragY by remember { mutableFloatStateOf(0f) }

    // Drag sensitivity factor - lower value = slower, more controlled movement
    val dragSensitivity = 0.6f

    // Scale up slightly when dragging for visual feedback
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.25f else 1.1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        label = "fab_scale",
    )

    // Increase shadow when dragging
    val shadowElevation by animateDpAsState(
        targetValue = if (isDragging) 24.dp else 16.dp,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        label = "fab_shadow",
    )

    // Animate the horizontal offset when not dragging
    val animatedOffsetX by animateDpAsState(
        targetValue = offsetX,
        animationSpec =
            spring(
                dampingRatio = if (isDragging) Spring.DampingRatioLowBouncy else Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessVeryLow,
            ),
        label = "fab_offset_x",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier
                .offset(x = animatedOffsetX)
                .offset { IntOffset(currentDragOffset.roundToInt(), 0) }
                .then(
                    if (enableDragToSelect) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    currentDragOffset = 0f
                                    cumulativeDragY = 0f
                                    onDragStart()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    // Track vertical movement for throw-up detection
                                    cumulativeDragY += dragAmount.y
                                    // Apply drag sensitivity for slower, more controlled movement
                                    currentDragOffset += dragAmount.x * dragSensitivity
                                    onDrag(currentDragOffset)
                                },
                                onDragEnd = {
                                    val verticalDominant = abs(cumulativeDragY) > abs(currentDragOffset) * 1.2f
                                    // Negative Y = upward drag; positive Y = downward drag
                                    val thrownUp =
                                        verticalDominant &&
                                            cumulativeDragY < -FAB_THROW_UP_THRESHOLD_PX
                                    val collapsedDown =
                                        verticalDominant &&
                                            cumulativeDragY > FAB_COLLAPSE_DOWN_THRESHOLD_PX
                                    currentDragOffset = 0f
                                    cumulativeDragY = 0f
                                    onDragEnd(thrownUp, collapsedDown)
                                },
                                onDragCancel = {
                                    currentDragOffset = 0f
                                    cumulativeDragY = 0f
                                    onDragEnd(false, false)
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
    ) {
        BadgedBox(
            badge = {
                if (currentItem.badgeCount != null && currentItem.badgeCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ) {
                        Text(
                            text = if (currentItem.badgeCount > 99) "99+" else currentItem.badgeCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            },
        ) {
            Box(
                modifier =
                    Modifier
                        .size(fabSize)
                        .scale(scale)
                        .shadow(
                            elevation = shadowElevation,
                            shape = CircleShape,
                            clip = false,
                        )
                        .clip(CircleShape)
                        .background(primaryColor)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick,
                            enabled = !isDragging,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                // Support both ImageVector and painter-slot icons
                when {
                    currentItem.selectedIcon != null -> {
                        Icon(
                            imageVector = currentItem.selectedIcon,
                            contentDescription = currentItem.label,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    currentItem.painter != null -> {
                        // Larger size for brand-mark icons in the FAB
                        Icon(
                            painter = currentItem.painter.invoke(),
                            contentDescription = currentItem.label,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
        if (showCollapseAffordance && !isDragging && onCollapseClick != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier =
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCollapseClick,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Collapse bottom navigation",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Collapsed bottom navigation puck with an amulet-like wheel selector.
 *
 * - Tap expands the full bottom bar.
 * - Long press opens a wheel around the puck.
 * - While holding, drag to cycle through options and release to navigate.
 */
@Composable
fun CollapsedBottomPuck(
    items: List<BubbleNavItem>,
    selectedItemIndex: Int,
    onExpand: () -> Unit,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val selectedItem = items.getOrNull(selectedItemIndex) ?: return
    val wheelItemIndexes =
        remember(items, selectedItemIndex) {
            items.indices.filter { it != selectedItemIndex }
        }
    val wheelItemAngles =
        remember(wheelItemIndexes) {
            if (wheelItemIndexes.isEmpty()) {
                emptyMap()
            } else {
                val step =
                    if (wheelItemIndexes.size == 1) {
                        0f
                    } else {
                        (COLLAPSED_WHEEL_END_ANGLE_DEGREES - COLLAPSED_WHEEL_START_ANGLE_DEGREES) /
                            (wheelItemIndexes.size - 1).toFloat()
                    }
                wheelItemIndexes.mapIndexed { index, itemIndex ->
                    itemIndex to (COLLAPSED_WHEEL_START_ANGLE_DEGREES + (index * step))
                }.toMap()
            }
        }
    val puckSize = 56.dp
    val wheelDiameter = 176.dp
    val wheelRadius = 74.dp
    val chipSize = 38.dp
    val hoveredChipSize = 44.dp
    val wheelOffset = (wheelDiameter - puckSize) / 2
    val wheelSweepAngle = COLLAPSED_WHEEL_END_ANGLE_DEGREES - COLLAPSED_WHEEL_START_ANGLE_DEGREES
    val hoverActivationRadiusPx = with(density) { 18.dp.toPx() }
    val wheelRadiusPx = with(density) { wheelRadius.toPx() }
    val wheelArcPrimaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
    val wheelArcInnerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
    var isWheelVisible by remember { mutableStateOf(false) }
    var hoveredItemIndex by remember { mutableStateOf<Int?>(null) }
    var lastHapticItemIndex by remember { mutableIntStateOf(-1) }
    val closeWheel = {
        isWheelVisible = false
        hoveredItemIndex = null
        lastHapticItemIndex = -1
    }
    val commitSelection = {
        hoveredItemIndex?.let { index ->
            if (index != selectedItemIndex) {
                haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                onItemSelected(index)
            }
        }
        closeWheel()
    }
    val updateHoveredSelection = { x: Float, y: Float ->
        if (wheelItemIndexes.isEmpty()) {
            hoveredItemIndex = null
        } else {
            val center = with(density) { puckSize.toPx() / 2f }
            val deltaX = x - center
            val deltaY = y - center
            val distance = sqrt((deltaX * deltaX) + (deltaY * deltaY))
            if (distance < hoverActivationRadiusPx) {
                hoveredItemIndex = null
            } else {
                val angle = normalizeAngle(radiansToDegrees(atan2(deltaY, deltaX)))
                val nextHoveredIndex =
                    wheelItemAngles.minByOrNull { (_, targetAngle) ->
                        angleDistanceDegrees(angle, targetAngle)
                    }?.key
                if (nextHoveredIndex != hoveredItemIndex) {
                    hoveredItemIndex = nextHoveredIndex
                    if (nextHoveredIndex != null && nextHoveredIndex != lastHapticItemIndex) {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        lastHapticItemIndex = nextHoveredIndex
                    }
                }
            }
        }
    }
    Box(
        modifier =
            modifier
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 16.dp)
                .size(puckSize),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = isWheelVisible && wheelItemIndexes.isNotEmpty(),
            enter =
                fadeIn(
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                ) +
                    scaleIn(
                        initialScale = 0.65f,
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessVeryLow,
                            ),
                    ),
            exit =
                fadeOut(
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                ) +
                    scaleOut(
                        targetScale = 0.75f,
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                    ),
            modifier =
                Modifier
                    .requiredSize(wheelDiameter)
                    .offset(x = -wheelOffset, y = -wheelOffset),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(wheelDiameter)
                            .clip(CircleShape)
                            .background(
                                brush =
                                    Brush.radialGradient(
                                        colors =
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                Color.Transparent,
                                            ),
                                    ),
                            ),
                )
                Canvas(
                    modifier = Modifier.size((wheelRadius * 2) + 34.dp),
                ) {
                    drawArc(
                        color = wheelArcPrimaryColor,
                        startAngle = COLLAPSED_WHEEL_START_ANGLE_DEGREES,
                        sweepAngle = wheelSweepAngle,
                        useCenter = false,
                        style =
                            Stroke(
                                width = 4.dp.toPx(),
                                cap = StrokeCap.Round,
                            ),
                    )
                    drawArc(
                        color = wheelArcInnerColor,
                        startAngle = COLLAPSED_WHEEL_START_ANGLE_DEGREES + 2f,
                        sweepAngle = wheelSweepAngle - 4f,
                        useCenter = false,
                        style =
                            Stroke(
                                width = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                            ),
                    )
                }
                wheelItemIndexes.forEach { itemIndex ->
                    val angle = wheelItemAngles[itemIndex] ?: return@forEach
                    val angleInRadians = degreesToRadians(angle)
                    val x = (cos(angleInRadians) * wheelRadiusPx).roundToInt()
                    val y = (sin(angleInRadians) * wheelRadiusPx).roundToInt()
                    val item = items[itemIndex]
                    val isHovered = hoveredItemIndex == itemIndex
                    val chipScale by animateFloatAsState(
                        targetValue = if (isHovered) 1.14f else 1f,
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                        label = "collapsed_wheel_chip_scale",
                    )
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .offset { IntOffset(x, y) }
                                .size(if (isHovered) hoveredChipSize else chipSize)
                                .scale(chipScale)
                                .clip(CircleShape)
                                .background(
                                    brush =
                                        Brush.radialGradient(
                                            colors =
                                                if (isHovered) {
                                                    listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                                                    )
                                                } else {
                                                    listOf(
                                                        MaterialTheme.colorScheme.surface,
                                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
                                                    )
                                                },
                                        ),
                                )
                                .border(
                                    width = 1.dp,
                                    color =
                                        if (isHovered) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                                        } else {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        },
                                    shape = CircleShape,
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        hoveredItemIndex = itemIndex
                                        commitSelection()
                                    },
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            item.selectedIcon != null -> {
                                Icon(
                                    imageVector = item.selectedIcon,
                                    contentDescription = "Switch to ${item.label}",
                                    modifier = Modifier.size(if (isHovered) 22.dp else 19.dp),
                                    tint =
                                        if (isHovered) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                )
                            }
                            item.painter != null -> {
                                Icon(
                                    painter = item.painter.invoke(),
                                    contentDescription = "Switch to ${item.label}",
                                    modifier = Modifier.size(if (isHovered) 24.dp else 20.dp),
                                    tint =
                                        if (isHovered) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                )
                            }
                        }
                    }
                }
            }
        }
        Surface(
            modifier =
                Modifier
                    .size(puckSize)
                    .pointerInput(wheelItemIndexes, wheelItemAngles, selectedItemIndex) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { dragStartOffset ->
                                if (wheelItemIndexes.isEmpty()) return@detectDragGesturesAfterLongPress
                                isWheelVisible = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                updateHoveredSelection(dragStartOffset.x, dragStartOffset.y)
                            },
                            onDrag = { change, _ ->
                                if (!isWheelVisible) return@detectDragGesturesAfterLongPress
                                change.consume()
                                updateHoveredSelection(change.position.x, change.position.y)
                            },
                            onDragEnd = {
                                if (isWheelVisible) {
                                    commitSelection()
                                }
                            },
                            onDragCancel = {
                                closeWheel()
                            },
                        )
                    }
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            if (isWheelVisible) {
                                closeWheel()
                            } else {
                                onExpand()
                            }
                        },
                        onLongClick = {
                            if (wheelItemIndexes.isNotEmpty()) {
                                isWheelVisible = true
                                hoveredItemIndex = null
                                lastHapticItemIndex = -1
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                    ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = if (isWheelVisible) 20.dp else 14.dp,
            tonalElevation = 2.dp,
            border =
                BorderStroke(
                    width = if (isWheelVisible) 2.dp else 1.dp,
                    color =
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = if (isWheelVisible) 0.62f else 0.34f,
                        ),
                ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                brush =
                                    Brush.radialGradient(
                                        colors =
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                                                MaterialTheme.colorScheme.primary,
                                            ),
                                    ),
                            ),
                )
                when {
                    selectedItem.selectedIcon != null -> {
                        Icon(
                            imageVector = selectedItem.selectedIcon,
                            contentDescription = "Collapsed navigation for ${selectedItem.label}",
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    selectedItem.painter != null -> {
                        Icon(
                            painter = selectedItem.painter.invoke(),
                            contentDescription = "Collapsed navigation for ${selectedItem.label}",
                            modifier = Modifier.size(34.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Navigation item with dynamic selection indicator and hover state during drag.
 */
@Composable
fun DynamicNavItem(
    item: BubbleNavItem,
    isSelected: Boolean,
    showLabel: Boolean,
    isCompact: Boolean,
    isHovered: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val primaryColor = MaterialTheme.colorScheme.primary

    // Detect if we're in a layout with many items
    val itemCount = LocalItemCount.current
    val hasLotsOfItems = itemCount >= 6

    // Animate icon scale - scale up when hovered during drag
    val iconScale by animateFloatAsState(
        targetValue =
            when {
                isHovered -> 1.15f
                isSelected -> if (hasLotsOfItems) 1.15f else 1.2f
                else -> 1f
            },
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        label = "icon_scale",
    )

    // Animate dot size
    val dotSize by animateDpAsState(
        targetValue =
            if (isSelected) {
                if (hasLotsOfItems) {
                    4.dp
                } else {
                    6.dp
                }
            } else {
                0.dp
            },
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        label = "dot_size",
    )

    // Animate icon alpha when hovered
    val iconAlpha by animateFloatAsState(
        targetValue = if (isHovered) 0.6f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        label = "icon_alpha",
    )

    // Adjust sizes based on number of items
    val iconSize =
        when {
            hasLotsOfItems -> if (isCompact) 22.dp else 24.dp
            isCompact -> 24.dp
            else -> 26.dp
        }

    // Make items narrower when we have many
    val itemWidth =
        when {
            hasLotsOfItems -> if (isCompact) 46.dp else 52.dp
            isCompact -> 52.dp
            else -> 60.dp
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(
                    horizontal = if (hasLotsOfItems) 1.dp else 2.dp,
                    vertical = if (hasLotsOfItems) 5.dp else 6.dp,
                )
                .width(itemWidth),
    ) {
        BadgedBox(
            badge = {
                if (item.badgeCount != null && item.badgeCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.offset(x = (-2).dp, y = 2.dp),
                    ) {
                        Text(
                            text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            },
        ) {
            // Support both ImageVector and painter-slot icons
            when {
                item.selectedIcon != null && item.unselectedIcon != null -> {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        modifier =
                            Modifier
                                .size(iconSize)
                                .scale(iconScale)
                                .graphicsLayer {
                                    alpha = iconAlpha
                                },
                        tint =
                            if (isSelected || isHovered) {
                                primaryColor
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
                item.painter != null -> {
                    Icon(
                        painter = item.painter.invoke(),
                        contentDescription = item.label,
                        modifier =
                            Modifier
                                .size(iconSize)
                                .scale(iconScale)
                                .graphicsLayer {
                                    alpha = iconAlpha
                                },
                        tint =
                            if (isSelected || isHovered) {
                                primaryColor
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.label,
                fontSize =
                    when {
                        hasLotsOfItems -> if (isCompact) 8.sp else 9.sp
                        isCompact -> 9.sp
                        else -> 10.sp
                    },
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color =
                    if (isSelected || isHovered) {
                        primaryColor.copy(alpha = if (isHovered) 0.6f else 1f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Animated dot indicator
            Box(
                modifier =
                    Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(primaryColor),
            )
        }
    }
}
