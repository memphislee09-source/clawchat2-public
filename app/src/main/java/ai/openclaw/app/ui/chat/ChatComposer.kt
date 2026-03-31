package ai.openclaw.app.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.openclaw.app.chat.ChatThinkingOption
import ai.openclaw.app.chat.ChatModelOption
import ai.openclaw.app.ui.mobileAccent
import ai.openclaw.app.ui.mobileAccentSoft
import ai.openclaw.app.ui.mobileBorder
import ai.openclaw.app.ui.mobileCallout
import ai.openclaw.app.ui.mobileCaption1
import ai.openclaw.app.ui.mobileDanger
import ai.openclaw.app.ui.mobileSurfaceStrong
import ai.openclaw.app.ui.mobileText
import ai.openclaw.app.ui.mobileTextSecondary
import ai.openclaw.app.ui.mobileTextTertiary
import ai.openclaw.app.ui.tr

@Composable
fun ChatComposer(
  healthOk: Boolean,
  thinkingLevel: String,
  thinkingSupported: Boolean,
  modelSupported: Boolean,
  pendingRunCount: Int,
  abortSupported: Boolean,
  stopInFlight: Boolean,
  currentModel: ChatModelOption?,
  modelOptions: List<ChatModelOption>,
  modelOptionsLoading: Boolean,
  modelOptionsError: String?,
  modelSwitchingLabel: String?,
  thinkingOptions: List<ChatThinkingOption>,
  thinkingOptionsLoading: Boolean,
  thinkingOptionsError: String?,
  thinkingModelLabel: String?,
  thinkingSwitchingLevel: String?,
  readoutEnabled: Boolean,
  attachments: List<PendingAttachment>,
  onPickAttachments: () -> Unit,
  onRemoveAttachment: (id: String) -> Unit,
  onOpenModelMenu: () -> Unit,
  onSetModel: (provider: String, model: String) -> Unit,
  onOpenThinkingMenu: () -> Unit,
  onSetThinkingLevel: (level: String) -> Unit,
  onToggleReadout: (Boolean) -> Unit,
  onNew: () -> Unit,
  onAbort: () -> Unit,
  onInputFocusChanged: (Boolean) -> Unit = {},
  onSend: (text: String) -> Unit,
) {
  var input by rememberSaveable { mutableStateOf("") }
  var showModelMenu by remember { mutableStateOf(false) }
  var showThinkingMenu by remember { mutableStateOf(false) }

  val sendBusy = pendingRunCount > 0
  val hasDraft = input.trim().isNotEmpty() || attachments.isNotEmpty()
  val sendEnabled = if (sendBusy) abortSupported && !stopInFlight else hasDraft && healthOk
  val newEnabled = !sendBusy && !stopInFlight && healthOk
  val sendContentDescription = if (sendBusy) tr("Stop current reply", "停止当前回复") else tr("Send message", "发送消息")
  val modelButtonEnabled = modelSupported && !sendBusy && !stopInFlight
  val thinkingButtonEnabled = thinkingSupported && !sendBusy && !stopInFlight
  val sendTint = if (sendBusy) mobileDanger else mobileAccent

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (attachments.isNotEmpty()) {
      AttachmentsStrip(attachments = attachments, onRemoveAttachment = onRemoveAttachment)
    }

    OutlinedTextField(
      value = input,
      onValueChange = { input = it },
      modifier =
        Modifier
          .fillMaxWidth()
          .heightIn(min = 56.dp, max = 156.dp)
          .onFocusChanged { state -> onInputFocusChanged(state.isFocused) },
      placeholder = { Text(tr("Type a message", "输入消息"), style = mobileBodyStyle(), color = mobileTextTertiary) },
      minLines = 1,
      maxLines = 6,
      textStyle = mobileBodyStyle().copy(color = mobileText),
      shape = RoundedCornerShape(12.dp),
      colors = chatTextFieldColors(),
    )

    if (!healthOk) {
      Text(
        text = tr("Chat is temporarily unavailable. Check the gateway connection and try again.", "聊天暂时不可用。检查网关连接后再试。"),
        style = mobileCallout,
        color = ai.openclaw.app.ui.mobileWarning,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      CompactIconAction(
        modifier = Modifier.weight(1f),
        icon = Icons.Default.AttachFile,
        contentDescription = tr("Add attachment", "添加附件"),
        enabled = true,
        highlighted = attachments.isNotEmpty(),
        tint = if (attachments.isNotEmpty()) mobileAccent else mobileTextSecondary,
        onClick = onPickAttachments,
        content = {
          Icon(
            imageVector = Icons.Default.AttachFile,
            contentDescription = null,
            modifier =
              Modifier
                .size(20.dp)
                .graphicsLayer(rotationZ = 30f),
            tint = if (attachments.isNotEmpty()) mobileAccent else mobileTextSecondary,
          )
        },
      )
      CompactIconAction(
        modifier = Modifier.weight(1f),
        icon = Icons.Default.Refresh,
        contentDescription = tr("Start a new chat", "开始新会话"),
        enabled = newEnabled,
        highlighted = false,
        tint = mobileTextSecondary,
        onClick = {
          showModelMenu = false
          showThinkingMenu = false
          onNew()
        },
      )
      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        CompactIconAction(
          modifier = Modifier,
          icon = Icons.Default.AutoAwesome,
          contentDescription =
            currentModel?.label?.trim()?.takeIf { it.isNotEmpty() }?.let {
              tr("Model: $it", "模型：$it")
            } ?: tr("Model options", "模型选项"),
          enabled = modelButtonEnabled,
          highlighted = showModelMenu || currentModel != null,
          tint = if (showModelMenu || currentModel != null) mobileAccent else mobileTextSecondary,
          onClick = {
            showModelMenu = !showModelMenu
            if (showModelMenu) {
              showThinkingMenu = false
              onOpenModelMenu()
            }
          },
        )
        DropdownMenu(
          expanded = modelButtonEnabled && showModelMenu,
          onDismissRequest = { showModelMenu = false },
          modifier = Modifier.width(280.dp),
          shape = RoundedCornerShape(12.dp),
          containerColor = mobileSurfaceStrong,
          tonalElevation = 0.dp,
          shadowElevation = 6.dp,
          border = androidx.compose.foundation.BorderStroke(1.dp, mobileBorder.copy(alpha = 0.32f)),
        ) {
          ModelMenuStatus(
            loading = modelOptionsLoading,
            errorText = modelOptionsError,
            switchingLabel = modelSwitchingLabel,
          )
          if (!modelOptionsLoading && modelOptions.isEmpty()) {
            ModelMenuEmptyState(message = modelOptionsError ?: tr("No models are available right now.", "当前没有可切换的模型。"))
          } else {
            modelOptions.forEach { option ->
              ModelMenuItem(
                option = option,
                currentModel = currentModel,
                switchingLabel = modelSwitchingLabel,
                onSet = onSetModel,
              )
            }
          }
        }
      }
      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        CompactIconAction(
          modifier = Modifier,
          icon = Icons.Default.Tune,
          contentDescription =
            tr(
              "Thinking: ${thinkingLevel.trim().ifEmpty { tr("off", "关闭") }}",
              "思考：${thinkingLevel.trim().ifEmpty { tr("off", "关闭") }}",
            ),
          enabled = thinkingButtonEnabled,
          highlighted = showThinkingMenu || thinkingLevel.isNotBlank(),
          tint = if (showThinkingMenu || thinkingLevel.isNotBlank()) mobileAccent else mobileTextSecondary,
          onClick = {
            showThinkingMenu = !showThinkingMenu
            if (showThinkingMenu) {
              showModelMenu = false
              onOpenThinkingMenu()
            }
          },
        )
        DropdownMenu(
          expanded = thinkingButtonEnabled && showThinkingMenu,
          onDismissRequest = { showThinkingMenu = false },
          modifier = Modifier.width(240.dp),
          shape = RoundedCornerShape(12.dp),
          containerColor = mobileSurfaceStrong,
          tonalElevation = 0.dp,
          shadowElevation = 6.dp,
          border = androidx.compose.foundation.BorderStroke(1.dp, mobileBorder.copy(alpha = 0.32f)),
        ) {
          ThinkingMenuHeader(
            currentLevel = thinkingLevel,
            modelLabel = thinkingModelLabel,
            loading = thinkingOptionsLoading,
            errorText = thinkingOptionsError,
          )
          if (!thinkingOptionsLoading && thinkingOptions.isEmpty()) {
            ThinkingMenuEmptyState(message = thinkingOptionsError ?: tr("No thinking options are available for this model.", "当前模型没有可切换的思考选项。"))
          } else {
            thinkingOptions.forEach { option ->
              ThinkingMenuItem(
                option = option,
                current = thinkingLevel,
                switchingLevel = thinkingSwitchingLevel,
                onSet = onSetThinkingLevel,
              )
            }
          }
        }
      }
      CompactIconAction(
        modifier = Modifier.weight(1f),
        icon = if (readoutEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
        contentDescription = if (readoutEnabled) tr("Disable readout", "关闭朗读") else tr("Enable readout", "开启朗读"),
        enabled = true,
        highlighted = readoutEnabled,
        tint = if (readoutEnabled) mobileAccent else mobileTextSecondary,
        onClick = { onToggleReadout(!readoutEnabled) },
      )
      CompactIconAction(
        modifier = Modifier.weight(1f),
        icon = if (sendBusy) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
        contentDescription = sendContentDescription,
        enabled = sendEnabled,
        highlighted = sendEnabled,
        tint = if (sendBusy) mobileDanger else sendTint,
        onClick = {
          if (sendBusy) {
            onAbort()
          } else {
            val text = input
            input = ""
            onSend(text)
          }
        },
        content = {
          if (stopInFlight) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = mobileDanger)
          } else {
            Icon(
              imageVector = if (sendBusy) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
              tint =
                when {
                  !sendEnabled -> mobileTextTertiary
                  sendBusy -> mobileDanger
                  else -> mobileAccent
                },
            )
          }
        },
      )
    }
  }
}

