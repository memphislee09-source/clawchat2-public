package ai.openclaw.app.ui.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import ai.openclaw.app.ui.mobileBorder
import ai.openclaw.app.ui.mobileBorderStrong
import ai.openclaw.app.ui.mobileCallout
import ai.openclaw.app.ui.mobileCaption1
import ai.openclaw.app.ui.mobileCaption2
import ai.openclaw.app.ui.mobileCodeBg
import ai.openclaw.app.ui.mobileCodeText
import ai.openclaw.app.ui.mobileHeadline
import ai.openclaw.app.ui.mobileSurfaceStrong
import ai.openclaw.app.ui.mobileText
import ai.openclaw.app.ui.mobileTextSecondary
import ai.openclaw.app.ui.mobileWarning
import ai.openclaw.app.ui.mobileWarningSoft
import ai.openclaw.app.ui.tr
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.isSystemInDarkTheme

private data class ChatBubbleStyle(
  val containerColor: Color,
  val roleColor: Color,
  val borderColor: Color,
  val textColor: Color,
)

@Composable
fun ChatMessageBubble(message: ChatMessage, assistantLabel: String = "assistant", userLabel: String = "我") {
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
    roleLabel = roleLabel(role = role, assistantLabel = assistantLabel, userLabel = userLabel),
    timestampLabel = formatBubbleTimestamp(message.timestampMs),
  ) {
    ChatMessageBody(content = displayableContent, textColor = style.textColor)
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
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 2.dp),
      horizontalArrangement = Arrangement.Start,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = roleLabel,
        style = mobileCaption2.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp),
        color = style.roleColor,
        textAlign = TextAlign.Start,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (!timestampLabel.isNullOrBlank()) {
        Text(
          text = timestampLabel,
          modifier = Modifier.padding(start = 6.dp),
          style = mobileCaption2,
          color = style.roleColor.copy(alpha = 0.78f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    Surface(
      shape = RoundedCornerShape(14.dp),
      color = style.containerColor,
      tonalElevation = 0.dp,
      shadowElevation = 0.dp,
      border = BorderStroke(1.dp, style.borderColor),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 11.dp),
        contentAlignment = Alignment.CenterStart,
      ) {
        content()
      }
    }
  }
}

@Composable
private fun ChatMessageBody(content: List<ChatMessageContent>, textColor: Color) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.Start,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    content.forEachIndexed { index, part ->
      when (part.type) {
        "text" -> {
          val text = part.text ?: return@forEachIndexed
          Box(
            modifier =
              Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart,
          ) {
            SelectionContainer {
              ChatMarkdown(text = text, textColor = textColor)
            }
          }
        }
        else -> {
          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            ChatMediaAttachment(descriptor = part.toAttachmentDescriptor())
          }
        }
      }
    }
  }
}

@Composable
fun ChatTypingIndicatorBubble(assistantLabel: String = "assistant") {
  ChatBubbleContainer(
    style = bubbleStyle("assistant"),
    roleLabel = roleLabel(role = "assistant", assistantLabel = assistantLabel, userLabel = "我"),
  ) {
    Row(
      modifier = Modifier,
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      DotPulse(color = mobileTextSecondary)
      Text(tr("Working…", "处理中…"), style = mobileCallout, color = mobileTextSecondary)
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
    Column(
      modifier = Modifier,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(tr("Running tools…", "正在调用工具…"), style = mobileCaption1.copy(fontWeight = FontWeight.SemiBold), color = mobileTextSecondary)
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
          text = tr("… +${toolCalls.size - 6} more", "… 还有 ${toolCalls.size - 6} 项"),
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
    style = bubbleStyle("assistant"),
    roleLabel = "$assistantLabel · live",
  ) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
      ChatMarkdown(text = text, textColor = mobileText)
    }
  }
}

