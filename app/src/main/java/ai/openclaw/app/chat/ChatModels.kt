package ai.openclaw.app.chat

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
  val id: String,
  val role: String,
  val content: List<ChatMessageContent>,
  val timestampMs: Long?,
)

@Serializable
data class ChatMessageContent(
  val type: String = "text",
  val text: String? = null,
  val mimeType: String? = null,
  val fileName: String? = null,
  val base64: String? = null,
  val mediaUrl: String? = null,
  val mediaPath: String? = null,
  val mediaPort: Int? = null,
  val mediaSha256: String? = null,
  val sizeBytes: Long? = null,
)

internal fun ChatMessage.sanitizedForCache(): ChatMessage {
  return copy(content = content.map { it.copy(base64 = null) })
}

data class ChatPendingToolCall(
  val toolCallId: String,
  val name: String,
  val args: kotlinx.serialization.json.JsonObject? = null,
  val startedAtMs: Long,
  val isError: Boolean? = null,
)

data class ChatThinkingOption(
  val value: String,
  val label: String,
)

data class ChatModelOption(
  val provider: String,
  val model: String,
  val label: String,
  val available: Boolean = true,
)

data class ChatSessionEntry(
  val key: String,
  val updatedAtMs: Long?,
  val displayName: String? = null,
)

data class ChatHistory(
  val sessionKey: String,
  val sessionId: String?,
  val thinkingLevel: String?,
  val messages: List<ChatMessage>,
)

data class OutgoingAttachment(
  val type: String,
  val mimeType: String,
  val fileName: String,
  val base64: String,
)
