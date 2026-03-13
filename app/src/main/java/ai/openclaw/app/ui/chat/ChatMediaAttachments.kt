package ai.openclaw.app.ui.chat

import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.util.Base64
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import ai.openclaw.app.SecurePrefs
import ai.openclaw.app.ui.mobileAccent
import ai.openclaw.app.ui.mobileAccentSoft
import ai.openclaw.app.ui.mobileBorder
import ai.openclaw.app.ui.mobileBorderStrong
import ai.openclaw.app.ui.mobileCallout
import ai.openclaw.app.ui.mobileCaption1
import ai.openclaw.app.ui.mobileCaption2
import ai.openclaw.app.ui.mobileCodeBg
import ai.openclaw.app.ui.mobileSurface
import ai.openclaw.app.ui.mobileText
import ai.openclaw.app.ui.mobileTextSecondary
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

internal data class ResolvedMediaFileState(
  val file: File?,
  val loading: Boolean,
  val failed: Boolean,
  val errorText: String? = null,
)

private data class MediaPreviewState(
  val durationMs: Long?,
  val previewFrame: ImageBitmap?,
  val failed: Boolean,
)

private val mediaHttpClient: OkHttpClient by lazy {
  OkHttpClient.Builder().build()
}

@Composable
internal fun ChatMediaAttachment(descriptor: ChatAttachmentDescriptor) {
  when (descriptor.kind) {
    ChatAttachmentKind.Image -> ChatImageAttachment(descriptor = descriptor)

    ChatAttachmentKind.Audio -> ChatAudioAttachment(descriptor = descriptor)
    ChatAttachmentKind.Video -> ChatVideoAttachment(descriptor = descriptor)
    ChatAttachmentKind.Unknown ->
      ChatUnsupportedAttachmentCard(
        descriptor = descriptor,
        title = "Unsupported attachment",
        detail = descriptor.mimeType ?: "Unknown media type",
        iconTint = mobileTextSecondary,
      )
  }
}

@Composable
private fun ChatImageAttachment(descriptor: ChatAttachmentDescriptor) {
  when {
    descriptor.base64 != null -> {
      ChatBase64Image(base64 = descriptor.base64, mimeType = descriptor.mimeType)
    }

    descriptor.mediaUrl != null -> {
      val fileState = rememberResolvedMediaFileState(descriptor = descriptor)
      when {
        fileState.file != null -> ChatFileImage(file = fileState.file, mimeType = descriptor.mimeType)
        fileState.loading ->
          ChatLoadingAttachmentCard(
            descriptor = descriptor,
            title = "Loading image",
            detail = descriptor.mimeType ?: "Fetching remote media",
          )

        else ->
          ChatUnsupportedAttachmentCard(
            descriptor = descriptor,
            title = "Image unavailable",
            detail = fileState.errorText ?: "Could not fetch image attachment",
            iconTint = mobileTextSecondary,
          )
      }
    }

    else -> {
      ChatUnsupportedAttachmentCard(
        descriptor = descriptor,
        title = "Image unavailable",
        detail = "Attachment content missing",
        iconTint = mobileTextSecondary,
      )
    }
  }
}

