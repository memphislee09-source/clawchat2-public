package ai.openclaw.app.chat

import ai.openclaw.app.SecurePrefs
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WebChatControllerTest {
  @Test
  fun abort_postsSessionStopWhileSendRequestIsPending() =
    runBlocking {
      val json = Json { ignoreUnknownKeys = true }
      val app = RuntimeEnvironment.getApplication()
      val prefs = SecurePrefs(app)
      val historyCacheStore = WebChatHistoryCacheStore(app, json)
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

      val sendStarted = CountDownLatch(1)
      val releaseSend = CountDownLatch(1)
      val stopRequested = CountDownLatch(1)
      val stopPath = AtomicReference<String?>(null)

      val server =
        MockWebServer().apply {
          dispatcher =
            object : Dispatcher() {
              override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.requestUrl?.encodedPath) {
                  "/api/openclaw-webchat/sessions/openclaw-webchat%3Atest-agent/send" -> {
                    sendStarted.countDown()
                    releaseSend.await(5, TimeUnit.SECONDS)
                    MockResponse().setResponseCode(200).setBody("""{"ok":true,"message":null,"aborted":true}""")
                  }

                  "/api/openclaw-webchat/sessions/openclaw-webchat%3Atest-agent/stop" -> {
                    stopPath.set(request.path)
                    stopRequested.countDown()
                    MockResponse().setResponseCode(200).setBody("""{"ok":true,"aborted":true}""")
                  }

                  else -> MockResponse().setResponseCode(404)
                }
              }
            }
          start()
        }

      val controller =
        WebChatController(
          scope = scope,
          prefs = prefs,
          json = json,
          historyCacheStore = historyCacheStore,
          initialSessionKey = "openclaw-webchat:test-agent",
        )
      setPreferredBaseUrl(controller, server.url("/").toString().removeSuffix("/"))
      setComposerCapabilities(controller, supported = true)

      try {
        controller.sendMessage(message = "hello", thinkingLevel = "off", attachments = emptyList())

        assertTrue(sendStarted.await(5, TimeUnit.SECONDS))
        withTimeout(5_000L) {
          while (controller.pendingRunCount.value != 1) {
            delay(10)
          }
        }

        controller.abort()

        assertTrue(stopRequested.await(5, TimeUnit.SECONDS))
        assertEquals(
          "/api/openclaw-webchat/sessions/openclaw-webchat%3Atest-agent/stop",
          stopPath.get(),
        )
        assertTrue(controller.stopInFlight.value)

        releaseSend.countDown()

        withTimeout(5_000L) {
          while (controller.pendingRunCount.value != 0 || controller.stopInFlight.value) {
            delay(10)
          }
        }

        assertFalse(controller.stopInFlight.value)
      } finally {
        releaseSend.countDown()
        scope.cancel()
        server.shutdown()
      }
    }

  @Test
  fun thinkingPicker_loadsOptionsAndSwitchesCurrentSessionLevel() =
    runBlocking {
      val json = Json { ignoreUnknownKeys = true }
      val app = RuntimeEnvironment.getApplication()
      val prefs = SecurePrefs(app)
      val historyCacheStore = WebChatHistoryCacheStore(app, json)
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

      val currentThinking = AtomicReference("medium")

      val server =
        MockWebServer().apply {
          dispatcher =
            object : Dispatcher() {
              override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.requestUrl?.encodedPath) {
                  "/api/openclaw-webchat/sessions/openclaw-webchat%3Atest-agent/thinking-options" -> {
                    MockResponse().setResponseCode(200).setBody(thinkingPayload(currentThinking.get()))
                  }

                  "/api/openclaw-webchat/sessions/openclaw-webchat%3Atest-agent/thinking" -> {
                    currentThinking.set("high")
                    MockResponse().setResponseCode(200).setBody(thinkingPayload(currentThinking.get(), ok = true))
                  }

                  else -> MockResponse().setResponseCode(404)
                }
              }
            }
          start()
        }

      val controller =
        WebChatController(
          scope = scope,
          prefs = prefs,
          json = json,
          historyCacheStore = historyCacheStore,
          initialSessionKey = "openclaw-webchat:test-agent",
        )
      setPreferredBaseUrl(controller, server.url("/").toString().removeSuffix("/"))
      setComposerCapabilities(controller, supported = true)

      try {
        controller.refreshThinkingOptions(force = true, silent = false)

        withTimeout(5_000L) {
          while (controller.thinkingOptionsLoading.value || controller.thinkingOptions.value.isEmpty()) {
            delay(10)
          }
        }

        assertEquals("medium", controller.thinkingLevel.value)
        assertEquals("gpt-test", controller.thinkingModelLabel.value)
        assertEquals(listOf("off", "medium", "high"), controller.thinkingOptions.value.map { it.value })

        controller.setThinkingLevel("high")

        withTimeout(5_000L) {
          while (controller.thinkingSwitchingLevel.value != null) {
            delay(10)
          }
        }

        assertEquals("high", controller.thinkingLevel.value)
        assertEquals("high", currentThinking.get())
        assertEquals(null, controller.thinkingOptionsError.value)
      } finally {
        scope.cancel()
        server.shutdown()
      }
    }

  @Test
  fun modelPicker_loadsOptionsAndSwitchesCurrentSessionModel() =
    runBlocking {
      val json = Json { ignoreUnknownKeys = true }
      val app = RuntimeEnvironment.getApplication()
      val prefs = SecurePrefs(app)
      val historyCacheStore = WebChatHistoryCacheStore(app, json)
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

      val currentLabel = AtomicReference("openai/gpt-5")

      val server =
        MockWebServer().apply {
          dispatcher =
            object : Dispatcher() {
              override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.requestUrl?.encodedPath) {
                  "/api/openclaw-webchat/sessions/openclaw-webchat%3Atest-agent/model-options" -> {
                    MockResponse().setResponseCode(200).setBody(modelPayload(currentLabel.get()))
                  }

                  "/api/openclaw-webchat/sessions/openclaw-webchat%3Atest-agent/model" -> {
                    currentLabel.set("anthropic/claude-sonnet-4")
                    MockResponse().setResponseCode(200).setBody(modelPayload(currentLabel.get(), ok = true))
                  }

                  else -> MockResponse().setResponseCode(404)
                }
              }
            }
          start()
        }

      val controller =
        WebChatController(
          scope = scope,
          prefs = prefs,
          json = json,
          historyCacheStore = historyCacheStore,
          initialSessionKey = "openclaw-webchat:test-agent",
        )
      setPreferredBaseUrl(controller, server.url("/").toString().removeSuffix("/"))
      setComposerCapabilities(controller, supported = true)

      try {
        controller.refreshModelOptions(force = true, silent = false)

        withTimeout(5_000L) {
          while (controller.modelOptionsLoading.value || controller.modelOptions.value.isEmpty()) {
            delay(10)
          }
        }

        assertEquals("openai/gpt-5", controller.currentModel.value?.label)
        assertEquals(
          listOf("openai/gpt-5", "anthropic/claude-sonnet-4"),
          controller.modelOptions.value.map { it.label },
        )
        assertEquals(listOf(true, true), controller.modelOptions.value.map { it.available })

        controller.setModel(provider = "anthropic", model = "claude-sonnet-4")

        withTimeout(5_000L) {
          while (controller.modelSwitchingLabel.value != null) {
            delay(10)
          }
        }

        assertEquals("anthropic/claude-sonnet-4", controller.currentModel.value?.label)
        assertEquals("anthropic/claude-sonnet-4", currentLabel.get())
        assertEquals(null, controller.modelOptionsError.value)
      } finally {
        scope.cancel()
        server.shutdown()
      }
    }

  @Test
  fun composerControls_followWebChatVersionCapabilityGate() =
    runBlocking {
      val json = Json { ignoreUnknownKeys = true }
      val app = RuntimeEnvironment.getApplication()
      val prefs = SecurePrefs(app)
      val historyCacheStore = WebChatHistoryCacheStore(app, json)
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

      val version = AtomicReference("0.1.5")

      val server =
        MockWebServer().apply {
          dispatcher =
            object : Dispatcher() {
              override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.requestUrl?.encodedPath) {
                  "/api/openclaw-webchat/agents" -> {
                    MockResponse().setResponseCode(200).setBody(agentsPayload())
                  }

                  "/api/openclaw-webchat/settings" -> {
                    MockResponse().setResponseCode(200).setBody(settingsPayload(version.get()))
                  }

                  else -> MockResponse().setResponseCode(404)
                }
              }
            }
          start()
        }

      val controller =
        WebChatController(
          scope = scope,
          prefs = prefs,
          json = json,
          historyCacheStore = historyCacheStore,
          initialSessionKey = "openclaw-webchat:test-agent",
        )
      setPreferredBaseUrl(controller, server.url("/").toString().removeSuffix("/"))

      try {
        controller.refreshAgentContacts()
        awaitCapabilityValue(controller, expectedSupported = false)

        version.set("0.1.6")
        controller.refreshAgentContacts()
        awaitCapabilityValue(controller, expectedSupported = true)
      } finally {
        scope.cancel()
        server.shutdown()
      }
    }

  private fun setPreferredBaseUrl(controller: WebChatController, baseUrl: String) {
    val field = controller.javaClass.getDeclaredField("preferredBaseUrl")
    field.isAccessible = true
    field.set(controller, baseUrl)
  }

  private fun setComposerCapabilities(controller: WebChatController, supported: Boolean) {
    setBooleanStateFlow(controller, "_abortSupported", supported)
    setBooleanStateFlow(controller, "_thinkingSupported", supported)
    setBooleanStateFlow(controller, "_modelSupported", supported)
  }

  private fun setBooleanStateFlow(controller: WebChatController, fieldName: String, value: Boolean) {
    val field = controller.javaClass.getDeclaredField(fieldName)
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val flow = field.get(controller) as MutableStateFlow<Boolean>
    flow.value = value
  }

  private suspend fun awaitCapabilityValue(controller: WebChatController, expectedSupported: Boolean) {
    withTimeout(5_000L) {
      while (
        controller.agentContactsRefreshing.value ||
        controller.agentContacts.value.isEmpty() ||
        controller.abortSupported.value != expectedSupported ||
        controller.thinkingSupported.value != expectedSupported ||
        controller.modelSupported.value != expectedSupported
      ) {
        delay(10)
      }
    }
  }

  private fun agentsPayload(): String {
    return """
      {
        "agents": [
          {
            "agentId": "test-agent",
            "name": "Test Agent",
            "sessionKey": "openclaw-webchat:test-agent",
            "hasSession": true,
            "presence": "idle"
          }
        ]
      }
    """.trimIndent()
  }

  private fun settingsPayload(version: String): String {
    return """
      {
        "userProfile": {
          "displayName": "我"
        },
        "serviceSettings": {
          "networkAccess": "local"
        },
        "projectInfo": {
          "name": "openclaw-webchat",
          "version": "$version"
        },
        "authStatus": {
          "enabled": false,
          "authenticated": true
        }
      }
    """.trimIndent()
  }

  private fun thinkingPayload(level: String, ok: Boolean = false): String {
    return buildString {
      append("{")
      if (ok) {
        append("\"ok\":true,")
      }
      append("\"currentLevel\":\"")
      append(level)
      append("\",")
      append("\"options\":[")
      append("""{"value":"off","label":"off"},""")
      append("""{"value":"medium","label":"medium"},""")
      append("""{"value":"high","label":"high"}""")
      append("],")
      append("\"modelLabel\":\"gpt-test\",")
      append("\"updatedAt\":\"2026-03-28T13:30:00Z\"")
      append("}")
    }
  }

  private fun modelPayload(currentLabel: String, ok: Boolean = false): String {
    val currentProvider = currentLabel.substringBefore("/")
    val currentModel = currentLabel.substringAfter("/")
    return buildString {
      append("{")
      if (ok) {
        append("\"ok\":true,")
      }
      append("\"current\":{")
      append("\"provider\":\"")
      append(currentProvider)
      append("\",\"model\":\"")
      append(currentModel)
      append("\",\"label\":\"")
      append(currentLabel)
      append("\"},")
      append("\"models\":[")
      append("""{"provider":"openai","model":"gpt-5","label":"openai/gpt-5"},""")
      append("""{"provider":"anthropic","model":"claude-sonnet-4","label":"anthropic/claude-sonnet-4"}""")
      append("],")
      append("\"updatedAt\":\"2026-03-28T14:20:00Z\"")
      append("}")
    }
  }
}
