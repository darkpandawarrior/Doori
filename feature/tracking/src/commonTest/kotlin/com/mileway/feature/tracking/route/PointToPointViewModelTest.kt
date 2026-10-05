@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.mileway.feature.tracking.route

import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.domain.policy.TripClassification
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedPlaceEntity
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.session.ActiveAccountSource
import com.mileway.core.network.routing.OsrmClient
import com.mileway.core.network.routing.OsrmConfiguration
import com.mileway.core.network.routing.RouteEstimate
import com.mileway.core.platform.ShareSheet
import com.mileway.feature.tracking.places.SavedPlacesRepository
import com.mileway.feature.tracking.repository.HardwareEventRepository
import com.mileway.feature.tracking.repository.LocationRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.submission.SubmitMilesRequestBuilder
import com.mileway.feature.tracking.ui.components.ExportFormat
import com.mileway.feature.tracking.ui.components.LocationDataFilter
import com.mileway.feature.tracking.viewmodel.EventLogDao
import com.mileway.feature.tracking.viewmodel.ExportViewModel
import com.mileway.feature.tracking.viewmodel.FakeLocationDao
import com.mileway.feature.tracking.viewmodel.FakeSavedTrackDao
import com.mileway.feature.tracking.viewmodel.SubmissionFormUi
import com.siddharth.kmp.network.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PointToPointViewModelTest {
    private val origin = SavedPlaceEntity("origin", "OTHER", "Start", "", 12.0, 34.0, 0L)
    private val destination = origin.copy(id = "destination", label = "End", latitude = 13.0)
    private val at = 100_000L
    private val account =
        object : ActiveAccountSource {
            override val activeAccountId = MutableStateFlow<String?>("account")

            override suspend fun setActiveAccountId(accountId: String) {
                activeAccountId.value = accountId
            }
        }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun trip() =
        SavedTrack(
            routeId = "return",
            name = "Recorded return",
            isCompleted = true,
            startedByAccountId = "account",
            startLatitude = 13.0,
            startLongitude = 34.0,
            endLatitude = 12.0,
            endLongitude = 34.0,
            pausedLatitude = 0.0,
            pausedLongitude = 0.0,
            startTime = 50_000L,
            endTime = 90_000L,
            distance = 12_000.0,
            duration = 40_000L,
        )

    @Test
    fun routedPairNeverAddsASecondReturnAndUsesTheExistingRoundTripField() =
        runTest {
            val trackDao = FakeSavedTrackDao(listOf(trip()))
            val http =
                createHttpClient(
                    engine =
                        MockEngine { request ->
                            assertEquals("/route/v1/driving/34.0,12.0;34.0,13.0", request.url.encodedPath)
                            respond(
                                """{"code":"Ok","routes":[{"distance":15000}]}""",
                                HttpStatusCode.OK,
                                headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        },
                    retry = false,
                )
            val config = OsrmConfiguration()
            val repository = SavedTrackRepository(trackDao)
            val places = SavedPlacesRepository(RoutePlaceDao(), RouteFavouriteDao())
            val vm = PointToPointViewModel(places, repository, account, OsrmClient(config) { http }, config, now = { at })
            val input = RouteInput(origin, destination, roundTrip = true, server = "http://route.test")
            try {
                vm.estimate(input)
                vm.state.first { !it.busy }
                assertTrue(vm.state.value.returnAlreadyRecorded)
                vm.save(input, null)
                val row = repository.getByRouteId(requireNotNull(vm.state.value.savedRouteId))
                assertNotNull(row)
                assertEquals(15_000.0, row.distance)
                assertFalse(row.roundTrip)
                assertTrue(row.isDraft)
                assertFalse(row.isCompleted)
            } finally {
                http.close()
            }
        }

    @Test
    fun protectedHomeFallsBackToApproximateAndPersistsManualTotalWithoutDoubling() =
        runTest {
            val placeDao = RoutePlaceDao()
            val places = SavedPlacesRepository(placeDao, RouteFavouriteDao())
            val home = origin.copy(id = "home", type = "HOME", address = "Private road")
            places.save(home)
            val repository = SavedTrackRepository(FakeSavedTrackDao())
            val config = OsrmConfiguration()
            val vm = PointToPointViewModel(places, repository, account, OsrmClient(config) { error("Home must not reach HTTP") }, config, now = { at })
            val input = RouteInput(home, destination, roundTrip = true, server = "http://route.test")
            vm.estimate(input)
            assertIs<RouteEstimate.ManualRequired>(vm.state.value.estimate)
            assertEquals(
                TripClassification.PERSONAL,
                vm.state.value.decision
                    ?.classification,
            )
            vm.save(input, 20.0, favourite = true)
            vm.save(input, 20.0, favourite = true)
            val row = repository.rawTracksFlow().first().single()
            assertEquals(20_000.0, row.distance)
            assertTrue(row.roundTrip)
            assertEquals("MANUAL_APPROXIMATE", row.violationRemarks)
            assertEquals("PERSONAL", row.notes)
            assertEquals(1, places.favouriteRoutes.first().size)
        }

    @Test
    fun staleQuoteInvalidDistanceAndAccountSwitchCannotSave() =
        runTest {
            val repository = SavedTrackRepository(FakeSavedTrackDao())
            val places = SavedPlacesRepository(RoutePlaceDao(), RouteFavouriteDao())
            val config = OsrmConfiguration()
            val vm = PointToPointViewModel(places, repository, account, OsrmClient(config), config, now = { at })
            val input = RouteInput(origin, destination)
            vm.estimate(input)
            vm.save(input.copy(vehicleKey = "BIKE"), 12.0)
            assertEquals(0L, repository.count())
            vm.save(input, Double.NaN)
            assertEquals(0L, repository.count())
            account.activeAccountId.value = "other"
            vm.save(input, 12.0)
            assertNull(vm.state.value.savedRouteId)
            vm.invalidate()
            vm.save(input, 12.0)
            assertNull(vm.state.value.savedRouteId)
        }

    @Test
    fun recordedLoopAndSeparateReturnDoNotSetAnExtraReturnOnSubmission() {
        val outbound = trip().copy(routeId = "outbound", startLatitude = 12.0, endLatitude = 13.0, endTime = at)

        fun build(
            row: SavedTrack,
            recorded: List<SavedTrack>,
        ) = SubmitMilesRequestBuilder.build(
            row.routeId,
            "CAR",
            12.0,
            row.startTime,
            row.endTime,
            at,
            SubmissionFormUi(roundTrip = true),
            track = row,
            recordedTracks = recorded,
        )
        assertTrue(build(outbound, emptyList()).roundTrip)
        assertFalse(build(outbound, listOf(trip())).roundTrip)
        assertFalse(build(outbound.copy(roundTrip = true), emptyList()).roundTrip)
        assertFalse(build(outbound.copy(endLatitude = 12.0), emptyList()).roundTrip)
        assertTrue(build(outbound, listOf(trip().copy(isDraft = true))).roundTrip)
        assertTrue(build(outbound, listOf(trip().copy(startedByAccountId = "other"))).roundTrip)
        assertTrue(build(outbound, listOf(trip().copy(endTime = at + 1L))).roundTrip)
        assertFalse(build(outbound.copy(endTime = 60_000L), listOf(trip())).roundTrip)
    }

    @Test
    fun exportCannotDisableProtectedHomeRedaction() =
        runTest {
            val track = trip().copy(name = "Private road")
            val home = origin.copy(type = "HOME", address = "Private road")
            val places = RoutePlaceDao().also { it.upsert(home) }
            val point = LocationData(activity = "Stationary", speed = 0f, lat = 12.0, lng = 34.0, token = track.routeId, batteryPercentage = 100.0)
            val locations =
                object : LocationDao by FakeLocationDao() {
                    override suspend fun getLocationsByTokenPaged(
                        token: String,
                        limit: Int,
                        offset: Int,
                    ) = listOf(point)
                }
            var exported = ""
            val share =
                object : ShareSheet {
                    override fun share(
                        text: String,
                        subject: String?,
                        fileUri: String?,
                    ) {
                        exported = text
                    }
                }
            val vm =
                ExportViewModel(
                    SavedTrackRepository(FakeSavedTrackDao(listOf(track))),
                    LocationRepository(locations),
                    HardwareEventRepository(EventLogDao()),
                    share,
                    places,
                )
            vm.export(track.routeId, ExportFormat.JSON, LocationDataFilter(redactHome = false))
            assertFalse(exported.contains("Private road"))
            assertFalse(exported.contains("\"startLat\": 12.0"))
            assertTrue(exported.contains("\"startLat\": null"))
        }
}
