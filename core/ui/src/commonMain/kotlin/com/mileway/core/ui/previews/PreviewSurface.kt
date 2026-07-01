package com.mileway.core.ui.previews

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.mileway.core.ui.theme.MilewayTheme
import com.mileway.core.ui.theme.MilewayThemeVariant

/**
 * Wraps preview content in the app's real [MilewayTheme] + a Surface. Use instead of calling a
 * raw `MaterialTheme {}` so previews (and the Roborazzi catalog) render in the actual Design
 * Language v2 scheme — Matrix by default — rather than the stock Material baseline.
 *
 * @param theme the curated scheme to render in. Defaults to [MilewayThemeVariant.MATRIX] (the app default).
 */
@Composable
fun PreviewSurface(
    theme: MilewayThemeVariant = MilewayThemeVariant.MATRIX,
    content: @Composable () -> Unit,
) {
    MilewayTheme(milewayTheme = theme) {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}
