package com.mileway.feature.logging.justification

import com.mileway.core.data.domain.claim.JustificationReason
import com.siddharth.kmp.llmchat.ProviderId
import com.siddharth.kmp.result.AiFailure
import com.siddharth.kmp.result.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class JustificationReasonSuggesterTest {
    @Test
    fun noKeySkipsTheProviderEntirely() = runTest {
        var calls = 0
        val suggester = JustificationReasonSuggester({ null }) { calls++; error("Must stay offline") }
        assertNull(suggester.suggest("FOOD", 100, listOf("RECEIPT_RECOMMENDED")))
        assertEquals(0, calls)
    }

    @Test
    fun fakeProviderCodeMapsExactlyAndPromptContainsOnlyAllowedContext() = runTest {
        var prompt = ""
        val suggester = JustificationReasonSuggester({ if (it == ProviderId.OPENAI) "fake-key" else null }) {
            prompt = it
            Result.Success("client_request")
        }
        assertEquals(JustificationReason.CLIENT_REQUEST, suggester.suggest("FOOD", 1234, listOf("RECEIPT_RECOMMENDED")))
        assertEquals(
            "Suggest one expense exception code: business_necessity, client_request, no_alternative, other. " +
                "Return only the exact code. Category=FOOD; amountMinor=1234; policyFlags=RECEIPT_RECOMMENDED.",
            prompt,
        )
        assertFalse(prompt.contains("fake-key"))
    }

    @Test
    fun failuresAndGarbageAreSilentWithoutRetry() = runTest {
        val answers = listOf(
            Result.Failure(AiFailure.NoKey),
            Result.Success("garbage"),
            Result.Success("CLIENT_REQUEST"),
            Result.Success(" client_request"),
            Result.Success("client_request\n"),
        )
        for (answer in answers) {
            var calls = 0
            val suggester = JustificationReasonSuggester({ "fake-key" }) { calls++; answer }
            assertNull(suggester.suggest("FOOD", 100, emptyList()))
            assertEquals(1, calls)
        }
        assertNull(JustificationReasonSuggester({ "fake-key" }) { error("Network down") }.suggest("FOOD", 100, emptyList()))
        assertNull(JustificationReasonSuggester({ "fake-key" }) { delay(9_000); Result.Success("other") }.suggest("FOOD", 100, emptyList()))
        assertFailsWith<CancellationException> {
            JustificationReasonSuggester({ "fake-key" }) { throw CancellationException() }.suggest("FOOD", 100, emptyList())
        }
    }
}
