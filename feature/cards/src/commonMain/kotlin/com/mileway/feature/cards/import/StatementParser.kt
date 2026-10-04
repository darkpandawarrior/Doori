package com.mileway.feature.cards.import

import com.mileway.core.data.domain.claim.isFxSourceDate
import com.mileway.core.data.session.sha256Hex
import com.mileway.core.forms.parseMinorAmount

/** A positive purchase in billed currency, optionally retaining the original foreign amount. */
data class StatementRow(
    val id: String,
    val postingDate: String,
    val merchant: String,
    val amountMinor: Long,
    val currency: String = "INR",
    val foreignAmountMinor: Long? = null,
    val foreignCurrency: String? = null,
)

data class ParsedStatement(
    val source: String,
    val rows: List<StatementRow>,
)

/** Offline CSV and OFX SGML/XML purchase reader. No XML resolver, account linking or network I/O. */
object StatementParser {
    const val MaxCharacters = 2_000_000
    private val currencies = setOf("INR", "USD", "EUR", "GBP", "AED", "SGD")

    fun parse(text: String): ParsedStatement {
        require(text.length <= MaxCharacters) { "Statement exceeds the 2 MB text limit" }
        val result = if (text.contains("<OFX>", ignoreCase = true)) ParsedStatement("OFX", ofx(text)) else ParsedStatement("CSV", csv(text))
        require(result.rows.isNotEmpty()) { "Statement contains no purchases" }
        require(
            result.rows
                .map { it.id }
                .distinct()
                .size == result.rows.size,
        ) { "Duplicate transaction ids in statement" }
        return result
    }

    /** CSV uses positive purchases, ISO dates and exact two-decimal major-unit amounts. */
    private fun csv(text: String): List<StatementRow> {
        val records = csvRecords(text.removePrefix("\uFEFF"))
        val header = records.firstOrNull()?.map { it.trim().lowercase() }.orEmpty()
        require(header.distinct().size == header.size && header.containsAll(listOf("date", "merchant", "amount", "currency"))) {
            "CSV requires date, merchant, amount, currency headers"
        }
        return records.drop(1).filterNot { it.all(String::isBlank) }.map { cells ->
            require(cells.size == header.size) { "CSV column count differs from header" }

            fun cell(name: String): String =
                header
                    .indexOf(name)
                    .takeIf { it >= 0 }
                    ?.let { cells[it].trim() }
                    .orEmpty()
            val foreign = cell("foreign_amount").takeIf { it.isNotBlank() }?.let(::positiveAmount)
            val foreignCurrency = cell("foreign_currency").takeIf { it.isNotBlank() }
            row(
                cell("id").ifBlank { "csv:${sha256Hex(cells.joinToString("|"))}" },
                cell("date"),
                cell("merchant"),
                positiveAmount(cell("amount")),
                cell("currency"),
                foreign,
                foreignCurrency,
            )
        }
    }

    private fun ofx(text: String): List<StatementRow> {
        require(Regex("<CURDEF>", RegexOption.IGNORE_CASE).findAll(text).count() == 1) { "Import one OFX account at a time" }
        val currency = tag(text, "CURDEF")
        val account = tag(text, "ACCTID")
        require(account.isNotBlank()) { "OFX account id is required for transaction identity" }
        return Regex("<STMTTRN>(.*?)</STMTTRN>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .findAll(text)
            .mapNotNull { match ->
                val block = match.groupValues[1]
                val amount = tag(block, "TRNAMT")
                // Credits and bill payments are not expense purchases.
                if (!amount.startsWith('-') || tag(block, "TRNTYPE").uppercase() !in setOf("DEBIT", "POS")) return@mapNotNull null
                val posted = tag(block, "DTPOSTED").take(8)
                require(posted.length == 8 && posted.all(Char::isDigit)) { "Invalid OFX posting date" }
                val id = tag(block, "FITID")
                require(id.isNotBlank()) { "OFX transaction id is required" }
                row(
                    "ofx:${sha256Hex("$account:$id")}",
                    "${posted.take(4)}-${posted.substring(4, 6)}-${posted.takeLast(2)}",
                    tag(block, "NAME").ifBlank { tag(block, "MEMO") },
                    positiveAmount(amount.removePrefix("-")),
                    currency,
                    null,
                    null,
                )
            }.toList()
    }

    private fun tag(
        text: String,
        name: String,
    ): String =
        Regex("<$name>([^<]*)", RegexOption.IGNORE_CASE)
            .find(text)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.replace("&amp;", "&")
            ?.replace("&lt;", "<")
            ?.replace("&gt;", ">")
            ?.replace("&quot;", "\"")
            ?.replace("&apos;", "'")
            .orEmpty()

    private fun row(
        id: String,
        date: String,
        merchant: String,
        amount: Long,
        currency: String,
        foreign: Long?,
        foreignCurrency: String?,
    ): StatementRow {
        require(id.isNotBlank() && id.length <= 256) { "Invalid statement transaction id" }
        require(isFxSourceDate(date)) { "Invalid statement date; use YYYY-MM-DD" }
        require(merchant.isNotBlank() && merchant.length <= 256) { "Statement merchant is required" }
        require(currency in currencies && (foreignCurrency == null || foreignCurrency in currencies)) { "Unsupported statement currency" }
        require((foreign == null) == (foreignCurrency == null)) { "Foreign amount and currency must both be present" }
        return StatementRow(id, date, merchant, amount, currency, foreign, foreignCurrency)
    }

    private fun positiveAmount(text: String): Long = requireNotNull(parseMinorAmount(text)?.takeIf { it > 0 }) { "Invalid purchase amount" }
}

/** Quoted CSV fields support commas, embedded newlines and doubled quotes without floating money. */
private fun csvRecords(text: String): List<List<String>> {
    val records = mutableListOf<List<String>>()
    val cells = mutableListOf<String>()
    val value = StringBuilder()
    var quoted = false
    var closed = false
    var index = 0

    fun field() {
        cells.add(value.toString())
        value.clear()
        closed = false
    }
    while (index < text.length) {
        val char = text[index++]
        when {
            quoted && char == '"' -> {
                if (text.getOrNull(index) == '"') {
                    value.append('"')
                    index++
                } else {
                    quoted = false
                    closed = true
                }
            }
            quoted -> value.append(char)
            char == '"' -> {
                require(value.isEmpty() && !closed) { "Invalid CSV quote" }
                quoted = true
            }
            char == ',' -> field()
            char == '\n' -> {
                field()
                records.add(cells.toList())
                cells.clear()
            }
            char == '\r' -> {
                require(text.getOrNull(index) == '\n') { "Use LF or CRLF line endings" }
            }
            else -> {
                require(!closed) { "Unexpected text after CSV quote" }
                value.append(char)
            }
        }
    }
    require(!quoted) { "Unclosed CSV quote" }
    if (value.isNotEmpty() || cells.isNotEmpty() || closed) {
        field()
        records.add(cells.toList())
    }
    return records
}
