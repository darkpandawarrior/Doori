package com.miletracker

import app.cash.turbine.test
import com.miletracker.core.platform.InMemoryPushTokenStore
import com.miletracker.core.platform.LocalPushMessaging
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** FCM.1 — push token store + local messaging. */
class PushMessagingTest {
    @Test
    fun `token defaults to null and updates`() =
        runTest {
            val store = InMemoryPushTokenStore()
            val messaging = LocalPushMessaging(store)
            assertNull(messaging.currentToken())
            store.setToken("tok-123")
            assertEquals("tok-123", messaging.currentToken())
        }

    @Test
    fun `subscribe and unsubscribe mutate the topic set`() =
        runTest {
            val store = InMemoryPushTokenStore()
            val messaging = LocalPushMessaging(store)
            messaging.subscribeTopic("trips")
            messaging.subscribeTopic("offers")
            assertTrue(store.subscribedTopics().containsAll(setOf("trips", "offers")))
            messaging.unsubscribeTopic("offers")
            assertEquals(setOf("trips"), store.subscribedTopics())
        }

    @Test
    fun `onTokenRefresh emits non-null tokens`() =
        runTest {
            val store = InMemoryPushTokenStore()
            val messaging = LocalPushMessaging(store)
            messaging.onTokenRefresh.test {
                store.setToken("first")
                assertEquals("first", awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
