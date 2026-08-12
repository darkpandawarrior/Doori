package com.mileway.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Design direction: **Signal**.
 *
 * FOR: fixing the #1 vision-review complaint ("the visual language looks so sad") without adding
 * decoration. Real tonal range on a dark foundation, saturated colour that carries meaning
 * (money is never the same hue as "recording"), bold type hierarchy, and shadows a card can
 * actually be seen sitting on.
 *
 * APPEALS TO: the reviewer who opens the app expecting it to look like it was built by the same
 * team that builds fintech dashboards — confident, legible at a glance, colour-coded so a manager
 * scanning ten claims doesn't have to read every line to know which ones are still moving vs.
 * which ones are money already. Not the minimalist-mono crowd (that's what Ember/Matrix already
 * serve) and not the corporate-neutral crowd (that's Daybreak's job).
 *
 * SACRIFICES: restraint. Signal spends five distinct hues (indigo, cyan, emerald, amber, red)
 * where Ember spends one. That is a deliberate bet that semantic colour-coding is worth more here
 * than a hushed, monochrome mood — but it means Signal will read as "busier" next to Ember/Matrix
 * in a side-by-side, and a reviewer who wants maximum calm should reject it in favour of one of
 * those. It also breaks the "mono is the whole voice" convention the other four themes share —
 * Signal's mono footprint is deliberately narrower (data only), which is the point, not a bug.
 *
 * BEST SCREEN: the trip summary / claim-review screen — one hero mileage number in
 * [SignalType.heroNumber], a reimbursement total in [SignalColors.moneyValue], a live-tracking
 * chip in the motion cyan ([MilewaySchemeSpec.info]), and an approve/reject action pair that reads
 * unambiguously (emerald primary vs. red destructive) without a legend. That's the screen this
 * direction was built to answer for.
 *
 * SCOPE NOTE: per the multi-agent file-ownership rule, this pass only registers the
 * [MilewayThemeVariant.SIGNAL] colour scheme. The typography ([SignalTypography], [SignalType]),
 * shape ([SignalShapes], [SignalCorners]), elevation ([SignalElevation]) and spacing
 * ([SignalSpacing]) tokens below are the complete direction, ready to be wired into
 * `MilewayTheme()` as the active `Typography`/`Shapes` when — and if — Signal is the direction
 * that's picked; that swap touches the shared theme entry point and is deliberately left for the
 * follow-up phase that owns that file.
 */

/**
 * =============================================================================
 * Colour — dark foundation, semantic accent roles
 * =============================================================================
 *
 * Signal (dark, registered as [MilewayThemeVariant.SIGNAL]).
 *
 * Role mapping onto [MilewaySchemeSpec]'s five accent slots:
 *  - [MilewaySchemeSpec.accent] → **primary action** (electric indigo). Every primary button,
 *    selected state and interactive link.
 *  - [MilewaySchemeSpec.info] → **motion / active tracking** (electric cyan). Reserved for "this
 *    is happening right now" — the live-recording chip, an in-progress trip, a syncing badge.
 *    Never reused for a static button.
 *  - [MilewaySchemeSpec.success] → **money / value** (emerald). An approved claim *is* a money
 *    event in this product, so success and the money role deliberately share one hue rather than
 *    adding a sixth — see [SignalColors.moneyValue] for the explicit, separately-named accessor
 *    call sites should reach for when they mean "this is a number of rupees," not just "this
 *    state is good."
 *  - [MilewaySchemeSpec.warning] → amber. Needs-attention, pending, low balance.
 *  - [MilewaySchemeSpec.danger] → red. Destructive actions and rejected/overdue states only —
 *    never repurposed for a non-destructive control (the exact failure the brief flagged).
 */
internal val SignalSpec =
    MilewaySchemeSpec(
        canvas = Color(0xFF0A0B12),
        surface = Color(0xFF12131E),
        surfaceCard = Color(0xFF171928),
        surfaceRaised = Color(0xFF1F2236),
        surfaceHighest = Color(0xFF2A2D47),
        border = Color(0xFF383C5C),
        text = Color(0xFFF4F5FB),
        textMuted = Color(0xFFA6AAC8),
        accent = Color(0xFF5B6EFF),
        accentDim = Color(0xFF3F4ECC),
        accentGlow = Color(0xFF8B97FF),
        onAccent = Color(0xFFFFFFFF),
        accentContainer = Color(0xFF262C52),
        onAccentContainer = Color(0xFFC7CDFF),
        warning = Color(0xFFFFB020),
        danger = Color(0xFFFF4560),
        info = Color(0xFF22D3EE),
        success = Color(0xFF1FCE8C),
        useGlow = true,
    )

/**
 * Signal (light). Not registered as a selectable [MilewayThemeVariant] — the direction's brief is
 * explicitly "keep a dark foundation," and only one enum entry is permitted per direction in this
 * pass. Kept here, fully AA-verified, so the direction doesn't stop at "dark only" on paper; wire
 * it up as [MilewayThemeVariant.SIGNAL]'s light counterpart if a light mode is ever wanted.
 */
