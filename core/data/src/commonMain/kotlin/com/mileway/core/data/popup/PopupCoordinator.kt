package com.mileway.core.data.popup

/**
 * PLAN_V24 P13.3 — one pending forced-popup announcement. The reference app fired several stacked
 * login-driven dialogs (self-audit, digital-signature, agreement, subscription-expiry); Mileway
 * shows AT MOST ONE per app-open — a deliberate improvement — chosen by ascending [priority].
 *
 * @param id stable identity used for the persisted per-account acknowledgement.
 * @param priority ascending — the lowest-priority un-acknowledged candidate wins the single slot.
 */
data class PopupRequest(
    val id: String,
    val priority: Int,
) {
    companion object {
        // Priority ladder — lower shows first. Owning features gate whether a candidate is offered.
        const val SIGNATURE_RESIGN = 10
        const val SUBSCRIPTION_EXPIRING = 20
        const val SELF_AUDIT_DUE = 30
        const val WHATS_NEW = 40
        const val OFFER = 50

        const val ID_SIGNATURE_RESIGN = "popup_signature_resign"
        const val ID_OFFER = "popup_offer"
    }
}

/**
 * The pure coordinator: from the eligible [candidates] (each already plugin-gated by its owning
 * feature), drop any the account has already acknowledged, then return the single highest-priority
 * (lowest [PopupRequest.priority]) one — or null. Returning at most one is what enforces the
 * "one forced popup per app-open" rule. Side-effect-free so it is exhaustively unit-tested.
 */
object PopupCoordinator {
    fun next(
        candidates: List<PopupRequest>,
        acknowledgedIds: Set<String>,
    ): PopupRequest? =
        candidates
            .filterNot { it.id in acknowledgedIds }
            .minByOrNull { it.priority }
}
