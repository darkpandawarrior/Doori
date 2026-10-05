package com.mileway.feature.logging.model

import com.mileway.core.data.domain.claim.formatMinorCurrency
import kotlin.math.roundToLong

/** Saved minor units win; legacy major amounts round to the nearest minor unit for display. */
fun formatExpenseAmount(
    amountMajor: Double,
    currency: String,
    amountMinor: Long? = null,
): String = formatMinorCurrency(amountMinor ?: (amountMajor * 100).roundToLong(), currency)
