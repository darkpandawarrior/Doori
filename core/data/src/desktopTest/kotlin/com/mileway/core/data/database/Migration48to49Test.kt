package com.mileway.core.data.database

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * L2 — the shared migration gate for MIGRATION_48_49, run against a real SQLite engine
 * ([BundledSQLiteDriver]) on the JVM. Lives in `desktopTest`, not `androidHostTest`, for two
 * compounding, empirically-verified reasons rather than one:
 *
 *  1. `androidx.room.testing.MigrationTestHelper` loads its schema bundle exclusively through
 *     `Context.getAssets()` — even its `SQLiteDriver`-based constructor (which *also* accepts a
 *     `File`, for the actual db location, not the schema) still routes through
 *     `AndroidMigrationTestHelper.loadSchema`. Neither AGP's unified KMP `android { withHostTest {} }`
 *     target nor Room 2.8.5's Gradle plugin wires the exported schema JSON into that source set's
 *     assets (`room-gradle-plugin-2.8.5.jar` has no `androidHostTest`/`hostTest` string at all —
 *     confirmed by decompiling it), so `MigrationTestHelper` is unusable there regardless of
 *     `exportSchema`.
 *  2. Dropping `MigrationTestHelper` for a plain [BundledSQLiteDriver] test — this codebase's own
 *     established pattern for every other `Migration*Test` (see `app/src/androidTest/.../
 *     MediaLibraryMigration39to40Test` etc.) — still doesn't run on `core:data`'s `android` target's
 *     host-test JVM: that target resolves the ANDROID flavor of `androidx.sqlite:sqlite-bundled`,
 *     whose `NativeLibraryLoader` expects the native lib pre-extracted onto `java.library.path` by
 *     the Android runtime/APK packaging (`UnsatisfiedLinkError: no sqliteJni in java.library.path`
 *     on a plain host JVM). The `desktop` target resolves the JVM flavor instead, whose loader
 *     unpacks the bundled native lib itself — the same one `MilewayDatabaseBuilder`'s desktop
 *     actual already proves works standalone in production. `app`'s own `androidTest` migration
 *     tests hit constraint #1's sibling (`exportSchema=false` blocking `MigrationTestHelper`) and
 *     chose the real-driver shape for that reason too (see `DelegationDaoTest`'s doc comment); this
 *     file keeps that shape and additionally finds a JVM target where it actually runs.
 *
 * MIGRATION_48_49 is pure `CREATE TABLE IF NOT EXISTS`/`CREATE INDEX` — no ALTER on any pre-v48
 * table — so unlike the ALTER-column migration tests above, this one doesn't need to seed a v48
 * row first: the assertion is "every documented table/index exists with the documented shape
 * after migrate() runs against a connection", which needs no pre-existing schema to be meaningful.
 */
class Migration48to49Test {
    @Test
    fun `migration_48_49 creates every new claim-domain table`() {
        BundledSQLiteDriver().open(":memory:").use { connection ->
            MIGRATION_48_49.migrate(connection)

            val tables =
                buildSet {
                    connection.prepare("SELECT name FROM sqlite_master WHERE type = 'table'").use { stmt ->
                        while (stmt.step()) add(stmt.getText(0))
                    }
                }
            for (
            table in
            listOf(
                "reports",
                "claim_lines",
                "approval_steps",
                "policy_violations",
                "per_diem_rates",
                "delegate_assignments",
                "period_locks",
                "gl_mappings",
                "statement_imports",
                "pending_payment_journals",
            )
            ) {
                assertTrue(table in tables, "expected `$table` to exist after MIGRATION_48_49")
            }
        }
    }

    @Test
    fun `claim_lines sourceTripId UNIQUE index rejects a second insert for the same trip`() {
        BundledSQLiteDriver().open(":memory:").use { connection ->
            MIGRATION_48_49.migrate(connection)

            connection.execSQL(
                "INSERT INTO `claim_lines` (`id`,`reportId`,`type`,`amountMinor`,`currency`,`policyFlagsCsv`,`sourceTripId`,`detailsJson`,`createdAtMs`) " +
                    "VALUES ('l1','r1','mileage',100,'INR','','trip1','{}',0)",
            )

            assertFailsWith<Exception>("a second claim_lines row with the same sourceTripId must violate the UNIQUE index") {
                connection.execSQL(
                    "INSERT INTO `claim_lines` (`id`,`reportId`,`type`,`amountMinor`,`currency`,`policyFlagsCsv`,`sourceTripId`,`detailsJson`,`createdAtMs`) " +
                        "VALUES ('l2','r1','mileage',200,'INR','','trip1','{}',0)",
                )
            }

            var count = 0L
            connection.prepare("SELECT COUNT(*) FROM `claim_lines` WHERE `sourceTripId` = 'trip1'").use { stmt ->
                if (stmt.step()) count = stmt.getLong(0)
            }
            assertEquals(1L, count)
        }
    }

    @Test
    fun `claim_lines allows multiple NULL sourceTripId rows (manually-entered lines)`() {
        BundledSQLiteDriver().open(":memory:").use { connection ->
            MIGRATION_48_49.migrate(connection)

            connection.execSQL(
                "INSERT INTO `claim_lines` (`id`,`reportId`,`type`,`amountMinor`,`currency`,`policyFlagsCsv`,`sourceTripId`,`detailsJson`,`createdAtMs`) " +
                    "VALUES ('l1','r1','expense',100,'INR','',NULL,'{}',0)",
            )
            connection.execSQL(
                "INSERT INTO `claim_lines` (`id`,`reportId`,`type`,`amountMinor`,`currency`,`policyFlagsCsv`,`sourceTripId`,`detailsJson`,`createdAtMs`) " +
                    "VALUES ('l2','r1','expense',200,'INR','',NULL,'{}',0)",
            )

            var count = 0L
            connection.prepare("SELECT COUNT(*) FROM `claim_lines`").use { stmt ->
                if (stmt.step()) count = stmt.getLong(0)
            }
            assertEquals(2L, count)
        }
    }
}
