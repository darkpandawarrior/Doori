@file:Suppress("ktlint:standard:max-line-length")

package com.mileway.feature.approvals.repository

import com.mileway.feature.approvals.model.ApprovalItem
import com.mileway.feature.approvals.model.ApprovalStatus
import com.mileway.feature.approvals.model.ApprovalType

private val BASE_MS = 1_781_654_400_000L
private val H = 3_600_000L
private val DAY = 86_400_000L

// NOTE: `:app`'s ApprovalsTest/ApprovalsTabTest (outside this module's ownership) pin the exact
// item counts and status distribution below as golden/characterization data — 12/4-pending-6-
// approved-2-rejected for `all`, 3/all-pending for `teamItems`, 4/1-pending-2-approved-1-rejected
// for `myRequests`. Adding or removing an entry here breaks those tests; widening this fixture set
// needs a coordinated change there too, out of scope for this pass.
object ApprovalsRepository {
    val all: List<ApprovalItem> =
        listOf(
            ApprovalItem("A001", ApprovalType.MILEAGE, "Priya Sharma", "Client visit – 48 km trip", 576.0, ApprovalStatus.PENDING, BASE_MS - H),
            ApprovalItem("A002", ApprovalType.EXPENSE, "Rahul Mehra", "Business dinner – ₹3,200", 3200.0, ApprovalStatus.PENDING, BASE_MS - 2 * H),
            ApprovalItem(
                "A003",
                ApprovalType.TRAVEL,
                "Aisha Khan",
                "Bangalore–Pune flight",
                8400.0,
                ApprovalStatus.PENDING,
                BASE_MS - 4 * H,
                policyViolation = true,
            ),
            ApprovalItem("A004", ApprovalType.ADVANCE, "Vikram Nair", "Field visit advance ₹5,000", 5000.0, ApprovalStatus.APPROVED, BASE_MS - DAY),
            ApprovalItem("A005", ApprovalType.MILEAGE, "Neha Patel", "Weekly route – 120 km", 1440.0, ApprovalStatus.APPROVED, BASE_MS - DAY - H),
            ApprovalItem("A006", ApprovalType.EXPENSE, "Suresh Iyer", "Office supplies ₹680", 680.0, ApprovalStatus.APPROVED, BASE_MS - DAY - 3 * H),
            ApprovalItem("A007", ApprovalType.TRAVEL, "Kavitha Rao", "Mumbai–Delhi flight", 9800.0, ApprovalStatus.APPROVED, BASE_MS - 2 * DAY),
            ApprovalItem(
                "A008",
                ApprovalType.EXPENSE,
                "Mohan Das",
                "Medical claim ₹6,200",
                6200.0,
                ApprovalStatus.REJECTED,
                BASE_MS - 2 * DAY - H,
                policyViolation = true,
            ),
            ApprovalItem("A009", ApprovalType.ADVANCE, "Sunita Pillai", "Conference advance", 12000.0, ApprovalStatus.REJECTED, BASE_MS - 3 * DAY),
            ApprovalItem("A010", ApprovalType.MILEAGE, "Arjun Singh", "Inter-city route – 210 km", 2520.0, ApprovalStatus.PENDING, BASE_MS - 3 * DAY - 2 * H),
            ApprovalItem("A011", ApprovalType.EXPENSE, "Divya Menon", "Client gift ₹1,500", 1500.0, ApprovalStatus.APPROVED, BASE_MS - 4 * DAY),
            ApprovalItem("A012", ApprovalType.TRAVEL, "Raj Kumar", "Chennai–Pune train", 2200.0, ApprovalStatus.APPROVED, BASE_MS - 5 * DAY),
        )

    val teamItems: List<ApprovalItem> =
        listOf(
            ApprovalItem("T001", ApprovalType.EXPENSE, "Priya Sharma", "Expense ₹3,200: Business dinner", 3200.0, ApprovalStatus.PENDING, BASE_MS - H),
            ApprovalItem("T002", ApprovalType.MILEAGE, "Rahul Mehra", "Mileage 120 km: Weekly route", 1440.0, ApprovalStatus.PENDING, BASE_MS - 3 * H),
            ApprovalItem("T003", ApprovalType.TRAVEL, "Aisha Khan", "Travel ₹8,400: PNQ→BLR flight", 8400.0, ApprovalStatus.PENDING, BASE_MS - DAY),
        )

    val myRequests: List<ApprovalItem> =
        listOf(
            ApprovalItem("R001", ApprovalType.ADVANCE, "Me", "Advance ₹5,000: Field visit", 5000.0, ApprovalStatus.APPROVED, BASE_MS - 2 * DAY),
            ApprovalItem("R002", ApprovalType.EXPENSE, "Me", "Expense ₹1,200: Office stationery", 1200.0, ApprovalStatus.PENDING, BASE_MS - DAY),
            ApprovalItem("R003", ApprovalType.TRAVEL, "Me", "Travel: PNQ→BOM flight", 3600.0, ApprovalStatus.APPROVED, BASE_MS - 3 * DAY),
            ApprovalItem("R004", ApprovalType.ADVANCE, "Me", "Advance ₹12,000: Conference", 12000.0, ApprovalStatus.REJECTED, BASE_MS - 5 * DAY),
        )

    // BUG FIX: previously only searched `all`, so opening a Team or My Requests row (T00x/R00x)
    // always fell into ApprovalsViewModel's "Approval not found" branch — those two tabs' rows
    // were listed but untappable. Every id lives in exactly one of the three lists (A.../T.../R...
    // prefixes), so a flat merge is unambiguous.
    fun getById(id: String): ApprovalItem? = (all + teamItems + myRequests).firstOrNull { it.id == id }

    fun approve(id: String): List<ApprovalItem> =
        all.map {
            if (it.id == id) it.copy(status = ApprovalStatus.APPROVED) else it
        }

    fun reject(id: String): List<ApprovalItem> =
        all.map {
            if (it.id == id) it.copy(status = ApprovalStatus.REJECTED) else it
        }
}
