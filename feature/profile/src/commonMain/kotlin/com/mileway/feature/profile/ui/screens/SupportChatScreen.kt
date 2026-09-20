package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.support_chat_disclaimer
import com.mileway.core.ui.resources.support_chat_greeting_bot
import com.mileway.core.ui.resources.support_chat_input_hint
import com.mileway.core.ui.resources.support_chat_reply_account
import com.mileway.core.ui.resources.support_chat_reply_expense
import com.mileway.core.ui.resources.support_chat_reply_fallback
import com.mileway.core.ui.resources.support_chat_reply_greeting
import com.mileway.core.ui.resources.support_chat_reply_permissions
import com.mileway.core.ui.resources.support_chat_reply_thanks
import com.mileway.core.ui.resources.support_chat_reply_ticket
import com.mileway.core.ui.resources.support_chat_reply_tracking
import com.mileway.core.ui.resources.support_chat_send
import com.mileway.core.ui.resources.support_chat_subtitle
import com.mileway.core.ui.resources.support_chat_title
import com.mileway.core.ui.resources.support_hub_back
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.profile.support.SupportChatResponder
import com.mileway.feature.profile.support.SupportChatTopic
import org.jetbrains.compose.resources.stringResource

private data class ChatMessage(val text: String, val fromUser: Boolean)

/**
 * PLAN_V24 P12.2: the in-app support chat channel. A deterministic canned-response bot — the user's
 * message is classified by [SupportChatResponder] and answered with the matching localized reply.
 *
 * ponytail: chat history lives in composable state (ephemeral) and there is no real agent — the bot
 * is a keyword matcher (see [SupportChatResponder]'s doc for the ceiling + upgrade path). For a
 * persisted transcript, back this with a Room table; for real answers, swap the classifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportChatScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val greeting = stringResource(Res.string.support_chat_greeting_bot)
    val messages = remember { listOf(ChatMessage(greeting, fromUser = false)).toMutableStateList() }
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val replyFor: @Composable (SupportChatTopic) -> String = { topic ->
        when (topic) {
            SupportChatTopic.GREETING -> stringResource(Res.string.support_chat_reply_greeting)
            SupportChatTopic.TRACKING -> stringResource(Res.string.support_chat_reply_tracking)
            SupportChatTopic.EXPENSE -> stringResource(Res.string.support_chat_reply_expense)
            SupportChatTopic.ACCOUNT -> stringResource(Res.string.support_chat_reply_account)
            SupportChatTopic.PERMISSIONS -> stringResource(Res.string.support_chat_reply_permissions)
            SupportChatTopic.TICKET -> stringResource(Res.string.support_chat_reply_ticket)
            SupportChatTopic.THANKS -> stringResource(Res.string.support_chat_reply_thanks)
            SupportChatTopic.FALLBACK -> stringResource(Res.string.support_chat_reply_fallback)
        }
    }
    // Pre-resolve all replies (stringResource must be called in composition, not in the send lambda).
    val replies = SupportChatTopic.entries.associateWith { replyFor(it) }

    LaunchedEffect(Unit) {
        snapshotFlow { messages.size }.collect {
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty()) return
        messages.add(ChatMessage(text, fromUser = true))
        val topic = SupportChatResponder.classify(text)
        messages.add(ChatMessage(replies.getValue(topic), fromUser = false))
        input = ""
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.support_chat_title),
                subtitle = stringResource(Res.string.support_chat_subtitle),
                depth = NavigationDepth.LEVEL_2,
                titleIcon = Icons.AutoMirrored.Filled.Chat,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.support_hub_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            ) {
                items(messages.size) { index -> ChatBubble(messages[index]) }
            }
            Text(
                stringResource(Res.string.support_chat_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = DesignTokens.Spacing.l),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(stringResource(Res.string.support_chat_input_hint)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = DesignTokens.Shape.roundedSm,
                )
                IconButton(onClick = { send() }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(Res.string.support_chat_send))
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.fromUser) Alignment.CenterEnd else Alignment.CenterStart
    val container =
        if (message.fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val content =
        if (message.fromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(shape = DesignTokens.Shape.roundedMd, color = container, modifier = Modifier.widthIn(max = 280.dp)) {
            Text(
                message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = content,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}
