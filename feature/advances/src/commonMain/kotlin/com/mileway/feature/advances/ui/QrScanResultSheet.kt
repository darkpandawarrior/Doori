package com.mileway.feature.advances.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.sheet.DetailInfoBottomSheet
import com.mileway.core.ui.components.sheet.DetailInfoCard
import com.mileway.core.ui.components.sheet.DetailInfoRow
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.advances_scan_amount
import com.mileway.core.ui.resources.advances_scan_parse_error
import com.mileway.core.ui.resources.advances_scan_parse_error_body
import com.mileway.core.ui.resources.advances_scan_pay_from
import com.mileway.core.ui.resources.advances_scan_payee
import com.mileway.core.ui.resources.advances_scan_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.advances.ui.components.QrScanResult
import com.mileway.feature.advances.ui.components.formatMoney
import com.mileway.feature.advances.ui.components.parseScanResult
import org.jetbrains.compose.resources.stringResource

/**
 * PLAN_V35.P4: scan-to-pay result — the host (feature/media's QR scanner) passes the raw scanned
 * text in; this sheet is pure UI over [parseScanResult]. No camera integration here, and real UPI
 * payment execution is deferred (no backend) — [onPayFrom] is a stub hoisted callback.
 */
@Composable
fun QrScanResultSheet(
    scannedText: String,
    payFromCardTitle: String,
    onPayFrom: () -> Unit,
    onDismiss: () -> Unit,
) {
    val result = remember(scannedText) { parseScanResult(scannedText) }
    when (result) {
        is QrScanResult.Parsed ->
            DetailInfoBottomSheet(
                title = stringResource(Res.string.advances_scan_title),
                headerGradient = listOf(Color(0xFF2D2F6B), Color(0xFF6367FA)),
                headerIcon = Icons.Filled.QrCodeScanner,
                onDismiss = onDismiss,
            ) {
                DetailInfoCard {
                    DetailInfoRow(stringResource(Res.string.advances_scan_payee), result.payment.pn ?: result.payment.pa)
                    result.payment.am?.let { amount ->
                        DetailInfoRow(stringResource(Res.string.advances_scan_amount), "₹${formatMoney(amount)}")
                    }
                }
                Button(onClick = onPayFrom, shape = DesignTokens.Shape.button) {
                    Text(stringResource(Res.string.advances_scan_pay_from, payFromCardTitle))
                }
            }
        QrScanResult.ParseError ->
            DetailInfoBottomSheet(
                title = stringResource(Res.string.advances_scan_parse_error),
                headerGradient = listOf(DesignTokens.StatusColors.error, DesignTokens.StatusColors.error),
                headerIcon = Icons.Filled.ErrorOutline,
                onDismiss = onDismiss,
            ) {
                Text(
                    stringResource(Res.string.advances_scan_parse_error_body),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
    }
}
