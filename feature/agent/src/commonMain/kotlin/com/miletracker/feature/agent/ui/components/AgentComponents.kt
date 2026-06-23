@file:Suppress("ktlint:standard:function-naming")

package com.miletracker.feature.agent.ui.components

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.miletracker.feature.agent.model.AgentMessage

private val USER_BUBBLE_GRADIENT = Brush.horizontalGradient(listOf(Color(0xFF5C6BC0), Color(0xFF7E57C2)))

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
            Box(
                modifier =
                    Modifier
                        .widthIn(max = 280.dp)
                        .background(USER_BUBBLE_GRADIENT, RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                androidx.compose.material3.Text(message.text, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
        } else {
            Box(
                modifier =
                    Modifier
                        .widthIn(max = 280.dp)
                        .background(
                            Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Markdown(
                    content = message.text,
                    colors = markdownColor(text = Color.White),
                    typography =
                        markdownTypography(
                            text = MaterialTheme.typography.bodyMedium,
                            paragraph = MaterialTheme.typography.bodyMedium,
                        ),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { onFeedback(1) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.ThumbUp, contentDescription = "Helpful", tint = if (feedbackRating == 1) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { onFeedback(-1) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.ThumbDown, contentDescription = "Not helpful", tint = if (feedbackRating == -1) MaterialTheme.colorScheme.error else Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
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
        Box(
            modifier =
                Modifier
                    .widthIn(max = 280.dp)
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            androidx.compose.material3.Text(
                text = "$text${if (cursorAlpha > 0.5f) "|" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
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
        Box(
            modifier =
                Modifier
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            androidx.compose.material3.Text(
                text = "$phrase ···",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}
