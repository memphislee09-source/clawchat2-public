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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.openclaw.app.chat.ChatThinkingOption
import ai.openclaw.app.chat.ChatModelOption
import ai.openclaw.app.ui.mobileAccent
import ai.openclaw.app.ui.mobileAccentSoft
import ai.openclaw.app.ui.mobileBorder
import ai.openclaw.app.ui.mobileBorderStrong
import ai.openclaw.app.ui.mobileCallout
import ai.openclaw.app.ui.mobileCaption1
import ai.openclaw.app.ui.mobileDanger
import ai.openclaw.app.ui.mobileHeadline
import ai.openclaw.app.ui.mobileSurface
import ai.openclaw.app.ui.mobileText
import ai.openclaw.app.ui.mobileTextSecondary
import ai.openclaw.app.ui.mobileTextTertiary

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
  onSend: (text: String) -> Unit,
) {
  var input by rememberSaveable { mutableStateOf("") }
  var showModelMenu by remember { mutableStateOf(false) }
  var showThinkingMenu by remember { mutableStateOf(false) }

  val sendBusy = pendingRunCount > 0
  val hasDraft = input.trim().isNotEmpty() || attachments.isNotEmpty()
  val sendEnabled = if (sendBusy) abortSupported && !stopInFlight else hasDraft && healthOk
  val newEnabled = !sendBusy && !stopInFlight && healthOk
  val sendContentDescription = if (sendBusy) "Stop current reply" else "Send message"
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
      modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp, max = 156.dp),
      placeholder = { Text("输入消息", style = mobileBodyStyle(), color = mobileTextTertiary) },
      minLines = 1,
      maxLines = 6,
      textStyle = mobileBodyStyle().copy(color = mobileText),
      shape = RoundedCornerShape(6.dp),
      colors = chatTextFieldColors(),
    )

    if (!healthOk) {
      Text(
        text = "Chat service unavailable. Reconnect the gateway or reopen this chat.",
        style = mobileCallout,
        color = ai.openclaw.app.ui.mobileWarning,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(2.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FlatIconAction(
        modifier = Modifier.weight(1f),
        icon = Icons.Default.AttachFile,
        contentDescription = "Add file attachment",
        enabled = true,
        tint = mobileTextSecondary,
        onClick = onPickAttachments,
      )
      FlatIconAction(
        modifier = Modifier.weight(1f),
        icon = if (sendBusy) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
        contentDescription = sendContentDescription,
        enabled = sendEnabled,
        tint = sendTint,
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
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = mobileDanger)
          } else if (sendBusy) {
            Icon(
              imageVector = Icons.Default.Stop,
              contentDescription = null,
              modifier = Modifier.size(22.dp),
              tint = if (sendEnabled) mobileDanger else mobileTextTertiary,
            )
          } else {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = null,
              modifier = Modifier.size(22.dp),
              tint = if (sendEnabled) mobileAccent else mobileTextTertiary,
            )
          }
        },
      )
      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Text(
          text = "N",
          modifier =
            Modifier
              .clickable(enabled = newEnabled) {
                showModelMenu = false
                showThinkingMenu = false
                onNew()
              }
              .padding(horizontal = 8.dp, vertical = 10.dp),
          style = mobileHeadline.copy(fontWeight = FontWeight.Bold),
          color = if (newEnabled) mobileAccent else mobileTextTertiary,
          textAlign = TextAlign.Center,
        )
      }
      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Text(
          text = "M",
          modifier =
            Modifier
              .clickable(enabled = modelButtonEnabled) {
                showModelMenu = !showModelMenu
                if (showModelMenu) {
                  showThinkingMenu = false
                  onOpenModelMenu()
                }
              }
              .padding(horizontal = 8.dp, vertical = 10.dp),
          style = mobileHeadline.copy(fontWeight = FontWeight.Bold),
          color = if (modelButtonEnabled) mobileAccent else mobileTextTertiary,
          textAlign = TextAlign.Center,
        )
        DropdownMenu(
          expanded = modelButtonEnabled && showModelMenu,
          onDismissRequest = { showModelMenu = false },
          modifier = Modifier.width(260.dp),
          shape = RoundedCornerShape(6.dp),
          containerColor = mobileSurface,
          tonalElevation = 0.dp,
          shadowElevation = 0.dp,
          border = BorderStroke(1.dp, mobileBorder),
        ) {
          ModelMenuStatus(
            loading = modelOptionsLoading,
            errorText = modelOptionsError,
            switchingLabel = modelSwitchingLabel,
          )
          if (!modelOptionsLoading && modelOptions.isEmpty()) {
            ModelMenuEmptyState(message = modelOptionsError ?: "当前没有可切换的模型。")
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
        Text(
          text = "T",
          modifier =
            Modifier
              .clickable(enabled = thinkingButtonEnabled) {
                showThinkingMenu = !showThinkingMenu
                if (showThinkingMenu) {
                  showModelMenu = false
                  onOpenThinkingMenu()
                }
              }
              .padding(horizontal = 8.dp, vertical = 10.dp),
          style = mobileHeadline.copy(fontWeight = FontWeight.Bold),
          color = if (thinkingButtonEnabled) mobileAccent else mobileTextTertiary,
          textAlign = TextAlign.Center,
        )
        DropdownMenu(
          expanded = thinkingButtonEnabled && showThinkingMenu,
          onDismissRequest = { showThinkingMenu = false },
          modifier = Modifier.width(220.dp),
          shape = RoundedCornerShape(6.dp),
          containerColor = mobileSurface,
          tonalElevation = 0.dp,
          shadowElevation = 0.dp,
          border = BorderStroke(1.dp, mobileBorder),
        ) {
          ThinkingMenuHeader(
            currentLevel = thinkingLevel,
            modelLabel = thinkingModelLabel,
            loading = thinkingOptionsLoading,
            errorText = thinkingOptionsError,
          )
          if (!thinkingOptionsLoading && thinkingOptions.isEmpty()) {
            ThinkingMenuEmptyState(message = thinkingOptionsError ?: "当前模型没有可切换的 thinking 选项。")
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
      FlatIconAction(
        modifier = Modifier.weight(1f),
        icon = if (readoutEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
        contentDescription = if (readoutEnabled) "Disable reply readout" else "Enable reply readout",
        enabled = true,
        tint = if (readoutEnabled) mobileAccent else mobileTextSecondary,
        onClick = { onToggleReadout(!readoutEnabled) },
      )
    }
  }
}

@Composable
private fun FlatIconAction(
  modifier: Modifier,
  icon: ImageVector,
  contentDescription: String,
  enabled: Boolean,
  tint: Color,
  onClick: () -> Unit,
  content: @Composable (() -> Unit)? = null,
) {
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    IconButton(onClick = onClick, enabled = enabled) {
      if (content != null) {
        content()
      } else {
        Icon(
          imageVector = icon,
          contentDescription = contentDescription,
          modifier = Modifier.size(22.dp),
          tint = if (enabled) tint else mobileTextTertiary,
        )
      }
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
      switchingLabel != null -> "正在切换模型到 $switchingLabel…"
      loading -> "正在加载模型选项…"
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
        isSwitching -> Text("切换中", style = mobileCallout, color = mobileTextSecondary)
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
    Text(text = "Thinking", style = mobileCaption1, color = mobileTextSecondary)
    Text(
      text = modelLabel?.takeIf { it.isNotBlank() } ?: "选择这个 agent 当前模型的 thinking level。",
      style = mobileCallout,
      color = mobileText,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = "当前 thinking：${currentLevel.ifBlank { "off" }}",
      style = mobileCallout,
      color = mobileAccent,
      maxLines = 1,
    )
    when {
      errorText != null -> {
        Text(text = errorText, style = mobileCallout, color = mobileDanger)
      }
      loading -> {
        Text(text = "正在加载 thinking 选项…", style = mobileCallout, color = mobileTextSecondary)
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
        isSwitching -> Text("切换中", style = mobileCallout, color = mobileTextSecondary)
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
    color = mobileAccentSoft,
    border = BorderStroke(1.dp, mobileBorderStrong),
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
        color = mobileSurface,
        border = BorderStroke(1.dp, mobileBorderStrong),
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
    focusedContainerColor = mobileSurface,
    unfocusedContainerColor = mobileSurface,
    focusedBorderColor = mobileBorderStrong,
    unfocusedBorderColor = mobileBorder,
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
