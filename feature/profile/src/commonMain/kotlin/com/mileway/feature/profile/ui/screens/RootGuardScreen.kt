package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mileway.core.security.RootDetector
import com.mileway.core.ui.previews.PreviewSurface
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_root_cd_root
import com.mileway.core.ui.resources.profile_root_cd_secure
import com.mileway.core.ui.resources.profile_root_continue
import com.mileway.core.ui.resources.profile_root_continue_anyway
import com.mileway.core.ui.resources.profile_root_root_body
import com.mileway.core.ui.resources.profile_root_root_title
import com.mileway.core.ui.resources.profile_root_secure_body
import com.mileway.core.ui.resources.profile_root_secure_title
import com.mileway.core.ui.resources.profile_root_signal_item
import com.mileway.core.ui.resources.profile_root_signals_label
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.feature.profile.ui.previews.LightDarkPreview
import org.jetbrains.compose.resources.stringResource

@Composable
fun RootGuardScreen(
    onContinue: () -> Unit,
    signals: List<String> = RootDetector.check().signals,
) {
    val isClean = signals.isEmpty()
    val bgColor = if (isClean) MilewayColors.success else MilewayColors.danger

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(bgColor)
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (isClean) Icons.Default.GppGood else Icons.Default.GppBad,
            contentDescription = if (isClean) stringResource(Res.string.profile_root_cd_secure) else stringResource(Res.string.profile_root_cd_root),
            tint = Color.White,
            modifier = Modifier.size(96.dp),
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = if (isClean) stringResource(Res.string.profile_root_secure_title) else stringResource(Res.string.profile_root_root_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text =
                if (isClean) {
                    stringResource(Res.string.profile_root_secure_body)
                } else {
                    stringResource(Res.string.profile_root_root_body)
                },
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.87f),
            textAlign = TextAlign.Center,
        )

        if (!isClean) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(Res.string.profile_root_signals_label),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            signals.forEach { signal ->
                Text(
                    text = stringResource(Res.string.profile_root_signal_item, signal),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        Button(
            shape = DesignTokens.Shape.button,
            onClick = onContinue,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = bgColor,
                ),
        ) {
            Text(
                text = if (isClean) stringResource(Res.string.profile_root_continue) else stringResource(Res.string.profile_root_continue_anyway),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@LightDarkPreview
@Composable
private fun RootGuardScreenPreview() {
    PreviewSurface {
        RootGuardScreen(
            onContinue = {},
            signals = listOf("su binary found at /system/xbin/su", "test-keys build", "Magisk detected"),
        )
    }
}

@Preview(name = "Clean device", showBackground = true)
@Composable
private fun RootGuardScreenCleanPreview() {
    PreviewSurface {
        RootGuardScreen(
            onContinue = {},
            signals = emptyList(),
        )
    }
}
