package com.mileway

import android.content.Context
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mileway.core.data.database.MIGRATION_39_40
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * V26 P26.LIB.2: verifies MIGRATION_39_40 — five additive columns on the pre-existing
 * `media_library` table (isFavorite/isDeleted/deletedAt/lastAccessedAt/hasOcr). Seeds a bare v39
 * `media_library` row first (mirrors MIGRATION_2_3's shape) so this is a genuine ALTER-preserves-data
 * check, unlike [DraftExpenseMigration6to7Test]'s CREATE-only case. Instrumented (bundled SQLite
 * native lib), runs on the GMD — not the JVM unit-test gate (exportSchema=false blocks
 * MigrationTestHelper, same as the other migration tests in this file's package).
 */
@RunWith(AndroidJUnit4::class)
class MediaLibraryMigration39to40Test {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration_39_40_test.db"
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
    fun migration_39_40_adds_columns_with_safe_defaults_and_preserves_existing_rows() {
        val connection = BundledSQLiteDriver().open(path)
        try {
            // Bare v39 `media_library` table (MIGRATION_2_3's shape) with one pre-existing row.
            connection.execSQL(
                """
                CREATE TABLE `media_library` (
                    `id`        TEXT    NOT NULL PRIMARY KEY,
                    `uri`       TEXT    NOT NULL,
                    `mimeType`  TEXT    NOT NULL,
                    `label`     TEXT    NOT NULL,
                    `source`    TEXT    NOT NULL,
                    `savedAtMs` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            connection.execSQL(
                "INSERT INTO `media_library` (`id`,`uri`,`mimeType`,`label`,`source`,`savedAtMs`) " +
                    "VALUES ('m1','file:///m1.jpg','image/jpeg','Odometer','CAMERA',1000)",
            )

            MIGRATION_39_40.migrate(connection)

            val columns = mutableSetOf<String>()
            connection.prepare("PRAGMA table_info(`media_library`)").use { stmt ->
                while (stmt.step()) columns += stmt.getText(1) // index 1 = column name
            }
            assertTrue("isFavorite column exists", "isFavorite" in columns)
            assertTrue("isDeleted column exists", "isDeleted" in columns)
            assertTrue("deletedAt column exists", "deletedAt" in columns)
            assertTrue("lastAccessedAt column exists", "lastAccessedAt" in columns)
            assertTrue("hasOcr column exists", "hasOcr" in columns)

            // The pre-existing row survives with the documented safe defaults.
            var isFavorite = -1L
            var isDeleted = -1L
            var deletedAt: Long? = -1L
            var lastAccessedAt: Long? = -1L
            var hasOcr = -1L
            connection.prepare(
                "SELECT `isFavorite`,`isDeleted`,`deletedAt`,`lastAccessedAt`,`hasOcr` FROM `media_library` WHERE `id` = 'm1'",
            ).use { stmt ->
                if (stmt.step()) {
                    isFavorite = stmt.getLong(0)
                    isDeleted = stmt.getLong(1)
                    deletedAt = if (stmt.isNull(2)) null else stmt.getLong(2)
                    lastAccessedAt = if (stmt.isNull(3)) null else stmt.getLong(3)
                    hasOcr = stmt.getLong(4)
                }
            }
            assertEquals(0L, isFavorite)
            assertEquals(0L, isDeleted)
            assertEquals(null, deletedAt)
            assertEquals(null, lastAccessedAt)
            assertEquals(0L, hasOcr)

            // New columns are writable — the soft-delete/favorite/touch DAO methods' underlying SQL shape.
            connection.execSQL(
                "UPDATE `media_library` SET `isFavorite` = 1, `isDeleted` = 1, `deletedAt` = 2000, " +
                    "`lastAccessedAt` = 3000, `hasOcr` = 1 WHERE `id` = 'm1'",
            )
            var updatedIsDeleted = 0L
            connection.prepare("SELECT `isDeleted` FROM `media_library` WHERE `id` = 'm1'").use { stmt ->
                if (stmt.step()) updatedIsDeleted = stmt.getLong(0)
            }
            assertEquals(1L, updatedIsDeleted)
        } finally {
            connection.close()
        }
    }
}