@Composable
private fun ChatAudioAttachment(descriptor: ChatAttachmentDescriptor) {
  if (descriptor.base64 == null && descriptor.mediaUrl == null) {
    ChatUnsupportedAttachmentCard(
      descriptor = descriptor,
      title = "Audio unavailable",
      detail = "Attachment content missing",
      iconTint = mobileTextSecondary,
    )
    return
  }

  val fileState = rememberResolvedMediaFileState(descriptor = descriptor)
  if (fileState.file == null) {
    if (fileState.loading) {
      ChatLoadingAttachmentCard(
        descriptor = descriptor,
        title = "Loading audio",
        detail = descriptor.mimeType ?: "Fetching remote media",
      )
    } else {
      ChatUnsupportedAttachmentCard(
        descriptor = descriptor,
        title = "Audio unavailable",
        detail = fileState.errorText ?: "Could not decode audio attachment",
        iconTint = mobileTextSecondary,
      )
    }
    return
  }

  val scope = rememberCoroutineScope()
  val file = fileState.file
  var player by remember(file.absolutePath) { mutableStateOf<MediaPlayer?>(null) }
  var isPreparing by remember(file.absolutePath) { mutableStateOf(false) }
  var isPlaying by remember(file.absolutePath) { mutableStateOf(false) }
  var durationMs by remember(file.absolutePath) { mutableStateOf<Long?>(null) }
  var positionMs by remember(file.absolutePath) { mutableStateOf(0L) }
  var errorText by remember(file.absolutePath) { mutableStateOf<String?>(null) }

  DisposableEffect(file.absolutePath) {
    onDispose {
      runCatching { player?.release() }
      player = null
    }
  }

  LaunchedEffect(isPlaying, player) {
    val activePlayer = player ?: return@LaunchedEffect
    while (isPlaying) {
      positionMs = activePlayer.currentPosition.toLong().coerceAtLeast(0L)
      val nextDuration = activePlayer.duration.toLong().takeIf { it > 0L }
      if (nextDuration != null) durationMs = nextDuration
      delay(250)
    }
  }

  ChatAttachmentCard(
    descriptor = descriptor,
    title = "Audio",
    subtitle =
      listOfNotNull(
        descriptor.mimeType,
        durationMs?.takeIf { it > 0L }?.let(::formatMediaDuration),
        errorText,
      ).joinToString(" · ").ifBlank { "Tap to play" },
    leadingIcon = {
      Icon(
        imageVector = Icons.Default.Audiotrack,
        contentDescription = null,
        tint = mobileAccent,
        modifier = Modifier.size(20.dp),
      )
    },
    action = {
      IconButton(
        enabled = !isPreparing,
        onClick = {
          if (isPlaying) {
            runCatching { player?.pause() }
            isPlaying = false
          } else {
            scope.launch {
              try {
                errorText = null
                isPreparing = true
                val preparedPlayer =
                  player ?: prepareAudioPlayer(
                    file = file,
                    onCompleted = {
                      isPlaying = false
                      positionMs = 0L
                    },
                    onFailed = { message ->
                      isPlaying = false
                      errorText = message
                    },
                  ).also { next ->
                  player = next
                  durationMs = next.duration.toLong().takeIf { it > 0L }
                  }
                val duration = preparedPlayer.duration.toLong().takeIf { it > 0L }
                if (duration != null && preparedPlayer.currentPosition.toLong() >= duration) {
                  preparedPlayer.seekTo(0)
                  positionMs = 0L
                }
                preparedPlayer.start()
                isPlaying = true
              } catch (err: Throwable) {
                errorText = err.message ?: "Playback failed"
                isPlaying = false
              } finally {
                isPreparing = false
              }
            }
          }
        },
      ) {
        when {
          isPreparing -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = mobileAccent)
          isPlaying ->
            Icon(
              imageVector = Icons.Default.Pause,
              contentDescription = "Pause audio",
              tint = mobileAccent,
            )

          else ->
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = "Play audio",
              tint = mobileAccent,
            )
        }
      }
    },
    footer = {
      val total = durationMs ?: 0L
      val progress =
        if (total > 0L) {
          (positionMs.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        } else {
          0f
        }
      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth(),
        color = mobileAccent,
        trackColor = mobileAccentSoft,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(formatMediaDuration(positionMs), style = mobileCaption2, color = mobileTextSecondary)
        Text(
          durationMs?.let(::formatMediaDuration) ?: "--:--",
          style = mobileCaption2,
          color = mobileTextSecondary,
        )
      }
    },
  )
}

