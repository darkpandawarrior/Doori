package com.mileway.core.data.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSHomeDirectory

fun buildMilewayDatabase(): MilewayDatabase =
    Room.databaseBuilder<MilewayDatabase>(
        name = NSHomeDirectory() + "/Documents/mileway.db",
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
        .build()
