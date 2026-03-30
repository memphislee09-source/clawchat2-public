package ai.openclaw.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import ai.openclaw.app.chat.ChatMessage
import ai.openclaw.app.chat.ChatPendingToolCall
import ai.openclaw.app.ui.mobileAccent
import ai.openclaw.app.ui.mobileBackground
import ai.openclaw.app.ui.mobileBorder
import ai.openclaw.app.ui.mobileCallout
import ai.openclaw.app.ui.mobileHeadline
import ai.openclaw.app.ui.mobileSurface
import ai.openclaw.app.ui.mobileSurfaceStrong
import ai.openclaw.app.ui.mobileText
import ai.openclaw.app.ui.mobileTextSecondary
import ai.openclaw.app.ui.tr
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

private sealed interface ChatTimelineEntry {
  val key: String

  data class Message(val message: ChatMessage) : ChatTimelineEntry {
    override val key: String = "message:${message.id}"
  }

  data class Streaming(val text: String) : ChatTimelineEntry {
    override val key: String = "stream"
  }

  data class PendingTools(val toolCalls: List<ChatPendingToolCall>) : ChatTimelineEntry {
    override val key: String = "tools"
  }

  data object Typing : ChatTimelineEntry {
    override val key: String = "typing"
  }
}

@Composable
fun ChatMessageListCard(
  sessionKey: String,
  messages: List<ChatMessage>,
  pendingRunCount: Int,
  pendingToolCalls: List<ChatPendingToolCall>,
  streamingAssistantText: String?,
  healthOk: Boolean,
  assistantLabel: String = "assistant",
  userLabel: String = "我",
  onPullDown: () -> Unit = {},
  onRetryUnavailable: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()
  var followBottom by remember(sessionKey) { mutableStateOf(true) }
  var anchoredSessionKey by remember { mutableStateOf<String?>(null) }
  val dismissImeOnPullDown =
    remember(onPullDown) {
      object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
          if (source == NestedScrollSource.UserInput && available.y > 12f) {
            onPullDown()
          }
          return Offset.Zero
        }
      }
    }

  LaunchedEffect(listState, sessionKey) {
    snapshotFlow {
      val layoutInfo = listState.layoutInfo
      val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index
      if (lastVisible == null) {
        null
      } else {
        val total = layoutInfo.totalItemsCount
        total to lastVisible
      }
    }
      .filterNotNull()
      .map { (total, lastVisible) -> total == 0 || lastVisible >= total - 2 }
      .distinctUntilChanged()
      .collect { isNearBottom ->
        followBottom = isNearBottom
      }
  }

  val timelineEntries =
    remember(messages, pendingRunCount, pendingToolCalls, streamingAssistantText) {
      buildList {
        messages.forEach { add(ChatTimelineEntry.Message(it)) }
        if (pendingRunCount > 0) add(ChatTimelineEntry.Typing)
        if (pendingToolCalls.isNotEmpty()) add(ChatTimelineEntry.PendingTools(pendingToolCalls))
        streamingAssistantText?.trim()?.takeIf { it.isNotEmpty() }?.let { add(ChatTimelineEntry.Streaming(it)) }
      }
    }

  LaunchedEffect(sessionKey, timelineEntries.size, pendingRunCount, pendingToolCalls, streamingAssistantText) {
    if (timelineEntries.isNotEmpty()) {
      if (anchoredSessionKey != sessionKey) {
        listState.scrollToConversationBottom(timelineEntries.lastIndex)
        anchoredSessionKey = sessionKey
        followBottom = true
      } else if (followBottom) {
        listState.animateToConversationBottom(timelineEntries.lastIndex)
      }
    }
  }

  Box(modifier = modifier.fillMaxWidth()) {
    LazyColumn(
      modifier =
        Modifier
          .fillMaxSize()
          .background(mobileBackground)
          .nestedScroll(dismissImeOnPullDown),
      state = listState,
      verticalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 8.dp),
    ) {
      itemsIndexed(items = timelineEntries, key = { _, entry -> entry.key }) { index, entry ->
        when (entry) {
          is ChatTimelineEntry.Message ->
            ChatMessageBubble(message = entry.message, assistantLabel = assistantLabel, userLabel = userLabel)
          is ChatTimelineEntry.PendingTools ->
            ChatPendingToolsBubble(toolCalls = entry.toolCalls, assistantLabel = assistantLabel)
          is ChatTimelineEntry.Streaming ->
            ChatStreamingAssistantBubble(text = entry.text, assistantLabel = assistantLabel)
          ChatTimelineEntry.Typing ->
            ChatTypingIndicatorBubble(assistantLabel = assistantLabel)
        }
      }
    }

    if (messages.isEmpty() && pendingRunCount == 0 && pendingToolCalls.isEmpty() && streamingAssistantText.isNullOrBlank()) {
      EmptyChatHint(modifier = Modifier.align(Alignment.Center), healthOk = healthOk, onRetryUnavailable = onRetryUnavailable)
    }
  }
}

private suspend fun LazyListState.scrollToConversationBottom(lastIndex: Int) {
  if (lastIndex < 0) return
  scrollToItem(index = lastIndex)
  settleConversationBottom()
}

private suspend fun LazyListState.animateToConversationBottom(lastIndex: Int) {
  if (lastIndex < 0) return
  animateScrollToItem(index = lastIndex)
  settleConversationBottom()
}

private suspend fun LazyListState.settleConversationBottom() {
  var attempts = 0
  while (canScrollForward && attempts < 32) {
    val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
    val step = viewportHeight.toFloat().coerceAtLeast(1f)
    scrollBy(step)
    attempts += 1
  }
}

@Composable
private fun EmptyChatHint(modifier: Modifier = Modifier, healthOk: Boolean, onRetryUnavailable: (() -> Unit)?) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = mobileSurfaceStrong,
    border = androidx.compose.foundation.BorderStroke(1.dp, mobileBorder.copy(alpha = 0.28f)),
  ) {
    androidx.compose.foundation.layout.Column(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(tr("No messages yet", "还没有消息"), style = mobileHeadline, color = mobileText)
      Text(
        text =
          if (healthOk) {
            tr("Send a message or add an attachment to start this conversation.", "发送一条消息或添加附件，开始这段对话。")
          } else {
            tr("Chat is temporarily unavailable. Check the gateway connection, then try again.", "聊天暂时不可用。检查网关连接后再试。")
          },
        style = mobileCallout,
        color = mobileTextSecondary,
      )
      if (!healthOk && onRetryUnavailable != null) {
        TextButton(onClick = onRetryUnavailable) {
          Text(tr("Retry", "重试"), color = mobileAccent, style = mobileCallout)
        }
      }
    }
  }
}