@Composable
private fun CompactIconAction(
  modifier: Modifier,
  icon: ImageVector,
  contentDescription: String,
  enabled: Boolean,
  highlighted: Boolean,
  tint: Color,
  onClick: () -> Unit,
  content: @Composable (() -> Unit)? = null,
) {
  Box(
    modifier =
      modifier
        .semantics { this.contentDescription = contentDescription }
        .clickable(enabled = enabled, onClick = onClick)
        .sizeIn(minHeight = 40.dp),
    contentAlignment = Alignment.Center,
  ) {
    if (content != null) {
      content()
    } else {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint =
          when {
            !enabled -> mobileTextTertiary
            highlighted -> tint
            else -> tint
          },
      )
    }
  }
}

@Composable
private fun ModelMenuStatus(
  loading: Boolean,
  errorText: String?,
  switchingLabel: String?,
) {
  val message =
    when {
      errorText != null -> errorText
      switchingLabel != null -> tr("Switching model to $switchingLabel…", "正在切换模型到 $switchingLabel…")
      loading -> tr("Loading model options…", "正在加载模型选项…")
      else -> null
    } ?: return

  Text(
    text = message,
    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
    style = mobileCallout,
    color = if (errorText != null) mobileDanger else mobileTextSecondary,
  )
}

@Composable
private fun ModelMenuEmptyState(message: String) {
  Text(
    text = message,
    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
    style = mobileCallout,
    color = mobileTextSecondary,
  )
}

