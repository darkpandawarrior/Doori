package com.mileway.feature.profile.viewmodel

import com.mileway.core.data.dao.DocumentDao
import com.mileway.core.data.model.db.DocumentEntity
import com.mileway.core.data.review.SimulatedReviewEngine
import com.mileway.core.data.verification.DocStatus
import com.mileway.feature.profile.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PLAN_V24 P4.2: covers [VerificationCentreViewModel]'s seed/counters, the submit gate, and the
 * [SimulatedReviewEngine]-driven approve/reject resolution. A 0-delay engine makes the review
 * resolve immediately so the approve/reject paths are deterministic under [runTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VerificationCentreViewModelTest {
    @BeforeTest
    fun setMain() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMain() {
        Dispatchers.resetMain()
    }

    private fun vm(
        dao: FakeDocumentDao = FakeDocumentDao(),
        delayMillis: Long = SimulatedReviewEngine.DEFAULT_SIM_DELAY_MILLIS,
    ) = VerificationCentreViewModel(DocumentRepository(dao), SimulatedReviewEngine(simDelayMillis = delayMillis))

    @Test
    fun `seeds the catalogue and exposes counters`() =
        runTest {
            val v = vm()
            advanceUntilIdle()

            // 5 DRIVER + 2 CORPORATE + 2 VEHICLE (P11.2) = 9.
            assertEquals(9, v.state.value.documents.size)
            // driving_license + vehicle_rc are seeded VERIFIED.
            assertEquals(2, v.state.value.verifiedCount)
            assertEquals(1, v.state.value.rejectedCount)
            assertFalse(v.state.value.canSubmit)
        }

    @Test
    fun `submit is blocked and names the first incomplete mandatory doc`() =
        runTest {
            val v = vm()
            advanceUntilIdle()

            v.submit()
            advanceUntilIdle()

            assertEquals("Insurance", v.state.value.submitError)
        }

    @Test
    fun `completing mandatory docs then submitting verifies the approved ones`() =
        runTest {
            val v = vm(delayMillis = 0L)
            advanceUntilIdle()

            v.uploadSlot("insurance", "stub://ins.jpg")
            v.updateInfoField("insurance", "policy_number", "POL-1")
            v.uploadSlot("corporate_id", "stub://id.jpg")
            v.updateInfoField("corporate_id", "employee_id", "E-1")
            advanceUntilIdle()

            v.submit()
            advanceUntilIdle()

            val docs = v.state.value.documents
            assertEquals(DocStatus.VERIFIED, docs.single { it.docType == "insurance" }.status)
            assertTrue(docs.none { it.status == DocStatus.UPLOADED })
        }

    @Test
    fun `a reject marker in a field rejects that document with a reason`() =
        runTest {
            val v = vm(delayMillis = 0L)
            advanceUntilIdle()

            v.uploadSlot("insurance", "stub://ins.jpg")
            v.updateInfoField("insurance", "policy_number", "POL-1")
            v.uploadSlot("corporate_id", "stub://id.jpg")
            // Plant the reject marker so the simulated review rejects this doc.
            v.updateInfoField("corporate_id", "employee_id", "reject: bad scan")
            advanceUntilIdle()

            v.submit()
            advanceUntilIdle()

            val corporate = v.state.value.documents.single { it.docType == "corporate_id" }
            assertEquals(DocStatus.REJECTED, corporate.status)
            assertTrue(corporate.reason.isNotBlank())
        }
}

/** In-memory fake for [DocumentDao] — mirrors [DocumentRepositoryTest]'s fake shape. */
private class FakeDocumentDao : DocumentDao {
    private val rows = MutableStateFlow<Map<String, DocumentEntity>>(emptyMap())

    override fun observeAll(): Flow<List<DocumentEntity>> = rows.map { it.values.sortedWith(compareBy({ row -> row.category }, { row -> row.docType })) }

    override suspend fun count(): Int = rows.value.size

    override suspend fun get(docType: String): DocumentEntity? = rows.value[docType]

    override suspend fun upsertAll(entities: List<DocumentEntity>) {
        rows.value = rows.value + entities.associateBy { it.docType }
    }

    override suspend fun upsert(entity: DocumentEntity) {
        rows.value = rows.value + (entity.docType to entity)
    }
}
