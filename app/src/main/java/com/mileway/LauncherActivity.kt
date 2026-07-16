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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.common.deeplink.DeepLinkRouter
import com.mileway.core.common.deeplink.DeepLinkValidator
import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.session.SessionState
import com.mileway.core.network.config.ConfigProvider
import com.mileway.core.ui.platform.LocalAnalyticsHelper
import com.mileway.core.ui.platform.LocalManagerProvider
import com.mileway.core.ui.platform.MaintenanceGate
import com.mileway.core.ui.platform.UpdateGate
import com.mileway.core.ui.platform.isUnderMaintenance
import com.mileway.core.ui.theme.AppLocaleEnvironment
import com.mileway.core.ui.theme.MilewayTheme
import com.mileway.core.ui.theme.ThemeController
import com.mileway.ui.MilewayAppRoot
import com.mileway.core.data.otp.LocalOtpEngine
import com.mileway.core.data.otp.OtpPurpose
import com.mileway.ui.auth.rememberPermissionsController
import com.mileway.ui.auth.CheckPinScreen
import com.mileway.ui.auth.LoginScreen
import com.mileway.ui.auth.OnboardingFormConfig
import com.mileway.ui.auth.OtpVerificationScreen
import com.mileway.ui.auth.SetPinScreen
import com.mileway.ui.auth.SignupOnboardingScreen
import com.mileway.ui.auth.SplashScreen
import com.mileway.ui.toAppRoute
import com.siddharth.kmp.appshell.AnalyticsEvent
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin
import org.koin.compose.koinInject

/**
 * App startup stages: splash → fake login → PLAN_V22 P7.4's local PIN/biometric gate → the
 * bottom-navigation shell. [PIN] sits between [LOGIN] and [APP] on every path into the app
 * (fresh sign-in and a returning already-signed-in session both pass through it), rendering
 * `SetPinScreen` the first time (`SessionState.hasPin == false`) or `CheckPinScreen` on every
 * later launch until sign-out.
 */
private enum class AppStage { SPLASH, LOGIN, MFA, PIN, ONBOARDING, APP }

/**
 * Single entry point for the app. Plays the splash and fake-login theatre, then hosts the
 * unified bottom-navigation shell ([MilewayAppRoot]).
 *
 * Handles `mileway://` deep links by mapping the host segment to a graph route and
 * forwarding it to [MilewayAppRoot] as [initialRoute].
 */
class LauncherActivity : ComponentActivity() {

