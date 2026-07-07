package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_vehicle_fuel_type
import com.mileway.core.ui.resources.profile_vehicle_make
import com.mileway.core.ui.resources.profile_vehicle_model
import com.mileway.core.ui.resources.profile_vehicle_registration
import com.mileway.core.ui.resources.profile_vehicle_save
import com.mileway.core.ui.resources.profile_vehicle_seating
import com.mileway.core.ui.resources.profile_vehicle_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.model.VehicleDetails
import org.jetbrains.compose.resources.stringResource

/**
 * PLAN_V22 P6.2: add/edit sheet for the Vehicle tile — Room-backed via
 * [PersonalDetailsViewModel][com.mileway.feature.profile.viewmodel.PersonalDetailsViewModel] so
 * edits persist across restart. Own Matrix/terminal bottom-sheet layout (matches
 * [AccountDetailsSheet]'s idiom), not a port of any reference app UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailsSheet(
    initial: VehicleDetails?,
    onSave: (VehicleDetails) -> Unit,
    onDismiss: () -> Unit,
) {
    val existing = initial ?: VehicleDetails.EMPTY
    var make by remember { mutableStateOf(existing.make) }
    var model by remember { mutableStateOf(existing.model) }
    var registration by remember { mutableStateOf(existing.registrationNumber) }
    var fuelType by remember { mutableStateOf(existing.fuelType) }
    var seatingText by remember { mutableStateOf(if (existing.seatingCapacity > 0) existing.seatingCapacity.toString() else "") }

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
                text = stringResource(Res.string.profile_vehicle_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = make,
                onValueChange = { make = it },
                label = { Text(stringResource(Res.string.profile_vehicle_make)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text(stringResource(Res.string.profile_vehicle_model)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = registration,
                onValueChange = { registration = it },
                label = { Text(stringResource(Res.string.profile_vehicle_registration)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = fuelType,
                onValueChange = { fuelType = it },
                label = { Text(stringResource(Res.string.profile_vehicle_fuel_type)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = seatingText,
                onValueChange = { seatingText = it.filter(Char::isDigit) },
                label = { Text(stringResource(Res.string.profile_vehicle_seating)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Button(
                onClick = {
                    onSave(
                        VehicleDetails(
                            make = make.trim(),
                            model = model.trim(),
                            registrationNumber = registration.trim(),
                            fuelType = fuelType.trim(),
                            seatingCapacity = seatingText.toIntOrNull() ?: 0,
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
}
