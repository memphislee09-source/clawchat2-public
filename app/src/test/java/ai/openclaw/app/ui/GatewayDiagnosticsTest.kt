package ai.openclaw.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class GatewayDiagnosticsTest {
  @Test
  fun classifyGatewayRoute_detectsTailscale() {
    assertEquals(
      "Tailscale",
      classifyGatewayRoute("https://memphismac-mini.tail154c1d.ts.net:443"),
    )
  }

  @Test
  fun classifyGatewayRoute_detectsEmulatorLoopbackAsSameMachine() {
    assertEquals(
      "same machine",
      classifyGatewayRoute("http://10.0.2.2:18789"),
    )
  }

  @Test
  fun classifyGatewayFailure_detectsTlsTrustErrors() {
    assertEquals(
      "TLS trust",
      classifyGatewayFailure("Failed: can't read TLS fingerprint"),
    )
  }
}
