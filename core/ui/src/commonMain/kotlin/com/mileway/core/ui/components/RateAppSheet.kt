package com.mileway.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.rate_app_dismiss
import com.mileway.core.ui.resources.rate_app_star
import com.mileway.core.ui.resources.rate_app_submit
import com.mileway.core.ui.resources.rate_app_subtitle
import com.mileway.core.ui.resources.rate_app_thanks
import com.mileway.core.ui.resources.rate_app_title
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/**
 * PLAN_V24 P12.3: the native "Rate Mileway" sheet — a self-contained star-rating bottom sheet with
 * no Play In-App Review SDK dependency (the offline demo can't call Play). Purely local: a rating
 * shows a thank-you and dismisses; [onSubmit] reports the star count so the caller can log it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateAppSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onSubmit: (Int) -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState()
    var rating by remember { mutableStateOf(0) }
    var submitted by remember { mutableStateOf(false) }

    if (submitted) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1200)
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            if (submitted) {
                Text(
                    stringResource(Res.string.rate_app_thanks),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                Text(
                    stringResource(Res.string.rate_app_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(Res.string.rate_app_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (star in 1..5) {
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = stringResource(Res.string.rate_app_star, star),
                                tint = if (star <= rating) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        if (rating > 0) {
                            onSubmit(rating)
                            submitted = true
                        }
                    },
                    enabled = rating > 0,
                    shape = DesignTokens.Shape.button,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.rate_app_submit))
                }
                TextButton(onClick = onDismiss, shape = DesignTokens.Shape.button) {
                    Text(stringResource(Res.string.rate_app_dismiss))
                }
            }
        }
    }
}
