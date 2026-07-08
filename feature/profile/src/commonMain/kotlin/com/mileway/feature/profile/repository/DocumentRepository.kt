package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.DocumentDao
import com.mileway.core.data.model.db.DocumentEntity
import com.mileway.core.data.verification.DocRequirement
import com.mileway.core.data.verification.DocStatus
import com.mileway.core.data.verification.DocumentCategory
import com.mileway.core.data.verification.DocumentCodec
import com.mileway.core.data.verification.VerificationDocument
import com.mileway.feature.profile.data.DocumentMockData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * PLAN_V24 P4.1: Room-backed store for the verification centre's documents. Seeded once from
 * [DocumentMockData] (mirroring [NotificationRepository]); every mutation writes straight back to
 * Room so status changes survive navigation and process death. The status-flip on submit is the
 * demo's "sent for review" step — no network; [com.mileway.core.data.review.SimulatedReviewEngine]
 * (P0.5) later flips APPROVAL_PENDING → VERIFIED/REJECTED (wired in P4.2).
 */
class DocumentRepository(private val dao: DocumentDao, private val clock: Clock = Clock.System) {
    /** Live, category-then-type ordered list of verification documents. */
    fun observeAll(): Flow<List<VerificationDocument>> = dao.observeAll().map { rows -> rows.map { it.toDocument() } }

    /** Seeds [DocumentMockData.all] on first run only; a no-op on every subsequent launch. */
    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        val now = clock.now().toEpochMilliseconds()
        dao.upsertAll(DocumentMockData.all.map { it.toEntity(now) })
    }

    /**
     * Records an uploaded image slot for [docType]: appends [url] (capped at the doc's `docCount`),
     * flips the status to [DocStatus.UPLOADED] and clears any prior rejection reason. A no-op if the
     * doc is locked (verified/pending) or unknown.
     */
    suspend fun uploadSlot(
        docType: String,
        url: String,
    ) {
        val entity = dao.get(docType) ?: return
        val current = entity.toDocument()
        if (current.status.isLocked) return
        val urls = (current.docUrls + url).takeLast(current.docCount.coerceAtLeast(1))
        dao.upsert(
            entity.copy(
                status = DocStatus.UPLOADED.name,
                docUrlsJson = DocumentCodec.encodeDocUrls(urls),
                reason = "",
                updatedAtMs = clock.now().toEpochMilliseconds(),
            ),
        )
    }

    /** Updates one editable info field's value for [docType]. A no-op if the doc/field is unknown. */
    suspend fun updateInfoField(
        docType: String,
        key: String,
        value: String,
    ) {
        val entity = dao.get(docType) ?: return
        val info = DocumentCodec.decodeDocInfo(entity.docInfoJson)
        if (info.none { it.key == key }) return
        val updated = info.map { if (it.key == key) it.copy(value = value) else it }
        dao.upsert(
            entity.copy(
                docInfoJson = DocumentCodec.encodeDocInfo(updated),
                updatedAtMs = clock.now().toEpochMilliseconds(),
            ),
        )
    }

    /** Directly sets a document's status + reason (used by the review engine wiring in P4.2). */
    suspend fun setStatus(
        docType: String,
        status: DocStatus,
        reason: String = "",
    ) {
        val entity = dao.get(docType) ?: return
        dao.upsert(
            entity.copy(status = status.name, reason = reason, updatedAtMs = clock.now().toEpochMilliseconds()),
        )
    }

    /**
     * Validates that every MANDATORY document is uploaded with its editable fields filled, mirroring
     * the source's `Pair<Boolean, String>` "all mandatory fields filled?" check. On success, moves
     * every UPLOADED document to APPROVAL_PENDING and returns `(true, null)`. On failure returns
     * `(false, <first failing document's display name>)` and changes nothing.
     */
    suspend fun submitForVerification(): Pair<Boolean, String?> {
        val docs = currentDocuments()
        val firstIncomplete = docs.firstOrNull { !it.mandatoryFieldsFilled }
        if (firstIncomplete != null) return false to firstIncomplete.docTypeText
        val now = clock.now().toEpochMilliseconds()
        docs.filter { it.status == DocStatus.UPLOADED }.forEach { doc ->
            dao.get(doc.docType)?.let { entity ->
                dao.upsert(entity.copy(status = DocStatus.APPROVAL_PENDING.name, updatedAtMs = now))
            }
        }
        return true to null
    }

    /** One-shot snapshot of the persisted documents (for validation that must read the full set). */
    private suspend fun currentDocuments(): List<VerificationDocument> {
        val types = DocumentMockData.all.map { it.docType }
        return types.mapNotNull { dao.get(it)?.toDocument() }
    }

    private fun DocumentEntity.toDocument(): VerificationDocument =
        VerificationDocument(
            docType = docType,
            docTypeText = docTypeText,
            requirement = DocRequirement.entries.firstOrNull { it.name == requirement } ?: DocRequirement.OPTIONAL,
            status = DocStatus.entries.firstOrNull { it.name == status } ?: DocStatus.NOT_UPLOADED,
            docCount = docCount,
            isEditable = isEditable,
            docUrls = DocumentCodec.decodeDocUrls(docUrlsJson),
            reason = reason,
            instructions = instructions,
            galleryRestricted = galleryRestricted,
            category = DocumentCategory.entries.firstOrNull { it.name == category } ?: DocumentCategory.DRIVER,
            docInfo = DocumentCodec.decodeDocInfo(docInfoJson),
            isDocInfoEditable = isDocInfoEditable,
            updatedAtMillis = updatedAtMs,
        )

    private fun VerificationDocument.toEntity(now: Long): DocumentEntity =
        DocumentEntity(
            docType = docType,
            docTypeText = docTypeText,
            requirement = requirement.name,
            status = status.name,
            docCount = docCount,
            isEditable = isEditable,
            docUrlsJson = DocumentCodec.encodeDocUrls(docUrls),
            reason = reason,
            instructions = instructions,
            galleryRestricted = galleryRestricted,
            category = category.name,
            docInfoJson = DocumentCodec.encodeDocInfo(docInfo),
            isDocInfoEditable = isDocInfoEditable,
            updatedAtMs = now,
        )
}
