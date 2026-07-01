package com.mileway.core.ui.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Presentation model for a quick action entry (icon + label + click handler).
 *
 * Lives in `core:ui` rather than `core:data` because it carries Compose UI types
 * ([ImageVector], [Color]) that are unavailable in the KMP `commonMain` source set.
 */
data class QuickActionItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val customColor: Color? = null,
    val description: String? = null,
    val badgeCount: Int? = null,
)
