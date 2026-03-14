package ai.openclaw.app.ui.chat

import ai.openclaw.app.chat.ChatMessageContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatAttachmentSupportTest {
  @Test
  fun `classify prefers explicit type`() {
    assertEquals(ChatAttachmentKind.Audio, classifyChatAttachment(type = "audio", mimeType = "application/octet-stream"))
    assertEquals(ChatAttachmentKind.Video, classifyChatAttachment(type = "video", mimeType = null))
    assertEquals(ChatAttachmentKind.Image, classifyChatAttachment(type = "image", mimeType = "audio/mpeg"))
  }

  @Test
  fun `classify falls back to mime type`() {
    assertEquals(ChatAttachmentKind.Image, classifyChatAttachment(type = null, mimeType = "image/png"))
    assertEquals(ChatAttachmentKind.Audio, classifyChatAttachment(type = "", mimeType = "audio/mpeg"))
    assertEquals(ChatAttachmentKind.Video, classifyChatAttachment(type = "file", mimeType = "video/mp4"))
  }

  @Test
  fun `classify unknown attachment`() {
    assertEquals(ChatAttachmentKind.Unknown, classifyChatAttachment(type = "file", mimeType = "application/pdf"))
    assertEquals(ChatAttachmentKind.Unknown, classifyChatAttachment(type = null, mimeType = null))
  }

  @Test
  fun `descriptor preserves remote media fields`() {
    val descriptor =
      ChatMessageContent(
        type = "image",
        mimeType = "image/png",
        fileName = "photo.png",
        mediaUrl = "http://10.0.2.2:39393/media/photo.png",
        mediaPath = "/media/photo.png",
        mediaPort = 39393,
        mediaSha256 = "abc123",
        sizeBytes = 42L,
      ).toAttachmentDescriptor()

    assertEquals(ChatAttachmentKind.Image, descriptor.kind)
    assertEquals("http://10.0.2.2:39393/media/photo.png", descriptor.mediaUrl)
    assertEquals("/media/photo.png", descriptor.mediaPath)
    assertEquals(39393, descriptor.mediaPort)
    assertEquals("abc123", descriptor.mediaSha256)
    assertEquals(42L, descriptor.sizeBytes)
    assertNull(descriptor.base64)
  }

  @Test
  fun `fetch error explains emulator only host`() {
    val message =
      formatAttachmentFetchErrorMessage(
        mediaUrl = "http://10.0.2.2:39393/media/photo.png",
        fallbackMessage = "failed to connect",
      )

    assertTrue(message.contains("emulator-only host 10.0.2.2"))
    assertTrue(message.contains("mediaPath/mediaPort"))
  }

  @Test
  fun `fetch error falls back to original message for normal host`() {
    val message =
      formatAttachmentFetchErrorMessage(
        mediaUrl = "http://192.0.2.10:39393/media/photo.png",
        fallbackMessage = "failed to connect",
      )

    assertEquals("failed to connect", message)
  }

  @Test
  fun `attachment fetch urls include current lan and tailscale hosts`() {
    val urls =
      resolveAttachmentFetchUrls(
        mediaUrl = "http://10.0.2.2:39393/media/photo.png",
        mediaPath = "/media/photo.png",
        mediaPort = 39393,
        gatewayRemoteAddress = "192.0.2.10:18789",
        manualHost = "192.0.2.10",
        tailscaleHost = "tailnet.example.ts.net",
      )

    assertEquals(
      listOf(
        "http://192.0.2.10:39393/media/photo.png",
        "http://tailnet.example.ts.net:39393/media/photo.png",
        "http://10.0.2.2:39393/media/photo.png",
      ),
      urls,
    )
  }

  @Test
  fun `attachment fetch urls dedupe repeated hosts`() {
    val urls =
      resolveAttachmentFetchUrls(
        mediaUrl = "http://tailnet.example.ts.net:39393/media/photo.png",
        mediaPath = "/media/photo.png",
        mediaPort = 39393,
        gatewayRemoteAddress = "tailnet.example.ts.net:18789",
        manualHost = "tailnet.example.ts.net",
        tailscaleHost = "tailnet.example.ts.net",
      )

    assertEquals(
      listOf(
        "http://tailnet.example.ts.net:39393/media/photo.png",
      ),
      urls,
    )
  }

  @Test
  fun `attachment fetch urls can resolve from gateway path without media url`() {
    val urls =
      resolveAttachmentFetchUrls(
        mediaUrl = null,
        mediaPath = "/media/photo.png",
        mediaPort = 39393,
        gatewayRemoteAddress = "tailnet.example.ts.net:18789",
        manualHost = "192.0.2.10",
        tailscaleHost = "tailnet.example.ts.net",
      )

    assertEquals(
      listOf(
        "http://tailnet.example.ts.net:39393/media/photo.png",
        "http://192.0.2.10:39393/media/photo.png",
      ),
      urls,
    )
  }
}
