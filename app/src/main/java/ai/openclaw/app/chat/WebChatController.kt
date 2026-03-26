package ai.openclaw.app.chat

import android.os.Build
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ai.openclaw.app.SecurePrefs

private const val WEBCHAT_PORT = 3770
private const val WEBCHAT_PREFIX = "openclaw-webchat:"
private const val WEBCHAT_POLL_INTERVAL_MS = 10_000L
private const val WEBCHAT_HISTORY_PREFETCH_LIMIT = 8
private val WEBCHAT_JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

class WebChatController(
  private val scope: CoroutineScope,
  private val prefs: SecurePrefs,
  private val json: Json,
  private val historyCacheStore: WebChatHistoryCacheStore,
  initialSessionKey: String = "",
) {
  private val client = OkHttpClient.Builder().build()

  private val _sessionKey = MutableStateFlow(initialSessionKey.trim())
  val sessionKey: StateFlow<String> = _sessionKey.asStateFlow()

  private val _sessionId = MutableStateFlow<String?>(null)
  val sessionId: StateFlow<String?> = _sessionId.asStateFlow()

  private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
  val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

  private val _errorText = MutableStateFlow<String?>(null)
  val errorText: StateFlow<String?> = _errorText.asStateFlow()

  private val _healthOk = MutableStateFlow(false)
  val healthOk: StateFlow<Boolean> = _healthOk.asStateFlow()

  private val _thinkingLevel = MutableStateFlow("off")
  val thinkingLevel: StateFlow<String> = _thinkingLevel.asStateFlow()

  private val _pendingRunCount = MutableStateFlow(0)
  val pendingRunCount: StateFlow<Int> = _pendingRunCount.asStateFlow()

  private val _streamingAssistantText = MutableStateFlow<String?>(null)
  val streamingAssistantText: StateFlow<String?> = _streamingAssistantText.asStateFlow()

  private val _pendingToolCalls = MutableStateFlow<List<ChatPendingToolCall>>(emptyList())
  val pendingToolCalls: StateFlow<List<ChatPendingToolCall>> = _pendingToolCalls.asStateFlow()

  private val _sessions = MutableStateFlow<List<ChatSessionEntry>>(emptyList())
  val sessions: StateFlow<List<ChatSessionEntry>> = _sessions.asStateFlow()

  private val _agentContacts = MutableStateFlow<List<AgentContactEntry>>(emptyList())
  val agentContacts: StateFlow<List<AgentContactEntry>> = _agentContacts.asStateFlow()

  private val _userDisplayName = MutableStateFlow("我")
  val userDisplayName: StateFlow<String> = _userDisplayName.asStateFlow()

  private val _agentContactsRefreshing = MutableStateFlow(false)
  val agentContactsRefreshing: StateFlow<Boolean> = _agentContactsRefreshing.asStateFlow()

  private val _agentContactsError = MutableStateFlow<String?>(null)
  val agentContactsError: StateFlow<String?> = _agentContactsError.asStateFlow()

  private val _voiceSupported = MutableStateFlow(false)
  val voiceSupported: StateFlow<Boolean> = _voiceSupported.asStateFlow()

  private val _abortSupported = MutableStateFlow(false)
  val abortSupported: StateFlow<Boolean> = _abortSupported.asStateFlow()

  private val _thinkingSupported = MutableStateFlow(false)
  val thinkingSupported: StateFlow<Boolean> = _thinkingSupported.asStateFlow()

  private var activeAgentId: String? = parseAgentIdFromSessionKey(initialSessionKey)
  private var preferredBaseUrl: String? = null
  private var startupContactsLoaded = false
  private var startupContactsRefreshInFlight = false
  private val navigationVersion = AtomicLong(0L)
  private var navigationTargetSessionKey: String? = null
  private var loadedSessionKey: String? = null
  private val sessionHistoryCache = historyCacheStore.load().toMutableMap()
  private val prefetchingSessionKeys = mutableSetOf<String>()

  init {
    scope.launch {
      while (isActive) {
        delay(WEBCHAT_POLL_INTERVAL_MS)
        val pendingBefore = _pendingRunCount.value > 0
        runCatching {
          if (activeAgentId != null && (!pendingBefore || _pendingRunCount.value == 0)) {
            refreshActiveConversationInternal(silent = true)
          }
        }
      }
    }
  }

  fun onDisconnected(message: String) {
    if (!_healthOk.value && _errorText.value.isNullOrBlank()) {
      _errorText.value = message
    }
  }

  @Suppress("UNUSED_PARAMETER")
  fun applyMainSessionKey(mainSessionKey: String) {
    // WebChat runs on its own session namespace and does not follow gateway mainSessionKey.
  }

  fun load(sessionKey: String) {
    val trimmed = sessionKey.trim()
    if (trimmed.isEmpty()) {
      val navVersion = navigationVersion.incrementAndGet()
      navigationTargetSessionKey = null
      scope.launch {
        switchSessionInternal(sessionKey = "", navVersion = navVersion)
      }
      return
    }
    if (trimmed == loadedSessionKey || trimmed == navigationTargetSessionKey) return
    val optimisticAgentId = resolveAgentIdForSessionKey(trimmed)
    val navVersion = beginNavigation(targetSessionKey = trimmed, optimisticAgentId = optimisticAgentId)
    scope.launch {
      switchSessionInternal(sessionKey = trimmed, navVersion = navVersion)
    }
  }

  fun refresh() {
    scope.launch {
      if (activeAgentId != null) {
        refreshActiveConversationInternal(silent = false)
      }
    }
  }

  @Suppress("UNUSED_PARAMETER")
  fun refreshSessions(limit: Int? = null) {
    // Contacts are refreshed only at startup and by explicit pull-to-refresh in the contacts tab.
  }

  @Suppress("UNUSED_PARAMETER")
  suspend fun refreshSessionsSnapshot(limit: Int? = null): List<ChatSessionEntry> {
    return _sessions.value
  }

  fun refreshAgentContacts() {
    scope.launch {
      refreshAgentsInternal(silent = false)
    }
  }

  fun ensureInitialAgentContactsLoaded() {
    if (startupContactsLoaded || startupContactsRefreshInFlight) return
    startupContactsRefreshInFlight = true
    scope.launch {
      try {
        refreshAgentsInternal(silent = true)
      } finally {
        startupContactsRefreshInFlight = false
      }
    }
  }

  fun setThinkingLevel(thinkingLevel: String) {
    _thinkingLevel.value = normalizeThinkingLevel(thinkingLevel)
  }

  fun openAgentChat(agentId: String) {
    val normalized = agentId.trim()
    if (normalized.isEmpty()) return
    val sessionKey = resolveSessionKeyForAgentId(normalized)
    if (sessionKey == loadedSessionKey && activeAgentId == normalized) return
    if (sessionKey == navigationTargetSessionKey) return
    val navVersion = beginNavigation(targetSessionKey = sessionKey, optimisticAgentId = normalized)
    scope.launch {
      openAgentInternal(agentId = normalized, reportErrors = true, navVersion = navVersion)
    }
  }

  fun switchSession(sessionKey: String) {
    val trimmed = sessionKey.trim()
    if (trimmed.isEmpty()) {
      val navVersion = navigationVersion.incrementAndGet()
      navigationTargetSessionKey = null
      scope.launch {
        switchSessionInternal(sessionKey = "", navVersion = navVersion)
      }
      return
    }
    if (trimmed == loadedSessionKey || trimmed == navigationTargetSessionKey) return
    val optimisticAgentId = resolveAgentIdForSessionKey(trimmed)
    val navVersion = beginNavigation(targetSessionKey = trimmed, optimisticAgentId = optimisticAgentId)
    scope.launch {
      switchSessionInternal(sessionKey = trimmed, navVersion = navVersion)
    }
  }

  fun abort() {
    // openclaw-webchat does not currently expose an abort API.
  }

  fun sendMessage(message: String, thinkingLevel: String, attachments: List<OutgoingAttachment>) {
    val text = message.trim()
    if (text.isEmpty() && attachments.isEmpty()) return

    scope.launch {
      val agentId = activeAgentId
      val currentSessionKey = _sessionKey.value.trim()
      if (agentId.isNullOrEmpty() || currentSessionKey.isEmpty()) {
        _errorText.value = "No active WebChat conversation"
        return@launch
      }

      _thinkingLevel.value = normalizeThinkingLevel(thinkingLevel)
      _errorText.value = null
      _streamingAssistantText.value = null
      _pendingToolCalls.value = emptyList()
      _pendingRunCount.value = 1

      if (text.startsWith("/") && attachments.isEmpty()) {
        handleSlashCommand(currentSessionKey = currentSessionKey, command = text)
        return@launch
      }

      val optimisticMessage =
        ChatMessage(
          id = UUID.randomUUID().toString(),
          role = "user",
          content =
            buildList {
              if (text.isNotEmpty()) {
                add(ChatMessageContent(type = "text", text = text))
              }
              attachments.forEach { attachment ->
                add(
                  ChatMessageContent(
                    type = attachment.type,
                    mimeType = attachment.mimeType,
                    fileName = attachment.fileName,
                    base64 = attachment.base64,
                  ),
                )
              }
            },
          timestampMs = System.currentTimeMillis(),
        )
      _messages.value = _messages.value + optimisticMessage
      updateSessionCache(sessionKey = currentSessionKey, agentId = agentId, messages = _messages.value)

      try {
        val uploadedBlocks = attachments.map { uploadAttachment(it) }
        val response =
          apiPost(
            path = "/api/openclaw-webchat/sessions/${encodePath(currentSessionKey)}/send",
            body =
              buildJsonObject {
                put("text", JsonPrimitive(text))
                if (uploadedBlocks.isNotEmpty()) {
                  put("blocks", JsonArray(uploadedBlocks))
                }
              },
        )
        val assistantMessage = parsePresentedHistoryEntry(response["message"]) ?: return@launch
        _messages.value = _messages.value + assistantMessage
        updateSessionCache(sessionKey = currentSessionKey, agentId = agentId, messages = _messages.value)
      } catch (err: Throwable) {
        _errorText.value = err.message ?: "Send failed"
      } finally {
        _pendingRunCount.value = 0
      }
    }
  }

  @Suppress("UNUSED_PARAMETER")
  fun handleGatewayEvent(event: String, payloadJson: String?) {
    // WebChat state is polled over HTTP and does not consume gateway event streams.
  }

  private suspend fun switchSessionInternal(sessionKey: String, navVersion: Long) {
    val trimmed = sessionKey.trim()
    if (trimmed.isEmpty()) {
      val contacts = refreshAgentsInternal(silent = true)
      val fallback = activeAgentId ?: contacts.firstOrNull()?.agentId
      if (!fallback.isNullOrBlank()) {
        val targetSessionKey = buildFallbackSessionKey(fallback)
        if (navigationTargetSessionKey != targetSessionKey) {
          beginNavigation(targetSessionKey = targetSessionKey, optimisticAgentId = fallback)
        }
        openAgentInternal(fallback, reportErrors = false, navVersion = navVersion)
      }
      return
    }

    val contactAgentId = resolveAgentIdForSessionKey(trimmed)

    if (!contactAgentId.isNullOrBlank()) {
      openAgentInternal(contactAgentId, reportErrors = false, navVersion = navVersion)
      return
    }

    val contacts = refreshAgentsInternal(silent = true)
    val resolvedAgentId =
      contacts.firstOrNull { it.directSessionKey == trimmed }?.agentId
        ?: parseAgentIdFromSessionKey(trimmed)
        ?: contacts.firstOrNull()?.agentId

    if (!resolvedAgentId.isNullOrBlank()) {
      openAgentInternal(resolvedAgentId, reportErrors = false, navVersion = navVersion)
    }
  }

  private suspend fun handleSlashCommand(currentSessionKey: String, command: String) {
    try {
      val response =
        apiPost(
          path = "/api/openclaw-webchat/sessions/${encodePath(currentSessionKey)}/command",
          body =
            buildJsonObject {
              put("command", JsonPrimitive(command))
            },
        )
      parsePresentedHistoryEntry(response["message"])?.let { message ->
        _messages.value = _messages.value + message
        updateSessionCache(sessionKey = currentSessionKey, agentId = activeAgentId, messages = _messages.value)
      }
    } catch (err: Throwable) {
      _errorText.value = err.message ?: "Command failed"
    } finally {
      _pendingRunCount.value = 0
    }
  }

  private suspend fun refreshActiveConversationInternal(silent: Boolean) {
    val agentId = activeAgentId ?: return
    val sessionKey = _sessionKey.value.trim()
    try {
      val historyPayload =
        apiGet(
          path = "/api/openclaw-webchat/agents/${encodePath(agentId)}/history?limit=200",
        )
      if (agentId != activeAgentId) return
      val parsedMessages = parsePresentedHistory(historyPayload["messages"])
      _messages.value = parsedMessages
      if (sessionKey.isNotEmpty()) {
        updateSessionCache(sessionKey = sessionKey, agentId = agentId, messages = parsedMessages)
      }
      _errorText.value = null
      _healthOk.value = true
    } catch (err: Throwable) {
      if (agentId != activeAgentId) return
      if (!silent) {
        _errorText.value = err.message ?: "Failed to refresh chat"
      }
      _healthOk.value = false
    }
  }

  private suspend fun openAgentInternal(agentId: String, reportErrors: Boolean, navVersion: Long) {
    if (agentId.isBlank()) return
    try {
      val response =
        apiPost(
          path = "/api/openclaw-webchat/agents/${encodePath(agentId)}/open",
          body = buildJsonObject {},
        )
      if (!isCurrentNavigation(navVersion)) return
      val resolvedSessionKey = response["sessionKey"].stringOrNull()?.trim().orEmpty().ifBlank { buildFallbackSessionKey(agentId) }
      activeAgentId = agentId
      _sessionKey.value = resolvedSessionKey
      _sessionId.value = null
      val parsedMessages = parsePresentedHistory(response["history"].asObjectOrNull()?.get("messages"))
      _messages.value = parsedMessages
      _errorText.value = null
      _healthOk.value = true
      loadedSessionKey = resolvedSessionKey
      navigationTargetSessionKey = null
      updateSessionCache(sessionKey = resolvedSessionKey, agentId = agentId, messages = parsedMessages)
      refreshActiveConversationInternal(silent = true)
      _pendingRunCount.value = 0
    } catch (err: Throwable) {
      if (!isCurrentNavigation(navVersion)) return
      navigationTargetSessionKey = null
      if (reportErrors) {
        _errorText.value = err.message ?: "Failed to open conversation"
      }
      _healthOk.value = false
    }
  }

  private suspend fun refreshAgentsInternal(silent: Boolean): List<AgentContactEntry> {
    if (!silent) {
      _agentContactsRefreshing.value = true
    }
    return try {
      val payload = apiGet("/api/openclaw-webchat/agents")
      val agents = parseAgents(payload["agents"])
      runCatching { apiGet("/api/openclaw-webchat/settings") }
        .getOrNull()
        ?.get("userProfile")
        .asObjectOrNull()
        ?.get("displayName")
        .stringOrNull()
        ?.trim()
        .takeUnless { it.isNullOrEmpty() }
        ?.let { _userDisplayName.value = it }
      val sessions =
        agents.map { agent ->
          ChatSessionEntry(
            key = agent.sessionKey ?: buildFallbackSessionKey(agent.agentId),
            updatedAtMs = agent.lastMessageAtMs,
            displayName = agent.name,
          )
        }
      _agentContacts.value = agents.map { it.toContactEntry() }
      _sessions.value = sessions
      startupContactsLoaded = true
      _agentContactsError.value = null
      _errorText.value = _errorText.value?.takeIf { it.startsWith("Send failed") || it.startsWith("Command failed") }
      _healthOk.value = true
      publishPendingStateFromContacts()
      scheduleHistoryPrefetch(_agentContacts.value)
      _agentContacts.value
    } catch (err: Throwable) {
      _agentContactsError.value = err.message ?: "Failed to refresh contacts"
      _healthOk.value = false
      if (!silent) {
        _errorText.value = _agentContactsError.value
      }
      _agentContacts.value
    } finally {
      if (!silent) {
        _agentContactsRefreshing.value = false
      }
    }
  }

  private fun publishPendingStateFromContacts() {
    val currentAgentId = activeAgentId
    val activeContact = _agentContacts.value.firstOrNull { it.agentId == currentAgentId }
    _pendingRunCount.value = if (activeContact?.presence == "running") 1 else 0
  }

  private fun scheduleHistoryPrefetch(contacts: List<AgentContactEntry>) {
    contacts
      .asSequence()
      .filter { it.hasDirectSession }
      .filter { it.directSessionKey.isNotBlank() }
      .filterNot { sessionHistoryCache.containsKey(it.directSessionKey) }
      .take(WEBCHAT_HISTORY_PREFETCH_LIMIT)
      .forEach { contact ->
        val sessionKey = contact.directSessionKey.trim()
        synchronized(prefetchingSessionKeys) {
          if (!prefetchingSessionKeys.add(sessionKey)) {
            return@forEach
          }
        }
        scope.launch {
          try {
            prefetchAgentHistory(contact.agentId, sessionKey)
          } finally {
            synchronized(prefetchingSessionKeys) {
              prefetchingSessionKeys.remove(sessionKey)
            }
          }
        }
      }
  }

  private suspend fun prefetchAgentHistory(agentId: String, sessionKey: String) {
    runCatching {
      val historyPayload =
        apiGet(
          path = "/api/openclaw-webchat/agents/${encodePath(agentId)}/history?limit=80",
        )
      val parsedMessages = parsePresentedHistory(historyPayload["messages"])
      if (parsedMessages.isNotEmpty()) {
        updateSessionCache(sessionKey = sessionKey, agentId = agentId, messages = parsedMessages)
        if (_sessionKey.value.trim() == sessionKey && _messages.value.isEmpty()) {
          _messages.value = parsedMessages
        }
      }
    }
  }

  private suspend fun uploadAttachment(attachment: OutgoingAttachment): JsonObject {
    val kind = attachment.type.trim().lowercase(Locale.US)
    val response =
      apiPost(
        path = "/api/openclaw-webchat/uploads",
        body =
          buildJsonObject {
            put("kind", JsonPrimitive(kind))
            put("filename", JsonPrimitive(attachment.fileName))
            put("mimeType", JsonPrimitive(attachment.mimeType))
            put("contentBase64", JsonPrimitive(attachment.base64))
          },
      )
    val upload = response["upload"].asObjectOrNull() ?: error("Upload response missing payload")
    return buildJsonObject {
      put("type", JsonPrimitive(kind))
      put("source", JsonPrimitive(upload["source"].stringOrNull() ?: error("Upload source missing")))
      upload["name"].stringOrNull()?.let { put("name", JsonPrimitive(it)) }
      upload["mimeType"].stringOrNull()?.let { put("mimeType", JsonPrimitive(it)) }
      upload["transcriptStatus"].stringOrNull()?.let { put("transcriptStatus", JsonPrimitive(it)) }
      upload["transcriptText"].stringOrNull()?.let { put("transcriptText", JsonPrimitive(it)) }
      upload["transcriptError"].stringOrNull()?.let { put("transcriptError", JsonPrimitive(it)) }
      upload["size"].longOrNull()?.let { put("sizeBytes", JsonPrimitive(it)) }
    }
  }

  private suspend fun apiGet(path: String): JsonObject {
    return requestJson(method = "GET", path = path, body = null)
  }

  private suspend fun apiPost(path: String, body: JsonObject): JsonObject {
    return requestJson(method = "POST", path = path, body = body)
  }

  private suspend fun requestJson(method: String, path: String, body: JsonObject?): JsonObject =
    withContext(Dispatchers.IO) {
      val candidateUrls = candidateBaseUrls()
      var lastError: Throwable? = null
      for (baseUrl in candidateUrls) {
        try {
          val requestBuilder =
            Request.Builder()
              .url(baseUrl.trimEnd('/') + path)
              .header("accept", "application/json")
          if (method == "POST") {
            requestBuilder.post((body?.toString() ?: "{}").toRequestBody(WEBCHAT_JSON_MEDIA_TYPE))
            requestBuilder.header("content-type", "application/json")
          }
          client.newCall(requestBuilder.build()).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
              throw IllegalStateException(parseErrorMessage(responseText).ifBlank { "HTTP ${response.code}" })
            }
            val root = json.parseToJsonElement(responseText).asObjectOrNull()
              ?: error("Expected JSON object response")
            preferredBaseUrl = baseUrl
            return@withContext root
          }
        } catch (err: Throwable) {
          lastError = err
        }
      }
      throw lastError ?: IllegalStateException("openclaw-webchat is unavailable")
    }

  private fun parseAgents(element: JsonElement?): List<WebChatAgentEntry> {
    val array = element as? JsonArray ?: return emptyList()
    return array.mapNotNull { item ->
      val obj = item.asObjectOrNull() ?: return@mapNotNull null
      val agentId = obj["agentId"].stringOrNull()?.trim().orEmpty()
      if (agentId.isEmpty()) return@mapNotNull null
      WebChatAgentEntry(
        agentId = agentId,
        name = obj["name"].stringOrNull()?.trim().takeUnless { it.isNullOrEmpty() } ?: agentId,
        avatarUrl = resolveAbsoluteMediaUrl(obj["avatarUrl"].stringOrNull()),
        sessionKey = obj["sessionKey"].stringOrNull()?.trim(),
        hasSession = obj["hasSession"].booleanOrFalse(),
        summary = obj["summary"].stringOrNull(),
        lastMessageAtMs = obj["lastMessageAt"].isoMillisOrNull(),
        presence = obj["presence"].stringOrNull()?.trim()?.lowercase(Locale.US) ?: "idle",
      )
    }
  }

  private fun parsePresentedHistory(element: JsonElement?): List<ChatMessage> {
    val array = element as? JsonArray ?: return emptyList()
    return array.mapNotNull(::parsePresentedHistoryEntry)
  }

  private fun parsePresentedHistoryEntry(element: JsonElement?): ChatMessage? {
    val obj = element.asObjectOrNull() ?: return null
    val role = obj["role"].stringOrNull()?.trim()?.lowercase(Locale.US).orEmpty()
    val timestampMs = obj["createdAt"].isoMillisOrNull()
    val id = obj["id"].stringOrNull() ?: UUID.randomUUID().toString()

    if (role == "marker") {
      val label = obj["label"].stringOrNull()?.trim().orEmpty()
      return ChatMessage(
        id = id,
        role = "system",
        content = listOf(ChatMessageContent(type = "text", text = label.ifEmpty { "已重置上下文" })),
        timestampMs = timestampMs,
      )
    }

    val blocks = obj["blocks"] as? JsonArray ?: JsonArray(emptyList())
    val content =
      blocks.mapNotNull { block ->
        val blockObj = block.asObjectOrNull() ?: return@mapNotNull null
        when (blockObj["type"].stringOrNull()?.trim()?.lowercase(Locale.US).orEmpty()) {
          "text" -> {
            val text = blockObj["text"].stringOrNull()?.trim().orEmpty()
            if (text.isEmpty()) null else ChatMessageContent(type = "text", text = text)
          }
          else ->
            ChatMessageContent(
              type = blockObj["type"].stringOrNull() ?: "file",
              mimeType = blockObj["mimeType"].stringOrNull(),
              fileName = blockObj["name"].stringOrNull(),
              mediaUrl = resolveAbsoluteMediaUrl(blockObj["url"].stringOrNull()),
              sizeBytes = blockObj["sizeBytes"].longOrNull(),
            )
        }
      }
    return ChatMessage(
      id = id,
      role = if (role == "assistant") "assistant" else "user",
      content = content,
      timestampMs = timestampMs,
    )
  }

  private fun candidateBaseUrls(): List<String> {
    val candidates = linkedSetOf<String>()
    preferredBaseUrl?.trim()?.takeIf { it.isNotEmpty() }?.let(candidates::add)
    if (isAndroidEmulator()) {
      candidates += "http://10.0.2.2:$WEBCHAT_PORT"
    }
    buildBaseUrl(host = prefs.tailscaleHost.value, https = false)?.let(candidates::add)
    buildBaseUrl(host = prefs.manualHost.value, https = prefs.manualTls.value)?.let(candidates::add)
    extractHostCandidate(prefs.lastGatewayRemoteAddress.value)?.let { host ->
      buildBaseUrl(host = host, https = false)?.let(candidates::add)
    }
    return candidates.toList()
  }

  private fun resolveAbsoluteMediaUrl(rawUrl: String?): String? {
    val candidate = rawUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
      return candidate
    }
    val baseUrl = preferredBaseUrl ?: candidateBaseUrls().firstOrNull() ?: return null
    return baseUrl.trimEnd('/') + if (candidate.startsWith('/')) candidate else "/$candidate"
  }

  private fun parseErrorMessage(responseText: String): String {
    if (responseText.isBlank()) return ""
    return try {
      val root = json.parseToJsonElement(responseText).asObjectOrNull() ?: return responseText
      root["error"].stringOrNull()?.ifBlank { responseText } ?: responseText
    } catch (_: Throwable) {
      responseText
    }
  }

  private fun buildBaseUrl(host: String, https: Boolean): String? {
    val normalizedHost = extractHostCandidate(host) ?: return null
    val displayHost =
      if (normalizedHost.contains(':') && !normalizedHost.startsWith("[")) {
        "[$normalizedHost]"
      } else {
        normalizedHost
      }
    val scheme = if (https) "https" else "http"
    return "$scheme://$displayHost:$WEBCHAT_PORT"
  }

  private fun extractHostCandidate(rawValue: String?): String? {
    val trimmed = rawValue?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val parsed =
      runCatching {
        if (trimmed.contains("://")) {
          URI(trimmed)
        } else {
          URI("ws://$trimmed")
        }
      }.getOrNull()
    return parsed?.host?.trim()?.takeIf { it.isNotEmpty() }?.lowercase(Locale.US)
  }

  private fun isAndroidEmulator(): Boolean {
    val fingerprint = Build.FINGERPRINT.lowercase(Locale.US)
    val model = Build.MODEL.lowercase(Locale.US)
    val product = Build.PRODUCT.lowercase(Locale.US)
    return (
      fingerprint.contains("generic") ||
        fingerprint.contains("emulator") ||
        model.contains("emulator") ||
        model.contains("android sdk") ||
        product.contains("sdk")
      )
  }

  private fun parseAgentIdFromSessionKey(sessionKey: String): String? {
    val trimmed = sessionKey.trim()
    if (!trimmed.startsWith(WEBCHAT_PREFIX)) return null
    return trimmed.removePrefix(WEBCHAT_PREFIX).trim().takeIf { it.isNotEmpty() }
  }

  private fun buildFallbackSessionKey(agentId: String): String = WEBCHAT_PREFIX + agentId.trim()

  private fun encodePath(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

  private fun normalizeThinkingLevel(level: String): String {
    return when (level.trim().lowercase(Locale.US)) {
      "low" -> "low"
      "medium" -> "medium"
      "high" -> "high"
      else -> "off"
    }
  }

  private fun resolveAgentIdForSessionKey(sessionKey: String): String? {
    val trimmed = sessionKey.trim()
    if (trimmed.isEmpty()) return null
    return _agentContacts.value.firstOrNull { it.directSessionKey == trimmed }?.agentId
      ?: parseAgentIdFromSessionKey(trimmed)
  }

  private fun resolveSessionKeyForAgentId(agentId: String): String {
    val normalized = agentId.trim()
    if (normalized.isEmpty()) return ""
    return _agentContacts.value.firstOrNull { it.agentId == normalized }?.directSessionKey?.trim().orEmpty()
      .ifBlank { buildFallbackSessionKey(normalized) }
  }

  private fun beginNavigation(targetSessionKey: String, optimisticAgentId: String?): Long {
    val normalized = targetSessionKey.trim()
    navigationTargetSessionKey = normalized
    if (_sessionKey.value != normalized) {
      _sessionKey.value = normalized
    }
    if (!optimisticAgentId.isNullOrBlank()) {
      activeAgentId = optimisticAgentId
    }
    if (loadedSessionKey != normalized) {
      _sessionId.value = null
      _messages.value = sessionHistoryCache[normalized].orEmpty()
      _errorText.value = null
      _streamingAssistantText.value = null
      _pendingToolCalls.value = emptyList()
      _pendingRunCount.value = 0
    }
    return navigationVersion.incrementAndGet()
  }

  private fun isCurrentNavigation(navVersion: Long): Boolean = navigationVersion.get() == navVersion

  private fun updateSessionCache(sessionKey: String, agentId: String?, messages: List<ChatMessage>) {
    val normalized = sessionKey.trim()
    if (normalized.isEmpty()) return
    val cachedMessages = messages.takeLast(WEBCHAT_HISTORY_CACHE_MAX_MESSAGES).map { it.sanitizedForCache() }
    sessionHistoryCache[normalized] = cachedMessages
    agentId?.trim()?.takeIf { it.isNotEmpty() }?.let { normalizedAgentId ->
      sessionHistoryCache[buildFallbackSessionKey(normalizedAgentId)] = cachedMessages
    }
    historyCacheStore.save(sessionHistoryCache)
  }
}

