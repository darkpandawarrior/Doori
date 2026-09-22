package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.SupportTicketDao
import com.mileway.core.data.model.db.SupportTicketEntity
import com.mileway.feature.profile.model.SupportTicket
import com.mileway.feature.profile.model.SupportTicketStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/** Generated ids carry the last eight digits of the creation timestamp - unique enough offline. */
private const val ID_SUFFIX_DIGITS = 8

/**
 * PLAN_V22 P6.8: Room-backed store for `HelpScreen`'s "Contact Support" form, replacing its
 * previous fire-and-forget `snackbarHostState.showSnackbar(...)`-only tap with a real, persisted
 * ticket visible afterward in "My Tickets".
 */
class SupportTicketRepository(
    private val dao: SupportTicketDao,
    private val clock: Clock = Clock.System,
) {
    /** Live, most-recent-first list of this account's submitted tickets. */
    fun observeAll(): Flow<List<SupportTicket>> = dao.observeAll().map { rows -> rows.map { it.toTicket() } }

    /**
     * Submits a new ticket. [subject] and [body] must both be non-blank — callers should validate
     * before calling this (the ViewModel surfaces the error instead of silently no-oping here).
     * Every new ticket starts [SupportTicketStatus.OPEN] (see that enum's doc for why this demo
     * never auto-progresses a ticket further).
     */
    suspend fun submit(
        subject: String,
        body: String,
    ) {
        val now = clock.now().toEpochMilliseconds()
        dao.upsert(
            SupportTicketEntity(
                id = "TCK-" + now.toString().takeLast(ID_SUFFIX_DIGITS),
                subject = subject,
                body = body,
                createdAtMs = now,
                status = SupportTicketStatus.OPEN.name,
            ),
        )
    }

    private fun SupportTicketEntity.toTicket(): SupportTicket =
        SupportTicket(
            id = id,
            subject = subject,
            body = body,
            createdAtMs = createdAtMs,
            status = SupportTicketStatus.valueOf(status),
        )
}
