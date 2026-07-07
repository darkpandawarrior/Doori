package com.mileway.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.otp.OtpDelivery
import com.mileway.core.data.otp.OtpPurpose
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.shared_auth_demo_code
import com.mileway.core.ui.resources.shared_otp_change_number
import com.mileway.core.ui.resources.shared_otp_error_expired
import com.mileway.core.ui.resources.shared_otp_error_locked
import com.mileway.core.ui.resources.shared_otp_error_wrong
import com.mileway.core.ui.resources.shared_otp_resend
import com.mileway.core.ui.resources.shared_otp_resend_in
import com.mileway.core.ui.resources.shared_otp_subtitle
import com.mileway.core.ui.resources.shared_otp_title
import com.mileway.core.ui.resources.shared_otp_via_call
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V24 P1.2 — the one OTP entry screen, reused for every purpose. Six boxes backed by a single
 * hidden [BasicTextField], a resend countdown (ring + seconds), a "Get OTP via call" secondary
 * action gated by [otpViaCallEnabled], wrong/expired/locked error states, and a change-number
 * link. Being an offline demo, the dispatched code is shown with a one-tap autofill (from P0.4's
 * delivery). Auto-submits when the sixth digit lands; [onVerified] fires on success.
 */
@Composable
fun OtpVerificationScreen(
    purpose: OtpPurpose,
    target: String,
    delivery: OtpDelivery?,
    onVerified: () -> Unit,
    onChangeNumber: () -> Unit,
    modifier: Modifier = Modifier,
    otpViaCallEnabled: Boolean = false,
    // P1.3: MFA has no number to change, so the host hides the link.
    showChangeNumber: Boolean = true,
    viewModel: OtpVerificationViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(purpose, target) {
        viewModel.start(purpose, target, delivery)
    }

    LaunchedEffect(state.verified) {
        if (state.verified) onVerified()
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.shared_otp_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(DesignTokens.Spacing.xs))
        Text(
            text = stringResource(Res.string.shared_otp_subtitle, target),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(DesignTokens.Spacing.l))

        OtpBoxes(
            code = state.code,
            isError = state.error != null,
            onCodeChange = viewModel::onCodeChange,
        )

        state.error?.let { error ->
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Text(
                text =
                    when (error) {
                        OtpError.WRONG -> stringResource(Res.string.shared_otp_error_wrong)
                        OtpError.EXPIRED -> stringResource(Res.string.shared_otp_error_expired)
                        OtpError.LOCKED -> stringResource(Res.string.shared_otp_error_locked)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(DesignTokens.Spacing.l))

        // Resend: a small countdown ring while cooling down, an enabled button once ready.
        if (state.resendInSeconds > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CountdownRing(remaining = state.resendInSeconds, total = 10)
                Spacer(Modifier.size(DesignTokens.Spacing.s))
                Text(
                    text = stringResource(Res.string.shared_otp_resend_in, state.resendInSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            TextButton(onClick = viewModel::resend) {
                Text(stringResource(Res.string.shared_otp_resend))
            }
        }

        if (otpViaCallEnabled) {
            TextButton(onClick = viewModel::requestViaCall) {
                Text(stringResource(Res.string.shared_otp_via_call))
            }
        }

        if (showChangeNumber) {
            TextButton(onClick = onChangeNumber) {
                Text(stringResource(Res.string.shared_otp_change_number))
            }
        }

        // Offline demo affordance: show + autofill the dispatched code.
        state.delivery?.let { d ->
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            TextButton(onClick = viewModel::autofillDemoCode) {
                Text(stringResource(Res.string.shared_auth_demo_code, d.code))
            }
        }
    }
}

/** Six digit boxes reading a single [code] string; a transparent [BasicTextField] captures input. */
@Composable
private fun OtpBoxes(
    code: String,
    isError: Boolean,
    onCodeChange: (String) -> Unit,
) {
    Box {
        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
            repeat(6) { index ->
                val char = code.getOrNull(index)?.toString() ?: ""
                Box(
                    modifier =
                        Modifier
                            .size(width = 44.dp, height = 52.dp)
                            .clip(DesignTokens.Shape.roundedSm)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color =
                                    if (isError) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                shape = DesignTokens.Shape.roundedSm,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = char,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        BasicTextField(
            value = code,
            onValueChange = onCodeChange,
            modifier = Modifier.matchParentSize(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            // Fully transparent: the boxes above are the visible affordance.
            textStyle = LocalTextStyle.current.merge(TextStyle(color = androidx.compose.ui.graphics.Color.Transparent)),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}

/** A small circular countdown, sweeping from full to empty as the resend cooldown ticks down. */
@Composable
private fun CountdownRing(
    remaining: Int,
    total: Int,
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
        Canvas(modifier = Modifier.size(28.dp)) {
            val stroke = 3.dp.toPx()
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * (remaining.toFloat() / total.toFloat()).coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}
