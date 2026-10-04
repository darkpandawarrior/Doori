package com.mileway.feature.cards.payout

import com.mileway.core.network.payout.PayoutBeneficiaryStore
import org.koin.core.scope.Scope

/** Uses the same toolkit secure-settings factory as the platform AuthTokenStore binding. */
internal expect fun Scope.payoutBeneficiaryStore(): PayoutBeneficiaryStore
