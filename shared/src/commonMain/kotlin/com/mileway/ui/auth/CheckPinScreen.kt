package com.mileway.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.platform.BiometricAuthenticator
import com.mileway.core.platform.BiometricResult
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.shared_auth_enter_pin
import com.mileway.core.ui.resources.shared_auth_locked_out
import com.mileway.core.ui.resources.shared_auth_unlock
import com.mileway.core.ui.resources.shared_auth_unlock_subtitle
import com.mileway.core.ui.resources.shared_auth_use_biometrics
import com.mileway.core.ui.theme.DesignTokens
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V22 P7.4: PIN/biometric gate shown on every app launch after the first (once
 * [com.mileway.core.data.session.SessionState.hasPin] is true), between `AppStage.LOGIN` and
 * `AppStage.APP`. Layers [BiometricAuthenticator] as an optional first attempt — offered
 * automatically when the device has biometrics enrolled — before falling back to PIN entry, mirroring the
 * reference app's biometric-first UX *pattern* locally (own visuals, no `CryptoObject`: there is
 * no secret this demo needs biometrics to unlock, only friction to reproduce).
 *
 * @param onUnlocked invoked once either biometric auth or the correct PIN succeeds.
 */
@Composable
fun CheckPinScreen(
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PinViewModel = koinViewModel(),
) {
    val currentOnUnlocked by rememberUpdatedState(onUnlocked)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val biometric = koinInject<BiometricAuthenticator>()
    val scope = rememberCoroutineScope()
    val biometricAvailable = remember { biometric.isAvailable() }
    var biometricOffered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.reset() }
    LaunchedEffect(state.completed) {
        if (state.completed) currentOnUnlocked()
    }

    // Offer biometrics exactly once per screen composition, before the reviewer has to type
    // anything, if the device actually has biometrics enrolled — otherwise this falls straight
    // through to PIN entry.
    LaunchedEffect(biometricAvailable) {
        if (biometricOffered || !biometricAvailable) return@LaunchedEffect
        biometricOffered = true
        scope.launch {
            when (biometric.authenticate(reason = "Use your fingerprint or face to continue")) {
                is BiometricResult.Success -> currentOnUnlocked()
                else -> { /* fall through to PIN entry, no error copy needed for a cancelled prompt */ }
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = DesignTokens.Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PinLockBadge()

            Spacer(Modifier.height(DesignTokens.Spacing.l))
            Text(
                text = stringResource(Res.string.shared_auth_enter_pin),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            Text(
                text = stringResource(Res.string.shared_auth_unlock_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(DesignTokens.Spacing.xl))
            PinEntryDots(enteredCount = state.digits.length, isError = state.error != null)

            Spacer(Modifier.height(DesignTokens.Spacing.m))
            val statusText =
                when {
                    state.isLockedOut -> stringResource(Res.string.shared_auth_locked_out)
                    state.error != null -> state.error.orEmpty()
                    else -> " "
                }
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = if (state.error != null || state.isLockedOut) DesignTokens.StatusColors.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(DesignTokens.Spacing.xl))
            PinEntryKeypad(
                enabled = !state.isLockedOut,
                onDigit = viewModel::onDigitEntered,
                onBackspace = viewModel::onBackspace,
            )

            Spacer(Modifier.height(DesignTokens.Spacing.xl))
            Button(
                onClick = viewModel::verify,
                enabled = !state.isLockedOut && state.digits.length == LOGIN_PIN_LENGTH,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = DesignTokens.Shape.roundedSm,
                colors = ButtonDefaults.buttonColors(),
            ) {
                Text(stringResource(Res.string.shared_auth_unlock), fontWeight = FontWeight.Bold)
            }

            if (biometricAvailable) {
                Spacer(Modifier.height(DesignTokens.Spacing.m))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            when (biometric.authenticate(reason = "Use your fingerprint or face to continue")) {
                                is BiometricResult.Success -> currentOnUnlocked()
                                else -> {}
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = DesignTokens.Shape.roundedSm,
                ) {
                    Icon(imageVector = Icons.Filled.Fingerprint, contentDescription = null)
                    Spacer(Modifier.width(DesignTokens.Spacing.s))
                    Text(stringResource(Res.string.shared_auth_use_biometrics), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
