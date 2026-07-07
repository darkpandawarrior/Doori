package com.mileway.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.ui.theme.DesignTokens
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V22 P7.4: first-time PIN setup, shown once between `AppStage.LOGIN` and `AppStage.APP`
 * when [com.mileway.core.data.session.SessionState.hasPin] is false. Own full-screen design
 * (dot indicators + numeric keypad on the Matrix/terminal surface, matching
 * [com.mileway.feature.profile.ui.screens.SwitchAccountPinSheet]'s idiom), not a port of the
 * reference app's lock-screen UI — the reference app's *pattern* (PIN entry, then re-entry to
 * confirm) is what carries over, not its visuals.
 *
 * Two steps in one composable, driven by [PinUiState.isConfirmStep]: enter a 4-digit PIN, then
 * re-enter it to confirm. A mismatch resets only the confirm step, not the whole flow.
 *
 * @param onSkip lets a reviewer skip PIN setup entirely (proceeds straight to `AppStage.APP`,
 *   leaving [com.mileway.core.data.session.SessionState.hasPin] false — the next launch will show
 *   this screen again rather than [CheckPinScreen]).
 */
@Composable
fun SetPinScreen(
    onCompleted: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PinViewModel = koinViewModel(),
) {
    val currentOnCompleted by rememberUpdatedState(onCompleted)
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.reset() }
    LaunchedEffect(state.completed) {
        if (state.completed) currentOnCompleted()
    }

    val activeDigits = if (state.isConfirmStep) state.confirmDigits else state.digits
    val title = if (state.isConfirmStep) "Confirm your PIN" else "Set a PIN"
    val subtitle =
        if (state.isConfirmStep) {
            "Re-enter your 4-digit PIN to confirm"
        } else {
            "Choose a 4-digit PIN to protect this app"
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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(DesignTokens.Spacing.xl))
            PinEntryDots(enteredCount = activeDigits.length, isError = state.error != null)

            Spacer(Modifier.height(DesignTokens.Spacing.m))
            Text(
                text = state.error ?: " ",
                style = MaterialTheme.typography.labelMedium,
                color = if (state.error != null) DesignTokens.StatusColors.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(DesignTokens.Spacing.xl))
            PinEntryKeypad(
                enabled = true,
                onDigit = viewModel::onDigitEntered,
                onBackspace = viewModel::onBackspace,
            )

            Spacer(Modifier.height(DesignTokens.Spacing.xl))
            Button(
                onClick = { if (state.isConfirmStep) viewModel.confirmSetPin() else viewModel.onProceedToConfirm() },
                enabled = activeDigits.length == LOGIN_PIN_LENGTH,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = DesignTokens.Shape.roundedSm,
            ) {
                Text(if (state.isConfirmStep) "Confirm" else "Continue", fontWeight = FontWeight.Bold)
            }

            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text("Skip for now")
            }
        }
    }
}

/** Rounded icon badge shared by [SetPinScreen] and [CheckPinScreen]. */
@Composable
internal fun PinLockBadge() {
    Box(
        modifier =
            Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(DesignTokens.IconSize.actionTileLarge),
        )
    }
}

/** Dot-indicator row shared by [SetPinScreen] and [CheckPinScreen]. */
@Composable
internal fun PinEntryDots(
    enteredCount: Int,
    isError: Boolean,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
        repeat(LOGIN_PIN_LENGTH) { index ->
            val filled = index < enteredCount
            val color =
                when {
                    isError -> DesignTokens.StatusColors.error
                    filled -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                }
            Box(
                modifier =
                    Modifier
                        .size(14.dp)
                        .background(color, CircleShape),
            )
        }
    }
}

private val PinKeypadRows =
    listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
    )

/** Numeric keypad shared by [SetPinScreen] and [CheckPinScreen]. */
@Composable
internal fun PinEntryKeypad(
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
        PinKeypadRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l)) {
                row.forEach { digit -> PinKeypadKey(label = digit.toString(), enabled = enabled) { onDigit(digit) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l)) {
            Spacer(Modifier.size(56.dp))
            PinKeypadKey(label = "0", enabled = enabled) { onDigit('0') }
            PinKeypadKey(label = "⌫", enabled = enabled, onClick = onBackspace)
        }
    }
}

@Composable
private fun PinKeypadKey(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                .let { base -> if (enabled) base.clickable(onClick = onClick) else base },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}