@Composable
private fun ChatVideoAttachment(descriptor: ChatAttachmentDescriptor) {
  if (descriptor.base64 == null && descriptor.mediaUrl == null) {
    ChatUnsupportedAttachmentCard(
      descriptor = descriptor,
      title = "Video unavailable",
      detail = "Attachment content missing",
      iconTint = mobileTextSecondary,
    )
    return
  }

  val fileState = rememberResolvedMediaFileState(descriptor = descriptor)
  if (fileState.file == null) {
    if (fileState.loading) {
      ChatLoadingAttachmentCard(
        descriptor = descriptor,
        title = "Loading video",
        detail = descriptor.mimeType ?: "Fetching remote media",
      )
    } else {
      ChatUnsupportedAttachmentCard(
        descriptor = descriptor,
        title = "Video unavailable",
        detail = fileState.errorText ?: "Could not decode video attachment",
        iconTint = mobileTextSecondary,
      )
    }
    return
  }

  val file = fileState.file
  val previewState = rememberMediaPreviewState(file = file, includeFrame = true)
  var showPlayer by remember(file.absolutePath) { mutableStateOf(false) }

  ChatAttachmentCard(
    descriptor = descriptor,
    title = "Video",
    subtitle =
      listOfNotNull(
        descriptor.mimeType,
        previewState.durationMs?.takeIf { it > 0L }?.let(::formatMediaDuration),
      ).joinToString(" · ").ifBlank { "Tap to play" },
    leadingIcon = {
      Icon(
        imageVector = Icons.Default.Videocam,
        contentDescription = null,
        tint = mobileAccent,
        modifier = Modifier.size(20.dp),
      )
    },
    action = {
      IconButton(onClick = { showPlayer = true }) {
        Icon(
          imageVector = Icons.Default.PlayArrow,
          contentDescription = "Play video",
          tint = mobileAccent,
        )
      }
    },
    footer = {
      if (previewState.previewFrame != null) {
        Surface(
          shape = RoundedCornerShape(5.dp),
          border = BorderStroke(1.dp, mobileBorder),
          color = mobileCodeBg,
          modifier =
            Modifier
              .fillMaxWidth()
              .aspectRatio(16f / 9f)
              .clickable { showPlayer = true },
        ) {
          Box {
            Image(
              bitmap = previewState.previewFrame,
              contentDescription = descriptor.mimeType ?: "video preview",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxWidth(),
            )
            Box(
              modifier =
                Modifier
                  .matchParentSize()
                  .background(Color.Black.copy(alpha = 0.16f)),
            )
            Surface(
              color = Color.Black.copy(alpha = 0.55f),
              shape = RoundedCornerShape(999.dp),
              modifier = Modifier.align(Alignment.Center),
            ) {
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(10.dp),
              )
            }
          }
        }
      } else if (previewState.failed) {
        Text("Preview unavailable", style = mobileCaption1, color = mobileTextSecondary)
      }
    },
  )

  if (showPlayer) {
    Dialog(onDismissRequest = { showPlayer = false }) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = mobileSurface,
        border = BorderStroke(1.dp, mobileBorderStrong),
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = attachmentDisplayName(descriptor),
                style = mobileCallout.copy(fontWeight = FontWeight.SemiBold),
                color = mobileText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = descriptor.mimeType ?: "video",
                style = mobileCaption1,
                color = mobileTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            IconButton(onClick = { showPlayer = false }) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close video",
                tint = mobileTextSecondary,
              )
            }
          }

          AndroidView(
            modifier =
              Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black, RoundedCornerShape(8.dp)),
            factory = { context ->
              VideoView(context).apply {
                setVideoPath(file.absolutePath)
                setMediaController(MediaController(context).also { controller -> controller.setAnchorView(this) })
                setOnPreparedListener { mediaPlayer ->
                  mediaPlayer.isLooping = false
                  start()
                }
              }
            },
          )
        }
      }
    }
  }
}

@Composable
private fun ChatAttachmentCard(
  descriptor: ChatAttachmentDescriptor,
  title: String,
  subtitle: String,
  leadingIcon: @Composable () -> Unit,
  action: @Composable () -> Unit,
  footer: @Composable (() -> Unit)? = null,
) {
  Surface(
    shape = RoundedCornerShape(6.dp),
    border = BorderStroke(1.dp, mobileBorder),
    color = mobileSurface,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier
              .size(36.dp)
              .background(mobileAccentSoft, RoundedCornerShape(10.dp)),
          contentAlignment = Alignment.Center,
        ) {
          leadingIcon()
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = title,
            style = mobileCaption2.copy(fontWeight = FontWeight.SemiBold),
            color = mobileTextSecondary,
          )
          Text(
            text = attachmentDisplayName(descriptor),
            style = mobileCallout.copy(fontWeight = FontWeight.SemiBold),
            color = mobileText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = subtitle,
            style = mobileCaption1,
            color = mobileTextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
        action()
      }
      footer?.invoke()
    }
  }
}

