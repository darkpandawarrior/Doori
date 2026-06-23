@file:Suppress("ktlint:standard:function-naming")

package com.miletracker.feature.agent.ui.screens

import com.miletracker.feature.agent.ui.components.VoiceWaveformOverlay
import com.miletracker.feature.agent.ui.components.WaveformState
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.components.sheet.AppActionSheet
import com.miletracker.core.ui.theme.MilewayColors
import com.miletracker.feature.agent.model.AgentMessage
import com.miletracker.feature.agent.model.PopularQuestion
import com.miletracker.feature.agent.model.UnansweredQuestion
import com.miletracker.feature.agent.ui.components.AgentMessageBubble
import com.miletracker.feature.agent.ui.components.AgentStreamingBubble
import com.miletracker.feature.agent.ui.components.AgentThinkingIndicator
import com.miletracker.feature.agent.viewmodel.AgentAction
import com.miletracker.feature.agent.viewmodel.AgentEffect
import com.miletracker.feature.agent.viewmodel.AgentViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

private val AI_GRADIENT = Brush.verticalGradient(listOf(Color(0xFF0D1B2A), Color(0xFF1A237E)))
private val CHIP_BG = Color(0x22FFFFFF)
private val QUICK_PROMPTS =
    listOf(
        "Summarise my week",
        "Why was EXP-003 rejected?",
        "How much have I spent on travel?",
        "Check mileage policy",
        "Plan a trip to Delhi",
        "Pending approvals count",
    )

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AgentChatScreen(
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: AgentViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var inputText by remember { mutableStateOf("") }
    var showQuestionDialog by remember { mutableStateOf(false) }
    var dialogText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var micPulsing by remember { mutableStateOf(false) }

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
                is AgentEffect.ShareTranscript -> Unit // handled in P4.2
                is AgentEffect.ShowSnackbar -> scope.launch { snackbarState.showSnackbar(effect.text) }
                is AgentEffect.FillInput -> inputText = effect.text
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "cursorAlpha",
    )
    val micScale by rememberInfiniteTransition(label = "mic").animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(300, easing = LinearEasing), RepeatMode.Reverse),
        label = "micScale",
    )

    fun sendText(text: String) {
        if (text.isBlank()) return
        inputText = ""
        viewModel.onAction(AgentAction.SendMessage(text))
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarState) },
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize().background(AI_GRADIENT)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Custom header
                AgentHeader(
                    onBack = onBack,
                    onHistory = onOpenHistory,
                    onDownload = { scope.launch { snackbarState.showSnackbar("Export available in full version") } },
                )

                // Tab row
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                ) {
                    listOf("CHAT", "POPULAR", "UNANSWERED").forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                }

                when (selectedTab) {
                    0 ->
                        ChatTab(
                            messages = uiState.messages,
                            isStreaming = uiState.isStreaming,
                            streamedText = uiState.streamedText,
                            thinkingPhrase = uiState.thinkingPhrase,
                            cursorAlpha = cursorAlpha,
                            listState = listState,
                            inputText = inputText,
                            onInputChange = { inputText = it },
                            onSend = { sendText(inputText) },
                            onChipClicked = { sendText(it) },
                            onMic = {
                                if (uiState.isListening) {
                                    viewModel.onAction(AgentAction.StopVoice)
                                } else {
                                    micPulsing = true
                                    viewModel.onAction(AgentAction.StartVoice)
                                }
                            },
                            onToggleVoiceConversation = { viewModel.onAction(AgentAction.ToggleVoiceConversation) },
                            onFeedback = { msgId, rating ->
                                viewModel.onAction(AgentAction.SubmitFeedback(msgId, rating))
                            },
                            feedbackMap = uiState.feedback,
                            micPulsing = micPulsing,
                            micScale = micScale,
                            isListening = uiState.isListening,
                            isSpeaking = uiState.isSpeaking,
                            isVoiceConversationMode = uiState.isVoiceConversationMode,
                            popularPrompts = uiState.popularTab.map { it.question },
                            voiceRms = uiState.voiceRms,
                            voiceTranscript = uiState.voiceTranscript,
                        )
                    1 ->
                        PopularTab(
                            questions = uiState.popularTab,
                            onQuestion = {
                                sendText(it.question)
                                selectedTab = 0
                            },
                        )
                    2 ->
                        UnansweredTab(
                            questions = uiState.unansweredTab,
                            onSubmit = { showQuestionDialog = true },
                        )
                }
            }
        }
    }

    if (showQuestionDialog) {
        AppActionSheet(
            onDismiss = { showQuestionDialog = false },
            title = "Submit a question",
        ) {
            OutlinedTextField(
                value = dialogText,
                onValueChange = { dialogText = it },
                placeholder = { Text("Type your question…") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { showQuestionDialog = false },
                    modifier = Modifier.weight(1f),
                ) { Text("Cancel") }
                Button(
                    onClick = {
                        showQuestionDialog = false
                        dialogText = ""
                        scope.launch { snackbarState.showSnackbar("Question submitted.") }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Submit") }
            }
        }
    }
}

@Composable
private fun AgentHeader(
    onBack: () -> Unit,
    onHistory: () -> Unit,
    onDownload: () -> Unit,
) {
    val orbGradient = Brush.linearGradient(listOf(Color(0xFF3949AB), Color(0xFF6A1B9A)))
    val ringGradient = Brush.linearGradient(listOf(Color(0xFF80DEEA), Color(0xFFB39DDB)))

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.07f)),
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .border(width = 1.5.dp, brush = ringGradient, shape = CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(orbGradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("AI Assistant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Powered by Mileway AI", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                }
                IconButton(onClick = onHistory) {
                    Icon(Icons.Filled.Schedule, contentDescription = "History", tint = Color.White)
                }
                IconButton(onClick = onDownload) {
                    Icon(Icons.Filled.Download, contentDescription = "Export", tint = Color.White)
                }
            }
            Spacer(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color.White.copy(alpha = 0.18f)),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatTab(
    messages: List<AgentMessage>,
    isStreaming: Boolean,
    streamedText: String,
    thinkingPhrase: String,
    cursorAlpha: Float,
    listState: androidx.compose.foundation.lazy.LazyListState,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onChipClicked: (String) -> Unit,
    onFeedback: (messageId: String, rating: Int) -> Unit,
    feedbackMap: Map<String, Int>,
    onMic: () -> Unit,
    onToggleVoiceConversation: () -> Unit,
    micPulsing: Boolean,
    micScale: Float,
    isListening: Boolean,
    isSpeaking: Boolean,
    isVoiceConversationMode: Boolean,
    voiceRms: Float,
    voiceTranscript: String,
    popularPrompts: List<String> = QUICK_PROMPTS,
) {
    val waveformState = when {
        isListening -> WaveformState.Listening
        isSpeaking -> WaveformState.Speaking
        isStreaming -> WaveformState.Processing
        else -> WaveformState.Idle
    }
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        ) {
            if (messages.isEmpty() && !isStreaming) {
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        popularPrompts.forEach { prompt ->
                            Surface(
                                onClick = { onChipClicked(prompt) },
                                color = CHIP_BG,
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            ) {
                                Text(
                                    prompt,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }

            items(messages) { message ->
                AgentMessageBubble(
                    message = message,
                    onFeedback = { rating -> onFeedback(message.id, rating) },
                    feedbackRating = feedbackMap[message.id],
                )
            }

            if (isStreaming) {
                item {
                    if (streamedText.isEmpty()) {
                        AgentThinkingIndicator(phrase = thinkingPhrase)
                    } else {
                        AgentStreamingBubble(text = streamedText, cursorAlpha = cursorAlpha)
                    }
                }
            }

            if (messages.isNotEmpty() && !isStreaming) {
                item {
                    FlowRow(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        popularPrompts.take(3).forEach { prompt ->
                            Surface(
                                onClick = { onChipClicked(prompt) },
                                color = CHIP_BG,
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            ) {
                                Text(
                                    prompt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = isListening || isSpeaking) {
            VoiceWaveformOverlay(
                state = waveformState,
                rms = voiceRms,
                transcript = voiceTranscript,
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                placeholder = { Text("Ask me anything…", color = Color.White.copy(alpha = 0.5f)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White.copy(alpha = 0.6f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                    ),
            )
            IconButton(
                onClick = onMic,
                modifier = if (micPulsing) Modifier.scale(micScale) else Modifier,
            ) {
                Icon(
                    Icons.Filled.Mic,
                    contentDescription = if (isListening) "Stop voice" else "Start voice",
                    tint = if (isListening) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                )
            }
            IconButton(onClick = onToggleVoiceConversation) {
                Icon(
                    if (isVoiceConversationMode) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = "Toggle hands-free mode",
                    tint = if (isVoiceConversationMode) MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = 0.5f),
                )
            }
            IconButton(
                onClick = onSend,
                enabled = inputText.isNotBlank() && !isStreaming,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank()) Color.White else Color.White.copy(alpha = 0.4f),
                )
            }
        }
    }
}


@Composable
private fun PopularTab(
    questions: List<PopularQuestion>,
    onQuestion: (PopularQuestion) -> Unit,
) {
    val grouped = questions.groupBy { it.module }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
    ) {
        grouped.forEach { (module, items) ->
            item {
                Text(
                    text = module,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            items(items) { question ->
                Surface(
                    onClick = { onQuestion(question) },
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ListItem(
                        headlineContent = {
                            Text(question.question, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (question.isTrending) {
                                    Badge(containerColor = MilewayColors.warning) {
                                        Text("Trending", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                    }
                                }
                                Text("${question.askCount}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }
    }
}

@Composable
private fun UnansweredTab(
    questions: List<UnansweredQuestion>,
    onSubmit: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
    ) {
        items(questions) { question ->
            ListItem(
                headlineContent = {
                    Text(question.question, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                },
                supportingContent = {
                    Text(question.module, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (question.askCount > 3) {
                            Box(modifier = Modifier.size(8.dp).background(MilewayColors.danger, CircleShape))
                        }
                        Text("${question.askCount} asked", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.White.copy(alpha = 0.06f)),
            )
        }
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.CenterEnd) {
                OutlinedButton(
                    onClick = onSubmit,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                ) {
                    Text("Submit a question", color = Color.White)
                }
            }
        }
    }
}
