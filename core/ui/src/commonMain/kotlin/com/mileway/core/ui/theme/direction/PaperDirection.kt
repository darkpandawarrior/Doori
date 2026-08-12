package com.mileway.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Direction: **Paper**. Light-first — a claim is a document, not a terminal readout. Warm
 * off-white surfaces, strong dark ink text, colour reserved almost entirely for status; a real
 * (not inverted) dark counterpart, [PaperNightSpec], covers night driving.
 *
 * File ownership: this whole direction lives in this one file. [MilewayThemes.kt] gets exactly
 * one enum entry ([MilewayThemeVariant.PAPER]) referencing [PaperSpec] below — nothing else
 * changes there. Declared in the shared `com.mileway.core.ui.theme` package (despite living under
 * `theme/direction/`, a Kotlin package need not mirror its directory) so that one-line addition
 * needs no new import either.
 *
 * Registering only the light spec as the flagship keeps that MilewayThemes.kt edit to the single
 * entry the five-way merge needs — see [PaperNightSpec]'s doc for why the dark counterpart isn't
 * a second enum entry.
 */
internal val PaperSpec =
    MilewaySchemeSpec(
        canvas = Color(0xFFF7F3EA),
        surface = Color(0xFFFFFFFF),
        surfaceCard = Color(0xFFFFFFFF),
        surfaceRaised = Color(0xFFF1ECDF),
        surfaceHighest = Color(0xFFE8E1CC),
        border = Color(0xFFDDD3B8),
        text = Color(0xFF1F1B14),
        textMuted = Color(0xFF6E6353),
        accent = Color(0xFF1E3A5F),
        accentDim = Color(0xFF14293F),
        accentGlow = Color(0xFF2E4F7A),
        onAccent = Color(0xFFFFFFFF),
        accentContainer = Color(0xFFD9E3EE),
        onAccentContainer = Color(0xFF0F2338),
        warning = Color(0xFF8A5A0A),
        danger = Color(0xFFA3291E),
        info = Color(0xFF1D5FA8),
        success = Color(0xFF1B7A43),
        // A driving screen should never glare, day or night — Paper's depth is a real shadow
        // (see PaperElevation), not a light-emitting edge like Ember/Matrix/Ion.
        useGlow = false,
    )

/**
 * The night counterpart — hand-tuned separately, not an algorithmic inversion of [PaperSpec]:
 * darker warm canvas (not neutral black), lightened ink-blue for AA on dark, brighter-but-still-
 * muted status hues.
 *
 * Wired in on 2026-08-10 when Paper became the default, as `PAPER`'s `darkSpec` — not as a second
 * `PAPER_NIGHT` enum entry, which is what an earlier revision of this doc predicted. A second
 * entry would have put both faces in the theme picker as separate choices and let a stored
 * preference disagree with the device setting. One identity, two faces, resolved by
 * `MilewayThemeVariant.specFor(dark)`.
 */
internal val PaperNightSpec =
    MilewaySchemeSpec(
        canvas = Color(0xFF14120E),
        surface = Color(0xFF1C1912),
        surfaceCard = Color(0xFF1C1912),
        surfaceRaised = Color(0xFF241F16),
        surfaceHighest = Color(0xFF2E271A),
        border = Color(0xFF3A3324),
        text = Color(0xFFF1E9D8),
        textMuted = Color(0xFFAA9F89),
        accent = Color(0xFF8FB4E0),
        accentDim = Color(0xFF5F86B4),
        accentGlow = Color(0xFFAEC9EC),
        onAccent = Color(0xFF0C1B2C),
        accentContainer = Color(0xFF223349),
        onAccentContainer = Color(0xFFCBDEF2),
        warning = Color(0xFFE0A64B),
        danger = Color(0xFFE2685C),
        info = Color(0xFF6FA6DD),
        success = Color(0xFF5FBE84),
        useGlow = false,
    )

