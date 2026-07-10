package com.mileway.feature.tracking.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.core.data.model.display.OdometerCaptureResult
import com.mileway.core.data.model.display.OdometerPurpose
import com.mileway.core.data.model.display.OdometerReadingSource
import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.core.data.plugin.PluginValue
import com.mileway.core.data.settings.DemoSettings
import com.mileway.core.data.settings.DemoSettingsRepository
import com.mileway.core.data.settings.LAST_ODOMETER_NONE
import com.mileway.core.ui.AppHost
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
import com.mileway.feature.tracking.viewmodel.TrackingSuccessAction
import com.mileway.feature.tracking.viewmodel.TrackingSuccessArgs
import com.mileway.feature.tracking.viewmodel.TrackingSuccessEffect
import com.mileway.feature.tracking.viewmodel.TrackingSuccessViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

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

    // reimbursement + voucher are computed/persisted by TrackingSuccessViewModel from vehicleKey,
    // so the route no longer carries the mock reimbursable/voucher values as args.
    const val SUCCESS =
        "success?distanceKm={distanceKm}&vehicleKey={vehicleKey}&vehicleName={vehicleName}" +
            "&startTime={startTime}&endTime={endTime}&transId={transId}&status={status}" +
            "&violationCount={violationCount}&violationMsg={violationMsg}"

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
        return "success?distanceKm=${r.distanceKm}&vehicleKey=${enc(r.vehicleKey)}" +
            "&vehicleName=${enc(r.vehicleName)}&startTime=${r.startTime}&endTime=${r.endTime}" +
            "&transId=${enc(r.transactionId ?: "")}&status=${r.submissionStatus}" +
            "&violationCount=${r.violationCount}&violationMsg=${enc(r.violationMessage ?: "")}"
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
 *
 * [onAddExpense] (P27.E.5) is supplied by the app shell so feature:tracking never depends on
 * feature:logging directly — it only hands back the [ExpenseSourceContext] the app shell needs to
 * build feature:logging's expense-entry route. Defaults to a no-op so [TrackingNavHost] (the
 * standalone `TrackMilesActivity` host, which has no expense flow to link to) needs no change.
 */
