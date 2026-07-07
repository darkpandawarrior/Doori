package com.mileway

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.common.deeplink.DeepLinkRouter
import com.mileway.core.common.deeplink.DeepLinkValidator
import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.session.SessionState
import com.mileway.core.network.config.ConfigProvider
import com.mileway.core.platform.AnalyticsEvent
import com.mileway.core.ui.platform.LocalAnalyticsHelper
import com.mileway.core.ui.platform.LocalManagerProvider
import com.mileway.core.ui.platform.MaintenanceGate
import com.mileway.core.ui.platform.UpdateGate
import com.mileway.core.ui.platform.isUnderMaintenance
import com.mileway.core.ui.theme.MilewayTheme
import com.mileway.core.ui.theme.ThemeController
import com.mileway.ui.MilewayAppRoot
import com.mileway.ui.auth.rememberPermissionsController
import com.mileway.ui.auth.CheckPinScreen
import com.mileway.ui.auth.LoginScreen
import com.mileway.ui.auth.SetPinScreen
import com.mileway.ui.auth.SplashScreen
import com.mileway.ui.toAppRoute
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * App startup stages: splash → fake login → PLAN_V22 P7.4's local PIN/biometric gate → the
 * bottom-navigation shell. [PIN] sits between [LOGIN] and [APP] on every path into the app
 * (fresh sign-in and a returning already-signed-in session both pass through it), rendering
 * `SetPinScreen` the first time (`SessionState.hasPin == false`) or `CheckPinScreen` on every
 * later launch until sign-out.
 */
private enum class AppStage { SPLASH, LOGIN, PIN, APP }

/**
 * Single entry point for the app. Plays the splash and fake-login theatre, then hosts the
 * unified bottom-navigation shell ([MilewayAppRoot]).
 *
 * Handles `mileway://` deep links by mapping the host segment to a graph route and
 * forwarding it to [MilewayAppRoot] as [initialRoute].
 */
class LauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySecurityFlags()
        enableEdgeToEdge()
        setContent { AppEntry(initialRoute = intent.deepLinkRoute()) }
    }

    /**
     * Blocks screenshots/screen-recording of sensitive expense data (FLAG_SECURE) and rejects
     * touches while another app's overlay is drawn on top of us (tapjacking guard).
     */
    private fun applySecurityFlags() {
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        window.decorView.filterTouchesWhenObscured = true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        setContent { AppEntry(initialRoute = intent.deepLinkRoute()) }
    }
}

private fun Intent.deepLinkRoute(): String? {
    if (action != Intent.ACTION_VIEW) return null
    val raw = data?.toString() ?: return null
    if (!DeepLinkValidator.isAllowed(raw)) return null
    // DL.2/DL.4: route both mileway:// and verified https App Links through the shared router, then map
    // to a concrete nav route (including sub-destinations: checkin / expense / settings) via toAppRoute().
    return DeepLinkRouter.resolve(raw).toAppRoute()
}

/**
 * Pure reconciliation of [AppStage] against the persisted [session] — extracted from [AppEntry]'s
 * `LaunchedEffect` so the branching lives in a small, directly-testable function rather than
 * inflating that composable's cyclomatic complexity.
 *
 * A.1: once a session resolves as already-signed-in, splash/login should never replay for it.
 * P7.4: a signed-in session still has to pass the PIN gate — only jumps straight to [AppStage.APP]
 * once [SessionState.hasPin] is true; a fresh sign-in with no PIN yet routes to [AppStage.PIN]
 * instead. P2.4: the reverse transition — a session flipping back to signed-out while past LOGIN
 * falls back to [AppStage.LOGIN].
 *
 * @return the stage [AppEntry] should move to, or `null` if [currentStage] already reflects [session].
 */
private fun nextStageForSession(
    session: SessionState?,
    currentStage: AppStage,
): AppStage? {
    if (session == null) return null
    return when {
        session.isSignedIn && session.hasPin && currentStage != AppStage.PIN && currentStage != AppStage.APP ->
            AppStage.APP
        session.isSignedIn && !session.hasPin && currentStage == AppStage.SPLASH -> AppStage.PIN
        !session.isSignedIn && (currentStage == AppStage.PIN || currentStage == AppStage.APP) -> AppStage.LOGIN
        else -> null
    }
}

