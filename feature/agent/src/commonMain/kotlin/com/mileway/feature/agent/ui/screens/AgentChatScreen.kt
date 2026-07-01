@file:Suppress("ktlint:standard:function-naming")

package com.mileway.feature.agent.ui.screens

import com.mileway.feature.agent.ui.components.VoiceWaveformOverlay
import com.mileway.feature.agent.ui.components.WaveformState
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
import com.mileway.core.ui.components.CrtVignette
import com.mileway.core.ui.components.PhosphorDotGrid
import com.mileway.core.ui.components.ScanlineOverlay
import com.mileway.core.ui.components.TerminalCursor
import com.mileway.core.ui.components.sheet.AppActionSheet
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.core.ui.theme.TerminalType
import com.mileway.feature.agent.model.AgentMessage
import com.mileway.feature.agent.model.PopularQuestion
import com.mileway.feature.agent.model.UnansweredQuestion
import com.mileway.feature.agent.ui.components.AgentMessageBubble
import com.mileway.feature.agent.ui.components.AgentStreamingBubble
import com.mileway.feature.agent.ui.components.AgentThinkingIndicator
import com.mileway.feature.agent.viewmodel.AgentAction
import com.mileway.feature.agent.viewmodel.AgentEffect
import com.mileway.feature.agent.viewmodel.AgentViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

private val AI_GRADIENT = Brush.verticalGradient(listOf(Color(0xFF010701), Color(0xFF030D05)))
private val CHIP_BG = Color(0x1400FF41)
private val PHOSPHOR_GREEN = Color(0xFF00FF41)
private val PHOSPHOR_DIM = Color(0xFF3A6645)
private val TERMINAL_BORDER = Color(0xFF1C3522)
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
            PhosphorDotGrid(dotAlpha = 0.055f)
            Column(modifier = Modifier.fillMaxSize()) {
                // Custom header
                AgentHeader(
                    onBack = onBack,
                    onHistory = onOpenHistory,
                    onDownload = { scope.launch { snackbarState.showSnackbar("Export available in full version") } },
                )

                // Tab row — terminal bracket style
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF040C06),
                    contentColor = PHOSPHOR_GREEN,
                    divider = {
                        androidx.compose.foundation.layout.Spacer(
                            Modifier.height(1.dp).fillMaxWidth().background(TERMINAL_BORDER),
                        )
                    },
                ) {
                    listOf("[ CHAT ]", "[ POPULAR ]", "[ UNANSWERED ]").forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    style = TerminalType.sectionLabel,
                                    color = if (selectedTab == index) PHOSPHOR_GREEN else PHOSPHOR_DIM,
                                )
                            },
                            selectedContentColor = PHOSPHOR_GREEN,
                            unselectedContentColor = PHOSPHOR_DIM,
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
            // Terminal post-processing layers
            ScanlineOverlay()
            CrtVignette()
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF010701)),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PHOSPHOR_DIM)
                }
                // Terminal avatar: thin phosphor-green border ring, monogram inside
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(width = 1.dp, color = PHOSPHOR_GREEN, shape = RoundedCornerShape(4.dp))
                        .background(Color(0xFF00280E), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        ">_",
                        style = TerminalType.prompt,
                        color = PHOSPHOR_GREEN,
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(
                        "MILEWAY_AI",
                        style = TerminalType.display,
                        color = PHOSPHOR_GREEN,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "●",
                            style = TerminalType.statusLine,
                            color = PHOSPHOR_GREEN,
                        )
                        Text(
                            "SYSTEM ONLINE",
                            style = TerminalType.statusLine,
                            color = PHOSPHOR_DIM,
                        )
                    }
                }
                IconButton(onClick = onHistory) {
                    Icon(Icons.Filled.Schedule, contentDescription = "History", tint = PHOSPHOR_DIM)
                }
                IconButton(onClick = onDownload) {
                    Icon(Icons.Filled.Download, contentDescription = "Export", tint = PHOSPHOR_DIM)
                }
            }
            // Phosphor separator line
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.fillMaxWidth().height(1.dp).background(TERMINAL_BORDER),
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
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "SYSTEM READY. ASK ANYTHING.",
                            style = TerminalType.sectionLabel,
                            color = PHOSPHOR_DIM,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            popularPrompts.forEach { prompt ->
                                Surface(
                                    onClick = { onChipClicked(prompt) },
                                    color = CHIP_BG,
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, TERMINAL_BORDER),
                                ) {
                                    Text(
                                        "> $prompt",
                                        style = TerminalType.prompt,
                                        color = PHOSPHOR_GREEN,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    )
                                }
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
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, TERMINAL_BORDER),
                            ) {
                                Text(
                                    "> $prompt",
                                    style = TerminalType.statusLine,
                                    color = PHOSPHOR_DIM,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                leadingIcon = {
                    Text(
                        ">",
                        style = TerminalType.prompt,
                        color = PHOSPHOR_GREEN,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                },
                placeholder = {
                    Text(
                        "type query_",
                        style = TerminalType.prompt,
                        color = PHOSPHOR_DIM,
                    )
                },
                textStyle = TerminalType.prompt.copy(color = PHOSPHOR_GREEN),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PHOSPHOR_GREEN,
                    unfocusedBorderColor = TERMINAL_BORDER,
                    focusedTextColor = PHOSPHOR_GREEN,
                    unfocusedTextColor = PHOSPHOR_GREEN,
                    cursorColor = PHOSPHOR_GREEN,
                    focusedContainerColor = Color(0xFF040C06),
                    unfocusedContainerColor = Color(0xFF010701),
                ),
            )
            IconButton(
                onClick = onMic,
                modifier = if (micPulsing) Modifier.scale(micScale) else Modifier,
            ) {
                Icon(
                    Icons.Filled.Mic,
                    contentDescription = if (isListening) "Stop voice" else "Start voice",
                    tint = if (isListening) PHOSPHOR_GREEN else PHOSPHOR_DIM,
                )
            }
            IconButton(onClick = onToggleVoiceConversation) {
                Icon(
                    if (isVoiceConversationMode) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = "Toggle hands-free mode",
                    tint = if (isVoiceConversationMode) PHOSPHOR_GREEN else PHOSPHOR_DIM,
                )
            }
            IconButton(
                onClick = onSend,
                enabled = inputText.isNotBlank() && !isStreaming,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank()) PHOSPHOR_GREEN else PHOSPHOR_DIM,
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
