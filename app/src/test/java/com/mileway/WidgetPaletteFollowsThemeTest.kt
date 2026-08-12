package com.mileway

import com.mileway.core.ui.theme.MilewayThemeVariant
import com.mileway.core.ui.theme.ThemeController
import com.mileway.widget.ThemeWidgetPaletteSource
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * The widget hardcoded Ember's amber and kept painting it after the app's default became Paper.
 * Nothing failed and nothing warned — it just quietly stopped being the same product as the app
 * beside it on the home screen.
 *
 * These assert the widget reads the *live* theme rather than a copy of one, for every variant, so
 * the next default change carries automatically instead of leaving the widget a theme behind.
 */
class WidgetPaletteFollowsThemeTest {

    private fun sourceFor(variant: MilewayThemeVariant): ThemeWidgetPaletteSource {
        val controller = ThemeController()
        controller.setMilewayTheme(variant)
        return ThemeWidgetPaletteSource(controller)
    }

    @Test
    fun `every variant's widget palette matches that variant's own spec`() {
        for (variant in MilewayThemeVariant.entries) {
            val palette = sourceFor(variant).current()
            val spec = variant.spec
            val label = variant.id
            assertEquals(spec.canvas.argb(), palette.surface, "$label surface")
            assertEquals(spec.accent.argb(), palette.accent, "$label accent")
            assertEquals(spec.danger.argb(), palette.live, "$label live")
            assertEquals(spec.text.argb(), palette.onSurface, "$label onSurface")
            assertEquals(spec.textMuted.argb(), palette.stale, "$label stale")
        }
    }

    // The regression itself: the shipped default is Paper, so the widget must no longer be painting
    // Ember's warm-black canvas. If someone re-pins the widget to a literal, this fails.
    @Test
    fun `the default no longer paints Ember's canvas`() {
        val default = sourceFor(MilewayThemeVariant.DEFAULT).current()
        val ember = sourceFor(MilewayThemeVariant.EMBER).current()
        assertNotEquals(ember.surface, default.surface)
        assertEquals(MilewayThemeVariant.PAPER.spec.canvas.argb(), default.surface)
    }

    @Test
    fun `switching theme switches the widget`() {
        assertNotEquals(
            sourceFor(MilewayThemeVariant.PAPER).current(),
            sourceFor(MilewayThemeVariant.MATRIX).current(),
        )
    }

    private fun androidx.compose.ui.graphics.Color.argb(): Long = value.toLong() shr 32 and 0xFFFFFFFFL
}
