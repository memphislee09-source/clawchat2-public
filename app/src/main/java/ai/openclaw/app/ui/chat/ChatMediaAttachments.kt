package ai.openclaw.app.ui.chat

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
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
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.math.max
import org.json.JSONObject

internal data class ResolvedMediaFileState(
  val file: File?,
  val loading: Boolean,
  val failed: Boolean,
  val errorText: String? = null,
)

private data class MediaPreviewState(
  val durationMs: Long?,
  val previewFrame: ImageBitmap?,
  val videoWidth: Int?,
  val videoHeight: Int?,
  val rotationDegrees: Int?,
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
  val attachmentStateKey = stableAttachmentStateKey(descriptor)
  var showViewer by remember(attachmentStateKey) {
    mutableStateOf(false)
  }

  when {
    descriptor.base64 != null -> {
      ChatBase64Image(
        base64 = descriptor.base64,
        mimeType = descriptor.mimeType,
        onClick = { showViewer = true },
      )
    }

    descriptor.mediaUrl != null || descriptor.mediaPath != null -> {
      val fileState = rememberResolvedMediaFileState(descriptor = descriptor)
      when {
        fileState.file != null ->
          ChatFileImage(
            file = fileState.file,
            mimeType = descriptor.mimeType,
            onClick = { showViewer = true },
          )
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

  if (showViewer) {
    val fileState = if (descriptor.base64 == null) rememberResolvedMediaFileState(descriptor) else null
    FullscreenImageDialog(
      title = attachmentDisplayName(descriptor),
      mimeType = descriptor.mimeType,
      file = fileState?.file,
      base64 = descriptor.base64,
      onDismiss = { showViewer = false },
    )
  }
}

@Composable
private fun ChatAudioAttachment(descriptor: ChatAttachmentDescriptor) {
  if (descriptor.base64 == null && descriptor.mediaUrl == null && descriptor.mediaPath == null) {
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
  if (descriptor.base64 == null && descriptor.mediaUrl == null && descriptor.mediaPath == null) {
    ChatUnsupportedAttachmentCard(
      descriptor = descriptor,
      title = "Video unavailable",
      detail = "Attachment content missing",
      iconTint = mobileTextSecondary,
    )
    return
  }

  val attachmentStateKey = stableAttachmentStateKey(descriptor)
  val context = LocalContext.current
  val prefs = remember(context.applicationContext) { SecurePrefs(context.applicationContext) }
  val streamUrl =
    remember(
      descriptor.mediaUrl,
      descriptor.mediaPath,
      descriptor.mediaPort,
      prefs.lastGatewayRemoteAddress.value,
      prefs.manualHost.value,
      prefs.tailscaleHost.value,
    ) {
      resolveAttachmentFetchUrls(
        mediaUrl = descriptor.mediaUrl,
        mediaPath = descriptor.mediaPath,
        mediaPort = descriptor.mediaPort,
        gatewayRemoteAddress = prefs.lastGatewayRemoteAddress.value,
        manualHost = prefs.manualHost.value,
        tailscaleHost = prefs.tailscaleHost.value,
      ).firstOrNull()
    }
  val fallbackFileState =
    if (streamUrl == null) {
      rememberResolvedMediaFileState(descriptor = descriptor)
    } else {
      null
    }
  val fallbackFile = fallbackFileState?.file
  val previewState = fallbackFile?.let { rememberMediaPreviewState(file = it, includeFrame = true) }
  val playbackSource = streamUrl ?: fallbackFile?.absolutePath
  val preferredOrientation = preferredVideoFullscreenOrientation(previewState)
  val displayAspectRatio = previewState?.let(::mediaDisplayAspectRatio)
  var showPlayer by
    remember(attachmentStateKey) {
      mutableStateOf(false)
    }
  var activePlaybackSource by
    remember(attachmentStateKey) {
      mutableStateOf<String?>(null)
    }
  var activePreferredOrientation by
    remember(attachmentStateKey) {
      mutableStateOf<Int?>(null)
    }
  var activeDisplayAspectRatio by
    remember(attachmentStateKey) {
      mutableStateOf<Float?>(null)
    }

  val openFullscreenVideo =
    remember(playbackSource, preferredOrientation, displayAspectRatio) {
      {
        if (playbackSource != null) {
          activePlaybackSource = playbackSource
          activePreferredOrientation = preferredOrientation
          activeDisplayAspectRatio = displayAspectRatio
          showPlayer = true
        }
      }
    }

  ChatAttachmentCard(
    descriptor = descriptor,
    title = "Video",
    subtitle =
      listOfNotNull(
        descriptor.mimeType,
        previewState?.durationMs?.takeIf { it > 0L }?.let(::formatMediaDuration),
        when {
          streamUrl != null -> "Tap to stream"
          fallbackFileState?.loading == true -> "Preparing video"
          else -> null
        },
      ).joinToString(" · ").ifBlank { if (playbackSource != null) "Tap to play" else "Video unavailable" },
    leadingIcon = {
      Icon(
        imageVector = Icons.Default.Videocam,
        contentDescription = null,
        tint = mobileAccent,
        modifier = Modifier.size(20.dp),
      )
    },
    action = {
      IconButton(onClick = openFullscreenVideo) {
        Icon(
          imageVector = Icons.Default.PlayArrow,
          contentDescription = "Play video",
          tint = mobileAccent,
        )
      }
    },
    footer = {
      if (previewState?.previewFrame != null) {
        VideoPreviewTile(
          preview = previewState.previewFrame,
          mimeType = descriptor.mimeType,
          onClick = openFullscreenVideo,
        )
      } else if (streamUrl != null) {
        VideoPlaceholderTile(
          message = "Tap to stream full screen",
          onClick = openFullscreenVideo,
        )
      } else if (previewState?.failed == true) {
        Text("Preview unavailable", style = mobileCaption1, color = mobileTextSecondary)
      } else if (fallbackFileState?.loading == true) {
        Text("Preparing preview", style = mobileCaption1, color = mobileTextSecondary)
      } else {
        Text(
          fallbackFileState?.errorText ?: "Could not prepare video attachment",
          style = mobileCaption1,
          color = mobileTextSecondary,
        )
      }
    },
  )

  if (showPlayer) {
    FullscreenVideoDialog(
      playbackSource = activePlaybackSource,
      preferredOrientation = activePreferredOrientation,
      displayAspectRatio = activeDisplayAspectRatio,
      isPreparing = activePlaybackSource == null && fallbackFileState?.loading == true,
      statusText =
        when {
          activePlaybackSource == null && fallbackFileState?.loading == true -> "Preparing video..."
          activePlaybackSource == null && fallbackFileState?.failed == true ->
            fallbackFileState.errorText ?: "Could not prepare video attachment"
          activePlaybackSource == null -> "Video unavailable"
          else -> null
        },
      onDismiss = {
        showPlayer = false
        activePlaybackSource = null
        activePreferredOrientation = null
        activeDisplayAspectRatio = null
      },
    )
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
private fun ChatFileImage(file: File, mimeType: String?, onClick: () -> Unit) {
  val decodeRequest = rememberViewportImageDecodeRequest(preferLowMemory = true)
  val imageState = rememberImageFileState(file, decodeRequest = decodeRequest)
  val image = imageState.image

  if (image != null) {
    Surface(
      shape = RoundedCornerShape(5.dp),
      color = Color.Transparent,
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
          sizeBytes = null,
        ),
      title = "Loading image",
      detail = mimeType ?: "Decoding image",
    )
  }
}

@Composable
private fun VideoPreviewTile(
  preview: ImageBitmap,
  mimeType: String?,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(5.dp),
    border = BorderStroke(1.dp, mobileBorder),
    color = mobileCodeBg,
    modifier =
      Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f)
        .clickable(onClick = onClick),
  ) {
    Box {
      Image(
        bitmap = preview,
        contentDescription = mimeType ?: "video preview",
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
}

@Composable
private fun VideoPlaceholderTile(message: String, onClick: () -> Unit) {
  Surface(
    shape = RoundedCornerShape(5.dp),
    border = BorderStroke(1.dp, mobileBorder),
    color = Color.Black,
    modifier =
      Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f)
        .clickable(onClick = onClick),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Videocam,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(28.dp),
        )
        Text(
          text = message,
          style = mobileCaption1,
          color = Color.White,
        )
      }
    }
  }
}

@Composable
private fun FullscreenImageDialog(
  title: String,
  mimeType: String?,
  file: File?,
  base64: String?,
  onDismiss: () -> Unit,
) {
  val decodeRequest = rememberViewportImageDecodeRequest(preferLowMemory = false)
  val fileImageState =
    if (file != null) rememberImageFileState(file, decodeRequest = decodeRequest) else null
  val base64ImageState =
    if (base64 != null) rememberBase64ImageState(base64, decodeRequest = decodeRequest) else null
  val image = fileImageState?.image ?: base64ImageState?.image
  val failed = fileImageState?.failed == true || base64ImageState?.failed == true

  Dialog(
    onDismissRequest = onDismiss,
    properties =
      DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false,
        dismissOnClickOutside = false,
      ),
  ) {
    PrepareFullscreenDialogWindow()
    Surface(
      color = Color.Black,
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        if (image != null) {
          Image(
            bitmap = image,
            contentDescription = mimeType ?: "image",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
          )
        } else if (failed) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Image unavailable", style = mobileCallout, color = Color.White)
          }
        } else {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = title,
              style = mobileCallout.copy(fontWeight = FontWeight.SemiBold),
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = mimeType ?: "image",
              style = mobileCaption1,
              color = Color.White.copy(alpha = 0.72f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close image",
              tint = Color.White,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FullscreenVideoDialog(
  playbackSource: String?,
  preferredOrientation: Int?,
  displayAspectRatio: Float?,
  isPreparing: Boolean,
  statusText: String?,
  onDismiss: () -> Unit,
) {
  ApplyRequestedOrientation(preferredOrientation = preferredOrientation)
  val context = LocalContext.current
  var playerStatusText by remember(playbackSource) { mutableStateOf<String?>(null) }
  var playerReady by remember(playbackSource) { mutableStateOf(false) }
  var playerBuffering by remember(playbackSource) { mutableStateOf(false) }
  val player =
    remember(playbackSource, context.applicationContext) {
      playbackSource?.let { source ->
        ExoPlayer.Builder(context.applicationContext)
          .build()
          .apply {
            setMediaItem(MediaItem.fromUri(resolvePlaybackUri(source)))
            playWhenReady = true
            prepare()
          }
      }
    }

  DisposableEffect(player) {
    if (player == null) {
      onDispose {}
    } else {
      val listener =
        object : Player.Listener {
          override fun onPlaybackStateChanged(playbackState: Int) {
            playerBuffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
            if (playbackState == Player.STATE_READY) {
              playerReady = true
              playerStatusText = null
            } else if (playbackState == Player.STATE_ENDED) {
              playerBuffering = false
              playerStatusText = null
            }
          }

          override fun onPlayerError(error: PlaybackException) {
            playerBuffering = false
            playerStatusText = error.localizedMessage ?: "Playback error"
          }
        }
      player.addListener(listener)
      onDispose {
        player.removeListener(listener)
        player.release()
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    PrepareFullscreenDialogWindow()
    Surface(
      color = Color.Black,
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        Box(
          modifier =
            Modifier
              .fillMaxSize()
              .background(Color.Black),
        )

        if (player != null) {
          BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            val containerAspectRatio =
              if (maxHeight.value > 0f) {
                maxWidth.value / maxHeight.value
              } else {
                displayAspectRatio ?: (16f / 9f)
              }
            val playerModifier =
              when {
                displayAspectRatio == null ->
                  Modifier
                    .fillMaxSize()
                    .background(Color.Black)

                displayAspectRatio >= containerAspectRatio ->
                  Modifier
                    .fillMaxWidth()
                    .aspectRatio(displayAspectRatio)
                    .background(Color.Black)

                else ->
                  Modifier
                    .fillMaxHeight()
                    .aspectRatio(displayAspectRatio)
                    .background(Color.Black)
              }

            AndroidView(
              modifier = playerModifier,
              factory = { context ->
                PlayerView(context).apply {
                  useController = true
                  controllerAutoShow = true
                  controllerHideOnTouch = true
                  resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                  setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                  setBackgroundColor(android.graphics.Color.BLACK)
                }
              },
              update = { playerView ->
                if (playerView.player !== player) {
                  playerView.player = player
                }
              },
            )
          }
        } else {
          val showInlineProgress = isPreparing
          Column(
            modifier =
              Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            if (showInlineProgress) {
              CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
            }
            val resolvedStatusText = playerStatusText ?: statusText
            resolvedStatusText?.let { message ->
              Text(
                text = message,
                color = Color.White,
                style = mobileCallout,
                modifier =
                  Modifier.padding(
                    top = if (showInlineProgress) 16.dp else 0.dp,
                  ),
              )
            }
          }
        }

        if (player != null && playerBuffering && !playerReady && playerStatusText == null) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
          }
        }

        playerStatusText?.let { message ->
          Box(
            modifier =
              Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(text = message, color = Color.White, style = mobileCallout)
          }
        }

        Box(
          modifier =
            Modifier
              .align(Alignment.TopEnd)
              .padding(12.dp),
        ) {
          Surface(
            color = Color.Black.copy(alpha = 0.52f),
            shape = RoundedCornerShape(999.dp),
          ) {
            IconButton(onClick = onDismiss) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close video",
                tint = Color.White,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PrepareFullscreenDialogWindow() {
  val view = LocalView.current
  val dialogWindow = (view.parent as? DialogWindowProvider)?.window ?: return

  DisposableEffect(dialogWindow) {
    dialogWindow.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    dialogWindow.setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK))
    dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    dialogWindow.setDimAmount(1f)
    dialogWindow.decorView.setBackgroundColor(android.graphics.Color.BLACK)
    dialogWindow.findViewById<View>(android.R.id.content)?.setBackgroundColor(android.graphics.Color.BLACK)
    WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
    val insetsController = WindowInsetsControllerCompat(dialogWindow, dialogWindow.decorView)
    insetsController.hide(WindowInsetsCompat.Type.systemBars())
    insetsController.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

    onDispose {
      insetsController.show(WindowInsetsCompat.Type.systemBars())
    }
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
  val attachmentStateKey = stableAttachmentStateKey(descriptor)
  var state by remember(
    attachmentStateKey,
  ) {
    mutableStateOf(ResolvedMediaFileState(file = null, loading = true, failed = false))
  }

  LaunchedEffect(
    attachmentStateKey,
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
    mutableStateOf(
      MediaPreviewState(
        durationMs = null,
        previewFrame = null,
        videoWidth = null,
        videoHeight = null,
        rotationDegrees = null,
        failed = false,
      ),
    )
  }

  LaunchedEffect(file.absolutePath, includeFrame) {
    state =
      withContext(Dispatchers.IO) {
        loadMediaPreviewState(source = file.absolutePath, includeFrame = includeFrame)
      }
  }

  return state
}

@Composable
private fun ApplyRequestedOrientation(preferredOrientation: Int?) {
  val context = LocalContext.current
  val activity = remember(context) { context.findActivity() }

  DisposableEffect(activity, preferredOrientation) {
    if (activity == null || preferredOrientation == null) {
      onDispose {}
    } else {
      val previousOrientation = activity.requestedOrientation
      activity.requestedOrientation = preferredOrientation
      onDispose {
        activity.requestedOrientation = previousOrientation
      }
    }
  }
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
  val remoteKey = descriptor.mediaSha256 ?: sha256Hex(stableAttachmentStateKey(descriptor).toByteArray())
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
        val tempFile = File(mediaDir, "$remoteKey.$extension.part")
        streamResponseBodyToFile(body = body, destination = tempFile)
        require(tempFile.length() > 0L) { "empty attachment" }
        descriptor.mediaSha256?.let { expected ->
          val actual = sha256Hex(tempFile)
          require(actual.equals(expected, ignoreCase = true)) { "attachment checksum mismatch" }
        }
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

private fun sha256Hex(file: File): String {
  val digest = MessageDigest.getInstance("SHA-256")
  file.inputStream().buffered().use { input ->
    val buffer = ByteArray(DEFAULT_MEDIA_BUFFER_SIZE)
    while (true) {
      val read = input.read(buffer)
      if (read <= 0) break
      digest.update(buffer, 0, read)
    }
  }
  return digest.digest().joinToString(separator = "") { each -> "%02x".format(each) }
}

private fun streamResponseBodyToFile(body: okhttp3.ResponseBody, destination: File) {
  destination.parentFile?.mkdirs()
  body.byteStream().use { input ->
    FileOutputStream(destination).use { output ->
      val buffer = ByteArray(DEFAULT_MEDIA_BUFFER_SIZE)
      while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        if (read == 0) continue
        output.write(buffer, 0, read)
      }
      output.flush()
    }
  }
}

private fun loadMediaPreviewState(source: String, includeFrame: Boolean): MediaPreviewState {
  return runCatching {
    val retriever = MediaMetadataRetriever()
    try {
      if (source.startsWith("http://") || source.startsWith("https://")) {
        retriever.setDataSource(source, emptyMap())
      } else {
        retriever.setDataSource(source)
      }
      val durationMs =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
      val videoWidth =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
      val videoHeight =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
      val rotationDegrees =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull()
      val preview =
        if (includeFrame) {
          loadScaledVideoPreviewFrame(
            retriever = retriever,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            rotationDegrees = rotationDegrees,
          )?.asImageBitmap()
        } else {
          null
        }
      MediaPreviewState(
        durationMs = durationMs,
        previewFrame = preview,
        videoWidth = videoWidth,
        videoHeight = videoHeight,
        rotationDegrees = rotationDegrees,
        failed = false,
      )
    } finally {
      runCatching { retriever.release() }
    }
  }.getOrElse {
    MediaPreviewState(
      durationMs = null,
      previewFrame = null,
      videoWidth = null,
      videoHeight = null,
      rotationDegrees = null,
      failed = true,
    )
  }
}

private fun loadScaledVideoPreviewFrame(
  retriever: MediaMetadataRetriever,
  videoWidth: Int?,
  videoHeight: Int?,
  rotationDegrees: Int?,
): android.graphics.Bitmap? {
  val sourceWidth = videoWidth?.takeIf { it > 0 } ?: return retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
  val sourceHeight = videoHeight?.takeIf { it > 0 } ?: return retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
  val rotation = ((rotationDegrees ?: 0) % 360 + 360) % 360
  val displayWidth = if (rotation == 90 || rotation == 270) sourceHeight else sourceWidth
  val displayHeight = if (rotation == 90 || rotation == 270) sourceWidth else sourceHeight
  val maxEdge = 960
  val scale =
    max(
      1f,
      max(
        displayWidth / maxEdge.toFloat(),
        displayHeight / maxEdge.toFloat(),
      ),
    )
  val targetDisplayWidth = (displayWidth / scale).toInt().coerceAtLeast(1)
  val targetDisplayHeight = (displayHeight / scale).toInt().coerceAtLeast(1)
  val targetWidth = if (rotation == 90 || rotation == 270) targetDisplayHeight else targetDisplayWidth
  val targetHeight = if (rotation == 90 || rotation == 270) targetDisplayWidth else targetDisplayHeight

  return runCatching {
    retriever.getScaledFrameAtTime(
      0L,
      MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
      targetWidth,
      targetHeight,
    )
  }.getOrElse {
    retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
  }
}

private const val DEFAULT_MEDIA_BUFFER_SIZE = 64 * 1024

private fun stableAttachmentStateKey(descriptor: ChatAttachmentDescriptor): String {
  descriptor.mediaSha256?.trim()?.takeIf { it.isNotEmpty() }?.let { return "sha256:$it" }
  descriptor.base64?.trim()?.takeIf { it.isNotEmpty() }?.let { return "base64:${sha256Hex(it.toByteArray())}" }
  descriptor.mediaPath?.trim()?.takeIf { it.isNotEmpty() }?.let { return "path:$it" }
  descriptor.mediaUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { mediaUrl ->
    decodeWebChatMediaTokenPath(mediaUrl)?.let { resolvedPath ->
      return "webchat:$resolvedPath"
    }
    return "url:$mediaUrl"
  }
  return buildString {
    append(descriptor.kind.name)
    append('|')
    append(descriptor.fileName.orEmpty())
    append('|')
    append(descriptor.mimeType.orEmpty())
    append('|')
    append(descriptor.sizeBytes ?: -1L)
  }
}

private fun decodeWebChatMediaTokenPath(mediaUrl: String): String? {
  val token =
    runCatching { Uri.parse(mediaUrl).getQueryParameter("token") }.getOrNull()
      ?.trim()
      ?.takeIf { it.isNotEmpty() }
      ?: return null
  return runCatching {
    val decoded =
      String(
        Base64.decode(token, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
        Charsets.UTF_8,
      )
    JSONObject(decoded)
      .optJSONObject("payload")
      ?.optString("path")
      ?.trim()
      ?.takeIf { it.isNotEmpty() }
  }.getOrNull()
}

private fun resolvePlaybackUri(playbackSource: String): Uri {
  return when {
    playbackSource.startsWith("http://") ||
      playbackSource.startsWith("https://") ||
      playbackSource.startsWith("content://") ||
      playbackSource.startsWith("file://") -> Uri.parse(playbackSource)

    else -> Uri.fromFile(File(playbackSource))
  }
}

private fun mediaDisplayAspectRatio(state: MediaPreviewState): Float? {
  val (displayWidth, displayHeight) = mediaDisplaySize(state) ?: return null
  if (displayWidth <= 0 || displayHeight <= 0) return null
  return displayWidth.toFloat() / displayHeight.toFloat()
}

private fun preferredVideoFullscreenOrientation(state: MediaPreviewState?): Int? {
  val (displayWidth, displayHeight) = mediaDisplaySize(state) ?: return null
  return if (displayWidth > displayHeight) {
    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
  } else {
    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
  }
}

private fun mediaDisplaySize(state: MediaPreviewState?): Pair<Int, Int>? {
  val videoWidth = state?.videoWidth ?: return null
  val videoHeight = state.videoHeight ?: return null
  if (videoWidth <= 0 || videoHeight <= 0) return null
  val rotation = ((state.rotationDegrees ?: 0) % 360 + 360) % 360
  return if (rotation == 90 || rotation == 270) {
    videoHeight to videoWidth
  } else {
    videoWidth to videoHeight
  }
}

private tailrec fun Context.findActivity(): Activity? {
  return when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }
}

private fun formatMediaDuration(durationMs: Long): String {
  val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
  val minutes = totalSeconds / 60L
  val seconds = totalSeconds % 60L
  return "%d:%02d".format(minutes, seconds)
}
