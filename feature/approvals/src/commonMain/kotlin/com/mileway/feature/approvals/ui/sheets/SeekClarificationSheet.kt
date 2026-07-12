package com.mileway.feature.approvals.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.approvals_cd_close_room
import com.mileway.core.ui.resources.approvals_cd_send
import com.mileway.core.ui.resources.approvals_clarification_closed_banner
import com.mileway.core.ui.resources.approvals_message_placeholder
import com.mileway.core.ui.resources.approvals_meta_pinned
import com.mileway.core.ui.resources.approvals_meta_saved
import com.mileway.core.ui.resources.approvals_seek_clarification
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.feature.approvals.model.ClarificationMessage
import com.mileway.feature.approvals.model.ClarificationRoom
import com.mileway.feature.approvals.model.ClarificationRoomMeta
import com.mileway.feature.approvals.model.ClarificationRoomStatus
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SeekClarificationSheet(
    room: ClarificationRoom?,
    thread: List<ClarificationMessage>,
    draftMessage: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onRequestCloseRoom: () -> Unit,
    onDismiss: () -> Unit,
    /** P28.4: null while the meta row hasn't loaded yet — chips still render, just unchecked. */
    meta: ClarificationRoomMeta? = null,
    onToggleSaved: () -> Unit = {},
    onTogglePinned: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isClosed = room?.status == ClarificationRoomStatus.CLOSED

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.approvals_seek_clarification),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(bottom = 12.dp),
                )
                // P28.3: only offered while ACTIVE — a closed room is read-only history, not
                // something you close a second time.
                if (!isClosed && room != null) {
                    IconButton(onClick = onRequestCloseRoom) {
                        Icon(Icons.Filled.Lock, contentDescription = stringResource(Res.string.approvals_cd_close_room))
                    }
                }
            }
            if (room != null) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = meta?.isSaved == true,
                        onClick = onToggleSaved,
                        label = { Text(stringResource(Res.string.approvals_meta_saved)) },
                        leadingIcon = {
                            Icon(
                                if (meta?.isSaved == true) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                    FilterChip(
                        selected = meta?.isPinned == true,
                        onClick = onTogglePinned,
                        label = { Text(stringResource(Res.string.approvals_meta_pinned)) },
                        leadingIcon = {
                            Icon(
                                if (meta?.isPinned == true) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                    meta?.tags.orEmpty().forEach { tag ->
                        FilterChip(selected = false, onClick = {}, label = { Text(tag) })
                    }
                }
            }
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(thread, key = { it.id }) { msg ->
                    ChatBubble(message = msg)
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            if (isClosed) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = MilewayColors.warning, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.approvals_clarification_closed_banner),
                        style = MaterialTheme.typography.bodySmall,
                        color = MilewayColors.warning,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = draftMessage,
                        onValueChange = onDraftChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(Res.string.approvals_message_placeholder)) },
                        singleLine = true,
                        shape = DesignTokens.Shape.button,
                    )
                    IconButton(
                        onClick = onSend,
                        enabled = draftMessage.isNotBlank(),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(Res.string.approvals_cd_send),
                            tint =
                                if (draftMessage.isNotBlank()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ChatBubble(message: ClarificationMessage) {
    val isApprover = !message.isFromRequester
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape =
                RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (isApprover) 2.dp else 12.dp,
                    bottomEnd = if (isApprover) 12.dp else 2.dp,
                ),
            color =
                if (isApprover) {
                    Color(0xFF1AB090).copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            modifier =
                Modifier
                    .align(if (isApprover) Alignment.CenterEnd else Alignment.CenterStart)
                    .fillMaxWidth(0.78f),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
