package com.mileway.navgraph

import androidx.navigation3.runtime.NavKey
import com.github.skydoves.navgraph.annotations.NavGraphRoot
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppKey : NavKey

@NavGraphRoot
@Serializable
data object Home : AppKey

@Serializable
data object TrackMiles : AppKey

@Serializable
data object LogMiles : AppKey

@Serializable
data class TripDetail(val routeId: String) : AppKey

@Serializable
data object CheckInHistory : AppKey

@Serializable
data object Approvals : AppKey

@Serializable
data object Payables : AppKey

@Serializable
data object Travel : AppKey

@Serializable
data object Profile : AppKey

@Serializable
data object AgentChat : AppKey

@Serializable
data object DebugMenu : AppKey
