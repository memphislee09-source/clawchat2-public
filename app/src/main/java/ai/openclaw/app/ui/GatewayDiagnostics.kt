package ai.openclaw.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import ai.openclaw.app.BuildConfig
import java.net.URI
import java.util.Locale

internal fun openClawAndroidVersionLabel(): String {
  val versionName = BuildConfig.VERSION_NAME.trim().ifEmpty { "dev" }
  return if (BuildConfig.DEBUG && !versionName.contains("dev", ignoreCase = true)) {
    "$versionName-dev"
  } else {
    versionName
  }
}

internal fun gatewayStatusForDisplay(statusText: String): String {
  return statusText.trim().ifEmpty { "Offline" }
}

internal fun gatewayStatusHasDiagnostics(statusText: String): Boolean {
  val lower = gatewayStatusForDisplay(statusText).lowercase()
  return lower != "offline" && !lower.contains("connecting")
}

internal fun gatewayStatusLooksLikePairing(statusText: String): Boolean {
  val lower = gatewayStatusForDisplay(statusText).lowercase()
  return lower.contains("pair") || lower.contains("approve")
}

internal fun openClawAndroidBuildIdentity(): String {
  val flavor = BuildConfig.FLAVOR.trim().ifEmpty { "main" }
  return buildString {
    append(openClawAndroidVersionLabel())
    append(" (versionCode ")
    append(BuildConfig.VERSION_CODE)
    append(", ")
    append(flavor)
    append("/")
    append(BuildConfig.BUILD_TYPE)
    if (BuildConfig.DEBUG) {
      append(", debug")
    }
    append(")")
  }
}

internal fun classifyGatewayRoute(gatewayAddress: String): String {
  val endpoint = parseGatewayEndpointForDiagnostics(gatewayAddress) ?: return "unknown"
  val host = endpoint.host.trim().lowercase(Locale.US)
  return when {
    host.endsWith(".ts.net") -> "Tailscale"
    host == "localhost" || host == "127.0.0.1" || host == "::1" || host == "10.0.2.2" -> "same machine"
    host.startsWith("10.") ||
      host.startsWith("192.168.") ||
      host.startsWith("172.16.") ||
      host.startsWith("172.17.") ||
      host.startsWith("172.18.") ||
      host.startsWith("172.19.") ||
      host.startsWith("172.2") ||
      host.startsWith("172.30.") ||
      host.startsWith("172.31.") ||
      host.startsWith("fd") ||
      host.startsWith("fc") -> "same LAN"
    else -> "public URL"
  }
}

internal fun classifyGatewayFailure(statusText: String): String {
  val lower = gatewayStatusForDisplay(statusText).lowercase(Locale.US)
  return when {
    lower.contains("fingerprint") || lower.contains("certificate") || lower.contains("tls") -> "TLS trust"
    lower.contains("pair") || lower.contains("approve") || lower.contains("auth") || lower.contains("unauthorized") -> "pairing/auth"
    lower.contains("refused") || lower.contains("unreachable") || lower.contains("timeout") || lower.contains("dns") -> "wrong address/port or gateway down"
    lower.contains("offline") -> "gateway down"
    else -> "needs classification"
  }
}

internal fun buildGatewayDiagnosticsReport(
  screen: String,
  gatewayAddress: String,
  statusText: String,
): String {
  val device =
    listOfNotNull(Build.MANUFACTURER, Build.MODEL)
      .joinToString(" ")
      .trim()
      .ifEmpty { "Android" }
  val deviceBuild =
    listOfNotNull(Build.BRAND, Build.DEVICE, Build.PRODUCT)
      .joinToString(" / ")
      .trim()
      .ifEmpty { "unknown" }
  val androidVersion = Build.VERSION.RELEASE?.trim().orEmpty().ifEmpty { Build.VERSION.SDK_INT.toString() }
  val endpoint = gatewayAddress.trim().ifEmpty { "unknown" }
  val parsedEndpoint = parseGatewayEndpointForDiagnostics(endpoint)
  val status = gatewayStatusForDisplay(statusText)
  val route = classifyGatewayRoute(endpoint)
  val failureClass = classifyGatewayFailure(status)
  return """
    Help diagnose this OpenClaw Android gateway connection failure.

    Please:
    - pick one route only: same machine, same LAN, Tailscale, or public URL
    - classify this as pairing/auth, TLS trust, wrong advertised route, wrong address/port, or gateway down
    - quote the exact app status/error below
    - tell me whether `openclaw devices list` should show a pending pairing request
    - if more signal is needed, ask for `openclaw qr --json`, `openclaw devices list`, and `openclaw nodes status`
    - give the next exact command or tap

    Debug info:
    - screen: $screen
    - app build: ${openClawAndroidBuildIdentity()}
    - app package: ${BuildConfig.APPLICATION_ID}
    - device: $device
    - device build: $deviceBuild
    - android: $androidVersion (SDK ${Build.VERSION.SDK_INT})
    - route guess: $route
    - gateway address: $endpoint
    - gateway host: ${parsedEndpoint?.host ?: "unknown"}
    - gateway port: ${parsedEndpoint?.port?.toString() ?: "unknown"}
    - gateway tls: ${parsedEndpoint?.tls?.toString() ?: "unknown"}
    - likely failure class: $failureClass
    - status/error: $status
  """.trimIndent()
}

private data class GatewayEndpointDiagnostics(
  val host: String,
  val port: Int,
  val tls: Boolean,
)

private fun parseGatewayEndpointForDiagnostics(rawInput: String): GatewayEndpointDiagnostics? {
  val raw = rawInput.trim()
  if (raw.isEmpty()) return null

  val normalized = if (raw.contains("://")) raw else "https://$raw"
  val uri = runCatching { URI(normalized) }.getOrNull() ?: return null
  val host = uri.host?.trim().orEmpty()
  if (host.isEmpty()) return null

  val scheme = uri.scheme?.trim()?.lowercase(Locale.US).orEmpty()
  val tls =
    when (scheme) {
      "ws", "http" -> false
      "wss", "https" -> true
      else -> true
    }
  val port = uri.port.takeIf { it in 1..65535 } ?: if (tls) 443 else 18789
  return GatewayEndpointDiagnostics(host = host, port = port, tls = tls)
}

internal fun copyGatewayDiagnosticsReport(
  context: Context,
  screen: String,
  gatewayAddress: String,
  statusText: String,
) {
  val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
  val report = buildGatewayDiagnosticsReport(screen = screen, gatewayAddress = gatewayAddress, statusText = statusText)
  clipboard.setPrimaryClip(ClipData.newPlainText("OpenClaw gateway diagnostics", report))
  Toast.makeText(context, "Copied gateway diagnostics", Toast.LENGTH_SHORT).show()
}