@Composable
private fun AppEntry(
    initialRoute: String? = null,
    themeController: ThemeController = koinInject(),
    configProvider: ConfigProvider = koinInject(),
    sessionRepository: SessionRepository = koinInject(),
) {
    val sessionScope = rememberCoroutineScope()
    // A.1: the persisted session is the source of truth for "did the user pass login, and how".
    // null = still loading; once known we can skip the splash/login theatre for a returning user
    // (guest or credentials), so navigation, deep links and process recreation never bounce them.
    val session by sessionRepository.sessionState.collectAsStateWithLifecycle(initialValue = null)
    val updateConfig = remember(configProvider) { configProvider.getUpdateConfig() }
    // CF.5: kill-switch / min-version maintenance gate (inert in the demo: killSwitch off, versionCode ≥ min).
    val underMaintenance =
        remember(configProvider, updateConfig) {
            isUnderMaintenance(
                killSwitchOn = configProvider.isKillSwitchOn(),
                currentVersionCode = BuildConfig.VERSION_CODE.toLong(),
                minSupportedVersionCode = updateConfig.minSupportedVersionCode,
            )
        }
    val systemDark = isSystemInDarkTheme()
    val override by themeController.darkThemeOverride.collectAsStateWithLifecycle()
    val milewayTheme by themeController.milewayTheme.collectAsStateWithLifecycle()
    val palette by themeController.accentPalette.collectAsStateWithLifecycle()
    val customSeedHex by themeController.customSeedHex.collectAsStateWithLifecycle()
    val useSystemColors by themeController.useSystemColors.collectAsStateWithLifecycle()
    val paletteStyle by themeController.paletteStyle.collectAsStateWithLifecycle()

    var stage by rememberSaveable { mutableStateOf(AppStage.SPLASH) }

    // See nextStageForSession's doc for the full A.1/P7.4/P2.4 reconciliation rationale;
    // `MilewayAppRoot`'s `onSignedOut` below is the fast path for the live composition, this is
    // the durable, process-death-safe path.
    LaunchedEffect(session) {
        nextStageForSession(session, stage)?.let { stage = it }
    }

    // PF.4: seed the Activity-scoped platform managers (in-app update / review / share / …) so any
    // shared screen can read them via `LocalAppReviewManager.current` with no expect/actual at the call site.
    LocalManagerProvider {
        // CF.2: one representative event showing the AnalyticsSink wiring end to end — fires once
        // per stage transition into the app shell (LocalAnalyticsHelper resolves to a real Koin
        // binding here, the shared no-op default otherwise, e.g. previews).
        val analyticsHelper = LocalAnalyticsHelper.current
        LaunchedEffect(stage) {
            if (stage == AppStage.APP) analyticsHelper.log(AnalyticsEvent("app_session_start"))
        }
        MilewayTheme(
            darkTheme = override ?: systemDark,
            milewayTheme = milewayTheme,
            palette = palette,
            customSeedHex = customSeedHex,
            useSystemColors = useSystemColors,
            paletteStyle = paletteStyle,
        ) {
            MaintenanceGate(underMaintenance = underMaintenance) {
                // P7.6: the update gate now wraps every stage (splash/login/PIN/app), not just APP,
                // so a forced/flexible update blocks the login screen itself, matching MaintenanceGate's
                // existing scope — only AppStage.APP's inner behavior is unchanged.
                UpdateGate(config = updateConfig) {
                    // Android runtime-permission bridge, passed into the (now shared) LoginScreen so the
                    // welcome-disclaimer "Continue" still triggers the real system permission dialog.
                    val permissionsController = rememberPermissionsController(onResult = {})
                    AnimatedContent(
                        targetState = stage,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "appStage",
                    ) { current ->
                        when (current) {
                            AppStage.SPLASH -> SplashScreen(onFinished = { stage = AppStage.LOGIN })
                            AppStage.LOGIN ->
                                LoginScreen(
                                    // A.1: persist how the user signed in BEFORE entering the app, so the
                                    // session (guest or credentials) survives recreation and deep links.
                                    // P7.4: land on the PIN gate next, not the app shell directly — every
                                    // sign-in (fresh or guest) passes through SetPinScreen/CheckPinScreen.
                                    onSignInWithCredentials = { email ->
                                        sessionScope.launch { sessionRepository.signInWithCredentials(email) }
                                        stage = AppStage.PIN
                                    },
                                    onContinueAsGuest = {
                                        sessionScope.launch { sessionRepository.continueAsGuest() }
                                        stage = AppStage.PIN
                                    },
                                    onRequestPermissions = { permissionsController.request() },
                                    // P7.5: show WelcomeDisclaimerSheet once per install, persisted so
                                    // it never replays after this first shown-and-dismissed session.
                                    hasShownWelcomeDisclaimer = session?.hasShownWelcomeDisclaimer == true,
                                    onWelcomeDisclaimerShown = {
                                        sessionScope.launch { sessionRepository.markWelcomeDisclaimerShown() }
                                    },
                                )
                            AppStage.PIN ->
                                PinGateStage(hasPin = session?.hasPin == true, onPassed = { stage = AppStage.APP })
                            AppStage.APP ->
                                MilewayAppRoot(
                                    deepLinkRoute = initialRoute,
                                    // P2.4: the last persona was just signed out of — jump back to
                                    // login immediately rather than waiting on the DataStore
                                    // round-trip below (the LaunchedEffect(session) below is the
                                    // durable/process-death-safe path; this is the fast path).
                                    onSignedOut = { stage = AppStage.LOGIN },
                                )
                        }
                    }
                }
            }
        }
    }
}

/**
 * `AppStage.PIN`'s screen picker, extracted from [AppEntry] to keep that composable's branching
 * flat. `SetPinScreen` the first time this session has ever passed the gate ([hasPin] false),
 * `CheckPinScreen` on every later launch until sign-out. The session can briefly be null right
 * after a fresh sign-in (DataStore write still in flight) — [AppEntry] treats that as "no PIN yet"
 * ([hasPin] false) so the reviewer sees setup rather than a flash of the verify screen.
 */
@Composable
private fun PinGateStage(
    hasPin: Boolean,
    onPassed: () -> Unit,
) {
    if (hasPin) {
        CheckPinScreen(onUnlocked = onPassed)
    } else {
        SetPinScreen(onCompleted = onPassed, onSkip = onPassed)
    }
}
