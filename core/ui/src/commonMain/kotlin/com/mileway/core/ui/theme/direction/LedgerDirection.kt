package com.mileway.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Design direction: **Ledger** — a financial instrument (Stripe dashboard / a well-set annual
 * report), not a terminal. Registered as [MilewayThemeVariant.LEDGER] (light, primary). See
 * `MilewayThemes.kt` for the one-line registration; everything else this direction needs lives
 * here so five directions can land in the same package without touching each other's files.
 *
 * The rule that makes this direction legible: **proportional sans for every piece of chrome**
 * (headings, labels, body, nav) and **monospace tabular figures reserved for money, distance and
 * timestamps only**. The other four curated themes run mono through headline/title/label too —
 * that's the "everything is a terminal" flatness this direction exists to fix. [LedgerTypography]
 * is the chrome scale (all [FontFamily.Default]); [LedgerDataType] is the narrow mono set for the
 * numbers a claim actually turns on.
 *
 * Depth comes from tone, not glow: [LedgerSpec.useGlow] is `false`, and elevation is real tonal
 * steps (`canvas` → `surface` → `surfaceRaised` → `surfaceHighest`) plus a hairline `border`,
 * reusing [DesignTokens.Elevation]'s existing card/raised/prominent dp scale — this direction
 * needed a different *palette* to stand on, not different elevation dp values. Corners tighten
 * from the app-wide 12dp "square rounded" language to [LedgerShapes]' 6–10dp: a ledger has edges,
 * not a terminal's soft pill chrome. [LedgerSpacing] widens the 4dp app rhythm to a 8dp one for
 * the "generous whitespace" the direction asks for.
 *
 * Note on wiring: [MilewayTheme] currently applies [MilewayTypography] / the app-wide `Shapes`
 * unconditionally — no curated variant switches type or shape yet, only [MilewaySchemeSpec.toColorScheme].
 * [LedgerTypography]/[LedgerShapes]/[LedgerSpacing] are complete and ready for that per-variant
 * wiring; adding it is a shared-file change deliberately left to the integration pass that reconciles
 * all five directions, not to this one.
 */

/**
 * =============================================================================
 * Colour — light (registered as MilewayThemeVariant.LEDGER)
 * =============================================================================
 *
 * Ledger, light (primary/registered). Cool near-white canvas, white cards separated by tonal
 * steps + hairline border rather than fill, one restrained financial-navy accent used only for
 * the primary action and links — never decorative. No glow: this direction reads as calm because
 * it refuses to, not because it forgot to add colour.
 */
internal val LedgerSpec =
    MilewaySchemeSpec(
        canvas = Color(0xFFF4F5F7),
        surface = Color(0xFFFFFFFF),
        surfaceCard = Color(0xFFFFFFFF),
        surfaceRaised = Color(0xFFEBEDF1),
        surfaceHighest = Color(0xFFE0E3E8),
        border = Color(0xFFD8DCE2),
        text = Color(0xFF161A20),
        textMuted = Color(0xFF6B7280),
        accent = Color(0xFF1E3A5F),
        accentDim = Color(0xFF3D5A80),
        accentGlow = Color(0xFF4A73B0),
        onAccent = Color(0xFFFFFFFF),
        accentContainer = Color(0xFFDCE6F2),
        onAccentContainer = Color(0xFF102A4C),
        warning = Color(0xFF9A6A00),
        danger = Color(0xFFB3261E),
        info = Color(0xFF0E7C86),
        success = Color(0xFF1D7A4C),
        useGlow = false,
    )

