package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.coupon.Coupon
import com.mileway.core.data.coupon.CouponApplyResult
import com.mileway.core.data.coupon.CouponStatus
import com.mileway.core.data.dao.NotificationDao
import com.mileway.core.data.model.db.NotificationEntity
import com.mileway.feature.profile.repository.CouponsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * PLAN_V24 P5.2: drives `CouponsScreen` — active vs inactive (expired/redeemed) sections and the
 * "Have a code?" apply entry with the invalid/expired/already-used/success outcomes. A successful
 * redeem logs a Notification Centre entry ([logRedeem], called by the screen with localized text).
 */
data class CouponsUiState(
    val active: List<Coupon> = emptyList(),
    val inactive: List<Coupon> = emptyList(),
    val code: String = "",
    val applyResult: CouponApplyResult? = null,
) {
    val canApply: Boolean get() = code.isNotBlank()
}

class CouponsViewModel(
    private val repository: CouponsRepository,
    private val notificationDao: NotificationDao,
    private val clock: Clock = Clock.System,
) : ViewModel() {
    private val _state = MutableStateFlow(CouponsUiState())
    val state: StateFlow<CouponsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { repository.seedIfEmpty() }
        repository.observeAll()
            .onEach { coupons ->
                _state.update {
                    it.copy(
                        active = coupons.filter { c -> c.status == CouponStatus.ACTIVE },
                        inactive = coupons.filter { c -> c.status != CouponStatus.ACTIVE },
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onCodeChange(value: String) {
        _state.update { it.copy(code = value, applyResult = null) }
    }

    fun applyCode() {
        val code = _state.value.code
        if (code.isBlank()) return
        viewModelScope.launch {
            val result = repository.applyCode(code)
            _state.update { it.copy(applyResult = result, code = if (result == CouponApplyResult.SUCCESS) "" else it.code) }
        }
    }

    fun clearResult() {
        _state.update { it.copy(applyResult = null) }
    }

    /** Logs a redeem to the Notification Centre — called by the screen after a SUCCESS with localized text. */
    fun logRedeem(
        title: String,
        body: String,
        timeLabel: String,
    ) {
        viewModelScope.launch {
            val now = clock.now().toEpochMilliseconds()
            notificationDao.upsertAll(
                listOf(
                    NotificationEntity(
                        id = "CPN-$now",
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
