package com.mileway.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * **Layer 2 of 3 — SEMANTIC.** See `LAYERS.md` in this package for the full rule.
 *
 * The colours Mileway's *domain* needs, named for what they **mean**, never for what they look
 * like. A screen asks for `pending`; it must never ask for amber, and it must never know that
 * `pending` happens to be amber today.
 *
 * Mileway turns a recorded drive into a reimbursement claim read by an employee, their manager,
 * finance, and possibly a tax auditor. These twelve roles are that document's vocabulary. They are
 * the *only* meanings the product has — a colour that does not express one of them is either
 * chrome (Layer 1, [MilewaySchemeSpec] / [MaterialTheme.colorScheme]) or it is a mistake.
 *
 * ### Why these are derived, not hand-tuned per theme
 *
 * Ten design directions × twelve roles would be 120 hand-picked hexes — the 290-colour problem
 * rebuilt inside `theme/`. Instead every role is derived in [roleColorsFrom] from the six colours a
 * direction already declares (`accent`, `accentGlow`, `warning`, `danger`, `info`, `success`) plus
 * its muted text tone. Adding an eleventh direction gets all twelve roles for free and correct.
 *
 * A direction that genuinely disagrees with a derivation overrides exactly that field via
 * `spec.roleColors().copy(pending = …)` — one deliberate, reviewable exception, not a new palette.
 */
@Immutable
data class MilewayRoleColors(
    /** A monetary figure: amount, total, reimbursable value. The number the whole product exists to produce. */
    val money: Color,
    /** A measured distance, duration or route. The other half of a claim's evidence. */
    val distance: Color,
    /** Terminal-good: approved, verified, settled, passed, earned. */
    val approved: Color,
    /** In-flight and blocking someone: awaiting approval, under review, processing. */
    val pending: Color,
    /** Terminal-bad by a person's decision: rejected, failed, expired, declined. */
    val rejected: Color,
    /** Flagged by the policy engine — the claim breaks a rule but nobody has ruled on it yet. Sits deliberately between [pending] and [rejected]. */
    val policyViolation: Color,
    /** Captured locally, not yet synced. Offline-first is the base layer, so this is a first-class state, not an error. */
    val offlineQueued: Color,
    /** A recording is running right now: live drive, active timer, capturing. Never used for chrome. */
    val activeTracking: Color,
    /** The affordance that ends or destroys something: stop recording, delete, close account. */
    val destructive: Color,
    /** Neutral information, hints, non-blocking notices. */
    val informational: Color,
    /** Draft, inactive, disabled, not-yet-started. Deliberately low-chroma. */
    val inactive: Color,
    /** Paid tier, corporate card, reward, club benefit — value the user unlocked. */
    val premium: Color,
)

/**
 * The single derivation. Both the curated path ([MilewaySchemeSpec.roleColors]) and the legacy
 * seed / system-colour path ([derivedRoleColors]) come through here, so there is exactly one
 * definition of what "pending" means in this product.
 */
internal fun roleColorsFrom(
    accent: Color,
    accentGlow: Color,
    warning: Color,
    danger: Color,
    info: Color,
    success: Color,
    muted: Color,
): MilewayRoleColors =
    MilewayRoleColors(
        // Money takes the direction's accent: the amount IS the app's primary value, so picking a
        // direction should visibly change how money reads. Deliberately NOT domain-shifted — see
        // MilewayDomain: an amount must look identical in approvals and in expenses or the reader
        // cannot compare two screens.
        money = accent,
        // Distance inherits the info hue — the same lineage as the route polyline on the map.
        distance = info,
        approved = success,
        pending = warning,
        rejected = danger,
        // Between amber and red, at amber's tone: reads as "stop and look" without claiming a
        // decision has been made. Derived so a direction retuning warning/danger retunes this too.
        policyViolation = hueBlend(warning, danger, 0.5),
        // Queued is informational, not alarming: info's hue at low chroma so it recedes but stays
        // distinguishable from plain disabled text.
        offlineQueued = info.scaleChroma(0.42),
        // The live/recording state is the direction's glow ramp — this is the token every direction
        // already tunes for "alive". Stopping is not this; stopping is `destructive`.
        activeTracking = accentGlow,
        destructive = danger,
        informational = info,
        inactive = muted,
        // A shifted, intensified accent: unmistakably "special" while still inside the direction.
        premium = accent.rotateHue(42.0).scaleChroma(1.18),
    )

