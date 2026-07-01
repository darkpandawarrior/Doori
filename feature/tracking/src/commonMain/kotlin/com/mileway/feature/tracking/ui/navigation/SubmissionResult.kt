package com.mileway.feature.tracking.ui.navigation

/** P-E.1: platform-neutral result of a mileage submission. Passed back through the nav back-stack. */
data class SubmissionResult(
    val distanceKm: Double,
    val reimbursableAmount: Double,
    val vehicleName: String,
    val startTime: Long,
    val endTime: Long,
    val transactionId: String?,
    val submissionStatus: String,
    val violationCount: Int,
    val violationMessage: String?,
    val voucherNumber: String?,
    val voucherAmount: Double,
)
