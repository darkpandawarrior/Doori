package com.mileway.core.data.outbox

import com.mileway.core.data.dao.SubmitDraftDao
import com.mileway.core.data.model.db.SubmitDraftEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomSubmitOutbox(
    private val dao: SubmitDraftDao,
    private val json: Json,
) : SubmitOutbox<TripDraft> {
    override fun drafts(formKey: String): Flow<List<DraftEntry<TripDraft>>> =
        dao.observeByFormKey(formKey).map { rows ->
            rows.map { row ->
                DraftEntry(
                    formKey = row.formKey,
                    uniqueKey = row.uniqueKey,
                    payload = json.decodeFromString(row.payloadJson),
                    status = DraftStatus.valueOf(row.status),
                    errorMessage = row.errorMessage,
                    createdAt = row.createdAt,
                    updatedAt = row.updatedAt,
                )
            }
        }

    override suspend fun enqueue(
        formKey: String,
        uniqueKey: String,
        payload: TripDraft,
    ) {
        val now = epochMillis()
        dao.upsert(
            SubmitDraftEntity(
                formKey = formKey,
                uniqueKey = uniqueKey,
                payloadJson = json.encodeToString(payload),
                status = DraftStatus.PENDING.name,
                errorMessage = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        RoomChangeBus.notify("submit_drafts")
    }

    override suspend fun markSubmitted(
        formKey: String,
        uniqueKey: String,
    ) {
        dao.updateStatus(formKey, uniqueKey, DraftStatus.SUBMITTED.name, epochMillis())
        RoomChangeBus.notify("submit_drafts")
    }

    override suspend fun markFailed(
        formKey: String,
        uniqueKey: String,
        error: String,
    ) {
        dao.updateStatusWithError(formKey, uniqueKey, DraftStatus.FAILED.name, error, epochMillis())
        RoomChangeBus.notify("submit_drafts")
    }

    override suspend fun clear(formKey: String) {
        dao.deleteByFormKey(formKey)
        RoomChangeBus.notify("submit_drafts")
    }
}

// KMP-safe epoch millis: use Kotlin's TimeSource or platform-provided time.
// For simplicity here, callers can inject clock if needed; this is the common-main default.
internal expect fun epochMillis(): Long