/**
 * [PaperNightSpec]'s own day face — the same night scheme brought up to daylight, hand-picked
 * channel by channel rather than inverted. An inversion of the night spec lands on flat neutral
 * greys with a cold blue cast, which is exactly the "dead screen" Paper exists to avoid; every
 * surface here keeps the ~45° warm hue the night canvas carries, just at paper luminance, and the
 * ink-blue accent is re-darkened for light rather than mirrored (the night accent `0xFF8FB4E0` at
 * light luminance would be a pastel with no authority).
 *
 * It is deliberately *not* a duplicate of [PaperSpec]: this is the warmer, deeper-parchment read of
 * the same identity — creamier canvas, a card that stays off-white rather than pure white, and
 * status hues that are the daylight counterparts of the night set's brighter-but-muted family. Same
 * direction, different time of day.
 *
 * Contrast, computed (WCAG 2.x relative luminance) against each surface it can sit on —
 * canvas / surface = surfaceCard / surfaceRaised / surfaceHighest:
 *
 * - `text` on them: **15.61 / 17.46 / 14.66 / 13.14** — AAA everywhere.
 * - `textMuted` (secondary body copy) : **5.99 / 6.70 / 5.63 / 5.04** — AA at every step, worst
 *   case still 5.04 on `surfaceHighest`, so muted labels stay legal even on the deepest chip.
 * - `accent` as text/icon: **9.53 / 10.66 / 8.95 / 8.02**.
 * - `warning` **5.76 / 6.44 / 5.41 / 4.85**, `danger` **6.27 / 7.01 / 5.89 / 5.28**,
 *   `info` **6.70 / 7.50 / 6.30 / 5.64**, `success` **5.47 / 6.12 / 5.14 / 4.61** — every status
 *   hue clears 4.5:1 as *text*, not merely as a fill, which is why they are this deep. The two
 *   tightest (`success` and `warning` on `surfaceHighest`) were darkened until they passed rather
 *   than shipped with a "decorative use only" note.
 * - `onAccent` on `accent` **10.93**, on `accentDim` **13.82**;
 *   `onAccentContainer` on `accentContainer` **11.93**.
 *
 * `border` (1.41:1 on canvas) is a non-text divider and is not held to 4.5:1 — it is a ruled line on
 * a page, and darkening it to text contrast would turn cards into boxes.
 */
internal val PaperNightSpecDay =
    MilewaySchemeSpec(
        canvas = Color(0xFFF5EFE1),
        surface = Color(0xFFFFFCF4),
        surfaceCard = Color(0xFFFFFCF4),
        surfaceRaised = Color(0xFFEFE8D8),
        surfaceHighest = Color(0xFFE6DCC6),
        border = Color(0xFFD6CBAF),
        text = Color(0xFF1A170F),
        textMuted = Color(0xFF63594A),
        accent = Color(0xFF1D3E63),
        accentDim = Color(0xFF142E4A),
        accentGlow = Color(0xFF33608F),
        onAccent = Color(0xFFFFFFFF),
        accentContainer = Color(0xFFD8E4F0),
        onAccentContainer = Color(0xFF10263C),
        warning = Color(0xFF855109),
        danger = Color(0xFFA32B1F),
        info = Color(0xFF17558F),
        success = Color(0xFF1A6E3F),
        // Same reason as both other Paper faces: depth is a shadow (PaperElevation), never a
        // light-emitting edge — and a glow edge on a light canvas is invisible anyway.
        useGlow = false,
    )

