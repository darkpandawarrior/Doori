package com.mileway

import com.mileway.core.ui.theme.AccentPalette
import com.mileway.core.ui.theme.AppLanguage
import com.mileway.core.ui.theme.ExperimentalFlags
import com.mileway.core.ui.theme.MilewayTheme
import com.mileway.core.ui.theme.MilewayThemeVariant
import com.mileway.core.ui.theme.ThemeController
import com.mileway.core.ui.theme.ThemeDefaults
import com.mileway.core.ui.theme.parseHexColor
import com.mileway.feature.profile.repository.FakeProfileRepository
import com.mileway.feature.profile.viewmodel.ProfileViewModel
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the theme/locale customization and settings logic.
 *
 * All tests are pure JVM, no Android framework, no Koin, no Compose.
 * Each test is isolated via a fresh [ThemeController] + [ProfileViewModel] pair.
 */
class CustomizationSettingsTest {

    private fun controller() = ThemeController()
    private fun viewModel(tc: ThemeController = controller()) =
        ProfileViewModel(FakeProfileRepository(), tc)

    // =========================================================================
    // 1. Palette selection, mapping to expected color set
    // =========================================================================

    @Test
    fun `default palette starts as DEFAULT`() {
        val tc = controller()
        assertEquals(AccentPalette.DEFAULT, tc.accentPalette.value)
    }

    @Test
    fun `setPalette changes to TEAL`() {
        val tc = controller()
        tc.setPalette(AccentPalette.TEAL)
        assertEquals(AccentPalette.TEAL, tc.accentPalette.value)
    }

    @Test
    fun `setPalette changes to INDIGO`() {
        val tc = controller()
        tc.setPalette(AccentPalette.INDIGO)
        assertEquals(AccentPalette.INDIGO, tc.accentPalette.value)
    }

    @Test
    fun `setPalette changes to AMBER`() {
        val tc = controller()
        tc.setPalette(AccentPalette.AMBER)
        assertEquals(AccentPalette.AMBER, tc.accentPalette.value)
    }

    @Test
    fun `each palette maps to a distinct seed color`() {
        val seeds = AccentPalette.entries.map { it.seedHex }
        assertEquals(seeds.size, seeds.toSet().size,
            "Each AccentPalette must map to a unique seed color")
    }

    @Test
    fun `every preset seed is a parseable hex color`() {
        AccentPalette.entries.forEach { palette ->
            assertTrue(parseHexColor(palette.seedHex) != null,
                "Seed for ${palette.name} must parse: ${palette.seedHex}")
        }
    }

    @Test
    fun `default preset uses the canonical base seed`() {
        assertEquals(ThemeDefaults.BASE_COLOR, AccentPalette.DEFAULT.seedHex)
        assertEquals("#6367FA", ThemeDefaults.BASE_COLOR)
    }

    @Test
    fun `custom seed overrides until cleared and presets clear it`() {
        val tc = controller()
        tc.setCustomSeed("#A1B2C3")
        assertEquals("#A1B2C3", tc.customSeedHex.value)
        // Picking a preset visibly takes effect by clearing the custom seed
        tc.setPalette(AccentPalette.TEAL)
        assertEquals("", tc.customSeedHex.value)
    }

    @Test
    fun `malformed custom seed is rejected`() {
        val tc = controller()
        tc.setCustomSeed("#A1B2C3")
        tc.setCustomSeed("not-a-color")
        assertEquals("#A1B2C3", tc.customSeedHex.value)
    }

    @Test
    fun `parseHexColor handles rgb and argb and rejects garbage`() {
        assertTrue(parseHexColor("#6367FA") != null)
        assertTrue(parseHexColor("#FF6367FA") != null)
        assertTrue(parseHexColor("6367FA") != null)
        assertEquals(null, parseHexColor(""))
        assertEquals(null, parseHexColor("#12345"))
        assertEquals(null, parseHexColor("#ZZZZZZ"))
    }

    @Test
    fun `theme settings start at source defaults`() {
        val tc = controller()
        assertEquals(ThemeDefaults.CUSTOM_THEME, tc.customSeedHex.value)
        assertEquals(ThemeDefaults.USE_SYSTEM_COLORS, tc.useSystemColors.value)
        assertEquals(ThemeDefaults.PALETTE_STYLE, tc.paletteStyle.value)
        assertEquals(ThemeDefaults.THEME_VARIANT, tc.themeVariant.value)
        assertEquals(ThemeDefaults.MAP_PROVIDER, tc.mapProvider.value)
        assertEquals("TonalSpot", ThemeDefaults.PALETTE_STYLE)
    }

