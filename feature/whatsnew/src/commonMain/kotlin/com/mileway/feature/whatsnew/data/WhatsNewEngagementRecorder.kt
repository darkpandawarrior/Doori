package com.mileway.feature.whatsnew.data

import com.siddharth.kmp.offlineoutbox.OpOutbox
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock

/**
 * PLAN_V36 P7 (spec §8) — offline-first mirror of the reference marketing flow's
 * FEATURE-OPENED / INTERESTED-CLICK analytics POSTs: every What's New engagement just appends an
 * op to the durable [OpOutbox] instead of firing a network call. [Clock.System] is read here (the
 * runtime call site), never in the catalog — the determinism rule only binds catalog content.
 *
 * // ponytail: no drain wired — backend phase owns replay
 */
class WhatsNewEngagementRecorder(
    private val outbox: OpOutbox,
) {
    suspend fun record(
        type: String,
        entryId: String,
    ) {
        val payload =
            buildJsonObject {
                put("entryId", entryId)
                put("atMs", Clock.System.now().toEpochMilliseconds())
            }
        outbox.enqueue(type, payload.toString())
    }

    companion object {
        const val TYPE_OPENED = "whatsnew_opened"
        const val TYPE_SHARED = "whatsnew_shared"
        const val TYPE_CONTACT = "whatsnew_contact"
        const val TYPE_BANNER_OPEN = "whatsnew_banner_open"
    }
}
