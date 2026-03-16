package ai.openclaw.app.chat

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val CLAWCHAT2_SESSION_SUFFIX = "clawchat2"

data class ConfiguredAgentEntry(
  val agentId: String,
  val displayName: String,
  val emoji: String? = null,
)

data class AgentContactEntry(
  val agentId: String,
  val displayName: String,
  val emoji: String? = null,
  val directSessionKey: String,
  val directSessionUpdatedAtMs: Long? = null,
  val hasDirectSession: Boolean = false,
  val previewText: String? = null,
  val avatarUrl: String? = null,
  val presence: String = "idle",
  val lastMessageAtMs: Long? = null,
)

fun clawChat2SessionKey(agentId: String): String {
  return "agent:${agentId.trim()}:$CLAWCHAT2_SESSION_SUFFIX"
}

fun webChatSessionKey(agentId: String): String {
  return "openclaw-webchat:${agentId.trim()}"
}

fun formatAgentContactTitle(displayName: String, emoji: String?): String {
  val parts =
    buildList {
      emoji?.trim()?.takeIf { it.isNotEmpty() }?.let(::add)
      displayName.trim().takeIf { it.isNotEmpty() }?.let(::add)
    }
  return parts.joinToString(" ").ifBlank { displayName }
}

fun parseConfiguredAgents(configJson: String, json: Json): List<ConfiguredAgentEntry> {
  val root = runCatching { json.parseToJsonElement(configJson).asObjectOrNull() }.getOrNull() ?: return emptyList()
  val config = root["config"].asObjectOrNull() ?: return emptyList()
  val agents =
    extractAgentList(config) ?: return emptyList()

  val seen = mutableSetOf<String>()
  return agents.mapNotNull { item ->
    val obj = item.asObjectOrNull() ?: return@mapNotNull null
    val agentId =
      sequenceOf("id", "agentId", "key")
        .mapNotNull { field -> obj[field].asStringOrNull()?.trim() }
        .firstOrNull { it.isNotEmpty() }
        ?: return@mapNotNull null
    if (!seen.add(agentId)) return@mapNotNull null

    val identity = obj["identity"].asObjectOrNull()
    val displayName =
      sequenceOf(
        identity?.get("name").asStringOrNull()?.trim(),
        obj["name"].asStringOrNull()?.trim(),
        agentId,
      ).firstOrNull { !it.isNullOrEmpty() } ?: agentId
    val emoji =
      sequenceOf(
        identity?.get("emoji").asStringOrNull()?.trim(),
        obj["emoji"].asStringOrNull()?.trim(),
      ).firstOrNull { !it.isNullOrEmpty() }

    ConfiguredAgentEntry(
      agentId = agentId,
      displayName = displayName,
      emoji = emoji,
    )
  }
}

fun resolveAgentContacts(
  agents: List<ConfiguredAgentEntry>,
  sessions: List<ChatSessionEntry>,
  previewsBySessionKey: Map<String, String> = emptyMap(),
): List<AgentContactEntry> {
  val sessionsByKey =
    sessions.associateBy { entry ->
      entry.key.trim()
    }

  return agents.withIndex()
    .map { (index, agent) ->
      val directSessionKey = clawChat2SessionKey(agent.agentId)
      val directSession = sessionsByKey[directSessionKey]
      IndexedValue(
        index = index,
        value =
          AgentContactEntry(
            agentId = agent.agentId,
            displayName = agent.displayName,
            emoji = agent.emoji,
            directSessionKey = directSessionKey,
            directSessionUpdatedAtMs = directSession?.updatedAtMs,
            hasDirectSession = directSession != null,
            previewText = previewsBySessionKey[directSessionKey],
          ),
      )
    }.sortedWith(
      compareByDescending<IndexedValue<AgentContactEntry>> { it.value.hasDirectSession }
        .thenByDescending { it.value.directSessionUpdatedAtMs ?: Long.MIN_VALUE }
        .thenBy { it.index },
    ).map { it.value }
}

private fun extractAgentList(config: JsonObject): JsonArray? {
  val agentsNode = config["agents"]
  return when (agentsNode) {
    is JsonArray -> agentsNode
    is JsonObject -> agentsNode["list"].asArrayOrNull()
    else -> config["agents.list"].asArrayOrNull()
  }
}

private fun JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject

private fun JsonElement?.asArrayOrNull(): JsonArray? = this as? JsonArray

private fun JsonElement?.asStringOrNull(): String? =
  when (this) {
    is JsonNull -> null
    is JsonPrimitive -> content
    else -> null
  }

fun extractLatestPreviewText(historyJson: String, json: Json): String? {
  val root = runCatching { json.parseToJsonElement(historyJson).asObjectOrNull() }.getOrNull() ?: return null
  val messages = root["messages"].asArrayOrNull() ?: return null
  for (index in messages.size - 1 downTo 0) {
    val message = messages[index].asObjectOrNull() ?: continue
    val role = message["role"].asStringOrNull()?.trim().orEmpty()
    if (role == "system") continue
    val content = message["content"].asArrayOrNull() ?: continue
    val preview = summarizeMessageContent(content)
    if (!preview.isNullOrBlank()) return preview
  }
  return null
}

private fun summarizeMessageContent(content: JsonArray): String? {
  val pieces = mutableListOf<String>()
  for (item in content) {
    val obj = item.asObjectOrNull() ?: continue
    when (obj["type"].asStringOrNull()?.trim().orEmpty()) {
      "text" -> {
        val text = obj["text"].asStringOrNull()?.trim().orEmpty()
        if (text.isNotEmpty()) {
          pieces += text
        }
      }
      "image" -> pieces += "[Image]"
      else -> pieces += "[Attachment]"
    }
  }
  return pieces.joinToString(" ").replace(Regex("\\s+"), " ").trim().ifBlank { null }
}
