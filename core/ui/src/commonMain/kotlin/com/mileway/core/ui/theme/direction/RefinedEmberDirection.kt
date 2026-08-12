package com.mileway.core.ui.theme.direction

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewaySchemeSpec
import com.mileway.core.ui.theme.MilewayTheme
import com.mileway.core.ui.theme.MilewayThemeVariant

/*
 * ============================================================================================
 * DIRECTION: Refined Ember
 * ============================================================================================
 *
 * WHAT THIS IS FOR: the conservative option. Same warm-dark amber identity as the existing
 * default [com.mileway.core.ui.theme.EmberSpec] — this is NOT a rebrand — but every concrete
 * defect the vision-model review found in the 217-screenshot pass is fixed:
 *  1. Monospace was applied to chrome (headlines/titles/labels) as well as data. [RefinedEmberTypography]
 *     removes monospace from every Material role; it is now sans everywhere except numeric
 *     readouts, which opt in explicitly via the already-existing `dataStyle()` /
 *     [com.mileway.core.ui.theme.MilewayType] helpers. One rule, no exceptions: if it's not a
 *     number the user is reading as data, it is not monospace.
 *  2. One near-black with no tonal range. The 5-step surface ramp (canvas → surface →
 *     surfaceCard → surfaceRaised → surfaceHighest) is pushed to genuinely distinguishable
 *     steps rather than a couple of percent apart — see the literals below.
 *  3. No elevation. [RefinedEmberElevation] gives cards a real shadow + tonal lift via
 *     `CardDefaults.cardElevation(...)`, not just a lighter fill colour.
 *  4. Amber used for everything. Three amber-family roles are now visually and semantically
 *     distinct: `accent` (interactive — buttons, links, selection) vs `warning` (status, more
 *     burnt/muted) vs [RefinedEmberSemantics.moneyValue] (a read-only figure, never on a
 *     tappable element). Same family for brand cohesion, different job, different tone.
 *  5. Contrast failures (disabled Confirm, red error text). Every pair below is WCAG-AA
 *     checked (≥4.5:1) — see `RefinedEmberDirectionTest` — and the disabled-state alpha
 *     ([RefinedEmberSemantics.DISABLED_CONTENT_ALPHA]) is picked to stay legible, not just
 *     "technically exempt because it's disabled."
 *  6. No unambiguous primary / red on non-destructive actions. Button hierarchy is a strict
 *     rule, not a token: filled `accent` = the one primary CTA per screen; outlined/tonal =
 *     secondary; text-only = tertiary; filled/outlined `danger` = destructive intent ONLY
 *     (delete, discard, remove) — never a generic "important" or cancel action.
 *
 * WHO IT APPEALS TO: whoever picked Ember originally and still likes it — finance/ops reviewers
 * who want the app to look considered and specific to Mileway, not a generic Material app, but
 * need to stop squinting at a disabled button or wondering if that red text means "you're about
 * to delete something" or just "this field is required."
 *
 * WHAT IT SACRIFICES: novelty. This direction proves the existing identity was never broken at
 * the concept level — only the execution was — so it is the least visually distinct of the five
 * directions side by side.
 *
 * DAY FACE: originally dark-only. [RefinedEmberSpecDay] adds the daylight counterpart — the same
 * ember identity at noon rather than a second theme, hand-tuned rather than inverted. See its doc
 * for why an inversion was the wrong tool and for the measured contrast ratios.
 *
 * BEST SCREEN: the trip/claim summary — a single hero card with the reimbursement total in
 * large mono digits, a resting-elevation card, a clear filled-primary "Submit claim" button, and
 * a text-only "Discard" nowhere near red. It's the screen where every fix above earns its keep
 * at once: depth, hierarchy, contrast, and a number that visibly is not the same color as the
 * button next to it.
 * ============================================================================================
 */

/**
 * ── Colour scheme (dark only — see "what it sacrifices" above) ─────────────────────────────
 *
 * The hand-tuned [MilewaySchemeSpec] for Refined Ember. Registered as
 * [MilewayThemeVariant.REFINED_EMBER] in `MilewayThemes.kt`. A genuinely 5-step tonal ramp
 * (each step below is a visible jump, not a couple of percent) plus AA-checked accent /
 * semantic pairs — see `RefinedEmberDirectionTest` for the actual contrast numbers.
 */