/** Roles for a curated design direction (Ledger / Signal / Paper / Instrument / Refined Ember / …). */
fun MilewaySchemeSpec.roleColors(): MilewayRoleColors =
    roleColorsFrom(
        accent = accent,
        accentGlow = accentGlow,
        warning = warning,
        danger = danger,
        info = info,
        success = success,
        muted = textMuted,
    )

/** Roles for the legacy seed / Material You path, where no [MilewaySchemeSpec] exists. */
internal fun derivedRoleColors(
    scheme: ColorScheme,
    semantic: MilewaySemanticColors,
): MilewayRoleColors =
    roleColorsFrom(
        accent = scheme.primary,
        accentGlow = semantic.accentGlow,
        warning = semantic.warning,
        danger = semantic.danger,
        info = semantic.info,
        success = semantic.success,
        muted = scheme.onSurfaceVariant,
    )

/** Fallback mirrors the default direction so a preview that forgets [MilewayTheme] still reads correctly. */
val LocalMilewayRoleColors: ProvidableCompositionLocal<MilewayRoleColors> =
    staticCompositionLocalOf { EmberSpec.roleColors() }

/**
 * How a screen reads Layer 2. Mirrors the `MaterialTheme.colorScheme` / [MilewayColors] ergonomics:
 *
 * ```
 * Text(text = amount, color = MilewayRoles.money)
 * Surface(color = MilewayRoles.tint(MilewayRoles.pending)) { StatusLabel("Awaiting approval") }
 * ```
 *
 * Nothing here takes a hex, and nothing here can be reached from outside a composable — which is
 * the point. A feature that wants a colour has to name a meaning to get one.
 */
object MilewayRoles {
    val money: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewayRoleColors.current.money
    val distance: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewayRoleColors.current.distance
    val approved: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewayRoleColors.current.approved
    val pending: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewayRoleColors.current.pending
    val rejected: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewayRoleColors.current.rejected
    val policyViolation: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewayRoleColors.current.policyViolation
    val offlineQueued: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewayRoleColors.current.offlineQueued
    val activeTracking: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewayRoleColors.current.activeTracking
    val destructive: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewayRoleColors.current.destructive
    val informational: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewayRoleColors.current.informational
    val inactive: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewayRoleColors.current.inactive
    val premium: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewayRoleColors.current.premium

    /**
     * The one sanctioned tinted background for a role — chip fill, callout surface, badge ground.
     *
     * Exists because the pre-migration codebase carried the same idea at five different alphas
     * (`.copy(alpha = 0.10f)`, `0.12f`, `0.15f`, `0.16f`, `0.35f`). One alpha, chosen for the
     * current surface's polarity, so every status chip in the app has the same weight.
     */
    @Composable
    @ReadOnlyComposable
    fun tint(role: Color): Color = role.copy(alpha = if (isLightSurface()) 0.12f else 0.18f)

    /**
     * A readable foreground for text/icons drawn **on top of** a solid [role] fill.
     *
     * Contrast-driven, not theme-driven: black-on-light / white-on-dark is the correct answer on
     * every design direction, so this deliberately does not consult the scheme.
     */
    fun onFilled(role: Color): Color = if (role.tone() > 60.0) Color(0xFF101010) else Color(0xFFFFFFFF)
}

@Composable
@ReadOnlyComposable
private fun isLightSurface(): Boolean =
    MaterialTheme.colorScheme.surface.tone() > MaterialTheme.colorScheme.onSurface.tone()
