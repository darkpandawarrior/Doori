package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * P31.MISC.1: one shake-to-report submission — captured locally, never sent to a backend (see
 * CLAUDE.md "The backend"). [screen] is a free-form route/screen identifier so a report is
 * reproducible without a live crash-reporting SDK; [appVersion] is read once at submit time (not
 * derived at query time) so an old report keeps showing the version it was actually filed against.
 */
@Entity(tableName = "bug_reports")
data class BugReportEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val screen: String,
    val timestampMs: Long,
    val appVersion: String,
)