    // PLAN_V35: ActivityResult bridge for the KMP PermissionsProvider — the shared tracking
    // screens request runtime permissions through it. One in-flight request at a time (the
    // system dialog is modal anyway).
    private var pendingGrants: kotlinx.coroutines.CompletableDeferred<Map<String, Boolean>>? = null
    private val permissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            pendingGrants?.complete(grants)
            pendingGrants = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySecurityFlags()
        enableEdgeToEdge()
        val permissionsProvider = getKoin().get<com.siddharth.kmp.appshell.PermissionsProvider>()
        (permissionsProvider as? com.mileway.core.platform.AndroidPermissionsProvider)?.requestBridge = { permissions ->
            val deferred = kotlinx.coroutines.CompletableDeferred<Map<String, Boolean>>()
            pendingGrants?.cancel()
            pendingGrants = deferred
            permissionLauncher.launch(permissions)
            deferred.await()
        }
        setContent { AppLocaleEnvironment { AppEntry(initialRoute = intent.deepLinkRoute()) } }
    }

    override fun onDestroy() {
        // Drop the bridge so a destroyed activity can't be resumed into; the provider degrades
        // to grant-state checks until the next activity re-registers.
        val provider = getKoin().get<com.siddharth.kmp.appshell.PermissionsProvider>()
        (provider as? com.mileway.core.platform.AndroidPermissionsProvider)?.requestBridge = null
        pendingGrants?.cancel()
        super.onDestroy()
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
        setContent { AppLocaleEnvironment { AppEntry(initialRoute = intent.deepLinkRoute()) } }
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
/**
 * P1.3: true when a signed-in session still owes its MFA step (not yet done, and not already past
 * it via a set PIN). Extracted so [nextStageForSession] stays under the complexity budget.
 */
private fun sessionOwesMfa(
    session: SessionState,
    mfaRequired: Boolean,
): Boolean = session.isSignedIn && mfaRequired && !session.mfaDone && !session.hasPin

/** A stage the app only reaches after login — used to bounce a signed-out session back to LOGIN. */
private fun AppStage.isPastLogin(): Boolean =
    this == AppStage.PIN || this == AppStage.APP || this == AppStage.MFA || this == AppStage.ONBOARDING

/** Stages where the "signed-in + has PIN → go to APP" reconciliation must NOT yank the user out. */
private fun AppStage.isMidAuthFlow(): Boolean = this == AppStage.PIN || this == AppStage.APP || this == AppStage.ONBOARDING

private fun nextStageForSession(
    session: SessionState?,
    currentStage: AppStage,
    mfaRequired: Boolean = false,
): AppStage? {
    if (session == null) return null
    return when {
        sessionOwesMfa(session, mfaRequired) && currentStage != AppStage.MFA && currentStage != AppStage.APP ->
            AppStage.MFA
        session.isSignedIn && session.hasPin && !currentStage.isMidAuthFlow() ->
            AppStage.APP
        session.isSignedIn && !session.hasPin && currentStage == AppStage.SPLASH -> AppStage.PIN
        !session.isSignedIn && currentStage.isPastLogin() -> AppStage.LOGIN
        else -> null
    }
}

@Composable
private fun AppEntry(
    initialRoute: String? = null,
    themeController: ThemeController = koinInject(),
    configProvider: ConfigProvider = koinInject(),
    sessionRepository: SessionRepository = koinInject(),
    pluginRegistry: PluginRegistry = koinInject(),
) {
    val sessionScope = rememberCoroutineScope()
    // PLAN_V24 P1.1/P1.2: phone-OTP login + via-call fallback, each gated by its plugin.
    val phoneLoginEnabled by pluginRegistry.observe("phoneLoginEnabled").collectAsStateWithLifecycle(initialValue = false)
    val otpViaCallEnabled by pluginRegistry.observe("otpViaCallEnabled").collectAsStateWithLifecycle(initialValue = false)
    // PLAN_V24 P1.3: MFA step gated by the `mfaRequired` plugin (Corporate Commuter persona).
    val mfaRequired by pluginRegistry.observe("mfaRequired").collectAsStateWithLifecycle(initialValue = false)
    // PLAN_V24 P2.1: signup onboarding form, gated + config-driven by the onboarding plugins.
    val signupOnboardingEnabled by pluginRegistry.observe("signupOnboardingEnabled").collectAsStateWithLifecycle(initialValue = false)
    val onboardingConfig =
        OnboardingFormConfig(
            lastNameOptional = pluginRegistry.observe("signupLastNameOptional").collectAsStateWithLifecycle(initialValue = true).value,
            emailOptional = pluginRegistry.observe("signupEmailOptional").collectAsStateWithLifecycle(initialValue = true).value,
            genderRequired = pluginRegistry.observe("genderRequired").collectAsStateWithLifecycle(initialValue = false).value,
            dobRequired = pluginRegistry.observe("dobRequired").collectAsStateWithLifecycle(initialValue = false).value,
            showPromo = pluginRegistry.observe("showPromoOnboarding").collectAsStateWithLifecycle(initialValue = false).value,
            showSkip = pluginRegistry.observe("showSkipOnboarding").collectAsStateWithLifecycle(initialValue = true).value,
        )
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
    LaunchedEffect(session, mfaRequired) {
        nextStageForSession(session, stage, mfaRequired)?.let { stage = it }
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
                                        // P1.3: credentials sign-in may owe an MFA step before the PIN gate.
                                        stage = if (mfaRequired) AppStage.MFA else AppStage.PIN
                                    },
                                    onContinueAsGuest = {
                                        sessionScope.launch { sessionRepository.continueAsGuest() }
                                        // Guests never do MFA (continueAsGuest marks mfaDone).
                                        stage = AppStage.PIN
                                    },
                                    onRequestPermissions = { permissionsController.request() },
                                    // P7.5: show WelcomeDisclaimerSheet once per install, persisted so
                                    // it never replays after this first shown-and-dismissed session.
                                    hasShownWelcomeDisclaimer = session?.hasShownWelcomeDisclaimer == true,
                                    onWelcomeDisclaimerShown = {
                                        sessionScope.launch { sessionRepository.markWelcomeDisclaimerShown() }
                                    },
                                    phoneLoginEnabled = phoneLoginEnabled,
                                    otpViaCallEnabled = otpViaCallEnabled,
                                )
                            AppStage.MFA ->
                                MfaStage(
                                    target = session?.email ?: DEMO_MFA_TARGET,
                                    otpViaCallEnabled = otpViaCallEnabled,
                                    onVerified = {
                                        sessionScope.launch { sessionRepository.markMfaDone() }
                                        stage = AppStage.PIN
                                    },
                                )
                            AppStage.PIN ->
                                PinGateStage(
                                    hasPin = session?.hasPin == true,
                                    onPassed = {
                                        // P2.1: a fresh persona that owes onboarding sees the form before the app.
                                        stage =
                                            if (signupOnboardingEnabled && session?.onboardingDone != true) {
                                                AppStage.ONBOARDING
                                            } else {
                                                AppStage.APP
                                            }
                                    },
                                )
                            AppStage.ONBOARDING ->
                                SignupOnboardingScreen(
                                    config = onboardingConfig,
                                    onComplete = { stage = AppStage.APP },
                                )
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

/** Fallback MFA target when a credentials session has no email (e.g. a demo persona). */
private const val DEMO_MFA_TARGET = "demo@mileway.app"

/**
 * PLAN_V24 P1.3: the MFA stage between LOGIN and PIN. Dispatches an MFA OTP once on entry (there is
 * no phone-entry step post-credential) and reuses the shared [OtpVerificationScreen] with
 * `purpose = MFA`. No change-number link — MFA has no number to change.
 */
@Composable
private fun MfaStage(
    target: String,
    otpViaCallEnabled: Boolean,
    onVerified: () -> Unit,
    otpEngine: LocalOtpEngine = koinInject(),
) {
    val delivery = remember(target) { otpEngine.send(OtpPurpose.MFA, target) }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        OtpVerificationScreen(
            purpose = OtpPurpose.MFA,
            target = target,
            delivery = delivery,
            otpViaCallEnabled = otpViaCallEnabled,
            showChangeNumber = false,
            onVerified = onVerified,
            onChangeNumber = {},
        )
    }
}
