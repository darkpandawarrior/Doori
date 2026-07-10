package com.mileway.feature.travel.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.StatusTone
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.travel_booking_bus
import com.mileway.core.ui.resources.travel_booking_flight
import com.mileway.core.ui.resources.travel_booking_hotel
import com.mileway.core.ui.resources.travel_booking_mjp
import com.mileway.core.ui.resources.travel_booking_visa
import com.mileway.core.ui.resources.travel_chip_business
import com.mileway.core.ui.resources.travel_chip_deluxe
import com.mileway.core.ui.resources.travel_chip_economy
import com.mileway.core.ui.resources.travel_chip_premium
import com.mileway.core.ui.resources.travel_chip_seater
import com.mileway.core.ui.resources.travel_chip_semi_sleeper
import com.mileway.core.ui.resources.travel_chip_sleeper
import com.mileway.core.ui.resources.travel_chip_standard
import com.mileway.core.ui.resources.travel_chip_suite
import com.mileway.core.ui.resources.travel_chip_tourist
import com.mileway.core.ui.resources.travel_chip_transit
import com.mileway.core.ui.resources.travel_status_approved
import com.mileway.core.ui.resources.travel_status_completed
import com.mileway.core.ui.resources.travel_status_pending
import com.mileway.core.ui.resources.travel_status_rejected
import com.mileway.core.ui.resources.travel_toast_awaiting_desk
import com.mileway.core.ui.resources.travel_toast_policy_violations
import com.mileway.core.ui.resources.travel_toast_reference
import com.mileway.core.ui.resources.travel_toast_sent_for_approval
import com.mileway.core.ui.resources.travel_toast_submitted_title
import com.mileway.core.ui.toast.ToastType
import com.mileway.core.ui.toast.Toasts
import com.mileway.feature.travel.model.BookingType
import com.mileway.feature.travel.model.TravelReqStatus
import com.mileway.feature.travel.viewmodel.TravelCreateEffect
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Shared effect collection for every TR create screen, routes the rotating success / approval / violation
 * outcomes to the app-wide [Toasts] and forwards a submitted id to [onSubmitted]. The [noun] phrases the toast
 * per flow ("Trip request", "Flight booking", …).
 */
@Composable
fun HandleTravelCreateEffects(
    effects: Flow<TravelCreateEffect>,
    noun: String,
    onSubmitted: (id: String) -> Unit,
) {
    val submittedTitle = stringResource(Res.string.travel_toast_submitted_title, noun)
    val sentForApprovalTitle = stringResource(Res.string.travel_toast_sent_for_approval)
    val policyViolationsTitle = stringResource(Res.string.travel_toast_policy_violations)
    LaunchedEffect(Unit) {
        effects.collect { effect ->
            when (effect) {
                is TravelCreateEffect.Success -> {
                    Toasts.show(submittedTitle, getString(Res.string.travel_toast_reference, effect.id), ToastType.Success)
                    onSubmitted(effect.id)
                }
                is TravelCreateEffect.NeedsApproval -> {
                    Toasts.show(sentForApprovalTitle, getString(Res.string.travel_toast_awaiting_desk, effect.id), ToastType.Info)
                    onSubmitted(effect.id)
                }
                is TravelCreateEffect.Violation ->
                    Toasts.show(policyViolationsTitle, effect.messages.joinToString(" · "), ToastType.Warning)
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

/** Localized display label for a travel-request status; the enum's `label` stays canonical for search. */
@Composable
internal fun TravelReqStatus.localizedLabel(): String =
    when (this) {
        TravelReqStatus.PENDING -> stringResource(Res.string.travel_status_pending)
        TravelReqStatus.APPROVED -> stringResource(Res.string.travel_status_approved)
        TravelReqStatus.REJECTED -> stringResource(Res.string.travel_status_rejected)
        TravelReqStatus.COMPLETED -> stringResource(Res.string.travel_status_completed)
    }

/** Localized display label for a booking type; the enum's `label` stays canonical for search. */
@Composable
internal fun BookingType.localizedLabel(): String =
    when (this) {
        BookingType.FLIGHT -> stringResource(Res.string.travel_booking_flight)
        BookingType.BUS -> stringResource(Res.string.travel_booking_bus)
        BookingType.HOTEL -> stringResource(Res.string.travel_booking_hotel)
        BookingType.MJP -> stringResource(Res.string.travel_booking_mjp)
        BookingType.VISA -> stringResource(Res.string.travel_booking_visa)
    }

/**
 * Localized display label for the raw chip-option values used by the TR create forms (seat/room/cabin/visa
 * preference chips). The stored/compared value in each screen's ViewModel state stays the canonical English
 * word; only the rendered chip text is localized.
 */
@Composable
internal fun travelChipLabel(value: String): String =
    when (value) {
        "Seater" -> stringResource(Res.string.travel_chip_seater)
        "Sleeper" -> stringResource(Res.string.travel_chip_sleeper)
        "Semi-sleeper" -> stringResource(Res.string.travel_chip_semi_sleeper)
        "Business" -> stringResource(Res.string.travel_chip_business)
        "Tourist" -> stringResource(Res.string.travel_chip_tourist)
        "Transit" -> stringResource(Res.string.travel_chip_transit)
        "Standard" -> stringResource(Res.string.travel_chip_standard)
        "Deluxe" -> stringResource(Res.string.travel_chip_deluxe)
        "Suite" -> stringResource(Res.string.travel_chip_suite)
        "Economy" -> stringResource(Res.string.travel_chip_economy)
        "Premium" -> stringResource(Res.string.travel_chip_premium)
        else -> value
    }
