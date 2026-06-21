package com.miletracker

import com.miletracker.core.data.model.db.AttachmentType
import com.miletracker.core.data.model.db.TripAttachmentEntity
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * JVM-pure unit tests for the [TripAttachmentEntity] model and attachment grouping / mapping
 * logic that mirrors what the ViewModel and detail screen consume.
 *
 * No Room, no Android, no Robolectric — all tests run on the plain JVM.
 */
class TripAttachmentTest {

    // -----------------------------------------------------------------------
    // Model construction & field correctness
    // -----------------------------------------------------------------------

    @Test
    fun `receipt entity has correct type and uri`() {
        val entity = TripAttachmentEntity(
            trackToken = "track-1",
            type = AttachmentType.RECEIPT,
            uri = "content://media/receipt1.jpg",
            createdAt = 1_000L
        )
        assertEquals(AttachmentType.RECEIPT, entity.type)
        assertEquals("content://media/receipt1.jpg", entity.uri)
        assertNull(entity.ocrText)
    }

    @Test
    fun `odometer start entity stores ocr text`() {
        val entity = TripAttachmentEntity(
            trackToken = "track-1",
            type = AttachmentType.ODOMETER_START,
            uri = "content://media/odo_start.jpg",
            ocrText = "48213",
            createdAt = 2_000L
        )
        assertEquals(AttachmentType.ODOMETER_START, entity.type)
        assertEquals("48213", entity.ocrText)
    }

    @Test
    fun `odometer end entity stores ocr text`() {
        val entity = TripAttachmentEntity(
            trackToken = "track-1",
            type = AttachmentType.ODOMETER_END,
            uri = "content://media/odo_end.jpg",
            ocrText = "48221",
            createdAt = 3_000L
        )
        assertEquals(AttachmentType.ODOMETER_END, entity.type)
        assertEquals("48221", entity.ocrText)
    }

    @Test
    fun `entity defaults id to 0 when not supplied`() {
        val entity = TripAttachmentEntity(
            trackToken = "track-1",
            type = AttachmentType.RECEIPT,
            uri = "content://media/r.jpg"
        )
        assertEquals(0L, entity.id)
    }

    // -----------------------------------------------------------------------
    // Grouping helpers (mirrors what the detail screen does in-memory)
    // -----------------------------------------------------------------------

    private fun makeEntities(): List<TripAttachmentEntity> = listOf(
        TripAttachmentEntity(id = 1, trackToken = "t1", type = AttachmentType.RECEIPT,        uri = "uri-r1",  createdAt = 100L),
        TripAttachmentEntity(id = 2, trackToken = "t1", type = AttachmentType.RECEIPT,        uri = "uri-r2",  createdAt = 200L),
        TripAttachmentEntity(id = 3, trackToken = "t1", type = AttachmentType.ODOMETER_START, uri = "uri-os",  ocrText = "48213", createdAt = 50L),
        TripAttachmentEntity(id = 4, trackToken = "t1", type = AttachmentType.ODOMETER_END,   uri = "uri-oe",  ocrText = "48221", createdAt = 300L),
    )

    @Test
    fun `filter receipts returns only receipt-type rows`() {
        val receipts = makeEntities().filter { it.type == AttachmentType.RECEIPT }
        assertEquals(2, receipts.size)
        assertTrue(receipts.all { it.type == AttachmentType.RECEIPT })
    }

    @Test
    fun `latest odometer start is resolved by created_at descending`() {
        // Add a second odometer-start with later timestamp to ensure "last" wins.
        val entities = makeEntities() + TripAttachmentEntity(
            id = 5, trackToken = "t1",
            type = AttachmentType.ODOMETER_START,
            uri = "uri-os-newer",
            ocrText = "48215",
            createdAt = 500L
        )
        val latestStart = entities
            .filter { it.type == AttachmentType.ODOMETER_START }
            .maxByOrNull { it.createdAt }
        assertEquals("uri-os-newer", latestStart?.uri)
        assertEquals("48215", latestStart?.ocrText)
    }

    @Test
    fun `latest odometer end returns correct entity`() {
        val latestEnd = makeEntities()
            .filter { it.type == AttachmentType.ODOMETER_END }
            .maxByOrNull { it.createdAt }
        assertEquals("48221", latestEnd?.ocrText)
    }

    @Test
    fun `no odometer end returns null`() {
        val receiptsOnly = makeEntities().filter { it.type == AttachmentType.RECEIPT }
        val odoEnd = receiptsOnly.lastOrNull { it.type == AttachmentType.ODOMETER_END }
        assertNull(odoEnd)
    }

    // -----------------------------------------------------------------------
    // Odometer distance calculation (same logic as OdometerCard in UI)
    // -----------------------------------------------------------------------

    @Test
    fun `odometer distance is end minus start when both present`() {
        val startOcr = "48213"
        val endOcr = "48221"
        val distance = odometerDistance(startOcr, endOcr)
        assertEquals(8.0, distance)
    }

    @Test
    fun `odometer distance returns null when start is blank`() {
        assertNull(odometerDistance("", "48221"))
    }

    @Test
    fun `odometer distance returns null when end is less than start`() {
        assertNull(odometerDistance("48221", "48213"))
    }

    @Test
    fun `odometer distance returns null when reading is non-numeric`() {
        assertNull(odometerDistance("abc", "def"))
    }

    // -----------------------------------------------------------------------
    // AttachmentType enum — completeness guard
    // -----------------------------------------------------------------------

    @Test
    fun `AttachmentType has exactly three values`() {
        assertEquals(3, AttachmentType.entries.size)
        assertTrue(AttachmentType.entries.contains(AttachmentType.RECEIPT))
        assertTrue(AttachmentType.entries.contains(AttachmentType.ODOMETER_START))
        assertTrue(AttachmentType.entries.contains(AttachmentType.ODOMETER_END))
    }

    // -----------------------------------------------------------------------
    // Multiple track tokens are independent
    // -----------------------------------------------------------------------

    @Test
    fun `attachments for different track tokens do not bleed into each other`() {
        val allAttachments = listOf(
            TripAttachmentEntity(id = 1, trackToken = "track-A", type = AttachmentType.RECEIPT, uri = "a1", createdAt = 1L),
            TripAttachmentEntity(id = 2, trackToken = "track-B", type = AttachmentType.RECEIPT, uri = "b1", createdAt = 2L),
        )
        val forA = allAttachments.filter { it.trackToken == "track-A" }
        val forB = allAttachments.filter { it.trackToken == "track-B" }
        assertEquals(1, forA.size)
        assertEquals("a1", forA.first().uri)
        assertEquals(1, forB.size)
        assertEquals("b1", forB.first().uri)
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /** Pure Kotlin mirror of the OdometerCard distance calculation. */
    private fun odometerDistance(startReading: String, endReading: String): Double? {
        val s = startReading.toDoubleOrNull()
        val e = endReading.toDoubleOrNull()
        return if (s != null && e != null && e >= s) e - s else null
    }
}
