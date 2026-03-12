package ai.openclaw.app.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import ai.openclaw.app.chat.ChatMessage
import ai.openclaw.app.chat.ChatPendingToolCall
import ai.openclaw.app.ui.mobileBorder
import ai.openclaw.app.ui.mobileCallout
import ai.openclaw.app.ui.mobileHeadline
import ai.openclaw.app.ui.mobileSurface
import ai.openclaw.app.ui.mobileText
import ai.openclaw.app.ui.mobileTextSecondary

@Composable
fun ChatMessageListCard(
  messages: List<ChatMessage>,
  pendingRunCount: Int,
  pendingToolCalls: List<ChatPendingToolCall>,
  streamingAssistantText: String?,
  healthOk: Boolean,
  assistantLabel: String = "assistant",
  onPullDown: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()
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

  // With reverseLayout the newest item is at index 0 (bottom of screen).
  LaunchedEffect(messages.size, pendingRunCount, pendingToolCalls.size, streamingAssistantText) {
    listState.animateScrollToItem(index = 0)
  }

  Box(modifier = modifier.fillMaxWidth()) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().nestedScroll(dismissImeOnPullDown),
      state = listState,
      reverseLayout = true,
      verticalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp),
    ) {
      // With reverseLayout = true, index 0 renders at the BOTTOM.
      // So we emit newest items first: streaming → tools → typing → messages (newest→oldest).

      val stream = streamingAssistantText?.trim()
      if (!stream.isNullOrEmpty()) {
        item(key = "stream") {
          ChatStreamingAssistantBubble(text = stream, assistantLabel = assistantLabel)
        }
      }

      if (pendingToolCalls.isNotEmpty()) {
        item(key = "tools") {
          ChatPendingToolsBubble(toolCalls = pendingToolCalls, assistantLabel = assistantLabel)
        }
      }

      if (pendingRunCount > 0) {
        item(key = "typing") {
          ChatTypingIndicatorBubble(assistantLabel = assistantLabel)
        }
      }

      items(count = messages.size, key = { idx -> messages[messages.size - 1 - idx].id }) { idx ->
        ChatMessageBubble(message = messages[messages.size - 1 - idx], assistantLabel = assistantLabel)
      }
    }

    if (messages.isEmpty() && pendingRunCount == 0 && pendingToolCalls.isEmpty() && streamingAssistantText.isNullOrBlank()) {
      EmptyChatHint(modifier = Modifier.align(Alignment.Center), healthOk = healthOk)
    }
  }
}

@Composable
private fun EmptyChatHint(modifier: Modifier = Modifier, healthOk: Boolean) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(6.dp),
    color = mobileSurface,
    border = androidx.compose.foundation.BorderStroke(1.dp, mobileBorder),
  ) {
    androidx.compose.foundation.layout.Column(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text("No messages yet", style = mobileHeadline, color = mobileText)
      Text(
        text =
          if (healthOk) {
            "Send the first prompt to start this session."
          } else {
            "Connect gateway first, then return to chat."
          },
        style = mobileCallout,
        color = mobileTextSecondary,
      )
    }
  }
}
