package com.mileway.core.data.plugin

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * PLAN_V24 P0.1 — the PRESET resolution layer. Maps the active account to its persona preset's
 * plugin overrides (raw strings, uniform with every other layer). P0.2 supplies the real
 * implementation from `:stub` (four seeded personas); until then [EmptyPersonaPresetProvider]
 * keeps the registry resolving to DEFAULT.
 */
interface PersonaPresetProvider {
    /** Raw plugin overrides (toggle "true"/"false" and value strings) for the account's persona. */
    fun presetOverrides(accountId: String?): Flow<Map<String, String>>

    /** The personas the Master Plugin page's switcher (P0.3) can apply. Empty if none. */
    fun availablePersonas(): List<PersonaSummary> = emptyList()
}

/**
 * A persona the Master Plugin page can apply onto the current account. [nameKey]/[descriptionKey]
 * are Compose-resource string names (resolved at the UI layer, like plugin descriptors);
 * [overrides] is the same raw map [PersonaPresetProvider.presetOverrides] returns.
 */
data class PersonaSummary(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val overrides: Map<String, String>,
)

/** No-preset default: every account resolves to descriptor defaults. Replaced by P0.2. */
object EmptyPersonaPresetProvider : PersonaPresetProvider {
    override fun presetOverrides(accountId: String?): Flow<Map<String, String>> = flowOf(emptyMap())
}

/**
 * PLAN_V24 P0.1 — the FORCED resolution layer (highest priority). Debug forces survive account
 * switch (they are not per-account — mirrors the reference app `DebugDataStore`). The Master Plugin page's
 * experimental section (P0.3) and the tuning editor (P10.3) write here. Platform DataStore-backed
 * implementations live in androidMain/iosMain ([PluginDebugForceStore]); tests use
 * [InMemoryPluginDebugForceSource].
 */
interface PluginDebugForceSource {
    /** Raw forced overrides by plugin id ("true"/"false" for toggles, value strings otherwise). */
    val overrides: Flow<Map<String, String>>

    /** Set or (with null) clear a forced override. */
    suspend fun setForce(
        id: String,
        raw: String?,
    )

    /** Clear every forced override. */
    suspend fun clearAll()
}

/** In-memory [PluginDebugForceSource] for unit tests and as a safe default binding. */
class InMemoryPluginDebugForceSource(
    initial: Map<String, String> = emptyMap(),
) : PluginDebugForceSource {
    private val state = MutableStateFlow(initial)
    override val overrides: Flow<Map<String, String>> = state.asStateFlow()

    override suspend fun setForce(
        id: String,
        raw: String?,
    ) {
        state.value = state.value.toMutableMap().apply { if (raw == null) remove(id) else put(id, raw) }
    }

    override suspend fun clearAll() {
        state.value = emptyMap()
    }
}
