package com.mileway.feature.payments.viewmodel

import com.mileway.core.ai.model.AnalyzerSource
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DocType
import com.mileway.core.ai.model.DocumentAnalysis
import com.mileway.core.ai.model.DuplicateVerdict
import com.mileway.core.ai.model.ExtractedValue
import com.mileway.feature.payments.model.PaymentDirection
import com.mileway.feature.payments.model.PaymentStatus
import com.mileway.feature.payments.model.PaymentTransactionStatus
import com.mileway.feature.payments.repository.PaymentsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * P29.C.6/C.7: the IDLE/SUBMITTING/POLLING/SUCCESS/FAILED state machine, the PIN gate, and the
 * invoice-attach duplicate-detection wiring (PaymentsRepository.checkInvoiceDuplicate, backed by
 * core:ai's real DuplicateDetector/DuplicateVerdict — not a hand-rolled stub).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreatePaymentViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = CreatePaymentViewModel(PaymentsRepository())

    private fun fillForm(
        vm: CreatePaymentViewModel,
        amount: String = "100",
    ) {
        vm.onAction(CreatePaymentAction.SetCounterparty("chai@stall"))
        vm.onAction(CreatePaymentAction.SetAmount(amount))
    }

    private fun analysisWith(
        merchant: String,
        total: String,
    ) = DocumentAnalysis(
        docType = DocType.INVOICE,
        fields =
            mapOf(
                DocField.MERCHANT to ExtractedValue(merchant, 0.9f, AnalyzerSource.HEURISTIC_CLASSIFIER),
                DocField.TOTAL to ExtractedValue(total, 0.9f, AnalyzerSource.HEURISTIC_CLASSIFIER),
            ),
        rawText = "",
        // Deliberately Unique here: the ViewModel ignores this and re-checks against
        // PaymentsRepository's own history instead (see the comment in `attachInvoice`).
        duplicate = DuplicateVerdict.Unique,
        overallConfidence = 0.9f,
        contributingSources = setOf(AnalyzerSource.HEURISTIC_CLASSIFIER),
    )

    @Test
    fun `small amount submits straight through submitting and polling to a final state`() =
        runTest {
            val vm = newViewModel()
            fillForm(vm, amount = "100")

            vm.onAction(CreatePaymentAction.Submit)
            runCurrent()
            assertEquals(PaymentTransactionStatus.SUBMITTING, vm.state.value.transactionStatus)

            advanceUntilIdle()
            assertTrue(
                vm.state.value.transactionStatus == PaymentTransactionStatus.SUCCESS ||
                    vm.state.value.transactionStatus == PaymentTransactionStatus.FAILED,
            )
        }

    @Test
    fun `large amount is gated behind a PIN before the state machine runs`() =
        runTest {
            val vm = newViewModel()
            fillForm(vm, amount = "9999")

            vm.onAction(CreatePaymentAction.Submit)

            assertTrue(vm.state.value.awaitingPin)
            assertEquals(PaymentTransactionStatus.IDLE, vm.state.value.transactionStatus)

            vm.onAction(CreatePaymentAction.SetPinDigits("12"))
            vm.onAction(CreatePaymentAction.ConfirmPin)
            assertTrue(vm.state.value.pinError) // too short, still gated
            assertTrue(vm.state.value.awaitingPin)

            vm.onAction(CreatePaymentAction.SetPinDigits("1234"))
            vm.onAction(CreatePaymentAction.ConfirmPin)

            assertFalse(vm.state.value.awaitingPin)
            assertTrue(vm.state.value.pinVerified)
            advanceUntilIdle()
            assertTrue(vm.state.value.transactionStatus != PaymentTransactionStatus.IDLE)
        }

    @Test
    fun `unique invoice attaches immediately, no confirmation needed`() =
        runTest {
            val vm = newViewModel()
            fillForm(vm)
            vm.onAction(CreatePaymentAction.Submit)
            advanceUntilIdle()

            vm.onAction(CreatePaymentAction.AttachInvoice(analysisWith("some-cafe", "42.0"), "file://invoice.jpg"))

            assertEquals("file://invoice.jpg", vm.state.value.attachmentUrl)
            assertNull(vm.state.value.duplicatePrompt)
        }

    @Test
    fun `possible duplicate needs an explicit confirm before attaching`() =
        runTest {
            val vm = newViewModel()
            fillForm(vm)
            vm.onAction(CreatePaymentAction.Submit)
            advanceUntilIdle()

            // Matches PaymentsRepository's seeded "amazon" / "2499.0" invoice, attached one
            // minute ago — inside DuplicateDetector's 5-minute window.
            vm.onAction(CreatePaymentAction.AttachInvoice(analysisWith("amazon", "2499.0"), "file://invoice.jpg"))

            assertNotNull(vm.state.value.duplicatePrompt)
            assertNull(vm.state.value.attachmentUrl)

            vm.onAction(CreatePaymentAction.ConfirmDuplicateAttach)

            assertEquals("file://invoice.jpg", vm.state.value.attachmentUrl)
            assertNull(vm.state.value.duplicatePrompt)
        }

    @Test
    fun `cancelling a possible-duplicate prompt leaves nothing attached`() =
        runTest {
            val vm = newViewModel()
            fillForm(vm)
            vm.onAction(CreatePaymentAction.Submit)
            advanceUntilIdle()

            vm.onAction(CreatePaymentAction.AttachInvoice(analysisWith("amazon", "2499.0"), "file://invoice.jpg"))
            assertNotNull(vm.state.value.duplicatePrompt)

            vm.onAction(CreatePaymentAction.CancelDuplicateAttach)

            assertNull(vm.state.value.duplicatePrompt)
            assertNull(vm.state.value.attachmentUrl)
        }

    // P29.C.8: the declaration gate only applies to REQUEST (a QR "collect" ask), not PAY.
    @Test
    fun `REQUEST cannot submit until the declaration checkbox is accepted`() =
        runTest {
            val vm = newViewModel()
            vm.onAction(CreatePaymentAction.SetDirection(PaymentDirection.REQUEST))
            fillForm(vm)

            assertFalse(vm.state.value.canSubmit)

            vm.onAction(CreatePaymentAction.SetDeclarationAccepted(true))

            assertTrue(vm.state.value.canSubmit)
        }

    @Test
    fun `PAY direction never requires the declaration checkbox`() =
        runTest {
            val vm = newViewModel()
            fillForm(vm)
            assertFalse(vm.state.value.requiresDeclaration)
            assertTrue(vm.state.value.canSubmit)
        }

    // P29.C.8: QR advance/request lifecycle — ACTIVE flips to EXPIRED once its validity window
    // has elapsed unpaid, purely by re-reading (no mutation of the stored seed row).
    @Test
    fun `an ACTIVE request past its validity window reads as EXPIRED`() {
        val clock = Clock.System
        val repository = PaymentsRepository(clock)

        val expired = repository.payments(PaymentStatus.EXPIRED)
        assertTrue(expired.isNotEmpty())
        assertTrue(expired.all { it.direction == PaymentDirection.REQUEST })

        val active = repository.payments(PaymentStatus.ACTIVE)
        assertTrue(active.isNotEmpty())
        assertTrue(active.all { it.expiresAtMillis != null && it.expiresAtMillis!! > clock.now().toEpochMilliseconds() })
    }
}
