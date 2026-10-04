package com.mileway.core.network.fx

import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.FxRate
import com.mileway.core.data.domain.claim.FxRateSource
import com.siddharth.kmp.network.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FrankfurterFxClientTest {
    @Test
    fun ecbRoutePreservesTheReturnedBusinessDateAndPinsOnlyOnce() = runTest {
        var requests = 0
        val engine = MockEngine { request ->
            requests++
            assertEquals("api.frankfurter.dev", request.url.host)
            assertEquals("/v2/providers/ecb/rate/USD/INR", request.url.encodedPath)
            assertEquals("2026-09-27", request.url.parameters["date"])
            respond("""{"date":"2026-09-25","base":"USD","quote":"INR","rate":95.82}""")
        }
        val http = createHttpClient(engine = engine, retry = false)
        try {
            val client = FrankfurterFxClient(http)
            val pinner = FxRatePinner(reference = client::rate, nowMillis = { 123L })
            val pinned = pinner.pin(line(), "2026-09-27")
            assertEquals(95.82, pinned.fxRate?.rate)
            assertEquals("2026-09-25", pinned.fxRate?.sourceDate)
            assertEquals(123L, pinned.fxRatePinnedAt)
            assertEquals(1000L, pinned.amountMinor)
            assertEquals(pinned, pinner.pin(pinned, "2026-09-28"))
            assertEquals(1, requests)
        } finally { http.close() }
    }

    @Test
    fun cardRateWinsWithoutCallingReferenceAndManualFallbackIsApproximate() = runTest {
        var calls = 0
        val pinner = FxRatePinner(reference = { _, _, _ -> calls++; null }, nowMillis = { 42 })
        val card = FxRate(90.0, "USD", sourceDate = "2026-09-24", source = FxRateSource.CARD_MATCHED)
        val matched = pinner.pin(line().copy(cardMatchId = "match"), cardRate = card, manualRate = 80.0)
        assertEquals(card, matched.fxRate)
        assertEquals(0, calls)
        val manual = pinner.pin(line(), manualRate = 80.0)
        assertEquals(FxRateSource.MANUAL_APPROXIMATE, manual.fxRate?.source)
        assertNull(manual.fxRate?.sourceDate)
        assertEquals(42L, manual.fxRatePinnedAt)
        assertNull(pinner.pin(line(), manualRate = Double.NaN).fxRate)
        assertNull(pinner.pin(line(), manualRate = -1.0).fxRatePinnedAt)
    }

    @Test
    fun referenceWinsOverManualAndAnUnmatchedCardRateCannotWin() = runTest {
        val reference = FxRate(95.82, "USD", sourceDate = "2026-09-25")
        val pinner = FxRatePinner(reference = { _, _, _ -> reference })
        val card = reference.copy(rate = 80.0, source = FxRateSource.CARD_MATCHED)
        assertEquals(reference, pinner.pin(line(), cardRate = card, manualRate = 70.0).fxRate)
        assertEquals(reference, pinner.pin(line(), cardRate = card.copy(baseCurrency = "EUR"), manualRate = 70.0).fxRate)
    }

    @Test
    fun offlineAndInvalidResponsesReturnNoRateAndCancellationPropagates() = runTest {
        val bodies = listOf(
            "{}",
            """{"date":"bad","base":"USD","quote":"INR","rate":80}""",
            """{"date":"2026-09-28","base":"USD","quote":"INR","rate":80}""",
            """{"date":"2026-09-25","base":"EUR","quote":"INR","rate":80}""",
            """{"date":"2026-09-25","base":"USD","quote":"INR","rate":-80}""",
        )
        for (body in bodies) {
            val http = createHttpClient(engine = MockEngine { respond(body) }, retry = false)
            try { assertNull(FrankfurterFxClient(http).rate("USD", date = "2026-09-27")) } finally { http.close() }
        }
        val offline = createHttpClient(engine = MockEngine { throw IllegalStateException("offline") }, retry = false)
        try {
            val client = FrankfurterFxClient(offline)
            assertNull(client.rate("USD"))
            val manual = FxRatePinner(reference = client::rate).pin(line(), manualRate = 85.0)
            assertEquals(FxRateSource.MANUAL_APPROXIMATE, manual.fxRate?.source)
        } finally { offline.close() }
        val unavailable = createHttpClient(engine = MockEngine { respond("unavailable", HttpStatusCode.ServiceUnavailable) }, retry = false)
        try { assertNull(FrankfurterFxClient(unavailable).rate("USD")) } finally { unavailable.close() }
        val cancelled = createHttpClient(engine = MockEngine { throw CancellationException("cancel") }, retry = false)
        try { assertFailsWith<CancellationException> { FrankfurterFxClient(cancelled).rate("USD") } } finally { cancelled.close() }
    }

    private fun line() = ExpenseLine("foreign", 1000, "USD", merchant = "Cafe", category = "FOOD")
}
