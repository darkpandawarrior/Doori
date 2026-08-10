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

/**
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

// =============================================================================
// Colour — light (registered as MilewayThemeVariant.LEDGER)
// =============================================================================

/**
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
 * Ledger, dark companion — the direction's answer to "colour scheme, light AND dark". Same navy
 * accent family and the same no-glow discipline, just inverted tonal steps. **Not** registered as
 * a [MilewayThemeVariant] — the task caps this file's enum touch to one line, and a dark Ledger
 * needs its own `LEDGER_DARK` entry. Kept here, ready to promote in the integration pass, rather
 * than left undesigned.
 */
internal val LedgerDarkSpec =
    MilewaySchemeSpec(
        canvas = Color(0xFF11151C),
        surface = Color(0xFF171C24),
        surfaceCard = Color(0xFF1D232D),
        surfaceRaised = Color(0xFF242B37),
        surfaceHighest = Color(0xFF2D3543),
        border = Color(0xFF333C4C),
        text = Color(0xFFE7EAF0),
        textMuted = Color(0xFF9AA4B5),
        accent = Color(0xFF6E93D6),
        accentDim = Color(0xFF4E6FA0),
        accentGlow = Color(0xFF89ADEF),
        onAccent = Color(0xFF0B1420),
        accentContainer = Color(0xFF223252),
        onAccentContainer = Color(0xFFCBDCF7),
        warning = Color(0xFFD8A73D),
        danger = Color(0xFFE0665C),
        info = Color(0xFF4FB5C2),
        success = Color(0xFF4CC486),
        useGlow = false,
    )

// =============================================================================
// Type — proportional sans for chrome, mono ONLY for money / distance / timestamps
// =============================================================================

/**
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

// =============================================================================
// Money / value colour role
// =============================================================================

/**
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

// =============================================================================
// Shape & spacing — a ledger has edges; whitespace is generous, not cramped
// =============================================================================

/**
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
