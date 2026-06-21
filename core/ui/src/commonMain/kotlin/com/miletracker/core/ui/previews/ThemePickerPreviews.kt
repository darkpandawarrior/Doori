package com.miletracker.core.ui.previews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.components.theme.MilewayThemePicker
import com.miletracker.core.ui.theme.DesignTokens
import com.miletracker.core.ui.theme.MilewayTheme

// ---------------------------------------------------------------------------
// Design Language v2 — theme-picker gallery previews.
//
// Each preview renders the curated MilewayThemePicker inside one of the four
// schemes, so the Roborazzi catalog captures the picker as it looks in every
// theme (canvas / card / accent / text all painted from that scheme).
// ---------------------------------------------------------------------------

@Composable
private fun ThemePickerInScheme(theme: MilewayTheme) {
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
fun PreviewThemePickerMatrix() = ThemePickerInScheme(MilewayTheme.MATRIX)

@Preview(name = "Theme picker · Amoled", showBackground = true, widthDp = 360, heightDp = 360)
@Composable
fun PreviewThemePickerAmoled() = ThemePickerInScheme(MilewayTheme.AMOLED)

@Preview(name = "Theme picker · Ion", showBackground = true, widthDp = 360, heightDp = 360)
@Composable
fun PreviewThemePickerIon() = ThemePickerInScheme(MilewayTheme.ION)

@Preview(name = "Theme picker · Daybreak", showBackground = true, widthDp = 360, heightDp = 360)
@Composable
fun PreviewThemePickerDaybreak() = ThemePickerInScheme(MilewayTheme.DAYBREAK)
