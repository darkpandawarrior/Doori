package com.mileway

import android.content.Intent
import android.os.Bundle
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
import com.mileway.core.network.config.ConfigProvider
import kotlinx.coroutines.launch
import com.mileway.core.ui.platform.LocalManagerProvider
import com.mileway.core.ui.platform.MaintenanceGate
import com.mileway.core.ui.platform.UpdateGate
import com.mileway.core.ui.platform.isUnderMaintenance
import com.mileway.core.ui.theme.MilewayTheme
import com.mileway.core.ui.theme.ThemeController
import com.mileway.ui.MilewayAppRoot
import com.mileway.ui.auth.LoginScreen
import com.mileway.ui.auth.SplashScreen
import com.mileway.ui.toAppRoute
import org.koin.compose.koinInject

/** App startup stages: splash → fake login → the bottom-navigation shell. */
private enum class AppStage { SPLASH, LOGIN, APP }

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
        enableEdgeToEdge()
        setContent { AppEntry(initialRoute = intent.deepLinkRoute()) }
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

    // A.1: once the persisted session resolves as already-signed-in, skip splash + login entirely.
    // This makes a guest session survive process recreation and lets deep links resolve for a guest
    // without bouncing back to the login screen. A signed-out user falls through to the normal flow.
    LaunchedEffect(session) {
        val current = session
        if (current != null && current.isSignedIn && stage != AppStage.APP) {
            stage = AppStage.APP
        }
    }

    // PF.4: seed the Activity-scoped platform managers (in-app update / review / share / …) so any
    // shared screen can read them via `LocalAppReviewManager.current` with no expect/actual at the call site.
    LocalManagerProvider {
        MilewayTheme(
            darkTheme = override ?: systemDark,
            milewayTheme = milewayTheme,
            palette = palette,
            customSeedHex = customSeedHex,
            useSystemColors = useSystemColors,
            paletteStyle = paletteStyle,
        ) {
            MaintenanceGate(underMaintenance = underMaintenance) {
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
                                onSignInWithCredentials = { email ->
                                    sessionScope.launch { sessionRepository.signInWithCredentials(email) }
                                    stage = AppStage.APP
                                },
                                onContinueAsGuest = {
                                    sessionScope.launch { sessionRepository.continueAsGuest() }
                                    stage = AppStage.APP
                                },
                            )
                        AppStage.APP ->
                            UpdateGate(config = updateConfig) {
                                MilewayAppRoot(deepLinkRoute = initialRoute)
                            }
                    }
                }
            }
        }
    }
}
