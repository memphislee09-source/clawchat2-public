package ai.openclaw.app.ui.chat

import ai.openclaw.app.chat.ChatMessageContent
import java.util.Locale

internal enum class ChatAttachmentKind {
  Image,
  Audio,
  Video,
  Unknown,
}

internal data class ChatAttachmentDescriptor(
  val kind: ChatAttachmentKind,
  val mimeType: String?,
  val fileName: String?,
  val base64: String?,
  val mediaUrl: String?,
  val mediaSha256: String?,
  val sizeBytes: Long?,
)

internal fun ChatMessageContent.toAttachmentDescriptor(): ChatAttachmentDescriptor {
  return ChatAttachmentDescriptor(
    kind = classifyChatAttachment(type = type, mimeType = mimeType),
    mimeType = mimeType?.trim()?.ifEmpty { null },
    fileName = fileName?.trim()?.ifEmpty { null },
    base64 = base64?.trim()?.ifEmpty { null },
    mediaUrl = mediaUrl?.trim()?.ifEmpty { null },
    mediaSha256 = mediaSha256?.trim()?.ifEmpty { null },
    sizeBytes = sizeBytes?.takeIf { it >= 0L },
  )
}

internal fun classifyChatAttachment(type: String?, mimeType: String?): ChatAttachmentKind {
  val normalizedType = type?.trim()?.lowercase(Locale.US).orEmpty()
  val normalizedMime = mimeType?.trim()?.lowercase(Locale.US).orEmpty()

  return when {
    normalizedType == "image" || normalizedMime.startsWith("image/") -> ChatAttachmentKind.Image
    normalizedType == "audio" || normalizedMime.startsWith("audio/") -> ChatAttachmentKind.Audio
    normalizedType == "video" || normalizedMime.startsWith("video/") -> ChatAttachmentKind.Video
    else -> ChatAttachmentKind.Unknown
  }
}

internal fun attachmentDisplayName(descriptor: ChatAttachmentDescriptor): String {
  return descriptor.fileName ?: defaultAttachmentName(descriptor.kind, descriptor.mimeType)
}

private fun defaultAttachmentName(kind: ChatAttachmentKind, mimeType: String?): String {
  return when (kind) {
    ChatAttachmentKind.Image -> "image"
    ChatAttachmentKind.Audio -> "audio"
    ChatAttachmentKind.Video -> "video"
    ChatAttachmentKind.Unknown -> mimeType ?: "attachment"
  }
}
