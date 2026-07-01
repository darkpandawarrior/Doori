package com.mileway.feature.tracking.ui.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode

object TrackingTheme {
    val LiveTrackingGradient =
        Brush.linearGradient(
            colors = listOf(Color(0xFF4CAF50), Color(0xFF2196F3)),
            tileMode = TileMode.Clamp,
        )
    val PausedGradient =
        Brush.linearGradient(
            colors = listOf(Color(0xFFFF9800), Color(0xFFFF5722)),
            tileMode = TileMode.Clamp,
        )
    val CompletedGradient =
        Brush.linearGradient(
            colors = listOf(Color(0xFF9C27B0), Color(0xFF673AB7)),
            tileMode = TileMode.Clamp,
        )

    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFF9800)
    val Error = Color(0xFFF44336)
    val Info = Color(0xFF2196F3)
    val Neutral = Color(0xFF9E9E9E)
    val Premium = Color(0xFF9C27B0)
}
