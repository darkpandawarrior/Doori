package com.mileway.core.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Platform-neutral [DeepLinkHandler] (DL.3): the platform entry point (Android Intent / iOS onOpenURL /
 * NSUserActivity) calls [handle]; nav observes [incoming] and routes each raw URI through
 * `DeepLinkRouter`. Replay=1 so a link delivered before the collector attaches (cold start) is not lost.
 */
class DefaultDeepLinkHandler : DeepLinkHandler {
    private val mutableIncoming = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 8)

    override val incoming: Flow<String> = mutableIncoming.asSharedFlow()

    override fun handle(url: String) {
        mutableIncoming.tryEmit(url)
    }
}
