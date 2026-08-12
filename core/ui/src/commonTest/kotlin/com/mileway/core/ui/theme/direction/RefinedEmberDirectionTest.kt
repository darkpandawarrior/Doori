package com.mileway.core.ui.theme.direction

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Runnable check for Refined Ember's two hardest claims: every text/state pair hits WCAG-AA
 * (≥4.5:1), and the three amber-family semantic roles (accent / warning / money) are actually
 * distinct colours rather than a copy-paste of the same literal wearing three names.
 */
class RefinedEmberDirectionTest {
    private fun Color.relativeLuminance(): Float {
        fun lin(c: Float): Float = if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
        return 0.2126f * lin(red) + 0.7152f * lin(green) + 0.0722f * lin(blue)
    }

    private fun contrast(
        a: Color,
        b: Color,
    ): Float {
        val l1 = maxOf(a.relativeLuminance(), b.relativeLuminance())
        val l2 = minOf(a.relativeLuminance(), b.relativeLuminance())
        return (l1 + 0.05f) / (l2 + 0.05f)
    }

    private val spec = RefinedEmberSpec
    private val wcagAaNormalText = 4.5f

    @Test
    fun body_text_meets_aa_contrast_on_canvas() {
        assertTrue(contrast(spec.text, spec.canvas) >= wcagAaNormalText)
        assertTrue(contrast(spec.textMuted, spec.canvas) >= wcagAaNormalText)
    }

    @Test
    fun filled_button_text_meets_aa_contrast_on_accent() {
        assertTrue(contrast(spec.onAccent, spec.accent) >= wcagAaNormalText)
    }

    @Test
    fun semantic_state_colours_meet_aa_contrast_on_canvas() {
        assertTrue(contrast(spec.warning, spec.canvas) >= wcagAaNormalText)
        assertTrue(contrast(spec.danger, spec.canvas) >= wcagAaNormalText)
        assertTrue(contrast(spec.info, spec.canvas) >= wcagAaNormalText)
        assertTrue(contrast(spec.success, spec.canvas) >= wcagAaNormalText)
    }

    @Test
    fun money_value_colour_meets_aa_contrast_on_canvas_and_on_cards() {
        assertTrue(contrast(RefinedEmberSemantics.moneyValue, spec.canvas) >= wcagAaNormalText)
        assertTrue(contrast(RefinedEmberSemantics.moneyValue, spec.surfaceCard) >= wcagAaNormalText)
    }

    @Test
    fun disabled_content_alpha_stays_legible_over_a_disabled_surface() {
        // Flatten textMuted at the disabled alpha over surfaceRaised (the disabled-button
        // background) and check the result still clears AA — the review's "disabled Confirm is
        // unusable" bug is exactly this pair rendered too faint to read.
        val a = RefinedEmberSemantics.DISABLED_CONTENT_ALPHA
        val flattened =
            Color(
                red = spec.textMuted.red * a + spec.surfaceRaised.red * (1 - a),
                green = spec.textMuted.green * a + spec.surfaceRaised.green * (1 - a),
                blue = spec.textMuted.blue * a + spec.surfaceRaised.blue * (1 - a),
            )
        assertTrue(contrast(flattened, spec.surfaceRaised) >= wcagAaNormalText)
    }

    @Test
    fun the_three_amber_family_roles_are_visually_distinct_not_the_same_literal_renamed() {
        // Guards against the exact bug the review flagged: "amber is used for everything, so it
        // signals nothing". Interactive accent, status warning, and the read-only money figure
        // must be three different colours even though they share a hue family.
        assertNotEquals(spec.accent, spec.warning)
        assertNotEquals(spec.accent, RefinedEmberSemantics.moneyValue)
        assertNotEquals(spec.warning, RefinedEmberSemantics.moneyValue)
    }

    @Test
    fun refined_ember_is_a_dark_scheme_only_matching_its_stated_scope() {
        assertTrue(spec.useGlow, "dark schemes carry the raised-surface glow; Refined Ember is dark-only by design")
    }
}
