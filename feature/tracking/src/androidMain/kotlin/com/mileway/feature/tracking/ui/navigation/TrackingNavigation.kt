package com.mileway.feature.tracking.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mileway.core.data.model.display.OdometerCaptureResult
import com.mileway.core.data.model.display.OdometerPurpose
import com.mileway.core.data.model.display.OdometerReadingSource
import com.mileway.core.data.settings.DemoSettings
import com.mileway.core.data.settings.DemoSettingsRepository
import com.mileway.core.data.settings.LAST_ODOMETER_NONE
import com.mileway.core.ui.AppHost
import com.mileway.feature.tracking.ui.screens.CheckInHistoryItem
import com.mileway.feature.tracking.ui.screens.CheckInHistoryScreen
import com.mileway.feature.tracking.ui.screens.CreateVoucherScreen
import com.mileway.feature.tracking.ui.screens.GeoCheckInScreen
import com.mileway.feature.tracking.ui.screens.HardwareEventsLogScreen
import com.mileway.feature.tracking.ui.screens.LocationMapScreen
import com.mileway.feature.tracking.ui.screens.ManualCheckInScreen
import com.mileway.feature.tracking.ui.screens.OdometerCameraScreen
import com.mileway.feature.tracking.ui.screens.RoutePointsScreen
import com.mileway.feature.tracking.ui.screens.SavedTracksScreen
import com.mileway.feature.tracking.ui.screens.SetupGuideScreen
import com.mileway.feature.tracking.ui.screens.TrackCustomizationScreen
import com.mileway.feature.tracking.ui.screens.TrackDataPreviewScreen
import com.mileway.feature.tracking.ui.screens.TrackDetailScreen
import com.mileway.feature.tracking.ui.screens.TrackInsightsScreen
import com.mileway.feature.tracking.ui.screens.TrackMilesScreen
import com.mileway.feature.tracking.ui.screens.TrackSettingsScreen
import com.mileway.feature.tracking.ui.screens.TrackSubmissionScreen
import com.mileway.feature.tracking.ui.screens.TrackingSuccessScreen
import com.mileway.feature.tracking.viewmodel.MileageSubmissionAction
import com.mileway.feature.tracking.viewmodel.MileageSubmissionViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

object TrackingRoutes {
    const val SAVED_TRACKS = "saved_tracks"
    const val LIVE_TRACK = "live_track/{routeId}"
    const val LIVE_MAP = "live_map/{routeId}"
    const val DETAIL = "detail/{routeId}"
    const val INSIGHTS = "insights/{routeId}"
    const val HW_EVENTS = "hw_events/{routeId}"
    const val ROUTE_POINTS = "route_points/{routeId}"
    const val CHECK_IN_HISTORY = "check_in_history"
    const val ROUTE_MAP = "route_map/{routeId}"
    const val SUBMIT = "submit/{routeId}?distanceKm={distanceKm}&vehicleKey={vehicleKey}&startTime={startTime}&endTime={endTime}"
    const val CREATE_VOUCHER = "create_voucher"
    const val TRACK_SETTINGS = "track_settings"
    const val TRACK_CUSTOMIZATION = "track_customization"
    const val SETUP_GUIDE = "setup_guide"
    const val ODOMETER_CAMERA = "odometer_camera/{purpose}?distanceKm={distanceKm}&startReading={startReading}"
    const val GEO_CHECKIN = "geo_checkin"
    const val MANUAL_CHECKIN = "manual_checkin"
    const val TRACK_DATA_PREVIEW = "track_data_preview/{routeId}"

    fun trackDataPreview(routeId: String) = "track_data_preview/$routeId"

    const val SUCCESS =
        "success?distanceKm={distanceKm}&reimbursable={reimbursable}&vehicleName={vehicleName}" +
            "&startTime={startTime}&endTime={endTime}&transId={transId}&status={status}" +
            "&violationCount={violationCount}&violationMsg={violationMsg}" +
            "&voucherNumber={voucherNumber}&voucherAmount={voucherAmount}"

    fun liveTrack(routeId: String) = "live_track/$routeId"

    fun liveMap(routeId: String) = "live_map/$routeId"

    fun detail(routeId: String) = "detail/$routeId"

    fun insights(routeId: String) = "insights/$routeId"

    fun hwEvents(routeId: String) = "hw_events/$routeId"

    fun routePoints(routeId: String) = "route_points/$routeId"

    fun routeMap(routeId: String) = "route_map/$routeId"

