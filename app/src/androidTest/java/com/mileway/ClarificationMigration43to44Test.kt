package com.mileway

import android.content.Context
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mileway.core.data.database.MIGRATION_41_42
import com.mileway.core.data.database.MIGRATION_42_43
import com.mileway.core.data.database.MIGRATION_43_44
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PLAN_V28 P28.6: verifies MIGRATION_43_44 — additive `senderName`/`senderRole`/`attachmentUrl`
 * columns on `clarification_messages`. Same shape as [ClarificationMigration42to43Test].
 */
@RunWith(AndroidJUnit4::class)
class ClarificationMigration43to44Test {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration_43_44_test.db"
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
    fun migration_43_44_adds_sender_and_attachment_columns() {
        val connection = BundledSQLiteDriver().open(path)
        try {
            MIGRATION_41_42.migrate(connection)
            MIGRATION_42_43.migrate(connection)
            MIGRATION_43_44.migrate(connection)

            val columns = mutableSetOf<String>()
            connection.prepare("PRAGMA table_info(`clarification_messages`)").use { stmt ->
                while (stmt.step()) columns += stmt.getText(1)
            }
            assertTrue("senderName column exists", "senderName" in columns)
            assertTrue("senderRole column exists", "senderRole" in columns)
            assertTrue("attachmentUrl column exists", "attachmentUrl" in columns)

            connection.execSQL(
                "INSERT INTO `clarification_rooms` " +
                    "(`roomId`,`approvalId`,`status`,`participantsCsv`,`createdAtMs`,`updatedAtMs`) " +
                    "VALUES ('room_A001','A001','ACTIVE','Priya Sharma,approver',1000,1000)",
            )
            connection.execSQL(
                "INSERT INTO `clarification_messages` " +
                    "(`id`,`roomId`,`senderId`,`isFromRequester`,`text`,`timestampMs`,`senderName`,`senderRole`,`attachmentUrl`) " +
                    "VALUES ('msg1','room_A001','approver',0,'Please see attached',2000,'You','Approver','file:///receipt.jpg')",
            )

            var senderName = ""
            var attachmentUrl = ""
            connection.prepare("SELECT senderName, attachmentUrl FROM `clarification_messages` WHERE id = 'msg1'").use { stmt ->
                if (stmt.step()) {
                    senderName = stmt.getText(0)
                    attachmentUrl = stmt.getText(1)
                }
            }
            assertEquals("You", senderName)
            assertEquals("file:///receipt.jpg", attachmentUrl)
        } finally {
            connection.close()
        }
    }
}
