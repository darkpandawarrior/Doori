package com.mileway.core.data.verification

import kotlinx.serialization.Serializable

/*
 * PLAN_V24 P4.1: verification-document model, rebuilt from the the reference app app's DocRequirementResponse
 * shape. Offline/mock for now — no real backend — so these live in core:data (the base module the
 * Room entity and every consumer can reach) rather than core:network as the plan sketched: core:data
 * has no dependency on core:network (the arrow runs the other way), so a core:network home would make
 * the Room DocumentEntity unable to reference these enums. Noted as a deliberate seam choice.
 */

/**
 * Document status lifecycle (string-coded in the source, an enum here):
 * NOT_UPLOADED → UPLOADED → APPROVAL_PENDING → VERIFIED | REJECTED(with reason → re-upload → UPLOADED…).
 * VERIFIED and APPROVAL_PENDING are locked; REJECTED and NOT_UPLOADED are editable.
 */
enum class DocStatus {
    NOT_UPLOADED,
    UPLOADED,
    APPROVAL_PENDING,
    VERIFIED,
    REJECTED,
    ;

    /** Verified/pending docs are locked from editing (source: `is_editable` false in these states). */
    val isLocked: Boolean get() = this == VERIFIED || this == APPROVAL_PENDING
}

/** Whether a document must be provided to submit for verification (source: `doc_requirement`). */
enum class DocRequirement { MANDATORY, OPTIONAL }

/** Broad grouping a document belongs to (source: `document_category`). */
enum class DocumentCategory { DRIVER, VEHICLE, CORPORATE }

/**
 * An editable text field attached to a document (source: `doc_info[]` — e.g. licence number,
 * expiry date). [editable] mirrors `is_doc_info_editable`.
 */
@Serializable
data class DocInfoField(
    val key: String,
    val label: String,
    val value: String,
    val editable: Boolean,
)

/**
 * A verification document as consumed by the verification centre UI (P4.2). Domain-level mirror of
 * the persisted `DocumentEntity`, with the JSON-encoded list columns decoded.
 */
data class VerificationDocument(
    val docType: String,
    val docTypeText: String,
    val requirement: DocRequirement,
    val status: DocStatus,
    val docCount: Int,
    val isEditable: Boolean,
    val docUrls: List<String>,
    val reason: String,
    val instructions: String,
    val galleryRestricted: Boolean,
    val category: DocumentCategory,
    val docInfo: List<DocInfoField>,
    val isDocInfoEditable: Boolean,
) {
    /**
     * A mandatory doc counts as "filled" when it sits in a good uploaded state (UPLOADED / pending /
     * verified — NOT rejected or not-uploaded, both of which need action) and every editable field
     * is set. Optional docs never block submission.
     */
    val mandatoryFieldsFilled: Boolean
        get() =
            requirement == DocRequirement.OPTIONAL ||
                (
                    status in GOOD_UPLOAD_STATES &&
                        docInfo.all { !it.editable || it.value.isNotBlank() }
                )

    private companion object {
        val GOOD_UPLOAD_STATES = setOf(DocStatus.UPLOADED, DocStatus.APPROVAL_PENDING, DocStatus.VERIFIED)
    }
}
