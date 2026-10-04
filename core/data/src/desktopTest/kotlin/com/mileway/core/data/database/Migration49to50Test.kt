package com.mileway.core.data.database

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Migration49to50Test {
    @Test
    fun `migration 49 to 50 preserves existing reports and actions with null new columns`() {
        BundledSQLiteDriver().open(":memory:").use { db ->
            MIGRATION_48_49.migrate(db)
            db.execSQL("INSERT INTO reports VALUES ('old', 'employee', 'SUBMITTED', 7, 123, 456)")
            db.execSQL("INSERT INTO approval_steps VALUES (9, 'old', 0, 'manager', 1000, 'proxy', 'manager', 'APPROVE', 'Checked', 789)")
            MIGRATION_49_50.migrate(db)
            db.prepare("SELECT * FROM reports WHERE id = 'old'").use { row ->
                assertTrue(row.step())
                assertEquals("old", row.getText(0))
                assertEquals("employee", row.getText(1))
                assertEquals("SUBMITTED", row.getText(2))
                assertEquals(7L, row.getLong(3))
                assertEquals(123L, row.getLong(4))
                assertEquals(456L, row.getLong(5))
                assertTrue(row.isNull(6))
                assertTrue(row.isNull(7))
            }
            db.prepare("SELECT * FROM approval_steps WHERE id = 9").use { row ->
                assertTrue(row.step())
                assertEquals(9L, row.getLong(0))
                assertEquals("old", row.getText(1))
                assertEquals(0L, row.getLong(2))
                assertEquals("manager", row.getText(3))
                assertEquals(1000L, row.getLong(4))
                assertEquals("proxy", row.getText(5))
                assertEquals("manager", row.getText(6))
                assertEquals("APPROVE", row.getText(7))
                assertEquals("Checked", row.getText(8))
                assertEquals(789L, row.getLong(9))
                assertTrue(row.isNull(10))
            }
        }
    }
}
