package ai.openclaw.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.openclaw.app.MainViewModel
import ai.openclaw.app.chat.ChatSessionEntry
import ai.openclaw.app.ui.chat.friendlySessionName
import ai.openclaw.app.ui.chat.resolveSessionChoices

@Composable
fun ContactsScreen(
  viewModel: MainViewModel,
  onOpenChat: (String) -> Unit,
) {
  val chatSessionKey by viewModel.chatSessionKey.collectAsState()
  val chatSessions by viewModel.chatSessions.collectAsState()
  val mainSessionKey by viewModel.mainSessionKey.collectAsState()

  LaunchedEffect(Unit) {
    viewModel.refreshChatSessions(limit = 200)
  }

  val contacts =
    remember(chatSessionKey, chatSessions, mainSessionKey) {
      resolveSessionChoices(
        currentSessionKey = chatSessionKey,
        sessions = chatSessions,
        mainSessionKey = mainSessionKey,
      )
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      Text(
        text = tr("Contacts", "联系人"),
        style = mobileTitle1,
        color = mobileText,
      )
      Text(
        text = tr("Choose a conversation to open.", "选择一个联系人进入对话。"),
        style = mobileCallout,
        color = mobileTextSecondary,
        modifier = Modifier.padding(top = 4.dp),
      )
    }

    if (contacts.isEmpty()) {
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
              text = tr("No contacts yet", "暂无联系人"),
              style = mobileHeadline,
              color = mobileText,
            )
            Text(
              text = tr("Start a conversation first, then it will appear here.", "先开始一个对话，之后它会出现在这里。"),
              style = mobileCallout,
              color = mobileTextSecondary,
            )
          }
        }
      }
    } else {
      items(items = contacts, key = { it.key }) { entry ->
        ContactRow(
          entry = entry,
          active = entry.key == chatSessionKey,
          onClick = { onOpenChat(entry.key) },
        )
      }
    }
  }
}

@Composable
private fun ContactRow(
  entry: ChatSessionEntry,
  active: Boolean,
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
      Text(
        text = friendlySessionName(entry.displayName ?: entry.key),
        style = mobileHeadline,
        color = mobileText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = if (active) tr("Current conversation", "当前对话") else tr("Tap to open chat", "点击进入对话"),
        style = mobileCallout.copy(fontWeight = FontWeight.Medium),
        color = if (active) mobileAccent else mobileTextSecondary,
      )
    }
  }
}
