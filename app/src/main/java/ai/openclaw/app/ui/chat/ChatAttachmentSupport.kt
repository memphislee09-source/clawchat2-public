package ai.openclaw.app.ui.chat

import ai.openclaw.app.chat.ChatMessageContent
import java.net.URI
import java.util.Locale

internal enum class ChatAttachmentKind {
  Image,
  Audio,
  Video,
  Unknown,
}

internal data class ChatAttachmentDescriptor(
  val kind: ChatAttachmentKind,
  val mimeType: String?,
  val fileName: String?,
  val base64: String?,
  val mediaUrl: String?,
  val mediaPath: String?,
  val mediaPort: Int?,
  val mediaSha256: String?,
  val sizeBytes: Long?,
)

internal fun ChatMessageContent.toAttachmentDescriptor(): ChatAttachmentDescriptor {
  return ChatAttachmentDescriptor(
    kind = classifyChatAttachment(type = type, mimeType = mimeType),
    mimeType = mimeType?.trim()?.ifEmpty { null },
    fileName = fileName?.trim()?.ifEmpty { null },
    base64 = base64?.trim()?.ifEmpty { null },
    mediaUrl = mediaUrl?.trim()?.ifEmpty { null },
    mediaPath = mediaPath?.trim()?.ifEmpty { null },
    mediaPort = mediaPort?.takeIf { it in 1..65535 },
    mediaSha256 = mediaSha256?.trim()?.ifEmpty { null },
    sizeBytes = sizeBytes?.takeIf { it >= 0L },
  )
}

internal fun classifyChatAttachment(type: String?, mimeType: String?): ChatAttachmentKind {
  val normalizedType = type?.trim()?.lowercase(Locale.US).orEmpty()
  val normalizedMime = mimeType?.trim()?.lowercase(Locale.US).orEmpty()

  return when {
    normalizedType == "image" || normalizedMime.startsWith("image/") -> ChatAttachmentKind.Image
    normalizedType == "audio" || normalizedMime.startsWith("audio/") -> ChatAttachmentKind.Audio
    normalizedType == "video" || normalizedMime.startsWith("video/") -> ChatAttachmentKind.Video
    else -> ChatAttachmentKind.Unknown
  }
}

internal fun attachmentDisplayName(descriptor: ChatAttachmentDescriptor): String {
  return descriptor.fileName ?: defaultAttachmentName(descriptor.kind, descriptor.mimeType)
}

internal fun formatAttachmentFetchErrorMessage(mediaUrl: String?, fallbackMessage: String?): String {
  val normalizedFallback = fallbackMessage?.trim()?.ifEmpty { null }
  val host =
    runCatching { mediaUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { URI(it).host } }.getOrNull()
      ?.lowercase(Locale.US)

  if (host == "10.0.2.2" || host == "10.2.2.2") {
    return "Legacy mediaUrl uses emulator-only host $host. Updated ClawChat2 builds should resolve media via mediaPath/mediaPort on the current gateway host."
  }

  return normalizedFallback ?: "Attachment fetch failed"
}

internal fun resolveAttachmentFetchUrls(
  mediaUrl: String?,
  mediaPath: String?,
  mediaPort: Int?,
  gatewayRemoteAddress: String?,
  manualHost: String?,
  tailscaleHost: String?,
): List<String> {
  val directUrl = mediaUrl?.trim()?.ifEmpty { null }
  val relativePath = mediaPath?.trim()?.ifEmpty { null }
  val resolvedPort = mediaPort?.takeIf { it in 1..65535 }
  val urls = linkedSetOf<String>()

  if (relativePath != null && resolvedPort != null) {
    resolveGatewayMediaHosts(
      gatewayRemoteAddress = gatewayRemoteAddress,
      manualHost = manualHost,
      tailscaleHost = tailscaleHost,
      mediaUrl = directUrl,
    ).forEach { host ->
      urls += buildMediaUrl(host = host, port = resolvedPort, path = relativePath)
    }
  }

  if (directUrl != null) {
    urls += resolveLegacyAttachmentFetchUrls(directUrl, manualHost, tailscaleHost)
  }

  return urls.toList().ifEmpty { listOfNotNull(directUrl) }
}

private fun resolveLegacyAttachmentFetchUrls(
  mediaUrl: String,
  manualHost: String?,
  tailscaleHost: String?,
): List<String> {
  val parsed = runCatching { URI(mediaUrl.trim()) }.getOrNull() ?: return listOf(mediaUrl)
  val scheme = parsed.scheme?.trim()?.ifEmpty { null } ?: return listOf(mediaUrl)
  val rawPath = parsed.rawPath?.ifEmpty { "/" } ?: "/"
  val rawQuery = parsed.rawQuery
  val rawFragment = parsed.rawFragment

  val hosts =
    buildList {
      add(parsed.host.orEmpty())
      manualHost?.trim()?.takeIf { it.isNotEmpty() }?.let(::add)
      tailscaleHost?.trim()?.takeIf { it.isNotEmpty() }?.let(::add)
    }.map { it.lowercase(Locale.US) }
      .filter { it.isNotEmpty() }
      .distinct()

  if (hosts.isEmpty()) return listOf(mediaUrl)

  return hosts.map { host ->
    URI(scheme, null, host, parsed.port, rawPath, rawQuery, rawFragment).toASCIIString()
  }
}

private fun resolveGatewayMediaHosts(
  gatewayRemoteAddress: String?,
  manualHost: String?,
  tailscaleHost: String?,
  mediaUrl: String?,
): List<String> {
  return buildList {
    gatewayRemoteAddress?.trim()?.takeIf { it.isNotEmpty() }?.let(::add)
    manualHost?.trim()?.takeIf { it.isNotEmpty() }?.let(::add)
    tailscaleHost?.trim()?.takeIf { it.isNotEmpty() }?.let(::add)
    mediaUrl?.trim()?.takeIf { it.isNotEmpty() }?.let(::add)
  }.mapNotNull(::extractHostCandidate)
    .distinct()
}

private fun extractHostCandidate(rawValue: String): String? {
  val trimmed = rawValue.trim()
  if (trimmed.isEmpty()) return null

  if (trimmed.contains("://")) {
    val parsed = runCatching { URI(trimmed) }.getOrNull() ?: return null
    return parsed.host?.trim()?.ifEmpty { null }?.lowercase(Locale.US)
  }

  val parsed = runCatching { URI("ws://$trimmed") }.getOrNull()
  return parsed?.host?.trim()?.ifEmpty { null }?.lowercase(Locale.US)
}

private fun buildMediaUrl(host: String, port: Int, path: String): String {
  val normalizedPath = if (path.startsWith('/')) path else "/$path"
  return URI("http", null, host, port, normalizedPath, null, null).toASCIIString()
}

private fun defaultAttachmentName(kind: ChatAttachmentKind, mimeType: String?): String {
  return when (kind) {
    ChatAttachmentKind.Image -> "image"
    ChatAttachmentKind.Audio -> "audio"
    ChatAttachmentKind.Video -> "video"
    ChatAttachmentKind.Unknown -> mimeType ?: "attachment"
  }
}
