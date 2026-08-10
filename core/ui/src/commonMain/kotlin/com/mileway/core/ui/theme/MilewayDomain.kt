package com.mileway.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * **Layer 3 of 3 — DOMAIN.** See `LAYERS.md` in this package for the full rule.
 *
 * An optional per-feature accent overlay, so approvals can *feel* different from tracking without
 * fragmenting back into per-screen colours.
 *
 * ### The rule that keeps this from becoming five sanctioned exceptions
 *
 * A domain declares **only a hue rotation and a chroma scale**. It cannot name a colour. Its accent
 * is computed from the active direction's accent, in HCT, at **unchanged tone** — so:
 *
 *  - switching design direction still moves every domain (Approvals under Paper is a Paper colour);
 *  - contrast against the direction's surfaces is preserved by construction, not by re-verification;
 *  - there is no field a future PR can set to a hex.
 *
 * A domain overlay touches the **accent ramp only** — `primary`, `secondary`, `primaryContainer`,
 * `surfaceTint`. Surfaces, canvas, type and every Layer-2 role are untouched: `approved` is the
 * same green in payables as in approvals, and an amount reads identically on both, because a
 * reader compares those across screens.
 *
 * ### What this fixes concretely
 *
 * `ApprovalsScreen` hard-coded `Brush.horizontalGradient(Color(0xFF6C63FF), Color(0xFF9C6BFF))`,
 * so the most contested screen in the app rendered the same purple under all five design
 * directions. Wrapped in `MilewayDomainTheme(MilewayDomain.APPROVALS)`, the existing
 * `DesignTokens.topBarGradientBrush()` — which already reads `primary` and `secondary` — produces
 * an approvals-flavoured gradient that tracks the chosen direction, with no colour at the call site.
 */
enum class MilewayDomain(
    internal val hueShift: Double,
    internal val chromaScale: Double,
) {
    /** No overlay. The default everywhere; app chrome and cross-cutting surfaces stay here. */
    NONE(hueShift = 0.0, chromaScale = 1.0),

    /**
     * Tracking is the app's identity surface — the live drive *is* the brand moment, so it renders
     * in the unmodified direction accent. Named rather than folded into [NONE] so a tracking screen
     * still declares its domain, and so a future direction could give it a shift without a rename.
     */
    TRACKING(hueShift = 0.0, chromaScale = 1.0),

    /** Approvals: pulled slightly cooler and calmer — a manager's review surface, not a hype surface. */
    APPROVALS(hueShift = -30.0, chromaScale = 0.94),

    /** Expenses / logging: warmed off the base accent. */
    EXPENSES(hueShift = 34.0, chromaScale = 1.0),

    /** Payables: furthest and most muted — back-office finance, deliberately the quietest domain. */
    PAYABLES(hueShift = 64.0, chromaScale = 0.86),

    /** Cards: cool and slightly richer, the one domain that is allowed to look like a product. */
    CARDS(hueShift = -58.0, chromaScale = 1.06),

    /** Travel: the widest swing, matching how different a trip itinerary is from a mileage log. */
    TRAVEL(hueShift = 96.0, chromaScale = 0.92),
    ;

    internal val isIdentity: Boolean get() = hueShift == 0.0 && chromaScale == 1.0
}

/** The domain a subtree is rendering in. Read it for analytics/behaviour; do not derive colour from it by hand. */
val LocalMilewayDomain: ProvidableCompositionLocal<MilewayDomain> =
    staticCompositionLocalOf { MilewayDomain.NONE }

/**
 * Scope a feature's navigation graph (or a single screen) to a [domain].
 *
 * Wrap once, at the feature's entry point in the nav graph — not per screen, and never per
 * component:
 *
 * ```
 * composable<Approvals> { MilewayDomainTheme(MilewayDomain.APPROVALS) { ApprovalsScreen(…) } }
 * ```
 *
 * Everything inside then picks the domain accent up through `MaterialTheme.colorScheme.primary`,
 * which is what M3 components, `DesignTokens.topBarGradientBrush()` and `topBarContainerColor()`
 * already read — so the overlay lands on existing call sites with no edits.
 */
@Composable
fun MilewayDomainTheme(
    domain: MilewayDomain,
    content: @Composable () -> Unit,
) {
    val base = MaterialTheme.colorScheme
    val scheme = remember(base, domain) { base.withDomainAccent(domain) }
    CompositionLocalProvider(LocalMilewayDomain provides domain) {
        // Nested MaterialTheme: typography and shapes default to the enclosing theme's, so a domain
        // can never accidentally change the type scale a design direction chose.
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/**
 * Derive [domain]'s accent ramp from this scheme. Accent roles only — every surface, on-surface,
 * outline and error role is passed through untouched.
 */
internal fun ColorScheme.withDomainAccent(domain: MilewayDomain): ColorScheme {
    if (domain.isIdentity) return this
    fun Color.shift(): Color = rotateHue(domain.hueShift).scaleChroma(domain.chromaScale)
    return copy(
        primary = primary.shift(),
        primaryContainer = primaryContainer.shift(),
        onPrimaryContainer = onPrimaryContainer.shift(),
        inversePrimary = inversePrimary.shift(),
        secondary = secondary.shift(),
        surfaceTint = surfaceTint.shift(),
    )
}
