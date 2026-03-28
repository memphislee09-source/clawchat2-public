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
private const val WEBCHAT_COMPOSER_CONTROL_MIN_VERSION = "0.1.6"
private val WEBCHAT_JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

class WebChatController(
  private val scope: CoroutineScope,
  private val prefs: SecurePrefs,
  private val json: Json,
  private val historyCacheStore: WebChatHistoryCacheStore,
  private val onAssistantReplyPresented: suspend (sessionKey: String, message: ChatMessage) -> Unit = { _, _ -> },
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

  private val _modelSupported = MutableStateFlow(false)
  val modelSupported: StateFlow<Boolean> = _modelSupported.asStateFlow()

  private val _stopInFlight = MutableStateFlow(false)
  val stopInFlight: StateFlow<Boolean> = _stopInFlight.asStateFlow()

  private val _thinkingOptions = MutableStateFlow<List<ChatThinkingOption>>(emptyList())
  val thinkingOptions: StateFlow<List<ChatThinkingOption>> = _thinkingOptions.asStateFlow()

  private val _thinkingOptionsLoading = MutableStateFlow(false)
  val thinkingOptionsLoading: StateFlow<Boolean> = _thinkingOptionsLoading.asStateFlow()

  private val _thinkingOptionsError = MutableStateFlow<String?>(null)
  val thinkingOptionsError: StateFlow<String?> = _thinkingOptionsError.asStateFlow()

  private val _thinkingModelLabel = MutableStateFlow<String?>(null)
  val thinkingModelLabel: StateFlow<String?> = _thinkingModelLabel.asStateFlow()

  private val _thinkingSwitchingLevel = MutableStateFlow<String?>(null)
  val thinkingSwitchingLevel: StateFlow<String?> = _thinkingSwitchingLevel.asStateFlow()

  private val _currentModel = MutableStateFlow<ChatModelOption?>(null)
  val currentModel: StateFlow<ChatModelOption?> = _currentModel.asStateFlow()

  private val _modelOptions = MutableStateFlow<List<ChatModelOption>>(emptyList())
  val modelOptions: StateFlow<List<ChatModelOption>> = _modelOptions.asStateFlow()

  private val _modelOptionsLoading = MutableStateFlow(false)
  val modelOptionsLoading: StateFlow<Boolean> = _modelOptionsLoading.asStateFlow()

  private val _modelOptionsError = MutableStateFlow<String?>(null)
  val modelOptionsError: StateFlow<String?> = _modelOptionsError.asStateFlow()

  private val _modelSwitchingLabel = MutableStateFlow<String?>(null)
  val modelSwitchingLabel: StateFlow<String?> = _modelSwitchingLabel.asStateFlow()

  private var activeAgentId: String? = parseAgentIdFromSessionKey(initialSessionKey)
  private var preferredBaseUrl: String? = null
  private var startupContactsLoaded = false
  private var startupContactsRefreshInFlight = false
  private val navigationVersion = AtomicLong(0L)
  private var navigationTargetSessionKey: String? = null
  private var loadedSessionKey: String? = null
  private val sessionHistoryCache = historyCacheStore.load().toMutableMap()
  private val prefetchingSessionKeys = mutableSetOf<String>()
  private var stopRequestedSessionKey: String? = null
  private var thinkingOptionsSessionKey: String? = null
  private var modelOptionsSessionKey: String? = null

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
    if (!_thinkingSupported.value) return
    val normalized = normalizeThinkingLevel(thinkingLevel)
    val currentSessionKey = _sessionKey.value.trim()
    if (currentSessionKey.isEmpty()) return
    if (_thinkingOptionsLoading.value || _thinkingSwitchingLevel.value != null) return
    if (normalized == _thinkingLevel.value && thinkingOptionsSessionKey == currentSessionKey) return

    _thinkingOptionsError.value = null
    _thinkingSwitchingLevel.value = normalized
    scope.launch {
      try {
        val payload =
          apiPatch(
            path = "/api/openclaw-webchat/sessions/${encodePath(currentSessionKey)}/thinking",
            body =
              buildJsonObject {
                put("thinkingLevel", JsonPrimitive(normalized))
              },
          )
        if (_sessionKey.value.trim() != currentSessionKey) return@launch
        applyThinkingPickerPayload(payload, currentSessionKey)
      } catch (err: Throwable) {
        if (_sessionKey.value.trim() == currentSessionKey) {
          _thinkingOptionsError.value = err.message ?: "Failed to switch thinking level"
        }
      } finally {
        if (_sessionKey.value.trim() == currentSessionKey) {
          _thinkingSwitchingLevel.value = null
        }
      }
    }
  }

  fun refreshThinkingOptions(force: Boolean = false, silent: Boolean = false) {
    if (!_thinkingSupported.value) {
      clearThinkingPickerState()
      return
    }
    val currentSessionKey = _sessionKey.value.trim()
    if (currentSessionKey.isEmpty()) {
      clearThinkingPickerState()
      return
    }
    if (
      !force &&
      thinkingOptionsSessionKey == currentSessionKey &&
      (_thinkingOptions.value.isNotEmpty() || _thinkingLevel.value.isNotBlank())
    ) {
      return
    }

    scope.launch {
      _thinkingOptionsLoading.value = true
      if (!silent) {
        _thinkingOptionsError.value = null
      }
      try {
        val payload =
          apiGet(
            path = "/api/openclaw-webchat/sessions/${encodePath(currentSessionKey)}/thinking-options",
          )
        if (_sessionKey.value.trim() != currentSessionKey) return@launch
        applyThinkingPickerPayload(payload, currentSessionKey)
      } catch (err: Throwable) {
        if (_sessionKey.value.trim() == currentSessionKey && !silent) {
          _thinkingOptionsError.value = err.message ?: "Failed to load thinking options"
        }
      } finally {
        if (_sessionKey.value.trim() == currentSessionKey) {
          _thinkingOptionsLoading.value = false
        }
      }
    }
  }

  fun setModel(provider: String, model: String) {
    if (!_modelSupported.value) return
    val currentSessionKey = _sessionKey.value.trim()
    if (currentSessionKey.isEmpty()) return
    if (_modelOptionsLoading.value || _modelSwitchingLabel.value != null) return

    val normalizedProvider = provider.trim().ifEmpty { "default" }
    val normalizedModel = model.trim()
    if (normalizedModel.isEmpty()) return
    if (_currentModel.value?.provider == normalizedProvider && _currentModel.value?.model == normalizedModel) return

    val target =
      _modelOptions.value.firstOrNull { it.provider == normalizedProvider && it.model == normalizedModel }
        ?: ChatModelOption(
          provider = normalizedProvider,
          model = normalizedModel,
          label = "$normalizedProvider/$normalizedModel",
        )

    _modelOptionsError.value = null
    _modelSwitchingLabel.value = target.label
    scope.launch {
      try {
        val payload =
          apiPatch(
            path = "/api/openclaw-webchat/sessions/${encodePath(currentSessionKey)}/model",
            body =
              buildJsonObject {
                put("provider", JsonPrimitive(normalizedProvider))
                put("model", JsonPrimitive(normalizedModel))
              },
          )
        if (_sessionKey.value.trim() != currentSessionKey) return@launch
        applyModelPickerPayload(payload, currentSessionKey)
        clearThinkingPickerState()
      } catch (err: Throwable) {
        if (_sessionKey.value.trim() == currentSessionKey) {
          _modelOptionsError.value = err.message ?: "Failed to switch model"
        }
      } finally {
        if (_sessionKey.value.trim() == currentSessionKey) {
          _modelSwitchingLabel.value = null
        }
      }
    }
  }

  fun refreshModelOptions(force: Boolean = false, silent: Boolean = false) {
    if (!_modelSupported.value) {
      clearModelPickerState()
      return
    }
    val currentSessionKey = _sessionKey.value.trim()
    if (currentSessionKey.isEmpty()) {
      clearModelPickerState()
      return
    }
    if (
      !force &&
      modelOptionsSessionKey == currentSessionKey &&
      (_currentModel.value != null || _modelOptions.value.isNotEmpty())
    ) {
      return
    }

    scope.launch {
      _modelOptionsLoading.value = true
      if (!silent) {
        _modelOptionsError.value = null
      }
      try {
        val payload =
          apiGet(
            path = "/api/openclaw-webchat/sessions/${encodePath(currentSessionKey)}/model-options",
          )
        if (_sessionKey.value.trim() != currentSessionKey) return@launch
        applyModelPickerPayload(payload, currentSessionKey)
      } catch (err: Throwable) {
        if (_sessionKey.value.trim() == currentSessionKey && !silent) {
          _modelOptionsError.value = err.message ?: "Failed to load model options"
        }
      } finally {
        if (_sessionKey.value.trim() == currentSessionKey) {
          _modelOptionsLoading.value = false
        }
      }
    }
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
    if (!_abortSupported.value) return
    val currentSessionKey = _sessionKey.value.trim()
    if (currentSessionKey.isEmpty()) return
    if (_pendingRunCount.value <= 0) return
    if (_stopInFlight.value) return

    stopRequestedSessionKey = currentSessionKey
    _stopInFlight.value = true
    scope.launch {
      try {
        apiPost(
          path = "/api/openclaw-webchat/sessions/${encodePath(currentSessionKey)}/stop",
          body = buildJsonObject {},
        )
        _errorText.value = null
      } catch (err: Throwable) {
        if (stopRequestedSessionKey == currentSessionKey) {
          _errorText.value = err.message ?: "Failed to stop reply"
          _stopInFlight.value = false
        }
      }
    }
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
      _stopInFlight.value = false
      stopRequestedSessionKey = null

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
        val uploadedBlocks = mutableListOf<JsonObject>()
        for (attachment in attachments) {
          if (isStopRequestedFor(currentSessionKey)) {
            return@launch
          }
          uploadedBlocks += uploadAttachment(attachment)
        }
        if (isStopRequestedFor(currentSessionKey)) {
          return@launch
        }
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
        if (response["aborted"].booleanOrFalse()) {
          return@launch
        }
        val assistantMessage = parsePresentedHistoryEntry(response["message"]) ?: return@launch
        _messages.value = _messages.value + assistantMessage
        updateSessionCache(sessionKey = currentSessionKey, agentId = agentId, messages = _messages.value)
        onAssistantReplyPresented(currentSessionKey, assistantMessage)
      } catch (err: Throwable) {
        if (!isStopRequestedFor(currentSessionKey)) {
          _errorText.value = err.message ?: "Send failed"
        }
      } finally {
        clearStopState(currentSessionKey)
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
      clearStopState(currentSessionKey)
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
      clearStopState()
      clearModelPickerState()
      clearThinkingPickerState()
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
      val settingsPayload = runCatching { apiGet("/api/openclaw-webchat/settings") }.getOrNull()
      applyComposerCapabilities(settingsPayload?.get("projectInfo"))
      settingsPayload
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
    if (_pendingRunCount.value == 0) {
      clearStopState()
    }
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

  private suspend fun apiPatch(path: String, body: JsonObject): JsonObject {
    return requestJson(method = "PATCH", path = path, body = body)
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
          if (method == "POST" || method == "PATCH") {
            val requestBody = (body?.toString() ?: "{}").toRequestBody(WEBCHAT_JSON_MEDIA_TYPE)
            if (method == "PATCH") {
              requestBuilder.patch(requestBody)
            } else {
              requestBuilder.post(requestBody)
            }
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
    return level.trim().lowercase(Locale.US).ifEmpty { "off" }
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

  private fun applyThinkingPickerPayload(payload: JsonObject, sessionKey: String) {
    _thinkingLevel.value = payload["currentLevel"].stringOrNull()?.trim().orEmpty().ifBlank { "off" }
    _thinkingOptions.value =
      (payload["options"] as? JsonArray).orEmpty().mapNotNull { option ->
        val obj = option.asObjectOrNull() ?: return@mapNotNull null
        val value = obj["value"].stringOrNull()?.trim().orEmpty()
        if (value.isEmpty()) return@mapNotNull null
        ChatThinkingOption(
          value = value,
          label = obj["label"].stringOrNull()?.trim().takeUnless { it.isNullOrEmpty() } ?: value,
        )
      }
    _thinkingModelLabel.value = payload["modelLabel"].stringOrNull()?.trim()
    _thinkingOptionsError.value = null
    thinkingOptionsSessionKey = sessionKey
  }

  private fun applyModelPickerPayload(payload: JsonObject, sessionKey: String) {
    _currentModel.value = parseModelOption(payload["current"])
    _modelOptions.value = (payload["models"] as? JsonArray).orEmpty().mapNotNull(::parseModelOption)
    _modelOptionsError.value = null
    modelOptionsSessionKey = sessionKey
  }

  private fun parseModelOption(element: JsonElement?): ChatModelOption? {
    val obj = element.asObjectOrNull() ?: return null
    val model = obj["model"].stringOrNull()?.trim().orEmpty()
    if (model.isEmpty()) return null
    val provider = obj["provider"].stringOrNull()?.trim().orEmpty().ifEmpty { "default" }
    val label = obj["label"].stringOrNull()?.trim().takeUnless { it.isNullOrEmpty() } ?: "$provider/$model"
    return ChatModelOption(
      provider = provider,
      model = model,
      label = label,
      available =
        when (obj["available"]) {
          null -> true
          else -> obj["available"].booleanOrFalse()
        },
    )
  }

  private fun clearModelPickerState() {
    _currentModel.value = null
    _modelOptions.value = emptyList()
    _modelOptionsLoading.value = false
    _modelOptionsError.value = null
    _modelSwitchingLabel.value = null
    modelOptionsSessionKey = null
  }

  private fun clearThinkingPickerState() {
    _thinkingLevel.value = "off"
    _thinkingOptionsLoading.value = false
    _thinkingOptionsError.value = null
    _thinkingModelLabel.value = null
    _thinkingSwitchingLevel.value = null
    _thinkingOptions.value = emptyList()
    thinkingOptionsSessionKey = null
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
      clearStopState()
      clearModelPickerState()
      clearThinkingPickerState()
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

  private fun isStopRequestedFor(sessionKey: String): Boolean = stopRequestedSessionKey == sessionKey

  private fun clearStopState(sessionKey: String? = null) {
    if (sessionKey == null || stopRequestedSessionKey == sessionKey) {
      stopRequestedSessionKey = null
    }
    _stopInFlight.value = false
  }

  private fun applyComposerCapabilities(projectInfo: JsonElement?) {
    val version = projectInfo.asObjectOrNull()?.get("version").stringOrNull()
    val supported = isVersionAtLeast(version, WEBCHAT_COMPOSER_CONTROL_MIN_VERSION)
    _abortSupported.value = supported
    _thinkingSupported.value = supported
    _modelSupported.value = supported
    if (!supported) {
      clearStopState()
      clearThinkingPickerState()
      clearModelPickerState()
    }
  }

  private fun isVersionAtLeast(version: String?, minimum: String): Boolean {
    val actualParts = parseVersionParts(version) ?: return false
    val minimumParts = parseVersionParts(minimum) ?: return false
    for (index in 0 until maxOf(actualParts.size, minimumParts.size)) {
      val actual = actualParts.getOrElse(index) { 0 }
      val required = minimumParts.getOrElse(index) { 0 }
      if (actual != required) {
        return actual > required
      }
    }
    return true
  }

  private fun parseVersionParts(version: String?): List<Int>? {
    val normalized =
      version
        ?.trim()
        ?.removePrefix("v")
        ?.substringBefore('-')
        ?.substringBefore('+')
        ?.takeIf { it.isNotEmpty() }
        ?: return null
    val parts = normalized.split('.')
    if (parts.isEmpty()) return null
    return parts.map { it.toIntOrNull() ?: return null }
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
