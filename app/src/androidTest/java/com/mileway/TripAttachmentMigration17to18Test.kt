package com.mileway

import android.content.Context
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mileway.core.data.database.MIGRATION_17_18
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * §2.4: verifies MIGRATION_17_18 directly — it ALTERs the v17 `trip_attachments` table to add
 * `odometerAnalysisJson`, preserving existing rows. Runs the migration's own SQL against a
 * hand-built v17 table (focused on the migration, no full-schema Room open needed). Instrumented
 * (bundled SQLite native lib), runs on the GMD — not the JVM unit-test gate.
 */
@RunWith(AndroidJUnit4::class)
class TripAttachmentMigration17to18Test {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration_17_18_test.db"
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
    fun migration_17_18_adds_column_and_preserves_rows() {
        val connection = BundledSQLiteDriver().open(path)
        try {
            // v17 trip_attachments: v4 shape plus the file_name/ocr_confidence/ocr_verified columns
            // added by MIGRATION_4_5 (no further trip_attachments changes landed between 4 and 17).
            connection.execSQL(
                "CREATE TABLE `trip_attachments` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                    "`track_token` TEXT NOT NULL, `type` TEXT NOT NULL, `uri` TEXT NOT NULL, " +
                    "`ocr_text` TEXT, `file_name` TEXT, `ocr_confidence` REAL NOT NULL DEFAULT 0, " +
                    "`ocr_verified` INTEGER NOT NULL DEFAULT 0, `created_at` INTEGER NOT NULL)",
            )
            connection.execSQL(
                "INSERT INTO `trip_attachments` " +
                    "(`track_token`,`type`,`uri`,`ocr_text`,`file_name`,`ocr_confidence`,`ocr_verified`,`created_at`) " +
                    "VALUES ('t1','ODOMETER_START','file:///x/o.jpg','48213','o.jpg',0.85,1,200)",
            )

            // Run the migration under test.
            MIGRATION_17_18.migrate(connection)

            // The new column now exists.
            val columns = mutableSetOf<String>()
            connection.prepare("PRAGMA table_info(`trip_attachments`)").use { stmt ->
                while (stmt.step()) columns += stmt.getText(1) // index 1 = column name
            }
            assertTrue("odometerAnalysisJson added", "odometerAnalysisJson" in columns)

            // Pre-existing row survived (with a NULL snapshot), and a new row can round-trip a JSON value.
            connection.execSQL(
                "INSERT INTO `trip_attachments` " +
                    "(`track_token`,`type`,`uri`,`ocr_text`,`odometerAnalysisJson`,`created_at`) " +
                    "VALUES ('t1','ODOMETER_END','file:///x/o2.jpg','48263'," +
                    "'{\"reading\":48263,\"source\":\"DEVICE_OCR\",\"computedDistance\":0," +
                    "\"rolledOver\":false,\"synthetic\":false,\"validationError\":null,\"analyzedAtMs\":300}',300)",
            )
            var count = 0
            connection.prepare("SELECT COUNT(*) FROM `trip_attachments`").use { stmt ->
                if (stmt.step()) count = stmt.getLong(0).toInt()
            }
            assertEquals(2, count)

            var preExistingSnapshot: String? = "not-read"
            connection.prepare(
                "SELECT `odometerAnalysisJson` FROM `trip_attachments` WHERE `track_token` = 't1' AND `type` = 'ODOMETER_START'",
            ).use { stmt ->
                if (stmt.step()) preExistingSnapshot = if (stmt.isNull(0)) null else stmt.getText(0)
            }
            assertEquals(null, preExistingSnapshot)
        } finally {
            connection.close()
        }
    }
}
