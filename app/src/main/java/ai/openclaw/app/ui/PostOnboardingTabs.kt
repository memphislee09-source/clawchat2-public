package ai.openclaw.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.openclaw.app.MainViewModel
import ai.openclaw.app.ui.chat.friendlySessionName
import ai.openclaw.app.ui.chat.resolveSessionChoices

private enum class HomeTab(
  val labelEn: String,
  val labelZh: String,
  val icon: ImageVector,
) {
  Connect(labelEn = "Connect", labelZh = "连接", icon = Icons.Default.CheckCircle),
  Chat(labelEn = "Chat", labelZh = "聊天", icon = Icons.Default.ChatBubble),
  Screen(labelEn = "Screen", labelZh = "屏幕", icon = Icons.AutoMirrored.Filled.ScreenShare),
  Settings(labelEn = "Settings", labelZh = "设置", icon = Icons.Default.Settings),
}

private enum class StatusVisual {
  Connected,
  Connecting,
  Warning,
  Error,
  Offline,
}

private val overflowMenuTabs = listOf(HomeTab.Chat, HomeTab.Connect, HomeTab.Screen, HomeTab.Settings)

@Composable
fun PostOnboardingTabs(viewModel: MainViewModel, modifier: Modifier = Modifier) {
  var activeTab by rememberSaveable { mutableStateOf(HomeTab.Chat) }
  var voiceDialogOpen by rememberSaveable { mutableStateOf(false) }
  val context = LocalContext.current

  LaunchedEffect(voiceDialogOpen) {
    if (!voiceDialogOpen) {
      viewModel.setVoiceScreenActive(false)
    }
  }

  val statusText by viewModel.statusText.collectAsState()
  val isConnected by viewModel.isConnected.collectAsState()
  val chatSessionKey by viewModel.chatSessionKey.collectAsState()
  val chatSessions by viewModel.chatSessions.collectAsState()
  val mainSessionKey by viewModel.mainSessionKey.collectAsState()

  val chatTopBarTitle =
    remember(chatSessionKey, chatSessions, mainSessionKey) {
      val sessionChoices =
        resolveSessionChoices(
          currentSessionKey = chatSessionKey,
          sessions = chatSessions,
          mainSessionKey = mainSessionKey,
        )
      val currentLabel = sessionChoices.firstOrNull { it.key == chatSessionKey }?.displayName ?: chatSessionKey
      friendlySessionName(currentLabel).ifBlank { "Conversation" }
    }

  BackHandler(enabled = voiceDialogOpen || activeTab != HomeTab.Chat) {
    when {
      voiceDialogOpen -> voiceDialogOpen = false
      activeTab != HomeTab.Chat -> activeTab = HomeTab.Chat
    }
  }

  val statusVisual =
    remember(statusText, isConnected) {
      val lower = statusText.lowercase()
      when {
        isConnected -> StatusVisual.Connected
        lower.contains("connecting") || lower.contains("reconnecting") -> StatusVisual.Connecting
        lower.contains("pairing") || lower.contains("approval") || lower.contains("auth") -> StatusVisual.Warning
        lower.contains("error") || lower.contains("failed") -> StatusVisual.Error
        else -> StatusVisual.Offline
      }
    }

  Scaffold(
    modifier = modifier,
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopStatusBar(
        title = if (activeTab == HomeTab.Chat) chatTopBarTitle else tr(activeTab.labelEn, activeTab.labelZh),
        statusText = statusText,
        statusVisual = statusVisual,
        activeTab = activeTab,
        onSelectTab = { activeTab = it },
        onOpenAppInfo = { openAppInfo(context) },
      )
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .consumeWindowInsets(innerPadding)
          .background(mobileBackgroundGradient),
    ) {
      when (activeTab) {
        HomeTab.Connect -> ConnectTabScreen(viewModel = viewModel)
        HomeTab.Chat ->
          ChatSheet(
            viewModel = viewModel,
            onOpenVoice = {
              viewModel.prepareVoiceConversation(chatSessionKey)
              voiceDialogOpen = true
            },
          )
        HomeTab.Screen -> ScreenTabScreen(viewModel = viewModel)
        HomeTab.Settings -> SettingsSheet(viewModel = viewModel)
      }

      if (voiceDialogOpen) {
        VoiceDialog(
          viewModel = viewModel,
          onDismissRequest = { voiceDialogOpen = false },
        )
      }
    }
  }
}

