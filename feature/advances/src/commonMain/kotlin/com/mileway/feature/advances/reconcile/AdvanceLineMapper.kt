package com.mileway.feature.advances.reconcile

import com.mileway.core.data.domain.claim.AdvanceLine
import com.mileway.feature.advances.data.AdvancesRepository
import com.mileway.feature.advances.model.PettyCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A transient minor-unit view of funded cards, derived from the existing repository. */
fun AdvancesRepository.reconciliationLines(): Flow<List<AdvanceLine>> = activePettyCards().map { cards -> cards.map(PettyCard::toAdvanceLine) }

/** Uses the funded amount, not the remaining balance or pending transaction amount. */
fun PettyCard.toAdvanceLine(): AdvanceLine = AdvanceLine("advance-$id", rupeesToMinorHalfEven(amount), currency, advanceId = id.toString())

/**
 * Converts the Double's shortest decimal representation once at the repository boundary.
 * HALF_EVEN rounds an exact half paisa to the nearest even paisa, including x.xx5 boundaries.
 * Decimal digits avoid binary multiplication moving a decimal tie below or above half.
 * Non-finite, negative and overflowing amounts are refused rather than saturated.
 */
fun rupeesToMinorHalfEven(amount: Double): Long {
    require(amount.isFinite() && amount >= 0) { "Advance amount must be finite and nonnegative" }
    val decimal = amount.toString().lowercase().removePrefix("-")
    val mantissa = decimal.substringBefore('e')
    val exponent = decimal.substringAfter('e', "0").toInt()
    val digits = mantissa.replace(".", "")
    val places = mantissa.substringAfter('.', "").length - exponent - MinorPlaces
    val whole = if (places <= 0) digits + "0".repeat(-places) else digits.dropLast(places).ifEmpty { "0" }
    val minor = requireNotNull(whole.toLongOrNull()) { "Advance amount exceeds supported minor units" }
    if (places <= 0) return minor
    val discarded = digits.takeLast(places).padStart(places, '0')
    val aboveHalf = discarded.first() > '5' || (discarded.first() == '5' && discarded.drop(1).any { it != '0' })
    val half = discarded.first() == '5' && discarded.drop(1).all { it == '0' }
    val roundUp = aboveHalf || (half && minor % EvenDivisor != 0L)
    require(!roundUp || minor < Long.MAX_VALUE) { "Advance amount exceeds supported minor units" }
    return if (roundUp) minor + 1 else minor
}

private const val MinorPlaces = 2
private const val EvenDivisor = 2L
