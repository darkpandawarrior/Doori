package com.miletracker.core.ui.components.pickers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.components.sheet.AppActionSheet
import dev.darkokoa.datetimewheelpicker.WheelDatePicker
import dev.darkokoa.datetimewheelpicker.WheelTimePicker
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

private val systemTz: TimeZone get() = TimeZone.currentSystemDefault()

private fun Long.toLocalDate(): LocalDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(systemTz).date

private fun LocalDate.toEpochMillis(): Long = atStartOfDayIn(systemTz).toEpochMilliseconds()

/**
 * A spinning-WHEEL date picker in a modal bottom sheet (multiplatform — Android + iOS), backed by
 * darkokoa/datetime-wheel-picker. Drop-in replacement for the Material3 dial/calendar date dialog;
 * the public signature is unchanged so existing call sites keep working (project convention is bottom
 * sheets over dialogs — matches Dice's WheelDatePickerBottomSheet).
 */
@Composable
fun WheelDatePickerDialog(
    initialDateMillis: Long?,
    onConfirm: (millis: Long) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select date",
) {
    val startDate =
        remember(initialDateMillis) {
            (initialDateMillis ?: Clock.System.now().toEpochMilliseconds()).toLocalDate()
        }
    var snapped by remember { mutableStateOf(startDate) }
    AppActionSheet(onDismiss = onDismiss, title = title) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            WheelDatePicker(
                startDate = startDate,
                onSnappedDate = { snapped = it },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = { onConfirm(snapped.toEpochMillis()) },
                modifier = Modifier.weight(1f),
            ) { Text("OK") }
        }
    }
}

/**
 * A spinning-WHEEL time picker in a modal bottom sheet (multiplatform). Takes/returns minutes-since-midnight
 * as hour + minute. Drop-in replacement for the Material3 dial time dialog; the public signature is unchanged.
 */
@Composable
fun WheelTimePickerDialog(
    initialMinutes: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select time",
) {
    val startTime =
        remember(initialMinutes) {
            LocalTime(
                hour = (initialMinutes / 60).coerceIn(0, 23),
                minute = (initialMinutes % 60).coerceIn(0, 59),
            )
        }
    var snapped by remember { mutableStateOf(startTime) }
    AppActionSheet(onDismiss = onDismiss, title = title) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            WheelTimePicker(
                startTime = startTime,
                onSnappedTime = { snapped = it },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = { onConfirm(snapped.hour, snapped.minute) },
                modifier = Modifier.weight(1f),
            ) { Text("OK") }
        }
    }
}
