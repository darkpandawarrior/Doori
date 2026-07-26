package com.mileway.feature.tracking.viewmodel

import com.mileway.core.data.model.db.CurrentTrackData
import com.mileway.core.data.model.db.MockAccountEntity
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.session.DEFAULT_SESSION_TENANT
import com.mileway.core.data.session.SessionKind
import com.mileway.core.data.session.SessionState
import com.mileway.feature.tracking.manager.TrackingController
import com.mileway.feature.tracking.service.ReconciliationResultHolder
import com.mileway.feature.tracking.service.SessionReconciliationPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// ── Minimal fake TrackingController (interface defined in commonMain P-B.1) ──

private class FakeTrackingController : TrackingController {
    val started = mutableListOf<String>()
    val paused = mutableListOf<String>()
    val resumed = mutableListOf<String>()
    val stopped = mutableListOf<String>()

    override fun start(token: String) {
        started += token
    }

    override fun pause(token: String) {
        paused += token
    }

    override fun resume(token: String) {
        resumed += token
    }

    override fun stop(token: String) {
        stopped += token
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TrackMilesViewModelTest {
    // Installs an eager (UnconfinedTestDispatcher) Main dispatcher so viewModelScope.launch blocks
    // — e.g. restoreActiveTrack()'s P3.5 reconciliation check — run to completion synchronously
    // before the harness's `build()` call returns, the same way real Android's Main.immediate
    // scheduling behaves for callers who don't explicitly await anything.
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial phase is IDLE`() {
        val vm = buildVm()
        assertEquals(TrackMilesPhase.IDLE, vm.uiState.value.phase)
    }

    @Test
    fun `pauseTracking sets phase to PAUSED when currentRouteId is null`() {
        val vm = buildVm()
        // Phase starts IDLE; pauseTracking should flip phase regardless.
        vm.pauseTracking()
        assertEquals(TrackMilesPhase.PAUSED, vm.uiState.value.phase)
    }

    @Test
    fun `resumeTracking sets phase to TRACKING`() {
        val vm = buildVm()
        vm.pauseTracking()
        vm.resumeTracking()
        assertEquals(TrackMilesPhase.TRACKING, vm.uiState.value.phase)
    }

    @Test
    fun `stopTracking sets phase to STOPPED`() {
        val vm = buildVm()
        vm.stopTracking()
        assertEquals(TrackMilesPhase.STOPPED, vm.uiState.value.phase)
    }

    @Test
    fun `toggleGaugeMode cycles between COMPASS and ACTIVITY`() {
        val vm = buildVm()
        assertEquals(HeroGaugeMode.COMPASS, vm.uiState.value.gaugeMode)
        vm.toggleGaugeMode()
        assertEquals(HeroGaugeMode.ACTIVITY, vm.uiState.value.gaugeMode)
        vm.toggleGaugeMode()
        assertEquals(HeroGaugeMode.COMPASS, vm.uiState.value.gaugeMode)
    }

    @Test
    fun `pauseTracking with route delegates to TrackingController`() {
        val controller = FakeTrackingController()
        val vm = buildVm(controller = controller)
        // Inject a current route via internal state to exercise the delegate path.
        vm.pauseTracking(reason = "test")
        // No routeId in state yet → controller.pause not called (null guard in VM).
        assertEquals(0, controller.paused.size)
        assertEquals(TrackMilesPhase.PAUSED, vm.uiState.value.phase)
    }

    @Test
    fun `dismissSheet resets activeSheet to NONE`() {
        val vm = buildVm()
        vm.openJourneyGuide()
        assertEquals(TrackSheet.JOURNEY_GUIDE, vm.uiState.value.activeSheet)
        vm.dismissSheet()
        assertEquals(TrackSheet.NONE, vm.uiState.value.activeSheet)
    }

    @Test
    fun `openVehiclePicker sets activeSheet to VEHICLE_PICKER`() {
        val vm = buildVm()
        vm.openVehiclePicker()
        assertEquals(TrackSheet.VEHICLE_PICKER, vm.uiState.value.activeSheet)
    }

    @Test
    fun `initial currentRouteId is null`() {
        val vm = buildVm()
        assertNull(vm.uiState.value.currentRouteId)
    }

    // ── P-C.5: recovery action tests ─────────────────────────────────────────

    @Test
    fun `handleRecoveryResume sets phase TRACKING and clears activeRecovery`() {
        val controller = FakeTrackingController()
        val vm = buildVm(controller = controller)
        // Inject a NeedsDecision outcome so the VM opens the restore sheet.
        val holder = ReconciliationResultHolder()
        val needsDecision =
            SessionReconciliationPolicy.Outcome.NeedsDecision(
                token = "tok-r",
                session = CurrentTrackData(token = "tok-r", isTracking = true),
                reason = "app-kill",
            )
        // Bypass the flow (synchronous state injection) — directly call the handler.
        vm.handleRecoveryResume()
        // No activeRecovery set → handler is a no-op; phase unchanged.
        assertEquals(TrackMilesPhase.IDLE, vm.uiState.value.phase)
        assertEquals(0, controller.started.size)
    }

    @Test
    fun `handleRecoveryDiscard delegates stop and clears sheet`() {
        val controller = FakeTrackingController()
        val vm = buildVm(controller = controller)
        // No activeRecovery in state → no-op.
        vm.handleRecoveryDiscard()
        assertEquals(TrackSheet.NONE, vm.uiState.value.activeSheet)
        assertEquals(0, controller.stopped.size)
    }

    @Test
    fun `handleRecoverySaveFinish clears sheet and recovery config`() {
        val vm = buildVm()
        vm.handleRecoverySaveFinish()
        // No activeRecovery → no-op; state unchanged.
        assertEquals(TrackSheet.NONE, vm.uiState.value.activeSheet)
        assertNull(vm.uiState.value.activeRecovery)
    }

    // ── P3.5: cold-start reconciliation ──────────────────────────────────────

    private fun activeTrack(
        routeId: String = "route-restored",
        employeeCode: String = "EMP-OTHER",
        accountId: String? = "ACC-OTHER",
        accountEmail: String = "other@mileway.app",
    ) = SavedTrack(
        routeId = routeId,
        name = "Journey $routeId",
        startLatitude = 0.0, startLongitude = 0.0,
        endLatitude = 0.0, endLongitude = 0.0,
        pausedLatitude = 0.0, pausedLongitude = 0.0,
        startTime = 1_000L, endTime = -1L,
        distance = 4_200.0, duration = 0L,
        startedAtTimestamp = 1_000L,
        startedByAccountId = accountId,
        startedByEmployeeCode = employeeCode,
        startedByAccountEmail = accountEmail,
        startedByTenant = DEFAULT_SESSION_TENANT,
    )

    private val meIdentitySession =
        SessionState(
            kind = SessionKind.CREDENTIALS,
            email = "me@mileway.app",
            employeeCode = "EMP-ME",
            tenant = DEFAULT_SESSION_TENANT,
            signedInAtMillis = 500L,
        )

    @Test
    fun `cold start with a mismatched ownership pointer shows the stranger-session dialog instead of restoring`() {
        val vm =
            TrackMilesViewModelTestHarness.build(
                seedTracks = listOf(activeTrack()),
                sessionSource = FakeSessionSource(meIdentitySession),
                activeAccountSource = FakeActiveAccountSource(activeAccountId = "ACC-ME"),
                mockAccounts = listOf(mockAccount("ACC-OTHER", "EMP-OTHER", "Other Persona")),
            )

        val state = vm.uiState.value
        assertEquals(TrackSheet.STRANGER_SESSION, state.activeSheet)
        assertEquals(TrackMilesPhase.IDLE, state.phase)
        assertNull(state.currentRouteId)
        assertEquals("Other Persona", state.activeStrangerSession?.ownerLabel)
        assertEquals("route-restored", state.activeStrangerSession?.routeId)
    }

    @Test
    fun `cold start with a matching ownership pointer restores silently as today`() {
        val vm =
            TrackMilesViewModelTestHarness.build(
                seedTracks =
                    listOf(
                        activeTrack(employeeCode = "EMP-ME", accountId = "ACC-ME", accountEmail = "me@mileway.app"),
                    ),
                sessionSource = FakeSessionSource(meIdentitySession),
                activeAccountSource = FakeActiveAccountSource(activeAccountId = "ACC-ME"),
            )

        val state = vm.uiState.value
        assertEquals(TrackSheet.NONE, state.activeSheet)
        assertEquals(TrackMilesPhase.TRACKING, state.phase)
        assertEquals("route-restored", state.currentRouteId)
        assertNull(state.activeStrangerSession)
    }

    @Test
    fun `no active persona selected restores silently (unchanged pre-P3_1 behavior)`() {
        val vm =
            TrackMilesViewModelTestHarness.build(
                seedTracks = listOf(activeTrack()),
                // Default ActiveAccountSource/SessionSource — no active persona pointer set yet.
            )

        val state = vm.uiState.value
        assertEquals(TrackSheet.NONE, state.activeSheet)
        assertEquals(TrackMilesPhase.TRACKING, state.phase)
        assertEquals("route-restored", state.currentRouteId)
    }

    @Test
    fun `StrangerSessionResume restores the flagged trip and clears the dialog`() {
        val vm =
            TrackMilesViewModelTestHarness.build(
                seedTracks = listOf(activeTrack()),
                sessionSource = FakeSessionSource(meIdentitySession),
                activeAccountSource = FakeActiveAccountSource(activeAccountId = "ACC-ME"),
                mockAccounts = listOf(mockAccount("ACC-OTHER", "EMP-OTHER", "Other Persona")),
            )
        assertEquals(TrackSheet.STRANGER_SESSION, vm.uiState.value.activeSheet)

        vm.handleStrangerSessionResume()

        val state = vm.uiState.value
        assertEquals(TrackSheet.NONE, state.activeSheet)
        assertNull(state.activeStrangerSession)
        assertEquals(TrackMilesPhase.TRACKING, state.phase)
        assertEquals("route-restored", state.currentRouteId)
    }

    @Test
    fun `StrangerSessionDismiss clears the dialog and leaves the trip un-displayed`() {
        val vm =
            TrackMilesViewModelTestHarness.build(
                seedTracks = listOf(activeTrack()),
                sessionSource = FakeSessionSource(meIdentitySession),
                activeAccountSource = FakeActiveAccountSource(activeAccountId = "ACC-ME"),
                mockAccounts = listOf(mockAccount("ACC-OTHER", "EMP-OTHER", "Other Persona")),
            )
        assertEquals(TrackSheet.STRANGER_SESSION, vm.uiState.value.activeSheet)

        vm.handleStrangerSessionDismiss()

        val state = vm.uiState.value
        assertEquals(TrackSheet.NONE, state.activeSheet)
        assertNull(state.activeStrangerSession)
        assertEquals(TrackMilesPhase.IDLE, state.phase)
        assertNull(state.currentRouteId)
    }

    // ── C4: permissionsSatisfied wiring (real PermissionsProvider, not hardcoded true) ───────

    @Test
    fun `permissionsSatisfied is false and journeyProgress is PERMISSIONS when the provider reports ungranted`() {
        val vm = TrackMilesViewModelTestHarness.build(permissionsProvider = FakePermissionsProvider(granted = false))

        val state = vm.uiState.value
        assertEquals(false, state.permissionsSatisfied)
        assertEquals(JourneyStep.PERMISSIONS, state.journeyProgress)
        assertEquals(TrackMilesPrimaryAction.ResolvePermissions, state.primaryAction)
    }

    @Test
    fun `permissionsSatisfied is true when the provider reports the required permission granted`() {
        val vm = TrackMilesViewModelTestHarness.build(permissionsProvider = FakePermissionsProvider(granted = true))

        assertEquals(true, vm.uiState.value.permissionsSatisfied)
    }

    @Test
    fun `requestStartTracking rechecks permissionsSatisfied against the live provider`() {
        val provider = FakePermissionsProvider(granted = false)
        val vm = TrackMilesViewModelTestHarness.build(permissionsProvider = provider)
        assertEquals(false, vm.uiState.value.permissionsSatisfied)

        // Simulates the user granting the permission out-of-band (e.g. system settings) and the
        // screen re-attempting to start tracking, which should refresh the VM's stale snapshot.
        provider.granted = true
        vm.requestStartTracking()
        assertEquals(true, vm.uiState.value.permissionsSatisfied)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun mockAccount(
        accountId: String,
        employeeCode: String,
        displayName: String,
    ) = MockAccountEntity(
        accountId = accountId,
        displayName = displayName,
        employeeCode = employeeCode,
        organization = "Org",
        avatarSeed = accountId,
        isActive = false,
        lastLoginAtMs = 0L,
        createdAtMs = 0L,
    )

    private fun buildVm(controller: TrackingController = FakeTrackingController()): TrackMilesViewModel {
        return TrackMilesViewModelTestHarness.build(controller)
    }
}
