package com.mileway.core.data.claim

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.siddharth.kmp.offlineoutbox.MIGRATION_1_2
import com.siddharth.kmp.offlineoutbox.OutboxDatabase
import com.siddharth.kmp.offlineoutbox.RoomOpOutbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * L2 acceptance #5: [ReportRepository.save] durably queues a `"report"` op onto the kmp-toolkit
 * offline-outbox (`op_outbox`, via [RoomOpOutbox]), and that queue survives a simulated process
 * kill. Lives in `desktopTest`, not `androidHostTest`, for the same reason as `Migration48to49Test`
 * (see its doc comment): `core:data`'s `android` target's host-test JVM resolves the ANDROID flavor
 * of `androidx.sqlite:sqlite-bundled`, which needs a real Android runtime to load its native
 * library (`UnsatisfiedLinkError` on a plain host JVM, confirmed empirically) — the `desktop` target
 * resolves the JVM flavor instead, which is why the context-free `Room.databaseBuilder` below (the
 * same shape the toolkit's own `offline-outbox` jvmMain `buildOutboxDatabase()` and
 * `MilewayDatabaseBuilder`'s desktop actual both use in production) works here.
 *
 * "Enqueue via one [RoomOpOutbox] instance, close its [OutboxDatabase], open a brand-new
 * [RoomOpOutbox]/database pair against the same on-disk file, read it back" IS what a process kill
 * and restart looks like to a durable, file-backed SQLite store.
 */
class OpOutboxDurabilityTest {
    private val dbFile = File.createTempFile("op_outbox_durability_test", ".db")

    private fun openOutbox(): OutboxDatabase =
        Room
            .databaseBuilder<OutboxDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .addMigrations(MIGRATION_1_2)
            .build()

    @After
    fun cleanup() {
        dbFile.delete()
    }

    @Test
    fun `a report op enqueued before a simulated process kill is still pending after`() =
        runBlocking {
            val firstProcess = openOutbox()
            RoomOpOutbox(firstProcess.opOutboxDao()).enqueue(type = "report", payload = """{"id":"r1"}""")
            firstProcess.close() // simulates the process dying with the write already committed to disk

            val secondProcess = openOutbox() // simulates a fresh process re-opening the same on-disk file
            val pending = RoomOpOutbox(secondProcess.opOutboxDao()).pending().first()
            secondProcess.close()

            assertEquals(1, pending.size)
            assertEquals("report", pending.single().type)
            assertTrue(pending.single().payload.contains("r1"))
        }
}
