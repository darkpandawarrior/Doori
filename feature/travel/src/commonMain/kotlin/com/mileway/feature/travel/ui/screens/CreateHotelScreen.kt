package com.mileway.feature.travel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.SectionCard
import com.mileway.core.ui.components.scaffold.FormSubmissionScaffold
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.travel_create_hotel_subtitle
import com.mileway.core.ui.resources.travel_guests_required
import com.mileway.feature.travel.viewmodel.CreateHotelAction
import com.mileway.feature.travel.viewmodel.CreateHotelViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val ROOM_PREFERENCES = listOf("Standard", "Deluxe", "Suite")

/** TR.5: Add Hotel booking request, built on the shared F0.1 FormSubmissionScaffold + SectionCards. */
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
        subtitle = stringResource(Res.string.travel_create_hotel_subtitle),
        titleIcon = Icons.Filled.Hotel,
        onBack = onBack,
        onSubmit = { viewModel.onAction(CreateHotelAction.Submit) },
        modifier = modifier,
        canSubmit = ui.canSubmit,
        isSubmitting = ui.isSubmitting,
        submitLabel = "Request hotel",
        submitIcon = Icons.Filled.Check,
    ) { contentPadding ->
        TravelFormBody(contentPadding) {
            SectionCard(title = "Stay", leadingIcon = null) {
                TravelField("City *", ui.city) { viewModel.onAction(CreateHotelAction.SetCity(it)) }
                TravelField("Check-in date *", ui.checkInDate) { viewModel.onAction(CreateHotelAction.SetCheckInDate(it)) }
                TravelField("Check-out date *", ui.checkOutDate) { viewModel.onAction(CreateHotelAction.SetCheckOutDate(it)) }
                OutlinedTextField(
                    value = ui.guestsText,
                    onValueChange = { viewModel.onAction(CreateHotelAction.SetGuests(it)) },
                    label = { Text(stringResource(Res.string.travel_guests_required)) },
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