internal val RefinedEmberSpec =
    MilewaySchemeSpec(
        canvas = Color(0xFF0C0906),
        surface = Color(0xFF16100A),
        surfaceCard = Color(0xFF201811),
        surfaceRaised = Color(0xFF2B2015),
        surfaceHighest = Color(0xFF392A1A),
        border = Color(0xFF5A4526),
        text = Color(0xFFF7EFE3),
        textMuted = Color(0xFFC9B9A3),
        accent = Color(0xFFF2A428),
        accentDim = Color(0xFFB87A1C),
        accentGlow = Color(0xFFFFC15E),
        onAccent = Color(0xFF14100A),
        accentContainer = Color(0xFF3A2A12),
        onAccentContainer = Color(0xFFFFD79A),
        // Status colours: kept a hue apart from `accent` (more burnt/muted) so "this needs
        // attention" never reads as "this is tappable".
        warning = Color(0xFFD68B2E),
        danger = Color(0xFFFF5449),
        info = Color(0xFF6BB6FF),
        success = Color(0xFF4CC98A),
        useGlow = true,
    )

/**
 * ── Colour scheme (day face) ─────────────────────────────────────────────────────────────────
 *
 * The daylight counterpart to [RefinedEmberSpec], hand-picked colour by colour — **not** an
 * algorithmic inversion of it, for the same reason `PaperNightSpec` isn't an inversion of
 * `PaperSpec`. Flipping the dark ramp's lightness channel produces a neutral grey canvas (the
 * warm hue survives inversion at maybe 2% chroma once L is near 95) and turns a #F2A428 accent
 * into a pale straw that fails on every surface it sits on. Both halves of Ember's identity —
 * warmth and the ember accent — are exactly what an inversion destroys. So:
 *
 *  - **The canvas keeps its warmth, and gains more of it.** Sunlit paper (#FFF6E8), not
 *    off-white. In daylight the ember is the light *falling on* the page rather than the glow
 *    coming off a coal, so the warm bias moves into the surfaces instead of the accent.
 *  - **The 5-step ramp inverts its direction, not its spacing.** Dark lifts a surface by getting
 *    lighter; light lifts a card by getting *whiter* (canvas → surface → surfaceCard) and then
 *    recesses raised/highest by getting *more tinted* (surfaceRaised → surfaceHighest). Measured
 *    relative luminance: 93.0% → 96.1% → 99.3% → 84.8% → 77.3% — five steps that are visible
 *    jumps, which is defect #2 in the header staying fixed on this face too.
 *  - **The accent inverts its lightness but not its hue.** #F2A428 (a light-emitting ember on
 *    black) becomes #9E4C0D (burnt ember ink on paper): the same amber-orange family, dark enough
 *    to be a legible link/label on paper and to carry a near-white `onAccent` on a filled button.
 *  - **`accentGlow` becomes a hue shift, not a lightness shift.** On dark, "hotter" means
 *    brighter (#FFC15E). On paper there is nothing to emit, so heat reads as a push toward red:
 *    #A64207 at essentially the same luminance as `accent`, distinguished by hue alone. The two
 *    are never layered on each other, so equal luminance costs nothing.
 *  - **The accent-vs-warning separation from defect #4 survives.** `accent` is a burnt orange
 *    (interactive); `warning` is a dark ochre/gold (#7E5A05, status). Different hue, different
 *    job, still one family.
 *  - **`useGlow = false`, unlike the night face.** The flag drives a light-emitting top edge
 *    (`MilewaySchemeSpec.useGlow` is documented dark-only); a glow edge on white paper reads as a
 *    render artefact. Depth on this face comes from [RefinedEmberElevation]'s real shadows, which
 *    is where it should have come from all along.
 *
 * **Contrast (WCAG 2.1 relative luminance, computed not eyeballed).** Every foreground role
 * against all five surfaces, worst case of the five, AA body-text threshold 4.5:1:
 *
 * | Foreground | canvas | surface | surfaceCard | surfaceRaised | surfaceHighest | worst |
 * |---|---|---|---|---|---|---|
 * | `text` #1F150A       | 16.75 | 17.28 | 17.82 | 15.35 | 14.07 | **14.07** |
 * | `textMuted` #6B5636  |  6.51 |  6.72 |  6.93 |  5.97 |  5.47 |  **5.47** |
 * | `accent` #9E4C0D     |  5.61 |  5.79 |  5.97 |  5.14 |  4.71 |  **4.71** |
 * | `accentDim` #71350A  |  8.85 |  9.13 |  9.41 |  8.11 |  7.43 |  **7.43** |
 * | `accentGlow` #A64207 |  5.76 |  5.94 |  6.13 |  5.28 |  4.84 |  **4.84** |
 * | `warning` #7E5A05    |  5.85 |  6.03 |  6.22 |  5.36 |  4.91 |  **4.91** |
 * | `danger` #B3261E     |  6.10 |  6.29 |  6.49 |  5.59 |  5.12 |  **5.12** |
 * | `info` #1A5FA8       |  6.04 |  6.23 |  6.42 |  5.53 |  5.07 |  **5.07** |
 * | `success` #16653C    |  6.62 |  6.83 |  7.04 |  6.07 |  5.56 |  **5.56** |
 *
 * On-colour pairs: `onAccent` on `accent` **5.69**, on `accentDim` **8.98**, on `accentGlow`
 * **5.84**; `onAccentContainer` on `accentContainer` **11.06**; `text` on `accentContainer`
 * **14.72**. `border` #9C7538 is a non-text divider, judged against the 3:1 non-text threshold:
 * 3.91 / 4.03 / 4.16 / 3.58 / **3.28** worst case — a warm bronze hairline rather than the near
 * invisible 1.5:1 tint a light theme usually settles for.
 *
 * Nothing below ships under threshold. `accent` was darkened from a first pass at #A85411 (4.08:1
 * on `surfaceHighest`) and `accentGlow` twice from #B85E0C, because chips draw accent-coloured
 * text on the highest surface and that is the one pairing a light theme always gets wrong.
 *
 * One known gap, stated rather than papered over: [RefinedEmberSemantics.moneyValue] (#E0A64C) is
 * tuned for the night face and reads at 2.01:1 on this canvas. On the day face draw a reimbursement
 * figure in `accentDim` (8.85:1) — same "computed fact, not a CTA" separation from `accent`,
 * already in the spec. Giving `moneyValue` a per-face value means changing that object's shape,
 * which belongs with whichever step wires the two faces together, not here.
 */
