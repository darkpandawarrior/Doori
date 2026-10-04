package com.mileway.core.forms.field

import com.mileway.core.data.domain.claim.CostSplit
import com.mileway.core.data.domain.claim.SplitTarget
import com.mileway.core.forms.checkedMinorTotal
import com.mileway.core.forms.parseMinorAmount

private const val FullPercentage = 10_000L

/** Raw edit state preserves incomplete percentage input without converting money through Double. */
data class PercentageSplitInput(
    val target: SplitTarget = SplitTarget.COST_CENTER,
    val targetId: String = "",
    val percentageText: String = "",
)

/** Computes exact allocations; when percentages total 100%, the last line receives rounding cents. */
fun percentageAllocations(
    anchorMinor: Long,
    entries: List<PercentageSplitInput>,
): List<CostSplit>? {
    if (anchorMinor <= 0 || entries.isEmpty()) return null
    val percentages = entries.map { parseMinorAmount(it.percentageText) ?: return null }
    if (percentages.any { it <= 0 || it > FullPercentage } || checkedMinorTotal(percentages) != FullPercentage) return null
    if (entries.any { it.targetId.isBlank() }) return null
    if (entries.map { it.target to it.targetId.trim().lowercase() }.distinct().size != entries.size) return null
    val amounts = percentages.map { anchorMinor / FullPercentage * it + anchorMinor % FullPercentage * it / FullPercentage }.toMutableList()
    val floorTotal = checkedMinorTotal(amounts) ?: return null
    amounts[amounts.lastIndex] += anchorMinor - floorTotal
    return entries.mapIndexed { index, entry -> CostSplit(entry.target, entry.targetId.trim(), percentages[index].toInt(), amounts[index]) }
}

/** Rejects missing targets, invalid percentages, or totals which cannot reconcile to the host anchor. */
fun splitError(
    anchorMinor: Long,
    entries: List<PercentageSplitInput>,
): String? =
    if (entries.isEmpty() ||
        percentageAllocations(anchorMinor, entries) != null
    ) {
        null
    } else {
        "Splits must total 100% of the anchored amount with distinct targets"
    }
