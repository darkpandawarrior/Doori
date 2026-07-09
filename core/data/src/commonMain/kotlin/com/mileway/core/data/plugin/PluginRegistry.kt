package com.mileway.core.data.plugin

import com.mileway.core.data.dao.PluginOverrideDao
import com.mileway.core.data.model.db.PluginOverrideEntity
import com.mileway.core.data.session.ActiveAccountSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * PLAN_V24 P0.1 — the single feature-composition mechanism. Resolves every plugin's value by
 * layering four sources, highest priority first:
 *
 * ```
 * FORCED (debug)  →  USER (per-account override)  →  PRESET (persona)  →  DEFAULT (descriptor)
 * ```
 *
 * All layers persist as raw strings keyed by plugin id, so one code path serves TILE, CAPABILITY
 * and VALUE plugins. The composition root reads [observe]/[observeValue]; the Master Plugin page
 * (P0.3) additionally reads [observeSource] to show *which* layer won.
 *
 * The reference app merges its three tiers invisibly inside helper methods; Mileway externalizes the merge here
 * so [observeSource] can name the winning layer per flag (the demo story).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PluginRegistry(
    private val catalog: PluginCatalog = PluginCatalog,
    private val overrideDao: PluginOverrideDao,
    private val activeAccount: ActiveAccountSource,
    private val presets: PersonaPresetProvider = EmptyPersonaPresetProvider,
    private val debugForce: PluginDebugForceSource,
) {
    private data class Layers(
        val accountId: String?,
        val user: Map<String, String>,
        val preset: Map<String, String>,
        val force: Map<String, String>,
    )

    /** Live snapshot of all four layers, re-derived whenever any of them changes. */
    private val layers: Flow<Layers> =
        activeAccount.activeAccountId.flatMapLatest { accountId ->
            val overridesFlow =
                if (accountId == null) {
                    flowOf(emptyList())
                } else {
                    overrideDao.observeForAccount(accountId)
                }
            combine(
                overridesFlow,
                presets.presetOverrides(accountId),
                debugForce.overrides,
            ) { overrides, preset, force ->
                Layers(
                    accountId = accountId,
                    user = overrides.associate { it.pluginId to it.value },
                    preset = preset,
                    force = force,
                )
            }
        }

    /** Resolved on/off for a TILE/CAPABILITY plugin (false for an unknown or VALUE id). */
    fun observe(id: String): Flow<Boolean> {
        val descriptor = catalog.byId(id) ?: return flowOf(false)
        return layers.map { resolveValue(descriptor, it) }
            .map { (it as? PluginValue.Bool)?.value ?: descriptor.defaultOn }
            .distinctUntilChanged()
    }

    /** Resolved typed value for any plugin (default for an unknown id). */
    fun observeValue(id: String): Flow<PluginValue> {
        val descriptor = catalog.byId(id) ?: return flowOf(PluginValue.Bool(false))
        return layers.map { resolveValue(descriptor, it) }.distinctUntilChanged()
    }

    /** Which layer currently supplies [id]'s value (DEFAULT for an unknown id). */
    fun observeSource(id: String): Flow<PluginSource> {
        if (catalog.byId(id) == null) return flowOf(PluginSource.DEFAULT)
        return layers.map { sourceOf(id, it) }.distinctUntilChanged()
    }

    /** One-shot resolved value (for non-reactive callers). */
    suspend fun value(id: String): PluginValue = catalog.byId(id)?.let { resolveValue(it, layers.first()) } ?: PluginValue.Bool(false)

    /** Every catalog plugin resolved with its winning value + source — drives the Master Plugin page. */
    fun observeResolved(): Flow<List<ResolvedPlugin>> =
        layers.map { l ->
            catalog.all.map { descriptor ->
                ResolvedPlugin(descriptor, resolveValue(descriptor, l), sourceOf(descriptor.id, l))
            }
        }

    /**
     * PLAN_V24 P0.3 — the experimental-section unlock (the reference app's tap-to-unlock pattern),
     * persisted per-account by reusing the USER-override table under a reserved id (no new store).
     * The id is not a catalog plugin, so it never renders as a togglable row.
     */
    fun observeExperimentalUnlocked(): Flow<Boolean> =
        activeAccount.activeAccountId.flatMapLatest { accountId ->
            if (accountId == null) {
                flowOf(false)
            } else {
                overrideDao.observeForAccount(accountId)
                    .map { rows -> rows.any { it.pluginId == EXPERIMENTAL_UNLOCK_ID && it.value == "true" } }
                    .distinctUntilChanged()
            }
        }

    suspend fun unlockExperimental() {
        val accountId = activeAccount.activeAccountId.first() ?: return
        overrideDao.upsert(PluginOverrideEntity(accountId, EXPERIMENTAL_UNLOCK_ID, "true"))
    }

    /** Set a per-account USER override on the active account (no-op with no active account). */
    suspend fun setUserOverride(
        id: String,
        value: PluginValue,
    ) {
        val accountId = activeAccount.activeAccountId.first() ?: return
        overrideDao.upsert(PluginOverrideEntity(accountId, id, value.toRaw()))
    }

    /** Clear a single USER override, falling back to PRESET/DEFAULT. */
    suspend fun clearUserOverride(id: String) {
        val accountId = activeAccount.activeAccountId.first() ?: return
        overrideDao.delete(accountId, id)
    }

    /** Clear every USER override for the active account ("reset to persona defaults", P0.3). */
    suspend fun resetActiveAccountToPreset() {
        val accountId = activeAccount.activeAccountId.first() ?: return
        overrideDao.deleteForAccount(accountId)
    }

    private fun rawFor(
        id: String,
        layers: Layers,
    ): Pair<String, PluginSource>? =
        when {
            layers.force.containsKey(id) -> layers.force.getValue(id) to PluginSource.FORCED
            layers.user.containsKey(id) -> layers.user.getValue(id) to PluginSource.USER
            layers.preset.containsKey(id) -> layers.preset.getValue(id) to PluginSource.PRESET
            else -> null
        }

    private fun resolveValue(
        descriptor: PluginDescriptor,
        layers: Layers,
    ): PluginValue =
        rawFor(descriptor.id, layers)
            ?.let { (raw, _) -> PluginValue.parse(raw, descriptor) }
            ?: descriptor.defaultValue

    private fun sourceOf(
        id: String,
        layers: Layers,
    ): PluginSource = rawFor(id, layers)?.second ?: PluginSource.DEFAULT

    private companion object {
        const val EXPERIMENTAL_UNLOCK_ID = "__experimental_unlocked"
    }
}

/** One catalog plugin resolved to its winning value + which layer won. */
data class ResolvedPlugin(
    val descriptor: PluginDescriptor,
    val value: PluginValue,
    val source: PluginSource,
)
