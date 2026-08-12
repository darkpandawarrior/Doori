package com.mileway.core.ui.theme

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The load-bearing properties of the three-layer theme system. If any of these break, a design
 * direction stops applying and the app goes back to looking incoherent no matter which theme is
 * chosen — which is the exact failure this system exists to prevent.
 */
class ThemeLayersTest {
    private val variants = MilewayThemeVariant.entries

    // ── Layer 2: SEMANTIC ─────────────────────────────────────────────────────────────────────

    @Test
    fun everyDirectionProducesItsOwnRoles() {
        // The whole point: picking a direction must move the product's colours. If two directions
        // agreed on every role, one of them was rendering the other's palette.
        val moneys = variants.map { it.spec.roleColors().money }.toSet()
        assertEquals(
            variants.map { it.spec.accent }.toSet().size,
            moneys.size,
            "money must track each direction's accent, not a fixed hex",
        )
        assertTrue(
            variants.map { it.spec.roleColors() }.toSet().size > 1,
            "role bundles collapsed to one — the semantic layer is theme-blind",
        )
    }

    @Test
    fun policyViolationSitsBetweenPendingAndRejected() {
        // It must be its own signal: a policy flag is not "rejected" (nobody decided yet) and not
        // plain "pending" (something is wrong). Distinct from both, on every direction.
        variants.forEach { v ->
            val r = v.spec.roleColors()
            assertNotEquals(r.pending, r.policyViolation, "${v.id}: policyViolation == pending")
            assertNotEquals(r.rejected, r.policyViolation, "${v.id}: policyViolation == rejected")
            // Derived at pending's tone, so it never becomes the least-legible thing on the screen.
            assertToneEquals(r.pending, r.policyViolation, "${v.id} policyViolation")
        }
    }

    @Test
    fun offlineQueuedIsQuieterThanInformational() {
        // Queued is a normal offline-first state, not an alert: same hue family, visibly calmer.
        variants.forEach { v ->
            val r = v.spec.roleColors()
            assertTrue(
                r.offlineQueued.hct().chroma < r.informational.hct().chroma,
                "${v.id}: offlineQueued is not muted relative to informational",
            )
            assertToneEquals(r.informational, r.offlineQueued, "${v.id} offlineQueued")
        }
    }

    @Test
    fun stoppingIsDestructiveAndLiveIsNot() {
        // The live_drive stop button rendered the same red under all five directions because it was
        // a hex. destructive follows each direction's danger; activeTracking follows its glow ramp.
        assertTrue(
            variants.map { it.spec.roleColors().destructive }.toSet().size > 1,
            "destructive is identical on every direction — still theme-blind",
        )
        assertTrue(
            variants.map { it.spec.roleColors().activeTracking }.toSet().size > 1,
            "activeTracking is identical on every direction — still theme-blind",
        )
    }

    @Test
    fun onFilledPicksTheReadableForeground() {
        variants.forEach { v ->
            val r = v.spec.roleColors()
            listOf(r.money, r.approved, r.pending, r.rejected, r.destructive).forEach { fill ->
                val fg = MilewayRoles.onFilled(fill)
                assertTrue(
                    abs(fg.tone() - fill.tone()) > 40.0,
                    "${v.id}: onFilled returned a foreground within 40 tone of its fill",
                )
            }
        }
    }

    // ── Layer 3: DOMAIN ───────────────────────────────────────────────────────────────────────

    @Test
    fun domainOverlayPreservesToneOnEveryDirection() {
        // THE guarantee. A domain rotates hue and scales chroma at constant tone, so contrast
        // against the direction's surfaces is preserved by construction — no per-domain WCAG
        // re-verification, and no way for a future domain to darken itself into illegibility.
        variants.forEach { v ->
            val base = v.colorScheme()
            MilewayDomain.entries.forEach { domain ->
                val shifted = base.withDomainAccent(domain)
                assertToneEquals(base.primary, shifted.primary, "${v.id}/$domain primary")
                assertToneEquals(base.secondary, shifted.secondary, "${v.id}/$domain secondary")
                assertToneEquals(base.primaryContainer, shifted.primaryContainer, "${v.id}/$domain container")
            }
        }
    }

    @Test
    fun domainOverlayNeverTouchesSurfacesOrRoles() {
        // A domain carries identity; it must not restyle the app underneath it, or "derive from the
        // base" has quietly become "override the base".
        val base = MilewayThemeVariant.DEFAULT.colorScheme()
        MilewayDomain.entries.forEach { domain ->
            val shifted = base.withDomainAccent(domain)
            assertEquals(base.background, shifted.background, "$domain moved the canvas")
            assertEquals(base.surface, shifted.surface, "$domain moved a surface")
            assertEquals(base.surfaceContainerHigh, shifted.surfaceContainerHigh, "$domain moved a surface")
            assertEquals(base.onSurface, shifted.onSurface, "$domain moved body text")
            assertEquals(base.outline, shifted.outline, "$domain moved borders")
            assertEquals(base.error, shifted.error, "$domain moved the error role")
        }
    }

    @Test
    fun eachNonIdentityDomainIsActuallyDistinct() {
        val base = MilewayThemeVariant.DEFAULT.colorScheme()
        val accents = MilewayDomain.entries.map { it to base.withDomainAccent(it).primary }
        val nonIdentity = accents.filterNot { it.first.isIdentity }
        assertEquals(
            nonIdentity.size,
            nonIdentity.map { it.second }.toSet().size,
            "two domains resolved to the same accent",
        )
        nonIdentity.forEach { (domain, accent) ->
            assertNotEquals(base.primary, accent, "$domain produced the base accent unchanged")
        }
    }

    @Test
    fun identityDomainsAreExactPassThrough() {
        val base = MilewayThemeVariant.DEFAULT.colorScheme()
        listOf(MilewayDomain.NONE, MilewayDomain.TRACKING).forEach {
            assertEquals(base, base.withDomainAccent(it), "$it is not a pass-through")
        }
    }

    @Test
    fun switchingDirectionMovesEveryDomain() {
        // Approvals under Paper must be a Paper colour. This is the property ApprovalsScreen's
        // hardcoded 0xFF6C63FF gradient violated across all five directions.
        MilewayDomain.entries.filterNot { it.isIdentity }.forEach { domain ->
            val perDirection = variants.map { it.colorScheme().withDomainAccent(domain).primary }.toSet()
            assertTrue(
                perDirection.size > 1,
                "$domain renders the same accent on every design direction",
            )
        }
    }

    private fun assertToneEquals(
        expected: androidx.compose.ui.graphics.Color,
        actual: androidx.compose.ui.graphics.Color,
        label: String,
    ) {
        // 1.5 tone ≈ the rounding an 8-bit sRGB round-trip through HCT can introduce.
        assertTrue(
            abs(expected.tone() - actual.tone()) <= 1.5,
            "$label: tone drifted ${expected.tone()} → ${actual.tone()}",
        )
    }
}
