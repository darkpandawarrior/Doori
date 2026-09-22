package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.SupportTicketDao
import com.mileway.core.data.model.db.SupportTicketEntity
import com.mileway.feature.profile.model.SupportTicketStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/** Advances by 1ms on every [now] call so successive [SupportTicketRepository.submit]s get distinct ids/timestamps. */
private class IncrementingClock(
    startMs: Long,
) : Clock {
    private var currentMs = startMs

    override fun now(): Instant = Instant.fromEpochMilliseconds(currentMs++)
}

/**
 * PLAN_V22 P6.8: [SupportTicketRepository] persists `HelpScreen`'s "Contact Support"/"Report a Bug"
 * submissions via [SupportTicketDao] — proven here against an in-memory fake dao (same shape
 * `PersonalDetailsRepositoryTest` uses), so a fresh repository instance backed by the same dao sees
 * whatever a prior instance wrote (i.e. it survives "process death" for a JVM test's purposes).
 */
class SupportTicketRepositoryTest {
    @Test
    fun `observeAll emits nothing before anything is submitted`() =
        runTest {
            val repository = SupportTicketRepository(FakeSupportTicketDao())

            assertTrue(repository.observeAll().first().isEmpty())
        }

    @Test
    fun `submit persists a ticket in the OPEN status`() =
        runTest {
            val repository = SupportTicketRepository(FakeSupportTicketDao(), clock = IncrementingClock(1_700_000_000_000L))

            repository.submit(subject = "Can't submit expense", body = "The submit button does nothing")

            val ticket = repository.observeAll().first().single()
            assertEquals("Can't submit expense", ticket.subject)
            assertEquals(SupportTicketStatus.OPEN, ticket.status)
        }

    @Test
    fun `submitted tickets survive a fresh repository instance backed by the same dao`() =
        runTest {
            val dao = FakeSupportTicketDao()
            SupportTicketRepository(dao, clock = IncrementingClock(1_700_000_000_000L))
                .submit(subject = "GPS drift", body = "Route looks noisy on rural roads")

            val restored = SupportTicketRepository(dao).observeAll().first()

            assertEquals(1, restored.size)
            assertEquals("GPS drift", restored.single().subject)
        }

    @Test
    fun `observeAll orders tickets newest first`() =
        runTest {
            val repository = SupportTicketRepository(FakeSupportTicketDao(), clock = IncrementingClock(1_700_000_000_000L))
            repository.submit(subject = "First issue", body = "Body 1")
            repository.submit(subject = "Second issue", body = "Body 2")

            val subjects = repository.observeAll().first().map { it.subject }

            assertEquals(listOf("Second issue", "First issue"), subjects)
        }
}

private class FakeSupportTicketDao : SupportTicketDao {
    private val rows = MutableStateFlow<Map<String, SupportTicketEntity>>(emptyMap())

    override fun observeAll(): Flow<List<SupportTicketEntity>> = rows.map { it.values.sortedByDescending { row -> row.createdAtMs } }

    override suspend fun upsert(entity: SupportTicketEntity) {
        rows.value = rows.value + (entity.id to entity)
    }
}
