package com.mileway.core.data.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object DateUtils {
    fun epochToDisplayDate(epochMs: Long): String {
        val local =
            Instant.fromEpochMilliseconds(epochMs)
                .toLocalDateTime(TimeZone.currentSystemDefault())
        val monthAbbr = local.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        return "${local.day} $monthAbbr ${local.year}"
    }

    fun epochToTime12h(epochMs: Long): String {
        val local =
            Instant.fromEpochMilliseconds(epochMs)
                .toLocalDateTime(TimeZone.currentSystemDefault())
        val hour12 =
            if (local.hour == 0) {
                12
            } else if (local.hour > 12) {
                local.hour - 12
            } else {
                local.hour
            }
        val amPm = if (local.hour < 12) "AM" else "PM"
        return "$hour12:${local.minute.pad2()} $amPm"
    }

    fun epochToTime(epochMs: Long): String {
        val local =
            Instant.fromEpochMilliseconds(epochMs)
                .toLocalDateTime(TimeZone.currentSystemDefault())
        return "${local.hour.pad2()}:${local.minute.pad2()}"
    }

    fun epochToTime24h(epochMs: Long): String {
        val local =
            Instant.fromEpochMilliseconds(epochMs)
                .toLocalDateTime(TimeZone.currentSystemDefault())
        return "${local.hour}:${local.minute.pad2()}:${local.second.pad2()}"
    }

    fun epochToDateTime(epochMs: Long): String {
        val local =
            Instant.fromEpochMilliseconds(epochMs)
                .toLocalDateTime(TimeZone.currentSystemDefault())
        val monthAbbr = local.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        val hour12 =
            if (local.hour == 0) {
                12
            } else if (local.hour > 12) {
                local.hour - 12
            } else {
                local.hour
            }
        val amPm = if (local.hour < 12) "AM" else "PM"
        return "$monthAbbr ${local.day}, ${local.year} ${hour12.pad2()}:${local.minute.pad2()} $amPm"
    }

    fun epochToDisplayDateTime(epochMs: Long): String = epochToDateTime(epochMs)

    fun epochToDateSlash(epochMs: Long): String {
        val local =
            Instant.fromEpochMilliseconds(epochMs)
                .toLocalDateTime(TimeZone.currentSystemDefault())
        return "${local.day.pad2()}/${local.month.number.pad2()}/${local.year}"
    }

    fun monthStartMillis(): Long {
        val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val monthStart = LocalDateTime(now.year, now.month, 1, 0, 0, 0)
        return monthStart.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }

    fun getDurationDifferenceInMinutes(pastTimeInMillis: Long): Long {
        val diffMs = kotlin.time.Clock.System.now().toEpochMilliseconds() - pastTimeInMillis
        return diffMs / 60_000L
    }

    fun dateStringToMilliseconds(dateString: String): Long {
        return try {
            val parts = dateString.split("-")
            if (parts.size != 3) return -1L
            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = parts[2].toInt()
            val date = LocalDateTime(year, month, day, 0, 0, 0)
            date.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        } catch (e: Exception) {
            -1L
        }
    }
}