/**
 * Canonical name for Paper's dark face under the `<Direction>SpecNight` convention the other four
 * directions follow. It is an **alias**, not a second scheme, and that is deliberate.
 *
 * Paper is the direction the counterpart convention was derived *from* — [PaperNightSpec] above is
 * already the hand-tuned, non-inverted night face (warm near-black canvas rather than neutral grey,
 * ink-blue lightened for AA on dark, status hues brightened but kept muted), and it is already
 * registered as `MilewayThemeVariant.PAPER.darkSpec`. Authoring a second set of eighteen colours
 * here would give Paper two dark faces free to drift apart — the picker would still show one theme
 * while `PaperTheme()` and the variant registry rendered different nights. Pointing the new name at
 * the existing object makes that divergence impossible: registering either name registers the same
 * instance.
 *
 * Contrast, computed over [PaperNightSpec]'s actual values (sRGB relative luminance, WCAG 2.x);
 * every pair below clears AA 4.5:1 for body text, and the surface ramp clears AAA 7:1:
 *
 * - `text` #F1E9D8 on `canvas` 15.49:1 · `surface`/`surfaceCard` 14.52:1 · `surfaceRaised` 13.56:1 ·
 *   `surfaceHighest` 12.23:1 · `accentContainer` 10.62:1
 * - `textMuted` #AA9F89 on `canvas` 7.15:1 · `surface` 6.71:1 · `surfaceRaised` 6.26:1 ·
 *   `surfaceHighest` 5.65:1 — the worst case in the whole scheme and still above AA
 * - `accent` 8.16:1, `accentDim` 4.64:1, `accentGlow` 10.33:1 on `surface`
 * - `warning` 8.12:1, `success` 7.67:1, `info` 6.83:1, `danger` 5.32:1 on `surface`
 * - `onAccent` on `accent` 8.08:1 · `onAccentContainer` on `accentContainer` 9.32:1
 * - [PaperMoneyDark]: `value` on `surface` 9.21:1 · `onValue` on `value` 8.61:1 ·
 *   `onValueContainer` on `valueContainer` 10.07:1
 *
 * `border` #3A3324 sits at 1.40:1 against `surface` by design — it is a hairline separator, not
 * text, and is exempt from the 4.5:1 body-text rule (it also clears the 3:1 non-text threshold only
 * as a decorative divider, never as the sole indicator of a control's bounds; `useGlow = false`
 * means Paper's depth comes from a real shadow, see [PaperElevation]).
 */
internal val PaperSpecNight: MilewaySchemeSpec = PaperNightSpec

/**
 * The money/value semantic role this direction was asked to name explicitly. Deliberately its
 * own hue, not borrowed from [MilewaySchemeSpec.success] — a reimbursement figure is a fact to be
 * verified, not a "good news" checkmark, so the number and a success chip must never share a
 * colour signal next to each other. A deeper, more muted green than `success` reads as ledger ink
 * rather than a status badge.
 */
data class PaperMoneyColors(
    val value: Color,
    val onValue: Color,
    val valueContainer: Color,
    val onValueContainer: Color,
)

internal val PaperMoneyLight =
    PaperMoneyColors(
        value = Color(0xFF3B6E4E),
        onValue = Color(0xFFFFFFFF),
        valueContainer = Color(0xFFDCEADD),
        onValueContainer = Color(0xFF163823),
    )

internal val PaperMoneyDark =
    PaperMoneyColors(
        value = Color(0xFF8FC9A0),
        onValue = Color(0xFF0D2416),
        valueContainer = Color(0xFF1E3526),
        onValueContainer = Color(0xFFC8E9D1),
    )

/** Fallback mirrors [PaperMoneyLight] so a screen that forgets to wrap in [PaperTheme] still reads on-brand. */
val LocalPaperMoneyColors: ProvidableCompositionLocal<PaperMoneyColors> =
    staticCompositionLocalOf { PaperMoneyLight }

/** `PaperColors.money`-style accessor, mirroring [MilewayColors]'s ergonomics for this direction's extra role. */
object PaperColors {
    val money: Color
        @Composable @ReadOnlyComposable
        get() = LocalPaperMoneyColors.current.value
    val onMoney: Color
        @Composable @ReadOnlyComposable
        get() = LocalPaperMoneyColors.current.onValue
    val moneyContainer: Color
        @Composable @ReadOnlyComposable
        get() = LocalPaperMoneyColors.current.valueContainer
    val onMoneyContainer: Color
        @Composable @ReadOnlyComposable
        get() = LocalPaperMoneyColors.current.onValueContainer
}

