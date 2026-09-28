package com.mileway.core.data.domain.claim

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One line in a [Report]: an expense or a mileage claim. `kotlinx.serialization` encodes this
 * sealed interface with a `"type"` discriminator field, and each subtype's [SerialName] below is
 * that discriminator's wire value — explicit and stable, never the default class name, per L1a's
 * "the discriminator value set is one-way once persisted" rule.
 *
 * ponytail: the full spec also lists PerDiemLine/AdvanceLine subtypes (L1a). This backend slice
 * covers only what the task brief names ("a ClaimLine (expense or mileage)"); add a subtype here
 * — never rename an existing one — when a later lane needs per-diem or advance lines.
 */
@Serializable
sealed interface ClaimLine {
    val id: String
    val amountMinor: Long
    val currency: String
}

@Serializable
@SerialName("expense")
data class ExpenseLine(
    override val id: String,
    override val amountMinor: Long,
    override val currency: String,
    val merchant: String,
    val category: String,
) : ClaimLine

@Serializable
@SerialName("mileage")
data class MileageLine(
    override val id: String,
    override val amountMinor: Long,
    override val currency: String,
    val distanceKm: Double,
    val vehicleKey: String,
) : ClaimLine
