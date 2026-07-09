package com.mileway.core.data.session

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * PLAN_V24 P7.3: contract coverage for the session-delegation overlay (start/end/isActingAsDelegate/
 * nested-block), exercised against [InMemoryDelegationSessionSource] which holds the same logic the
 * DataStore-backed [DelegationSessionController] does. The real DataStore round-trip is exercised by
 * instrumentation, mirroring how [ActiveAccountStore] is faked in `ActiveAccountStoreTest`.
 */
class DelegationSessionControllerTest {
    @Test
    fun `starts out not acting`() =
        runTest {
            val source = InMemoryDelegationSessionSource()
            assertFalse(source.isActingAsDelegate.first())
            assertFalse(source.delegationState.first().isActing)
        }

    @Test
    fun `startDelegation flips to acting with the delegate identity`() =
        runTest {
            val source = InMemoryDelegationSessionSource()

            val started = source.startDelegation("Priya Sharma", "priya@mileway.app", "EMP-2101")

            assertTrue(started)
            assertTrue(source.isActingAsDelegate.first())
            val state = source.delegationState.first()
            assertEquals("Priya Sharma", state.actingName)
            assertEquals("priya@mileway.app", state.actingEmail)
            assertEquals("EMP-2101", state.actingCode)
        }

    @Test
    fun `nested delegation is blocked while already acting`() =
        runTest {
            val source = InMemoryDelegationSessionSource()
            source.startDelegation("Priya Sharma", "priya@mileway.app", "EMP-2101")

            val nested = source.startDelegation("Rahul Mehra", "rahul@mileway.app", "EMP-2102")

            assertFalse(nested, "cannot start a second delegation while one is active")
            // The original delegate is retained, not overwritten.
            assertEquals("EMP-2101", source.delegationState.first().actingCode)
        }

    @Test
    fun `endDelegation restores the base identity`() =
        runTest {
            val source = InMemoryDelegationSessionSource()
            source.startDelegation("Priya Sharma", "priya@mileway.app", "EMP-2101")

            source.endDelegation()

            assertFalse(source.isActingAsDelegate.first())
            val state = source.delegationState.first()
            assertFalse(state.isActing)
            assertNull(state.actingCode)
            // And a fresh delegation can begin again afterwards.
            assertTrue(source.startDelegation("Rahul Mehra", "rahul@mileway.app", "EMP-2102"))
        }
}
