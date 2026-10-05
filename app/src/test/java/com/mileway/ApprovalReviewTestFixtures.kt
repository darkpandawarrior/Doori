package com.mileway

import com.mileway.core.data.claim.ApprovalReview
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.model.db.ApprovalStepEntity

internal fun approvalReviewOf(report: Report): ApprovalReview =
    ApprovalReview(
        report,
        report.approvalChain.steps.map { step ->
            ApprovalStepEntity(
                reportId = report.id,
                stepIndex = step.stepIndex,
                role = step.role,
                thresholdMinor = step.thresholdMinor,
                actedBy = step.actedBy,
                onBehalfOf = step.onBehalfOf,
                action = step.action.name,
                comment = step.comment,
                actedAtMillis = step.actedAtMillis,
            )
        },
    )