    @Test
    fun `system colors and palette style round-trip through the controller`() {
        val tc = controller()
        tc.setUseSystemColors(true)
        tc.setPaletteStyle("Vibrant")
        assertTrue(tc.useSystemColors.value)
        assertEquals("Vibrant", tc.paletteStyle.value)
    }

    @Test
    fun `reset restores seed and generator settings to defaults`() {
        val tc = controller()
        tc.setPalette(AccentPalette.AMBER)
        tc.setCustomSeed("#123456")
        tc.setUseSystemColors(true)
        tc.setPaletteStyle("Monochrome")
        tc.resetCustomization()
        assertEquals(AccentPalette.DEFAULT, tc.accentPalette.value)
        assertEquals("", tc.customSeedHex.value)
        assertFalse(tc.useSystemColors.value)
        assertEquals("TonalSpot", tc.paletteStyle.value)
    }

    @Test
    fun `ViewModel exposes palette from ThemeController`() {
        val tc = controller()
        val vm = viewModel(tc)
        tc.setPalette(AccentPalette.INDIGO)
        assertEquals(AccentPalette.INDIGO, vm.accentPalette.value)
    }

    @Test
    fun `ViewModel setPalette delegates to ThemeController`() {
        val tc = controller()
        val vm = viewModel(tc)
        vm.setPalette(AccentPalette.TEAL)
        assertEquals(AccentPalette.TEAL, tc.accentPalette.value)
    }

    @Test
    fun `palette label is non-blank for all entries`() {
        AccentPalette.entries.forEach { palette ->
            assertTrue(palette.label.isNotBlank(), "Palette ${palette.name} must have a non-blank label")
        }
    }

    // =========================================================================
    // 2. Settings state reducer, experimental flag toggles
    // =========================================================================

    @Test
    fun `experimental flags start all false`() {
        val flags = ExperimentalFlags()
        assertFalse(flags.batteryAwareTracking)
        assertFalse(flags.lowEndDeviceTuning)
        assertFalse(flags.aggressiveGpsFilter)
    }

    @Test
    fun `toggleBatteryAwareTracking flips the flag`() {
        val tc = controller()
        val vm = viewModel(tc)
        assertFalse(vm.experimentalFlags.value.batteryAwareTracking)
        vm.toggleBatteryAwareTracking()
        assertTrue(vm.experimentalFlags.value.batteryAwareTracking)
        vm.toggleBatteryAwareTracking()
        assertFalse(vm.experimentalFlags.value.batteryAwareTracking)
    }

    @Test
    fun `toggleLowEndDeviceTuning flips only that flag`() {
        val tc = controller()
        val vm = viewModel(tc)
        vm.toggleBatteryAwareTracking() // set battery flag true
        vm.toggleLowEndDeviceTuning()
        assertTrue(vm.experimentalFlags.value.lowEndDeviceTuning)
        // battery flag should be unaffected
        assertTrue(vm.experimentalFlags.value.batteryAwareTracking)
    }

    @Test
    fun `toggleAggressiveGpsFilter flips only that flag`() {
        val tc = controller()
        val vm = viewModel(tc)
        vm.toggleAggressiveGpsFilter()
        assertTrue(vm.experimentalFlags.value.aggressiveGpsFilter)
        assertFalse(vm.experimentalFlags.value.batteryAwareTracking)
        assertFalse(vm.experimentalFlags.value.lowEndDeviceTuning)
    }

    @Test
    fun `all three experimental flags can be enabled independently`() {
        val tc = controller()
        val vm = viewModel(tc)
        vm.toggleBatteryAwareTracking()
        vm.toggleLowEndDeviceTuning()
        vm.toggleAggressiveGpsFilter()
        val flags = vm.experimentalFlags.value
        assertTrue(flags.batteryAwareTracking)
        assertTrue(flags.lowEndDeviceTuning)
        assertTrue(flags.aggressiveGpsFilter)
    }

