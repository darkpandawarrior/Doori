package com.mileway

import android.content.Context
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mileway.core.data.database.MIGRATION_41_42
import com.mileway.core.data.database.MIGRATION_42_43
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PLAN_V28 P28.4: verifies MIGRATION_42_43 — CREATEs `clarification_room_meta`, FK-cascaded to its
 * room. Same shape as [ClarificationMigration41to42Test]; instrumented for the same
 * `exportSchema=false` reason.
 */
@RunWith(AndroidJUnit4::class)
class ClarificationMigration42to43Test {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration_42_43_test.db"
    private val path get() = context.getDatabasePath(dbName).absolutePath

    @Before
    fun clean() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migration_42_43_creates_meta_table_writable_with_cascade_delete() {
        val connection = BundledSQLiteDriver().open(path)
        try {
            MIGRATION_41_42.migrate(connection)
            MIGRATION_42_43.migrate(connection)
            connection.execSQL("PRAGMA foreign_keys = ON")

            val columns = mutableSetOf<String>()
            connection.prepare("PRAGMA table_info(`clarification_room_meta`)").use { stmt ->
                while (stmt.step()) columns += stmt.getText(1)
            }
            assertTrue("roomId column exists", "roomId" in columns)
            assertTrue("isSaved column exists", "isSaved" in columns)
            assertTrue("isPinned column exists", "isPinned" in columns)
            assertTrue("tagsCsv column exists", "tagsCsv" in columns)
            assertTrue("note column exists", "note" in columns)
            assertTrue("reminderAtMs column exists", "reminderAtMs" in columns)

            connection.execSQL(
                "INSERT INTO `clarification_rooms` " +
                    "(`roomId`,`approvalId`,`status`,`participantsCsv`,`createdAtMs`,`updatedAtMs`) " +
                    "VALUES ('room_A001','A001','ACTIVE','Priya Sharma,approver',1000,1000)",
            )
            connection.execSQL(
                "INSERT INTO `clarification_room_meta` (`roomId`,`isSaved`,`isPinned`,`tagsCsv`,`note`,`reminderAtMs`) " +
                    "VALUES ('room_A001',1,0,'urgent,client',NULL,NULL)",
            )

            var metaCount = 0
            connection.prepare("SELECT COUNT(*) FROM `clarification_room_meta`").use { stmt ->
                if (stmt.step()) metaCount = stmt.getLong(0).toInt()
            }
            assertEquals(1, metaCount)

            // FK CASCADE: deleting the room deletes its meta row too.
            connection.execSQL("DELETE FROM `clarification_rooms` WHERE `roomId` = 'room_A001'")
            var metaCountAfterDelete = 0
            connection.prepare("SELECT COUNT(*) FROM `clarification_room_meta`").use { stmt ->
                if (stmt.step()) metaCountAfterDelete = stmt.getLong(0).toInt()
            }
            assertEquals(0, metaCountAfterDelete)
        } finally {
            connection.close()
        }
    }
}
