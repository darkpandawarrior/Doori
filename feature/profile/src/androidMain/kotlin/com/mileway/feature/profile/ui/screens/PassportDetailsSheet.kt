package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.pickers.WheelDatePickerDialog
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_passport_country
import com.mileway.core.ui.resources.profile_passport_expiry_title
import com.mileway.core.ui.resources.profile_passport_number
import com.mileway.core.ui.resources.profile_passport_title
import com.mileway.core.ui.resources.profile_vehicle_save
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.model.PassportDetails
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

private val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun formatDate(ms: Long): String =
    if (ms <= 0L) {
        "Select expiry date"
    } else {
        Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault()).let { ldt ->
            "${ldt.dayOfMonth} ${MONTHS[ldt.monthNumber - 1]} ${ldt.year}"
        }
    }

/**
 * PLAN_V22 P6.2: add/edit sheet for the Passport tile — Room-backed via
 * [PersonalDetailsViewModel][com.mileway.feature.profile.viewmodel.PersonalDetailsViewModel] so
 * edits persist across restart. Own Matrix/terminal bottom-sheet layout, not a port of any
 * reference app UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassportDetailsSheet(
    initial: PassportDetails?,
    onSave: (PassportDetails) -> Unit,
    onDismiss: () -> Unit,
) {
    val existing = initial ?: PassportDetails.EMPTY
    var passportNumber by remember { mutableStateOf(existing.passportNumber) }
    var issuingCountry by remember { mutableStateOf(existing.issuingCountry) }
    var expiryMillis by remember { mutableStateOf(existing.expiryDateMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = DesignTokens.Spacing.xl)
                    .padding(bottom = DesignTokens.Spacing.xl, top = DesignTokens.Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Text(
                text = stringResource(Res.string.profile_passport_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = passportNumber,
                onValueChange = { passportNumber = it },
                label = { Text(stringResource(Res.string.profile_passport_number)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = issuingCountry,
                onValueChange = { issuingCountry = it },
                label = { Text(stringResource(Res.string.profile_passport_country)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(formatDate(expiryMillis))
            }

            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Button(
                onClick = {
                    onSave(
                        PassportDetails(
                            passportNumber = passportNumber.trim(),
                            issuingCountry = issuingCountry.trim(),
                            expiryDateMillis = expiryMillis,
                        ),
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(),
            ) {
                Text(stringResource(Res.string.profile_vehicle_save), fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDatePicker) {
        WheelDatePickerDialog(
            initialDateMillis = expiryMillis.takeIf { it > 0L },
            onConfirm = { millis ->
                expiryMillis = millis
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            title = stringResource(Res.string.profile_passport_expiry_title),
        )
    }
}