    @Test
    fun `updateExperimentalFlags replaces all flags atomically`() {
        val tc = controller()
        val vm = viewModel(tc)
        vm.toggleBatteryAwareTracking()
        tc.updateExperimentalFlags(ExperimentalFlags(aggressiveGpsFilter = true))
        val flags = vm.experimentalFlags.value
        assertFalse(flags.batteryAwareTracking, "Battery flag should be replaced")
        assertTrue(flags.aggressiveGpsFilter)
    }

    // =========================================================================
    // 3. Locale tag validation
    // =========================================================================

    @Test
    fun `ENGLISH language tag is 'en'`() {
        assertEquals("en", AppLanguage.ENGLISH.tag)
    }

    @Test
    fun `HINDI language tag is 'hi'`() {
        assertEquals("hi", AppLanguage.HINDI.tag)
    }

    @Test
    fun `all language tags are non-blank and lowercase BCP-47`() {
        AppLanguage.entries.forEach { lang ->
            assertTrue(lang.tag.isNotBlank(), "${lang.name}: tag must be non-blank")
            assertEquals(lang.tag, lang.tag.lowercase(), "${lang.name}: tag should be lowercase")
        }
    }

    @Test
    fun `all language displayNames are non-blank`() {
        AppLanguage.entries.forEach { lang ->
            assertTrue(lang.displayName.isNotBlank(), "${lang.name}: displayName must be non-blank")
        }
    }

    @Test
    fun `language tags are unique across all entries`() {
        val tags = AppLanguage.entries.map { it.tag }
        assertEquals(tags.size, tags.toSet().size, "All language tags must be distinct")
    }

    @Test
    fun `default language is ENGLISH`() {
        val tc = controller()
        assertEquals(AppLanguage.ENGLISH, tc.language.value)
    }

    @Test
    fun `setLanguage changes to HINDI`() {
        val tc = controller()
        tc.setLanguage(AppLanguage.HINDI)
        assertEquals(AppLanguage.HINDI, tc.language.value)
    }

    @Test
    fun `ViewModel setLanguage delegates to ThemeController`() {
        val tc = controller()
        val vm = viewModel(tc)
        vm.setLanguage(AppLanguage.HINDI)
        assertEquals(AppLanguage.HINDI, tc.language.value)
    }

    // =========================================================================
    // 4. Reset customization
    // =========================================================================

    @Test
    fun `resetCustomization restores palette to DEFAULT`() {
        val tc = controller()
        tc.setPalette(AccentPalette.AMBER)
        tc.resetCustomization()
        assertEquals(AccentPalette.DEFAULT, tc.accentPalette.value)
    }

    @Test
    fun `resetCustomization restores language to ENGLISH`() {
        val tc = controller()
        tc.setLanguage(AppLanguage.HINDI)
        tc.resetCustomization()
        assertEquals(AppLanguage.ENGLISH, tc.language.value)
    }

    @Test
    fun `resetCustomization clears all experimental flags`() {
        val tc = controller()
        tc.updateExperimentalFlags(ExperimentalFlags(batteryAwareTracking = true, aggressiveGpsFilter = true))
        tc.resetCustomization()
        assertEquals(ExperimentalFlags(), tc.experimentalFlags.value)
    }

    @Test
    fun `ViewModel resetCustomization delegates to ThemeController`() {
        val tc = controller()
        val vm = viewModel(tc)
        vm.setPalette(AccentPalette.INDIGO)
        vm.setLanguage(AppLanguage.HINDI)
        vm.toggleAggressiveGpsFilter()
        vm.resetCustomization()
        assertEquals(AccentPalette.DEFAULT, vm.accentPalette.value)
        assertEquals(AppLanguage.ENGLISH, vm.language.value)
        assertFalse(vm.experimentalFlags.value.aggressiveGpsFilter)
    }

    @Test
    fun `resetCustomization does not affect dark theme override`() {
        val tc = controller()
        tc.set(true)
        tc.resetCustomization()
        assertEquals(true, tc.darkThemeOverride.value,
            "Dark theme override is separate from customization reset")
    }

    // =========================================================================
    // 5. Design Language v2 — curated MilewayTheme picker
    // =========================================================================

    @Test
    fun `default curated theme is Matrix`() {
        val tc = controller()
        assertEquals(MilewayThemeVariant.MATRIX, tc.milewayTheme.value)
        assertEquals(MilewayThemeVariant.MATRIX, MilewayThemeVariant.DEFAULT)
    }

