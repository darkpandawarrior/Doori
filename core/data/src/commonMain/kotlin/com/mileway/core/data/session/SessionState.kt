package com.mileway.core.data.session

/** How the current session was established. */
enum class SessionKind { NONE, CREDENTIALS, GUEST }

/** Persisted sign-in state. [isSignedIn] is true once the user has signed in (any kind). */
data class SessionState(
    val kind: SessionKind = SessionKind.NONE,
    val email: String? = null,
) {
    val isSignedIn: Boolean get() = kind != SessionKind.NONE
    val isGuest: Boolean get() = kind == SessionKind.GUEST
}
