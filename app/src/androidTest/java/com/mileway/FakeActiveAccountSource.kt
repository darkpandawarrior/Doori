package com.mileway

import com.mileway.core.data.session.ActiveAccountSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory fake for [ActiveAccountSource] (P2.1) — lets tests construct `AuthViewModel` without a
 * DataStore-backed `Context`. Duplicated into androidTest (the JVM `src/test` copy is not visible
 * to the instrumented source set). [seed] pre-populates the pointer.
 */
class FakeActiveAccountSource(seed: String? = null) : ActiveAccountSource {
    private val current = MutableStateFlow(seed)

    override val activeAccountId: Flow<String?> = current

    override suspend fun setActiveAccountId(accountId: String) {
        current.value = accountId
    }
}
