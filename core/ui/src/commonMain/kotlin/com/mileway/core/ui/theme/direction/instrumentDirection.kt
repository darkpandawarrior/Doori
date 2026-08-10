package com.mileway.core.ui.theme.direction

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mileway.core.ui.theme.MilewaySchemeSpec

/**
 * Direction: INSTRUMENT — automotive/cockpit HMI.
 *
 * FOR: the live-drive surface specifically — the screen on screen while the phone is in a mount
 * and the user is a driver, not a reader. Everything here optimises for "readable in under a
 * second, from arm's length, in peripheral vision, while the vehicle is moving" — the same brief
 * as a digital instrument cluster or a driver-info display, not a mobile app.
 *
 * APPEALS TO: a user who wants the app to disappear into "gauge" the moment it matters — the
 * opposite of Ember's warm, personality-forward duotone. Instrument has almost no personality on
 * purpose; a dashboard that's trying to have a vibe is a dashboard that's in the way.
 *
 * SACRIFICES: warmth, density, and decoration. There's no room in this direction for a card with
 * three text sizes and an icon — every screen inherits the same discipline as the live-drive
 * surface even where a normal list screen could afford to be richer. It will read as stark, even
 * cold, on a settings or history screen. That's the trade: uniform cockpit discipline everywhere,
 * rather than "glanceable while driving, ordinary app the rest of the time."
 *
 * BEST SCREEN: the active trip / live tracking screen — one hero number (distance or running
 * value), a start/stop affordance, nothing else competing for the eye.
 *
 * Colour scheme is dark-only, deliberately — not "dark by default, light available." A day-mode
 * instrument cluster exists in real cars because sun glare defeats a dark panel; Mileway is a
 * phone in a mount, not a fixed panel with sun-facing glass, and a light Instrument variant would
 * just be Daybreak with different numerals. One enum entry, one honest register: dark.
 *
 * Semantic roles:
 *  - primary action  → [accent], a cool telltale blue. Reserved for what the driver *does*
 *    (Start/Stop, Submit) — never for the number itself, so action and readout are never visually
 *    interchangeable when both are on screen together.
 *  - destructive      → [danger], saturated brake-light red.
 *  - warning           → [warning], high-visibility amber (caution-light amber, not Ember's warm
 *    honey amber — cooler, more saturated, reads as "alert" not "brand").
 *  - success           → [success], go-light green — and this is also the **money/value role**.
 *    A reimbursement total is an outcome the driver receives, not a button they press, so it is
 *    deliberately NOT accent-blue. Green is the universal "you're getting this back" register,
 *    it's a fourth hue distinct from accent/warning/danger so a running total is never confused
 *    with the Stop button next to it, and it means the app doesn't need a sixth colour role for a
 *    concept ([MilewaySchemeSpec] intentionally isn't extended with a `value` field here — five
 *    directions edit this package concurrently, so the shared spec stays untouched and reuse
 *    carries the semantics instead).
 *  - info              → [info], a distinct lighter cyan — kept apart from accent-blue so
 *    "here is a fact" (info) and "press here" (accent) never collapse into the same blue.
 */
internal val InstrumentSpec =
    MilewaySchemeSpec(
        canvas = Color(0xFF0A0C0E),
        surface = Color(0xFF121518),
        surfaceCard = Color(0xFF171B1F),
        surfaceRaised = Color(0xFF1D2227),
        surfaceHighest = Color(0xFF262C32),
        border = Color(0xFF333A41),
        text = Color(0xFFF5F7FA),
        textMuted = Color(0xFF9AA5AD),
        accent = Color(0xFF2F8FFF),
        accentDim = Color(0xFF1F5FB0),
        accentGlow = Color(0xFF6FBBFF),
        onAccent = Color(0xFF04101F),
        accentContainer = Color(0xFF14304F),
        onAccentContainer = Color(0xFFBFE0FF),
        warning = Color(0xFFFFB020),
        danger = Color(0xFFFF3B30),
        info = Color(0xFF33C7FF),
        success = Color(0xFF22D46B),
        // The live number gets a halo, nothing else does — glow signals "this is live/active",
        // not decoration, so it stays scoped rather than painted on every raised surface.
        useGlow = true,
    )

