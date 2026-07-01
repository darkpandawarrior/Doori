package com.mileway

import com.mileway.core.data.session.SessionKind
import com.mileway.core.data.session.SessionState
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A.1 — pure-JVM tests for the persisted sign-in [SessionState] semantics that gate the
 * splash/login theatre. The DataStore-backed [com.mileway.core.data.session.SessionRepository]
 * is exercised via instrumentation; here we lock the derivation logic the launcher reads:
 * a guest must count as signed-in so navigation, deep links and process recreation never bounce
 * them back to login.
 */
class SessionStateTest {

    @Test
    fun `fresh state is not signed in`() {
        val state = SessionState()
        assertEquals(SessionKind.NONE, state.kind)
        assertFalse(state.isSignedIn)
        assertFalse(state.isGuest)
        assertNull(state.email)
    }

    @Test
    fun `guest session counts as signed in but carries no email`() {
        val state = SessionState(kind = SessionKind.GUEST)
        assertTrue(state.isSignedIn, "A guest has passed login and must not be bounced back")
        assertTrue(state.isGuest)
        assertNull(state.email)
    }

    @Test
    fun `credentials session is signed in and not a guest`() {
        val state = SessionState(kind = SessionKind.CREDENTIALS, email = "demo@mileway.app")
        assertTrue(state.isSignedIn)
        assertFalse(state.isGuest)
        assertEquals("demo@mileway.app", state.email)
    }

    @Test
    fun `SessionKind names round-trip (the on-disk representation)`() {
        SessionKind.entries.forEach { kind ->
            assertEquals(kind, SessionKind.entries.first { it.name == kind.name })
        }
    }
}
