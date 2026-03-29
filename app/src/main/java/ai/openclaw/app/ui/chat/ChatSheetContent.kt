package ai.openclaw.app.ui.chat

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.openclaw.app.MainViewModel
import ai.openclaw.app.chat.OutgoingAttachment
import ai.openclaw.app.chat.formatAgentContactTitle
import ai.openclaw.app.ui.mobileBorder
import ai.openclaw.app.ui.mobileCallout
import ai.openclaw.app.ui.mobileCaption2
import ai.openclaw.app.ui.mobileDanger
import ai.openclaw.app.ui.mobileDangerSoft
import ai.openclaw.app.ui.mobileText
import ai.openclaw.app.ui.mobileAccent
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ai.openclaw.app.ui.tr

@Composable
fun ChatSheetContent(viewModel: MainViewModel) {
  val messages by viewModel.chatMessages.collectAsState()
  val errorText by viewModel.chatError.collectAsState()
  val pendingRunCount by viewModel.pendingRunCount.collectAsState()
  val healthOk by viewModel.chatHealthOk.collectAsState()
  val chatSessionKey by viewModel.chatSessionKey.collectAsState()
  val thinkingLevel by viewModel.chatThinkingLevel.collectAsState()
  val streamingAssistantText by viewModel.chatStreamingAssistantText.collectAsState()
  val pendingToolCalls by viewModel.chatPendingToolCalls.collectAsState()
  val agentContacts by viewModel.agentContacts.collectAsState()
  val userLabel by viewModel.chatUserDisplayName.collectAsState()
  val abortSupported by viewModel.chatAbortSupported.collectAsState()
  val stopInFlight by viewModel.chatStopInFlight.collectAsState()
  val thinkingSupported by viewModel.chatThinkingSupported.collectAsState()
  val modelSupported by viewModel.chatModelSupported.collectAsState()
  val thinkingOptions by viewModel.chatThinkingOptions.collectAsState()
  val thinkingOptionsLoading by viewModel.chatThinkingOptionsLoading.collectAsState()
  val thinkingOptionsError by viewModel.chatThinkingOptionsError.collectAsState()
  val thinkingModelLabel by viewModel.chatThinkingModelLabel.collectAsState()
  val thinkingSwitchingLevel by viewModel.chatThinkingSwitchingLevel.collectAsState()
  val currentModel by viewModel.chatCurrentModel.collectAsState()
  val modelOptions by viewModel.chatModelOptions.collectAsState()
  val modelOptionsLoading by viewModel.chatModelOptionsLoading.collectAsState()
  val modelOptionsError by viewModel.chatModelOptionsError.collectAsState()
  val modelSwitchingLabel by viewModel.chatModelSwitchingLabel.collectAsState()
  val speakerEnabled by viewModel.speakerEnabled.collectAsState()

  LaunchedEffect(chatSessionKey) {
    viewModel.loadChat(chatSessionKey)
    viewModel.refreshChatSessions(limit = 200)
  }

  LaunchedEffect(chatSessionKey, messages.size) {
    viewModel.markChatSessionRead(chatSessionKey)
  }

  val context = LocalContext.current
  val resolver = context.contentResolver
  val scope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current

  val attachments = remember { mutableStateListOf<PendingAttachment>() }
  val assistantLabel =
    agentContacts.firstOrNull { it.directSessionKey == chatSessionKey }?.let { contact ->
      formatAgentContactTitle(displayName = contact.displayName, emoji = contact.emoji)
    } ?: tr("assistant", "助手")

  val pickAttachments =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
      if (uris.isNullOrEmpty()) return@rememberLauncherForActivityResult
      val slotsLeft = (MAX_PENDING_ATTACHMENTS - attachments.size).coerceAtLeast(0)
      if (slotsLeft == 0) return@rememberLauncherForActivityResult
      scope.launch(Dispatchers.IO) {
        val next =
          uris.take(slotsLeft).mapNotNull { uri ->
            try {
              loadAttachment(resolver, uri)
            } catch (_: Throwable) {
              null
            }
          }
        withContext(Dispatchers.Main) {
          attachments.addAll(next)
        }
      }
    }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(horizontal = 8.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    if (!errorText.isNullOrBlank()) {
      ChatErrorRail(
        errorText = errorText!!,
        onRetry = {
          viewModel.refreshChat()
          viewModel.refreshChatSessions(limit = 200)
        },
      )
    }

    ChatMessageListCard(
      sessionKey = chatSessionKey,
      messages = messages,
      pendingRunCount = pendingRunCount,
      pendingToolCalls = pendingToolCalls,
      streamingAssistantText = streamingAssistantText,
      healthOk = healthOk,
      assistantLabel = assistantLabel,
      userLabel = userLabel,
      onPullDown = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
      },
      onRetryUnavailable = {
        viewModel.refreshChat()
        viewModel.refreshChatSessions(limit = 200)
      },
      modifier = Modifier.weight(1f, fill = true),
    )

    Row(modifier = Modifier.fillMaxWidth().imePadding()) {
      ChatComposer(
        healthOk = healthOk,
        thinkingLevel = thinkingLevel,
        thinkingSupported = thinkingSupported && chatSessionKey.isNotBlank(),
        modelSupported = modelSupported && chatSessionKey.isNotBlank(),
        pendingRunCount = pendingRunCount,
        abortSupported = abortSupported,
        stopInFlight = stopInFlight,
        currentModel = currentModel,
        modelOptions = modelOptions,
        modelOptionsLoading = modelOptionsLoading,
        modelOptionsError = modelOptionsError,
        modelSwitchingLabel = modelSwitchingLabel,
        thinkingOptions = thinkingOptions,
        thinkingOptionsLoading = thinkingOptionsLoading,
        thinkingOptionsError = thinkingOptionsError,
        thinkingModelLabel = thinkingModelLabel,
        thinkingSwitchingLevel = thinkingSwitchingLevel,
        readoutEnabled = speakerEnabled,
        attachments = attachments,
        onPickAttachments = { pickAttachments.launch(arrayOf("*/*")) },
        onRemoveAttachment = { id -> attachments.removeAll { it.id == id } },
        onOpenModelMenu = { viewModel.refreshChatModelOptions(force = false, silent = false) },
        onSetModel = { provider, model -> viewModel.setChatModel(provider = provider, model = model) },
        onOpenThinkingMenu = { viewModel.refreshChatThinkingOptions(force = false, silent = false) },
        onSetThinkingLevel = { level -> viewModel.setChatThinkingLevel(level) },
        onToggleReadout = { enabled -> viewModel.setSpeakerEnabled(enabled) },
        onNew = {
          viewModel.sendChat(message = "/new", thinking = thinkingLevel, attachments = emptyList())
        },
        onAbort = { viewModel.abortChat() },
        onSend = { text ->
          val outgoing =
            attachments.map { att ->
              OutgoingAttachment(
                type = att.type,
                mimeType = att.mimeType,
                fileName = att.fileName,
                base64 = att.base64,
              )
            }
          viewModel.sendChat(message = text, thinking = thinkingLevel, attachments = outgoing)
          attachments.clear()
        },
      )
    }
  }
}

