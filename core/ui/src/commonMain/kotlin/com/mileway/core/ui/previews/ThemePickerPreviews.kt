package com.mileway.core.ui.previews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mileway.core.ui.components.theme.MilewayThemePicker
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewayTheme
import com.mileway.core.ui.theme.MilewayThemeVariant

// ---------------------------------------------------------------------------
// Design Language v2 — theme-picker gallery previews.
//
// Each preview renders the curated MilewayThemePicker inside one of the four
// schemes, so the Roborazzi catalog captures the picker as it looks in every
// theme (canvas / card / accent / text all painted from that scheme).
// ---------------------------------------------------------------------------

@Composable
private fun ThemePickerInScheme(theme: MilewayThemeVariant) {
    PreviewSurface(theme = theme) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            MilewayThemePicker(
                selected = theme,
                onSelect = {},
            )
        }
    }
}

@Preview(name = "Theme picker · Matrix", showBackground = true, widthDp = 360, heightDp = 360)
@Composable
fun PreviewThemePickerMatrix() = ThemePickerInScheme(MilewayThemeVariant.MATRIX)

@Preview(name = "Theme picker · Amoled", showBackground = true, widthDp = 360, heightDp = 360)
@Composable
fun PreviewThemePickerAmoled() = ThemePickerInScheme(MilewayThemeVariant.AMOLED)

@Preview(name = "Theme picker · Ion", showBackground = true, widthDp = 360, heightDp = 360)
@Composable
fun PreviewThemePickerIon() = ThemePickerInScheme(MilewayThemeVariant.ION)

@Preview(name = "Theme picker · Daybreak", showBackground = true, widthDp = 360, heightDp = 360)
@Composable
fun PreviewThemePickerDaybreak() = ThemePickerInScheme(MilewayThemeVariant.DAYBREAK)
