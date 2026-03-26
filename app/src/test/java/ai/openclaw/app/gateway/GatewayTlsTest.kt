package ai.openclaw.app.gateway

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GatewayTlsTest {
  @Test
  fun buildGatewayHttpsProbeUrl_formatsDomainHost() {
    assertEquals(
      "https://example.ts.net:443/",
      buildGatewayHttpsProbeUrl(host = "example.ts.net", port = 443),
    )
  }

  @Test
  fun buildGatewayHttpsProbeUrl_wrapsIpv6Hosts() {
    assertEquals(
      "https://[fd7a:115c:a1e0::1]:443/",
      buildGatewayHttpsProbeUrl(host = "fd7a:115c:a1e0::1", port = 443),
    )
  }

  @Test
  fun normalizeGatewayFingerprintOrNull_acceptsOpensslOutput() {
    assertEquals(
      "aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899",
      normalizeGatewayFingerprintOrNull(
        "sha256 Fingerprint=AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99",
      ),
    )
  }

  @Test
  fun normalizeGatewayFingerprintOrNull_rejectsWrongLength() {
    assertNull(normalizeGatewayFingerprintOrNull("sha256 Fingerprint=AA:BB"))
  }
}
