package ai.openclaw.app.chat

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentContactsTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun `parseConfiguredAgents reads identity name and emoji`() {
    val configJson =
      """
      {
        "config": {
          "agents": {
            "list": [
              {
                "id": "atlas",
                "identity": {
                  "name": "Atlas",
                  "emoji": "🦊"
                }
              },
              {
                "id": "nova",
                "identity": {
                  "name": "Nova"
                }
              }
            ]
          }
        }
      }
      """.trimIndent()

    val agents = parseConfiguredAgents(configJson = configJson, json = json)

    assertEquals(2, agents.size)
    assertEquals(ConfiguredAgentEntry(agentId = "atlas", displayName = "Atlas", emoji = "🦊"), agents[0])
    assertEquals(ConfiguredAgentEntry(agentId = "nova", displayName = "Nova", emoji = null), agents[1])
  }

  @Test
  fun `resolveAgentContacts only matches clawchat2 direct sessions`() {
    val agents =
      listOf(
        ConfiguredAgentEntry(agentId = "atlas", displayName = "Atlas", emoji = "🦊"),
        ConfiguredAgentEntry(agentId = "nova", displayName = "Nova", emoji = "🌙"),
      )
    val sessions =
      listOf(
        ChatSessionEntry(key = "agent:atlas:clawchat2", updatedAtMs = 200L),
        ChatSessionEntry(key = "whatsapp:atlas-room", updatedAtMs = 500L),
        ChatSessionEntry(key = "agent:nova:main", updatedAtMs = 800L),
      )

    val contacts = resolveAgentContacts(agents = agents, sessions = sessions)

    assertEquals(listOf("atlas", "nova"), contacts.map { it.agentId })
    assertTrue(contacts[0].hasDirectSession)
    assertEquals("agent:atlas:clawchat2", contacts[0].directSessionKey)
    assertEquals(200L, contacts[0].directSessionUpdatedAtMs)
    assertFalse(contacts[1].hasDirectSession)
    assertEquals("agent:nova:clawchat2", contacts[1].directSessionKey)
    assertNull(contacts[1].directSessionUpdatedAtMs)
  }

  @Test
  fun `clawChat2SessionKey uses strict clawchat2 suffix`() {
    assertEquals("agent:atlas:clawchat2", clawChat2SessionKey("atlas"))
    assertEquals("agent:nova:clawchat2", clawChat2SessionKey(" nova "))
  }

  @Test
  fun `extractLatestPreviewText prefers the latest non-system message`() {
    val historyJson =
      """
      {
        "messages": [
          {
            "role": "system",
            "content": [{"type": "text", "text": "ignore"}]
          },
          {
            "role": "assistant",
            "content": [{"type": "text", "text": "Earlier reply"}]
          },
          {
            "role": "user",
            "content": [{"type": "text", "text": "Latest user message"}]
          }
        ]
      }
      """.trimIndent()

    val preview = extractLatestPreviewText(historyJson = historyJson, json = json)

    assertEquals("Latest user message", preview)
  }
}
