package com.mileway.feature.advances.ui.previews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mileway.core.ui.previews.PreviewLightDark
import com.mileway.core.ui.previews.PreviewSurface
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.advances.data.AdvancesMockData
import com.mileway.feature.advances.ui.components.PettyAdvanceCardFace
import com.mileway.feature.advances.ui.components.QrCardFace

// ---------------------------------------------------------------------------
// feature:advances had 40 composables and no previews. These two card faces are
// the module's only composables reused across more than one screen (the home list
// and both detail screens render them), and both are driven by `cardHealth`, which
// resolves to one of three tones from a balance ratio.
//
// The gallery already captures advances_history_screen.png, but a screen capture
// pins one card to one health state. The health ladder — and the progress bar
// colour that follows it — is only visible with all three stacked.
//
// Data is AdvancesMockData, the same list the screens render in demo mode, so
// these previews cannot drift from what the app shows. No new sample fixtures.
// ---------------------------------------------------------------------------

/**
 * All three [com.mileway.feature.advances.model.CardHealth] tones from the real mock kit:
 * 64% remaining (Active), 30% (Low balance), 8% (Critical).
 */
@PreviewLightDark
@Composable
fun PreviewPettyAdvanceCardFaceHealthLadder() {
    PreviewSurface {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            AdvancesMockData.activePettyCards.forEach { PettyAdvanceCardFace(card = it) }
        }
    }
}

/**
 * The QR face's scan shortcut is disabled at zero balance, so the two mock cards are
 * rendered beside a zero-balance copy — the only way to see the disabled tile.
 */
@PreviewLightDark
@Composable
fun PreviewQrCardFaceStates() {
    val cards = AdvancesMockData.activeQrCards
    PreviewSurface {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            cards.forEach { QrCardFace(card = it, onScan = {}) }
            cards.first().copy(balance = 0.0).let { QrCardFace(card = it, onScan = {}) }
        }
    }
}
