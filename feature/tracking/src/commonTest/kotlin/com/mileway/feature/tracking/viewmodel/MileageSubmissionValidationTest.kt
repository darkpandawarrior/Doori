package com.mileway.feature.tracking.viewmodel

import com.mileway.core.data.dao.TripAttachmentDao
import com.mileway.core.data.model.db.AttachmentType
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.TripAttachmentEntity
import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.SubmissionStatus
import com.mileway.core.data.model.network.SubmitMilesRequestK
import com.mileway.core.data.model.state.LogMilesPluginConfig
import com.mileway.core.data.model.state.TrackMilesPluginConfig
import com.mileway.core.network.api.MilewayNetworkApi
import com.mileway.core.network.config.ConfigProvider
import com.mileway.feature.tracking.manager.TrackingConfigManager
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.repository.TripAttachmentRepository
import com.siddharth.kmp.appshell.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

// ── Minimal stub deps ──────────────────────────────────────────
// None of these are exercised by the assertions below (submission is either blocked before the
// network call, or the network call is a one-line success stub) — every method that isn't used
// is a plain no-op/error("unused"), same idiom as FakeNetworkApi above.

private object FakeSubmissionConfigProvider : ConfigProvider {
    override fun getTrackMilesConfig(): TrackMilesPluginConfig = TrackMilesPluginConfig()

    override fun getLogMilesConfig(): LogMilesPluginConfig = LogMilesPluginConfig()

    override fun isMilesEnabled(): Boolean = true

    override fun isLogMilesEnabled(): Boolean = true

    override fun getCurrency(): String = "USD"
}

private class NoOpTripAttachmentDao : TripAttachmentDao {
    override suspend fun insert(attachment: TripAttachmentEntity): Long = 0L

    override fun observeForTrack(trackToken: String): Flow<List<TripAttachmentEntity>> = MutableStateFlow(emptyList())

    override suspend fun getForTrack(trackToken: String): List<TripAttachmentEntity> = emptyList()

    override suspend fun getLatestOfType(
        trackToken: String,
        type: AttachmentType,
    ): TripAttachmentEntity? = null

    override fun observeByType(
        trackToken: String,
        type: AttachmentType,
    ): Flow<List<TripAttachmentEntity>> = MutableStateFlow(emptyList())

    override suspend fun delete(id: Long) = Unit

    override suspend fun deleteForTrack(trackToken: String) = Unit

    override suspend fun countForTrack(trackToken: String): Int = 0
}

private object NoOpNotificationScheduler : NotificationScheduler {
    override suspend fun ensurePermission(): Boolean = true

    override fun notify(
        id: Int,
        title: String,
        body: String,
    ) = Unit

    override fun cancel(id: Int) = Unit
}

/** Always succeeds — only used to prove a non-blocked submission reaches the network call. */
private class SucceedingNetworkApi : MilewayNetworkApi by FakeNetworkApi() {
    override suspend fun submitMiles(request: SubmitMilesRequestK): ExpenseSubmissionResponse =
        ExpenseSubmissionResponse(transId = "DEMO-OK", submissionStatus = SubmissionStatus.SUCCESS, reimbursableAmount = 10.0)
}

/**
 * `JourneyValidator` wiring: [MileageSubmissionViewModel.submit] must gate on
 * [com.mileway.core.data.model.validator.JourneyValidator.validateBeforeSubmission] — an
 * ERROR-severity finding blocks the submission (surfaced via the existing
 * [SubmissionUiState.Error] channel), a WARNING-severity finding does not.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MileageSubmissionValidationTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun track(
        routeId: String,
        serverUploaded: Boolean = false,
        distance: Double = 3_000.0,
    ) = SavedTrack(
        routeId = routeId,
        name = "Trip",
        serverUploaded = serverUploaded,
        startTime = 1_700_000_000_000L,
        endTime = 1_700_000_060_000L,
        startLatitude = 18.52,
        startLongitude = 73.85,
        endLatitude = 18.52,
        endLongitude = 73.85,
        pausedLatitude = 0.0,
        pausedLongitude = 0.0,
        distance = distance,
        duration = 60_000L,
        selectedVehicleType = "fourWheelerPetrol",
    )

    private fun viewModel(seed: List<SavedTrack>) =
        MileageSubmissionViewModel(
            api = SucceedingNetworkApi(),
            trackRepository = SavedTrackRepository(FakeSavedTrackDao(seed)),
            attachmentRepository = TripAttachmentRepository(NoOpTripAttachmentDao()),
            configManager = TrackingConfigManager(FakeSubmissionConfigProvider),
            notificationScheduler = NoOpNotificationScheduler,
        )

    private fun submit(
        vm: MileageSubmissionViewModel,
        routeId: String,
    ) = vm.onAction(
        MileageSubmissionAction.Submit(routeId, distanceKm = 3.0, vehicleKey = "fourWheelerPetrol", startTime = 0L, endTime = 1L),
    )

    @Test
    fun `a clean submission passes through to Success`() {
        val vm = viewModel(seed = listOf(track("route-clean")))

        submit(vm, "route-clean")

        assertTrue(vm.state.value.submissionState is SubmissionUiState.Success)
    }

    @Test
    fun `an ERROR finding blocks submission via the existing Error channel`() {
        // serverUploaded = true -> JourneyErrorCode.ALREADY_SUBMITTED, JourneySeverity.ERROR.
        val vm = viewModel(seed = listOf(track("route-error", serverUploaded = true)))

        submit(vm, "route-error")

        val state = vm.state.value.submissionState
        assertTrue(state is SubmissionUiState.Error, "Expected submission to be blocked, was $state")
        assertTrue((state as SubmissionUiState.Error).message.contains("already submitted", ignoreCase = true))
    }

    @Test
    fun `a WARNING finding does not block submission`() {
        // Negative distance -> JourneyErrorCode.UNUSUAL_VALUES, JourneySeverity.WARNING only.
        val vm = viewModel(seed = listOf(track("route-warning", distance = -1.0)))

        submit(vm, "route-warning")

        assertTrue(vm.state.value.submissionState is SubmissionUiState.Success)
    }
}
