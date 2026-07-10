package com.mileway.core.data.popup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** PLAN_V24 P13.3: the pure at-most-one / priority / acknowledgement logic of the popup coordinator. */
class PopupCoordinatorTest {
    private val signature = PopupRequest(PopupRequest.ID_SIGNATURE_RESIGN, PopupRequest.SIGNATURE_RESIGN)
    private val offer = PopupRequest(PopupRequest.ID_OFFER, PopupRequest.OFFER)

    @Test
    fun `returns the single highest-priority candidate`() {
        // Both eligible on the same app-open — only the lowest-priority (signature) shows.
        val next = PopupCoordinator.next(candidates = listOf(offer, signature), acknowledgedIds = emptySet())
        assertEquals(signature, next)
    }

    @Test
    fun `skips acknowledged candidates and falls through to the next`() {
        val next = PopupCoordinator.next(candidates = listOf(signature, offer), acknowledgedIds = setOf(signature.id))
        assertEquals(offer, next)
    }

    @Test
    fun `returns null when everything is acknowledged`() {
        val next = PopupCoordinator.next(candidates = listOf(signature, offer), acknowledgedIds = setOf(signature.id, offer.id))
        assertNull(next)
    }

    @Test
    fun `returns null when there are no candidates`() {
        assertNull(PopupCoordinator.next(candidates = emptyList(), acknowledgedIds = emptySet()))
    }
}
