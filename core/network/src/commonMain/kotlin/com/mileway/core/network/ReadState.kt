package com.mileway.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/** PLAN_V33 A6: a screen's cache-then-refresh read state. */
sealed interface ReadState<out T> {
    data object Loading : ReadState<Nothing>

    /** [isStale] is true when this came from [cache] with no successful online refresh backing it. */
    data class Content<T>(val data: T, val isStale: Boolean) : ReadState<T>

    data class Error(val message: String?) : ReadState<Nothing>
}

/**
 * Cache-then-refresh read for an offline-capable screen: emits [cache] immediately, and when
 * [isOnline] launches [refresh] (which is expected to write back into whatever backs [cache], so
 * a successful refresh surfaces as a fresh [ReadState.Content] emission from [cache] itself
 * re-emitting). Offline with no cached value yet surfaces as [ReadState.Error]; offline with a
 * cached value serves it as stale.
 *
 * // ponytail: single policy, add an enum at the 2nd — no FetchPolicy variants (cache-first vs
 * network-first vs cache-only) until a second real caller needs one.
 */
fun <T> screenStateStream(
    cache: Flow<T?>,
    isOnline: () -> Boolean,
    refresh: suspend () -> Unit,
): Flow<ReadState<T>> =
    channelFlow {
        val online = isOnline()
        if (online) {
            launch { runCatching { refresh() } }
        }
        cache.collect { value ->
            send(
                when {
                    value != null -> ReadState.Content(value, isStale = !online)
                    online -> ReadState.Loading
                    else -> ReadState.Error("Offline and no cached data")
                },
            )
        }
    }
