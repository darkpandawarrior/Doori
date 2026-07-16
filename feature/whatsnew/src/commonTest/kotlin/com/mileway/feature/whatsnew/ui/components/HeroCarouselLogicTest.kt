package com.mileway.feature.whatsnew.ui.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * PLAN_V36 P6 — pure-logic coverage for the auto-advance gate ([shouldAutoAdvance]) and the
 * shared-element key builders, both extracted specifically so they're testable without a Compose
 * test harness (spec §6.1/§6.3).
 */
class HeroCarouselLogicTest {
    @Test
    fun `auto advance runs only when not reduced-motion, not zoomed, not mid-scroll`() {
        assertTrue(shouldAutoAdvance(reducedMotion = false, isZoomed = false, isScrollInProgress = false))
    }

    @Test
    fun `reduced motion blocks auto advance`() {
        assertFalse(shouldAutoAdvance(reducedMotion = true, isZoomed = false, isScrollInProgress = false))
    }

    @Test
    fun `zoomed page blocks auto advance`() {
        assertFalse(shouldAutoAdvance(reducedMotion = false, isZoomed = true, isScrollInProgress = false))
    }

    @Test
    fun `mid-scroll blocks auto advance`() {
        assertFalse(shouldAutoAdvance(reducedMotion = false, isZoomed = false, isScrollInProgress = true))
    }

    @Test
    fun `hero and title shared keys are distinct for the same entry`() {
        val entryId = "v36-shared-transitions"
        assertNotEquals(whatsNewHeroSharedKey(entryId), whatsNewTitleSharedKey(entryId))
    }

    @Test
    fun `shared keys differ across entries`() {
        assertNotEquals(whatsNewHeroSharedKey("a"), whatsNewHeroSharedKey("b"))
        assertNotEquals(whatsNewTitleSharedKey("a"), whatsNewTitleSharedKey("b"))
    }
}
