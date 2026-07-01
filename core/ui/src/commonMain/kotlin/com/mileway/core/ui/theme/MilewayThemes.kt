package com.mileway.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Design Language v2 — the four curated, hand-tuned theme schemes.
 *
 * Unlike [AccentPalette] (a single MaterialKolor seed the whole scheme is *generated* from),
 * a [MilewayThemeVariant] ships a **fully hand-tuned [ColorScheme]**: every surface, accent,
 * on-colour and container triplet is set by hand and verified for WCAG-AA contrast — no
 * auto-generated tonal pairs. The seed is retained only so MaterialKolor can derive the few
 * tones we don't pin (used for ripples / tertiary surfaces) and so future per-feature accent
 * tints can layer on with no refactor.
 *
 * Geometry (square-rounded radii), the mono-for-data typography, and edge-to-edge behaviour are
 * theme-independent — they live in [DesignTokens] / [Type] and apply identically across all four.
 *
 * Architecture note (extensibility): each scheme is described by a small [MilewaySchemeSpec]
 * token bundle. Under-themes (driving mode, focus mode, per-feature accent tints) are expected
 * to be expressed later as a *transform* over a base [MilewaySchemeSpec] — e.g. swapping the
 * accent ramp or dimming glow — without touching the curated bases here. That follow-up is
 * deliberately NOT built yet.
 */