/**
 * Paper's typography scale. The explicit rule this direction fixes:
 *
 * - **Serif** ([FontFamily.Serif]) — `headlineLarge/Medium/Small` and `titleLarge` only. Screen
 *   headers and hero card titles get the document voice.
 * - **Sans** ([FontFamily.Default]) — everything else that is chrome: `titleMedium/Small`, all
 *   `body*`, all `label*` (buttons, chips, nav). Small UI text needs legibility, not character.
 * - **Monospace** — data only, never chrome. Reached exactly the way the rest of the app already
 *   does: `MaterialTheme.typography.<role>.dataStyle()` or `MilewayType.dataLarge/Medium/Small`
 *   (both defined once in `Type.kt`, reused here unchanged). Distances, currency amounts,
 *   odometer/reference IDs, timestamps needing tabular alignment — nothing else.
 *
 * Conflating those two (mono for chrome AND data) was the #1 flaw the reference screenshots
 * called out; this scale makes the conflation impossible; there is no monospace entry below at all.
 */
val PaperTypography =
    Typography(
        headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = 0.sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = 0.sp),
        headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = 0.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = 0.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.15.sp),
        titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp),
        bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.3.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
        bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.2.sp),
        labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.2.sp),
        labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.2.sp),
    )

/**
 * Corner language: a crisp index-card radius, tighter than the app-wide 12–16dp squared-rounded
 * scale in [DesignTokens.Shape] — sharper corners read as a printed sheet rather than a soft app
 * tile. Buttons still take `DesignTokens.Shape.button` explicitly (that's every theme's contract,
 * unchanged here); this [Shapes] only governs what Material sources from `MaterialTheme.shapes`
 * (Card, chips/menus/text fields, dialogs/sheets).
 */
private val PaperShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(6.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(10.dp),
        extraLarge = RoundedCornerShape(10.dp),
    )

/**
 * Elevation as real depth, not glow. Ember/Matrix/Ion raise a surface with a light-emitting edge;
 * Paper raises it the way a sheet lifts off a desk — a genuine Material shadow. Values stay
 * modest (a document doesn't float): pair with `CardDefaults.cardElevation(defaultElevation = ...)`.
 */
object PaperElevation {
    val resting = 1.dp
    val raised = 3.dp
    val prominent = 6.dp
}

/**
 * Full Paper theme root — colour + [PaperTypography] + [PaperShapes] + the [PaperColors] money
 * role, all in one call. Screens render with `PaperTheme { ... }` the same way they render with
 * `MilewayTheme { ... }` today; swap [darkTheme] (or follow the system default) to get
 * [PaperNightSpec]'s counterpart.
 *
 * This direction doesn't route through `MilewayTheme()`'s picker: that composable hardcodes
 * `MilewayTypography`/the app-wide `Shapes` for every curated variant (see MilewayTheme.kt), and
 * changing that to be per-variant is a shared-file edit five concurrent one-file directions can't
 * safely make at once. `PaperTheme` is the complete, self-contained way this direction compiles
 * and renders today; wiring the picker to delegate typography/shapes per variant is a small,
 * explicit follow-up once a direction is chosen.
 */
@Composable
fun PaperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val spec = if (darkTheme) PaperNightSpec else PaperSpec
    val money = if (darkTheme) PaperMoneyDark else PaperMoneyLight
    CompositionLocalProvider(
        LocalMilewaySemanticColors provides spec.semanticColors(),
        LocalPaperMoneyColors provides money,
    ) {
        MaterialTheme(
            colorScheme = spec.toColorScheme(!darkTheme),
            typography = PaperTypography,
            shapes = PaperShapes,
            content = content,
        )
    }
}
