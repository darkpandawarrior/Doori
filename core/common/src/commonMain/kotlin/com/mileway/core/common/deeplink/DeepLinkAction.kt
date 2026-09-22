package com.mileway.core.common.deeplink

/**
 * DL.5: the tracking control-op surface reachable from an external caller (partner integration,
 * OS Shortcuts/App Intents, iOS URL scheme) — distinct from [DeepLinkTarget], which only routes to
 * a *screen*. These carry an imperative verb straight to [com.mileway.feature.tracking.manager.TrackingController]
 * (or the check-in screen for [CheckIn]), and a caller expects a result back (see
 * `DeepLinkResultActivity` on Android).
 */
sealed interface DeepLinkAction {
    data object Start : DeepLinkAction

    data object Stop : DeepLinkAction

    data object Pause : DeepLinkAction

    data object Discard : DeepLinkAction

    data object CheckIn : DeepLinkAction

    /** Unrecognised or malformed action link; dispatch is a safe no-op. */
    data class Unknown(
        val raw: String,
    ) : DeepLinkAction
}

/**
 * Pure URI → [DeepLinkAction] resolver for the `mileway://track/<verb>` action links (both the
 * custom scheme and the `https://mileway.example.com/track/<verb>` App Link form, mirroring
 * [DeepLinkRouter]'s two-scheme handling). Kept separate from [DeepLinkRouter.resolve] because
 * these five segments dispatch an *operation*, not a navigation route.
 */
object DeepLinkActionDispatcher {
    private val VERB_TO_ACTION =
        mapOf(
            "start" to DeepLinkAction.Start,
            "stop" to DeepLinkAction.Stop,
            "pause" to DeepLinkAction.Pause,
            "discard" to DeepLinkAction.Discard,
            "checkin" to DeepLinkAction.CheckIn,
        )

    fun resolve(uri: String): DeepLinkAction {
        val parsed = DeepLinkRouter.parse(uri) ?: return DeepLinkAction.Unknown(uri)
        val segments =
            when (parsed.scheme) {
                "mileway" -> (listOf(parsed.host) + parsed.path.split("/"))
                else -> parsed.path.split("/")
            }.filter { it.isNotBlank() }

        if (segments.size != 2 || segments[0] != "track") return DeepLinkAction.Unknown(uri)
        return VERB_TO_ACTION[segments[1]] ?: DeepLinkAction.Unknown(uri)
    }

    /** Stop/Discard end the active trip irreversibly — those two require a confirmation dialog. */
    fun requiresConfirmation(action: DeepLinkAction): Boolean = action is DeepLinkAction.Stop || action is DeepLinkAction.Discard
}
