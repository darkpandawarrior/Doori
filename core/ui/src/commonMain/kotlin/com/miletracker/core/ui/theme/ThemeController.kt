package com.miletracker.core.ui.theme

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Selectable seed-colour presets. The whole Material colour scheme is generated from the
 * chosen seed (see [MileTrackerTheme]); these are just convenient starting points — a fully
 * custom seed can be picked with the colour wheel and is stored in [ThemeController.customSeedHex].
 */
enum class AccentPalette(val label: String, val seedHex: String) {
    DEFAULT("Default", ThemeDefaults.BASE_COLOR),
    TEAL("Teal", "#00897B"),
    INDIGO("Indigo", "#3F51B5"),
    AMBER("Amber", "#FFB300"),
}

/**
 * BCP-47 locale tag for supported demo languages.
 * Full string translation is out of scope for the demo; the mechanism (AppCompatDelegate
 * per-app locales) is wired and a couple of UI strings use localized overrides.
 */
enum class AppLanguage(val tag: String, val displayName: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिन्दी (Hindi)"),
}

/**
 * Three experimental optimization toggles.
 * - [batteryAwareTracking]: defers location polling when battery < 15% (real runtime
 *   effect wired in tracking service; no-op when battery is healthy).
 * - [lowEndDeviceTuning]: reduces recomposition frequency and drops some UI animations
 *   (cosmetic in demo — real tuning would require Baseline Profile gating).
 * - [aggressiveGpsFilter]: tightens the spike-rejection radius from 80 m to 40 m
 *   (real effect on the LocationProcessor filter; marked for demo because actual
 *   field impact requires extended trips to observe).
 */
data class ExperimentalFlags(
    val batteryAwareTracking: Boolean = false,
    val lowEndDeviceTuning: Boolean = false,
    val aggressiveGpsFilter: Boolean = false,
)

/** DataStore keys for persisted theme preferences. */
object ThemePreferenceKeys {
    val USE_DARK_THEME = booleanPreferencesKey("use_dark_theme")
    val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
    val CUSTOM_THEME = stringPreferencesKey("custom_theme")
    val USE_SYSTEM_COLORS = booleanPreferencesKey("use_system_colors")
    val PALETTE_STYLE = stringPreferencesKey("palette_style")
    val THEME_VARIANT = stringPreferencesKey("theme_variant")
    val MAP_PROVIDER = stringPreferencesKey("map_provider")
    val LANGUAGE = stringPreferencesKey("language")
}

/**
 * App-wide theme state holder.
 *
 * [MileTrackerTheme] defaults to following the system setting; this controller lets the
 * Settings screen override dark mode, seed colour (preset or fully custom), palette style,
 * system dynamic colours, and language at runtime. A `null` dark-mode value means "follow
 * the system". Held as a Koin singleton so the shell (reader) and Settings (writer) share
 * one instance.
 *
 * When constructed with a [DataStore], every value is persisted and restored across process
 * death; with `null` (unit tests) it is purely in-memory.
 */
