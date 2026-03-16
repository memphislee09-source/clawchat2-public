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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
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
import ai.openclaw.app.ui.mobileAccent
import ai.openclaw.app.ui.mobileAccentSoft
import ai.openclaw.app.ui.mobileBorder
import ai.openclaw.app.ui.mobileBorderStrong
import ai.openclaw.app.ui.mobileCallout
import ai.openclaw.app.ui.mobileCaption1
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
  pendingRunCount: Int,
  abortSupported: Boolean,
  voiceSupported: Boolean,
  attachments: List<PendingImageAttachment>,
  onPickImages: () -> Unit,
  onRemoveAttachment: (id: String) -> Unit,
  onSetThinkingLevel: (level: String) -> Unit,
  onRefresh: () -> Unit,
  onOpenVoice: () -> Unit,
  onAbort: () -> Unit,
  onSend: (text: String) -> Unit,
) {
  var input by rememberSaveable { mutableStateOf("") }
  var showThinkingMenu by remember { mutableStateOf(false) }

  val canSend = pendingRunCount == 0 && (input.trim().isNotEmpty() || attachments.isNotEmpty()) && healthOk
  val sendBusy = pendingRunCount > 0

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
      modifier = Modifier.fillMaxWidth().heightIn(min = 82.dp, max = 118.dp),
      placeholder = { Text("输入消息", style = mobileBodyStyle(), color = mobileTextTertiary) },
      minLines = 2,
      maxLines = 5,
      textStyle = mobileBodyStyle().copy(color = mobileText),
      shape = RoundedCornerShape(6.dp),
      colors = chatTextFieldColors(),
    )

  if (!healthOk) {
      Text(
        text = "Chat service unavailable. Pull to refresh.",
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
        icon = Icons.Default.Refresh,
        contentDescription = "Refresh chat",
        enabled = true,
        tint = mobileTextSecondary,
        onClick = onRefresh,
      )
      FlatIconAction(
        modifier = Modifier.weight(1f),
        icon = Icons.Default.AttachFile,
        contentDescription = "Add attachment",
        enabled = true,
        tint = mobileTextSecondary,
        onClick = onPickImages,
      )
      FlatIconAction(
        modifier = Modifier.weight(1f),
        icon = Icons.AutoMirrored.Filled.Send,
        contentDescription = "Send message",
        enabled = canSend,
        tint = mobileAccent,
        onClick = {
          val text = input
          input = ""
          onSend(text)
        },
        content = {
          if (sendBusy) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = mobileAccent)
          } else {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = null,
              modifier = Modifier.size(22.dp),
              tint = if (canSend) mobileAccent else mobileTextTertiary,
            )
          }
        },
      )
      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Text(
          text = "T",
          modifier =
            Modifier
              .clickable(enabled = thinkingSupported) { showThinkingMenu = !showThinkingMenu }
              .padding(horizontal = 8.dp, vertical = 10.dp),
          style = mobileHeadline.copy(fontWeight = FontWeight.Bold),
          color = if (thinkingSupported) mobileAccent else mobileTextTertiary,
          textAlign = TextAlign.Center,
        )
        DropdownMenu(
          expanded = thinkingSupported && showThinkingMenu,
          onDismissRequest = { showThinkingMenu = false },
          modifier = Modifier.width(148.dp),
          shape = RoundedCornerShape(6.dp),
          containerColor = mobileSurface,
          tonalElevation = 0.dp,
          shadowElevation = 0.dp,
          border = BorderStroke(1.dp, mobileBorder),
        ) {
          ThinkingMenuItem("off", thinkingLevel, onSetThinkingLevel) { showThinkingMenu = false }
          ThinkingMenuItem("low", thinkingLevel, onSetThinkingLevel) { showThinkingMenu = false }
          ThinkingMenuItem("medium", thinkingLevel, onSetThinkingLevel) { showThinkingMenu = false }
          ThinkingMenuItem("high", thinkingLevel, onSetThinkingLevel) { showThinkingMenu = false }
        }
      }
      FlatIconAction(
        modifier = Modifier.weight(1f),
        icon = Icons.Default.Mic,
        contentDescription = "Open voice conversation",
        enabled = healthOk && voiceSupported,
        tint = mobileAccent,
        onClick = onOpenVoice,
      )
      FlatIconAction(
        modifier = Modifier.weight(1f),
        icon = Icons.Default.Stop,
        contentDescription = "Abort response",
        enabled = abortSupported && pendingRunCount > 0,
        tint = if (abortSupported && pendingRunCount > 0) mobileAccent else mobileTextTertiary,
        onClick = onAbort,
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
private fun ThinkingMenuItem(
  value: String,
  current: String,
  onSet: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  DropdownMenuItem(
    text = {
      Text(
        text = thinkingLabel(value),
        style = mobileCallout,
        color = if (value == current.trim().lowercase()) mobileAccent else mobileText,
        maxLines = 1,
      )
    },
    onClick = {
      onSet(value)
      onDismiss()
    },
    trailingIcon = {
      if (value == current.trim().lowercase()) {
        Text("✓", style = mobileCallout, color = mobileAccent)
      }
    },
  )
}

private fun thinkingLabel(raw: String): String {
  return when (raw.trim().lowercase()) {
    "low" -> "Low"
    "medium" -> "Medium"
    "high" -> "High"
    else -> "Off"
  }
}

@Composable
private fun AttachmentsStrip(
  attachments: List<PendingImageAttachment>,
  onRemoveAttachment: (id: String) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    for (att in attachments) {
      AttachmentChip(
        fileName = att.fileName,
        onRemove = { onRemoveAttachment(att.id) },
      )
    }
  }
}

@Composable
private fun AttachmentChip(fileName: String, onRemove: () -> Unit) {
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
