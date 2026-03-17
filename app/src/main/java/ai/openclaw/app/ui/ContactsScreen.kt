package ai.openclaw.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.openclaw.app.MainViewModel
import ai.openclaw.app.chat.AgentContactEntry
import ai.openclaw.app.chat.formatAgentContactTitle
import coil.compose.SubcomposeAsyncImage

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

  val pullToRefreshState = rememberPullToRefreshState()

  PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = { viewModel.refreshAgentContacts() },
    state = pullToRefreshState,
    modifier = Modifier.fillMaxSize(),
  ) {
    LazyColumn(
      modifier =
        Modifier
          .fillMaxSize()
          .background(mobileSurface)
          .padding(vertical = 6.dp),
    ) {
      if (!errorText.isNullOrBlank()) {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            color = mobileDangerSoft,
            border = BorderStroke(1.dp, mobileDanger.copy(alpha = 0.35f)),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = tr("Refresh failed", "刷新失败"),
                style = mobileHeadline.copy(fontWeight = FontWeight.Bold),
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
            shape = RoundedCornerShape(6.dp),
            color = mobileSurface,
            border = BorderStroke(1.dp, mobileBorder),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = tr("No agents yet", "暂无 agent"),
                style = mobileHeadline.copy(fontWeight = FontWeight.Bold),
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
        itemsIndexed(items = agentContacts, key = { _, it -> it.agentId }) { index, entry ->
          ContactListItem(
            entry = entry,
            active = entry.directSessionKey == chatSessionKey,
            unread = isContactUnread(entry = entry, lastReadAtMs = chatLastReadAtMs[entry.directSessionKey]),
            showDivider = index < agentContacts.lastIndex,
            onClick = { onOpenChat(entry.agentId) },
          )
        }
      }
    }
  }
}

@Composable
private fun ContactListItem(
  entry: AgentContactEntry,
  active: Boolean,
  unread: Boolean,
  showDivider: Boolean,
  onClick: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(if (active) mobileAccentSoft.copy(alpha = 0.55f) else Color.Transparent)
          .clickable(onClick = onClick)
          .padding(horizontal = 14.dp, vertical = 11.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ContactLeadingAvatar(entry = entry, active = active)
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = formatAgentContactTitle(displayName = entry.displayName, emoji = entry.emoji),
          style = mobileHeadline.copy(fontWeight = FontWeight.Bold),
          color = mobileText,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = entry.previewText ?: tr("No messages yet", "暂无消息"),
          style = mobileCallout,
          color = mobileTextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Box(modifier = Modifier.size(14.dp), contentAlignment = Alignment.Center) {
        if (unread) {
          Surface(
            modifier = Modifier.size(8.dp),
            shape = RoundedCornerShape(999.dp),
            color = mobileSuccess,
          ) {
            Box(modifier = Modifier.size(8.dp))
          }
        }
      }
    }
    if (showDivider) {
      HorizontalDivider(
        modifier = Modifier.padding(start = 74.dp),
        color = mobileBorder.copy(alpha = 0.28f),
      )
    }
  }
}

@Composable
private fun ContactLeadingAvatar(entry: AgentContactEntry, active: Boolean) {
  val avatarShape = RoundedCornerShape(12.dp)
  val borderColor =
    if (active) {
      mobileAccent.copy(alpha = 0.4f)
    } else {
      mobileBorder.copy(alpha = 0.9f)
    }
  Surface(
    modifier = Modifier.size(46.dp),
    shape = avatarShape,
    color = mobileSurfaceStrong,
    border = BorderStroke(1.dp, borderColor),
  ) {
    val avatarUrl = entry.avatarUrl?.trim().takeUnless { it.isNullOrEmpty() }
    if (avatarUrl != null) {
      SubcomposeAsyncImage(
        model = avatarUrl,
        contentDescription = null,
        modifier = Modifier.fillMaxSize().clip(avatarShape),
        contentScale = ContentScale.Crop,
        loading = { ContactAvatarFallback(entry = entry, active = active) },
        error = { ContactAvatarFallback(entry = entry, active = active) },
      )
    } else {
      ContactAvatarFallback(entry = entry, active = active)
    }
  }
}

@Composable
private fun ContactAvatarFallback(entry: AgentContactEntry, active: Boolean) {
  val avatarShape = RoundedCornerShape(12.dp)
  val label = contactFallbackLabel(entry)
  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .clip(avatarShape)
        .background(if (active) mobileAccentSoft else mobileSurface),
    contentAlignment = Alignment.Center,
  ) {
    if (label != null) {
      Text(
        text = label,
        style = mobileCallout.copy(fontWeight = FontWeight.Bold),
        color = if (active) mobileAccent else mobileText,
        textAlign = TextAlign.Center,
        maxLines = 1,
      )
    } else {
      Icon(
        imageVector = Icons.Default.SmartToy,
        contentDescription = null,
        modifier = Modifier.size(19.dp),
        tint = if (active) mobileAccent else mobileTextSecondary,
      )
    }
  }
}

private fun isContactUnread(entry: AgentContactEntry, lastReadAtMs: Long?): Boolean {
  val updatedAt = entry.directSessionUpdatedAtMs ?: return false
  val lastRead = lastReadAtMs ?: return true
  return updatedAt > lastRead
}

private fun contactFallbackLabel(entry: AgentContactEntry): String? {
  entry.emoji?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
  val first =
    entry.displayName
      .trim()
      .split(Regex("\\s+"))
      .firstOrNull()
      ?.firstOrNull()
      ?.uppercaseChar()
  return first?.toString()
}