@Composable
private fun ChatFileImage(file: File, mimeType: String?) {
  val imageState = rememberImageFileState(file)
  val image = imageState.image

  if (image != null) {
    Surface(
      shape = RoundedCornerShape(5.dp),
      border = BorderStroke(1.dp, mobileBorder),
      color = mobileSurface,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Image(
        bitmap = image,
        contentDescription = mimeType ?: "attachment",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  } else if (imageState.failed) {
    Text("Unsupported attachment", style = mobileCaption1, color = mobileTextSecondary)
  } else {
    ChatLoadingAttachmentCard(
      descriptor =
        ChatAttachmentDescriptor(
          kind = ChatAttachmentKind.Image,
          mimeType = mimeType,
          fileName = file.name,
          base64 = null,
          mediaUrl = null,
          mediaPath = null,
          mediaPort = null,
          mediaSha256 = null,
          sizeBytes = file.length(),
        ),
      title = "Loading image",
      detail = mimeType ?: "Decoding image",
    )
  }
}

@Composable
private fun ChatLoadingAttachmentCard(
  descriptor: ChatAttachmentDescriptor,
  title: String,
  detail: String,
) {
  ChatAttachmentCard(
    descriptor = descriptor,
    title = title,
    subtitle = detail,
    leadingIcon = {
      CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        strokeWidth = 2.dp,
        color = mobileAccent,
      )
    },
    action = {},
  )
}

@Composable
private fun ChatUnsupportedAttachmentCard(
  descriptor: ChatAttachmentDescriptor,
  title: String,
  detail: String,
  iconTint: Color,
) {
  ChatAttachmentCard(
    descriptor = descriptor,
    title = title,
    subtitle = detail,
    leadingIcon = {
      Icon(
        imageVector =
          when (descriptor.kind) {
            ChatAttachmentKind.Image -> Icons.Default.BrokenImage
            ChatAttachmentKind.Audio -> Icons.Default.Audiotrack
            ChatAttachmentKind.Video -> Icons.Default.Videocam
            ChatAttachmentKind.Unknown -> Icons.AutoMirrored.Filled.InsertDriveFile
          },
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(20.dp),
      )
    },
    action = {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
        contentDescription = null,
        tint = mobileTextSecondary,
      )
    },
  )
}

@Composable
internal fun rememberResolvedMediaFileState(
  descriptor: ChatAttachmentDescriptor,
): ResolvedMediaFileState {
  val context = LocalContext.current
  val prefs = remember(context.applicationContext) { SecurePrefs(context.applicationContext) }
  var state by remember(
    descriptor.base64,
    descriptor.mediaUrl,
    descriptor.mediaPath,
    descriptor.mediaPort,
    descriptor.mediaSha256,
    descriptor.mimeType,
    descriptor.fileName,
    descriptor.kind,
  ) {
    mutableStateOf(ResolvedMediaFileState(file = null, loading = true, failed = false))
  }

  LaunchedEffect(
    descriptor.base64,
    descriptor.mediaUrl,
    descriptor.mediaPath,
    descriptor.mediaPort,
    descriptor.mediaSha256,
    descriptor.mimeType,
    descriptor.fileName,
    descriptor.kind,
  ) {
    state = ResolvedMediaFileState(file = null, loading = true, failed = false)
    val resolved =
      withContext(Dispatchers.IO) {
        runCatching {
          resolveAttachmentFile(
            cacheDir = context.cacheDir,
            descriptor = descriptor,
            gatewayRemoteAddress = prefs.lastGatewayRemoteAddress.value,
            manualHost = prefs.manualHost.value,
            tailscaleHost = prefs.tailscaleHost.value,
          )
        }
      }
    state =
      resolved.fold(
        onSuccess = { file ->
          ResolvedMediaFileState(file = file, loading = false, failed = false, errorText = null)
        },
        onFailure = { err ->
          ResolvedMediaFileState(
            file = null,
            loading = false,
            failed = true,
            errorText =
              formatAttachmentFetchErrorMessage(
                mediaUrl = descriptor.mediaUrl,
                fallbackMessage = err.message,
              ),
          )
        },
      )
  }

  return state
}

@Composable
private fun rememberMediaPreviewState(file: File, includeFrame: Boolean): MediaPreviewState {
  var state by remember(file.absolutePath, includeFrame) {
    mutableStateOf(MediaPreviewState(durationMs = null, previewFrame = null, failed = false))
  }

  LaunchedEffect(file.absolutePath, includeFrame) {
    state =
      withContext(Dispatchers.IO) {
        runCatching {
          val retriever = MediaMetadataRetriever()
          try {
            retriever.setDataSource(file.absolutePath)
            val durationMs =
              retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            val preview =
              if (includeFrame) {
                retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.asImageBitmap()
              } else {
                null
              }
            MediaPreviewState(durationMs = durationMs, previewFrame = preview, failed = false)
          } finally {
            runCatching { retriever.release() }
          }
        }.getOrElse {
          MediaPreviewState(durationMs = null, previewFrame = null, failed = true)
        }
      }
  }

  return state
}

