package com.mileway.widget

import androidx.compose.ui.graphics.Color
import com.mileway.core.data.widget.WidgetPalette
import com.mileway.core.data.widget.WidgetPaletteSource
import com.mileway.core.ui.theme.ThemeController

/**
 * Paints the home-screen widget in whatever theme the app is currently wearing.
 *
 * Lives in `:app` because it is the only module that sees both `:core:ui`'s theme specs and
 * `:core:data`'s widget contract. `:widget` is Glance and deliberately does not depend on
 * `:core:ui`.
 *
 * Derived from the spec, never re-typed. The widget used to hardcode Ember's amber and kept
 * rendering it after the default became Paper, which put a warm-dark widget on the home screen
 * beside a light-document app. Copying the Paper hexes across would only have moved the drift one
 * theme along; reading the live variant means picking a new default — or a user picking any variant
 * — carries to the widget for free.
 */
class ThemeWidgetPaletteSource(
    private val themeController: ThemeController,
) : WidgetPaletteSource {

    override fun current(): WidgetPalette {
        // milewayTheme, NOT themeVariant. They are different properties: themeVariant is the
        // legacy palette-style string, while setMilewayTheme() writes milewayTheme. Reading the
        // wrong one made this follow nothing — every variant produced the same palette, which the
        // test caught before it shipped.
        val spec = themeController.milewayTheme.value.spec
        return WidgetPalette(
            surface = spec.canvas.argb(),
            accent = spec.accent.argb(),
            // The widget's "live" dot is a danger/attention signal, not a second accent — it maps
            // to the theme's own danger colour so it stays legible on a light canvas too.
            live = spec.danger.argb(),
            onSurface = spec.text.argb(),
            stale = spec.textMuted.argb(),
        )
    }

    /**
     * Compose packs colour into the high 32 bits of a ULong; the widget wants a plain ARGB long.
     */
    private fun Color.argb(): Long = value.toLong() shr ARGB_SHIFT and ARGB_MASK

    private companion object {
        /** Compose's Color.value stores RGBA in the top half of a 64-bit word. */
        const val ARGB_SHIFT = 32
        const val ARGB_MASK = 0xFFFFFFFFL
    }
}
