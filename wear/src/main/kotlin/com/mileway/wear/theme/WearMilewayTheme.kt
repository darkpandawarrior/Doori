package com.mileway.wear.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.Typography

/**
 * P2.3: Wear Compose theme wrapper, mapping Mileway's Ember palette onto
 * [androidx.wear.compose.material3.ColorScheme] + [Typography].
 *
 * [WearAppGraph][com.mileway.wear.WearAppGraph]'s doc comment is the reason this file hand-copies
 * hex values instead of depending on `core:ui`: `core:ui` is the Compose *Multiplatform* theming
 * module ([androidx.compose.material3.ColorScheme], a different type from Wear's own
 * `androidx.wear.compose.material3.ColorScheme]`) — Wear renders with `androidx.wear.compose`, its
 * own design system, and must never pull the phone/iOS CMP theming module onto its classpath.
 *
 * The literal hex values below are copied 1:1 from the Ember scheme (`MilewayThemeVariant.EMBER`,
 * the app default, T.1) so the watch stays visually on-brand with the phone:
 *  - Accent ramp + canvas/surface ramp: `core/ui/.../theme/MilewayThemes.kt` (`EmberSpec`).
 *  - Semantic status colours: `core/ui/.../theme/DesignTokens.kt` (`DesignTokens.StatusColors`),
 *    which itself documents being "kept in lock-step" with the curated default scheme — the same
 *    discipline applies here. If the Ember palette changes, update both places.
 * Only the phone's curated default theme is mirrored — Wear OS doesn't expose the phone's in-app
 * theme picker (Matrix/Amoled/Ion/Daybreak), so there's exactly one Wear scheme for now.
 */
private object WearEmberPalette {
    val canvas = Color(0xFF0B0806)
    val surface = Color(0xFF17110B)
    val surfaceCard = Color(0xFF1C140D)
    val surfaceRaised = Color(0xFF241A10)
    val surfaceHighest = Color(0xFF2E2113)
    val border = Color(0xFF3D2E1C)
    val text = Color(0xFFF7EFE3)
    val textMuted = Color(0xFFC9B9A3)
    val accent = Color(0xFFF5A623)
    val accentDim = Color(0xFFB87A1C)
    val onAccent = Color(0xFF0B0806)
    val accentContainer = Color(0xFF3A2A12)
    val onAccentContainer = Color(0xFFFFD79A)
    val warning = Color(0xFFFF8C1A)
    val danger = Color(0xFFFF453A)
    val info = Color(0xFF5BA8F5)
}

/** The Ember palette mapped onto Wear Compose Material3's colour roles. */
private val wearEmberColorScheme: ColorScheme by lazy {
    ColorScheme(
        primary = WearEmberPalette.accent,
        primaryDim = WearEmberPalette.accentDim,
        primaryContainer = WearEmberPalette.accentContainer,
        onPrimary = WearEmberPalette.onAccent,
        onPrimaryContainer = WearEmberPalette.onAccentContainer,
        secondary = WearEmberPalette.accentDim,
        secondaryDim = WearEmberPalette.accentDim,
        secondaryContainer = WearEmberPalette.surfaceRaised,
        onSecondary = WearEmberPalette.onAccent,
        onSecondaryContainer = WearEmberPalette.text,
        tertiary = WearEmberPalette.info,
        tertiaryDim = WearEmberPalette.info,
        tertiaryContainer = WearEmberPalette.surfaceRaised,
        onTertiary = WearEmberPalette.canvas,
        onTertiaryContainer = WearEmberPalette.text,
        surfaceContainerLow = WearEmberPalette.surface,
        surfaceContainer = WearEmberPalette.surfaceCard,
        surfaceContainerHigh = WearEmberPalette.surfaceRaised,
        onSurface = WearEmberPalette.text,
        onSurfaceVariant = WearEmberPalette.textMuted,
        outline = WearEmberPalette.border,
        outlineVariant = WearEmberPalette.border.copy(alpha = 0.7f),
        background = WearEmberPalette.canvas,
        onBackground = WearEmberPalette.text,
        error = WearEmberPalette.danger,
        errorDim = WearEmberPalette.danger,
        errorContainer = WearEmberPalette.danger.copy(alpha = 0.22f),
        onError = WearEmberPalette.canvas,
        onErrorContainer = WearEmberPalette.danger,
    )
}

/**
 * Wear Compose typography for the "mono for data" aesthetic (see `core/ui/.../theme/Type.kt`):
 * numeric readouts (distance, speed, duration) use a monospaced family so digits stay tabular on
 * the small round display. Built from Wear Compose's default [Typography] so every role Mileway's
 * screens don't explicitly override (arc labels, button copy, etc.) keeps its Wear-tuned metrics.
 */
private val wearMilewayTypography: Typography by lazy {
    val default = Typography()
    default.copy(
        displayLarge = default.displayLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
        displayMedium = default.displayMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
        displaySmall = default.displaySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
        titleLarge = default.titleLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
        titleMedium = default.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium),
        titleSmall = default.titleSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium),
        numeralExtraLarge = default.numeralExtraLarge.copy(fontFamily = FontFamily.Monospace),
        numeralLarge = default.numeralLarge.copy(fontFamily = FontFamily.Monospace),
        numeralMedium = default.numeralMedium.copy(fontFamily = FontFamily.Monospace),
        numeralSmall = default.numeralSmall.copy(fontFamily = FontFamily.Monospace),
        numeralExtraSmall = default.numeralExtraSmall.copy(fontFamily = FontFamily.Monospace),
    )
}

/**
 * Wraps [content] in Wear Compose Material3's [MaterialTheme] using the Ember palette above.
 * Every Wear screen/tile-preview/complication-preview roots itself here instead of the bare
 * [MaterialTheme] default, so the watch surfaces match the phone's signature amber+red look.
 */
@Composable
fun WearMilewayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = wearEmberColorScheme,
        typography = wearMilewayTypography,
        content = content,
    )
}

/**
 * Acceptance for P2.3: a preview renders the Ember palette on a round-watch preview. Visual-only —
 * this task is explicitly unit-free (see PLAN_V23 P2.3 acceptance).
 *
 * Devices.WEAR_OS_SMALL_ROUND is deprecated in favour of androidx.wear:wear-tooling-preview's
 * WearDevices.SMALL_ROUND — not worth a new dependency for a preview-only device string constant
 * (CLAUDE.md: no dependencies beyond what a task genuinely needs); the deprecated constant still
 * resolves correctly.
 * UnusedPrivateMember: only called by the Compose/Android Studio preview renderer via
 * reflection on the @Preview annotation, never from Kotlin call sites — a known detekt false
 * positive for private @Preview composables (no detekt-compose ruleset in this repo to exempt it).
 */
@Suppress("DEPRECATION", "UnusedPrivateMember")
@Preview(name = "Wear Ember theme", device = Devices.WEAR_OS_SMALL_ROUND, showBackground = true)
@Composable
private fun WearMilewayThemePreview() {
    WearMilewayTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Mileway",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}