    fun submit(
        routeId: String,
        distanceKm: Double,
        vehicleKey: String,
        startTime: Long,
        endTime: Long,
    ) = "submit/$routeId?distanceKm=$distanceKm&vehicleKey=$vehicleKey&startTime=$startTime&endTime=$endTime"

    fun odometerCamera(
        purpose: String,
        distanceKm: Double = 0.0,
        startReading: Int = 45_000,
    ) = "odometer_camera/$purpose?distanceKm=$distanceKm&startReading=$startReading"

    fun success(r: SubmissionResult): String {
        fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
        return "success?distanceKm=${r.distanceKm}&reimbursable=${r.reimbursableAmount}" +
            "&vehicleName=${enc(r.vehicleName)}&startTime=${r.startTime}&endTime=${r.endTime}" +
            "&transId=${enc(r.transactionId ?: "")}&status=${r.submissionStatus}" +
            "&violationCount=${r.violationCount}&violationMsg=${enc(r.violationMessage ?: "")}" +
            "&voucherNumber=${enc(r.voucherNumber ?: "")}&voucherAmount=${r.voucherAmount}"
    }
}

/**
 * Standalone host used by [com.mileway.feature.tracking.TrackMilesActivity].
 * Applies the theme itself. When hosted inside the app shell, use [trackingGraph]
 * directly instead so theming is applied only once at the root.
 */
@Composable
fun TrackingNavHost(navController: NavHostController = rememberNavController()) {
    AppHost {
        NavHost(navController = navController, startDestination = TrackingRoutes.SAVED_TRACKS) {
            trackingGraph(navController)
        }
    }
}

/**
 * The tracking destinations as a reusable nav-graph builder so the app shell can host
 * them inside a nested graph without an inner [NavHost] or duplicate theming.
 */
