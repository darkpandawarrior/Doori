package com.mileway

import android.content.Context
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mileway.core.data.database.MIGRATION_4_5
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * D.5: verifies MIGRATION_4_5 directly — it ALTERs the v4 `trip_attachments` table to add
 * `file_name` / `ocr_confidence` / `ocr_verified`, preserving existing rows. Runs the migration's own
 * SQL against a hand-built v4 table (focused on the migration, no full-schema Room open needed).
 * Instrumented (bundled SQLite native lib), runs on the GMD — not the JVM unit-test gate.
 */
@RunWith(AndroidJUnit4::class)
class TripAttachmentMigration4to5Test {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration_4_5_test.db"
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
    fun migration_4_5_adds_columns_and_preserves_rows() {
        val connection = BundledSQLiteDriver().open(path)
        try {
            // v4 trip_attachments exactly as MIGRATION_1_2 created it.
            connection.execSQL(
                "CREATE TABLE `trip_attachments` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "`track_token` TEXT NOT NULL, `type` TEXT NOT NULL, `uri` TEXT NOT NULL, " +
                    "`ocr_text` TEXT, `created_at` INTEGER NOT NULL)",
            )
            connection.execSQL(
                "INSERT INTO `trip_attachments` (`track_token`,`type`,`uri`,`ocr_text`,`created_at`) " +
                    "VALUES ('t1','RECEIPT','file:///x/r.jpg',NULL,100)",
            )

            // Run the migration under test.
            MIGRATION_4_5.migrate(connection)

            // The three new columns now exist.
            val columns = mutableSetOf<String>()
            connection.prepare("PRAGMA table_info(`trip_attachments`)").use { stmt ->
                while (stmt.step()) columns += stmt.getText(1) // index 1 = column name
            }
            assertTrue("file_name added", "file_name" in columns)
            assertTrue("ocr_confidence added", "ocr_confidence" in columns)
            assertTrue("ocr_verified added", "ocr_verified" in columns)

            // Pre-existing row survived, and the new columns are writable.
            connection.execSQL(
                "INSERT INTO `trip_attachments` " +
                    "(`track_token`,`type`,`uri`,`ocr_text`,`file_name`,`ocr_confidence`,`ocr_verified`,`created_at`) " +
                    "VALUES ('t1','ODOMETER_START','file:///x/o.jpg','48213','o.jpg',0.85,1,200)",
            )
            var count = 0
            connection.prepare("SELECT COUNT(*) FROM `trip_attachments`").use { stmt ->
                if (stmt.step()) count = stmt.getLong(0).toInt()
            }
            assertEquals(2, count)
        } finally {
            connection.close()
        }
    }
}
