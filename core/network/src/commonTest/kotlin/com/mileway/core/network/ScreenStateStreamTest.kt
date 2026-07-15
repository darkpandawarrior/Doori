package com.mileway.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** PLAN_V33 A6: [screenStateStream]'s cache-first, refresh-when-online contract. */
class ScreenStateStreamTest {
    @Test
    fun `online emits cache first then the refreshed value once refresh writes back`() =
        runTest {
            val cache = MutableStateFlow<String?>(null)
            val stream =
                screenStateStream(
                    cache = cache,
                    isOnline = { true },
                    refresh = { cache.value = "fresh" },
                )

            val emissions = stream.take(2).toList()

            assertIs<ReadState.Loading>(emissions[0])
            val second = emissions[1]
            assertIs<ReadState.Content<String>>(second)
            assertEquals("fresh", second.data)
            assertEquals(false, second.isStale)
        }

    @Test
    fun `offline with a cached value serves it as stale and never calls refresh`() =
        runTest {
            val cache = MutableStateFlow<String?>("cached")
            var refreshCalls = 0
            val stream =
                screenStateStream(
                    cache = cache,
                    isOnline = { false },
                    refresh = { refreshCalls++ },
                )

            val first = stream.take(1).toList().single()

            assertIs<ReadState.Content<String>>(first)
            assertEquals("cached", first.data)
            assertEquals(true, first.isStale)
            assertEquals(0, refreshCalls)
        }

    @Test
    fun `offline with no cached value is an error, not an infinite loading state`() =
        runTest {
            val cache = MutableStateFlow<String?>(null)
            val stream = screenStateStream(cache = cache, isOnline = { false }, refresh = { })

            val first = stream.take(1).toList().single()

            assertIs<ReadState.Error>(first)
        }
}
