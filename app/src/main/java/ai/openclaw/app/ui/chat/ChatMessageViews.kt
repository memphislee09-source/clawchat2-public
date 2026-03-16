package ai.openclaw.app.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.openclaw.app.chat.ChatMessage
import ai.openclaw.app.chat.ChatMessageContent
import ai.openclaw.app.chat.ChatPendingToolCall
import ai.openclaw.app.chat.formatAgentContactTitle
import ai.openclaw.app.tools.ToolDisplayRegistry
import ai.openclaw.app.ui.mobileAccent
import ai.openclaw.app.ui.mobileAccentSoft
import ai.openclaw.app.ui.mobileBorder
import ai.openclaw.app.ui.mobileBorderStrong
import ai.openclaw.app.ui.mobileCallout
import ai.openclaw.app.ui.mobileCaption1
import ai.openclaw.app.ui.mobileCaption2
import ai.openclaw.app.ui.mobileCodeBg
import ai.openclaw.app.ui.mobileCodeText
import ai.openclaw.app.ui.mobileHeadline
import ai.openclaw.app.ui.mobileSurface
import ai.openclaw.app.ui.mobileText
import ai.openclaw.app.ui.mobileTextSecondary
import ai.openclaw.app.ui.mobileWarning
import ai.openclaw.app.ui.mobileWarningSoft
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class ChatBubbleStyle(
  val alignEnd: Boolean,
  val containerColor: Color,
  val borderColor: Color,
  val roleColor: Color,
  val roleTextAlign: TextAlign,
)

@Composable
fun ChatMessageBubble(message: ChatMessage, assistantLabel: String = "assistant") {
  val role = message.role.trim().lowercase(Locale.US)
  val style = bubbleStyle(role)

  // Filter to text content or attachment-like parts with at least some metadata.
  val displayableContent =
    message.content.filter { part ->
      when (part.type) {
        "text" -> !part.text.isNullOrBlank()
        else ->
          !part.base64.isNullOrBlank() ||
            !part.mediaUrl.isNullOrBlank() ||
            !part.fileName.isNullOrBlank() ||
            !part.mimeType.isNullOrBlank()
      }
    }

  if (displayableContent.isEmpty()) return

  ChatBubbleContainer(
    style = style,
    roleLabel = roleLabel(role, assistantLabel),
    timestampLabel = formatBubbleTimestamp(message.timestampMs),
  ) {
    ChatMessageBody(content = displayableContent, textColor = mobileText)
  }
}

@Composable
private fun ChatBubbleContainer(
  style: ChatBubbleStyle,
  roleLabel: String,
  modifier: Modifier = Modifier,
  timestampLabel: String? = null,
  content: @Composable () -> Unit,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = if (style.alignEnd) Arrangement.End else Arrangement.Start,
  ) {
    Surface(
      shape = RoundedCornerShape(6.dp),
      border = BorderStroke(1.dp, style.borderColor),
      color = style.containerColor,
      tonalElevation = 0.dp,
      shadowElevation = 0.dp,
      modifier = Modifier.fillMaxWidth(0.99f),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text =
            if (timestampLabel.isNullOrBlank()) {
              roleLabel
            } else {
              "$roleLabel  $timestampLabel"
            },
          modifier = Modifier.fillMaxWidth(),
          style = mobileCaption2.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp),
          color = style.roleColor,
          textAlign = style.roleTextAlign,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        content()
      }
    }
  }
}

@Composable
private fun ChatMessageBody(content: List<ChatMessageContent>, textColor: Color) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    for (part in content) {
      when (part.type) {
        "text" -> {
          val text = part.text ?: continue
          SelectionContainer(modifier = Modifier.fillMaxWidth()) {
            ChatMarkdown(text = text, textColor = textColor)
          }
        }
        else -> {
          ChatMediaAttachment(descriptor = part.toAttachmentDescriptor())
        }
      }
    }
  }
}

@Composable
fun ChatTypingIndicatorBubble(assistantLabel: String = "assistant") {
  ChatBubbleContainer(
    style = bubbleStyle("assistant"),
    roleLabel = roleLabel("assistant", assistantLabel),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      DotPulse(color = mobileTextSecondary)
      Text("Thinking...", style = mobileCallout, color = mobileTextSecondary)
    }
  }
}

