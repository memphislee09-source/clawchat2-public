package ai.openclaw.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ai.openclaw.app.MainViewModel

@Composable
fun ConnectTabScreen(viewModel: MainViewModel) {
  val statusText by viewModel.statusText.collectAsState()
  val isConnected by viewModel.isConnected.collectAsState()
  val remoteAddress by viewModel.remoteAddress.collectAsState()
  val manualHost by viewModel.manualHost.collectAsState()
  val manualPort by viewModel.manualPort.collectAsState()
  val manualTls by viewModel.manualTls.collectAsState()
  val manualEnabled by viewModel.manualEnabled.collectAsState()
  val gatewayToken by viewModel.gatewayToken.collectAsState()
  val tailscaleHost by viewModel.tailscaleHost.collectAsState()
  val tailscalePort by viewModel.tailscalePort.collectAsState()
  val pendingTrust by viewModel.pendingGatewayTrust.collectAsState()

  var advancedOpen by rememberSaveable { mutableStateOf(false) }
  var inputMode by
    remember(manualEnabled, manualHost, gatewayToken) {
      mutableStateOf(
        if (manualEnabled || manualHost.isNotBlank() || gatewayToken.trim().isNotEmpty()) {
          if (manualHost.contains(".ts.net", ignoreCase = true)) ConnectGatewayInputMode.Tailscale else ConnectGatewayInputMode.Manual
        } else {
          ConnectGatewayInputMode.SetupCode
        },
      )
    }
  var setupCode by rememberSaveable { mutableStateOf("") }
  var manualHostInput by rememberSaveable { mutableStateOf(manualHost.ifBlank { "10.0.2.2" }) }
  var manualPortInput by rememberSaveable { mutableStateOf(manualPort.toString()) }
  var manualTlsInput by rememberSaveable { mutableStateOf(manualTls) }
  var tailscaleHostInput by rememberSaveable { mutableStateOf(tailscaleHost.ifBlank { "100.103.47.113" }) }
  var tailscalePortInput by rememberSaveable { mutableStateOf(tailscalePort.toString()) }
  var passwordInput by rememberSaveable { mutableStateOf("") }
  var validationText by rememberSaveable { mutableStateOf<String?>(null) }

  if (pendingTrust != null) {
    val prompt = pendingTrust!!
    AlertDialog(
      onDismissRequest = { viewModel.declineGatewayTrustPrompt() },
      title = { Text(tr("Trust this gateway?", "信任该网关吗？")) },
      text = {
        Text(
          "First-time TLS connection.\n\nVerify this SHA-256 fingerprint before trusting:\n${prompt.fingerprintSha256}",
          style = mobileCallout,
        )
      },
      confirmButton = {
        TextButton(onClick = { viewModel.acceptGatewayTrustPrompt() }) {
          Text(tr("Trust and continue", "信任并继续"))
        }
      },
      dismissButton = {
        TextButton(onClick = { viewModel.declineGatewayTrustPrompt() }) {
          Text(tr("Cancel", "取消"))
        }
      },
    )
  }

  val setupResolvedEndpoint = remember(setupCode) { decodeGatewaySetupCode(setupCode)?.url?.let { parseGatewayEndpoint(it)?.displayUrl } }
  val manualResolvedEndpoint = remember(manualHostInput, manualPortInput, manualTlsInput) {
    composeGatewayManualUrl(manualHostInput, manualPortInput, manualTlsInput)?.let { parseGatewayEndpoint(it)?.displayUrl }
  }
  val tailscaleResolvedEndpoint = remember(tailscaleHostInput, tailscalePortInput) {
    composeGatewayManualUrl(tailscaleHostInput, tailscalePortInput, tls = true)?.let { parseGatewayEndpoint(it)?.displayUrl }
  }

  val activeEndpoint =
    remember(isConnected, remoteAddress, setupResolvedEndpoint, manualResolvedEndpoint, tailscaleResolvedEndpoint, inputMode) {
      when {
        isConnected && !remoteAddress.isNullOrBlank() -> remoteAddress!!
        inputMode == ConnectGatewayInputMode.SetupCode -> setupResolvedEndpoint ?: "Not set"
        inputMode == ConnectGatewayInputMode.Tailscale -> tailscaleResolvedEndpoint ?: "Not set"
        else -> manualResolvedEndpoint ?: "Not set"
      }
    }

  val primaryLabel = if (isConnected) tr("Disconnect Gateway", "断开网关") else tr("Connect Gateway", "连接网关")

  Column(
    modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(tr("Connection Control", "连接控制"), style = mobileCaption1.copy(fontWeight = FontWeight.Bold), color = mobileAccent)
      Text(tr("Gateway Connection", "网关连接"), style = mobileTitle1, color = mobileText)
      Text(
        "One primary action. Open advanced controls only when needed.",
        style = mobileCallout,
        color = mobileTextSecondary,
      )
    }

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      color = mobileSurface,
      border = BorderStroke(1.dp, mobileBorder),
    ) {
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(tr("Active endpoint", "当前端点"), style = mobileCaption1.copy(fontWeight = FontWeight.SemiBold), color = mobileTextSecondary)
        Text(activeEndpoint, style = mobileBody.copy(fontFamily = FontFamily.Monospace), color = mobileText)
      }
    }

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      color = mobileSurface,
      border = BorderStroke(1.dp, mobileBorder),
    ) {
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(tr("Gateway state", "网关状态"), style = mobileCaption1.copy(fontWeight = FontWeight.SemiBold), color = mobileTextSecondary)
        Text(statusText, style = mobileBody, color = mobileText)
      }
    }

    Button(
      onClick = {
        if (isConnected) {
          viewModel.disconnect()
          validationText = null
          return@Button
        }
        if (statusText.contains("operator offline", ignoreCase = true)) {
          validationText = null
          viewModel.refreshGatewayConnection()
          return@Button
        }

        val config =
          resolveGatewayConnectConfig(
            inputMode = inputMode,
            setupCode = setupCode,
            manualHost = manualHostInput,
            manualPort = manualPortInput,
            manualTls = manualTlsInput,
            tailscaleHost = tailscaleHostInput,
            tailscalePort = tailscalePortInput,
            fallbackToken = gatewayToken,
            fallbackPassword = passwordInput,
          )

        if (config == null) {
          validationText =
            when (inputMode) {
              ConnectGatewayInputMode.SetupCode -> "Paste a valid setup code to connect."
              ConnectGatewayInputMode.Tailscale -> "Enter a valid Tailscale host and port to connect."
              ConnectGatewayInputMode.Manual -> "Enter a valid manual host and port to connect."
            }
          return@Button
        }

        validationText = null
        viewModel.setManualEnabled(true)
        viewModel.setManualHost(config.host)
        viewModel.setManualPort(config.port)
        viewModel.setManualTls(config.tls)
        viewModel.setTailscaleHost(tailscaleHostInput)
        viewModel.setTailscalePort(tailscalePortInput.toIntOrNull() ?: 443)
        if (config.token.isNotBlank()) {
          viewModel.setGatewayToken(config.token)
        }
        viewModel.setGatewayPassword(config.password)
        viewModel.connectManual()
      },
      modifier = Modifier.fillMaxWidth().height(52.dp),
      shape = RoundedCornerShape(14.dp),
      colors =
        ButtonDefaults.buttonColors(
          containerColor = if (isConnected) mobileDanger else mobileAccent,
          contentColor = Color.White,
        ),
    ) {
      Text(primaryLabel, style = mobileHeadline.copy(fontWeight = FontWeight.Bold))
    }

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      color = mobileSurface,
      border = BorderStroke(1.dp, mobileBorder),
      onClick = { advancedOpen = !advancedOpen },
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(tr("Advanced controls", "高级控制"), style = mobileHeadline, color = mobileText)
          Text("Setup code, endpoint, TLS, token, password, onboarding.", style = mobileCaption1, color = mobileTextSecondary)
        }
        Icon(
          imageVector = if (advancedOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = if (advancedOpen) "Collapse advanced controls" else "Expand advanced controls",
          tint = mobileTextSecondary,
        )
      }
    }

    AnimatedVisibility(visible = advancedOpen) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, mobileBorder),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(tr("Connection method", "连接方式"), style = mobileCaption1.copy(fontWeight = FontWeight.SemiBold), color = mobileTextSecondary)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MethodChip(
              label = tr("Setup Code", "配置码"),
              active = inputMode == ConnectGatewayInputMode.SetupCode,
              onClick = { inputMode = ConnectGatewayInputMode.SetupCode },
            )
            MethodChip(
              label = tr("Manual", "手动"),
              active = inputMode == ConnectGatewayInputMode.Manual,
              onClick = { inputMode = ConnectGatewayInputMode.Manual },
            )
            MethodChip(
              label = tr("Tailscale", "Tailscale"),
              active = inputMode == ConnectGatewayInputMode.Tailscale,
              onClick = { inputMode = ConnectGatewayInputMode.Tailscale },
            )
          }

          Text("Run these on the gateway host:", style = mobileCallout, color = mobileTextSecondary)
          CommandBlock("openclaw qr --setup-code-only")
          CommandBlock("openclaw qr --json")

          when (inputMode) {
            ConnectGatewayInputMode.SetupCode -> {
              Text(tr("Setup Code", "配置码"), style = mobileCaption1.copy(fontWeight = FontWeight.SemiBold), color = mobileTextSecondary)
              OutlinedTextField(
                value = setupCode,
                onValueChange = {
                  setupCode = it
                  validationText = null
                },
                placeholder = { Text("Paste setup code", style = mobileBody, color = mobileTextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                textStyle = mobileBody.copy(fontFamily = FontFamily.Monospace, color = mobileText),
                shape = RoundedCornerShape(14.dp),
                colors = outlinedColors(),
              )
              if (!setupResolvedEndpoint.isNullOrBlank()) {
                EndpointPreview(endpoint = setupResolvedEndpoint)
              }
            }

            ConnectGatewayInputMode.Manual -> {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickFillChip(
                  label = "Android Emulator",
                  onClick = {
                    manualHostInput = "10.0.2.2"
                    manualPortInput = "18789"
                    manualTlsInput = false
                    validationText = null
                  },
                )
                QuickFillChip(
                  label = "Localhost",
                  onClick = {
                    manualHostInput = "127.0.0.1"
                    manualPortInput = "18789"
                    manualTlsInput = false
                    validationText = null
                  },
                )
              }

              Text(tr("Host", "主机"), style = mobileCaption1.copy(fontWeight = FontWeight.SemiBold), color = mobileTextSecondary)
              OutlinedTextField(
                value = manualHostInput,
                onValueChange = {
                  manualHostInput = it
                  validationText = null
                },
                placeholder = { Text("10.0.2.2", style = mobileBody, color = mobileTextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                textStyle = mobileBody.copy(color = mobileText),
                shape = RoundedCornerShape(14.dp),
                colors = outlinedColors(),
              )

              Text(tr("Port", "端口"), style = mobileCaption1.copy(fontWeight = FontWeight.SemiBold), color = mobileTextSecondary)
              OutlinedTextField(
                value = manualPortInput,
                onValueChange = {
                  manualPortInput = it
                  validationText = null
                },
                placeholder = { Text("18789", style = mobileBody, color = mobileTextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = mobileBody.copy(fontFamily = FontFamily.Monospace, color = mobileText),
                shape = RoundedCornerShape(14.dp),
                colors = outlinedColors(),
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(tr("Use TLS", "启用 TLS"), style = mobileHeadline, color = mobileText)
                  Text("Switch to secure websocket (`wss`).", style = mobileCallout, color = mobileTextSecondary)
                }
                Switch(
                  checked = manualTlsInput,
                  onCheckedChange = {
                    manualTlsInput = it
                    validationText = null
                  },
                  colors =
                    SwitchDefaults.colors(
                      checkedTrackColor = mobileAccent,
                      uncheckedTrackColor = mobileBorderStrong,
                      checkedThumbColor = Color.White,
                      uncheckedThumbColor = Color.White,
                    ),
                )
              }

              if (!manualResolvedEndpoint.isNullOrBlank()) {
                EndpointPreview(endpoint = manualResolvedEndpoint)
              }
            }

            ConnectGatewayInputMode.Tailscale -> {
              Text(
                "Use your Tailnet gateway address (for example: openclaw.yourname.ts.net).",
                style = mobileCallout,
                color = mobileTextSecondary,
              )
              Text(tr("Tailscale host", "Tailscale 主机"), style = mobileCaption1.copy(fontWeight = FontWeight.SemiBold), color = mobileTextSecondary)
              OutlinedTextField(
                value = tailscaleHostInput,
                onValueChange = {
                  tailscaleHostInput = it
                  validationText = null
                },
                placeholder = { Text("openclaw.yourname.ts.net", style = mobileBody, color = mobileTextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                textStyle = mobileBody.copy(color = mobileText),
                shape = RoundedCornerShape(14.dp),
                colors = outlinedColors(),
              )

              Text(tr("Port", "端口"), style = mobileCaption1.copy(fontWeight = FontWeight.SemiBold), color = mobileTextSecondary)
              OutlinedTextField(
                value = tailscalePortInput,
                onValueChange = {
                  tailscalePortInput = it
                  validationText = null
                },
                placeholder = { Text("443", style = mobileBody, color = mobileTextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = mobileBody.copy(fontFamily = FontFamily.Monospace, color = mobileText),
                shape = RoundedCornerShape(14.dp),
                colors = outlinedColors(),
              )

              if (!tailscaleResolvedEndpoint.isNullOrBlank()) {
                EndpointPreview(endpoint = tailscaleResolvedEndpoint)
              }
            }
          }

          Text(tr("Token (optional)", "令牌（可选）"), style = mobileCaption1.copy(fontWeight = FontWeight.SemiBold), color = mobileTextSecondary)
          OutlinedTextField(
            value = gatewayToken,
            onValueChange = { viewModel.setGatewayToken(it) },
            placeholder = { Text("token", style = mobileBody, color = mobileTextTertiary) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            textStyle = mobileBody.copy(color = mobileText),
            shape = RoundedCornerShape(14.dp),
            colors = outlinedColors(),
          )

          Text(tr("Password (optional)", "密码（可选）"), style = mobileCaption1.copy(fontWeight = FontWeight.SemiBold), color = mobileTextSecondary)
          OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            placeholder = { Text("password", style = mobileBody, color = mobileTextTertiary) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            textStyle = mobileBody.copy(color = mobileText),
            shape = RoundedCornerShape(14.dp),
            colors = outlinedColors(),
          )

          HorizontalDivider(color = mobileBorder)

          TextButton(onClick = { viewModel.setOnboardingCompleted(false) }) {
            Text(tr("Run onboarding again", "重新运行引导"), style = mobileCallout.copy(fontWeight = FontWeight.SemiBold), color = mobileAccent)
          }
        }
      }
    }

    if (!validationText.isNullOrBlank()) {
      Text(validationText!!, style = mobileCaption1, color = mobileWarning)
    }
  }
}

@Composable
private fun MethodChip(label: String, active: Boolean, onClick: () -> Unit) {
  Button(
    onClick = onClick,
    modifier = Modifier.height(40.dp),
    shape = RoundedCornerShape(12.dp),
    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    colors =
      ButtonDefaults.buttonColors(
        containerColor = if (active) mobileAccent else mobileSurface,
        contentColor = if (active) Color.White else mobileText,
      ),
    border = BorderStroke(1.dp, if (active) Color(0xFF184DAF) else mobileBorderStrong),
  ) {
    Text(label, style = mobileCaption1.copy(fontWeight = FontWeight.Bold))
  }
}

@Composable
private fun QuickFillChip(label: String, onClick: () -> Unit) {
  Button(
    onClick = onClick,
    shape = RoundedCornerShape(999.dp),
    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    colors =
      ButtonDefaults.buttonColors(
        containerColor = mobileAccentSoft,
        contentColor = mobileAccent,
      ),
    elevation = null,
  ) {
    Text(label, style = mobileCaption1.copy(fontWeight = FontWeight.SemiBold))
  }
}

@Composable
private fun CommandBlock(command: String) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    color = mobileCodeBg,
    border = BorderStroke(1.dp, Color(0xFF2B2E35)),
  ) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Box(modifier = Modifier.width(3.dp).height(42.dp).background(Color(0xFF3FC97A)))
      Text(
        text = command,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        style = mobileCallout.copy(fontFamily = FontFamily.Monospace),
        color = mobileCodeText,
      )
    }
  }
}

@Composable
private fun EndpointPreview(endpoint: String) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    HorizontalDivider(color = mobileBorder)
    Text(tr("Resolved endpoint", "解析后的端点"), style = mobileCaption1.copy(fontWeight = FontWeight.SemiBold), color = mobileTextSecondary)
    Text(endpoint, style = mobileCallout.copy(fontFamily = FontFamily.Monospace), color = mobileText)
    HorizontalDivider(color = mobileBorder)
  }
}

@Composable
private fun outlinedColors() =
  OutlinedTextFieldDefaults.colors(
    focusedContainerColor = mobileSurface,
    unfocusedContainerColor = mobileSurface,
    focusedBorderColor = mobileAccent,
    unfocusedBorderColor = mobileBorder,
    focusedTextColor = mobileText,
    unfocusedTextColor = mobileText,
    cursorColor = mobileAccent,
  )
