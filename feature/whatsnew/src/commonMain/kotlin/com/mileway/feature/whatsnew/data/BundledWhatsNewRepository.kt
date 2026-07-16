package com.mileway.feature.whatsnew.data

import com.mileway.feature.whatsnew.model.WhatsNewEntry

/**
 * PLAN_V36 P1 — [WhatsNewRepository] backed by the bundled [WhatsNewCatalog]. The [catalog]
 * constructor param defaults to the real catalog but stays overridable so tests can inject a
 * fixed entry list without touching the shipped content.
 */
class BundledWhatsNewRepository(
    private val catalog: List<WhatsNewEntry> = WhatsNewCatalog.entries,
) : WhatsNewRepository {
    private val sortedByReleaseDesc = catalog.sortedByDescending { it.releasedOn }

    override fun entries(): List<WhatsNewEntry> = sortedByReleaseDesc

    override fun entry(id: String): WhatsNewEntry? = catalog.firstOrNull { it.id == id }

    override val currentVersion: Int = catalog.maxOf { it.version }
}
