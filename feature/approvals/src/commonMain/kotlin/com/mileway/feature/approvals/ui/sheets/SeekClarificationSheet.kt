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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.media.model.CaptureMode
import com.mileway.core.media.model.MediaCaptureConfig
import com.mileway.core.media.model.MediaCaptureResult
import com.mileway.core.media.rememberMediaCaptureLauncher
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.approvals_attachment_label
import com.mileway.core.ui.resources.approvals_cd_attach
import com.mileway.core.ui.resources.approvals_cd_close_room
import com.mileway.core.ui.resources.approvals_cd_remove_attachment
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
import com.mileway.feature.approvals.model.DeliveryState
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
    /** P28.6/V26 P-STR.1: the in-flight draft's picked attachment URI, and its setter (null clears it). */
    draftAttachmentUrl: String? = null,
    onDraftAttachmentChange: (String?) -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isClosed = room?.status == ClarificationRoomStatus.CLOSED

    val launchAttach =
        rememberMediaCaptureLauncher(
            config = MediaCaptureConfig(allowedModes = setOf(CaptureMode.Camera, CaptureMode.Gallery, CaptureMode.Files, CaptureMode.Document)),
            onResult = { result ->
                if (result is MediaCaptureResult.Attachments) {
                    result.items.firstOrNull()?.let { onDraftAttachmentChange(it.uri) }
                }
            },
        )

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

            val rows = remember(thread) { buildChatRows(thread) }
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rows, key = { it.key }) { row ->
                    when (row) {
                        is ChatRow.DateSeparator -> DateSeparator(row.label)
                        is ChatRow.Message -> ChatBubble(message = row.message)
                    }
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
                if (draftAttachmentUrl != null) {
                    AssistChip(
                        onClick = { onDraftAttachmentChange(null) },
                        label = { Text(attachmentLabel(draftAttachmentUrl)) },
                        leadingIcon = { Icon(Icons.Filled.InsertDriveFile, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(Res.string.approvals_cd_remove_attachment),
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = launchAttach) {
                        Icon(Icons.Filled.AttachFile, contentDescription = stringResource(Res.string.approvals_cd_attach))
                    }
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
                        enabled = draftMessage.isNotBlank() || draftAttachmentUrl != null,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(Res.string.approvals_cd_send),
                            tint =
                                if (draftMessage.isNotBlank() || draftAttachmentUrl != null) {
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

/** Row shape the chat LazyColumn renders — a real message or an inserted date header. */
private sealed interface ChatRow {
    val key: String

    data class DateSeparator(val label: String, val dayEpoch: Long) : ChatRow {
        override val key get() = "sep_$dayEpoch"
    }

    data class Message(val message: ClarificationMessage) : ChatRow {
        override val key get() = message.id
    }
}

/** Groups [thread] by calendar day (device time zone), inserting a [ChatRow.DateSeparator] before each new day's first message. */
private fun buildChatRows(thread: List<ClarificationMessage>): List<ChatRow> {
    var lastDayEpoch: Long? = null
    return buildList {
        thread.forEach { message ->
            val localDate = Instant.fromEpochMilliseconds(message.timestampMs).toLocalDateTime(TimeZone.currentSystemDefault()).date
            val dayEpoch = localDate.toEpochDays().toLong()
            if (dayEpoch != lastDayEpoch) {
                add(ChatRow.DateSeparator(localDate.toString(), dayEpoch))
                lastDayEpoch = dayEpoch
            }
            add(ChatRow.Message(message))
        }
    }
}

@Composable
private fun DateSeparator(label: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

/** Local-only display name for a picked attachment's URI — no path-parsing dependency beyond the stdlib. */
@Composable
private fun attachmentLabel(uri: String): String = uri.substringAfterLast('/').ifBlank { stringResource(Res.string.approvals_attachment_label) }

@Composable
private fun ChatBubble(message: ClarificationMessage) {
    val isApprover = !message.isFromRequester
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .align(if (isApprover) Alignment.CenterEnd else Alignment.CenterStart)
                    .fillMaxWidth(0.78f),
            horizontalAlignment = if (isApprover) Alignment.End else Alignment.Start,
        ) {
            val header = listOfNotNull(message.senderName, message.senderRole).joinToString(" · ")
            if (header.isNotBlank()) {
                Text(
                    text = header,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
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
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (message.attachmentUrl != null) {
                        AssistChip(
                            onClick = {},
                            label = { Text(attachmentLabel(message.attachmentUrl)) },
                            leadingIcon = { Icon(Icons.Filled.InsertDriveFile, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            colors = AssistChipDefaults.assistChipColors(),
                            modifier = Modifier.padding(bottom = if (message.text.isNotBlank()) 4.dp else 0.dp),
                        )
                    }
                    if (message.text.isNotBlank()) {
                        Text(text = message.text, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            // P28.6: SENT/DELIVERED/SEEN is computed locally per bubble (never persisted) and only
            // shown on the approver's own outgoing messages — the requester side of this chat has
            // no read-receipt concept to report back.
            if (isApprover) {
                DeliveryIndicator()
            }
        }
    }
}

private const val DELIVERED_AFTER_MS = 800L
private const val SEEN_AFTER_MS = 1_200L

@Composable
private fun DeliveryIndicator() {
    var state by remember { mutableStateOf(DeliveryState.SENT) }
    LaunchedEffect(Unit) {
        delay(DELIVERED_AFTER_MS)
        state = DeliveryState.DELIVERED
        delay(SEEN_AFTER_MS)
        state = DeliveryState.SEEN
    }
    val (icon, tint) =
        when (state) {
            DeliveryState.SENT -> Icons.Filled.Done to MaterialTheme.colorScheme.onSurfaceVariant
            DeliveryState.DELIVERED -> Icons.Filled.DoneAll to MaterialTheme.colorScheme.onSurfaceVariant
            DeliveryState.SEEN -> Icons.Filled.DoneAll to MaterialTheme.colorScheme.primary
        }
    Icon(icon, contentDescription = state.name, tint = tint, modifier = Modifier.padding(top = 2.dp, end = 4.dp).size(14.dp))
}
