package com.mileway.core.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier
import com.mileway.core.ui.theme.DesignTokens

/** Row reorder/insert/remove motion for keyed lazy lists, on the DESIGN.md STANDARD duration. */
fun LazyItemScope.listItemMotion(): Modifier =
    Modifier.animateItem(
        placementSpec = tween(DesignTokens.Motion.STANDARD, easing = DesignTokens.Motion.standardEasing),
    )
