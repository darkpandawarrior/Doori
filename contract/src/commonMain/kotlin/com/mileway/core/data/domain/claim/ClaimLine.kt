package com.mileway.core.data.domain.claim

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One line in a [Report]: an expense, mileage, per-diem or advance claim. `kotlinx.serialization`
 * encodes this sealed interface with a `"type"` discriminator field, and each subtype's
 * [SerialName] below is that discriminator's wire value — explicit and stable, never the default
 * class name, per L1a's "the discriminator value set is one-way once persisted" rule.
 *
 * The five fields declared here are common to every claim line: [fxRatePinnedAt] is the epoch-
 * millis instant the FX rate used to compute [amountMinor] was locked (null when the line's
 * [currency] needs no conversion), [policyFlags] are policy-engine violation codes attached at
 * capture time, [cardMatchId] links a line to a matched corporate-card transaction, and
 * [sourceTripId] links a line back to the trip/check-in it was generated from.
 */
@Serializable
sealed interface ClaimLine {
    val id: String
    val amountMinor: Long
    val currency: String
    val fxRatePinnedAt: Long?
    val policyFlags: List<String>
    val cardMatchId: String?
    val sourceTripId: String?
}

@Serializable
@SerialName("expense")
data class ExpenseLine(
    override val id: String,
    override val amountMinor: Long,
    override val currency: String,
    override val fxRatePinnedAt: Long? = null,
    override val policyFlags: List<String> = emptyList(),
    override val cardMatchId: String? = null,
    override val sourceTripId: String? = null,
    val merchant: String,
    val category: String,
) : ClaimLine

@Serializable
@SerialName("mileage")
data class MileageLine(
    override val id: String,
    override val amountMinor: Long,
    override val currency: String,
    override val fxRatePinnedAt: Long? = null,
    override val policyFlags: List<String> = emptyList(),
    override val cardMatchId: String? = null,
    override val sourceTripId: String? = null,
    val distanceKm: Double,
    val vehicleKey: String,
) : ClaimLine

@Serializable
@SerialName("per_diem")
data class PerDiemLine(
    override val id: String,
    override val amountMinor: Long,
    override val currency: String,
    override val fxRatePinnedAt: Long? = null,
    override val policyFlags: List<String> = emptyList(),
    override val cardMatchId: String? = null,
    override val sourceTripId: String? = null,
    val days: Int,
    val dailyRateMinor: Long,
) : ClaimLine

@Serializable
@SerialName("advance")
data class AdvanceLine(
    override val id: String,
    override val amountMinor: Long,
    override val currency: String,
    override val fxRatePinnedAt: Long? = null,
    override val policyFlags: List<String> = emptyList(),
    override val cardMatchId: String? = null,
    override val sourceTripId: String? = null,
    val advanceId: String,
    val reconciled: Boolean = false,
) : ClaimLine