fun NavGraphBuilder.trackingGraph(navController: NavHostController) {
    composable(TrackingRoutes.SAVED_TRACKS) {
        SavedTracksScreen(
            onTrackClick = { routeId -> navController.navigate(TrackingRoutes.detail(routeId)) },
            onStartNew = {
                val newId = java.util.UUID.randomUUID().toString()
                navController.navigate(TrackingRoutes.liveTrack(newId))
            },
        )
    }

    composable(
        route = TrackingRoutes.LIVE_TRACK,
        arguments = listOf(navArgument("routeId") { type = NavType.StringType }),
    ) { backStack ->
        val routeId = backStack.arguments?.getString("routeId") ?: return@composable
        TrackMilesScreen(
            onStop = { id, distKm, vehicleKey, startTime, endTime ->
                navController.navigate(TrackingRoutes.submit(id, distKm, vehicleKey, startTime, endTime)) {
                    popUpTo(TrackingRoutes.SAVED_TRACKS)
                }
            },
            onOpenMap = { navController.navigate(TrackingRoutes.liveMap(routeId)) },
            onOpenHwEvents = { navController.navigate(TrackingRoutes.hwEvents(routeId)) },
            onOpenCheckInHistory = { navController.navigate(TrackingRoutes.CHECK_IN_HISTORY) },
            onOpenSettings = { navController.navigate(TrackingRoutes.TRACK_SETTINGS) },
            onNavigateToGeoCheckIn = { navController.navigate(TrackingRoutes.GEO_CHECKIN) },
            onNavigateToManualCheckIn = { navController.navigate(TrackingRoutes.MANUAL_CHECKIN) },
        )
    }

    composable(
        route = TrackingRoutes.LIVE_MAP,
        arguments = listOf(navArgument("routeId") { type = NavType.StringType }),
    ) { backStack ->
        backStack.arguments?.getString("routeId") ?: return@composable
        LocationMapScreen(
            onNavigateBack = { navController.popBackStack() },
        )
    }

    composable(
        route = TrackingRoutes.DETAIL,
        arguments = listOf(navArgument("routeId") { type = NavType.StringType }),
    ) { backStack ->
        val routeId = backStack.arguments?.getString("routeId") ?: return@composable
        TrackDetailScreen(
            routeId = routeId,
            onBack = { navController.popBackStack() },
            onOpenInsights = { navController.navigate(TrackingRoutes.insights(routeId)) },
            onOpenMap = { navController.navigate(TrackingRoutes.routeMap(routeId)) },
            onOpenHwEvents = { navController.navigate(TrackingRoutes.hwEvents(routeId)) },
            onOpenRoutePoints = { navController.navigate(TrackingRoutes.routePoints(routeId)) },
            onOpenDataPreview = { navController.navigate(TrackingRoutes.trackDataPreview(routeId)) },
        )
    }

    composable(
        route = TrackingRoutes.INSIGHTS,
        arguments = listOf(navArgument("routeId") { type = NavType.StringType }),
    ) { backStack ->
        val routeId = backStack.arguments?.getString("routeId") ?: return@composable
        TrackInsightsScreen(routeId = routeId, onBack = { navController.popBackStack() })
    }

    composable(
        route = TrackingRoutes.HW_EVENTS,
        arguments = listOf(navArgument("routeId") { type = NavType.StringType }),
    ) { backStack ->
        val routeId = backStack.arguments?.getString("routeId") ?: return@composable
        HardwareEventsLogScreen(routeId = routeId, onBack = { navController.popBackStack() })
    }

    composable(
        route = TrackingRoutes.ROUTE_POINTS,
        arguments = listOf(navArgument("routeId") { type = NavType.StringType }),
    ) { backStack ->
        val routeId = backStack.arguments?.getString("routeId") ?: return@composable
        RoutePointsScreen(routeId = routeId, onBack = { navController.popBackStack() })
    }

    composable(TrackingRoutes.CHECK_IN_HISTORY) {
        CheckInHistoryScreen(
            events = DemoCheckInHistory.items,
            onBack = { navController.popBackStack() },
        )
    }

    composable(
        route = TrackingRoutes.ROUTE_MAP,
        arguments = listOf(navArgument("routeId") { type = NavType.StringType }),
    ) { backStack ->
        backStack.arguments?.getString("routeId") ?: return@composable
        LocationMapScreen(onNavigateBack = { navController.popBackStack() })
    }

    composable(
        route = TrackingRoutes.SUBMIT,
        arguments =
            listOf(
                navArgument("routeId") { type = NavType.StringType },
                navArgument("distanceKm") {
                    type = NavType.FloatType
                    defaultValue = 0f
                },
                navArgument("vehicleKey") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("startTime") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument("endTime") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
            ),
    ) { backStack ->
        val args = backStack.arguments!!
        val routeId = args.getString("routeId")!!
        val distKm = args.getFloat("distanceKm").toDouble()

        // Shared ViewModel, same instance as TrackSubmissionScreen's koinViewModel()
        val viewModel: MileageSubmissionViewModel = koinViewModel()

        // Observe backstack results from OdometerCameraScreen
        val sh = backStack.savedStateHandle
        val odoStartReading by sh.getStateFlow("odo_start_reading", -1).collectAsState()
        val odoEndReading by sh.getStateFlow("odo_end_reading", -1).collectAsState()

        // G7: persisted last-trip end-odometer reading; seeds the next trip's start capture so the
        // reading rolls over (the physical odometer keeps its value) instead of resetting to 45_000.
        val demoSettings = koinInject<DemoSettingsRepository>()
        val demoSettingsState by demoSettings.settings.collectAsState(initial = DemoSettings())
        val lastOdometerEnd = demoSettingsState.lastOdometerEndReading

        androidx.compose.runtime.LaunchedEffect(odoStartReading) {
            if (odoStartReading != -1) {
                viewModel.onAction(
                    MileageSubmissionAction.CaptureOdometerStart(
                        OdometerCaptureResult(
                            purpose = OdometerPurpose.START,
                            imageUri = sh.get<String>("odo_start_uri") ?: "",
                            reading = odoStartReading,
                            source = if (sh.get<Boolean>("odo_start_manual") == true) OdometerReadingSource.MANUAL else OdometerReadingSource.DEVICE_OCR,
                            captureTimeMs = sh.get<Long>("odo_start_time") ?: 0L,
                        ),
                    ),
                )
                sh.remove<Int>("odo_start_reading")
            }
        }
        androidx.compose.runtime.LaunchedEffect(odoEndReading) {
            if (odoEndReading != -1) {
                viewModel.onAction(
                    MileageSubmissionAction.CaptureOdometerEnd(
                        OdometerCaptureResult(
                            purpose = OdometerPurpose.END,
                            imageUri = sh.get<String>("odo_end_uri") ?: "",
                            reading = odoEndReading,
                            source = if (sh.get<Boolean>("odo_end_manual") == true) OdometerReadingSource.MANUAL else OdometerReadingSource.DEVICE_OCR,
                            captureTimeMs = sh.get<Long>("odo_end_time") ?: 0L,
                        ),
                    ),
                )
                sh.remove<Int>("odo_end_reading")
                // G7: the confirmed end reading becomes the next trip's start baseline (rollover).
                demoSettings.setLastOdometerEndReading(odoEndReading)
            }
        }

        TrackSubmissionScreen(
            routeId = routeId,
            distanceKm = distKm,
            vehicleKey = args.getString("vehicleKey") ?: "",
            startTime = args.getLong("startTime"),
            endTime = args.getLong("endTime"),
            onSuccess = { result ->
                navController.navigate(TrackingRoutes.success(result)) {
                    popUpTo(TrackingRoutes.SAVED_TRACKS)
                }
            },
            onBack = { navController.popBackStack() },
            onNavigateToOdometerStart = {
                // G7: prefer an explicit per-trip override, else roll over from the last trip's
                // end reading, else the cold-start default.
                val startReading =
                    viewModel.state.value.form.simulatedStartOdo
                        ?: lastOdometerEnd.takeIf { it != LAST_ODOMETER_NONE }
                        ?: 45_000
                navController.navigate(
                    TrackingRoutes.odometerCamera("START", distKm, startReading),
                )
            },
            onNavigateToOdometerEnd = {
                val startReading =
                    viewModel.state.value.form.simulatedStartOdo
                        ?: lastOdometerEnd.takeIf { it != LAST_ODOMETER_NONE }
                        ?: 45_000
                navController.navigate(
                    TrackingRoutes.odometerCamera("END", distKm, startReading),
                )
            },
            viewModel = viewModel,
        )
    }

    composable(
        route = TrackingRoutes.ODOMETER_CAMERA,
        arguments =
            listOf(
                navArgument("purpose") { type = NavType.StringType },
                navArgument("distanceKm") {
                    type = NavType.FloatType
                    defaultValue = 0f
                },
                navArgument("startReading") {
                    type = NavType.IntType
                    defaultValue = 45_000
                },
            ),
    ) { backStack ->
        val purpose =
            OdometerPurpose.valueOf(
                backStack.arguments?.getString("purpose") ?: "START",
            )
        val distKm = backStack.arguments?.getFloat("distanceKm")?.toDouble() ?: 0.0
        val startReading = backStack.arguments?.getInt("startReading") ?: 45_000
        OdometerCameraScreen(
            purpose = purpose,
            existingReading = startReading,
            sessionDistanceKm = distKm,
            onResult = { result ->
                val submitEntry = navController.previousBackStackEntry
                val prefix = if (result.purpose == OdometerPurpose.START) "odo_start" else "odo_end"
                submitEntry?.savedStateHandle?.apply {
                    set("${prefix}_reading", result.reading)
                    set("${prefix}_uri", result.imageUri)
                    set("${prefix}_manual", result.isManual)
                    set("${prefix}_time", result.captureTimeMs)
                }
                navController.popBackStack()
            },
            onBack = { navController.popBackStack() },
        )
    }

    composable(
        route = TrackingRoutes.SUCCESS,
        arguments =
            listOf(
                navArgument("distanceKm") {
                    type = NavType.FloatType
                    defaultValue = 0f
                },
                navArgument("reimbursable") {
                    type = NavType.FloatType
                    defaultValue = 0f
                },
                navArgument("vehicleName") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("startTime") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument("endTime") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument("transId") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("status") {
                    type = NavType.StringType
                    defaultValue = "SUCCESS"
                },
                navArgument("violationCount") {
                    type = NavType.IntType
                    defaultValue = 0
                },
                navArgument("violationMsg") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("voucherNumber") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("voucherAmount") {
                    type = NavType.FloatType
                    defaultValue = 0f
                },
            ),
    ) { backStack ->
        val args = backStack.arguments!!

        fun dec(s: String?) = s.orEmpty().let { java.net.URLDecoder.decode(it, "UTF-8") }
        val toSaved = {
            navController.navigate(TrackingRoutes.SAVED_TRACKS) {
                popUpTo(TrackingRoutes.SAVED_TRACKS) { inclusive = true }
            }
        }
        TrackingSuccessScreen(
            distanceKm = args.getFloat("distanceKm").toDouble(),
            reimbursableAmount = args.getFloat("reimbursable").toDouble(),
            vehicleName = dec(args.getString("vehicleName")),
            startTime = args.getLong("startTime"),
            endTime = args.getLong("endTime"),
            transactionId = dec(args.getString("transId")).ifBlank { null },
            submissionStatus = args.getString("status") ?: "SUCCESS",
            violationCount = args.getInt("violationCount"),
            violationMessage = dec(args.getString("violationMsg")).ifBlank { null },
            voucherNumber = dec(args.getString("voucherNumber")).ifBlank { null },
            voucherAmount = args.getFloat("voucherAmount").toDouble(),
            onTrackNewJourney = toSaved,
            onViewExpense = toSaved,
            onCreateVoucher = { navController.navigate(TrackingRoutes.CREATE_VOUCHER) },
        )
    }

    composable(TrackingRoutes.CREATE_VOUCHER) {
        CreateVoucherScreen(onBack = { navController.popBackStack() })
    }

    composable(TrackingRoutes.TRACK_SETTINGS) {
        TrackSettingsScreen(onBack = { navController.popBackStack() })
    }

    composable(TrackingRoutes.TRACK_CUSTOMIZATION) {
        // G6: bind the Kalman toggle to persisted DemoSettings so it actually drives the live
        // tracking pipeline (LocationTrackingService reads enableKalman at the next trip start).
        val demoSettings = koinInject<DemoSettingsRepository>()
        val settings by demoSettings.settings.collectAsState(initial = DemoSettings())
        val scope = rememberCoroutineScope()
        TrackCustomizationScreen(
            onBack = { navController.popBackStack() },
            kalmanEnabled = settings.enableKalman,
            onKalmanChange = { enabled -> scope.launch { demoSettings.setEnableKalman(enabled) } },
        )
    }

    composable(TrackingRoutes.SETUP_GUIDE) {
        SetupGuideScreen(
            onBack = { navController.popBackStack() },
            onOpenTrackSettings = { navController.navigate(TrackingRoutes.TRACK_SETTINGS) },
        )
    }

    composable(TrackingRoutes.GEO_CHECKIN) {
        GeoCheckInScreen(onBack = { navController.popBackStack() })
    }

    composable(TrackingRoutes.MANUAL_CHECKIN) {
        ManualCheckInScreen(onBack = { navController.popBackStack() })
    }

    composable(
        route = TrackingRoutes.TRACK_DATA_PREVIEW,
        arguments = listOf(navArgument("routeId") { type = NavType.StringType }),
    ) { backStack ->
        val routeId = backStack.arguments?.getString("routeId") ?: return@composable
        TrackDataPreviewScreen(routeId = routeId, onBack = { navController.popBackStack() })
    }
}

