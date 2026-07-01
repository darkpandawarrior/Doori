package com.mileway

import com.mileway.feature.profile.data.NotifType
import com.mileway.feature.profile.data.NotificationData
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationsTest {

    @Test
    fun `all has 8 deterministic entries`() {
        assertEquals(8, NotificationData.all.size)
    }

    @Test
    fun `ids are unique`() {
        val ids = NotificationData.all.map { it.id }
        assertEquals(ids.distinct(), ids)
    }

    @Test
    fun `unread count is 4`() {
        assertEquals(4, NotificationData.unread.size)
    }

    @Test
    fun `read count is 4`() {
        val read = NotificationData.all.filter { !it.isUnread }
        assertEquals(4, read.size)
    }

    @Test
    fun `approvals filter returns APPROVAL type only`() {
        assertTrue(NotificationData.approvals.isNotEmpty())
        NotificationData.approvals.forEach { notif ->
            assertEquals(NotifType.APPROVAL, notif.type)
        }
    }

    @Test
    fun `N001 is approval required and unread`() {
        val n001 = NotificationData.all.first { it.id == "N001" }
        assertEquals("Approval Required", n001.title)
        assertTrue(n001.isUnread)
        assertEquals(NotifType.APPROVAL, n001.type)
    }

    @Test
    fun `N002 advance approved is unread`() {
        val n002 = NotificationData.all.first { it.id == "N002" }
        assertEquals("Advance Approved", n002.title)
        assertTrue(n002.isUnread)
    }

    @Test
    fun `N004 policy alert is unread`() {
        val n004 = NotificationData.all.first { it.id == "N004" }
        assertEquals("Policy Alert", n004.title)
        assertTrue(n004.isUnread)
    }

    @Test
    fun `N005 onwards are read`() {
        val readIds = listOf("N005", "N006", "N007", "N008")
        readIds.forEach { id ->
            val notif = NotificationData.all.first { it.id == id }
            assertTrue(!notif.isUnread, "Expected $id to be read")
        }
    }

    @Test
    fun `system filter returns SYSTEM and APP_UPDATE types`() {
        val sys = NotificationData.system
        assertTrue(sys.isNotEmpty())
        sys.forEach { notif ->
            assertTrue(
                notif.type == NotifType.SYSTEM || notif.type == NotifType.APP_UPDATE,
                "Unexpected type ${notif.type} for ${notif.id}"
            )
        }
    }

    @Test
    fun `all entries have non-blank title and body`() {
        NotificationData.all.forEach { notif ->
            assertTrue(notif.title.isNotBlank(), "Blank title for ${notif.id}")
            assertTrue(notif.body.isNotBlank(), "Blank body for ${notif.id}")
        }
    }

    @Test
    fun `all entries have non-blank relative time`() {
        NotificationData.all.forEach { notif ->
            assertTrue(notif.relativeTime.isNotBlank(), "Blank relativeTime for ${notif.id}")
        }
    }
}