@Composable
private fun ChatErrorRail(errorText: String, onRetry: () -> Unit) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = mobileDangerSoft,
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, mobileDanger.copy(alpha = 0.35f)),
  ) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        text = tr("Chat issue", "聊天异常"),
        style = mobileCaption2.copy(letterSpacing = 0.6.sp),
        color = mobileDanger,
      )
      Text(text = errorText, style = mobileCallout, color = mobileText)
      TextButton(onClick = onRetry) {
        Text(tr("Retry", "重试"), color = mobileAccent, style = mobileCallout)
      }
    }
  }
}

private const val MAX_PENDING_ATTACHMENTS = 8

data class PendingAttachment(
  val id: String,
  val type: String,
  val fileName: String,
  val mimeType: String,
  val base64: String,
)

private suspend fun loadAttachment(resolver: ContentResolver, uri: Uri): PendingAttachment {
  val fileName = queryAttachmentFileName(resolver, uri)
  val detectedMimeType = resolver.getType(uri)?.trim()?.ifEmpty { null }
  val type = detectAttachmentType(mimeType = detectedMimeType, fileName = fileName)
  val mimeType = detectedMimeType ?: defaultMimeTypeForAttachmentType(type)
  val bytes =
    withContext(Dispatchers.IO) {
      resolver.openInputStream(uri)?.use { input ->
        val out = ByteArrayOutputStream()
        input.copyTo(out)
        out.toByteArray()
      } ?: ByteArray(0)
    }
  if (bytes.isEmpty()) throw IllegalStateException("empty attachment")
  val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
  return PendingAttachment(
    id = uri.toString() + "#" + System.currentTimeMillis().toString(),
    type = type,
    fileName = fileName,
    mimeType = mimeType,
    base64 = base64,
  )
}

private fun queryAttachmentFileName(resolver: ContentResolver, uri: Uri): String {
  resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    if (index >= 0 && cursor.moveToFirst()) {
      cursor.getString(index)?.trim()?.ifEmpty { null }?.let { return it }
    }
  }
  return (uri.lastPathSegment ?: "attachment").substringAfterLast('/')
}

private fun detectAttachmentType(mimeType: String?, fileName: String): String {
  val normalizedMime = mimeType?.trim()?.lowercase(Locale.US).orEmpty()
  val extension = fileName.substringAfterLast('.', "").trim().lowercase(Locale.US)

  return when {
    normalizedMime.startsWith("image/") || extension in imageExtensions -> "image"
    normalizedMime.startsWith("audio/") || extension in audioExtensions -> "audio"
    normalizedMime.startsWith("video/") || extension in videoExtensions -> "video"
    else -> "file"
  }
}

private fun defaultMimeTypeForAttachmentType(type: String): String {
  return when (type) {
    "image" -> "image/*"
    "audio" -> "audio/*"
    "video" -> "video/*"
    else -> "application/octet-stream"
  }
}

private val imageExtensions =
  setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg")

private val audioExtensions =
  setOf("mp3", "wav", "m4a", "aac", "ogg", "opus", "flac", "webm")

private val videoExtensions =
  setOf("mp4", "mpeg", "mov", "webm", "mkv", "avi", "3gp")
