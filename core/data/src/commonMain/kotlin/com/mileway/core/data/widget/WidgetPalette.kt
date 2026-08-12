package com.mileway.core.data.widget

/**
 * The colours a home-screen widget paints with, as plain ARGB longs.
 *
 * Longs rather than Compose `Color` because `:widget` is Glance and `:core:data` has no Compose
 * dependency — and because this crosses a module boundary that exists for a reason: a Glance widget
 * pulling all of `:core:ui` to read five values would be a heavy fix for a small problem.
 *
 * The defaults are the Ember values the widget used to hardcode. They are the fallback, not the
 * intent: if nothing binds a [WidgetPaletteSource] the widget looks exactly as it did before rather
 * than rendering black-on-black.
 */
data class WidgetPalette(
    val surface: Long = 0xFF17110B,
    val accent: Long = 0xFFF5A623,
    val live: Long = 0xFFFF453A,
    val onSurface: Long = 0xFFF7EFE3,
    val stale: Long = 0xFF8A7F6E,
)

/**
 * Supplies the palette for whichever theme the app is currently wearing.
 *
 * Bound by `:app`, which is the only module that can see both the theme specs in `:core:ui` and the
 * widget. Resolved with `getOrNull` so an unbound graph (tests, a stripped build) degrades to
 * [WidgetPalette]'s defaults instead of crashing the widget host.
 *
 * This exists because the widget hardcoded Ember's amber and kept rendering it after the app's
 * default became Paper — a warm-dark widget sitting on the home screen next to a light document of
 * an app. Reading the live theme is the only version of this that cannot drift again: pick a new
 * default, or let a user pick any variant, and the widget follows.
 */
interface WidgetPaletteSource {
    fun current(): WidgetPalette
}
