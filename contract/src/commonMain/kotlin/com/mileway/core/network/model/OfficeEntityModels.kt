package com.mileway.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A registered office a mileage submission can be billed against. */
@Serializable
data class Office(
    @SerialName("code") val code: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("address") val address: String = "",
    @SerialName("gstin") val gstin: String = "",
)

/** A business entity (legal company) that owns vouchers and transactions. */
@Serializable
data class BusinessEntity(
    @SerialName("name") val name: String = "",
    @SerialName("country") val country: String = "",
    @SerialName("currencySymbol") val currencySymbol: String = "",
)
