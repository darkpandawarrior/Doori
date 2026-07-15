package com.mileway

import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.display.OdometerCaptureResult
import com.mileway.core.data.model.display.OdometerReadingSource
import com.mileway.core.data.model.display.OdometerPurpose
import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.PolicyViolation
import com.mileway.core.data.model.network.ViolationSeverity
import com.mileway.core.data.model.network.SubmissionStatus
import com.mileway.core.data.model.network.SubmitMilesRequestK
import com.mileway.core.data.model.state.TrackMilesPluginConfig
import com.mileway.core.network.api.MilewayNetworkApi
import com.mileway.core.network.config.ConfigProvider
import com.mileway.feature.tracking.manager.TrackingConfigManager
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.repository.TripAttachmentRepository
import com.mileway.feature.tracking.viewmodel.MileageSubmissionAction
import com.mileway.feature.tracking.viewmodel.MileageSubmissionViewModel
import com.mileway.feature.tracking.viewmodel.SubmissionSheet
import com.mileway.feature.tracking.viewmodel.SubmissionUiState
import com.mileway.stub.DemoConfigManager
import com.mileway.stub.FakeTrackingNetworkApi
import com.siddharth.kmp.appshell.NotificationScheduler
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [MileageSubmissionViewModel] covering the submission state machine,
 * form intent reducers, and odometer capture flow.
 *
 * Uses the same fake-at-boundary strategy as [TrackMilesViewModelTest]:
 * - [FakeTrackingNetworkApi] and [DemoConfigManager] are the real offline stubs the app ships.
 * - [SavedTrackRepository] is backed by a fresh [FakeSavedTrackDao] (from TrackMilesViewModelTest).
 * - [TripAttachmentRepository] is a mockk(relaxed), persisting attachments is fire-and-forget.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MileageSubmissionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = FakeSavedTrackDao()
    private val trackRepo = SavedTrackRepository(dao)
    private val attachmentRepo: TripAttachmentRepository = mockk(relaxed = true)
    private val configManager = TrackingConfigManager(DemoConfigManager())

    private val noOpNotifier = object : NotificationScheduler {
        override suspend fun ensurePermission(): Boolean = true
        override fun notify(id: Int, title: String, body: String) = Unit
        override fun cancel(id: Int) = Unit
    }

    private fun viewModel(
        api: MilewayNetworkApi = FakeTrackingNetworkApi(),
        configManager: TrackingConfigManager = this.configManager,
    ) = MileageSubmissionViewModel(
        api = api,
        trackRepository = trackRepo,
        attachmentRepository = attachmentRepo,
        configManager = configManager,
        notificationScheduler = noOpNotifier,
    )

    // ── Initial form state ────────────────────────────────────────────────────

    @Test
    fun `init loads offices and entities from config`() = runTest {
        val vm = viewModel()
        val form = vm.state.value.form
        assertTrue(form.offices.isNotEmpty(), "Expected offices from DemoConfigManager")
        assertTrue(form.entities.isNotEmpty(), "Expected entities from DemoConfigManager")
    }

    @Test
    fun `init populates two required form fields`() = runTest {
        val vm = viewModel()
        assertEquals(2, vm.state.value.form.fields.size)
        assertTrue(vm.state.value.form.fields.all { it.required })
    }

    @Test
    fun `canSubmit is false when required fields are empty`() = runTest {
        val vm = viewModel()
        assertTrue(!vm.state.value.form.canSubmit)
    }

    @Test
    fun `canSubmit is true when all required fields and office and entity are filled`() = runTest {
        val vm = viewModel()
        vm.onAction(MileageSubmissionAction.SetFormValue("purpose", "Client visit"))
        vm.onAction(MileageSubmissionAction.SetFormValue("gender", "Male"))
        val office = vm.state.value.form.offices.first()
        val entity = vm.state.value.form.entities.first()
        vm.onAction(MileageSubmissionAction.SelectOffice(office.code))
        vm.onAction(MileageSubmissionAction.SelectEntity(entity.name))
        assertTrue(vm.state.value.form.canSubmit)
    }

    // ── Form intent reducers ──────────────────────────────────────────────────

    @Test
    fun `SetFormValue stores value by field id`() = runTest {
        val vm = viewModel()
        vm.onAction(MileageSubmissionAction.SetFormValue("purpose", "Audit visit"))
        assertEquals("Audit visit", vm.state.value.form.values["purpose"])
    }

    @Test
    fun `ToggleDraft updates saveAsDraft in form`() = runTest {
        val vm = viewModel()
        vm.onAction(MileageSubmissionAction.ToggleDraft(true))
        assertTrue(vm.state.value.form.saveAsDraft)
        vm.onAction(MileageSubmissionAction.ToggleDraft(false))
        assertTrue(!vm.state.value.form.saveAsDraft)
    }

    @Test
    fun `SelectOffice stores matching office and closes picker`() = runTest {
        val vm = viewModel()
        val target = vm.state.value.form.offices.first()
        vm.onAction(MileageSubmissionAction.OpenOfficePicker)
        assertEquals(SubmissionSheet.OFFICE_PICKER, vm.state.value.form.sheet)

        vm.onAction(MileageSubmissionAction.SelectOffice(target.code))
        assertEquals(target.code, vm.state.value.form.selectedOffice?.code)
        assertEquals(SubmissionSheet.NONE, vm.state.value.form.sheet)
    }

    @Test
    fun `SelectEntity stores matching entity and closes picker`() = runTest {
        val vm = viewModel()
        val target = vm.state.value.form.entities.first()
        vm.onAction(MileageSubmissionAction.OpenEntityPicker)
        assertEquals(SubmissionSheet.ENTITY_PICKER, vm.state.value.form.sheet)

        vm.onAction(MileageSubmissionAction.SelectEntity(target.name))
        assertEquals(target.name, vm.state.value.form.selectedEntity?.name)
        assertEquals(SubmissionSheet.NONE, vm.state.value.form.sheet)
    }

    @Test
    fun `DismissSheet resets active sheet to NONE`() = runTest {
        val vm = viewModel()
        vm.onAction(MileageSubmissionAction.OpenSubmitConfirm)
        assertEquals(SubmissionSheet.SUBMIT_CONFIRM, vm.state.value.form.sheet)
        vm.onAction(MileageSubmissionAction.DismissSheet)
        assertEquals(SubmissionSheet.NONE, vm.state.value.form.sheet)
    }

    // ── Odometer capture ──────────────────────────────────────────────────────

    @Test
    fun `CaptureOdometerStart stores reading and image uri`() = runTest {
        val vm = viewModel()
        vm.onAction(MileageSubmissionAction.CaptureOdometerStart(
            OdometerCaptureResult(
                purpose = OdometerPurpose.START,
                imageUri = "content://start.jpg",
                reading = 45_000,
                source = OdometerReadingSource.DEVICE_OCR,
                captureTimeMs = 1_000L,
            )
        ))

        val form = vm.state.value.form
        assertEquals(45_000, form.simulatedStartOdo)
        assertEquals("content://start.jpg", form.odometerStartImageUri)
        assertEquals(1_000L, form.odometerStartCaptureMs)
        assertTrue(!form.isManualStartOdo)
    }

    @Test
    fun `D3 - a MANUAL-source capture flags the reading as manual`() = runTest {
        val vm = viewModel()
        vm.onAction(
            MileageSubmissionAction.CaptureOdometerStart(
                OdometerCaptureResult(
                    purpose = OdometerPurpose.START,
                    imageUri = "",
                    reading = 45_000,
                    source = OdometerReadingSource.MANUAL,
                    captureTimeMs = 0L,
                ),
            ),
        )
        assertTrue(vm.state.value.form.isManualStartOdo)
    }

    @Test
    fun `CaptureOdometerEnd computes odometerKm from start reading`() = runTest {
        val vm = viewModel()
        vm.onAction(MileageSubmissionAction.CaptureOdometerStart(
            OdometerCaptureResult(OdometerPurpose.START, "uri://start", reading = 50_000, source = OdometerReadingSource.DEVICE_OCR, captureTimeMs = 0L)
        ))
        vm.onAction(MileageSubmissionAction.CaptureOdometerEnd(
            OdometerCaptureResult(OdometerPurpose.END, "uri://end", reading = 50_020, source = OdometerReadingSource.DEVICE_OCR, captureTimeMs = 1_000L)
        ))

        val form = vm.state.value.form
        assertEquals(50_020, form.simulatedEndOdo)
        assertEquals(20.0, form.smartDistanceOdometerKm, 1e-9)
    }

    // ── Submission state machine ───────────────────────────────────────────────

    @Test
    fun `submit success path emits Success state`() = runTest {
        val vm = viewModel()
        vm.onAction(MileageSubmissionAction.SetFormValue("purpose", "Field visit"))
        vm.onAction(MileageSubmissionAction.SetFormValue("gender", "Female"))
        vm.onAction(MileageSubmissionAction.SelectOffice(vm.state.value.form.offices.first().code))
        vm.onAction(MileageSubmissionAction.SelectEntity(vm.state.value.form.entities.first().name))

        vm.onAction(MileageSubmissionAction.Submit("route-001", distanceKm = 3.0, vehicleKey = "fourWheelerPetrol", startTime = 0L, endTime = 1L))
        advanceUntilIdle()

        val submissionState = vm.state.value.submissionState
        assertTrue(submissionState is SubmissionUiState.Success)
        assertNotNull((submissionState as SubmissionUiState.Success).response.transId)
    }

    @Test
    fun `submit policy violation path parks response and shows violation sheet`() = runTest {
        val violatingApi = object : MilewayNetworkApi by FakeTrackingNetworkApi() {
            override suspend fun submitMiles(request: SubmitMilesRequestK) =
                ExpenseSubmissionResponse(
                    transId = null,
                    submissionStatus = SubmissionStatus.POLICY_VIOLATION,
                    reimbursableAmount = 0.0,
                    violations = listOf(PolicyViolation(id = "DISTANCE_LIMIT", title = "Distance Limit", message = "Exceeds 500 km cap", severity = ViolationSeverity.VIOLATION))
                )
        }

        val vm = viewModel(api = violatingApi)
        vm.onAction(MileageSubmissionAction.Submit("route-002", distanceKm = 600.0, vehicleKey = "fourWheelerPetrol", startTime = 0L, endTime = 1L))
        advanceUntilIdle()

        assertEquals(SubmissionSheet.POLICY_VIOLATION, vm.state.value.form.sheet)
        assertTrue(vm.state.value.form.violations.isNotEmpty())
        assertEquals(SubmissionUiState.Idle, vm.state.value.submissionState)
    }

    @Test
    fun `ResolvePolicyAndFinalize transitions to Success after violation acknowledgement`() = runTest {
        val violatingApi = object : MilewayNetworkApi by FakeTrackingNetworkApi() {
            override suspend fun submitMiles(request: SubmitMilesRequestK) =
                ExpenseSubmissionResponse(
                    transId = "DEMO-VIOLATION",
                    submissionStatus = SubmissionStatus.POLICY_VIOLATION,
                    reimbursableAmount = 100.0,
                    violations = listOf(PolicyViolation(id = "DISTANCE_LIMIT", title = "Distance Limit", message = "Exceeds cap", severity = ViolationSeverity.VIOLATION))
                )
        }

        val vm = viewModel(api = violatingApi)
        vm.onAction(MileageSubmissionAction.Submit("route-003", 600.0, "fourWheelerPetrol", 0L, 1L))
        advanceUntilIdle()

        vm.onAction(MileageSubmissionAction.SetAskAuthorities(true))
        vm.onAction(MileageSubmissionAction.SetViolationNote("Approved by manager"))
        vm.onAction(MileageSubmissionAction.ResolvePolicyAndFinalize)
        advanceUntilIdle()

        assertTrue(vm.state.value.submissionState is SubmissionUiState.Success)
    }

    @Test
    fun `AddReceipt and RemoveReceipt update pendingReceipts in state`() = runTest {
        val vm = viewModel()
        vm.onAction(MileageSubmissionAction.AddReceipt("uri://receipt1"))
        vm.onAction(MileageSubmissionAction.AddReceipt("uri://receipt2"))
        assertEquals(2, vm.state.value.pendingReceipts.size)

        vm.onAction(MileageSubmissionAction.RemoveReceipt("uri://receipt1"))
        assertEquals(1, vm.state.value.pendingReceipts.size)
        assertEquals("uri://receipt2", vm.state.value.pendingReceipts.first())
    }

    @Test
    fun `Reset clears pending attachments and submission state`() = runTest {
        val vm = viewModel()
        vm.onAction(MileageSubmissionAction.AddReceipt("uri://r1"))
        vm.onAction(MileageSubmissionAction.SetOdometerStart("uri://odo", "45000"))
        vm.onAction(MileageSubmissionAction.Submit("route-004", 10.0, "fourWheelerPetrol", 0L, 1L))
        advanceUntilIdle()

        vm.onAction(MileageSubmissionAction.Reset)
        assertEquals(SubmissionUiState.Idle, vm.state.value.submissionState)
        assertTrue(vm.state.value.pendingReceipts.isEmpty())
        assertNull(vm.state.value.pendingOdoStart)
    }

    // ── P6.1: odometer-not-working fallback ────────────────────────────────────

    /** [ConfigProvider] whose track-miles config is overridable per test, everything else delegates to [DemoConfigManager]. */
    private class FakeOdometerConfigProvider(
        private val isOdometerMandatory: Boolean,
        private val calculateExpenseViaOdometer: Boolean,
    ) : ConfigProvider by DemoConfigManager() {
        override fun getTrackMilesConfig(): TrackMilesPluginConfig =
            TrackMilesPluginConfig(
                isOdometerMandatory = isOdometerMandatory,
                calculateExpenseViaOdometer = calculateExpenseViaOdometer,
            )
    }

    private fun fillRequiredFields(vm: MileageSubmissionViewModel) {
        vm.onAction(MileageSubmissionAction.SetFormValue("purpose", "Client visit"))
        vm.onAction(MileageSubmissionAction.SetFormValue("gender", "Male"))
        vm.onAction(MileageSubmissionAction.SelectOffice(vm.state.value.form.offices.first().code))
        vm.onAction(MileageSubmissionAction.SelectEntity(vm.state.value.form.entities.first().name))
    }

    @Test
    fun `canSubmit is false when odometer is mandatory and not captured, fallback off`() = runTest {
        val vm = viewModel(
            configManager = TrackingConfigManager(FakeOdometerConfigProvider(isOdometerMandatory = true, calculateExpenseViaOdometer = false)),
        )
        fillRequiredFields(vm)
        assertTrue(!vm.state.value.form.canSubmit)
        assertTrue(vm.state.value.form.remainingRequirements.contains("Odometer reading"))
    }

    @Test
    fun `SetOdometerNotWorking unblocks canSubmit when fallback config is enabled`() = runTest {
        val vm = viewModel(
            configManager = TrackingConfigManager(FakeOdometerConfigProvider(isOdometerMandatory = true, calculateExpenseViaOdometer = true)),
        )
        fillRequiredFields(vm)
        assertTrue(!vm.state.value.form.canSubmit)

        vm.onAction(MileageSubmissionAction.SetOdometerNotWorking(true))
        assertTrue(vm.state.value.form.canSubmit)
    }

    @Test
    fun `submit with odometer-not-working fallback carries GPS distance and audit remark`() = runTest {
        var capturedRequest: SubmitMilesRequestK? = null
        val capturingApi = object : MilewayNetworkApi by FakeTrackingNetworkApi() {
            override suspend fun submitMiles(request: SubmitMilesRequestK): ExpenseSubmissionResponse {
                capturedRequest = request
                return ExpenseSubmissionResponse(transId = "DEMO-ODO-FALLBACK", submissionStatus = SubmissionStatus.SUCCESS, reimbursableAmount = 42.0)
            }
        }

        val vm = viewModel(
            api = capturingApi,
            configManager = TrackingConfigManager(FakeOdometerConfigProvider(isOdometerMandatory = true, calculateExpenseViaOdometer = true)),
        )
        fillRequiredFields(vm)
        vm.onAction(MileageSubmissionAction.SetOdometerNotWorking(true))
        assertTrue(vm.state.value.form.canSubmit)

        vm.onAction(MileageSubmissionAction.Submit("route-odo-fallback", distanceKm = 12.5, vehicleKey = "fourWheelerPetrol", startTime = 0L, endTime = 1L))
        advanceUntilIdle()

        val request = capturedRequest
        assertNotNull(request)
        assertEquals(12.5, request.distance, 1e-9)
        assertTrue(request.odometerNotWorking)
        // PLAN_V33 C5: the audit marker moved from `notes` to `violationRemarks` (parity with the
        // reference builder) — notes is no longer used for this.
        assertEquals("ODOMETER_NOT_WORKING", request.violationRemarks)
        assertNull(request.notes)
        assertTrue(vm.state.value.submissionState is SubmissionUiState.Success)
    }

    // ── P6.2: round-trip auto-detection ────────────────────────────────────────

    private fun track(
        routeId: String,
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
    ) = SavedTrack(
        routeId = routeId,
        name = "Test journey",
        startLatitude = startLat, startLongitude = startLng,
        endLatitude = endLat, endLongitude = endLng,
        pausedLatitude = 0.0, pausedLongitude = 0.0,
        startTime = 0L, endTime = -1L,
        distance = 0.0, duration = 0L,
        selectedVehicleType = "fourWheelerPetrol",
        vehiclePricing = 10.0,
        createdAt = 0L, startedAtTimestamp = 0L,
        startedByEmployeeCode = "EMP001",
    )

    @Test
    fun `LoadTrackInfo marks form roundTrip true when start and end are close after a long trip`() = runTest {
        dao.preload(track("route-rt-001", 18.5204, 73.8567, 18.5204, 73.8567))
        val vm = viewModel()

        vm.onAction(MileageSubmissionAction.LoadTrackInfo("route-rt-001", "fourWheelerPetrol", distanceKm = 25.0))
        advanceUntilIdle()

        assertTrue(vm.state.value.form.roundTrip, "Same start/end after a long tracked trip should be a round trip")
    }

    @Test
    fun `LoadTrackInfo leaves form roundTrip false when start and end are far apart`() = runTest {
        dao.preload(track("route-rt-002", 18.5204, 73.8567, 18.6204, 73.8567))
        val vm = viewModel()

        vm.onAction(MileageSubmissionAction.LoadTrackInfo("route-rt-002", "fourWheelerPetrol", distanceKm = 25.0))
        advanceUntilIdle()

        assertTrue(!vm.state.value.form.roundTrip)
    }

    @Test
    fun `submit threads roundTrip into SubmitMilesRequestK`() = runTest {
        dao.preload(track("route-rt-003", 18.5204, 73.8567, 18.5204, 73.8567))
        var capturedRequest: SubmitMilesRequestK? = null
        val capturingApi = object : MilewayNetworkApi by FakeTrackingNetworkApi() {
            override suspend fun submitMiles(request: SubmitMilesRequestK): ExpenseSubmissionResponse {
                capturedRequest = request
                return ExpenseSubmissionResponse(transId = "DEMO-ROUND-TRIP", submissionStatus = SubmissionStatus.SUCCESS, reimbursableAmount = 10.0)
            }
        }

        val vm = viewModel(api = capturingApi)
        fillRequiredFields(vm)
        vm.onAction(MileageSubmissionAction.LoadTrackInfo("route-rt-003", "fourWheelerPetrol", distanceKm = 25.0))
        advanceUntilIdle()

        vm.onAction(MileageSubmissionAction.Submit("route-rt-003", distanceKm = 25.0, vehicleKey = "fourWheelerPetrol", startTime = 0L, endTime = 1L))
        advanceUntilIdle()

        val request = capturedRequest
        assertNotNull(request)
        assertTrue(request.roundTrip)
    }
}
