package com.mileway.feature.advances.ui.components

import com.mileway.feature.advances.upi.UpiPayment
import com.mileway.feature.advances.upi.UpiQrParser

/** UI-layer wrapper around [UpiQrParser] for [com.mileway.feature.advances.ui.QrScanResultSheet]. */
sealed interface QrScanResult {
    data class Parsed(val payment: UpiPayment) : QrScanResult

    data object ParseError : QrScanResult
}

fun parseScanResult(text: String): QrScanResult = UpiQrParser.parse(text)?.let { QrScanResult.Parsed(it) } ?: QrScanResult.ParseError
