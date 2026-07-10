package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.DocumentDao
import com.mileway.core.data.model.db.DocumentEntity
import com.mileway.core.data.verification.DocStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * PLAN_V24 P4.1: covers [DocumentRepository]'s seed/upload/edit/submit behaviour against a fake
 * [DocumentDao] — the status lifecycle and the "all mandatory fields filled" submit gate.
 */
class DocumentRepositoryTest {
    private fun repo(dao: FakeDocumentDao = FakeDocumentDao()) = DocumentRepository(dao)

    @Test
    fun `seedIfEmpty seeds the catalog exactly once`() =
        runTest {
            val dao = FakeDocumentDao()
            val r = repo(dao)

            r.seedIfEmpty()
            val afterFirst = r.observeAll().first().size
            r.seedIfEmpty()
            val afterSecond = r.observeAll().first().size

            // 5 DRIVER + 2 CORPORATE + 2 VEHICLE (P11.2) = 9.
            assertEquals(9, afterFirst)
            assertEquals(9, afterSecond)
        }

    @Test
    fun `seeded documents decode their list columns`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()

            val license = r.observeAll().first().single { it.docType == "driving_license" }
            assertEquals(2, license.docUrls.size)
            assertEquals(2, license.docInfo.size)
            assertEquals(DocStatus.VERIFIED, license.status)
        }

    @Test
    fun `uploadSlot flips a not-uploaded doc to UPLOADED and stores the url`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()

            r.uploadSlot("profile_photo", "stub://selfie.jpg")

            val photo = r.observeAll().first().single { it.docType == "profile_photo" }
            assertEquals(DocStatus.UPLOADED, photo.status)
            assertEquals(listOf("stub://selfie.jpg"), photo.docUrls)
        }

    @Test
    fun `uploadSlot is a no-op on a locked verified doc`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()

            r.uploadSlot("driving_license", "stub://tampered.jpg")

            val license = r.observeAll().first().single { it.docType == "driving_license" }
            assertEquals(DocStatus.VERIFIED, license.status)
            assertEquals(2, license.docUrls.size)
        }

    @Test
    fun `updateInfoField updates a single field value`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()

            r.updateInfoField("insurance", "policy_number", "POL-999")

            val insurance = r.observeAll().first().single { it.docType == "insurance" }
            assertEquals("POL-999", insurance.docInfo.single { it.key == "policy_number" }.value)
        }

    @Test
    fun `submit is blocked while a mandatory doc is incomplete`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()

            // Seed has a REJECTED insurance (blank policy) and a NOT_UPLOADED corporate id.
            val (ok, failing) = r.submitForVerification()

            assertFalse(ok)
            assertEquals("Insurance", failing)
        }

    @Test
    fun `submit succeeds once every mandatory doc is complete and moves uploads to pending`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()

            // Complete the two incomplete mandatory docs.
            r.uploadSlot("insurance", "stub://insurance_new.jpg")
            r.updateInfoField("insurance", "policy_number", "POL-1")
            r.uploadSlot("corporate_id", "stub://id.jpg")
            r.updateInfoField("corporate_id", "employee_id", "E-1")

            val (ok, failing) = r.submitForVerification()

            assertTrue(ok)
            assertNull(failing)
            val docs = r.observeAll().first()
            // Every previously-UPLOADED doc is now pending review; none left UPLOADED.
            assertTrue(docs.none { it.status == DocStatus.UPLOADED })
            assertEquals(DocStatus.APPROVAL_PENDING, docs.single { it.docType == "address_proof" }.status)
        }
}

/** In-memory fake for [DocumentDao] — mirrors [DelegationViewModelTest]'s fake shape. */
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
