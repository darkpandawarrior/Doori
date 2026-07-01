package com.mileway.core.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/** Local persistence for referral state (in-memory default; DataStore-backed in production). */
interface ReferralStore {
    fun myCode(): String?

    fun setMyCode(code: String)

    val pending: Flow<ReferralData?>

    fun setPending(data: ReferralData?)

    fun isRedeemed(code: String): Boolean

    fun markRedeemed(code: String)
}

/** Process-lifetime in-memory referral store (demo default). */
class InMemoryReferralStore : ReferralStore {
    private var code: String? = null
    private val pendingState = MutableStateFlow<ReferralData?>(null)
    private val redeemed = mutableSetOf<String>()

    override fun myCode(): String? = code

    override fun setMyCode(code: String) {
        this.code = code
    }

    override val pending: Flow<ReferralData?> = pendingState.asStateFlow()

    override fun setPending(data: ReferralData?) {
        pendingState.value = data
    }

    override fun isRedeemed(code: String): Boolean = code in redeemed

    override fun markRedeemed(code: String) {
        redeemed.add(code)
    }
}

/**
 * RF.1: commonMain [ReferralManager]: generates a stable per-install code, exposes the pending captured
 * referral, and redeems codes against a local mock (no backend). The platform impls (RF.2 Android
 * InstallReferrer, RF.3 iOS deferred link) call [capture] to push an attribution into the shared store.
 */
class LocalReferralManager(
    private val store: ReferralStore = InMemoryReferralStore(),
    private val codeGenerator: () -> String = ::defaultCode,
) : ReferralManager {
    override suspend fun myReferralCode(): String {
        store.myCode()?.let { return it }
        return codeGenerator().also { store.setMyCode(it) }
    }

    override fun pendingReferral(): Flow<ReferralData?> = store.pending

    override suspend fun redeem(code: String): Boolean {
        val normalized = code.trim().uppercase()
        if (normalized.isEmpty() || store.isRedeemed(normalized)) return false
        store.markRedeemed(normalized)
        store.setPending(ReferralData(code = normalized, source = "manual"))
        return true
    }

    /** Push a platform-captured attribution (install referrer / deferred deep link) into the store. */
    fun capture(data: ReferralData) {
        store.setPending(data)
    }

    companion object {
        private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        fun defaultCode(): String {
            val suffix = (1..6).map { ALPHABET[Random.nextInt(ALPHABET.length)] }.joinToString("")
            return "MT$suffix"
        }
    }
}