enum class MilewayThemeVariant(
    val id: String,
    val label: String,
    val description: String,
    /** True for the single light-first scheme; false for the three dark schemes. */
    val isLight: Boolean,
    /** MaterialKolor seed retained for derived tones / future per-feature tints. */
    val seedHex: String,
    val spec: MilewaySchemeSpec,
) {
    /** Default. Deep-dark phosphor-green terminal. */
    MATRIX(
        id = "MATRIX",
        label = "Matrix",
        description = "Deep-dark, phosphor green. The Mileway signature.",
        isLight = false,
        seedHex = "#00FF41",
        spec = MatrixSpec,
    ),

    /** True-black OLED variant — same phosphor accent, maximum contrast, battery-friendly. */
    AMOLED(
        id = "AMOLED",
        label = "Amoled",
        description = "True-black OLED. Maximum contrast, battery-friendly.",
        isLight = false,
        seedHex = "#00FF41",
        spec = AmoledSpec,
    ),

    /** Cooler cyan-on-dark variant for a data/AI feel. */
    ION(
        id = "ION",
        label = "Ion",
        description = "Cooler cyan-on-dark. A crisp, analytical feel.",
        isLight = false,
        seedHex = "#42E8E0",
        spec = IonSpec,
    ),

    /** First-class light theme — elevation-by-tint, deeper green for AA on white. */
    DAYBREAK(
        id = "DAYBREAK",
        label = "Daybreak",
        description = "Bright, airy light theme. Deep green for daylight legibility.",
        isLight = true,
        seedHex = "#2BB86C",
        spec = DaybreakSpec,
    ),
    ;

    /** The fully hand-tuned Material 3 [ColorScheme] for this theme. */
    fun colorScheme(): ColorScheme = spec.toColorScheme(isLight)

    companion object {
        val DEFAULT = MATRIX

        /** Tolerant lookup by persisted id; falls back to [DEFAULT] for unknown / legacy values. */
        fun fromId(id: String?): MilewayThemeVariant = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/**
 * The full per-theme token bundle. Every colour the curated schemes need is named here, so a
 * scheme is described declaratively in one place and assembled into a Material [ColorScheme] by
 * [toColorScheme]. Semantic state colours (warning/danger/info/success) are carried alongside the
 * Material roles and exposed app-wide via [LocalMilewaySemanticColors].
 */
data class MilewaySchemeSpec(
    // ── Core canvas / surface ramp (elevation by lightness in dark; by tint in light) ──
    val canvas: Color,
    val surface: Color,
    val surfaceCard: Color,
    val surfaceRaised: Color,
    val surfaceHighest: Color,
    val border: Color,
    // ── Text ──
    val text: Color,
    val textMuted: Color,
    // ── Accent ramp ──
    val accent: Color,
    val accentDim: Color,
    val accentGlow: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    // ── Semantic states (AA on this theme's canvas) ──
    val warning: Color,
    val danger: Color,
    val info: Color,
    val success: Color,
    /** Whether raised surfaces carry the +1px top-edge highlight & glow (dark only). */
    val useGlow: Boolean,
) {
    fun semanticColors(): MilewaySemanticColors =
        MilewaySemanticColors(
            warning = warning,
            danger = danger,
            info = info,
            success = success,
            accentGlow = accentGlow,
            accentDim = accentDim,
            border = border,
            surfaceRaised = surfaceRaised,
            surfaceHighest = surfaceHighest,
            useGlow = useGlow,
        )

    fun toColorScheme(isLight: Boolean): ColorScheme {
        val base = if (isLight) lightColorScheme() else darkColorScheme()
        return base.copy(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accentContainer,
            onPrimaryContainer = onAccentContainer,
            inversePrimary = accentDim,
            secondary = accentDim,
            onSecondary = onAccent,
            secondaryContainer = surfaceRaised,
            onSecondaryContainer = text,
            tertiary = info,
            onTertiary = if (isLight) Color.White else canvas,
            tertiaryContainer = surfaceRaised,
            onTertiaryContainer = text,
            background = canvas,
            onBackground = text,
            surface = surface,
            onSurface = text,
            surfaceVariant = surfaceRaised,
            onSurfaceVariant = textMuted,
            surfaceTint = accent,
            surfaceBright = surfaceHighest,
            surfaceDim = canvas,
            surfaceContainerLowest = canvas,
            surfaceContainerLow = surface,
            surfaceContainer = surfaceCard,
            surfaceContainerHigh = surfaceRaised,
            surfaceContainerHighest = surfaceHighest,
            inverseSurface = text,
            inverseOnSurface = canvas,
            outline = border,
            outlineVariant = border.copy(alpha = if (isLight) 0.6f else 0.7f),
            error = danger,
            onError = if (isLight) Color.White else canvas,
            errorContainer = danger.copy(alpha = if (isLight) 0.16f else 0.22f),
            onErrorContainer = if (isLight) danger else danger,
            scrim = Color(0xCC000000),
        )
    }
}

// =============================================================================
// Curated specs — hand-tuned, AA-verified. Move off purple/indigo entirely.
// =============================================================================

/** Matrix (default): deep phosphor-green terminal. canvas #010701, accent #00FF41. */
internal val MatrixSpec =
    MilewaySchemeSpec(
        canvas = Color(0xFF010701),
        surface = Color(0xFF040C06),
        surfaceCard = Color(0xFF080F0A),
        surfaceRaised = Color(0xFF0C1510),
        surfaceHighest = Color(0xFF111C14),
        border = Color(0xFF1C3522),
        text = Color(0xFFB8FFCC),
        textMuted = Color(0xFF3A6645),
        accent = Color(0xFF00FF41),
        accentDim = Color(0xFF00CC34),
        accentGlow = Color(0xFF39FF14),
        onAccent = Color(0xFF000000),
        accentContainer = Color(0xFF00280E),
        onAccentContainer = Color(0xFF7FFFAA),
        warning = Color(0xFFFFCC00),
        danger = Color(0xFFFF4455),
        info = Color(0xFF33AAFF),
        success = Color(0xFF00FF41),
        useGlow = true,
    )

/** Amoled: absolute-black canvas, same phosphor accent as Matrix. */
internal val AmoledSpec =
    MatrixSpec.copy(
        canvas = Color(0xFF000000),
        surface = Color(0xFF050505),
        surfaceCard = Color(0xFF0A0A0A),
        surfaceRaised = Color(0xFF0E0E0E),
        surfaceHighest = Color(0xFF141414),
        border = Color(0xFF1A2E1D),
        text = Color(0xFFCCFFDD),
        textMuted = Color(0xFF3A5A42),
        accentContainer = Color(0xFF001C08),
    )

/** Ion: cyan-on-dark. canvas #06100F, accent #42E8E0. */
internal val IonSpec =
    MilewaySchemeSpec(
        canvas = Color(0xFF06100F),
        surface = Color(0xFF0A1614),
        surfaceCard = Color(0xFF0F1E1C),
        surfaceRaised = Color(0xFF152825),
        surfaceHighest = Color(0xFF1C322F),
        border = Color(0xFF1F3330),
        text = Color(0xFFE3F2F0),
        textMuted = Color(0xFF8FA8A4),
        accent = Color(0xFF42E8E0),
        accentDim = Color(0xFF2BB6B0),
        accentGlow = Color(0xFF6FFAF3),
        onAccent = Color(0xFF002320),
        accentContainer = Color(0xFF0E3330),
        onAccentContainer = Color(0xFF93F7F1),
        warning = Color(0xFFF2C14E),
        danger = Color(0xFFF2545B),
        info = Color(0xFF5BA8F5),
        success = Color(0xFF3DDC84),
        useGlow = true,
    )

/** Daybreak (light, first-class): canvas #F4F7F4, accent #2BB86C (AA on white). */
internal val DaybreakSpec =
    MilewaySchemeSpec(
        canvas = Color(0xFFF4F7F4),
        surface = Color(0xFFFFFFFF),
        surfaceCard = Color(0xFFFFFFFF),
        surfaceRaised = Color(0xFFEDF2EE),
        surfaceHighest = Color(0xFFE4ECE6),
        border = Color(0xFFE2E8E2),
        text = Color(0xFF0B1410),
        textMuted = Color(0xFF5A6B61),
        accent = Color(0xFF2BB86C),
        accentDim = Color(0xFF1C8F52),
        accentGlow = Color(0xFF35C977),
        onAccent = Color(0xFFFFFFFF),
        accentContainer = Color(0xFFCFF0DD),
        onAccentContainer = Color(0xFF0A3A22),
        warning = Color(0xFFB8860B),
        danger = Color(0xFFC62F3B),
        info = Color(0xFF1C6FD6),
        success = Color(0xFF1C8F52),
        useGlow = false,
    )
