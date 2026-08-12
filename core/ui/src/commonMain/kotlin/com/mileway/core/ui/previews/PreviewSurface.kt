package com.mileway.core.ui.previews

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.mileway.core.ui.theme.MilewayTheme
import com.mileway.core.ui.theme.MilewayThemeVariant

/**
 * Wraps preview content in the app's real [MilewayTheme] + a Surface. Use instead of calling a
 * raw `MaterialTheme {}` so previews (and the Roborazzi catalog) render in the actual scheme
 * rather than the stock Material baseline.
 *
 * The default follows [MilewayThemeVariant.DEFAULT] rather than naming a variant. It used to be
 * pinned to `MATRIX`, described in this very doc as "the app default" — which stopped being true
 * when Ember took over and again when Paper did. The result was a catalog of previews rendering
 * phosphor green while the app rendered nothing of the sort, which is where the unexplained green
 * screenshots came from. Tracking DEFAULT means the previews cannot disagree with the app again.
 *
 * A preview that wants a *specific* scheme passes it — and must, since it can no longer inherit
 * one by accident.
 *
 * @param theme the curated scheme to render in. Defaults to whatever the app currently ships.
 */
@Composable
fun PreviewSurface(
    theme: MilewayThemeVariant = MilewayThemeVariant.DEFAULT,
    content: @Composable () -> Unit,
) {
    MilewayTheme(milewayTheme = theme) {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}
