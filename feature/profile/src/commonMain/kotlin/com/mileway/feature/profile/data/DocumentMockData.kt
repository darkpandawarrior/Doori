package com.mileway.feature.profile.data

import com.mileway.core.data.verification.DocInfoField
import com.mileway.core.data.verification.DocRequirement
import com.mileway.core.data.verification.DocStatus
import com.mileway.core.data.verification.DocumentCategory
import com.mileway.core.data.verification.VerificationDocument

/**
 * PLAN_V24 P4.1: the seed set for the verification centre — 5 driver-persona documents (with mixed
 * seeded statuses so the UI exercises every state) and 2 corporate-persona documents. Seeded once
 * by `DocumentRepository.seedIfEmpty()`, mirroring [NotificationData].
 */
object DocumentMockData {
    val all: List<VerificationDocument> =
        listOf(
            VerificationDocument(
                docType = "driving_license",
                docTypeText = "Driving Licence",
                requirement = DocRequirement.MANDATORY,
                status = DocStatus.VERIFIED,
                docCount = 2,
                isEditable = false,
                docUrls = listOf("stub://license_front.jpg", "stub://license_back.jpg"),
                reason = "",
                instructions = "Upload the front and back of a valid driving licence.",
                galleryRestricted = false,
                category = DocumentCategory.DRIVER,
                docInfo =
                    listOf(
                        DocInfoField("license_number", "Licence number", "MH12 2019 0001234", editable = true),
                        DocInfoField("expiry", "Expiry date", "2031-05-14", editable = true),
                    ),
                isDocInfoEditable = true,
            ),
            VerificationDocument(
                docType = "vehicle_registration",
                docTypeText = "Vehicle Registration",
                requirement = DocRequirement.MANDATORY,
                status = DocStatus.APPROVAL_PENDING,
                docCount = 1,
                isEditable = false,
                docUrls = listOf("stub://rc.jpg"),
                reason = "",
                instructions = "Upload the vehicle registration certificate (RC).",
                galleryRestricted = false,
                category = DocumentCategory.DRIVER,
                docInfo = listOf(DocInfoField("reg_number", "Registration number", "MH12AB1234", editable = true)),
                isDocInfoEditable = true,
            ),
            VerificationDocument(
                docType = "insurance",
                docTypeText = "Insurance",
                requirement = DocRequirement.MANDATORY,
                status = DocStatus.REJECTED,
                docCount = 1,
                isEditable = true,
                docUrls = listOf("stub://insurance_old.jpg"),
                reason = "Document expired. Please upload a currently valid policy.",
                instructions = "Upload a valid vehicle insurance policy document.",
                galleryRestricted = false,
                category = DocumentCategory.DRIVER,
                docInfo = listOf(DocInfoField("policy_number", "Policy number", "", editable = true)),
                isDocInfoEditable = true,
            ),
            VerificationDocument(
                docType = "address_proof",
                docTypeText = "Address Proof",
                requirement = DocRequirement.MANDATORY,
                status = DocStatus.UPLOADED,
                docCount = 1,
                isEditable = true,
                docUrls = listOf("stub://address.jpg"),
                reason = "",
                instructions = "Upload a utility bill or bank statement from the last 3 months.",
                galleryRestricted = false,
                category = DocumentCategory.DRIVER,
                docInfo = emptyList(),
                isDocInfoEditable = false,
            ),
            VerificationDocument(
                docType = "profile_photo",
                docTypeText = "Profile Photo",
                requirement = DocRequirement.OPTIONAL,
                status = DocStatus.NOT_UPLOADED,
                docCount = 1,
                isEditable = true,
                docUrls = emptyList(),
                reason = "",
                instructions = "Upload a clear passport-style photo.",
                galleryRestricted = true,
                category = DocumentCategory.DRIVER,
                docInfo = emptyList(),
                isDocInfoEditable = false,
            ),
            VerificationDocument(
                docType = "corporate_id",
                docTypeText = "Company ID Card",
                requirement = DocRequirement.MANDATORY,
                status = DocStatus.NOT_UPLOADED,
                docCount = 1,
                isEditable = true,
                docUrls = emptyList(),
                reason = "",
                instructions = "Upload your employer-issued ID card.",
                galleryRestricted = false,
                category = DocumentCategory.CORPORATE,
                docInfo = listOf(DocInfoField("employee_id", "Employee ID", "", editable = true)),
                isDocInfoEditable = true,
            ),
            VerificationDocument(
                docType = "corporate_address_proof",
                docTypeText = "Address Proof",
                requirement = DocRequirement.OPTIONAL,
                status = DocStatus.NOT_UPLOADED,
                docCount = 1,
                isEditable = true,
                docUrls = emptyList(),
                reason = "",
                instructions = "Upload a document confirming your residential address.",
                galleryRestricted = false,
                category = DocumentCategory.CORPORATE,
                docInfo = emptyList(),
                isDocInfoEditable = false,
            ),
        )
}
