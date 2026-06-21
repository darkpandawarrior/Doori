package com.miletracker.core.ui.previews

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.miletracker.core.ui.theme.MileTrackerTheme
import com.miletracker.core.ui.theme.MilewayTheme

/**
 * Wraps preview content in the app's real [MileTrackerTheme] + a Surface. Use instead of calling a
 * raw `MaterialTheme {}` so previews (and the Roborazzi catalog) render in the actual Design
 * Language v2 scheme — Matrix by default — rather than the stock Material baseline.
 *
 * @param theme the curated scheme to render in. Defaults to [MilewayTheme.MATRIX] (the app default).
 */
@Composable
fun PreviewSurface(
    theme: MilewayTheme = MilewayTheme.MATRIX,
    content: @Composable () -> Unit,
) {
    MileTrackerTheme(milewayTheme = theme) {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}
