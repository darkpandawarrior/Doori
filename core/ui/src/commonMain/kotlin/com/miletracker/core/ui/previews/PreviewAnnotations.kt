package com.miletracker.core.ui.previews

import androidx.compose.ui.tooling.preview.Preview

// ---------------------------------------------------------------------------
// Platform-neutral multipreview annotations for KMP commonMain.
//
// Rules:
// - Do NOT use the `device` parameter, it's Android-only. Use widthDp/heightDp.
// - Dark mode uses uiMode = 0x20 (Configuration.UI_MODE_NIGHT_YES literal).
// - fontScale uses @Preview's built-in fontScale parameter.
// ---------------------------------------------------------------------------

/** Light + dark theme pair. Primary matrix for all UI components. */
@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 800, uiMode = 0x20)
annotation class PreviewLightDark

/** Three font scales: default, large, extra-large. */
@Preview(name = "Font 1.0x", showBackground = true, fontScale = 1.0f, widthDp = 360)
@Preview(name = "Font 1.3x", showBackground = true, fontScale = 1.3f, widthDp = 360)
@Preview(name = "Font 1.5x", showBackground = true, fontScale = 1.5f, widthDp = 360)
annotation class PreviewFontScales

/** Phone, compact-wide, and mini device form-factors (widthDp/heightDp only). */
@Preview(name = "Phone 360", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Phone 411", showBackground = true, widthDp = 411, heightDp = 891)
@Preview(name = "Compact 600", showBackground = true, widthDp = 600, heightDp = 800)
annotation class PreviewDevices

/**
 * Full diversity matrix: phone light/dark × font scales.
 * Use on screens where you want thorough coverage.
 */
@Preview(name = "Ph Light 1.0", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Ph Dark 1.0", showBackground = true, widthDp = 360, heightDp = 800, uiMode = 0x20)
@Preview(name = "Ph Light 1.3", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 1.3f)
@Preview(name = "Ph Dark 1.3", showBackground = true, widthDp = 360, heightDp = 800, uiMode = 0x20, fontScale = 1.3f)
annotation class PreviewMatrix

/** State-labelled previews, callers provide the right data per annotation. */
@Preview(name = "Empty", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Loading", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Error", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(name = "Populated", showBackground = true, widthDp = 360, heightDp = 800)
annotation class PreviewStates
