package ai.openclaw.app.ui.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.io.File
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

internal data class Base64ImageState(
  val image: ImageBitmap?,
  val failed: Boolean,
)

internal data class ImageDecodeRequest(
  val maxWidthPx: Int? = null,
  val maxHeightPx: Int? = null,
  val preferLowMemory: Boolean = false,
)

private data class DecodeTargetSize(
  val widthPx: Int,
  val heightPx: Int,
)

@Composable
internal fun rememberViewportImageDecodeRequest(preferLowMemory: Boolean): ImageDecodeRequest {
  val configuration = LocalConfiguration.current
  val density = LocalDensity.current
  val widthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }.coerceAtLeast(1)
  val heightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }.coerceAtLeast(1)

  return remember(widthPx, heightPx, preferLowMemory) {
    ImageDecodeRequest(
      maxWidthPx = widthPx,
      maxHeightPx = heightPx,
      preferLowMemory = preferLowMemory,
    )
  }
}

@Composable
internal fun rememberBase64ImageState(
  base64: String,
  decodeRequest: ImageDecodeRequest = ImageDecodeRequest(),
): Base64ImageState {
  var image by remember(base64, decodeRequest) { mutableStateOf<ImageBitmap?>(null) }
  var failed by remember(base64, decodeRequest) { mutableStateOf(false) }

  LaunchedEffect(base64, decodeRequest) {
    failed = false
    image =
      withContext(Dispatchers.Default) {
        try {
          val bytes = Base64.decode(base64, Base64.DEFAULT)
          val bitmap = decodeImageBitmap(bytes, decodeRequest) ?: return@withContext null
          bitmap.asImageBitmap()
        } catch (_: Throwable) {
          null
        }
      }
    if (image == null) failed = true
  }

  return Base64ImageState(image = image, failed = failed)
}

@Composable
internal fun rememberImageFileState(
  file: File,
  decodeRequest: ImageDecodeRequest = ImageDecodeRequest(),
): Base64ImageState {
  var image by remember(file.absolutePath, decodeRequest) { mutableStateOf<ImageBitmap?>(null) }
  var failed by remember(file.absolutePath, decodeRequest) { mutableStateOf(false) }

  LaunchedEffect(file.absolutePath, decodeRequest) {
    failed = false
    image =
      withContext(Dispatchers.IO) {
        runCatching {
          decodeImageFile(file, decodeRequest)?.asImageBitmap()
        }.getOrNull()
      }
    if (image == null) failed = true
  }

  return Base64ImageState(image = image, failed = failed)
}

internal fun decodeImageBitmap(
  bytes: ByteArray,
  decodeRequest: ImageDecodeRequest = ImageDecodeRequest(),
): Bitmap? {
  decodeBitmapFactoryBytes(bytes, decodeRequest)?.let { return it }
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null

  return runCatching {
    val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
      configureImageDecoder(
        decoder = decoder,
        sourceWidth = info.size.width,
        sourceHeight = info.size.height,
        decodeRequest = decodeRequest,
      )
    }
  }.getOrNull()
}

internal fun decodeImageFile(
  file: File,
  decodeRequest: ImageDecodeRequest = ImageDecodeRequest(),
): Bitmap? {
  decodeBitmapFactoryFile(file, decodeRequest)?.let { return it }
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null

  return runCatching {
    val source = ImageDecoder.createSource(file)
    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
      configureImageDecoder(
        decoder = decoder,
        sourceWidth = info.size.width,
        sourceHeight = info.size.height,
        decodeRequest = decodeRequest,
      )
    }
  }.getOrNull()
}

internal fun computeInSampleSize(
  sourceWidth: Int,
  sourceHeight: Int,
  maxWidthPx: Int?,
  maxHeightPx: Int?,
): Int {
  val boundedWidth = maxWidthPx?.coerceAtLeast(1) ?: sourceWidth
  val boundedHeight = maxHeightPx?.coerceAtLeast(1) ?: sourceHeight
  var inSampleSize = 1

  if (sourceWidth <= boundedWidth && sourceHeight <= boundedHeight) return inSampleSize

  var nextWidth = sourceWidth / 2
  var nextHeight = sourceHeight / 2
  while (nextWidth / inSampleSize >= boundedWidth && nextHeight / inSampleSize >= boundedHeight) {
    inSampleSize *= 2
  }
  return inSampleSize.coerceAtLeast(1)
}

private fun decodeBitmapFactoryBytes(
  bytes: ByteArray,
  decodeRequest: ImageDecodeRequest,
): Bitmap? {
  val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
  if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

  return BitmapFactory.decodeByteArray(
    bytes,
    0,
    bytes.size,
    bitmapFactoryOptions(bounds.outWidth, bounds.outHeight, decodeRequest),
  )
}

private fun decodeBitmapFactoryFile(
  file: File,
  decodeRequest: ImageDecodeRequest,
): Bitmap? {
  if (!file.exists()) return null
  val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  BitmapFactory.decodeFile(file.absolutePath, bounds)
  if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

  return BitmapFactory.decodeFile(
    file.absolutePath,
    bitmapFactoryOptions(bounds.outWidth, bounds.outHeight, decodeRequest),
  )
}

private fun bitmapFactoryOptions(
  sourceWidth: Int,
  sourceHeight: Int,
  decodeRequest: ImageDecodeRequest,
): BitmapFactory.Options {
  return BitmapFactory.Options().apply {
    inSampleSize =
      computeInSampleSize(
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight,
        maxWidthPx = decodeRequest.maxWidthPx,
        maxHeightPx = decodeRequest.maxHeightPx,
      )
    inPreferredConfig =
      if (decodeRequest.preferLowMemory) {
        Bitmap.Config.RGB_565
      } else {
        Bitmap.Config.ARGB_8888
      }
  }
}

private fun configureImageDecoder(
  decoder: ImageDecoder,
  sourceWidth: Int,
  sourceHeight: Int,
  decodeRequest: ImageDecodeRequest,
) {
  resolveDecodeTargetSize(
    sourceWidth = sourceWidth,
    sourceHeight = sourceHeight,
    maxWidthPx = decodeRequest.maxWidthPx,
    maxHeightPx = decodeRequest.maxHeightPx,
  )?.let { target ->
    if (target.widthPx < sourceWidth || target.heightPx < sourceHeight) {
      decoder.setTargetSize(target.widthPx, target.heightPx)
    }
  }
  decoder.isMutableRequired = false
  decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
  if (decodeRequest.preferLowMemory) {
    decoder.memorySizePolicy = ImageDecoder.MEMORY_POLICY_LOW_RAM
  }
}

private fun resolveDecodeTargetSize(
  sourceWidth: Int,
  sourceHeight: Int,
  maxWidthPx: Int?,
  maxHeightPx: Int?,
): DecodeTargetSize? {
  if (sourceWidth <= 0 || sourceHeight <= 0) return null
  val boundedWidth = maxWidthPx?.coerceAtLeast(1) ?: sourceWidth
  val boundedHeight = maxHeightPx?.coerceAtLeast(1) ?: sourceHeight
  val scale =
    max(
      1f,
      max(
        sourceWidth.toFloat() / boundedWidth.toFloat(),
        sourceHeight.toFloat() / boundedHeight.toFloat(),
      ),
    )

  return DecodeTargetSize(
    widthPx = (sourceWidth / scale).roundToInt().coerceAtLeast(1),
    heightPx = (sourceHeight / scale).roundToInt().coerceAtLeast(1),
  )
}
