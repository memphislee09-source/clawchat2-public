package ai.openclaw.app.gateway

import android.annotation.SuppressLint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

data class GatewayTlsParams(
  val required: Boolean,
  val expectedFingerprint: String?,
  val allowTOFU: Boolean,
  val stableId: String,
)

data class GatewayTlsConfig(
  val sslSocketFactory: SSLSocketFactory,
  val trustManager: X509TrustManager,
  val hostnameVerifier: HostnameVerifier,
)

fun buildGatewayTlsConfig(
  params: GatewayTlsParams?,
  onStore: ((String) -> Unit)? = null,
): GatewayTlsConfig? {
  if (params == null) return null
  val expected = params.expectedFingerprint?.let(::normalizeFingerprint)
  val defaultTrust = defaultTrustManager()
  @SuppressLint("CustomX509TrustManager")
  val trustManager =
    object : X509TrustManager {
      override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        defaultTrust.checkClientTrusted(chain, authType)
      }

      override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        if (chain.isEmpty()) throw CertificateException("empty certificate chain")
        val fingerprint = sha256Hex(chain[0].encoded)
        if (expected != null) {
          if (fingerprint != expected) {
            throw CertificateException("gateway TLS fingerprint mismatch")
          }
          return
        }
        if (params.allowTOFU) {
          onStore?.invoke(fingerprint)
          return
        }
        defaultTrust.checkServerTrusted(chain, authType)
      }

      override fun getAcceptedIssuers(): Array<X509Certificate> = defaultTrust.acceptedIssuers
    }

  val context = SSLContext.getInstance("TLS")
  context.init(null, arrayOf(trustManager), SecureRandom())
  val verifier =
    if (expected != null || params.allowTOFU) {
      // When pinning, we intentionally ignore hostname mismatch (service discovery often yields IPs).
      HostnameVerifier { _, _ -> true }
    } else {
      HttpsURLConnection.getDefaultHostnameVerifier()
    }
  return GatewayTlsConfig(
    sslSocketFactory = context.socketFactory,
    trustManager = trustManager,
    hostnameVerifier = verifier,
  )
}

suspend fun probeGatewayTlsFingerprint(
  host: String,
  port: Int,
  timeoutMs: Int = 10_000,
): String? {
  val trimmedHost = host.trim()
  if (trimmedHost.isEmpty()) return null
  if (port !in 1..65535) return null

  return withContext(Dispatchers.IO) {
    val trustAll =
      @SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
      object : X509TrustManager {
        @SuppressLint("TrustAllX509TrustManager")
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        @SuppressLint("TrustAllX509TrustManager")
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
      }

    val context = SSLContext.getInstance("TLS")
    context.init(null, arrayOf(trustAll), SecureRandom())
    val socketFactory = context.socketFactory

    probeGatewayTlsFingerprintWithHttps(
      host = trimmedHost,
      port = port,
      timeoutMs = timeoutMs,
      socketFactory = socketFactory,
      trustManager = trustAll,
    ) ?: probeGatewayTlsFingerprintWithDirectSocket(
      host = trimmedHost,
      port = port,
      timeoutMs = timeoutMs,
      socketFactory = socketFactory,
    ) ?: probeGatewayTlsFingerprintWithSocket(
      host = trimmedHost,
      port = port,
      timeoutMs = timeoutMs,
      socketFactory = socketFactory,
    )
  }
}

private fun probeGatewayTlsFingerprintWithHttps(
  host: String,
  port: Int,
  timeoutMs: Int,
  socketFactory: SSLSocketFactory,
  trustManager: X509TrustManager,
): String? {
  val client =
    OkHttpClient.Builder()
      .connectTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
      .readTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
      .callTimeout((timeoutMs * 2L).coerceAtLeast(timeoutMs.toLong()), TimeUnit.MILLISECONDS)
      .followRedirects(false)
      .followSslRedirects(false)
      .retryOnConnectionFailure(false)
      .sslSocketFactory(socketFactory, trustManager)
      .hostnameVerifier(HostnameVerifier { _, _ -> true })
      .build()

  return try {
    val request = Request.Builder().url(buildGatewayHttpsProbeUrl(host, port)).get().build()
    client.newCall(request).execute().use { response ->
      val cert = response.handshake?.peerCertificates?.firstOrNull() as? X509Certificate ?: return null
      sha256Hex(cert.encoded)
    }
  } catch (_: Throwable) {
    null
  } finally {
    client.dispatcher.executorService.shutdown()
    client.connectionPool.evictAll()
  }
}