private suspend fun prepareAudioPlayer(
  file: File,
  onCompleted: () -> Unit,
  onFailed: (String) -> Unit,
): MediaPlayer {
  return withContext(Dispatchers.IO) {
    MediaPlayer().apply {
      setAudioAttributes(
        AudioAttributes.Builder()
          .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .build(),
      )
      setOnCompletionListener { onCompleted() }
      setOnErrorListener { _, what, extra ->
        onFailed("Playback error ($what/$extra)")
        true
      }
      setDataSource(file.absolutePath)
      prepare()
    }
  }
}

private fun resolveAttachmentFile(
  cacheDir: File,
  descriptor: ChatAttachmentDescriptor,
  gatewayRemoteAddress: String,
  manualHost: String,
  tailscaleHost: String,
): File {
  val mediaDir = File(cacheDir, "chat-media").apply { mkdirs() }
  val extension =
    resolveAttachmentExtension(
      kind = descriptor.kind,
      mimeType = descriptor.mimeType,
      fileName = descriptor.fileName,
    )

  descriptor.base64?.let { base64 ->
    val bytes = Base64.decode(base64, Base64.DEFAULT)
    require(bytes.isNotEmpty()) { "empty attachment" }
    val digest = descriptor.mediaSha256 ?: sha256Hex(bytes)
    val file = File(mediaDir, "$digest.$extension")
    if (!file.exists() || file.length() != bytes.size.toLong()) {
      file.writeBytes(bytes)
    }
    return file
  }

  val mediaUrl = descriptor.mediaUrl ?: descriptor.mediaPath ?: error("missing attachment source")
  val remoteKey = descriptor.mediaSha256 ?: sha256Hex(mediaUrl.toByteArray())
  val file = File(mediaDir, "$remoteKey.$extension")
  if (file.exists() && file.length() > 0L) return file

  val candidateUrls =
    resolveAttachmentFetchUrls(
      mediaUrl = mediaUrl,
      mediaPath = descriptor.mediaPath,
      mediaPort = descriptor.mediaPort,
      gatewayRemoteAddress = gatewayRemoteAddress,
      manualHost = manualHost,
      tailscaleHost = tailscaleHost,
    )
  var lastError: Throwable? = null

  for (candidateUrl in candidateUrls) {
    val request =
      Request.Builder()
        .get()
        .url(candidateUrl)
        .build()

    try {
      mediaHttpClient.newCall(request).execute().use { response ->
        require(response.isSuccessful) { "HTTP ${response.code}" }
        val body = response.body ?: error("empty response body")
        val bytes = body.bytes()
        require(bytes.isNotEmpty()) { "empty attachment" }
        descriptor.mediaSha256?.let { expected ->
          val actual = sha256Hex(bytes)
          require(actual.equals(expected, ignoreCase = true)) { "attachment checksum mismatch" }
        }
        val tempFile = File(mediaDir, "$remoteKey.$extension.part")
        tempFile.writeBytes(bytes)
        if (!tempFile.renameTo(file)) {
          tempFile.copyTo(file, overwrite = true)
          tempFile.delete()
        }
      }
      return file
    } catch (err: Throwable) {
      lastError = err
    }
  }

  throw (lastError ?: error("Attachment fetch failed"))
}

private fun resolveAttachmentExtension(kind: ChatAttachmentKind, mimeType: String?, fileName: String?): String {
  val nameExtension = fileName?.substringAfterLast('.', "")?.trim()?.lowercase(Locale.US).orEmpty()
  if (nameExtension.isNotEmpty()) return nameExtension

  return when (mimeType?.trim()?.lowercase(Locale.US)) {
    "image/jpeg" -> "jpg"
    "image/png" -> "png"
    "image/webp" -> "webp"
    "audio/mpeg" -> "mp3"
    "audio/mp4", "audio/aac" -> "m4a"
    "audio/wav", "audio/x-wav" -> "wav"
    "video/mp4" -> "mp4"
    else ->
      when (kind) {
        ChatAttachmentKind.Image -> "img"
        ChatAttachmentKind.Audio -> "audio"
        ChatAttachmentKind.Video -> "mp4"
        ChatAttachmentKind.Unknown -> "bin"
      }
  }
}

private fun sha256Hex(bytes: ByteArray): String {
  return MessageDigest.getInstance("SHA-256")
    .digest(bytes)
    .joinToString(separator = "") { each -> "%02x".format(each) }
}

private fun formatMediaDuration(durationMs: Long): String {
  val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
  val minutes = totalSeconds / 60L
  val seconds = totalSeconds % 60L
  return "%d:%02d".format(minutes, seconds)
}