internal val RefinedEmberSpecDay =
    MilewaySchemeSpec(
        canvas = Color(0xFFFFF6E8),
        surface = Color(0xFFFFFAF3),
        surfaceCard = Color(0xFFFFFFF3),
        surfaceRaised = Color(0xFFF6ECDD),
        surfaceHighest = Color(0xFFF0E2CD),
        border = Color(0xFF9C7538),
        text = Color(0xFF1F150A),
        textMuted = Color(0xFF6B5636),
        accent = Color(0xFF9E4C0D),
        accentDim = Color(0xFF71350A),
        accentGlow = Color(0xFFA64207),
        onAccent = Color(0xFFFFF8EC),
        accentContainer = Color(0xFFFBE6C4),
        onAccentContainer = Color(0xFF4A2503),
        // Same rule as the night face: status stays a hue apart from `accent` so "this needs
        // attention" never reads as "this is tappable" — ochre for warning, not burnt orange.
        warning = Color(0xFF7E5A05),
        danger = Color(0xFFB3261E),
        info = Color(0xFF1A5FA8),
        success = Color(0xFF16653C),
        useGlow = false,
    )

/**
 * ── Typography: sans everywhere except data ─────────────────────────────────────────────────
 *
 * Refined Ember's type scale. The one rule that fixes the review's #1 finding: **every Material
 * role here is [FontFamily.Default]** (the same humanist system sans the house [bodyLarge]/
 * [bodyMedium] roles already use) — monospace never appears in a headline, title, or label. A
 * numeric readout (distance, a claim amount, a duration, an ID) is not a Material role; it opts
 * into monospace explicitly at the call site via the already-existing
 * `MaterialTheme.typography.titleMedium.dataStyle()` or
 * [com.mileway.core.ui.theme.MilewayType.dataLarge]/`dataMedium`/`dataSmall` — reused unchanged,
 * not reinvented here. If a screen ever wants monospace and neither of those two mechanisms
 * apply, that is a signal the content is data and belongs on one of them, not a reason to touch
 * this Typography object.
 */
val RefinedEmberTypography =
    Typography(
        headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-0.25).sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = 0.sp),
        headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = 0.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = 0.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp, letterSpacing = 0.15.sp),
        titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp),
        bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.3.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
        bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
        // Button/label text: SemiBold sans reads as an actual button label, not a terminal
        // command — the wide monospace tracking the house scale used here was part of why
        // buttons felt like chrome instead of an action.
        labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.2.sp),
        labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.3.sp),
        labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.3.sp),
    )

