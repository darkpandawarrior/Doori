@file:Suppress("ktlint:standard:function-naming")

package com.mileway.feature.agent.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.agent_cd_open_assistant
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

@Composable
fun AssistantFab(
    onOpen: () -> Unit,
    onDismissToTopbar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val fabSizeDp = 52.dp
    val fabSizePx = with(density) { fabSizeDp.toPx() }
    val dismissThresholdPx = with(density) { 80.dp.toPx() }

    val animatedOffsetX by animateDpAsState(
        targetValue = if (isDragging) with(density) { offsetX.toDp() } else 0.dp,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "fab_x",
    )
    val animatedOffsetY by animateDpAsState(
        targetValue = if (isDragging) with(density) { offsetY.toDp() } else 0.dp,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "fab_y",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .offset { IntOffset(animatedOffsetX.roundToPx(), animatedOffsetY.roundToPx()) }
                .size(fabSizeDp)
                .shadow(8.dp, DesignTokens.Shape.button)
                .background(MaterialTheme.colorScheme.primary, DesignTokens.Shape.button)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        },
                        onDragEnd = {
                            isDragging = false
                            if (offsetY < -dismissThresholdPx || kotlin.math.abs(offsetX) > fabSizePx * 2) {
                                onDismissToTopbar()
                            }
                            offsetX = 0f
                            offsetY = 0f
                        },
                        onDragCancel = {
                            isDragging = false
                            offsetX = 0f
                            offsetY = 0f
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onOpen() })
                }
                .padding(4.dp),
    ) {
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = stringResource(Res.string.agent_cd_open_assistant),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}
