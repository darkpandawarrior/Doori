package com.miletracker.core.common.deeplink

/**
 * DL.1 — typed deep-link destinations. Covers the existing `home|track|log|profile` plus the new
 * `track/checkin`, `log/expense`, `profile/settings`, and `referral?code=…` targets.
 */
sealed interface DeepLinkTarget {
    data object Home : DeepLinkTarget

    data object Track : DeepLinkTarget

    data object TrackCheckIn : DeepLinkTarget

    data object Log : DeepLinkTarget

    data object LogExpense : DeepLinkTarget

    data object Profile : DeepLinkTarget

    data object ProfileSettings : DeepLinkTarget

    data class Referral(val code: String?) : DeepLinkTarget

    /** Unrecognised link; callers typically ignore it or fall back to Home. */
    data class Unknown(val raw: String) : DeepLinkTarget
}

/** A minimally-parsed URI (commonMain-pure — no java.net.URI / NSURL). */
internal data class ParsedUri(
    val scheme: String,
    val host: String,
    val path: String,
    val query: Map<String, String>,
)

/**
 * Pure URI → [DeepLinkTarget] resolver. Handles BOTH the custom `miletracker://<section>/…` scheme (where
 * the host carries the first section) and `https://<domain>/<section>/…` App/Universal Links (where the
 * path carries it), so the two platforms feed it the same way.
 */
object DeepLinkRouter {
    fun resolve(uri: String): DeepLinkTarget {
        val parsed = parse(uri) ?: return DeepLinkTarget.Unknown(uri)
        val segments =
            when (parsed.scheme) {
                "miletracker" -> (listOf(parsed.host) + parsed.path.split("/"))
                else -> parsed.path.split("/")
            }.filter { it.isNotBlank() }

        return when {
            segments.isEmpty() || segments == listOf("home") -> DeepLinkTarget.Home
            segments == listOf("track") -> DeepLinkTarget.Track
            segments == listOf("track", "checkin") -> DeepLinkTarget.TrackCheckIn
            segments == listOf("log") -> DeepLinkTarget.Log
            segments == listOf("log", "expense") -> DeepLinkTarget.LogExpense
            segments == listOf("profile") -> DeepLinkTarget.Profile
            segments == listOf("profile", "settings") -> DeepLinkTarget.ProfileSettings
            segments.firstOrNull() == "referral" -> DeepLinkTarget.Referral(parsed.query["code"])
            else -> DeepLinkTarget.Unknown(uri)
        }
    }

    internal fun parse(uri: String): ParsedUri? {
        val schemeSplit = uri.indexOf("://")
        if (schemeSplit <= 0) return null
        val scheme = uri.substring(0, schemeSplit).lowercase()
        val rest = uri.substring(schemeSplit + 3)

        val queryStart = rest.indexOf('?')
        val beforeQuery = if (queryStart >= 0) rest.substring(0, queryStart) else rest
        val queryString = if (queryStart >= 0) rest.substring(queryStart + 1) else ""

        val pathStart = beforeQuery.indexOf('/')
        val host = if (pathStart >= 0) beforeQuery.substring(0, pathStart) else beforeQuery
        val path = if (pathStart >= 0) beforeQuery.substring(pathStart) else ""

        val query =
            queryString
                .split('&')
                .filter { it.isNotBlank() }
                .mapNotNull { pair ->
                    val eq = pair.indexOf('=')
                    if (eq < 0) null else pair.substring(0, eq) to pair.substring(eq + 1)
                }.toMap()

        return ParsedUri(scheme = scheme, host = host.lowercase(), path = path, query = query)
    }
}

/**
 * Whitelist guard for incoming links: only the app's own custom scheme and verified App/Universal-Links
 * domain are honoured. Anything else is rejected before routing (prevents open-redirect-style abuse).
 */
object DeepLinkValidator {
    private const val CUSTOM_SCHEME = "miletracker"
    private val ALLOWED_HTTPS_HOSTS = setOf("miletracker.example.com")

    fun isAllowed(uri: String): Boolean {
        val parsed = DeepLinkRouter.parse(uri) ?: return false
        return when (parsed.scheme) {
            CUSTOM_SCHEME -> true
            "https" -> parsed.host in ALLOWED_HTTPS_HOSTS
            else -> false
        }
    }
}