private data class WebChatAgentEntry(
  val agentId: String,
  val name: String,
  val avatarUrl: String?,
  val sessionKey: String?,
  val hasSession: Boolean,
  val summary: String?,
  val lastMessageAtMs: Long?,
  val presence: String,
) {
  fun toContactEntry(): AgentContactEntry {
    val resolvedSessionKey = sessionKey?.trim().takeUnless { it.isNullOrEmpty() } ?: "$WEBCHAT_PREFIX$agentId"
    return AgentContactEntry(
      agentId = agentId,
      displayName = name,
      emoji = null,
      directSessionKey = resolvedSessionKey,
      directSessionUpdatedAtMs = lastMessageAtMs,
      hasDirectSession = hasSession || sessionKey != null,
      previewText = summary,
      avatarUrl = avatarUrl,
      presence = presence,
      lastMessageAtMs = lastMessageAtMs,
    )
  }
}

private fun JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject

private fun JsonElement?.stringOrNull(): String? =
  when (this) {
    is JsonNull -> null
    is JsonPrimitive -> content
    else -> null
  }

private fun JsonElement?.longOrNull(): Long? =
  when (this) {
    is JsonPrimitive -> content.toLongOrNull()
    else -> null
  }

private fun JsonElement?.booleanOrFalse(): Boolean =
  when (this) {
    is JsonPrimitive -> content.toBooleanStrictOrNull() ?: false
    else -> false
  }

private fun JsonElement?.isoMillisOrNull(): Long? {
  val text = stringOrNull()?.trim()?.takeIf { it.isNotEmpty() } ?: return null
  return runCatching { Instant.parse(text).toEpochMilli() }.getOrNull()
}