@Composable
fun ChatPendingToolsBubble(toolCalls: List<ChatPendingToolCall>, assistantLabel: String = "assistant") {
  val context = LocalContext.current
  val displays =
    remember(toolCalls, context) {
      toolCalls.map { ToolDisplayRegistry.resolve(context, it.name, it.args) }
    }

  ChatBubbleContainer(
    style = bubbleStyle("assistant"),
    roleLabel = "$assistantLabel · tools",
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text("Running tools...", style = mobileCaption1.copy(fontWeight = FontWeight.SemiBold), color = mobileTextSecondary)
      for (display in displays.take(6)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            "${display.emoji} ${display.label}",
            style = mobileCallout,
            color = mobileTextSecondary,
            fontFamily = FontFamily.Monospace,
          )
          display.detailLine?.let { detail ->
            Text(
              detail,
              style = mobileCaption1,
              color = mobileTextSecondary,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }
      if (toolCalls.size > 6) {
        Text(
          text = "... +${toolCalls.size - 6} more",
          style = mobileCaption1,
          color = mobileTextSecondary,
        )
      }
    }
  }
}

@Composable
fun ChatStreamingAssistantBubble(text: String, assistantLabel: String = "assistant") {
  ChatBubbleContainer(
    style = bubbleStyle("assistant").copy(borderColor = mobileAccent.copy(alpha = 0.4f)),
    roleLabel = "$assistantLabel · live",
  ) {
    ChatMarkdown(text = text, textColor = mobileText)
  }
}

private fun bubbleStyle(role: String): ChatBubbleStyle {
  return when (role) {
    "user" ->
      ChatBubbleStyle(
        alignEnd = true,
        containerColor = mobileAccentSoft,
        borderColor = mobileAccent.copy(alpha = 0.32f),
        roleColor = mobileAccent,
        roleTextAlign = TextAlign.End,
      )

    "system" ->
      ChatBubbleStyle(
        alignEnd = false,
        containerColor = mobileWarningSoft,
        borderColor = mobileWarning.copy(alpha = 0.45f),
        roleColor = mobileWarning,
        roleTextAlign = TextAlign.Start,
      )

    else ->
      ChatBubbleStyle(
        alignEnd = false,
        containerColor = mobileSurface,
        borderColor = mobileBorderStrong,
        roleColor = mobileTextSecondary,
        roleTextAlign = TextAlign.Start,
      )
  }
}

private fun roleLabel(role: String, assistantLabel: String): String {
  return when (role) {
    "user" -> "user"
    "system" -> "system"
    else -> formatAgentContactTitle(displayName = assistantLabel, emoji = null)
  }
}

@Composable
internal fun ChatBase64Image(base64: String, mimeType: String?, onClick: (() -> Unit)? = null) {
  val decodeRequest = rememberViewportImageDecodeRequest(preferLowMemory = true)
  val imageState = rememberBase64ImageState(base64, decodeRequest = decodeRequest)
  val image = imageState.image

  if (image != null) {
    Surface(
      shape = RoundedCornerShape(5.dp),
      border = BorderStroke(1.dp, mobileBorder),
      color = mobileSurface,
      modifier = if (onClick != null) Modifier.fillMaxWidth().clickable(onClick = onClick) else Modifier.fillMaxWidth(),
    ) {
      Image(
        bitmap = image,
        contentDescription = mimeType ?: "attachment",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  } else if (imageState.failed) {
    Text("Unsupported attachment", style = mobileCaption1, color = mobileTextSecondary)
  }
}

@Composable
private fun DotPulse(color: Color) {
  Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
    PulseDot(alpha = 0.38f, color = color)
    PulseDot(alpha = 0.62f, color = color)
    PulseDot(alpha = 0.90f, color = color)
  }
}

@Composable
private fun PulseDot(alpha: Float, color: Color) {
  Surface(
    modifier = Modifier.size(6.dp).alpha(alpha),
    shape = CircleShape,
    color = color,
  ) {}
}

@Composable
fun ChatCodeBlock(code: String, language: String?) {
  Surface(
    shape = RoundedCornerShape(5.dp),
    color = mobileCodeBg,
    border = BorderStroke(1.dp, mobileBorderStrong.copy(alpha = 0.4f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      if (!language.isNullOrBlank()) {
        Text(
          text = language.uppercase(Locale.US),
          style = mobileCaption2.copy(letterSpacing = 0.4.sp),
          color = mobileTextSecondary,
        )
      }
      Text(
        text = code.trimEnd(),
        fontFamily = FontFamily.Monospace,
        style = mobileCallout,
        color = mobileCodeText,
      )
    }
  }
}

private fun formatBubbleTimestamp(timestampMs: Long?): String? {
  val value = timestampMs ?: return null
  return runCatching {
    DateTimeFormatter.ofPattern("HH:mm")
      .withZone(ZoneId.systemDefault())
      .format(Instant.ofEpochMilli(value))
  }.getOrNull()
}