private object DemoCheckInHistory {
    private const val DAY_MS = 86_400_000L
    private const val HOUR_MS = 3_600_000L
    private const val BASE = 1_781_654_400_000L

    val items =
        listOf(
            CheckInHistoryItem("CI-001", "Speedline Transport Co.", "Client visit – Q2 review", BASE, 18.5204, 73.8567, "Client visit", false),
            CheckInHistoryItem("CI-002", "Metro Cargo Movers", "Pickup confirmation", BASE - 2 * HOUR_MS, 18.5480, 73.8718, "Pickup", false),
            CheckInHistoryItem("CI-003", "CityLink Telecom Services", "SIM card bulk order", BASE - 5 * HOUR_MS, 18.5601, 73.8234, "Delivery", true),
            CheckInHistoryItem("CI-004", "Eastern Freight Lines", null, BASE - DAY_MS, 18.5120, 73.9012, "Geo check-in", false),
            CheckInHistoryItem("CI-005", "Southside Auto Works", "Vehicle service drop-off", BASE - DAY_MS - 3 * HOUR_MS, 18.4890, 73.8350, "Service", true),
            CheckInHistoryItem("CI-006", "Westgate Fleet Services", "Fleet inspection", BASE - 2 * DAY_MS, 18.5913, 73.7389, "Inspection", false),
            CheckInHistoryItem("CI-007", "Hadapsar Logistics Park", null, BASE - 3 * DAY_MS, 18.5089, 73.9260, "Geo check-in", false),
            CheckInHistoryItem("CI-008", "Speedline Transport Co.", "Follow-up delivery", BASE - 4 * DAY_MS, 18.5204, 73.8567, "Delivery", true),
            CheckInHistoryItem("CI-009", "Metro Cargo Movers", "Returns processing", BASE - 6 * DAY_MS, 18.5480, 73.8718, "Returns", false),
            CheckInHistoryItem("CI-010", "CityLink Telecom Services", "Invoice collection", BASE - 8 * DAY_MS, 18.5601, 73.8234, "Invoice", true),
        )
}
