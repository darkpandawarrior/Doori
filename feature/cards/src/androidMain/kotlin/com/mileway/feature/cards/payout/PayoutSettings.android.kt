package com.mileway.feature.cards.payout

import com.mileway.core.network.payout.PayoutBeneficiaryStore
import com.siddharth.kmp.settings.SecureSettingsFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.scope.Scope

internal actual fun Scope.payoutBeneficiaryStore(): PayoutBeneficiaryStore = PayoutBeneficiaryStore(SecureSettingsFactory(androidContext()).create())
