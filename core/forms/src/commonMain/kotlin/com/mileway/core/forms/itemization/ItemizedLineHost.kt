package com.mileway.core.forms.itemization

import com.mileway.core.data.domain.claim.ItemizedLine
import com.mileway.core.forms.checkedMinorTotal
import com.mileway.core.forms.parseMinorAmount

/** Editable receipt child line; incomplete money remains visible instead of becoming zero. */
data class ItemizedLineInput(
    val description: String = "",
    val amountText: String = "",
)

/** Suggests labels only; receipt money is never invented or silently assigned to a hotel category. */
fun hotelTemplate(): List<ItemizedLineInput> = listOf("Room", "Meals", "Tax").map { ItemizedLineInput(it) }

/** Returns typed child lines only when all labels and amounts are valid. */
fun itemizedDetails(entries: List<ItemizedLineInput>): List<ItemizedLine>? =
    entries.map { entry ->
        if (entry.description.isBlank()) return null
        val amount = parseMinorAmount(entry.amountText) ?: return null
        ItemizedLine(entry.description.trim(), amount)
    }

/** Child total minus receipt total. A null delta means incomplete input or arithmetic overflow. */
fun reconciliationDelta(
    receiptMinor: Long,
    entries: List<ItemizedLineInput>,
): Long? {
    if (receiptMinor < 0) return null
    val details = itemizedDetails(entries) ?: return null
    val total = checkedMinorTotal(details.map { it.amountMinor }) ?: return null
    return total - receiptMinor
}

/** A selected itemization must reconcile; an empty optional field does not block submission. */
fun itemizationError(
    receiptMinor: Long,
    entries: List<ItemizedLineInput>,
): String? = if (entries.isEmpty() || reconciliationDelta(receiptMinor, entries) == 0L) null else "Itemized lines must match the receipt total"
