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
 * Signal's **day face** — the hand-tuned counterpart at the opposite luminance to [SignalSpec],
 * not an algorithmic inversion of it. Flipping [SignalSpec]'s lightness channel would land on a
 * neutral grey page with pastel accents: the exact "sad visual language" complaint Signal exists
 * to answer, restated in daylight. Every value below was picked on its own terms.
 *
 * **What carries over (the identity):** Signal is a *cool* direction and stays cool. The canvas is
 * indigo-tinted daylight (`#EEF1FC`), never neutral `#F5F5F5`; the surface ramp steps down from
 * white toward indigo rather than toward grey, so elevation-by-tint keeps the direction's hue
 * instead of washing it out. All five semantic hues survive — indigo primary, cyan motion,
 * emerald money, amber attention, red destructive — so a manager scanning ten claims in daylight
 * reads them by exactly the same colour code as at night.
 *
 * **What changes (and why):** saturation moves, hue doesn't. Signal's night accents are luminous
 * (`#22D3EE` cyan, `#1FCE8C` emerald) because they sit on a near-black canvas. On white those same
 * values are unreadable — an electric cyan on white is roughly 1.9:1. The day face therefore takes
 * each hue to its *deep, saturated* end rather than its bright end: `info` becomes a deep cyan
 * (`#076B7C`), `success` a deep emerald (`#0A6E4C`). Chroma stays high — these are not the muted
 * pastels a tint-inversion produces — but the luminance is inverted so the same hue reads *against*
 * a light page instead of glowing off a dark one. Confidence is preserved by being deep and
 * saturated, not by being bright.
 *
 * Two structural flips are deliberate:
 *  - [accentDim] is **darker** than [accent] here, the reverse of [SignalSpec]. `accentDim` maps to
 *    Material's `secondary`, whose `onSecondary` is [onAccent] — so on a light face it must be dark
 *    enough to carry white text (10.58:1 below). A "dim" that lightened, as a naive inversion would
 *    produce, ships an unreadable secondary button.
 *  - [useGlow] is `false`. Signal's night face raises a card with a light-emitting edge; on a light
 *    canvas a glow has nothing to emit against and reads as a smudge. Daylight depth comes from
 *    [SignalElevation]'s real shadows, which are already tuned high for exactly this reason.
 *
 * **Contrast (WCAG 2.1 relative luminance, computed not estimated).** Every foreground below
 * clears AA 4.5:1 against every one of this spec's five backgrounds; the worst cell in the whole
 * matrix is 4.59:1.
 *
 * ```
 * fg \ bg          canvas  surface  surfaceRaised  surfaceHighest  accentContainer
 * text             16.72    18.86       15.73          14.77           14.47
 * textMuted         6.68     7.53        6.28           5.90            5.78
 * accent            6.73     7.58        6.33           5.94            5.82
 * accentDim         9.38    10.58        8.82           8.28            8.11
 * accentGlow        5.43     6.13        5.11           4.80            4.70
 * warning           5.20     5.86        4.89           4.59            4.50
 * danger            5.49     6.19        5.17           4.85            4.75
 * info              5.47     6.17        5.15           4.83            4.73
 * success           5.56     6.27        5.23           4.91            4.81
 * ```
 *
 * `surface` == `surfaceCard` (both `#FFFFFF`), so the surface column covers cards too. Body text
 * on its own surface — [text] on [surface] — is 18.86:1, and the muted body role [textMuted] on
 * [surface] is 7.53:1; both clear AAA, not just AA.
 *
 * Filled-role pairings, all AA: [onAccent] on [accent] 7.58:1, on [accentDim] 10.58:1, on
 * [accentGlow] 6.13:1; [onAccentContainer] on [accentContainer] 11.25:1; white on [danger] 6.19:1,
 * on [info] 6.17:1, on [success] 6.27:1, on [warning] 5.86:1 — which matters because
 * `toColorScheme(isLight = true)` hardcodes white for `onError`/`onTertiary`.
 *
 * [border] is not text, so it takes the 3:1 non-text threshold instead: 3.39:1 on [surface] and
 * 3.01:1 on [canvas]. It is a visibly indigo hairline rather than the near-invisible `#D8DCEF` an
 * inversion suggests — a direction whose whole thesis is "cards you can see sitting on a
 * background" cannot ship a 1.4:1 outline on its text fields.
 *
 * Not registered as a selectable [MilewayThemeVariant]: one identity, two faces. Wire it as
 * `SIGNAL`'s counterpart the way `PAPER` carries `PaperNightSpec` — a second enum entry would put
 * both faces in the picker as competing choices and let a stored preference disagree with the
 * device setting.
 */
internal val SignalSpecDay =
    MilewaySchemeSpec(
        canvas = Color(0xFFEEF1FC),
        surface = Color(0xFFFFFFFF),
        surfaceCard = Color(0xFFFFFFFF),
        surfaceRaised = Color(0xFFE6EAFA),
        surfaceHighest = Color(0xFFDEE3F8),
        border = Color(0xFF7B87CD),
        text = Color(0xFF0E1020),
        textMuted = Color(0xFF4C5372),
        accent = Color(0xFF3341CC),
        accentDim = Color(0xFF232FA0),
        accentGlow = Color(0xFF4150DE),
        onAccent = Color(0xFFFFFFFF),
        accentContainer = Color(0xFFDCE0FC),
        onAccentContainer = Color(0xFF161E6B),
        warning = Color(0xFF8C5A08),
        danger = Color(0xFFBE1A36),
        info = Color(0xFF076B7C),
        success = Color(0xFF0A6E4C),
        // Depth by shadow, not by emitted edge — see the KDoc's second structural flip.
        useGlow = false,
    )

/**
 * The one semantic role [MilewaySchemeSpec] has no dedicated slot for: money specifically, as
 * distinct from "success" generally. Same hue as [SignalSpec.success] / [SignalSpecDay.success]
 * by design (see [SignalSpec] KDoc) — named separately so a screen showing a reimbursement total
 * can say "give me the money colour" and mean it, independent of whether that number also happens
 * to represent an approved state.
 */
object SignalColors {
    val moneyValueDark: Color = SignalSpec.success
    val moneyValueLight: Color = SignalSpecDay.success
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