    @Test
    fun `setMilewayTheme round-trips through the controller`() {
        val tc = controller()
        tc.setMilewayTheme(MilewayThemeVariant.ION)
        assertEquals(MilewayThemeVariant.ION, tc.milewayTheme.value)
    }

    @Test
    fun `selecting a curated theme clears any custom seed so it applies`() {
        val tc = controller()
        tc.setCustomSeed("#A1B2C3")
        assertEquals("#A1B2C3", tc.customSeedHex.value)
        tc.setMilewayTheme(MilewayThemeVariant.AMOLED)
        // A custom seed otherwise wins in MilewayTheme; picking a theme must clear it.
        assertEquals(ThemeDefaults.CUSTOM_THEME, tc.customSeedHex.value)
    }

    @Test
    fun `ViewModel setMilewayTheme delegates to ThemeController`() {
        val tc = controller()
        val vm = viewModel(tc)
        vm.setMilewayTheme(MilewayThemeVariant.DAYBREAK)
        assertEquals(MilewayThemeVariant.DAYBREAK, tc.milewayTheme.value)
        assertEquals(MilewayThemeVariant.DAYBREAK, vm.milewayTheme.value)
    }

    @Test
    fun `fromId tolerates unknown and null ids by falling back to Matrix`() {
        assertEquals(MilewayThemeVariant.MATRIX, MilewayThemeVariant.fromId(null))
        assertEquals(MilewayThemeVariant.MATRIX, MilewayThemeVariant.fromId("LEGACY_UNKNOWN"))
        assertEquals(MilewayThemeVariant.ION, MilewayThemeVariant.fromId("ION"))
    }

    @Test
    fun `resetCustomization restores the curated theme to Matrix`() {
        val tc = controller()
        tc.setMilewayTheme(MilewayThemeVariant.DAYBREAK)
        tc.resetCustomization()
        assertEquals(MilewayThemeVariant.MATRIX, tc.milewayTheme.value)
    }

    @Test
    fun `every curated theme has a stable id, non-blank label and description`() {
        MilewayThemeVariant.entries.forEach { theme ->
            assertTrue(theme.id.isNotBlank(), "${theme.name}: id must be non-blank")
            assertTrue(theme.label.isNotBlank(), "${theme.name}: label must be non-blank")
            assertTrue(theme.description.isNotBlank(), "${theme.name}: description must be non-blank")
            assertEquals(theme, MilewayThemeVariant.fromId(theme.id),
                "${theme.name}: id must round-trip through fromId")
        }
    }

    @Test
    fun `curated theme ids are unique`() {
        val ids = MilewayThemeVariant.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "Each MilewayTheme must have a unique id")
    }

    @Test
    fun `only Daybreak is the light scheme`() {
        assertTrue(MilewayThemeVariant.DAYBREAK.isLight)
        assertFalse(MilewayThemeVariant.MATRIX.isLight)
        assertFalse(MilewayThemeVariant.AMOLED.isLight)
        assertFalse(MilewayThemeVariant.ION.isLight)
    }

    @Test
    fun `dark schemes use glow and the light scheme does not`() {
        assertTrue(MilewayThemeVariant.MATRIX.spec.useGlow)
        assertTrue(MilewayThemeVariant.AMOLED.spec.useGlow)
        assertTrue(MilewayThemeVariant.ION.spec.useGlow)
        assertFalse(MilewayThemeVariant.DAYBREAK.spec.useGlow,
            "Light scheme is shadow-free / elevation-by-tint, so no glow")
    }

    @Test
    fun `every curated theme builds a Material ColorScheme with its accent as primary`() {
        MilewayThemeVariant.entries.forEach { theme ->
            val scheme = theme.colorScheme()
            assertEquals(theme.spec.accent, scheme.primary,
                "${theme.name}: primary must be the hand-tuned accent")
            assertEquals(theme.spec.canvas, scheme.background,
                "${theme.name}: background must be the canvas token")
        }
    }

    @Test
    fun `Amoled canvas is true black and Daybreak canvas is light`() {
        // True-black OLED canvas; the light scheme's canvas is near-white.
        assertEquals(androidx.compose.ui.graphics.Color(0xFF000000), MilewayThemeVariant.AMOLED.spec.canvas)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFF4F7F4), MilewayThemeVariant.DAYBREAK.spec.canvas)
    }
}
