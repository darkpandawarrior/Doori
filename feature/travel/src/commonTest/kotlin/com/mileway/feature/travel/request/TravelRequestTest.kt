package com.mileway.feature.travel.request

import com.mileway.core.data.claim.FINANCE_ROLE
import com.mileway.core.data.claim.MANAGER_ROLE
import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.ReportLifecycleEvent
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.domain.claim.formatMinorCurrency
import com.mileway.core.data.domain.notify.ReportLifecycleNotification
import com.mileway.core.data.domain.policy.rates.IrsMileageRates
import com.mileway.core.data.domain.travel.TravelRequest
import com.mileway.core.data.domain.travel.estimateTravel
import com.mileway.core.network.routing.OsrmClient
import com.mileway.core.network.routing.OsrmConfiguration
import com.mileway.core.network.routing.RouteEstimate
import com.siddharth.kmp.network.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TravelRequestTest {
    @BeforeTest fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun fullyApprovedRequestUsesManagerFinanceChainWithoutClaimOrPayout() =
        runTest {
            val notifications = mutableListOf<ReportLifecycleNotification>()
            val store = TravelRequestStore({ notifications += it }, { 1 })
            val submitted = store.submit(draft())
            assertEquals(MANAGER_ROLE, store.nextRole(submitted))
            val manager = store.act(submitted.id, submitted.recordVersion, "jordan", MANAGER_ROLE, ApprovalAction.APPROVE, "Business trip reviewed")
            assertEquals(ReportLifecycleState.SUBMITTED, manager.state)
            assertEquals(FINANCE_ROLE, store.nextRole(manager))
            assertFailsWith<IllegalArgumentException> {
                store.act(
                    manager.id,
                    submitted.recordVersion,
                    "finance",
                    FINANCE_ROLE,
                    ApprovalAction.APPROVE,
                    "Reviewed",
                )
            }
            val approved = store.act(manager.id, manager.recordVersion, "finance", FINANCE_ROLE, ApprovalAction.APPROVE, "Estimate authorized")
            assertEquals(ReportLifecycleState.APPROVED, approved.state)
            assertEquals(listOf(MANAGER_ROLE, FINANCE_ROLE), approved.approvalChain.steps.map { it.role })
            assertTrue(approved.lifecycleReport().lines.isEmpty())
            assertTrue(notifications.all { it.type == "APPROVAL" && it.deeplink.isEmpty() })
            assertEquals(3, notifications.map { it.id }.distinct().size)
            assertTrue(notifications.last().body.contains(formatMinorCurrency(approved.estimate.amountMinor, approved.estimate.currency)))
            assertFailsWith<IllegalArgumentException> { store.transition(approved.id, approved.recordVersion, ReportLifecycleEvent.RELEASE_FOR_PAYMENT) }
            assertFailsWith<IllegalArgumentException> { store.transition(approved.id, approved.recordVersion, ReportLifecycleEvent.REIMBURSE) }
            assertEquals(listOf(approved), store.requests.value)
        }

    @Test
    fun reviewRejectsSelfApprovalWrongRoleAndEmptyCommentsAndKeepsHistoryOnSendBack() =
        runTest {
            val store = TravelRequestStore({})
            val request = store.submit(draft())
            assertFailsWith<IllegalArgumentException> { store.act(request.id, request.recordVersion, "alex", MANAGER_ROLE, ApprovalAction.APPROVE, "Reviewed") }
            assertFailsWith<IllegalArgumentException> {
                store.act(
                    request.id,
                    request.recordVersion,
                    "jordan",
                    FINANCE_ROLE,
                    ApprovalAction.APPROVE,
                    "Reviewed",
                )
            }
            assertFailsWith<IllegalArgumentException> { store.act(request.id, request.recordVersion, "jordan", MANAGER_ROLE, ApprovalAction.APPROVE, " ") }
            val sentBack = store.act(request.id, request.recordVersion, "jordan", MANAGER_ROLE, ApprovalAction.SEND_BACK, "Confirm scope")
            assertEquals(ReportLifecycleState.SENT_BACK, sentBack.state)
            val resubmitted = store.transition(sentBack.id, sentBack.recordVersion, ReportLifecycleEvent.RESUBMIT)
            assertEquals(sentBack.approvalChain, resubmitted.approvalChain)
            assertFailsWith<IllegalArgumentException> { store.transition(resubmitted.id, resubmitted.recordVersion, ReportLifecycleEvent.RECALL) }
            val rejected = store.act(resubmitted.id, resubmitted.recordVersion, "jordan", MANAGER_ROLE, ApprovalAction.REJECT, "Outside scope")
            assertEquals(ReportLifecycleState.REJECTED, rejected.state)
        }

    @Test
    fun failedNotificationDoesNotAdvanceRequestAndRecallIsReplaySafe() =
        runTest {
            var fail = true
            val notifications = mutableListOf<ReportLifecycleNotification>()
            val store = TravelRequestStore({ if (fail) error("Inbox unavailable") else notifications += it })
            assertFailsWith<IllegalStateException> { store.submit(draft()) }
            assertTrue(store.requests.value.isEmpty())
            fail = false
            val submitted = store.submit(draft())
            val recalled = store.transition(submitted.id, submitted.recordVersion, ReportLifecycleEvent.RECALL)
            assertEquals(ReportLifecycleState.RECALLED, recalled.state)
            assertFailsWith<IllegalArgumentException> { store.transition(submitted.id, submitted.recordVersion, ReportLifecycleEvent.RECALL) }
            assertEquals(2, notifications.size)
        }

    @Test
    fun osrmRouteAheadUsesTravelDateRateAndActualRoundTripDistance() =
        runTest {
            val http =
                createHttpClient(
                    engine =
                        MockEngine { request ->
                            assertEquals("/route/v1/driving/34.0,12.0;35.0,13.0;34.0,12.0", request.url.encodedPath)
                            respond(
                                """{"code":"Ok","routes":[{"distance":16093.44}]}""",
                                HttpStatusCode.OK,
                                headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        },
                    retry = false,
                )
            try {
                val vm =
                    TravelRequestViewModel(TravelRequestStore({}), flowOf("alex"), route = {
                        a,
                        b,
                        round,
                        server,
                        ->
                        OsrmClient(OsrmConfiguration(server)) { http }.route(a, b, round)
                    })
                advanceUntilIdle()
                fill(vm)
                vm.onAction(TravelRequestAction.Edit(TravelRequestField.SERVER, "http://route.test"))
                vm.onAction(TravelRequestAction.RoundTrip(true))
                vm.onAction(TravelRequestAction.Estimate)
                val completed = vm.state.first { !it.busy }
                val quote = assertNotNull(completed.estimate, completed.message ?: completed.manualReason)
                assertEquals(760L, quote.amountMinor)
                assertEquals("2026-07-01", quote.rateEffectiveFrom)
                assertFalse(quote.approximate)
                vm.onAction(TravelRequestAction.Submit)
                advanceUntilIdle()
                assertEquals(
                    quote,
                    vm.state.value.selected
                        ?.estimate,
                )
            } finally {
                http.close()
            }
        }

    @Test
    fun homeNeverCreatesHttpClientAndUnconfiguredOfflineRequireApproximateManualDistance() =
        runTest {
            val protected = OsrmClient(OsrmConfiguration("http://route.test")) { error("Home must not create HTTP client") }
            val vm = TravelRequestViewModel(TravelRequestStore({}), flowOf("alex"), route = { a, b, round, _ -> protected.route(a, b, round) })
            advanceUntilIdle()
            fill(vm)
            vm.onAction(TravelRequestAction.Home(true, true))
            vm.onAction(TravelRequestAction.Estimate)
            advanceUntilIdle()
            assertNull(vm.state.value.estimate)
            assertTrue(assertNotNull(vm.state.value.manualReason).contains("Home"))
            vm.onAction(TravelRequestAction.Edit(TravelRequestField.MANUAL_DISTANCE, "16.09344"))
            vm.onAction(TravelRequestAction.Manual)
            advanceUntilIdle()
            assertTrue(assertNotNull(vm.state.value.estimate).approximate)
            val unconfigured = TravelRequestViewModel(TravelRequestStore({}), flowOf("alex"))
            advanceUntilIdle()
            fill(unconfigured)
            unconfigured.onAction(TravelRequestAction.Estimate)
            advanceUntilIdle()
            assertTrue(assertNotNull(unconfigured.state.value.manualReason).contains("unconfigured"))
            assertFalse(unconfigured.state.value.canSubmit)
            val http = createHttpClient(engine = MockEngine { error("Offline") }, retry = false)
            try {
                val offline = OsrmClient(OsrmConfiguration("http://route.test")) { http }
                val result =
                    offline.route(
                        com.mileway.core.network.routing
                            .RoutePoint(12.0, 34.0),
                        com.mileway.core.network.routing
                            .RoutePoint(13.0, 35.0),
                    )
                assertTrue(result is RouteEstimate.ManualRequired)
            } finally {
                http.close()
            }
        }

    @Test
    fun editingAndAccountChangesCancelQuotesAndSeparateSessionRequests() =
        runTest {
            val accounts = MutableStateFlow<String?>("alex")
            val store = TravelRequestStore({})
            store.submit(draft())
            val vm = TravelRequestViewModel(store, accounts, route = { _, _, _, _ -> awaitCancellation() })
            advanceUntilIdle()
            assertEquals(1, vm.state.value.requests.size)
            fill(vm)
            vm.onAction(TravelRequestAction.Estimate)
            advanceUntilIdle()
            assertTrue(vm.state.value.busy)
            vm.onAction(TravelRequestAction.Edit(TravelRequestField.DATE, "2026-06-30"))
            advanceUntilIdle()
            assertFalse(vm.state.value.busy)
            assertNull(vm.state.value.estimate)
            accounts.value = "jordan"
            advanceUntilIdle()
            assertTrue(
                vm.state.value.requests
                    .isEmpty(),
            )
            assertEquals("", vm.state.value.field(TravelRequestField.DATE))
            assertFalse(vm.state.value.canSubmit)
        }

    private fun fill(vm: TravelRequestViewModel) {
        mapOf(
            TravelRequestField.PURPOSE to "Client visit",
            TravelRequestField.DATE to "2026-07-01",
            TravelRequestField.ORIGIN_LAT to "12.0",
            TravelRequestField.ORIGIN_LON to "34.0",
            TravelRequestField.DESTINATION_LAT to "13.0",
            TravelRequestField.DESTINATION_LON to "35.0",
        ).forEach { (key, value) ->
            vm.onAction(TravelRequestAction.Edit(key, value))
        }
    }

    private fun draft() = TravelRequest("travel-1", "alex", "Client visit", "2026-07-01", estimateTravel(16.09344, false, "2026-07-01", IrsMileageRates.mirror))
}
