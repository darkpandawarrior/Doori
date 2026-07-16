package com.mileway.feature.whatsnew.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_cd_back
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.whatsnew.data.WhatsNewRepository
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * PLAN_V36 P3 — navigation destination + argument wiring only.
 *
 * ponytail: placeholder body, replaced in V36.P4 (`WhatsNewDetailViewModel` + `HeroCarousel` +
 * full spec §5.2 layout). This exists now so the [com.mileway.feature.whatsnew.ui.navigation.whatsNewGraph]
 * route, card taps and the digest sheet's row taps all navigate somewhere real today.
 */
@Composable
fun WhatsNewDetailScreen(
    entryId: String,
    onBack: () -> Unit,
    repository: WhatsNewRepository = koinInject(),
) {
    val entry = remember(entryId) { repository.entry(entryId) }
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(DesignTokens.Spacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.core_cd_back))
            }
        }
        Text(
            text = entry?.title ?: entryId,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = DesignTokens.Spacing.m),
        )
        if (entry != null) {
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = DesignTokens.Spacing.s),
            )
        }
    }
}
