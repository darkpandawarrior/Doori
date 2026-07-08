package com.mileway.feature.tracking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.dao.NotificationDao
import com.mileway.core.data.emergency.EmergencyContact
import com.mileway.core.data.emergency.EmergencyContactsRepository
import com.mileway.core.data.model.db.NotificationEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * PLAN_V24 P3.5: backs the tracking SOS sheet. Exposes the shared emergency-contacts list (from
 * core:data's [EmergencyContactsRepository]) and, on "Send SOS", logs a real Notification Centre
 * entry via [NotificationDao] — the demo's "we notified your contacts" affordance, no network.
 *
 * Reaches core:data directly rather than feature:profile so tracking keeps no feature→feature
 * dependency (the repository and NotificationDao both live in core:data).
 */
class SosViewModel(
    private val emergencyContactsRepository: EmergencyContactsRepository,
    private val notificationDao: NotificationDao,
    private val clock: Clock = Clock.System,
) : ViewModel() {
    val contacts: StateFlow<List<EmergencyContact>> =
        emergencyContactsRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Records that an SOS was fired: inserts a Notification Centre entry (type SYSTEM). The
     * [title]/[body]/[timeLabel] are localized by the caller so the persisted row matches the
     * active language. Fire-and-forget — the sheet shows its own confirmation.
     */
    fun logAlert(
        title: String,
        body: String,
        timeLabel: String,
    ) {
        viewModelScope.launch {
            val now = clock.now().toEpochMilliseconds()
            notificationDao.upsertAll(
                listOf(
                    NotificationEntity(
                        id = "SOS-$now",
                        title = title,
                        body = body,
                        relativeTime = timeLabel,
                        isUnread = true,
                        type = "SYSTEM",
                        createdAtMs = now,
                    ),
                ),
            )
        }
    }
}