fun NavGraphBuilder.trackingGraph(
    navController: NavHostController,
    onAddExpense: (ExpenseSourceContext) -> Unit = {},
) {
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
        val viewModel: com.mileway.feature.tracking.viewmodel.CheckInHistoryViewModel = koinViewModel()
        val events by viewModel.items.collectAsStateWithLifecycle()
        CheckInHistoryScreen(
            events = events,
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
                navArgument("vehicleKey") {
                    type = NavType.StringType
                    defaultValue = ""
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
            ),
    ) { backStack ->
        val args = backStack.arguments!!

        fun dec(s: String?) = s.orEmpty().let { java.net.URLDecoder.decode(it, "UTF-8") }
        val toSaved = {
            navController.navigate(TrackingRoutes.SAVED_TRACKS) {
                popUpTo(TrackingRoutes.SAVED_TRACKS) { inclusive = true }
            }
        }

        val successArgs =
            TrackingSuccessArgs(
                distanceKm = args.getFloat("distanceKm").toDouble(),
                vehicleKey = dec(args.getString("vehicleKey")),
                vehicleName = dec(args.getString("vehicleName")),
                startTime = args.getLong("startTime"),
                endTime = args.getLong("endTime"),
                transactionId = dec(args.getString("transId")).ifBlank { null },
                submissionStatus = args.getString("status") ?: "SUCCESS",
                violationCount = args.getInt("violationCount"),
                violationMessage = dec(args.getString("violationMsg")).ifBlank { null },
            )
        val viewModel: TrackingSuccessViewModel = koinViewModel { parametersOf(successArgs) }
        val state by viewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(viewModel) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    TrackingSuccessEffect.NavigateToHub -> toSaved()
                    // No per-transaction detail screen yet — route to the voucher/approvals list.
                    TrackingSuccessEffect.NavigateToExpenseList -> navController.navigate(TrackingRoutes.CREATE_VOUCHER)
                    is TrackingSuccessEffect.NavigateToAddExpense -> onAddExpense(effect.context)
                }
            }
        }

        TrackingSuccessScreen(
            distanceKm = state.distanceKm,
            reimbursableAmount = state.reimbursableAmount,
            vehicleName = state.vehicleName,
            startTime = state.startTime,
            endTime = state.endTime,
            transactionId = state.transactionId,
            submissionStatus = state.submissionStatus,
            violationCount = state.violationCount,
            violationMessage = state.violationMessage,
            voucherNumber = state.voucherNumber,
            voucherAmount = state.voucherAmount,
            onTrackNewJourney = { viewModel.onAction(TrackingSuccessAction.TrackNewJourney) },
            onViewExpense = { viewModel.onAction(TrackingSuccessAction.ViewExpense) },
            onCreateVoucher = { viewModel.onAction(TrackingSuccessAction.CreateVoucher) },
            onAddExpense = { viewModel.onAction(TrackingSuccessAction.AddExpense) },
        )
    }

    composable(TrackingRoutes.CREATE_VOUCHER) {
        CreateVoucherScreen(onBack = { navController.popBackStack() })
    }

    composable(TrackingRoutes.TRACK_SETTINGS) {
        // P10.1: bind each knob to its persisted PluginRegistry value (mirrors the G6 Kalman
        // pattern above). VALUE plugins map IntVal↔Float for the sliders; CAPABILITY plugins are
        // read via observe(id). The service re-reads these at the next trip start.
        val registry = koinInject<PluginRegistry>()
        val scope = rememberCoroutineScope()
        val gpsAccuracy by registry.observeValue("track_min_accuracy_m")
            .collectAsState(initial = PluginValue.IntVal(50))
        val locationInterval by registry.observeValue("track_location_interval_s")
            .collectAsState(initial = PluginValue.IntVal(10))
        val minDisplacement by registry.observeValue("track_min_displacement_m")
            .collectAsState(initial = PluginValue.IntVal(0))
        val uploadInBackground by registry.observe("track_upload_in_background").collectAsState(initial = true)
        val autoPause by registry.observe("track_auto_pause_detection").collectAsState(initial = false)
        val forceGpsOnly by registry.observe("track_force_gps_only").collectAsState(initial = false)
        TrackSettingsScreen(
            onBack = { navController.popBackStack() },
            gpsAccuracy = ((gpsAccuracy as? PluginValue.IntVal)?.value ?: 50).toFloat(),
            onGpsAccuracyChange = { v -> scope.launch { registry.setUserOverride("track_min_accuracy_m", PluginValue.IntVal(v.toInt())) } },
            locationInterval = ((locationInterval as? PluginValue.IntVal)?.value ?: 10).toFloat(),
            onLocationIntervalChange = { v -> scope.launch { registry.setUserOverride("track_location_interval_s", PluginValue.IntVal(v.toInt())) } },
            distanceThreshold = ((minDisplacement as? PluginValue.IntVal)?.value ?: 0).toFloat(),
            onDistanceThresholdChange = { v -> scope.launch { registry.setUserOverride("track_min_displacement_m", PluginValue.IntVal(v.toInt())) } },
            uploadInBackground = uploadInBackground,
            onUploadInBackgroundChange = { v -> scope.launch { registry.setUserOverride("track_upload_in_background", PluginValue.Bool(v)) } },
            autoPauseDetection = autoPause,
            onAutoPauseDetectionChange = { v -> scope.launch { registry.setUserOverride("track_auto_pause_detection", PluginValue.Bool(v)) } },
            forceGpsOnly = forceGpsOnly,
            onForceGpsOnlyChange = { v -> scope.launch { registry.setUserOverride("track_force_gps_only", PluginValue.Bool(v)) } },
        )
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
