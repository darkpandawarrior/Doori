package com.mileway

import com.mileway.core.data.session.ActiveAccountSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory fake for [ActiveAccountSource] (P2.1) — lets JVM tests construct `ProfileViewModel`
 * without a DataStore-backed `Context`, mirroring the same `FakeMockAccountDao` shape already
 * used elsewhere in this test suite. [seed] pre-populates the pointer, e.g. to simulate a
 * persisted value surviving process death across a fresh `ProfileViewModel` instance.
 */
class FakeActiveAccountSource(
    seed: String? = null,
) : ActiveAccountSource {
    private val current = MutableStateFlow(seed)

    override val activeAccountId: Flow<String?> = current

    override suspend fun setActiveAccountId(accountId: String) {
        current.value = accountId
    }
}
