package com.mileway.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.shared_assistant_input_placeholder
import com.mileway.core.ui.resources.shared_assistant_powered_by
import com.mileway.core.ui.resources.shared_assistant_title
import com.mileway.core.ui.resources.shared_cd_send
import com.mileway.feature.agent.ui.components.AgentMessageBubble
import com.mileway.feature.agent.ui.components.AgentStreamingBubble
import com.mileway.feature.agent.ui.components.AgentThinkingIndicator
import com.mileway.feature.agent.viewmodel.AgentAction
import com.mileway.feature.agent.viewmodel.AgentEffect
import com.mileway.feature.agent.viewmodel.AgentViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val SHEET_BG = Brush.verticalGradient(listOf(Color(0xFF0D1B2A), Color(0xFF1A237E)))
private val QUICK_ACTIONS = listOf("Summarise my week", "Why was my expense rejected?", "Plan a trip")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AssistantHomeSheet(
    onDismiss: () -> Unit,
    viewModel: AgentViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "cursorAlpha",
    )

    LaunchedEffect(uiState.messages.size, uiState.streamedText) {
        val count = uiState.messages.size + if (uiState.isStreaming) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AgentEffect.ScrollToBottom -> {
                    val s = viewModel.uiState.value
                    val count = s.messages.size + if (s.isStreaming) 1 else 0
                    if (count > 0) listState.animateScrollToItem(count - 1)
                }
                is AgentEffect.ShareTranscript -> Unit
                is AgentEffect.ShowSnackbar -> Unit
                is AgentEffect.FillInput -> Unit
            }
        }
    }

    fun sendMessage() {
        val text = inputText.trim()
        if (text.isBlank()) return
        inputText = ""
        viewModel.onAction(AgentAction.SendMessage(text))
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(SHEET_BG)
                    .navigationBarsPadding()
                    .imePadding(),
        ) {
            // Header
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column {
                    Text(
                        stringResource(Res.string.shared_assistant_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        stringResource(Res.string.shared_assistant_powered_by),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }

            // Quick action chips
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QUICK_ACTIONS.forEach { action ->
                    Surface(
                        onClick = {
                            inputText = ""
                            viewModel.onAction(AgentAction.SendMessage(action))
                        },
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text(
                            action,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Conversation — backed by real AgentViewModel
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.messages) { message ->
                    AgentMessageBubble(message = message, onFeedback = {})
                }
                if (uiState.isStreaming) {
                    item {
                        if (uiState.streamedText.isEmpty()) {
                            AgentThinkingIndicator(phrase = uiState.thinkingPhrase)
                        } else {
                            AgentStreamingBubble(text = uiState.streamedText, cursorAlpha = cursorAlpha)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Input row
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(stringResource(Res.string.shared_assistant_input_placeholder), color = Color.White.copy(alpha = 0.5f)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                )
                IconButton(
                    onClick = ::sendMessage,
                    enabled = inputText.isNotBlank() && !uiState.isStreaming,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(Res.string.shared_cd_send),
                        tint = if (inputText.isNotBlank()) Color.White else Color.White.copy(alpha = 0.4f),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
