package com.mileway.feature.advances.upi

/*
 * PLAN_V35.P3: scan-to-pay UPI QR parsing (pure Kotlin, no android.* / java.* — shared with iOS/Wear).
 * Two source formats seen in the wild: a `upi://pay?...` deep-link (BharatQR apps, most wallets)
 * and a raw EMV TLV payload (bank-issued static QR stickers). Both resolve to the same [UpiPayment].
 */

/** Parsed UPI payment intent. Only [pa] (payee VPA) is mandatory; everything else is optional. */
data class UpiPayment(
    val pa: String,
    val pn: String? = null,
    val mc: String? = null,
    val am: Double? = null,
    val tr: String? = null,
    val tn: String? = null,
)

object UpiQrParser {
    /** Returns null if [text] is neither a valid `upi://pay` link nor an EMV TLV blob with a `pa`. */
    fun parse(text: String): UpiPayment? {
        val trimmed = text.trim()
        return if (trimmed.startsWith("upi://", ignoreCase = true)) {
            parseUpiLink(trimmed)
        } else {
            parseEmv(trimmed)
        }
    }

    private fun parseUpiLink(text: String): UpiPayment? {
        val queryStart = text.indexOf('?')
        if (queryStart == -1) return null
        val params = mutableMapOf<String, String>()
        text.substring(queryStart + 1).split('&').forEach { pair ->
            if (pair.isEmpty()) return@forEach
            val eq = pair.indexOf('=')
            val key = if (eq == -1) pair else pair.substring(0, eq)
            val rawValue = if (eq == -1) "" else pair.substring(eq + 1)
            params[key] = decodePercent(rawValue)
        }
        val pa = params["pa"]?.takeIf { it.isNotBlank() } ?: return null
        return UpiPayment(
            pa = pa,
            pn = params["pn"]?.takeIf { it.isNotBlank() },
            mc = params["mc"]?.takeIf { it.isNotBlank() },
            am = params["am"]?.toDoubleOrNull(),
            tr = params["tr"]?.takeIf { it.isNotBlank() },
            tn = params["tn"]?.takeIf { it.isNotBlank() },
        )
    }

    /** Minimal `application/x-www-form-urlencoded` decoder — no java.net.URLDecoder in commonMain. */
    private fun decodePercent(s: String): String {
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            when (val c = s[i]) {
                '+' -> {
                    sb.append(' ')
                    i++
                }
                '%' -> {
                    val code = if (i + 3 <= s.length) s.substring(i + 1, i + 3).toIntOrNull(16) else null
                    if (code != null) {
                        sb.append(code.toChar())
                        i += 3
                    } else {
                        sb.append(c)
                        i++
                    }
                }
                else -> {
                    sb.append(c)
                    i++
                }
            }
        }
        return sb.toString()
    }

    // EMV QR (tag-length-value, 2-digit tag + 2-digit decimal length). Nested templates: tag 26
    // (merchant account info) subtag 01 -> pa; tag 62 (additional data) subtag 05 -> tr/tn.
    private fun parseEmv(text: String): UpiPayment? {
        val root = parseTlv(text) ?: return null
        val pa = root["26"]?.let { parseTlv(it) }?.get("01")?.takeIf { it.isNotBlank() } ?: return null
        val trTn = root["62"]?.let { parseTlv(it) }?.get("05")?.takeIf { it.isNotBlank() }
        return UpiPayment(
            pa = pa,
            pn = root["59"]?.takeIf { it.isNotBlank() },
            mc = root["52"]?.takeIf { it.isNotBlank() },
            am = root["54"]?.toDoubleOrNull(),
            tr = trTn,
            tn = trTn,
        )
    }

    private fun parseTlv(text: String): Map<String, String>? {
        val result = mutableMapOf<String, String>()
        var i = 0
        while (i + 4 <= text.length) {
            val tag = text.substring(i, i + 2)
            val len = text.substring(i + 2, i + 4).toIntOrNull() ?: break
            val valueStart = i + 4
            val valueEnd = valueStart + len
            if (valueEnd > text.length) break
            result[tag] = text.substring(valueStart, valueEnd)
            i = valueEnd
        }
        return result.ifEmpty { null }
    }
}
