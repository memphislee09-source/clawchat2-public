package ai.openclaw.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.openclaw.app.MainViewModel
import ai.openclaw.app.chat.AgentContactEntry
import ai.openclaw.app.chat.formatAgentContactTitle
import coil.compose.SubcomposeAsyncImage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
          .background(mobileBackground),
      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    ) {
      if (!errorText.isNullOrBlank()) {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = mobileSurfaceStrong,
            border = BorderStroke(1.dp, mobileDanger.copy(alpha = 0.20f)),
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
            shape = RoundedCornerShape(18.dp),
            color = mobileSurfaceStrong,
            border = BorderStroke(1.dp, mobileBorder.copy(alpha = 0.28f)),
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
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(0.dp),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .clickable(onClick = onClick)
          .background(if (active) mobileSurfaceStrong.copy(alpha = 0.9f) else Color.Transparent)
          .padding(horizontal = 12.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .width(4.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) mobileAccent else Color.Transparent),
      )
      ContactLeadingAvatar(entry = entry, active = active)
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = formatAgentContactTitle(displayName = entry.displayName, emoji = entry.emoji),
            style = mobileHeadline.copy(fontWeight = FontWeight.Bold),
            color = mobileText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          if (active) {
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = mobileAccentSoft.copy(alpha = 0.86f),
              border = BorderStroke(1.dp, mobileAccent.copy(alpha = 0.14f)),
            ) {
              Text(
                text = tr("Active", "当前"),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style = mobileCaption2.copy(fontWeight = FontWeight.SemiBold),
                color = mobileAccent,
              )
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
      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = formatContactRecency(entry.directSessionUpdatedAtMs),
          style = mobileCaption2,
          color = if (active) mobileAccent else mobileTextSecondary,
          maxLines = 1,
        )
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
        modifier = Modifier.padding(start = 68.dp),
        thickness = 1.dp,
        color = mobileBorder.copy(alpha = 0.20f),
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

@Composable
private fun formatContactRecency(timestampMs: Long?): String {
  val timestamp = timestampMs ?: return tr("New", "新会话")
  val nowMs = remember { System.currentTimeMillis() }
  val deltaMs = (nowMs - timestamp).coerceAtLeast(0L)
  val locale = LocalConfiguration.current.locales[0] ?: java.util.Locale.getDefault()
  val zoneId = remember(locale) {
    runCatching { ZoneId.systemDefault() }.getOrDefault(ZoneId.of("UTC"))
  }

  return when {
    deltaMs < 60_000L -> tr("Now", "刚刚")
    deltaMs < 3_600_000L -> {
      val minutes = (deltaMs / 60_000L).coerceAtLeast(1L)
      tr("${minutes}m", "${minutes}分")
    }
    deltaMs < 86_400_000L -> {
      val hours = (deltaMs / 3_600_000L).coerceAtLeast(1L)
      tr("${hours}h", "${hours}小时")
    }
    deltaMs < 7 * 86_400_000L -> {
      val days = (deltaMs / 86_400_000L).coerceAtLeast(1L)
      tr("${days}d", "${days}天")
    }
    else -> {
      val formatter =
        if ((locale.language ?: "").startsWith("zh")) {
          DateTimeFormatter.ofPattern("M月d日", locale)
        } else {
          DateTimeFormatter.ofPattern("MMM d", locale)
        }
      runCatching { formatter.format(Instant.ofEpochMilli(timestamp).atZone(zoneId)) }.getOrDefault("")
    }
  }
}
