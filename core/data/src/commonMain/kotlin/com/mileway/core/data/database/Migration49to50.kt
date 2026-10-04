package com.mileway.core.data.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** L11: nullable approval scope and booking metadata; all existing values remain untouched. */
val MIGRATION_49_50 =
    object : Migration(49, 50) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `reports` ADD COLUMN `submittedAtMs` INTEGER")
            connection.execSQL("ALTER TABLE `reports` ADD COLUMN `accountingPeriodKey` TEXT")
            connection.execSQL("ALTER TABLE `approval_steps` ADD COLUMN `claimLineId` TEXT")
        }
    }
