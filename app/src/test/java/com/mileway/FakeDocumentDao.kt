package com.mileway

import com.mileway.core.data.dao.DocumentDao
import com.mileway.core.data.model.db.DocumentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake for [DocumentDao] (PLAN_V24 P4.1/P4.2) — lets JVM/Robolectric tests construct
 * `DocumentRepository`/`VerificationCentreViewModel` without a Room instance. A relaxed mockk would
 * return a null Flow the ViewModel's `init` collector dereferences, so a real fake is required.
 */
class FakeDocumentDao : DocumentDao {
    private val rows = MutableStateFlow<Map<String, DocumentEntity>>(emptyMap())

    override fun observeAll(): Flow<List<DocumentEntity>> =
        rows.map { it.values.sortedWith(compareBy({ row -> row.category }, { row -> row.docType })) }

    override suspend fun count(): Int = rows.value.size

    override suspend fun get(docType: String): DocumentEntity? = rows.value[docType]

    override suspend fun upsertAll(entities: List<DocumentEntity>) {
        rows.value = rows.value + entities.associateBy { it.docType }
    }

    override suspend fun upsert(entity: DocumentEntity) {
        rows.value = rows.value + (entity.docType to entity)
    }
}
