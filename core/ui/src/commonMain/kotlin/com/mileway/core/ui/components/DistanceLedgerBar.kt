package com.mileway.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.DesignTokens
import kotlin.math.abs

/**
 * Shows how a raw GPS figure became the distance actually being claimed.
 *
 * This is the component that makes a reimbursement number defensible in conversation. Every mileage
 * app shows a single number, so when a reviewer asks "why 12.4 and not 14.9?" the only available
 * answer is "the app said so". Here the user can point at the row that removed the difference.
 *
 * It is also the honest counterpart to the algorithm's bucket accounting: the same
 * cleaned/abnormal/mock/spike split the distance pipeline produces internally is what is rendered,
 * so the explanation cannot drift away from the computation. If they ever disagree, the invariant
 * that governs both is already broken and that is the real bug.
 *
 * ponytail: a Canvas and some Rows — no chart library, no animation, no new dependency. This reads
 * as a receipt, and a receipt that animates is a receipt you trust less.
 */
@Composable
fun DistanceLedgerBar(
    ledger: DistanceLedger,
    modifier: Modifier = Modifier,
) {
    val deductions = ledger.deductions()
    val scale = ledger.rawKm.takeIf { it > 0.0 } ?: 1.0

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { contentDescription = ledger.accessibilitySummary() },
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        LedgerRow(
            label = "Raw GPS",
            valueKm = ledger.rawKm,
            fraction = 1.0,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            emphasis = FontWeight.Normal,
        )

        // Deductions read as an indented sub-list, because they are subordinate to the raw figure
        // rather than peers of it. A flat list would imply they are alternative totals.
        deductions.forEach { d ->
            LedgerRow(
                label = d.label,
                valueKm = d.km,
                fraction = if (scale > 0) d.km / scale else 0.0,
                color = d.tone.color,
                // real minus sign, not a hyphen
                prefix = "− ",
                indent = true,
                // A zero deduction still gets its row: "we checked and found none" is information,
                // and a row that appears only sometimes makes the list look incomplete.
                dim = d.km <= 0.0,
            )
        }

        LedgerRow(
            label = "Cleaned",
            valueKm = ledger.cleanedKm,
            fraction = if (scale > 0) ledger.cleanedKm / scale else 0.0,
            color = StatusTone.Success.color,
            emphasis = FontWeight.SemiBold,
        )

        ledger.odometerKm?.let { odo ->
            val delta = odo - ledger.cleanedKm
            val pct = if (ledger.cleanedKm > 0.0) abs(delta) / ledger.cleanedKm * 100.0 else 0.0
            val within = pct <= ledger.odometerTolerancePercent
            LedgerRow(
                label = "Odometer",
                valueKm = odo,
                fraction = if (scale > 0) odo / scale else 0.0,
                color = if (within) StatusTone.Success.color else StatusTone.Warning.color,
                // The cross-check is the point: a reviewer comparing GPS against a photographed
                // odometer is exactly the audit that this whole surface exists to survive.
                trailing = "${signed(delta)} km, ${fmt1(pct)}%${if (within) " within tolerance" else " over tolerance"}",
            )
        }

        LedgerRow(
            label = "Claimed",
            valueKm = ledger.claimedKm,
            fraction = if (scale > 0) ledger.claimedKm / scale else 0.0,
            color = MaterialTheme.colorScheme.primary,
            emphasis = FontWeight.Bold,
        )
    }
}

@Composable
private fun LedgerRow(
    label: String,
    valueKm: Double,
    fraction: Double,
    color: Color,
    modifier: Modifier = Modifier,
    prefix: String = "",
    emphasis: FontWeight = FontWeight.Normal,
    indent: Boolean = false,
    dim: Boolean = false,
    trailing: String? = null,
) {
    val alpha = if (dim) 0.45f else 1f
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        if (indent) Spacer(Modifier.width(DesignTokens.Spacing.m))
        Text(
            text = prefix + label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = emphasis,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            modifier = Modifier.width(96.dp),
        )
        Text(
            text = "${fmt2(valueKm)} km",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = emphasis,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            modifier = Modifier.width(72.dp),
        )
        LedgerBar(
            fraction = fraction.coerceIn(0.0, 1.0),
            color = color.copy(alpha = alpha),
            modifier = Modifier.weight(1f),
        )
    }
    trailing?.let {
        Row(modifier = Modifier.fillMaxWidth().padding(start = DesignTokens.Spacing.m)) {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LedgerBar(
    fraction: Double,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier =
            modifier
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
    ) {
        drawRect(color = track, size = size)
        val w = (size.width * fraction).toFloat()
        // A non-zero value must never render as literally nothing — a 0.02 km deduction that draws
        // zero pixels reads as "no deduction", which is the opposite of what the row says.
        val drawn = if (fraction > 0.0) maxOf(w, 2f) else 0f
        if (drawn > 0f) drawRect(color = color, size = Size(drawn, size.height), topLeft = Offset.Zero)
    }
}

/**
 * The numbers behind [DistanceLedgerBar].
 *
 * Mirrors the distance pipeline's bucket accounting exactly, so the explanation shown to a user and
 * the computation that produced it cannot drift apart.
 */
data class DistanceLedger(
    val rawKm: Double,
    val cleanedKm: Double,
    val claimedKm: Double,
    val abnormalKm: Double = 0.0,
    val mockKm: Double = 0.0,
    val spikeKm: Double = 0.0,
    val odometerKm: Double? = null,
    val odometerTolerancePercent: Double = 5.0,
) {
    internal data class Deduction(val label: String, val km: Double, val tone: StatusTone)

    internal fun deductions(): List<Deduction> =
        listOf(
            // Mock first: a mock location is the only entry here that implies intent rather than
            // instrument error, so it is the one a reviewer most wants to see called out.
            Deduction("mock", mockKm, StatusTone.Danger),
            Deduction("abnormal", abnormalKm, StatusTone.Warning),
            Deduction("spikes", spikeKm, StatusTone.Info),
        )

    /**
     * The invariant the distance pipeline guarantees. Exposed so a screen can assert rather than
     * assume — a ledger that does not balance is showing a number nobody can defend.
     */
    fun balances(toleranceKm: Double = 0.01): Boolean = abs(cleanedKm - (rawKm - abnormalKm - mockKm)) <= toleranceKm

    fun accessibilitySummary(): String =
        buildString {
            append("Distance ledger. Raw GPS ${fmt2(rawKm)} kilometres. ")
            deductions().filter { it.km > 0.0 }.forEach { append("Minus ${it.label} ${fmt2(it.km)}. ") }
            append("Cleaned ${fmt2(cleanedKm)}. ")
            odometerKm?.let { append("Odometer ${fmt2(it)}. ") }
            append("Claimed ${fmt2(claimedKm)} kilometres.")
        }
}

private fun signed(v: Double): String = (if (v >= 0) "+" else "") + fmt2(v)

private fun fmt2(v: Double): String {
    val r = kotlin.math.round(v * 100.0) / 100.0
    val whole = r.toLong()
    val frac = kotlin.math.round(abs(r - whole) * 100.0).toLong()
    return "$whole.${frac.toString().padStart(2, '0')}"
}

private fun fmt1(v: Double): String {
    val r = kotlin.math.round(v * 10.0) / 10.0
    val whole = r.toLong()
    val frac = kotlin.math.round(abs(r - whole) * 10.0).toLong()
    return "$whole.$frac"
}
