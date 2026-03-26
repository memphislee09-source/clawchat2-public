package ai.openclaw.app.chat

import android.content.Context
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val WEBCHAT_HISTORY_CACHE_FILE = "webchat-history-cache.json"
private const val WEBCHAT_HISTORY_CACHE_MAX_SESSIONS = 24
internal const val WEBCHAT_HISTORY_CACHE_MAX_MESSAGES = 80

@Serializable
private data class CachedSessionHistory(
  val sessionKey: String,
  val messages: List<ChatMessage>,
  val cachedAtMs: Long,
)

@Serializable
private data class WebChatHistoryCachePayload(
  val entries: List<CachedSessionHistory> = emptyList(),
)

class WebChatHistoryCacheStore(
  context: Context,
  private val json: Json,
) {
  private val cacheFile = File(context.filesDir, WEBCHAT_HISTORY_CACHE_FILE)

  fun load(): Map<String, List<ChatMessage>> {
    val payload =
      runCatching {
        if (!cacheFile.exists()) return emptyMap()
        json.decodeFromString<WebChatHistoryCachePayload>(cacheFile.readText())
      }.getOrNull() ?: return emptyMap()
    return payload.entries.associate { entry ->
      entry.sessionKey to entry.messages
    }
  }

  fun save(cache: Map<String, List<ChatMessage>>) {
    val payload =
      WebChatHistoryCachePayload(
        entries =
          cache.entries
            .asSequence()
            .mapNotNull { (sessionKey, messages) ->
              val normalized = sessionKey.trim()
              if (normalized.isEmpty()) {
                null
              } else {
                CachedSessionHistory(
                  sessionKey = normalized,
                  messages =
                    messages
                      .takeLast(WEBCHAT_HISTORY_CACHE_MAX_MESSAGES)
                      .map { it.sanitizedForCache() },
                  cachedAtMs = System.currentTimeMillis(),
                )
              }
            }
            .sortedByDescending { it.cachedAtMs }
            .take(WEBCHAT_HISTORY_CACHE_MAX_SESSIONS)
            .toList(),
      )

    runCatching {
      val tempFile = File(cacheFile.parentFile, "$WEBCHAT_HISTORY_CACHE_FILE.tmp")
      tempFile.writeText(json.encodeToString(payload))
      if (!tempFile.renameTo(cacheFile)) {
        cacheFile.writeText(tempFile.readText())
        tempFile.delete()
      }
    }
  }
}