@Composable
private fun bubbleStyle(role: String): ChatBubbleStyle {
  val darkMode = isSystemInDarkTheme()
  return when (role) {
    "user" ->
      ChatBubbleStyle(
        containerColor = mobileAccent,
        roleColor = Color(0xFF18211C).copy(alpha = 0.82f),
        borderColor = mobileAccent.copy(alpha = if (darkMode) 0.22f else 0.12f),
        textColor = Color(0xFF18211C),
      )

    "system" ->
      ChatBubbleStyle(
        containerColor = mobileWarningSoft,
        roleColor = mobileWarning,
        borderColor = mobileWarning.copy(alpha = if (darkMode) 0.16f else 0.10f),
        textColor = mobileText,
      )

    else ->
      ChatBubbleStyle(
        containerColor = mobileSurfaceStrong,
        roleColor = mobileTextSecondary,
        borderColor = mobileBorder.copy(alpha = if (darkMode) 0.22f else 0.16f),
        textColor = mobileText,
      )
  }
}

private fun roleLabel(role: String, assistantLabel: String, userLabel: String): String {
  return when (role) {
    "user" -> userLabel.trim().ifEmpty { "我" }
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
    BubbleImage(
      image = image,
      contentDescription = mimeType ?: "attachment",
      onClick = onClick,
    )
  } else if (imageState.failed) {
    Text(tr("Unsupported attachment", "暂不支持的附件"), style = mobileCaption1, color = mobileTextSecondary)
  }
}

@Composable
internal fun BubbleImage(
  image: ImageBitmap,
  contentDescription: String,
  onClick: (() -> Unit)? = null,
) {
  val density = LocalDensity.current
  val aspectRatio = remember(image) { image.width.toFloat() / image.height.coerceAtLeast(1).toFloat() }

  BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    val maxDisplayWidth = maxWidth
    val intrinsicWidth = remember(image, density) { with(density) { image.width.toDp() } }
    val displayWidth = remember(intrinsicWidth, maxDisplayWidth) { minOf(intrinsicWidth, maxDisplayWidth) }
    val imageModifier =
      Modifier
        .width(displayWidth)
        .aspectRatio(aspectRatio)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Surface(
      shape = RoundedCornerShape(5.dp),
      color = Color.Transparent,
      modifier = imageModifier,
    ) {
      Image(
        bitmap = image,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun DotPulse(color: Color) {
  Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
    PulseDot(index = 0, color = color)
    PulseDot(index = 1, color = color)
    PulseDot(index = 2, color = color)
  }
}

@Composable
private fun PulseDot(index: Int, color: Color) {
  val transition = rememberInfiniteTransition(label = "processing-dots")
  val animatedAlpha =
    transition.animateFloat(
      initialValue = 0.28f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 520, delayMillis = index * 140, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "dot-alpha-$index",
    )
  val animatedScale =
    transition.animateFloat(
      initialValue = 0.82f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 520, delayMillis = index * 140, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "dot-scale-$index",
    )
  Surface(
    modifier =
      Modifier
        .size(6.dp)
        .graphicsLayer {
          alpha = animatedAlpha.value
          scaleX = animatedScale.value
          scaleY = animatedScale.value
        },
    shape = CircleShape,
    color = color,
  ) {}
}

@Composable
fun ChatCodeBlock(code: String, language: String?) {
  Surface(
    shape = RoundedCornerShape(5.dp),
    color = mobileCodeBg,
    border = BorderStroke(1.dp, mobileBorderStrong.copy(alpha = 0.24f)),
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
    val zoneId = ZoneId.systemDefault()
    val instant = Instant.ofEpochMilli(value).atZone(zoneId)
    if (instant.toLocalDate() == LocalDate.now(zoneId)) {
      DateTimeFormatter.ofPattern("HH:mm").format(instant)
    } else {
      DateTimeFormatter.ofPattern("yyyy.M.d HH:mm").format(instant)
    }
  }.getOrNull()
}