@Composable
private fun ScreenTabScreen(viewModel: MainViewModel) {
  val isConnected by viewModel.isConnected.collectAsState()
  val isNodeConnected by viewModel.isNodeConnected.collectAsState()
  val canvasUrl by viewModel.canvasCurrentUrl.collectAsState()
  val canvasA2uiHydrated by viewModel.canvasA2uiHydrated.collectAsState()
  val canvasRehydratePending by viewModel.canvasRehydratePending.collectAsState()
  val canvasRehydrateErrorText by viewModel.canvasRehydrateErrorText.collectAsState()
  val isA2uiUrl = canvasUrl?.contains("/__openclaw__/a2ui/") == true
  val showRestoreCta = isConnected && isNodeConnected && (canvasUrl.isNullOrBlank() || (isA2uiUrl && !canvasA2uiHydrated))
  val restoreCtaText =
    when {
      canvasRehydratePending -> "Restore requested. Waiting for agent…"
      !canvasRehydrateErrorText.isNullOrBlank() -> canvasRehydrateErrorText!!
      else -> "Canvas reset. Tap to restore dashboard."
    }

  Box(modifier = Modifier.fillMaxSize()) {
    CanvasScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())

    if (showRestoreCta) {
      Surface(
        onClick = {
          if (canvasRehydratePending) return@Surface
          viewModel.requestCanvasRehydrate(source = "screen_tab_cta")
        },
        modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 16.dp, vertical = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = mobileSurface.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, mobileBorder),
        shadowElevation = 4.dp,
      ) {
        Text(
          text = restoreCtaText,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          style = mobileCallout.copy(fontWeight = FontWeight.Medium),
          color = mobileText,
        )
      }
    }
  }
}

@Composable
private fun TopStatusBar(
  title: String,
  statusText: String,
  statusVisual: StatusVisual,
  activeTab: HomeTab,
  onSelectTab: (HomeTab) -> Unit,
  onOpenAppInfo: () -> Unit,
) {
  val safeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
  var menuExpanded by remember { mutableStateOf(false) }

  val (chipBg, chipDot, chipText, chipBorder) =
    when (statusVisual) {
      StatusVisual.Connected ->
        listOf(
          mobileSuccessSoft,
          mobileSuccess,
          mobileSuccess,
          Color(0xFFCFEBD8),
        )
      StatusVisual.Connecting ->
        listOf(
          mobileAccentSoft,
          mobileAccent,
          mobileAccent,
          Color(0xFFD5E2FA),
        )
      StatusVisual.Warning ->
        listOf(
          mobileWarningSoft,
          mobileWarning,
          mobileWarning,
          Color(0xFFEED8B8),
        )
      StatusVisual.Error ->
        listOf(
          mobileDangerSoft,
          mobileDanger,
          mobileDanger,
          Color(0xFFF3C8C8),
        )
      StatusVisual.Offline ->
        listOf(
          mobileSurface,
          mobileTextTertiary,
          mobileTextSecondary,
          mobileBorder,
        )
    }

  Surface(
    modifier = Modifier.fillMaxWidth().windowInsetsPadding(safeInsets),
    color = Color.Transparent,
    shadowElevation = 0.dp,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = title,
        style = mobileHeadline,
        color = mobileText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f).padding(end = 12.dp),
      )
      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = chipBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, chipBorder),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              modifier = Modifier.padding(top = 1.dp),
              color = chipDot,
              shape = RoundedCornerShape(999.dp),
            ) {
              Box(modifier = Modifier.padding(4.dp))
            }
            Text(
              text = statusText.trim().ifEmpty { tr("Offline", "离线") },
              style = mobileCaption1,
              color = chipText,
              maxLines = 1,
            )
          }
        }
        Box {
          Surface(
            onClick = { menuExpanded = true },
            modifier = Modifier.size(38.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (activeTab in overflowMenuTabs) mobileAccentSoft else mobileSurface,
            border = BorderStroke(1.dp, if (activeTab in overflowMenuTabs) Color(0xFFD5E2FA) else mobileBorder),
            shadowElevation = 0.dp,
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = tr("More options", "更多选项"),
                tint = if (activeTab in overflowMenuTabs) mobileAccent else mobileTextSecondary,
              )
            }
          }
          DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            overflowMenuTabs.forEach { tab ->
              DropdownMenuItem(
                text = {
                  Text(
                    text = tr(tab.labelEn, tab.labelZh),
                    style = mobileBody.copy(fontWeight = if (tab == activeTab) FontWeight.Bold else FontWeight.Medium),
                    color = if (tab == activeTab) mobileAccent else mobileText,
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector = tab.icon,
                    contentDescription = null,
                    tint = if (tab == activeTab) mobileAccent else mobileTextSecondary,
                  )
                },
                onClick = {
                  menuExpanded = false
                  onSelectTab(tab)
                },
              )
            }
            HorizontalDivider(color = mobileBorder)
            DropdownMenuItem(
              text = {
                Text(
                  text = tr("App info", "应用信息"),
                  style = mobileBody.copy(fontWeight = FontWeight.Medium),
                  color = mobileText,
                )
              },
              onClick = {
                menuExpanded = false
                onOpenAppInfo()
              },
            )
          }
        }
      }
    }
  }
}

private fun openAppInfo(context: android.content.Context) {
  val intent =
    Intent(
      Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
      Uri.fromParts("package", context.packageName, null),
    )
  context.startActivity(intent)
}
