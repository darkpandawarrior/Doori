package com.mileway.core.data.banner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** PLAN_V24 P13.1: the pure priority-ordering + dismissal-filtering logic of the banner stack. */
class BannerAssemblerTest {
    @Test
    fun `orders by ascending priority`() {
        val out =
            BannerAssembler.assemble(
                banners =
                    listOf(
                        Banner.SubscriptionExpiry(daysLeft = 3),
                        Banner.UpdateReady,
                        Banner.Delegate(name = "Asha"),
                    ),
                dismissedIds = emptySet(),
            )
        assertEquals(
            listOf(Banner.UpdateReady.priority, Banner.Delegate("Asha").priority, Banner.SubscriptionExpiry(3).priority),
            out.map { it.priority },
        )
        assertEquals(Banner.UpdateReady, out.first())
    }

    @Test
    fun `filters out a dismissed dismissible banner`() {
        val custom = Banner.Custom(text = "Maintenance tonight")
        val out =
            BannerAssembler.assemble(
                banners = listOf(custom, Banner.DeletionRequested),
                dismissedIds = setOf(custom.id),
            )
        assertTrue(out.none { it.id == custom.id })
        assertTrue(out.any { it is Banner.DeletionRequested })
    }

    @Test
    fun `non-dismissible banners survive dismissal`() {
        val delegate = Banner.Delegate(name = "Ravi")
        val out =
            BannerAssembler.assemble(
                banners = listOf(delegate, Banner.UpdateReady),
                // Both ids present in the set — they must be ignored for non-dismissible banners.
                dismissedIds = setOf(delegate.id, Banner.UpdateReady.id),
            )
        assertEquals(2, out.size)
        assertTrue(out.contains(delegate))
        assertTrue(out.contains(Banner.UpdateReady))
    }
}
