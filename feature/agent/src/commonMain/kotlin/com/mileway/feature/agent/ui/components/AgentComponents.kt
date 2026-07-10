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
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.agent_cd_helpful
import com.mileway.core.ui.resources.agent_cd_not_helpful
import com.mileway.core.ui.resources.agent_terminal_prompt_label
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.TerminalType
import com.mileway.feature.agent.model.AgentMessage
import org.jetbrains.compose.resources.stringResource

private val USER_BUBBLE_SHAPE = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 4.dp)

private val EMBER_ACCENT = Color(0xFFF5A623)
private val EMBER_DIM = Color(0xFFB87A1C)
private val TERMINAL_BORDER = Color(0xFF3D2E1C)
private val TERMINAL_SURFACE = Color(0xFF17110B)

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
            // User: right-aligned with `>` terminal prompt prefix, dark bg, amber text
            Box(
                modifier =
                    Modifier
                        .widthIn(max = 300.dp)
                        .background(Color(0xFF3A2A12), USER_BUBBLE_SHAPE)
                        .border(1.dp, TERMINAL_BORDER, USER_BUBBLE_SHAPE)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        ">",
                        style = TerminalType.prompt,
                        color = EMBER_ACCENT,
                    )
                    Text(
                        message.text,
                        style = TerminalType.prompt,
                        color = EMBER_ACCENT,
                    )
                }
            }
        } else {
            // AI: left-aligned, no bubble — terminal output style with `$` prefix
            Column(
                modifier =
                    Modifier
                        .widthIn(max = 340.dp)
                        .background(
                            Color(0xFF0B0806),
                            DesignTokens.Shape.button,
                        )
                        .border(
                            1.dp,
                            TERMINAL_BORDER,
                            DesignTokens.Shape.button,
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                // System prompt line
                Text(
                    stringResource(Res.string.agent_terminal_prompt_label),
                    style = TerminalType.statusLine,
                    color = EMBER_DIM,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Markdown(
                    content = message.text,
                    colors = markdownColor(text = EMBER_ACCENT),
                    typography =
                        markdownTypography(
                            text = TerminalType.output,
                            paragraph = TerminalType.output,
                            code = TerminalType.output.copy(color = EMBER_ACCENT),
                        ),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { onFeedback(1) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.ThumbUp,
                        contentDescription = stringResource(Res.string.agent_cd_helpful),
                        tint = if (feedbackRating == 1) EMBER_ACCENT else EMBER_DIM,
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(onClick = { onFeedback(-1) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.ThumbDown,
                        contentDescription = stringResource(Res.string.agent_cd_not_helpful),
                        tint = if (feedbackRating == -1) MaterialTheme.colorScheme.error else EMBER_DIM,
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
            modifier =
                Modifier
                    .widthIn(max = 340.dp)
                    .background(Color(0xFF0B0806), DesignTokens.Shape.button)
                    .border(1.dp, TERMINAL_BORDER, DesignTokens.Shape.button)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                stringResource(Res.string.agent_terminal_prompt_label),
                style = TerminalType.statusLine,
                color = EMBER_DIM,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            // Block cursor `█` instead of pipe — classic terminal feel
            Text(
                text = "$text${if (cursorAlpha > 0.5f) "█" else ""}",
                style = TerminalType.output,
                color = EMBER_ACCENT,
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
            modifier =
                Modifier
                    .background(Color(0xFF0B0806), DesignTokens.Shape.button)
                    .border(1.dp, TERMINAL_BORDER, DesignTokens.Shape.button)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                stringResource(Res.string.agent_terminal_prompt_label),
                style = TerminalType.statusLine,
                color = EMBER_DIM,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = "$phrase...",
                style = TerminalType.output,
                color = EMBER_DIM,
            )
        }
    }
}