class ThemeController(
    private val prefs: DataStore<Preferences>? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _darkThemeOverride = MutableStateFlow<Boolean?>(null)
    private val _accentPalette = MutableStateFlow(AccentPalette.DEFAULT)
    private val _customSeedHex = MutableStateFlow(ThemeDefaults.CUSTOM_THEME)
    private val _useSystemColors = MutableStateFlow(ThemeDefaults.USE_SYSTEM_COLORS)
    private val _paletteStyle = MutableStateFlow(ThemeDefaults.PALETTE_STYLE)
    private val _themeVariant = MutableStateFlow(ThemeDefaults.THEME_VARIANT)
    private val _mapProvider = MutableStateFlow(ThemeDefaults.MAP_PROVIDER)
    private val _language = MutableStateFlow(AppLanguage.ENGLISH)
    private val _experimentalFlags = MutableStateFlow(ExperimentalFlags())

    /** `null` = follow system, `true` = force dark, `false` = force light. */
    val darkThemeOverride: StateFlow<Boolean?> = _darkThemeOverride.asStateFlow()

    /** Currently active seed-colour preset. */
    val accentPalette: StateFlow<AccentPalette> = _accentPalette.asStateFlow()

    /** Custom seed colour hex (e.g. "#A1B2C3"). Blank = use the preset's seed. */
    val customSeedHex: StateFlow<String> = _customSeedHex.asStateFlow()

    /** Use Android 12+ wallpaper-derived dynamic colours instead of the seed scheme. */
    val useSystemColors: StateFlow<Boolean> = _useSystemColors.asStateFlow()

    /** MaterialKolor palette-style name (see [PaletteStyleNames]). */
    val paletteStyle: StateFlow<String> = _paletteStyle.asStateFlow()

    /** Theme variant tag ("DEFAULT"; reserved for special variants like debug skins). */
    val themeVariant: StateFlow<String> = _themeVariant.asStateFlow()

    /** Map tile provider name ("OSM" — the demo's single provider). */
    val mapProvider: StateFlow<String> = _mapProvider.asStateFlow()

    /** Currently selected app language. */
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    /** Experimental optimization flags. */
    val experimentalFlags: StateFlow<ExperimentalFlags> = _experimentalFlags.asStateFlow()

    init {
        prefs?.let { store ->
            scope.launch {
                // A corrupt or unavailable store must never crash the app — fall back to defaults.
                val snap =
                    try {
                        store.data.first()
                    } catch (_: Throwable) {
                        return@launch
                    }
                snap[ThemePreferenceKeys.USE_DARK_THEME]?.let { _darkThemeOverride.value = it }
                snap[ThemePreferenceKeys.ACCENT_PALETTE]?.let { name ->
                    AccentPalette.entries.firstOrNull { it.name == name }
                        ?.let { _accentPalette.value = it }
                }
                snap[ThemePreferenceKeys.CUSTOM_THEME]?.let { _customSeedHex.value = it }
                snap[ThemePreferenceKeys.USE_SYSTEM_COLORS]?.let { _useSystemColors.value = it }
                snap[ThemePreferenceKeys.PALETTE_STYLE]?.let { _paletteStyle.value = it }
                snap[ThemePreferenceKeys.THEME_VARIANT]?.let { _themeVariant.value = it }
                snap[ThemePreferenceKeys.MAP_PROVIDER]?.let { _mapProvider.value = it }
                snap[ThemePreferenceKeys.LANGUAGE]?.let { tag ->
                    AppLanguage.entries.firstOrNull { it.tag == tag }
                        ?.let { _language.value = it }
                }
            }
        }
    }

    private fun persist(block: (MutablePreferences) -> Unit) {
        prefs?.let { store ->
            scope.launch {
                try {
                    store.edit { block(it) }
                } catch (_: Throwable) {
                    // Persistence is best-effort; the in-memory state is already updated.
                }
            }
        }
    }

    fun set(dark: Boolean?) {
        _darkThemeOverride.value = dark
        persist { p ->
            if (dark == null) {
                p.remove(ThemePreferenceKeys.USE_DARK_THEME)
            } else {
                p[ThemePreferenceKeys.USE_DARK_THEME] = dark
            }
        }
    }

    /** Flip between dark and light, resolving the current effective value against [systemDark]. */
    fun toggle(systemDark: Boolean) {
        val current = _darkThemeOverride.value ?: systemDark
        set(!current)
    }

    /** Selecting a preset clears any custom seed so the preset visibly takes effect. */
    fun setPalette(palette: AccentPalette) {
        _accentPalette.value = palette
        _customSeedHex.value = ""
        persist { p ->
            p[ThemePreferenceKeys.ACCENT_PALETTE] = palette.name
            p[ThemePreferenceKeys.CUSTOM_THEME] = ""
        }
    }

    /** A blank [hex] reverts to the preset seed. Malformed values are ignored. */
    fun setCustomSeed(hex: String) {
        if (hex.isNotBlank() && parseHexColor(hex) == null) return
        _customSeedHex.value = hex
        persist { p -> p[ThemePreferenceKeys.CUSTOM_THEME] = hex }
    }

    fun setUseSystemColors(enabled: Boolean) {
        _useSystemColors.value = enabled
        persist { p -> p[ThemePreferenceKeys.USE_SYSTEM_COLORS] = enabled }
    }

    /** Unknown style names are accepted here and resolved tolerantly by the theme. */
    fun setPaletteStyle(style: String) {
        _paletteStyle.value = style
        persist { p -> p[ThemePreferenceKeys.PALETTE_STYLE] = style }
    }

    fun setThemeVariant(variant: String) {
        _themeVariant.value = variant
        persist { p -> p[ThemePreferenceKeys.THEME_VARIANT] = variant }
    }

    fun setMapProvider(provider: String) {
        _mapProvider.value = provider
        persist { p -> p[ThemePreferenceKeys.MAP_PROVIDER] = provider }
    }

    fun setLanguage(language: AppLanguage) {
        _language.value = language
        persist { p -> p[ThemePreferenceKeys.LANGUAGE] = language.tag }
    }

    fun updateExperimentalFlags(flags: ExperimentalFlags) {
        _experimentalFlags.value = flags
    }

    fun resetCustomization() {
        _accentPalette.value = AccentPalette.DEFAULT
        _customSeedHex.value = ThemeDefaults.CUSTOM_THEME
        _useSystemColors.value = ThemeDefaults.USE_SYSTEM_COLORS
        _paletteStyle.value = ThemeDefaults.PALETTE_STYLE
        _themeVariant.value = ThemeDefaults.THEME_VARIANT
        _mapProvider.value = ThemeDefaults.MAP_PROVIDER
        _language.value = AppLanguage.ENGLISH
        _experimentalFlags.value = ExperimentalFlags()
        persist { p ->
            p[ThemePreferenceKeys.ACCENT_PALETTE] = AccentPalette.DEFAULT.name
            p[ThemePreferenceKeys.CUSTOM_THEME] = ThemeDefaults.CUSTOM_THEME
            p[ThemePreferenceKeys.USE_SYSTEM_COLORS] = ThemeDefaults.USE_SYSTEM_COLORS
            p[ThemePreferenceKeys.PALETTE_STYLE] = ThemeDefaults.PALETTE_STYLE
            p[ThemePreferenceKeys.THEME_VARIANT] = ThemeDefaults.THEME_VARIANT
            p[ThemePreferenceKeys.MAP_PROVIDER] = ThemeDefaults.MAP_PROVIDER
            p[ThemePreferenceKeys.LANGUAGE] = AppLanguage.ENGLISH.tag
        }
    }
}
