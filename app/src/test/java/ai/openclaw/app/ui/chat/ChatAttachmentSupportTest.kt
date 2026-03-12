package ai.openclaw.app.ui.chat

import ai.openclaw.app.chat.ChatMessageContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        mediaSha256 = "abc123",
        sizeBytes = 42L,
      ).toAttachmentDescriptor()

    assertEquals(ChatAttachmentKind.Image, descriptor.kind)
    assertEquals("http://10.0.2.2:39393/media/photo.png", descriptor.mediaUrl)
    assertEquals("abc123", descriptor.mediaSha256)
    assertEquals(42L, descriptor.sizeBytes)
    assertNull(descriptor.base64)
  }
}