private fun probeGatewayTlsFingerprintWithSocket(
  host: String,
  port: Int,
  timeoutMs: Int,
  socketFactory: SSLSocketFactory,
): String? {
  val addresses =
    runCatching {
      InetAddress.getAllByName(host)
        .sortedWith(compareBy<InetAddress>({ if (it is Inet4Address) 0 else 1 }, { it.hostAddress ?: "" }))
    }.getOrElse { emptyList() }
  if (addresses.isEmpty()) {
    return null
  }

  for (address in addresses) {
    val plainSocket = Socket()
    var socket: SSLSocket? = null
    try {
      plainSocket.soTimeout = timeoutMs
      plainSocket.connect(InetSocketAddress(address, port), timeoutMs)
      socket = socketFactory.createSocket(plainSocket, host, port, true) as SSLSocket
      socket.soTimeout = timeoutMs

      try {
        if (host.any { it.isLetter() }) {
          val params = SSLParameters()
          params.serverNames = listOf(SNIHostName(host))
          socket.sslParameters = params
        }
      } catch (_: Throwable) {
        // ignore
      }

      socket.startHandshake()
      val cert = socket.session.peerCertificates.firstOrNull() as? X509Certificate ?: continue
      return sha256Hex(cert.encoded)
    } catch (_: Throwable) {
      // Try the next resolved address.
    } finally {
      try {
        socket?.close()
      } catch (_: Throwable) {
        // ignore
      }
      try {
        plainSocket.close()
      } catch (_: Throwable) {
        // ignore
      }
    }
  }

  return null
}

private fun probeGatewayTlsFingerprintWithDirectSocket(
  host: String,
  port: Int,
  timeoutMs: Int,
  socketFactory: SSLSocketFactory,
): String? {
  var socket: SSLSocket? = null
  return try {
    socket = socketFactory.createSocket(host, port) as SSLSocket
    socket.soTimeout = timeoutMs

    try {
      if (host.any { it.isLetter() }) {
        val params = SSLParameters()
        params.serverNames = listOf(SNIHostName(host))
        socket.sslParameters = params
      }
    } catch (_: Throwable) {
      // ignore
    }

    socket.startHandshake()
    val cert = socket.session.peerCertificates.firstOrNull() as? X509Certificate ?: null
    cert?.let { sha256Hex(it.encoded) }
  } catch (_: Throwable) {
    null
  } finally {
    try {
      socket?.close()
    } catch (_: Throwable) {
      // ignore
    }
  }
}

internal fun buildGatewayHttpsProbeUrl(host: String, port: Int): String {
  val formattedHost =
    if (host.contains(":") && !host.startsWith("[")) {
      "[$host]"
    } else {
      host
    }
  return "https://$formattedHost:$port/"
}

private fun defaultTrustManager(): X509TrustManager {
  val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
  factory.init(null as java.security.KeyStore?)
  val trust =
    factory.trustManagers.firstOrNull { it is X509TrustManager } as? X509TrustManager
  return trust ?: throw IllegalStateException("No default X509TrustManager found")
}

private fun sha256Hex(data: ByteArray): String {
  val digest = MessageDigest.getInstance("SHA-256").digest(data)
  val out = StringBuilder(digest.size * 2)
  for (byte in digest) {
    out.append(String.format(Locale.US, "%02x", byte))
  }
  return out.toString()
}

private fun normalizeFingerprint(raw: String): String {
  val stripped =
    raw.trim()
      .substringAfterLast('=', missingDelimiterValue = raw.trim())
      .replace(Regex("^sha-?256\\s*(fingerprint)?\\s*:?\\s*", RegexOption.IGNORE_CASE), "")
  return stripped.lowercase(Locale.US).filter { it in '0'..'9' || it in 'a'..'f' }
}

internal fun normalizeGatewayFingerprintOrNull(raw: String): String? {
  val normalized = normalizeFingerprint(raw)
  return normalized.takeIf { it.length == 64 }
}
