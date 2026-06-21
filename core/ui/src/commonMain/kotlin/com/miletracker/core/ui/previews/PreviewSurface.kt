package com.miletracker.core.ui.previews

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable

/**
 * Wraps preview content in the app's MaterialTheme + a Surface.
 * Use instead of calling Theme { ... } directly so previews track theme changes in one place.
 */
@Composable
fun PreviewSurface(content: @Composable () -> Unit) {
    MaterialTheme {
        Surface(content = content)
    }
}
