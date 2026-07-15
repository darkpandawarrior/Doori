package com.mileway.feature.tracking.service

import com.mileway.core.data.model.network.PolicyViolation
import com.mileway.core.data.model.network.ViolationSeverity
import com.mileway.feature.tracking.insights.DistanceQualityAnalyzer
import kotlin.math.roundToLong

/**
 * Wave-3 notification depth (parity §3): quality band derived from [DistanceQualityAnalyzer]'s
 * existing 0–100 score — same thresholds as [DistanceQualityAnalyzer.getAssessment] so the
 * notification and the insights screen never disagree on where a track sits.
 */
enum class QualityBand { EXCELLENT, GOOD, ACCEPTABLE, FAIR, POOR, VERY_POOR }

private fun bandFor(score: Int): QualityBand =
    when {
        score >= 90 -> QualityBand.EXCELLENT
        score >= 75 -> QualityBand.GOOD
        score >= 60 -> QualityBand.ACCEPTABLE
        score >= 40 -> QualityBand.FAIR
        score >= 20 -> QualityBand.POOR
        else -> QualityBand.VERY_POOR
    }

/** Deep-link + label for the action that resolves a [PolicyViolation] — commonMain data only. */
data class FixIssueAction(
    val violationId: String,
    val label: String = "Fix Issue",
    val deepLink: String = TrackingNotificationMapper.TRACK_DEEP_LINK,
)

/** Pure notification payload; posting through [com.siddharth.kmp.appshell.NotificationScheduler] is the caller's job. */
data class SubmissionNotificationContent(
    val title: String,
    val body: String,
    val deepLink: String? = null,
)

/**
 * Wave-3 (parity §3): pure commonMain mapper turning a completed/submitted track's quality score,
 * violations, and reimbursement into notification content. No Android/iOS dependency — posting is
 * `core:platform`'s `NotificationScheduler`'s job, this only decides *what* to say.
 */
object SubmissionNotificationMapper {
    /** Quality-score notification: title/body driven by [bandFor], reusing [DistanceQualityAnalyzer]'s score. */
    fun qualityContent(score: Int): SubmissionNotificationContent {
        val band = bandFor(score)
        val title =
            when (band) {
                QualityBand.EXCELLENT, QualityBand.GOOD -> "Great trip data"
                QualityBand.ACCEPTABLE -> "Trip recorded"
                QualityBand.FAIR, QualityBand.POOR -> "Trip data needs a look"
                QualityBand.VERY_POOR -> "Trip quality issue"
            }
        return SubmissionNotificationContent(
            title = title,
            body = "Quality score $score/100 · ${DistanceQualityAnalyzer.getAssessment(score)}",
            deepLink = TrackingNotificationMapper.TRACK_DEEP_LINK,
        )
    }

    /**
     * Violation alert listing every violation, ordered highest-severity first (HARDSTOP > VIOLATION
     * > REIMBURSABLE), with a [FixIssueAction] for the top-priority one. Returns null for no violations.
     */
    fun violationContent(violations: List<PolicyViolation>): Pair<SubmissionNotificationContent, FixIssueAction>? {
        if (violations.isEmpty()) return null
        val ordered = violations.sortedByDescending { it.severity.priority() }
        val top = ordered.first()
        val body =
            if (ordered.size == 1) {
                top.message.ifBlank { top.title }
            } else {
                "${top.title.ifBlank { top.message }} + ${ordered.size - 1} more issue${if (ordered.size > 2) "s" else ""}"
            }
        val content =
            SubmissionNotificationContent(
                title = if (top.severity == ViolationSeverity.HARDSTOP) "Submission blocked" else "Policy violation",
                body = body,
                deepLink = TrackingNotificationMapper.TRACK_DEEP_LINK,
            )
        return content to FixIssueAction(violationId = top.id)
    }

    /** Completion summary: distance, reimbursement, and quality band in one line. */
    fun completionSummary(
        distanceKm: Double,
        reimbursableAmount: Double,
        score: Int,
    ): SubmissionNotificationContent {
        val band = bandFor(score)
        return SubmissionNotificationContent(
            title = "Trip submitted",
            body = "${distanceKm.fmt2()} km · ₹${reimbursableAmount.roundToLong()} pending · ${band.name.lowercase()} quality",
            deepLink = TrackingNotificationMapper.TRACK_DEEP_LINK,
        )
    }

    /** Higher number = higher priority. HARDSTOP fixes come before plain VIOLATION before REIMBURSABLE notes. */
    private fun ViolationSeverity.priority(): Int =
        when (this) {
            ViolationSeverity.HARDSTOP -> 2
            ViolationSeverity.VIOLATION -> 1
            ViolationSeverity.REIMBURSABLE -> 0
        }

    private fun Double.fmt2(): String {
        val scaled = (this * 100).roundToLong()
        val intPart = scaled / 100
        val fracPart = (scaled % 100).let { if (it < 0) -it else it }
        return "$intPart.${fracPart.toString().padStart(2, '0')}"
    }
}