@Composable
private fun ModelMenuItem(
  option: ChatModelOption,
  currentModel: ChatModelOption?,
  switchingLabel: String?,
  onSet: (provider: String, model: String) -> Unit,
) {
  val isCurrent = currentModel?.provider == option.provider && currentModel.model == option.model
  val isSwitching = switchingLabel == option.label

  DropdownMenuItem(
    text = {
      Text(
        text = option.label,
        style = mobileCallout,
        color = if (isCurrent) mobileAccent else mobileText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    },
    onClick = {
      if (!isCurrent && !isSwitching) {
        onSet(option.provider, option.model)
      }
    },
    enabled = option.available && !isSwitching,
    trailingIcon = {
      when {
        isSwitching -> Text(tr("Switching", "切换中"), style = mobileCallout, color = mobileTextSecondary)
        isCurrent -> Text("✓", style = mobileCallout, color = mobileAccent)
      }
    },
  )
}

@Composable
private fun ThinkingMenuHeader(
  currentLevel: String,
  modelLabel: String?,
  loading: Boolean,
  errorText: String?,
) {
  Column(
    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(text = tr("Thinking", "思考"), style = mobileCaption1, color = mobileTextSecondary)
    Text(
      text = modelLabel?.takeIf { it.isNotBlank() } ?: tr("Choose a thinking level for this agent's current model.", "选择这个 agent 当前模型的思考等级。"),
      style = mobileCallout,
      color = mobileText,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = tr("Current thinking: ${currentLevel.ifBlank { tr("off", "关闭") }}", "当前思考：${currentLevel.ifBlank { tr("off", "关闭") }}"),
      style = mobileCallout,
      color = mobileAccent,
      maxLines = 1,
    )
    when {
      errorText != null -> {
        Text(text = errorText, style = mobileCallout, color = mobileDanger)
      }
      loading -> {
        Text(text = tr("Loading thinking options…", "正在加载思考选项…"), style = mobileCallout, color = mobileTextSecondary)
      }
    }
  }
}

@Composable
private fun ThinkingMenuEmptyState(message: String) {
  Text(
    text = message,
    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
    style = mobileCallout,
    color = mobileTextSecondary,
  )
}

@Composable
private fun ThinkingMenuItem(
  option: ChatThinkingOption,
  current: String,
  switchingLevel: String?,
  onSet: (String) -> Unit,
) {
  val currentValue = current.trim().lowercase()
  val optionValue = option.value.trim().lowercase()
  val isCurrent = optionValue == currentValue
  val isSwitching = switchingLevel?.trim()?.lowercase() == optionValue

  DropdownMenuItem(
    text = {
      Text(
        text = option.label,
        style = mobileCallout,
        color = if (isCurrent) mobileAccent else mobileText,
        maxLines = 1,
      )
    },
    onClick = {
      if (!isCurrent && !isSwitching) {
        onSet(option.value)
      }
    },
    enabled = !isSwitching,
    trailingIcon = {
      when {
        isSwitching -> Text(tr("Switching", "切换中"), style = mobileCallout, color = mobileTextSecondary)
        isCurrent ->
        Text("✓", style = mobileCallout, color = mobileAccent)
      }
    },
  )
}

@Composable
private fun AttachmentsStrip(
  attachments: List<PendingAttachment>,
  onRemoveAttachment: (id: String) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    for (att in attachments) {
      AttachmentChip(
        type = att.type,
        fileName = att.fileName,
        onRemove = { onRemoveAttachment(att.id) },
      )
    }
  }
}

