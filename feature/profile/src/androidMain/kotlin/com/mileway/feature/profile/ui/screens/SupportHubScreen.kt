package com.mileway.feature.profile.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.support_channel_call_subtitle
import com.mileway.core.ui.resources.support_channel_call_title
import com.mileway.core.ui.resources.support_channel_chat_subtitle
import com.mileway.core.ui.resources.support_channel_chat_title
import com.mileway.core.ui.resources.support_channel_faq_subtitle
import com.mileway.core.ui.resources.support_channel_faq_title
import com.mileway.core.ui.resources.support_channel_mail_subtitle
import com.mileway.core.ui.resources.support_channel_mail_title
import com.mileway.core.ui.resources.support_channel_tickets_subtitle
import com.mileway.core.ui.resources.support_channel_tickets_title
import com.mileway.core.ui.resources.support_channel_tour_subtitle
import com.mileway.core.ui.resources.support_channel_tour_title
import com.mileway.core.ui.resources.support_hub_back
import com.mileway.core.ui.resources.support_hub_empty
import com.mileway.core.ui.resources.support_hub_subtitle
import com.mileway.core.ui.resources.support_hub_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private const val SUPPORT_PHONE = "tel:+18005550100"
private const val SUPPORT_MAIL = "mailto:support@mileway.app"

/**
 * PLAN_V24 P12.2: the unified Support hub replacing the bare Help entry. Each channel — FAQ, My
 * tickets, Chat, Call, Email — is its own registry plugin, so a persona exposes a different support
 * mix. FAQ/tickets/chat navigate to their screens; Call/Mail fire platform intents. All local; the
 * chat channel is a deterministic canned-response bot (see [SupportChatScreen]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportHubScreen(
    onBack: () -> Unit,
    onOpenFaq: () -> Unit,
    onOpenTickets: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenTour: () -> Unit,
    modifier: Modifier = Modifier,
    pluginRegistry: PluginRegistry = koinInject(),
) {
    val context = LocalContext.current
    val faqOn by pluginRegistry.observe("faqSupport").collectAsStateWithLifecycle(initialValue = true)
    val ticketOn by pluginRegistry.observe("ticketSupport").collectAsStateWithLifecycle(initialValue = true)
    val chatOn by pluginRegistry.observe("chatSupport").collectAsStateWithLifecycle(initialValue = false)
    val callOn by pluginRegistry.observe("callSupport").collectAsStateWithLifecycle(initialValue = true)
    val mailOn by pluginRegistry.observe("mailSupport").collectAsStateWithLifecycle(initialValue = true)
    // P12.5: training-tour re-entry — only when the tour plugin is on (Gig Driver persona).
    val tourOn by pluginRegistry.observe("trainingTour").collectAsStateWithLifecycle(initialValue = false)

    Scaffold(
        modifier = modifier,
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.support_hub_title),
                subtitle = stringResource(Res.string.support_hub_subtitle),
                depth = NavigationDepth.LEVEL_1,
                titleIcon = Icons.AutoMirrored.Filled.HelpOutline,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.support_hub_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            val anyOn = faqOn || ticketOn || chatOn || callOn || mailOn || tourOn
            if (!anyOn) {
                Text(
                    stringResource(Res.string.support_hub_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (faqOn) {
                SupportChannelRow(
                    Icons.AutoMirrored.Filled.HelpOutline,
                    Color(0xFF2563EB),
                    stringResource(Res.string.support_channel_faq_title),
                    stringResource(Res.string.support_channel_faq_subtitle),
                    onOpenFaq,
                )
            }
            if (ticketOn) {
                SupportChannelRow(
                    Icons.Filled.ConfirmationNumber,
                    Color(0xFF7C3AED),
                    stringResource(Res.string.support_channel_tickets_title),
                    stringResource(Res.string.support_channel_tickets_subtitle),
                    onOpenTickets,
                )
            }
            if (chatOn) {
                SupportChannelRow(
                    Icons.AutoMirrored.Filled.Chat,
                    Color(0xFF16A34A),
                    stringResource(Res.string.support_channel_chat_title),
                    stringResource(Res.string.support_channel_chat_subtitle),
                    onOpenChat,
                )
            }
            if (callOn) {
                SupportChannelRow(
                    Icons.Filled.Call,
                    Color(0xFFEA580C),
                    stringResource(Res.string.support_channel_call_title),
                    stringResource(Res.string.support_channel_call_subtitle),
                ) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(SUPPORT_PHONE))) }
                }
            }
            if (mailOn) {
                SupportChannelRow(
                    Icons.Filled.Email,
                    Color(0xFF0F766E),
                    stringResource(Res.string.support_channel_mail_title),
                    stringResource(Res.string.support_channel_mail_subtitle),
                ) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(SUPPORT_MAIL))) }
                }
            }
            if (tourOn) {
                SupportChannelRow(
                    Icons.Filled.School,
                    Color(0xFFD97706),
                    stringResource(Res.string.support_channel_tour_title),
                    stringResource(Res.string.support_channel_tour_subtitle),
                    onOpenTour,
                )
            }
        }
    }
}

@Composable
private fun SupportChannelRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = DesignTokens.Shape.roundedMd,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
        ) {
            Surface(shape = CircleShape, color = accent.copy(alpha = 0.15f), modifier = Modifier.size(40.dp)) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
