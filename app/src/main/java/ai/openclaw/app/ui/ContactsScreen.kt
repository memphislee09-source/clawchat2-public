package ai.openclaw.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.openclaw.app.MainViewModel
import ai.openclaw.app.chat.AgentContactEntry
import ai.openclaw.app.chat.formatAgentContactTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
  viewModel: MainViewModel,
  onOpenChat: (String) -> Unit,
) {
  val chatSessionKey by viewModel.chatSessionKey.collectAsState()
  val agentContacts by viewModel.agentContacts.collectAsState()
  val isRefreshing by viewModel.agentContactsRefreshing.collectAsState()
  val errorText by viewModel.agentContactsError.collectAsState()
  val chatLastReadAtMs by viewModel.chatLastReadAtMs.collectAsState()

  LaunchedEffect(Unit) {
    viewModel.refreshAgentContacts()
  }

  val pullToRefreshState = rememberPullToRefreshState()

  PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = { viewModel.refreshAgentContacts() },
    state = pullToRefreshState,
    modifier = Modifier.fillMaxSize(),
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (!errorText.isNullOrBlank()) {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, mobileDanger.copy(alpha = 0.35f)),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = tr("Refresh failed", "刷新失败"),
                style = mobileHeadline,
                color = mobileDanger,
              )
              Text(
                text = errorText!!,
                style = mobileCallout,
                color = mobileText,
              )
            }
          }
        }
      }

      if (agentContacts.isEmpty() && !isRefreshing) {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, mobileBorder),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = tr("No agents yet", "暂无 agent"),
                style = mobileHeadline,
                color = mobileText,
              )
              Text(
                text = tr("Connect to OpenClaw and pull down to refresh.", "连接 OpenClaw 后下拉刷新。"),
                style = mobileCallout,
                color = mobileTextSecondary,
              )
            }
          }
        }
      } else {
        items(items = agentContacts, key = { it.agentId }) { entry ->
          ContactRow(
            entry = entry,
            active = entry.directSessionKey == chatSessionKey,
            unread = isContactUnread(entry = entry, lastReadAtMs = chatLastReadAtMs[entry.directSessionKey]),
            onClick = { onOpenChat(entry.agentId) },
          )
        }
      }
    }
  }
}

@Composable
private fun ContactRow(
  entry: AgentContactEntry,
  active: Boolean,
  unread: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = if (active) mobileAccentSoft else Color.White,
    border = BorderStroke(1.dp, if (active) Color(0xFFD5E2FA) else mobileBorder),
    shadowElevation = 0.dp,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Box(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = formatAgentContactTitle(displayName = entry.displayName, emoji = entry.emoji),
          style = mobileHeadline,
          color = mobileText,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.fillMaxWidth().padding(end = if (unread) 18.dp else 0.dp),
        )
        if (unread) {
          Surface(
            modifier = Modifier.size(10.dp).align(androidx.compose.ui.Alignment.TopEnd),
            shape = CircleShape,
            color = mobileSuccess,
          ) {
            Box(modifier = Modifier.size(10.dp))
          }
        }
      }

      Text(
        text = entry.previewText ?: tr("No messages yet", "暂无消息"),
        style = mobileCallout,
        color = mobileTextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

private fun isContactUnread(entry: AgentContactEntry, lastReadAtMs: Long?): Boolean {
  val updatedAt = entry.directSessionUpdatedAtMs ?: return false
  val lastRead = lastReadAtMs ?: return true
  return updatedAt > lastRead
}