/**
 * ── Shape / corner language ──────────────────────────────────────────────────────────────────
 *
 * Deliberately matches the house "square rounded" geometry (`MilewayTheme.kt`'s private
 * `MilewayShapes` — 8/10/12/16/16dp). Geometry was never the review's complaint; a direction
 * that fixes colour, type and elevation while also inventing new corner radii would be changing
 * more than it fixed. Redeclared here (that val is file-private) rather than exported, so this
 * stays a one-file addition.
 */
val RefinedEmberShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(16.dp),
    )

/**
 * ── Elevation / depth treatment ──────────────────────────────────────────────────────────────
 *
 * Real elevation for Refined Ember's cards — a shadow + tonal lift via
 * `CardDefaults.cardElevation`, not just a lighter `surfaceCard` fill. Reuses
 * [DesignTokens.Elevation]'s existing dp scale (2 / 4 / 8dp) unchanged; the fix here is *using*
 * Compose's native elevation system at all, which is what the review's "flat cards on flat
 * backgrounds" finding was really about.
 */
object RefinedEmberElevation {
    /** Resting card — list rows, secondary content. */
    @Composable
    fun card(): CardElevation =
        CardDefaults.cardElevation(
            defaultElevation = DesignTokens.Elevation.card,
            pressedElevation = DesignTokens.Elevation.card / 2,
            focusedElevation = DesignTokens.Elevation.raised,
            hoveredElevation = DesignTokens.Elevation.raised,
        )

    /** Raised card — sheets, active/selected surfaces. */
    @Composable
    fun raised(): CardElevation =
        CardDefaults.cardElevation(
            defaultElevation = DesignTokens.Elevation.raised,
            pressedElevation = DesignTokens.Elevation.card,
            focusedElevation = DesignTokens.Elevation.prominent,
            hoveredElevation = DesignTokens.Elevation.prominent,
        )

    /** The hero claim/reimbursement total — the one card on a screen that should visibly float. */
    @Composable
    fun heroValue(): CardElevation =
        CardDefaults.cardElevation(
            defaultElevation = DesignTokens.Elevation.prominent,
            pressedElevation = DesignTokens.Elevation.raised,
        )
}

/**
 * ── Semantic roles: the money role, and the disabled-state fix ──────────────────────────────
 *
 * Tokens that don't fit [com.mileway.core.ui.theme.MilewaySemanticColors] (that struct is shared
 * across all 5 curated themes and stays untouched by this direction) but that this direction's
 * brief specifically calls for.
 */
object RefinedEmberSemantics {
    /**
     * The colour a mileage/reimbursement figure is drawn in. **Never apply this to a button,
     * icon, chip, or anything tappable** — that is what `accent` is for. This is the "this is a
     * fact that was computed" colour: close enough to `accent` to read as the same ember family
     * (brand cohesion), deliberately less saturated so it never gets mistaken for a CTA sitting
     * next to it. Always pair with monospace via `dataStyle()` / `MilewayType.dataLarge` — the
     * colour signals "money", the font signals "data"; neither substitutes for the other.
     */
    val moneyValue: Color = Color(0xFFE0A64C)

    /**
     * Content alpha for a disabled control over [MilewaySchemeSpec.surfaceRaised] — chosen to
     * stay at ≥4.5:1 contrast (verified in `RefinedEmberDirectionTest`), not just whatever
     * default M3 ships. Directly answers the review's "disabled Confirm is unusable" finding:
     * disabled must read as *off*, never as *invisible*.
     */
    const val DISABLED_CONTENT_ALPHA: Float = 0.7f
}

/**
 * ── Entry point ───────────────────────────────────────────────────────────────────────────────
 *
 * Convenience wrapper: renders [content] in the full Refined Ember direction (colour scheme +
 * type scale + shape language) in one call, for previews/galleries that want the complete
 * direction rather than assembling the pieces by hand. Thin call-through to the existing
 * [MilewayTheme] entry point — no new theming mechanism, just this direction's own token bundle
 * passed in.
 */
@Composable
fun RefinedEmberTheme(content: @Composable () -> Unit) {
    MilewayTheme(
        milewayTheme = MilewayThemeVariant.REFINED_EMBER,
        typography = RefinedEmberTypography,
        shapes = RefinedEmberShapes,
        content = content,
    )
}