internal val SignalLightSpec =
    MilewaySchemeSpec(
        canvas = Color(0xFFF6F7FB),
        surface = Color(0xFFFFFFFF),
        surfaceCard = Color(0xFFFFFFFF),
        surfaceRaised = Color(0xFFEEF0FA),
        surfaceHighest = Color(0xFFE2E5F5),
        border = Color(0xFFD8DCEF),
        text = Color(0xFF10121F),
        textMuted = Color(0xFF565C7A),
        accent = Color(0xFF3B4CDB),
        accentDim = Color(0xFF6472E8),
        accentGlow = Color(0xFF5B6EFF),
        onAccent = Color(0xFFFFFFFF),
        accentContainer = Color(0xFFDDE1FA),
        onAccentContainer = Color(0xFF1B2470),
        warning = Color(0xFFA8660A),
        danger = Color(0xFFD82A44),
        info = Color(0xFF0E93AB),
        success = Color(0xFF11835A),
        useGlow = false,
    )

/**
 * The one semantic role [MilewaySchemeSpec] has no dedicated slot for: money specifically, as
 * distinct from "success" generally. Same hue as [SignalSpec.success] / [SignalLightSpec.success]
 * by design (see [SignalSpec] KDoc) — named separately so a screen showing a reimbursement total
 * can say "give me the money colour" and mean it, independent of whether that number also happens
 * to represent an approved state.
 */
object SignalColors {
    val moneyValueDark: Color = SignalSpec.success
    val moneyValueLight: Color = SignalLightSpec.success
}

/**
 * =============================================================================
 * Typography — bold chrome, mono confined to data
 * =============================================================================
 *
 * The explicit rule the other four themes don't state out loud: **monospace is for figures the
 * user reads as data, and nowhere else.**
 *
 * - Headlines, titles, labels, body — anything that is UI chrome, a heading, a button label, a
 *   nav item — use [FontFamily.Default] (system sans) here. This is the fix for the #1 vision
 *   complaint: the existing [MilewayTypography] puts monospace on headline/title/label too, so
 *   chrome and data read identically and nothing stands out as "the number."
 * - Numeric readouts (distance, amount, duration, a code) stay in [SignalType] — [MonoFamily],
 *   tabular figures, never used outside a data context.
 *
 * Sizes step up from [MilewayTypography] for a bolder hierarchy (headlineLarge 32→36sp, titleLarge
 * 22→24sp) and lean on [FontWeight.Bold]/[FontWeight.ExtraBold] rather than SemiBold/Medium, so
 * chrome reads with more authority against the deep, tonal-range surfaces.
 */
val SignalTypography =
    Typography(
        headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, letterSpacing = 0.sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, letterSpacing = 0.sp),
        headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 25.sp, letterSpacing = 0.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 0.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, letterSpacing = 0.15.sp),
        titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.15.sp),
        bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.3.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
        bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.2.sp),
        labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.2.sp),
        labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.2.sp),
    )

/**
 * Signal's data-mono presets. Same [FontFamily.Monospace] backbone as [MilewayType] (no bundled
 * font asset, multiplatform-safe) but with a dedicated [heroNumber] the other themes don't have —
 * "larger numerals" is a literal ask, and the biggest number on a claim screen (the reimbursement
 * total, the trip distance) is the one figure that should dominate the frame.
 */
object SignalType {
    val MonoFamily: FontFamily = FontFamily.Monospace

    /** The hero figure — a claim total or a trip distance on its own summary card. Bigger and
     * heavier than [MilewayType.dataLarge] (40sp Bold) on purpose: this is the number the whole
     * screen exists to show. */
    val heroNumber =
        TextStyle(
            fontFamily = MonoFamily,
            fontWeight = FontWeight.Black,
            fontSize = 56.sp,
            letterSpacing = (-1).sp,
        )

    /** Section stats (speed, duration, secondary amounts) — same size as [MilewayType.dataMedium]
     * but heavier, matching Signal's bolder overall weight. */
    val dataMedium =
        TextStyle(
            fontFamily = MonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = 0.sp,
        )

    /** Inline data chips, codes, coordinates. */
    val dataSmall =
        TextStyle(
            fontFamily = MonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            letterSpacing = 0.4.sp,
        )
}

/**
 * =============================================================================
 * Shape — a distinct corner language from the shared "square rounded" 12dp system
 * =============================================================================
 *
 * Signal's shape scheme: cards round noticeably more than controls, so a raised surface reads as
 * a distinct object sitting above the canvas rather than a variation on a button. Contrast with
 * the app-wide [MilewayShapes] / [DesignTokens.Shape], which use one 12dp radius everywhere.
 */
val SignalShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(24.dp),
    )

/** Direct dp radii for call sites that don't route through [androidx.compose.material3.Shapes]. */
object SignalCorners {
    val button = RoundedCornerShape(14.dp)
    val card = RoundedCornerShape(20.dp)
    val chip = RoundedCornerShape(12.dp)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}

/**
 * =============================================================================
 * Elevation & spacing — real depth, a slightly more generous rhythm
 * =============================================================================
 *
 * Visibly higher than [DesignTokens.Elevation] (2dp/4dp/8dp) — the vision review's #2 complaint
 * was "flat cards on flat backgrounds," so Signal's shadows are meant to actually read at a
 * glance, not just satisfy an elevation API technically.
 */
object SignalElevation {
    val card = 3.dp
    val raised = 10.dp
    val prominent = 24.dp
}

/**
 * An 8dp-based rhythm (vs. [DesignTokens.Spacing]'s 4dp base) — one deliberate notch more
 * generous, so the bigger type in [SignalTypography]/[SignalType] gets room to breathe instead of
 * fighting the same tight gutters tuned for the smaller mono-headline system.
 */
object SignalSpacing {
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}
