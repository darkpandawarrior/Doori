package com.mileway.core.network.routing

import com.siddharth.kmp.network.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.get
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

private const val MaximumLatitude = 90.0
private const val MaximumLongitude = 180.0
private const val MetresPerKm = 1_000.0

/** Session configuration. No public routing host is selected automatically. */
class OsrmConfiguration(
    var baseUrl: String? = null,
)

/** A routing endpoint; protected system places must remain on the device. */
data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val protected: Boolean = false,
) {
    init {
        require(latitude.isFinite() && latitude in -MaximumLatitude..MaximumLatitude)
        require(longitude.isFinite() && longitude in -MaximumLongitude..MaximumLongitude)
    }
}

/** Unavailable routes require user-entered distance, never a straight-line routing claim. */
sealed interface RouteEstimate {
    data class Routed(
        val distanceKm: Double,
    ) : RouteEstimate

    data class ManualRequired(
        val reason: String,
    ) : RouteEstimate
}

/** OSRM route/v1 (BSD-2). Distance is returned in metres by the self-hosted service. */
class OsrmClient(
    private val configuration: OsrmConfiguration,
    private val clientProvider: () -> HttpClient = {
        // Coordinates must not enter the factory's default request logger.
        createHttpClient(
            retry = false,
            logger =
                object : Logger {
                    override fun log(message: String) = Unit
                },
        )
    },
) {
    private val client by lazy(clientProvider)

    /** Guards privacy and configuration before creating the platform HTTP engine. */
    suspend fun route(
        origin: RoutePoint,
        destination: RoutePoint,
        roundTrip: Boolean = false,
    ): RouteEstimate {
        if (origin.protected || destination.protected) return RouteEstimate.ManualRequired("Home is protected; enter distance manually (approximate)")
        val base = configuration.baseUrl?.trim()?.trimEnd('/')
        if (base.isNullOrBlank()) return RouteEstimate.ManualRequired("Routing is unconfigured; enter distance manually (approximate)")
        return try {
            if (!validServer(base)) return RouteEstimate.ManualRequired("Invalid routing server; enter distance manually (approximate)")
            val points = listOf(origin, destination) + if (roundTrip) listOf(origin) else emptyList()
            val coordinates = points.joinToString(";") { "${it.longitude},${it.latitude}" }
            val response =
                client
                    .get("$base/route/v1/driving/$coordinates") {
                        url {
                            parameters.append("overview", "false")
                            parameters.append("steps", "false")
                        }
                    }.body<OsrmResponse>()
            val metres = response.routes.firstOrNull()?.distance
            val distance = metres?.takeIf { it.isFinite() && it > 0.0 }
            if (response.code == "Ok" && distance != null) {
                RouteEstimate.Routed(distance / MetresPerKm)
            } else {
                RouteEstimate.ManualRequired("No route match; enter distance manually (approximate)")
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            RouteEstimate.ManualRequired("Routing is unavailable; enter distance manually (approximate)")
        }
    }
    private fun validServer(base: String): Boolean {
        if (!(base.startsWith("http://") || base.startsWith("https://"))) return false
        val url = Url(base)
        if (url.protocol !in listOf(URLProtocol.HTTP, URLProtocol.HTTPS) || url.host.isBlank()) return false
        if (!url.user.isNullOrEmpty() || !url.password.isNullOrEmpty()) return false
        return url.parameters.names().isEmpty() && url.fragment.isEmpty()
    }

}

@Serializable
private data class OsrmResponse(
    val code: String,
    val routes: List<OsrmRoute> = emptyList(),
)

@Serializable
private data class OsrmRoute(
    val distance: Double,
)