/**
 * =============================================================================
 * Colour — night counterpart (hand-tuned, NOT an inversion of [LedgerSpec])
 * =============================================================================
 *
 * Ledger after hours: the same restrained financial instrument, read at night. Every value below
 * was picked deliberately, not derived — a channel-flip of [LedgerSpec] produces neutral charcoal
 * (`#F4F5F7` → `#0B0A08`), and a neutral grey ledger reads as dead switched-off hardware, which is
 * the exact opposite of the "calm because it refuses to shout" identity.
 *
 * What is deliberately preserved from the light face, and why:
 *
 * - **The cool cast stays cool.** Ledger's light canvas is blue-leaning near-white (`#F4F5F7`), and
 *   the night canvas keeps that bias hard: `#0E141C` is measurably navy (B > G > R), not `#111111`.
 *   Every surface step carries the same blue lean, so the direction still feels like a statement
 *   printed on cool stock rather than a terminal that happened to go dark.
 * - **Fill still doesn't do the separating.** Cards sit at the same value as `surface` (`surfaceCard`
 *   == `surface`, mirroring the light face's white-on-white), and structure comes from the hairline
 *   `border` plus the tonal ramp. The night `border` is pulled *up* relative to its ground
 *   (3.06:1 on `surface`) because a dark hairline at the light face's 1.15:1 separation would
 *   simply vanish — hairline rules are what make a table read as a table in this direction.
 * - **One navy accent, still non-decorative.** The light `#1E3A5F` is inverted-lightness but not
 *   inverted-hue: `#7DA5DC` is the same financial navy raised into the legible band for a dark
 *   ground. `accentGlow` exists only because [MilewaySchemeSpec] requires it; `useGlow` stays
 *   `false`, so nothing here emits an edge — depth remains tonal, exactly as in the light face.
 * - **Status hues keep their light-face families**: ochre warning, brick danger, teal info (Ledger's
 *   info is teal, not blue, so it can never be mistaken for the navy accent), ledger-green success.
 *   Each is lifted in lightness and pulled *down* in saturation from the naive dark equivalent — a
 *   fully-saturated status chip on a near-black ground is a warning light, and this direction owns
 *   a statement, not a dashboard.
 *
 * ### Measured contrast (sRGB relative luminance, WCAG 2.x)
 *
 * Body text on its own ground — the AA 4.5:1 gate:
 *
 * | Foreground | Ground | Ratio |
 * |---|---|---|
 * | `text` `#E4E9F0` | `canvas` `#0E141C` | **15.16:1** |
 * | `text` | `surface` / `surfaceCard` `#151C26` | **14.04:1** |
 * | `text` | `surfaceRaised` `#1D2632` | **12.51:1** |
 * | `text` | `surfaceHighest` `#27313F` | **10.78:1** |
 * | `textMuted` `#94A2B5` | `canvas` | **7.13:1** |
 * | `textMuted` | `surface` | **6.60:1** |
 * | `textMuted` | `surfaceRaised` | **5.88:1** |
 * | `textMuted` | `surfaceHighest` | **5.07:1** |
 *
 * Accent and its on-colours (`accentDim` is Material `secondary`/`inversePrimary`, and
 * `toColorScheme` pairs it with `onAccent`, so that pair is a real body-text pair too):
 *
 * | Pair | Ratio |
 * |---|---|
 * | `accent` `#7DA5DC` on `surface` | **6.76:1** |
 * | `accent` on `canvas` | **7.30:1** |
 * | `accent` on `surfaceHighest` | **5.19:1** |
 * | `accentDim` `#6688BA` on `surface` | **4.73:1** |
 * | `onAccent` `#0A1422` on `accent` | **7.29:1** |
 * | `onAccent` on `accentDim` | **5.10:1** |
 * | `onAccentContainer` `#C6D9F2` on `accentContainer` `#1F2E45` | **9.52:1** |
 * | `text` on `accentContainer` | **11.21:1** |
 *
 * Status colours, on every ground they can land on — and inverted, since `toColorScheme` maps
 * `onError`/`onTertiary` to `canvas` for dark schemes:
 *
 * | Pair | on `canvas` | on `surface` | on `surfaceHighest` |
 * |---|---|---|---|
 * | `warning` `#D9A441` | **8.22:1** | **7.62:1** | **5.85:1** |
 * | `danger` `#EC7C6F` | **6.76:1** | **6.26:1** | **4.81:1** |
 * | `info` `#4CC0CC` | **8.56:1** | **7.93:1** | **6.08:1** |
 * | `success` `#45BE80` | **7.86:1** | **7.28:1** | **5.59:1** |
 *
 * Lowest text ratio anywhere in this spec is **4.73:1** (`accentDim` on `surface`) — the whole
 * scheme clears AA with margin, and `danger` was lifted from a first pass at `#E3695E` (4.04:1 on
 * `surfaceHighest`, a fail) rather than shipped with a caveat.
 *
 * Non-text, for completeness: the `border` hairline `#5A6884` clears WCAG 1.4.11's 3:1 against the
 * two grounds it actually rules against — `canvas` (**3.30:1**) and `surface` (**3.06:1**). Against
 * filled `surfaceRaised` (2.72:1) / `surfaceHighest` (2.35:1) chips it sits below 3:1, which is
 * correct for this direction: there the fill is the boundary, not the stroke.
 *
 * Not registered as a [MilewayThemeVariant]. Registration is a `MilewayThemes.kt` edit, owned by
 * the integration pass — the same reasoning `PaperNightSpec` records: one identity, two faces,
 * resolved by the variant's dark/light lookup, never two picker entries a stored preference could
 * disagree with. This replaces the earlier `LedgerDarkSpec`, which was an honest tonal flip and
 * nothing more.
 */
