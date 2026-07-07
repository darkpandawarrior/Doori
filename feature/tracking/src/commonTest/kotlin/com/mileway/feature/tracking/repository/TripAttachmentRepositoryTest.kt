package com.mileway.feature.tracking.repository

import com.mileway.core.data.dao.TripAttachmentDao
import com.mileway.core.data.model.OdometerAnalysisSnapshot
import com.mileway.core.data.model.db.AttachmentType
import com.mileway.core.data.model.db.TripAttachmentEntity
import com.mileway.core.data.model.display.OdometerReadingSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Fake in-memory [TripAttachmentDao] — no Room needed for a pure snapshot-building test. */
private class FakeTripAttachmentDao : TripAttachmentDao {
    private var nextId = 1L
    val inserted = mutableListOf<TripAttachmentEntity>()

    override suspend fun insert(attachment: TripAttachmentEntity): Long {
        val withId = attachment.copy(id = nextId++)
        inserted += withId
        return withId.id
    }

    override fun observeForTrack(trackToken: String): Flow<List<TripAttachmentEntity>> = MutableStateFlow(inserted)

    override suspend fun getForTrack(trackToken: String): List<TripAttachmentEntity> = inserted

    override suspend fun getLatestOfType(
        trackToken: String,
        type: AttachmentType,
    ): TripAttachmentEntity? = inserted.lastOrNull { it.trackToken == trackToken && it.type == type }

    override fun observeByType(
        trackToken: String,
        type: AttachmentType,
    ): Flow<List<TripAttachmentEntity>> = MutableStateFlow(inserted.filter { it.type == type })

    override suspend fun delete(id: Long) {
        inserted.removeAll { it.id == id }
    }

    override suspend fun deleteForTrack(trackToken: String) {
        inserted.removeAll { it.trackToken == trackToken }
    }

    override suspend fun countForTrack(trackToken: String): Int = inserted.count { it.trackToken == trackToken }
}

/**
 * Wave-4 §2.4b: [TripAttachmentRepository.setOdometerStart]/[TripAttachmentRepository.setOdometerEnd]
 * must build the stored [OdometerAnalysisSnapshot] from the real typed capture (reading + source)
 * when the caller has one, only falling back to parsing [TripAttachmentEntity.ocrText] when the
 * typed reading is genuinely absent.
 */
class TripAttachmentRepositoryTest {
    @Test
    fun `uses the real typed reading and source when provided`() =
        runTest {
            val dao = FakeTripAttachmentDao()
            val repo = TripAttachmentRepository(dao)

            repo.setOdometerStart(
                trackToken = "route-1",
                uri = "content://odo-start.jpg",
                ocrText = "12345",
                typedReading = 45_678,
                typedSource = OdometerReadingSource.MANUAL,
            )

            val stored = dao.inserted.single().odometerAnalysisJson
            val snapshot = assertNotNull(OdometerAnalysisSnapshot.decode(stored))
            assertEquals(45_678, snapshot.reading)
            assertEquals(OdometerReadingSource.MANUAL, snapshot.source)
        }

    @Test
    fun `falls back to parsing ocrText as DEVICE_OCR when no typed reading is given`() =
        runTest {
            val dao = FakeTripAttachmentDao()
            val repo = TripAttachmentRepository(dao)

            repo.setOdometerEnd(
                trackToken = "route-1",
                uri = "content://odo-end.jpg",
                ocrText = "54321",
            )

            val stored = dao.inserted.single().odometerAnalysisJson
            val snapshot = assertNotNull(OdometerAnalysisSnapshot.decode(stored))
            assertEquals(54_321, snapshot.reading)
            assertEquals(OdometerReadingSource.DEVICE_OCR, snapshot.source)
        }
}
