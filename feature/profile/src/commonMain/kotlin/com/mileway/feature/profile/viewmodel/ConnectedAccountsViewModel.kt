package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.otp.LocalOtpEngine
import com.mileway.core.data.otp.OtpPurpose
import com.mileway.core.data.otp.OtpVerifyResult
import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.feature.profile.model.ConnectedAccount
import com.mileway.feature.profile.model.PaymentWallet
import com.mileway.feature.profile.repository.ConnectedAccountsRepository
import com.mileway.feature.profile.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** An in-progress "link this wallet" OTP challenge (Paytm/Mobikwik). Non-null shows the OTP sheet. */
data class WalletLinkFlow(
    val walletId: String,
    val providerName: String,
    val target: String,
    val demoCode: String,
    val code: String = "",
    val wrongCode: Boolean = false,
)

/**
 * PLAN_V22 P6.6 + PLAN_V24 P8.1: state for
 * [com.mileway.feature.profile.ui.screens.ConnectedAccountsScreen] — the connect/disconnect
 * integrations list plus (when [walletsEnabled]) the external payment-wallet section with its
 * offline OTP link flow.
 */
data class ConnectedAccountsUiState(
    val accounts: List<ConnectedAccount> = emptyList(),
    val walletsEnabled: Boolean = false,
    val wallets: List<PaymentWallet> = emptyList(),
    val linkFlow: WalletLinkFlow? = null,
)

class ConnectedAccountsViewModel(
    private val repository: ConnectedAccountsRepository,
    private val walletRepository: WalletRepository,
    private val otpEngine: LocalOtpEngine,
    pluginRegistry: PluginRegistry,
) : ViewModel() {
    private val _state = MutableStateFlow(ConnectedAccountsUiState())
    val state: StateFlow<ConnectedAccountsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedIfEmpty()
            walletRepository.seedIfEmpty()
        }
        repository.observeAll().onEach { list -> _state.update { it.copy(accounts = list) } }.launchIn(viewModelScope)
        combine(
            walletRepository.observeAll(),
            pluginRegistry.observe("walletLinkingEnabled"),
        ) { wallets, enabled -> wallets to enabled }
            .onEach { (wallets, enabled) ->
                _state.update { it.copy(wallets = wallets, walletsEnabled = enabled) }
            }
            .launchIn(viewModelScope)
    }

    /** Toggles a connected-account [id]'s state — a local flag flip only, never a real network call. */
    fun toggle(
        id: String,
        isConnected: Boolean,
    ) {
        viewModelScope.launch { repository.setConnected(id, isConnected) }
    }

    // ── P8.1: wallet link flow (offline OTP) ───────────────────────────────────

    /** Begin linking [wallet]: dispatch an OTP to its registered mobile and open the verify sheet. */
    fun startLink(wallet: PaymentWallet) {
        otpEngine.send(OtpPurpose.WALLET_LINK, wallet.mobile)
        _state.update {
            it.copy(
                linkFlow =
                    WalletLinkFlow(
                        walletId = wallet.id,
                        providerName = wallet.providerName,
                        target = wallet.mobile,
                        demoCode = otpEngine.codeFor(OtpPurpose.WALLET_LINK, wallet.mobile),
                    ),
            )
        }
    }

    fun onLinkCodeChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(6)
        _state.update { it.copy(linkFlow = it.linkFlow?.copy(code = digits, wrongCode = false)) }
        if (digits.length == 6) verifyLink()
    }

    /** Verify the entered code; on success persist the link and close the sheet. */
    fun verifyLink() {
        val flow = _state.value.linkFlow ?: return
        if (otpEngine.verify(OtpPurpose.WALLET_LINK, flow.target, flow.code) == OtpVerifyResult.Success) {
            viewModelScope.launch { walletRepository.setLinked(flow.walletId, true) }
            _state.update { it.copy(linkFlow = null) }
        } else {
            _state.update { it.copy(linkFlow = it.linkFlow?.copy(wrongCode = true)) }
        }
    }

    /** Dismiss the OTP sheet without linking. */
    fun cancelLink() {
        _state.update { it.copy(linkFlow = null) }
    }

    /** Unlink a previously linked wallet (confirmed at the UI). */
    fun unlink(id: String) {
        viewModelScope.launch { walletRepository.setLinked(id, false) }
    }
}
