package com.mileway

import com.mileway.core.data.session.PinHashSource

/**
 * In-memory fake for [PinHashSource] (P2.3) — lets JVM tests construct `SwitchAccountViewModel`
 * without a DataStore-backed `Context`, mirroring [FakeActiveAccountSource]'s shape. [seed]
 * pre-populates per-account hashes, e.g. to simulate a PIN set on a prior run.
 */
class FakePinHashSource(
    seed: Map<String, String> = emptyMap(),
) : PinHashSource {
    private val hashes = seed.toMutableMap()

    override suspend fun getPinHash(accountId: String): String? = hashes[accountId]

    override suspend fun setPinHash(
        accountId: String,
        pinHash: String,
    ) {
        hashes[accountId] = pinHash
    }
}
