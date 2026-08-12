package com.mileway.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * The JVM("screenshot") target that captures this shell's screens for CI. Same as desktop: no
 * wallpaper-derived dynamic colour source, always falls back to the curated theme.
 */
@Composable
actual fun systemDynamicColorScheme(darkTheme: Boolean): ColorScheme? = null
