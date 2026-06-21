package com.miletracker.feature.travel.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.components.StatusTone
import com.miletracker.core.ui.toast.ToastType
import com.miletracker.core.ui.toast.Toasts
import com.miletracker.feature.travel.model.TravelReqStatus
import com.miletracker.feature.travel.viewmodel.TravelCreateEffect
import kotlinx.coroutines.flow.Flow

/**
 * Shared effect collection for every TR create screen — routes the rotating success / approval / violation
 * outcomes to the app-wide [Toasts] and forwards a submitted id to [onSubmitted]. The [noun] phrases the toast
 * per flow ("Trip request", "Flight booking", …).
 */
@Composable
fun HandleTravelCreateEffects(
    effects: Flow<TravelCreateEffect>,
    noun: String,
    onSubmitted: (id: String) -> Unit,
) {
    LaunchedEffect(Unit) {
        effects.collect { effect ->
            when (effect) {
                is TravelCreateEffect.Success -> {
                    Toasts.show("$noun submitted", "Reference ${effect.id}", ToastType.Success)
                    onSubmitted(effect.id)
                }
                is TravelCreateEffect.NeedsApproval -> {
                    Toasts.show("Sent for approval", "${effect.id} is awaiting the travel desk", ToastType.Info)
                    onSubmitted(effect.id)
                }
                is TravelCreateEffect.Violation ->
                    Toasts.show("Policy violations", effect.messages.joinToString(" · "), ToastType.Warning)
            }
        }
    }
}

/** Shared single-line text field for the TR create forms. */
@Composable
internal fun TravelField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}

/** Shared form body column padding wrapper for the TR create forms. */
@Composable
internal fun TravelFormBody(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(contentPadding).padding(16.dp)) {
        content()
    }
}

/** Shared StatusChip tone mapping for the TR history surfaces (TR.8). */
internal fun travelStatusTone(status: TravelReqStatus): StatusTone =
    when (status) {
        TravelReqStatus.PENDING -> StatusTone.Warning
        TravelReqStatus.APPROVED -> StatusTone.Success
        TravelReqStatus.REJECTED -> StatusTone.Error
        TravelReqStatus.COMPLETED -> StatusTone.Info
    }
