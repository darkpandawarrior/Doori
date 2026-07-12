package com.mileway

import android.content.Context
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mileway.core.data.database.MIGRATION_41_42
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PLAN_V28 P28.2: verifies MIGRATION_41_42 — it CREATEs `clarification_rooms` and
 * `clarification_messages` (FK-cascaded to its room). CREATE-only case (no pre-existing rows to
 * preserve), same shape as [DraftExpenseMigration6to7Test]. Instrumented (bundled SQLite native
 * lib), runs on the GMD — not the JVM unit-test gate, per `exportSchema=false` blocking
 * `MigrationTestHelper` (same reason as the other migration tests in this file's package).
 */
@RunWith(AndroidJUnit4::class)
class ClarificationMigration41to42Test {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration_41_42_test.db"
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
    fun migration_41_42_creates_both_tables_writable_with_cascade_delete() {
        val connection = BundledSQLiteDriver().open(path)
        try {
            MIGRATION_41_42.migrate(connection)
            connection.execSQL("PRAGMA foreign_keys = ON")

            val roomColumns = mutableSetOf<String>()
            connection.prepare("PRAGMA table_info(`clarification_rooms`)").use { stmt ->
                while (stmt.step()) roomColumns += stmt.getText(1)
            }
            assertTrue("roomId column exists", "roomId" in roomColumns)
            assertTrue("approvalId column exists", "approvalId" in roomColumns)
            assertTrue("status column exists", "status" in roomColumns)
            assertTrue("participantsCsv column exists", "participantsCsv" in roomColumns)
            assertTrue("createdAtMs column exists", "createdAtMs" in roomColumns)
            assertTrue("updatedAtMs column exists", "updatedAtMs" in roomColumns)

            val messageColumns = mutableSetOf<String>()
            connection.prepare("PRAGMA table_info(`clarification_messages`)").use { stmt ->
                while (stmt.step()) messageColumns += stmt.getText(1)
            }
            assertTrue("id column exists", "id" in messageColumns)
            assertTrue("roomId column exists", "roomId" in messageColumns)
            assertTrue("senderId column exists", "senderId" in messageColumns)
            assertTrue("isFromRequester column exists", "isFromRequester" in messageColumns)
            assertTrue("text column exists", "text" in messageColumns)
            assertTrue("timestampMs column exists", "timestampMs" in messageColumns)

            connection.execSQL(
                "INSERT INTO `clarification_rooms` " +
                    "(`roomId`,`approvalId`,`status`,`participantsCsv`,`createdAtMs`,`updatedAtMs`) " +
                    "VALUES ('room_A001','A001','ACTIVE','Priya Sharma,approver',1000,1000)",
            )
            connection.execSQL(
                "INSERT INTO `clarification_messages` " +
                    "(`id`,`roomId`,`senderId`,`isFromRequester`,`text`,`timestampMs`) " +
                    "VALUES ('msg1','room_A001','approver',0,'Hi, could you clarify?',1000)",
            )

            var messageCount = 0
            connection.prepare("SELECT COUNT(*) FROM `clarification_messages`").use { stmt ->
                if (stmt.step()) messageCount = stmt.getLong(0).toInt()
            }
            assertEquals(1, messageCount)

            // FK CASCADE: deleting the room deletes its messages too.
            connection.execSQL("DELETE FROM `clarification_rooms` WHERE `roomId` = 'room_A001'")
            var messageCountAfterDelete = 0
            connection.prepare("SELECT COUNT(*) FROM `clarification_messages`").use { stmt ->
                if (stmt.step()) messageCountAfterDelete = stmt.getLong(0).toInt()
            }
            assertEquals(0, messageCountAfterDelete)
        } finally {
            connection.close()
        }
    }
}
