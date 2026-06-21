package com.miletracker.feature.profile.data

enum class NotifType { APPROVAL, ADVANCE, EXPENSE, POLICY, CARD, PAYABLES, APP_UPDATE, SYSTEM }

data class NotificationRecord(
    val id: String,
    val title: String,
    val body: String,
    val relativeTime: String,
    val isUnread: Boolean,
    val type: NotifType,
)

object NotificationData {
    val all: List<NotificationRecord> =
        listOf(
            NotificationRecord("N001", "Approval Required", "Priya Sharma's mileage claim needs your review", "2 min ago", isUnread = true, NotifType.APPROVAL),
            NotificationRecord("N002", "Advance Approved", "Your advance ADV-001 of ₹8,000 was approved", "1 hr ago", isUnread = true, NotifType.ADVANCE),
            NotificationRecord("N003", "Expense Rejected", "EXP-003 rejected: receipt unclear", "3 hrs ago", isUnread = true, NotifType.EXPENSE),
            NotificationRecord("N004", "Policy Alert", "3 claims exceed the ₹10/km daily cap this week", "Yesterday", isUnread = true, NotifType.POLICY),
            NotificationRecord("N005", "Card Blocked", "CARD-002 was blocked at your request", "Yesterday", isUnread = false, NotifType.CARD),
            NotificationRecord("N006", "Payables Update", "PO-2024-002 approved by finance", "2 days ago", isUnread = false, NotifType.PAYABLES),
            NotificationRecord("N007", "App Update", "Version 2.4.1 available: improved GPS accuracy", "3 days ago", isUnread = false, NotifType.APP_UPDATE),
            NotificationRecord("N008", "System", "Scheduled maintenance: Sun 02:00–04:00 IST", "4 days ago", isUnread = false, NotifType.SYSTEM),
        )

    val unread: List<NotificationRecord> get() = all.filter { it.isUnread }
    val approvals: List<NotificationRecord> get() = all.filter { it.type == NotifType.APPROVAL }
    val system: List<NotificationRecord> get() = all.filter { it.type == NotifType.SYSTEM || it.type == NotifType.APP_UPDATE }
}
