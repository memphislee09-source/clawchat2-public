package ai.openclaw.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.openclaw.app.MainViewModel
import ai.openclaw.app.R
import ai.openclaw.app.chat.formatAgentContactTitle
import ai.openclaw.app.ui.chat.friendlySessionName
import ai.openclaw.app.ui.chat.resolveSessionChoices

private enum class HomeTab(
  val labelEn: String,
  val labelZh: String,
) {
  Contacts(labelEn = "Contacts", labelZh = "联系人"),
  Connect(labelEn = "Connect", labelZh = "连接"),
  Chat(labelEn = "Chat", labelZh = "聊天"),
  Screen(labelEn = "Screen", labelZh = "屏幕"),
  Settings(labelEn = "Settings", labelZh = "设置"),
}

private val overflowMenuTabs = listOf(HomeTab.Connect, HomeTab.Screen, HomeTab.Settings)

@Composable
fun PostOnboardingTabs(viewModel: MainViewModel, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val activity = remember(context) { context.findActivity() }
  val appName = remember(context) { context.getString(R.string.app_name) }
  val savedShellScreen by viewModel.lastShellScreen.collectAsState()
  val savedVoiceDialogOpen by viewModel.lastVoiceDialogOpen.collectAsState()
  val savedChatSessionKey by viewModel.lastChatSessionKey.collectAsState()
  val currentChatSessionKey by viewModel.chatSessionKey.collectAsState()
  var activeTab by rememberSaveable(savedShellScreen) { mutableStateOf(savedShellScreen.toHomeTab()) }
  var voiceDialogOpen by rememberSaveable(savedVoiceDialogOpen) { mutableStateOf(savedVoiceDialogOpen) }

  LaunchedEffect(voiceDialogOpen) {
    viewModel.setLastVoiceDialogOpen(voiceDialogOpen)
    if (!voiceDialogOpen) {
      viewModel.setVoiceScreenActive(false)
    }
  }

  LaunchedEffect(activeTab) {
    viewModel.setLastShellScreen(activeTab.persistedName)
  }

  LaunchedEffect(savedChatSessionKey, currentChatSessionKey) {
    val restored = savedChatSessionKey.trim()
    if (restored.isNotEmpty() && restored != currentChatSessionKey) {
      viewModel.switchChatSession(restored)
    }
  }

  val chatSessionKey by viewModel.chatSessionKey.collectAsState()
  val chatSessions by viewModel.chatSessions.collectAsState()
  val agentContacts by viewModel.agentContacts.collectAsState()
  val mainSessionKey by viewModel.mainSessionKey.collectAsState()

  val chatTopBarTitle =
    remember(chatSessionKey, chatSessions, agentContacts, mainSessionKey) {
      agentContacts.firstOrNull { it.directSessionKey == chatSessionKey }?.let { contact ->
        return@remember formatAgentContactTitle(displayName = contact.displayName, emoji = contact.emoji)
      }
      val sessionChoices =
        resolveSessionChoices(
          currentSessionKey = chatSessionKey,
          sessions = chatSessions,
          mainSessionKey = mainSessionKey,
        )
      val currentLabel = sessionChoices.firstOrNull { it.key == chatSessionKey }?.displayName ?: chatSessionKey
      friendlySessionName(currentLabel).ifBlank { "Conversation" }
    }

  BackHandler(enabled = true) {
    when {
      voiceDialogOpen -> voiceDialogOpen = false
      activeTab == HomeTab.Chat -> activeTab = HomeTab.Contacts
      activeTab == HomeTab.Contacts -> activity?.finish()
      activeTab != HomeTab.Chat -> activeTab = HomeTab.Chat
    }
  }

  Scaffold(
    modifier = modifier,
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopStatusBar(
        title =
          when (activeTab) {
            HomeTab.Chat -> chatTopBarTitle
            HomeTab.Contacts -> appName
            else -> tr(activeTab.labelEn, activeTab.labelZh)
          },
        activeTab = activeTab,
        onSelectTab = { activeTab = it },
        onBack = {
          when (activeTab) {
            HomeTab.Chat -> activeTab = HomeTab.Contacts
            HomeTab.Contacts -> activity?.finish()
            else -> activeTab = HomeTab.Chat
          }
        },
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
          .background(mobileBackground),
    ) {
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .edgeSwipeRight(
              enabled = activeTab == HomeTab.Chat || activeTab == HomeTab.Contacts,
              onSwipe = {
                when (activeTab) {
                  HomeTab.Chat -> activeTab = HomeTab.Contacts
                  HomeTab.Contacts -> activity?.finish()
                  else -> Unit
                }
              },
            ),
      ) {
        when (activeTab) {
          HomeTab.Contacts ->
            ContactsScreen(
              viewModel = viewModel,
              onOpenChat = { agentId ->
                viewModel.openAgentChat(agentId)
                activeTab = HomeTab.Chat
              },
            )
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
  activeTab: HomeTab,
  onSelectTab: (HomeTab) -> Unit,
  onBack: () -> Unit,
  onOpenAppInfo: () -> Unit,
) {
  val safeInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
  var menuExpanded by remember { mutableStateOf(false) }
  val showBackButton = activeTab != HomeTab.Contacts

  Surface(
    modifier = Modifier.fillMaxWidth().windowInsetsPadding(safeInsets),
    color = mobileBackground,
    shadowElevation = 0.dp,
  ) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
      Text(
        text = title,
        modifier = Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 56.dp),
        style = if (activeTab == HomeTab.Contacts) mobileTitle2 else mobileHeadline,
        color = mobileText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
          if (showBackButton) {
            IconButton(onClick = onBack, modifier = Modifier.size(34.dp)) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = tr("Back", "返回"),
                modifier = Modifier.size(20.dp),
                tint = mobileTextSecondary,
              )
            }
          }
        }
        Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
          IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(34.dp)) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = tr("More options", "更多选项"),
              modifier = Modifier.size(20.dp),
              tint = if (activeTab in overflowMenuTabs) mobileAccent else mobileTextSecondary,
            )
          }
          DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            shape = RoundedCornerShape(6.dp),
            containerColor = mobileSurface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, mobileBorder),
          ) {
            overflowMenuTabs.forEach { tab ->
              DropdownMenuItem(
                text = {
                  Text(
                    text = tr(tab.labelEn, tab.labelZh),
                    style = mobileBody.copy(fontWeight = if (tab == activeTab) FontWeight.Bold else FontWeight.Medium),
                    color = if (tab == activeTab) mobileAccent else mobileText,
                  )
                },
                colors = androidx.compose.material3.MenuDefaults.itemColors(textColor = if (tab == activeTab) mobileAccent else mobileText),
                onClick = {
                  menuExpanded = false
                  onSelectTab(tab)
                },
              )
            }
            DropdownMenuItem(
              text = {
                Text(
                  text = tr("About", "关于"),
                  style = mobileBody.copy(fontWeight = FontWeight.Medium),
                  color = mobileText,
                )
              },
              colors = androidx.compose.material3.MenuDefaults.itemColors(textColor = mobileText),
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

private fun Context.findActivity(): Activity? =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }

private fun Modifier.edgeSwipeRight(
  enabled: Boolean,
  onSwipe: () -> Unit,
): Modifier =
  if (!enabled) {
    this
  } else {
    pointerInput(onSwipe) {
      val edgeWidth = 32.dp.toPx()
      val triggerDistance = 96.dp.toPx()
      var tracking = false
      var totalDrag = 0f
      detectHorizontalDragGestures(
        onDragStart = { offset ->
          tracking = offset.x <= edgeWidth
          totalDrag = 0f
        },
        onHorizontalDrag = { change, dragAmount ->
          if (!tracking) return@detectHorizontalDragGestures
          if (dragAmount <= 0f) return@detectHorizontalDragGestures
          totalDrag += dragAmount
          if (totalDrag >= triggerDistance) {
            tracking = false
            totalDrag = 0f
            change.consume()
            onSwipe()
          }
        },
        onDragEnd = {
          tracking = false
          totalDrag = 0f
        },
        onDragCancel = {
          tracking = false
          totalDrag = 0f
        },
      )
    }
  }

private val HomeTab.persistedName: String
  get() =
    when (this) {
      HomeTab.Chat -> "chat"
      HomeTab.Contacts -> "contacts"
      HomeTab.Connect -> "connect"
      HomeTab.Screen -> "screen"
      HomeTab.Settings -> "settings"
    }

private fun String.toHomeTab(): HomeTab =
  when (trim().lowercase()) {
    "chat" -> HomeTab.Chat
    "connect" -> HomeTab.Connect
    "screen" -> HomeTab.Screen
    "settings" -> HomeTab.Settings
    else -> HomeTab.Contacts
  }
