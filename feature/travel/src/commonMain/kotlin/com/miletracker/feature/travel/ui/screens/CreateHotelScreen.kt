package com.miletracker.feature.travel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.components.SectionCard
import com.miletracker.core.ui.components.scaffold.FormSubmissionScaffold
import com.miletracker.feature.travel.viewmodel.CreateHotelAction
import com.miletracker.feature.travel.viewmodel.CreateHotelViewModel
import org.koin.compose.viewmodel.koinViewModel

private val ROOM_PREFERENCES = listOf("Standard", "Deluxe", "Suite")

/** TR.5 — Add Hotel booking request, built on the shared F0.1 FormSubmissionScaffold + SectionCards. */
@Composable
fun CreateHotelScreen(
    onBack: () -> Unit,
    onSubmitted: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateHotelViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    HandleTravelCreateEffects(viewModel.effect, noun = "Hotel booking", onSubmitted = onSubmitted)

    FormSubmissionScaffold(
        title = "Add Hotel",
        onBack = onBack,
        onSubmit = { viewModel.onAction(CreateHotelAction.Submit) },
        modifier = modifier,
        canSubmit = ui.canSubmit,
        isSubmitting = ui.isSubmitting,
        submitLabel = "Request hotel",
    ) { contentPadding ->
        TravelFormBody(contentPadding) {
            SectionCard(title = "Stay", leadingIcon = null) {
                TravelField("City *", ui.city) { viewModel.onAction(CreateHotelAction.SetCity(it)) }
                TravelField("Check-in date *", ui.checkInDate) { viewModel.onAction(CreateHotelAction.SetCheckInDate(it)) }
                TravelField("Check-out date *", ui.checkOutDate) { viewModel.onAction(CreateHotelAction.SetCheckOutDate(it)) }
                OutlinedTextField(
                    value = ui.guestsText,
                    onValueChange = { viewModel.onAction(CreateHotelAction.SetGuests(it)) },
                    label = { Text("Guests *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                )
            }
            SectionCard(title = "Room", leadingIcon = null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ROOM_PREFERENCES.forEach { room ->
                        FilterChip(
                            selected = ui.roomPreference == room,
                            onClick = { viewModel.onAction(CreateHotelAction.SetRoomPreference(room)) },
                            label = { Text(room) },
                        )
                    }
                }
            }
        }
    }
}