@Composable
private fun AttachmentChip(type: String, fileName: String, onRemove: () -> Unit) {
  Surface(
    shape = RoundedCornerShape(999.dp),
    color = mobileSurfaceStrong,
    border = BorderStroke(1.dp, mobileBorder.copy(alpha = 0.24f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector =
          when (type.trim().lowercase()) {
            "image" -> Icons.Default.Image
            "audio" -> Icons.Default.Audiotrack
            "video" -> Icons.Default.Videocam
            else -> Icons.Default.AttachFile
          },
        contentDescription = null,
        tint = mobileTextSecondary,
        modifier = Modifier.size(14.dp),
      )
      Text(
        text = fileName,
        style = mobileCaption1,
        color = mobileText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Surface(
        onClick = onRemove,
        shape = RoundedCornerShape(999.dp),
        color = mobileAccentSoft.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, mobileBorder.copy(alpha = 0.18f)),
      ) {
        Text(
          text = "×",
          style = mobileCaption1.copy(fontWeight = FontWeight.Bold),
          color = mobileTextSecondary,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
      }
    }
  }
}

@Composable
private fun chatTextFieldColors() =
  OutlinedTextFieldDefaults.colors(
    focusedContainerColor = mobileSurfaceStrong,
    unfocusedContainerColor = mobileSurfaceStrong,
    focusedBorderColor = mobileAccent.copy(alpha = 0.30f),
    unfocusedBorderColor = mobileBorder.copy(alpha = 0.18f),
    focusedTextColor = mobileText,
    unfocusedTextColor = mobileText,
    cursorColor = mobileAccent,
  )

@Composable
private fun mobileBodyStyle() =
  MaterialTheme.typography.bodyMedium.copy(
    fontFamily = ai.openclaw.app.ui.mobileFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    lineHeight = 22.sp,
  )
