package com.miletracker.core.data.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Migration 4 → 5 (D.5): enrich trip_attachments with a file name + multi-pass OCR provenance
 * (confidence + verified flag from [com.miletracker.feature.media.ocr.OdometerOcrAggregator]).
 * Plain additive ALTERs; existing rows keep the column defaults.
 */
val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `trip_attachments` ADD COLUMN `file_name` TEXT")
            connection.execSQL("ALTER TABLE `trip_attachments` ADD COLUMN `ocr_confidence` REAL NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE `trip_attachments` ADD COLUMN `ocr_verified` INTEGER NOT NULL DEFAULT 0")
        }
    }

/**
 * Migration 1 → 2: introduce the trip_attachments table.
 *
 * Each row stores one photo attachment (receipt or odometer proof) keyed by the
 * track_token (= SavedTrack.routeId). The type column uses the AttachmentType enum
 * name (TEXT): RECEIPT, ODOMETER_START, ODOMETER_END.
 */
val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `submit_drafts` (
                    `formKey`      TEXT    NOT NULL,
                    `uniqueKey`    TEXT    NOT NULL,
                    `payloadJson`  TEXT    NOT NULL,
                    `status`       TEXT    NOT NULL,
                    `errorMessage` TEXT,
                    `createdAt`    INTEGER NOT NULL,
                    `updatedAt`    INTEGER NOT NULL,
                    PRIMARY KEY(`formKey`, `uniqueKey`)
                )
                """.trimIndent(),
            )
        }
    }

val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `media_library` (
                    `id`        TEXT    NOT NULL PRIMARY KEY,
                    `uri`       TEXT    NOT NULL,
                    `mimeType`  TEXT    NOT NULL,
                    `label`     TEXT    NOT NULL,
                    `source`    TEXT    NOT NULL,
                    `savedAtMs` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `trip_attachments` (
                    `id`          INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `track_token` TEXT    NOT NULL,
                    `type`        TEXT    NOT NULL,
                    `uri`         TEXT    NOT NULL,
                    `ocr_text`    TEXT,
                    `created_at`  INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_trip_attachments_track_token` ON `trip_attachments` (`track_token`)",
            )
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_trip_attachments_track_token_type` ON `trip_attachments` (`track_token`, `type`)",
            )
        }
    }
