package com.mileway.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.hct.Hct
import kotlin.math.pow

/*
 * Perceptual colour maths in HCT (hue / chroma / **tone**), the space Material 3 itself uses.
 *
 * The whole layered theme system rests on one property of this space: **tone is contrast**. Two
 * colours with the same HCT tone have the same relative luminance, so they read equally well on
 * the same surface regardless of hue. Every derivation in [MilewayRoleColors] and
 * [MilewayDomain] therefore moves hue and chroma and *never* tone — which is what makes
 * "a domain may derive an accent, but may not override the base" a mechanical guarantee rather
 * than a code-review convention. `ThemeLayersTest` asserts it for every domain × every direction.
 *
 * Internal on purpose: features must not do colour maths. They ask for a role.
 */

/**
 * ponytail: hand-rolled instead of `Color.toArgb()` — that extension's availability varies across
 * Compose Multiplatform targets and this is four lines. Upgrade path: delete in favour of
 * `toArgb()` once it is common on every target Mileway ships.
 */
private const val ChannelMax = 255f
private const val RoundHalf = 0.5f
private const val AlphaShift = 24
private const val RedShift = 16
private const val GreenShift = 8

private fun Color.toArgbInt(): Int {
    fun ch(v: Float): Int = (v.coerceIn(0f, 1f) * ChannelMax + RoundHalf).toInt()
    return (ch(alpha) shl AlphaShift) or (ch(red) shl RedShift) or (ch(green) shl GreenShift) or ch(blue)
}

internal fun Color.hct(): Hct = Hct.fromInt(toArgbInt())

private fun Hct.toColor(): Color = Color(toInt())

/** Perceptual lightness, 0..100. Equal tone ⇒ equal contrast on a given surface. */
internal fun Color.tone(): Double = hct().tone

/** Rotate hue, holding chroma and **tone** — a different colour, identical legibility. */
internal fun Color.rotateHue(degrees: Double): Color {
    if (degrees == 0.0) return this
    val h = hct()
    return Hct.from((h.hue + degrees).mod(360.0), h.chroma, h.tone).toColor()
}

/** Scale saturation, holding hue and **tone**. `< 1` mutes, `> 1` intensifies (gamut-clamped). */
internal fun Color.scaleChroma(factor: Double): Color {
    if (factor == 1.0) return this
    val h = hct()
    return Hct.from(h.hue, (h.chroma * factor).coerceAtLeast(0.0), h.tone).toColor()
}

/**
 * Move [from] a [fraction] of the way toward [to] along the **shortest hue arc**, keeping [from]'s
 * tone. Used to synthesise a role that must sit visually *between* two existing ones — amber-to-red
 * for `policyViolation` — without introducing a fixed hex that a design direction can't retune.
 */
internal fun hueBlend(
    from: Color,
    to: Color,
    fraction: Double,
): Color {
    val a = from.hct()
    val b = to.hct()
    val delta = ((b.hue - a.hue + 540.0).mod(360.0)) - 180.0
    return Hct
        .from(
            (a.hue + delta * fraction).mod(360.0),
            a.chroma + (b.chroma - a.chroma) * fraction,
            a.tone,
        ).toColor()
}

// WCAG 2.x relative-luminance coefficients (sRGB). These are the specification's numbers, not
// tuning knobs: https://www.w3.org/TR/WCAG21/#dfn-relative-luminance
private const val SrgbLinearCutoff = 0.03928f
private const val SrgbLinearDivisor = 12.92f
private const val SrgbGammaOffset = 0.055f
private const val SrgbGammaDivisor = 1.055f
private const val SrgbGammaExponent = 2.4
private const val LuminanceWeightRed = 0.2126f
private const val LuminanceWeightGreen = 0.7152f
private const val LuminanceWeightBlue = 0.0722f

/**
 * WCAG relative luminance (sRGB), multiplatform-safe (no `android.graphics`).
 *
 * The single copy. It previously existed three times — [DesignTokens]' light/dark heuristic,
 * [MilewaySemanticColors]' surface probe, and a test — which is three places for one formula to
 * drift.
 */
internal fun Color.relativeLuminance(): Float {
    fun lin(c: Float): Float =
        if (c <= SrgbLinearCutoff) {
            c / SrgbLinearDivisor
        } else {
            ((c + SrgbGammaOffset) / SrgbGammaDivisor).toDouble().pow(SrgbGammaExponent).toFloat()
        }
    return LuminanceWeightRed * lin(red) + LuminanceWeightGreen * lin(green) + LuminanceWeightBlue * lin(blue)
}