/**
 * Chrome typography for the Instrument direction. NOT yet wired into [com.mileway.core.ui.theme.MilewayTheme] —
 * per the architecture note in `MilewayThemes.kt`, typography is still theme-independent app-wide
 * (the shared [com.mileway.core.ui.theme.MilewayTypography]). Staged here for the follow-up phase that lets a
 * chosen direction supply its own type scale instead of the fixed global default.
 *
 * Explicit mono rule: monospace is used ONLY for a numeric readout the driver reads as data —
 * distance, speed, duration, a claim amount ([InstrumentDataType]). Every chrome role below
 * (headline/title/label/body) uses the system sans instead, which *reverses* the house default
 * (mono on titleLarge/labelLarge etc). The screenshot review named house-wide mono-for-chrome the
 * #1 cause of the "sad" feeling: it makes titles read like a spec sheet, which competes with the
 * one thing on this screen that's supposed to actually look like data. A glanceable cockpit needs
 * its chrome to be quiet and its numbers to be loud — monospacing both makes both shout evenly,
 * which reads as nothing shouting.
 */
internal val InstrumentTypography =
    Typography(
        headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, letterSpacing = 0.2.sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, letterSpacing = 0.2.sp),
        headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 0.2.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 0.3.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.3.sp),
        titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.3.sp),
        bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = 0.2.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp, letterSpacing = 0.2.sp),
        bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.6.sp),
        labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.6.sp),
        labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.6.sp),
    )

/**
 * Data/readout type for the Instrument direction — the only place monospace appears. Tabular
 * digits so a live-updating value (odometer, timer, running total) never reflows column width as
 * digits change; sizes step up from the house [com.mileway.core.ui.theme.MilewayType] scale because a glanceable
 * hero number has to win the "read in under a second from arm's length" test the house scale
 * wasn't tuned for.
 */
internal object InstrumentDataType {
    private val Mono = FontFamily.Monospace

    /** The single glanceable hero readout: live distance/speed, or a claim total on a summary. */
    val hero = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Black, fontSize = 72.sp, letterSpacing = 0.sp)

    /** Section-level stat (secondary reading alongside the hero — e.g. duration next to distance). */
    val dataLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, letterSpacing = 0.sp)

    /** List-row amounts, inline stats. */
    val dataMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 0.sp)

    /** Codes, coordinates, timestamps — data too, but never the thing the eye should land on first. */
    val dataSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.2.sp)

    /** The money/value role (see the KDoc on [InstrumentSpec] for why it's [InstrumentSpec.success], not accent). */
    val money = hero
}

/**
 * Shape / elevation / spacing tokens for the Instrument direction. Also staged, not yet wired —
 * see [InstrumentTypography]'s header note; the same "theme-independent today, per-direction
 * later" split applies to [com.mileway.core.ui.theme.DesignTokens].
 */
internal object InstrumentTokens {
    /**
     * Corner language: tight, technical radii — a gauge bezel, not a consumer card. Deliberately
     * tighter than the house 12dp "square rounded" system; Instrument should read as instrument
     * panel, not app chrome, and a big soft radius on a cockpit surface undoes that immediately.
     */
    val shapes =
        Shapes(
            extraSmall = RoundedCornerShape(2.dp),
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(4.dp),
            large = RoundedCornerShape(6.dp),
            extraLarge = RoundedCornerShape(6.dp),
        )
    val buttonShape = RoundedCornerShape(6.dp)

    /**
     * Elevation: flat, always. A drop shadow implies "this panel floats above the road" — the
     * wrong metaphor for a display bolted to a dashboard. Depth comes entirely from
     * [InstrumentSpec]'s surface-luminance ramp plus a 1dp hairline border, never from Material's
     * tonal/shadow elevation.
     */
    const val ELEVATION_DP = 0

    /**
     * Focus ring width — a driver may be navigating by d-pad/rotary (an Android Auto head unit)
     * or a gloved thumb bouncing with the vehicle; the focus indicator has to be unmissable, so
     * this is roughly 2x a typical Material focus outline.
     */
    val focusRingWidth = 3.dp

    /** 8dp base grid (vs. the house 4dp scale) — one glanceable element per visual beat, wide gutters, nothing crowded near the hero number. */
    object Spacing {
        val s = 8.dp
        val m = 16.dp
        val l = 24.dp
        val xl = 32.dp
    }

    /** Minimum touch target, 56dp vs. the house 48dp floor — vehicle motion and gloved hands both erode tap precision. */
    val minTouchTarget = 56.dp
}
