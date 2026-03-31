package ai.openclaw.app.gateway

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

private const val PAIRING_TEST_TIMEOUT_MS = 8_000L
private const val PAIRING_CONNECT_CHALLENGE_FRAME =
  """{"type":"event","event":"connect.challenge","payload":{"nonce":"android-test-nonce"}}"""

private class PairingTestDeviceAuthStore : DeviceAuthTokenStore {
  private val tokens = mutableMapOf<String, String>()

  override fun loadToken(deviceId: String, role: String): String? = tokens["${deviceId.trim()}|${role.trim()}"]

  override fun saveToken(deviceId: String, role: String, token: String) {
    tokens["${deviceId.trim()}|${role.trim()}"] = token.trim()
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GatewaySessionPairingTest {
  @Test
  fun pairingRequired_pausesAutoReconnectUntilManualReconnect() = runBlocking {
    val json = Json { ignoreUnknownKeys = true }
    val connectAttempts = AtomicInteger(0)
    val lastDisconnect = AtomicReference("")
    val server =
      MockWebServer().apply {
        dispatcher =
          object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
              return MockResponse().withWebSocketUpgrade(
                object : WebSocketListener() {
                  override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(PAIRING_CONNECT_CHALLENGE_FRAME)
                  }

                  override fun onMessage(webSocket: WebSocket, text: String) {
                    val frame = json.parseToJsonElement(text).jsonObject
                    if (frame["type"]?.jsonPrimitive?.content != "req") return
                    val id = frame["id"]?.jsonPrimitive?.content ?: return
                    val method = frame["method"]?.jsonPrimitive?.content ?: return
                    if (method != "connect") return
                    connectAttempts.incrementAndGet()
                    webSocket.send(
                      """{"type":"res","id":"$id","ok":false,"error":{"code":"PAIRING_REQUIRED","message":"pairing required"}}""",
                    )
                    webSocket.close(1008, "pairing required")
                  }
                },
              )
            }
          }
      }

    val app = RuntimeEnvironment.getApplication()
    val sessionJob = SupervisorJob()
    val session =
      GatewaySession(
        scope = CoroutineScope(sessionJob + Dispatchers.Default),
        identityStore = DeviceIdentityStore(app),
        deviceAuthStore = PairingTestDeviceAuthStore(),
        onConnected = { _, _, _ -> },
        onDisconnected = { message -> lastDisconnect.set(message) },
        onEvent = { _, _ -> },
      )

    try {
      session.connect(
        endpoint =
          GatewayEndpoint(
            stableId = "manual|127.0.0.1|${server.port}",
            name = "pairing-test",
            host = "127.0.0.1",
            port = server.port,
            tlsEnabled = false,
          ),
        token = null,
        bootstrapToken = "bootstrap-test-token",
        password = null,
        options =
          GatewayConnectOptions(
            role = "operator",
            scopes = listOf("operator.read", "operator.write"),
            caps = emptyList(),
            commands = emptyList(),
            permissions = emptyMap(),
            client =
              GatewayClientInfo(
                id = "openclaw-android-test",
                displayName = "Android Test",
                version = "1.0.0-test",
                platform = "android",
                mode = "ui",
                instanceId = "android-test-instance",
                deviceFamily = "android",
                modelIdentifier = "test-device",
              ),
          ),
        tls = null,
      )

      waitForCondition {
        connectAttempts.get() == 1 && lastDisconnect.get().contains("pairing required", ignoreCase = true)
      }

      delay(1_500)
      assertEquals(1, connectAttempts.get())

      session.reconnect()

      waitForCondition { connectAttempts.get() == 2 }
      assertTrue(lastDisconnect.get().contains("pairing required", ignoreCase = true))
    } finally {
      session.disconnect()
      sessionJob.cancelAndJoin()
      server.shutdown()
    }
  }

  @Test
  fun looksLikePairingRequired_matchesCommonGatewayMessages() {
    assertTrue(looksLikePairingRequired("PAIRING_REQUIRED: device not approved"))
    assertTrue(looksLikePairingRequired("Gateway closed: pairing required"))
    assertTrue(looksLikePairingRequired("awaiting approval"))
  }

  private suspend fun waitForCondition(predicate: () -> Boolean) {
    withTimeout(PAIRING_TEST_TIMEOUT_MS) {
      while (!predicate()) {
        delay(50)
      }
    }
  }
}
