@file:Suppress("ktlint:standard:function-naming")

package com.mileway.feature.agent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mileway.core.ui.theme.TerminalType
import com.mileway.feature.agent.model.AgentMessage

private val PHOSPHOR_GREEN = Color(0xFF00FF41)
private val PHOSPHOR_DIM = Color(0xFF3A6645)
private val TERMINAL_BORDER = Color(0xFF1C3522)
private val TERMINAL_SURFACE = Color(0xFF040C06)

@Composable
fun AgentMessageBubble(
    message: AgentMessage,
    onFeedback: (rating: Int) -> Unit,
    feedbackRating: Int? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
    ) {
        if (message.isUser) {
            // User: right-aligned with `>` terminal prompt prefix, dark bg, green text
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .background(
                        Color(0xFF00280E),
                        RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp, bottomStart = 8.dp, bottomEnd = 2.dp),
                    )
                    .border(
                        1.dp,
                        TERMINAL_BORDER,
                        RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp, bottomStart = 8.dp, bottomEnd = 2.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        ">",
                        style = TerminalType.prompt,
                        color = PHOSPHOR_GREEN,
                    )
                    Text(
                        message.text,
                        style = TerminalType.prompt,
                        color = PHOSPHOR_GREEN,
                    )
                }
            }
        } else {
            // AI: left-aligned, no bubble — terminal output style with `$` prefix
            Column(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .background(
                        Color(0xFF010701),
                        RoundedCornerShape(2.dp),
                    )
                    .border(
                        1.dp,
                        TERMINAL_BORDER,
                        RoundedCornerShape(2.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                // System prompt line
                Text(
                    "$ mileway_ai",
                    style = TerminalType.statusLine,
                    color = PHOSPHOR_DIM,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Markdown(
                    content = message.text,
                    colors = markdownColor(text = PHOSPHOR_GREEN),
                    typography = markdownTypography(
                        text = TerminalType.output,
                        paragraph = TerminalType.output,
                        code = TerminalType.output.copy(color = PHOSPHOR_GREEN),
                    ),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { onFeedback(1) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.ThumbUp,
                        contentDescription = "Helpful",
                        tint = if (feedbackRating == 1) PHOSPHOR_GREEN else PHOSPHOR_DIM,
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(onClick = { onFeedback(-1) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.ThumbDown,
                        contentDescription = "Not helpful",
                        tint = if (feedbackRating == -1) MaterialTheme.colorScheme.error else PHOSPHOR_DIM,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun AgentStreamingBubble(
    text: String,
    cursorAlpha: Float,
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Column(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .background(Color(0xFF010701), RoundedCornerShape(2.dp))
                .border(1.dp, TERMINAL_BORDER, RoundedCornerShape(2.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                "$ mileway_ai",
                style = TerminalType.statusLine,
                color = PHOSPHOR_DIM,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            // Block cursor `█` instead of pipe — classic terminal feel
            Text(
                text = "$text${if (cursorAlpha > 0.5f) "█" else ""}",
                style = TerminalType.output,
                color = PHOSPHOR_GREEN,
            )
        }
    }
}

@Composable
fun AgentThinkingIndicator(phrase: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .background(Color(0xFF010701), RoundedCornerShape(2.dp))
                .border(1.dp, TERMINAL_BORDER, RoundedCornerShape(2.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                "$ mileway_ai",
                style = TerminalType.statusLine,
                color = PHOSPHOR_DIM,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = "$phrase...",
                style = TerminalType.output,
                color = PHOSPHOR_DIM,
            )
        }
    }
}
