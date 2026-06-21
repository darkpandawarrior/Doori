package com.miletracker.core.data.outbox

import kotlinx.coroutines.flow.Flow

// Durable outbox for trip drafts that survive process death.
// On submit, enqueue a PENDING draft; the BackgroundScheduler drives retries.
// Idempotent by (formKey, uniqueKey), re-enqueueing the same key updates status.
interface SubmitOutbox<T : Any> {
    /** All drafts for [formKey], ordered newest-first. */
    fun drafts(formKey: String): Flow<List<DraftEntry<T>>>

    /** Idempotent upsert by (formKey, uniqueKey). Resets to PENDING if not yet SUBMITTED. */
    suspend fun enqueue(
        formKey: String,
        uniqueKey: String,
        payload: T,
    )

    /** Mark SUBMITTED after a successful sync. */
    suspend fun markSubmitted(
        formKey: String,
        uniqueKey: String,
    )

    /** Mark FAILED with an error message; caller may retry later. */
    suspend fun markFailed(
        formKey: String,
        uniqueKey: String,
        error: String,
    )

    /** Remove all drafts for [formKey]. */
    suspend fun clear(formKey: String)
}

data class DraftEntry<T>(
    val formKey: String,
    val uniqueKey: String,
    val payload: T,
    val status: DraftStatus,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class DraftStatus { PENDING, RETRYING, SUBMITTED, FAILED }