internal val LedgerSpecNight =
    MilewaySchemeSpec(
        // Navy-cast, never neutral: B > G > R holds at every step of the ramp.
        canvas = Color(0xFF0E141C),
        surface = Color(0xFF151C26),
        // Matches `surface` on purpose — the light face's card is white on white; structure is the
        // hairline and the tonal step, not a fill.
        surfaceCard = Color(0xFF151C26),
        surfaceRaised = Color(0xFF1D2632),
        surfaceHighest = Color(0xFF27313F),
        // Lifted well above a naive dark hairline: at the light face's separation it would vanish,
        // and a ledger without visible rules is just a list.
        border = Color(0xFF5A6884),
        text = Color(0xFFE4E9F0),
        textMuted = Color(0xFF94A2B5),
        // Same financial navy as light `#1E3A5F`, raised into the legible band — not re-hued.
        accent = Color(0xFF7DA5DC),
        accentDim = Color(0xFF6688BA),
        accentGlow = Color(0xFF9CBCEC),
        onAccent = Color(0xFF0A1422),
        accentContainer = Color(0xFF1F2E45),
        onAccentContainer = Color(0xFFC6D9F2),
        // Light-face hue families, lifted for the dark ground and held back on saturation so a
        // status chip reads as a note in a statement, not a warning lamp.
        warning = Color(0xFFD9A441),
        danger = Color(0xFFEC7C6F),
        info = Color(0xFF4CC0CC),
        success = Color(0xFF45BE80),
        // Same discipline as the light face: depth is tonal, never emitted.
        useGlow = false,
    )

/**
 * =============================================================================
 * Type — proportional sans for chrome, mono ONLY for money / distance / timestamps
 * =============================================================================
 *
 * Ledger's chrome type scale. Every role — headline through label — is [FontFamily.Default] (the
 * system proportional sans), unlike [MilewayTypography] which runs mono through headline/title/
 * label. This is the whole point of the direction: a number reads as data because everything
 * *around* it is deliberately not data. Body sizes/line-heights match [MilewayTypography] so the
 * two scales are drop-in compatible.
 */
val LedgerTypography =
    Typography(
        headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = 0.sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = 0.sp),
        headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = 0.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = 0.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp, letterSpacing = 0.15.sp),
        titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp),
        bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.3.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
        bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp),
        labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
        labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp),
    )

/**
 * The narrow mono exception: money, distance and timestamps ONLY. Everything else in this
 * direction — including section headers, chip labels and nav — stays on [LedgerTypography].
 * Mirrors [MilewayType]'s dataLarge/dataMedium/dataSmall ergonomics so call sites already using
 * that pattern port over with a one-word rename.
 */
object LedgerDataType {
    private val MonoFamily: FontFamily = FontFamily.Monospace

    /** The headline claim/reimbursement figure. Tight tracking so digits sit close, like a total. */
    val amountLarge =
        TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, letterSpacing = (-0.25).sp)

    /** Line-item amounts, trip distance, a secondary stat. */
    val amountMedium =
        TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 20.sp, letterSpacing = 0.sp)

    /** Inline figures: a timestamp, an odometer reading, a record ID in a table row. */
    val amountSmall =
        TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.2.sp)

    /** Recast any [LedgerTypography] role into the mono data family for an inline figure. */
    fun of(style: TextStyle): TextStyle = style.copy(fontFamily = MonoFamily, letterSpacing = 0.sp)
}

/**
 * =============================================================================
 * Money / value colour role
 * =============================================================================
 *
 * The colour role this product's core number needs. Deliberately **not** accent-tinted: a
 * reimbursement total in the primary-action colour reads as a button, and colouring it for
 * "positive" implies a judgement the app hasn't made yet. [value] is the same high-contrast ink as
 * body text — boring on purpose, so a big [LedgerDataType.amountLarge] figure earns its authority
 * from size, weight and whitespace, not hue. [credit]/[debit] are reserved for signed ledger line
 * items (a refund vs. a deduction) where colour *is* the right signal.
 */
object LedgerColors {
    /** The headline figure: a claim total, a trip distance. Neutral ink — never accent-coloured. */
    val value: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurface

    /** A positive ledger entry — reimbursement paid, a refund. */
    val credit: Color
        @Composable @ReadOnlyComposable
        get() = MilewayColors.success

    /** A negative ledger entry — a deduction, a rejected line, an over-policy flag. */
    val debit: Color
        @Composable @ReadOnlyComposable
        get() = MilewayColors.danger
}

/**
 * =============================================================================
 * Shape & spacing — a ledger has edges; whitespace is generous, not cramped
 * =============================================================================
 *
 * Tighter corner language than the app-wide 12dp "square rounded" default — 6–10dp reads as a
 * printed instrument (table, receipt, statement) rather than a soft app-chrome pill. Ready for
 * per-variant wiring alongside [LedgerTypography]; see the file header note.
 */
val LedgerShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(6.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(10.dp),
        extraLarge = RoundedCornerShape(10.dp),
    )

/**
 * Doubles [DesignTokens.Spacing]'s 4dp rhythm to 8dp — the "generous whitespace" a financial
 * instrument needs so a hairline-bordered table doesn't read as cramped. Screen padding widens
 * from 16dp to 24dp for the same reason.
 */
object LedgerSpacing {
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val screenHorizontal = 24.dp
    val sectionSpacing = 40.dp
}
