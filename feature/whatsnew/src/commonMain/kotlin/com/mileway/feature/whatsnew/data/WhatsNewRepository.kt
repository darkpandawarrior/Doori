package com.mileway.feature.whatsnew.data

import com.mileway.core.data.whatsnew.WhatsNewVersionProvider
import com.mileway.feature.whatsnew.model.WhatsNewEntry

/**
 * PLAN_V36 P1 — every consumer (sheet, list, detail, badge) reads through this interface, never
 * [WhatsNewCatalog] directly. This is the seam the future Ktor backend swaps (per the
 * backend-greenlit plan, repositories must already look one implementation swap away from a real
 * data source — this one literally is).
 *
 * Extends [WhatsNewVersionProvider] so `:feature:profile`'s Settings badge can depend on just the
 * version number without a feature-to-feature dependency on `:feature:whatsnew`.
 */
interface WhatsNewRepository : WhatsNewVersionProvider {
    /** All entries, sorted by [WhatsNewEntry.releasedOn] descending (newest first). */
    fun entries(): List<WhatsNewEntry>

    /** The entry with this id, or null if unknown. */
    fun entry(id: String): WhatsNewEntry?

    /** The max [WhatsNewEntry.version] across the catalog — the badge comparand. */
    override val currentVersion: Int
}
